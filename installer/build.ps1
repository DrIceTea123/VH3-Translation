param([switch]$Check)
$ErrorActionPreference = 'Stop'
$previousOutputEncoding = $OutputEncoding
$previousConsoleEncoding = [Console]::OutputEncoding
try {
    $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    [Console]::OutputEncoding = $OutputEncoding
    $javaCommand = if ($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME/bin/java.exe")) {
        "$env:JAVA_HOME/bin/java.exe"
    } else { (Get-Command java -ErrorAction Stop).Source }
    $arguments = @('-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8',
        '-Dsun.stdout.encoding=UTF-8', '-Dsun.stderr.encoding=UTF-8',
        (Join-Path $PSScriptRoot 'tools/Build.java'), '--project', $PSScriptRoot)
    if (-not $Check) { $arguments += '--release' }
    & $javaCommand @arguments
    if ($LASTEXITCODE -ne 0) { throw "Build failed ($LASTEXITCODE). Export serial unchanged." }
} finally {
    [Console]::OutputEncoding = $previousConsoleEncoding
    $OutputEncoding = $previousOutputEncoding
}
