# KB candidates — `c5-endless-goals`, 2026-08-13

Session: `c5-endless-goals` · `/wayfinder 12` → [#21 · `C5`](https://github.com/idomarhaim/Android_Final_Project/issues/21), resolved and closed.
Each entry stands alone. No other session's transcript is a source for any of them.

**Partly drained 2026-08-13 by `/kb-ingest` under `AUTO MODE`.** Entry 2 landed as
[`kb/dev/derive-dont-stamp.md`](file:///C:/Dev/JARVIS/kb/dev/derive-dont-stamp.md) **§7**
(journal: `C:\Dev\JARVIS\kb\log\2026-08-13.md`). Original numbering is kept, so the gap at 2 is
deliberate. This file is **not deleted** while entry 1 stands.

---

## Standing — always-ask

## 1 — The fork check misses the case where the two options are not rival values but rival *quantities* ⛔

**Claim.** `rules/question-axis-naming.md`'s fork check tells you to run the derivation closure and
look for a **third quantity** through which one option produces the other. That catches a fork
whose two sides are causally linked. It does **not** catch the case that fired here: the options
were not two values of one quantity at all — they were **two different quantities the system had
already ruled must both exist**, so neither could win and the picker had no answer to give. The
proposed amendment is one clause: **before offering options, name the quantity each one measures;
if two options measure different quantities, the answer is "both", and the question was a
decomposition failure, not a choice.**

**Why, and what was rejected.** The picker on `#21` offered a maintenance goal's bar **falling**,
**absent**, or **held**. Ido could not read it — *"I couldn't fully understand you or what each
option means"* — and handed the decision back. The pair-wise fork check **passed**: falling and
held are not derivable from one another. Widening one hop found the shared third quantity (`C9a`'s
occurrence stream), which correctly said none of them was a schema choice — but it still framed
them as rivals. The actual defect was one hop further out: `C3` (#18) had **already decided** that
a goal carries **two** quantities, *effort* and *outcome*, and that *"the gap between them is the
app's most valuable signal, not a bug to tidy away"*. "Falling" is upkeep; "held" is attainment;
the answer is both, drawn separately. **Rejected as the diagnosis:** *density* (making the picker
smaller would have produced a smaller unanswerable picker) and *form* (re-asking as a scenario —
it already **was** a scenario, with three rendered card mock-ups, and it was still unreadable,
which is evidence the existing tell table does not cover). **Rejected as a fix:** adding a fourth
option, since a fourth rendering would have shared the same premise.

**The tell is worth as much as the rule.** *"I couldn't understand the options"* on a question that
was already concrete, already scenario-shaped and already one axis wide — the three remedies the
existing table offers were all spent, and the question was still wrong.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — the fork-check bullet, and one row
in the tell table.

**Anchors.** [#21 resolution §0](https://github.com/idomarhaim/Android_Final_Project/issues/21#issuecomment-5273302788) ·
[#18 · `C3`](https://github.com/idomarhaim/Android_Final_Project/issues/18) ·
`CHANGELOG/2026-08-13/c5-endless-goals.md`

**Supersedes.** Nothing. It **extends** the fork-check clause rather than replacing it — the
existing check is correct and fired correctly; it just terminates one hop too early.

**Status.** ⛔ **Always-ask in both modes — still parked, and now parked *by name* in the file it
targets.** Destination is `rules/`, so the 🎬 walkthrough rule owns it and `AUTO MODE` does not
drain it.

**It has already done work while parked, which is worth recording.** `picker-queue-merge` shipped
the other six amendments as one reading (`daac210`, JARVIS) and then, **two minutes later**, read
this entry off the board note and **corrected one of them with it** (`3d0971a`): its
closed-blocker clause had been scoped *"when the options are **actions** rather than quantities"*,
on the reasoning that actions have no derivation closure so no grep can fire. This entry is the
counter-case — the options **were** quantities, the closure grep **did** run and widen a hop, and
it still framed them as rivals; what resolved it was `C3` (#18), a **closed** ticket. The clause is
now *"whenever the closure grep terminates without collapsing the fork"*.

**That is not this entry, and it was correctly not folded in.** What landed is a **widening of
their clause**, whose walkthrough Ido waived on 2026-08-10 so the mechanical half covered it. This
entry's own claim — *if two options measure different quantities the answer is "both", and the
question was a decomposition failure rather than a choice* — is a **separate rule with its own
tell-table row and has never been offered a walkthrough.** It is the **seventh** parked amendment
to `rules/question-axis-naming.md`: flagged in place there, not folded, not shipped. Ido's to
release.

---

## Drained

- **2 — a value that changes with wall-clock time silently rewrites every aggregate over it** →
  [`kb/dev/derive-dont-stamp.md`](file:///C:/Dev/JARVIS/kb/dev/derive-dont-stamp.md) **§7**, 2026-08-13.
  **Status: ingested.** No new page — the grep found that page already owning derived-vs-stored
  from the same repo and the same map (`C9a`); what was new was the **consumer**-side argument
  (aggregates, thresholds and predicates all move when the value moves, and no user action explains
  it), so it lands as a section under the claim it extends. Nothing superseded.
