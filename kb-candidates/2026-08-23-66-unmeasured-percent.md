# KB candidates — `66-unmeasured-percent`, 2026-08-23

Session: `#66` — stop the app stating a percentage for a goal that has no measure.
Brief: `sessions/66-unmeasured-percent.md`. Changelog: `CHANGELOG/2026-08-23/66-unmeasured-percent.md`.

---

## 1 · A site list "verified by reading each site" fails in **both** directions, and neither is visible from the list

**Claim.** When a ticket enumerates *every place the app does X*, reading each candidate site is
not enough, and the two failure modes are opposite and independent:

- **Over-reporting** — the site is real and **already guarded upstream**. `#66`'s brief listed
  `BuildWidgetSnapshotUseCase.kt:98` (`percent = progressPercent`) as *half-fixed*. Nine lines
  above, `goals = measured` where `measured = live.filter { it.hasMeasure }`, so that line is
  only ever evaluated for a goal that has a measure. The filter had been there since `b2ba24c`,
  **eight days before the brief was written**. The line is exactly what the brief says it is; the
  function is not.
- **Under-reporting** — the list was enumerated by **surface** (*six screens*) rather than by
  **the derived quantity's consumers**. One `grep 'progressPercent\|progressFraction'` over
  `app/src/main/java` found three more instances, and the one with the largest blast radius was in
  none of the six: `ProgressSummary.averageProgress` is rounded into the **text of a shared post**
  by `SocialRepositoryImpl.shareSummary`, so an unmeasured goal's fictional `0.0` was being
  published to other people. The brief's own priority site — an offline nudge — reaches one user;
  this one leaves the device.

`Observed:` 2026-08-23, `C:\Dev\Android_Final_Project`, `#66`, both in the same session.

**Why.** The two errors have one cause and it is not carelessness: **a site is a property of a
call graph and a list is a property of a screen inventory.** Reading a line tells you what the
line does, never what reaches it; walking screens tells you where a number is *drawn*, never where
it is *computed*. So the cheap discipline is to enumerate from the **quantity** — grep every
consumer of the derived field, then read each one's guards — and treat the surface list as a
cross-check rather than as the source.

**And the over-reporting direction is the expensive one to leave in.** An under-reported site is
found by the next grep. An over-reported one sends the next session to a file that is already
correct, and the natural resolution — *"add the guard the brief says is missing"* — produces a
redundant second filter that then has to be kept in step with the first, which is the class of
defect `#66` exists to remove.

**Destination.** New page, `kb/dev/auditing-a-derived-quantity.md`. Adjacent to but not the same
as `kb/dev/look-at-your-own-output.md` §4 (*a search run at the wrong width passes*): that one is
about the **query**, this one is about the **unit of enumeration**, and the width was correct here
— six sites, all real, all read.

**Anchors.** `sessions/66-unmeasured-percent.md` § *What `/kickoff` found before starting* ·
`app/src/test/java/com/idomarhaim/goalpilot/domain/UnmeasuredPercentTest.kt`
(`the widget snapshot already excludes unmeasured goals and counts them instead` — the assertion
that pins the over-report) · `CHANGELOG/2026-08-23/66-unmeasured-percent.md`.

**Supersedes.** Nothing.

**Status.** ready.

---

## 2 · The bidi-isolate matcher trap has a **silent** direction, and its own remedy has a second trap

**Claim.** `kb/dev/look-at-your-own-output.md` §5.4 records that `bidiIsolated()` defeats an
exact-text Compose matcher — `onNodeWithText("Measure it in kg lost")` finds nothing, because the
node holds `Measure it in ⁨kg lost⁩`. That instance was a **positive** assertion, so it failed
loudly and cost an investigation. **The negative form fails green and costs nothing until it
matters:**

```kotlin
composeRule.onAllNodesWithText("0%").assertCountEquals(0)   // passes either way
```

The isolates defeat the match whether or not the percentage is on screen, so a guard written to
catch a regression **can never fire**. Nothing in the run says so; the test is green on the day it
is written and green on the day the defect comes back.

**And `substring = true` — §5.4's own prescribed remedy — introduces a second trap in the negative
direction only.** `"40%"` **contains** `"0%"`, so a substring negative for the unmeasured row's
`0%` matches the *measured control* rendered beside it and fails for a reason that has nothing to
do with the defect. Positive assertions never hit this, because containment is what they want.

**Two remedies, and the second is the one worth carrying:**

1. Choose control values that share no digits with the probe (`55%`, not `40%`). Necessary, and
   fragile — it is a fact about two literals that no reader will re-derive.
2. **Assert a whole-tree count of the invariant token instead of a per-row absence.** Render the
   unmeasured row and a measured control in one tree and assert
   `onAllNodesWithText("%", substring = true).assertCountEquals(1)`. This cannot pass vacuously in
   *either* direction: two nodes means the fix did not take, zero means it over-reached and stripped
   the control, and a broken matcher gives zero rather than a false pass.

`Observed:` 2026-08-23 while writing `UnmeasuredPercentRenderTest` for `#66`. **`Untested:` on a
device** — the Gradle daemon and the AVD were held by two live sibling rows for the whole session,
so this was caught by recomputing the matcher's input by hand rather than by watching it fail.
That is the weaker evidence and is why it is said here rather than asserted.

**Why.** The general shape is bigger than bidi: **a guard whose matcher cannot match reports the
same result as a guard whose subject is absent.** Any assertion of the form *"this is not present"*
inherits the reliability of its matcher and reports nothing about it, so a negative assertion is
only evidence when the identical matcher is shown to succeed on a control **in the same tree**.
Every §5-family trap that produces a byte-different, pixel-identical string — directional isolates,
zero-width joiners, non-breaking spaces from a formatter, locale digit sets — reaches the negative
form this way.

*Rejected:* spelling `\u2068`/`\u2069` into the expected string, for the reason §5.4 already gives.

**Destination.** `kb/dev/look-at-your-own-output.md` §5.4 — extended in place with the negative
direction, the containment trap in the remedy, and the paired-control rule.

**Anchors.** `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/UnmeasuredPercentRenderTest.kt`
(`anUnmeasuredRowShowsNoPercentageWhileAMeasuredOneStillDoes`, and the KDoc above it) ·
`kb/dev/look-at-your-own-output.md` §5.4 · `kb-candidates/2026-08-23-65-measure-proposal.md`
(where §5.4 was flagged, one ticket earlier, in the same repo).

**Supersedes.** Nothing. It **extends** §5.4 and contradicts none of it — the positive-direction
account there stays exactly as written.

**Status.** ready.

---

## 3 · A test fixture that predates a schema change stays green **on the default the change deleted**

**Claim.** GoalPilot's `Goal.measure` replaced a `unit: String = "%"` default; §1.3 made **absence**
the default instead (`E6`). Every test fixture written before that — `Goal(currentValue = 50.0,
targetValue = 100.0)` — meant *half done*, and after the change means *a goal counting nothing
whose 100.0 target nobody set*. **Not one of them failed.** They kept computing the same numbers,
because the arithmetic never stopped working; only what the numbers *meant* changed.

The change surfaced when `#66` filtered an aggregation on the new field: **eleven assertions across
three suites went red at once**, in files with no obvious relation to the ticket
(`DerivedProgressTest`, `BuildSummaryUseCaseTest`, `RecommendationRepositoryFallbackTest`).

`Observed:` 2026-08-23, `#66`.

**Why, and the reading that will be wrong.** A wave of unrelated red reads as *my change is too
aggressive* — and here it was the opposite: those tests had been asserting the deleted semantics
for eight days. The tell that discriminates the two: **the failures are all in fixtures rather than
in assertions.** A too-aggressive change breaks the thing being asserted; a stale fixture breaks
the *setup*, and the fix is to write down what the fixture always meant (`measure = Measure(COUNT,
"km")`) rather than to weaken the new rule.

The deeper claim: **a schema change that widens a type's legal states leaves every existing fixture
sitting in a state that is now legal and means something else.** Nothing fails, so nothing prompts
the review, and the fixtures then read as evidence for behaviour they no longer describe. A rename
is caught by the compiler; this is not, because the old construction still compiles and still
passes.

**Destination.** New page, `kb/dev/fixtures-encode-the-old-default.md`. Adjacent to
`kb/dev/auditing-a-derived-quantity.md` (candidate 1) — that one is about finding every *production*
site of a changed quantity, this one about every *test* site, and the discovery mechanisms are
opposite: production sites are found by grep before the change, fixture sites announce themselves
as red after it.

**Anchors.** `app/src/test/java/com/idomarhaim/goalpilot/progress/DerivedProgressTest.kt`
(the `measured()` helper and its KDoc) ·
`app/src/test/java/com/idomarhaim/goalpilot/domain/BuildSummaryUseCaseTest.kt` ·
`app/src/test/java/com/idomarhaim/goalpilot/data/RecommendationRepositoryFallbackTest.kt` ·
`CHANGELOG/2026-08-23/66-unmeasured-percent.md`.

**Supersedes.** Nothing.

**Status.** ready.
