# KB candidates — `entity-model-intake`, 2026-08-09

Repo: `c:\Dev\Android_Final_Project` · Session row: `entity-model-intake`
Mode: **normal** — this list is a **proposal**. Nothing here is ingested; silence
is not approval. Drain via `/kb-ingest`.

---

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
whatever page covers `/grilling` and `AskUserQuestion` practice. **Note:** if the
drain concludes this belongs in `rules/` (it is arguably a change to how the
agent asks), that is **always-ask** in both modes.

**Anchors.** `Product and UX Reviews/2026-08-09-entity-model-brief.md` `E7`,
`E12`; GitHub issue
[#13](https://github.com/idomarhaim/Android_Final_Project/issues/13);
`CHANGELOG/2026-08-09/entity-model-intake.md` → *The headline*.

**Supersedes.** Nothing known.

**Status.** Pending — not ingested.

---

## 2 · A binary source document is invisible to future sessions; the transcription is the artifact, and the two live apart

**Claim.** A `.docx` / `.pdf` / image the user wrote is **not** a citable source
in an agent-run repo: it is binary (no diff, no grep), often not in the project's
working language, and unquotable in a ticket. The durable artifact is a
**transcription with stable per-item ids** — and once both exist, the **source
and the transcription belong in different folders**, because they have opposite
link profiles: the transcription accumulates inbound references and must never
move, while the source has one and can.

**Why.** Both halves were observed here. This repo already had the pattern once
(`R1`–`R28` from the 08-06 brief) and the reason was written down at the time:
*"the original is free-form Hebrew prose inside a `.docx` — unreadable by any
future agent session and unquotable in a ticket."* The **second** half is the new
part: when the user moved the `.docx` out to a new folder, the question of
whether the transcription should follow it was decided by counting references —
the transcription is linked from **seven** places including the body of a live
GitHub map issue, the `.docx` from **one**. Moving the linked half to sit beside
the unlinked half would have broken seven references, one of them inside an issue
three live sessions were reading.

Rejected: *"keep source and transcription together for provenance"* — provenance
is a one-line link in the transcription's header, which costs nothing and cannot
rot the other six references.

Also worth recording: the 08-06 transcription's source reference had gone stale
in **two ways at once** — the file had moved folders *and* been renamed — and
nothing detected it, because a prose filename reference is not a link any linter
follows. Naming the source as a **markdown link** rather than backticked prose is
what would have made it checkable.

**Destination.** `kb/dev/` — a page on ingesting user-authored source documents
into an agent-navigable repo.

**Anchors.** `AGENTS.md` → *Where things live*;
`Product and UX Reviews/2026-08-06-brief-review.md` header;
`Product and UX Reviews/2026-08-09-entity-model-brief.md` header.

**Supersedes.** Nothing known.

**Status.** Pending — not ingested.

---

## 3 · New user input arriving mid-map is routed, not re-charted — and routing is a table of id → ticket

**Claim.** When a user writes a **new** source document while a `/wayfinder` map
is already in flight with live sessions on its tickets, the correct response is
**not** to re-chart the map and **not** to answer the tickets the document
touches. It is to produce the transcription plus a **routing table** — one row
per ticket, listing which of the new ids bear on it and what *kind* of bearing
(answers it / constrains it / creates scope it does not cover). Answering is the
claimed sessions' job; re-charting is the map owner's.

**Why.** Three sessions were live on the board and between them owned the map
body and three of its tickets, so §5 rule 2 forbade writing to any of them. But
the document plainly *answered* part of one live ticket and *created scope on
none of them*, so doing nothing would have left three sessions working from a
superseded framing. The routing table is what the intake session can produce
without writing into anyone's paths: it is a reading list, and it makes the
"which of these is new scope" question explicit — here, five items belonged to no
ticket at all (a new entity, two new relationship cardinalities, an unanswered
question the user addressed to the agent, and a presentation requirement).

Rejected: (a) *"comment on the tickets, it's only a comment"* — a comment on a
claimed issue is a write to a claimed path, and the claim column names issues
explicitly in this repo; (b) *"file the new-scope items as issues"* — wiring
tickets into a map is a charting act and the map is claimed; (c) *"wait for the
sessions to finish"* — the live session was blocked on a question the document
answers, so waiting is the expensive option.

**Destination.** `kb/dev/decision-map-charting.md` — extends an existing page
(a constraint ticket "splits", a knot wants an "order"; this adds what happens
when the *source* changes under a live map).

**Anchors.** `SESSIONS.md` → the three live rows of 2026-08-09;
`Product and UX Reviews/2026-08-09-entity-model-brief.md` → *Where these items
land — routing only, not answers*;
`CHANGELOG/2026-08-09/entity-model-intake.md` → *What was deliberately not done*.

**Supersedes.** Nothing — it extends `kb/dev/decision-map-charting.md` rather
than replacing a claim on it. Confirm at drain time.

**Status.** Pending — not ingested.
