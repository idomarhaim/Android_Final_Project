# KB candidates — `c15b-stored-ai-text`, 2026-08-13

Session: `c15b-stored-ai-text` · repo `C:\Dev\Android_Final_Project` · branch
`feat/goalpilot-implementation` · map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12),
ticket [#35 · `C15b`](https://github.com/idomarhaim/Android_Final_Project/issues/35) (resolved).

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

---

## 1 · A read through an aggregate endpoint is a hypothesis, exactly like a write

**Claim.** Verification duty is usually stated over *writes* — you do not know a write landed until
you read it back. The same duty applies to **reads served by an aggregate or index endpoint**: such
an endpoint answers from its own materialised copy of the underlying records, and that copy can lag
the records themselves. A field read from the aggregate is a **hypothesis about the record**, not
the record; when a decision turns on it, confirm it against the primary object.

**Observed twice in one session, 2026-08-13, and the second instance is the sharper one.**

1. **A stale field.** Deriving the wayfinder frontier calls
   `gh api repos/<owner>/<repo>/issues/12/sub_issues`, which returns each child with `number`,
   `state`, `assignees` and `title` — everything the frontier query needs, in one call. It returned
   **26 children, 4 open**, listing `#21 · C5` as `open`. A direct `gh issue view 21` seconds later
   returned **CLOSED**, resolution comment timestamped `2026-08-12T22:03:10Z`, about sixty seconds
   earlier. Trusting it would have produced a frontier containing an already-resolved ticket.
2. **A read that ran somewhere else entirely.** Two `Bash` calls issued in one message **shared a
   working directory**. The second had been written to scan this repo's push range for secrets; it
   executed inside `C:\Dev\JARVIS`, where `@{u}..HEAD` was **empty**, and reported **clean**. A
   clean secret scan over an empty diff is indistinguishable, in its output, from a clean secret
   scan over the intended one. The fix that makes it self-verifying is to have the command **print
   the evidence of its own scope** — `git rev-parse --show-toplevel` and `--abbrev-ref @{u}` first,
   in the same output — so the reader can see *what was read*, not merely *what was concluded*.

**Why this is worth a page rather than a footnote.** Both failures are silent and shaped to be
believed: the response is well-formed, complete, internally consistent, and every *other* field in
it is correct. Nothing in either output says the read was wrong. The second instance generalises the
first — the danger is not staleness specifically but that **a read reports its result and never its
scope**, so the one fact that would falsify it is the one fact it omits.

**What was rejected.** *"Just always re-query everything"* — rejected as unaffordable and as the
wrong generalisation: the aggregate exists because N+1 direct reads are expensive, and 22 of the 26
children here were closed and irrelevant. The rule that survives is narrower: **confirm only the
records a decision turns on**, and **make every read print its own scope**. The asymmetry is what
makes it cheap — a stale `closed` merely hides a ticket and the next derivation finds it, while a
stale `open` costs a wasted claim.

**Relationship to the `c2-task-type` candidate, and why they should be drained together.**
`kb-candidates/2026-08-13-c2-task-type.md` entry 1 records *a write is a hypothesis until you read
it back*, from a `gh api --method PATCH` that silently did nothing. This is **the same claim from
the opposite direction**. Together they say the boundary is not read-vs-write but
**remote-state-vs-your-belief-about-it**, and that the confirming call must be a *different* call
from the one that formed the belief. Drained separately they would produce two half-arguments that
each read as a war story.

**Destination.** `C:\Dev\JARVIS\kb\dev\runtime-verification.md` — the same new section
`c2-task-type` entry 1 proposes, extended, **not** a second page.

**Anchors.** `SESSIONS.md` → the `c15b-stored-ai-text` claim note and release note, 2026-08-13.
`CHANGELOG/2026-08-13/c15b-stored-ai-text.md`. Prior art:
`kb-candidates/2026-08-13-c2-task-type.md` entry 1.

**Supersedes.** Nothing. It **extends** an un-ingested candidate; it contradicts no standing KB
claim.

**Status.** 🟢 Ordinary `kb/dev/` material, **`AUTO MODE`-eligible and genuinely this session's** —
held for one reason only: the destination is a **cross-repo write into `C:\Dev\JARVIS`**, whose own
board must be read and claimed first, and which needs `kb/index.md` and `kb/log/` alongside. That is
its own small unit. **Not drained here.**

---

## 2 · A hand-back repeated on the same subject means the *premise* is false, not the *form*

**Claim.** `rules/question-axis-naming.md`'s tell table routes *"I could not understand the
options"* through **ownership → premise → form → density**, and it routes to **form** — *re-ask the
mechanism as a situation I can picture* — only when the user answered part of a batch and refused
the rest. It says nothing about what a **second, identical hand-back on the same subject** means.
It should: **a hand-back repeated in the same words, after the form has already been changed, is
evidence that the fork itself is false.** The instrument that failed is not the phrasing; it is the
belief that a decision was owed at all.

**Observed, 2026-08-13, ticket #35.** Two pickers were put to Ido on one ticket. The first varied
along *how much groundwork before you are in the room*; he handed it back verbatim. The second was
deliberately built to the tell table's **form** remedy — the same decision re-asked as a **concrete
situation** (*you wrote a plan in Hebrew, switched to English, and opened it*), with a per-option
**ASCII preview of the actual screen**, which is as far toward picturable as the harness allows. He
handed it back **in exactly the same words**. Only then was the fork check run over the derivation
closure, and the fork collapsed: a stale draft requires an unfinished draft **and** a language
switch inside that window **and** a return to it — for a **per-device setting an audience of one
sets once** — and the app's `C9a` doctrine already governs unendorsed proposals (`EXPIRED` counts
for nothing). **All four options presupposed machinery worth building for a state that barely
occurs.** The right answer was outside the set, exactly as the hand-back rule predicts, and it cost
**no field, no dialog, no mechanism and zero model calls**.

**Why the existing rule did not catch it.** The tell table is indexed by *tell*, and both attempts
produced the **same** tell, so it kept routing to the same row. Nothing in it reads the **second
derivative** — that the tell repeated *after* a remedy was applied. That is the missing clause:
**a remedy applied without changing the tell falsifies the diagnosis, not the wording.** Stated
generally, it is the rule against retrying a failing command in a loop, applied to questions.

**What was rejected.** *"Two hand-backs mean stop asking and just decide"* — rejected because that
is already the hand-back rule's duty 3, and it explains nothing about **why** the answer was
outside the option set. The value here is diagnostic, not procedural: the repeat **tells you which
check you skipped**, and the check was premise.

**Destination.** `C:\Dev\JARVIS\rules\question-axis-naming.md` — a clause on the **tell table**,
adjacent to the *"I could not understand the options"* row.

**Anchors.** [#35](https://github.com/idomarhaim/Android_Final_Project/issues/35) — the two
hand-backs and the resolution §4 that withdraws this session's own earlier `languageTag`
recommendation on exactly this ground. `CHANGELOG/2026-08-13/c15b-stored-ai-text.md`.

**Supersedes.** Nothing outright — it **adds** to the tell table rather than rewriting a row. But it
is **adjacent to two other parked candidates on the same file**: `c2-task-type` entry 2 (*the fork
check must run against the code, not the ticket's statement of the fork*) and `c9e` entry 2 (*the
widening*). All three are about the same failure — the check that would have caught it was run at
the wrong width or not at all — and they should be **merged into one reading**, not shipped as three
clauses.

**Status.** ⛔ **Always-ask — not drained, and not proposed as a diff.** Destination `rules/`, which
`/kb-ingest` may not write and the 🎬 walkthrough gate owns. `AUTO MODE` does not cover it in either
direction. It is also the **eighth** parked amendment to `question-axis-naming.md`; the seventh was
filed hours ago by `c5-endless-goals` and is already owed a walkthrough offer that has never been
made. **It belongs in that one reading, not raced beside it.**
