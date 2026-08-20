package com.idomarhaim.goalpilot.progress

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.DerivedProgress
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.withDerivedProgress
import org.junit.Test

/**
 * The arithmetic that replaces the stored `goal.currentValue`
 * ([#49](https://github.com/idomarhaim/Android_Final_Project/issues/49), spec §4.6 / §5.2 / §1.5).
 *
 * These are the `facts → expected numbers` cases §5.2 asks for. They are pure —
 * no Firebase, no coroutines — because the whole point of the change is that the
 * number is a function of two lists and nothing else. A test that needed a
 * transaction to check it would be evidence the counter had not actually gone.
 */
class DerivedProgressTest {

    private fun entry(goalId: String, value: Double, id: String = "") =
        ProgressEntry(id = id, goalId = goalId, value = value)

    private fun task(goalId: String?, contribution: Double, done: Boolean, id: String = "") =
        Task(id = id, goalId = goalId, progressContribution = contribution, isDone = done)

    // ── The sum itself ────────────────────────────────────────────────────────

    @Test
    fun `a goal with no facts is zero, which is what an untouched document reads`() {
        assertThat(DerivedProgress.currentValueOf("g1", emptyList(), emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `entries sum`() {
        val entries = listOf(entry("g1", 3.0), entry("g1", 4.5), entry("g1", 0.5))
        assertThat(DerivedProgress.currentValueOf("g1", entries, emptyList()))
            .isWithin(1e-9).of(8.0)
    }

    @Test
    fun `completed tasks contribute and open ones do not`() {
        val tasks = listOf(
            task("g1", 10.0, done = true),
            task("g1", 7.0, done = false),
            task("g1", 2.0, done = true),
        )
        assertThat(DerivedProgress.currentValueOf("g1", emptyList(), tasks))
            .isWithin(1e-9).of(12.0)
    }

    @Test
    fun `entries and completed tasks add to one number`() {
        val entries = listOf(entry("g1", 5.0))
        val tasks = listOf(task("g1", 3.0, done = true))
        assertThat(DerivedProgress.currentValueOf("g1", entries, tasks))
            .isWithin(1e-9).of(8.0)
    }

    @Test
    fun `facts belonging to another goal are not counted`() {
        val entries = listOf(entry("g1", 5.0), entry("g2", 100.0))
        val tasks = listOf(task("g2", 50.0, done = true))
        assertThat(DerivedProgress.currentValueOf("g1", entries, tasks))
            .isWithin(1e-9).of(5.0)
    }

    @Test
    fun `an unlinked task belongs to no goal`() {
        val tasks = listOf(task(null, 9.0, done = true), task("", 9.0, done = true))
        assertThat(DerivedProgress.currentValues(emptyList(), tasks)).isEmpty()
    }

    @Test
    fun `a negative entry pulls the number back down`() {
        // §1.5: "progress can fall, which today it structurally cannot". Correcting
        // an over-log is logging beside it, so the sum has to be able to shrink.
        val entries = listOf(entry("g1", 10.0), entry("g1", -4.0))
        assertThat(DerivedProgress.currentValueOf("g1", entries, emptyList()))
            .isWithin(1e-9).of(6.0)
    }

    @Test
    fun `the sum is not clamped at the target, because overshoot is legal`() {
        // §1.5: past the target the app stops speaking in percent and says
        // "beat it by …". A clamp here would put that out of reach for everyone.
        val entries = listOf(entry("g1", 150.0))
        val goal = Goal(id = "g1", targetValue = 100.0).withDerivedProgress(entries, emptyList())
        assertThat(goal.currentValue).isWithin(1e-9).of(150.0)
    }

    @Test
    fun `the sum is not clamped at zero either`() {
        val entries = listOf(entry("g1", -5.0))
        assertThat(DerivedProgress.currentValueOf("g1", entries, emptyList()))
            .isWithin(1e-9).of(-5.0)
    }

    // ── The property that makes the stored counter unnecessary ────────────────

    @Test
    fun `deriving over an already-derived goal does not double-credit`() {
        // Idempotence is why §5.2 chose "project from facts" over "recompute and
        // store". Stated as `f(f(x)) == f(x)` over a *goal*, not as
        // `f(x) == f(x)` over the raw facts — the latter cannot fail for any
        // implementation and so tests nothing. This one fails the moment the seam
        // adds to `currentValue` instead of replacing it, which is exactly the
        // accumulator bug being deleted.
        val entries = listOf(entry("g1", 7.0))
        val tasks = listOf(task("g1", 3.0, done = true))
        val once = Goal(id = "g1").withDerivedProgress(entries, tasks)
        val twice = once.withDerivedProgress(entries, tasks)

        assertThat(once.currentValue).isWithin(1e-9).of(10.0)
        assertThat(twice.currentValue).isEqualTo(once.currentValue)
    }

    @Test
    fun `the sum does not depend on the order the facts arrive in`() {
        // The per-goal listeners in `GoalRepositoryImpl` are flattened in whatever
        // order `combine` emits them, so order-independence is a property the
        // repository relies on rather than a nicety.
        val entries = listOf(entry("g1", 1.0), entry("g2", 9.0), entry("g1", 2.0))
        val tasks = listOf(task("g1", 4.0, done = true))
        assertThat(DerivedProgress.currentValueOf("g1", entries.reversed(), tasks))
            .isEqualTo(DerivedProgress.currentValueOf("g1", entries, tasks))
    }

    @Test
    fun `un-ticking a task removes exactly what ticking it added`() {
        // The old accumulator lost 30 for a 10 whenever the stored points changed
        // between tick and untick (§7.2, TaskRepositoryImpl:120-127). A sum cannot
        // drift, because it never remembers what it added last time.
        val open = listOf(task("g1", 10.0, done = false, id = "t1"))
        val done = listOf(task("g1", 10.0, done = true, id = "t1"))
        assertThat(DerivedProgress.currentValueOf("g1", emptyList(), done)).isWithin(1e-9).of(10.0)
        assertThat(DerivedProgress.currentValueOf("g1", emptyList(), open)).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `an entry recorded with no counter update is already counted`() {
        // This is #49's crash window, expressed as a test: the entry is the record,
        // so there is no second step whose absence could make the goal read low.
        val entries = listOf(entry("g1", 20.0, id = "written-then-crashed"))
        assertThat(DerivedProgress.currentValueOf("g1", entries, emptyList()))
            .isWithin(1e-9).of(20.0)
    }

    // ── The batch form the goal list uses ─────────────────────────────────────

    @Test
    fun `currentValues keys every goal that has a fact and no others`() {
        val entries = listOf(entry("g1", 1.0), entry("g3", 2.0))
        val tasks = listOf(task("g2", 3.0, done = true), task("g4", 4.0, done = false))
        assertThat(DerivedProgress.currentValues(entries, tasks).keys)
            .containsExactly("g1", "g2", "g3")
    }

    @Test
    fun `withDerivedProgress overwrites whatever the document happened to store`() {
        // The stored field is not read any more. A document carrying a corrupted
        // 999.0 from the old two-step write reads as the sum of its facts instead,
        // which is why #49 needs no backfill.
        val goals = listOf(
            Goal(id = "g1", currentValue = 999.0),
            Goal(id = "g2", currentValue = 42.0),
        )
        val derived = goals.withDerivedProgress(
            entries = listOf(entry("g1", 6.0)),
            tasks = emptyList(),
        )
        assertThat(derived.single { it.id == "g1" }.currentValue).isWithin(1e-9).of(6.0)
        assertThat(derived.single { it.id == "g2" }.currentValue).isEqualTo(0.0)
    }

    @Test
    fun `withDerivedProgress leaves every other field alone`() {
        val goal = Goal(id = "g1", title = "Read 12 books", targetValue = 12.0, measure = Measure(MeasureKind.COUNT, "books"))
        val derived = goal.withDerivedProgress(listOf(entry("g1", 3.0)), emptyList())
        assertThat(derived).isEqualTo(goal.copy(currentValue = 3.0))
    }

    // ── The aggregation site, and the 16259% it produced ──────────────────────

    @Test
    fun `overall completion is bounded above however far one goal has run past its target`() {
        // The device pass read "Overall progress 16259%". A plain mean of
        // progressFraction can say that; this cannot say more than 100%.
        val beaten = Goal(id = "steps", targetValue = 70_000.0, currentValue = 34_000_000.0)
        val untouched = Goal(id = "b", targetValue = 100.0)
        assertThat(DerivedProgress.overallCompletionOf(listOf(beaten, untouched)))
            .isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun `overall completion is bounded below by a goal whose progress has gone negative`() {
        val negative = Goal(id = "a", targetValue = 100.0, currentValue = -500.0)
        val half = Goal(id = "b", targetValue = 100.0, currentValue = 50.0)
        assertThat(DerivedProgress.overallCompletionOf(listOf(negative, half)))
            .isWithin(1e-6f).of(0.25f)
    }

    @Test
    fun `an ordinary set of goals averages exactly as it always did`() {
        // The clamp must be invisible where nothing overshoots — otherwise this is
        // a behaviour change dressed as a bug fix.
        val goals = listOf(
            Goal(id = "a", targetValue = 100.0, currentValue = 20.0),
            Goal(id = "b", targetValue = 100.0, currentValue = 40.0),
            Goal(id = "c", targetValue = 100.0, currentValue = 90.0),
        )
        assertThat(DerivedProgress.overallCompletionOf(goals)).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun `no goals is zero rather than a division by zero`() {
        assertThat(DerivedProgress.overallCompletionOf(emptyList())).isEqualTo(0f)
        assertThat(DerivedProgress.overallCompletion(emptyList())).isEqualTo(0f)
    }

    @Test
    fun `clamping the aggregate does not clamp the goal`() {
        // §1.5's whole point: the overshoot stays readable where the goal speaks
        // for itself. This is the test that fails if someone "fixes" 16259% by
        // putting the clamp back on progressFraction.
        val beaten = Goal(id = "steps", targetValue = 70_000.0, currentValue = 210_000.0)
        assertThat(beaten.progressFraction).isWithin(1e-6f).of(3f)
        assertThat(beaten.progressPercent).isEqualTo(300)
        assertThat(DerivedProgress.overallCompletionOf(listOf(beaten))).isEqualTo(1f)
    }

    @Test
    fun `a periodic target fed by a daily sync is what produced the number`() {
        // Not a fix, a witness. The Health Connect sync writes one entry per day
        // against a WEEKLY target (70_000 steps), and DerivedProgress sums every
        // entry there has ever been — so the fraction grows without bound at about
        // one target per week. The old stored counter hid this by clamping at the
        // target, which is why it surfaced only when #49 removed the clamp.
        val ninetyDays = (1..90).map { day -> entry("steps", 8_000.0, id = "hc:steps:$day") }
        val goal = Goal(id = "steps", targetValue = 70_000.0)
            .withDerivedProgress(ninetyDays, emptyList())

        assertThat(goal.currentValue).isWithin(1e-6).of(720_000.0)
        assertThat(goal.progressPercent).isEqualTo(1028)
        // …and the headline stays sane regardless.
        assertThat(DerivedProgress.overallCompletionOf(listOf(goal))).isEqualTo(1f)
    }
}
