---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 56
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/TaskEstimate.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/ReminderTiming.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Dtos.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Mappers.kt
  - app/src/main/java/com/idomarhaim/goalpilot/notifications/ReminderScheduler.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BackfillDurationsUseCase.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/TimeAllocationUseCase.kt
  - app/src/main/java/com/idomarhaim/goalpilot/core/util/AnalyticsRange.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailScreen.kt
  - docs/PRODUCT_v0.3.md
  - CHANGELOG/<today>/56-occurrence-model.md
  - sessions/56-occurrence-model.md
created: 2026-08-20
completed: 2026-08-21
commit: c2c9171
---

# `#56` — the occurrence model: §2.2's four rungs and the §2.5 reminders they unlock

**In v0.3.** Ido, 2026-08-20: *"if they're not related to Hebrew, I do want them done now."*

> ⚠️ **RUN THIS AFTER [`55-scoring-model`](55-scoring-model.md).** Both edit
> `domain/model/Task.kt`, `data/firestore/dto/Dtos.kt` and `Mappers.kt` — **one working set,
> strictly sequential.** `#55` is a migration and this one is additive, so `#55` goes first and this
> adds to a settled shape.

> 🔒 **Singletons.** Needs the **Gradle daemon** and a **device** — the reminder has to be seen
> firing. Check the board.

## Read first

1. [AGENTS.md](../AGENTS.md)
2. [`docs/PRODUCT_v0.3.md`](../docs/PRODUCT_v0.3.md) **§2.2, §2.3, §2.5** — §2.2's rung table is
   normative and §2.3's *temporal state is derived, never stored* constrains every choice here.
3. `curl -s https://api.github.com/repos/idomarhaim/Android_Final_Project/issues/56` — the ticket.
4. [`app/src/main/java/com/idomarhaim/goalpilot/domain/model/ReminderTiming.kt`](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/ReminderTiming.kt)
   — **read its KDoc before anything else.** It already states this brief's premise in its own
   words: *"`Task` carries no due date, §2.2's four rungs are …"*.
5. [`kb/index.md`](file:///C:/Dev/JARVIS/kb/index.md) — before the first device command.

## The starting state, which is better than the ticket implies

**`#8` shipped more than the substrate.** Already built and tested, waiting for input:

- `domain/model/ReminderTiming.kt` — the **backward computation** (`dueAt - durationMinutes`), the
  **waking-hours clamp**, `ReminderPlan` carrying `movedForSleep`, and `containsMinuteOfDay`. It
  takes `dueAt` as a **parameter** because nothing supplies one.
- `notifications/ReminderScheduler.kt` — local scheduling.
- The whole notification substrate: channel, `POST_NOTIFICATIONS`, tap-through, in-app half,
  refusal handling.

**So this ticket is mostly about giving that machinery something to read.**

## Task

### 1 · §2.2 — the occurrence model

`Task` (`domain/model/Task.kt`) carries **no due date** and no rung. Add them, honouring §2.3:
**temporal state is derived, never stored** — store the occurrence, derive `OVERDUE` and everything
else at read time.

The four rungs and what a **miss** means, from §2.2 — this table is the spec, not a suggestion:

| Rung | What it is | A miss means |
|---|---|---|
| `ALL_DAY` | a day with no slot | the day passed |
| `DEADLINE` | a moment you owe something by | **late, still owed** |
| `BLOCK` | a span of time you are inside | the slot is gone |
| `SPAN` | *(read §2.2 — do not infer it from the other three)* | |

⚠️ `core/util/AnalyticsRange.kt` also contains the tokens `ALL_DAY`/`BLOCK`. **It is a different
concept** — an analytics window, not a task rung. Do not unify them; check before reusing a name.

### 2 · §2.5 — the reminders, which are the product's differentiator

- **One reminder per occurrence, timed per rung.**
- **The deadline's is computed backwards** from `minutesOf(task)`, clamped to waking hours, and
  **says why it moved** — *"due at 06:00 and it takes about 4 hours — worth starting tonight."*
  §2.5 calls this **"the one thing this app knows that Google Calendar does not"**, so the copy is
  part of the deliverable, not decoration.
- **A reminder re-checks at fire time** whether it is still needed — free, precisely because
  nothing is stored.
- **Misses meet Ido once, in a daily review on app open — never as a push saying he failed.**
  `OVERDUE` is the one state that keeps reminding.

## Carries over

- **`#8` is closed and its close comment is the contract.** It says the substrate is complete and
  names exactly what was held. If you find a substrate piece missing, that is a **defect in `#8`**
  worth saying out loud, not a silent addition here.
- **`minutesOf` already has six call sites** (`Task.kt`, `TaskEstimate.kt`,
  `BackfillDurationsUseCase.kt`, `TimeAllocationUseCase.kt`, `GoalDetailScreen.kt`,
  `ReminderTiming.kt`). `#9` made a typed duration **sticky** — a re-estimate must not overwrite it.
  A reminder computed from a duration the user typed must use **their** number.
- **`#55` will have just changed `Task`, `Dtos` and `Mappers`.** Re-read all three at `HEAD`; do not
  work from this brief's description of them.
- **Device-state claims in this brief rot fast** — `kb/dev/android-device-verification.md` §10.
  Re-read the device before relying on anything about its state, and record the delta.

## Out of scope

- **§1.4 / §1.5** — that is [`55-scoring-model`](55-scoring-model.md).
- **A calendar UI.** The rungs are a domain model plus reminders; a full scheduling surface is not
  in this ticket and needs its own decision.
- **Anything server-pushed.** §2.7: there is no credential for a background sync and cannot be one.
  Local scheduling only.
- **Hebrew.** `#51` stays frozen.

## Exit

- **JVM unit for each rung's miss semantics** — four rungs, four different meanings of a miss, and
  the one you skip is the one that breaks.
- **JVM unit that the deadline reminder is computed backwards and clamps to waking hours**, using
  `ReminderTiming`'s existing arithmetic rather than a second implementation. **If you write a
  second one, that is the defect.**
- **JVM unit that a reminder re-checks at fire time** and does not fire for a task already done.
- **JVM unit that a typed duration drives the reminder**, not a re-estimate — `#9`'s stickiness,
  end to end.
- **Instrumented** for the daily miss review appearing on app open and **not** as a push.
- **Seen** — a reminder actually firing on a device, with the *"worth starting tonight"* copy
  legible in the notification. This is the differentiator; a green test is not a look.
- `CHANGELOG/<today>/56-occurrence-model.md` · board row released · brief closed to
  `sessions/done/` with `status: done` in the same commit · **close `#56` with the evidence table**
  · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

If `#48`, `#53`, `#54` and `#55` have all landed, say that **v0.3 is feature-complete except `#51`
(frozen)** and name the Wave 4 verification pass as the next step. If any Exit item is owed, say
which — an owed **seen** is not the same as an owed test.
