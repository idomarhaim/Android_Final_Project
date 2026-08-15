# c20-derived-state — the drain ran, and the hold this session recorded was wrong

**Session:** `c20-derived-state` · **Date:** 2026-08-15 · **Mode:** `AUTO MODE`
**Branch:** `feat/goalpilot-implementation` · Continues [`CHANGELOG/2026-08-14/c20-derived-state.md`](../2026-08-14/c20-derived-state.md)

## The correction

Yesterday's entry recorded the KB drain as **held on a live sibling**, naming `c11b-output-formats`.
Ido asked why — *"you're not doing INGEST because you're waiting for another session to finish?"* —
and the hold does not survive the question. It was wrong on **both** halves:

1. **`c11b-output-formats` had released on the JARVIS board with a clean tree**; their `kb/` work was
   committed at `3f59fe9`. The recency signal used against them was a commit in **this** repo, and
   auto-push precondition 5's *"a recent commit means live"* governs **publishing someone else's
   commit** — not **writing into a repo whose board is clear**. One rule, applied to a question it
   does not answer.
2. **The JARVIS board was never actually read.** It was read as its **first 60 lines**, which show
   the header and the release notes; the Active-claims **rows** sit below that cut. So it was reported
   *empty* while `governance-backlog-sweep` held a live row throughout. A partial read of a board is
   not a read of the board, and it failed in the flattering direction twice at once — inventing a
   blocker that did not exist and missing a claim that did.

**The push hold is separate and still stands** — that one is precondition 5 inside its own domain.
Re-checked at the time of writing: `git log HEAD..@{u}` empty, this session's commits still unpushed.

## What the drain did

Claimed on the JARVIS board at `07ffcf1`, **disjoint path-by-path** from the live row (they own
`scripts/`, `user-rules/`, `TODO/`; this owned `kb/` only), released at `a6e0a79`.

- 📥 **`kb/dev/decision-map-charting.md` §10** — *an empty frontier is not a finished map.*
- 📥 **`kb/dev/derive-dont-stamp.md` §6 extended** — *idempotency is a property of the shape.*
- ⛔ **Entry 1 parked and re-gated to always-ask.**

Full account: `C:\Dev\JARVIS\CHANGELOG\2026-08-15\c20-derived-state.md`. Journal tie:
`C:\Dev\JARVIS\kb\log\2026-08-15.md`.

## ⚠️ Two of this session's own candidate entries were misfiled, and the drain is what caught it

The candidate file's bundle check named the page it **cleared** (`one-metric-and-its-mechanism.md`)
and never looked at `derive-dont-stamp.md` — where §6 already held entry 2's core claim **including
the same `TaskRepositoryImpl` observation**, committed 2026-08-10 by `c9a-schedule-a-task`.

Consequences: **entry 2 shrank from a proposed new page to a paragraph**, and **entry 1 changed gate**
— filed 🟢 with `Supersedes: nothing`, it in fact **narrows `derive-dont-stamp.md` §1** in place, whose
write-derived row reads *"a server-side trigger legitimately can, and often should"* own it. Rewriting
a standing claim is a deletion, so it is always-ask in both modes and `AUTO MODE` never covered it.

**A bundle check is only as good as the neighbourhood it searched, and a search run at the wrong width
does not fail — it passes.** Both corrections are written into the candidate file's own `Status` and
`Supersedes` fields rather than only here, since that file is what the next drain reads.

## Candidate file

[`kb-candidates/2026-08-14-c20-derived-state.md`](../../kb-candidates/2026-08-14-c20-derived-state.md)
**rewritten down to its survivor, not deleted** — a partial drain never is. Entry 1 keeps its original
number under `## Standing — always-ask`; entries 2 and 3 are listed beneath as drained records.

## 🧪 Tests

`Check-KbLinks.ps1` — **CLEAN**, 68 pages. Commit-hook parity **OK**; the JARVIS pre-commit hook
blocked once on a stale `CHANGELOG_README.md` generated region and was fixed by running
`New-ChangelogIndex.ps1 -Staged`, exactly as its own message prescribes. No other layer applies —
Markdown only, no source file touched in either repo.
