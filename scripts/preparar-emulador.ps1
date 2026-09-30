. "$PSScriptRoot\ambiente.ps1"
$toolsRoot = Join-Path $projectRoot '.tools'
$imageArchive = Join-Path $toolsRoot 'android36-image.zip'
$imageRoot = Join-Path $toolsRoot 'android36'
$avdRoot = Join-Path $toolsRoot 'avd'
$avdDirectory = Join-Path $avdRoot 'BateAi_API36.avd'
New-Item -ItemType Directory -Force -Path $toolsRoot,$imageRoot,$avdDirectory | Out-Null
if (-not (Test-Path -LiteralPath $imageArchive)) {
    & curl.exe --fail --location --silent --show-error 'https://dl.google.com/android/repository/sys-img/android/x86_64-36_r02.zip' --output $imageArchive
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível baixar a imagem oficial do Android.' }
}
$imageChecksum = (Get-FileHash -Algorithm SHA1 -LiteralPath $imageArchive).Hash.ToLowerInvariant()
if ($imageChecksum -ne '829c076e8ff448a336097ae25a355b495ba36e2c') { throw 'Checksum da imagem Android divergente.' }
if (-not (Test-Path -LiteralPath (Join-Path $imageRoot 'x86_64\system.img'))) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($imageArchive, $imageRoot)
}
$imageDirectory = Join-Path $imageRoot 'x86_64'
$avdConfig = @(
    'AvdId=BateAi_API36', 'avd.ini.encoding=UTF-8', 'PlayStore.enabled=false',
    'abi.type=x86_64', 'hw.cpu.arch=x86_64', 'hw.cpu.ncore=2',
    'hw.ramSize=1536', 'hw.gpu.enabled=yes', 'hw.gpu.mode=swiftshader',
    'hw.lcd.width=1080', 'hw.lcd.height=1920', 'hw.lcd.density=420',
    'hw.keyboard=yes', 'hw.initialOrientation=Portrait', 'disk.dataPartition.size=2G',
    'showDeviceFrame=no', 'tag.id=default', 'tag.display=Default', 'target=android-36',
    "image.sysdir.1=$imageDirectory\"
)
[IO.File]::WriteAllLines((Join-Path $avdDirectory 'config.ini'), $avdConfig, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllLines((Join-Path $avdRoot 'BateAi_API36.ini'), @('avd.ini.encoding=UTF-8', "path=$avdDirectory", 'target=android-36'), [Text.UTF8Encoding]::new($false))
Write-Output 'Emulador preparado. Execute scripts/iniciar-emulador.ps1 para abrir.'
