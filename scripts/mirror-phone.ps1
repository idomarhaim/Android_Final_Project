#Requires -Version 5.1
<#
.SYNOPSIS
    Mirror a physical Android phone onto this monitor, with mouse and keyboard
    control. What Android Studio's "Running Devices" pane does, without Android
    Studio.

.DESCRIPTION
    Wraps scrcpy (https://github.com/Genymobile/scrcpy). Builds nothing,
    installs nothing, and touches no Gradle daemon -- it only reads the screen
    over the cable and sends input back.

    Two things it does that a bare "scrcpy.exe" does not, and both matter here:

    1. It PINS adb. scrcpy ships its own adb.exe, and its version differs from
       this SDK's -- Observed: 37.0.0-14910828 bundled vs 37.0.1-15733141 in
       platform-tools. An adb client whose version disagrees with the running
       server KILLS that server and restarts it, which would drop a sibling
       session's emulator transport mid-run, looking to them like the emulator
       died. Setting $env:ADB to the SDK binary makes scrcpy reuse the server
       that is already up.

       Inferred: the kill-and-restart half is adb's documented mismatch
       behaviour and was NOT reproduced here -- reproducing it means letting the
       wrong client win while somebody holds a device, which is what this pin
       exists to prevent.

    2. It refuses emulators by default. SESSIONS.md declares each emulator an
       exclusive singleton; a physical phone is not one, so mirroring the phone
       collides with nobody. Pass -AllowEmulator only after claiming it.

.PARAMETER Serial
    adb serial to mirror. Default: the single physical device attached. With
    more than one connected the script lists them and stops rather than guessing.

.PARAMETER MaxSize
    Longest edge of the mirrored window, in pixels. Default 1200. Lower is
    smoother over a slow link; 0 means the phone's native resolution.

.PARAMETER Record
    Also record the session to this .mp4 path while mirroring. A relative path
    is taken as relative to the repo root.

.PARAMETER StayAwake
    Keep the phone's screen from sleeping while it is plugged in and mirrored.

.PARAMETER NoAudio
    Do not forward the phone's audio to the PC. Audio forwarding needs
    Android 11+ and can be noisy; this switches it off.

.PARAMETER NoControl
    View only. Mouse and keyboard do not reach the phone -- useful when you are
    demoing and do not want a stray click to open something.

.PARAMETER AllowEmulator
    Permit mirroring an emulator. Off by default; see the DESCRIPTION.

.NOTES
    scrcpy install: %LOCALAPPDATA%\Programs\scrcpy (portable zip, no admin).
    If it is missing this script says so and prints the download URL rather than
    installing anything behind your back.

    Handy keys while mirroring (Left-Alt is the modifier):
      Alt+F   fullscreen          Alt+G   resize window to 1:1
      Alt+H   HOME                Alt+B   BACK           Alt+S   app switcher
      Alt+P   power               Alt+O   turn phone screen off (keeps mirroring)
      Alt+C   copy phone clipboard to PC
      Drag-and-drop a file onto the window to push it to the phone.

.EXAMPLE
    .\scripts\mirror-phone.ps1
.EXAMPLE
    .\scripts\mirror-phone.ps1 -MaxSize 900 -StayAwake
.EXAMPLE
    .\scripts\mirror-phone.ps1 -Record docs\demo.mp4 -NoControl
#>
[CmdletBinding()]
param(
    [string]$Serial = '',
    [int]$MaxSize = 1200,
    [string]$Record = '',
    [switch]$StayAwake,
    [switch]$NoAudio,
    [switch]$NoControl,
    [switch]$AllowEmulator
)

$ErrorActionPreference = 'Stop'

function Write-Step { param([string]$Message) Write-Host "==> $Message" -ForegroundColor Cyan }
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

# -- Locate the repo ----------------------------------------------------------
$RepoRoot = Split-Path -Parent $PSScriptRoot

# -- Locate the Android SDK (same resolution order as run-goalpilot.ps1) ------
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
if (-not (Test-Path -LiteralPath $Adb)) {
    Fail "adb.exe not found at $Adb.`nInstall the SDK platform-tools."
}

# -- Locate scrcpy ------------------------------------------------------------
$ScrcpyHome = Join-Path $env:LOCALAPPDATA 'Programs\scrcpy'
$Scrcpy = Join-Path $ScrcpyHome 'scrcpy.exe'
if (-not (Test-Path -LiteralPath $Scrcpy)) {
    $onPath = Get-Command scrcpy -ErrorAction SilentlyContinue
    if ($onPath) {
        $Scrcpy = $onPath.Source
        $ScrcpyHome = Split-Path -Parent $Scrcpy
    }
    else {
        Fail @"
scrcpy is not installed.
Expected it at: $Scrcpy

Install the portable build (no admin rights needed):
  1. Download  https://github.com/Genymobile/scrcpy/releases/latest
     -> scrcpy-win64-<version>.zip
  2. Unzip it so that scrcpy.exe sits directly in
     $ScrcpyHome

Do NOT use 'winget install' on this machine: it blocks on an elevation prompt
it cannot display and produces no output at all.
"@
    }
}

# -- Pin adb, so scrcpy does not restart the server ---------------------------
# See point 1 in the DESCRIPTION. This is the single most important line here.
$env:ADB = $Adb
Write-Note "adb pinned to $Adb"

# -- Pick the device ----------------------------------------------------------
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

$all = @(Get-AdbDevices)

if ($Serial) {
    $chosen = @($all | Where-Object { $_.Serial -eq $Serial })
    if ($chosen.Count -eq 0) {
        $attached = ($all | ForEach-Object { $_.Serial }) -join ', '
        if (-not $attached) { $attached = '(nothing)' }
        Fail "No device with serial '$Serial' is attached.`nAttached: $attached"
    }
}
else {
    $candidates = @($all | Where-Object { $AllowEmulator -or -not $_.IsEmulator })

    if ($candidates.Count -eq 0) {
        $emulatorsOnly = @($all | Where-Object { $_.IsEmulator })
        if ($emulatorsOnly.Count -gt 0) {
            $names = ($emulatorsOnly | ForEach-Object { $_.Serial }) -join ', '
            Fail @"
No physical phone is attached -- only emulator(s): $names

SESSIONS.md declares each emulator an exclusive singleton, so this script will
not adopt one silently. If the emulator is yours, re-run with -AllowEmulator.

To mirror a phone instead:
  1. Plug it in over USB.
  2. Settings > Developer options > USB debugging = ON.
  3. Accept the 'Allow USB debugging?' prompt on the phone.
"@
        }
        Fail @"
No device is attached at all.

  1. Plug the phone in over USB.
  2. Settings > Developer options > USB debugging = ON.
  3. Accept the 'Allow USB debugging?' prompt on the phone.
  4. Re-run this script.
"@
    }

    if ($candidates.Count -gt 1) {
        Write-Host ''
        Write-Warn 'More than one device is attached; pick one explicitly.'
        foreach ($d in $candidates) { Write-Host "      $($d.Serial)  [$($d.State)]" }
        Write-Host ''
        Fail "Re-run with -Serial <serial>, e.g.`n  .\scripts\mirror-phone.ps1 -Serial $($candidates[0].Serial)"
    }

    $chosen = $candidates
}

$device = $chosen[0]

# adb reports several non-'device' states, and each has a different fix. Saying
# which one it is turns a dead end into a next step.
switch ($device.State) {
    'device' { }
    'unauthorized' {
        Fail @"
Device $($device.Serial) is attached but UNAUTHORIZED.

The phone never accepted this PC's debugging key. On the phone:
  Settings > Developer options > Revoke USB debugging authorisations
then unplug, plug back in, and tick 'Always allow from this computer'.
"@
    }
    'offline' {
        Fail @"
Device $($device.Serial) is OFFLINE.

Usually a wedged adb server. Try:
  & '$Adb' kill-server
  & '$Adb' devices
"@
    }
    default {
        Fail "Device $($device.Serial) is in state '$($device.State)', which is not usable for mirroring."
    }
}

# -- Build the scrcpy command line -------------------------------------------
$model = "$(& $Adb -s $device.Serial shell getprop ro.product.model 2>$null)".Trim()
if (-not $model) { $model = $device.Serial }

$scrcpyArgs = @(
    '--serial', $device.Serial,
    '--window-title', "$model  ($($device.Serial))"
)

if ($MaxSize -gt 0) { $scrcpyArgs += @('--max-size', "$MaxSize") }
if ($StayAwake) { $scrcpyArgs += '--stay-awake' }
if ($NoAudio) { $scrcpyArgs += '--no-audio' }
if ($NoControl) { $scrcpyArgs += '--no-control' }

if ($Record) {
    $recordPath = $Record
    if (-not [System.IO.Path]::IsPathRooted($recordPath)) {
        $recordPath = Join-Path $RepoRoot $recordPath
    }
    $recordDir = Split-Path -Parent $recordPath
    if ($recordDir -and -not (Test-Path -LiteralPath $recordDir)) {
        New-Item -ItemType Directory -Path $recordDir -Force | Out-Null
    }
    $scrcpyArgs += @('--record', $recordPath)
    Write-Note "recording to $recordPath"
}

Write-Step "Mirroring $model  [$($device.Serial)]"
Write-Note 'close the mirror window, or press Ctrl+C here, to stop'
Write-Note 'Alt+F fullscreen . Alt+H home . Alt+B back . Alt+O phone screen off'
Write-Host ''

& $Scrcpy @scrcpyArgs
$code = $LASTEXITCODE

Write-Host ''
if ($code -ne 0) {
    Write-Warn "scrcpy exited with code $code."
}
else {
    Write-Step 'Mirror closed.'
}
exit $code
