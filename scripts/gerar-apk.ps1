. "$PSScriptRoot\ambiente.ps1"
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Output 'APK: app\build\outputs\apk\debug\app-debug.apk'
} finally { Pop-Location }
