---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
owns:
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/ChallengesUiTest.kt
  - docs/render-passes/<the day you run>-challenge-scoring/**
  - app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/**
  - CHANGELOG/<the day you run>/challenge-scoring-render-pass.md
  - kb-candidates/<the day you run>-challenge-scoring-render-pass.md
  - sessions/challenge-scoring-render-pass.md
singletons:
  - a device (`emulator-5554` = `Pixel_10_Pro_XL_B`, or Ido's own phone)
  - Gradle daemon, for the debug build
created: 2026-08-24 by challenge-scoring
---

# Run the two layers `challenge-scoring` could not: the instrumented suite, and a look at the badge

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

## 📱 Device state

**SIGN IN NOT NEEDED, and do not sign out.** Everything below renders from a Compose
`setContent` with hand-built state — no Firestore read, no account. If you also want the
in-app look (§4 below), the app's own sign-in helps but is not required.

**Take the `adb install -r` + `am instrument` path, NOT `connectedDebugAndroidTest`** — that
Gradle task uninstalls the app and takes any Google account with it. Mechanism and the
measured proof it preserves a sign-in: `C:\Dev\JARVIS\kb\dev\android-device-verification.md`
§8.

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell am instrument -w com.idomarhaim.goalpilot.test/<runner>
```

## Why this is its own session

`challenge-scoring` shipped `C14`/[`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23)
green at four layers — JVM 1125/1125, functions arithmetic 105/105, **real triggers 17/17**,
security rules 55/55 — and could not run the two that need a device.

**`emulator-5554` was claimed on the board by `62-tour-assembly`**, whose row also still owed
*"Step 0: reverting the demo data seeded on Ido's live account 2026-08-24 11:07"*. Taking it
could have interfered with a revert against Ido's **live** account, and the board carried two
disagreeing statements about that serial (see `CHANGELOG/2026-08-24/challenge-scoring.md` §6).
So the device was left alone and the two layers were reported `unverified` rather than
skipped quietly.

**Check the board first.** If `62-tour-assembly` has released, the device is yours; if not,
this brief waits, and that is the correct outcome rather than a reason to take it anyway.

## Task

### 1. Run the instrumented suite

It **compiles clean** as of `challenge-scoring`'s commit and has never been executed.
`ChallengesUiTest` gained four cases there:

- `myCard_offersScoringFromAGoalAndSaysItIsNotLinkedYet`
- `myCard_saysWhenTheChallengeIsScoringItself`
- `standingsRow_saysWhoTypedTheScoreAndWhat`
- `standingsRow_saysNothingAtAllWhenTheScoreCameFromAGoal`

⚠️ **The whole instrumented suite is known to be order-dependent** —
[`#58`](https://github.com/idomarhaim/Android_Final_Project/issues/58), closed, but read it
before concluding a failure is yours: a different test failed on each full run and each
passed alone.

### 2. Look at the badge, and judge it as a claim about a person

This is the half a green assertion cannot do. `ReportedBadge` renders
*"Reported by Ann · 8200 steps · 24 Aug"* under a standings row, and it is **a claim about
another user shown to everyone in the challenge**. The register to match is `C4`'s — *the app
never asserts an intrinsic edge by itself*. Render it beside a `DERIVED` row and a `NONE` row
and answer:

- Does the badged row read as **information**, or as an **accusation**? If a reader would
  infer *"this one is cheating"*, it is wrong however factual the words are.
- Is it visually **subordinate** to the name and the number — `labelSmall`,
  `onSurfaceVariant`, no icon, no error colour?
- Does the **un**badged row read as the ordinary case, or does the absence look like missing
  data? The design depends on *no badge* being the honest default.
- At a long display name, does it ellipsize before it wraps the row?

### 3. Look at it in Hebrew

`feature/challenges` is **unswept** (`AnalyticsLiteralSweepTest.SWEPT_PACKAGES`), so these
literals are English by rule, not by accident (`AGENTS.md` §0.8). But `AGENTS.md` also says a
screen is not finished until seen in Hebrew, and the specific hazard is real: `ReportedBadge`
builds its sentence by **concatenation**, which is the shape that breaks first under bidi.
Report what you see; **do not sweep the package** — that is
[`#51`](https://github.com/idomarhaim/Android_Final_Project/issues/51)'s, and adding a package
to the sweep as a favour is explicitly forbidden.

### 4. Optional, if the device is signed in: the real flow

Create a challenge, link a goal, log progress against that goal, and watch the standing move
with **no typing at all**. That is `R1` — *"a shared CHALLENGE does not sync with my tasks or
with my Health Connect"* — closing on the real app. The emulator triggers already prove the
server half; this proves the client renders it.

## Read first

1. `AGENTS.md`
2. `CHANGELOG/2026-08-24/challenge-scoring.md` — §2.4 is the badge's whole design and §6 is
   why this brief exists
3. `docs/PRODUCT_v0.3.md` §6
4. `C:\Dev\JARVIS\kb\dev\android-device-verification.md` §8 (the no-uninstall path) and §6.2

## Carries over

- `CHANGELOG/2026-08-24/challenge-scoring.md` — the four green layers and the two owed ones
- `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/ChallengesUiTest.kt` — compiles, unrun
- `SESSIONS.md` — the note addressed to `62-tour-assembly` about `emulator-5554`

## Out of scope

- The measure-approval flow (`sessions/challenge-measure-approval.md`)
- The Health Connect join gate and *"joining creates a goal"*
  (`sessions/challenge-health-gate.md`)
- Anything under `ui/theme`, `ui/components`, `ui/widget`, `feature/dashboard`,
  `feature/analytics` — `visual-parity` reserved those on 2026-08-24

## Exit

The instrumented suite run and its result recorded verbatim; the badge looked at and judged
in both languages, with screenshots under `docs/render-passes/<day>-challenge-scoring/`; a
`CHANGELOG/<day>/challenge-scoring-render-pass.md`; commit on approval. **A defect found here
is the point** — fix it if it is in `feature/challenges/**`, and file it if it is not.
