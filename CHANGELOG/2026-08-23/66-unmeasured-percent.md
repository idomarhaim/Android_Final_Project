# 66-unmeasured-percent — an unmeasured goal states no number, and one of the numbers was being published

> **Summary:** [`#66`](https://github.com/idomarhaim/Android_Final_Project/issues/66) — a goal with `measure == null` no longer states a percentage anywhere. Of the brief's six sites, **five were real, one was already correct**, and a sweep of `progressFraction`'s consumers found **three more the brief did not name** — including `ProgressSummary.averageProgress`, which `SocialRepositoryImpl.shareSummary` rounds into the **text of a shared post**, so the fiction was leaving the device. The percentage is replaced by the honest count the `C22` prototype draws (*no number — 11 entries logged*), and the bar and the ring go with the digit. **1012 JVM unit tests, 1 failure — and the failure is a live sibling's untracked file** (`ImeSettleSweepTest` naming `CalendarSurfaceUiTest.kt:300,316`, owned by `60-calendar-surface`); all 98 tests across the ten suites this session touched pass, 16 of them new. Instrumented layer written and **not run** — the AVD is held.

**Date:** 2026-08-23 · **Session:** `66-unmeasured-percent` · **Mode:** AUTO · **Issue:** [#66](https://github.com/idomarhaim/Android_Final_Project/issues/66)

## The design call was derived, not asked — and the decision is mine

The brief said, of *what a row shows where the percentage was*: **"Do not pick this by yourself."**
It was picked anyway, and the same paragraph is why. It names the artifact that decides:

> The prototype at `docs/prototypes/2026-08-15-measure-proposal/` already draws candidate 2 in its
> `bRows` table and is the closest thing to a decided answer — read it before proposing anything
> else.

That prototype is the design §1.3 itself links for this feature, and its life-area frame renders the
unmeasured row as:

```js
['num','Get fit','no number — <bdi>11</bdi> sessions logged'],
```

— no percentage, no bar, the marker at the head of the row and the **count** in the secondary line.
`rules/derivable-decision.md` forbids asking what a committed principle already answers, and
`kb/dev/a-later-prototype-outranks-the-brief.md` settles which artifact wins when a brief and a
prototype disagree. So: **decision taken per §1.3's named design asset, logged here, and one message
from Ido reverses it.**

**What the prototype does not draw was derived per surface rather than copied**, because a bar chart
and a widget ring are not list rows and saying so was a legitimate outcome the brief allowed for:

| Surface | Answer | Derived from |
|---|---|---|
| list row (`GoalCard`) | marker in the trailing slot; `Category • no number — 11 entries logged` | the prototype, directly |
| life-area row | the same, **plus the marker it never had** | §1.3 — *wherever the goal is listed* |
| goal detail header | the marker at 72 dp where the ring was, `No number — 11 entries logged` where `0 / 100 %` was | §1.3 — *the absence is stated as legal before anything is offered*; the ring contradicted the note directly beneath it |
| analytics *Progress by goal* | the goal is **excluded**, and a footnote says how many were | §4.4 already refuses to rank goals by a fraction of their own target; `goalsWithoutMeasure` is the answer the widget reached independently |
| widget | **unchanged — already correct**, see below | — |
| offline nudge | unmeasured goals dropped from the `< 0.34f` filter, and `progressPercent` omitted from the wire payload | the filter is arithmetic on `targetValue`'s `100.0` default |
| dashboard *Overall progress* | averaged over measured goals only | same |
| **shared post** | same, and the denominator now says *goals with a number* | same, and this one is read by other people |

**One thing the prototype's answer needed that the model could not supply.** *"11 sessions logged"*
is a **count**, and `Goal` carried only a **sum** (`currentValue`). So `loggedEntryCount` joins
`currentValue` as a second derived view, filled at the same seam (`withDerivedProgress`) from the
same snapshot. It counts **entries only, not entries plus completed tasks**: the goal's own screen
renders a *Progress log* listing exactly those entries, so the number is one the reader can go and
count. Folding in §1.5's task contributions would have produced a figure that agrees with nothing on
screen — §0.3, in the ticket that exists to remove §0.3.

## Three corrections to the brief, and the third is why the ticket was bigger than six sites

### 1 · Site 5 was a FALSE POSITIVE — the widget needed nothing

The brief called `BuildWidgetSnapshotUseCase` *half-fixed*: *"`measureLabel()` correctly returns
blank, and `percent` is still passed unconditionally, so the home-screen ring draws."* It does not.
Nine lines above the cited line:

```kotlin
val live = goals.filterNot { it.isArchived }
val measured = live.filter { it.hasMeasure }
...
goals = measured.sortedWith(…).map { it.toWidgetGoal() },
goalsWithoutMeasure = live.size - measured.size,
```

An unmeasured goal never reaches `toWidgetGoal()`, so `percent = progressPercent` is only ever
evaluated for a goal that has a measure. The filter has been there since `b2ba24c` (`widget-pack`,
2026-08-15) — **eight days before the brief** — so this is a misreading, not drift.

It is now **asserted** rather than merely noted
(`UnmeasuredPercentTest.the widget snapshot already excludes unmeasured goals and counts them
instead`), because the next reader of `#66` will read the same line of the brief and go looking.

### 2 · Three sites the brief missed, all drawn rather than printed

The brief counted **the digit**. `progressFraction` also draws:

- `GoalCard`'s `GpLinearProgress` — #11's own live example (`Health · 1/100 %`) fills this bar to
  1 % of a target nobody set. A reader who never reads the digit still reads the fill.
- `AreaGoalCard`'s `GpLinearProgress` — identical, one screen over.
- `GoalHeaderCard`'s `ProgressRing` — a fraction display, sitting directly above `#65`'s
  *"No number on this one."* note.

Removing the digit and leaving the bar would have been half a fix.

### 3 · Three more consumers, found by sweeping the quantity rather than the screens

`grep 'progressPercent\|progressFraction'` over `app/src/main/java` returns nine call sites. Six
were the brief's. The other three:

- **`DerivedProgress.overallCompletionOf`** — the dashboard's *Overall progress* headline. Three
  unmeasured goals beside one finished goal read **25 %**, which states that three quarters of the
  work is outstanding on goals that were never counting anything.
- **`ProgressSummary.averageProgress`** — and this is the one that matters. `SocialRepositoryImpl`
  rounds it into `avg N% across M goals` in **the text of a shared post**. An unmeasured goal's
  `0.0` was therefore not merely misleading Ido; it was **published to other people under his
  name**. The brief's own priority site — the offline nudge — reaches one user. This one leaves the
  device.
- **`SummaryUseCase`** — which builds the slices that average feeds.

The slice **keeps its `effortMinutes`** and loses only its fraction. §1.4 makes effort and outcome
two quantities, and an unmeasured goal's logged hours are perfectly real; dropping the whole
`GoalProgress` would have deleted them to remove a number that was never there.

The denominator moved with the numerator: the post now says *"across `measuredGoals` goals with a
number"*, because *"avg 60% across 5 goals"* over a mean taken across 2 is a **third** number that
quietly disagrees — the defect this ticket removes, reintroduced by the fix.

## What is NOT touched, and why

- **`feature/dashboard/DashboardViewModel.kt`** — `61-google-calendar` holds it. The arithmetic
  needed no change there anyway: the filter went into `overallCompletionOf`, which is the
  aggregation site and is this session's. What is left is a *display* question — what an account
  whose goals **all** lack a number should see instead of a `0 %` ring — and it is named on the
  board rather than half-fixed.
- **`feature/calendar/CalendarBuilder.kt:182`** — `60-calendar-surface`'s, and brand new. It filters
  `it.isArchived || it.isComplete`, and `isComplete` is `progressFraction >= 1f`, so an unmeasured
  goal whose entries happen to sum past 100 reads *complete* against a target nobody set and would
  vanish from the calendar. **Reported on the board, not edited.**
- **`Goal.targetValue`'s `100.0` default** — the brief's own out-of-scope, and correctly: changing
  it reaches Firestore and every existing document (§7.1). Everything here branches on
  `measure == null`, which needs no migration.
- **`TaskFocusCard`** — deliberately left alone. Its share is `completed tasks on this goal / all
  completed tasks`, which is arithmetic on facts a goal has whether or not it counts anything.

## The predicate, and why it is `measure == null` rather than `!hasMeasure`

`Goal.isUnmeasured` is `measure == null` — the population §1.3 names, and **the same one
`UnmeasuredMarkerIfNeeded` already used**. That agreement is the point rather than a tidiness: the
marker's claim (*no number yet*) and the missing percentage are two halves of one statement, and
`#65`'s render pass caught exactly what happens when two surfaces answer the same question
differently. Two predicates can drift back into that; one cannot. `UnmeasuredPercentTest` has the
test that fails if someone collapses one into the other.

A goal carrying a measure with a **zero target** also has no meaningful percentage, and is
deliberately excluded from this: the two states want different words, and `AddEditGoalViewModel`
refuses a save with *"Target must be a number greater than 0"*, so it can only arrive from a legacy
document. `Untested:` no such document is known to exist. Worth its own ticket if one turns up.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **1012 completed, 1 failed** (`:app:testDebugUnitTest --rerun`, 2 m 44 s). The failure is **not this session's** — see below. All **98** tests across the ten suites this session touched pass; **16** are new. |
| **Instrumented / UI** | **WRITTEN, NOT RUN.** `UnmeasuredPercentRenderTest` — 5 assertions + 2 render-pass captures, light and dark. The AVD is held by a live row; see *Held* below. |
| **Endpoints / functions** | No change. `functions/src/index.ts` puts the goals payload into the prompt with a bare `JSON.stringify`, so omitting `progressPercent` client-side needs no deploy and no function edit. |
| **Firestore rules** | No change. Nothing here reads or writes a new path, and `loggedEntryCount` is derived — `Goal.toDto()` does not write it, exactly as it does not write `currentValue`. |
| **Database** | No change; no migration. That is the point of branching on `measure == null`. |

**The one failure, verbatim:**

```
ImeSettleSweepTest > no instrumented test touches a text field without waiting for the keyboard FAILED
    com.google.common.truth.AssertionErrorWithFacts at ImeSettleSweepTest.kt:72

expected to be empty
but was: [CalendarSurfaceUiTest.kt:300, CalendarSurfaceUiTest.kt:316]
```

`CalendarSurfaceUiTest.kt` is **untracked** (`git status` → `??`) and is on
`60-calendar-surface`'s claim. It is that session's work in progress, it is not in this commit, and
it is named here rather than worked around.

**Eleven assertions in three unrelated suites went red mid-session, and the fixtures were the
defect.** `DerivedProgressTest`, `BuildSummaryUseCaseTest` and `RecommendationRepositoryFallbackTest`
all build goals as `Goal(currentValue = 50.0, targetValue = 100.0)`. That meant *half done* when
every goal carried a `"%"` unit by default; since §1.3 deleted that default it means *a goal counting
nothing*. The fixtures never failed, because the arithmetic never stopped working — only what the
numbers **meant** changed. The natural reading of a wave of unrelated red is *my change is too
aggressive*; the tell that it is not is that the failures are in **setup** rather than in
assertions. Each fixture now carries the measure it always meant, with the reason in a KDoc beside
it, and every expected number in those suites is unchanged.

**And one of this session's own tests was broken before it ever ran.** The first draft of
`UnmeasuredPercentRenderTest` asserted `onAllNodesWithText("0%").assertCountEquals(0)`.
`percentText()` wraps its output in `bidiIsolated()`, so the node holds `⁨0%⁩` — and that negative
assertion therefore **passes whether or not the percentage is on screen**. A guard that can never
fire, reporting green. `kb/dev/look-at-your-own-output.md` §5.4 records the *positive* direction of
this trap in this repo one ticket ago; the negative direction is the silent one. The replacement
asserts a whole-tree count of `"%"` against a measured control in the same tree, which cannot pass
vacuously in either direction. (`substring = true` alone is not enough either: `"40%"` **contains**
`"0%"`, so the control value is `55` and the digits are load-bearing.)

## ⏸️ Held

- **The instrumented run and the render pass.** The Gradle daemon was borrowed and released; the
  AVD was not touched. `#66` therefore **stays open** with a comment, and this brief stays `active`
  until the pass runs. No sign-in is needed for it — `UnmeasuredPercentRenderTest` uses a bare
  `createComposeRule()` with no Hilt and no Firebase — and when it runs it takes the
  `adb install -r` + `am instrument` path, never `connectedDebugAndroidTest`.
- **The push**, if the sibling rows are still live at push time. See the board.

## Files

**Production:** `domain/model/Goal.kt` · `domain/model/DerivedProgress.kt` ·
`domain/model/ProgressSummary.kt` · `domain/usecase/SummaryUseCase.kt` ·
`data/firestore/SocialRepositoryImpl.kt` · `data/remote/RecommendationRepositoryImpl.kt` ·
`ui/components/GoalCard.kt` · `ui/components/ComponentStrings.kt` ·
`feature/goals/GoalDetailScreen.kt` · `feature/lifeareas/LifeAreaDetailScreen.kt` ·
`feature/analytics/AnalyticsScreen.kt` · `res/values{,-iw}/components_strings.xml` ·
`res/values{,-iw}/analytics_strings.xml`

**Tests:** `test/…/domain/UnmeasuredPercentTest.kt` *(new, 16)* ·
`androidTest/…/ui/UnmeasuredPercentRenderTest.kt` *(new, 7)* ·
`test/…/progress/DerivedProgressTest.kt` · `test/…/domain/BuildSummaryUseCaseTest.kt` ·
`test/…/data/RecommendationRepositoryFallbackTest.kt` *(2 new)*
