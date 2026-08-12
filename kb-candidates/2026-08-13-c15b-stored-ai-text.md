# KB candidates — `c15b-stored-ai-text`, 2026-08-13

Session: `c15b-stored-ai-text` · repo `C:\Dev\Android_Final_Project` · branch
`feat/goalpilot-implementation` · map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12),
ticket [#35 · `C15b`](https://github.com/idomarhaim/Android_Final_Project/issues/35).

**Each entry stands alone.** No transcript is a source: everything needed to write the page is
below, including what was rejected and why.

---

## 1 · A read through an aggregate endpoint is a hypothesis, exactly like a write

**Claim.** Verification duty is usually stated over *writes* — you do not know a write landed until
you read it back. The same duty applies to **reads served by an aggregate or index endpoint**: such
an endpoint answers from its own materialised copy of the underlying records, and that copy can lag
the records themselves. A field read from the aggregate is a **hypothesis about the record**, not
the record; when a decision turns on it, confirm it against the primary object.

**Observed, 2026-08-13, GitHub Issues.** Deriving the wayfinder frontier for map `#12` calls
`gh api repos/<owner>/<repo>/issues/12/sub_issues`, which returns each child issue with `number`,
`state`, `assignees` and `title` — everything the frontier query needs, in one call. It returned
**26 children, 4 open**, listing `#21 · C5` as `open`. A direct `gh issue view 21` seconds later
returned **CLOSED**, with the resolution comment timestamped `2026-08-12T22:03:10Z` — roughly sixty
seconds before the listing call. The listing was serving a stale `state`.

**Why this is worth a page rather than a footnote.** The failure is silent and shaped to be
believed: the response is well-formed, complete, internally consistent, and every *other* field in
it is correct. Nothing in the output says the width of the read was wrong. Had it been trusted, this
session would have derived a frontier containing a ticket that was already resolved, claimed it, and
discovered the collision only after starting work — the exact failure the claim-before-write
discipline exists to prevent, arriving *through* the instrument that discipline depends on.

**What was rejected.** *"Just always re-query everything"* — rejected as the wrong generalisation
and unaffordable: the aggregate exists because N+1 direct reads are expensive, and 22 of the 26
children here were closed and irrelevant. The rule that survives is narrower and cheaper: **confirm
only the records a decision turns on** — here, the handful the listing calls *open*, since a stale
`closed` costs nothing (it hides a ticket, and the next derivation finds it) while a stale `open`
costs a wasted claim. Asymmetry of consequence, not blanket distrust.

**Relationship to the `c2-task-type` candidate, and why they should be drained together.**
`kb-candidates/2026-08-13-c2-task-type.md` entry 1 records *a write is a hypothesis until you read
it back*, from a `gh api --method PATCH` that silently did nothing. This is **the same claim from
the opposite direction**, and the two are stronger as one section than as two pages: together they
say the boundary is not read-vs-write but **remote-state-vs-your-belief-about-it**, and that the
confirming call is a different call from the one that formed the belief. Draining them separately
would produce two half-arguments that each look like a war story.

**Destination.** `C:\Dev\JARVIS\kb\dev\runtime-verification.md` — the same new section
`c2-task-type` entry 1 proposes, extended, **not** a second page.

**Anchors.** `SESSIONS.md` → the `c15b-stored-ai-text` claim note, 2026-08-13 (the derivation table
and the ⚠️ paragraph recording the stale read). Prior art in the same folder:
`kb-candidates/2026-08-13-c2-task-type.md` entry 1.

**Supersedes.** Nothing. It **extends** an un-ingested candidate; it contradicts no standing KB
claim.

**Status.** 🟢 Ordinary `kb/dev/` material, **`AUTO MODE`-eligible and genuinely this session's** —
and held anyway, for one reason and not two: the destination is a **cross-repo write into
`C:\Dev\JARVIS`, which is live** (`picker-queue-merge`, four files uncommitted). `kb/` is not in
that session's claimed paths, so the ingest is legitimate — it needs a row on **that** board plus
`kb/index.md` and `kb/log/`, which is its own small unit. **Not drained here.**
