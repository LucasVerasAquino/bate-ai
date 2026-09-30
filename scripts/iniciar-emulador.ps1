param([switch]$SemJanela)
. "$PSScriptRoot\ambiente.ps1"
$env:ANDROID_AVD_HOME = Join-Path $projectRoot '.tools\avd'
if (-not (Test-Path -LiteralPath (Join-Path $env:ANDROID_AVD_HOME 'BateAi_API36.ini'))) {
    throw 'Execute scripts/preparar-emulador.ps1 primeiro.'
}
$emulatorExecutable = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$emulatorArguments = @('-avd', 'BateAi_API36', '-no-snapshot', '-no-boot-anim', '-gpu', 'swiftshader', '-no-audio')
if ($SemJanela) { $emulatorArguments += '-no-window' }
& $emulatorExecutable @emulatorArguments
