# c8-ai-task-plans — claimed #24, and the re-derivation corrected a released session's own summary

**Session:** `c8-ai-task-plans` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#24 · `C8`](https://github.com/idomarhaim/Android_Final_Project/issues/24) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked with the **map**, not a ticket, so the frontier pick was the agent's
and the reasoning is recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block — every open child of `#12` queried for `blocked_by`:

| Ticket | Blocked by | Verdict |
|---|---|---|
| `#24 · C8` | `#13`, `#19`, `#37` (all closed) | **frontier — claimed** |
| `#20 · C2` | `#19` (closed) | frontier — left |
| `#21 · C5` | `#13`, `#18` (both closed) | frontier — left |
| `#22 · C6` | `#19`, `#18` (both closed) | frontier — left |
| `#31 · C12` | `#18`, `#14` (both closed) | unblocked but **assigned and live** |
| `#30 · C11b` | `#20` **open**, `#24` **open** (+ `#19`, `#29` closed) | still blocked |
| `#35 · C15b` | `#24` **open** (+ `#29` closed) | still blocked |

Map size verified against GitHub: **25 children, 18 closed, 7 open**. **Eighth** derivation
of the day, and the frontier has **doubled from two to four** now that `#19` has closed.

## The correction the re-derivation bought

`c1-points-and-time`'s release note on the board states that closing `#19` unblocked
*"`#20`, `#22`, `#24`, and through `#24` both `#30` and `#35` … nothing on `#12` is blocked
any more — the whole remaining map is frontier."*

**The last clause is false.** `#30` is blocked by `#20` **and** `#24`; `#35` is blocked by
`#24`. All three blockers are **open**, so neither `#30` nor `#35` is takeable. `C1`
unblocked **three** tickets, not five.

Nothing in that session's note was edited — a released session's row is not this session's
to rewrite — so the correction lives in this session's own claiming banner. The board's
**Unclaimed-work** block is separately stale: it was queried before `#19` closed and still
lists five tickets as blocked behind it.

This is the whole argument for re-deriving rather than inheriting: the inherited number was
wrong in the direction that would have made a session claim a blocked ticket.

## Why `#24`, and why not the other three

1. **`#24 · C8` is disjoint and has the only leverage on the frontier.** Both blockers —
   `C4` [#13](https://github.com/idomarhaim/Android_Final_Project/issues/13) and
   `C1` [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19) — are closed
   **and released**, so its inputs are foreign state to read. It is also a blocker of both
   remaining blocked tickets: `#35` waits on `#24` **alone**, `#30` on `#20` + `#24`.
   Closing it frees `#35` outright and halves `#30`. No other frontier ticket frees
   anything.
2. **`#20 · C2` — declined: it would change a live session's inputs mid-flight.** `C2` asks
   whether an AI-assigned task type is a second axis **or a replacement for life areas**,
   and its body names *"it drives the time-allocation analytics that already ship"* as a
   candidate purpose. `c12-charts-presentation` is live on
   [#31](https://github.com/idomarhaim/Android_Final_Project/issues/31) deciding the chart
   set and dashboard arrangement, with a `C9b` hand-off about that exact chart. Re-cutting
   what the charts group by while they are being drawn is what this board has refused five
   times.
3. **`#22 · C6` — declined on prototype contention.** `C6` decides what a user may edit in a
   **screen**, which under the now-normative design standard is prototype-grade work. `c12`
   is already at revision 2 of a prototype burning Ido's attention. Every frontier ticket is
   HITL, so HITL-ness discriminates nothing — a second *screen* does.
4. **`#21 · C5` — declined on subject overlap, not on the ground four earlier sessions
   used.** Their objection (too near the live `#19`) has **expired**. What remains: `C5`'s
   decay mechanic changes what a goal's **percentage** means, and a goal's percentage is
   what `#31`'s charts render. It is also the heaviest ticket left — a Firestore schema
   change over live data, migration still fog.

## Claim

- **`#24` assigned to `idomarhaim` on GitHub before any work** — the assignee *is* the
  claim, per the skill.
- Row written on `SESSIONS.md` with three coupling points named on claiming: the `#12`
  *Decisions so far* commons (re-fetch immediately before appending; the race has fired
  twice for real), the released decisions `C8` inherits as inputs (`C4`, `C1`, `C16`,
  `C11a`), and the one live edge into `#31`, which is **posted as a comment, never taken**.

## 📥 KB candidates

`kb-candidates/` listed before the first unit of work. **Four files, each opened and its
*Destination* line read** rather than inherited from the board notes:

| File | Destination | Drainable here? |
|---|---|---|
| `2026-08-10-c1-points-and-time.md` | `rules/question-axis-naming.md` | ⛔ always-ask |
| `2026-08-10-c9e-event-lifecycle.md` | `rules/question-axis-naming.md` | ⛔ always-ask |
| `2026-08-10-c16-milestone-model.md` | `rules/agent-topology-and-model-routing.md` §5 | ⛔ always-ask |
| `2026-08-09-c9f-consent-screen-state.md` | `kb/dev/` — **parked by Ido** pending a `rules/` proposal | ⛔ always-ask |

All four are always-ask in **both** modes, and none is this session's. **`AUTO MODE` drains
nothing here.** The two `question-axis-naming.md` entries should be read together.

## 🧪 Tests

**None run, and none applicable.** This is a planning session: Markdown and GitHub issues
only, no code touched. The project's test layers (server unit, instrumented, UI) are
untouched by this unit of work.

## Next

Resolve `#24` as a `wayfinder:grilling` ticket — HITL, one question at a time, with Ido.
