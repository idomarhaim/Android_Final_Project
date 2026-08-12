# KB candidates — `c15b-stored-ai-text`, 2026-08-13

Session: `c15b-stored-ai-text` · repo `C:\Dev\Android_Final_Project` · branch
`feat/goalpilot-implementation` · map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12),
ticket [#35 · `C15b`](https://github.com/idomarhaim/Android_Final_Project/issues/35) (resolved).

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

---

## 1 · ~~A read through an aggregate endpoint is a hypothesis, exactly like a write~~ — **DRAINED 2026-08-13**

**Ingested** into `C:\Dev\JARVIS\kb\dev\runtime-verification.md` as **new §6**, *"Your own
effect on the system is a hypothesis too — and so is a read that came through an aggregate"*,
together with entry 1 of `kb-candidates/2026-08-13-c2-task-type.md` — the same claim from the
opposite end. Index row rewritten; journalled in `kb/log/2026-08-13.md` with both source commits.

**Not drained separately, deliberately.** Each file proposed a section on the same page. A *write*
is a hypothesis until you read it back; an *aggregate read* is one too. Apart they read as two war
stories; together they state the boundary as **remote-state vs your belief about it**. A third case
from `c15b` was folded in and generalises both — a scan that ran in the wrong repo against an empty
range reported *clean*, because **a call reports its result and never its scope**.

**Drained by** session `c15b-stored-ai-text`, visiting `C:\Dev\JARVIS` with a row on that board,
on Ido's explicit *"do what you think is right, but verify it harms nothing"*. This entry was the committing session's own and `AUTO MODE`-eligible.

**The full text of this entry is not reproduced here** — it is the committed §6, which is the
canonical form. This file is kept, not deleted, because entry 2 below is still parked.

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
