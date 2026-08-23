package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.DerivedProgress
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.GoalProgress
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.model.withDerivedProgress
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetSnapshotUseCase
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocation
import com.idomarhaim.goalpilot.domain.usecase.TimeTrend
import org.junit.Test

/**
 * `#66` — **an unmeasured goal states no number**, at every site that computes one.
 *
 * ## What this suite is about
 *
 * §1.3 makes **absence the default** (`E6`): a goal that says nothing about what it
 * counts carries `measure == null`, and `"%"` survives only as a *chosen* `PERCENT`
 * measure. But `Goal.targetValue` still defaults to `100.0`, so `progressFraction`
 * — and everything derived from it — happily produces a number for a goal that has
 * none. `#65`'s render pass caught the visible half: the new dashed-square marker
 * meaning *no number yet* sat on the same row as `0%` and `0/100`, both computed
 * against a target nobody set.
 *
 * The digit was never the whole of it. What is asserted here is the **predicate**,
 * at each site that had to grow one, because that is the half a screenshot cannot
 * check and the half that will rot first.
 *
 * ## Why the JVM layer is where the value is
 *
 * Four of the sites are pure functions with no Android in them at all —
 * `DerivedProgress.overallCompletionOf`, `ProgressSummary.averageProgress`, the
 * widget snapshot, and the nudge filter — and one of those, per the brief, is the
 * priority of the whole ticket: an unmeasured goal sits at **exactly `0.0`**, which
 * is below every measured goal that has moved, so a `< 0.34f` filter did not merely
 * *include* unmeasured goals, it **preferred** them. Its repository-level test lives
 * with the rest of the offline-fallback suite in
 * `data/RecommendationRepositoryFallbackTest`, where the wiring already exists; the
 * arithmetic that makes it happen is asserted here, on nothing but a `Goal`.
 *
 * ## The site this suite deliberately asserts is ALREADY CORRECT
 *
 * The brief lists `BuildWidgetSnapshotUseCase` as *half-fixed*. It is not: the use
 * case filters to `hasMeasure` before it builds a single `WidgetGoal`, and has since
 * `b2ba24c` (2026-08-15). That is asserted below rather than merely noted, because a
 * claim that a site is fine is exactly the kind that decays silently — and because
 * the next person reading `#66` will read the same line of the brief and go looking.
 */
class UnmeasuredPercentTest {

    private fun measured(id: String, current: Double, target: Double = 100.0) = Goal(
        id = id,
        title = id,
        currentValue = current,
        targetValue = target,
        measure = Measure(MeasureKind.COUNT, "reps"),
    )

    /** The default a goal gets for saying nothing (§1.3, `E6`). */
    private fun unmeasured(id: String, current: Double = 0.0) = Goal(
        id = id,
        title = id,
        currentValue = current,
    )

    private fun entry(goalId: String, id: String, value: Double = 1.0) =
        ProgressEntry(id = id, goalId = goalId, value = value)

    private fun slice(
        id: String,
        fraction: Float,
        unmeasured: Boolean = false,
        minutes: Int = 0,
    ) = GoalProgress(
        goalId = id,
        title = id,
        category = GoalCategory.OTHER,
        fraction = fraction,
        effortMinutes = minutes,
        isUnmeasured = unmeasured,
    )

    // ------------------------------------------------------- the predicate

    @Test
    fun `a goal with no measure is unmeasured, and one with a measure is not`() {
        assertThat(unmeasured("a").isUnmeasured).isTrue()
        assertThat(measured("b", 10.0).isUnmeasured).isFalse()
    }

    @Test
    fun `the predicate is measure absence and deliberately NOT hasMeasure`() {
        // The two differ on a goal that carries a measure and a zero target, and
        // the difference is deliberate: `isUnmeasured` is the population §1.3
        // names and the population `UnmeasuredMarkerIfNeeded` already uses, so
        // the marker and the missing percentage make ONE statement rather than
        // two that can drift apart. This is what fails if someone "simplifies"
        // one predicate into the other.
        val wordButNoTarget = Goal(
            id = "g",
            targetValue = 0.0,
            measure = Measure(MeasureKind.COUNT, "books"),
        )
        assertThat(wordButNoTarget.hasMeasure).isFalse()
        assertThat(wordButNoTarget.isUnmeasured).isFalse()
    }

    @Test
    fun `a chosen PERCENT measure is measured, because choosing it is the point`() {
        // §7.1 keeps *chosen* and *defaulted* apart, and that is the whole reason
        // the field is nullable rather than defaulting to PERCENT. A goal Ido
        // deliberately measures in percent keeps its percentage everywhere.
        val chosen = Goal(
            id = "g",
            currentValue = 45.0,
            measure = Measure(MeasureKind.PERCENT, "%"),
        )
        assertThat(chosen.isUnmeasured).isFalse()
        assertThat(chosen.progressPercent).isEqualTo(45)
    }

    @Test
    fun `the fiction this ticket removes is real arithmetic, not a rendering choice`() {
        // Not a fix — a witness, and the one that explains why every branch below
        // exists. #11's own issue body opens with a live goal reading
        // `Health · 1/100 %`, and this is that goal.
        val logged = unmeasured("water").withDerivedProgress(
            listOf(entry("water", "e1", value = 1.0)),
            emptyList(),
        )
        assertThat(logged.currentValue).isWithin(1e-9).of(1.0)
        // Nobody set this…
        assertThat(logged.targetValue).isWithin(1e-9).of(100.0)
        // …so this is a fraction of a fiction.
        assertThat(logged.progressPercent).isEqualTo(1)
    }

    @Test
    fun `a chosen PERCENT measure restates its number, and nothing else does`() {
        // The eighth site, and it is the ticket's own sentence one step further:
        // "the reasoning is settled; it was applied at one site and not the
        // others." The tile has dropped this label since `#11`; the three
        // surfaces that draw a goal ROW had not, and `#66`'s render pass showed
        // `45%` beside `Other · 45/100 %` with every assertion green.
        assertThat(Goal(measure = Measure(MeasureKind.PERCENT, "%")).restatesPercent).isTrue()
        assertThat(measured("m", 10.0).restatesPercent).isFalse()
        assertThat(unmeasured("u").restatesPercent).isFalse()
    }

    @Test
    fun `the restated pair can actually disagree, which is why the LABEL is what goes`() {
        // Not a tidiness argument. `progressPercent` is `current / target`, so a
        // PERCENT goal whose target is not 100 renders one number beside a label
        // holding a different one. Dropping the label leaves the number the
        // goal's own arithmetic produced; reformatting the label would leave two.
        val odd = Goal(
            id = "g",
            currentValue = 45.0,
            targetValue = 50.0,
            measure = Measure(MeasureKind.PERCENT, "%"),
        )
        assertThat(odd.progressPercent).isEqualTo(90)          // what the row shows
        assertThat(odd.currentValue).isWithin(1e-9).of(45.0)   // what the label showed
        assertThat(odd.restatesPercent).isTrue()
    }

    // ------------------------------- the honest replacement: a count, not a %

    @Test
    fun `the logged entry count comes through the same seam as currentValue`() {
        val goals = listOf(unmeasured("a"), unmeasured("b")).withDerivedProgress(
            listOf(
                entry("a", "e1"),
                entry("a", "e2"),
                entry("a", "e3"),
                entry("b", "e4"),
            ),
            emptyList(),
        )
        assertThat(goals.first { it.id == "a" }.loggedEntryCount).isEqualTo(3)
        assertThat(goals.first { it.id == "b" }.loggedEntryCount).isEqualTo(1)
    }

    @Test
    fun `a goal nothing has been logged against counts zero rather than being absent`() {
        val goal = unmeasured("a").withDerivedProgress(emptyList(), emptyList())
        assertThat(goal.loggedEntryCount).isEqualTo(0)
    }

    @Test
    fun `the count is entries and is NOT the number currentValue sums`() {
        // The point of counting entries rather than reusing `currentValue`: the
        // count names a *Progress log* the reader can go and count, so three logs
        // of 25 each read "3 entries logged" and not "75". Two different
        // questions, and the row asks the countable one.
        val goal = unmeasured("a").withDerivedProgress(
            listOf(
                entry("a", "e1", value = 25.0),
                entry("a", "e2", value = 25.0),
                entry("a", "e3", value = 25.0),
            ),
            emptyList(),
        )
        assertThat(goal.currentValue).isWithin(1e-9).of(75.0)
        assertThat(goal.loggedEntryCount).isEqualTo(3)
    }

    @Test
    fun `a completed task contributing to a goal is not an entry and is not counted`() {
        // §1.5 lets a task declare a contribution, and `currentValue` takes it.
        // The count must not, or the number stops matching the Progress log it
        // is named after.
        val task = Task(
            id = "t1",
            title = "t1",
            goalEdges = listOf(GoalEdge(goalId = "a", contribution = 5.0)),
            completion = CompletionFact(),
        )
        val goal = unmeasured("a").withDerivedProgress(listOf(entry("a", "e1")), listOf(task))
        assertThat(goal.currentValue).isWithin(1e-9).of(6.0)
        assertThat(goal.loggedEntryCount).isEqualTo(1)
    }

    @Test
    fun `an entry with a blank goal id is counted against nobody`() {
        val counts = DerivedProgress.entryCounts(listOf(entry("", "e1"), entry("a", "e2")))
        assertThat(counts).containsExactly("a", 1)
    }

    // ------------------------------------------------ the dashboard headline

    @Test
    fun `overall completion averages only the goals that have a number`() {
        // Three unmeasured goals beside one finished goal used to read 25%, which
        // states that three quarters of the work is outstanding on goals that
        // were never counting anything.
        val goals = listOf(
            measured("done", 100.0),
            unmeasured("x"),
            unmeasured("y"),
            unmeasured("z"),
        )
        assertThat(DerivedProgress.overallCompletionOf(goals)).isWithin(1e-6f).of(1f)
    }

    @Test
    fun `an account whose goals all lack a number reads zero, not a division by zero`() {
        assertThat(DerivedProgress.overallCompletionOf(listOf(unmeasured("x"), unmeasured("y"))))
            .isEqualTo(0f)
    }

    @Test
    fun `the exclusion is invisible where every goal has a measure`() {
        // Otherwise this is a behaviour change dressed as a bug fix — the same
        // guard `DerivedProgressTest` keeps over the clamp, for the same reason.
        val goals = listOf(measured("a", 20.0), measured("b", 40.0), measured("c", 90.0))
        assertThat(DerivedProgress.overallCompletionOf(goals)).isWithin(1e-6f).of(0.5f)
    }

    // -------------------------------------------- the number that is published

    @Test
    fun `a shared summary averages only measured goals and says how many they were`() {
        // The highest-consequence site in the ticket: `SocialRepositoryImpl`
        // rounds `averageProgress` into the TEXT OF A SHARED POST. An unmeasured
        // goal at 0.0 does not merely mislead Ido — it is published to other
        // people under his name.
        val summary = ProgressSummary(
            goals = listOf(slice("a", 0.8f), slice("b", 0f, unmeasured = true)),
        )
        assertThat(summary.averageProgress).isWithin(1e-6f).of(0.8f)
        assertThat(summary.measuredGoals).isEqualTo(1)
    }

    @Test
    fun `an unmeasured goal keeps its effort minutes though it loses its fraction`() {
        // §1.4: effort and outcome are two quantities. Dropping the whole slice
        // to remove a number that was never there would have deleted hours of
        // real logged work.
        val summary = ProgressSummary(
            goals = listOf(slice("a", 0f, unmeasured = true, minutes = 260)),
        )
        assertThat(summary.goals.single().effortMinutes).isEqualTo(260)
        assertThat(summary.averageProgress).isEqualTo(0f)
        assertThat(summary.measuredGoals).isEqualTo(0)
    }

    // --------------------------- the nudge filter, which PREFERRED these goals

    @Test
    fun `an unmeasured goal sorts below every measured goal the nudge filter reads`() {
        // The arithmetic behind the brief's priority site, asserted on nothing but
        // a Goal: `< 0.34f` is not a neutral threshold when one population is
        // pinned at exactly 0.0 by a target nobody set.
        val untouched = unmeasured("fresh")
        assertThat(untouched.progressFraction).isEqualTo(0f)

        val candidates = listOf(measured("m1", 10.0), measured("m2", 20.0), untouched)
        val before = candidates.filter { it.progressFraction < 0.34f }
        val after = candidates.filter { !it.isUnmeasured && it.progressFraction < 0.34f }

        assertThat(before.map { it.id }).containsExactly("m1", "m2", "fresh")
        assertThat(after.map { it.id }).containsExactly("m1", "m2")
    }

    // ------------------------------------------------ the site the brief got wrong

    @Test
    fun `the widget snapshot already excludes unmeasured goals and counts them instead`() {
        val snapshot = BuildWidgetSnapshotUseCase()(
            capturedAtEpochMillis = 0L,
            user = User(uid = "u"),
            goals = listOf(measured("m", 50.0), unmeasured("x"), unmeasured("y")),
            allocation = TimeAllocation(),
            trend = TimeTrend(),
        )
        assertThat(snapshot.goals.map { it.id }).containsExactly("m")
        assertThat(snapshot.goalsWithoutMeasure).isEqualTo(2)
    }
}
