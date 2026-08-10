# KB candidates — `c16-milestone-model`, 2026-08-10

Written during `/wayfinder 12` (resolved
[#37](https://github.com/idomarhaim/Android_Final_Project/issues/37), `C16`).
**Not ingested** — normal mode, so this list is a proposal.

Each entry stands alone: no chat transcript is a source, and none is needed to write the
page.

---

## 1. Two "rival" numbers are usually one number and its mechanism

**Claim.** When a design question asks *"which of these two progress numbers wins?"*,
check first whether one of them **produces** the other. If it does, the question is
malformed and answering it as posed ships the malformation: every option on the table
inherits the false premise that the two are independent measurements.

Concretely, in GoalPilot: an objective's target (`6 / 10 courses`) and the completion of
the work beneath it (`1 of 3 tasks`) look like two competing answers to *"how far
along?"*. They are not — `Task.progressContribution` already means that completing a task
**advances the objective's number**. So there is exactly one number, and the work below
is how it moves.

**Why.** This session put the fork to the user as a three-option picker with a diagram
each, and he could not answer it — correctly, because none of the three was right. The
reframing produced a **fourth answer, better than all three offered**, and it also
disqualified two of them on independent grounds:

- *"show both numbers"* does not decide, it **defers** — and the deferral resurfaces at
  every ancestor, because a parent can only inherit one;
- *"the work below wins"* means splitting one task into three drops progress
  33% → 25%, so the app **punishes the user for planning properly** — a general
  anti-pattern in any roll-up, not a GoalPilot quirk.

The generalisable test: before adjudicating between two quantities, ask whether one is
already the *cause* of the other in the code that exists. If it is, collapse them.

**The collapse also exposes a signal that neither framing could see.** Once there is one
number, *the listed work summing to less than the target* becomes visible and meaningful
— "everything you planned adds up to 3 of 10". It is **subtraction, not inference**, so
it needs no model, no key and no network. Removing a false choice tends to reveal a true
one.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — new page, working title
`rival-numbers-are-one-number-and-its-mechanism.md`. Adjacent to
`dev/decision-map-charting.md`.

**Anchors.** [#37 resolution §4](https://github.com/idomarhaim/Android_Final_Project/issues/37) ·
`CHANGELOG/2026-08-10/c16-milestone-model.md` §"The finding worth keeping" ·
`app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt` (`progressContribution`).

**Supersedes.** Nothing.

**Status.** Ready to ingest — additive, contradicts no standing claim.

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
one round-trip from proposing that the user unassign a live sibling's ticket.

Rejected as the fix: *"check the GitHub assignee's activity instead"* — the assignee is
the user's own account on every ticket in this repo, so it carries no signal. Also
rejected: *"treat every claim as live"* — that defeats the board's stale-claim rule,
which exists because a stale claim blocks work nobody is doing.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — fold into the existing claim-board /
parallel-sessions page if one exists; otherwise a new page beside it. **Check before
ingesting:** this touches how `SESSIONS.md` rows are written and how a session
investigates a suspected-stale row, which is `rules/`-shaped territory
(`rules/agent-topology-and-model-routing.md` §5) rather than a knowledge page.

**Anchors.** `SESSIONS.md` (the `c10-quote-feed` row, `CHANGELOG/2026-08-09/…` vs the
actual `CHANGELOG/2026-08-10/c10-quote-feed.md`) ·
`CHANGELOG/2026-08-10/c16-milestone-model.md` §"Session hygiene".

**Supersedes.** Nothing. Extends the stale-claim guidance rather than replacing it.

**Status.** ⚠️ **Always-ask.** If the verdict is that this belongs in
`rules/agent-topology-and-model-routing.md` §5 rather than in `kb/`, it is a change to
how the agent behaves and the 🎬 walkthrough rule owns it — not `/kb-ingest`.

---

## Not filed here, deliberately

**"'I could not understand the options' means the axis is not reduced far enough."** Hit
twice in this session — on branch 1 and again on branch 4 — and in both cases the fix was
not simpler wording but a further reduction of the axis, after which one option turned
out to be **wrong** rather than merely unchosen. This is **already flagged** by
`c10-quote-feed` as entry 4 in `kb-candidates/2026-08-10-c10-quote-feed.md`, where it is
held as always-ask. Recorded here as **corroboration from an independent session on the
same day** — a second, separate occurrence — and not as a second candidate, so the two do
not race to create the same page.
