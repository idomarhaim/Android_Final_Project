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
      2. an emulator already serving -Avd    -> use it
      3. some other emulator running, and no -Avd was passed
                                             -> adopt it, with a note
      4. otherwise                           -> boot the AVD named by -Avd,
                                                alongside anything already up

.PARAMETER Target
    auto     - phone if one is plugged in, else emulator (default)
    emulator - always use / boot the emulator
    device   - require a physical phone; fail if none is connected

.PARAMETER Avd
    Name of the AVD to use. Defaults to Pixel_10_Pro_XL. Passing it explicitly is
    a demand, not a hint: a running emulator serving a *different* AVD is left
    alone and this one is booted next to it. That is what lets two sessions hold
    one device each (Pixel_10_Pro_XL and Pixel_10_Pro_XL_B) without colliding.

.PARAMETER SkipInstall
    Only bring the device up. Skips the Gradle build, install and app launch.

.PARAMETER ColdBoot
    Boot the emulator from scratch instead of restoring its snapshot.

.PARAMETER Logcat
    After launching, tail logcat filtered to the app's process. Ctrl+C to stop.

.PARAMETER Recover
    Repair an emulator stuck 'offline' after a corrupt quickboot snapshot. Shuts
    down only the processes serving -Avd, clears only that AVD's stale *.lock
    entries, then cold-boots. Implies -Target emulator and -ColdBoot.

.PARAMETER WindowScale
    Emulator window scale (e.g. 0.25). Default 0 = compute a scale that fits the
    primary monitor's working area.

.PARAMETER NoWindowFit
    Leave window size and position entirely to the emulator.

.PARAMETER BootTimeoutSec
    How long to wait for the emulator to finish booting. Default 300s.

.NOTES
    Concurrency: SESSIONS.md declares the emulator an exclusive singleton ("one
    screen, one driver"). This script takes an exclusive OS file-handle lock on
    the target device for the duration of a run, so a second session refuses to
    drive the same AVD instead of silently fighting for it. The lock is released
    by the OS if the process dies, so there are no stale claims to clear.

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
    [switch]$Recover,

    [double]$WindowScale = 0,
    [switch]$NoWindowFit,

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
# gradlew.bat itself boots on JAVA_HOME, which need not be the pinned JDK 21.
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
    Write-Warn 'No JDK 21 found; falling back to the ambient JAVA_HOME. This toolchain needs JDK 21.'
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

# An emulator's stable identity is its AVD name, not its serial — the port is
# recycled. Everything that locks or targets a device keys off this.
function Get-EmulatorAvdName {
    param([string]$Serial)
    $lines = & $Adb -s $Serial emu avd name 2>$null
    foreach ($line in $lines) {
        $trimmed = "$line".Trim()
        if ($trimmed -and $trimmed -ne 'OK' -and $trimmed -notlike 'KO*') { return $trimmed }
    }
    return $Serial
}

# Which running emulator, if any, may stand in for -Avd. Adopting "whatever is
# running" was harmless while the machine had one AVD. With two it attaches a
# session to the *other* session's screen, and the device lock then reports the
# collision as a baffling "already being driven" refusal instead of the second
# emulator the caller actually asked for.
function Select-ReusableEmulator {
    param(
        [object[]]$Emulators,
        [string]$Wanted,
        [bool]$AvdWasExplicit
    )

    $named = @(foreach ($e in $Emulators) {
        [pscustomobject]@{ Serial = $e.Serial; AvdName = (Get-EmulatorAvdName -Serial $e.Serial) }
    })

    $match = @($named | Where-Object { $_.AvdName -eq $Wanted })
    if ($match.Count -gt 0) { return $match[0] }

    # A name was asked for and nothing is serving it: boot it alongside the rest.
    # (Keep message strings ASCII. This file has no BOM, so PowerShell 5.1 decodes
    # it as CP1252, where the last byte of a UTF-8 em dash becomes U+201D — which
    # the parser honours as a closing quote. Safe in a comment, fatal in a string.)
    if ($AvdWasExplicit) {
        Write-Note "Running: $(($named.AvdName) -join ', '). -Avd asked for '$Wanted' - booting it alongside."
        return $null
    }

    # No -Avd given. Keep the old convenience of adopting a hand-booted emulator
    # instead of starting a second one — but never silently, now that which
    # emulator you got is a real question.
    if ($named.Count -gt 1) {
        Write-Warn "$($named.Count) emulators running and no -Avd given; taking '$($named[0].AvdName)'. Pass -Avd to choose."
    }
    else {
        Write-Note "Adopting running emulator '$($named[0].AvdName)' (no -Avd given; default is '$Wanted')."
    }
    return $named[0]
}

# ── Cross-session lock on the device singleton ────────────────────────────────
# SESSIONS.md: the emulator is "one screen, one driver". Two sessions driving one
# AVD is what corrupts its quickboot snapshot and yields 'InstallException:
# device offline'. This is an exclusive OS file handle rather than a PID file:
# Windows drops it when the process dies, so a crashed run cannot leave a stale
# claim that blocks work nobody is doing.
$script:LockHandle = $null
$script:LockPath = $null

function Lock-Device {
    param([string]$Key)

    $dir = Join-Path $env:LOCALAPPDATA 'GoalPilot\locks'
    if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $script:LockPath = Join-Path $dir (($Key -replace '[^A-Za-z0-9._-]', '_') + '.lock')

    try {
        $script:LockHandle = [System.IO.File]::Open(
            $script:LockPath,
            [System.IO.FileMode]::OpenOrCreate,
            [System.IO.FileAccess]::Write,
            [System.IO.FileShare]::Read)
    }
    catch [System.IO.IOException] {
        # Share mode Read lets a blocked session read who holds it.
        $holder = ''
        try {
            $peek = [System.IO.File]::Open($script:LockPath, [System.IO.FileMode]::Open,
                [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            try {
                $reader = New-Object System.IO.StreamReader($peek)
                $holder = $reader.ReadToEnd().Trim()
            }
            finally { $peek.Dispose() }
        }
        catch { }
        if (-not $holder) { $holder = '(the holder did not record details)' }

        Fail @"
'$Key' is already being driven by another session.
  $($holder -replace "`n", "`n  ")
SESSIONS.md declares the emulator an exclusive singleton. Two sessions driving
one AVD is what corrupts its quickboot snapshot. Wait for that run to finish, or
target something else with -Avd / -Target.
"@
    }

    $stamp = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
    $bytes = [System.Text.Encoding]::UTF8.GetBytes("PID $PID ($env:USERNAME) since $stamp`nrepo: $RepoRoot")
    $script:LockHandle.SetLength(0)
    $script:LockHandle.Write($bytes, 0, $bytes.Length)
    $script:LockHandle.Flush()
    Write-Note "Claimed '$Key' for this run"
}

function Unlock-Device {
    if ($script:LockHandle) {
        $script:LockHandle.Dispose()
        $script:LockHandle = $null
        if ($script:LockPath) {
            Remove-Item -LiteralPath $script:LockPath -Force -ErrorAction SilentlyContinue
        }
    }
}

# Repair an emulator wedged 'offline' by a corrupt quickboot snapshot. Everything
# here is scoped to ONE AVD on purpose: a blanket `Stop-Process qemu*` would take
# down another session's emulator, which is the exact failure this script exists
# to prevent.
function Invoke-EmulatorRecovery {
    param([string]$AvdName)

    Write-Step "Recovering AVD '$AvdName'"

    foreach ($d in @(Get-AdbDevices | Where-Object { $_.IsEmulator })) {
        if ((Get-EmulatorAvdName -Serial $d.Serial) -eq $AvdName) {
            Write-Note "Asking $($d.Serial) to shut down"
            & $Adb -s $d.Serial emu kill 2>$null | Out-Null
        }
    }
    Start-Sleep -Seconds 3

    $pattern = '-avd\s+' + [regex]::Escape($AvdName) + '(\s|$)'
    $procs = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -in @('qemu-system-x86_64.exe', 'emulator.exe') -and $_.CommandLine -match $pattern })
    foreach ($p in $procs) {
        Write-Warn "Force-stopping $($p.Name) pid $($p.ProcessId) (serves '$AvdName')"
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
    if ($procs.Count -gt 0) { Start-Sleep -Seconds 2 }

    $avdDir = Join-Path $env:USERPROFILE ".android\avd\$AvdName.avd"
    if (Test-Path -LiteralPath $avdDir) {
        $locks = @(Get-ChildItem -LiteralPath $avdDir -Filter '*.lock' -Recurse -Force -ErrorAction SilentlyContinue)
        foreach ($l in $locks) { Remove-Item -LiteralPath $l.FullName -Recurse -Force -ErrorAction SilentlyContinue }
        Write-Note "Cleared $($locks.Count) stale lock entries in $AvdName.avd"
    }
    else {
        Write-Warn "AVD folder not found at $avdDir - skipped lock cleanup."
    }
}

# ── Emulator window placement ─────────────────────────────────────────────────
# The Pixel 10 Pro XL skin is 1466x3101. Auto-scaled on a short monitor the
# window comes out taller than the desktop, and the emulator then centres it
# vertically — which puts the title bar ABOVE the top of the screen, so the
# window cannot be dragged back into view. Pick a scale that fits and a position
# that centres, before launching.
function Get-SkinSize {
    param([string]$AvdName)

    $cfg = Join-Path $env:USERPROFILE ".android\avd\$AvdName.avd\config.ini"
    $w = 0; $h = 0; $skinPath = $null
    if (Test-Path -LiteralPath $cfg) {
        foreach ($line in Get-Content -LiteralPath $cfg) {
            if ($line -match '^\s*skin\.path\s*=\s*(.+?)\s*$') { $skinPath = $Matches[1] }
            elseif ($line -match '^\s*hw\.lcd\.width\s*=\s*(\d+)') { $w = [int]$Matches[1] }
            elseif ($line -match '^\s*hw\.lcd\.height\s*=\s*(\d+)') { $h = [int]$Matches[1] }
        }
    }
    # Prefer the skin's own layout — it includes the bezel, and the bezel is part
    # of what gets scaled. hw.lcd.* alone under-reports the window by ~4%.
    if ($skinPath) {
        $layout = Join-Path $skinPath 'layout'
        if (Test-Path -LiteralPath $layout) {
            $widths = @(); $heights = @()
            foreach ($line in Get-Content -LiteralPath $layout) {
                if ($line -match '^\s*width\s+(\d+)') { $widths += [int]$Matches[1] }
                elseif ($line -match '^\s*height\s+(\d+)') { $heights += [int]$Matches[1] }
            }
            if ($widths.Count -gt 0 -and $heights.Count -gt 0) {
                $w = ($widths | Measure-Object -Maximum).Maximum
                $h = ($heights | Measure-Object -Maximum).Maximum
            }
        }
    }
    if ($w -le 0 -or $h -le 0) { return $null }
    return [pscustomobject]@{ Width = $w; Height = $h }
}

function Set-EmulatorWindowLayout {
    param([string]$AvdName, [double]$Scale = 0)

    $skin = Get-SkinSize -AvdName $AvdName
    if (-not $skin) {
        Write-Note 'Could not read the skin size; leaving window placement to the emulator.'
        return
    }

    Add-Type -AssemblyName System.Windows.Forms -ErrorAction SilentlyContinue
    $wa = [System.Windows.Forms.Screen]::PrimaryScreen.WorkingArea

    # 90px covers the title bar plus a margin above and below.
    if ($Scale -le 0) { $Scale = [math]::Round(($wa.Height - 90) / $skin.Height, 3) }
    if ($Scale -gt 1) { $Scale = 1 }
    if ($Scale -lt 0.1) { $Scale = 0.1 }

    $winW = [int]($skin.Width * $Scale)
    $winH = [int]($skin.Height * $Scale)
    $x = [int]($wa.X + [math]::Max(0, ($wa.Width - $winW) / 2))
    $y = [int]($wa.Y + [math]::Max(0, ($wa.Height - $winH - 31) / 2))

    # The emulator rewrites this file on exit, so it is set fresh every launch
    # rather than once. Other keys (uuid, posture, ...) are preserved.
    $ini = Join-Path $env:USERPROFILE ".android\avd\$AvdName.avd\emulator-user.ini"
    $keep = @()
    if (Test-Path -LiteralPath $ini) {
        $keep = @(Get-Content -LiteralPath $ini | Where-Object { $_ -notmatch '^\s*window\.(x|y|scale)\s*=' })
    }
    $lines = @("window.x = $x", "window.y = $y", ('window.scale = {0:F6}' -f $Scale)) + $keep
    Set-Content -LiteralPath $ini -Value $lines -Encoding ascii
    Write-Note ("Window: scale {0} -> {1}x{2} at ({3},{4})" -f $Scale, $winW, $winH, $x, $y)
}

# Safety net for a window that is already off-screen — including one stranded by
# an earlier run, which the ini alone cannot rescue because the emulator is
# already up.
function Repair-EmulatorWindowPosition {
    param([string]$Serial)

    if (-not ([System.Management.Automation.PSTypeName]'GpWin').Type) {
        Add-Type @'
using System;
using System.Runtime.InteropServices;
public class GpWin {
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
'@
    }
    Add-Type -AssemblyName System.Windows.Forms -ErrorAction SilentlyContinue

    $port = ($Serial -split '-')[-1]
    $proc = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -like "*:$port*" } |
        Select-Object -First 1
    if (-not $proc) { return }

    $r = New-Object GpWin+RECT
    if (-not [GpWin]::GetWindowRect($proc.MainWindowHandle, [ref]$r)) { return }
    $w = $r.Right - $r.Left
    $h = $r.Bottom - $r.Top

    # Judge against the monitor the window actually sits on, not the primary one.
    # A window deliberately parked on a second screen is not a problem to fix,
    # and dragging it back to the primary would just fight the user.
    $wa = ([System.Windows.Forms.Screen]::FromHandle($proc.MainWindowHandle)).WorkingArea

    # The only true failure is an unreachable title bar, or so little of the
    # window on-screen that it cannot be grabbed.
    $titleBarHidden = $r.Top -lt $wa.Y
    $barelyVisible = ($r.Right -le ($wa.X + 40)) -or ($r.Left -ge ($wa.X + $wa.Width - 40)) -or
                     ($r.Top -ge ($wa.Y + $wa.Height - 40))
    if (-not ($titleBarHidden -or $barelyVisible)) { return }

    $x = [int]($wa.X + [math]::Max(0, ($wa.Width - $w) / 2))
    $y = [int]($wa.Y + [math]::Max(0, ($wa.Height - $h) / 2))
    # SWP_NOSIZE (0x0001) | SWP_NOZORDER (0x0004) — move only.
    [void][GpWin]::SetWindowPos($proc.MainWindowHandle, [IntPtr]::Zero, $x, $y, 0, 0, 0x0005)
    Write-Warn "Emulator window was off-screen (top=$($r.Top)); moved it to ($x,$y)."
}

if ($Recover) { $Target = 'emulator'; $ColdBoot = $true }

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
$reusable = if ($emulators.Count -gt 0 -and -not $Recover) {
    Select-ReusableEmulator -Emulators $emulators -Wanted $Avd -AvdWasExplicit $PSBoundParameters.ContainsKey('Avd')
} else { $null }

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
    Lock-Device -Key "device-$serial"
    Write-Ok "Using physical device $serial"
}
elseif ($Target -eq 'auto' -and $phones.Count -gt 0) {
    $serial = $phones[0].Serial
    Lock-Device -Key "device-$serial"
    Write-Ok "Using physical device $serial"
}
elseif ($null -ne $reusable) {
    $serial = $reusable.Serial
    Lock-Device -Key "avd-$($reusable.AvdName)"
    Write-Ok "Reusing running emulator $serial ($($reusable.AvdName))"
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

    # Claim before booting, and before any recovery touches processes or files —
    # nothing below this line may run in two sessions at once.
    Lock-Device -Key "avd-$Avd"
    if ($Recover) {
        Invoke-EmulatorRecovery -AvdName $Avd
        $devices = @(Get-AdbDevices)
    }

    if (-not $NoWindowFit) { Set-EmulatorWindowLayout -AvdName $Avd -Scale $WindowScale }

    Write-Step "Booting emulator '$Avd'"
    $emuArgs = @('-avd', $Avd)
    if ($ColdBoot) { $emuArgs += '-no-snapshot-load' }
    Start-Process -FilePath $EmulatorExe -ArgumentList $emuArgs -WorkingDirectory (Split-Path -Parent $EmulatorExe) | Out-Null

    # Only *verified-ready* serials count as pre-existing. A serial that adb still
    # lists while its emulator shuts down must stay adoptable, because the AVD we
    # just launched can reclaim the very same port. Recomputed here rather than
    # reused from above: -Recover has just killed devices that were live then.
    $known = @(Get-AdbDevices |
        Where-Object { $_.State -eq 'device' -and (Test-DeviceReady -Serial $_.Serial) } |
        ForEach-Object { $_.Serial })
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
    # Runs on the reuse path too: rescues a window an earlier run left stranded.
    if (-not $NoWindowFit) { Repair-EmulatorWindowPosition -Serial $serial }
}

if ($SkipInstall) {
    Write-Host ''
    Write-Ok "Device $serial is up. Skipping build (-SkipInstall)."
    Write-Note 'Released the device claim; the emulator keeps running.'
    Unlock-Device
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
    Unlock-Device
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

# Released before the logcat tail: tailing is read-only and can run for hours,
# and holding the singleton that long would block every other session.
Unlock-Device

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
