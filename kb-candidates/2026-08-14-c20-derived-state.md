# KB candidates — `c20-derived-state`, 2026-08-14

## 1. A derived number needs a stored writer only when someone who cannot read its inputs has to read it

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
- **Destination.** `C:\Dev\JARVIS\kb\dev\` — **new page**, working title
  `derived-state-needs-a-reader-not-an-owner.md`. It is not `one-metric-and-its-mechanism.md` (that
  page is about a metric and how it is produced; this is about whether a produced value is stored at
  all) and not `runtime-verification.md`. **Entry 2 below belongs on the same page**, as the shape
  question that follows the placement question.
- **Anchors:** [#42 · `C20`](https://github.com/idomarhaim/Android_Final_Project/issues/42) and its
  resolution comment §1–§2; [#34](https://github.com/idomarhaim/Android_Final_Project/issues/34)
  (the recompute-and-store proposal this narrows); `firestore.rules` §`users/{uid}`,
  §`publicProfiles`; `app/src/main/java/com/idomarhaim/goalpilot/domain/model/User.kt#L14` (the
  worked example, in production, that the ticket had listed as a *problem* site);
  `CHANGELOG/2026-08-14/c20-derived-state.md`.
- **Supersedes:** nothing. It **narrows** `#34`, which is an open enhancement issue rather than a KB
  claim, so no committed knowledge is rewritten.
- **Status:** 🟢 **`AUTO MODE`-eligible** — destination `kb/dev/`, contradicts no standing KB claim.
  **Held only on the cross-repo write** into `C:\Dev\JARVIS` (a claim on that board, plus
  `kb/index.md` and `kb/log/`), and it should land **together with entry 2**, which is the same page.

## 2. Idempotency is a property of the shape you store, not a discipline the writer must keep

- **Claim:** When a handler can fire more than once, the usual remedy is to make the handler
  *careful* — dedupe keys, guards, "the function must be idempotent". That treats idempotency as a
  duty. It is more often a **choice of what you store**. A handler that **accumulates** (`total +=
  delta`, `FieldValue.increment`) is non-idempotent by construction and no amount of care removes
  the class of bug; a handler that **projects** — reads the record table and writes `f(records)` —
  produces the same output however many times it runs, so idempotency stops being violable. So the
  design question is not *"how do we make the trigger idempotent"* but *"is the number we store an
  accumulation or a projection"*, and choosing the projection deletes the whole failure class
  instead of guarding it.
- **Why:** `#34` named double-crediting *"exactly the failure that would be hardest to notice"* and
  proposed to solve it with care inside a recompute-and-store handler. Its own sibling decision
  (`C1`) had independently chosen the projection shape for a different reason — that an accumulator
  stores a number able to disagree with the facts under it — and the two arguments turn out to select
  the same design from opposite directions. The corroborating instance is a **live bug** in this
  repo: `TaskRepositoryImpl.setDone` accumulates `points` and reads `task.points` **at untick time**,
  so tick at 10 → re-score to 30 → untick loses 30 for a 10, and `.coerceAtLeast(0)` silently absorbs
  the drift at the floor. Careful code, wrong shape. Rejected alternative: idempotency keys on the
  event — real and standard, but it makes correctness depend on a key surviving every retry path,
  where the projection needs nothing to survive. **Cost, stated:** a projection reads N records per
  write, so it trades an O(1) write for an O(N) one — which is why the rule is about the *shape being
  available*, not about projections always winning.
- **Destination.** Same **new page** as entry 1, as its second section — placement first, then shape.
- **Anchors:** [#42 · `C20`](https://github.com/idomarhaim/Android_Final_Project/issues/42)
  resolution §3; [#34](https://github.com/idomarhaim/Android_Final_Project/issues/34) (its own
  idempotency paragraph and its `increment` rejection);
  `app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt#L121`;
  [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19).
- **Supersedes:** nothing.
- **Status:** 🟢 **`AUTO MODE`-eligible**, same hold as entry 1 — one cross-repo write, one page.

## 3. An empty frontier is not a finished map — a fog patch states its own precondition, and the session that discharges it is not the one that reads it

- **Claim:** On a decision map, work is found by querying the frontier (open, unblocked, unclaimed
  tickets). When that query returns nothing, the natural conclusion is *the map is done*. It is not:
  the map's **fog** section holds patches that are in scope and merely not sharp, and a patch
  typically **names the condition that would sharpen it** — *"not sharp until X decides Y"*. So the
  real frontier check is two queries, not one: **open tickets, then fog patches whose stated
  precondition has since been discharged.** The gap is structural rather than a lapse — the session
  that resolves X is looking at X's ticket, not at a fog bullet elsewhere on the map that happens to
  cite it, and the skill's *"graduate any fog the answer has made specifiable"* asks it to notice
  something written in a different section about a different subject. Graduating that patch is a
  complete unit of work, not a preamble to one.
- **Why:** Observed here: `#12`'s fog bullet said in its own words *"it is not sharp until `C1`
  decides whether `points` moves at all."* `C1` closed deciding exactly that, recorded *"filed
  nothing"*, and three subsequent sessions read the frontier as empty. Four of the five bullets on
  that map state a precondition, so the check is mechanical and cheap: grep the fog for *"until"*,
  *"not sharp"*, *"once X"*, and test each against the closed set. The remaining bullet's
  precondition was *"until the offer has been lived with"* — a precondition on **use**, not on a
  ticket, which is the honest negative case and shows the check discriminates. Rejected framing:
  treating this as a *discipline* failure to be fixed by telling resolving sessions to reread the
  fog — that has been the instruction all along and it did not fire, because the reader is looking
  at the wrong section. Making it a **claim-time** check moves it to the session that is already
  reading the whole map body.
- **Destination.** `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` — **new section**, sibling to §8
  (*a ticket body goes stale*) and §9 (*a verdict goes stale*). This is the third member of that
  family and is distinct from both: nothing here is **stale**, it is **discharged** — the fog text
  was accurate when written and remained accurate, and what changed was a fact elsewhere that the
  text itself told you to check. Verify the section number at ingest; §9 was added 2026-08-14 by
  `c11b-output-formats` and pushed the old *Adjacent* section down.
- **Anchors:** [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12) *Not yet
  specified* (before this session: five bullets; after: four);
  [#42 · `C20`](https://github.com/idomarhaim/Android_Final_Project/issues/42) (the graduation);
  [`C1` #19](https://github.com/idomarhaim/Android_Final_Project/issues/19) (*"filed nothing"*);
  `SESSIONS.md` claim note; `CHANGELOG/2026-08-14/c20-derived-state.md`.
- **Supersedes:** nothing — it **extends** §8/§9's family rather than correcting either.
- **Status:** 🟢 **`AUTO MODE`-eligible** — destination `kb/dev/`, adds a section, contradicts no
  standing claim. Same cross-repo hold as entries 1 and 2; a different page, so it can ride the same
  ingest.
