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

## ✅ Fully drained — the last survivor shipped 2026-08-13

The one remaining entry was a **behaviour change rather than knowledge**, the `rules/`-shaped
exclusion `/kb-ingest` may never decide on its own. It went to Ido as a 🎬 offer on 2026-08-13,
was **waived**, and shipped — see its Status below. **This file is kept, not deleted**: the
merge brief instructs that in writing, and deleting is always-ask regardless.

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

**Status.** ✅ **SHIPPED 2026-08-13** by `picker-queue-merge` into `C:\Dev\JARVIS` commit
**`daac210`** — new `rules/agent-topology-and-model-routing.md` **§5.3(a)**, one clause on §5's
*release when done*, and a new 🧭 bullet in `user-rules/my-rules.instructions.md`. 🎬 walkthrough
**offered and waived by Ido, 2026-08-13** (`waive`, the strong form); the declined-branch
fallback ran and is recorded in §5.3.

**Both halves shipped, and the entry's own ranking of them was kept.** Half (b) —
`ls CHANGELOG/*/<label>.md`, because *the label is stable and the date is not* — is the operative
check. Half (a) (write a row's dated paths from the day you actually work) shipped as the cheap
upstream prevention, and §5.3 records it as **the weaker of the two, exactly as this entry
said**: every recorded sighting is of a **reader** misjudging, not of a writer being caught, so
(a) has no instance of its own.

**It did not ship alone, and the pairing is the finding.** §5.3 also carries
`picker-delegation-clause` entry 2 — *"commit, don't push" holds nothing in a shared tree,
because `git push` is branch-scoped outbound as well as inbound*. Both are the same claim that
auto-push precondition 5 already makes in **one** direction — ***an absent row is not proof the
session is finished*** — restated in the direction it was not: a **present** row is not proof
either, and neither is your own decision to hold. The section's heading is that unification;
the two clauses are kept separate because one is answered by a different *command* and the other
by a check at a different *moment*.

**This entry's near-miss is what the shipped clause is anchored on.** `c16-milestone-model`
opened by reporting `#29 (C10)` abandoned on four true observations, and was one round-trip from
proposing Ido unassign a **live** sibling's ticket. The session was writing into the *next* day's
folder. That is now the founding instance in §5.3's regression table.

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
