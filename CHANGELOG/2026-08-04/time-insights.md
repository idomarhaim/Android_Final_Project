# Changes — 04/08/2026 · session `time-insights`

> **Branch:** `feat/goalpilot-implementation`

The last two follow-ups under "Life areas + time-allocation analytics": making the
pie's durations **measured rather than inferred**, and adding the chart that
answers the question a pie structurally cannot.

## 📈 A trend beside the pie

The donut answers *"what share of my time went into Health?"*. It cannot answer
*"is Health growing or shrinking?"* — that needs the same window cut into
consecutive buckets.

`AnalyticsRange.buckets()` does the cutting, one step finer than the range itself:
a week becomes days, a month and a quarter become weeks, a year becomes months. A
day has no finer calendar unit, so it becomes six four-hour blocks — the only view
that also shows *when* in the day the time went.

Three decisions worth keeping:

- **Buckets tile their range exactly**, derived from the existing
  `startDate`/`endDateExclusive` rather than re-derived. They are contiguous,
  non-overlapping, and inherit `TimeWindow`'s half-open convention, so a midnight
  completion lands in exactly one column. This is what makes "the columns add up
  to the donut" a property rather than a coincidence — and it is asserted for all
  five ranges.
- **Week-aligned buckets are clipped at both ends.** August 2026 starts on a
  Saturday, so with a Sunday week start the month opens with a one-day column. The
  alternative — a first column reaching back into July — would put last month's
  time in this month's chart.
- **DST is handled where it actually bites**, in the day view. Blocks are built by
  adding hours to a `ZonedDateTime` and then clamped to the day's real end,
  because a DST day is 23 or 25 hours long: unclamped, a short day's last block
  runs past midnight and a long day loses its final hour.

`TimeAllocationUseCase.trend()` takes the **finished allocation** rather than the
life-area list. Its slices already carry every name and colour a column needs, and
its order — biggest area first — becomes the stacking order, so the legend under
the donut reads the trend as well. It also means an area with no slice can have no
segment, which is how a task filed under a deleted life area lands in "Unassigned"
here without restating the rule that decided so.

`StackedColumnChart` is the new component. Every column is scaled against the
tallest, never against its own total — per-column normalisation would make a day
with ten minutes look as full as a day with ten hours, inverting the one question
the chart exists to answer. An empty bucket keeps a baseline tick rather than
vanishing, because a missing column reads as a rendering fault and a flat line
reads as a day off. Labels thin out instead of shrinking (`labelStride`), since 13
weeks in a quarter cannot each carry a legible date on a phone.

## 🤖 Re-estimating the durations that were only guessed

Tasks created before durations existed have no `estimatedMinutes` and fall back to
3 minutes per difficulty point. The analytics card already reported the split;
now there is a button that fixes it, and the count on that card is its success
metric.

Same policy as every other AI path here — propose, review, then write:

- **Capped at 15 per run**, deliberately the same number and the same reasoning as
  `DashboardViewModel.MAX_IMPORT`: one `scoreTask` call per row against a free tier
  allowing 30 requests/minute. The sheet says "15 of 42" so the cap does not read
  as the whole problem.
- **In-window completions are asked about first.** Under a cap, order *is* the
  feature: the run should spend its budget on the durations the user can see
  being wrong.
- **Only the minutes are ever written.** `scoreTask` returns points too, and
  rewriting those would be a bug with a long fuse — completing a task already
  awarded its points to the user and to the public leaderboard projection, so a
  task whose points changed afterwards refunds a different number when un-ticked.

### The bug this feature would have shipped with

An AI estimate that is really a silent fallback must not be *written* as an AI
estimate, or the card's "x of y estimated by AI" becomes a lie — which is the
exact number the feature exists to make honest. So a proposal is checked against
what a fallback would have produced, and arrives **unticked** if it matches.

Writing that check surfaced that `scoreTask` has **two** fallback signatures, and
`docs/OPERATIONS.md` documented only one:

1. **Client-side**, when the call never left the device: `5 + 3×words` points,
   `3 × points` minutes.
2. **Server-side**, when the call reached the function but GROQ did not answer: a
   flat `10 points / 30 minutes` from the `catch` in `functions/src/index.ts`.

`5 + 3×words` can never equal 10, so signature 2 is unreachable by rule 1 and
looks like a perfectly ordinary estimate. A check knowing only the client rule —
which is what the docs described — would have waved every server-side GROQ failure
through as a genuine AI duration. `TaskScoring.looksLikeFallback` now encodes
both, `docs/OPERATIONS.md` §4 documents both, and there is a test that asserts
the client heuristic cannot produce the server pair.

This is **evidence, not proof**: a model may legitimately answer 10 points / 30
minutes. The cost of that false positive is one task the user re-ticks by hand;
the cost of the false negative was the card claiming a `catch` block was the AI.

`TaskScoring` also now holds the offline point heuristic that
`RecommendationRepositoryImpl` used to keep privately — one copy, because the
repository *produces* that estimate and the back-fill *recognises* it, and two
copies would make the comparison meaningless the first time either drifted.

## 🧪 Tests

- **`:app:testDebugUnitTest` — 150 tests, 0 failed, 0 skipped** (20 suites; the
  count includes the sibling `lifearea-polish` session's suites in the shared
  tree). 28 of them are new or extended here:
  - `AnalyticsRangeTest` 10 → **16**: buckets tile every range exactly (no gap, no
    overlap, first/last aligned to the window), a week is seven day-long buckets,
    a year is twelve *unequal* month buckets, a day is six labelled blocks, a
    month opens with its partial week, a quarter is 13–14 weeks.
  - `TimeAllocationUseCaseTest` 7 → **13**: the trend redistributes exactly the
    pie's minutes, series order is the pie's order, a dangling area folds into
    "Unassigned" in both charts, a completion outside every bucket counts in
    neither, an empty window yields an empty trend, and quiet buckets stay present
    as zero columns rather than disappearing.
  - `BackfillDurationsUseCaseTest` *(new)* — **11**: candidate filtering and
    ordering, the cap keeping on-screen tasks, and six cases on proposal honesty
    including both fallback signatures, clamping, and a zero-minute answer.
  - `ChartLabelStrideTest` *(new)* — **5**: label thinning, kept free of Compose
    types precisely so it runs here instead of behind an emulator.
  - `RecommendationRepositoryFallbackTest` — unchanged and still green after the
    heuristic moved to `TaskScoring`, which is what makes that move safe.
- **Instrumented layer — compiled, not run.** `:app:compileDebugAndroidTestKotlin`
  passes, so `StackedColumnChartUiTest` (3 cases: labels when they fit, an empty
  column staying in place, thinning across a quarter) is known to build. It has
  **not been executed**: the emulator `Pixel_10_Pro_XL` is an exclusive singleton
  and `lifearea-polish` holds it. See "Still open".
- **Security-rules layer (`firestore-tests/`) — not run and not affected**: this
  change touches no rules and adds no new Firestore path.

## 📁 Modified / added
- `app/.../core/util/AnalyticsRange.kt` — `TimeBucket`, `buckets()`, `bucketNoun`
- `app/.../domain/usecase/TimeAllocationUseCase.kt` — `trend()`, `TimeTrend`
- `app/.../domain/usecase/BackfillDurationsUseCase.kt` *(new)*
- `app/.../domain/model/TaskEstimate.kt` — `TaskScoring`
- `app/.../data/remote/RecommendationRepositoryImpl.kt` — uses the shared heuristic
- `app/.../ui/components/StackedColumnChart.kt` *(new)*
- `app/.../feature/analytics/AnalyticsViewModel.kt` — trend + back-fill state
- `app/.../feature/analytics/AnalyticsScreen.kt` — trend card, button, review sheet
- `app/src/test/.../domain/BackfillDurationsUseCaseTest.kt` *(new)*
- `app/src/test/.../ui/ChartLabelStrideTest.kt` *(new)*
- `app/src/test/.../domain/TimeAllocationUseCaseTest.kt`,
  `app/src/test/.../core/AnalyticsRangeTest.kt` — extended
- `app/src/androidTest/.../ui/StackedColumnChartUiTest.kt` *(new)*
- `docs/OPERATIONS.md` — the second `scoreTask` fallback signature
- `SESSIONS.md`, `sessions/time-insights.md`, this entry *(new)*

## 🧭 Board
Claimed `time-insights` before the first write (`124d4e0`), paths disjoint from
the live `lifearea-polish` row. **Singletons were not**: that row holds the Gradle
daemon and the AVD. The daemon was shared by queueing — Gradle serialised the
builds and both sessions' suites are green in the same run — but the AVD was left
alone entirely, which is why the instrumented layer is compiled and not executed.

## ⚠️ Still open
- **`:app:connectedDebugAndroidTest` not run** — blocked on the emulator singleton.
  One task, after `lifearea-polish` releases its row.
- **No re-estimation run against the live model.** The brief asks for a duration
  the local heuristic could not have produced, verified against the model rather
  than the UI. That needs the app on the emulator against live `goalpilot-56e30`,
  so it is blocked on the same singleton. The fallback signatures above are what
  such a run should be checked against.
- **TODO checkboxes not flipped** — per the convention at the bottom of
  `TODO/TODO.md`, closing an item waits for Ido's confirmation.
