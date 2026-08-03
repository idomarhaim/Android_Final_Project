# Changes — 03/08/2026 · session `lifeareas`

> **Branch:** `feat/goalpilot-implementation` · **Mode:** AUTO MODE
> Sibling file for the same day: [`../2026-08-03.md`](../2026-08-03.md) — the
> `scaffold` session's template-library pass, written before day folders existed.

Four things were asked for, and they are one feature: **goals belong to areas of
your life, and the app can tell you what share of your life each one is getting.**

1. Goals are filed under user-defined **life areas**, which can be **synced from
   Google Tasks list names**.
2. An **interactive pie chart** of what percentage of your time goes into each
   life area — from completed tasks, weighted by the LLM's estimate of how long
   each took, followed up through their goals.
3. That chart at **day / week / month / quarter / year**.
4. Charts that **draw themselves** instead of appearing finished.

---

## 🧩 Life areas (spec §1)

`LifeArea` is deliberately **not** `GoalCategory`. The category is a fixed
ten-value taxonomy the LLM classifies against and the palette is built around; a
life area is whatever the user says their life is divided into, and it is the unit
the time chart reports on. A goal now carries both — `category` for colour, icon
and AI context, `lifeAreaId` for *which part of my life is this*.

- `domain/model/LifeArea.kt` — model + `LifeAreaPalette`: ten categorical hues
  (the `GoalCategory` set, because they end up side by side as pie slices), twelve
  icon keys, `nextHex()` (least-used colour, so a multi-area sync never hands out
  the same hue twice), and a **bilingual** name→icon guesser.
- `feature/lifeareas/` — define, edit, recolour, re-icon and delete areas; file
  loose goals from a per-goal menu; sync from Google Tasks.
- `AddEditGoalScreen` gained a life-area chip row; `GoalDetailScreen` states the
  goal's area (or says it has none, since unfiled time shows as "Unassigned").
- Deleting an area **unfiles its goals first, then deletes** — that order on
  purpose: a half-failure leaves the area alive and retryable rather than leaving
  goals pointing at an id nothing resolves.

### The Hebrew problem, and why the icon guesser is bilingual

The Google Tasks lists this syncs from are the user's own, and they are in Hebrew:
בריאות, לימודים, קריירה, זוגיות. An English-only keyword table would have given
every synced area the same generic flag — precisely the "it didn't understand me"
feeling the sync exists to avoid. `LifeAreaPalette.iconKeyFor` matches both
languages, ordered so the specific term wins (בריאות before the broader body/gym
words). Verified on the real account: heart, graduation cap, briefcase, people.

### Sync policy: propose, never write

`BuildLifeAreaProposalsUseCase` (pure, unit-tested) decides three things:

| Case | Result |
|---|---|
| List already carries an area's `googleListId` | **no row at all** — a second sync must not offer to redo the first |
| An area already has that name (case/space-insensitive) | **LINK** — keeps the user's colour and icon; "Health" typed by hand and "Health" synced are one area |
| Anything else | **CREATE** with an unused colour and a guessed icon |

`GoogleTasksClient.fetchTaskLists()` is a new call rather than a by-product of
`fetchOpenTasks()`: a list with no open tasks returns no tasks at all, and an
empty list is exactly the one a user just made for an area they are about to start
working on. Both calls now share one token helper, so consent granted from the
life-areas screen is not asked for again on the dashboard.

The **task** import also learned about areas: a task's Google Tasks list *is* an
area of the user's life, so the list wins over the model's guess, and confirming
an import will create the missing area (shown on the row as `new area "…"`) as
well as the goal.

## 🥧 Where your time goes (spec §6 Bonus)

```
completed Task ──(goalId)──▶ Goal ──(lifeAreaId)──▶ LifeArea
       └── estimatedMinutes (LLM) ──────────────────────┴──▶ slice
```

- `Task.estimatedMinutes` is new, and `classifyTask` / `scoreTask` now return it.
  **On the existing call, not a new one** — GROQ's free tier is 30 requests/minute
  and the Tasks import already spends one per row, so a second round trip about the
  same sentence would halve what an import can cover.
- `TaskDuration.minutesOf` is the single answer to "how long did that take": the
  stored estimate, else 3 minutes per difficulty point. Every completed task
  contributes *something* — a task worth zero minutes would silently shrink a whole
  area, and tasks created before this feature existed have no estimate at all.
- `TimeAllocationUseCase` (pure, unit-tested) walks the chain over a window.
  Everything unresolvable — no goal, no area, an area since deleted — lands in one
  honest **"Unassigned"** slice rather than being dropped. Time logged against an
  *archived* area keeps its own slice, because it really was spent there.
- The card states how many of the window's durations were the model's and how many
  were inferred. "62 % on Health" means something different when half the minutes
  were guessed, and the screen should not pretend otherwise.

## 📅 Day / week / month / quarter / year

`core/util/AnalyticsRange` — **calendar-aligned, not rolling.** `SummaryPeriod`
windows are rolling ("the last 7 days") because a shareable summary is about
momentum; *"what share of my life went into Health this month?"* is a question
about a calendar month, and a rolling window would keep moving under the user
between two glances at the same screen. Windows are half-open so a task completed
at exactly midnight belongs to exactly one bucket, and the week starts on the
**locale's** first day — Sunday here, which is why a Sunday workout must not land
in last week.

## ✨ Charts that draw themselves

`ui/components/ChartAnimation.kt`. The reason the analytics screen used to snap
into place fully formed is worth recording: **`animateFloatAsState` initialises at
its target on first composition**, so a value that never changes never animates. An
`Animatable` explicitly started at `0f` is the whole difference.

- Bars grow from zero, staggered top-to-bottom (capped at 420 ms total so a
  twenty-goal chart doesn't take two seconds), with their percentages counting up
  in step.
- `DonutChart` (new) sweeps clockwise out of 12 o'clock, and is **interactive**:
  tap a wedge and it thickens while the rest fade back, with the detail rendered in
  the hole; tap it again, or the hole, to clear. Butt caps and a gap taken *out of*
  each wedge, because a rounded cap would add a couple of degrees to every wedge
  and quietly inflate the small ones.
- `BarItem` carries a `countSuffix: String` and not a formatter lambda on purpose:
  it is the animation's restart key, and two structurally identical lambdas are not
  `equals`, so a formatter would restart the sweep on every recomposition.

## 🗂️ Files

**New:** `domain/model/LifeArea.kt`, `domain/model/TaskEstimate.kt`,
`domain/repository/LifeAreaRepository.kt`, `domain/usecase/TimeAllocationUseCase.kt`,
`domain/usecase/BuildLifeAreaProposalsUseCase.kt`, `core/util/AnalyticsRange.kt`,
`data/firestore/LifeAreaRepositoryImpl.kt`, `ui/components/DonutChart.kt`,
`ui/components/ChartAnimation.kt`, `feature/lifeareas/{LifeAreasScreen,LifeAreasViewModel}.kt`,
plus four unit-test classes and two instrumented ones.

**Changed:** `Goal` (+`lifeAreaId`), `Task` (+`estimatedMinutes`),
`TaskClassification` (+`suggestedLifeAreaId`, `+estimatedMinutes`),
`RecommendationRepository.scoreTask` → `TaskEstimate`, the DTOs/mappers,
`GoogleTasksClient`, `GoalRepository.setLifeArea`, `RepositoryModule`,
`Analytics{Screen,ViewModel}`, `Dashboard{Screen,ViewModel}`,
`AddEditGoal{Screen,ViewModel}`, `GoalDetail{Screen,ViewModel}`, `ProfileScreen`,
`GoalPilotRoot`, `Destinations`, `SimpleBarChart`, `GoalCategoryIcon`,
`functions/src/index.ts`.

**Firestore:** `users/{uid}/lifeAreas/{id}` — **no rules change needed**, it is
already covered by the owner-only `users/{uid}/{document=**}` match.

---

## 🧪 Tests

| Layer | Result |
|---|---|
| Server unit / integration / endpoints | **n/a** — the only server code is the three GROQ-proxy callables, which have no test harness in this project. `functions/` type-checks clean (`npm run build`). |
| Database | **n/a as a layer** — no DAO/Room; Firestore access is exercised through the repositories, and against the live project by hand (below). |
| Domain / JVM unit | **92 passed, 0 failed** (`:app:testDebugUnitTest`) — up from 60. New: `TimeAllocationUseCaseTest` (7), `AnalyticsRangeTest` (10), `LifeAreaSyncTest` (9), `TaskDurationTest` (6); `RecommendationRepositoryFallbackTest` grew 4 → 6. |
| Client component / UI (instrumented) | **12 passed, 0 failed** (`:app:connectedDebugAndroidTest` on `Pixel_10_Pro_XL`) — up from 6. New: `DonutChartUiTest` (4, including the wedge hit-test geometry) and `AnimatedBarChartUiTest` (2, that the count-up *arrives* at its final value). |
| UI E2E | **by hand on the emulator**, signed in as `name.iddo@gmail.com` — see below. |

Manual end-to-end pass (this is the record; nothing here was inferred from a
compile):

1. **Life-area sync against the real Google Tasks account** — 7 lists returned,
   all proposed as new; deselecting three left "Sync 4"; confirming created
   בריאות / לימודים / קריירה / זוגיות with distinct colours and the *correct*
   Hebrew-guessed icons, each marked "synced from Google Tasks".
2. **Filing goals** — the per-goal menu moved five goals into areas; the counts on
   the area rows tracked each move (0 → 1 → 2 → 3 goals).
3. **Smart add through GROQ** — "Bench press workout at the gym" came back with
   *Goal: Strength Training · Life area: בריאות · Worth 10 pts · about 30m* and a
   rationale in the model's own words ("…is a strength training activity that fits
   the existing 'Strength Training' goal"). The offline fallback can only ever say
   *"Matched by keyword … (offline heuristic)"*, so that call really reached the
   model — the check `docs/OPERATIONS.md` insists on.
4. **The chart** — with two tasks ticked off, the Week view (Aug 2 – Aug 8) drew
   **2h tracked · לימודים 1h 30m 75 % · בריאות 30m 25 %**, footnoted "1 of 2
   durations estimated by AI; the rest inferred from task difficulty". Tapping the
   purple wedge thickened it, faded the other, highlighted its legend row and put
   *75 % / לימודים / 1h 30m* in the hole.
5. **Ranges** — Week showed the empty-state copy before anything was completed
   this week; Year showed the older completed task as 100 % "Unassigned" with the
   "Fix" shortcut. Range labels render as `Aug 2 – Aug 8`, `2026`, `Q3 2026`.

Two environment notes, neither a code fault: the emulator's SystemUI ANR'd once
right after the instrumented run (dismissed with "Wait"), and the floating IME
panel swallowed the first tap on a task checkbox — both already documented in
`docs/OPERATIONS.md`.
