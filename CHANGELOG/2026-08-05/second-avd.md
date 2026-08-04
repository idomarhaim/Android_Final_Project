# Changes — 05/08/2026 · session `second-avd`

> **Branch:** `feat/goalpilot-implementation`

A second emulator, `Pixel_10_Pro_XL_B`, so the spec §7 sharing demo can have both
accounts signed in at the same time. Started as a question — *can two sessions
each drive their own emulator?* — and the honest answer turned out to be "yes for
the device, no for the build", which is now written down where the next session
will trip over it.

## 📱 The AVD, and why it was made by hand

`avdmanager create avd` was not usable: the cmdline-tools device catalogue on this
machine has no `pixel_10_pro_xl` profile (the first AVD came from Android Studio's
newer catalogue). So `_B` is the first AVD's `config.ini` copied with four values
changed — `AvdId`, `avd.ini.displayname`, `hw.ramSize`, `hw.cpu.ncore` — plus its
own `Pixel_10_Pro_XL_B.ini` pointer.

Deliberately **no copied user data**. The data partition is built fresh on first
boot, which is exactly what makes it a separate account rather than a clone of the
one already signed in as `name.iddo@gmail.com`.

## 🐛 `-Avd` was a hint; it is now a demand

`run-goalpilot.ps1` adopted `$emulators[0]` whenever *any* emulator was ready,
ignoring `-Avd` entirely. Harmless with one AVD. With two it silently attaches you
to the other emulator — and because the device lock keys on AVD name, the symptom
is not "wrong screen" but a baffling *"'avd-Pixel_10_Pro_XL_B' is already being
driven by another session"* while you stare at an idle second AVD.

`Select-ReusableEmulator` now resolves each running emulator's AVD name first:

1. one already serving `-Avd` → reuse it;
2. some other emulator up and **no** `-Avd` passed → adopt it, with a note saying
   which (the old convenience, kept, but no longer silent);
3. otherwise → boot `-Avd` **alongside** what is already running.

## ⚠️ 2 GB is too little — twice over

`_B` was created lean at 2048 MB, and the repo's own tuning table (added
2026-08-02, `scripts/README.md`) already said that would not work: *"2 GB is too
little for an API 37 image with Play Services"*. It was right.

| | 2048 MB | 3072 MB |
|---|---|---|
| Cold boot | **382 s**, then *"System UI isn't responding"* | **125 s** |
| Snapshot restore | never came online — still `offline` past 300 s | **33 s** |

Raised to **3072 MB** (still leaner than the first AVD's 4096, still 4 cores).
Decision taken per the derivable-decision rule: committed repo knowledge already
answered it, so it was applied and logged rather than asked.

An ANR still appears **once** at 3072 MB while the guest settles — after a cold
boot *and* after a snapshot restore, sometimes SystemUI's and sometimes the
launcher's — and that one is the *host*, not the AVD: with both emulators up
beside VS Code and WSL, free RAM measured 0.5–1.6 GB of 32 GB. Tapping **Wait**
and giving it a minute lands on a clean launcher every time (`mCurrentFocus` =
`NexusLauncherActivity`, 1.87 MB frame). `wsl --shutdown` frees ~2.5 GB if a demo
needs to be smoother than that.

Two further numbers worth having: `-BootTimeoutSec` defaults to **300 s**, which
is not enough for `_B`'s first-ever boot; and changing `hw.ramSize` invalidates
the snapshot, so the boot after a resize is automatically cold.

## 🧭 What a second emulator does not buy

Recorded in `SESSIONS.md`, `AGENTS.md` and `scripts/README.md` because it is the
question that will be asked again: `_B` exists so **one** session can drive two
accounts, not so **two** sessions can each run `:app:connectedDebugAndroidTest`.
Those still queue at the single Gradle daemon, and each would build its APK from
the other session's uncommitted edits — one working tree, one `app\build\`. Real
parallel instrumented testing needs a second checkout, which this repo has not
adopted. Both emulators also share the live Firebase project, so they must stay on
different accounts.

## 🧪 Tests

This session changed PowerShell tooling, Markdown docs and a machine-local AVD.
**No code layer of the app was touched**, so the JVM unit, instrumented,
security-rules and Cloud Functions suites are all untouched and were not run —
there is no test layer in this repo that covers `scripts/`. Verification was
behavioural, against the two live emulators:

| Check | Result |
|---|---|
| Script parses (`Parser::ParseFile`) | ✅ clean — after one fix, below |
| `-Avd _B` while A is running | ✅ booted `_B` alongside; did **not** adopt A |
| `-Avd Pixel_10_Pro_XL` while `_B`'s claim was held | ✅ reused A, claimed it independently |
| `-Avd _B` while `_B`'s claim was held | ✅ refused, naming the holding PID |
| `-Recover` on `_B` | ✅ force-stopped only `_B`'s processes; A stayed up |
| `_B` cold boot at 3072 MB | ✅ 125 s (was 382 s at 2048 MB) |
| `_B` snapshot restore at 3072 MB | ✅ 33 s (at 2048 MB it never came online) |
| `_B` renders (screencap size test) | ✅ 691 KB / 1.87 MB frames — a black frame is ~22 KB |
| `_B` settles after the one ANR | ✅ launcher focused both times, after tapping *Wait* |
| A renders after `_B` booted | ✅ 1.87 MB — no GPU contention damage on the shared iGPU |
| `_B` carries Play Services | ✅ `com.google.android.gms` present, API 37 |
| App installed on `_B` | ⬜ not done — that is the demo itself, and it needs the second Google account's credentials |

**One error hit and fixed while working:** the new code failed to parse with
*"Missing closing '}'"* pointing at a function that was balanced. Cause: this
script has no BOM, so PowerShell 5.1 decodes it as CP1252, where the last byte of
a UTF-8 em dash (`0x94`) becomes `U+201D` — which the parser honours as a closing
double quote. Safe in a comment, fatal inside a string. Message strings in
`run-goalpilot.ps1` are ASCII now, with a comment saying why.
