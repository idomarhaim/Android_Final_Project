# `c3-points-currency` — are task points and goal progress one currency or two?

**Session:** `c3-points-currency` · **Invocation:** `/wayfinder 12` *(bare — no ticket
named)* · **Branch:** `feat/goalpilot-implementation` · **Mode:** `AUTO MODE` ·
2026-08-10.

One ticket, which is the skill's limit. This map ships no code.

## Session hygiene, before the first unit of work

| | |
|---|---|
| `kb-candidates/` listed | **2 files, unchanged since `c9b-calendar-surface` re-confirmed them** — [`2026-08-09-c9f-consent-screen-state.md`](../../kb-candidates/2026-08-09-c9f-consent-screen-state.md) entry 1 and [`2026-08-10-c16-milestone-model.md`](../../kb-candidates/2026-08-10-c16-milestone-model.md) entry 2. Both `rules/`-destined, so `/kb-ingest` may not take them in **either** mode. Not a backlog; they wait on Ido and on `/walkthrough`, not on a session |
| `SESSIONS.md` read | **Two live claims** — `c9b-calendar-surface` (#26) and `c9c-calendar-sync` (#27). Both confirmed still open **and assigned on GitHub**, so neither row was stale. Claimed before the first write |
| Template parity | `Update-TemplateConsumers.ps1 -Check` → **49 files already current, 0 to upgrade** in this repo (`AGENTS.md` was bumped v15 → v16 earlier today by `c17-many-to-many`). Three files in `C:\Dev\FP_DEMO` still report **BLOCKED** (dirty tree) — unchanged from `c17`'s report, and Ido's to decide |
| Singletons | **None taken.** A decision ticket ships no code: no Gradle, no `adb`, no Firebase, no GROQ call |

## The frontier, re-derived rather than trusted

`/wayfinder 12` arrived bare, so the ticket is the session's to pick — the skill's
*"without one, you pick the next decision, not the user"*. Queried out of GitHub rather
than read off the board: **every** open child of #12 run through the native dependency
relation, `blocked_by` per ticket.

| Open child | Blocked by | Assigned | On the frontier? |
|---|---|---|---|
| **#18 `C3`** | #13 ✅ #37 ✅ #38 ✅ | — | **yes** |
| **#39 `C18`** | #37 ✅ | — | **yes** |
| #26 `C9b` | — | `idomarhaim` | claimed |
| #27 `C9c` | — | `idomarhaim` | claimed |
| #19 `C1` | #18, #39 | — | blocked |
| #20 `C2` | #19 | — | blocked |
| #21 `C5` | #13 ✅, #18 | — | blocked |
| #22 `C6` | #19, #18 | — | blocked |
| #23 `C14` | #14 ✅, #18 | — | blocked |
| #24 `C8` | #13 ✅, #19, #37 ✅ | — | blocked |
| #28 `C9e` | #27 | — | blocked |
| #30 `C11b` | #19, #20, #24, #29 ✅ | — | blocked |
| #31 `C12` | #18, #14 ✅ | — | blocked |
| #35 `C15b` | #24, #29 ✅ | — | blocked |

Two takeable. Took **#18** on leverage: it gated five tickets directly and nine
transitively, and `C17` closing an hour earlier is what freed it. The board's own
recommendation said the same, arrived at independently.

**Board corrected while claiming** — the *Unclaimed work* block still advertised #18 as
takeable and counted two in-flight tickets where there are three.

## What changed

| | |
|---|---|
| Resolved | [#18 · `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18) — closed, full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*. Body **re-fetched immediately before the write**, which mattered: `c9c-calendar-sync` had appended its own `C9c` line in the interval, and a blind write would have dropped it |
| Tickets created | **none** — every hand-off landed on a ticket that already existed |
| Hand-offs commented | [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21) `C5` (recurring work cannot sit in a denominator) · [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23) `C14` (points are effort, not a score) · [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) `C12` (a broken mean, and where each number may render) |
| Unblocked | **#21 `C5`**, **#23 `C14`**, **#31 `C12`** |
| Frontier now | **[#39 `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39) alone**, plus #26 and #27 in flight |

## How the question was put, and how it came back

Ido was asked in the picker on **two axes** — *what points ARE relative to progress*
(three options), and *what a goal at 130% should do* (three options). He answered both
identically: he could not read the options, asked for a **simple, schematic**
explanation, and **handed the choice back** with his standing instruction — *take the
highest-quality answer for the app, its purpose, the UX/UI and the software, and if you
can improve it, improve it.*

So the pick is the agent's and on the record, exactly as in `C17`. Per the
question-axis rule, the second attempt was **not** a longer explanation of the same
options — it was the decision plus a schematic.

**And the reduction the rule asks for found something.** The rule's *"check the fork is
real — grep for a write path between them before drafting the options"* step was run
too late: it was run on `Task.points` ↔ `Goal.currentValue`, found only
`progressContribution`, and concluded the fork was real. It is not. Running it one hop
wider — on `points` ↔ `estimatedMinutes` — turns up
[`TaskEstimate.kt:40`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/TaskEstimate.kt#L40),
and that changed the answer. **The picker's three options all inherited a false
premise**, which is precisely the failure the rule predicts, and Ido's *"I couldn't
understand the options"* is the tell it names.

## The decision

**Two quantities — effort and outcome — and `points` is not one of them.**

```
                      ┌─────────────────────────────────────────┐
                      │  minutes  ── the only FACT of the three  │
                      │  model estimates · USER overrides (#9)   │
                      └──────────────┬──────────────────────────┘
                                     │
                  ┌──────────────────┴──────────────────┐
                  ▼                                     ▼
        points = minutes/3 × difficulty        work-underneath progress
        "what the effort was worth to YOU"     = minutes done / minutes total
                  │                                     │
                  └── belongs to the PERSON             └── belongs to the OBJECTIVE
                      (ledger · level · leaderboard)        only when it has NO measure

        ── and entirely separate from both ──
        measured progress = (current − start) / (target − start)
        "how far the OBJECTIVE moved"          ← the objective's own measure (C7)
```

Effort and outcome may disagree, and that is the app's most valuable signal: you can run
every day and lose no weight. What must stop is presenting **reward** as if it were a
rival opinion about **outcome**.

### The finding that reframed the ticket

The ticket's body says *"the only bridge is `progressContribution`"*. False — and the
existing bridge runs the wrong way:

```
TaskScoring.heuristicPoints(title)   = 5 + 3×words          → points
TaskDuration.fallbackMinutes(points) = points × 3            → minutes
TaskDuration.minutesOf(task)         = estimatedMinutes ?: fallbackMinutes(points)
```

On every offline task — a **first-class path** guaranteed by spec §8, not an edge case —
the app invents a **reward** number from a **word count** and derives **how long your
life took** from it. `C17`'s time-allocation chart, the app's central picture of where a
life goes, is therefore downstream of a gamification currency.

So the fix **inverts a constant rather than adding one**.

### The eight parts

| # | | |
|---|---|---|
| 1 | **Points respecified** | `round(minutesOf(task) / 3) × difficulty`, difficulty ∈ `ROUTINE ×1 · FOCUSED ×1.5 · DEMANDING ×2` — the model's single enum choice. `C7`'s *the AI judges, the app computes*, reused unchanged; `C11a` priced it (free numbers swing **2×**, prompt-declared enums were **50/50**). Divisor is the constant already in the code, read backwards, so today's anchor survives exactly: 30 min routine = **10 points**. The `5..50` cap **goes** — it made an eight-hour task worth the same as a ninety-minute one |
| 2 | **`R12`'s book** | It is `C16` §4 clause 2 (*no target → how much of the work underneath is done*), and this ticket supplies the missing **weight: `minutes`** — the only candidate **conserved under splitting**. `C16` killed count-weighting with that exact test (60 min → 3 × 20 min drops 33%→25%); **points fail it too**, via the difficulty multiplier |
| 3 | **Progress gets an origin** | `(current − start) / (target − start)`. Closes `C7`'s named hole with **no `DIRECTION` enum** — *"lose 5 kg"* is expressible because the missing field was an **origin**, not a direction — fixes *"read 12 books"* when three were already read, and lets progress **fall**, which today it structurally cannot |
| 4 | **`progressContribution`'s `1.0`** | Never a value — a **silence**. Exactly `C7`'s `unit = "%"` disease: *the lazy path produced a goal that measured nothing while claiming to measure something*. On the edge (`C17`) it defaults to **undefined**: an edge declares its contribution in the objective's own word or contributes **nothing**, and the shortfall is **disclosed** (`C16` §4's *"everything you have planned adds up to 3 of 10"*). **No mixing** — measure or work-underneath, never an average of both |
| 5 | **Past 100%** | Legal and shown. **Three clamps, not two** — [`Goal.kt:34`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt#L34) (`C4`), [`TaskRepositoryImpl.kt:139`](../../app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt#L139) (`C17` §7), and **[`GoalDetailViewModel.kt:275`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailViewModel.kt#L275), found here.** Its own comment is why it cannot be left behind. **Past the target the app stops speaking in percent** and uses `C7`'s word — *`beat it by 1.5 kg`* — since `130%` of a loss parses for nobody |
| 6 | **`C9a`'s occurrences** | completed → moves progress, paid **once** (`C17` §2). **`OVERDUE` stays in the denominator**, which makes *late is not failed* true **arithmetically** and not just in wording. **`MISSED` and `EXPIRED` leave it.** Structural reason: **a recurring task generates unbounded occurrences, so `done/total` never converges** |
| 7 | **`Task.isDone`** | Splits three ways on `C9a`'s derive-don't-store precedent: **stored** with no occurrences · **derived** with them · **absent** on a recurring task. Harmless only because of §6 |
| 8 | **Where numbers may render** | **Points are never a property of an objective.** [`GoalDetailScreen.kt:154`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailScreen.kt#L154)/[`:435`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailScreen.kt#L435) put a goal % and `+40 pts` on one screen with no stated relationship, and [`SummaryUseCase.kt:41-42`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SummaryUseCase.kt#L41-L42) **publishes** it — `fraction` and `points` on the same per-goal object in the **shared** §7 summary. `GoalProgress.points` goes; the goal header's companion number becomes **effort** (*"4h 20m of work logged toward this"*) |

### The improvements, separated from the choice

Ido asked for improvement on top of the pick. Five, each traceable to something already
committed rather than invented here:

1. **Inverting `points ← minutes` instead of leaving them independent** — makes points
   *reproducible* (today a re-run can silently re-price yesterday by 2×) and makes them
   inherit Ido's own override from [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9).
2. **One free number per `scoreTask` call instead of two** — strictly less noise at
   identical cost, inside `C11a`'s *one wide call* rule. Also **simplifies
   `looksLikeFallback`**, whose second branch exists only to catch a two-number fallback.
3. **Killing `heuristicPoints`** — a word count is not evidence of effort. Offline
   becomes `DEFAULT_MINUTES` at `ROUTINE`: an honest *"I don't know"* rather than a
   confident wrong answer.
4. **Speaking in the goal's own word past the target** — pure subtraction, no model, and
   the one place a percentage loses information rather than compressing it.
5. **Effort beside outcome on the goal screen** — the screen then answers the two
   questions a person actually asks, *how far am I* and *how much have I put in*, and the
   reward economy moves to the profile where `user.points` already lives.

## Named, not specced

- **[`DashboardViewModel.kt:103`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt#L103) takes a plain mean of `progressFraction` across all goals.** Once overshoot is legal, one goal at 300% drags the dashboard's headline upward — and the goals being averaged no longer share a scale. → `C12` #31.
- **[`RecommendationRepositoryImpl.kt:175`](../../app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt#L175)'s `progressFraction < 0.34f`** attention filter — `C10`'s feed input — now measures a different quantity than when it was written.
- **`Leveling`'s thresholds shift** once the cap goes and long tasks pay proportionally. Build work over one live ledger, not a decision.
- **Migration stays additive** (`start` → `0.0`, difficulty → `ROUTINE`), so a half-completed one still reads — the property `C16` and `C17` both established.

## 🧪 Tests

**None run, and none owed.** This is a wayfinder decision ticket: it ships no code, so
there is no layer to test. The map's own standing preference is explicit — *"No ticket
on this map ships code."* Every layer this project has (server unit, endpoints,
Firestore rules, client component, UI E2E) is untouched.

The claims that *would* need verification were instead **checked against the source**
rather than asserted from the map — `TaskEstimate.kt`, `Goal.kt`, `Task.kt`,
`SummaryUseCase.kt`, `TaskRepositoryImpl.kt`, `GoalDetailViewModel.kt`,
`GoalDetailScreen.kt`, `DashboardViewModel.kt`, `RecommendationRepositoryImpl.kt`. Two
findings came out of that reading and out of nothing else: the third clamp, and the
`points → minutes` write path that reframed the whole ticket.

## Parallel sessions

Three sessions ran concurrently on one map, all three assigned on GitHub:
`c9b-calendar-surface` (#26), `c9c-calendar-sync` (#27), and this one (#18).

The coupling that was named on the board **actually fired**: #12's *Decisions so far* is
a commons, and `c9c-calendar-sync` appended its line between this session's first read
of the map and its write. Re-fetching the body immediately before the edit — the
discipline the board records — is what kept that line. A blind write would have deleted
another session's resolution.

No file, ticket or prototype was shared. `C9c`'s resolution (*Google holds the when,
GoalPilot holds what happened*) and this one do not overlap: `C9c` decides what crosses
the wire, `C3` decides what the states mean arithmetically.
