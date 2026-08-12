# c5-endless-goals — #21 (`C5`): how are endless and maintenance goals modelled?

> **Status: resolved and closed.** Two halves, written as the session ran: the **claim** below
> (frontier derivation, why `#21` and not the other two), then **[the resolution](#resolved--21-closed-the-ticket-had-no-schema-change-in-it-at-all)**
> after Ido handed the decision back. `/wayfinder 12` was invoked with the **map**, not a ticket, so
> both the pick and — after the hand-back — the answer were the agent's.
> [`#21 · C5`](https://github.com/idomarhaim/Android_Final_Project/issues/21) was **assigned on
> GitHub before any work**, which is what the claim is.

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

---

# Resolved — #21 closed: the ticket had no schema change in it at all

> **Outcome first.** [`#21`](https://github.com/idomarhaim/Android_Final_Project/issues/21) is
> **resolved and closed**. **There is no third goal kind and nothing decays.** *Endless* and
> *maintenance* are **views**, not kinds — **zero new fields on `Goal`, zero migration** — and a
> maintenance goal shows **two numbers**, attainment (history, does not decay) and upkeep (derived
> from occurrences, never a percentage). **The decision is the agent's**, taken on Ido's hand-back.
> Filed [#41 · `C19`](https://github.com/idomarhaim/Android_Final_Project/issues/41); retired the
> Firestore-migration fog. Full text:
> [resolution comment](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5273302788).

## The hand-back, and what it obliged

Ido answered the picker with a **delegation**: *"I couldn't fully understand you or what each
option means — explain it simply and schematically, and pick the solution that gives the app and
its purpose, the UX/UI and the software the highest standard and quality; and if you think the
solution you picked can be improved, improve it."*

Per `C:\Dev\JARVIS\rules\question-axis-naming.md` that governs the whole turn, and all four duties
were executed as written: **not re-asked in any form** (not smaller, not as a scenario, not a
narrower picker); the *couldn't-understand* half paid **once in the reply as an explanation**
rather than as a preamble to another question; the answer **derived** rather than taken by picking
my own Recommended; and the reasoning put on the **durable record saying the decision was the
agent's** — on `#21`, on `#12`'s index line, and here.

## The finding: the fork was false, and the tell was Ido's inability to read it

The rule warns that a delegated answer is often **not one of the options offered**. It was not.

Re-running the fork check **one hop wider than the pair** found the shared third quantity — `C9a`'s
**occurrence stream** — which correctly said none of the three options was a schema choice. But the
defect was one hop further out again: **the falling bar and the held bar were not rival values of
one quantity. They were two different quantities**, and `C3` (#18) had **already ruled that both
must exist** — *effort* and *outcome*, whose gap is *"the app's most valuable signal, not a bug to
tidy away"*. A picker built on that premise had no answer to give, which is why a question that was
already concrete, already scenario-shaped and already one axis wide was still unreadable.

**This is what the existing fork check does not catch**, and it is filed as candidate entry 1.

## The resolution, in seven sections

1. **No `GoalKind`.** *Endless* = intrinsic objective + **no measure** (`C7`: absence is the
   default) + instrumental tasks carrying `C9a`'s repeat rule. *Maintenance* = the same, plus a
   measure reached at least once. The kind is **read off the goal, never written onto it** — a
   stored `GoalKind` is a judgement derivable from per-item facts (`kb/dev/enum-and-label.md` §5)
   and would be wrong the moment a repeat rule is added. `E9`'s invitation, folded here by `C4`,
   **declined**.
2. **Two numbers, not one** — attainment `(current − start)/(target − start)`, history, does not
   decay; **upkeep** derived from occurrences with nothing stored, in `C9a`'s vocabulary
   (`fresh · due · overdue — 7 days`) plus the window run.
3. **An endless goal has no percentage and that is not degraded** — `C10`'s themes key on days
   idle, open work and age, so it is *well-aimed, not degraded*, and it must not be sent as
   `progressPercent: 0`.
4. **The history question dissolves**, and the map's standing fog question is answered: an infinite
   or maintenance goal **can** fail — **per window, never as a whole**. `MISSED` is a failure,
   `OVERDUE` is not, `EXPIRED` counts for nothing.
5. **Points are never clawed back** (derived from `C3`, logged not asked), and **`C3`'s
   convergence constraint is discharged**: a recurring task's completion never enters an outcome
   denominator at all.
6. **`C9a`'s shape inherited, not re-decided**, per Ido's own call on #25 — and `C7`'s period
   hand-off lands on the **repeat rule**, not a new `Goal` field.
7. **Improvements, since he asked:** `pausedUntil: Long?` on the repeat rule — **the one stored
   field this ticket adds**, without which the design manufactures the very failure §2 refuses;
   `C9b`'s *one chip may not carry two axes* binds the two numbers apart; `C15`'s *authored
   natively per language* binds the upkeep line, and `7 ימים באיחור` needs direction isolation.

**Overriding Ido's own decay proposal was the one contestable call**, and the deciding ground was
not taste: [`DashboardViewModel.kt:103`](../../app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt)
means a decaying bar drifts the dashboard average **while he sleeps**, and
[`RecommendationRepositoryImpl.kt:175`](../../app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt)
means goals cross the *needs attention* threshold **unprompted**. `C12` drew every chart it shipped
yesterday against a number that only moves when he moves it.

## Commons, filings and the map

- **`#12`'s body: re-fetched immediately before the write and `cmp`'d byte-for-byte** against the
  copy the line was built on — **unchanged, no race**. Verified **22 → 23 decision lines** and
  **6 → 4 fog bullets**, exactly **two deletions**, both the intended fog patches, then read back
  and diffed: identical but for one trailing blank line GitHub appends.
- **Filed [#41 · `C19`](https://github.com/idomarhaim/Android_Final_Project/issues/41)** and wired
  it as a sub-issue of `#12`, graduating the *per-life-area success and failure* fog whose own text
  said it hung on `C5` alone.
- **Retired the Firestore-migration fog outright** — `C5` was the last ticket that block waited on.
- **Commented on nothing:** every hand-off this ticket held is on a closed ticket.

## 📥 KB

- **Entry 2 drained cross-repo** — *a value that changes with wall-clock time silently rewrites
  every aggregate over it* → `C:\Dev\JARVIS\kb\dev\derive-dont-stamp.md` **§7** (`385e87b`).
  **No new page:** the grep found that page already owning derived-vs-stored from **this repo and
  this map** (`C9a`, 2026-08-10); what was new is the **consumer**-side argument — grep a value's
  **consumers**, not its writers. `Check-KbLinks` **CLEAN, 65 pages**. A **visitor row was held on
  the JARVIS board** for that unit, since the board follows the repo being written to.
- **Entry 1 held** — ⛔ always-ask, `rules/question-axis-naming.md`'s **fork check**. It did work
  while parked: `picker-queue-merge` read it off the board note and **corrected a clause it had
  shipped two minutes earlier** (`3d0971a`), widening *"when the options are actions"* to
  *"whenever the closure grep terminates without collapsing the fork"*. Its own distinct claim is
  the **seventh** parked amendment to that file — flagged in place, not folded, not shipped.
- The candidate file is **rewritten down to its survivor, not deleted.**

## ⚠️ Defect caused, not found

The JARVIS-side changelog and the regenerated `CHANGELOG_README.md` were **staged when
`picker-queue-merge` committed**, so both rode into **their** commit `3d0971a` rather than this
session's `385e87b`. Nothing lost, nothing rewritten — a history rewrite is always-ask. It is the
exact cross-contamination the explicit-path staging rule exists to prevent, arriving from the
**other direction**: the rule stops *you* sweeping a sibling's work in and says nothing about a
sibling sweeping *yours*. The narrow lesson: in a repo with a live sibling, **stage and commit in
one breath** — the window between `git add` and `git commit` is the whole exposure.

**The JARVIS commit is deliberately not pushed.** `picker-queue-merge` is still live on that board
and its commits are in the range, which is a stop-and-ask under auto-push precondition 5.

## 🧪 Tests — resolution half

**No code layer applies** — this is a wayfinder decision ticket and the map's standing preference is
that *"no ticket on this map ships code"*. Nothing was built, no device or emulator used, the Gradle
daemon untouched. **The layer that did run is the KB bundle's own lint**, for the cross-repo half:
`Check-KbLinks.ps1 -BundlePath C:\Dev\JARVIS\kb` → **CLEAN, 65 pages** (1 skipped under `_derived\`,
6 journal day-files exempt from the orphan check) — no broken links, no orphans, no wikilinks. Two
structural checks stood in for a test on the commons write: the `cmp` against the pre-write copy of
`#12`, and the insertion/deletion counts verified before sending and diffed on read-back.
