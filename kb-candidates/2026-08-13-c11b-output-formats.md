# KB candidates — `c11b-output-formats` (2026-08-13)

Session: `c11b-output-formats` · Ticket: [#30 · `C11b`](https://github.com/idomarhaim/Android_Final_Project/issues/30) · Mode: `AUTO MODE`

Each entry stands alone. No transcript is a source: everything needed to write the page is below.

**Neither entry is drainable by this session** — see each Status, and note that both Status blocks
were **rewritten after reading the destination files**, because the first draft of each was wrong.

---

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
of the night** across `c5-endless-goals`, `picker-queue-merge`, `c19-area-success-failure` and
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

**Status.** ⛔ **Always-ask in both modes, and held three times over. Not drained, and not proposed as
a diff.**
1. Destination is `rules/`, which `/kb-ingest` may not write and the 🎬 walkthrough gate owns.
2. It **contradicts a standing claim**, which is always-ask regardless of mode.
3. `rules/agent-topology-and-model-routing.md` is **owned by a live claim** — `liveness-from-transcript`
   on the JARVIS board, working §5.3 right now, with that file uncommitted in its tree.

**The first draft of this Status said the cross-repo hold "has expired" because `picker-queue-merge`
released. That was asserted without reading the JARVIS board, and it was wrong: the hold did not
expire, it moved.**

---

## 2 · On a wayfinder map, a **terminal** ticket accumulates declines — because each session inherits the last decline's *verdict* instead of re-deriving it

**Claim.** A ticket that is *last by design* is declined by session after session, and the mechanism
is specific and avoidable: the first session declines on a **real** ground; the board and changelogs
record that decline in the first session's words; and every later session reads **that sentence**
rather than the ticket's own charter. The charter is a **sequencing** rule, and sequencing rules
**expire by being satisfied** — but a recorded decline is phrased as a *property* (*"terminal by
design"*), and a property does not look like something that can expire.

**Why (the reasoning, and what was rejected).** The check that breaks the chain is mechanical: the
**frontier query is authoritative** — open, unblocked, unassigned — and a prose decline is a *note*,
which must be **re-derived**, never inherited. The failure survives precisely because the grounds
being cited were **not** wired edges: they were prose arguments in changelogs, invisible to the query
that decides what is takeable.

**Rejected: wire the extra grounds as blocking edges.** Attractive, and wrong. The later grounds were
about the **liveness of sibling claims** — board state, not ticket state — so encoding them as edges
would make the map lie the moment the sibling released, which is exactly what happened here twice in
one night.

**Observed, 2026-08-13, GoalPilot map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).**
`#30 · C11b` was declined by three consecutive sessions — `c5-endless-goals`, `c15b-stored-ai-text`,
`c19-area-success-failure` — all citing the map's sentence *"you cannot test a format nobody has
designed yet."* The ticket's body states its condition exactly: *"deliberately blocked on all four
features it serves"* (`C1` #19, `C2` #20, `C8` #24, `C10` #29). **All four had been closed for
hours**, and `C2`'s own resolution §6 had recorded in writing that *"#30 is now fully unblocked —
this was its last open blocker."* `#30` was the **map's terminal ticket**, so each decline delayed
the map's destination by a whole session.

**Destination — `C:\Dev\JARVIS\kb\dev\decision-map-charting.md`, as a new section beside §8. Checked,
not guessed: that page exists and §8 is the near neighbour** — *"a ticket body is written at charting
time and never ages — re-read its blockers before answering it."* **No new page.**

**Why it is a section and not a duplicate of §8.** §8 is about the ticket's **body** going stale, and
its remedy is *read the ticket's own closed blockers*. This is about a **previous session's prose
verdict** going stale — a different artifact with a different remedy. §8's failure is answering a
question that no longer exists; this one's failure is **never answering it at all**. They compound: a
session that inherits a decline never reaches §8's check, because it never opens the ticket.

**Anchors.** Issues `#12`, `#30`, `#20` §6, `#16` (`C11a`) §8; the decline notes in
`c:\Dev\Android_Final_Project\SESSIONS.md` recorded by `c5-endless-goals`, `c15b-stored-ai-text` and
`c19-area-success-failure`.

**Supersedes.** Nothing — extends `decision-map-charting.md`.

**Status.** 🟢 Ordinary `kb/dev/` material, genuinely this session's, and the only one of the two that
is `AUTO MODE`-eligible — **held on a live singleton, not on its own merits.** Every `/kb-ingest`
writes `kb/index.md` and `kb/log/2026-08-13.md`, and both are **uncommitted in the working tree of a
live visitor session**: `c15b-stored-ai-text`, mid-drain on `kb/dev/runtime-verification.md`, per the
JARVIS board. Racing a second ingest through those two commons files is the exact contamination
entry 1 is about. **Drainable by the next session into JARVIS once that visitor releases.**
