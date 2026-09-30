. "$PSScriptRoot\ambiente.ps1"
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:connectedDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
