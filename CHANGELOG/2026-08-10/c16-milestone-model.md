# `c16-milestone-model` — a milestone is a goal nobody wants for itself

> **Summary:** a milestone is a goal nobody wants for itself

**Session:** `c16-milestone-model` · **Invocation:** `/wayfinder 12` *(bare — no ticket
named)* · **Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL
throughout) · 2026-08-10.

One ticket resolved, which is the skill's limit. **No code was touched** — this map
ships no code, and that held: `Goal.kt`, `Task.kt`, `Constants.kt` and `firestore.rules`
were read and none was edited.

## What changed

| | |
|---|---|
| Resolved | [#37 · `C16` How is a milestone modelled?](https://github.com/idomarhaim/Android_Final_Project/issues/37) — closed, with the full resolution as a comment |
| Map updated | [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) — one line into *Decisions so far*; **two fog patches narrowed** (the Firestore migration from four dependent tickets to three, and per-life-area success/failure from two to one) |
| Tickets created | **none** — every hand-off landed on a ticket that already existed |
| Hand-offs commented | [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) `C17` (unblocked) · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` (unblocked) · [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) `C3` (one of three blockers cleared) |
| Unblocked | [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) and [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39). **Not** [#18](https://github.com/idomarhaim/Android_Final_Project/issues/18) — #38 is still ahead of it |
| Frontier now | [#38](https://github.com/idomarhaim/Android_Final_Project/issues/38) `C17` · [#39](https://github.com/idomarhaim/Android_Final_Project/issues/39) `C18` · [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) `C13` · [#25](https://github.com/idomarhaim/Android_Final_Project/issues/25) `C9a` *(claimed, live sibling)* |

## Why this ticket

`/wayfinder 12` arrived bare, so the frontier was re-derived out of GitHub — open,
unblocked, unassigned children of #12 — and came back **#25 (`C9a`)**, **#32 (`C13`)**
and **#37 (`C16`)**. Took #37 on leverage, not on issue order:

| Ticket | Closing it unblocks |
|---|---|
| **#37 `C16`** | **11** — sole blocker of #38 and #39; via #38 it gates #18, and behind #18 sit `C1`, `C2`, `C11b`, `C5`, `C6`, `C14`, `C12`, `C15b` |
| #25 `C9a` | 3 |
| #32 `C13` | 0 |

## The decision

**One collection, one edge, one number, and a marker that records *who*.**

```
  MY GOALS                                  ← only what you chose for its own sake
  ───────────────────────────────────────
  Be worth $100M                    chosen by you
     └ Do a SWE degree              chosen by you   ← also its own goal
          └ Finish year 1           milestone · 6/10 courses
               └ Pass the course    milestone
                    ├ Submit A1     task
                    └ Study exam    task
          └ Buy textbooks           task            ← hangs straight on the goal
  Gym                     ⚠ created by the AI   [ keep ]  [ make it a milestone ]
```

- **One collection.** Milestones live in `users/{uid}/goals`. There is no milestone
  entity. Two tests against `E19` decided it — *what does promoting "Finish year 1"
  cost*, and *how is "Do a SWE degree" stored when it is both* — and two collections
  fails both.
- **The marker carries provenance, not a boolean.** `intrinsic: { declaredBy: USER |
  AI_SUGGESTED | UNKNOWN, atEpochMillis, acceptedAtEpochMillis? }`. Same query cost as
  `isIntrinsic: Boolean`, and the only shape where `C4` §9's *must ask before asserting
  an intrinsic edge* has a **witness in the data**.
- **The edge is stored on the child** (`parentIds`), and nesting is that same edge
  repeated. Plurality is left shaped-but-unanswered for `C17`.
- **One progress number**, and the work underneath is what advances it.
- **A task attaches at any level**, settling `E19`'s stated ambiguity permissively.

Full argument in the [resolution comment](https://github.com/idomarhaim/Android_Final_Project/issues/37).

## The finding worth keeping: my own question was the bug

Branch 4 was put to Ido as *"a milestone with target 6/10 and 1-of-3 children — which
number wins, 60% or 33%?"*, with three options and a diagram each. He could not answer
it, and **he was right not to**: the two numbers are not rival measurements.
`Task.progressContribution` already exists and already means *completing a task advances
the objective's number*. The work below is the **mechanism** of the number, not a second
opinion about it. The rivalry was manufactured by the framing.

Reframing dissolved the question and produced a **better answer than any of the three
options offered**:

> An objective has exactly one progress number. If it has a target, that is the number,
> and finishing the work underneath is what advances it. If it has no target, the number
> is simply how much of the work underneath is done.

That kills *"show both"* — it does not decide, it defers, and the deferral resurfaces at
every ancestor — and it kills *children-win*, under which splitting one task into three
drops progress 33% → 25%, **punishing the user for planning properly**.

It also exposed a signal neither framing could see: when the listed work sums to **less
than the target**, say so. *"Everything you have planned adds up to 3 of 10."* That is
**subtraction, not inference**, so the map's permanent free-model rule costs nothing
here — there was never a model in it. Ido kept it inside `C16` rather than deferring it.

**The lesson generalises, and it is the second time this session:** three of my four
questions to Ido came back *"I could not understand the options."* Both times the fix was
not simpler wording — it was that the axis had not been reduced far enough, and once it
was, one option turned out to be wrong rather than merely unchosen. `c10-quote-feed`
flagged the identical lesson from its own session on the same day; this is corroboration,
not a new claim.

## Rejected, and why it is recorded

**One `nodes` collection holding goals, milestones *and* tasks.** It passes both storage
tests and is the most elegant on paper. Rejected because it merges two field sets that
are **never both valid** (`targetValue`/`currentValue`/`measure` against
`points`/`isDone`/`estimatedMinutes`) with nothing in Firestore to enforce which half
applies; it rewrites `Goal.kt`, `Task.kt`, every DTO, repository, query and screen in a
working app over live data; and it **erases in storage the line `E7`/`E12` draws in
prose**. Elegance that costs a rewrite and buys a weaker invariant is not the higher
standard.

## Session hygiene

**Two live siblings were found by checking rather than by asking, after Ido pushed back
on being asked.** The session opened by reporting an uncommitted `/kb-ingest` in the tree
and a stale claim on #29. Both reports were wrong:

- the ingest was **committed** at `7aedf9f` while the map was being read;
- **#29 is live**, not stale — its board row points at
  `CHANGELOG/2026-08-09/c10-quote-feed.md` while the session actually writes
  `CHANGELOG/2026-08-10/c10-quote-feed.md`. **A claim row whose paths carry the wrong
  date reads as an abandoned session.**
- a third session, `c9a-schedule-a-task`, claimed **#25** at 14:04 while this session was
  reading, and correctly declined to contest #37 — *"assigned with no board row, left
  untouched rather than contested."* It was right that the row was missing: the GitHub
  assignee had been taken first and the board row second.

Three concurrent sessions, three disjoint tickets, one shared artefact — the map body
#12 — which was **leased** (`#gh-issue-12`) rather than owned, held only across the
edit-and-push, and released.

## 🧪 Tests

**No test layer applies.** This session produced no code, no configuration and no
schema — it produced a GitHub issue resolution, a map edit and Markdown. The project's
layers (`:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest`, `firestore-tests/`)
have nothing to assert against a planning decision, and none was run. The Gradle daemon,
both AVDs and live `goalpilot-56e30` were never touched.

The verification that *does* apply to this work is that every grounded claim in the
resolution was read out of the repo rather than recalled: `Goal.kt`, `Task.kt`,
`Constants.kt` (collection paths), `firestore.rules` (the owner-only wildcard that makes
this a client-side change), and the blocked-by graph of all 19 open tickets via
`gh api .../dependencies/blocked_by`.

## 📥 KB candidates

Two, both listed in `kb-candidates/2026-08-10-c16-milestone-model.md` and **not
ingested** — normal mode, so the list is a proposal. One of them is `rules/`-shaped and
therefore always-ask in both modes.
