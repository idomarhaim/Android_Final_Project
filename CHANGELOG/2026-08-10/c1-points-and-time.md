# c1-points-and-time — claimed #19, the ticket every blocked ticket waits behind

**Session:** `c1-points-and-time` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#19 · `C1`](https://github.com/idomarhaim/Android_Final_Project/issues/19) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session has done so far

`/wayfinder 12` invoked **bare**, so the frontier pick was the agent's and the reasoning is
recorded on `SESSIONS.md` rather than left implicit.

**Frontier re-derived out of the GitHub dependencies API**, not read off the board's
Unclaimed-work block — that block has carried a stale count three separate times today and
says so in its own text. Every open child of `#12` queried for `blocked_by`:

| Ticket | Blocked by | Verdict |
|---|---|---|
| `#19 · C1` | `#18` (closed), `#39` (closed) | **frontier — claimed** |
| `#21 · C5` | `#13`, `#18` (both closed) | frontier — left |
| `#28 · C9e` | `#27` (closed) | frontier — left |
| `#31 · C12` | `#18`, `#14` (both closed) | frontier — left |
| `#26 · C9b` | `#25` (closed) | unblocked but **assigned and live** |
| `#20`, `#22`, `#24`, `#30`, `#35` | all wait on `#19`, directly or through `#24` | still blocked |

Map size verified against GitHub: **25 children, 15 closed, 10 open.** Membership of the
frontier is unchanged from `c14-challenge-scoring`'s derivation; this is the **fifth**
derivation of the day.

**Why `#19`:** it is the leverage. `#20`, `#22` and `#24` wait on it, and through `#24` so
do `#30` and `#35` — **every remaining blocked ticket on the map, with no exceptions.** Its
two blockers (`C3` #18 and `C18` #39) are closed *and released*, so nothing live sits in the
scoring/structural half at all. The other three frontier tickets were each declined for a
reason the board already records: `#28` is the calendar half against a live `#26`, `#31` is a
second HITL prototype contending for Ido himself, `#21`'s recurrence flows into `#26`'s
surface. One of `#31`'s two standing objections has **expired** (`C3` and `C18` have closed);
the HITL one has not.

## Claim

- **GitHub:** `#19` assigned to `idomarhaim` **before any other work**, so concurrent
  sessions skip it.
- **`SESSIONS.md`:** row added to *Active claims* with paths and singletons (**none** — a
  grilling ticket ships no code, so no build, no device, no Firebase), plus the frontier
  reasoning and two coupling points.

## Coupling points, named on claiming

1. **`#12`'s *Decisions so far* is a commons.** Re-fetch the body immediately before
   appending, write only this session's line, verify a pure insertion. `#26`'s line is still
   owed by `c9b-calendar-surface`; `C13` (#32)'s index gap stays Ido's to assign.
2. **`#19` arrives with more decided than open, all of it from *released* sessions.** `C3` §1
   already made `points = round(minutes / 3) × difficulty` — computed, never authored — which
   is most of `R7`. `C18` answered what a point total sums over (**leaves**). `C17` answered
   how a shared task pays (**pooled, once**) and routed the *bonus* question here as
   motivation design. These are inputs, never subjects; a contradiction gets posted to that
   ticket, not edited into a released session's artifacts.

## `kb-candidates/`

Re-listed at session start, as the folder's existence requires. **Seven files — the board's
standing note counts four and is three stale in that direction.** Nothing is drainable by
this session, for two different reasons: six are **always-ask** (five target `rules/`, and
four of those target `rules/question-axis-naming.md` — one accumulating amendment that should
be read together); the seventh, `2026-08-10-c9b-calendar-surface.md`, holds three ordinary
`AUTO MODE`-eligible entries but is **owned by a live row** and drains with that session.

## 🧪 Tests

**None run, and none owed.** This is a `wayfinder:grilling` ticket on a planning map — the
map's standing preference is *plan, don't do*, and no ticket on it ships code. Nothing in
this unit touched `app/`, `functions/` or `firestore-tests/`, so no server, client, endpoint,
database or UI layer was exercised. The unit's own verification is structural and was
performed: the frontier was re-derived from the dependencies API rather than trusted, the
assignment was confirmed back out of GitHub, and the map body will be re-fetched immediately
before its index line is appended.

## Status

**Claim only — the resolution is not written.** `#19` is HITL: it needs Ido in the loop on
what is genuinely his (whether a human may correct points, and whether multi-purpose work
deserves a bonus), while everything derivable from `C3`, `C17`, `C18` and `C11a` is taken and
logged rather than asked.
