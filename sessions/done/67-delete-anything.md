---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 67
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/DeleteConfirm.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalsScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalsViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailViewModel.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/SuccessFailureRun.kt
  - app/src/main/res/values/components_strings.xml
  - app/src/main/res/values-iw/components_strings.xml
  - app/src/test/java/com/idomarhaim/goalpilot/domain/DeletionReachTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/DeleteAnythingUiTest.kt
  - kb-candidates/2026-08-23-67-delete-anything.md
  - CHANGELOG/2026-08-23/67-delete-anything.md
  - sessions/67-delete-anything.md
created: 2026-08-23 by 64-area-success-failure
done: 2026-08-23 by 67-delete-anything
commits:
  - c11c629  # the reach, the confirm, and two repository fixes
  - ec9996e  # the device run: 15 + 320 instrumented, 6 PNGs
result: |
  Shipped and green. The ticket was LEFT OPEN for one reason: the brief asked for the
  unfiled-task defect to be confirmed on a device end to end, and what ran was the code
  path plus DeletionReachTest, with the device covering the components. The push was HELD
  on precondition 5 -- a live sibling commit sat in the range.

  RESOLVED 2026-08-24 by session `67-close-or-finish`, and #67 IS NOW CLOSED. That session
  had the signed-in account and live Firestore this one lacked, and c11c629 was already
  inside the installed APK, so it needed no build: smart-add produced a genuinely unfiled
  task, `Filed nowhere` caught it, the confirm showed WHAT GOES with no WHAT STAYS block,
  and deleting it removed it with the goal counts unchanged. The push was long since
  carried (tour-refresh's 07c467d). See CHANGELOG/2026-08-24/67-close-or-finish.md --
  including a near-miss on a real goal, which #67's own confirm requirement is what caught.
---

# `#67` — *"I need to be able to delete anything"*

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Ido, 2026-08-23**, answering whether `C19`'s `Let it go` button should exist:

> *"I do, but I need to have the ability to delete anything — `GOALS`, `TASKS`, `MILESTONES`,
> `LIFE_AREAS`."*

⚠️ **COLLIDES with [`#68`](https://github.com/idomarhaim/Android_Final_Project/issues/68) —
these two CANNOT run in parallel.** Checked mechanically against that brief's `owns:` list, not by
eye: both own
**`app/src/main/java/com/idomarhaim/goalpilot/feature/calendar/CalendarScreen.kt`**. That is the
whole overlap — every other path is disjoint — but one shared file is enough, and both also need
the **AVD** and the **Gradle daemon**, which are exclusive singletons whatever the file lists say.

**Take `#68` first.** `#68` builds the per-occurrence action menu on the calendar (it needs one for
*Skip*), and `#67` then adds **one item** to a menu that already exists. In the other order `#67`
invents a delete affordance and `#68` reworks it while building the menu — the same work, done
twice. `#67`'s own first item — the undeletable unfiled task — is on the **dashboard**, not the calendar, so nothing about that defect waits on `#68`.

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67) ·
[`CHANGELOG/2026-08-23/64-area-success-failure.md`](../CHANGELOG/2026-08-23/64-area-success-failure.md)
§ *`Let it go` — Ido answered* — the survey that produced this brief.

## ⚠️ The capability already exists. Do not build a deletion layer.

This is the finding that decides the whole shape of the ticket, and it was measured rather than
assumed:

| Entity | Repository method | Reachable from |
|---|---|---|
| **Goal** | `GoalRepository.deleteGoal` **and** `setArchived` | `GoalDetailScreen` **only** |
| **Task** | `TaskRepository.deleteTask` | `GoalDetailScreen` **only** — and see the defect below |
| **Life area** | `LifeAreaRepository.deleteLifeArea` | the life-**areas list** only |
| **Milestone** | — | **not an entity.** `Goal.declaredBy == null` *is* a milestone (that property's own KDoc), so deleting one is deleting a goal. **Nothing to build.** |

So **the gap is `reach`, not capability**: three deletes exist and each is wired to exactly one
screen. Adding a fourth repository method is the wrong move and would be a second way to do one
thing — §0.3's *second number that quietly disagrees*, in verb form.

## 🐛 The defect this survey found: an unfiled task cannot be deleted at all

`Observed:` mechanically over `app/src/main`, not by eye — `GoalDetailViewModel` reads
`observeTasks(goalId)`, so it lists only tasks **filed under that goal**; `DashboardScreen` and
`CalendarScreen` contain **no delete control at all**.

`Inferred:` from those two facts, a task with **no** goal — which `Task.goalEdges`' KDoc calls
*"unfiled, which is a legitimate state"*, and which smart-add can produce — is listed on no screen
that offers a delete, and so **cannot be deleted from the UI at any point in its life.**

`Untested:` on a device. **Confirm this first**, by creating an unfiled task and trying every
surface, before building anything — if it turns out to be reachable somewhere, the ticket shrinks
and the brief was wrong.

## Task

Give every entity a delete that is reachable **from where the user is looking at it**, using the
repository methods that already exist.

1. **The unfiled task.** The defect above is the ticket's first item and its acceptance test.
2. **One confirm component**, in `ui/components/` — deletion is irreversible and §0.4 forbids the
   app being silent about it. It states **what is about to go and what survives**, in the app's
   own words, with live counts. A goal's deletion is not the same sentence as a life area's: an
   area's deletion **unfiles its goals and keeps them** (`LifeAreaDetailScreen` already says so in
   its empty state), while a goal's takes its edges with it.
3. **`Archive` and `Delete` are different verbs and both must be offered where both make sense.**
   `setArchived` already exists and is the reversible one; `GoalDetailViewModel` calls both today.
   Do not collapse them, and do not make `Delete` the default action of any row.
4. **`C19`'s `Let it go`** — `ui/components/SuccessFailureRun.kt`'s `NoNextStepSection`, whose KDoc
   marks the spot. It is the goal-level instance of item 2 and lands last, once the confirm exists.

## The decisions that are already taken — do not reopen them

- **`Let it go` stays a command, never an inference** (§4.7, `C4`). The button may exist; the app
  may never *suggest* that a goal is over, and no copy anywhere may imply it.
- **A demotion is not a deletion.** §1.1's *lossless demotion* is why `GoalsScreen`'s suggested-goal
  banner has **no** `Delete` — *"the task underneath is real work he typed in"*. That note is
  correct and stays; do not add a delete to that banner.
- **A missed occurrence is never edited — it is history** (§2.3). Deleting a **task** may remove
  its future occurrences; it must not rewrite what already happened, and `#64`'s run counts that
  history. Check what `deleteTask` does to `users/{uid}/occurrences` and to `completionFacts` —
  **if it orphans them, that is part of this ticket.**

## Exit

- **The unfiled-task defect closed**, with the device check that confirms it was real.
- **JVM tests** for whatever decides *what survives a deletion* — that is the part with rules in
  it, and it is where a wrong answer is silent.
- **Instrumented test + render pass** of the confirm component in both themes. §0.8 is suspended,
  so **English only**.
- ⚠️ `adb install -r` + `am instrument`, **never `connectedDebugAndroidTest`** — it uninstalls the
  app and takes the Google sign-in with it.
- `CHANGELOG/2026-08-23/67-delete-anything.md` with counts verbatim.

## Out of scope

- **Any new repository method.** Three deletes exist; this ticket is about reaching them.
- **A milestone entity.** `C16` `#37` is where that lives, if it ever does.
- **Undo / a trash bin.** Not asked for, and it is a storage design of its own. If the confirm
  turns out not to be enough, say so and stop — do not build one.
