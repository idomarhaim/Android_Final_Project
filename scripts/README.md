# scripts/

One-click launchers so you can develop entirely in VS Code and never open the
project in Android Studio. Everything here uses only the standalone Android SDK
command-line tools (`emulator.exe`, `adb.exe`) plus the Gradle wrapper.

## Double-click launchers

| File | What it does |
|------|--------------|
| `Run GoalPilot.cmd` | Phone if one is plugged in, otherwise boots the emulator → `:app:installDebug` → launches the app. |
| `Start Emulator Only.cmd` | Boots `Pixel_10_Pro_XL` and stops. No Gradle, so `app\build\` is never touched. |
| `Run On Phone.cmd` | Same as *Run*, but fails fast if no authorized physical device is attached. |

Each window stays open at the end (`pause`) so you can read the output.

## Desktop icons

```powershell
.\scripts\create-desktop-shortcuts.ps1
```

Creates three `.lnk` shortcuts on the Desktop pointing at the `.cmd` files above.
Delete the `.lnk` files to undo — nothing else is modified.

## The underlying script

`run-goalpilot.ps1` does the real work. Useful flags:

```powershell
.\scripts\run-goalpilot.ps1                        # auto: phone, else emulator
.\scripts\run-goalpilot.ps1 -Target emulator       # force the emulator
.\scripts\run-goalpilot.ps1 -Target device         # force the physical phone
.\scripts\run-goalpilot.ps1 -SkipInstall           # bring a device up only
.\scripts\run-goalpilot.ps1 -ColdBoot              # ignore the emulator snapshot
.\scripts\run-goalpilot.ps1 -Recover               # repair a wedged AVD, then cold-boot
.\scripts\run-goalpilot.ps1 -Logcat                # tail app logcat after launch
.\scripts\run-goalpilot.ps1 -Avd Some_Other_AVD
```

What it handles for you:

- Reads `sdk.dir` from `local.properties` (falls back to `ANDROID_HOME` /
  `%LOCALAPPDATA%\Android\Sdk`), so no PATH setup is required.
- Sets `JAVA_HOME` from `org.gradle.java.home` in `gradle.properties` — the
  machine default is JDK 25, which AGP rejects (see [AGENTS.md](../AGENTS.md)).
- Reuses an already-running emulator instead of booting a second one.
- Waits for `sys.boot_completed=1`, not just for adb to see the device.
- Pins `ANDROID_SERIAL` so Gradle installs to exactly one device even when both
  the emulator and a phone are connected.
- Warns if Android Studio is running (its Gradle daemon can lock `app\build\`).
- Explains the Windows "Could not delete/move" lock error if the build hits it.
- Refuses to drive an emulator another session is already driving — see
  [Running more than one session at a time](#running-more-than-one-session-at-a-time).

## Running more than one session at a time

[SESSIONS.md](../SESSIONS.md) declares the emulator an exclusive singleton —
*"one screen, one driver"*. Two sessions driving one AVD is what corrupts its
quickboot snapshot and produces `InstallException: device offline`.

The script enforces that itself. It takes an **exclusive claim** on the target
device for the duration of a run:

```
    Claimed 'avd-Pixel_10_Pro_XL' for this run
```

A second session that tries the same AVD is refused, and told who holds it:

```
ERROR: 'avd-Pixel_10_Pro_XL' is already being driven by another session.
ERROR:   PID 22816 (namei) since 2026-08-01 20:32:29
ERROR:   repo: C:\Dev\Android_Final_Project
```

Details worth knowing:

- The claim keys on the **AVD name** (via `adb emu avd name`), not the serial —
  ports get recycled, AVD names don't. Physical phones key on their serial.
- It is an exclusive **OS file handle**, not a PID file, so Windows drops it the
  moment the process dies. A crashed or killed run **cannot** leave a stale claim
  blocking work nobody is doing. There is nothing to clean up by hand.
- It is released before the `-Logcat` tail, since tailing is read-only and can
  run for hours.
- `-SkipInstall` releases it on exit — the emulator stays up, unowned, and the
  next session is free to reuse it.

## Troubleshooting

**The emulator starts but stays `offline` forever, or `adb devices` shows
`offline` and nothing ever boots.** The quickboot snapshot is corrupt — this
happens after the emulator is killed abruptly (Task Manager, a hard reboot, or a
terminal that took the process down with it). Stale `*.lock` entries are left
behind in the AVD folder. Recover with:

```powershell
.\scripts\run-goalpilot.ps1 -Recover
```

`-Recover` claims the AVD, asks it to shut down, force-stops **only** the
`emulator.exe` / `qemu-system-x86_64.exe` processes whose command line names that
AVD, clears **only** that AVD's stale `*.lock` entries, then cold-boots.

Do **not** reach for a blanket `Get-Process qemu-system-x86_64* | Stop-Process`.
It kills every emulator on the machine, including another session's — which is
the exact failure this tooling exists to prevent.

If the snapshot is merely stale rather than the AVD being wedged, the lighter
`-ColdBoot` is enough:

```powershell
.\scripts\run-goalpilot.ps1 -ColdBoot        # ignores the snapshot, kills nothing
```

A cold boot takes noticeably longer than a snapshot restore (~65–90 s vs ~15 s on
this machine) — that is expected, not a hang.

**Gradle fails with `InstallException: device offline`.** The device dropped out
*during* the install. Confirm with `adb devices`; if the emulator is gone or
offline, apply the cold-boot recovery above and re-run.

## Why avoid opening the project in Android Studio

Android Studio never overwrites your source files, and everything it generates
(`.idea/`, `.gradle/`, `build/`, `local.properties`) is git-ignored. The real
collision is the **Gradle daemon**: two daemons on the same `app\build\` directory
produce Windows file-lock failures. Using these scripts means only one build
system ever runs.

If you do want Android Studio's Device Manager GUI, open it **without a project**:
Welcome screen → *More Actions* → *Virtual Device Manager*. No project open means
no Gradle sync, and therefore no daemon.
