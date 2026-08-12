# KB candidates — `c11b-output-formats` (2026-08-13)

Session: `c11b-output-formats` · Ticket: [#30 · `C11b`](https://github.com/idomarhaim/Android_Final_Project/issues/30) · Mode: `AUTO MODE`

Each entry stands alone. No transcript is a source: everything needed to write the page is below.

---

## 1 · Index-level staging is a **one-sided** guard, exactly like per-file staging — and trying it is how you find out

**Claim.** When two agent sessions share one working tree, the standing advice — *stage explicit
paths, never `git add -A`* — protects **the sibling from you** and does nothing to protect **you from
the sibling**. The natural next move is to reach past `git add` and write the index directly, so your
commit contains only your own hunk of a shared commons file. **That works, verifies, and still
loses**, because the git index is a **shared singleton**: when the sibling runs `git add`, it reads
the **working tree**, not your index, and their commit carries your uncommitted worktree lines. Your
carefully-built index is then refreshed out from under you and your own commit ships without the file
at all.

**Why (the reasoning, and what was rejected).** The technique is sound in isolation and was verified
mid-flight — `git diff --cached --stat` reported the intended single insertion:

```bash
git show HEAD:SESSIONS.md > head.md         # the committed content
#   …insert only this session's row…
SHA=$(git hash-object -w staged.md)
git update-index --cacheinfo 100644,$SHA,SESSIONS.md
```

What it cannot do is make the *working tree* private, and the working tree is what a sibling's
`git add <path>` reads. So the countermeasure addresses the direction that was already covered and
leaves the uncovered direction untouched. **Rejected alternatives**, and why each fails:
*wait for the sibling to commit* — unbounded, and they may be waiting on the human; *don't write the
commons at all* — the row **is** the claim's durable record; *amend afterwards* — a history rewrite,
always-ask in both modes, and it would rewrite **their** commit.

**What actually partitions two sessions is a worktree per session**, which is heavier and off by
default under the standing no-worktrees rule. That is not a workaround to find — it is the honest
answer, and the value of this entry is that it **closes off the plausible cheaper one** so the next
session does not spend the same twenty minutes discovering it.

**Observed, 2026-08-13, GoalPilot.** This session built and verified the index blob at
`git diff --cached` → `SESSIONS.md | 1 +`. `c15b-stored-ai-text` then committed `406874d`
(*"c15b: resolve #35"*), which carries **this session's claim row** alongside their own 57-line
release note; this session's `86f3f87` shipped with only its changelog. **Fifth cross-contamination
of the night** across `c5-endless-goals`, `picker-queue-merge`, `c19-area-success-failure` and
`session-titles` — and the **first in which a deliberate countermeasure was tried and lost**, which
is the part none of the other four could record.

**Destination.** `C:\Dev\JARVIS\kb\dev\` — extends the staging finding `session-titles` filed today
(*"staging discipline protects the sibling from you, not you from the sibling"*). This is its
**second half**: the obvious fix does not close the gap either. Grep for that page first and add a
section rather than starting a new one; only create a page if no such page exists.

**Anchors.** `git update-index --cacheinfo`, `git hash-object -w`, `SESSIONS.md`, commits `86f3f87`,
`406874d`, `8b3974a` in `c:\Dev\Android_Final_Project`.

**Supersedes.** Nothing. **Extends** `session-titles`' staging entry rather than replacing it — that
entry's claim is still true, this one bounds how far it can be pushed.

**Status.** 🟢 Ordinary `kb/dev/` material, **`AUTO MODE`-eligible and genuinely this session's.**
The cross-repo hold that parked `c2-task-type`, `c15b` and `c19`'s entries — *`picker-queue-merge` is
live in `C:\Dev\JARVIS`* — **has expired**: that session released at `912d4bc`. Drainable, subject to
a visitor row on the JARVIS board.

---

## 2 · On a wayfinder map, a **terminal** ticket accumulates declines — because each session inherits the last decline's *ground* instead of re-reading the ticket's own charter

**Claim.** A ticket that is *last by design* is declined by session after session, and the mechanism
is specific and avoidable: the first session declines it on a **real** ground; the board and the
changelogs record the decline in the first session's words; and every later session reads **that
sentence** rather than the ticket's own statement of what it is waiting for. The ticket's charter is
a **sequencing rule**, and sequencing rules **expire by being satisfied** — but the recorded decline
is phrased as a *property* (*"terminal by design"*), and a property does not look like something that
can expire.

**Why (the reasoning, and what was rejected).** The check that breaks the chain is cheap and
mechanical: **read the ticket's own blocking condition, in its own words, and evaluate it against
current state** — not the previous session's summary of it. Where the tracker has native blocking
edges, the same check is *already automated* and the frontier query answers it. The failure survives
precisely because the ground the sessions were citing was **not** one of the wired edges: it was a
prose argument in a changelog, invisible to the query that decides what is takeable.

**Rejected:** *wire the extra edges* — attractive, but the later grounds were about **liveness of
sibling claims**, which is board state, not ticket state, and encoding it as a blocking edge would
make the map lie once the sibling releases. *Trust the accumulated declines* — that is the failure.
The right shape is: **the frontier query is authoritative; a prose decline is a note, and it must be
re-derived, never inherited.**

**Observed, 2026-08-13, GoalPilot map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12).**
`#30 · C11b` was declined by three consecutive sessions — `c5-endless-goals`, `c15b-stored-ai-text`,
`c19-area-success-failure` — all citing the map's sentence *"you cannot test a format nobody has
designed yet."* The ticket's body states its condition exactly: *"deliberately blocked on all four
features it serves"* (`C1` #19, `C2` #20, `C8` #24, `C10` #29). **All four had been closed for
hours**, and `C2`'s own resolution §6 had recorded in writing that *"#30 is now fully unblocked —
this was its last open blocker."* The two later grounds that were not about those four were checked
individually and both had expired or did not apply. `#30` was the **map's terminal ticket**, so each
decline delayed the map's destination by a whole session.

**Destination.** `C:\Dev\JARVIS\kb\dev\flows\wayfinder.md` if a wayfinder flow page exists, otherwise
`kb/dev/` as a short page on decision-map hygiene. **Grep for an existing wayfinder page first** —
the walkthrough rule puts flow pages under `kb/dev/flows/<flow>.md` and one may already be there.

**Anchors.** Issues `#12`, `#30`, `#20` §6, `#16` (`C11a`) §8; `SESSIONS.md` decline notes recorded by
`c15b-stored-ai-text` and `c19-area-success-failure`.

**Supersedes.** Nothing.

**Status.** 🟢 Ordinary `kb/dev/` material, **`AUTO MODE`-eligible and genuinely this session's** —
same cross-repo consideration as entry 1, same expiry. Drainable.
