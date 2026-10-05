@echo off
setlocal DisableDelayedExpansion
for /f "tokens=2 delims=:" %%C in ('chcp') do set "launcher_codepage=%%C"
chcp 65001 >nul
set "VH3_INSTALLER_ENTRY=%~f0"
set "VH3_INSTALLER_MODE=%~1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$s=[IO.File]::ReadAllText($env:VH3_INSTALLER_ENTRY,[Text.Encoding]::UTF8); & ([scriptblock]::Create(($s -split '(?m)^# POWERSHELL_BEGIN\r?$',2)[1]))"
set "launcher_exit=%errorlevel%"
if not "%launcher_exit%"=="0" pause
if defined launcher_codepage chcp %launcher_codepage% >nul
exit /b %launcher_exit%
# POWERSHELL_BEGIN
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = $OutputEncoding
$installerArguments = @()
if ($env:VH3_INSTALLER_MODE) {
    if ($env:VH3_INSTALLER_MODE -notin @('--check', '--console')) { Write-Host '仅支持 --check 或 --console 参数。'; exit 1 }
    $installerArguments = @($env:VH3_INSTALLER_MODE)
}

function Test-Java17([string]$Candidate) {
    if (-not $Candidate -or -not (Test-Path -LiteralPath $Candidate -PathType Leaf)) { return $false }
    try {
        $start = New-Object System.Diagnostics.ProcessStartInfo
        $start.FileName = $Candidate
        $start.Arguments = '-version'
        $start.UseShellExecute = $false
        $start.CreateNoWindow = $true
        $start.RedirectStandardError = $true
        $start.RedirectStandardOutput = $true
        $process = [System.Diagnostics.Process]::Start($start)
        try {
            $version = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
            $process.WaitForExit()
            return $process.ExitCode -eq 0 -and $version -match '(?m)^(?:openjdk|java)(?:\s+version)?\s+"?(?:1\.)?(\d+)' -and [int]$Matches[1] -ge 17
        } finally { $process.Dispose() }
    } catch { return $false }
}

# 按数字段比较版本，确保 2.10 高于 2.9，也不依赖固定整数宽度。
function Compare-NumberText([string]$Left, [string]$Right) {
    $leftValue = $Left.TrimStart([char]'0'); if (-not $leftValue) { $leftValue = '0' }
    $rightValue = $Right.TrimStart([char]'0'); if (-not $rightValue) { $rightValue = '0' }
    if ($leftValue.Length -ne $rightValue.Length) { return $leftValue.Length.CompareTo($rightValue.Length) }
    return [string]::CompareOrdinal($leftValue, $rightValue)
}
function Compare-TranslationVersion([string]$Left, [string]$Right) {
    $leftParts = $Left.Split('.'); $rightParts = $Right.Split('.')
    for ($i = 0; $i -lt [Math]::Max($leftParts.Length, $rightParts.Length); $i++) {
        $leftPart = if ($i -lt $leftParts.Length) { $leftParts[$i] } else { '0' }
        $rightPart = if ($i -lt $rightParts.Length) { $rightParts[$i] } else { '0' }
        $comparison = Compare-NumberText $leftPart $rightPart
        if ($comparison -ne 0) { return $comparison }
    }
    return 0
}

try {
    $bundleDirectory = Split-Path -Parent $env:VH3_INSTALLER_ENTRY
    $jars = @(Get-ChildItem -LiteralPath $bundleDirectory -Filter '*.jar' -File | Sort-Object Name)
    if ($jars.Count -eq 0) { throw '同目录没有找到安装器 JAR。请将启动文件与安装器 JAR 放在一起。' }
    $latest = $null
    $latestVersion = '0'; $latestSerial = '0'
    $duplicateLatest = $false
    foreach ($jar in $jars) {
        if ($jar.Name -cmatch '-V([0-9]+(?:\.[0-9]+)*)-([1-9][0-9]*)\.jar$') {
            $version = $Matches[1]; $serial = $Matches[2]
            $comparison = Compare-TranslationVersion $version $latestVersion
            if ($comparison -eq 0) { $comparison = Compare-NumberText $serial $latestSerial }
            if (-not $latest -or $comparison -gt 0) {
                $latest = $jar; $latestVersion = $version; $latestSerial = $serial; $duplicateLatest = $false
            } elseif ($comparison -eq 0) { $duplicateLatest = $true }
        }
    }
    if ($duplicateLatest) { throw '存在汉化包版本和导出序列号均相同的最新 JAR，请只保留需要启动的那个文件。' }
    if (-not $latest -and $jars.Count -eq 1) { $latest = $jars[0] }
    if (-not $latest) { throw '无法确定最新安装器。请保留文件名末尾的 -V汉化包版本-导出序列号.jar，或只保留一个 JAR。' }
    $jarPath = $latest.FullName
    Write-Host ("自动启动：{0}" -f $latest.Name)
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            if (-not $archive.GetEntry('cn/vmct/installer/Main.class')) { throw 'installer entry missing' }
        } finally { $archive.Dispose() }
    } catch { throw '最新文件不是有效的汉化安装器，或文件已损坏。请重新获取安装器。' }

    $candidates = @((Join-Path $bundleDirectory 'runtime/bin/java.exe'))
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin/java.exe' }
    $candidates += @(Get-Command java.exe -CommandType Application -All -ErrorAction SilentlyContinue | ForEach-Object Source)
    $selectedJava = $null
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-Java17 $candidate) { $selectedJava = $candidate; break }
    }
    if (-not $selectedJava) { throw '未找到 Java 17 或更新版本。请安装适合本系统的 Java 17+ 并启用 PATH，或将 JAVA_HOME 设置为已有 Java 的目录，然后重新运行。无需修改 JAR 文件关联。' }
    Write-Host '正在启动汉化安装器，请稍候……'
    & $selectedJava '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-Dsun.stdout.encoding=UTF-8' '-Dsun.stderr.encoding=UTF-8' '-jar' $jarPath @installerArguments
    $result = $LASTEXITCODE
    if ($result -ne 0) { Write-Host "安装器启动或运行失败（退出码 $result）。请保留上方错误信息；无图形桌面时可以使用 --console 参数。" }
    exit $result
} catch {
    Write-Host $_.Exception.Message
    exit 1
}
