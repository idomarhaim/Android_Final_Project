# phone-mirror — 2026-08-24

**`scripts/` gains the one thing it never had: a way to *look* at the phone.**
Every launcher in that folder builds and installs; none of them puts the device on the monitor.
That was the last standing reason to open Android Studio in a repo whose `scripts/README.md`
opens by saying you never have to.

Ido's ask, verbatim: *"האם יש לי סקריפט שמאפשר לי לפתוח את תצוגת הסמארטפון שלי על המסך (כמו שרואים
בAndroid studio)?"* — the answer was **no**, and this is the repair.

## 🛠️ What shipped

| File | What it is |
|---|---|
| `scripts/Mirror Phone.cmd` *(new)* | Double-click launcher, in the house shape of `Run On Phone.cmd` — `.cmd` → `powershell -File …` → `pause` so the window survives a failure |
| `scripts/mirror-phone.ps1` *(new)* | The real work. Resolves the SDK exactly as `run-goalpilot.ps1` does, picks the device, launches `scrcpy` |
| `scripts/README.md` | New **Mirroring the phone** section; launcher table row; Desktop-icon count four → five |
| `scripts/create-desktop-shortcuts.ps1` | Fifth shortcut, *GoalPilot - Mirror phone* |

**`scrcpy` v4.1** installed to `%LOCALAPPDATA%\Programs\scrcpy` — portable zip, no admin, **SHA256
verified against the release's own `SHA256SUMS.txt`** (`5b12172b…65db`, match). Not in git; the
script fails with the download URL rather than installing anything on your behalf.

## ⚠️ The finding this session exists to record — scrcpy's bundled adb can kill your adb server

`scrcpy` ships **its own `adb.exe`**, and on this machine it is a **different version** from the
SDK's:

| Binary | Version |
|---|---|
| `%LOCALAPPDATA%\Programs\scrcpy\adb.exe` | `37.0.0-14910828` |
| `…\Android\Sdk\platform-tools\adb.exe` | `37.0.1-15733141` |

An adb client whose version disagrees with the running server **kills that server and restarts
it**. `challenge-scoring` holds `emulator-5554`; a mirror launched from the raw `scrcpy.exe` would
have dropped their transport mid-run, and the symptom they'd see is *the emulator died* — pointing
at everything except the real cause.

⚠️ **Provenance, because these two halves are not equally certain.** The **version difference** is
`Observed:` — both binaries were run and printed the strings above. The **consequence** is
`Inferred:` from adb's documented version-mismatch behaviour; it was **not** reproduced here,
because reproducing it means deliberately letting the wrong client win while a sibling holds a
device, which is the exact outcome the pin exists to prevent. `Untested:` the check that would
settle it is launching `scrcpy.exe` from its own folder with `ADB` unset and re-reading
`adb version` — worth running some day on an idle machine, and never while somebody is mid-run.

**Neutralised, not merely noted:** `mirror-phone.ps1` sets `$env:ADB` to the SDK binary before
launching. `Observed:` after a real mirror session the server still reports `37.0.1-15733141` and
**both** transports survive — `R5CY21NM30D` *and* `emulator-5554` still `device`. Written onto
`SESSIONS.md` as a note addressed to `challenge-scoring`, and into `scripts/README.md` for whoever
runs `scrcpy.exe` by hand later.

## 🧭 Two decisions taken by deriving, both mine to overturn

1. **Emulators are refused unless `-AllowEmulator` is passed.** `SESSIONS.md` declares each
   emulator an exclusive singleton; a mirror that adopts one silently is exactly the collision the
   board exists to prevent. A physical phone is *not* a declared singleton, so the default target
   collides with nobody. Per the derivable-decision rule — decided from a committed principle, not
   asked.
2. **`scrcpy` over Android Studio's *Running Devices*.** Studio is installed and its pane does the
   same job, but using it contradicts `scripts/README.md`'s own premise. `scrcpy` is ~30 MB,
   needs no admin, and opens in about a second.

## 🧪 Tests

**There is no test layer in this project for `scripts/*.ps1`** — no PowerShell test framework is
configured, and these files are outside every Gradle source set. Stated explicitly rather than
skipped silently. What was run instead, all against the real machine:

| Check | Result |
|---|---|
| `[Parser]::ParseFile` on `mirror-phone.ps1` | **0 errors** |
| Negative path — `-Serial NOPE123` | Fails cleanly: names the serial, lists what *is* attached, exit **1**. No stack trace |
| Real launch, phone attached | `scrcpy` running, window title **`SM-S938B  (R5CY21NM30D)`** |
| adb server after that launch | still `37.0.1-15733141` — **the pin held** |
| Both transports after that launch | `R5CY21NM30D` and `emulator-5554` both `device` — sibling unharmed |
| `create-desktop-shortcuts.ps1 -Destination <scratch>` | **5** `.lnk` files, the new one among them. Run into the scratchpad, **not** onto Ido's Desktop |
| Icon index `imageres.dll,175` | Valid — `ExtractIconEx` reports **369** icons in that DLL |
| `README.md` internal anchors | Recomputed from the headings: **5/5 resolve**, including the two new links |
| Control characters in every new/edited file | **0** |

⚠️ **One defect found in my own work and fixed before commit.** The board note was first written
through a Python heredoc where `\a` was interpreted as **BELL (0x07)**, leaving a literal control
character in `SESSIONS.md` — in a repo that ships `scripts/Assert-NoControlChars.ps1` and a
pre-commit hook. Caught by grepping for control characters rather than by reading the line, which
rendered as plausible text. Same family as `CLAUDE.md`'s `local.properties` and XML-comment traps:
**the file format read my prose as syntax.**

## 📱 Device

The phone was **not** written to. No install, no uninstall, no `pm` command, no `connectedDebug…`
task — mirroring reads the framebuffer and sends input. **Ido's Firebase sign-in is untouched**,
and the release app `com.idomarhaim.goalpilot` was opened (not installed) at his request earlier
in the session.

## 🧾 Singletons

`R5CY21NM30D` claimed and released with this commit. **`adb` was never taken** — every command was
`-s R5CY21NM30D`, and `emulator-5554` was never addressed. No Gradle daemon, no Firebase, no
emulator.
