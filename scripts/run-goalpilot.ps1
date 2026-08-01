#Requires -Version 5.1
<#
.SYNOPSIS
    One-click GoalPilot runner. Brings a device up (emulator or plugged-in phone),
    builds the debug APK, installs it, and launches the app.

.DESCRIPTION
    Deliberately does NOT need Android Studio. It only uses the standalone Android
    SDK command-line tools (emulator.exe, adb.exe) plus the Gradle wrapper, so it
    can be run from VS Code, a terminal, or a double-clicked .cmd shortcut without
    a second IDE ever touching the build directory.

    Device selection ('auto' = the default):
      1. a physical phone in state 'device'  -> use it
      2. an emulator already running         -> use it
      3. otherwise                           -> boot the AVD named by -Avd

.PARAMETER Target
    auto     - phone if one is plugged in, else emulator (default)
    emulator - always use / boot the emulator
    device   - require a physical phone; fail if none is connected

.PARAMETER Avd
    Name of the AVD to boot. Defaults to Pixel_10_Pro_XL.

.PARAMETER SkipInstall
    Only bring the device up. Skips the Gradle build, install and app launch.

.PARAMETER ColdBoot
    Boot the emulator from scratch instead of restoring its snapshot.

.PARAMETER Logcat
    After launching, tail logcat filtered to the app's process. Ctrl+C to stop.

.PARAMETER BootTimeoutSec
    How long to wait for the emulator to finish booting. Default 300s.

.EXAMPLE
    .\scripts\run-goalpilot.ps1
.EXAMPLE
    .\scripts\run-goalpilot.ps1 -Target device -Logcat
.EXAMPLE
    .\scripts\run-goalpilot.ps1 -SkipInstall        # just start the emulator
#>
[CmdletBinding()]
param(
    [ValidateSet('auto', 'emulator', 'device')]
    [string]$Target = 'auto',

    [string]$Avd = 'Pixel_10_Pro_XL',

    [string]$Package = 'com.idomarhaim.goalpilot.debug',

    [string]$Activity = 'com.idomarhaim.goalpilot.MainActivity',

    [switch]$SkipInstall,
    [switch]$ColdBoot,
    [switch]$Logcat,

    [int]$BootTimeoutSec = 300
)

# Native tools write progress to stderr; do not let that abort the script.
$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

# ── Output helpers ────────────────────────────────────────────────────────────
function Write-Step { param([string]$Message) Write-Host "==> $Message" -ForegroundColor Cyan }
function Write-Ok { param([string]$Message) Write-Host "    $Message" -ForegroundColor Green }
function Write-Note { param([string]$Message) Write-Host "    $Message" -ForegroundColor DarkGray }
function Write-Warn { param([string]$Message) Write-Host "!!  $Message" -ForegroundColor Yellow }

# Expected, actionable failures exit cleanly. A PowerShell stack trace in a
# double-clicked window is noise, not information.
function Fail {
    param([string]$Message)
    Write-Host ''
    foreach ($line in ($Message -split "`n")) { Write-Host "ERROR: $line" -ForegroundColor Red }
    Write-Host ''
    exit 1
}

# ── Locate the repo ───────────────────────────────────────────────────────────
$RepoRoot = Split-Path -Parent $PSScriptRoot
$Gradlew = Join-Path $RepoRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $Gradlew)) {
    Fail "Gradle wrapper not found at $Gradlew.`nIs this script still inside <repo>\scripts\ ?"
}

# ── Locate the Android SDK ────────────────────────────────────────────────────
function Resolve-AndroidSdk {
    param([string]$Root)

    $localProps = Join-Path $Root 'local.properties'
    if (Test-Path -LiteralPath $localProps) {
        foreach ($line in Get-Content -LiteralPath $localProps) {
            if ($line -match '^\s*sdk\.dir\s*=\s*(.+?)\s*$') {
                # Java properties escaping: Android Studio writes C\:\\Users\\name\\...
                $candidate = $Matches[1].Replace('\\', '\').Replace('\:', ':')
                if (Test-Path -LiteralPath $candidate) { return $candidate }
            }
        }
    }
    foreach ($envVar in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if ($envVar -and (Test-Path -LiteralPath $envVar)) { return $envVar }
    }
    $default = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $default) { return $default }

    Fail "Could not locate the Android SDK.`nSet sdk.dir in local.properties, or set the ANDROID_HOME environment variable."
}

$Sdk = Resolve-AndroidSdk -Root $RepoRoot
$Adb = Join-Path $Sdk 'platform-tools\adb.exe'
$EmulatorExe = Join-Path $Sdk 'emulator\emulator.exe'

if (-not (Test-Path -LiteralPath $Adb)) {
    Fail "adb.exe not found at $Adb.`nInstall 'Android SDK Platform-Tools' via the SDK Manager."
}

# ── Locate a JDK 21 for the Gradle launcher ───────────────────────────────────
# gradle.properties pins the Gradle *daemon* to JDK 21 (org.gradle.java.home), but
# gradlew.bat itself boots on JAVA_HOME, and this machine's default is JDK 25.
function Resolve-Jdk21 {
    $pinned = $null
    $gradleProps = Join-Path $RepoRoot 'gradle.properties'
    if (Test-Path -LiteralPath $gradleProps) {
        foreach ($line in Get-Content -LiteralPath $gradleProps) {
            if ($line -match '^\s*org\.gradle\.java\.home\s*=\s*(.+?)\s*$') {
                $pinned = $Matches[1].Replace('\\', '\')
                break
            }
        }
    }
    if ($pinned -and (Test-Path -LiteralPath $pinned)) { return $pinned }

    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }

    $found = Get-ChildItem -Path 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($found) { return $found.FullName }

    return $null
}

$Jdk = Resolve-Jdk21
if ($Jdk) {
    $env:JAVA_HOME = $Jdk
    Write-Note "JAVA_HOME -> $Jdk"
}
else {
    Write-Warn 'No JDK 21 found; falling back to the ambient JAVA_HOME. AGP rejects JDK 25.'
}

# ── Warn if Android Studio is holding the build directory ─────────────────────
$studio = Get-Process -Name 'studio64', 'studio' -ErrorAction SilentlyContinue
if ($studio) {
    Write-Warn 'Android Studio is running. If it has this project open, its Gradle daemon'
    Write-Warn 'can lock app\build\ and this build may fail with "Could not delete/move ...".'
    Write-Warn 'Close the project in Android Studio (File > Close Project) if that happens.'
}

# ── adb helpers ───────────────────────────────────────────────────────────────
function Get-AdbDevices {
    $lines = & $Adb devices
    $result = @()
    foreach ($line in $lines) {
        $trimmed = "$line".Trim()
        if (-not $trimmed) { continue }
        if ($trimmed -like 'List of devices*') { continue }
        if ($trimmed -like '*daemon*') { continue }
        $parts = $trimmed -split '\s+'
        if ($parts.Count -lt 2) { continue }
        $result += [pscustomobject]@{
            Serial     = $parts[0]
            State      = $parts[1]
            IsEmulator = $parts[0].StartsWith('emulator-')
        }
    }
    return $result
}

# adb keeps listing a device as 'device' while it is shutting down or after the
# cable is yanked, so state alone is not proof it is usable. Probe it.
function Test-DeviceReady {
    param([string]$Serial)
    $prop = "$(& $Adb -s $Serial shell getprop sys.boot_completed 2>$null)".Trim()
    return ($prop -eq '1')
}

Write-Step 'Starting adb server'
& $Adb start-server | Out-Null
$devices = @(Get-AdbDevices)

$unauthorized = @($devices | Where-Object { $_.State -eq 'unauthorized' })
if ($unauthorized.Count -gt 0) {
    Write-Warn "Device $($unauthorized[0].Serial) is 'unauthorized'."
    Write-Warn 'Unlock the phone and tap "Allow" on the USB-debugging prompt, then re-run.'
}

$phones = @($devices | Where-Object { -not $_.IsEmulator -and $_.State -eq 'device' -and (Test-DeviceReady -Serial $_.Serial) })
$emulators = @($devices | Where-Object { $_.IsEmulator -and $_.State -eq 'device' -and (Test-DeviceReady -Serial $_.Serial) })

# ── Pick / bring up a target ──────────────────────────────────────────────────
$serial = $null

if ($Target -eq 'device') {
    if ($phones.Count -eq 0) {
        Fail @"
No authorized physical device found.
  1. Settings > About phone > tap 'Build number' 7 times
  2. Settings > System > Developer options > enable 'USB debugging'
  3. Plug the phone in, then tap 'Allow' on the USB-debugging prompt
Then re-run. (Use 'Run GoalPilot.cmd' instead to fall back to the emulator.)
"@

    }
    $serial = $phones[0].Serial
    Write-Ok "Using physical device $serial"
}
elseif ($Target -eq 'auto' -and $phones.Count -gt 0) {
    $serial = $phones[0].Serial
    Write-Ok "Using physical device $serial"
}
elseif ($emulators.Count -gt 0) {
    $serial = $emulators[0].Serial
    Write-Ok "Reusing running emulator $serial"
}
else {
    # Boot the AVD.
    if (-not (Test-Path -LiteralPath $EmulatorExe)) {
        Fail "emulator.exe not found at $EmulatorExe.`nInstall the 'Android Emulator' package via the SDK Manager."
    }

    $avds = @(& $EmulatorExe -list-avds | ForEach-Object { "$_".Trim() } | Where-Object { $_ })
    if ($avds -notcontains $Avd) {
        $available = if ($avds.Count -gt 0) { $avds -join ', ' } else { '(none)' }
        Fail "AVD '$Avd' not found.`nAvailable AVDs: $available`nPass a different one with -Avd <name>."
    }

    Write-Step "Booting emulator '$Avd'"
    $emuArgs = @('-avd', $Avd)
    if ($ColdBoot) { $emuArgs += '-no-snapshot-load' }
    Start-Process -FilePath $EmulatorExe -ArgumentList $emuArgs -WorkingDirectory (Split-Path -Parent $EmulatorExe) | Out-Null

    # Only *verified-ready* serials count as pre-existing. A serial that adb still
    # lists while its emulator shuts down must stay adoptable, because the AVD we
    # just launched can reclaim the very same port.
    $known = @($phones + $emulators | ForEach-Object { $_.Serial })
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $announced = $false

    # Wait for a new emulator to both attach AND finish booting. Attaching alone is
    # not enough: adb answers long before Android is usable.
    while ($watch.Elapsed.TotalSeconds -lt $BootTimeoutSec) {
        $candidates = @(Get-AdbDevices | Where-Object { $_.IsEmulator -and $_.State -eq 'device' -and $known -notcontains $_.Serial })
        if ($candidates.Count -gt 0 -and -not $announced) {
            Write-Note "Emulator attached as $($candidates[0].Serial), waiting for boot to complete..."
            $announced = $true
        }
        foreach ($candidate in $candidates) {
            if (Test-DeviceReady -Serial $candidate.Serial) { $serial = $candidate.Serial; break }
        }
        if ($serial) { break }
        Start-Sleep -Seconds 2
    }
    if (-not $serial) {
        Fail "Emulator '$Avd' did not finish booting within $BootTimeoutSec seconds.`nRetry with -ColdBoot (ignores a corrupt snapshot) or a longer -BootTimeoutSec."
    }

    Write-Ok "Emulator ready ($([int]$watch.Elapsed.TotalSeconds)s)"
}

# Pin every downstream adb/Gradle call to exactly this device.
$env:ANDROID_SERIAL = $serial

# Wake a sleeping emulator. Also needed when *reusing* one that has been idle,
# otherwise the app launches behind a black screen. Never touch a real phone's
# lock screen.
if ($serial.StartsWith('emulator-')) {
    & $Adb -s $serial shell input keyevent 224 | Out-Null   # KEYCODE_WAKEUP
    & $Adb -s $serial shell wm dismiss-keyguard | Out-Null
}

if ($SkipInstall) {
    Write-Host ''
    Write-Ok "Device $serial is up. Skipping build (-SkipInstall)."
    # Explicit: otherwise the script inherits $LASTEXITCODE from the last adb call.
    exit 0
}

# ── Build + install ───────────────────────────────────────────────────────────
Write-Step 'Building and installing the debug APK (gradlew :app:installDebug)'
Push-Location $RepoRoot
try {
    & $Gradlew ':app:installDebug'
    $gradleExit = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($gradleExit -ne 0) {
    Write-Host ''
    Write-Warn "Gradle failed with exit code $gradleExit."
    Write-Warn 'If the error mentions "Could not delete/move", it is a Windows file lock, not a code error:'
    Write-Warn '  - close the project in Android Studio, then re-run; or'
    Write-Warn '  - run: .\gradlew --stop ; Remove-Item -Recurse -Force app\build\generated\ksp'
    exit $gradleExit
}
Write-Ok 'Installed'

# ── Launch ────────────────────────────────────────────────────────────────────
Write-Step "Launching $Package"
# `am start` exits 0 even when it prints "Error: Activity class ... does not exist",
# so the output has to be inspected rather than trusting the exit code alone.
$amText = (& $Adb -s $serial shell am start -n "$Package/$Activity" | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $amText -match '(?m)^\s*Error') {
    if ($amText) { Write-Host $amText }
    Fail "Failed to launch $Package/$Activity on $serial."
}

Write-Host ''
Write-Ok "GoalPilot is running on $serial"
Write-Host ''

if ($Logcat) {
    Write-Step 'Tailing logcat (Ctrl+C to stop)'
    # The process may take a moment to appear; an empty pid would make --pid= invalid.
    $appPid = $null
    for ($i = 0; $i -lt 10 -and -not $appPid; $i++) {
        $candidate = "$(& $Adb -s $serial shell pidof -s $Package)".Trim()
        if ($candidate -match '^\d+$') { $appPid = $candidate } else { Start-Sleep -Seconds 1 }
    }
    if ($appPid) {
        & $Adb -s $serial logcat "--pid=$appPid"
    }
    else {
        Write-Warn 'Could not resolve the app process id; tailing unfiltered logcat instead.'
        & $Adb -s $serial logcat
    }
}

exit 0
