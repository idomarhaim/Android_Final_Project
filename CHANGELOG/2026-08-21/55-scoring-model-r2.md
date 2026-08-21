# 55-scoring-model r2 — the standing Firebase grant, the deploy, and a claim of mine that was too strong

> **Summary:** Ido gave a **standing authorisation** for every Firebase action that costs him nothing, and asked for it written where it belongs — so it is now in `docs/OPERATIONS.md` §2 as the canonical text, pointed at from `AGENTS.md`, with `CLAUDE.md`'s now-false *"the deploy is gated by Ido's authorisation"* **deleted rather than hedged**. Then the grant was used: `firebase deploy --only functions` shipped all six functions, including `projectPointsOnTaskWrite` — which **was never exported from `index.ts` and would silently not have deployed**, caught by reading the module's own export list rather than trusting the source. His live total went **40 → 75**, and 75 is not a typo for 70: the one migrated task is now priced from its **105 recorded minutes** (35) instead of the **30** a word-count heuristic gave it. That means r1's *"nothing already stored was re-priced"* was **too strong** and is corrected here — a legacy task with no stored duration round-trips exactly; one **with** a duration re-prices to its real minutes, which is the inversion working, not a defect. Also opened [#58](https://github.com/idomarhaim/Android_Final_Project/issues/58) for the instrumented suite's order-dependence, with a concrete IME hypothesis.

**Session:** `55-scoring-model` (round 2) · **Mode:** AUTO · **Round 1:** [`55-scoring-model.md`](55-scoring-model.md)

---

## 1 · The standing authorisation

**Ido, 2026-08-21:** *"I already gave you authorisation to do any Firebase action that does not
require me to pay money — I want that written where it needs to be."*

| File | What it now says |
|---|---|
| `docs/OPERATIONS.md` §2 | **canonical** — the grant, the permitted list, the always-ask list, and the honest limit |
| `AGENTS.md` | a pointer with the boundary in one bullet, so Copilot reads it too |
| `CLAUDE.md` | the **false** sentence deleted, with what it cost named |

**Why `docs/OPERATIONS.md` and not JARVIS `rules/`.** The grant is **project-scoped** — it is a
fact about *this* Firebase project's costs and Ido's tolerance for them, not a change to how
outward actions work in general. `outward-action-governance.md` is untouched and still governs
everything outward that is not this grant. Putting it in `rules/` would have made a global
behaviour change out of a local permission, and would have needed the 🎬 gate for a sentence
that only describes one project.

**The boundary, because *"costs nothing"* is not self-evaluating.** The project is on **Blaze**
(v2 functions require it), so the honest phrasing is *"inside the free allowance at this
project's size"*, not *"cannot be billed"* — and that is what the doc says. Still always-ask:
moving the billing plan, enabling a paid API, provisioning anything that bills by **existing**
rather than by use, **deleting** anything, project settings, IAM.

## 2 · The deploy, and the export that would have made it a no-op

`firebase deploy --only functions` with `FUNCTIONS_DISCOVERY_TIMEOUT=120` (this machine's
documented trap — the module takes **12 s** to load and the default budget is 10 s).

**Before deploying, the module's own export list was printed rather than assumed:**

```
node -e "console.log(Object.keys(require('./lib/index.js')))"
→ projectPoints, projectChallengeScore, getRecommendations, classifyTask, scoreTask
```

**`projectPointsOnTaskWrite` was missing.** Round 1 added it to `projection.ts` but never
re-exported it from `index.ts`, whose export line still named two projection functions.
Firebase deploys what `index.ts` exports, so the second trigger would have been written,
tested, committed, pushed — and **never created**, with nothing anywhere reporting its absence.

That trigger is not optional: a task **deleted** while done removes its fact in the same batch,
but a task completed **before** `#55` has no fact to delete — only its own legacy `done` field,
which the `completionFacts` trigger never sees. Without it, those totals stop tracking their
own deletions.

`Observed:` the deploy log after the fix — `Successful create operation` for
`projectPointsOnTaskWrite`, `Successful update` for the other five.

**The generalisable half:** a re-export is the one edit that no compiler, no test and no lint in
this project checks. `tsc` is happy, `node --test` is happy, the function simply does not exist.
Printing the artifact's own export list is a two-second check that no amount of reading the
source replaces — the same shape as the KB's *run the consumer, don't inspect the output*.

## 3 · The total: 40 → 75, and why 75 is right

The projection recomputes **on a write**, so the deploy alone changed nothing — the dashboard
still read 40 after it. One write was needed to fire it.

**Blast radius stated before touching the device**, which is the discipline this session's own
parked KB candidate argues for: create one task `deploy-probe-55` in the saxophone goal, delete
it, touch nothing else. Success = total moves, tasks-done stays 5. Both writes landed, the
probe was deleted, tasks-done stayed **5**.

**Result: 75 pts.** The arithmetic closes exactly:

| | |
|---|---|
| four un-migrated legacy tasks, still summed from their stored `points` | **40** |
| the one migrated task, priced from its banked fact: `round(105 min / 3) × ROUTINE` | **35** |
| | **75** |

and the original 70 was `40 + 30`, so that task's stored legacy value was **30** — while its row
had been displaying **+35** all along, because `Task.points` was already derived.

### 3.1 ⚠️ Correction to round 1

Round 1's summary said **"nothing already stored was re-priced."** That is **too strong**, and
this is the correction:

- a legacy task with **no stored duration** round-trips exactly — `p → 3p minutes → p` — and is
  not re-priced. That is the identity the no-backfill decision rests on and it holds.
- a legacy task **with** a stored duration is re-priced to what its minutes are actually worth.
  **That is §1.4 working**, not a defect: *"the fix inverts a constant"*, so a task that
  recorded 105 real minutes stops being worth what a word count said.

The distinction was already correct in the code and in the tests —
`TaskScoringMigrationTest` has a case named *"a legacy task with a real duration is re-priced
from it, which is the inversion"* — so what was wrong was the **summary sentence**, not the
work. Corrected here, in the `#55` thread, and in `docs/PRODUCT_v0.3.md` §1.4's status box.

**The consequence to expect:** as the remaining four legacy tasks are next ticked, each
re-prices from its own duration and the total drifts. Upward, in this account's case. That is
intended and is worth knowing before it looks like a bug.

## 4 · `#58` — the instrumented suite is order-dependent

Round 1 reported *"the one failure is unrelated and flaky"* and left it as an open issue with
no owner. Ido asked whether a ticket existed and whether handling it was the right move.
**It did not exist** (searched open and closed, `flaky OR flake OR instrumented OR androidTest`),
so [#58](https://github.com/idomarhaim/Android_Final_Project/issues/58) was opened.

**Not fixed here, and that is a decision rather than an omission.** `#55` is closed and pushed;
diagnosing cross-test interference means running the suite repeatedly to establish ordering,
which is its own unit of work and would have ridden into a shipped ticket's session. What the
ticket carries instead is the part that would otherwise be lost: both runs' data, the isolation
results, and a **concrete hypothesis** — `performTextInput` raises the IME, the following
`performClick` on the save button is consumed dismissing it, and whether the IME is up depends
on the previous test. `docs/OPERATIONS.md` §4 already documents that exact mechanism for humans
driving the emulator.

## 🧪 Tests

| Layer | Result |
|---|---|
| **Cloud Functions** (`node --test`) | **67 / 0** — re-run after the export fix |
| **Functions build** (`tsc`) | green; export list verified by printing it |
| **Deploy** | 6/6 functions, 1 created + 5 updated |
| **Device** | total 40 → **75**, tasks-done unchanged at 5, probe created and deleted |

JVM (**646/0**), instrumented and rules were run in round 1 and no app source changed in
round 2 — only `functions/src/index.ts`, docs, and this file.

## Files

`docs/OPERATIONS.md` · `AGENTS.md` · `CLAUDE.md` · `functions/src/index.ts` ·
`docs/PRODUCT_v0.3.md` (§1.4 correction) · `SESSIONS.md` · this file

**Not touched:** any app source, `firestore.rules`, the parked `rules/` KB candidate.
