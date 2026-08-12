# KB candidates — `c8-ai-task-plans`

**Session:** `c8-ai-task-plans` · **Date:** 2026-08-10 · **Repo:** `C:\Dev\Android_Final_Project`
**Ticket:** [#24 · `C8`](https://github.com/idomarhaim/Android_Final_Project/issues/24) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

Each entry stands alone. No transcript is a source.

---

## 1 · The framing tell can fire with the axis *right* and the enumeration *short* — and the missing member is never in the code

**Claim.** `rules/question-axis-naming.md` records the tell *"I answer fluently, and the answer
is not on the menu"* under **granularity** (right dimension, wrong unit) and, in its own
paragraph, under **framing** (*"every option was a property of the same kind of thing and the
axis I hold is not in the set"*). Round 1 of `C8` produced a **third** shape that neither
description covers, and the remedy differs.

The question was: *the AI proposed 7 steps, you delete #2 and #5 — what happens to the
remaining 5?* Three options, all variants of one axis (**how much the AI reacts to a
deletion**): renumber only · renumber + flag dependents · re-plan the gap. The axis was
**correct** — Ido's answer moves along it, and it is not a unit-vs-dimension error either;
nothing about scope or per-instance granularity is in play.

What his answer added was a **member of the action set the question quantified over**, not a
different axis. My options assumed a proposed step has **two** exits — *keep* or *delete*. He
named a third: **already done**. *"If the tasks were already carried out, the user can mark
them as already done in the proposal — they are not deleted, and the agent takes that into
account in its planning."* Marking-done is not a weak deletion; it is **evidence flowing the
other way**, into the next plan. He then split *delete* into two acts with different
consequences (a `Renumber` button, versus an `explain delete` free-text box feeding an
`Adjust Plan` call) — so the axis I named turned out to be a **user-chosen action taken after
the fact**, not a policy to be set once.

**Why.** This matters because the two remedies already on the books both fail here.
*Granularity* says "derive the scope my answer left open" — but no scope was left open; a
whole exit was missing. *Framing* says "the axis I hold is not in the set" — but it was in the
set. The operative failure is that the question **quantified over an enumeration the agent
authored**, and the agent could only enumerate the exits it had seen in the code. `keep` and
`delete` are both present in `TaskRepository`; `already done in a proposal that has not been
committed yet` exists nowhere, because the draft state itself does not exist yet. **A
derivation-closure grep cannot find a member that the code has no reason to contain** — which
is the same blind spot *The widening* names for a closed sibling decision, one step further
out.

Rejected: filing this under the existing *framing* row. Its remedy (*more options on that axis
would not have helped*) is **false here** — a fourth option reading *"mark it already done"*
would have landed the answer exactly. That is the discriminator between the two, and folding
them together would delete it.

**Proposed shape.** A clause under the tell table: when a question quantifies over a **set of
actions or states the agent enumerated itself**, the completeness of that set is the first
thing to check — and where the object being acted on **does not exist in the product yet**,
assume the set is short and ask the user to complete it rather than adjudicating between the
members you could think of.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — a clause on the tell table,
beside the existing *granularity* and *framing* rows.

**Anchors.** `#24` round 1, question 2, and Ido's reply naming *mark as already done* ·
`Renumber` · `explain delete` → `Adjust Plan`. Sibling precedent for a rule-level miss caught
by an out-of-set answer: `C4` ([#13](https://github.com/idomarhaim/Android_Final_Project/issues/13))
§1, where four proposed proxies were all properties of the object and his discriminator was a
property of the relationship.

**Supersedes.** Nothing. It **extends** the tell table that `picker-rule-consolidation`
(`d9616b9`) built and that `c9e-event-lifecycle`'s two parked entries also amend — those three
should be read together before anyone edits that file.

**Status.** ⛔ **Always-ask — not drained.** Destination `rules/`, which `/kb-ingest` may not
take in either mode. `AUTO MODE` does not apply.

---

## 2 · ~~Don't buy a global judgement you can derive from per-item enums~~ — **✅ DRAINED**

**Ingested 2026-08-12** into `C:\Dev\JARVIS\kb\dev\enum-and-label.md` **§5**, commit
`3d30391` in the JARVIS repo, released `7850e0e`. `Check-KbLinks` **CLEAN at 63 pages**;
nothing superseded; `kb/index.md` row and `kb/log/2026-08-12.md` updated in the same commit.

Full text of what landed and why it went into an existing page rather than a new one:
`C:\Dev\JARVIS\CHANGELOG\2026-08-12\c8-ai-task-plans-ingest.md` and the journal entry in
`kb/log/2026-08-12.md`.

**This file is rewritten down to its survivor rather than deleted** — entry 1 above is still
pending, so the partial-drain rule applies and the file stays until entry 1 is resolved.
