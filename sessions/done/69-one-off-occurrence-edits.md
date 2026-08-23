---
repo: c:\Dev\Android_Final_Project
branch: main
mode: normal
status: done
result: |
  Shipped in 49e1bde (fix + JVM layer) and the commit that carries this close (device layer).
  1084 JVM unit tests / 0 failures; 15 instrumented / 0 failures on emulator-5554.
  Every Exit item met. The one gap, named in the changelog: no instrumented RED run --
  the guard-removal claim is red-proven on the JVM layer and green-proven on the device.
  #69 closed.
issue: 69
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Schedule.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarModel.kt
  - app/src/test/java/com/idomarhaim/goalpilot/feature/calendar/DragToMoveTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/DragToMoveUiTest.kt
  - CHANGELOG/2026-08-23/69-one-off-occurrence-edits.md
  - sessions/69-one-off-occurrence-edits.md
created: 2026-08-23 by 70-verify-dashboard-average
---

# `#69` — `ScheduleEdits.apply` cannot address a one-off's document, and both scopes fail silently

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `normal`

ℹ️ **Why this brief exists, written 2026-08-23 by a session that is not going to do the work.**
`#69` was filed by `68-drag-to-move` as a defect it found by **reading**, not by a session that set
out to fix it — so it got a ticket and a guard and never got a `sessions/` file. Ido noticed the gap
and asked for it. Nothing about the ticket has changed; this is the work order it was missing.

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`#69`](https://github.com/idomarhaim/Android_Final_Project/issues/69) — **the whole specification
is there**, including the fix and where to start ·
[`CHANGELOG/2026-08-23/68-drag-to-move.md`](../CHANGELOG/2026-08-23/68-drag-to-move.md)

## Task

Widen `ScheduleEdits.apply`'s `seriesDate` parameter to `LocalDate?` and make its instance lookup
able to find a document whose `seriesDate` is `null`. Then **remove the guard that exists only
because it could not**, and the test that pins the guard, together in the same commit.

`#68` named *"any change to `ScheduleEdits`' semantics"* as out of scope, which is why it guarded
instead of fixing. This session is the one that is allowed to.

## The defect, in one table

`apply` finds the instance an edit is about with
`schedule.stored.firstOrNull { it.seriesDate == seriesDate }`, over a **non-null** `seriesDate`. A
one-off's document carries `seriesDate = null` by construction, so that clause can **never** match
it and control always falls through to `instanceOn`.

| Scope | What happens | What Ido sees |
|---|---|---|
| `THIS_OCCURRENCE` | `instanceOn` returns an instance with a **blank id**, so the upsert **creates a second document** | the row is drawn **twice** |
| `THIS_AND_FUTURE` | `moveSeries`' no-rule branch writes `Task.occurrence` and touches **no** document — but a one-off *with* a document is drawn **from that document** (`TaskSchedule.occurrencesIn`, source 3) | the move appears to **do nothing** |

Both directions are **silent**, and one of them duplicates data.

## The four sites, and they must move together

The codebase already carries forward pointers to this ticket; the risk is fixing the arithmetic and
leaving a pointer standing, which is exactly the failure `#70` recorded one ticket ago (*a
correction that changes what a thing means has to move every label that names it*).

1. **`domain/model/Schedule.kt:399`** — `ScheduleEdits.apply`: the parameter and the lookup.
2. **`feature/calendar/CalendarModel.kt:342`** — `CalendarEntry.isEditable`'s **third** condition,
   `(seriesDate != null || occurrenceId == null)`. Its KDoc says outright that this condition comes
   off when `#69` lands. The other two conditions (`isTickable`, `!isSettled`) are **product** rules
   and stay.
3. **`feature/calendar/CalendarModel.kt:595`** — `MoveScope.seriesDateOf`'s KDoc argues its
   `?: entry.date` fallback is safe **because of** that guard. Re-read it; do not leave it asserting
   a premise this session removes.
4. **`test/…/calendar/DragToMoveTest.kt`** — *"a one-off that already has a document is not editable,
   and that is a hole not a rule"*. Its own comment says it is what allows the guard to come off.

## What actually reaches this in the shipped app

Two ways a one-off gets a document, and only one is the reachable case:

- **Ticking it** — `CalendarViewModel.setDone` writes `seriesDate = null`. Already excluded by
  `isSettled`, because a settled window is history under §2.3/§2.8 — **not** because of this defect.
- **`#61` pushing it to Google** — `SyncCalendarUseCase.link` mints the document when
  `entry.occurrence.id.isBlank()`, with `seriesDate = null`, `googleEventId` set and outcome still
  `Planned`. **This is the case the third condition actually buys**, and the one to build a test
  around.

## Exit

- **A test written red first** for each direction — the duplicate document under `THIS_OCCURRENCE`,
  and the no-op under `THIS_AND_FUTURE` — against a one-off carrying a `googleEventId`, which is the
  reachable shape rather than the tickable one.
- `:app:testDebugUnitTest` green. ⚠️ **Read the result files, not the verdict** — a `BUILD
  SUCCESSFUL` in a few seconds with every task `UP-TO-DATE` is a replay of somebody else's build over
  their tree, and it looks exactly like a pass. Check
  `ls -l app/build/test-results/testDebugUnitTest/*.xml | head -1` for a timestamp inside your own
  session before believing it; `--rerun-tasks` if in doubt. `Observed:` this cost `#70` its first
  result, on 2026-08-23. **The count at that date was 1068 — treat any number in a brief as stale,
  not as an assertion.**
- **A device check that the guard is genuinely gone**: a Google-linked one-off can be dragged and the
  calendar draws it **once**, in its new place. ⚠️ `adb install -r` on both APKs + `am instrument`,
  **never** `connectedDebugAndroidTest` — it uninstalls the app and takes the Firebase sign-in with
  it. The existing `DragToMoveUiTest` uses a bare `createComposeRule()`, so it needs no account; a
  test that exercises the `#61` link path may.
- `CHANGELOG/2026-08-23/69-one-off-occurrence-edits.md` *(or the day you actually run it)*, and this
  brief moved to `sessions/done/` with `status: done` in the same commit.
- Close `#69` — nothing else carries `issue: 69`.

## Out of scope

- **`#68`'s drag-to-move behaviour itself**, which shipped and is tested.
- **Anything about `#61`'s sync** beyond reading `SyncCalendarUseCase.link` to build the fixture.
- **Widening `isEditable`'s other two conditions.** `isTickable` and `!isSettled` are product rules
  from §2.3/§2.8 and have nothing to do with this defect; only the third condition is this ticket's.

## Note — not urgent, and that is a reason to write it down rather than to hurry

Nothing in the shipped app can reach this: `isEditable` gates both *drag to move* and *Skip*. It
matters because it is a **live trap for the next person to touch either file** — silent in both
directions, and one of them corrupts data by duplicating a document. So it is worth a session, and
it is not worth jumping the queue for.
