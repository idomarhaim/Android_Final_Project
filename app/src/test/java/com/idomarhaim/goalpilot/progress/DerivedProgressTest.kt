package com.idomarhaim.goalpilot.progress

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.DerivedProgress
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
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

    /**
     * A task with one edge, declaring [contribution] (§1.5, `#55`).
     *
     * `contribution` is nullable here because *undeclared* is now a state the arithmetic has
     * to have an answer for, and it is not `0.0`: `0.0` says this work is worth nothing,
     * `null` says nobody said. See `an edge that declares nothing adds nothing` below.
     */
    private fun task(
        goalId: String?,
        contribution: Double?,
        done: Boolean,
        id: String = "",
    ) = Task(
        id = id,
        goalEdges = goalEdgesOf(goalId, contribution),
        completion = if (done) CompletionFact() else null,
    )

    /** A task serving more than one objective, each edge with its own worth (§1.5). */
    private fun multiEdgeTask(vararg edges: Pair<String, Double?>, done: Boolean, id: String = "") =
        Task(
            id = id,
            goalEdges = edges.map { (goalId, contribution) -> GoalEdge(goalId, contribution) },
            completion = if (done) CompletionFact() else null,
        )

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

    // ── §1.5's edges, and the silence that used to be a 1.0 ───────────────────

    @Test
    fun `an edge that declares nothing adds nothing`() {
        // §1.5: "an edge declares its contribution in the objective's own word, or
        // contributes nothing to the measure". `progressContribution`'s 1.0 default was a
        // SILENCE, not a value — nobody was ever asked what a task is worth to a goal, and
        // the app answered "one" on their behalf, in whatever unit the goal happened to use.
        val tasks = listOf(task("g1", contribution = null, done = true))
        assertThat(DerivedProgress.currentValues(emptyList(), tasks)).isEmpty()
    }

    @Test
    fun `zero is a declaration and is not the same as saying nothing`() {
        // Both sum to 0.0, and they are still different: a declared 0.0 makes the goal
        // PRESENT in the map at zero, which is what "this work does not move that measure"
        // looks like. Silence leaves it absent — the state `currentValues` documents as
        // "no facts". One meaning, one representation, in both directions.
        val declared = listOf(task("g1", contribution = 0.0, done = true))
        val silent = listOf(task("g1", contribution = null, done = true))

        assertThat(DerivedProgress.currentValues(emptyList(), declared)).containsKey("g1")
        assertThat(DerivedProgress.currentValues(emptyList(), silent)).doesNotContainKey("g1")
    }

    @Test
    fun `every edge advances its own objective fully, and they are not divided`() {
        // §1.5's many-to-many table: goal progress is OWNED by each objective, so it
        // duplicates across edges — unlike minutes, which are pooled and get divided,
        // because one afternoon happened once. Dividing progress would make a goal's number
        // depend on how many OTHER goals the same task happens to serve.
        val tasks = listOf(multiEdgeTask("g1" to 1.0, "g2" to 5.0, done = true))
        val sums = DerivedProgress.currentValues(emptyList(), tasks)

        assertThat(sums["g1"]).isWithin(1e-9).of(1.0)
        assertThat(sums["g2"]).isWithin(1e-9).of(5.0)
    }

    @Test
    fun `one task can declare to one objective and stay silent to another`() {
        // The shape that made a per-TASK number wrong in the first place: a 30-minute run is
        // "1" to "run 20 times" and has no expressible worth at all to "lose 5 kg". The old
        // field forced one answer to serve both.
        val tasks = listOf(multiEdgeTask("runs" to 1.0, "weight" to null, done = true))
        val sums = DerivedProgress.currentValues(emptyList(), tasks)

        assertThat(sums["runs"]).isWithin(1e-9).of(1.0)
        assertThat(sums).doesNotContainKey("weight")
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
        // `loggedEntryCount` joined `currentValue` at this seam in `#66`: two derived
        // views of the same facts, filled in the same pass. Naming it here rather than
        // relaxing the assertion is the point of the test — it fails the moment a
        // THIRD field starts being derived silently.
        assertThat(derived).isEqualTo(goal.copy(currentValue = 3.0, loggedEntryCount = 1))
    }

    // ── The aggregation site, and the 16259% it produced ──────────────────────

    /**
     * A goal that is genuinely counting something, for the aggregation tests below.
     *
     * ⚠️ **They did not need this before `#66`, and the reason is worth reading.**
     * These fixtures were written when a bare `Goal(targetValue = 100.0,
     * currentValue = 50.0)` meant *half done*, which was true while every goal
     * carried a `"%"` unit by default. §1.3 deleted that default — absence is now
     * the default (`E6`) — so the same construction now means *a goal that counts
     * nothing, whose 100.0 target nobody set*, and `#66` makes
     * [DerivedProgress.overallCompletionOf] skip exactly those.
     *
     * So the measure is not a concession to a stricter rule: it writes down what the
     * fixture always meant. What these tests are **about** — the clamp, and the
     * 16259% that motivated it — is untouched, and every expected number below is
     * the number it was before.
     */
    private fun measured(id: String, target: Double, current: Double = 0.0) = Goal(
        id = id,
        targetValue = target,
        currentValue = current,
        measure = Measure(MeasureKind.COUNT, "reps"),
    )

    @Test
    fun `overall completion is bounded above however far one goal has run past its target`() {
        // The device pass read "Overall progress 16259%". A plain mean of
        // progressFraction can say that; this cannot say more than 100%.
        val beaten = measured("steps", target = 70_000.0, current = 34_000_000.0)
        val untouched = measured("b", target = 100.0)
        assertThat(DerivedProgress.overallCompletionOf(listOf(beaten, untouched)))
            .isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun `overall completion is bounded below by a goal whose progress has gone negative`() {
        val negative = measured("a", target = 100.0, current = -500.0)
        val half = measured("b", target = 100.0, current = 50.0)
        assertThat(DerivedProgress.overallCompletionOf(listOf(negative, half)))
            .isWithin(1e-6f).of(0.25f)
    }

    @Test
    fun `an ordinary set of goals averages exactly as it always did`() {
        // The clamp must be invisible where nothing overshoots — otherwise this is
        // a behaviour change dressed as a bug fix.
        val goals = listOf(
            measured("a", target = 100.0, current = 20.0),
            measured("b", target = 100.0, current = 40.0),
            measured("c", target = 100.0, current = 90.0),
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
        val beaten = measured("steps", target = 70_000.0, current = 210_000.0)
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
        val goal = measured("steps", target = 70_000.0)
            .withDerivedProgress(ninetyDays, emptyList())

        assertThat(goal.currentValue).isWithin(1e-6).of(720_000.0)
        assertThat(goal.progressPercent).isEqualTo(1028)
        // …and the headline stays sane regardless.
        assertThat(DerivedProgress.overallCompletionOf(listOf(goal))).isEqualTo(1f)
    }
}
