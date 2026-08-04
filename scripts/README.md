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
| `Run GoalPilot on Second Device.cmd` | Boots `Pixel_10_Pro_XL_B` **alongside** the first emulator and installs there. The second-account half of the spec §7 demo. |

Each window stays open at the end (`pause`) so you can read the output.

## Desktop icons

```powershell
.\scripts\create-desktop-shortcuts.ps1
```

Creates four `.lnk` shortcuts on the Desktop pointing at the `.cmd` files above.
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
.\scripts\run-goalpilot.ps1 -WindowScale 0.3       # override the emulator window scale
.\scripts\run-goalpilot.ps1 -NoWindowFit           # leave window size/position to the emulator
.\scripts\run-goalpilot.ps1 -Logcat                # tail app logcat after launch
.\scripts\run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B   # the second device
```

What it handles for you:

- Reads `sdk.dir` from `local.properties` (falls back to `ANDROID_HOME` /
  `%LOCALAPPDATA%\Android\Sdk`), so no PATH setup is required.
- Sets `JAVA_HOME` from `org.gradle.java.home` in `gradle.properties` — the
  machine default is JDK 25, which AGP rejects (see [AGENTS.md](../AGENTS.md)).
- Reuses an already-running emulator instead of booting a second one — unless you
  named a different one with `-Avd`, which is a demand rather than a hint. See
  [Two emulators](#two-emulators-pixel_10_pro_xl-and-_b).
- Waits for `sys.boot_completed=1`, not just for adb to see the device.
- Pins `ANDROID_SERIAL` so Gradle installs to exactly one device even when both
  the emulator and a phone are connected.
- Warns if Android Studio is running (its Gradle daemon can lock `app\build\`).
- Explains the Windows "Could not delete/move" lock error if the build hits it.
- Refuses to drive an emulator another session is already driving — see
  [Running more than one session at a time](#running-more-than-one-session-at-a-time).

## Two emulators: `Pixel_10_Pro_XL` and `_B`

Added 2026-08-05, for one reason: the spec §7 sharing demo needs **two accounts
signed in at once**, and one AVD cannot do that. Both are the same API 37
Google-APIs-with-Play-Store image; `_B` is deliberately the leaner of the two.

**Two of them is what this host can just about carry, not comfortably.** With
both up beside VS Code and WSL, free RAM measured **0.5–1.6 GB of 32 GB**, and
`_B` throws an ANR — *"System UI isn't responding"*, or the launcher's — **once
while it settles**, after a cold boot *and* after a snapshot restore. Tap
**Wait**, give it a minute, and it lands on a clean launcher every time. Budget
for it before a live demo rather than being surprised by it. If you want it
smoother, shut WSL down (`wsl --shutdown` frees ~2.5 GB) rather than shrinking
either AVD.

Boot times measured on this host with the first emulator already up:
**125 s** cold, **33 s** from `_B`'s snapshot.

| | `Pixel_10_Pro_XL` | `Pixel_10_Pro_XL_B` |
|---|---|---|
| RAM / cores | 4096 MB / 6 | 3072 MB / 4 |
| `vm.heapSize` | 512 | 384 |
| GPU mode | `angle_indirect` | `angle_indirect` |
| Signed in as | `name.iddo@gmail.com` | *(fresh — sign in as `rachil751@gmail.com`)* |

**`_B` was tried at 2048 MB first and that does not work** — which is the tuning
table further down this file saying the same thing twice. At 2 GB the first cold
boot took 382 s and landed on *"System UI isn't responding"*, and the snapshot
restore afterwards never came online at all (still `offline` after 300 s). At 3 GB
the cold boot is **125 s** and the snapshot restore works; only the one settling
ANR above remains. Do not lower it back to save host RAM; lower `hw.cpu.ncore`
instead.

`_B` was created by hand rather than through `avdmanager create`: the bundled
cmdline-tools device catalogue on this machine has no `pixel_10_pro_xl` profile,
so `_B` is a copy of the first AVD's `config.ini` with the id, display name and
the three resource values above changed. It carries **no** copied user data — the
data partition is built fresh on first boot, which is what makes it a genuinely
separate account.

**`-Avd` is now a demand, not a hint.** Device selection used to adopt any
running emulator, which with two AVDs silently attached you to the *other*
session's screen; the device lock then reported it as a baffling "already being
driven" refusal. Now:

1. an emulator already serving `-Avd` → use it;
2. some other emulator running and **no** `-Avd` passed → adopt it, with a note;
3. otherwise → boot `-Avd` **alongside** whatever is already up.

**What a second emulator does not buy you.** It is a second *device*, not a
second *build*. Two sessions running `:app:connectedDebugAndroidTest` still queue
at the Gradle daemon, and each would build its APK out of the other's
uncommitted edits — one working tree, one `app\build\`. Both emulators also talk
to the same live Firebase project, so keep them on different accounts or the
writes are attributable to nobody.

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

**The emulator boots to a black screen, or System UI ANRs.** First check whether
the *guest* is rendering, not just the window — they look identical on screen:

```powershell
adb shell screencap -p /sdcard/g.png; adb pull /sdcard/g.png .
```

A fully black frame compresses to ~22 KB; a real frame is ~500–700 KB. If it is
black, GPU emulation is wedged — usually GL state restored from a snapshot. Run
`-Recover` (cold boot), which fixes it.

If it recurs, the AVD's hardware profile is the cause. **This machine's AVD has
been retuned** (`~/.android/avd/Pixel_10_Pro_XL.avd/config.ini`, original kept as
`config.ini.bak-before-tuning`):

| Setting | Default | Here | Why |
|---|---|---|---|
| `hw.ramSize` | 2048 | 4096 | 2 GB is too little for an API 37 image with Play Services |
| `vm.heapSize` | 256 | 512 | matches the larger RAM |
| `hw.cpu.ncore` | 4 | 6 | host is 12-core / 14-thread |
| `hw.gpu.mode` | `auto` | `angle_indirect` | host has **no discrete GPU** |

That last one matters most here. With integrated Intel graphics, `auto` resolves
to the host **OpenGL** path, and Intel's Windows OpenGL driver is where the GLES
translator dies with `Failed to find EmulatedEglImage` and
`glAttachShader ... error 0x502`. `angle_indirect` goes through **Direct3D 11**
instead. If that ever misbehaves, `swiftshader_indirect` is pure software —
slower, but it works anywhere.

These values are tuned to one machine and are deliberately **not** project
defaults. Changing `hw.ramSize` invalidates the snapshot, so the next boot is
automatically cold.

**The emulator window opens half off the top of the screen and can't be
dragged.** The `pixel_10_pro_xl` skin is **1466 × 3101**. Auto-scaled on a short
monitor the window comes out taller than the desktop, and the emulator then
centres it *vertically* — which puts the title bar above the top edge, leaving
nothing to grab. (On a 1440 × 900 display the working height is 852 px, so this
happens every time.)

The script prevents it: before each launch it computes a scale that fits the
primary monitor and writes `window.x` / `window.y` / `window.scale` into
`<avd>.avd\emulator-user.ini`. It has to be rewritten *every* launch, because the
emulator overwrites that file with the window's last position when it exits.

If a window is *already* stranded, just run the script again — it also checks the
live window after attaching and moves it back:

```
!!  Emulator window was off-screen (top=-420); moved it to (566,96).
```

That check is judged against the monitor the window is actually on, so an
emulator you deliberately parked on a second screen is left where you put it.

Override with `-WindowScale 0.3`, or opt out entirely with `-NoWindowFit`.
Note that `-scale` on the emulator command line does **not** work — it has been
obsolete and silently ignored since Emulator 2.0.

## Why avoid opening the project in Android Studio

Android Studio never overwrites your source files, and everything it generates
(`.idea/`, `.gradle/`, `build/`, `local.properties`) is git-ignored. The real
collision is the **Gradle daemon**: two daemons on the same `app\build\` directory
produce Windows file-lock failures. Using these scripts means only one build
system ever runs.

If you do want Android Studio's Device Manager GUI, open it **without a project**:
Welcome screen → *More Actions* → *Virtual Device Manager*. No project open means
no Gradle sync, and therefore no daemon.
