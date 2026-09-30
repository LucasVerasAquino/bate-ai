. "$PSScriptRoot\ambiente.ps1"
Push-Location $projectRoot
try {
    & .\gradlew.bat :demo-web:run --console=plain
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
