# `7-quickadd-complete` r2 — 2026-08-20

> **Summary:** Ido delegated the `#19` question (*"do what you think is most right"*), so the
> decision below is **mine** and is recorded as mine. **No ticket was opened and none was
> reopened.** The finding was never *"§1.4 is unbuilt"* — that is deliberate and fine — it was
> that **four artifacts say somebody owns building it and nobody does**, so the fix is to the
> **record**, not to the tracker. §1.4 was audited clause by clause against `HEAD` (six clauses,
> all absent) and now carries a **DECIDED — NOT BUILT** box at the top, because the section is
> written entirely in the present tense and reads as a description of the app. One claim in it was
> stale in the *other* direction — it cites a running-accumulator defect that `C20` fixed in
> `731961b` — and that is corrected too. The same status is recorded on the two surfaces a session
> actually reads (`TODO/TODO_FUTURE/`, `docs/OPERATIONS.md` §3) and in `TaskEstimate.kt`'s KDoc,
> which was the **fourth** copy of the false deferral. JVM unit **515/0**, unchanged.

**Session:** `7-quickadd-complete` r2 · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` ·
**Follows:** `153620b` (`#7` ships) · **Claim:** `279bd2e`

---

## What was delegated, and what I decided

Ido asked *"do you want to reopen closed tickets?"* — I said no. He then said
**תעשה מה שאתה חושב יהיה הכי נכון** (*do what you think is most right*).

That is a **delegation**: it removes the judgment half and leaves the mechanical half owed, and
the reasoning goes on the durable record **as mine**, not as his answer. I had offered two
options — *open an implementation issue*, or *write a line saying it is not in v0.3* — and the
hand-back rule says the delegated answer is often **neither**. It was neither.

**What I decided, and why it is not on that menu.** Re-reading the problem rather than breaking
my own tie: the thing that is broken is **not** that §1.4 is unbuilt. §1.4 being unbuilt is a
reasonable state — points work today, the app is whole, and it is a model migration rather than
a late edit, which is a poor fit for a submission push. What is broken is that **the written
record asserts an owner that does not exist**, in four places, and each copy reads as
corroboration of the last.

So the correct action is a **documentation** one, and it is the one I can take without deciding
anything that is Ido's:

- **I did not open an issue.** Opening one is an outward-facing write and a scoping decision
  about his product and his time. A seventh open issue that nobody will build during a
  submission push also degrades the frontier `ticket-close-gap` had just spent a session
  cleaning up — five open issues, each *correctly* open.
- **I did not decide §1.4 is out of v0.3.** That is his. What I recorded is the **fact** — decided,
  not built, not owned, not scheduled — plus what it would take if he wants it.
- **I did not reopen #19.** It is a decision ticket, it is correctly closed, and nothing here
  touches it.

## Why §1.4 itself was the highest-value place to write it

`decision-map-charting` §12's own account of this failure names the artifacts that carry it
forward, and puts **the spec section the decision produced** first. §1.4 is written **entirely in
the present tense**, as accomplished fact — *"the `5..50` cap **is deleted**"*, *"it **retires**
`heuristicPoints`"*, *"`GoalProgress.points` **is deleted**"*. A reader arriving there has no way
to tell design from description, and it is the artifact everyone reads.

**Audited clause by clause, by grep, before writing a word of the box:**

| §1.4 says | At `HEAD` |
|---|---|
| `points = round(minutes/3) × difficulty` | ❌ no `difficulty` enum anywhere in `app/src/main` |
| the `5..50` cap is deleted; ceiling 50 → 240 | ❌ `TaskScoring.MIN_POINTS = 5`, `MAX_POINTS = 50` |
| `heuristicPoints` is retired | ❌ alive, **two** live call sites in `RecommendationRepositoryImpl` |
| points banked as a timestamped completion fact | ❌ no `completionFacts`; the fact is still `done` + `completedAt` on the task |
| `GoalProgress.points` is deleted | ❌ still on `ProgressSummary.kt:40` |
| §1.5's `goalEdges: [{ goalId, contribution }]` | ❌ absent; `Task.progressContribution` still carries it |

The *"ceiling rises 50 → 240"* clause is the **same constant** as the cap row (`MAX_POINTS`), not
a separate one — folded rather than listed twice, because a table that inflates its own count is
the thing this box exists to stop.

### The one claim that was stale in the other direction

§1.4 cites `TaskRepositoryImpl.kt:120-127` as a **live defect** — a running accumulator over
`task.points` that loses 30 for a 10 on an untick, with `.coerceAtLeast(0)` absorbing the drift.
**That accumulator is gone**, removed by `C20` (#42) in `731961b`; `setDone` is a single-document
`update` and the total is summed by `functions/src/projection.ts`.

Corrected in place rather than deleted: the paragraph's *argument* — bank points as their inputs
so the arithmetic never branches — stands on its own merits. It simply no longer has this defect
to point at, and a spec that keeps citing a fixed bug as motivation is how a fixed bug gets
"fixed" a second time.

## Where else it is now recorded, and why each one

| File | Why there |
|---|---|
| `docs/PRODUCT_v0.3.md` §1.4 | the artifact that states the design as fact — §12's first-named carrier |
| `TODO/TODO_FUTURE/ProductModel.TODO.future.md` | its `C1` checkbox is now `[x]`, **for the decision** — and that ambiguity is named explicitly, because a ticked box on a decision map is exactly what let four artifacts read *decided* as *handled* |
| `docs/OPERATIONS.md` §3 *What's left* | `AGENTS.md` calls this the start-here doc for a new session. It is the one piece of remaining work with **no ticket and no brief**, so nothing else would surface it |
| `domain/model/TaskEstimate.kt` KDoc | the **fourth** copy of the false deferral, and the only one sitting next to the code it is about — `heuristicPoints` survives with no scheduled retirement |

`TaskEstimate.kt` is the one worth calling out: its KDoc read *"the inversion that retires
`heuristicPoints` is §1.4's, and it belongs to `C1` #19."* A reader takes that at face value and
concludes this code has an owner waiting to delete it. It does not.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **515 / 0**, unchanged — nothing here changes behaviour |
| **Build** | `:app:compileDebugKotlin` green (the only code touched is a KDoc block in `TaskEstimate.kt`) |
| **Instrumented** | **Not re-run, and not applicable** — no composable, no ViewModel and no repository changed. `139/0` from `153620b` stands |
| **Cloud Functions / `firestore.rules`** | **Not applicable** — untouched |

## One thing worth keeping from how this went

The `TaskEstimate.kt` edit shipped a literal `''' + chr(92) + '''` seven times into a KDoc — a Python
expression that had been written inside a string rather than beside it. It compiled fine, because
it is a comment. It was caught by **reading the file after writing it**, which is the same duty
`kb/dev/look-at-your-own-output.md` §5 states for computed output and the same one that caught
r1's truncated render capture. Two instruments in one session, both silent, both found by looking.
