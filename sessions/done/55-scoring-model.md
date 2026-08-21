---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 55
created: 2026-08-20
---

# `#55` — the scoring model: §1.4's points inversion and §1.5's `goalEdges`

**In v0.3.** Ido, 2026-08-20: *"if they're not related to Hebrew, I do want them done now."* This is
not Hebrew-related; `#51` is the only deliberate v0.3 cut.

> ⚠️ **RUN THIS BEFORE [`56-occurrence-model`](56-occurrence-model.md).** Both edit
> `domain/model/Task.kt`, `data/firestore/dto/Dtos.kt` and `Mappers.kt`. **They are one working set
> and cannot be parallelised.** This one is a **migration** — it deletes a field and moves where a
> fact lives — and `#56` is **additive**, so landing the migration first means `#56` adds to a
> settled shape instead of being carried through it.

> 🔒 **Singletons.** Needs the **Gradle daemon**; a **device** for the render pass. Check the board —
> `c12-material-contract` and then `c13-key-store` hold both. Do not start until they release.

## Read first

1. [AGENTS.md](../AGENTS.md)
2. [`docs/PRODUCT_v0.3.md`](../docs/PRODUCT_v0.3.md) **§1.4 and §1.5** — and read §1.4's status box
   at the top **first**: it is a clause-by-clause audit of this ticket against `HEAD`, done
   2026-08-20, and every ❌ in it was still ❌ when this brief was written. It is the most accurate
   description of the starting state that exists.
3. `curl -s https://api.github.com/repos/idomarhaim/Android_Final_Project/issues/55` — the ticket.
4. [`kb/index.md`](file:///C:/Dev/JARVIS/kb/index.md) — before the first device command, per
   `rules/memory-promotion.md` § *The withdrawal side*.

## Task

**Two halves of one model change. Do them in one session — splitting them leaves the app in a state
where points and contribution disagree about where a fact lives.**

### 1 · §1.4 — the points inversion

| Change | Where it is now |
|---|---|
| `points = round(minutesOf(task) / 3) × difficulty` | no `difficulty` enum exists anywhere in `app/src/main` |
| `difficulty ∈ LIGHT · ROUTINE · DEMANDING` at `×0.75 / ×1.0 / ×1.5`, **multipliers in the app, never in the prompt** | — |
| the `5..50` cap **deleted**, ceiling 50 → 240 | `TaskScoring.MIN_POINTS`/`MAX_POINTS` in `domain/model/TaskEstimate.kt` |
| `heuristicPoints` (`5 + 3×words`) **retired** | alive, **two** call sites in `data/remote/RecommendationRepositoryImpl.kt` |
| points banked as a **timestamped completion fact**; lifetime total is a **sum over facts** | no `completionFacts` collection — the fact is `isDone` + `completedAtEpochMillis` on the task document |
| `GoalProgress.points` **deleted** | `domain/model/ProgressSummary.kt`, consumed by `domain/usecase/SummaryUseCase.kt` |

**The model never emits a point value** — §3.3's `A` has no `points` field. If the prompt or the
parser can produce one, that is the defect.

### 2 · §1.5 — `goalEdges`

Delete `Task.progressContribution` (`domain/model/Task.kt:15`) and move contribution onto the edge:
`goalEdges: [{ goalId, contribution }]`, defaulting to **undefined**. An edge declares its
contribution **in the objective's own word**, or contributes nothing to the measure.

Live call sites to migrate: `data/firestore/TaskRepositoryImpl.kt`, `data/firestore/dto/Dtos.kt`,
`data/firestore/dto/Mappers.kt`, `domain/model/DerivedProgress.kt`, `domain/model/Goal.kt`,
`feature/goals/GoalDetailViewModel.kt`.

## Carries over

- **`#7`'s create-and-complete path must keep working and must not become a second pipe.** `#7`
  shipped on the reading that §1.4's clause binding it is about **plumbing** — *emit that same fact*
  — and it emits `done` + `completedAt` through the one write everything uses
  (`CHANGELOG/2026-08-20/7-quickadd-complete.md` § *Deliberately not built*). When the fact moves to
  `completionFacts`, **that path moves with it**. If you find yourself adding a second write for
  quick-add, stop: the design is wrong, not `#7`.
- **`C20` already removed the running accumulator.** §1.4 cites `TaskRepositoryImpl.kt:120-127` as a
  live defect losing 30 points on an untick. It was fixed in `731961b`
  ([#42](https://github.com/idomarhaim/Android_Final_Project/issues/42)); `setDone` is a single
  `update()` and the total is summed by `functions/src/projection.ts`. **Do not "fix" it again** —
  and check whether the projection function needs to change with the fact's new home.
- **Three sessions deferred this work to closed `C1` #19** (`9-duration-box` ×2, `11-fill-buttons`,
  `#7`'s brief) and `domain/model/TaskEstimate.kt`'s KDoc still points there. **Repair those
  pointers to `#55` as you go** — a stale pointer to a closed ticket is what created this whole
  situation.
- **This is a data migration**, not a late-stage edit. Existing task documents carry
  `progressContribution` and `points`; decide and state the read-path story for documents written
  before this change, and test both directions.

## Out of scope

- **§2.2 / §2.5 — the occurrence model.** That is [`56-occurrence-model`](56-occurrence-model.md).
  If you find yourself needing a due date, you are in the wrong brief.
- **Hebrew.** `#51` is frozen; do not un-freeze anything.
- **`#48`'s two holes.** `c12` and `c13` own those.

## Exit

- **JVM unit for the points formula at every difficulty**, including the boundary the old cap used
  to clamp — a 240-point task must be expressible, and a 4-point one must not be raised to 5.
- **JVM unit for the migration, both directions**: a document written before this change reads
  correctly, and a document written after it does not lose contribution.
- **JVM unit that the lifetime total is a sum over facts** and that an untick removes exactly the
  fact it added — the defect `C20` fixed must not return by a different route.
- **A stated, checked answer to "can the model emit a point value?"** — name what you grepped in the
  prompt and the parser, not just that you looked.
- **Instrumented** for a completion writing a fact and the total moving.
- **Seen** — complete a task on a device and watch the number change. A green test is not a look.
- `CHANGELOG/<today>/55-scoring-model.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · **close `#55` with the evidence table** (`/kickoff` §5
  step 4) · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Say whether `56-occurrence-model` may now start, and name it as the next step. If any part of the
migration was left half-done, say **which document shape is now in the database** — that is the one
fact the next session cannot re-derive.
