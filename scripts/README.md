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

## Troubleshooting

**The emulator starts but stays `offline` forever, or `adb devices` shows
`offline` and nothing ever boots.** The quickboot snapshot is corrupt — this
happens after the emulator is killed abruptly (Task Manager, a hard reboot, or a
terminal that took the process down with it). Stale `*.lock` entries are left
behind in the AVD folder. Recover with:

```powershell
.\scripts\run-goalpilot.ps1 -ColdBoot        # ignores the snapshot
```

If it still hangs, clear the locks by hand and cold-boot again:

```powershell
Get-Process qemu-system-x86_64* -ErrorAction SilentlyContinue | Stop-Process -Force
Get-ChildItem "$env:USERPROFILE\.android\avd\Pixel_10_Pro_XL.avd" -Filter *.lock -Recurse -Force |
    Remove-Item -Recurse -Force
```

A cold boot takes noticeably longer than a snapshot restore (~90 s vs ~15 s on
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
