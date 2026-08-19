# c1-points-and-time — claimed #19, the ticket every blocked ticket waits behind

> **Summary:** claimed #19, the ticket every blocked ticket waits behind

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

---

# Part 2 — the resolution

## How the decision was reached

Three picker questions went to Ido: which input a human may author and whether the reward
follows it · what `R10`'s re-score may move · whether multi-purpose work earns a bonus.
**He answered none of them on their merits.** He said the first was not legible, asked for a
simple schematic explanation, and handed **all three** back with his standing instruction —
*take the highest-quality answer and improve it.*

**No question was re-asked.** The ❓ rule's remedy for *"I don't understand"* is *make it
smaller, never explain more* — but that governs a **legibility** failure, and this was a
**delegation**. Re-asking a delegated question is not a smaller question, it is a refusal to
accept the delegation. What was owed instead was the schematic he asked for, once, in the
reply. Filed as an always-ask KB candidate, since the rule has no name for this shape.
So every pick below is the agent's and is on the record in the resolution comment.

## The decision

**`R7`'s line is not human-vs-AI. It is fact-vs-judgement.**

- **`minutes` is a fact about Ido's life** and he is its authority; **`difficulty` is a
  judgement about the work** and only the model makes it (`C7`'s *the AI judges, the app
  computes*, priced by `C11a`: prompt-declared enums **50/50**, free numbers swinging **2×**);
  nobody authors their product. That restates `R7` more precisely than its own wording and
  **dissolves #19's fourth bullet** — a human may never correct points and never needs to.
- **`R8`'s box wins: points recompute from a typed duration.** The alternative was killed not
  by the cheat argument but by this map's most repeated finding — it stores a second number
  quietly disagreeing with the one on screen (`C7`'s `"%"`, `C3`'s `1.0`, `C18`'s residual).
  The cheat objection is weak: `C14` already banned `points` from challenge scoring, so an
  inflated total wins nothing against anyone.
- **A hand-typed duration is sticky, unconditionally** — answering [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9)
  without the threshold it was waiting for, because any threshold makes the app judge when
  Ido is wrong about his own day.
- **What is banked on completion is the inputs, not the number.** Points stay computed; the
  lifetime total is a sum over timestamped completion facts.
- **`R10`** runs one **wide** call per *plan* (`C11a`), over **open leaves only** (`C18`),
  never touching a completed task or a hand-typed number, and **disclosed**.
- **`R9`'s shape: the model never emits a point value** — `taskId` (membership-checked),
  `difficulty ∈ LIGHT · ROUTINE · DEMANDING` at ×0.75/×1.0/×1.5, `estimatedMinutes`.
- **Shared work is paid once, in full, and said out loud** — no bonus (`C4` §9 lets the app
  add edges silently), recognition at the tick, counting only edges with a declared
  contribution (`C3` §5).
- **`points` moves server-side** — fourth site of one pattern, at no product cost.
- **Levelling needs no new thresholds**; only the ceiling rises 50 → 240.

## The defect this closed

[`TaskRepositoryImpl.kt:120-127`](app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt)
keeps the lifetime total as a **running accumulator** — `newPoints = currentPoints + sign *
task.points` — reading `task.points` **at untick time**. Tick a task worth 10, let anything
re-score it to 30, untick: the total loses 30 for a 10 that was added, and `.coerceAtLeast(0)`
**silently absorbs** the drift at the floor. Dormant only because nothing re-prices a task
yet — `R10` would have made it routine.

## The commons race fired for the second time

`#12`'s body grew **139 → 141 lines between this session's session-start fetch and its
write** — `c9b-calendar-surface` appended `C9b`'s index line in that interval. Re-fetching
immediately before the edit is the only reason that line survives. Verified afterwards:
141 → 143, **0 deleted lines**, all **16** decision lines present, and the written body read
back identical but for a trailing newline GitHub appends.

## 📥 KB

- **Ingested:** *a running accumulator is not a derived total* → `C:\Dev\JARVIS\kb\dev\derive-dont-stamp.md`
  §6 (update in place; `dev/firestore-write-semantics.md` considered and rejected as host).
  `Check-KbLinks` **CLEAN at 61 pages**. JARVIS commit `8affcf6`.
- **Parked, always-ask:** a fresh **post-consolidation** picker sighting for
  `rules/question-axis-naming.md` — see `kb-candidates/2026-08-10-c1-points-and-time.md`.

## 🧪 Tests

**None run, and none owed** — a `wayfinder:grilling` ticket on a planning map, and the map's
standing preference is *plan, don't do*. Nothing touched `app/`, `functions/` or
`firestore-tests/`; no server, client, endpoint, database or UI layer was exercised. The KB
half ran the bundle's own gate instead: `Check-KbLinks.ps1` **CLEAN, 61 pages**, and JARVIS's
pre-commit hooks (parity, link check, changelog index) all passed.

## ⚠️ Not pushed to JARVIS

The JARVIS commit `8affcf6` is **committed and deliberately not pushed.**
`git log @{u}..HEAD` there carries **three foreign commits** from
`picker-rule-consolidation`, and although its board row says released, its brief
`sessions/done/picker-rule-consolidation.md` is **still modified and uncommitted in the
tree** — which the push rule names as evidence the session is live, not finished. Ido's call.
