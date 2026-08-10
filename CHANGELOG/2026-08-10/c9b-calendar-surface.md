# c9b-calendar-surface — 2026-08-10

`/wayfinder 12`, no ticket named → **work through the map**, and choosing the ticket
is the session's job, not Ido's (wayfinder skill, *Work through the map* step 2).

## 🧭 Which ticket, and why

The frontier was **re-derived out of GitHub**, not read off the board — `SESSIONS.md`
says in as many words that every session which tried to predict it has been wrong at
least once. Querying `dependencies/blocked_by` for all 15 open children of
[#12](https://github.com/idomarhaim/Android_Final_Project/issues/12):

| Ticket | Blockers | On the frontier? |
|---|---|---|
| [#26 · `C9b`](https://github.com/idomarhaim/Android_Final_Project/issues/26) | #25 closed | **yes** |
| [#27 · `C9c`](https://github.com/idomarhaim/Android_Final_Project/issues/27) | #25, #17, #33 all closed | **yes** |
| [#39 · `C18`](https://github.com/idomarhaim/Android_Final_Project/issues/39) | #37 closed | **yes** |
| [#38 · `C17`](https://github.com/idomarhaim/Android_Final_Project/issues/38) | #37 closed | unblocked but **assigned** — live session `c17-many-to-many` |
| #35, #31, #30, #28, #24, #23, #22, #21, #20, #19, #18 | ≥1 open blocker | no |

Took **#26**. It is the lowest-numbered frontier ticket, which is the skill's own
tie-break — but two things made it the right pick rather than merely the first:

1. **#39 is the wrong ticket to run beside a live `C17`.** Their *paths* are disjoint
   and their *subjects* are not: `C17` decides how a task attaches to several goals,
   `C18` decides what every roll-up sums over. Both write the same arithmetic. The
   board's disjointness test is about paths and would have passed this; it would still
   have been two sessions deciding one thing.
2. **#26 is the only `prototype` ticket on the frontier**, and every frontier ticket is
   HITL. Ido's attention is the scarcest singleton on this map and the one the board
   cannot enforce — a prototype front-loads agent work (build the artifact) before it
   needs him, where a second grilling would contend for him immediately.

## 🔒 Claim

- Ticket claimed the way this map claims: **assignee on GitHub** (#26 → `idomarhaim`).
- Board row added. `SESSIONS.md` taken under a **lease**, not a claim
  (`Lock-Path.ps1`), because it is a commons every session touches for seconds.
- **Overlap recorded rather than discovered later:** `c17-many-to-many` and this
  session both owe a line in #12's *Decisions so far*. Append-only, one line each,
  re-read the map body immediately before writing. Neither session edits the other's
  line, ticket or scope.
- `kb-candidates/` listed before the first unit of work, as owed: **two files, one
  always-ask entry each**, both destined for `rules/`, so nothing is drainable in
  either mode. They wait on Ido and on `/walkthrough`, not on a session.

## 🧪 Tests

Not applicable so far — no code has been written. `C9b` is a decision ticket and the
map's standing preference is **plan, don't do**: nothing here ships into `app/`.
The prototype is a throwaway artifact under `docs/prototypes/`, which has no test
layer by construction.
