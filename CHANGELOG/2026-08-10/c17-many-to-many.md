# `c17-many-to-many` — a goal in several life areas, a task under several goals

**Session:** `c17-many-to-many` · **Invocation:** `/wayfinder 12` *(bare — no ticket
named)* · **Branch:** `feat/goalpilot-implementation` · **Mode:** `AUTO MODE` ·
2026-08-10.

One ticket, which is the skill's limit. This map ships no code.

## Session hygiene, before the first unit of work

| | |
|---|---|
| `kb-candidates/` listed | **2 files, both already drained down to always-ask survivors** — [`2026-08-09-c9f-consent-screen-state.md`](../../kb-candidates/2026-08-09-c9f-consent-screen-state.md) entry 1 and [`2026-08-10-c16-milestone-model.md`](../../kb-candidates/2026-08-10-c16-milestone-model.md) entry 2. Both are `rules/`-destined, so `/kb-ingest` may not take them in **either** mode; both wait on Ido, not on a session. Nothing here is a backlog |
| `SESSIONS.md` read | No active claims. The frontier block was refreshed 2026-08-10 by `c9a-schedule-a-task` and is **accurate** — re-derived from GitHub independently below and it matches |
| Template parity | `Update-TemplateConsumers.ps1` → **`AGENTS.md` v15 → v16** in this repo (verbatim projection, provenance verified, one block: `routing`). Applied and committed as its own mechanical commit. Three files in `C:\Dev\FP_DEMO` reported **BLOCKED** (dirty tree) and were not touched — Ido's to decide |
| Leases | `AGENTS.md`, `SESSIONS.md` taken via `Lock-Path.ps1` before the first write, released at the commit |

**What v16 changed, and it landed before the work rather than after:** the claim rule
now says the board belongs to **the repo being edited**, not the directory the session
started in — so a `/kb-ingest` into the central bundle owes a row in `C:\Dev\JARVIS`
too, with a carve-out for a mechanical sweep. That is why the `AGENTS.md` bump above is
recorded here rather than claimed on the board.

## The frontier, re-derived rather than trusted

`/wayfinder 12` arrived bare, so the ticket is the session's to pick. Queried out of
GitHub — open, unblocked, unassigned children of #12 — via the native dependency
relation:

| Frontier ticket | Closing it unblocks |
|---|---|
| **[#38 · `C17`](https://github.com/idomarhaim/Android_Final_Project/issues/38)** | **9** — sole remaining blocker of [#18 `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18), and behind #18 sit `C1` #19, `C5` #21, `C6` #22, `C14` #23, `C12` #31, then `C2` #20, `C8` #24, `C11b` #30, `C15b` #35 |
| [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39) | 1 directly (#19, jointly with #18) |
| [#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) | 1 (#28) |
| [#26 · `C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26) | 0 |

Took **#38** on leverage. It matches the board's own recommendation, arrived at
independently.

## What changed

| | |
|---|---|
| Resolved | [#38 · `C17`](https://github.com/idomarhaim/Android_Final_Project/issues/38) — closed, full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*; **two fog patches narrowed** (the Firestore migration from three dependent tickets to two; per-life-area success/failure gains its counting half) |
| Tickets created | **none** — every hand-off landed on a ticket that already existed |
| Hand-offs commented | [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) `C3` (unblocked) · [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) `C1` (points bonus) · [#21](https://github.com/idomarhaim/Android_Final_Project/issues/21) `C5` (`E4` counting half) · [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) `C12` (the disclosure line) |
| Unblocked | **[#18 `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18)** — nine tickets sit behind it |
| Frontier now | [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) `C3` · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` · #26 and #27 are assigned (see below) |

## The decision

**Divide what is drawn from one pool; duplicate what each destination owns.**

```
        "run 4 km with my partner"   —   40 minutes of real work, done ONCE
                        │
            ┌───────────┴───────────┐
            ▼                       ▼
      "run 4 km a week"    "shared activities with my partner"
         HEALTH                 RELATIONSHIPS

   minutes  (one pool)   →  20 / 20      divided
   points   (one pool)   →  paid once    not doubled
   progress (each owns)  →  4 km / 1 occasion   both, in full
   success  (each owns)  →  1 / 1        both, in full
```

One rule, four of the ticket's five sub-questions, and they do **not** come out the same
way. *"Does it advance both goals?"* — yes, in full. *"Does it pay points twice?"* — no.
Those look parallel and are not.

**The task→goal edge is a record, not an id** — `goalEdges: [{goalId, contribution}]` —
forced by `C7`, which named this hole and pointed it here: one `Double` cannot be right
for *"4 km"* and *"one shared activity"* at once, because `C7` put the measure on the
object at the far end of the edge. `parentIds` is plural for the same reason `C16` shaped
it plural.

### The fork was smaller than the ticket thought

The body treats the pie and the *"what have I done for Relationships?"* view as needing
one answer. They don't — a life-area detail screen shows the **whole** 40-minute run
under every option. Only the chart that must total 100% was ever forked. Collapsing that
false premise is what left one question instead of five.

### The one question, and what Ido did with it

Put to him as three options on one axis. **He handed it back** — *"choose the solution
that gives the highest standard and quality of the app and its purpose, UX/UI and the
software, and improve it if you can"* — so the pick is the agent's, recorded as such.

**Divide**, on three arguments in order of weight:

1. **It is the only option an autonomous agent cannot inflate.** `C4` §9 lets the app add
   instrumental edges *silently*. Under *credit-both*, a silently-added area raises that
   area's share with no work done — the central chart would reward **re-filing over
   doing**, in an app whose purpose is catching neglect.
2. **Primary demands a ranking `C4` §9 forbids the AI to make**, and prints `0` for an
   area genuinely served — the exact false negative the chart exists to prevent.
3. **It composes**: a goal in 2 areas × a task under 2 goals still sums to the task's
   minutes, not 4×.

**The ticket's own headline objection is weaker than it looks, and is not the reason.**
*"The total exceeds the time that actually passed"* — the chart counts only completed
tasks and substitutes a fallback duration for unestimated ones, so it was never an audit
of elapsed life. Inflatability is what decided it.

### Three improvements on top

1. **The chart discloses that it divided** — *"40 of your 100 tracked minutes served more
   than one life area."* Pure subtraction. The precedent is already on that screen:
   `TimeAllocation.estimatedTaskCount` exists so a **guessed** number says so, and a
   **split** number is owed the same.
2. **The division never leaves the pie.**
3. **Integer remainder distributed by largest-remainder** — rounding each
   `TimeSlice.minutes` independently breaks the sum-to-total invariant that chose the
   option in the first place.

## 🧪 Tests

**No test layer applies, and that is a property of the ticket rather than a gap.** This
map ships no code (`#12` Notes, *Standing preferences*): the deliverable is a decision
recorded on an issue. Nothing under `app/src/` was edited — verified, the working tree
carries no `.kt` change — so there is no unit, integration, endpoint, database, component,
page or E2E layer to exercise. The layers exist in this repo and are simply not reached.

**What stands in for a test here** is the pre-commit self-review, which caught two errors
in the resolution draft *before* it was posted:

- a claim that the time chart *"counts only completed tasks that carry a duration"* —
  **false**; `TaskDuration.minutesOf` supplies a fallback rather than dropping the task.
  The conclusion survived, the reason did not, and the reason was load-bearing in the
  argument against duplication;
- a mock UI line quoting *"12 of your 100 minutes"* against a worked example whose answer
  is **40**.

## Session hygiene, continued

**A stale claim found on the tracker, not acted on.**
[#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) is assigned —
which takes it off the frontier — but **no session holds it**: no board row, no brief in
`sessions/`, and no changelog under the `c9c` label in **any** day folder. Both comments
on it are hand-offs written *by other sessions* (`C9d` 08-07, `C9a` today). Checked by
**label across all day folders** rather than at one declared path, which is exactly the
procedure the parked `c16` candidate entry 2 prescribes — its first field trial, and it
returned a negative cleanly. Unassigning is Ido's call, so it is reported, not done.

**Concurrency.** `c9b-calendar-surface` claimed #26 mid-session and recorded the shared
`#12` index on the board before either of us wrote to it. Honoured: #12's body was
re-fetched immediately before appending and byte-compared against the copy this session
had read (41 000 bytes, unchanged), then only this session's own line was added.

## 📥 KB candidates

Two, both `kb/dev/`-destined and ready — written to
[`kb-candidates/2026-08-10-c17-many-to-many.md`](../../kb-candidates/2026-08-10-c17-many-to-many.md):

1. **Divide the pooled, duplicate the owned** — and the same completion legitimately
   reads as *20 minutes* and *one whole success* on one screen.
2. **Prefer the aggregation an autonomous agent cannot inflate** — invariance under adding
   a link beats the arithmetic objections, which in this case were nearly worthless.
