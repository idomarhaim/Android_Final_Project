# 55-scoring-model — §1.4's points inversion and §1.5's `goalEdges`, as one migration

> **Summary:** `#55` ships. Points stop being a stored number and become a **view of effort** — `round(minutes / 3) × difficulty`, with `LIGHT · ROUTINE · DEMANDING` at `×0.75 / ×1.0 / ×1.5` and the multipliers in the app, never in the prompt. The `5..50` cap is deleted at **both** ends (a 15-minute light task is worth 4, an eight-hour one 160), `heuristicPoints` (`5 + 3×words`) is gone with no offline substitute, and a completion is now its own document at `users/{uid}/completionFacts/{taskId}` **banking its inputs** so a later re-estimate cannot re-price history. §1.5's `goalEdges` replaces `Task.progressContribution`: contribution belongs to the *pair*, and an edge that declares nothing adds nothing. The model can no longer emit a point value — four sites closed, in the prompt, the validator and both client parsers. **No backfill:** a legacy point value `p` reconstructs to `3p` minutes at `ROUTINE` and prices back at exactly `p`, so the migration is lossless by arithmetic. JVM **646/0** (+29), instrumented **177/178** (the one failure unrelated and flaky — passes 13/13 in isolation), functions `node --test` **67/0** (+4), rules **44/0** (+3), device pass on `emulator-5554`. **Two findings beyond the ticket:** a second reader of the tasks collection had no fact join and made a task done on one screen and open on another — found by *looking*, not by a test; and **the Cloud Functions must deploy before this app build reaches a device**, observed as a live total falling 70 → 40.

**Session:** `55-scoring-model` · **Ticket:** [#55](https://github.com/idomarhaim/Android_Final_Project/issues/55) · **Brief:** [`sessions/55-scoring-model.md`](../../sessions/55-scoring-model.md) · **Mode:** AUTO

---

## 1 · What shipped

### §1.4 — the points inversion

| Change | Where it landed |
|---|---|
| `points = round(minutes / 3) × difficulty` | `TaskScoring.pointsFor`, `domain/model/TaskEstimate.kt` |
| `Difficulty ∈ LIGHT · ROUTINE · DEMANDING` at `×0.75 / ×1.0 / ×1.5` | `domain/model/Difficulty.kt` *(new)* |
| the `5..50` cap **deleted**, ceiling 50 → 240 | `MIN_POINTS`/`MAX_POINTS` gone; 240 is *yielded*, not written |
| `heuristicPoints` (`5 + 3×words`) **retired** | deleted; both call sites in `RecommendationRepositoryImpl` closed |
| points banked as a **timestamped completion fact** | `CompletionFact` + `users/{uid}/completionFacts/{taskId}` |
| `GoalProgress.points` **deleted** | replaced by `effortMinutes` (`ProgressSummary.kt`, `SummaryUseCase.kt`) |
| the model never emits a point value | `functions/src/index.ts`, `classify.ts`, `RecommendationRepositoryImpl` |

### §1.5 — `goalEdges`

`Task.progressContribution` is deleted. `Task.goalEdges: List<GoalEdge>` carries
`{ goalId, contribution }` with contribution **nullable and defaulting to absent**, and
`DerivedProgress` adds nothing for an edge that declares nothing.

`Task.goalId` survives as a **derived property** (`goalEdges.firstOrNull()?.goalId`) so a
dozen readers did not have to learn about edges at once; `TaskDto.goalId` survives as a
**stored projection**, rewritten from the edge list on every write, because Firestore cannot
filter an array of maps on one member's key.

---

## 2 · The three decisions worth arguing with

### 2.1 The completion is its own document, and the tick is still one commit

§1.4 banks the completion's **inputs**. They cannot live on the task, because `upsertTask` is
a whole-document `set()` and an ordinary retitle would overwrite them — retitle a completed
task and its history gets re-priced at today's estimate.

That makes a completion two documents, and §1.4 is explicit that a create-and-complete must
*"emit that same fact, not a second pipe"*. Both hold, via a **`WriteBatch`**: one commit, one
failure mode, no window in which half the fact exists. A batch is applied to the Firestore
cache synchronously exactly as a single write is, so `C20`'s offline win is untouched — the
KB's own `firestore-write-semantics.md` says a batch *"keeps the offline cache"* where a
transaction does not. And there is **one minting function**, `TaskCompletion.of`, called by
the tick and by the born-done create with the same arguments: the pipe is single because the
*fact* has one author, not because the write has one document.

**`isDone` is now `completion != null`.** The half-written fact `#7` documented at length —
`done` with no stamp, read by four consumers that disagreed about which field was the fact —
has no representation, so `TaskCompletion.stamp` is deleted rather than rewritten. Most of
`TaskCompletionTest` went with it; what is left asserts minting, unrepresentability, and the
real downstream use cases.

### 2.2 A legacy `progressContribution` is read verbatim, not as a silence

§1.5 calls `progressContribution`'s `1.0` *"a silence, not a value"*, and new edges therefore
declare nothing. **Stored numbers are read at their stored value anyway**, and that is a
decision this session took rather than one §1.5 states:

- re-reading every stored `1.0` as `null` would zero the task half of **every existing goal's
  progress**, with no user action and no way back — a data-destroying migration, and
  deletions are always-ask;
- the undeclared default governs what a **new** edge gets when nothing declares one, which is
  what the sentence is actually about.

`TaskDto.progressContribution` is `Double?` **defaulting to null** for exactly this: it is the
only way to tell a document that stored `1.0` from one that never had the field.

**The visible consequence is real and is the spec's answer, not a defect:** a task created
after `#55` moves no measure when ticked, because nobody declared what it is worth in the
objective's own word. §1.5's shortfall disclosure is the other half of that sentence and is
**not built** — no ticket owns it.

### 2.3 No backfill, because the arithmetic is lossless

A task completed before `#55` carries `done` + `completedAt` + `points` and no fact. It is
read into an equivalent fact at the mapper, and the reconstruction is **exact**: a legacy
point value `p` becomes `3p` minutes at `ROUTINE`, which prices back at
`round(3p / 3) × 1.0 = p`. So the migration moves where the fact lives without re-pricing a
single historical completion, and needs no write over anybody's data.

The old `TaskDuration.fallbackMinutes` survives as `legacyMinutesFromPoints`, **deliberately
unclamped** — clamping would break that identity at both ends and silently re-price the
oldest and largest tasks, which is the one thing it exists to avoid.

The projection function reads the **union** of banked facts and un-migrated legacy tasks,
keyed by task id so a re-ticked task is counted once, from its fact.

---

## 3 · Can the model emit a point value? — checked, not assumed

§3.3 A says *"there is no `points` field, and there never will be."* **That was false at
`HEAD` in four places.** What was grepped, and what each now says:

| Site | Was | Now |
|---|---|---|
| `functions/src/index.ts` `scoreTask` prompt | *"Estimate difficulty points (integer 5-50)"*, returning `{points, minutes}` | asks for `{difficulty, minutes}`; `difficulty` is one of three declared words |
| `functions/src/index.ts` `classify` prompt | declared `"estimatedPoints":number` and *"estimatedPoints is 5-50 by difficulty"* | declares `"difficulty":string` |
| `functions/src/classify.ts` `validateClassification` | `asInt(m.estimatedPoints)` admitted in `5..50` | `asMember(m.difficulty, DIFFICULTIES)`; a response smuggling `estimatedPoints` or `points` back in does not survive — asserted |
| `RecommendationRepositoryImpl` | `data["points"]` in `scoreTask`, `m["estimatedPoints"]` in `parseClassification` | both read `difficulty` |

**One `points` string survives in a prompt and is correct:** `index.ts:221` puts
`Total points: ${totalPoints}` into the **daily feed** prompt as *context about the user*. It
asks for nothing back — schema C has no numeric field at all.

The failure mode narrowed with the contract: an unparseable difficulty is `ROUTINE`, which is
`×1.0` and therefore prices the task on its minutes alone — the **absence** of a judgement
rather than a guess at one. `classify.ts` **omits** an unusable difficulty while `asDifficulty`
(used by `scoreTask`, whose whole response *is* the estimate group) **substitutes** `ROUTINE`;
the two differ deliberately and both are asserted.

---

## 4 · The two findings beyond the ticket

### 4.1 `defect` — a task done on one screen and open on another *(found, then fixed)*

Two repositories read the tasks collection: `TaskRepositoryImpl` for the task list and
`GoalRepositoryImpl` for the completed-task half of a goal's derived progress. **The fact join
went into the first one only.**

`Observed:` on `emulator-5554`, 2026-08-21 — a completed task re-ticked after the migration
showed its checkbox ticked and its `+35` in the task list, while the goal ring above it read
**0% / 0 of 100** where it had read **1% / 1 of 100** a moment earlier.

**No test caught this, and none could have**, because both suites were green: the JVM tests
exercise `DerivedProgress` with tasks handed to them directly, and the instrumented tests
never construct a second repository over the same data. It was found by *looking at the
screen*, which is exactly what the render-pass duty is for.

The fix is not a second copy of the join — a copy is two things to keep in step, which is the
same shape as the defect. `data/firestore/TaskStream.kt` *(new)* is the one seam that turns a
task document into a `Task`, and both repositories go through it. Re-verified on the device:
the goal is back at **1% / 1 of 100**, and every other goal matches the pre-session baseline.

### 4.2 ⚠️ `blocked` — the Cloud Functions must deploy before this app build ships

The lifetime total is written by `functions/src/projection.ts`, never by the device. The
**deployed** projection triggers on `users/{uid}/tasks` and sums `points` over documents
where `done === true` — and the new client writes neither field.

`Observed:` 2026-08-21 against the live `goalpilot-56e30`, signed in as
`name.iddo@gmail.com` on `emulator-5554`. Completing one task took the stored total from
**70 → 40**, where the shipped-together pair would have given **70 → 115**. Screenshots:
`dash1.png` (70, baseline) and `total2.png` (40) in this session's scratchpad.

**It repairs itself on deploy.** `pointsFromFacts` reads the union of both shapes and the
projection is structurally idempotent — it recomputes from the whole fact set on the next
write, so the number returns to 70 without any data repair. **Not deployed:** outward-facing,
and Ido's word is the gate (`rules/outward-action-governance.md`). The command, and this
machine's discovery-timeout trap, are in `CLAUDE.md`.

**Live data touched, and its exact state:** one existing task —
`להגיש פרויקט גמר בפיתוח אנדרואיד`, under *Submit Android final project* — was unticked and
re-ticked during the device pass (a mis-aimed tap, then its repair). It is **complete again**,
worth the same `+35`, its goal reads 1% as before, and it is now **migrated**: `done` and
`completedAt` cleared, a `completionFacts` document in their place. Nothing else was altered;
the probe task created for the pass was deleted.

---

## 5 · 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **646 / 0** (+29 over 617) |
| **Instrumented** (`am instrument`, `emulator-5554`) | **177 / 178** — see below |
| **Cloud Functions** (`node --test`) | **67 / 0** (+4) |
| **Security rules** (`firestore-tests`, emulator) | **44 / 0** (+3) |
| **Build** (`assembleDebug`, `assembleDebugAndroidTest`) | green |
| **Device pass** | done — see §4 |

**New suites:** `TaskScoringTest` (the formula at every difficulty, both deleted cap ends, the
rounding order, the sum-over-facts property), `TaskScoringMigrationTest` (both directions of
the data migration).
**Rewritten:** `TaskCompletionTest` (the invariant became unrepresentable, so most of it was
deleted rather than ported), `DerivedStateFixtureTest` (now calls the production
`TaskScoring.pointsFor` rather than reimplementing the rule).
**Shared fixture:** 8 new cases in `shared-fixtures/derived-state.json`, read by both
languages — including the two migration cases that decide whether a deploy preserves history.

**The one instrumented failure is unrelated and flaky.**
`AiSectionUiTest.replacingTheKeyChangesTheMaskedTail` (BYO-key masking, `c13` territory) failed
in the full run and **passed 13/13 in isolation**; its source is untouched by this change. A
*different* unrelated test — `NotificationObservedFireTest` — failed on the previous full run
and passed on this one, also passing in isolation. Two different tests failing on two
different full runs and both passing alone is full-suite flakiness, not a regression.
`Untested:` what causes it; that is not this ticket's.

**Two of my own arithmetic errors were caught by the suites rather than by me**, both in
fixture expectations I wrote: `60 min` across three difficulties is 65, not 90, and 240 points
is level **2**, not 3.

**And one real defect was caught by a test I had just written** — `TaskCompletion.of` used
`task.completion ?: …`, which returned `#7`'s born-done **placeholder** untouched and banked a
completion stamped at the epoch. That is precisely the failure the object exists to prevent,
arriving through the new shape; the check is on the timestamp now, not on nullness.

---

## 6 · Files

**New:** `domain/model/Difficulty.kt` · `data/firestore/TaskStream.kt` ·
`app/src/test/.../domain/TaskScoringTest.kt` · `app/src/test/.../data/TaskScoringMigrationTest.kt` ·
this file

**Domain:** `Task.kt` (rewritten) · `TaskCompletion.kt` (rewritten) · `TaskEstimate.kt` ·
`DerivedProgress.kt` · `ProgressSummary.kt` · `Goal.kt` · `Recommendation.kt` ·
`usecase/SummaryUseCase.kt`

**Data:** `firestore/TaskRepositoryImpl.kt` · `firestore/GoalRepositoryImpl.kt` ·
`firestore/dto/Dtos.kt` · `firestore/dto/Mappers.kt` · `remote/RecommendationRepositoryImpl.kt` ·
`core/util/Constants.kt`

**Feature:** `dashboard/DashboardViewModel.kt` · `dashboard/DashboardScreen.kt` ·
`goals/GoalDetailViewModel.kt` · `goals/GoalDetailScreen.kt`

**Functions:** `src/index.ts` · `src/classify.ts` · `src/derived.ts` · `src/projection.ts` ·
`test/classify.test.mjs` · `test/projection.test.mjs`

**Tests:** 9 existing JVM suites · 4 instrumented suites · `firestore-tests/rules.test.mjs` ·
`shared-fixtures/derived-state.json`

**Docs:** `docs/PRODUCT_v0.3.md` (§1.4 and §1.5 status boxes) ·
`CHANGELOG/2026-08-20/9-duration-box.md` and `sessions/done/9-duration-box.md`
(pointer repairs, **appended** not edited) · `SESSIONS.md` · `sessions/55-scoring-model.md`

**Not touched:** `firestore.rules` itself — the recursive `users/{uid}/{document=**}` wildcard
already covers `completionFacts` under `isOwner`, and the three new rules tests exist to catch
a future narrowing of it. `TODO/`, `notifications/`, anything Hebrew (`#51` stays frozen).
