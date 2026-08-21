# `58-instrumented-order` — 2026-08-21

**`#58` — the instrumented suite is order-dependent.** Two causes, as the brief
predicted; one of them turned out to be already fixed, and a third defect fell
out of checking the exit conditions.

---

## 🔎 Cause 1 — `AiSectionUiTest`: the keyboard moves the layout out from under the click

**The ticket's hypothesis was wrong, and the stack trace killed it in the first
run.** `#58` proposed that `performTextInput` raises the IME and the *following*
`performClick` is consumed dismissing it — the behaviour `docs/OPERATIONS.md` §4
already recorded for a human tapping the screen. Compose's `performClick` injects
straight into the view hierarchy, so no IME window can eat it, and the first
failure I captured was on a `performClick` **before any text had been typed**.

**What is actually happening.** Focusing a text field raises the soft keyboard.
On Android 11+ the keyboard arrives as a **window inset animation**: the window
is resized and everything in it slides upward over a few hundred milliseconds.
Compose's idling resource tracks recompositions, its own frame clock and pending
measure/layout passes — a *system* inset animation is none of those. So
`waitForIdle()` reports idle while the layout is still travelling, the next
`performClick` reads its target's bounds, injects a touch at their centre, and
the target has moved by the time the event lands. **The click is silently lost:**
no exception, no failed assertion at that line.

**The evidence, in order.**

1. `ComposeNotIdleException: … busy due to pending setContent`, twice in one run.
   Decompiling `ui-test-android:1.7.6` gives that diagnostic's exact meaning —
   `isBusyAttaching = view.rootView.parent != null && !view.isAttachedToWindow` —
   a compose root added to the window manager but never attached.
2. `settings get system screen_off_timeout` = `2147483647` and
   `stay_on_while_plugged_in` = `1`, killing "the display slept mid-suite" for
   the price of one command.
3. **The semantics tree dumped at the moment of failure**, via a `catch` that
   logged and rethrew (so the happy path was unperturbed):
   - `settings_ai_key_field` held `EditableText = '••••••••••••••••••••••••••••'`
     — all **28** typed characters, `Focused = 'true'`. The text landed.
   - `settings_ai_save` carried `Actions = [OnClick, RequestFocus]` and no
     `Disabled`. The button was live.
   - The editor sheet's content box sat at `0..1984px` inside a `2992px` window —
     a *bottom* sheet nowhere near the bottom, because the keyboard was holding
     the lower third.
   - No `onSave` was logged between the marks. **The click never arrived.**
4. **JUnit's default method order is `String.hashCode()`, so it is fixed per
   build.** Computing it puts `aBlankModelRendersTheProvidersDefaultRatherThanNothing`
   at position **1** and `replacingTheKeyChangesTheMaskedTail` at position **2** —
   the only two tests in the class that have ever failed, and the two that run
   right after the previous class tore its activity down. So this half of the
   "order-dependence" is not a dependency between tests at all; it is a
   dependency on how far the keyboard had got.
5. **An accidental control.** My first diagnostic added `fetchSemanticsNode()`
   calls between the steps, and the suite then passed **8/8**. Each of those
   calls is an extra idle sync — the Heisenbug was the first evidence that an
   extra settle removes the failure, and it is what the fix does deliberately.

**Why the two symptoms looked unrelated.** Both are the same lost click.
`replacingTheKey…` had an existing credential, so Save was enabled and *would*
have fired — the mask stayed `9876` instead of `1111` (the ticket's shape).
`aBlankModel…` had none, so the missed click left the sheet open and
`settings_ai_model` never rendered: *"Expected exactly '1' node but could not
find any"*.

### The fix — class-level, and it changes nothing on the device

`app/src/androidTest/.../ui/ImeSettling.kt` — `performTextInputAndSettle`,
`performTextReplacementAndSettle`, `performTextClearanceAndSettle`. Each performs
the real action, then samples the node's `boundsInRoot` until it is identical
across 3 consecutive reads 32 ms apart (bounded at 2 s, so a layout that never
settles fails the caller's assertion rather than hanging the suite).
`boundsInRoot` is deliberately the quantity sampled: it is exactly what
`performTouchInput` converts into the coordinates it injects at.

**All 22 call sites converted**, across 6 classes — not just the two that
happened to show the failure. The whole `performText*` family is covered, not
only `performTextInput`: `performTextReplacement` and `performTextClearance` call
`getNodeAndFocus` too, so they raise the keyboard identically, and clearance is
the one least likely to be *expected* to.

**The fix needs no AVD or device setting.** `#58`'s option 3 (disable the
emulator's soft keyboard) would also plausibly work, but it persists on the AVD
and silently changes the ground under every other session sharing it. The wait
needs nothing from the device, so it holds on CI and for a human running
`adb shell am instrument` by hand. `--no-window-animation` was measured as an
alternative and abandoned — see *Not done*, below.

⚠️ **That is a statement about the fix, not about how tidily this session behaved.**
I *did* leave the animation scales at `0.0` for about 90 minutes — see the
correction at the end of this file. They are restored, and the AVD is back where
the session found it.

### A test that would have caught it

`app/src/test/.../testing/ImeSettleSweepTest.kt` — a JVM sweep, in the idiom of
`AnalyticsLiteralSweepTest`, that fails the build if any raw member of the family
reappears in `androidTest/`. It costs no device and no minutes, and it fires when
the line is *written* rather than one run in four thereafter.

⚠️ **And it was silent when I first checked it.** `testDebugUnitTest`'s tracked
inputs are the main and unit-test source sets, so a commit touching only
`src/androidTest/` leaves the task **UP-TO-DATE** and the sweep reports its
previous result — green — without running. That is precisely the case it exists
for. `Observed:` a raw `performTextInput` deliberately reintroduced into
`SilentFilingUiTest.kt` passed with **no output**, and failed correctly only under
`--rerun-tasks`. Fixed by declaring `src/androidTest` as an input to `Test` tasks
in `app/build.gradle.kts`.

The guard was then verified in **both** directions, without `--rerun-tasks`:
green on a clean tree, and red — naming the right file and line — for each of the
three family members reintroduced one at a time.

---

## 🔎 Cause 2 — `NotificationObservedFireTest`: already fixed, by `#56`, after the ticket was written

The brief was right that this has a different cause, and right to say the ticket
had never read the file. It is not the IME.

The version that failed read the shade on the line after posting:

```kotlin
notifier.notifyPlanTomorrow()
val posted = active().single { it.id == GoalPilotNotifier.ID_PLAN_TOMORROW }
```

`NotificationManagerCompat.notify` is a binder hop to `NotificationManagerService`
and returns before `activeNotifications` reflects the post, so `single` throws
`NoSuchElementException` when nothing has landed yet. It is order-dependent
because this suite *deliberately accumulates* notifications: the more records the
service is holding, the likelier the read outruns the write. In isolation the
shade is nearly empty and the post always wins.

**`56-occurrence-model` diagnosed and fixed this in `c2c9171` (2026-08-21 16:59)
— after `#58` was opened**, replacing the immediate read with `awaitPosted`, a
bounded 3 s poll. That commit's KDoc says so explicitly and calls out that it is
*not* `#58`'s IME hypothesis. Nothing was owed here beyond confirming it: the
suite passed in **every** run today — 13 full-suite runs and ~40 harness cycles.

---

## 🐛 Found while checking the exit conditions — the shade was being wiped

`#58`'s exit says the notification suite must still leave its notifications
posted, because that is what `dumpsys` evidence collection depends on. **It was
not holding.** After a fully green suite run,
`adb shell dumpsys notification --noredact` showed **zero** GoalPilot records.

`NotificationPermissionRefusedTest.startFromAKnownShade()` called
`notificationManager().cancelAll()` in `@Before`. The runner orders classes by
name, `Observed` < `Permission`, so it ran immediately after
`NotificationObservedFireTest` and wiped exactly the evidence that suite exists
to leave — **one file away from the KDoc explaining why it must not be, and
shipped in the same commit as it (`99d3e31`, `8-notifications`, 2026-08-20).**
`#58`'s option 4 proposed this for the *other* suite and was withdrawn on the
ticket for this reason. It had already shipped here.

**Fixed properly rather than by deleting the assertion.** The three tests there
claim *the refused notifier posted nothing*; spelled as "the shade is empty" that
is only true if the shade started empty, which is what the `cancelAll` was
buying. They now compare a **fingerprint of the shade before and after** —
`id`, `tag` and `postTime` per record. That is strictly stronger than the old
assertion: `ID_FILING` and `ID_PLAN_TOMORROW` are usually already up when this
class runs, so a notifier that posted despite being refused would **replace**
them, leaving an unchanged set of ids and tags and passing for the wrong reason.
`postTime` moves on every post, so a replacement shows.

Verified after the fix: the shade holds all four observed notifications
(`8001` filing, `8002` plan-tomorrow, `8003/observed-deadline`,
`8003/observed-unmoved`) plus the system's own `AUTOGROUP_SUMMARY`.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **708 tests, 0 failures, 0 errors** (`:app:testDebugUnitTest --rerun-tasks`, so not a cached result) |
| **Instrumented** | **190/190 green ×3 consecutive** at animation scale `1.0` (219–226 s each), via `install -r` + `am instrument`. A further 13 green full runs ran earlier with the scales at `0.0` — see the correction below |
| **Targeted repro harness** | **20/20 green** at scale `1.0` on the exact two-class boundary that failed — against a measured control of **4 failures in 18 cycles**, so 20 clean cycles is ~0.7% likely by chance |
| **Guard, negative control** | red for each of `performTextInput`, `performTextReplacement`, `performTextClearance` reintroduced one at a time, each naming the right file and line; green on a clean tree; both **without** `--rerun-tasks` |
| **`dumpsys` evidence property** | 10 GoalPilot records still in the shade after the final run (4 observed + system summaries) |
| Security rules (`firestore-tests`) | not touched by this change — no rules, DTO or function was modified |

**Never `connectedDebugAndroidTest`** — every instrumented run used `install -r`
plus `adb shell am instrument`, so the app was never uninstalled.

---

## ⚠️ Two operational findings, neither of them `#58`

**1 · Killing the `am instrument` *client* does not stop the run on the device.**
A tool-call timeout killed my adb client; the instrumentation kept running. A
second run started on top of it drove `emulator-5554` to `offline` — `adb devices`
still listed it and `adb get-state` said `device`, while every `adb shell` hung
forever. Recovery cost a scoped force-stop of the AVD's two processes, a lock
cleanup and a cold boot, **and the app package with its data** (the system Google
account survived). Every later run went through a script that force-stops any
leftover instrumentation first and is launched with `nohup` so no timeout can
orphan it.

**2 · The AVD dies under back-to-back instrumented runs, always in the locale
classes.** Three separate deaths today, each during `AppLocaleDialogTest` or
`ComponentsLocaleTest` — the two suites that create the most dialog windows. The
host had ~4 GB free at the time, and `AGENTS.md`'s own CI note says an AVD costs
2–4 GB on top of the Gradle daemon's 2.5 GB. Stopping the daemon before device
work made the runs stable (20 harness cycles, then 3+3+4 full runs, no death).
`Inferred:` host memory pressure; not proven, and **not `#58`** — it is an
emulator/host failure, not a test failure. Worth its own ticket if it recurs.

---

## Not done, and why

- **`--no-window-animation` / `testOptions { animationsDisabled = true }`.**
  Measured as an alternative fix (8 clean cycles before the AVD died, which is
  not enough to conclude anything) and dropped. It would only *mask* the race by
  making the animation instant, the repo already tells people not to use
  `connectedDebugAndroidTest` locally, and CI's emulator action already sets
  `disable-animations: true` — which is why CI never saw this. The wait works with
  animation scales at `1.0` — verified by a re-run at `1.0` after the correction below,
  **not** by the 13 runs first reported here, which turned out to have animations off.
- **The two failing tests' assertions** — out of scope by the brief, and they were
  never wrong. They are unchanged.
- **A broader flake audit** — out of scope by the brief. The two recorded
  instances have their causes.

---

## ⚠️ Correction, same session, appended after the end-of-session device check

**The claim *"all 13 green runs were done with animation scales at `1.0`"* was FALSE when
written, and the sentences carrying it are corrected in place above rather than left to
propagate by copying.** What is true is now true by *re-measurement*, not by those runs.

**What happened.** The `--no-window-animation` arm (measured as a rival fix and rejected) works by
setting `window_animation_scale`, `transition_animation_scale` and `animator_duration_scale` to
`0`, and **restoring them when the run ends**. Two cycles of that arm hung and were killed by my
own `timeout`, so they never reached the restore. The scales stayed at `0.0` for the rest of the
evening, and **every verification run after ~19:55 inherited them** — including the 13 I then
reported as evidence that the fix works with animations on.

`Observed:` the end-of-session check printed `window_animation_scale=0.0`, and the wall-clock
corroborates it independently — those runs took **140–171 s**, while a run at `1.0` takes
**219–226 s**. The suite was ~35% faster because the animations it was supposedly waiting through
were not running.

**Why it matters rather than being a footnote.** It is the load-bearing claim of the whole fix.
*"The wait works with animations on"* is the entire argument for rejecting `--no-window-animation`,
and for the board note telling the `#57` briefs they inherit no device setting. Both were being
asserted from runs that had animations **off** — i.e. from runs the rejected fix was silently
applying.

**Re-verified after restoring the scales to `1.0`:**

| Check | Result |
|---|---|
| Boundary harness (`ComponentsLocaleTest` + `AiSectionUiTest`) | **20/20 green** at `1.0` |
| Full suite | **190/190 green ×3 consecutive** at `1.0` (219 s, 223 s, 226 s) |
| Scales after the run | `1.0` / `1.0` / `1.0` — the AVD is back where the session found it |
| `dumpsys` evidence property | 10 GoalPilot records still posted |

So the fix does hold with animations on; the earlier evidence for it simply was not evidence.

**The generalisable bit, and it is nastier than this one instance.** *A control run that
temporarily changes global state, killed before its restore, leaves every later measurement
silently inside the arm you thought you had finished.* The rejected arm goes on running, unlabelled
and unnoticed — and it makes the thing under test **pass**, so nothing goes red to tell you. Two
cheap habits: **re-read the state you borrowed after any killed run**, not just at the end; and
treat a **sudden speed-up** in a suite you did not make faster as a signal about the environment,
not a reward.

`Untested:` whether any of the earlier 13 runs would have failed at `1.0`. They were not re-run at
their own builds, and the three builds differ — so the honest reading is that those runs measured
something other than what they claimed, not that they were wrong about the outcome.
