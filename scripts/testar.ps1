. "$PSScriptRoot\ambiente.ps1"
Push-Location $projectRoot
try {
    & .\gradlew.bat :core:test :demo-web:test :app:lintDebug :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
