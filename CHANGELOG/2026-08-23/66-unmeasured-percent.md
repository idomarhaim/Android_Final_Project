# 66-unmeasured-percent — an unmeasured goal states no number, and one of the numbers was being published

> **Summary (revised 04:58 — the device pass ran, and found an eighth site):** [`#66`](https://github.com/idomarhaim/Android_Final_Project/issues/66) — a goal with `measure == null` no longer states a percentage anywhere. Of the brief's six sites, **five were real, one was already correct**, and a sweep of `progressFraction`'s consumers found **three more the brief did not name** — including `ProgressSummary.averageProgress`, which `SocialRepositoryImpl.shareSummary` rounds into the **text of a shared post**, so the fiction was leaving the device. The percentage is replaced by the honest count the `C22` prototype draws (*no number — 11 entries logged*), and the bar and the ring go with the digit. **1015 JVM unit tests, 0 failures** and **14 instrumented tests, 0 failures** on `emulator-5554`, with **six render-pass PNGs pulled and looked at** — which is how the eighth site was found at all.

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
  vanish from the calendar. **Reported on the board, not edited** — and **fixed by that session in
  `a3e91c5`**, using the `isUnmeasured` accessor this ticket shipped that morning. Their commit
  message calls it *"`#66`'s seventh site"* and makes the observation this session's own eighth site
  then repeated: **a defect class is at its most reproducible while it is being fixed elsewhere**,
  because the sweep that enumerated it cannot see code that does not exist yet.
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
| **JVM unit** | **1015 completed, 0 failed** at 04:56 (the 1-failure run at 04:00 is below, and the failure was a sibling's). Earlier: **1012 completed, 1 failed** (`:app:testDebugUnitTest --rerun`, 2 m 44 s). The failure is **not this session's** — see below. All **98** tests across the ten suites this session touched pass; **16** are new. |
| **Instrumented / UI** | **14 tests, 0 failures** on `emulator-5554` (`adb install -r` + `am instrument`, 43.7 s). 8 assertions + **6 render-pass captures** — list row, goal header ×2, life-area row, analytics bar — all in **light and dark**, all pulled and looked at. Two of them failed first and correctly; see the revision below. |
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

---

# Revision, 04:58 — the pass ran, and looking at it was not a formality

`60-calendar-surface` released in `5d5e2a3`, freeing `emulator-5554` and the Gradle daemon, and
`a3e91c5` fixed the seventh site this session had reported on the board. Both of the things held at
04:12 stopped being held, so the render pass ran. It did three things.

## 1 · It passed

14 instrumented tests green, via `adb install -r` + `am instrument` — never
`connectedDebugAndroidTest`. **No sign-in was needed and none was destroyed.** Six PNGs: the list
row, the goal header (unmeasured and control), the life-area row and the analytics bar, **light and
dark**, which is the whole of the brief's Exit for this layer.

Before installing, the test APK reported `UP-TO-DATE` — the `look-at-your-own-output.md` §4c-ii
trap, since a sibling had run the same task. It was verified rather than trusted: the class was
probed for **inside the built APK's dex**, including a method name that exists only in the rewritten
version, and again after each rebuild.

## 2 · It found an EIGHTH site, and only by being looked at

Frame 4 rendered `Renovate the flat  45%` with `Other • 45/100 %` underneath — **the same number
twice on one row** — while **every assertion in the file passed**. A per-node Compose query cannot
see a relation between two marks, which is §4e's finding arriving on this ticket.

`BuildWidgetSnapshotUseCase.measureLabel()` has dropped exactly that label since `#11`:

> a goal that genuinely chose `PERCENT` still belongs on the tile, but its label would restate the
> ring digit for digit

The **tile** had the rule. The three surfaces that draw a goal row did not — which is this ticket's
own opening sentence one step further on: *the reasoning is settled; it was applied at one site and
not the others.* `Goal.restatesPercent` now carries it, and `GoalCard`, `AreaGoalCard` and
`GoalHeaderCard` all **suppress** the label.

⚠️ **Suppress, not reformat, and the reason is that the pair can genuinely disagree.**
`progressPercent` is `currentValue / targetValue`, so a `PERCENT` goal with a target of `50` and
`45` logged renders **`90%`** beside a label reading **`45/50 %`**. Dropping the label leaves the one
number the goal's own arithmetic produced. Asserted both ways in `UnmeasuredPercentTest`.

## 3 · The capture helper's own floor was wrong, and its fix fired on first run

`file.length()`, `bitmap.width` and `bitmap.height` all describe the **container**;
`look-at-your-own-output.md` §4g records this exact helper shape passing over a capture missing two
of the five states it existed to show. `capture()` now takes a `lastFrameProbe` and asserts it
**displayed** before capturing — and on its first execution it **failed twice, correctly**: two goal
headers on one page put the control's ratio below the fold, so the PNG would not have contained it.
Split to one header per capture.

**Its second failure was mine, and is the more useful one.** The probe was `40/100`, copied from the
list row — the **header** renders `"$current / $target $unit"`, i.e. `40 / 100`, with spaces. The run
said *"component is not displayed"* about a card that was rendering perfectly. Same family as §5.4:
recompute the string the consumer sees, never the one you remember writing. Two different formats
for one ratio, in one ticket, is itself worth a later look.

## What that cost, and what it bought

Three composables moved from `private` to `internal` (`GoalHeaderCard`, `AreaGoalCard`,
`ProgressByGoalCard`). The alternatives were driving the real screens — Hilt, Firebase, seeded data,
a different test — or rebuilding approximations in the test file, which would **exhibit something
that is not the thing under test**. `GoalDetailScreen.kt` already exposes `AddTaskRow` this way.

## The widget needs no look, and that is a finding rather than a skip

The brief says *"widgets need their own look (site 5) — a snapshot change that renders off-app, so
check the home screen and not only the test."* There **is** no snapshot change: site 5 was a false
positive, `BuildWidgetSnapshotUseCase` filters unmeasured goals out before building a `WidgetGoal`,
and this session's only edit to that file is none. Nothing about the home screen moved, so there is
nothing there to look at.

## ⏸️ Held

- ~~**The instrumented run and the render pass.**~~ **RUN at 04:45.** 14 tests, 0 failures, six PNGs
  looked at. `#66` is closed.
- **The push** — still held. `61-google-calendar`'s row is live and its commits are in
  `@{u}..HEAD`, so auto-push precondition 5 stops. Precondition 2 stops independently on a rename in
  the range (`sessions/unmeasured-percent.md` → `sessions/66-unmeasured-percent.md`, in `0a4f012`,
  which is not a brief close). It needs Ido's word, or that row releasing.

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

---

# Second revision, 15:05 — the one defect this ticket left open, and half of it was mine

`61-google-calendar` released `feature/dashboard/`, so the item `#66` closed with — *the dashboard
would show a `0 %` ring for an account whose goals all lack a number* — became reachable. Reading it
at `HEAD` showed it was **worse than recorded**, and the extra half is this ticket's own regression.

## What `#66` broke on its way past

`#66` moved `DerivedProgress.overallCompletionOf` to average **measured goals only**. That is right,
and it is exactly the same correction the ticket made to `ProgressSummary.averageProgress`. But on
the summary it also moved the **denominator** — the shared post now says *"across N goals with a
number"* — and on the dashboard it could not, because the screen was another session's file. So
`main` has carried, for eleven hours:

> **Overall progress**
> *Averaged across all your goals*

over a number that is **not** averaged across all your goals. A caption naming a population the
number is not taken over is §0.3's *second number that quietly disagrees* — reintroduced by the fix
that removed it, on the most-visited screen in the app.

**The general shape is worth more than the instance.** A correction that changes what a number
**means** has to move every label that names its population, and those labels are usually in
different files from the arithmetic — so a session that fixes the arithmetic and cannot reach the
label ships a *worse* disagreement than the one it removed. `#66` got this right where the file was
its own and wrong where it was not, in the same commit.

## What shipped

| | |
|---|---|
| `DashboardUiState.measuredGoalCount` | the population the mean is taken over, as a derived accessor — the twin of `ProgressSummary.measuredGoals`, added in the same ticket for the same reason |
| the caption | `Averaged across all your goals` only when the two counts agree; otherwise `Averaged across the N goals that have a number`; and `No goal has a number yet` when none do |
| the ring | replaced by `UnmeasuredMarker` at 56 dp when nothing is measured — a ring reading `0 %` there states an aggregate over goals that were never counting anything, which is this ticket's own defect at the top of the app |

## ⚠️ UNVERIFIED — and this section is the point of the entry

**No test ran on this change, and no build.** `68-drag-to-move` is live, owns `feature/calendar/**`,
declares the **Gradle daemon** as its singleton, and was **actively building** when this was written
— `.gradle/file-system.probe` 41 s old and two JVMs at `+2.2 s` and `+2.3 s` CPU over a 15 s sample.
Its uncommitted calendar work is in this shared tree, so a run here would compile **its** sources and
report about **its** tree, which is `look-at-your-own-output.md` §4p.

**The reachable prefix that was run**, and it is static only:

- `DashboardUiState` is `public` (a JVM test can import it) — checked at the declaration.
- `UnmeasuredMarker(modifier, size)` — the call uses named arguments matching the declared signature.
- `OverviewCard`'s call site and signature agree on all six parameters.
- `feature/dashboard` is **unswept** by `AnalyticsLiteralSweepTest` (`SWEPT_PACKAGES` is
  `feature/analytics` + `ui/components`), so the new plain-English literals are legal; re-checked
  mechanically over both swept packages, which stay clean.
- No dialog was added, so `DialogLocaleGuardTest` is untouched.

**What the first real run will most likely fail on, in order:**

1. **Nothing at all** — this is three literals, one accessor and one `if`. The honest expectation is
   green, which is exactly why it must be said rather than assumed.
2. **The `Row`'s vertical alignment around the marker.** `UnmeasuredMarker` at 56 dp inside 18 dp of
   padding is 92 dp square, chosen to match the ring it replaces; if that arithmetic is off the card
   changes height and the render will show it. **Nothing asserts it**, and only a look would.
3. **The three new JVM cases** in `UnmeasuredPercentTest`, which import `DashboardUiState` from a
   `feature/` package for the first time in that suite.

**Owed:** `:app:testDebugUnitTest` and a look at the dashboard, once `68-drag-to-move` releases the
daemon. Neither is blocked on anything but that row.

