---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: done
created: 2026-08-03
completed: 2026-08-04
commit: 342af48
---

> **Done 2026-08-04 in `342af48`** (run under `AUTO MODE`, which the user set at
> kickoff and which overrides the `mode: normal` above). Both items shipped; see
> `CHANGELOG/2026-08-04/time-insights.md`.
>
> **All exit criteria met.** Two were initially blocked on the emulator singleton
> held by `lifearea-polish`; both were finished in a second sitting once the AVD
> came free — `:app:connectedDebugAndroidTest` 20/20 green, and a live
> re-estimation run that returned 105 minutes for a five-word title, which neither
> fallback rule can produce. See the changelog's "Verified against the live model".
>
> **One thing this brief did not anticipate:** `scoreTask` has a *second* fallback
> signature — the Cloud Function's own flat `10 points / 30 minutes` — which the
> client heuristic can never produce and which `docs/OPERATIONS.md` §4 did not
> list. Any verification run must check against both.

# Time insights: honest durations, and a trend beside the pie

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`. (The session that built the pie ran under `AUTO MODE`; that
was scoped to it.)

**Read first** — [`AGENTS.md`](../AGENTS.md), then the "Life areas and the
time-allocation chart" section of [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md),
then "Verifying the time-allocation chart" and "GROQ rate limits" in
[`docs/OPERATIONS.md`](../docs/OPERATIONS.md).

**Task** — the last two follow-ups under **"Life areas + time-allocation
analytics"** in `TODO/TODO.md`:

1. **Back-fill durations for older tasks.** Tasks created before this feature have
   no `estimatedMinutes` and fall back to 3 minutes per difficulty point
   (`TaskDuration.fallbackMinutes`). A "re-estimate with AI" action would make the
   pie measured rather than inferred — but GROQ's free tier is **30 requests per
   minute**, so it needs the same per-run cap the Google Tasks import uses
   (`MAX_IMPORT = 15` in `DashboardViewModel`) and the same review-before-write
   policy every other AI path in this app follows. The analytics card already
   reports the estimated-vs-inferred split; that line is the success metric.
2. **A trend chart.** The pie answers *"what share"*, not *"is Health growing or
   shrinking"*. A stacked bar per bucket across the selected range (days in a week,
   weeks in a quarter, months in a year) is the natural next chart — bucket
   boundaries should come from `AnalyticsRange`, not be re-derived.

**Carries over**

- The whole computation is pure and already tested:
  `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/TimeAllocationUseCase.kt`
  + `app/src/test/java/com/idomarhaim/goalpilot/domain/TimeAllocationUseCaseTest.kt`.
  A per-bucket trend is the same walk over narrower windows — extend the use case,
  do not recompute in the ViewModel.
- Windows, labels and the locale-aware week start:
  `app/src/main/java/com/idomarhaim/goalpilot/core/util/AnalyticsRange.kt` +
  `app/src/test/java/com/idomarhaim/goalpilot/core/AnalyticsRangeTest.kt`.
  Windows are **half-open** so a midnight completion belongs to exactly one bucket.
- Where durations come from and why one call returns both points and minutes:
  `TaskEstimate.kt`, `functions/src/index.ts`, and the rate-limit reasoning in
  `CHANGELOG/2026-08-03/lifeareas.md`.
- **Chart animation traps, both load-bearing** (in `AGENTS.md` pitfalls):
  `animateFloatAsState` initialises *at* its target so it cannot animate a chart
  into existence — use `rememberChartProgress`; and whatever you pass as its `key`
  must have stable structural equality, which is why `BarItem` carries a
  `countSuffix: String` and not a formatter lambda.
- Proving an LLM call really happened rather than falling back: the fallback
  signatures listed in `docs/OPERATIONS.md` §4.

**Out of scope**

- Reordering areas and surfacing them on the goals list — those have their own
  brief (`sessions/lifearea-polish.md`) and a disjoint working set.
- Any change to how a *new* task gets its duration; that path works and is
  verified.
- The two MUST items (two-account demo, spec title page).

**Exit**

- `:app:testDebugUnitTest` green with new cases for whatever bucketing you add,
  and `:app:connectedDebugAndroidTest` green if you touch a composable (the
  emulator `Pixel_10_Pro_XL` is an exclusive singleton — claim it on
  [`SESSIONS.md`](../SESSIONS.md) first).
- A re-estimation run **verified against the model, not the UI** — a changed
  estimated-vs-inferred count on the analytics card, or a duration the local
  heuristic could not have produced.
- Your own `CHANGELOG/YYYY-MM-DD/<session-label>.md`, written before the commit and
  used verbatim as the commit message.
- Commit on approval; flip the TODO checkboxes only once Ido confirms.
- Release your row on `SESSIONS.md`, and move this file to `sessions/done/` with
  the commit hash.
