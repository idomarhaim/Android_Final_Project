# KB candidates — `c9f-consent-screen-state`, 2026-08-09

Written during `/wayfinder 12 33` (resolved
[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33), `C9f`).

> **Partially drained 2026-08-10** by session `kb-ingest-backlog-drain`, pass 3 of 5.
> **Entries 2, 3, 4 and 5 are ingested** and removed from this file — they landed in
> `C:\Dev\JARVIS\kb` as `dev/google-oauth-scopes-and-consent.md` §3–§6 and
> `dev/learning-pipeline.md`. See `kb/log/2026-08-10.md` for the full record; that
> journal entry is the only tie between this file and those pages, because the two
> live in different repos.
>
> **Entry 1 survives, with its original number**, because it is always-ask in both
> modes. The file is rewritten rather than deleted: deleting on a partial drain
> discards exactly what the always-ask exclusions exist to preserve.
>
> **Entry 4's flag was resolved, not inherited.** It carried *"⚠️ Supersedes — check
> before ingesting"* on the possibility that a KB page held the "production
> hard-blocks sensitive scopes" claim. The drain grepped the whole bundle: **no page
> carries it.** That claim only ever lived in this repo's own docs, which this
> session had already corrected on 2026-08-09. So it superseded nothing and was
> ordinary to ingest.

---

## Standing — always-ask

Not eligible for an automatic drain in either mode. Do not re-reason about the
disposition; the question is only whether Ido wants the change.

## 1. An untested claim written as fact propagates by copying, and ends up as an order

**Claim.** A single unhedged sentence in a docs file — asserting a *counterfactual*
nobody had observed — spread to three files in nine days, and in one of them became a
standing instruction to future sessions (*"leave it there"*). It blocked the correct
fix for a real problem until someone spent a session disproving it. **The tell is
structural and checkable: a claim of the form "if we did X, it would fail" that nobody
could have observed, because observing it requires doing X.** Prose gives no way to
distinguish that from a claim someone watched happen — so the hedge has to be written
at the moment of authorship, when the difference is still known.

**Why.** The concrete case: `docs/OPERATIONS.md` said *"an unverified app in production
returns `Error 403: access_denied` with no override."* What was actually observed on
2026-07-31 was a **Testing**-mode 403 (project owner is not automatically a test user)
— true, and about a different regime. The generalisation was plausible (it *is* true of
Google's **restricted** scopes) and wrong for **sensitive** ones. Rejected alternative:
"require a source for every claim" — too blunt, and the original claim was partly derived
from real experience. What actually distinguishes them is whether the author *ran* the
thing, which is cheap to state and impossible to reconstruct later.

**Destination.** `kb/dev/` — a page on documentation discipline / claim provenance. Likely
new; check for overlap with the existing `mechanism-vs-compliance.md`, which is about a
neighbouring failure (an observation that looked like proof of a mechanism).

**Anchors.** `CHANGELOG/2026-07-31.md:337-346` (origin) ·
`CHANGELOG/2026-08-01.md:252-259` (what was actually observed) ·
`docs/research/2026-08-09-oauth-production-test/README.md` §0 and §3 (disproof).

**Supersedes.** Nothing.

**Status.** ⏸️ **Always-ask, awaiting Ido — 2026-08-10.** Held back by
`kb-ingest-backlog-drain` because the entry's own note is right: this is arguably a
change to **how agents write documentation**, which makes it a `rules/` change rather
than a KB page, and `rules/` is always-ask in both modes and owned by the 🎬 walkthrough
rule. Ido's call at the drain was **park it and be given the `rules/` proposal** — that
draft is written to its canonical JARVIS home, uncommitted and unsynced, pending
`/walkthrough`. This entry closes when he accepts or rejects that draft; if he rejects
the `rules/` framing, it drains here as an ordinary `kb/dev/` page.

> ✅ **Resolved 2026-08-10 — the status above is stale and is kept for the record rather
> than rewritten.** The draft it describes as *"uncommitted and unsynced"* was **accepted
> and shipped**: `C:\Dev\JARVIS\rules\claim-provenance.md`, commit `a7180c6`
> (*"rules-drafts-ship: claim-provenance and question-axis-naming in force"*), listed in
> [`rules/README.md`](file:///C:/Dev/JARVIS/rules/README.md) as *"Promoted 2026-08-10 from
> GoalPilot `c9f-consent-screen-state`"* — this entry, by name. The entry's own close
> condition (*"closes when he accepts or rejects that draft"*) was therefore met **two days
> before this note**, and nothing has pointed at it since.
>
> **Two consequences, neither of them this visitor's to take.**
> 1. **This file is now fully drained**, and `/kb-ingest` §7.5 would `git rm` it. It is
>    **not** deleted here: deleting is always-ask regardless of derivability, and the
>    carve-out for a fully-drained file applies only in *the same commit as the pages it
>    produced* — that commit was `a7180c6`, two days ago. **Awaiting Ido's word.**
> 2. **The rule shipped but never reached the file agents load.** `claim-provenance.md` is
>    cited **nowhere** in `user-rules/my-rules.instructions.md`, while
>    [`AGENTS.md`](file:///C:/Dev/JARVIS/AGENTS.md) line 77 states that file *"carries the
>    same duties in the form the agent sees in **every** repo"*. So a session in **this**
>    repo — the repo the rule was promoted from — cannot reach it. `decision-ladder.md` has
>    the same gap. That is **one more sighting** of the `rules/` → `user-rules/` parity gap
>    (*Inferred*: `picker-delegation-clause` counted six on 2026-08-11 and those six were not
>    re-counted here), and it is named as a standing item in
>    `C:\Dev\JARVIS\sessions\picker-queue-merge.md`.
>
> Annotated by `candidate-queue-audit` (visitor from `C:\Dev\JARVIS`, 2026-08-12) while
> auditing the parked always-ask queue. The stale text above is **left intact on purpose**:
> another session's committed reasoning is not silently rewritten, and how the status came
> to be two days wrong is itself the evidence for the queue-surfacing problem.
