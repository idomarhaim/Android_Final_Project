---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 65
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/MeasureProposal.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/ProposeMeasureUseCase.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/UnmeasuredMarker.kt
  - functions/src/**
  - app/src/test/java/com/idomarhaim/goalpilot/domain/MeasureProposalTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/MeasureProposalUiTest.kt
  - kb-candidates/2026-08-23-65-measure-proposal.md
  - CHANGELOG/2026-08-23/65-measure-proposal.md
  - sessions/65-measure-proposal.md
created: 2026-08-23
---

# `#65` — the measure proposal, and the tone it has to be offered in

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`docs/PRODUCT_v0.3.md` §1.3, §2.1, §3.3 E, §3.4 and §10.1](../docs/PRODUCT_v0.3.md) · the
prototype at
[`docs/prototypes/2026-08-15-measure-proposal/`](../docs/prototypes/2026-08-15-measure-proposal/)
(revision 2, what `#44` was resolved against) ·
[`#65`](https://github.com/idomarhaim/Android_Final_Project/issues/65).

**Task:** build the agent's offer of a concrete measure for a goal that has none.

## Two questions for Ido, and they come first

The spec marks both `Untested:` and both are his:

1. **Does he agree with the two-surface answer at all?** It was **decided by the agent on his
   hand-back**, and §10.1 says outright it is his to overturn.
2. **Would he rather fold this into `estimate`** than give it a fifth AI call?

**Ask before building.** They change what gets built, not how it looks.

## Why nothing exists, and why that history shapes the ticket

`C7` specced this and handed it on in so many words — *"a **fifth AI feature** for `C11b` to write
an output format for."* **`C11b` wrote four schemas — `estimate`, `plan`, `daily`, `classify` —
and none of them is it.** `C11b` reached "five" by a different route, counting `classify`.
**Two tickets each said "fifth" about a different feature, and `C7`'s never got written.**

`Observed:` 2026-08-23 — `grep -rn "MeasureProposal\|measureProposal"` over `app/src/main`
returns **zero hits**.

⚠️ **This is a `prototype` ticket, not a format one, and the distinction is load-bearing.** §10.1's
first recommendation was *"it is a format question, which is `C11b`'s subject"* — and **that
reading is exactly what lost it**. The schema is a *part* of this, not the whole.

## What was decided — do not reopen it

**Two surfaces, not one:**

1. **A marker** — a **dashed square**, no words, no buttons — wherever the goal is listed.
2. **The offer itself, only on the goal's own screen**, because **opening the goal is the consent**
   §0.7 requires for intrinsic structure.

- **Never in the daily surface.** `C10` allocated those slots, and it is the one screen that
  arrives unasked.
- **Never auto-applied.** **Dismissible per goal, and dismissal is permanent — not snoozed**,
  because a default that re-asks is not a default.
- **The absence is stated as legal *before* anything is offered**, or the offer reads as a
  correction of a deliberate choice.
- **Dashed and hollow throughout**, so it can never borrow the visual language of an outcome.
- **A non-AI fallback is mandatory** — the **mechanical proposal**, §3.3 E — because §0.1 says an
  AI feature that cannot run reliably on the free tier ships with a non-AI fallback beside it or
  is not specced at all.
- **Changing a kind is never silent.** Either **reset**, or **a proposed adaptation of logged
  history shown before it applies** — **arithmetic first, the model only where arithmetic cannot
  answer**: percent → litres against a known target is division; *"12 books"* → pages is not.
  On a shared challenge, **every participant must approve** (§6 has the mechanism).

## Tone is the whole ticket

The spec's own words: *an offer appearing on a goal Ido **deliberately left unmeasured** is the
most easily-resented surface on this map.* §0.4 makes it **legal**; **tone is what makes it
wanted**, and no ticket designed that — the prototype is where it was drawn. Read it and match it.
If the wording feels like a nag in the render pass, it is wrong, and that judgement is part of the
work rather than a polish pass afterwards.

## Exit

- **JVM tests** for the mechanical fallback's arithmetic and for the dismissal being permanent
  (a dismissed goal never offers again, across process death).
- **Instrumented test + render pass** for both surfaces — the marker in a list, the offer on the
  goal screen. §0.8 is suspended: **English only.**
- ⚠️ `adb install -r` + `am instrument`, **never `connectedDebugAndroidTest`**.
- If the AI call ships, it is **one wide call** (§3.2) with a stated failure contract (§3.4), and
  the free-tier budget in §0.1 is a hard ceiling.
- ⚠️ **Deploying the Cloud Function is standing-authorised** — `deploy --only functions` costs
  nothing and needs no permission ([`docs/OPERATIONS.md`](../docs/OPERATIONS.md) § *Standing
  authorisation*). **Deploy; do not wait.** Five sessions have stopped at that gate wrongly.
- `CHANGELOG/2026-08-23/65-measure-proposal.md`.

## Out of scope

The measure model itself (`measureKind` / `measureWord` shipped with `#55`), and the fill buttons
(`#11`). This ticket proposes a measure; it does not redesign what a measure is.
