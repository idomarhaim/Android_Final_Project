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
