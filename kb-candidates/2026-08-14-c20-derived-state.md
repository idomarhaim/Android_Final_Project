# KB candidates — `c20-derived-state`, 2026-08-14

> **Partial drain, 2026-08-15.** Entries 2 and 3 landed in `C:\Dev\JARVIS\kb` — see
> `kb/log/2026-08-15.md` for the tie. Entry 1 survives below and is **always-ask**; it keeps its
> original number. **Not deleted**, because a partial drain never is.

## Standing — always-ask

### 1. A derived number needs a stored writer only when someone who cannot read its inputs has to read it

- **Claim:** "Who owns this derived value" is routinely asked as a **topology** question — one
  service, one handler per site, or a shared module — and every one of those answers presumes the
  value needs an owner at all. It usually does not. The discriminator is **readership against the
  authorization boundary**: a derived number is computed at the **read site** unless a reader exists
  who is forbidden from reading its inputs, and only then is it materialised, with the write on the
  side of the boundary that can see the inputs. Applied to GoalPilot's seven derived quantities,
  **five need no writer**, one (`publicProfiles.level`) is a stored function of a field in the same
  document and is simply **deleted**, and the two survivors are exactly the two numbers that cross
  from one user to another. The rule is **checkable, not a matter of taste** — the boundary is
  already written down in `firestore.rules` as `isOwner(uid)`, so applying it is a grep, not a
  judgement.
- **Why:** The question arrived as a three-way fork and the fork was false in the way this map's
  forks keep being false — the options were rival *mechanisms* for a job nobody had checked was
  needed. What made it collapse was **enumerating the readers**, not comparing the mechanisms. The
  cheap tell that the fork is this shape: the candidate answers differ in *where code runs* and none
  of them differ in *who reads the output*. Rejected alternative: deciding per-site by cost (cold
  start, deploy count, latency) — that ranks the mechanisms without ever asking whether five of the
  seven sites are in the question at all, and it was the framing the ticket itself shipped with.
  **Second-order consequence, and it is the one that pays:** the numbers that stay at the read site
  are computed from records that are ordinary writes, so on a client with an offline cache they
  render **instantly and offline** — the eventual-consistency cost the server-owned proposal had
  priced and accepted (`#34`: *"a second or two before the donut moves"*) is not paid at all. An
  architecture decision taken on a correctness argument discharged a *product* defect nobody was
  working on.
- **Destination.** `C:\Dev\JARVIS\kb\dev\derive-dont-stamp.md` — **§1, rewritten in place.**
  *(Corrected 2026-08-15. The original entry said "new page, working title
  `derived-state-needs-a-reader-not-an-owner.md`". That was wrong: see the Status block.)*
- **Anchors:** [#42 · `C20`](https://github.com/idomarhaim/Android_Final_Project/issues/42) and its
  resolution comment §1–§2; [#34](https://github.com/idomarhaim/Android_Final_Project/issues/34)
  (the recompute-and-store proposal this narrows); `firestore.rules` §`users/{uid}`,
  §`publicProfiles`; `app/src/main/java/com/idomarhaim/goalpilot/domain/model/User.kt#L14` (the
  worked example, in production, that the ticket had listed as a *problem* site);
  `CHANGELOG/2026-08-14/c20-derived-state.md`.
- **Supersedes:** ⚠️ **corrected 2026-08-15 — it was filed as `nothing` and that was false.**
  `derive-dont-stamp.md` **§1**'s table has a *write-derived* row reading *"a server-side trigger
  legitimately can, and often should"* own it. This entry's claim is *can, but usually needn't —
  check the readership against the authorization boundary first*, which **narrows that row rather
  than sitting beside it**. Rewriting a standing claim is a deletion of committed knowledge.
- **Status:** ⛔ **ALWAYS-ASK IN BOTH MODES. `AUTO MODE` does not cover it.** It contradicts a
  standing KB claim, which `rules/memory-promotion.md` treats as a deletion. **Not dropped** — it
  wants Ido's word plus one small unit, and it is the better half of this session's output.
  *(2026-08-15: this is a **change of gate**, not a re-statement. The entry was filed 🟢
  `AUTO MODE`-eligible on a bundle check that ruled out `one-metric-and-its-mechanism.md` and never
  looked at `derive-dont-stamp.md`. The drain read that page and the gate changed. A bundle check is
  only as good as the neighbourhood it searched, and a search run at the wrong width does not fail —
  it passes.)*

---

## Drained 2026-08-15 — kept as a record, do not re-ingest

- **Entry 2** — *idempotency is a property of the shape you store, not a discipline the writer must
  keep* → landed as an extension to **`kb/dev/derive-dont-stamp.md` §6**, not as the new page it
  proposed. **§6 already held its core claim**, including the same `TaskRepositoryImpl` observation,
  committed 2026-08-10 by `c9a-schedule-a-task`. What was genuinely new is the **delivery** argument
  (an accumulator is non-idempotent by construction; a projection cannot be) beside §6's existing
  **history** argument — two arguments that never meet, selecting the same row.
- **Entry 3** — *an empty frontier is not a finished map; a fog patch states its own precondition* →
  landed as **`kb/dev/decision-map-charting.md` §10**, sibling to §8 (a stale body) and §9 (a stale
  verdict), old §10 *Adjacent* renumbered to §11.

Journal tie: `C:\Dev\JARVIS\kb\log\2026-08-15.md`.
