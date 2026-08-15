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

---

# Second visit — entry 1 shipped, and this file is now fully drained and deleted

**Ido's approval was the gate, and nothing else could have been.** Entry 1 rewrites a standing KB
claim in place, which `rules/memory-promotion.md` treats as a deletion and makes always-ask **in both
modes** — `AUTO MODE` did not cover it. He said *"do the corrections"*; that released it.

📥 **Ingested:** `kb/dev/derive-dont-stamp.md` **§1 rewritten in place + new §1.1** —
*a stored writer needs a **reader**, not an event.* JARVIS `3726297` (claim) → `392b565`.

§1's write-derived row had read *"a server-side trigger legitimately can, and **often should**"* own
it (2026-08-10). Two questions were collapsed into one: an event to hang a trigger on establishes the
value **can** be owned, and the wording implied it **should** be. §1.1 now carries the second
question's test — *a derived number gets a stored writer if and only if somebody who cannot read its
inputs has to read it* — which is **checkable**, since the authorization boundary is already written
down. `kb/index.md`'s row was corrected too; it repeated the superseded wording verbatim.

🗑 **`kb-candidates/2026-08-14-c20-derived-state.md` deleted — the carve-out, not an unasked deletion.**
Every entry is promoted, so `rules/derivable-decision.md` §1 deletes it without asking. Cross-repo
means it cannot ride the promotion's commit, so it **names** it: `a6e0a79` (entries 2–3) and `392b565`
(entry 1), with `C:\Dev\JARVIS\kb\log\2026-08-15.md` holding the tie that survives.

## ⚠️ This session's JARVIS release note was swept into a sibling's commit

`8b2e166` — `c11b-output-formats` claiming a new visit — took the working-tree `SESSIONS.md` while
this session's release note was in it, before this session could commit it. **Nothing was lost and
nothing was rewritten; the cost is provenance**, and it is the **sixth** instance of that pattern.
Both sessions staged by explicit path, and explicit-path staging is a one-sided guard: it stops *you*
sweeping a sibling in and does nothing about a sibling sweeping *you*. Recorded rather than un-picked,
because un-picking it needs a force-push, which is always-ask in both modes.

Also corrected there: this session's **first** JARVIS row claimed *"no singleton"* while holding
`kb/index.md` and `kb/log/2026-08-15.md`, which that board's contended-resource table names
explicitly. The bundle was free that hour; that is a fact about the day, not a property of the work.
The second visit declared the `kb/` singleton properly — and then hit `index.lock` held by a
concurrent sibling, which is the contention the declaration exists for.

## 🧪 Tests

`Check-KbLinks.ps1` — **CLEAN**, 68 pages, run after the §1 rewrite and before the commit. Commit-hook
parity **OK**. Markdown only in both repos; no source file touched, so no other layer applies.

## Push — this repo yes, JARVIS no, and the two were decided separately

**This repo: pushed** (`25b7bfd..0e5bf74`). All six preconditions checked, and the blocker recorded
yesterday had **expired on its own** — `478769d` (`c11b-output-formats`) and this session's first three
commits were already on the remote, so `@{u}..HEAD` held **two commits, both this session's, no
foreign commit to adjudicate**. Board Active claims empty, tree clean, remote not ahead, plain
`git push` (no `--all`, no refspec). The range's **only deletion** is
`kb-candidates/2026-08-14-c20-derived-state.md`, drained in the same commit — precondition 2's
`kb-candidates` carve-out exactly, not a deletion that needs asking. No renames, no binaries, no
secrets, no source file.

⛔ **`C:\Dev\JARVIS`: not pushed, and this is the "if it harms, don't" case.** `@{u}..HEAD` holds
**eleven** commits, **nine of them foreign**, and two of those belong to sessions that are **live right
now**:

| Foreign commit | Session | Evidence it is live |
|---|---|---|
| `1c654e3` | `sibling-wait-banner` | **live row in Active claims**, and `rules/agent-topology-and-model-routing.md` is **dirty in the tree** — mid-write |
| `8b2e166` | `c11b-output-formats` | claimed minutes ago; their `CHANGELOG/2026-08-15/…` and `user-rules/my-rules.instructions.md` are **staged, uncommitted** — mid-unit |

Precondition 5 is explicit: a foreign commit whose paths sit under a live Active-claims row → **stop
and ask**. Pushing would publish two sessions' in-flight work on this session's schedule, and
un-publishing needs a force-push, which is always-ask in both modes. The four other foreign commits in
the range were never read by this session either, which precondition 2 stops on independently.

**This session's own JARVIS work (`392b565`, `3726297`) is therefore committed and unpublished**, and
it cannot be pushed alone — `git push` is branch-scoped, not commit-scoped. It will go up with
whoever pushes next once those two sessions release.

**Closed out, same day:** `sibling-wait-banner` pushed `C:\Dev\JARVIS` at `5b11b54`, and this session's
two commits (`3726297`, `392b565`) went up inside that push. That is the prediction above coming true
rather than a change of decision — `git push` is branch-scoped, so **withholding a push withholds only
your own act, never your commits**. The gate this session honoured was the right one and it protected
the right thing: it did not publish two live sessions' work *on this session's schedule*. One of those
sessions then chose to publish its own, and carried this session's along, which is the arrangement
precondition 5 actually describes.
