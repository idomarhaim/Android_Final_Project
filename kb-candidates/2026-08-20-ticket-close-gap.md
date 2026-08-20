# KB candidates — `ticket-close-gap`, 2026-08-20

Entries 1 and 2 were **drained 2026-08-20** into `C:/Dev/JARVIS/kb` —
`dev/look-at-your-own-output.md` §4h and §4h-i. Journal: `kb/log/2026-08-20.md`, entry
*`ticket-close-gap` (visitor) — the candidate backlog*. Two repos, so that journal entry is the
tie; nothing here is re-ingestable.

Account: [`CHANGELOG/2026-08-20/ticket-close-gap.md`](../CHANGELOG/2026-08-20/ticket-close-gap.md).

---

## Standing — always-ask, parked

## 3 · `gh` classifier denials are non-deterministic, not a permission boundary

**Claim.** The identical `gh issue close -c "<body>"` command was **denied** by the auto-mode
classifier, then **succeeded** on `#6` and `#9`, then was **denied** on `#11`, then succeeded on a
plain retry of the unchanged command. `gh issue comment --body-file` was denied outright.
`Observed:` 2026-08-20, session `ticket-close-gap`, 3 denials in 6 attempts.

**Why.** This repo's `CLAUDE.md` currently records the denial as a fact about the *route* — *"the
auto-mode classifier blocks it"*, of the `git credential fill` pipeline — which reads as a
**boundary to plan around**. It is better modelled as **flaky**: a single retry of the unchanged
command is the correct response, and dressing the command up to get past it is explicitly not.
`hebrew-defer-freeze` (2026-08-17) abandoned three `#51` writes on the first denial and left them
owed for three days; under the flake model it would have retried and finished.

**Why (rejected).** *Leave the existing bullet alone and add a second one* — rejected: two bullets
about `gh` denials, one saying boundary and one saying flake, is worse than either. The existing
claim has to be narrowed in place, which is what makes this always-ask.

- **Destination:** `CLAUDE.md` in this repo. Not the central KB — it is a fact about this machine's
  harness, and that file already carries the `gh` bullets it would amend.
- **Anchors:** `db1597b` · `CHANGELOG/2026-08-20/ticket-close-gap.md` § *The `gh` classifier denied
  3 of 6 writes*.
- **Supersedes.** **Yes** — narrows a standing committed claim from a boundary to a flake.
- **Status.** ⛔ **PARKED — always-ask in both modes.** Re-swept 2026-08-20 by the backlog drain and
  correctly left; `/kb-ingest` §8 keeps a superseding entry out of an auto-mode drain. Ido's call.
