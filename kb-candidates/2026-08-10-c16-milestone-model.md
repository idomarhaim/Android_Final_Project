# KB candidates — `c16-milestone-model`, 2026-08-10

Written during `/wayfinder 12` (resolved
[#37](https://github.com/idomarhaim/Android_Final_Project/issues/37), `C16`).

> **Partially drained 2026-08-10** by the same session, on Ido invoking `/kb-ingest`.
> **Entry 1 is ingested** and removed from this file — it landed in `C:\Dev\JARVIS\kb`
> as `dev/one-metric-and-its-mechanism.md` (new). See `kb/log/2026-08-10.md` for the
> full record; that journal entry is the only tie between this file and that page,
> because they live in different repos.
>
> `Check-KbLinks` **CLEAN at 48 pages** after the ingest.
>
> **Nothing was superseded** — the entry was additive, and the reconciliation grep
> **confirmed** its destination rather than correcting it, which is the rarer outcome.

---

## Standing — always-ask

One survivor. It is a **behaviour change rather than knowledge**, which is the
`rules/`-shaped exclusion `/kb-ingest` may not decide on its own. It is **not dropped**,
and it goes into `/handoff` if a session ends before Ido rules on it.

---

## 2. A claim-board row whose paths carry the wrong date reads as an abandoned session

**Claim.** `SESSIONS.md` rows list the paths a session owns. When one of those paths is a
dated artefact (`CHANGELOG/YYYY-MM-DD/<label>.md`, `kb-candidates/YYYY-MM-DD-<label>.md`)
and the date is wrong — typically because the session was claimed near midnight, or the
row was copied from an earlier one — the next session **cannot distinguish it from an
abandoned claim**: it checks the declared path, finds nothing there, and reports a stale
row for a session that is running right now.

The fix has two halves and the second is the one that survives: **(a)** a row's dated
paths must be written from the day the session is actually working, not the day it was
planned; **(b)** a session investigating a suspected-stale claim must check the **label**
across all day folders (`ls CHANGELOG/*/<label>.md`) rather than the exact declared path,
because the label is stable and the date is not.

**Why.** This session opened by reporting `#29 (C10)` as an abandoned claim on exactly
this evidence — assigned since the previous day, no changelog at the declared path, no
candidate file, no comment on the issue. All four observations were true and the
conclusion was wrong: the session was live and writing to the *next* day's folder. It was
one round-trip from proposing that Ido unassign a live sibling's ticket.

Rejected as the fix: *"check the GitHub assignee's activity instead"* — the assignee is
Ido's own account on every ticket in this repo, so it carries no signal. Also rejected:
*"treat every claim as live"* — that defeats the board's stale-claim rule, which exists
because a stale claim blocks work nobody is doing.

**Destination.** `rules/agent-topology-and-model-routing.md` §5 — **not** a `kb/` page.
Both halves of the fix change how an agent behaves (how a row is written, and what a
session does before reporting a stale claim), which is the 🎬 walkthrough rule's
territory.

**Anchors.** `SESSIONS.md` (the `c10-quote-feed` row, declaring
`CHANGELOG/2026-08-09/…` against the actual `CHANGELOG/2026-08-10/c10-quote-feed.md`) ·
`CHANGELOG/2026-08-10/c16-milestone-model.md` §"Session hygiene".

**Supersedes.** Nothing. Extends the stale-claim guidance rather than replacing it.

**Status.** ⚠️ **Always-ask — parked 2026-08-10, awaiting Ido.** Destination `rules/`, so
`/kb-ingest` may not take it in either mode. Held, not dropped.

---

## Not filed here — checked against what already shipped, on Ido's call

**"'I could not understand the options' means the axis is not reduced far enough."** Hit
**three times** in this session — branches 1, 2 and 4.

This looked like a fourth candidate converging with two parked ones. **It is not: both
had already shipped**, hours earlier the same day, and reading them was cheaper than
drafting anything —

| Parked entry | Where it actually is |
|---|---|
| `2026-08-09-entity-model-intake.md` entry 1 | **`rules/question-axis-naming.md`** — *"Name the axis before you offer the options"*, in force 2026-08-10, applied to `user-rules/my-rules.instructions.md` and projected |
| `2026-08-10-c10-quote-feed.md` entry 4 | the same page's **"Amendment — option density"** — *the picker carries the choice, the reply above it carries the reasoning; when the user says they do not understand, make the question **smaller**, do not explain more* |

So this session's occurrence is a **third data point for a rule already in force**, not a
new claim — and both of those candidate files carry a stale *"awaiting Ido"* status that
belongs to released sessions in this repo.

**One clause is genuinely uncovered, and it is what branch 4 exposed.** The shipped rule
catches *the axis is wrong* and *the options are too dense*. Neither catches **a fork
whose premise is false** — two options that are secretly one thing and its mechanism,
where reducing the axis cannot help because the axis should not exist at all. The
detection differs too: the existing rule keys on the user not understanding, whereas this
keys on a **grep of the code for a write path from one quantity to the other**, runnable
before the options are drafted. The design half is now
[`kb/dev/one-metric-and-its-mechanism.md`](https://github.com/idomarhaim/Android_Final_Project) in the
central bundle; the **asking** half would be one sub-bullet on `question-axis-naming.md`.

**Not drafted here** — `rules/`-shaped, so the 🎬 walkthrough rule owns it and it is
Ido's to call.
