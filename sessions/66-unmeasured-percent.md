---
repo: c:\Dev\Android_Final_Project
branch: main
mode: normal
status: active
issue: 66
owns:
  # Corrected at /kickoff, 2026-08-23 -- see "What the owns list got wrong" below.
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Goal.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/DerivedProgress.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/GoalCard.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/ComponentStrings.kt
  - app/src/main/java/com/idomarhaim/goalpilot/ui/components/UnmeasuredMarker.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/AnalyticsScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/AnalyticsStrings.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-iw/strings.xml
  - app/src/test/java/com/idomarhaim/goalpilot/domain/UnmeasuredPercentTest.kt
  - app/src/androidTest/java/com/idomarhaim/goalpilot/ui/UnmeasuredPercentRenderTest.kt
  - kb-candidates/2026-08-23-66-unmeasured-percent.md
  - CHANGELOG/2026-08-23/66-unmeasured-percent.md
  - sessions/66-unmeasured-percent.md
  # NOT owned, and the brief was wrong to list it:
  #   domain/usecase/BuildWidgetSnapshotUseCase.kt -- site 5 is a FALSE POSITIVE.
created: 2026-08-23
result: |
  Code half SHIPPED in 7de9bc0, 2026-08-23. 1012 JVM unit tests, 1 failure, and the failure is
  60-calendar-surface's untracked CalendarSurfaceUiTest.kt -- all 98 tests across the ten suites
  this session touched pass, 16 of them new.
  STILL OWED, and it is why status is still `active` and #66 is still open:
    - the instrumented run + render pass (UnmeasuredPercentRenderTest is WRITTEN and has NEVER
      executed). Blocked twice over: emulator-5554 is 60-calendar-surface's, and a second AVD
      cannot boot -- 408 MB free of 16 GB, observed 2026-08-23 04:02. No sign-in is needed for
      it; use `adb install -r` + `am instrument`, never connectedDebugAndroidTest.
    - the push (auto-push preconditions 5 and 2 both stop; see SESSIONS.md).
---

# An unmeasured goal still renders a percentage — on six surfaces, and the app says one out loud

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `normal`

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`docs/PRODUCT_v0.3.md` §0.3, §1.3, §1.5, §1.6, §7.1](../docs/PRODUCT_v0.3.md) · the §Ticket section
below · `BuildWidgetSnapshotUseCase.measureLabel()`'s KDoc, which is the **precedent** and already
carries most of the argument.

**Task:** stop the app stating a percentage for a goal that has no measure.

## Where this came from

Found by `#65`'s **render pass**, 2026-08-23, and recorded in
[`CHANGELOG/2026-08-23/65-measure-proposal.md`](../CHANGELOG/2026-08-23/65-measure-proposal.md).
`#65` added a dashed-square marker meaning *no number yet* to every list row of an unmeasured goal.
The dark render shows it beside `0%` and `0/100` on the same row — the marker says there is no
number and the row prints two.

⚠️ **This is not a regression `#65` introduced, and reading it as one will send you to the wrong
file.** The percentage was always there. `#11`'s own issue body opens with it — *"it currently reads
`Health · 1/100 %`"* — as a **live goal on Ido's account**. The marker only makes it legible as
wrong, which is arguably the marker doing its job.

## Why it is a defect and not a rendering preference

- **§1.3.** A measure is *a closed kind plus a free word*, and **absence is the default** (`E6`).
  `"%"` survives **only as a *chosen* `PERCENT` measure**; it stops being what a goal gets for saying
  nothing. A goal with `measure == null` that renders `0%` is showing the very default §1.3 deleted.
- **§0.3, the map's most-repeated finding** — *a second number that quietly disagrees*. Here it is
  worse than disagreement: `Goal.targetValue` defaults to `100.0`, so the ratio is against a target
  **nobody set**, and the percent is a fraction of it. Both are arithmetic on a fiction.
- **The precedent is already in the codebase.** `BuildWidgetSnapshotUseCase.measureLabel()` gates on
  `hasMeasure` **and** suppresses the label for a chosen `PERCENT` measure, with a KDoc that makes
  exactly this argument: *"printing `45%` beside a ring reading 45 … trains the eye to read two
  numbers as two facts."* The reasoning is settled; it was applied at one site and not at the others.

## The six sites — verified by reading each, not by grep

| # | Site | What it does today |
|---|---|---|
| 1 | [`ui/components/GoalCard.kt:107`](../app/src/main/java/com/idomarhaim/goalpilot/ui/components/GoalCard.kt#L107) | `percentText(goal.progressPercent)` plus a `current/target` meta line — unconditional |
| 2 | [`feature/goals/GoalDetailScreen.kt:184`](../app/src/main/java/com/idomarhaim/goalpilot/feature/goals/GoalDetailScreen.kt#L184) | the header ring and percent, now sitting directly above `#65`'s offer |
| 3 | [`feature/lifeareas/LifeAreaDetailScreen.kt:271`](../app/src/main/java/com/idomarhaim/goalpilot/feature/lifeareas/LifeAreaDetailScreen.kt#L271) | `"${goal.progressPercent}%"` — unconditional |
| 4 | [`feature/analytics/AnalyticsScreen.kt:827`](../app/src/main/java/com/idomarhaim/goalpilot/feature/analytics/AnalyticsScreen.kt#L827) | a bar whose `trailing` **and** `countUpTo` animate to the percent |
| 5 | [`domain/usecase/BuildWidgetSnapshotUseCase.kt:98`](../app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildWidgetSnapshotUseCase.kt#L98) | **half-fixed** — `measureLabel()` correctly returns blank, and `percent` is still passed unconditionally, so the home-screen ring draws |
| 6 | [`data/remote/RecommendationRepositoryImpl.kt:389`](../app/src/main/java/com/idomarhaim/goalpilot/data/remote/RecommendationRepositoryImpl.kt#L389) | **the app says it out loud** — `"<goal>" is at <n>%. Break it into a tiny next step` |

⚠️ **Site 6 is the one to fix first, and it is worse than a stray number.** Its filter is
`goals.filter { it.progressFraction < 0.34f }.take(2)`. An unmeasured goal reads
`currentValue / 100.0`, so a goal nobody has logged against sits at **exactly `0.0`** and is
therefore **systematically the first thing picked**. The offline nudge feed preferentially selects
goals that have no number and then tells the user their percentage. `Observed:` 2026-08-23 by reading
the filter and `Goal.progressFraction`; **`Untested:` on a live account** — worth reproducing before
deciding how far the fix reaches.

Site 6 also has a wire half one line up: `RecommendationRepositoryImpl.kt:103` sends
`"progressPercent" to it.progressPercent` **to the model**, so the model is told a number for a goal
that has none and may repeat it in speech the fallback never touches.

## The one design call — it is Ido's, and it is why this is not a one-liner

**What does a row show where the percentage was?** Three candidates, and none is obviously right:

1. **Nothing** — the row carries title, category and `#65`'s marker, and no number at all. Cleanest
   against §1.3; leaves visible dead space where every other row has a figure.
2. **The raw logged count** — *"11 sessions logged"* rather than a percent of nothing. This is what
   the `C22` prototype's own life-area frame renders (`'num','Get fit','no number — 11 sessions
   logged'`), so there is a drawn answer to copy.
3. **The marker alone, widened** — the dashed square carries the whole statement and the trailing
   slot stays empty.

**Do not pick this by yourself.** The prototype at
[`docs/prototypes/2026-08-15-measure-proposal/`](../docs/prototypes/2026-08-15-measure-proposal/)
already draws candidate 2 in its `bRows` table and is the closest thing to a decided answer — read it
before proposing anything else. A bar chart (site 4) and a widget ring (site 5) may also need
different answers from a list row, and saying so is a legitimate outcome.

## What `/kickoff` found before starting — two corrections to this brief *(2026-08-23)*

**Mode.** This brief says `mode: normal`; Ido opened the session with `AUTO MODE`, and the session's
own message wins. Commits, pushes and KB drains are ungated for this session.

### 1 · Site 5 is a FALSE POSITIVE — `BuildWidgetSnapshotUseCase` needs no change

The brief calls it *"half-fixed — `percent` is still passed unconditionally, so the home-screen ring
draws"*. It does not. Nine lines above the site the brief cites:

```kotlin
val live = goals.filterNot { it.isArchived }
val measured = live.filter { it.hasMeasure }
...
goals = measured.sortedWith(...).map { it.toWidgetGoal() },
goalsWithoutMeasure = live.size - measured.size,
```

An unmeasured goal never reaches `toWidgetGoal()` at all, so `percent = progressPercent` is only ever
evaluated for a goal that has a measure. `Observed:` by reading the function at `HEAD`; the filter
has been there since `b2ba24c` (`widget-pack`, 2026-08-15), eight days before this brief was written,
so it is a misreading of the file and not drift. The widget already **counts** the excluded goals
(`goalsWithoutMeasure`), which is the same answer this ticket reaches everywhere else.

**The brief's line-98 citation is what caused it** — the line is real and the code around it is not
what the line implies. The lesson generalises past this ticket: a site list built by reading a *line*
rather than the *function that reaches it* over-reports.

### 2 · Three sites the brief missed, and they are the same defect

The brief counts **the digit**. The digit is not the only thing computed from `progressFraction`:

- **`GoalCard`'s `GpLinearProgress`** — an unmeasured goal's bar fills to `currentValue / 100.0`.
  `#11`'s own live example (`Health · 1/100 %`) draws a 1 %-full bar, which is the same fiction with
  no digit attached.
- **`AreaGoalCard`'s `GpLinearProgress`** — identical, one screen over.
- **`GoalHeaderCard`'s `ProgressRing`** — the ring is a fraction display and sits directly above
  `#65`'s *"this goal has no number"* note.

Removing the digit and leaving the bar would be half the fix, so all three go with their digits.

### 3 · `AreaGoalCard` carries no marker at all

`#65` put `UnmeasuredMarkerIfNeeded` on **one** row (`GoalCard`), not *"every list row"*. §1.3 says
the marker belongs **wherever the goal is listed**, and the life-area screen lists goals. Added here,
because this ticket is already rewriting that row's trailing slot and leaving it the only unmarked
goal list in the app would be a worse state than before.

### 4 · The design call — DERIVED and DECIDED, not asked *(and it is Ido's to overturn)*

The brief says *"do not pick this by yourself"* and, in the same paragraph, names the artifact that
picks it: the prototype §1.3 itself links as the design for this feature draws the unmeasured row as

```js
['num','Get fit','no number — <bdi>11</bdi> sessions logged'],
```

— **candidate 2**, with **no trailing percentage and no bar**, the marker at the head of the row and
the honest count in the secondary line. `rules/derivable-decision.md` says not to ask what a
committed principle already answers, and `kb/dev/a-later-prototype-outranks-the-brief.md` says which
artifact wins when a brief and a prototype disagree. So: **decision taken per §1.3's named design
asset**, logged here, and one message from Ido reverses it.

**What the prototype does not draw, derived per surface rather than copied:**

| Surface | Answer | Derived from |
|---|---|---|
| goal detail header | marker at ring size + `no number — N logged`; no ring, no `0 / 100` | §1.3 *"the absence is stated as legal before anything is offered"* — the ring contradicted the note under it |
| analytics progress bar | the goal is **excluded**, and a footnote says how many were | §4.4 already refuses to rank goals by a fraction of their own target; `goalsWithoutMeasure` is the same answer the widget reached |
| widget | unchanged — already correct | see correction 1 |
| offline nudge | unmeasured goals excluded from the `< 0.34f` filter | the filter is arithmetic on `targetValue`'s `100.0` default |
| wire payload | `progressPercent` omitted when `measure == null` | the model is told a number the goal does not have |

**The count is entries only, not entries + completed tasks.** The goal's own screen renders a
*Progress log* section listing exactly those entries, so `N` is a number the user can count. Adding
task contributions would make it agree with nothing on screen, which is §0.3.

## Exit

- **JVM tests** for the predicate itself — that a goal with `measure == null` is excluded wherever
  the six sites now branch, **including the `progressFraction < 0.34f` nudge filter**, which is pure
  and is the highest-value test here.
- **Instrumented test + render pass** covering a list row, the goal header, the life-area row and the
  analytics bar, **light and dark**. §0.8 is suspended: **English only.**
- ⚠️ `adb install -r` + `am instrument`, **never** `connectedDebugAndroidTest` — it uninstalls the app
  and takes the Firebase sign-in with it.
- **Widgets need their own look** (site 5) — a snapshot change that renders off-app, so check the
  home screen and not only the test.
- `CHANGELOG/<date>/unmeasured-percent.md`, and this brief moved to `sessions/done/` with
  `status: done` in the same commit.

## Out of scope

- **The measure proposal itself** — shipped in `#65`, and this ticket does not touch the marker, the
  offer, the schema or the dismissal.
- **Deciding what a measure is** — `#11` and §1.3 closed that; this is purely about what the app
  *renders* when there isn't one.
- **`Goal.targetValue`'s `100.0` default.** Changing it is a data-shape question (§7.1) that reaches
  Firestore and every existing document. Leave it; branch on `measure == null` instead, which needs
  no migration.

---

## Ticket — POSTED as [`#66`](https://github.com/idomarhaim/Android_Final_Project/issues/66) on 2026-08-23

*Kept verbatim so the brief and the issue can be diffed. `#66`'s body is this section with the
blockquote prefix stripped; it was read back from the API and is byte-identical to what was sent.*

**Title:** `An unmeasured goal still renders a percentage — six surfaces, and one of them speaks it`

**Body:**

> Found by `#65`'s render pass, 2026-08-23. **Not a regression `#65` introduced** — the percentage was
> always there, and `#11`'s body opens with it as a live goal reading `Health · 1/100 %`. `#65`'s new
> dashed-square marker (*no number yet*) now sits beside `0%` and `0/100` on the same row, which is
> what made it legible.
>
> §1.3 makes absence the default and keeps `"%"` **only as a chosen `PERCENT` measure**. §0.3 is the
> map's most-repeated finding, *a second number that quietly disagrees* — and here both numbers are
> arithmetic against `Goal.targetValue`'s `100.0` default, a target nobody set.
>
> **Six sites, each verified by reading it:**
>
> 1. `ui/components/GoalCard.kt:107` — percent + `current/target`, unconditional
> 2. `feature/goals/GoalDetailScreen.kt:184` — header ring and percent, directly above `#65`'s offer
> 3. `feature/lifeareas/LifeAreaDetailScreen.kt:271` — unconditional
> 4. `feature/analytics/AnalyticsScreen.kt:827` — bar `trailing` and `countUpTo` both animate to it
> 5. `domain/usecase/BuildWidgetSnapshotUseCase.kt:98` — **half-fixed**: `measureLabel()` already
>    returns blank for `!hasMeasure`, `percent` is still passed, so the home-screen ring draws
> 6. `data/remote/RecommendationRepositoryImpl.kt:389` — **the app says it out loud**:
>    `"<goal>" is at <n>%. Break it into a tiny next step`
>
> **Site 6 is the priority and is worse than a stray number.** Its filter is
> `goals.filter { it.progressFraction < 0.34f }.take(2)`, and an unmeasured goal sits at exactly
> `0.0`, so the offline nudge feed **preferentially selects goals with no number** and then quotes
> their percentage. One line up, `:103` also sends `progressPercent` to the model.
> `Observed:` by reading the filter and `Goal.progressFraction`. `Untested:` on a live account.
>
> **The precedent is already in the repo.** `BuildWidgetSnapshotUseCase.measureLabel()` gates on
> `hasMeasure` and suppresses a chosen `PERCENT` label, with a KDoc making exactly this argument. The
> reasoning is settled; it was applied at one site and not the rest.
>
> **One design call, and it is Ido's:** what a row shows where the percentage was — nothing, the raw
> logged count (*"11 sessions logged"*, which the `C22` prototype's life-area frame already draws), or
> the marker alone. A bar chart and a widget ring may want different answers from a list row.
>
> **Out of scope:** the measure proposal itself (`#65`), what a measure is (`#11`, §1.3), and
> `targetValue`'s `100.0` default — branch on `measure == null`, which needs no migration.
>
> Brief: `sessions/66-unmeasured-percent.md`.
