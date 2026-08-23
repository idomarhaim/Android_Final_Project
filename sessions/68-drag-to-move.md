---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 68
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/ScopeSheet.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-iw/strings.xml
  - app/src/test/java/com/idomarhaim/goalpilot/feature/calendar/DragToMoveTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/DragToMoveUiTest.kt
  - kb-candidates/YYYY-MM-DD-68-drag-to-move.md
  - CHANGELOG/YYYY-MM-DD/68-drag-to-move.md
  - sessions/68-drag-to-move.md
singletons:
  - the AVD and adb, for the gesture pass — a drag cannot be verified any other way
created: 2026-08-23 by 64-area-success-failure
---

# `#68` — drag to move, and the sheet that makes `#63`'s question askable

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

⚠️ **COLLIDES with [`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67) —
these two CANNOT run in parallel.** Checked mechanically against that brief's `owns:` list, not by
eye: both own
**`app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarScreen.kt`**. That is the
whole overlap — every other path is disjoint — but one shared file is enough, and both also need
the **AVD** and the **Gradle daemon**, which are exclusive singletons whatever the file lists say.

**Take `#68` first.** `#68` builds the per-occurrence action menu on the calendar (it needs one for
*Skip*), and `#67` then adds **one item** to a menu that already exists. In the other order `#67`
invents a delete affordance and `#68` reworks it while building the menu — the same work, done
twice. Nothing in `#68` depends on `#67`; the ordering is about not doing the calendar's action menu twice.

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`#68`](https://github.com/idomarhaim/Android_Final_Project/issues/68) ·
[`docs/PRODUCT_v0.3.md` §4.3](../docs/PRODUCT_v0.3.md) (line 1279 is the sentence) and **§2.1,
§2.3, §2.8** · [`Schedule.kt`](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Schedule.kt)
— `ScheduleEdits`, `EditScope`, `SchedulePlan`, and read their KDoc before writing anything, because
the decisions are all in there.

**Task:** build §4.3's *drag to move* on the calendar, and the scope sheet that lets it — and
`Skip` — reach the machinery `#63` already shipped.

## ⚠️ Verify these two claims before you start. They are why this ticket exists.

Both were measured on 2026-08-23 and both should be re-measured, because a brief is not the
authority on the state of the code:

1. **No drag gesture exists.** `grep -n -i 'drag\|detectDragGestures\|pointerInput' feature/calendar/*.kt`
   returned **KDoc mentions only** — no gesture.
2. **`ScheduleEdits`, `EditScope`, `ScheduleEdit` and `OccurrenceRepository.apply` have ZERO call
   sites** under `feature/` and `ui/`. `#63` built the complete answer to *"this occurrence, or
   all future ones?"* and **nothing in the app can reach it.**

If either has changed, say so and re-scope — that is the right outcome of opening this brief.

**What IS already wired, so you do not rebuild it:** *tick to complete*. `CalendarViewModel`
calls `occurrences.setOutcome(existing, OccurrenceOutcome.Done(...))`. Tick needs no scope
question — it is per-instance by nature — which is exactly why it shipped without any of this.

## What this ticket owes

1. **Drag to move**, on the calendar surface. §4.3 names it beside *create* and *tick*, both of
   which shipped with `#60`.
2. **The scope sheet.** A move on a **repeating** task asks *"this occurrence, or all future
   ones?"* and hands the answer to `ScheduleEdits.apply`. On a **non-repeating** task there is
   nothing to ask and **the sheet must not appear** — §2.1's question exists only where a rule
   does, and asking it anyway teaches the user it is meaningless.
3. **Skip**, as the second entry point to the same machinery — one menu item once the sheet
   exists. Leaving it out keeps `ScheduleEdit.Skip` unreachable for no reason.
4. **`SchedulePlan.TooLarge` surfaced, never swallowed.** Reachable from exactly one edit
   (`THIS_AND_FUTURE` + `MoveTo` on a long daily series) and §0.4 forbids silence about a
   refusal. It already carries both numbers — show them.

## Already decided — do not reopen

- **A missed occurrence is never edited — it is history** (§2.3); §2.8: *"every destructive effect
  splits by tense: future events cancel, past events stay."* `EditScope` deliberately has **no**
  `ALL` reaching backwards. Do not add one.
- **A skip is not a miss.** Excluded from `Doneness`' totals and from `#64`'s run. Counting it
  against the user is §2.3's *"an over-eager agent manufactures failures"* in reverse.
- **`ScheduleEdits.apply` is pure and takes its clock as an argument.** The decision stays there
  and the commit stays in the repository; do not re-derive a plan in a ViewModel.
- **`THIS_AND_FUTURE` + `MoveTo` writes the past down before moving the anchor** — that is not an
  implementation detail to optimise away, it is what stops the move rewriting history. Its
  `RepeatEnd.AfterCount` decrement is the branch `Schedule.kt` itself calls *"worth a test"*.

## One stale comment to correct while you are in there

`feature/calendar/CalendarModel.kt:157` — *"the only kind the user can tick, **drag** or
reschedule"*. Two of those three are false today. It is the forward-pointer class
(`kb/dev/retracting-a-copied-claim.md` §5): a comment describing a capability that does not exist.
Either make it true or correct it; do not leave it standing.

## Exit

- **JVM tests** for the branch that decides *whether the sheet appears at all* (rule present vs
  absent) and for what each scope produces. `ScheduleEdits` is pure and already tested — what is
  new is the **mapping from a gesture to an `EditScope`**, and that is where a wrong answer is
  silent.
- **Instrumented test + a device gesture pass.** A drag cannot be verified any other way; a unit
  test of a drag handler is a test of your own arithmetic. §0.8 is suspended, so **English only**.
- ⚠️ `adb install -r` + `am instrument`, **never `connectedDebugAndroidTest`** — it uninstalls the
  app and takes the Google sign-in with it.
- `CHANGELOG/YYYY-MM-DD/68-drag-to-move.md` with counts verbatim.

## Out of scope

Google Calendar write-back beyond what §2.7 and `#61` already do · any change to `ScheduleEdits`'
semantics · drag on any surface other than the calendar.

## Note on ordering

This and [`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67) both change
surfaces that [`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62)'s tour video
walks through, so both belong **before** it — `#62` is already a `v2` because features landed
after the first recording.

**No *dependency* runs between this and `#67` — but they still cannot run at the same time.** The
two are different questions and the collision note at the top of this brief is the one that
governs: neither needs the other's output, and they share a file and two singletons. Sequential,
`#68` first.
