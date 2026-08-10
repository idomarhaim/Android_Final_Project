# KB candidates — `c9e-event-lifecycle`, 2026-08-10

Session: `c9e-event-lifecycle` · Ticket: [#28 · `C9e`](https://github.com/idomarhaim/Android_Final_Project/issues/28) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)
Mode: `AUTO MODE`. **Neither entry is drainable in either mode** — both target `rules/`, which is always-ask.

> **Re-based before filing.** `picker-rule-consolidation` (a JARVIS visitor session) drained
> the four parked picker candidates into `rules/question-axis-naming.md` **while this session
> was resolving `#28`** — commits `d9616b9`, `5b5e113`. Both entries below were written against
> the *pre*-consolidation file and have been rewritten against the committed text, which
> already carries **Mode 6 (form)** and **The widening (fork check over the derivation
> closure)**. What survives is what those two sections do **not** cover. These are the first
> two candidates against the consolidated rule, not the fifth and sixth against the old one.

---

## 1. Mode 6's test is stated on the question; it has to be stated on the **options** — and its batch gate produced a false negative here

**Claim.** Two refinements to **Mode 6 — form**, both from one exchange:

1. **A mechanism fork survives a scenario stem.** Mode 6 contrasts *"a participant with no
   Health Connect connection joins a step race; what happens?"* (answered) against three
   mechanism questions (refused), and reads the variable as *the question's form*. This
   session's round 2 asked **two scenario-stemmed questions** — *"It's an ordinary Tuesday.
   You re-estimate three tasks and reschedule two others. How many times has GoalPilot asked
   you…?"* and *"…40 tasks shift time at once. What do you want to happen?"* — set in his own
   daily life, about his own product. He refused both, in almost the committed words: *"I
   couldn't understand what the implications of each option are."* The **options** varied along
   prompt cadence (every action / once then remember / once per launch) and dialog shape
   (count / checkboxes / never). Those are mechanisms. **The stem was a situation and the fork
   was a mechanism, and the fork is what he had to answer.** So the diagnostic is *read the
   options*, not *read the question* — otherwise an author satisfies Mode 6 by rewriting the
   stem, which changes nothing and feels like compliance.
2. **The consolidation's gate is scoped to the batch, and the split can run across batches.**
   The consolidation table admits *form* as a candidate cause **"only if some question in this
   batch was answered"**, and Mode 6 states the claim explicitly as *"inside one picker."*
   Here **nothing in round 2 was answered**, so the gate excludes the one diagnosis that was
   correct. The answered/refused split existed — round 1 was answered instantly, round 2
   refused entirely — but it ran **across two pickers minutes apart**, not inside one. The
   comparison window wants to be **the session**, not the batch.

**Why.** (1) closes a compliance loophole that the rule's own wording opens. (2) is a false
negative in the diagnostic table — the table is the part an agent actually executes under
time pressure, and it currently routes this exchange to *density* (reduce the shape) when the
shape was fine and the form was wrong.

**Rejected:** filing this as a seventh mode. The consolidation explicitly records *"this is not
a seventh mode, and filing it as one is the recorded misread"* about the widening, and the same
applies here: this is a defect in **Mode 6's test and its gate**, not a new way for an axis to
be wrong.

**Destination:** `rules/question-axis-naming.md` — edits **Mode 6** and the **consolidation
table** in place.
**Anchors:** `rules/question-axis-naming.md` §"Mode 6 — form" (lines ~355-390) and
§"The consolidation" table (~430-434) · [#28's resolution](https://github.com/idomarhaim/Android_Final_Project/issues/28#issuecomment-5245577162), whose §1 is what the delegation produced.
**Supersedes:** nothing. **Amends** `picker-rule-consolidation`'s committed text — which makes
it always-ask twice over: destination `rules/`, *and* it rewrites a standing claim.
**Status:** always-ask. Not drained.

---

## 2. The fork check's widening reaches derivation closures in code; it does not reach a **closed sibling decision** that removed the premise

**Claim.** The committed widening says: run the fork check over each quantity's **derivation
closure** — what is computed from it and what it is computed from — and look for an
intersection. That catches `c3-points-currency`'s miss, where a third *quantity*
(`estimatedMinutes`) bridged two that looked independent.

This session's round 1 was false for a reason that shape cannot reach. The options were
**actions**, not quantities — *remove the event / keep it / retitle it* — so there is no
derivation closure to intersect, and no grep that would have fired. Ido answered from outside
the set (*"the app should ask me"*), which was reasonable and also priced against a risk that
did not exist: **`C9d`, a closed sibling ticket on the same map, had already bought
`calendar.app.created` and a dedicated calendar**, so the app's entire blast radius is a
calendar it created itself. Every option in the set, his included, assumed the app could touch
calendars that matter.

**The check that would have caught it:** name the **risk or capability the options are
adjudicating**, then read the **closed decisions this ticket depends on** for something that
already bounds it. The constraint was documentary, not computational — it lived in a resolution
comment, not in a `.kt` file.

**Why this generalises rather than being one miss.** On a wayfinder map it is *structural*.
Ticket bodies are written at charting time; blockers resolve afterwards; **the frontier moves
and the body does not.** `#28` was charted asking *"is the event removed, or left as a record
of what was planned?"* — written before `C9d` narrowed the scope. Any ticket that waited behind
a blocker is at risk of carrying a premise its own blocker has since deleted, and the agent
reads the body as the statement of the question. The same true-negative-that-reads-like-a-true-positive
failure the widening describes applies here: nothing in the ticket body signals staleness.

**Rejected:** *"re-read every closed ticket before drafting any picker"* — too coarse, and the
consolidation already rejected the frequency-based fix for the same reason. The narrow version
is scoped to the ticket's **own closed blockers**, which is a set of two or three.

**Destination:** `rules/question-axis-naming.md`, as a second clause of **The widening**.
Possibly also `kb/dev/flows/wayfinder.md` — *body drifts, frontier moves* is a flow property
rather than a picker property, and may belong in both. **Ingest decides; flagging both.**
**Anchors:** [`C9d` #17](https://github.com/idomarhaim/Android_Final_Project/issues/17) (the scope decision) · [#28's resolution §1](https://github.com/idomarhaim/Android_Final_Project/issues/28#issuecomment-5245577162) · `rules/question-axis-naming.md` §"The widening" (~392-426)
**Supersedes:** nothing. **Extends** the widening; the derivation-closure form stays correct for
the case it was written from.
**Status:** always-ask. Not drained.
