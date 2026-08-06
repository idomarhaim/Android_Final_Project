# KB candidates — `product-review`, 2026-08-06

Un-ingested. Normal mode, so this list is a **proposal**: nothing is ingested
until Ido approves it. Each entry stands alone — no transcript is a source.

---

## 1. A free-form review document is split by ceremony tier *before* it is planned, not after

**Claim** — When a stakeholder hands over a freehand list of product problems, the
first act is transcription into a citable, numbered artifact; the second is
classifying each item by the ceremony it earns (defect · single-session build ·
undecided model question). Only the third tier goes to `/wayfinder`. Handing the
whole document to a map is the default move and it is wrong twice over: the
already-answerable items become decision tickets nobody needs to decide (the
over-ceremony signal `scale-adaptive-ceremony.md` already names), and a map cannot
cite a source that is free prose in a binary.

**Why** — Rejected: charting the map directly from the `.docx`, which is what was
proposed and what looked efficient. Rejected: skipping transcription and citing
the `.docx` by paragraph, which no future session can resolve. The transcription
is not busywork — the stable ids (`R1`–`R28`) are what let a TODO entry, a GitHub
issue and a wayfinder ticket all point at the same sentence without restating it.

**Destination** — central KB, `kb/dev/` — it generalises past GoalPilot and past
Android; it is a planning-intake method. Adjacent to, and citing,
`rules/scale-adaptive-ceremony.md`.

**Anchors** — `Product and UX Reviews/2026-08-06-brief-review.md`,
`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`,
`TODO/TODO_FUTURE/ProductModel.TODO.future.md`,
`CHANGELOG/2026-08-06/product-review.md`.

**Supersedes** — nothing. Extends `scale-adaptive-ceremony.md` rather than
contradicting it: that rule says which tier to spend, this says how to find out
which tier a mixed document is in.

**Status** — pending Ido's approval.

---

## 2. A reported bug can be an unspecified model, and filing it as a bug hardens the wrong answer

**Claim** — When a user reports two numbers that "don't match", check whether they
were ever specified to match. If nothing in the model joins them, it is not a
defect — it is a decision that was never taken, and fixing the symptom picks one
of the candidate models by accident and makes it permanent. Route it to the
decision backlog, not the issue tracker.

**Why** — The concrete case: `Task.points` (an LLM-set gamification currency,
default `10`) and a goal's `currentValue / targetValue` are independent by
construction, joined only by `Task.progressContribution`, which defaults to `1.0`
and is surfaced nowhere. So every completed task advances its goal by exactly one
unit regardless of what it was worth — which is precisely the symptom reported.
Rejected: opening it as a defect alongside the other four, which is what the
`תקלה` label in the source asked for. The user's label is evidence about the
experience, not about the cause.

**Destination** — central KB, `kb/dev/`. Same page as candidate 1 or adjacent to
it; the two were found together and the second is the sharpest evidence for the
first.

**Anchors** — `app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt`,
`.../domain/model/Goal.kt`,
`TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md` (`D6`),
`TODO/TODO_FUTURE/ProductModel.TODO.future.md` (`C3`).

**Supersedes** — nothing.

**Status** — pending Ido's approval.
