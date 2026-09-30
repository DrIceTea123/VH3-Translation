param(
    [string]$JavaHome,
    [string[]]$GradleArgs = @('build', '--console=plain')
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
