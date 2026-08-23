# The guided tour, brought back into line with the app it describes

**Session** `tour-refresh` · **2026-08-24** · no ticket — Ido asked whether the tutorial covered
`#60`–`#70`, and it did not

The in-app tour shipped on 2026-08-22 (`8b4407e`) and had **exactly one commit in its history**.
`#60` shipped the next day and moved the bottom bar under it. Nothing re-read the tour afterwards,
so step 6 spent two days pointing at a hole around four tabs and naming one that was no longer in
it.

**This is not a content gap. It was a false statement in shipped copy**, in both locales, on the
sixth of seven screens of the first thing a new user sees.

---

## What was wrong

`tutorial_explore_body` read *"Social holds challenges and friends. Profile holds your analytics,
life areas and achievements."* — with `TutorialAnchor.NAV_BAR` cutting a spotlight around the whole
bottom bar.

`#60` (`7452122`) implemented spec §4.2: *"Five is a crowded bar, so Profile moves to an avatar in
Home's top-right, and Calendar takes the freed tab."* From that commit onward the hole contained
**Home · Goals · Calendar · Social**, and the sentence inside it named a destination that was not
there — while the app's newest surface went unmentioned by the tour that exists to introduce it.

Nothing failed. `TutorialStepsTest` checks that every anchor a step names is *applied by some
screen*; `NAV_BAR` still was. No test reads what a step **says** about what is inside its spotlight,
and none reasonably could.

## What changed

| | Before | After |
|---|---|---|
| Step 6 | `EXPLORE` → `NAV_BAR`, "Social … Profile …" | `CALENDAR` → `TAB_CALENDAR`, "Your goals, as time" |
| Step 7 | "Your account, and this tour" — appearance, language, replay | "Your profile, and this tour" — profile, analytics, life areas, achievements, **then** settings and replay |
| Anchors | 6, including `NAV_BAR` | 6, `NAV_BAR` retired for `TAB_CALENDAR` |
| `TUTORIAL_VERSION` | `1` | **`2`** — every existing install is re-shown the tour |

**Still seven steps.** The tour did not grow; it stopped being wrong. Profile moved into the last
step, where the avatar was *already* under the spotlight — which is the only place the two facts can
honestly be said together now that the tab is gone. Social lost its mention and does not want one: a
labelled tab with an icon is discoverable by looking, which is this file's own stated cut.

## The drag is a sentence, not a step — and the reason is the user's calendar, not the step budget

`#68` shipped *drag to move*, a `detectDragGesturesAfterLongPress` on the timed lane, and a hidden
gesture is exactly what a coach mark is for. It still did not get a step of its own:

> **A first-run calendar is empty.** A dedicated step would spotlight a lane with nothing in it and
> ask for a gesture with no target — on the one run of the app where that is *guaranteed*.

So the tab is what gets pointed at, and the drag is the second sentence of that step: a promise
about what is done there once there is something to drag. This is a correctness argument, not a
length one; the seven-step ceiling in `TutorialStepsTest` was never reached.

## What deliberately got nothing

- **`#61` Google Calendar sync** — no user-facing control to teach. `Observed:` grepping
  `feature/settings/` and `feature/profile/` for a calendar toggle returns nothing; `RootViewModel`
  pulls on `APP_FOREGROUND` and the disappearance card surfaces itself on the dashboard.
  `Untested:` whether the Google sign-in consent screen names the calendar scope in a way a user
  would want explained — that is a sign-in flow, not a tour step.
- **`#64` success/failure run, `#65` measure proposal** — one tap behind a step that *is* here, and
  each is a better fit for a first-use tip on its own screen (the file's own rule).
- **`#66` · `#69` · `#70`** — fixes and a verification. Nothing to teach.
- **`#67`** — still open.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **1084 tests, 89 classes, 0 failures, 0 errors** — results stamped 00:15, this session |
| **Instrumented** (`adb install -r` + `am instrument`, `Tutorial*UiTest`) | **14 tests, 0 failures**, `OK (14 tests)` in 20.6 s |
| **Render pass** | 7 screenshots, `emulator-5554`, **looked at** — steps 1, 4, 6 and 7 individually |
| Firestore rules | untouched by this change — not run |

**The version bump was verified end to end rather than reasoned about.** The emulator's
`goalpilot_ui_prefs.xml` held `tutorial_seen_version = 1` before the install. After `adb install -r`
the tour **started by itself** on an install that had already seen the old one, and after pressing
*Done* the same preference read `2`. That is the whole argument for storing an `Int` instead of a
`Boolean`, executed once against a real device.

**Device:** `emulator-5554`, signed in as an existing user throughout. `adb install -r` on both
APKs — never `connectedDebugAndroidTest` — so the app's Firebase auth store survived and no sign-in
was destroyed.

## ⚠️ `#62` now has to record a tour it has not seen

[`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62) is *"re-record the app tour
after `#59`/`#60`/`#61` land"* — written when the thing being re-recorded was the **app**, with the
tour a fixed camera path over it. As of this commit the **tour itself** changed: step 6 is a
different step, step 7 says different words, and every existing install re-runs it.

A note to that effect is on `sessions/62-tour-video-v2.md`. It is the one file outside this
session's original claim, added to the row for one paragraph, because a brief that sends a session
to film a stale script costs a whole recording pass — `#62`'s own brief already warns that running
it in the wrong order buys a third recording.

**`app/release-notes.txt` was the same trap in miniature** and is updated here too: it described the
0.3.2 tour to testers, step by step, including *"the rest of the bottom bar"* — the step this
commit deleted.

---

# 📦 The build Ido asked to be sent — and the defect that stopped it

Ido asked, mid-session, for the new build to go to his phone and to
`rachil751@gmail.com`. Both are already in the `testers` group, so nothing outward was newly
granted; the distribution is to a group that already exists.

`v0.3.3` (`versionCode` 7 → 8), release `6thlp69g1amj0`, uploaded to `goalpilot-56e30` by
`:app:appDistributionUploadRelease`.

## ⚠️ The local upload route had been dead for two days, and the guard written to protect it said green

The first upload attempt failed:

```
Failed to read file "C:/Dev/Android_Final_Project/release-notes.txt"
```

**naming the repo root**, with `app/release-notes.txt` present the whole time. So the App
Distribution plugin resolves `releaseNotesFile` against the **root project**, not the `app` module
— the exact opposite of what `ReleaseNotesGuardTest`'s KDoc, `docs/RELEASING.md` and the property's
own value all asserted.

That belief was not free. On 2026-08-22 a session deleted the stray copy at the repo root *because
of it* — and that copy was the file the plugin had been reading all along. Nothing went red for two
days because nobody ran the local route.

| Claim, as it stood | After measuring |
|---|---|
| Plugin resolves against the `app` module | **False.** Repo root, `Observed:` under a real upload |
| `20f3b7e` / `67c21e5` shipped testers a placeholder (`Inferred:`) | **Refuted.** The plugin read the root file, which is the one people were editing |
| Guard asserts both routes name the same file | It **passed while they named different files** — it applied the wrong rule to the Gradle side, and the two errors cancelled |

### Fixed, in the form that cannot rot back

`releaseNotesFile = "app/release-notes.txt"`. That names the same file under **either** reading of
the rule, which a comment explaining which reading is right does not.

### The guard is now non-vacuous, and it was not before — measured twice

- Mutating the property back to `"release-notes.txt"` and re-running the class: **`BUILD SUCCESSFUL`
  in 9 s, nothing executed.** With `--rerun-tasks`, **three of four assertions fail.**
- The cause is a **third missing `inputs.file`**. The KDoc says *"if this class grows a third file,
  declare that one too"* without noticing the class already had one: it reads `app/build.gradle.kts`,
  and a build script is not an input to a test task. Declared now.
- Re-measured after declaring it: the same mutation fails **without** `--rerun-tasks`. The guard
  finally catches the edit it exists to catch.

This is the third instance in three days of *a guard reading a file Gradle does not associate with
it*, and the same shape as `f25cca5`'s cached-green: **a test that never ran is not a test that
passed**, and the two are indistinguishable from the console line.

## 🧪 Tests, after the release-path fix

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **1084 tests, 89 classes, 0 failures, 0 errors** |
| **Guard mutation check** | broken value → 3 of 4 assertions fail, no `--rerun-tasks` needed |
| **Release build** | `:app:assembleRelease` green, signed with the real release key (`validateSigningRelease` ran; the upload task's `hasReleaseKey` check refuses a debug-signed artifact) |
| **Upload** | `6thlp69g1amj0`, group `testers` |

`Untested:` what the two testers actually see on their phones. The Firebase CLI has no
`appdistribution:releases:list`, so the notes cannot be read back from a shell — this is the same
honest limit the guard's KDoc already records, and it is why the notes file is guarded rather than
verified.
