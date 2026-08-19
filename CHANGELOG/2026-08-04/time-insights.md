# Changes — 04/08/2026 · session `time-insights`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** The last two follow-ups under "Life areas + time-allocation analytics": making the pie's durations **measured rather than inferred**, and adding the chart that answers the question a pie structurally cannot.

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
- **`:app:connectedDebugAndroidTest` — 20 tests, 0 failed** on `Pixel_10_Pro_XL`,
  run after `lifearea-polish` released the AVD. Includes the three new
  `StackedColumnChartUiTest` cases (labels when they fit, an empty column staying
  in place, thinning across a quarter) and confirms the sibling session's earlier
  report of the same suite rather than relying on it.
- **Security-rules layer (`firestore-tests/`) — not run and not affected**: this
  change touches no rules and adds no new Firestore path.

## ✅ Verified against the live model, not the UI

Run on `Pixel_10_Pro_XL` against live `goalpilot-56e30`, signed in as the real
account. The analytics card offered **"Re-estimate 1 duration"**; the sheet came
back with **8 candidates, 1 of them flagged as unanswered and unticked**.

The point of the run is that a duration was returned that the offline heuristic
is *arithmetically incapable* of producing. The client rule is
`points = 5 + 3×words` (clamped 5..50) and `minutes = 3 × points`; the Cloud
Function's own failure path is a flat 30 minutes:

| Task | Words | Client fallback | Server fallback | Returned | |
|---|---|---|---|---|---|
| להגיש פרויקט גמר בפיתוח אנדרואיד | 5 | 60m | 30m | **105m** | model |
| להגיש ספר פרויקט גמר | 4 | 51m | 30m | **90m** | model |
| אימון כח | 2 | 33m | 30m | **60m** | model |
| אימון ריצה | 2 | 33m | 30m | **60m** | model |
| Morning run | 2 | 33m | 30m | *(matched a fallback)* | **unticked by the app** |

105 minutes for a five-word title cannot come from a rule whose ceiling for five
words is 60, and it is not 30 — so GROQ answered. The titles are Hebrew, which the
function's system prompt explicitly anticipates, and the values are semantically
sensible (a 10 k training run at an hour, a final-project submission at 1 h 45).

**The honesty check fired in production on the first run.** One of eight came back
matching a fallback and arrived unticked without being asked to. That is the
mechanism described above doing exactly its job on real data, not a hypothetical.

**Confirming wrote 7 durations to live Firestore**, and the card moved the way it
had to:

| | Before | After |
|---|---|---|
| Tracked | `2h` | **`2h 15m`** |
| לימודים | `1h 30m · 75 %` | **`1h 45m · 78 %`** |
| בריאות | `30m · 25 %` | `30m · 22 %` |
| Footnote | *1 of 2 durations estimated by AI; the rest inferred from task difficulty* | **_Durations estimated by AI for all 2 tasks_** |

The re-estimate button is now absent, because nothing in the window is inferred
any more. That footnote was named in the brief as this feature's success metric,
and it is the thing that moved.

Screenshots: `screen1`/`s3`–`s10.png` in this session's scratchpad (not committed).

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

**Nothing.** Both exit criteria that were blocked at first release are now met —
the instrumented suite ran green and the live re-estimation run is recorded
above — and on Ido's confirmation the whole **"Life areas + time-allocation
analytics"** item is closed in `TODO/TODO.md`, parent included: all four of its
follow-ups landed on 04/08/2026, two here and two in `lifearea-polish`.

`.vscode/` is now git-ignored (Ido's call). It held one machine-local setting,
`java.configuration.updateBuildConfiguration: disabled`, which suits this
machine's JDK layout and has no business in a fresh clone — the same reasoning
`.idea/` was already ignored under.

## 🕐 Two sittings, and why the entry says so
This session was written, committed and **released with two exit criteria unmet**
because the emulator was held by `lifearea-polish`, then re-claimed and finished
once the AVD came free. The release was the right call — a claim held open across
a wait blocks work nobody is doing — but it is worth noticing what it cost: the
board had to carry the unfinished verification as prose under "Unclaimed work",
and the next session had to re-derive the device state. **A session that ends
blocked on a singleton should say so in the board row itself**, which is what the
re-claim did.
