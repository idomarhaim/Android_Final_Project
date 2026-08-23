# `#67` — delete anything, and the task that could not be deleted at all

**Session** `67-delete-anything` · **2026-08-23** · brief
[`sessions/67-delete-anything.md`](../../sessions/done/67-delete-anything.md) ·
ticket [`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67)

> **Summary:** `#67` — every entity now has a delete reachable from where it is looked at, behind
> one shared confirm that states what goes and what stays; the quick-add task that no screen listed
> is reachable, and two repository deletes that were quietly orphaning documents are fixed.

The brief's central finding held: **the capability existed and the gap was reach.** No new
repository method was added. What the survey did **not** predict is that two of the three existing
deletes were leaving documents behind, and that one of them was *manufacturing* the ticket's own
defect on every use.

---

## The defect, confirmed — and it has a front door

`Deletion.unreachableTasks`' KDoc carries the mechanism; the short form:

| Surface | What it lists |
|---|---|
| `GoalDetailScreen` | `observeTasks(goalId)` — tasks filed under **that** goal |
| `CalendarScreen` | every task, drawn only where `TaskSchedule.occurrencesIn` produces an instance |
| everything else | no task list at all — the dashboard has cards, not rows |

A task with **no goal** and **no *when*** is in neither set. `Observed:` in code, and the creation
path is the ordinary one: `DashboardViewModel.classifyForSmartAdd` writes
`goalEdges = goalEdgesOf(null)` for a `FilingDecision.NoGoal` and sets no `occurrence`. The undo
snackbar was the only control that could ever remove one, and it lives for a few seconds.

⚠️ **`Untested:` on a device.** The brief asked for the device check *first*; it has not run yet —
this commit is the JVM-green half and the instrumented run and render pass are the unit that
follows. The defect is established from the code paths above and from `DeletionReachTest`, not from
a phone.

## The half the survey missed: `deleteGoal` was making more of them

`deleteGoal` deleted **one document** and nothing else. Two consequences, both invisible:

1. **Every task kept its edge to the deleted goal.** Such a task reads as *filed* — `goalEdges` is
   not empty — while being listed nowhere, because the goal detail that would show it cannot be
   opened. That is `#67`'s founding defect, produced by the app's own delete.
2. **The progress log became unreachable.** `progressEntries` is a subcollection, and deleting a
   document does not delete its subcollections; `GoalRepositoryImpl.entriesFlow` fans out over *the
   goals that exist*, so those entries had no reader left and no route to one.

Now it unfiles the tasks (keeping them — §1.1's *lossless demotion*, *"the task underneath is real
work he typed in"*), deletes the log, then deletes the goal, in that order and for the reason
`deleteLifeArea` already writes down.

**`Deletion.unreachableTasks` takes the goal list as a parameter because of consequence 1** — a
dangling edge has to be resolved against the goals that exist, or the existing data this created
stays invisible.

## `deleteTask` was orphaning occurrence documents

It deleted the task and its completion fact and left every row in `users/{uid}/occurrences` whose
`taskId` was this one. Checked mechanically against all four consumers —
`CalendarViewModel.schedules`, `BuildSuccessFailureRunUseCase`, `AnalyticsViewModel`,
`SyncCalendarUseCase.entriesIn` — **every one of them joins the collection back to the task list**,
so an orphan is dropped from every count and every lane. No number was ever wrong; the rows simply
accumulated for the life of the account with no reader and no way to remove them.

That is the same argument the method's own existing comment already makes about the completion
fact: *"an orphan fact would add points the user cannot see, find or remove."*

**§2.3 is not violated.** *"A missed occurrence is never edited — it is history"* governs the
**scoped verbs**: `EditScope` offers no `ALL` reaching backwards, so no move and no skip can rewrite
what happened. A delete is not one of those verbs, and the confirm says how many of the windows
going with it had already happened, before it happens.

## 🐛 Found, not fixed, and deliberately out of scope

**Deleting a task leaves its Google Calendar events on Google.** `CalendarSync` emits a
`CalendarPush.Cancel` only for an entry that **still exists** with `draft == null`; a deleted task
produces no entry at all, so nothing cancels its mirrored events.

`Observed:` by reading `CalendarSync.kt:305` against `SyncCalendarUseCase.entriesIn`.
**Pre-existing and unchanged by this ticket** — before it, the occurrence documents survived but the
sync iterates *tasks*, so no `Cancel` was produced then either. Fixing it needs the sync to know
about entries that are **gone**, which is a tombstone or a mirror list — a storage design, and `#67`
is explicit that it does not build one. Named here rather than fixed; it wants its own ticket beside
§2.7.

## What shipped

### The decision layer — `domain/model/Deletion.kt` *(new)*

`Deletion.unreachableTasks` plus `DeletionImpact`, a sealed type carrying **what goes** and **what
stays** per entity. It is a domain type and not three sentences in three dialogs because each count
is a **claim about a write**: written as literals, a claim drifts from its repository silently — the
dialog composes, the English render is perfect, and the sentence is false.

`DeletionReachTest` *(new, 12 tests)* pins each one, including the three cases that are easy to get
backwards: an **archived** goal still counts as reach (archiving is the reversible verb), an **open**
task has banked no points so its deletion takes none, and a **blank** id on either side never
matches.

### One confirm — `ui/components/DeleteConfirm.kt` *(new)*

Two blocks, *WHAT GOES* and *WHAT STAYS*, then *this cannot be undone* last. The second block is
**absent** rather than empty when nothing survives: a *what stays* heading over nothing implies
something does and names none of it.

It replaced two dialogs that were each right and silent — the goal's said *"this permanently removes
the goal"* and nothing about its tasks; the life area's said *"the goals filed under it are kept"*
in prose, on the one screen that already knows the number.

### Reach, per entity

| Where | What changed |
|---|---|
| **Dashboard** | `UnfiledTasksCard` — the tasks no screen lists, each with a delete. Absent when the list is empty, which is almost every open. |
| **Calendar** | `EntryActionSheet` gains `Delete task`, past a divider, in the error colour. `#68` left the spot marked. |
| **Goal detail** | The task row's bare delete icon — the only irreversible act in the app that asked nothing — now opens the confirm. The goal's dialog gained its counts. |
| **Life areas (list)** | The prose dialog became the shared confirm with the count already on the row. |
| **Life area (detail)** | An overflow menu with `Delete` — deleting an area was possible only from the list. |
| **`SuccessFailureRunCard`** | **`Let it go` is here** — `C19`'s third offer, wired on both placements. |

### `Let it go`, and why it may exist now

`#64` shipped without it for a reason that has since expired: *"there is no command behind it, and a
button proposing a goal is over while doing nothing is worse than the honest silence."* `#67` gave
goals a reachable delete, so there is a command.

`C4` is **not** relaxed. The button says `Let it go`; the app still never *suggests* a goal is over.
The row it sits on says `no next step`, which is a statement about what is **scheduled**.

⚠️ **It is drawn in the error colour, and `SuccessFailureRun.kt`'s own KDoc said there is no red in
this file.** That paragraph is **narrowed in place rather than left to rot**: §4.7's ban is on
**outcome state** riding on hue — no window, no dot, no count may be tinted, and none is. A
destructive **control** is a different thing, and the invariant that survives is *no colour here
carries information about how the person is doing*.

`onLetGo` is nullable and the button is **absent** where a host passes `null`, rather than present
and inert — which is precisely the failure `#64`'s omission was protecting against.

## The scope question that is not asked, and why

`Delete` on the calendar gets **no `ScopeSheet`**. That sheet's two answers exist because a *move*
and a *skip* can honestly apply to one instance or to the rest of a series. *"Delete only this
occurrence"* is not a third such answer — it is **`Skip`**, one button up, which
`OccurrenceOutcome.Skipped` already records as a decision rather than a failure. So `Delete` names
the task, and the confirm carries what a scope sheet would have communicated, minus the offer of an
operation the app does not have.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest --rerun-tasks`) | **1084 tests, 0 failures, 0 errors** — genuinely re-executed |
| — of which `DeletionReachTest` *(new)* | **12 tests, 0 failures** |
| **Resource guards** | `AnalyticsLiteralSweepTest`, `HebrewLocaleResourceTest`, `DialogLocaleGuardTest` all green |
| **Instrumented** | ⚠️ **NOT RUN** — owed, and it is the next unit |
| **Render pass** | ⚠️ **NOT RUN** — owed, both themes, English only (§0.8 suspended) |

⚠️ **`AnalyticsLiteralSweepTest` failed first, and it was right.** `DeleteConfirm.kt`'s test tags
were snake_case (`"delete_confirm_cancel"`), which the prose rule reads as five alphabetic words —
correctly, since it cannot tell a tag from a sentence and its own KDoc says the rule is crude on
purpose. Every other tag in `ui/components/` is camelCase; these now are too, with the reason
written beside them.

⚠️ **The suite ran over a SHARED tree, and the count says so.** `69-one-off-occurrence-edits`
claimed one minute after this session (`4d0dee6`) and holds `Schedule.kt`, `CalendarModel.kt`,
`DragToMoveTest.kt` and `DragToMoveUiTest.kt` **uncommitted**. Those edits were in the tree this run
compiled. `Observed:` mechanically rather than assumed — `#70` measured **1068** tests earlier on
2026-08-23, this ticket adds **12** (`DeletionReachTest`), and the run reported **1084**; the
**4**-test remainder is theirs. Their paths and mine are disjoint (checked against their board row,
not by eye) and the run is green, so nothing here is in doubt — but *"1084, 0 failures"* is a
statement about **both** changes, not about this one alone, and §4p is why that is said rather than
left for a reader to work out from a number.

⚠️ **No device was touched and no sign-in was needed or destroyed.**

## Hebrew

`ui/components/` is a **swept** package, so the confirm's eleven strings and `Let it go` went into
`components_strings.xml` **and** `values-iw/` with real translations, and every count is
`bidiIsolated()`. `HebrewLocaleResourceTest` enforces parity in both directions and that an
untranslated copy is not a translation; it passes.

`Untested:` the Hebrew has **not been rendered**. §0.8's *"a design is not finished until it has
been seen in Hebrew"* is suspended, and this ticket is English-only by its own `Exit`. The
translations are authored, parity-checked, and unseen.

## Files

**New** — `domain/model/Deletion.kt` · `ui/components/DeleteConfirm.kt` ·
`app/src/test/java/.../domain/DeletionReachTest.kt`

**Changed** — `data/firestore/TaskRepositoryImpl.kt` · `data/firestore/GoalRepositoryImpl.kt` ·
`feature/dashboard/DashboardScreen.kt` + `DashboardViewModel.kt` ·
`feature/calendar/CalendarScreen.kt` + `CalendarViewModel.kt` + `ScopeSheet.kt` ·
`feature/goals/GoalDetailScreen.kt` + `GoalDetailViewModel.kt` ·
`feature/lifeareas/LifeAreasScreen.kt` + `LifeAreaRows.kt` + `LifeAreaDetailScreen.kt` +
`LifeAreaDetailViewModel.kt` · `feature/analytics/AnalyticsScreen.kt` + `AnalyticsViewModel.kt` ·
`ui/components/SuccessFailureRun.kt` · four `*_strings.xml` ·
`app/src/test/java/.../feature/goals/GoalDetailViewModelTest.kt` (new constructor arg)
