# KB candidates — `c1-points-and-time`, 2026-08-10

Session: `/wayfinder 12` → resolved [#19 · `C1`](https://github.com/idomarhaim/Android_Final_Project/issues/19),
the points-and-time model. Mode: `AUTO MODE`.

**Two entries were flagged. Entry 1 is drained; entry 2 is parked always-ask.** This file is
rewritten down to its survivor rather than deleted, per the partial-drain rule.

---

## ✅ 1 — *(ingested 2026-08-10 — no longer a candidate, kept as a pointer)*

**A running accumulator is not a derived total, and the floor it carries is what hides the drift.**

**Landed:** `C:\Dev\JARVIS\kb\dev\derive-dont-stamp.md` **§6**, as an **update in place** —
it opens §1's existing *write-derived* row rather than creating a fourth page about totals.
Index row extended, journal entry in `kb/log/2026-08-10.md`, `Check-KbLinks` **CLEAN at 61
pages**. `dev/firestore-write-semantics.md` §6 was considered as the host and **rejected**
(that page is about what `FieldValue.increment` *cannot express* and is Firestore-scoped;
this claim is about the shape of the stored quantity and holds in any store).
**Superseded nothing.**

---

## Standing — always-ask

## ⛔ 2 · *"I could not understand you — you choose"* is a **delegation**, and the rule's only remedy for it is the wrong one

**Claim.** The picker guidance in `rules/question-axis-naming.md` treats *"I don't understand
the options"* as a **legibility** failure and prescribes exactly one remedy: **make the
question smaller, do not explain more.** Observed here is a case that presents identically
and is not that: Ido said the first question was not legible, asked for *"a simple, schematic
explanation"*, **and in the same breath handed all three questions back** with his standing
*take the highest-quality answer and improve it*.

Those two halves pull in opposite directions. The legibility clause says re-ask, shorter. The
hand-back says **do not re-ask at all** — re-asking a delegated question is not a smaller
question, it is a refusal to accept the delegation. Following the written remedy here would
have been wrong.

**Why it matters, and what was rejected.** The tempting reading is *"he refused, so the
questions were mine to answer"* — which the ownership clause already covers. That is **not**
what happened: he did not say the questions were misdirected, he said one was **unreadable**
and then delegated **all three**, including the two he had not complained about. So the
signal is not *"you asked the wrong person"* and not *"you asked badly"* — it is a **third
thing**: *the cost of getting me to a position where I can answer exceeds the value of my
answering*. That is a judgement about the **exchange**, not about the question or the axis.

The remedy that seems to follow is also worth recording as **rejected**: *"ask fewer, simpler
questions next time"* is what the existing clause already says, and it does not fit — the
three questions here were on three genuinely different axes, each already reduced. What
actually discharged the turn was **explaining the schematic once, in the reply, and then not
re-asking anything** — a shape the rule has no name for.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — either a **seventh mode**
beside the six `picker-rule-consolidation` merged on 2026-08-10, or (more likely, and this is
the drafter's call not a drainer's) a **clause on the existing legibility mode** distinguishing
*re-ask smaller* from *accept the delegation and explain once*. It is **post-consolidation**:
that merge shipped hours before this observation, so this is a genuinely new sighting rather
than a fifth copy of an already-drained one.

**Anchors.** `CHANGELOG/2026-08-10/c1-points-and-time.md`; the resolution comment on
[#19](https://github.com/idomarhaim/Android_Final_Project/issues/19), whose opening paragraph
records the hand-back verbatim; `C:\Dev\JARVIS\CHANGELOG\2026-08-10\picker-rule-consolidation.md`
for what the six modes already cover.

**Supersedes.** Nothing. It is additive to a rule amended the same day, and should be read
**with** that amendment rather than folded into it by a session that cannot approve it.

**Status.** ⛔ **Always-ask — parked for Ido, 2026-08-10.** Destination `rules/`, which
`/kb-ingest` may not take in **either** mode. Not drainable by any session in this repo; it
moves through a JARVIS session on Ido's word, exactly as its four predecessors did.
