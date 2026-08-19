# `kb-ingest-map-method` — the map-charting method leaves this repo for the central KB

**Session:** `kb-ingest-map-method` · **Trigger:** bare `/kb-ingest` ·
**Branch:** `feat/goalpilot-implementation` · **Mode:** normal (HITL) ·
**Date:** 2026-08-08.

Ingest only, Markdown only. **No `app/`, `functions/`, `firestore.rules`,
`scripts/` or `docs/` file was touched**, no build ran, no device or emulator was
taken, and live `goalpilot-56e30` was never contacted.

## What left this repo

[`kb-candidates/2026-08-08-product-model-map.md`](../../kb-candidates/2026-08-08-product-model-map.md)
— written by the released `product-model-map` session, **3 entries, all drained**,
file `git rm`'d. The pages landed in the central bundle `C:\Dev\JARVIS\kb`, so they
are not in this repo's diff:

| Entry | Page |
|---|---|
| 1 — a constraint ticket that "prices everything" is two tickets | `kb/dev/decision-map-charting.md` §1 *(new)* |
| 2 — "one knot" usually means the chain was never drawn | `kb/dev/decision-map-charting.md` §2 *(same page)* |
| 3 — `gh` sub-issues and GraphQL `blockedBy` are native | `kb/dev/github-issue-graphs.md` *(new)* |

Entries 1 and 2 were merged into one page deliberately — they were found in the
same document minutes apart, they look alike, and the fix is **opposite** each
time (split what looks atomic; order what looks tangled), which is the whole reason
either is worth writing down. Entry 3 was kept separate: a capability claim about a
third-party product rots on a different clock than a method does, and `gh` 2.96.0
was re-checked on this machine during the ingest rather than taken on trust.

Two repos, so no single commit holds both halves. The tie is the journal entry in
`kb/log/2026-08-08.md`, which names this file **with this repo's path** — the tie
`rules/memory-promotion.md` prescribes for exactly this case.

## What did **not** leave, and why

[`kb-candidates/2026-08-08-c9d-calendar-scopes.md`](../../kb-candidates/2026-08-08-c9d-calendar-scopes.md)
— 3 entries, **not drained, not touched**. That exact path is in the `Owns` column
of the **live** `c9d-calendar-scopes` row on [`SESSIONS.md`](../../SESSIONS.md),
and draining a candidate file means rewriting or deleting it. §5 rule 2: *never
write outside your paths; say so and let the user re-assign.*

Ingesting its content while leaving the file standing was considered and rejected —
it is worse than either option, because the file would go on advertising "pending"
for claims already on a page and the next drain would duplicate them.

The cost is visible and recorded rather than hidden: that file's entry 3 (*a
research ticket's best output is sometimes a task ticket — the deciding fact lives
in an account, not in documentation*) is a decomposition claim that belongs in
`kb/dev/decision-map-charting.md` as its §3. The page was drafted with it and then
cut; the journal entry records the shape of the hole so the next drain fills it
rather than opening a second page.

## 🧪 Tests

**No suite run and none applicable.** No Kotlin, Gradle, Firestore-rules or Cloud
Functions file was touched, so the JVM, instrumented and `firestore-tests/` layers
have nothing to exercise — the same verdict as `product-review`,
`product-device-pass` and `product-model-map` before it. The verification that does
apply is the bundle's own and it is green: `Check-KbLinks.ps1` reports **CLEAN** at
30 pages, no broken links, no orphans, no wikilinks.

## Board notes

Claimed here **and** in `C:\Dev\JARVIS` before the first write — the board belongs
to the repo being edited, not the one the session started in, and an ingest is
cross-repo by definition. `SESSIONS.md` leased per §5.2 rather than claimed, since
four sessions share this board.

**This session's board row is deliberately left uncommitted, and so is not in this
commit.** `SESSIONS.md` currently holds **three** other uncommitted rows —
`c9d-calendar-scopes`, `c11a-free-model-probe` and `c15-language-switching` — so
staging the file would commit three live sessions' claims on their behalf. That is
precisely the commons hazard this board already recorded once: `product-model-map`
never got a commit of its own row because a concurrent session staged it into
`9466990` first. The row still does its job meanwhile — all five sessions share one
working tree, so it is visible to every sibling the moment it is written, and
whichever session next commits the board carries it.

**Recorded, not papered over:** this session added **no row to
[`CHANGELOG/CHANGELOG_README.md`](../CHANGELOG_README.md)**, which it otherwise
owes. That file currently carries `c9d-calendar-scopes`'s **uncommitted** index
row, so editing and staging it would carry a live sibling's prose into this commit.
A lease would not help — the obstacle is the file's uncommitted state, not its
ownership (`kb/dev/flows/lease.md` §4a). The row is owed and left owed, to be paid
by a later session once the file is free.
