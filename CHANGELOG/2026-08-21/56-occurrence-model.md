# `56-occurrence-model` — 2026-08-21

> **Summary:** `#56` ships — §2.2's four rungs exist, and §2.5's reminders finally have something to read. `Occurrence` is a **sealed** type per rung (`AllDay` · `Deadline` · `Block` · `Span`), so each carries exactly the fields its miss semantics need and an `ALL_DAY` with an end time is unrepresentable rather than normalised; `BlockPlacement` hangs off `Block` alone, which is what makes §2.3's `EXPIRED` impossible for the three rungs that occupy no slot. Nothing temporal is stored — `stateAt(now)` is the only derivation. Every rung's reminder goes through **`#8`'s** `ReminderTiming.plan`, three of them as the deadline's arithmetic with a **zero lead-in**, so the waking clamp cannot be skipped for any of them; the test asserts the deadline plan is *identical to* what `#8` returns, so a second implementation fails on its first divergence. The fire-time re-check is a pure function (`Fire` / `Rearm` / `Skip`), and the daily miss review is a card that posts **nothing** — asserted against a live control notification, so the negative is not vacuous. JVM **706/0** (+60), instrumented **190/0** (+13), and the differentiator was **seen** in the shade: *“Due at 6:00 AM and it takes about 4h — worth starting tonight.”* **One declared deviation:** the occurrence lives on the task, not in §7.1's `…/occurrences` subcollection — that collection exists for recurrence, `googleEventId` and per-occurrence outcomes, none of which `#56` builds, and the migration is additive. **One spec gap found by enumeration:** §2.3's vocabulary names two of §2.2's four miss meanings, so `DAY_PASSED` and `WINDOW_CLOSED` were named rather than folded into `MISSED`, which §2.3 marks a **failure** and neither of them is. **Two defects found by looking, not by tests:** the when-picker's clock was one glyph for two opposite actions, and the one still-open row in the review looked like history. **And a race that predates this ticket:** `NotificationManagerCompat.notify` returns before `activeNotifications` reflects it, which failed a different case on each of three full-suite runs — including `#8`'s own, unchanged since it shipped.

`#56` — §2.2's occurrence model, and the §2.5 reminders it unlocks.
Mode **AUTO**. Brief: `sessions/56-occurrence-model.md`.

---

## 1 · What shipped

**§2.2's four rungs exist, and §2.5's reminders now have something to read.** `#8` shipped the
reminder arithmetic with nothing to feed it; this ticket built the thing that feeds it.

### 1.1 The occurrence model (§2.2, §2.3)

`domain/model/Occurrence.kt` — a **sealed** hierarchy, one type per rung:

| Rung | Carries | A miss means | Derived state |
|---|---|---|---|
| `AllDay` | a `LocalDate` | the day passed | `DAY_PASSED` |
| `Deadline` | a `LocalDateTime` | late, still owed | `OVERDUE` |
| `Block` | start + end + placement | the slot is gone | `MISSED`, or `EXPIRED` when unconfirmed |
| `Span` | a date range | the window closed | `WINDOW_CLOSED` |

- **Sealed, not a rung plus two nullable dates.** The flat shape admits an `ALL_DAY` with an end
  time and a `BLOCK` with no end — states a normaliser then has to invent an answer for. Each rung
  carries exactly the fields its miss semantics need, so the normaliser and its tests do not exist.
  Same move `#55` made twice (`done` beside a stamp; a point value beside its inputs).
- **`BlockPlacement` hangs off `Block` alone** (§2.4: the other three rungs occupy no slot and
  cannot collide), which makes §2.3's `EXPIRED` unrepresentable for them rather than merely unused.
- **Nothing temporal is stored** (§2.3). `Occurrence.stateAt(now)` is the only derivation; there is
  no `isOverdue`, no `status`, no wire field, and nothing a sweep would have to keep true.

### 1.2 The reminders (§2.5)

`domain/usecase/OccurrenceReminders.kt` + `notifications/OccurrenceReminderWorker.kt`.

- **One arithmetic, not two.** Every rung goes through `#8`'s `ReminderTiming.plan`. Three of the
  four are the deadline's computation with a **zero lead-in**, so the waking clamp cannot be
  skipped for any of them. `OccurrenceRemindersTest` asserts the deadline plan is *identical to*
  what `ReminderTiming.plan` returns, so a second implementation fails on its first divergence —
  the brief's named defect, asserted rather than avoided.
- **Timed per rung:** deadline ← computed backwards from `minutesOf(task)`; block ← the slot's
  start; all-day and span ← that day's **first waking minute** (their window opens at midnight, and
  clamping *backwards* from midnight would remind the previous evening about a day not yet begun).
- **The re-check is a pure function.** `decideAtFireTime(task, scheduledFor, schedule)` returns
  `Fire` / `Rearm` / `Skip(reason)`. The worker reads three values and obeys. That is why *"does
  not fire for a task already done"* is a JVM test rather than a device and a wall-clock wait.
- **Nothing already past is ever armed** — that would be the *"push saying he failed"* §2.5 forbids.

### 1.3 The daily miss review (§2.5)

`domain/usecase/DailyMissReview.kt` + a card at the top of the dashboard.

- *"once"* is a boundary, not a flag: misses whose window closed **before the last review** are
  left out — **except `OVERDUE`**, which §2.3 makes the one state that keeps reminding.
- The only stored thing this feature has is `missReviewLastShownAt`, because *whether he has
  already seen it* is not derivable from any task.
- It posts nothing. `DailyMissReviewUiTest` asserts the shade is empty of the reviewed tasks
  **while a control notification is demonstrably in it**, so the negative is not vacuous.

### 1.4 A way to type one

`feature/goals/WhenPicker.kt` on the add-task row: a day → `ALL_DAY`, a day and a time →
`DEADLINE`. `BLOCK` and `SPAN` are modelled, stored, derived, reminded and reviewed end to end and
have **no author** — §2.4 gives blocks to §3.7's batch sheet (`#24`) and spans to §4.3's calendar
surface (`#26`), and `#56`'s brief puts a full scheduling surface out of scope.

---

## 2 · Decisions taken, and one deviation

### 2.1 ⚠️ DEVIATION — the occurrence lives on the task, not in `…/occurrences`

§7.1 specifies a subcollection, one document per *when*. **Four fields on the task document
shipped instead.** Declared in `docs/PRODUCT_v0.3.md` §7.1 with the reasoning, and repeated here.

The subcollection exists for the three things `#56` does not build: **many** occurrences per task
(§2.1's rule), a **`googleEventId`** (§2.6, unbuilt), and a **per-occurrence outcome** (§2.8,
unbuilt). With one occurrence, no rule and no Google it buys a second collection, a second snapshot
listener joined into `TaskStream`, and a `firestore.rules` change for a shape nothing reads. The
brief directs the task-field form in as many words. Migration when recurrence lands is additive.

**The cost, stated:** until then, no moved instance, no skip, no recurring task, and §7.1's
three-way `isDone` split stays two-way.

### 2.2 §2.3's state vocabulary names two of §2.2's four miss meanings

Found by writing the four cases out. `MISSED` is *a block whose slot has gone* and is **a failure**;
`OVERDUE` is the deadline's. Neither fits *the day passed* or *the window closed*, and folding them
into `MISSED` would mark them failures the spec never called failures. Named `DAY_PASSED` and
`WINDOW_CLOSED`, resolving toward §2.2 because §2.2 is the section that says what a miss *means*.
Recorded in the spec.

### 2.3 The wire form is ISO-8601 **local text**, not epoch millis

A `LocalDate` is not an instant. Stored as millis, *which day an all-day task is on* would depend
on the reading device's zone — and §2.3 derives every temporal state from exactly that value, so
the miss would move with the reader. `createdAt` and `completedAt` stay millis: they record **when
something happened**, which genuinely is an instant.

### 2.4 Hebrew parity maintained, `#51` untouched

Five new notification strings in `values/` **and** `values-iw/`. `HebrewLocaleResourceTest` enforces
parity on every translatable key, so adding an English-only string fails the build. That is resource
parity, not `#51` work; no formatter, no locale switch and no frozen surface was touched.

### 2.5 `#8`'s substrate was complete — no defect to report against it

The brief asks for a missing substrate piece to be *"said out loud"* as a defect in `#8`. There was
none. The channel, `POST_NOTIFICATIONS`, the tap-through, the in-app half, refusal handling, local
scheduling and `ReminderTiming` were all present and sufficient; this ticket **added** a notifier
method and scheduler functions, which is extension rather than a gap being filled. `#8`'s close
comment stands as written.

### 2.6 `AnalyticsRange` was checked before reusing a name

The brief warns it carries `ALL_DAY`/`BLOCK`. It carries `DAY_BLOCKS` / `DAY_BLOCK_HOURS` — chart
bucketing — and **no `ALL_DAY` at all**. Different concept, not unified, no collision.

---

## 3 · 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **706 tests, 0 failures, 0 errors, 0 skipped** (66 classes) |
| **Instrumented (device)** | **190 tests, 0 failures** — full suite, `emulator-5554` |
| Server unit / endpoints / database | untouched by this ticket; `functions/` and `firestore.rules` have no occurrence code |
| **Seen** | the reminder read out of the shade and looked at — §4 |

### New JVM classes

- `domain/model/OccurrenceTest` — **each rung's miss semantics**, plus the properties as a whole
  (*exactly one state counts as a failure; exactly one keeps reminding*), boundary cases at
  midnight, malformed input, and `OccurrenceDraft`'s rung rule.
- `domain/OccurrenceRemindersTest` — the backwards computation, §2.5's own 06:00 example, the
  waking clamp (including a night-shift wrap), identity with `ReminderTiming.plan`, `#9`'s typed
  duration driving the reminder, and all four fire-time re-check outcomes.
- `domain/DailyMissReviewTest` — the four meanings reaching the review, `EXPIRED` never reaching it,
  *once*, `OVERDUE`'s exemption from *once*, ordering, and `isDue`'s calendar-day boundary.
- `data/TaskOccurrenceMappingTest` — round trip per rung, the stored shape, absence on pre-`#56`
  documents, and five malformed-document cases.

### New instrumented classes

- `ui/WhenPickerUiTest` — the control's transitions, including that the second press **demotes the
  rung without losing the day**.
- `ui/DailyMissReviewUiTest` — the card on screen with each rung's own sentence, and the shade
  empty of it **against a live control notification**.
- `ui/OccurrenceRenderTest` — §0.8's render pass, two PNGs.
- `ui/NotificationObservedFireTest` — extended with four reminder cases: the moved copy, the
  unmoved copy, two tasks staying up together, and cancelling one leaving the other.

### ⚠️ Two errors hit while working, both fixed

1. **A vacuous assertion I wrote, caught by running it.** The `#9` typed-duration test first used
   §2.5's own 06:00 example — but `06:00 − 30 min` is `05:30`, which is *also* asleep, so both the
   typed 240 and the estimated 30 clamp to the same `22:59` and the test passes whichever number
   wins. Moved to a 09:00 deadline, where the two land on **different sides of sleep** (previous
   evening vs `08:30`). The vacuity is now written into the test as a warning.
2. **A notification-propagation race, including in tests that predate this ticket.**
   `NotificationManagerCompat.notify` returns before `activeNotifications` reflects it, so reading
   the shade on the next line is a race. `Observed:` the full suite failed on a **different** case
   each run — `twoRemindersForTwoTasksBothStayUp`, then `cancellingOneTasksReminderLeavesTheOthersUp`
   (cancel is async too), then `theFilingNotificationReallyAppearsInTheShade`, which is **`#8`'s,
   unchanged since it shipped**. Each passed alone (`3/3` verified). Fixed with bounded waits —
   `awaitPosted` / `awaitReminderTags` / `awaitReminderTagsGone` — which still fail loudly, just
   later. `#56` did not cause this; it posts four more notifications and made it likelier.
   **This is a distinct cause from [`#58`](https://github.com/idomarhaim/Android_Final_Project/issues/58)'s
   IME hypothesis and `#58` is not claimed fixed.** `Observed:` the **two** full-suite runs after
   the last of these fixes were green (188/188, then 190/190) where the three before it each failed
   one case. Two runs is not a lot of evidence about a race — stated as what it is.
   `Untested:` whether `#58`'s IME hypothesis has any remaining instances; nothing here looked.

---

## 4 · 👁️ Seen — the differentiator, in the shade

`adb shell dumpsys notification --noredact`, after `theDeadlineReminderReallyAppearsAndSaysWhyItMoved`:

```
android.title = Finish the quarterly report
android.text  = Due at 6:00 AM and it takes about 4h — worth starting tonight.
```

Screenshot of the expanded shade confirms it renders at the top, two lines, em dash intact. That is
§2.5's *"the one thing this app knows that Google Calendar does not"*, on a device.

### Two defects found by LOOKING at the render pass, not by any test

Both were green under every assertion, and both are §0.8's *one chip may not carry two axes*:

1. **The `WhenPicker` clock was the same glyph in two opposite states** — *add a time* and *remove
   it* — distinguished only by a content description nobody sees. Now outlined-and-muted when there
   is no time, filled-and-primary when there is.
2. **The one still-open row in the review looked like history.** All four rows rendered identically,
   so §2.3's `OVERDUE`/`MISSED` split — the thing it says *"earns its keep twice"* — was invisible on
   the only surface a person reads, and `MissedOccurrence.stillOwed` existed with no reader. Now
   `primary`, **never an error colour**: it marks something still doable, and red there would be the
   *"push saying he failed"* arriving as a swatch.

Artefacts (session scratchpad, not committed): `issue-56-when-control.png`,
`issue-56-daily-miss-review.png`, `shade.png`.

---

## 5 · 🔧 One pre-existing defect fixed in passing

`docs/PRODUCT_v0.3.md` carried a **broken anchor** to §0.5 (`#05--the-ai-judges-…`, double dash
where GitHub's slug has one). Found by recomputing all 55 in-document anchors against all 78
headings rather than by reading them — the *"recompute what a consumer computes"* check. All 55
resolve now. One character, in a file this brief owns.

---

## 6 · 📱 Device

`emulator-5554` (`Pixel_10_Pro_XL`, `sdk_gphone64_x86_64`). Took the **`install -r` + `am instrument`**
path throughout, never `connectedDebugAndroidTest`.

**The Google sign-in is INTACT** — `FIREBASE_USER` present in the app's Firebase auth store and
`name.iddo@gmail.com` still on the device, verified before and after the full run.

**Blast radius, declared before touching it and honoured:** notifications posted and cancelled under
this app's own ids and tags; two PNGs written to the app's external files dir; APKs reinstalled in
place. **No live Firestore data was created, edited or deleted** — every test builds its `Task`
objects in memory, and nothing in this session tapped through the running app (no `adb shell input`
was issued at all).

⚠️ **One thing the app itself did, worth naming because it is new in this ticket.** `GoalPilotApp`
now observes the signed-in account's tasks to arm reminders, so the instrumented runs **read** Ido's
real task list. It is a read: no occurrence exists on any of those documents (the fields ship
empty), so `planFor` returned `null` for every one and the sync issued only `cancelUniqueWork` for
work that was never enqueued. Nothing was written, and no reminder was armed against live data.

---

## 7 · Files

**New (main):** `domain/model/Occurrence.kt` · `domain/usecase/OccurrenceReminders.kt` ·
`domain/usecase/DailyMissReview.kt` · `notifications/OccurrenceReminderWorker.kt` ·
`feature/goals/WhenPicker.kt`

**Modified (main):** `domain/model/Task.kt` · `data/firestore/dto/Dtos.kt` ·
`data/firestore/dto/Mappers.kt` · `notifications/GoalPilotNotifier.kt` ·
`notifications/ReminderScheduler.kt` · `notifications/NotificationEntryPoint.kt` ·
`GoalPilotApp.kt` · `domain/repository/AppPreferencesRepository.kt` ·
`data/prefs/AppPreferencesRepositoryImpl.kt` · `feature/goals/GoalDetailScreen.kt` ·
`feature/goals/GoalDetailViewModel.kt` · `feature/dashboard/DashboardScreen.kt` ·
`feature/dashboard/DashboardViewModel.kt` · `res/values/strings.xml` · `res/values-iw/strings.xml`

**Tests:** 4 new JVM classes, 3 new instrumented classes, `NotificationObservedFireTest` extended;
`HealthSyncTest`'s fake and four `AddTaskRow` callbacks updated for the widened interfaces.

**Docs:** `docs/PRODUCT_v0.3.md` (§2.2, §2.5, §7.1 + the deviation note, one anchor fix).
