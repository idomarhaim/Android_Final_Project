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
- **Supersedes:** **it now partially contradicts a committed claim, and that changed the gate.**
  Predicted above: *"if that rule reaches the KB first as written, this entry rewrites it rather than
  sitting beside it."* It did — `kb/dev/runtime-verification.md` **§6** was created at `385e87b`
  /`dfb8707` from `c2-task-type`'s and `c15b`'s entries, about forty-five minutes before this was
  written, and its **duty 2** rests on an *asymmetry of consequence*: *"a stale `closed` merely hides
  an item and the next pass finds it, while a stale `open` costs a wasted claim."* **That is exactly
  the half this entry disputes.** In a frontier derivation the hidden item is not merely delayed: the
  derived set **is** the input to the decision made now — which ticket to claim, and the leverage
  argument computed over it — and the output is silent about being short, which is §6's own
  *"a call reports its result and never its scope"*. The correction is narrow and cheap, and it is
  **not** the "always re-query everything" that §6 already rejects: reconcile the aggregate's
  **totals** against one **collection-wide authoritative query**, which either balances or does not.
- **Status.** ⛔ **Always-ask in both modes — not drained, and the reason has changed.** The
  cross-repo hold that held it (a live `picker-queue-merge` in `C:\Dev\JARVIS`) has **expired**: that
  board is now empty and §6 exists. But per `rules/memory-promotion.md` the second always-ask class
  applies — *anything that supersedes or contradicts a standing KB claim* — because this rewrites
  §6's duty 2 in place rather than appending beside it, and overwriting committed knowledge is a
  deletion. So `AUTO MODE` does **not** cover it. It wants Ido's word and then one small unit: a
  visitor row on the JARVIS board, the §6 edit, `kb/log/`, and a lint.
