# c9e-event-lifecycle — claimed #28, the calendar half's last open ticket

**Session:** `c9e-event-lifecycle` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#28 · `C9e`](https://github.com/idomarhaim/Android_Final_Project/issues/28) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked **bare**, so the frontier pick was the agent's and the reasoning is
recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block — every open child of `#12` queried for `blocked_by`:

| Ticket | Blocked by | Verdict |
|---|---|---|
| `#28 · C9e` | `#27` (closed) | **frontier — claimed** |
| `#21 · C5` | `#13`, `#18` (both closed) | frontier — left |
| `#31 · C12` | `#18`, `#14` (both closed) | frontier — left |
| `#19 · C1` | `#18`, `#39` (both closed) | unblocked but **assigned and live** |
| `#20`, `#22`, `#24`, `#30`, `#35` | all wait on `#19`, directly or through `#24` | still blocked |

Map size verified against GitHub: **25 children, 16 closed, 9 open** — one more closed than
`c1-points-and-time` counted, because `#26` closed in between. **Sixth** derivation of the
day, and the first whose *membership* differs from the previous one.

**Why `#28`:** the single reason it was declined four times — *"the calendar half is live,
`c9b-calendar-surface` is mid-prototype on `#26`"* — **expired 101 seconds before this
claim**. `#26` closed at `19:23:52Z` (verified via `closedAt`) and its `C9b` line is already
in `#12`'s index. Every calendar predecessor is now closed *and* released (`C9d` #17,
`C9a` #25, `C9c` #27, `C9b` #26), which makes `#28` the calendar's **last open ticket** —
closing it finishes a subsystem rather than opening one.

`#21 · C5` was declined because it sits **nearer** the live `#19` than `C9e` does: it models
a goal with no target, and what effort and progress mean for such a goal is exactly what
`c1-points-and-time` is deciding right now. `#31 · C12` was declined on the prototype
contention — every frontier ticket here is HITL, so HITL-ness discriminates nothing, but a
*prototype* is the heavy kind and `#26` just spent **eight revisions** of Ido's attention.

**`c9b-calendar-surface`'s row was left untouched.** Ticket closed, index line written,
working tree clean — that reads as a session **mid-release**, not mid-work, and releasing its
row is that session's move. A row edited for another session is a report, not a claim.

## Claim

- **GitHub:** `#28` assigned to `idomarhaim` **before any other work**, so concurrent
  sessions skip it. Assignment confirmed by reading it back out of GitHub.
- **`SESSIONS.md`:** row added to *Active claims* with paths and singletons (**none** — a
  grilling ticket ships no code, so no build, no device, no Firebase), plus the frontier
  reasoning and two coupling points.

## Coupling points, named on claiming

1. **`#12`'s *Decisions so far* is a commons**, and the race it names has fired for real once
   (`c3-points-currency` records it from both sides). Re-fetch the body immediately before
   appending, write only this session's line, verify a pure insertion. The standing *"`#26`'s
   line is still owed"* note in the older board banners is now **discharged** — that line is
   written. `C13` (#32)'s index gap stays Ido's to assign.
2. **`C9e` arrives with four rules inherited and exactly one live edge.** The inherited four
   are `C9c`'s, from a **released** session, so they are inputs and never subjects: matching
   is by `googleEventId` · times cross the sync and state never does · titles are written but
   never read back · a cancelled event unsyncs and never deletes. The one live edge is `#19`'s
   **bulk re-scoring pass** — if re-scoring can move times, it is a bulk write into Ido's real
   calendar. Anything found here that bears on `#19` gets **posted there**, not decided there.

## `kb-candidates/`

Re-listed at session start, as the folder's existence requires — **seven files**, agreeing
with `c1-points-and-time`'s correction. Nothing is drainable by this session: six are
**always-ask** (five target `rules/`, four of those `rules/question-axis-naming.md`, one
accumulating amendment to be read together); the seventh,
`2026-08-10-c9b-calendar-surface.md`, is ordinary and `AUTO MODE`-eligible but is **owned by a
row still on the board**, so it drains with that session's release.

## 🧪 Tests

**None run, and none owed.** This is a `wayfinder:grilling` ticket on a planning map — the
map's standing preference is *plan, don't do*, and no ticket on it ships code. Nothing in this
unit touched `app/`, `functions/` or `firestore-tests/`, so no server, client, endpoint,
database or UI layer was exercised. The unit's own verification is structural and was
performed: the frontier was re-derived from the dependencies API rather than trusted, `#26`'s
closure was verified by timestamp rather than assumed from the board, the assignment was read
back out of GitHub, and the map body will be re-fetched immediately before its index line is
appended.

## Status

**Claim only — the resolution is not written.** `#28` is HITL: deletion, completion,
rescheduling from either side, goal archival, bulk writes and orphaned events are choices
about Ido's own shared calendar, where a wrong deletion is not recoverable from inside
GoalPilot. Whatever is derivable from `C9a`, `C9b` and `C9c` will be taken and logged rather
than asked.
