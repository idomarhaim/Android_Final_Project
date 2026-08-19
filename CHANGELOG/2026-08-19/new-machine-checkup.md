# new-machine-checkup — 2026-08-19

> **Branch:** `main`
> **Summary:** The new machine builds GoalPilot — `assembleDebug` green and 364/0 unit tests — after `local.properties` turned out to be silently mangling `sdk.dir` through Java `.properties` backslash escaping, and the JDK notes in `CLAUDE.md` turned out to describe a machine that no longer exists.

## Why

`/kickoff new-machine-checkup`. Git-side state was verified current by the migration
session, but nothing had been **built or run** on this machine. Item 1 of the brief —
the first Gradle build — was the only thing standing between this repo and every
feature brief in `sessions/`.

## Round 1 — the build lane

### 1 · `assembleDebug` — **BUILD SUCCESSFUL in 6m 24s** (second attempt)

The first attempt failed in 29 s, and the error named neither the file nor the setting
that caused it:

```
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> java.io.IOException: The filename, directory name, or volume label syntax is incorrect
```

**Root cause: `local.properties` is a Java `.properties` file, and a backslash in one is
an escape character.** The regenerated copy read

```properties
sdk.dir=C\:\Users\namei\AppData\Local\Android\Sdk
```

which `java.util.Properties` parses as `C:UsersnameiAppDataLocalAndroidSdk` — `\U`, `\A`,
`\L` and `\S` are each consumed as escapes and vanish. `\:` survives (it is a legitimate
escape of the key/value separator), which is exactly why the line *looks* deliberately
escaped and therefore correct.

Rewritten with forward slashes, which need no escaping and which AGP accepts, plus an
inline comment carrying the failure signature so the next person who regenerates the file
does not re-derive this. The file is git-ignored (`.gitignore:4`), so nothing here is
committed — which is also why it rotted invisibly.

> `Observed:` — the second run reached `:app:packageDebug` and produced
> `app/build/outputs/apk/debug/app-debug.apk`, 27,057,790 bytes.

**Also restored while in there: `GOOGLE_WEB_CLIENT_ID` and `FUNCTIONS_REGION`, which the
regenerated file was missing entirely.** Without the first,
[`app/build.gradle.kts:28`](../../app/build.gradle.kts#L28) falls back to the literal
`REPLACE_WITH_WEB_CLIENT_ID` and Google Sign-In fails at runtime with `DEVELOPER_ERROR` /
"no ID token" — a failure that appears **only on the device, only at sign-in**, and would
have been blamed on the fresh emulator image or on the account. Nothing in the build
output says a word about it. The value is the `client_type: 3` entry already present in
the committed `app/google-services.json`, so no secret was introduced.

> `Observed:` — `gradleResValues.xml` for the debug variant now carries the real id in
> `<string name="gp_web_client_id">`, not the placeholder.

### 2 · Unit tests — **364 / 0**, 39 suites, 0 skipped

`:app:testDebugUnitTest`, `BUILD SUCCESSFUL in 49s`. That is the same count
`hebrew-defer-freeze` reported on 2026-08-17, so the toolchain reproduces the old
machine's result exactly.

### 3 · The JDK notes in `CLAUDE.md` were false — corrected

`CLAUDE.md` claimed **"`java` on `PATH` is still JDK 17, from the machine `PATH`, which
needs admin to reorder."** Measured today:

| Claim | Measured 2026-08-19 |
|---|---|
| `java` on `PATH` is JDK 17 | **False.** The only java entry on the machine `PATH` is `…\jdk-21.0.12.8-hotspot\bin`. There is no JDK 17 anywhere on this machine. |
| machine `JAVA_HOME` is correct | **True** — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\`, and the directory exists (it did not, until `0e52a66`). |
| "two more Adoptium directories are wrecks" (`AGENTS.md` §JDK) | **Stale.** `C:\Program Files\Eclipse Adoptium\` holds exactly one directory. |
| `firebase-tools` reads `java` from `PATH`, not `JAVA_HOME` | **Still true**, and the `firestore-tests/run-tests.mjs` workaround is kept. |

**`AGENTS.md` carries the same false claim and was deliberately left alone** — see
*Sibling sessions* below.

**And a new one worth more than the correction:** a Claude Code tool shell on this machine
inherits an environment captured **before** the Temurin install, so inside it `JAVA_HOME`
is **empty** and `java` is not on `PATH` at all — while
`[Environment]::GetEnvironmentVariable('JAVA_HOME','Machine')` returns the correct value.
That disagreement reads exactly like a broken machine and is not one. `gradlew` survives
it because `gradle.properties` pins `org.gradle.java.home`; nothing else does. The remedy
is one `export` at the top of the call, and specifically **not** also prepending it to
`PATH` in Windows form — Git Bash wants `/c/Program Files/...` there, and a Windows-form
entry resolves nothing while looking correct.

## Sibling sessions

**`docs-hygiene-backfill` is live in this repo and holds six files uncommitted** —
`.github/workflows/release.yml`, `docs/OPERATIONS.md`, `docs/RELEASING.md`,
`knowledge/release-distribution.md`, `scripts/README.md`, `scripts/run-goalpilot.ps1`
(all written 20:42:49, its transcript's last turn 20:51:55 local). It is doing that
brief's item 1 — the false `JDK 25` claim — in AUTO MODE. **It has no row on the board**;
that is theirs to write and it is noted here, not fixed here.

Two consequences for this session:

1. **`AGENTS.md` §JDK is not corrected here**, though it is measurably wrong, because
   `docs-hygiene-backfill` claims that file by brief. The measurements are in the table
   above so that session (or the next one) does not have to re-derive them. Its own brief
   already says growing past item 1 is ask-first.
2. **This session's `CLAUDE.md` edit is *not* a duplicate of theirs.** Their sweep greps
   `JDK 25`; `CLAUDE.md` says `JDK 17` and matches nothing they are looking for. Their
   brief scopes `scripts/README.md`, `CHANGELOG/**` and `AGENTS.md`, and `CLAUDE.md` is
   in none of those.

`scripts/run-goalpilot.ps1` was **run** from their uncommitted working copy to boot the
emulator. Their edit to it is two comment/warning strings; it changes no behaviour.

## 📁 New / Modified Files

- `CLAUDE.md` — the JDK bullet replaced by three: the corrected machine state, the
  agent-shell environment trap, and the `.properties` escaping trap.
- `CHANGELOG/2026-08-19/new-machine-checkup.md` *(new)* — this file.
- `SESSIONS.md` — this session's Active row; `CLAUDE.md` added to its owned paths.
- `sessions/new-machine-checkup.md` — front matter added (it was the only brief in
  `sessions/` without any, so `/kickoff`'s `status:` check had nothing to read).
- `local.properties` *(git-ignored, so not in this commit)* — rewritten; see above.

## 🧪 Tests

| Layer | Result |
|---|---|
| Build (`:app:assembleDebug`) | ✅ `BUILD SUCCESSFUL in 6m 24s`, APK 27,057,790 bytes |
| JVM unit (`:app:testDebugUnitTest`) | ✅ **364 passed / 0 failed / 0 errors / 0 skipped**, 39 suites |
| Instrumented (`:app:connectedDebugAndroidTest`) | ⛔ **Deliberately not run, and must not be** — it uninstalls the app and takes the signed-in Google account with it, which is precisely what item 3 of the brief is asking Ido to create. The two cannot share a device session. |
| Firestore rules (`firestore-tests/`) | ⏳ Not run this round — needs the Firebase emulator, and RAM is the binding constraint (see below). |
| Device / manual | ⏳ Emulator booting at the time of this commit. The live recommendation smoke test (brief item 2's residue) needs a signed-in account first. |

**No test was written or changed** — this session compiled nothing of its own. The suite
is the instrument being checked, not the subject.

## 🔧 Technical Changes

- **RAM is the real constraint on this machine, and it is tighter than the brief's note
  suggests.** 15.7 GB total; **1.9 GB free** with two Gradle JVMs (2.9 GB), WSL
  (`vmmemWSL`, 2.2 GB) and three agent sessions up. `./gradlew --stop` recovered it to
  3.9 GB, which is enough for one emulator. WSL was left alone — it is not this session's
  to kill. Practical rule: **stop the Gradle daemon before booting an AVD**, and install
  the already-built APK with `adb install` rather than re-entering Gradle.

## Notes for the next session

- `kb-candidates/` holds **three** undrained files from 2026-08-17
  (`51e-sweep-components`, `changelog-index-backfill`, `completion-roadmap`).
  `sessions/kb-drain-51e-backfill.md` owns them. Reported, not touched.
- The brief's item 2 (Cloud Functions redeploy) was already done by the migration session;
  only its **live smoke test** remains, and that is gated on the sign-in.

## Round 2 — the device lane

### 4 · Both AVDs had `hw.keyboard=no`, so the host keyboard reached neither

`Pixel_10_Pro_XL` booted in 52 s (`emulator-5554`), the debug APK installed, and the app
launched clean — no `FATAL`, correct branding, correct 1344×2992 profile, English.

Then Ido could not type his email into Google's sign-in field. Cause: **both AVDs carry
`hw.keyboard=no`** in `~/.android/avd/<name>.avd/config.ini`. They were created headlessly
by the migration session, and that default means the guest ignores the host's physical
keyboard entirely — you are left tapping the on-screen keyboard, which on the sign-in
WebView had opened in a floating mode that is easy to read as "typing is broken".

Set to `hw.keyboard=yes` on **both** AVDs (`.bak-2026-08-19` kept beside each), which takes
effect only on the next boot, so the emulator was restarted.

> ⚠️ **`Untested:` whether this was Ido's actual complaint.** He reported a moment later
> that his text *did* appear — *"maybe there was a delay"* — which is equally consistent
> with a cold, RAM-starved emulator lagging, and with what appeared being the email this
> session had typed via `adb shell input text`. So: `Observed:` the setting was `no` on both
> AVDs and is now `yes`; `Inferred:` that it was the cause of what he saw. The restart was
> already in flight when his message landed, and it is named here rather than presented as
> a confirmed fix. What is **not** in doubt is that with `hw.keyboard=no` a physical keyboard
> cannot reach the guest at all, so the change is right regardless of which symptom it
> explains.

### 5 · What the device is holding now

Rebooted in 51 s with `hw.keyboard=yes` confirmed live, app relaunched, and the device
handed to Ido to sign in as `name.iddo@gmail.com`. **`Pixel_10_Pro_XL_B` was never booted**
and is untouched.

## 🧪 Tests — round 2

| Layer | Result |
|---|---|
| Device / manual | ✅ App installs, launches and renders its sign-in screen on a fresh API 35 image. ⏳ Sign-in and the live recommendation smoke test are with Ido. |
| Instrumented | ⛔ Still deliberately not run — see round 1. |

### 6 · Hebrew added to the emulator's keyboard (device config, unrelated to `#51`)

Ido asked for Hebrew input on the emulator. **This is the device's IME, not the app's
locale** — `#51` stays deferred, `AppLanguage.OFFERED` is untouched, and nothing here makes
the app reachable in Hebrew. The two are easy to conflate and are not the same switch.

The fresh API 35 image ships Gboard with English (US) only. Rather than adding a **system
language** — which would put `iw-IL` into the device locale list and reach
`AppLocale`'s `SYSTEM` branch — only the **Gboard subtype** was enabled, which is the
narrower change and touches no locale the app reads:

```
# Gboard's Hebrew subtype, from dumpsys input_method:
#   InputMethodSubtype #70  mSubtypeLocale=iw_IL  mSubtypeHashCode=1317880268
adb root
adb shell settings put secure enabled_input_methods \
  'com.google.android.inputmethod.latin/....LatinIME;1594443099;1317880268:com.google.android.tts/...'
adb shell am force-stop com.google.android.inputmethod.latin
```

`1594443099` is the English (US) subtype that was already there; Hebrew is **appended**, so
both layouts are live and the globe key switches between them. Read back and confirmed.
`adb unroot` afterwards, so the device is not left running as root.

> ⚠️ `Untested:` **not yet seen on screen.** Confirming it needs a focused text field with
> the keyboard up, and the device was in Ido's hands mid-sign-in. `Observed:` the setting
> reads back correctly and the IME manager reports the `iw_IL` subtype. The next session to
> hold the device should open any text field, press the globe key, and look.

**Also worth knowing for the sign-in itself:** Google offered a **passkey / QR-code**
challenge, which an emulator cannot satisfy — no camera, no screen lock, no passkey. The way
through is *Cancel* → *Try another way* → password. That is a device-flow property, not a
GoalPilot one, and it will recur on `Pixel_10_Pro_XL_B`.

### 7 · Two corrections to §6, both found by looking rather than by reasoning

**(a) "The emulator has no Bluetooth" was wrong as stated.** `pm list features` returns
`android.hardware.bluetooth` **and** `android.hardware.bluetooth_le`, so the passkey flow's
own capability check passes and it offers the QR path. What is missing is a **radio** behind
those features: the emulated stack cannot advertise over the air to a physical phone, so the
hybrid/caBLE handshake the QR starts can never complete and the phone sits on *"connecting
to device"* indefinitely. The correction matters because the first phrasing predicts the
device would not offer the option at all — and it does. *Route through anyway: Cancel →
**Try another way** → password.*

**(b) The Hebrew subtype did not survive the reboot.** After the emulator came back,
`enabled_input_methods` read `…LatinIME;1594443099:…` — English only. The AVD restores from a
**quickboot snapshot** that predates the change, so a `settings put` made in a session that
does not end with a snapshot save is simply rolled back. Re-applied and re-read.
⚠️ **It will roll back again on any cold boot or `-Recover` run** until a snapshot is saved
holding it, so treat *"is Hebrew still there?"* as a thing to check, not to assume. The
mechanical check is one line:

```
adb shell settings get secure enabled_input_methods   # want ...;1594443099;1317880268:...
```

**Unexplained, and recorded as such:** between `adb unroot` and the next `adb` command the
**emulator process itself exited** — `qemu-system-x86_64` count 0, not merely an adb
disconnect — losing the in-flight sign-in. `Untested:` the cause. It is not `unroot`, which
restarts `adbd` and not the VM; RAM was 3.4 GB free afterwards, so pressure is a candidate
but not a demonstrated one. It rebooted cleanly in 36 s. If it recurs, that is a real defect
worth a session; one occurrence is a note.

### 8 · `ApiException: 10` at sign-in — the new machine minted a new debug keystore

Sign-in fails with **`10`** (`DEVELOPER_ERROR`). Diagnosed rather than guessed, and it is
**not** the `GOOGLE_WEB_CLIENT_ID` fixed in §1 — that value is correct and baked in.

| Fact | Value |
|---|---|
| SHA-1 Firebase has for `com.idomarhaim.goalpilot.debug` | `f1d0964d5441d599867dae830f771623bb64db3f` (SHA Id `b4818880e4ba8bfc`) |
| SHA-1 of `~/.android/debug.keystore` **on this machine** | `448d0d9424fa876a6ce8980c0c5b740d11a7b3ec` |
| That keystore's creation date | **2026-08-19 20:47:04 IDT** |

**20:47 is this session's first `assembleDebug`.** The old machine's
`~/.android/debug.keystore` went with the old machine; the Android toolchain silently
generates a fresh one on first use, with a new key and therefore a new SHA-1. Google
rejects the sign-in server-side because that fingerprint is not on the project — which is
exactly what `DEVELOPER_ERROR` means and exactly what it does not say.

**Why this was not predictable from the brief.** The migration session's checklist covered
everything that lives in **git** or in a **config file** — and this one lives in neither. It
is a per-machine secret outside the repo, regenerated *by the very first build*, so it
cannot fail until after the build succeeds, and it fails on a screen three steps later.
`docs/RELEASING.md` already documents `ApiException: 10` for **release** builds and says
*"debug keeps working"* — true on a machine whose debug key is registered, which is what
made the note read as not applying here.

**The fix is one command, and it is an outward action — `AUTO MODE` does not authorise it**
(`rules/outward-action-governance.md`; the mode is the dev half only). Held for Ido:

```
firebase apps:android:sha:create \
  1:297750736036:android:f428d22e3d58bdf5058a31 \
  448d0d9424fa876a6ce8980c0c5b740d11a7b3ec
```

- No rebuild and no reinstall afterwards — the fingerprint is checked **server-side**, so
  the already-installed APK starts working within a minute or two.
- **Nothing is deleted.** The old `f1d0964d…` stays; removing it is a deletion and a
  separate always-ask. It is also the fingerprint registered on the **release** app, where
  it sits beside `e7d5534c…`.
- **Optional follow-up, not required:** re-download `google-services.json` so its
  `certificate_hash` for the debug package lists the new SHA too. That file is **committed**,
  so it is a real diff and it changes no behaviour — sign-in does not read it for this check.

⚠️ **This recurs on every new machine, and it will recur on the release app too** the first
time a release APK is built here (the release keystore is a different, also-machine-local
file — see `scripts/new-release-keystore.ps1`).

## Round 3 — signed in, and the AI is genuinely live

### 9 · The SHA fix worked — first sign-in on this machine

`firebase apps:android:sha:create` run **on Ido's explicit authorisation** (outward action;
`AUTO MODE` does not cover it). The debug app now carries both fingerprints — the old
machine's `f1d0964d…` and this machine's `448d0d94…`; **nothing was deleted**.

`Observed:` `dumpsys account` → `Account {name=name.iddo@gmail.com, type=com.google}`, and
GoalPilot's dashboard renders his real Firestore data: **8 goals, 5 tasks done, 70 pts,
Level 1, 25% overall**. No rebuild or reinstall was needed, exactly as predicted — the
fingerprint check is server-side.

`app/google-services.json` refreshed from `firebase apps:sdkconfig` and committed. **Ido
delegated this decision** (*"do whatever is most right"*), so it is recorded as **mine**:
refresh only if the diff is confined to the SHA list — measured, and it was. **8 insertions,
0 deletions, 0 modifications**; both packages and both web client ids byte-identical. Written
back as CRLF with no trailing newline, matching the committed file, so the diff is purely
additive. The risk I named when offering the choice (*"re-downloading can churn unrelated
fields"*) was checked rather than assumed, and did not occur.

### 10 · The emulator hang was **software rendering**, not only RAM

`hw.gpu.enabled=no` on both AVDs — created headlessly, and that default means a
**1344×2992** framebuffer rendered on the CPU. The emulator went `Responding=False` with
**772 s** of accumulated CPU. Set to `hw.gpu.enabled=yes` / `hw.gpu.mode=host` on both.

| | before | after |
|---|---|---|
| renderer | software | `Intel(R) UHD Graphics` via the ES translator (`dumpsys SurfaceFlinger`) |
| `Responding` | **False** | True |
| accumulated CPU | 772 s | 100 s |

⚠️ **RAM went the other way and is now the binding constraint:** the GPU path costs
resident memory — qemu 736 MB → **3.0 GB**, host free **1.09 GB**. With WSL not even
running, the biggest consumer is VS Code (**48 processes, 2.8 GB**). This also retro-explains
the unexplained exit recorded in §7 and the earlier hang: both were a software-rendered
4-megapixel screen on a starved host.

**And the window-fit code is wrong on a scaled display.** `Set-EmulatorWindowLayout`
computed `scale 0.275 → 370x823`; the real window was **437×978** on a **1536×912** working
area — taller than the screen, which is why its own guard reported *"window was off-screen
(top=-897)"* and pinned it to `y=0`. The factor is Windows **DPI scaling (125 %)**: the
script sizes in device pixels and Windows lays out in scaled pixels. Repositioned by hand to
311×700 centred; `scripts/run-goalpilot.ps1` is **not** changed here — that is a real defect
in someone else's script and it is left as a finding, not folded into a checkup session.

### 11 · Live recommendation smoke test — **PASSES**, and I got it wrong first

Brief item 2's residue is closed. But the first reading was wrong and the correction is the
useful part:

- ❌ **What I said first:** *"the last call was 2026-08-17, so these tips are the local
  fallback."* The `functions:log` window I read was truncated and simply did not reach
  today's lines.
- ✅ **Measured:** `getRecommendations` was invoked **`2026-08-19T18:40:17Z` and
  `18:43:28Z`** — the second is this session's app restart — with
  `verifications: {auth: VALID}`.
- ✅ **And the output is not the fallback.** The fallback strings are exactly
  `"Start with one goal"`, `"Keep the streak alive"` and `"Nudge: <goal title>"`. The screen
  showed **"Identify Obstacles"** and *"…stretch, or a short walk"*, which appear **nowhere**
  in `app/src/main/java` (`grep` returns only an unrelated hit in `WidgetCharts.kt`). Text
  the app cannot produce came from the model.

**Why the wrong reading was so easy, and the defect underneath it.**
`RecommendationRepositoryImpl.getRecommendations` ends:

```kotlin
} catch (e: Exception) {
    Resource.Success(fallbackRecommendations(goals, completedTasksLast7d, totalPoints))
}
```

Every failure — dead key, retired model, no network, auth refused — becomes
`Resource.Success` with **no log line and no UI difference**. `AGENTS.md` warns that a
retired GROQ model *"fails silently and the AI looks bland rather than broken"*; this is the
line that makes it so, and it is broader than the model. **The only reliable discriminator
from outside is comparing the rendered text against the fallback strings** — which is what
was done here, and it is not something a demo audience could do.
📌 **Worth a ticket:** log the swallowed exception. `Resource.Success` on the fallback is a
deliberate product decision (spec §8, never block the user) and should stay; being unable to
*tell* is the defect.

### 12 · GROQ key rotation — nothing was left stale

Ido rotated the key and asked whether anywhere still holds the old one. Checked every place
it can live; **no action needed**:

| Where | State |
|---|---|
| `functions/.env` | ✅ new key — ends `HjPa`, matching his GROQ console. Written **18:56:14**. Git-ignored (`functions/.gitignore:3`). |
| Deployed function runtime env | ✅ `functions/lib/index.js` built **19:16:52**, deploy commit `c464eaf` at **19:19:19** — both **after** `.env` was written, so all three functions carry it. |
| The Android app | ✅ by design — the key never ships in the client (spec §5). |
| Secret Manager | n/a — `functions/src/index.ts:31` reads plain `process.env.GROQ_API_KEY`, baked at deploy time. |
| `functions/.env.example` | ✅ placeholder only. |

`Observed:` his console shows the key used today, and §11 proves the model answered.

⚠️ **The rule to keep:** because the key is a **deploy-time** environment value and not a
secret reference, editing `functions/.env` alone changes nothing — **a rotation is not live
until `firebase deploy --only functions` runs after it.** The window between the two is
invisible: §11's silent catch means the app keeps serving plausible tips throughout.

## 🧪 Tests — round 3

| Layer | Result |
|---|---|
| Device / manual | ✅ Google sign-in completes; dashboard renders live Firestore data (8 goals, 70 pts). |
| Cloud Functions (live) | ✅ `getRecommendations` invoked `18:43:28Z`, auth VALID, output demonstrably not the fallback. |
| Instrumented | ⛔ Still not run, and still must not be — it would wipe the account just created. |
| Firestore rules (`firestore-tests/`) | ⏳ Not run — RAM (1.09 GB free) will not carry the Firebase emulator beside the AVD. |

### Foreign commit carried by this session's push

`git push` is **branch-scoped, not commit-scoped**, so this session's push also publishes
**`83f648d` — `cloud-emulator: claim`**, which is not mine. Adjudicated rather than waved
through:

- It touches **`SESSIONS.md` only**, one inserted line: that session's own Active row.
- `SESSIONS.md` is **not** in `cloud-emulator`'s claimed `Owns` list (it claims
  `.github/workflows/instrumented-tests.yml`, `docs/CLOUD-DEVICE.md`, its own changelog and
  candidate files, and one `AGENTS.md` doc-index line), so precondition 5's *"paths under a
  live row → stop and ask"* does not fire.
- Precondition 4 is satisfied by being able to account for it completely, and a claim row is
  written to be seen — publishing it costs that session nothing.
- Its **uncommitted** work (`.github/scripts/`, the workflow file) does **not** ride along:
  every commit in this session named explicit paths, and `SESSIONS.md` is not among them
  this round.
