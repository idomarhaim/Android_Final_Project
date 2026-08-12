# c5-endless-goals — claimed #21 (`C5`): how are endless and maintenance goals modelled?

> **Status: claimed, not yet resolved.** `/wayfinder 12` was invoked with the **map**, not a
> ticket, so the pick was the agent's. [`#21 · C5`](https://github.com/idomarhaim/Android_Final_Project/issues/21)
> is **assigned on GitHub before any work**, which is what the claim is. The resolution is a
> `wayfinder:grilling` ticket — HITL — so it is Ido's to answer and this entry grows when it does.

## The frontier, derived twice — because it moved underneath the derivation

Derived from the **dependencies API**, never read off the board's stale *Unclaimed work* block:
`/issues/12/sub_issues` enumerated, then every open child queried for `blocked_by`.

**At session start (00:36)** — 25 children, 21 closed, 4 open:

| Ticket | Blocked by | Assignee | Verdict |
|---|---|---|---|
| `#20 · C2` | `#19` ✅ | `idomarhaim` | unblocked but **live** (`c2-task-type`) |
| `#21 · C5` | `#13` ✅ `#18` ✅ | — | **frontier — CLAIMED** |
| `#30 · C11b` | `#19` ✅ **`#20` open** `#24` ✅ `#29` ✅ | — | blocked |
| `#35 · C15b` | `#24` ✅ `#29` ✅ | — | frontier |

**Re-derived after claiming**, because two siblings released mid-session — `c6-log-progress`
closed `#22` at `faddfc7` (00:41) and `c2-task-type` closed `#20` at `b9d1be7` (00:43):

| Ticket | Blocked by | Assignee | Verdict |
|---|---|---|---|
| `#21 · C5` | `#13` ✅ `#18` ✅ | `idomarhaim` | **CLAIMED — this session** |
| `#30 · C11b` | `#19` ✅ `#20` ✅ `#24` ✅ `#29` ✅ | — | **newly unblocked** |
| `#35 · C15b` | `#24` ✅ `#29` ✅ | — | frontier |

**The map now has no blocked ticket at all** — three open, three unblocked, and this is the first
derivation of the effort where that is true. It follows that **leverage discriminates nothing**:
closing any of the three unblocks nothing, because nothing is left to unblock. The pick therefore
had to be made on collision and readiness rather than on what it frees.

## Why `#21`, and the two declines

**The objection that refused `C5` at every earlier derivation has expired.** `c8-ai-task-plans` and then
`c6-log-progress` both declined it on a **subject** collision — *"`C5`'s decay mechanic changes
what a goal's **percentage** means, and a goal's percentage is what `#31`'s charts render"* — with
`c12-charts-presentation` then at revision 3 with Ido. **`#31` is closed and released**
(`22ac7d9`, 2026-08-12 20:52), so it is now foreign state to *read*. That is the same shape of
reasoning `c2-task-type` used thirteen minutes ago to take `#20` after six refusals: the condition the
refusal itself named is met, so taking it now obeys that decision rather than overturning it. The
four grounds older than that (proximity to the then-live `#19`, then `#28`) had already expired.
Both of `#21`'s own blockers — `C4` (#13) and `C3` (#18) — are closed and long released.

1. **`#35 · C15b` — declined on a *freshness* collision.** It asks what happens to
   already-generated AI text when the language changes, and **the set of AI-generated fields was
   being re-cut in the same minute**: `c2-task-type` closed `#20` at 00:43, deciding whether a
   task carries an AI-assigned type at all. It is now takeable and is the natural next claim.
2. **`#30 · C11b` — declined because it graduated onto the frontier ninety seconds before the
   claim row was written.** It is the per-feature output-format spec for **every** AI feature, was
   blocked by `#20` until `b9d1be7`, and is the map's terminal ticket by design (*"you cannot test
   a format nobody has designed yet"*). Taking it before the surviving decisions land inverts the
   map.

## Couplings named on claiming, not discovered later

1. **`#12`'s *Decisions so far* is a commons and its race has fired for real twice** — re-fetch
   `#12`'s body immediately before appending, `cmp` against the copy the line was built on, write
   only this session's line, verify a pure insertion afterwards.
2. **Two closed tickets already handed work to `#21` by name.** `C7` (#14): *"The period is
   `C5`'s — `E18`'s '4 km' is settled here, 'a week' is #21."* `C4` (#13) **folded `E9`'s
   third-goal-kind invitation into this ticket rather than filing it**. Inputs to read, never
   decisions to reopen.
3. **The ticket's own framing may be partly obsolete and does not know it.** It asks *"what is its
   percentage, if it has one at all?"*, but `C7` has since made **a measure optional with absence
   the default** (`E6`) and `C4` made goal/milestone **roles carried by an edge** — so "endless"
   may already be sayable without a new goal kind. Read as an input; not re-scoped unilaterally.

## Board and candidates

- **`SESSIONS.md` claimed under a lease** (`Lock-Path.ps1 -Action Acquire -Session
  c5-endless-goals -Path SESSIONS.md`), one row plus one note, released after this commit.
- ⚠️ **Two rows this session did not write and deliberately did not fix.** `ux-backlog-triage` is
  committing into this repo **with no row on the board** (two untracked files on disk);
  `c6-log-progress`'s row is **stale** — it closed `#22` at `faddfc7` and has not released. A row
  another session invents is a report, not a claim, and a released row is that session's to write.
- 📥 **`kb-candidates/` listed before the first unit of work — five files**, each opened rather
  than inherited: `c16` ⚠️, `c9e` ⛔, `c8` ⛔ (all `rules/`), `c12` (entry 1 always-ask, the rest
  held by their own text), and the untracked `2026-08-13-ux-backlog-triage.md`. **None is this
  session's**, so `AUTO MODE` drains nothing here — the auto-ingest gate covers the candidates the
  committing unit produced.

## 🧪 Tests

**No test layer applies.** This is a wayfinder decision ticket: it ships no code, touches no
Kotlin, no Cloud Function and no `firestore.rules`. The map's own standing preference is explicit
— *"No ticket on this map ships code."* Nothing was built, no device or emulator was used, and the
Gradle daemon was not taken.
