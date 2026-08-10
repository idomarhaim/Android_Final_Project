# KB candidates — `entity-model-intake`, 2026-08-09

Repo: `c:\Dev\Android_Final_Project` · Session row: `entity-model-intake`

> **Partially drained 2026-08-10** by session `kb-ingest-backlog-drain`, pass 4 of 5.
> **Entries 2 and 3 are ingested** and removed from this file — they landed in
> `C:\Dev\JARVIS\kb` as `dev/review-intake-and-triage.md` §1.1 and
> `dev/decision-map-charting.md` §4. Both were **updates in place**, exactly as the
> entries proposed. See `kb/log/2026-08-10.md` for the full record; that journal
> entry is the only tie between this file and those pages, because they live in
> different repos.
>
> **Entry 1 survives, with its original number**, because it is always-ask in both
> modes. The file is rewritten rather than deleted: deleting on a partial drain
> discards exactly what the always-ask exclusions exist to preserve.

---

## Standing — always-ask

Not eligible for an automatic drain in either mode. Do not re-reason about the
disposition; the question is only whether Ido wants the change.

## 1 · "It doesn't understand me" usually means every option shares a framing the user doesn't hold

**Claim.** When a user rejects a question picker as *not understanding them*, the
common cause is not that the recommended option is wrong — it is that all N
options are variants of **one axis**, and the user's actual discriminator is a
different axis that no option names. The picker's own shape hides this: options
are presented as exhaustive, and "Other" is a free-text escape hatch that costs
the user the work of articulating the missing axis themselves.

**Why.** Observed here concretely and at full resolution. A `/wayfinder` ticket
asked *"what makes something a GOAL rather than a TASK?"* and offered four
options: **measured vs done**, **size/effort**, **endures vs completes**, and
**no fixed rule — the AI decides**. Every one of them is a property of the
*object* — how it is tracked, how big it is, whether it terminates. The user
stopped the session, went away, and wrote a document whose answer is a property
of **the user's relationship to the object**: a goal matters to you *in its own
right*, a task is a *means to something else* (intrinsic vs instrumental). That
axis is not a refinement of any of the four; it is orthogonal to all of them, and
each of the four is at best a mechanical proxy for it.

Rejected framings: (a) *"the recommended option was wrong, pick a different
one"* — no option was on the right axis, so ranking them differently changes
nothing; (b) *"the user should have clicked Other"* — Other asks the user to
supply the analysis the picker exists to elicit, and this user's response was
instead to spend a sitting writing a 26-line document, which is the honest signal
that the question was mis-shaped; (c) *"more options would have caught it"* — a
fifth option on the same axis would not have.

**What to do about it.** Before offering options, name the **axis** each option
varies along; if every option shares one axis, say so in the question text and
say what other axes were considered and dropped. A picker whose options span one
axis should say *"assuming we discriminate by X, which cut?"* rather than
presenting itself as the whole question.

**Destination.** `kb/dev/` — a page on question-picker design, or a section in
whatever page covers `/grilling` and `AskUserQuestion` practice.

**Anchors.** `Product and UX Reviews/2026-08-09-entity-model-brief.md` `E7`,
`E12`; GitHub issue
[#13](https://github.com/idomarhaim/Android_Final_Project/issues/13);
`CHANGELOG/2026-08-09/entity-model-intake.md` → *The headline*.

**Supersedes.** Nothing known.

**Status.** ⏸️ **Always-ask, awaiting Ido — 2026-08-10.** Held back by
`kb-ingest-backlog-drain` for the reason the entry itself names: this is arguably a
change to **how the agent asks questions**, which makes it a `rules/` change rather
than a KB page, and `rules/` is always-ask in both modes and owned by the 🎬
walkthrough rule. It also bears directly on the ❓ Ambiguity rule's
`AskUserQuestion` clause, which is live text Ido reads every session. His call at
the drain was **park it and be given the `rules/` proposal**; that draft is written
to its canonical JARVIS home, uncommitted and unsynced, pending `/walkthrough`.
This entry closes when he accepts or rejects that draft; if he rejects the `rules/`
framing, it drains here as a section of `kb/dev/grilling-question-framing.md`, which
is the existing page on question shape.
