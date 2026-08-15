# KB candidates — `36-tasks-consent`, 2026-08-15

Session: `/implement #36` (Google Tasks consent legibility), branch
`feat/goalpilot-implementation`, `AUTO MODE`.

**Drained 2026-08-15 by `/kb-ingest` into `C:\Dev\JARVIS\kb`.** Both entries' KB halves landed;
this file is rewritten down to its one survivor. Journal entry:
`kb/log/2026-08-15.md` → *"`36-tasks-consent` — compilability as an unclaimable shared resource, and
a screen-entry lifetime trap"*.

- **1 (KB half) — INGESTED** → `kb/dev/agent-topology-and-routing.md`, new section *"The singleton
  nobody can claim — compilability (2026-08-15)"*. Its `rules/` half survives below.
- **2 — INGESTED** → `kb/dev/screen-entry-effects-and-viewmodel-lifetime.md` *(new page)*, plus a
  row in `kb/index.md` and a two-way link with `kb/dev/stale-is-a-data-property.md`.
- Also landed, not originally flagged: a §4 addition to `kb/dev/google-oauth-scopes-and-consent.md`
  bounding how far `GoogleSignIn.hasPermissions` can be trusted. **Additive** — the standing claim
  is left verbatim.

---

## Standing — always-ask

### 1 (residue) · Should §5's parallel-sessions precondition say more than *disjoint working sets*?

**Claim.** `rules/agent-topology-and-model-routing.md` §5 states the precondition for running
sessions in parallel as **disjoint working sets**. 2026-08-15 produced a case where working sets were
disjoint — one session claimed *all-new paths*, sharing nothing with anyone — and it was still both
blocked and blocking, because in a single-module build the **compile state of each source set is
shared by construction and cannot be claimed**. Disjointness buys write-safety; it does not buy
verification-independence. Whether §5's sentence should gain a clause saying so is the open question.

**Why this is not drained with the rest.** Destination `rules/` is a change to how sessions behave,
not a KB page, so it is **always-ask in both modes** and the 🎬 walkthrough rule owns it
(`memory-promotion.md`). Nothing is lost by parking it: the **mechanism and all three instances are
already committed** to `kb/dev/agent-topology-and-routing.md`, which is where a future drafting
session would read them from. This entry holds only the *should-we* question.

**What was rejected, so a later session need not re-derive it.** Adding `#compile` as a claimable
singleton — a claim **excludes**, and excluding sessions from compiling is worse than the problem;
the tree is red for everyone regardless of who holds a token. And *build less often*, which is what
produced the silent blockage in the first place. What actually worked was **notification**: a board
note naming the file, the error and the owner, the shape §5.4 already prescribes for unpublished
work. If a clause is written, that is the behaviour it should require — and note it may not need one
at all, since §5.4 arguably already covers it by analogy.

**A drafting session should also weigh the cost of *not* acting**, which this run measured: blocked
verification blocks committing, and in a shared tree **delay is not free** — a pathspec commit takes
the working tree, so the longer a session holds, the more sibling in-flight work its eventual commit
must carry. The blockage therefore *manufactures* the provenance problem the pathspec remedy exists
to limit. That happened here (`9c6741f` publishing another session's call sites), and it is the
strongest argument that this is more than a nuisance.

**Destination.** `rules/agent-topology-and-model-routing.md` §5 — or a decision that §5.4 already
covers it.
**Anchors.** `kb/dev/agent-topology-and-routing.md` § *"The singleton nobody can claim"*;
`CHANGELOG/2026-08-15/36-tasks-consent.md` § Concurrency; commits `9c6741f`, `c208352`, `70a0a39`,
`fba4197` and the three 📣 board notes of 2026-08-15.
**Supersedes.** Nothing. Would **extend** §5, not correct it.
**Status.** `awaiting 🎬` — always-ask, destination `rules/`. Not offered to Ido as a draft; no
wording exists yet, deliberately (the 🎬 rule's order is draft → walkthrough → write, and drafting
was not this session's unit of work).

---

## Not this session's — reported, not drained

Four `kb-candidates/` files were already in the folder at session start and belong to other
sessions; `backlog-triage` reported the same four on 2026-08-15 and also did not drain them:

- `2026-08-13-c15b-stored-ai-text.md`
- `2026-08-13-c2-task-type.md`
- `2026-08-15-c23-goal-category.md`
- `2026-08-15-c24-settings-surface.md`
