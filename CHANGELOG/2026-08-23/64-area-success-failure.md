# 64-area-success-failure — `kept · missed · still-owed`, and never a rate

> **Summary:** [`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64) — `C19`'s success/failure run ships in **both** placements from **one component**: above the goal list on the life-area screen, and beside the time donut on analytics, where `C17`'s asymmetry sentence lives and nowhere else. `BuildSuccessFailureRunUseCase` counts **windows**, not tasks — a window is kept when everything due in it was done — and the pair is a **tally of the run itself**, so the two numbers on the card cannot disagree with the dots above them. **Two numbers, never a rate**, and there is **no red** anywhere in the component. **1037 JVM unit tests, 0 failures** and **282 instrumented tests, 0 failures** on `emulator-5554` (the whole suite; **20** of them this ticket's), with **seven render-pass PNGs pulled and looked at** — which is how the one defect in this ticket was found.

**Date:** 2026-08-23 · **Session:** `64-area-success-failure` · **Mode:** AUTO · **Issue:** [#64](https://github.com/idomarhaim/Android_Final_Project/issues/64)

## The decision this ticket had to make, and it was not in the brief

§4.7 states its vocabulary in one sentence: **`MISSED` is a failure, `OVERDUE` is not, `EXPIRED`
counts for nothing.** The brief repeats it, the issue repeats it, and it is a sentence about
**three** of `OccurrenceState`'s five constants. `#56` added the other two — `DAY_PASSED` and
`WINDOW_CLOSED` — and §4.7 says nothing about either.

`OccurrenceState.countsAsFailure` answers with **`MISSED` alone**, and its KDoc named this very
surface as *"the reader it is written for"*. **It was not used, and that is the ticket's one real
decision.** Three reasons, and the third is the decisive one:

1. **§2.3's three words predate `#56`'s split.** `MISSED` is defined there as *"a block whose slot
   has gone"*, so *"`MISSED` is a failure"* is a sentence about **the block rung**. §4.7 counts
   **windows**, and all four rungs have one.
2. **The app already calls all four misses.** `OccurrenceState.meetsUserInDailyReview` groups
   `DAY_PASSED`, `OVERDUE`, `MISSED` and `WINDOW_CLOSED` and excludes only `EXPIRED`, and
   `DailyMissReview` puts all four in front of the user as misses. A run built on `countsAsFailure`
   would print `0 missed` about windows the daily review had named that morning — §0.3's *second
   number that quietly disagrees*, manufactured by this ticket.
3. **It would have been structurally always zero.** `OccurrenceDraft.toOccurrence` can produce only
   `ALL_DAY` and `DEADLINE`; there is no way to **type** a `BLOCK` yet (§3.7's batch sheet is `#24`).
   So `MISSED` is unreachable in the shipped app, and a component whose entire subject is two
   numbers would have shipped with one of them frozen at `0` forever — passing every test, and
   wrong on every device.

So §4.7's three words are honoured **exactly as it states them** — `MISSED` counts, `OVERDUE` is
`STILL_OWED` and not a failure, `EXPIRED` counts for nothing — and the two names `#56` added take
the meaning of the rung they belong to. **`countsAsFailure` is untouched**, along with the
whole-enum test that pins it: it is not wrong, it answers a narrower question. Its KDoc's stale
pointer *is* corrected, because a sentence saying *"§4.7 is the reader this is for"* now sends the
next reader to the wrong property.

**Decision taken per `rules/derivable-decision.md`** — it turns on the artifact, not on Ido — and
recorded here as **mine**. One message reverses it.

## The second decision: a window is a **bucket**, and the pair is its tally

The brief says *"a window **is** an occurrence"*. The screen's own sentence says *"a window counts as
kept when **everything** due in it was done"*. Those cannot both be the whole truth, and the
prototype is internally inconsistent about it — `learn`'s tally (`5 kept · 2 missed · 1 nothing-due`)
matches its 8-dot run exactly, while `health`'s (`23 · 5 · 2`) matches nothing on its own frame.

Resolved toward the bucket, and the prototype's own README is why:

- **`nothing-due` is only representable on a bucket.** It is in §4.7's state list and on the legend,
  and an occurrence cannot be *"nothing due"*. A model where a spec'd state has no referent has
  dropped it.
- ***Everything*** **is plural.** An occurrence is one thing.
- **Round 5 of the prototype fixed exactly this defect**: *"Learning showed 11 kept / 7 missed above
  a list with one active goal and two asleep. The numbers contradicted the list they sat on top of.
  **Now 5 / 2, and the run pattern matches both.**" Making the pair a tally **of** the run makes that
  class of contradiction unrepresentable rather than merely fixed — there is only one number.

The brief's sentence still did its job: the run needs `#63`'s collection to know what was due in each
bucket, which is exactly what it was arguing.

**One dot per window**, oldest first: 30 daily windows · 8 weekly · 6 monthly, rolling from today so
the newest window always ends today. Rolling rather than snapped to a calendar week, for
`RepeatRule.datesFrom`'s reason — *"`AppRegion`'s week start governs how a **calendar is drawn**; it
has no business deciding when a commitment recurs."*

## What shipped

**`domain/usecase/BuildSuccessFailureRunUseCase.kt`** (new) — pure, takes its clock and its zone as
arguments, and knows nothing about life areas. That last part is what makes `C17`'s asymmetry true by
construction: it takes **the goals it should count**, so the life-area screen hands it one area's and
analytics hands it all of them, and a task serving two areas is counted **whole** under each because
there is no arithmetic that could divide a success. `TimeAllocationUseCase` is the half that divides,
and it divides minutes.

Per-occurrence classification, in order: a **skip** counts for nothing (§2.1 — *"a skip is not a
miss"*); a stored `Done` is **kept**; a task with no occurrence documents and no rule falls back to
§7.1's *stored* leg — its completion fact is the answer, which is every task written before `#63`,
and deliberately narrow so one legacy flag can never mark a whole series kept; otherwise §2.3's
derived state decides. Work **not yet due** counts for nothing, or today's window would read missed
every morning.

**`ui/components/SuccessFailureRun.kt`** (new) — one component, both placements, differing only by
`showAsymmetryNote`. Outcome state is drawn by **form** and never hue: filled · hollow ·
dashed-with-a-centre-pip · dotted, plus §4.7's fifth shape, a dashed ring carrying a `+` for
*no next step*. **There is no red in the file**, no `colorScheme.error`, and no ratio anywhere.

**Both screens** — `LifeAreaDetailScreen` between the header and the goal list, `AnalyticsScreen`
directly under the donut card. The life-area screen's standing note saying the run was *"left out
rather than mocked up"* is **discharged rather than deleted**: it named what was missing and why, and
`#63` retired the reason.

**Two stale pointers to this ticket, corrected in place.** Both were written by an earlier
session predicting what `#64` would be, and both had gone stale in the **flattering** direction —
they read as decisions already taken. `OccurrenceState.countsAsFailure` said §4.7 was *"the reader
it is written for"* (see above). `OccurrenceOutcome.Done` said *"points **per occurrence** … is
`#64`'s"* — `#64`'s own text scopes itself to the run and lists what is out of scope, and points
are in neither list, so that was `#63` assigning work to a ticket that never accepted it. Both now
say what is true, keep the constraint that was the useful half, and point at where the reasoning
lives. Neither property's behaviour changed and no test moved.

**Strings** in `values/` and `values-iw/`. §0.8 is suspended so nobody has **seen** the Hebrew, but it
is authored — taken from the prototype's own Hebrew frame — so resuming `#51` is a render pass rather
than a writing job.

## `Let it go` is not here, and that is the deviation to know about

§4.7 has it *"beside the offer"*. It is **not rendered**, and the reason is the rest of that same
sentence: *"`Let it go` stays a command, never an inference — `C4` forbids the app asserting an
intrinsic edge by itself."* There is no such command in the app today, and a button proposing that a
goal is over **while doing nothing** is worse than the honest silence. `GoalRepository.setArchived`
exists and would be the obvious wiring, but archiving a goal from a summary card is a **deletion-class
action** on a surface that is not the goal's own — always-ask, and not this ticket's. Named in the
component's KDoc and here; the two offers §4.7 tabulates both ship.

## 🧪 Tests

| Layer | Result |
|---|---|
| **Server unit / integration / endpoints** | Not exercised — this ticket touches no `functions/` code |
| **Database (`firestore-tests/`)** | Not exercised — no `firestore.rules` change. `users/{uid}/occurrences` is already covered by the owner-only `users/{uid}/{document=**}` match (`#63`), and this ticket only **reads** it |
| **JVM unit** | **1037 tests, 0 failures, 0 errors, 0 skipped** (`:app:testDebugUnitTest`). **22 are new** — `SuccessFailureRunTest`, one test per sentence of §4.7 |
| **Client component / UI (instrumented)** | **20 tests, 0 failures** — `SuccessFailureRunUiTest` on `emulator-5554`, via `adb install -r` + `am instrument` |
| **Instrumented, whole suite** | **282 tests, 0 failures** across 39 classes — the regression check, run after the work was complete. Nothing this ticket touched broke anything: `SettingsScreenTest`, `CalendarSurfaceUiTest`, `UnmeasuredPercentRenderTest` and every other render pass are green, and so are the two screens that gained a new injected repository |
| **Render pass** | **7 PNGs** in `docs/render-passes/2026-08-23-64-area-success-failure/`, pulled and looked at. Light and dark for all three frames plus the 30-day wrap |

`SuccessFailureRunTest`'s 22 are named after §4.7's sentences rather than after functions, so a rule
that is quietly dropped fails a test whose name says what was lost: *a missed block makes its window
missed* · *an overdue deadline is still owed and is NOT missed* · *an expired block counts for
nothing* · *a skipped window counts for nothing — a skip is not a miss* · *a passed all-day and a
closed span both count as missed* · *a window is kept only when everything due in it was done* · *a
task serving two areas counts whole under each of them* · *a miss outside the chosen range is
filtered out, not forgotten* · *there is no rate anywhere — the pair is a tally of the run itself* ·
and the rest.

### Three failures found while running, all mine and all fixed

1. **The literal sweep caught the test tags.** `ui/components` is a **swept** package, so
   `AnalyticsLiteralSweepTest` reads every literal in the file and calls `"success_failure_kept"`
   user-facing prose — three alphabetic words, to a regex. `MaterialPicker`'s tags in the same
   package are the idiom (`"materialTile_" + id`), and camelCase plus a concatenation keeps the
   literal to one word.
2. **A Hebrew prefix was bonded to a format argument.** `ל%1$s יעדים` is §4.8's named defect —
   `HebrewTerminologyTest` fails on it mechanically. Rewritten as `יש כאן %1$s יעדים`, which gives
   the preposition a Hebrew word to attach to.
3. **Six instrumented assertions used `onNodeWithText` where several nodes matched.** `kept` is on
   the pair label, in the legend **and** inside the window sentence; `months` is in the idle line
   **and** on the `6 months` range chip. Fixed with `onFirst()` / pinned counts, and the `capture`
   probe now takes `onLast()` — which is strictly the better check anyway, since the bottom-most
   occurrence is the one a clipped capture loses first.

## 👁️ What the render pass caught, and it was invisible in the source

**The `no next step` mark filled itself in.** §4.7 wants *"a dashed ring carrying a `+`…
deliberately unlike all four, because it is an invitation and not an outcome"*. Drawn first at
`17.dp` with arms at `0.24` of the diameter and a plus as thick as its ring, it read as a **dense
blob** whose nearest neighbour on the screen was the **still-owed pip** — the one dot it must never
be confused with, and §0.8's *one chip may not carry two axes*. Every assertion in the suite passed
while it was true: a per-node query cannot see that two marks resemble each other.

Redrawn at `20.dp` with a smaller, thinner plus and air between it and the ring. The frames in the
folder are the second pass. Full account: that folder's `README.md`.

## What is NOT covered, said rather than skipped

- **Hebrew.** §0.8 is suspended by this ticket's brief; the strings exist and pass the parity guard,
  and nobody has looked at them rendered.
- **The four materials.** These frames are the default material. The component inherits `GpCard`, so
  `MaterialRenderPass` is the surface that photographs all four and this pass does not duplicate it.
- **Points per occurrence.** `OccurrenceOutcome.Done`'s KDoc names it as `#64`'s and it is **not
  here**: it needs `completionFacts`' key to widen, which is a migration on live data. `#64`'s own
  text scopes itself to the run, and the run counts windows rather than points.
