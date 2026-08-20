# `8-notifications` — 2026-08-20

> **Summary:** `#8`'s notification substrate ships: two channels created at app start, `POST_NOTIFICATIONS` asked **at the first filing outcome that speaks** rather than at launch, a tap-through that lands on the proposed goal, `WorkManager` local scheduling with the nightly *plan tomorrow* prompt riding it, and §2.5's backwards-from-duration reminder arithmetic as a pure, tested domain function. The permission-refused path is safe **structurally**, not by a check: the notifier is a second, independent consumer of `DashboardViewModel.filed`, so deleting it entirely leaves `#6`'s snackbar and Undo untouched — a refusal is a non-event rather than a degraded mode. **Two things the brief asked for are NOT built, and both for the same reason:** §2.5's *one reminder per occurrence, timed per rung* and its daily miss review need an **occurrence model that does not exist** — `Task` carries no due date, §2.2's four rungs appear in no Kotlin file, and §2.1 is `C9a` **#25, which is CLOSED**. That is the second instance of the pattern `7-quickadd-complete` found on §1.4/#19 the same day: **a spec section deferring implementation to a closed decision ticket that carries no build and has no owner.** The arithmetic those reminders need is shipped and tested so the loop that walks occurrences has nothing left to derive.

**Session:** `8-notifications` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Brief:** [`sessions/8-notifications.md`](../../sessions/8-notifications.md) · **Issue:** [#8](https://github.com/idomarhaim/Android_Final_Project/issues/8)

---

## What shipped, against the brief's six pieces

| # | Piece | State |
|---|---|---|
| 1 | Notification channel | ✅ **Two**, created in `GoalPilotApp.onCreate`, text in `res/values/strings.xml` |
| 2 | `POST_NOTIFICATIONS` + *when* to ask | ✅ `NotificationPermissionPolicy`, asked at the first **speaking** filing |
| 3 | Tap-through destination | ✅ `NotificationDeepLink` → `goal_detail/<id>` for `NewGoal`, dashboard for `NoGoal` |
| 4 | In-app half | ✅ Already done by `#6`; **nothing added** |
| 5 | Graceful when refused | ✅ Structural — see below |
| 6 | Local scheduling | ✅ `WorkManager`, nightly *plan tomorrow* on it |
| — | §2.5 backwards-from-duration reminder | ✅ **arithmetic** shipped and tested; ⛔ **scheduler blocked, see §"What is not built"** |

---

## The two channels, and why not one

The system's own control is **per channel**, so a channel is the finest grain at which a person
can say *stop telling me this*. `gp_filing` and `gp_reminders` are things someone genuinely
wants separately: silencing *"the sorter could not place that task"* must not also silence
tonight's planning prompt. Splitting further would be worse — a channel per notification is a
settings screen nobody reads.

Their names and descriptions are the one class of user-facing text this ticket **could not**
keep as a Kotlin literal, whatever §0.8's freeze permits in an unswept package: the **system**
renders them, on a screen this app never draws, so nothing else can supply them at the moment
Android asks.

`GoalPilotChannels.ensure` runs unconditionally on every process start rather than behind a
first-run flag. `createNotificationChannel` on an existing id updates the name and description
and leaves the user's own importance and sound choices alone — so a flag would buy nothing and
would freeze the channel text at whatever it said when the flag was written (§0.2).

---

## Piece 2 — the decision the brief asked to be written down

**Ask at the first filing outcome that speaks. Never at launch.**

1. **A denial is close to permanent, so the first ask is very nearly the only ask.** From
   Android 13 the system stops rendering the dialog after two dismissals — `requestPermission`
   still returns, it just returns `false` with nothing shown, and the only remaining route is
   the system settings screen. A cold-launch prompt spends that one ask at its weakest moment.
2. **In context the question answers itself.** The user has just quick-added a task and watched
   a snackbar say no goal fitted. *"Tell you about this next time?"* is legible then, and
   abstract on a sign-in screen.
3. **§0.4** — speak about a failure the user can act on. The dialog is itself an interruption.

**What is deliberately not stored:** there is no *"we already asked"* preference. Android cannot
distinguish **never asked** from **permanently denied** (`shouldShowRequestPermissionRationale`
is `false` for both), so a stored flag is the only way to tell them apart — and it buys nothing,
because a permanently-denied request renders no dialog and re-asking on a later cold start is
invisible. Paying a persisted field, its migration and its capacity to disagree with the device
to avoid an invisible no-op is the wrong trade. The in-memory `NotificationAsk` guard exists
only to stop one launch stacking two dialogs.

**A correction found in self-review before commit.** The policy was first fed
`GoalPilotNotifier.canPost()`, which is *permission* **and** *notifications not switched off in
settings*. That is the wrong question for a **dialog**: on a device where the user had granted
the permission and then switched notifications off, the app would raise a permission request it
already held — one that returns granted instantly, shows nothing, and cannot fix the state that
prompted it. Split into `hasPostPermission()` (what the dialog can change) and `canPost()`
(whether a post could be **seen**), and the policy is now asked the first.

---

## Piece 5 — refusal is structural, which is why it is safe

The brief calls this *"the piece most likely to be skipped, and the one a grader will try."*
What makes it hold here is **not** a check inside `GoalPilotNotifier`:

> `R5`'s in-app half is `DashboardViewModel.filed` → `SmartAddReceiptSnackbar`, a separate
> collector of a separate flow. The notifier is a **second, independent** consumer of the same
> event. It reads and never writes back. Delete the whole `notifications/` package and the
> snackbar, the Undo and the filing itself are unchanged.

So there is no path from *"may not post"* to any other behaviour in the app, and `notifyFiling`
returns `Unit` — never a result anything could branch on. It also swallows the two ways Android
can still refuse **after** the check passes: a `SecurityException` from a permission revoked
between the check and the post, and a channel the user has blocked individually (which
`areNotificationsEnabled` does not report).

`NotificationPermissionRefusedTest` asserts the **positive** half — that the snackbar still
renders with the notification refused — because that is the half that can rot silently. A
refused permission produces no crash and no failing assertion anywhere else; the app just stops
saying one of the two things it was supposed to say and the other looks fine.

---

## Piece 6 — WorkManager, and why not AlarmManager

§2.7 settles the wider question first: `GoogleAuthUtil` mints only short-lived tokens with no
refresh token and `C9d` banned the service account, so *"there is no credential for a background
sync and cannot be one."* Nothing server-pushed is available at any price. Then:

1. **Exactness costs a permission this app cannot honestly claim.** From Android 14 an exact
   alarm needs `SCHEDULE_EXACT_ALARM` (user-revocable, and the system may deny it) or
   `USE_EXACT_ALARM`, which is restricted to alarm-clock and calendar apps and asserts to Play
   that this *is* one. A nightly *plan tomorrow* nudge is not an alarm clock.
2. **Nothing scheduled here needs the minute.** The one thing that would — a deadline reminder
   computed to the minute — has nothing to schedule against yet.
3. **Reboot and process death are free.** WorkManager restores its own queue; an `AlarmManager`
   schedule needs a `RECEIVE_BOOT_COMPLETED` receiver of ours to rebuild.
4. **Doze governs both anyway**, so the precision an exact alarm nominally buys is not reliably
   there.

**A chain of one-time requests rather than `PeriodicWorkRequest`:** a periodic period is measured
from enqueue, so it cannot be pinned to a wall-clock time and would drift off 22:00 across a DST
change. Each run re-arms from the clock it actually woke at.

**§2.5's re-check at fire time**, made concrete: the worker compares the minute it was *enqueued*
for against the setting's *current* value. Mismatch → post nothing, enqueue the correct run. It
is free in exactly the sense §2.5 means — the answer is derived from the live setting, so there
is no second copy of the schedule to go stale.

`ReminderScheduler.nextOccurrence` is **strictly** after `now`, and that is the one line with a
real bug behind it: the worker re-arms from the instant it woke, so the first caller every night
passes a `now` equal to the target. A non-strict comparison returns that same instant,
WorkManager enqueues with zero delay, and the app posts the prompt in a loop for a day. It
cannot happen on a developer's machine and cannot be missed on a user's phone.

---

## What is NOT built, and why — read this before assuming `#8` is finished

The brief's *"What the scheduled half must do — §2.5"* has four bullets. **Two are shipped, two
are not buildable**, and the reason is one fact:

> **There is no occurrence model in this codebase.** `Task` carries no due date. §2.2's four
> rungs — `ALL_DAY` · `DEADLINE` · `BLOCK` · `SPAN` — appear in **no Kotlin file**. §2.1's
> `Occurrence` is unbuilt. Verified 2026-08-20 by grep across `app/src/main` for
> `Occurrence|ScheduleRung|OccurrenceState|ALL_DAY|DEADLINE` (**zero hits**) and by reading
> `domain/model/Task.kt` field by field.

| §2.5 bullet | State |
|---|---|
| One reminder **per occurrence**, timed **per rung** | ⛔ nothing to attach a reminder to |
| Deadline reminder **computed backwards** from `minutesOf(task)`, clamped, says why it moved | ✅ **arithmetic shipped and tested** (`ReminderTiming`); its caller is blocked with the row above |
| A reminder **re-checks at fire time** | ✅ built, on the one reminder that exists |
| Misses meet Ido once, in a **daily review on app open** | ⛔ a *miss* is an occurrence state (§2.3), derived from occurrences that do not exist |

**Building the scheduler anyway would have meant inventing the occurrence model in passing**, on
a ticket nobody reviewed for it — and §2.1 spends six paragraphs on decisions that model has to
get right (*"how many independent whens one piece of work may have, and what remembers the
outcome of each"*). So the arithmetic ships **complete and tested**, and the loop that walks a
list of occurrences calling it belongs to whoever creates the list.

### 🚩 And that ticket does not exist — the same defect `#19` has

§2.1–§2.5 attribute the occurrence model to **`C9a` #25**. **#25 is CLOSED** — it is a *decision*
ticket (*"What does it mean to schedule a task?"*), like #19 before it. The open issues on this
repo as of 2026-08-20 are **#54, #53, #51, #48, #40, #8, #7**, and **none of them carries the
build.** `Observed:` by reading the GitHub issues API directly.

This is the **second instance in one day** of the shape `7-quickadd-complete` recorded for §1.4:
a spec section written in the present tense, deferring implementation to a **closed** `C`-series
decision ticket, with no open issue, no brief and no TODO row that owns the work. The first was
found by auditing one section; this one was found by trying to *use* the model and discovering
it was not there. **The generalisation is that every `C`-series reference in
`docs/PRODUCT_v0.3.md` is a candidate**, because the whole series was closed when the map was
charted, and a reader cannot tell a decision that was *implemented* from one that was merely
*taken*.

**No issue filed and none reopened — outward-facing, Ido's call.**

---

## 🧪 Tests

| Layer | Result |
|---|---|
| JVM unit | **540 / 0** (+25 from 515) |
| Instrumented (`am instrument`, API 35, AVD `Pixel_10_Pro_XL`) | **150 / 0** (+11 from 139) |
| `assembleDebug` | green |
| Cloud Functions (`node --test`) | **n/a** — no `functions/` change |
| Security rules (`firestore-tests/`) | **n/a** — no `firestore.rules` change; nothing here touches Firestore |
| **Observed notification on a device** | ✅ **both fired, seen, and evidenced** — below |

**New suites:**

- `test/.../domain/model/ReminderTimingTest.kt` — **12** cases: the backwards computation, the
  clamp in both directions, §2.5's own 06:00/4 h example, a wrapping night-shift span, an empty
  span (the non-termination guard), degenerate durations, and the `end`-exclusive predicate
  agreeing with `lengthMinutes`.
- `test/.../notifications/NotificationPermissionPolicyTest.kt` — **7** cases over the decision
  table, including the guard that exactly two states permit posting.
- `test/.../notifications/ReminderSchedulerTest.kt` — **6** cases, the important one being *at*
  the target minute resolving to tomorrow rather than to zero milliseconds.
- `androidTest/.../ui/NotificationPermissionRefusedTest.kt` — **5** cases, running the real
  `FilingNotificationEffect` beside the real `SmartAddReceiptSnackbar` in one composition.
- `androidTest/.../ui/NotificationObservedFireTest.kt` — **6** cases: both notifications really
  in the shade, both channels carrying their system-rendered text, and the tap-through route.

### The observed fire — evidence, not "it should work"

Run through `adb shell am instrument`, **not** `connectedDebugAndroidTest`: the Gradle task
**uninstalls the app when it finishes**, which would take the notifications (and the device's
signed-in Google account) with it. Permission granted from outside with `pm grant`, and asserted
inside the test rather than assumed — an ungranted run must fail loudly, because the failure
mode this exists to prevent is a green suite that posted nothing.

```
$ adb shell dumpsys notification --noredact
NotificationRecord(pkg=com.idomarhaim.goalpilot.debug id=8001 tag=null importance=3
  Notification(channel=gp_filing flags=AUTO_CANCEL vis=PRIVATE))
  icon=Icon(typ=RESOURCE pkg=com.idomarhaim.goalpilot.debug id=0x7f07003c)
  contentIntent=PendingIntent{PendingIntentRecord{com.idomarhaim.goalpilot.debug startActivity}}
  android.title=String (No goal fitted)
  android.text=String (Suggested a new goal, "Run better retrospectives", for
                       "Draft the retrospective". Tap to keep it or drop it.)

NotificationRecord(pkg=com.idomarhaim.goalpilot.debug id=8002 tag=null importance=3
  Notification(channel=gp_reminders flags=AUTO_CANCEL vis=PRIVATE))
  contentIntent=PendingIntent{PendingIntentRecord{com.idomarhaim.goalpilot.debug startActivity}}
  android.title=String (Plan tomorrow)
  android.text=String (A few minutes now saves the morning. Tap to look at what is open.)
```

**And looked at, not only dumped.** The shade was expanded and screenshotted, which is the only
instrument that could confirm the thing `dumpsys` cannot: **the small icon renders as the target
glyph rather than a solid white blob.** Android masks a notification's small icon to a flat
colour and reads only its **alpha** channel, so reusing `ic_launcher_foreground` — full-colour,
108 dp — would have produced a white rectangle that looks perfectly correct in every preview,
because nothing masks it there. `drawable/ic_notification.xml` exists for that one reason: 24 dp,
one colour, shape carried entirely by alpha. Confirmed in the shade **and** in the status bar.

**The tap-through was followed, not just asserted.** Launching `MainActivity` with the intent
shape the notification builds landed on **Goal detail** for `observed-fire-goal`, rendering
*"Goal not found — it may have been deleted"*. That empty state is the **correct** result: the id
is the test's fabrication. What it proves is the part that matters — the route was carried,
consumed and navigated, rather than the app merely opening on whatever screen it was last on.

---

## Two defects the runs found that no amount of reading would have

### 1. `--` is illegal inside an XML comment, and it takes out the manifest as well

The first build failed three tasks with
`[Fatal Error] ic_notification.xml:8:23: The string "--" is not permitted within comments` —
plus `processDebugMainManifest`, because `AndroidManifest.xml` had the same thing. Both were
prose comments where an em dash had been typed as a double hyphen. Nothing about the Kotlin,
the resources or the logic was wrong; the file simply was not XML.

Worth recording because the error names the *file* but not the *habit*, and the habit is one
this repo's comment style makes constant: these files carry long explanatory comments, and a
double hyphen is the natural ASCII stand-in for the em dash used everywhere else. Fixed by
replacing `--` with `—` **inside comment bodies only**, then re-checking mechanically rather
than by eye.

### 2. A test that passed alone and failed in the suite — Android's own auto-group

`NotificationObservedFireTest.theFilingNotificationReallyAppearsInTheShade` located its
notification with `single { channel == gp_filing }`. That passed when the class was run in
isolation and failed the full suite with *"Collection contains more than one matching
element."*

**The second element is the system's.** When two of an app's notifications are up at once
Android synthesises its own `AUTOGROUP_SUMMARY` record — and it carries the **channel id of the
first one**. So a channel filter matches two, and only in the run orders that leave both posted.

Fixed by matching on the app's own notification **id** (`GoalPilotNotifier.ID_FILING`, made
`internal` for exactly this), and asserting the channel separately rather than using it as the
key. `Observed:` 2026-08-20 — green in isolation, red in the suite, same code.

The general shape: **an isolated run is a weaker instrument than the suite, and the difference is
state the platform contributes.** A notification test that never sees a second notification never
sees the grouping behaviour that only exists because there is a second one.

---

## The Hebrew half, which the freeze does **not** exempt

`HebrewLocaleResourceTest > every translatable english string has a hebrew counterpart` went red
on the first green build: 10 new keys in `values/strings.xml`, none in `values-iw/`.

**§0.8's suspension does not reach this.** What the freeze permits is *"write plain English
literals in any unswept package"* and *"ship a screen without a Hebrew render pass"* — both about
**Kotlin literals** and **rendering**. This guard is about **resource parity**, it is app-wide
like `DialogLocaleGuardTest`, and it was green before this ticket. Shipping the English half
alone would have turned a parked suite red and left the next session to discover it.

So the counterparts were authored, following this file's own rules: the header's terminology
(*a `Goal` is יעד, never מטרה*) and §4.8's prefix rule (no Hebrew prefix attached to a Latin run).
Marking them `translatable="false"` was rejected — it would have been a lie about strings that
are plainly translatable.

⚠️ **`Untested:` these have not been seen on a Hebrew device.** §0.8's render pass is `#51`'s and
`#51` is parked, so this is **parity, not a verified rendering** — said in the file itself as
well as here. Worth Ido's eye when `#51` resumes; the notification bodies interpolate a
user-authored goal title into an RTL sentence, which is §4.8's exact hazard class.

---

## 🤝 Concurrency — my claim row was published under a sibling's commit message

`0f7dadd` reads *"51-freeze-verify: claim before the first write"* and its body says *"Board row
claimed (**0 rows were active**)"* — but the commit adds **two** rows, and the first is mine. It
was true when they read the board and false by the time they committed: I wrote my row into the
working tree in the seconds between, and a pathspec commit of `SESSIONS.md` takes the
**working-tree** content of that path.

Nothing was lost, and their commit is **not** being amended — an amend of a shared file re-runs
the same hazard over a window that spans turns. The repair the rule prescribes is to **name what
rode along**, which is what this section is.

It is also the one hole the pathspec remedy cannot close, exactly as the rule predicts:
`git commit -F msg -- <path>` protects a sibling's *other* files and never one that is in your
pathspec **and** theirs. `SESSIONS.md` is that file by construction. Both sessions did the right
thing; the board is simply not coverable by the mechanism that covers everything else.

**Singletons:** `51-freeze-verify` holds the **Gradle daemon**, **adb** and AVD
`Pixel_10_Pro_XL`. Liveness read per §5.3(c): their transcript's last turn was **12:22:53Z**,
about one minute before I looked — live, not stale. All code for this ticket was therefore
written first and verification deferred rather than the daemon taken.
