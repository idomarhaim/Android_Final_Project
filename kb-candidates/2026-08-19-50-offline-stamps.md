# KB candidates — `50-offline-stamps`, 2026-08-19

Session: `50-offline-stamps` · issue [#50](https://github.com/idomarhaim/Android_Final_Project/issues/50) ·
mode `AUTO MODE` · commit `d577dcf`.
Account: [`CHANGELOG/2026-08-19/50-offline-stamps.md`](../CHANGELOG/2026-08-19/50-offline-stamps.md).

---

> **PARTIALLY DRAINED 2026-08-19** — entries 1 and 2 landed in `C:\Dev\JARVIS\kb\`.
>
> **DRAINED AGAIN 2026-08-20 by `50b-transaction-guard` r2, on Ido's *"ingest what is needed as
> long as it does not harm anything"*.** Entry 3 was **split**: its **knowledge** half — the
> mechanism — landed as `kb/dev/decision-map-charting.md` **§12a**, the action corollary of §12.
> Its ***`rules/`* half is still parked**, which is why this file still exists. A partly-drained
> file is rewritten down to its survivors, never deleted.

## Drained — recorded, not re-ingestable

- **1 · A decision issue closed as *decided* is read downstream as *built*** → `kb/dev/decision-map-charting.md` **§12**, filed as the third staleness member after §8 (ticket body) and §9 (previous verdict). Index row and `kb/log/2026-08-19.md` updated.
- **2 · `MetadataChanges.EXCLUDE` hides an empty snapshot's cache→server transition** → `kb/dev/firestore-write-semantics.md` **§8**, the read-side sibling of a page that was entirely about writes. Same.
- **3a · The mechanism half of entry 3** → `kb/dev/decision-map-charting.md` **§12a** (2026-08-20). Index row and `kb/log/2026-08-20.md` updated. **What is left below is the behaviour question only.**

---

## Standing — always-ask, parked

## 3 — A ticket's authorisation for a deletion is only as good as the premise the ticket states

**Claim.** A ticket that authorises a deletion almost always states *why* the thing is now
dead. That stated reason is a **factual claim about HEAD**, and it was true when the ticket was
written, not necessarily when it is executed. So the authorisation must be re-earned by
re-verifying the premise at HEAD — and where the premise fails, the authorisation does not
reach the deletion, even though the ticket's words still appear to grant it. Not-deleting is
the reversible direction; the deletion costs one line on the day the premise becomes true.

**Why.** `Observed:` #50 §5 authorised deleting `core/net/ConnectivityMonitor.kt` on the stated
premise *"`C20` removes the transaction"*, and the brief carried that authorisation forward
explicitly (*"the deletion in item 5 is prescribed by the ticket, so it is authorised"*). At
HEAD the transaction is still there, so executing the authorisation would have re-opened closed
#3. The generalisation matters because a deletion is the one action the ruleset otherwise makes
**always-ask**: a ticket-granted deletion is precisely the case where the human gate has already
been spent, so nothing downstream will catch a premise that has since gone false. This is the
same object as candidate 1 seen from the *action* side rather than the *evidence* side.

**Rejected:** *"the brief authorised it, so execute it"* — the brief's authorisation is a
pointer to the ticket's, not an independent one, so it inherits the ticket's premise and cannot
outlive it. Also rejected: widening scope to make the premise true (removing the transaction
from `setDone`), which is `C20`'s build half and an unfiled issue, not this session's.

**Destination.** `C:\Dev\JARVIS\rules\` — this changes when an agent may execute a
ticket-authorised deletion, so it is a **behaviour** change, not a KB page.

**Anchors.** #50 §5 · `sessions/done/50-offline-stamps.md` *Carries over* · #3 ·
`TaskRepositoryImpl.kt:98`.

**Supersedes.** Possibly narrows the always-ask deletion carve-outs in
`C:\Dev\JARVIS\rules\derivable-decision.md` — check before drafting.

**Status.** ⛔ **STILL PARKED — always-ask in both modes, and now narrower than it was.**

The **mechanism** is no longer owed: it is committed at `kb/dev/decision-map-charting.md` §12a
(2026-08-20), which records the claim, the incident and the asymmetry, and says in its own text
that it does **not** enact a rule. `look-at-your-own-output.md` §4b holds the guard that made it
checkable rather than merely written down.

What remains is **only the behaviour question**: *may an agent execute a ticket-granted deletion
without re-verifying the stated premise at HEAD?* Destination `C:\Dev\JARVIS\rules\`, which
`AUTO MODE` never covers and which the 🎬 walkthrough rule owns, and it may narrow
`rules/derivable-decision.md` §1's deletion carve-outs — a rewrite of a standing claim, always-ask
twice over. Not dropped: it stays here until Ido rules on it.
