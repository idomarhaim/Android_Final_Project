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

⚠️ **`Untested:` end to end on a phone, and that is stated rather than glossed.** The brief asked
for a device check that creates an unfiled task through smart-add and hunts for it on every surface.
What ran instead is the **code** path above plus `DeletionReachTest`, and on the device the
**component** half — `DeleteAnythingUiTest` drives the real confirm and the real calendar sheet with
hand-built inputs. Nobody has watched a quick-add produce an orphan and then deleted it from the
dashboard card. The reason is that the end-to-end path needs a signed-in account and a live
Firestore, which is a test of the network with a card attached; the reach claim is a claim about
**which screens list what**, and that is decided in code and pinned in the JVM layer.

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
| **Instrumented** (`DeleteAnythingUiTest`, new) | **15 tests, 0 failures** |
| **Instrumented — full regression** (`am instrument`, no class filter) | **320 tests, 0 failures** across 41 classes |
| **Render pass** | **6 PNGs**, three subjects × light/dark plus the calendar sheet, read by eye |

`adb install -r` on both APKs then `adb shell am instrument -w`, per
`kb/dev/android-device-verification.md` §8 — **`connectedDebugAndroidTest` was never used.**

⚠️ **The 320 included one temporary class.** `TmpDeleteDump` was a scratch test written to dump the
semantics tree (below); it is **removed from the repo**, the test APK was rebuilt without it, and
running it now fails *class not found*. So the honest figure for the suite as committed is **319**,
and `DeleteAnythingUiTest`'s **15** were re-run green after the removal.

⚠️ **A failed build reported a green test run, and `${PIPESTATUS[0]}` is the only reason it was
caught.** One `./gradlew assembleDebug assembleDebugAndroidTest` failed in 3 s (a transient Windows
KSP lock; it succeeded unchanged on the next run), and because the **previous** APK was still at the
output path, `adb install -r` said `Success` twice and `am instrument` reported `OK (15 tests)` — for
the build before the fix. AGENTS.md's pitfall predicted exactly this; it fired.

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

## 🔎 Two findings the run produced, and neither was predicted

### 1. `onNodeWithText(substring = true)` cannot find a count in this app

**8 of the 15 instrumented tests failed on the first run, and every one of them was matching a
string with a number in it.** The failure message is `Assert failed: The component is not
displayed!`, which points at layout; the cause is the string.

Every count goes through `bidiIsolated()` (§4.8), so the node holds

```
⁨4⁩ entries in its progress log.        codes: 8296, 52, 8297, 32, 101, …
```

— `U+2069 POP DIRECTIONAL ISOLATE` sits **between the digit and the space**, so `"4 entries"` is not
a substring of it. The marks are zero-width, the text renders perfectly, and nothing in the failure
suggests the string. It was diagnosed by **dumping the semantics tree on the device** rather than by
reading the code, which is the only thing that would have settled it.

**The tempting fix is the wrong one.** Matching `"entries in its progress log"` and dropping the
number would go green while leaving the one thing this ticket added — the counts — unasserted. The
**matcher** is fixed instead: `Bidi.strip` already exists for this and its KDoc says so, *"for tests
and for logging, never for display."* `DeleteAnythingUiTest.hasStrippedText` carries the whole
finding.

⚠️ This is not specific to `#67`. **Every** swept-package string with a count is unmatchable by a
naive substring assertion, and any future test written that way will fail with a message about
layout.

### 2. A flat list of four lines invited an addition that is wrong

With all 15 tests green, the task confirm read:

```
WHAT GOES
This task.
12 scheduled occurrences.
Including 5 that already happened.
The 40 points it earned.
```

The third line is a **subset of the second**, drawn as a fourth peer — so the page invites a total of
`1 + 12 + 5 + 40`. That is §0.3's *second number that quietly disagrees*, arriving as typography
rather than as a field, and no matcher can see it. Found by looking at
`issue-67-confirm-task-light.png`.

Fixed by **subordination** — smaller, muted, indented in the layout direction — and not by folding
the two counts into one sentence, which would need two interacting plural forms per language where
Hebrew has four categories to English's two. Re-rendered and re-read.

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
`app/src/test/java/.../domain/DeletionReachTest.kt` ·
`app/src/androidTest/java/.../ui/DeleteAnythingUiTest.kt` ·
`docs/render-passes/2026-08-23-67-delete-anything/` (6 PNGs)

**Changed** — `data/firestore/TaskRepositoryImpl.kt` · `data/firestore/GoalRepositoryImpl.kt` ·
`feature/dashboard/DashboardScreen.kt` + `DashboardViewModel.kt` ·
`feature/calendar/CalendarScreen.kt` + `CalendarViewModel.kt` + `ScopeSheet.kt` ·
`feature/goals/GoalDetailScreen.kt` + `GoalDetailViewModel.kt` ·
`feature/lifeareas/LifeAreasScreen.kt` + `LifeAreaRows.kt` + `LifeAreaDetailScreen.kt` +
`LifeAreaDetailViewModel.kt` · `feature/analytics/AnalyticsScreen.kt` + `AnalyticsViewModel.kt` ·
`ui/components/SuccessFailureRun.kt` · four `*_strings.xml` ·
`app/src/test/java/.../feature/goals/GoalDetailViewModelTest.kt` (new constructor arg)
