# `kb-ingest-c10` — this repo's half of a cross-repo ingest

**Session:** `kb-ingest-c10` · **Date:** 2026-08-10 · **Mode:** normal
**Invocation:** bare `/kb-ingest` · **Ingest only, Markdown only. No code.**

## What changed *here*

Exactly two things, because the knowledge landed somewhere else:

- `kb-candidates/2026-08-10-c10-quote-feed.md` **rewritten down to its survivors** —
  4 of 6 entries drained, 2 held back.
- `SESSIONS.md` — row claimed before the first write, released at the end.
- (this changelog and its index row)

**The pages are in another repo.** They went into the central bundle
`C:\Dev\JARVIS\kb`, so no commit can hold both halves — the tie is the journal entry
at `C:\Dev\JARVIS\kb\log\2026-08-10.md`, which names this file *and this repo*. See
`C:\Dev\JARVIS\CHANGELOG\2026-08-10\kb-ingest-c10.md` for the full record.

## What was drained, in one line each

| Entry | Landed as |
|---|---|
| 1 | `kb/dev/split-at-the-inviolable-constraint.md` — **new**. Put the seam at the obligation that is unforgivable, not at the natural functional boundary |
| 2 | `kb/dev/llm-structured-output.md` **§2.1** — **fold**. Where a closed vocabulary can stand in for an opaque token, don't send the token: the corruption becomes unreachable rather than checked |
| 3 | `kb/dev/degraded-mode-decides.md` — **new**. The fallback shape is the tiebreaker between two close designs |
| 5 | `kb/dev/localization-axes.md` **§5** — **fold**. A translation carries its own copyright; a public-domain original yields nothing |
| 6 | `kb/dev/confirmation-vs-correctness.md` — **new**. A confirmation gate tests comprehension, not correctness |

## What was held back

- **Entry 4** — corroborates a **parked** entry in this repo's
  `kb-candidates/2026-08-09-entity-model-intake.md`, which is itself awaiting Ido.
  Draining the corroboration alone would split one finding across two states.
- **Entry 6b** — entry 6's page landed; installing its three-question review as a
  standing pre-commit step is a **behaviour change**, so it is `rules/`-shaped and
  always-ask in both modes.

Both are rewritten into a `## Standing — always-ask` section with their original
numbers and dated reasons, exactly as a partial drain requires. **Nothing was deleted.**

## The two other candidate files — named, not touched

`kb-candidates/` was listed first, as the session-start duty requires. Besides this
session's own file it holds:

- `2026-08-09-c9f-consent-screen-state.md` — partially drained, **1 parked entry**
- `2026-08-09-entity-model-intake.md` — partially drained, **1 parked entry**

Both are down to a single always-ask entry awaiting Ido, so there is **no backlog of
un-offered knowledge** in this repo — only two decisions he has not yet made. Reported
rather than absorbed.

## 🧪 Tests

**No suite run and none applicable** — no Kotlin, Gradle, `firestore.rules` or Cloud
Functions file was created or modified. The only mechanical check that applies to this
work is the bundle linter, which runs in the other repo: `Check-KbLinks` **CLEAN at 47
pages** after every page and index edit.

## Singletons and concurrency

**None taken** — no `#gradle-daemon`, neither AVD, live `goalpilot-56e30` never
contacted, no GROQ call.

Three sibling sessions were live on this board throughout —
`c9a-schedule-a-task` (#25), `c13-byo-api-key` (#32) and `c16-milestone-model` (#37) —
and none owns a candidate file or the central bundle, so the working sets are disjoint.
`SESSIONS.md` and `#git-index` were **leased** before the first write and held through
the commit; a row was claimed on the JARVIS board too, because a cross-repo ingest owes
one in every repo it writes to. Staged by explicit path in both.

---

## Addendum — the file is now fully drained

Ido approved both held-back entries the same day and **waived the 🎬 walkthrough**, so
entries **4** and **6b** shipped as rule changes rather than KB pages:

- `C:\Dev\JARVIS\rules\question-axis-naming.md` — **amended** with the option-density
  clause (the picker carries the choice, the reply above it carries the reasoning; when
  the user says they don't understand, make the question **smaller**, don't explain
  more).
- `C:\Dev\JARVIS\rules\pre-commit-self-review.md` — **new** (three questions about the
  artifact immediately before `Commit this?`).

Both were applied to `user-rules/my-rules.instructions.md` and projected, so they are
live for both agents in every session on this machine. JARVIS-side commit `4b6940a`;
see `C:\Dev\JARVIS\CHANGELOG\2026-08-10\rules-ship-c10.md`.

**`kb-candidates/2026-08-10-c10-quote-feed.md` is therefore `git rm`'d** — 6 of 6
entries disposed of, nothing left parked. Per §7.5 a fully-drained file is deleted
rather than rewritten.

**Where the record of it now lives, precisely** — worth stating, because the obvious
answer is half wrong. The KB journal entry `kb/log/2026-08-10.md` covers entries
**1, 2, 3 and 5** and describes 4 and 6b as *held back*, which was true when it was
written and is no longer. The disposition of **4 and 6b** is recorded in
`C:\Dev\JARVIS\CHANGELOG\2026-08-10\rules-ship-c10.md` and in the two rule files
themselves, each of which names this candidate file and entry number in its provenance
line. The journal is append-only, so it is not rewritten; this paragraph is the bridge.

**One correction it forced, worth carrying:** entry 4 described itself as corroborating
a *parked* candidate in `kb-candidates/2026-08-09-entity-model-intake.md`. That
candidate had **already shipped** hours earlier as `question-axis-naming.md` — so the
entry was not a second voice for an existing rule, it was evidence that **the existing
rule was followed and was still not enough.** That other file's *"awaiting Ido"* status
line is stale and was **left untouched**: it belongs to a released session and
correcting another session's record is Ido's call, not this one's. → **Superseded by
Addendum 2 below:** it was reported to him, he chose to close it, and the file is gone.

---

## Addendum 2 — the stale note in the *other* candidate file, closed on Ido's call

`kb-candidates/2026-08-09-entity-model-intake.md` held exactly one surviving entry, and
its status read *"⏸️ Always-ask, awaiting Ido — the draft is written to its canonical
JARVIS home, **uncommitted and unsynced, pending `/walkthrough`**. This entry closes
when he accepts or rejects that draft."*

**Every claim it made about the *state* of that draft was false by then** — the draft
existed, which is the one true word in the sentence, and *uncommitted*, *unsynced*,
*pending `/walkthrough`* and *awaiting Ido* were all wrong. That draft is
`C:\Dev\JARVIS\rules\question-axis-naming.md`: committed, projected, walkthrough
**waived**, and **in force since 2026-08-10** — hours before this session read the file.
So the note was advertising a decision as pending that Ido had already made, in a folder
**every session reads before its first unit of work**. The concrete cost is not
untidiness: the next session asks him about a live rule, or drafts it a second time.

With that entry closed the file has nothing left to drain, so it is **`git rm`'d** under
the same §7.5 clause that deleted this session's own file.

**Where its record survives** — the entry is not lost by deleting the file that carried
it: `rules/question-axis-naming.md` names this candidate file **and entry number** in its
provenance line, `kb/log/2026-08-10.md` records the drain that parked it, and
`C:\Dev\JARVIS\CHANGELOG\2026-08-10\rules-ship-c10.md` records the shipping that closed
it. Three independent anchors, none of them the deleted file.

**One deliberate simplification, named rather than slipped in:** the approved option was
*correct the status, then delete the file*. Writing a corrected status into a file
removed by the same commit is a record nobody can ever read, so the correction is carried
here and in the commit message instead. Same outcome, one fewer piece of theatre.

**Why this session did it rather than the file's owner:** `entity-model-intake` released
long ago, and rewriting or deleting a released session's record is on Ido's always-ask
list — so it was reported, not taken, and done only once he chose it.
