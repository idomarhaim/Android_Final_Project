# 68-drag-to-move — §4.3's third verb, and the door onto `#63`'s machinery

> **Summary:** [`#68`](https://github.com/idomarhaim/Android_Final_Project/issues/68) — §4.3's **drag to move** ships on the calendar's hour grid, and with it the **scope sheet** that finally makes §2.1's *"this occurrence, or all future ones?"* askable: `ScheduleEdits`, `EditScope` and `ScheduleEdit` had **zero call sites** in the whole app before this commit. **Skip** ships as the second door onto the same machinery, and `SchedulePlan.TooLarge` is surfaced with **both its numbers** rather than swallowed. One long press serves two verbs — carried, it moves the row; released where it started, it opens the entry menu — because §4.3's own column measurement leaves no room for a third control. All the arithmetic is a pure function of a measured geometry and is tested with no device. **1068 JVM unit tests, 0 failures** and **296 instrumented tests, 0 failures** on `emulator-5554` (**14** of them this ticket's), with **four render-pass PNGs pulled and looked at**. The pre-commit self-review found a third defect in this session's own code — see below — and it has the only test that can reach it.

**Date:** 2026-08-23 · **Session:** `68-drag-to-move` · **Mode:** AUTO · **Issue:** [#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)

## The brief's two claims, re-measured — and one of them was half wrong

The brief asked for both to be checked before starting, *"because a brief is not the authority on
the state of the code"*. Both were, on 2026-08-23:

1. ✅ **No drag gesture exists.** Holds exactly. The three grep hits under `feature/calendar/` are
   KDoc prose (`CalendarModel.kt:157`, `:275`, `SlotDraft.kt:117`) and not one of them is a
   gesture.
2. ⚠️ **HALF-STALE, and the load-bearing half holds.** `ScheduleEdits`, `EditScope` and
   `ScheduleEdit` really do have **zero** call sites under `feature/` and `ui/` — `#63`'s answer
   was unreachable, which is why this ticket exists. But `OccurrenceRepository.apply` is **not**
   uncalled, and the brief named it alongside the three that were:
   - `CalendarViewModel.kt:226` calls it from `setDone()`, handing it a hand-built
     `SchedulePlan.Writes` to bring a generated instance's document into existence on first tick.
   - `SyncCalendarUseCase.kt:424` calls it from `commit()`, on `#61`'s pull path.

   Neither goes through `ScheduleEdits`, so neither weakens the ticket. Recorded because the
   second one turned out to matter — see the defect below.

## What shipped

| §4.3 / the ticket asked for | Where it lives |
|---|---|
| **Drag to move**, on the calendar | `DraggableEntry` + `DragToMove` (`CalendarScreen.kt`, `CalendarModel.kt`) |
| **The scope sheet**, only where a rule exists | `ScopeSheet.kt`, gated by `MoveScope.isAsked` |
| **Skip**, the second entry point | `EntryActionSheet` (`ScopeSheet.kt`) → `CalendarViewModel.skip` |
| **`TooLarge` surfaced, never swallowed** | `CalendarNotice.TooLarge` → a snackbar naming both numbers |
| The stale comment at `CalendarModel.kt:157` | Corrected in place, with why it was wrong |

## The decision the ticket did not name, and it is the one that would have shipped wrong

**Which `EditScope` a *non-repeating* task's move gets.** The sheet must not appear for one — §2.1's
question exists only where a rule does — so the scope is derived rather than chosen, and the
obvious answer is wrong.

`THIS_OCCURRENCE` and `THIS_AND_FUTURE` name the same single window on a one-off, so either
*should* do. They do different things, and the difference is **which document the write lands in**:

- `THIS_OCCURRENCE` writes an **occurrence document** carrying the new *when*.
- `THIS_AND_FUTURE` writes **`Task.occurrence`** — the anchor — through `ScheduleEdits.moveSeries`'
  no-rule branch.

For a one-off the anchor **is** the *when*. `CalendarViewModel.create` puts a new task's occurrence
there rather than in a document precisely because *"a document for a one-off with no series would
be the 26-duplicate-documents-a-year shape §2.1 rejects, arriving one at a time"*, and every other
surface that shows a task's *when* reads that field. So `THIS_OCCURRENCE` would manufacture the
document §2.1 refuses **and** leave the task's own anchor pointing at the old time — §0.3's *second
number that quietly disagrees*, on the field the rest of the app reads.

So `MoveScope.whenNotAsked` is **`THIS_AND_FUTURE`**, and `DragToMoveTest` asserts **both halves**:
that the chosen scope writes the anchor and creates nothing, and that the rejected one really does
produce the wrong write. A test that stopped at *"we passed `THIS_AND_FUTURE`"* would pass just as
happily if that were the wrong constant, which is the whole failure mode.

**Decision taken per `rules/derivable-decision.md`** — it turns on the artifact, not on Ido — and
recorded here as **mine**. One message reverses it.

## 🐞 A defect in `#63`'s machinery, found while wiring it up

`ScheduleEdits.apply` identifies the instance an edit is about with
`stored.firstOrNull { it.seriesDate == seriesDate }`, and its `seriesDate` parameter is a
**non-null** `LocalDate`. A one-off's document carries `seriesDate = null` by construction
(`ScheduledOccurrence`: *"`null` means this document is not part of a series at all"*), so **that
lookup can never find it.** Both scopes then go wrong, in opposite directions and both silently:

- `THIS_OCCURRENCE` falls through to `TaskSchedule.instanceOn`, whose result has a blank id, so the
  upsert **creates a second one-off document** and the calendar draws the row twice.
- `THIS_AND_FUTURE` writes the anchor and touches no document — but a one-off *with* a document is
  drawn **from that document**, so the move appears to do nothing at all.

`Observed:` read out of `ScheduleEdits.apply`, `TaskSchedule.occurrencesIn` and
`CalendarViewModel.seriesDateOf`, 2026-08-23. `Untested:` on a device — `CalendarEntry.isEditable`
is what stops the app reaching it.

**Reachable two ways today**, and only one survives the guard:

1. **Ticking a one-off** (`CalendarViewModel.setDone` writes `seriesDate = null`). Already excluded,
   because a settled window is history (§2.3, §2.8) and is not editable on its own account.
2. **`#61` pushing a one-off to Google** (`SyncCalendarUseCase.kt:403` creates the document). This
   is what the third condition on `isEditable` actually buys.

**Not fixed here, and deliberately.** The fix is widening that parameter to `LocalDate?`, which is a
change to `ScheduleEdits`' semantics and is out of `#68`'s scope **by name**. The guard is a
property with the whole argument in its KDoc, and `DragToMoveTest` pins it with a test whose comment
says it is what allows the guard to come off. **This is worth a ticket** — it is not urgent, because
nothing in the shipped app can reach it, and it is a live trap for the next person to touch either
file.

## A second §0.4 hole, found by a failing test rather than by reading

The first build put the whole notice in one `LaunchedEffect(notice) { showSnackbar(text);
onNoticeShown() }`. `showSnackbar` **suspends for the entire time the bar is up**, so the notice sat
on the state for four seconds after being shown — and a second refusal inside that window produces
an **equal** `CalendarNotice`, whose key does not change, so `LaunchedEffect` never restarts and
**the second refusal is silent**. That is precisely what §0.4 forbids.

It surfaced as `aRefusedMoveSaysSoWithBothNumbers` failing on `noticesShown` — the assertion was
written about consumption and caught a product defect. Fixed by splitting it in two: the notice is
taken off the state immediately and the bar is shown from a local copy, so the state round-trips
through `null` and an identical refusal is a new one.

**A third silence, closed while re-reading the same function.** `CalendarViewModel.edit` bailed with
`onDone(false)` and no notice when the task was not in the stream. That is **not** an unreachable
guard: the task can leave the stream while a sheet sits open over its row, and then answering the
scope question would do nothing at all — indistinguishable, from the person's side, from the app
ignoring the answer. It now emits `EditFailed`, and the *genuinely* unreachable guard beside it
(`entry.taskId == null`, which `isEditable` already excludes for every non-task kind) is commented as
such rather than given a message it can never show. `Untested:` no test reaches either branch, and
that is said here rather than papered over.

## 🐞 A third defect, in this session's own code, found by the pre-commit self-review

`rules/pre-commit-self-review.md` asks *which of my own arguments does my output contradict*. This
one did, and the answer was two paragraphs apart in the same function.

`DraggableEntry` pins `onHold` and `onDrop` with `rememberUpdatedState`, with a comment saying that
**anything** captured inside a `pointerInput` block goes stale because the block restarts only when
its keys change. It then captured **`entry` itself** and did not pin it.

That is not harmless. The block is keyed on `entry.key`, and a **stored** instance's key is its
*document id* — which does **not** change when the instance moves (`CalendarBuilder` falls back to a
synthesised key only for a generated instance with no document). So after one successful drag the
same key carries a new occurrence at a new time, the block does not restart, and a second drag would
compute its landing from the **old** `opensAt`: the row would jump back to roughly where it started,
which reads as the calendar fighting the finger.

Fixed by pinning `entry` the same way. `geometry` needs no such treatment and the comment now says
why — it *is* a key, so a change to it restarts the block outright, which is what a zoom or a scroll
should do to a drag in flight.

**It has its own test**, because nothing else in the suite could reach it:
`draggingTheSameStoredInstanceTwiceReadsItsNewTimeTheSecondTime` needs three things at once — a
stored instance so the key is stable, a state that actually updates after the move, and two drags.
It asserts the second landing is 11:00 and not 09:00; with the bug in place it is 09:00 both times.

## The gesture, and why one long press has to carry two verbs

The chip has **no room for a third control**. §4.3's own measurement spends a three-day column
(~110 dp) on the time column, the title and the tick, and `TimeColumn`'s KDoc already records what
happened the one time something on that row was allowed to grow. So:

- **`detectDragGesturesAfterLongPress`**, not a plain drag — the grid lives inside a
  `verticalScroll`, and a gesture starting on touch-down would compete with the scroll on every
  pixel.
- **A long press that goes nowhere opens the entry menu**; one that travels moves the row.
  `DragToMove.isMove` is the whole of that decision and is tested on the JVM. **Both** terminal
  callbacks route through it, not just `onDragEnd`: a release with no travel is delivered as a
  **cancel**, so reading one of the two would drop half the presses.
- **Rows outside the grid have no drag** — a banner, the untimed strip and the agenda list are
  ordered lists, and a row's position in them carries no *when*, so a drag there could only mean
  *reorder*, which this app does not have. They still reach `Skip` through the same menu, and
  `aBannerHasNoDragButStillReachesSkip` pins that asymmetry end to end.

**The geometry is a pitch, not a width.** `DragToMove.Geometry.columnPitchPx` is measured on the Row
that lays the grids out and divided by the column count. Each column carries 2 dp of padding on both
sides, so consecutive columns sit 4 dp further apart than they are wide; rounding a drag against the
width would land every long drag short of the finger, and the error grows with distance.

**Clamp, then snap — in that order.** Snapping first can push a landing back off the grain, because
the last legal start is not a multiple of the grain in general. `a clamped landing is still on the
grain` is the test, written against a 20-minute grain so it fails if the order is ever reversed.

**`rememberUpdatedState` on the entry and on the two escape lambdas.** A `pointerInput` block
outlives the composition that created it, so anything captured inside goes stale the moment the
caller recomposes — and nothing about that failure is visible, because the gesture keeps working and
reports into an older frame. See the third defect above for what that cost.

## 🧪 Tests

| Layer | Result |
|---|---|
| **Server unit / integration / endpoints** | **n/a** — this ticket touches no Cloud Function. |
| **Database (`firestore-tests/`)** | **n/a, and structurally so** — `users/{uid}/occurrences` is already covered by the owner-only `users/{uid}/{document=**}` match, and nothing here adds a collection. |
| **Client / domain (JVM)** | **1068 tests, 0 failures** — the whole suite. **28** are this ticket's, in `DragToMoveTest`; the calendar package alone is 118. |
| **UI E2E (instrumented, `emulator-5554`)** | **296 tests, 0 failures** — the whole suite. **14** are this ticket's, in `DragToMoveUiTest`. |
| **Render pass** | **4 PNGs pulled and looked at** — see below. |

### What the two test layers deliberately do *not* share

Every question about **where a drag lands** is a pure function of a geometry and is answered on the
JVM with no emulator: the column arithmetic, the snap, both clamps, which scope a one-off gets, and
whether the sheet appears at all. The instrumented suite asks the complementary question — **does
the gesture arrive** — which is a fact about the Compose gesture graph and is not expressible in a
unit test. Neither repeats the other.

`DragToMoveTest` runs the **real `ScheduleEdits`** in six of its cases. Not to re-test `#63`, but
because the thing being checked is *the scope this package chooses produces the write the product
wants*, and only running it end to end can say so.

### Render pass

| File | What it shows |
|---|---|
| `issue-68-scope-sheet.png` | §2.1's question over a repeating row: two equal-weight answers, no nominated default, and a `Cancel` |
| `issue-68-action-sheet.png` | the entry menu on a one-off: `Skip`, and the sentence saying a skip is not a miss |
| `issue-68-too-large.png` | the refusal, naming **812** and **500** on the calendar it refused |
| `issue-68-mid-drag.png` | a row **in the air** — pointer still down, chip offset and dimmed |

The last one exists because *"picked up, and it reads as picked up"* is a claim about a picture, and
the only moment it is true is mid-gesture. `aRowBeingCarriedLooksCarried` holds the pointer down,
captures, asserts the row actually moved, and only then lets go.

## Out of scope, as the brief said

Google Calendar write-back beyond §2.7 and `#61` · any change to `ScheduleEdits`' semantics (which
is why the defect above is reported rather than fixed) · drag on any surface other than the
calendar.

**`strings.xml` and `values-iw/strings.xml` were named in the brief and are untouched, deliberately.**
§0.8 is suspended and `feature/calendar` is not in `AnalyticsLiteralSweepTest`'s `SWEPT_PACKAGES`,
so the whole package writes plain English literals inline and every new string here follows it.
Adding two resource files for this one feature would leave the package half-swept, which is the
shape `AGENTS.md`'s freeze block warns against. **`DialogLocaleGuardTest` stays honoured** — both
new windows go through `AppModalBottomSheet`.

## 🤝 This push carries three foreign commits, and it discharges half of what they said they owed

`git log @{u}..HEAD` at push time held three commits from **`66-unmeasured-percent` (follow-on)**,
which committed into this shared working tree while `#68` was being built:

| Commit | What it is |
|---|---|
| `fc5a8e9` | its claim |
| `f25cca5` | the dashboard caption `#66` made false — *"Averaged across all your goals"* over a number now taken across measured goals only |
| `254872f` | its row release, plus the brief `sessions/69-verify-dashboard-average.md` |

**Adjudicated on the board, not on git state**, per the auto-push preconditions: that session left an
**explicit, self-written release note** (`SESSIONS.md`, dated 2026-08-23 15:10) and its paths are
quiet in the tree, so a positive signal settles it and no transcript check is owed. Its row names
`68-drag-to-move` and asserts disjointness — `feature/dashboard/` against `feature/calendar/**` —
and that held: not one of this ticket's files overlaps it.

✅ **Its *"IT IS UNVERIFIED"* flag is half discharged by this session's runs, and it is worth saying
so.** That session released without building because `#68` held the Gradle daemon and its own
uncommitted calendar work was in the tree — a run there would have reported about *this* tree. This
session's two suites were then built and run at **`HEAD = 254872f`**, i.e. **over `f25cca5`**:
**1068 JVM, 0 failures** and **296 instrumented, 0 failures**. So *it compiles and breaks nothing*
is now established. **What is NOT discharged is the semantic half** — whether the new caption
actually describes the population the number is taken over — which is exactly what
`sessions/69-verify-dashboard-average.md` exists for. `/kickoff 69-verify-dashboard-average` still
has work to do; it just no longer has to find out whether the tree builds.
