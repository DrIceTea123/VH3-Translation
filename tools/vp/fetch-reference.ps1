$ErrorActionPreference = 'Stop'
# Official Modrinth release 1.5.3-hotfix; upstream filename is 1.5.3-fix.
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$dependencyDir = Join-Path $repoRoot 'local-deps/vp-1.5.3'
[System.IO.Directory]::CreateDirectory($dependencyDir) | Out-Null
$referenceFile = Join-Path $dependencyDir 'vaultpatcher-all-1.5.3-fix.jar'
$expected = 'd79668dd66f3edeffa347917ba10ff7930c9d09788aa86528d4af6514fc6af52'
if (!(Test-Path -LiteralPath $referenceFile) -or (Get-FileHash -LiteralPath $referenceFile -Algorithm SHA256).Hash -ne $expected) {
    Invoke-WebRequest 'https://cdn.modrinth.com/data/NLV0Mnpu/versions/oATpDq2Q/vaultpatcher-all-1.5.3-fix.jar' -OutFile $referenceFile
}
if ((Get-FileHash -LiteralPath $referenceFile -Algorithm SHA256).Hash -ne $expected) {
    throw 'Official VP reference SHA-256 mismatch; do not use this file.'
}
Write-Output 'Verified VaultPatcher 1.5.3-hotfix reference JAR (test dependency only).'
