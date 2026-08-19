# `c18-subtask-depth` — sub-tasks at arbitrary depth: what does every roll-up sum over?

**Session:** `c18-subtask-depth` · **Invocation:** `/wayfinder 12` *(bare — no ticket
named)* · **Branch:** `feat/goalpilot-implementation` · **Mode:** `AUTO MODE` ·
2026-08-10.

One ticket, which is the skill's limit. This map ships no code.

## Session hygiene, before the first unit of work

| | |
|---|---|
| `kb-candidates/` listed | **3 files, every entry always-ask** — [`2026-08-09-c9f-consent-screen-state.md`](../../kb-candidates/2026-08-09-c9f-consent-screen-state.md), [`2026-08-10-c16-milestone-model.md`](../../kb-candidates/2026-08-10-c16-milestone-model.md) and [`2026-08-10-c9c-calendar-sync.md`](../../kb-candidates/2026-08-10-c9c-calendar-sync.md) (already drained to its one parked survivor). All `rules/`-destined, so `/kb-ingest` may not take them in **either** mode. Nothing drainable by this session |
| `SESSIONS.md` read | **Two live claims at session start** — `c9b-calendar-surface` (#26) and `c3-points-currency` (#18), both confirmed open **and assigned on GitHub**. Claimed before the first write. Two more sessions joined during this one (`c14-challenge-scoring` on #23), and `c3` released mid-session |
| Singletons | **None taken.** A decision ticket ships no code: no Gradle, no `adb`, no Firebase, no GROQ call |

## The frontier, re-derived rather than trusted — three times, and it moved twice

`/wayfinder 12` arrived bare, so the ticket is the session's to pick — the skill's
*"without one, you pick the next decision, not the user."* Queried out of GitHub rather
than read off the board: every open child of #12 run through the native dependency
relation, `blocked_by` per ticket.

**Pass 1 — before claiming.** Two takeable, not the one the board advertised:

| Open child | Blocked by | Assigned | Frontier? |
|---|---|---|---|
| **#39 `C18`** | #37 ✅ | — | **yes** |
| **#28 `C9e`** | #27 ✅ | — | **yes** — the board had not caught `C9c` closing |
| #26 `C9b` | — | `idomarhaim` | claimed |
| #18 `C3` | — | `idomarhaim` | claimed |
| #19, #20, #21, #22, #23, #24, #30, #31, #35 | ≥1 open blocker | — | blocked |

**Pass 2 — minutes after claiming.** `#18` closed at **17:55Z**, unblocking `#21`, `#23`
and `#31`. The board line this session had *already written* — *"`#28` is the whole
frontier"* — was wrong before it reached a commit, and was corrected because the frontier
was re-derived a second time rather than trusted.

**Pass 3 — on release.** Closing `#39` put **`#19 · C1`** on the frontier. Final state:
takeable are **#19, #21, #28, #31**; `#23` and `#26` are claimed; and **every remaining
blocked ticket is blocked behind `#19` alone.**

## Why #39 and not #28 — the pick is the agent's, so the reasoning is recorded

- **#28 is the calendar half, where `c9b-calendar-surface` is live and mid-prototype.**
  The board already records the calendar split (*surface* vs *semantics*) as the **less**
  disjoint pair. `C9e` decides what happens to a synced event when its task changes —
  more foreign state `#26`'s prototype would have to draw, a second time in one day.
- **#39's two immediate predecessors are closed *and released*** — `C16` (#37) and
  `C17` (#38). No session is live in the structural half.

`#28` was left **unassigned and takeable on purpose**, and the board says so, so the next
session does not read the omission as an oversight.

## What changed

| Artifact | Change |
|---|---|
| [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) | Resolution comment posted, issue **closed** |
| [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) | One line appended to *Decisions so far*; the **Firestore-migration fog paragraph narrowed** to `C5` alone. Body re-read immediately before **each** of the two writes and the result verified both times: **12 → 13 decision lines, +3 lines, 1 replaced, nothing else deleted**, then a 2-line correction pass. `c14-challenge-scoring` appended its own `C14` line between the two; it survives intact, and this session's line appears exactly once — the commons discipline held from both sides |
| `SESSIONS.md` | Claim row added on claiming, removed on release; release banner on the claim-time note; frontier block refreshed three times; *Recently released* row |
| `kb-candidates/2026-08-10-c18-subtask-depth.md` | One entry, always-ask *(new)* |

## The decision

**A parent task is a container, never a second worker; every roll-up sums over *leaves*.
Depth is capped at 10.**

### Three of the ticket's five bullets were already answered — and were not re-decided

`#39` was charted by `C4` *before* `C16`, `C17` and `C3` closed. Read today, most of its
body is settled upstream:

| the ticket asked | answered by | answer |
|---|---|---|
| **Points** at a parent | `C3` §1 | Not a fork. Points are a **view of effort computed from minutes × difficulty**, never authored — they follow whatever minutes do |
| **Splitting** — does it move progress? | `C3` §2 | **No.** The weight is minutes, chosen because minutes are the only weight conserved under splitting |
| **`progressContribution`** — direct or via parent? | `C17` + `C3` §4 | **Directly, through the task's own `goalEdges`, or not at all.** Parentage is not a channel |

Recording this *as an inheritance* rather than answering it again is the substance of the
`C3`/`C18` boundary named on the claim row. What survived was **one question wearing two
hats**: *is a parent tickable* and *does a 120-minute parent over 120 minutes of children
count the afternoon twice* are the same question about what a parent's number **means**.

### Depth — Ido's call, plus one derived reading

**Ido answered: max depth 10.** Derived and flagged for override: **10 levels from an
intrinsic goal down to a leaf task**, not 10 task levels on top of `C16`'s undecided
objective depth — a roll-up walks one chain. **`E19`'s own chain runs 6 deep** (*$100M*
→ *SWE degree* → *finish year 1* → *pass the course* → *study for the exam* → *study
topic A*), so 10 leaves four levels of headroom over Ido's most elaborate example.

**The cap binds the user; the agent must never be the reason it is reached.** The
ticket's *"what happens to an AI plan that would exceed the cap?"* is answered by
denying its premise: a plan proposing ten levels is a pathology, not a deep plan. A
proposed plan is **flattened to fit at the deepest legal level, never refused**, and it
gives way to structure Ido built rather than consuming his depth. The plan's *shape* is
`C8` (#24)'s and inherits this as a constraint. A refusal surfacing to the user would
violate `C4` §9, which lets the app act silently on instrumental structure precisely so
it does not narrate its own limits.

**New constraint, not a preference:** a depth cap is meaningless against a **cycle**
(`A` parent of `B` parent of `A` is depth ∞). Reject a parent edge reachable from the
child, at the write site. Client-side over the loaded list, so no extra reads.

### The parent question, and why the other two shapes die

| | **A · Container** ✅ | **B · Residual** | **C · Flag per task** |
|---|---|---|---|
| the **120** you typed means | the whole thing | **extra**, on top of children | whichever was flagged |
| the app computes | 60 + 60 = **120** | 120 + 60 + 60 = **240** | depends |
| kinds of parent to render and sum | **one** | two | two, plus a flag |

**B reads the most natural input backwards** — nobody types *"2 hours"* on *Study for the
exam* meaning *on top of the topics*. That is `C7`'s `"%"` disease and `C3`'s `1.0`
silence one level up, and it compounds at every level it passes. **C is killed by
`C16`'s own argument against *"show both"*:** it defers, and the deferral resurfaces at
all ten levels.

### Four improvements, each reusing a rule already on the map

1. **Residual work becomes a child** — nothing is unsayable (`C16` §3's one repeated edge).
2. **The gap is disclosed, one tap from closing.** Adding one 15-min sub-task to a
   120-min parent would silently collapse it; instead: *"your sub-tasks add up to 15 of
   the 120 you estimated"* → **[make the rest a sub-task]**. Pure subtraction, identical
   offline (`C16` §4, `C17` §6).
3. **Ticking a parent writes its children** rather than being forbidden — the gesture
   survives and the arithmetic never branches.
4. **A `MISSED` child never holds its parent hostage** (`C3` §6) — without it, one
   skipped run manufactures the failure `C9a` forbade.

### `C4` §4's milestone/sub-task line, made arithmetic

A **milestone may measure**, so it can disagree with the work below it (`C16` §4 clause
1). A **parent task may only sum**, so it cannot. That disagreement is `C3` §1's
effort-vs-outcome gap, and it belongs to objectives only.

## Named, not specced

- **Promotion of a deep task subtree to a milestone** — by `C4` §2 that is adding one
  edge, not a migration. Whether the app should *suggest* it is `C12` (#31)'s.
- **Code consequences**, each grounded rather than asserted:
  `TimeAllocationUseCase` sums a flat list at [`:136`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/TimeAllocationUseCase.kt#L136)
  and [`:204`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/TimeAllocationUseCase.kt#L204)
  and must sum leaves; [`TaskDuration.minutesOf`](../../app/src/main/java/com/idomarhaim/goalpilot/domain/model/TaskEstimate.kt#L45)
  would invent a parent's minutes from a **word count** via `fallbackMinutes(points)` —
  `C3` §0's defect one level up — so a non-leaf returns its children's sum and never
  falls back; the dashboard's `doneTasks`/`totalTasks` count leaves; the parent edge is
  stored **on the child**, cardinality **one**.
- **Migration share: one nullable field backfilled to `null`.** Every existing task is a
  leaf, so day one reads identically — which **retires** the map's standing fear that
  this ticket would restructure every goal and task.

## 🧪 Tests

**None run, and none owed.** This is a wayfinder decision ticket: it ships no code, so
there is no layer to test. The map's standing preference is explicit — *"No ticket on
this map ships code."* Every layer this project has (server unit, endpoints, Firestore
rules, client component, client page, UI E2E) is untouched.

The claims that *would* need verification were **checked against the source** rather than
asserted from the map: `Task.kt`, `TaskEstimate.kt`, `TimeAllocationUseCase.kt`, and the
`estimatedMinutes` / `minutesOf` call sites across `data/` and `feature/`. The
`minutesOf`-falls-back-on-a-parent finding came out of that reading and out of nothing
else. Ticket state, assignees, `blocked_by` edges and the map body were each read back
from GitHub after every write rather than assumed — the `#12` write was verified byte-for-byte
against the intended body (one trailing newline added by GitHub, content identical).

**The pre-commit self-review earned its place, and caught two things nothing else would
have.** *Which factual claim did I not verify?* — **`E19`'s depth**. The resolution, the
`#12` index line and this changelog all said the worked example runs **5** levels deep;
counting it says **6** ($100M → SWE degree → finish year 1 → pass the course → study for
the exam → study topic A). The claim was load-bearing — it is the evidence that a cap of
10 is roomy — and it was asserted from memory rather than counted. Corrected in all
three, after the comment and the map body had already been written.
*Which open ticket names this one?* — **`C8` (#24)**, which surfaced an unanswered
sub-bullet: `#39` explicitly asked *"what happens to an AI-proposed plan that would
exceed the cap?"* and the first draft of the resolution never answered it. Answered now,
and handed to `C8` as a constraint rather than a question.

## Parallel sessions

Four other sessions touched this repo today. `c3-points-currency` closed `#18` **mid-session**,
between this session's two frontier derivations — caught only because the frontier was
re-derived rather than trusted, and it invalidated a board line already drafted.
`c14-challenge-scoring` joined on `#23` during this session, **and released before it** —
so for a stretch two sessions were writing `SESSIONS.md` and `#12` at the same time.
Nothing owned by `c9b-calendar-surface`, `c3-points-currency` or `c14-challenge-scoring`
was edited; `#12`'s shared index was appended to under the commons discipline (re-read
immediately before each write, own line only, insertion verified), and `c14`'s `C14`
line, appended between this session's two writes, survives intact.

**The board collision resolved itself, and the resolution is worth recording because the
rule does not cover it.** `SESSIONS.md` came to hold *two* sessions' releases as
uncommitted changes in **one shared checkout**, which explicit-path staging cannot
separate — the collision is inside a single file, not across files. This session had
decided to commit the joint state and name the rider, per the *"their committed work will
ride along; the duty is to look and say so"* clause. Before it could, **`c14`'s commit
[`d7f0b83`](../../SESSIONS.md) carried both releases** — it made the same call from the
other side. Nothing was lost in either direction; every line this session wrote is in
that commit, verified line by line after the fact. Two intermediate greps here reported
*"my edits are gone"* and were **wrong** — a backtick escaped badly inside a quoted
shell pattern, not a lost update. Worth flagging as a trap: on a shared checkout the
panic reading is the plausible one, and the check that produced it must be trusted less
than the file.

The one thing explicit-path staging genuinely protected: `c9b-calendar-surface`'s live
`docs/prototypes/2026-08-10-calendar-surface/index.html` was modified in the tree
throughout and was **never staged** by this session. No blanket staging at any point.
