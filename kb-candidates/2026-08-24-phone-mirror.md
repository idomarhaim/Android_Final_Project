# KB candidates — `phone-mirror`, 2026-08-24

Written in normal mode, so this is a **proposal**. Nothing here has been ingested.

---

## 1. A second adb client of a different version silently restarts the adb server — and any bundled `adb.exe` is that client

- **Claim.** `adb` refuses to run a client against a server of a different version: it **kills the
  running server and starts its own**. Every transport on the old server drops. Tools that ship
  their own `adb.exe` — `scrcpy` is the one measured here — therefore become a concurrency hazard
  the moment a sibling session is driving a device, and the hazard is invisible in their output.
  `Observed:` 2026-08-24 — `scrcpy` v4.1's bundled adb is `37.0.0-14910828`, the SDK's is
  `37.0.1-15733141`. The remedy is `ADB=<sdk>/platform-tools/adb.exe` in the environment, which
  `scrcpy` honours; verified by launching a real mirror and re-reading the server version
  (`37.0.1`, unchanged) with both transports still attached.
  `Inferred:` the kill-and-restart step itself — adb's documented mismatch behaviour, **not**
  reproduced here. `Untested:` running `scrcpy.exe` from its own folder with `ADB` unset while a
  second transport is attached, then re-reading `adb version`. **An ingesting session should run
  that check on an idle machine before the page states the consequence flatly** — the whole value
  of the page is the consequence, and it is the half nobody has watched.
- **Why.** The symptom lands on the **wrong session**: the one whose emulator transport dies sees
  *the emulator died* and has no way to connect that to a mirroring tool somebody else started.
  This is the same shape as the board's other cross-session hazards — an action of yours degrading
  a sibling's state through a shared singleton neither of you named. Rejected as a framing: *"a
  read-only tool cannot disturb anything"* — mirroring is read-only **on the device** and not on
  the adb server.
- **Destination.** `kb/dev/android-device-verification.md`, new section — it already owns
  `adb`-mechanics findings (§8 is the `install -r` one).
- **Anchors.** `scripts/mirror-phone.ps1` (the `$env:ADB` pin and the comment above it);
  `scripts/README.md` § *The adb-version trap this script exists to avoid*;
  `SESSIONS.md` note addressed to `challenge-scoring`.
- **Supersedes.** Nothing.
- **Status.** Proposed, not ingested — normal mode.

---

## 2. `scrcpy` is installed on this machine, and how

- **Claim.** `scrcpy` **v4.1** lives at `%LOCALAPPDATA%\Programs\scrcpy` — portable zip, no admin,
  SHA256 checked against the release's `SHA256SUMS.txt`. As with `JAVA_HOME`, `gh` and `ffmpeg`
  before it, **a tool shell opened before the install does not see it**, so use the absolute path
  for the rest of such a session. `winget` was not used and should not be: it blocks on an
  elevation prompt it cannot display, exactly as recorded for `GitHub.cli` and `ffmpeg`.
- **Why.** `CLAUDE.md` already carries this exact pattern three times over; a fourth tool with the
  same trap belongs beside them, not in a session's changelog where nobody looks. Note the
  regularity worth stating once: **every** manually-installed tool on this machine has produced
  the same stale-`PATH` surprise.
- **Destination.** `CLAUDE.md` (project), beside the `ffmpeg` bullet.
- **Anchors.** `scripts/mirror-phone.ps1` (its scrcpy-locating block and its install message).
- **Supersedes.** Nothing.
- **Status.** Proposed, not ingested — normal mode.

---

## 3. A Python heredoc turned `\a` into a BELL character inside a committed Markdown file

- **Claim.** Writing repo prose through a non-raw Python string is a member of the family
  `CLAUDE.md` already documents twice — `local.properties` backslash escapes, and `--` inside an
  XML comment. Here `"…\\platform-tools\\adb.exe"` reached Python as `\platform-tools\adb.exe`,
  and `\a` became **0x07**, planted in `SESSIONS.md` in a repo that ships
  `scripts/Assert-NoControlChars.ps1` and a pre-commit hook. **The line rendered as plausible
  text**, so reading it back proved nothing; a `grep -P '[\x00-\x08\x0b\x0c\x0e-\x1f]'` found it
  immediately.
- **Why.** The general rule is already committed (`kb/dev/look-at-your-own-output.md` — *recompute,
  do not read*). What this adds is the **specific check** for prose destined for a repo, and a
  third instance establishing that the failure mode is the *authoring pipeline*, not any one file
  format. Rejected: filing it as a one-off typo — three instances in one `CLAUDE.md` is a pattern.
- **Destination.** `kb/dev/look-at-your-own-output.md`, as a worked instance; a one-line pointer in
  `CLAUDE.md` beside the two existing traps.
- **Anchors.** `CHANGELOG/2026-08-24/phone-mirror.md` § *Tests*.
- **Supersedes.** Nothing.
- **Status.** Proposed, not ingested — normal mode.
