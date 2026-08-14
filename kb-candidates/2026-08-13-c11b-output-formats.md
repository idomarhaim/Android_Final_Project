# KB candidates — `c11b-output-formats` (2026-08-13)

Session: `c11b-output-formats` · Ticket: [#30 · `C11b`](https://github.com/idomarhaim/Android_Final_Project/issues/30) · Mode: `AUTO MODE`

Each entry stands alone. No transcript is a source: everything needed to write the page is below.

**Drained 2026-08-14 — partial.** Entry 2 shipped as
`C:\Dev\JARVIS\kb\dev\decision-map-charting.md` **§9** (a new section; the old §9 *Adjacent*
renumbered to §10). **No new page** — the bundle check was confirming and re-confirmed 32 hours
later. Journal: `kb/log/2026-08-14.md`. **Entry 1 survives below and keeps its original number.**

---

## Standing — always-ask

## 1 · Exposure to a sibling's commit opens when the content reaches the **working tree**, not when you `git add` ⛔

**Claim.** When two agent sessions share one working tree, the committed rule already says the
pathspec commit *"fixes one direction only… it protects **others from you**"*, and locates the
remaining exposure precisely: *"your exposure opens the moment you `git add`."* **That sentence is
too late by one step.** A sibling's `git add <path>` reads the **working tree**, so your lines can
ride their commit having never been staged by you at all. Exposure opens the moment the content
**exists in the file**.

**Why this matters, and what it rejects.** It kills a plausible fourth remedy and weakens a listed
one. The fourth remedy — bypass `git add` and write the index directly, so your commit contains only
your own hunk of a shared commons file — is sound in isolation and *verifies*:

```bash
git show HEAD:SESSIONS.md > head.md         # the committed content
#   …insert only this session's row…
SHA=$(git hash-object -w staged.md)
git update-index --cacheinfo 100644,$SHA,SESSIONS.md
```

`git diff --cached --stat` then reports exactly the one intended insertion. It still loses, because
it makes the *index* private and the index was never the leak. And the rule's first listed remedy,
**stage as late as possible**, shrinks nothing under the corrected model: lateness is measured from
`git add`, but the window opened when the file was written.

**What survives unchanged:** the other two remedies — lease the index, or a worktree per session —
are unaffected, and a worktree per session remains the only thing that actually partitions this. The
value here is closing off the cheap-looking fourth option so the next session does not spend twenty
minutes rediscovering it.

**Observed, 2026-08-13, GoalPilot.** This session built and verified the index blob
(`git diff --cached` → `SESSIONS.md | 1 +`). `c15b-stored-ai-text` then committed `406874d`
(*"c15b: resolve #35"*), which carries **this session's claim row** alongside their own 57-line
release note; this session's `86f3f87` shipped with only its changelog. **Fifth cross-contamination
of that night** across `c5-endless-goals`, `picker-queue-merge`, `c19-area-success-failure` and
`session-titles` — and the **first in which a deliberate countermeasure was tried and lost**, which
is the part none of the other four could record.

**Destination — corrected after reading the target, and the correction is most of this entry's
value.** It was filed believing it was new `kb/dev/` material. It is not: `picker-queue-merge`
committed the governing block into `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5 hours
earlier (`843a0b4`), and that block already carries the one-direction-only finding and its three
remedies. **What is new is one clause, and it is a *correction* to that text rather than an
addition** — the exposure-window sentence above.

**Anchors.** `git update-index --cacheinfo`, `git hash-object -w`, `SESSIONS.md`; commits `86f3f87`,
`406874d`, `8b3974a` in `c:\Dev\Android_Final_Project`; commit `843a0b4` and
`rules/agent-topology-and-model-routing.md` §5 in `C:\Dev\JARVIS`.

**Supersedes.** ⚠️ **It narrows a standing committed claim** — the *"exposure opens the moment you
`git add`"* sentence.

**Status.** ⛔ **Always-ask in both modes. Not drained, and not proposed as a diff.** Two gates, and
both are still standing as of **2026-08-14**:
1. Destination is `rules/`, which `/kb-ingest` may not write and the 🎬 walkthrough gate owns.
2. It **contradicts a standing claim**, which is always-ask regardless of mode.

**The third hold has lifted and is recorded so the next session does not re-derive it.** On
2026-08-13 this entry was also blocked because `rules/agent-topology-and-model-routing.md` was owned
by the live `liveness-from-transcript` claim. **That session has released; the JARVIS board's Active
claims table is empty as of 2026-08-14 19:50.** Only the two always-ask gates remain, and neither is
an agent's to clear.

**One correction this file owes its own earlier draft.** Its first Status block said the cross-repo
hold *"has expired"* because `picker-queue-merge` released. That was asserted from a release commit
without reading the JARVIS board, and it was wrong at the time: two other sessions were live. It is
true **now**, for a different reason, and the difference between those two states is exactly what
reading the board tells you and inferring from a commit does not.

---

## Drained

- **Entry 2 — *on a wayfinder map, a terminal ticket accumulates declines, because each session
  inherits the last decline's verdict instead of re-deriving it*.**
  **Status: ingested 2026-08-14.** Landed as `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` **§9**,
  updated in place — **no new page**, because §8 (*a ticket body never ages*) was already the near
  neighbour and this is its sibling case: §8 is the ticket's **body** going stale, §9 is a previous
  session's **verdict** going stale. `Check-KbLinks` **CLEAN, 67 pages**. A visitor row was held on
  the JARVIS board for the unit, since the board follows the repo being written to.
