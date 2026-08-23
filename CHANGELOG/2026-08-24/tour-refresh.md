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
