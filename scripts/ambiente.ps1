$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaCandidates = @(
    $env:BATEAI_JAVA_HOME,
    'C:\Program Files\Amazon Corretto\jdk25.0.2_10',
    'C:\Program Files\Android\Android Studio\jbr',
    $env:JAVA_HOME
)
$javaRuntime = $javaCandidates | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe')) } | Select-Object -First 1
if (-not $javaRuntime) { throw 'Configure BATEAI_JAVA_HOME com um JDK 17 a 25.' }
$env:JAVA_HOME = $javaRuntime
$env:DEBUG = $null
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools\android-user'
New-Item -ItemType Directory -Force -Path $env:ANDROID_USER_HOME | Out-Null
$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
