---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 7
created: 2026-08-20
done: 2026-08-20 by `7-quickadd-complete` — `e573404` (claim) → `153620b` (#7 ships) → `0234745` (KB, in `C:\Dev\JARVIS`)
---

# `#7` — complete a task from inside the add flow

**Depends on `c20-build-half`.** Needs the **Gradle daemon**; a device only for the render pass.

> ✅ **BOTH ORDERING CONSTRAINTS ARE DISCHARGED — `9-duration-box` and `6-silent-filing` have
> both shipped (2026-08-20). Nothing blocks this brief.**
>
> · `#9` landed in `8187bbb`. `GoalDetailScreen.kt`'s `AddTaskRow` now reads a `DurationEntry`
>   rather than reconstructing `aiMinutes`, so the value this brief was told to wait for is
>   settled: build on it.
> · `#6` landed after it, and it **ran second rather than first** — `6-silent-filing` checked the
>   constraint instead of obeying it on sight, and found that `613b454` justified this pair on
>   **file adjacency alone**, which binds concurrent sessions and not sequential ones. Its own
>   check went further: this brief puts the done-affordance in `SmartAddCard`, and removing the
>   dialog *forces* that placement rather than removing it.
>
> ⚠️ **So the landmarks in *Where it goes* below have MOVED. Re-read the file, do not trust the
> line numbers.** `SmartAddDialog` **no longer exists** — deleted with `confirmSmartAdd` and
> `dismissSmartAdd`. `SmartAddCard` is now `internal`, takes a `SmartAddState`, and shows an
> in-place progress row while a task is in flight (`SmartAddTestTags.SORTING`).
> `classifyForSmartAdd` classifies **and writes**, then posts a `SmartAddReceipt` that the screen
> turns into a snackbar with **Undo**.
>
> 🔑 **What that means for THIS ticket.** There is no confirmation step left to hang a
> *"...and it is already done"* checkbox on, so the affordance belongs in the card's input row
> beside *Sort* — which is what this brief already said. Whatever it emits has to ride the
> **same single write** `classifyForSmartAdd` already makes (see *The trap* below), and the
> receipt is where its undo belongs, not a second snackbar.

## Why it exists

`R6`: *"There should be a way to complete the task from within 'quick add'."* Today a task you type
in *because you have already done it* needs four navigations: add it on the dashboard, find the
goal, open goal detail, tick it.

## ⛔ Precondition

`#3`'s connectivity pre-check must be **gone** — i.e. `50-finish` has landed, or at minimum
`c20-build-half` has. #7's own ticket says so: *"Under `C20` facts are ordinary writes that hit the
offline cache, so completing a task offline works for real."* Building a create-and-complete on top
of a transactional `setDone` ships the 7.9 s lie in a second place.

Check: `grep -c runTransaction app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt`
must be **0**.

> ⚠️ **That check is a FALSE NEGATIVE as written — it returns `3`, and the precondition is
> nevertheless MET.** *(Found by `50-finish`, 2026-08-20, by running it.)* All three hits are
> **prose** — `:31` and `:42` in the KDoc and `:136` in a comment, each describing the transaction
> that *used to* be there. **Zero are code.** `setDone` has been a single
> `document(taskId).update(…)` since `c20-build-half` (`731961b`), and `ConnectivityMonitor` plus
> the `GoalDetailViewModel` pre-check are deleted (`941d6a8`, #50 item 5). Use instead:
>
> ```bash
> grep -n "firestore.runTransaction" app/…/TaskRepositoryImpl.kt   # code only — must be empty
> ```
>
> Same family as the brief-list rot written up at `kb/dev/decision-map-charting.md` §12b: a
> mechanical check is a grep frozen at authoring time, and this one was authored while the removal
> it tests for was still in the future. **The ordering warning above it is unaffected and still
> binds — #9 first.**

## The three questions the spec already answers — do not re-derive them

1. **What completing awards — §1.4.** `round(minutesOf(task) / 3) × difficulty`, with
   `LIGHT · ROUTINE · DEMANDING` at `×0.75 / ×1.0 / ×1.5`. The multipliers live in the **app**, never
   in the prompt, and **the model never emits a point value** (§3.3 A has no `points` field).
   Completion stamps `minutes` and `difficulty` into a **timestamped completion fact**; the lifetime
   total is a **sum over facts**. So create-and-complete must emit **that same fact** — not a second
   pipe — and the `5..50` cap is deleted.
2. **Whether it moves the goal — §1.5, and only if the edge says so.** `Task.progressContribution`
   moves onto the edge as `goalEdges: [{ goalId, contribution }]`, defaulting to **undefined**.
   Silence is **not** a `1.0`; the shortfall is **disclosed** — *"everything you have planned adds up
   to 3 of 10"*.
3. **A shared task under several edges — §1.5.** `minutes` **divide** (one afternoon happened once),
   `points` are **paid once**, goal progress advances **fully on every edge** by its own
   contribution, and the chart **discloses that it divided**.

## Where it goes

- `feature/dashboard/DashboardScreen.kt` — `SmartAddCard`, the quick-add entry point. **The
  line number is stale since `#6`; grep for `internal fun SmartAddCard`.**
- `feature/goals/GoalDetailScreen.kt:319` — `AddTaskRow`, the other add surface. **Decide
  explicitly whether it gets the affordance too, and say why** — an affordance on one add surface
  and not the other is the kind of asymmetry that reads as a bug.

## The trap

**One code path, not two.** The temptation is `upsertTask()` then `setDone()`. That is two writes,
two failure modes, and a window where the task exists un-completed. §1.4's *"that same fact"* means
the create carries the completion, so a partial failure cannot leave a half-done state.

## Exit

- Tests at every layer that exists — JVM unit for the points/edge arithmetic, instrumented for the
  affordance. Any layer that does not apply, said explicitly.
- **Seen on a device or the cloud emulator.** This is a UI affordance; a green test is not a look.
- `CHANGELOG/<today>/7-quickadd-complete.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. Name whichever of `/kickoff 9-duration-box`, `/kickoff
11-fill-buttons`, `/kickoff 8-notifications` or `/kickoff 6-silent-filing` is still unrun — they are
mutually independent, so say which is cheapest from where you are standing.
