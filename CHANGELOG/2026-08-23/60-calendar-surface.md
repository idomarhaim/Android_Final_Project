# 60-calendar-surface — `#60`, the calendar you can look at

> **Summary:** Spec §4.3's calendar surface — a 3-day/week/agenda zoom on the tab §4.2 freed, the first UI author `BLOCK` and `SPAN` have ever had, the all-day and untimed strips, and a load bar and booked/free ring that are arithmetic rather than inference. 1013 JVM tests green (90 new), 14 instrumented green, and seven defects found — three by writing tests, three by looking at the PNG with every test green, and one reported by a sibling session reading this code.

**Ticket:** [`#60`](https://github.com/idomarhaim/Android_Final_Project/issues/60) ·
**Brief:** `sessions/60-calendar-surface.md` · **Mode:** auto

---

## What shipped

`BLOCK` and `SPAN` have been *"modelled, stored, derived, reminded and reviewed end to end"*
since `#56` with **no way to type one**, and the app had no calendar to look at at all. Both
are now closed.

### 1 · The surface — `feature/calendar/`

| File | What it is |
|---|---|
| `CalendarModel.kt` | The vocabulary: `CalendarZoom`, `CalendarLane`, `TimeColumnForm`, `RungPresentation`, `CalendarEntry`, `CarryForward`. Pure. |
| `DayLoad.kt` | §4.3's load bar and booked/free ring, as minute-interval arithmetic. Pure. |
| `CalendarBuilder.kt` | Assembles the columns from four streams. Pure. |
| `SlotDraft.kt` | The `BLOCK`/`SPAN` author, as data. Pure. |
| `CalendarViewModel.kt` | Wiring. Owns two mutable things — the zoom and the anchor — and caches no derived state. |
| `CalendarScreen.kt` | `CalendarScreen` (Hilt) delegating to a stateless `CalendarSurface`. |
| `SlotSheet.kt` | The sheet a tapped slot or the FAB opens. |

Everything that decides anything is in the first four files, none of which imports Android or
Firebase. That is why §4.3's rules are checked on the JVM and the instrumented layer is about the
surface composing rather than about arithmetic.

### 2 · Navigation — §4.2's tab swap, and it cost one enum row

§4.2: *"Five is a crowded bar, so **Profile moves to an avatar in Home's top-right** … and Calendar
takes the freed tab."* **The avatar half was already shipped** — `DashboardScreen`'s top bar has
opened a *Your profile* / *Settings* sheet since `#48` (`4ed625b`, session `48-settings-surface`)
— so, measured rather than assumed, the swap
removed **no** route and left Profile reachable in one tap from Home. `Routes.PROFILE` stays
registered in the graph and is still reached from that sheet.

`TutorialStepsTest` asserts every tour step's route is a top-level tab; **no step points at
Profile**, so the swap moved nothing there either.

### 3 · The three lanes, which make §4.3's rule structural

A rung maps to exactly one lane, from one `when`:

| Rung | Lane | Time-column form |
|---|---|---|
| `BLOCK` | `GRID` | rail, start over end |
| `DEADLINE` | `ALL_DAY` | a point, `due` + time |
| `SPAN` | `ALL_DAY` | a soft capsule + a day count |
| `ALL_DAY` | `UNTIMED` | the words *all-day* |

So ***"a `DEADLINE` is only ever a banner in the all-day strip, never a timed box"*** is true by
construction rather than by a check — there is no state in which a deadline has been put in the
grid and something has to notice. The rung is carried by the **form** of the leading column and the
chip carries **only** the life area, per §0.8's surviving sub-rule.

### 4 · The load bar and the ring — three decisions, all derived

1. **Only `BLOCK` books time.** §2.4 (the other three *"occupy no slot"*) + §2.2 (*"spans
   contribute nothing"*).
2. **Booked minutes are a set union, not a sum.** Two blocks at the same hour are two commitments
   and *one* booked hour; summing gives a day reading 26 hours booked out of 16.
3. **The numerator is clipped to the waking window, because the denominator already is.** §4.3
   reddens *"past 75% of waking hours"*, so a 03:00 block counted into a 16 h denominator would be
   §0.3's second number that quietly disagrees.

The 75 % threshold is `WakingHours.loadBarRedMinutes`, **not** re-derived — §4.9's own KDoc says
why, and it was already wired to `Settings → Your day`.

### 5 · `#61`'s slot is built and empty, and that is the stated boundary

`EntryKind.EXTERNAL` has a lane, a grey fill, an ordering and tests. What it has no *source* for is
hand-made Google events, because [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61)
has not shipped. `CalendarViewModel` passes `external = emptyList()` rather than omitting the
parameter, so the day `#61` lands the change is one flow.

Likewise `AWAY` is a **parameter** on `CalendarEntry`, not a derivation: §2.7 says a disappearance
*"keeps its date, clears its `googleEventId`"*, so from stored data alone it is indistinguishable
from an occurrence that never had one — wiring it to `googleEventId == null` would mark **every**
occurrence in the database `AWAY` the day `#61` ships. `CarryForward` is tested through both
branches regardless.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`app/src/test/`) | ✅ **1013 tests, 0 failures** — **90 new** across 5 classes |
| **Instrumented** (`app/src/androidTest/`) | ✅ **14 tests, 0 failures**, `emulator-5554` |
| **Render pass** | ✅ 4 PNGs, looked at — and 4 defects found, below |
| Firestore rules (`firestore-tests/`) | **Not run — nothing to run.** This ticket adds no collection and no rule; it *reads* `users/{uid}/occurrences`, which `#63` shipped and tested. |
| Endpoints / Cloud Functions | **Not run — none touched.** §0.1's free-model rule is the point of the load bar: no model call, no network. |

New JVM classes: `DayLoadTest` (23) · `SlotDraftTest` (21) · `CalendarBuilderTest` (20) ·
`CarryForwardTest` (14) · `CalendarLaneTest` (11) — **90** (`CalendarBuilderTest` gained one
after the fix below). *(Counted from the JUnit XML rather
than from memory; the first draft of this line had all five numbers wrong and the total right.)*

**Commands, verbatim:**

```bash
./gradlew :app:testDebugUnitTest --rerun          # 1013 tests completed, 0 failed
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w \
  -e class com.idomarhaim.goalpilot.ui.CalendarSurfaceUiTest \
  com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
# OK (14 tests)
```

📱 **The Google sign-in survived**, checked before and after: `install -r` + `am instrument`, never
`connectedDebugAndroidTest`. `FIREBASE_USER` still holds `name.iddo@gmail.com`.

---

## What writing the tests and looking at the output actually found

Eight defects, and **not one of them was found by reading the code I had just written**.

### Found by writing a test

1. **A tap in the last hour of the day produced a zero-length block.** `LocalTime.plusMinutes`
   *wraps*: `23:45` + 30 min is `00:15`, which as a time on the *same date* is fourteen hours before
   the start, and `Block.closesAt` coerces the lot to zero. Fixed with `SlotDraft.crossesMidnight`
   and modular `minutes`. The draft compiled and read perfectly.
2. **A three-day span drew on one column.** `CalendarEntry.covers(date)` replaces keying a column on
   the start date — a challenge running all month was invisible on every column but the first, which
   is the opposite of what a *window* is for.
3. **Carry-forward found nothing to carry.** The sweep only saw days the calendar was *drawing*, so
   an overdue deadline older than the first visible column — the ordinary case, since that column is
   usually today — was never built. `carryBackDays` (90, named as a bound rather than a truth) widens
   the load window without widening the draw.

### Found by looking at the PNG — and every test passed

4. ⚠️ **The hour grids did not line up across columns.** Each day was its own `Column`, so a day
   carrying four banners started its grid four rows lower than a day carrying one: **Monday's 07:00
   sat below Tuesday's 09:00.** Every assertion passed, because every node was present and displayed.
   An hour grid whose rows do not align is not a grid — it is three unrelated lists with hour lines
   drawn on them, and comparing two days is the one thing a multi-day calendar is for. Fixed with a
   `Band` layout at `IntrinsicSize.Max`.
5. **The span's date range ate its own title.** An unbounded leading column is a leading column that
   wins, and the thing it won against was the only text identifying the row — the span rendered as a
   date and a tick with no name. Fixed with a fixed-width time column; the capsule now shows a day
   count, which is what fits.
6. **The week zoom reproduced, exactly, the failure §4.3 measured.** Times clipped to `07:0`,
   `09:3`, `12:3`; titles reduced to an ellipsis. §4.3 had already said why — *"~46 dp per day … week
   view stacks the times **start over end**, the only thing that fits at 46 dp"* — and that is not an
   instruction to stack times in a narrow column *beside a title*; it is *the stacked time is what the
   column is for*. `StackedChip` gives the week its own layout.

Also fixed on sight: a filled tick button reading as the heaviest thing on every row, and `1 days`.

### Found by a sibling session reading this code

8. **A goal that counts *nothing* would have lost its deadline banner.**
   `66-unmeasured-percent` was sweeping §1.3 (*absence is the default*), found
   `CalendarBuilder`'s `filterNot { it.isArchived || it.isComplete }`, and **reported it on
   `SESSIONS.md` rather than editing another row's file**. `Goal.isComplete` is
   `progressFraction >= 1f`, and for an unmeasured goal that fraction is measured against a
   `targetValue` **nobody set** — so logged entries summing past the default would silently delete
   the banner, on the one surface whose whole job is to say when things are due. Now
   `!it.isUnmeasured && it.isComplete`, with a test. It is `#66`'s **seventh site**, written the
   same day the other six were being removed — which is the more useful half of the finding: a
   defect class is at its most reproducible while it is being fixed elsewhere.
   *(The existing `an archived or completed goal's deadline is not drawn` fixture was also given a
   real `Measure`. Without one it had been passing for the reason this case forbids rather than
   because the goal was finished.)*

### Found by a sweep somebody else wrote

7. **`ImeSettleSweepTest` caught both of this file's `performTextInput` call sites** (`#58`). They
   *passed* — which is precisely the failure mode the sweep exists for: focusing a field raises the
   keyboard, whose inset animation slides the sheet while Compose reports itself idle, so the next
   click lands where Save used to be and is silently lost, **1 full-suite run in 4**. Now
   `performTextInputAndSettle`.

---

## Two things about the shared tree

⚠️ **`BUILD SUCCESSFUL` and a green test run lied once, together.** `assembleDebug` failed with the
Windows KSP lock (`Could not delete …/generated/ksp/debug/resources`), the previous APK was still at
the output path, `install -r` succeeded, and the suite reported **14/14 green for the build
before**. Reading `${PIPESTATUS[0]}` is not enough on its own — the exit code was read, and the
temptation was to trust the green that followed it. The lock is documented in `CLAUDE.md` and
cleared on a re-run both times.

📣 **Nine JVM failures seen mid-session belonged to `66-unmeasured-percent`**, not to this work —
`DerivedProgressTest`, `BuildSummaryUseCaseTest`, `RecommendationRepositoryFallbackTest` and
`HebrewTerminologyTest`, every one of them over a file that session had uncommitted and whose own
comments name `#66`. They were gone by the final run. Recorded because a red suite in a shared tree
is the thing most likely to be misattributed by whoever runs next.

---

## Out of scope, and stated

- **Google Calendar in either direction** — `#61`. The grey layer is built and empty; this surface
  is complete and useful with no Google account at all.
- **Drag to move.** §4.3 lists it beside create and tick. Create and tick shipped; drag did not.
  `ScheduleEdits` already answers *"this occurrence, or all future ones?"* and
  `OccurrenceRepository.apply` commits the plan, so the domain half is done and what is missing is
  the gesture plus the sheet that asks the scope question.

  ⚠️ **This was first reported as *"`#60` stays open for it"*, and that was wrong.** Ido asked why
  `#60` was still open and the answer did not survive re-reading the **ticket**: `#60` separates
  *"What was already decided — do not reopen it"* (design context, where the drag sentence lives)
  from ***"What this ticket owes"***, four numbered deliverables that do not include it. All four
  shipped; `#60` is **closed**. Drag is specced in §4.3 and owed by **no open issue**, so it wants
  its own ticket — flagged to Ido, whose call it is to file.

  **The mechanism, because no rule caught it.** `/kickoff` §5 step 4's last check is *"a read of
  your own `Exit` against what you actually built"* — but the **brief** is not the authority on
  whether a **ticket** is done. This brief paraphrased §4.3's design paragraph into a *"do not
  reopen the design"* section, drag included; measured against that, the work read as incomplete.
  Read the issue body, not the brief, before deciding a ticket's state.
- **Points per occurrence** — `#64`'s. A tick records that the window was honoured (§4.7's count);
  it banks no points, per `OccurrenceOutcome.Done`.

## The one part of this that is not covered by a test

`CalendarViewModel.setDone` picks between two write paths — `setOutcome` for an instance that
already has a document, and `apply(SchedulePlan.Writes(...))` for a **generated** one, which is what
brings its document into existence carrying the `seriesDate` that keeps the series recognisable.
That branch is `§2.1` exactly and it is **`Untested:`** — exercising it needs a live Firestore, and
this session added no fake for `OccurrenceRepository`. The pure half it depends on
(`ScheduleEdits`, `TaskSchedule`) is `#63`'s and is tested there. Said plainly rather than left to
be discovered: it is the one place in this ticket where a claim rests on reading rather than on a
run.

## What this unblocks

[`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62) (tour video v2) declares
`blocked_on: [60, 61]`. This closes the first of the two; `#61` is live in a sibling session.
