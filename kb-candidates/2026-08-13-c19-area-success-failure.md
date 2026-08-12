# KB candidates — `c19-area-success-failure`, 2026-08-13

## 1. Confirming an aggregate's "open" rows checks one direction only — reconcile against the collection-wide query

- **Claim:** When a listing endpoint serves a stale `state` for its members, the repair *"confirm
  every item the listing calls open"* is **half a check**. It catches a **closed** item reported
  open, because you query that item. It cannot catch an **open** item reported **closed** — that
  item is never queried, so the error is silent and it *removes* work from the derived set. The
  covering check is a **collection-wide authoritative query plus reconciliation of the totals**:
  `gh issue list --state open` over the whole repo, then account for every returned id against the
  aggregate's own open set. Concretely tonight: `/issues/12/sub_issues` said *26 children, 23
  closed, 3 open*, and the repo said *15 open issues* — three map children + the map itself +
  eleven non-map issues = 15, so **nothing was hiding behind a stale `closed`**. The check costs one
  extra call and its output is a number that either balances or does not.
- **Why:** `c15b-stored-ai-text` hit the stale-`state` defect for real (a ticket closed sixty
  seconds earlier was still served as open) and wrote the rule from the instance it saw. Written
  from one instance, a rule inherits that instance's direction — and the untested direction here is
  the more dangerous one, because a frontier ticket dropped from the set produces a *plausible,
  smaller* frontier with no error anywhere in the output. Rejected alternative: querying all 26
  children individually — correct but 26 calls, and it still trusts the aggregate's **membership**
  list, which the collection-wide query does not.
  **Generalises past `gh`:** any cached or denormalised index over records that also has a
  per-record read. Verifying the rows the index flags is not verifying the index.
- **Destination.** `C:\Dev\JARVIS\kb\dev\runtime-verification.md` — the **same section**
  `c2-task-type`'s entry 1 and `c15b`'s entry 1 are both waiting to open. The three are one claim
  from three directions: a **write** is a hypothesis until you read it back; a **read through an
  aggregate** is one too; and a **confirmation** is one as well, because it can only confirm what
  the aggregate agreed to show you. They should land as one section with three instances, not as
  three pages or three raced writes.
- **Anchors:** `SESSIONS.md`, the `c15b-stored-ai-text` claim note (the original stale-`state`
  observation, with timestamps) and the `c19-area-success-failure` claim note (the reconciliation);
  `CHANGELOG/2026-08-13/c19-area-success-failure.md`. No in-repo source file — the instrument is the
  GitHub API, not project code.
- **Supersedes:** nothing, but it **extends** the rule stated in `c15b-stored-ai-text`'s claim note.
  If that rule reaches the KB first as written, this entry rewrites it rather than sitting beside it
  — which makes it always-ask under the supersedes gate at that point, though not today.
- **Status.** 🟢 Ordinary `kb/dev/` material, **`AUTO MODE`-eligible and genuinely this session's**
  — **held**, for one reason and it is the same reason the two sibling entries are held: the
  destination is a cross-repo write into `C:\Dev\JARVIS`, where `picker-queue-merge` is **live**.
  `kb/` is not in that session's claimed paths, so the ingest is legitimate; it needs a visitor row
  on **that** board plus `kb/index.md` and `kb/log/`, which is its own small unit — and it should
  drain **together with** `c2-task-type` entry 1 and `c15b` entry 1, since separately they would
  write the same section three times.
