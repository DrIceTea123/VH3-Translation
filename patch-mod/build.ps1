param(
    [string]$JavaHome,
    [string[]]$GradleArgs = @('build', '--console=plain'),
    [switch]$BumpPatch,
    [switch]$BumpCore
)
$ErrorActionPreference = 'Stop'
$previousJavaHome = $env:JAVA_HOME
$previousGradleHome = $env:GRADLE_USER_HOME
try {
    if (!$JavaHome) {
        $candidates = @($env:JAVA_HOME, (Join-Path $env:ProgramFiles 'Java\jdk-17'))
        foreach ($candidate in $candidates) {
            if (!$candidate) { continue }
            $release = Join-Path $candidate 'release'
            if ((Test-Path -LiteralPath $release) -and
                (Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="17[.\"]' -Quiet)) {
                $JavaHome = $candidate
                break
            }
        }
    }
    if (!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) {
        throw 'A JDK 17 installation is required. Pass -JavaHome <JDK17 directory>.'
    }
    if (!(Select-String -LiteralPath (Join-Path $JavaHome 'release') -Pattern '^JAVA_VERSION="17[.\"]' -Quiet)) {
        throw 'This build requires JDK 17.'
    }
    $env:JAVA_HOME = $JavaHome
    if (!$env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle' }
    if ($BumpPatch -and $BumpCore) { throw 'Choose either -BumpPatch or -BumpCore, not both.' }
    if ($BumpPatch -or $BumpCore) {
        if ($GradleArgs -notcontains 'build' -and $GradleArgs -notcontains 'exportToProgram') {
            throw 'Version increments require the build or exportToProgram task.'
        }
        $propertiesPath = Join-Path $PSScriptRoot 'gradle.properties'
        $propertiesText = [System.IO.File]::ReadAllText($propertiesPath)
        $versionPattern = '(?m)^mod_version=1\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?=\r?$)'
        $versionMatches = [regex]::Matches($propertiesText, $versionPattern)
        if ($versionMatches.Count -ne 1) { throw 'Expected exactly one 1.x.x mod_version in gradle.properties.' }
        $minorVersion = [long]::Parse($versionMatches[0].Groups[1].Value)
        $patchVersion = [long]::Parse($versionMatches[0].Groups[2].Value)
        if ($patchVersion -eq [long]::MaxValue) { throw 'Patch version overflow.' }
        if ($BumpCore) {
            if ($minorVersion -eq [long]::MaxValue) { throw 'Minor version overflow.' }
            $minorVersion++
        }
        # 核心更新也累加修订号，不归零：例如 1.0.3 -> 1.1.4。
        $nextVersion = '1.{0}.{1}' -f $minorVersion, ($patchVersion + 1)
        $updatedProperties = [regex]::Replace($propertiesText, $versionPattern, "mod_version=$nextVersion")
        [System.IO.File]::WriteAllText($propertiesPath, $updatedProperties, [System.Text.UTF8Encoding]::new($false))
        Write-Host "Mod version updated to $nextVersion. If the build fails, retry without version increment switches."
    }
    Push-Location -LiteralPath $PSScriptRoot
    try {
        & (Join-Path $PSScriptRoot 'gradlew.bat') @GradleArgs
        $buildExit = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:GRADLE_USER_HOME = $previousGradleHome
}
exit $buildExit
