<#
.SYNOPSIS
Builds the debug APK signed with the keystore from scripts/.env, installs it on the connected
device and starts it. See BUILDING.md.

.EXAMPLE
scripts\run-debug.ps1             # build, install, launch
scripts\run-debug.ps1 -NoLaunch   # build and install only
#>
param([switch]$NoLaunch)

$ErrorActionPreference = 'Stop'
$ScriptDir = $PSScriptRoot
$Root = Split-Path $ScriptDir -Parent
$EnvFile = Join-Path $ScriptDir '.env'
$AppId = 'pl.cukrzycowy.labelbite'
$Apk = Join-Path $Root 'app/build/outputs/apk/debug/app-debug.apk'

function Fail([string]$Message) { Write-Error "error: $Message" -ErrorAction Continue; exit 1 }

# --- read scripts/.env ------------------------------------------------------------------------
if (-not (Test-Path $EnvFile)) { Fail "$EnvFile not found. Copy scripts/.env.example to scripts/.env and fill it in." }

$cfg = @{}
foreach ($line in Get-Content $EnvFile) {
    if ($line -match '^\s*(#|$)' -or $line -notmatch '=') { continue }
    $key, $value = $line -split '=', 2
    $key = $key.Trim(); $value = $value.Trim()
    if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") { $value = $Matches[1] }
    $cfg[$key] = $value
}
foreach ($k in 'KEYSTORE_PATH', 'KEYSTORE_PASSWORD', 'KEY_ALIAS', 'KEY_PASSWORD') {
    if (-not $cfg[$k]) { Fail "$k is not set in $EnvFile" }
}

# Relative keystore paths are relative to scripts/.
$keystore = $cfg['KEYSTORE_PATH']
if (-not [IO.Path]::IsPathRooted($keystore)) { $keystore = Join-Path $ScriptDir $keystore }
if (-not (Test-Path $keystore -PathType Leaf)) { Fail "keystore not found: $keystore" }
$keystore = (Resolve-Path $keystore).Path

# --- find adb ---------------------------------------------------------------------------------
function Find-Adb {
    if ($env:ADB) { return $env:ADB }
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
    $props = Join-Path $Root 'local.properties'
    if (-not $sdk -and (Test-Path $props)) {
        $line = Select-String -Path $props -Pattern '^sdk\.dir=(.*)$' | Select-Object -First 1
        if ($line) { $sdk = $line.Matches[0].Groups[1].Value -replace '\\:', ':' -replace '\\\\', '\' }
    }
    if ($sdk) {
        foreach ($name in 'adb.exe', 'adb') {
            $candidate = Join-Path $sdk "platform-tools/$name"
            if (Test-Path $candidate) { return $candidate }
        }
    }
    return $null
}
$adb = Find-Adb
if (-not $adb) { Fail 'adb not found. Put platform-tools on PATH, or set ANDROID_HOME or ADB.' }

if ($cfg['ANDROID_SERIAL']) { $env:ANDROID_SERIAL = $cfg['ANDROID_SERIAL'] }
$devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' } | ForEach-Object { ($_ -split '\t')[0] })
if (-not $env:ANDROID_SERIAL) {
    if ($devices.Count -eq 0) { Fail "no device connected (or it is unauthorised). Check 'adb devices'." }
    if ($devices.Count -gt 1) { Fail "$($devices.Count) devices connected; set ANDROID_SERIAL in scripts/.env to pick one." }
}

# --- pick a JDK 17+ ---------------------------------------------------------------------------
function Get-JavaMajor([string]$JdkHome) {
    if (-not $JdkHome) { return 0 }
    $bin = Join-Path $JdkHome 'bin/java.exe'
    if (-not (Test-Path $bin)) { $bin = Join-Path $JdkHome 'bin/java' }
    if (-not (Test-Path $bin)) { return 0 }
    $first = (& cmd /c "`"$bin`" -version 2>&1" | Select-Object -First 1) -as [string]
    if ($first -match 'version "(1\.)?(\d+)') { return [int]$Matches[2] }
    return 0
}
if ($cfg['JAVA_HOME']) {
    $env:JAVA_HOME = $cfg['JAVA_HOME']
} elseif ((Get-JavaMajor $env:JAVA_HOME) -lt 17) {
    # Fall back to the JDK bundled with Android Studio.
    $candidates = @(
        (Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'),
        '/Applications/Android Studio.app/Contents/jbr/Contents/Home',
        '/opt/android-studio/jbr'
    )
    foreach ($jbr in $candidates) {
        if ((Get-JavaMajor $jbr) -ge 17) { $env:JAVA_HOME = $jbr; break }
    }
}
if ($env:JAVA_HOME -and (Get-JavaMajor $env:JAVA_HOME) -lt 17) {
    Fail "JDK 17 or newer is needed; JAVA_HOME is $env:JAVA_HOME. Set JAVA_HOME in scripts/.env."
}

# --- build ------------------------------------------------------------------------------------
$env:DEBUG_SIGNING_KEYSTORE_PATH = $keystore
$env:DEBUG_SIGNING_KEYSTORE_PASSWORD = $cfg['KEYSTORE_PASSWORD']
$env:DEBUG_SIGNING_KEY_ALIAS = $cfg['KEY_ALIAS']
$env:DEBUG_SIGNING_KEY_PASSWORD = $cfg['KEY_PASSWORD']

Write-Host '==> Building debug APK'
Push-Location $Root
try {
    & (Join-Path $Root 'gradlew.bat') assembleDebug
    if ($LASTEXITCODE -ne 0) { Fail 'build failed' }
} finally { Pop-Location }
if (-not (Test-Path $Apk)) { Fail "APK not found at $Apk" }

# --- install and run --------------------------------------------------------------------------
$target = if ($env:ANDROID_SERIAL) { $env:ANDROID_SERIAL } else { $devices[0] }
Write-Host "==> Installing on $target"
$out = & $adb install -r $Apk 2>&1 | Out-String
Write-Host $out.Trim()
if ($LASTEXITCODE -ne 0) {
    if ($out -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
        Fail "the installed app is signed with a different key. Uninstall it first (this deletes its data): adb uninstall $AppId"
    }
    Fail 'install failed'
}

if (-not $NoLaunch) {
    Write-Host "==> Starting $AppId"
    & $adb shell am start -n "$AppId/.MainActivity" | Out-Null
}
Write-Host 'Done.'
