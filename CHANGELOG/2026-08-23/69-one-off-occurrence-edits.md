# 69-one-off-occurrence-edits — a `null` that names an instance, not the absence of one

> **Summary:** [`#69`](https://github.com/idomarhaim/Android_Final_Project/issues/69) — `ScheduleEdits.apply` could not address a **one-off's** occurrence document, because its `seriesDate` parameter was a non-null `LocalDate` and a one-off's document carries `seriesDate = null` by construction. Both `EditScope` values went wrong on it in opposite directions and **both silently**: `THIS_OCCURRENCE` fell through to a blank-id instance and **created a second document** (the row drawn twice), `THIS_AND_FUTURE` moved `Task.occurrence` while the calendar kept drawing the untouched document (the move a **no-op**). The parameter is now `LocalDate?`, `TaskSchedule.storedFor` is the lookup that can receive the null, and `moveSeries`' no-rule branch carries the document across instead of leaving it behind. The guard `#68` put on `CalendarEntry.isEditable` — its third condition — comes off in the same commit, together with the test that pinned it and `MoveScope.seriesDateOf`'s `?: entry.date` fallback, which was itself the defect at the call site. Written **red first**: with the two production files restored to `HEAD` the five new assertions fail and their messages name the defect verbatim — a second document `minted-1` sitting beside `occ-1`, and an empty upsert list. **1084 JVM unit tests, 0 failures, 0 errors, 0 skipped** across 89 suites after the fix. The instrumented check is **written and not run** — the emulator is held by a live sibling; see *Tests*.

**Date:** 2026-08-23 · **Session:** `69-one-off-occurrence-edits` · **Mode:** AUTO (Ido's message opened the session with `AUTO MODE`; the brief's front matter said `mode: normal`, which was the intent recorded when it was written — this session's message wins, per `/kickoff` §4) · **Issue:** [#69](https://github.com/idomarhaim/Android_Final_Project/issues/69)

## The four sites the brief named, and they moved together

`#70` recorded the failure this was written to avoid one ticket earlier — *a correction that changes
what a thing means has to move every label that names it*. All four are in this commit:

| # | Site | What changed |
|---|---|---|
| 1 | `domain/model/Schedule.kt` — `ScheduleEdits.apply` | `seriesDate: LocalDate` → `LocalDate?`; the lookup is now `TaskSchedule.storedFor`, which can receive the null |
| 2 | `feature/calendar/CalendarModel.kt` — `CalendarEntry.isEditable` | third condition `(seriesDate != null \|\| occurrenceId == null)` **removed**; the KDoc now records what it guarded and that `#69` closed it |
| 3 | `feature/calendar/CalendarModel.kt` — `MoveScope.seriesDateOf` | returns `LocalDate?`; the `?: entry.date` fallback **removed** — see below, it was not merely made redundant, it was the call-site half of the defect |
| 4 | `test/…/calendar/DragToMoveTest.kt` | the pinning test is **inverted, not deleted** — it now asserts the row *is* editable, which is what fails if the guard is ever put back |

## The fix is two changes, and the ticket only named one of them

`#69` said *"widen the parameter and make the lookup able to find a document whose `seriesDate` is
null"*. That closes `THIS_OCCURRENCE` completely. It does **not** close `THIS_AND_FUTURE`, and the
ticket's own table says why without drawing the conclusion:

> `moveSeries`' no-rule branch writes `Task.occurrence` and touches **no** document — but a one-off
> *with* a document is drawn **from that document** (`TaskSchedule.occurrencesIn`, source 3).

Widening the parameter changes nothing about a branch that never looks a document up. So the no-rule
branch now writes **both halves** — the anchor, which every other surface reads, and the document,
which is what the calendar draws:

```kotlin
val rule = task.repeatRule ?: return SchedulePlan.Writes(
    task = task.copy(occurrence = moved),
    upserts = if (existing.id.isBlank()) emptyList() else listOf(existing.copy(occurrence = moved)),
)
```

**The blank id is the discriminator, and it is load-bearing.** A one-off with **no** document still
gains none — `existing` is then the instance `apply` synthesised from the anchor, its id is blank,
and manufacturing a document for a one-off with no series is exactly the shape §2.1 rejects and
`MoveScope.whenNotAsked`'s KDoc exists to avoid. A one-off **with** a document gets that document
updated in place. One expression separates the two cases and neither needs a new flag.

## `seriesDateOf`'s fallback was not redundant — it was the defect, at the call site

The brief asked for site 3 to be *re-read* rather than left asserting a premise this session
removes. Re-reading it turned up more than a stale premise. It read:

```kotlin
fun seriesDateOf(entry: CalendarEntry): LocalDate = entry.seriesDate ?: entry.date
```

and its KDoc argued the fallback was safe **because** `isEditable` guaranteed it was only reached
where `entry.date` really is the task's anchor. That argument is **true and beside the point**. The
day was right; the *field* was wrong. No document carries a one-off's calendar day in `seriesDate` —
it carries `null` there — so the fallback handed `apply` a value that could match nothing, whatever
day it was. Removing the guard without removing the fallback would have left `#69` open with its
guard gone: the widened lookup would have been passed a non-null date and matched nothing, exactly
as before.

`null` is now the answer for a one-off, and that is the whole shape of the fix: **`null` names an
instance here, it does not mean the absence of one.** Both `apply`'s KDoc and `storedFor`'s say so
in those words, because a nullable parameter whose null means *a specific thing* is the kind of
signature a later reader "tidies up".

## Two smaller consequences, recorded because they are behaviour and not refactoring

1. **`endSeries` and `moveSeries` now take the resolved instance.** `apply` already resolved
   `existing`; both helpers used to re-run the same lookup for their no-rule branches, which is how
   the hole reached `Skip` as well as `MoveTo`. They take it as a parameter now, so there is one
   lookup and one place it can be wrong.
2. **A `null` series date on a *ruled* task names the series.** The two helpers need a non-null date
   once a rule exists, and `null` there cannot mean *no instance* — a rule generates from an anchor.
   Both fall back to `schedule.anchorDate`, which ends or moves the series from its own first
   instance. Not reachable from the calendar (`seriesDateOf` returns a non-null date for every
   repeating entry) and written down rather than left to `!!`.

## What this does NOT change

- **`isEditable`'s other two conditions.** `isTickable` and `!isSettled` are product rules from §2.3
  and §2.8 and had nothing to do with this defect. Out of scope by name in the brief, and untouched.
- **`#68`'s drag-to-move behaviour**, which shipped and is tested. Every one of its existing
  assertions still holds — including the two that pin `whenNotAsked`'s choice for a one-off with no
  document, which is the case the blank-id branch above preserves deliberately.
- **`#61`'s sync.** `SyncCalendarUseCase.link` was read to build the fixture and not otherwise
  touched.

## 🧪 Tests

### Red first, and the failures name the defect

The two production files were restored to `HEAD` with the new tests left in place, so what ran was
the new assertions against the **old** code. `19:50:28 → 19:52:48`, `--tests DragToMoveTest
--rerun-tasks`, `32 actionable tasks: 32 executed` — a real compile, not a replay.

```
32 tests completed, 5 failed
```

| Failing test | What it said |
|---|---|
| `…draws the linked one-off once, in its new place` | `expected: 1 but was: 2` — `[occ-1 @ Mon 09:00, minted-1 @ Tue 14:00]`, the duplicate document, printed |
| `…through THIS_OCCURRENCE overwrites its document` | `expected: occ-1 but was an empty string` — the blank id that makes the repository **create** |
| `…through the default scope moves its document too` | `NoSuchElementException: List is empty` — `moveSeries`' no-rule branch upserted nothing at all |
| `skipping a linked one-off settles its document…` | `expected: occ-1 but was an empty string` — the same hole on the `Skip` leg |
| `a one-off that already has a document is editable…` | `value of: isEditable() expected to be true` — the guard, still standing |

### Green after

`19:53:17 → 19:55:58`, full `:app:testDebugUnitTest --rerun-tasks`, `BUILD SUCCESSFUL`,
`32 actionable tasks: 32 executed`.

**Read out of the result XMLs rather than off the verdict**, which is `#70`'s lesson from this
morning — a `BUILD SUCCESSFUL` whose tasks were `UP-TO-DATE` is a replay of somebody else's run:

```
suites 89 | tests 1084 | failures 0 | errors 0 | skipped 0
```

- Every result file is stamped `19:55:5x`, **inside this session**.
- Exactly **three** tasks reported `UP-TO-DATE` — `:app:preBuild`, `:app:preDebugBuild`,
  `:app:preDebugUnitTestBuild`, all lifecycle no-ops. `:app:testDebugUnitTest` executed.
- **The count is accounted for, not assumed.** `#68` measured **1068** this morning.
  `1068 + 12 + 4 = 1084`: twelve are `DeletionReachTest`, which arrived on `main` in
  `67-delete-anything`'s `c11c629` while this session was writing, and four are this ticket's
  (`DragToMoveTest` 28 → 32). Nothing is unexplained.

One pre-existing Kotlin warning surfaced and is **not** this ticket's:
`HebrewLocaleResourceTest.kt:158` — a nullable `java.io.File?` receiver. Untouched.

### ⚠️ The instrumented check is written and NOT run — the emulator is held

`DragToMoveUiTest.aGoogleLinkedOneOffCanBeDraggedNowThatItsDocumentIsAddressable` is the device half
of the exit: a one-off carrying `occurrenceId = "occ-1"` and `seriesDate = null` — asserted to be
that shape before the gesture, so the fixture cannot drift into testing something easier — dragged
one column, expecting one reported move at `THIS_AND_FUTURE` and **no** scope sheet. It uses the
file's existing bare `createComposeRule()`, so it needs no Google account and destroys no sign-in.

It did not run, and the reason is not capability:

- **`emulator-5554` and `adb` are claimed by `67-delete-anything`**, which is live and mid-run
  (last transcript turn 19:56, one commit `c11c629` landed at 19:47). Two sessions driving one AVD
  corrupts its quickboot snapshot — `AGENTS.md` § Pitfalls.
- **Its two untracked `androidTest` files are in this shared tree** (`DeleteAnythingUiTest.kt`,
  `TmpDeleteDump.kt`), so building the test APK now would compile that session's unfinished work
  into the run and report about **its** tree, not this one — §4p, the failure `f25cca5` refused to
  risk this morning and `#70` then caught arriving anyway one step later.

The JVM layer is unaffected by both: `:app:testDebugUnitTest` does not compile the `androidTest`
source set, and every tracked file the run did compile is committed.

**`#69` therefore stays OPEN with this commit**, naming the one thing outstanding.

## Layers

| Layer | Status |
|---|---|
| Domain / JVM unit (`:app:testDebugUnitTest`) | **green** — 1084 tests, 0 failures, 0 errors, 0 skipped, 89 suites, run at 19:53–19:56 with `--rerun-tasks`; **red first**, 5 failures against `HEAD` |
| Instrumented UI (`androidTest`) | **written, NOT run** — one new test; `emulator-5554` is claimed and in use by `67-delete-anything`, and its untracked `androidTest` files are in this tree. Owed. |
| Firestore rules (`firestore-tests/`) | **not applicable** — this ticket changes no rules and writes through the same `OccurrenceRepository.apply` batch as before |
| Cloud Functions (`functions/`) | **not applicable** — nothing server-side is involved |

## Notes for whoever comes next

- **`storedFor(null)` on a task that has a rule would match a stray one-off document.** It cannot be
  reached that way today: `apply` is only passed `null` by `seriesDateOf`, which returns `null` only
  for an entry with no series date, and a ruled task's entries all carry one. Written down because
  the lookup is `internal` and the next caller will not know that.
- **The Google-linked one-off is now editable and nothing verifies it end to end on a device.** The
  fixture in `DragToMoveTest` is built from `SyncCalendarUseCase.link`'s shape by reading it, not by
  running the sync. See the tests section for what was and was not exercised.
