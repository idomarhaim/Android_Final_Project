---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 9
created: 2026-08-20
---

# `#9` — the duration box: AI estimate by default, typed value wins forever

> ✅ **DONE 2026-08-20.** Shipped in round 2 of session `9-duration-box`, after round 1 was blocked at
> kickoff by `11-fill-buttons`. Rounds: `48e94bc` (r1, the conflict finding) → `1e42b5d` (r2 claim) →
> **this commit** (r2, the ticket). JVM unit **481/0**, instrumented **105/0**, `assembleDebug` green,
> render pass looked at twice —
> [`docs/render-passes/2026-08-20-9-duration-box/duration-box.png`](../docs/render-passes/2026-08-20-9-duration-box/duration-box.png).
> Account: [`CHANGELOG/2026-08-20/9-duration-box.md`](../CHANGELOG/2026-08-20/9-duration-box.md).
>
> **Everything the ✅ block below predicted held**, including the migration decision. Two things it
> did not predict, both found by looking rather than by testing: the state marker was pixel-identical
> to the AI *button* beside it (13 green assertions, all correct, defect in the relation between two
> nodes), and `TimeAllocation.estimatedTaskCount` would have counted a hand-typed duration as the
> AI's. Both fixed here.

> ⛔ **CORRECTION 2026-08-20 — THIS BRIEF SAID IT COULD RUN IN PARALLEL WITH `#11`. IT CANNOT.**
> The struck line below read: *"Independent of `C20` — can run at any time, in parallel with #11, #8
> or #6. Verified 2026-08-20: no file touching `TaskEstimate` also touches `Goal.unit`, so this and
> `11-fill-buttons` share nothing and will not collide."*
>
> **That grep is true and it is not a conflict test.** It asks whether one file mentions both symbols
> *today*; a conflict is about the edits each ticket is *about to make*. `#11` adds a per-goal input
> mode to `GoalDetailScreen.kt`; `#9` adds the duration box to the same file. Neither symbol ever has
> to appear beside the other. **The brief already applies the right test two paragraphs down** — *"both
> land in `GoalDetailScreen.kt:319` `AddTaskRow`"* is a **file**-level argument, and it was simply not
> run against `#11`.
>
> `11-fill-buttons`'s board row owns `feature/goals/` · `data/firestore/dto/Dtos.kt` ·
> `data/firestore/dto/Mappers.kt` · `data/remote/RecommendationRepositoryImpl.kt` · new suites under
> `app/src/test/` and `app/src/androidTest/` — **and the Gradle daemon.** `Observed:` 2026-08-20,
> `8eb37b9`, with their claim landing between two of this session's tool calls.
>
> **`TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` Wave 3 was right**: `#6 → #7 → #9 → #11` is
> **one working set, strictly sequential**. `505f083`'s *"#9 and #11 turn out to be parallel"* is
> withdrawn. Account: [`CHANGELOG/2026-08-20/9-duration-box.md`](../CHANGELOG/2026-08-20/9-duration-box.md).
>
> **Precondition for the next `/kickoff 9-duration-box`:** `11-fill-buttons` released on the board.
> Check it before the first write, not after.

**Independent of `C20`** — nothing here waits on the Eventarc work.

Needs the **Gradle daemon**; a device or the cloud emulator for the render pass.

> ⚠️ **Runs BEFORE `7-quickadd-complete`. Verified 2026-08-20.** Both land in
> `GoalDetailScreen.kt:319` `AddTaskRow`, which already holds `aiMinutes` at `:332` — *"the AI's
> answer while the title still matches, and a function of the points the moment the user types
> something new"*. That reconstruction **is** what this brief replaces with stored provenance, so
> #7 built first would be built on a value about to be redefined.
>
> ~~Independent of `11-fill-buttons` — different documents, and **no file mentions both**
> (`grep -rl unit … | xargs grep -l looksLikeFallback` returns empty).~~ **Withdrawn — see the
> correction at the top of this file.** Both tickets edit `GoalDetailScreen.kt`.

## Why it exists

`R8`: *"A person may optionally type the work duration into a box, but the default is that the AI
estimates it; there should be an icon inside the box for as long as the person has not entered a
number."*

The data is already there — `Task.estimatedMinutes` is nullable-with-fallback and the LLM fills it.
What is missing is the affordance and, more importantly, **the provenance**.

## The precedence rule — decided, unconditional, and not yours to soften (§1.4)

> **A hand-typed duration is sticky.** The typed number **is** the duration, points recompute from
> it, and **no re-estimation ever overwrites it** — unconditionally, with **no threshold**.

§0.6: a **fact about Ido's life** is his. Any threshold makes the app judge when he is wrong about
his own day. If you find yourself designing a "only re-estimate if the typed value is wildly off"
rule, that is the rule this decision already rejected.

**And it is structural, not a prompt instruction (§3.3 A):** the `estimate` call *"never re-prices a
completed task or a hand-typed duration — those tasks are not in `tasks[]` at all."* So the box is
enforced by **what is sent**, not by a check on what comes back. Build it that way.

## The placeholder icon becomes stored provenance — this is the real change (§1.4)

`domain/model/TaskEstimate.kt` today reconstructs provenance by guessing:

- `:100` `looksLikeFallback(taskTitle, estimate)` — *"evidence, not proof"* by its own KDoc, and
  §0.3's fifth instance of *a second number that quietly disagrees*
- `:110` `private val SERVER_FALLBACK = TaskEstimate(points = 10, minutes = DEFAULT_MINUTES)` — the
  sentinel it compares against

**Both are deleted.** A stored provenance value replaces them, and the icon reads it instead of
recomputing a guess. Check `domain/usecase/BackfillDurationsUseCase.kt`, the other caller, before
you delete.

## What the box shows when there is no estimate (§3.4)

An absent `estimatedMinutes` means **ask the user *how long?***, `DEFAULT_MINUTES` if skipped.
**The app never guesses a duration from a word count.** §0.3's house rule: when a value is unknown
it is **absent** — never a default that looks like an answer.

## The trap

The migration. Existing tasks have no stored provenance, and the honest value for them is
**absent**, not a guessed `SERVER_FALLBACK`. Backfilling them by running `looksLikeFallback` one
last time re-imports the exact guess this ticket deletes. Decide it explicitly and write the
decision down.

---

## ✅ Already derived 2026-08-20 while blocked — do not re-derive, do check it still holds

The blocked `9-duration-box` session could not compile anything, so it spent the wait on the design.
Full reasoning and evidence: [`CHANGELOG/2026-08-20/9-duration-box.md`](../CHANGELOG/2026-08-20/9-duration-box.md).
All of it is `Untested:` — nothing was built or run.

**1 · Provenance is a stored enum, three values.** `enum class DurationSource { USER, AI, UNKNOWN }`
on `Task` (`durationSource: DurationSource = UNKNOWN`) and on `TaskDto` as a string.
`looksLikeFallback` and `SERVER_FALLBACK` are deleted — the repository knows at the point of
production whether a model answered. A fourth `NONE` was considered and collapsed:
`estimatedMinutes == null` already says *no duration*, and two fields that can disagree is §0.3's
defect rather than its fix.

**2 · `TaskEstimate.minutes` becomes nullable.** `null` = the model supplied none (§3.4). `scoreTask`
stops calling `fallbackMinutes(points)` on **both** the missing-minutes path and the `catch` path —
that is the concrete site of *"never guesses a duration from a word count"*. **`points` keeps its
heuristic.** The §1.4 points inversion (`round(minutes/3) × difficulty`, the `difficulty` enum, the
`5..50` cap deletion, completion facts) is `C1` [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19),
**not this ticket** — §1.4 discusses both in one paragraph and only one of them is yours.

**3 · The precedence rule as a pure, JVM-testable object** — this is what makes the Exit's *"both
directions"* test possible with no emulator, and it keeps `AddTaskRow` thin:

```kotlin
data class DurationEntry(val minutes: Int? = null, val source: DurationSource = UNKNOWN) {
    val isTyped: Boolean           get() = source == DurationSource.USER
    val showsEstimateIcon: Boolean get() = !isTyped        // R8, exactly as worded
    fun withEstimate(m: Int?): DurationEntry = if (isTyped) this else …  // unconditional, no threshold
    fun withRetitle(): DurationEntry         = if (isTyped) this else DurationEntry()
    fun resolve(): Pair<Int, DurationSource> = …           // DEFAULT_MINUTES + UNKNOWN when skipped
}
```

It replaces the `aiMinutes` reconstruction at `GoalDetailScreen.kt:332`. Note the consequence worth
watching in the instrumented test: **a retitle no longer clears a typed duration.**
`BackfillDurationsUseCase.invoke` gains an explicit `.filter { it.durationSource != DurationSource.USER }`
— redundant today, but it **is** the rule and §3.3 A wants it structural.

**4 · The migration — decided, on a fact.** **Legacy rows read as `UNKNOWN`, no backfill write runs
at all, and `UNKNOWN` is not sticky.** `Observed:` 2026-08-20 — **no code path lets a person type a
duration today**; every `estimatedMinutes` write under `app/src/main` is model- or fallback-derived
(`Mappers.kt:83,96` · `RecommendationRepositoryImpl.kt:139,207,220` · `AnalyticsViewModel.kt:205` ·
`DashboardViewModel.kt:246,497` · `GoalDetailViewModel.kt:115`), and a grep for a minutes
`TextField`/`onValueChange` across `feature/` and `ui/` returns nothing. So the one value stickiness
protects **provably cannot exist yet**, which is what makes non-sticky `UNKNOWN` safe rather than
merely convenient. The migration is a **read**, not a write.

## Exit

- JVM unit for the precedence rule **in both directions**: a typed value survives a re-estimate,
  **and** an untyped one is still re-estimated. The direction you did not think to write is the one
  the edit broke.
- Instrumented for the icon appearing and disappearing.
- **Seen** — the icon is the deliverable; a green test is not a look.
- `CHANGELOG/<today>/9-duration-box.md` · board row released · brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, one heading. Name whichever of `/kickoff 11-fill-buttons`, `/kickoff
8-notifications`, `/kickoff 6-silent-filing` is still unrun.


---

## 📌 Pointer repair — `#55` built it *(appended 2026-08-21 by session `55-scoring-model`)*

**Appended, not edited.** Everything above is this session's own account of what it did and
is left exactly as it was written; what follows is the one fact a later reader needs and
cannot get from it.

Where this file defers §1.4's points inversion to **`C1` [#19]**, that pointer was already
stale when it was written: #19 is a **decision** ticket, `state_reason: completed`, closed
2026-08-10, and it was never going to build anything. `7-quickadd-complete` found that on
2026-08-20 and [#55](https://github.com/idomarhaim/Android_Final_Project/issues/55) was
opened as the carrier.

**`#55` is now built** — the inversion, the difficulty enum, the deleted `5..50` cap, the
retired `heuristicPoints`, the completion facts and §1.5's `goalEdges`. So the correct
forward pointer from every sentence above that says *"that is #19's"* is **`#55`**, and the
account of the work is `CHANGELOG/2026-08-21/55-scoring-model.md`.
