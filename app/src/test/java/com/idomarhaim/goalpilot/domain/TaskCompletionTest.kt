package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskCompletion
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocationUseCase
import org.junit.Test

/**
 * `#7`'s invariant — a completion is **one fact**, and `isDone` without
 * `completedAtEpochMillis` is not half of it but a different, worse thing.
 *
 * The rule exists because `#7` is the first ticket in which a task can be **created done**.
 * Before it, the only writer of `done` was `setDone`, which has always written both fields in
 * one `update` and nulled the stamp on an untick — so the invariant held by having exactly one
 * writer, and nothing stated it. A second writer that did not know the rule would produce a
 * task that **awards points and is invisible in every place the user could go and check**:
 * `pointsFromTasks` counts `done`, while the weekly summary, the dashboard's done-this-week
 * count and the time chart all require the stamp.
 *
 * That is why the last cases here assert against the **real consumers** rather than only
 * against the normaliser. A test that only checked `stamp` would be checking that a function
 * does what its own branches say; these check that what it produces is what the readers
 * downstream actually need, which is the claim the ticket is making.
 */
class TaskCompletionTest {

    private val now = 1_755_000_000_000L

    private fun task(
        done: Boolean = false,
        completedAt: Long? = null,
        minutes: Int? = 45,
    ) = Task(
        id = "t-1",
        goalId = "g-1",
        title = "Run 5 km",
        points = 15,
        isDone = done,
        estimatedMinutes = minutes,
        durationSource = DurationSource.USER,
        createdAtEpochMillis = now - 10_000L,
        completedAtEpochMillis = completedAt,
    )

    // ── The three cases ──────────────────────────────────────────────

    @Test
    fun `a task created done gets its stamp from the write that created it`() {
        // `#7` itself: the quick-add "Already done" chip produces exactly this input.
        val stamped = TaskCompletion.stamp(task(done = true), now)

        assertThat(stamped.completedAtEpochMillis).isEqualTo(now)
        assertThat(stamped.isDone).isTrue()
    }

    @Test
    fun `a completion that already happened is never re-dated`() {
        // AnalyticsViewModel's duration backfill re-saves completed tasks routinely. Moving
        // the timestamp would rewrite history in the time chart every time somebody corrected
        // a duration — the task would hop into whichever week the correction was made.
        val yesterday = now - 86_400_000L
        val stamped = TaskCompletion.stamp(task(done = true, completedAt = yesterday), now)

        assertThat(stamped.completedAtEpochMillis).isEqualTo(yesterday)
    }

    @Test
    fun `a task that is not done carries no completion time, even if one was handed in`() {
        // The invariant in the other direction. `setDone(false)` has always nulled the stamp,
        // so after this the create path and the tick path produce the same shape — which is
        // what makes the four readers safe to trust.
        val stamped = TaskCompletion.stamp(task(done = false, completedAt = now - 5_000L), now)

        assertThat(stamped.completedAtEpochMillis).isNull()
    }

    @Test
    fun `an ordinary open task is returned untouched`() {
        val open = task(done = false)

        assertThat(TaskCompletion.stamp(open, now)).isSameInstanceAs(open)
    }

    @Test
    fun `stamping is idempotent`() {
        // Every write goes through `upsertTask`, so a task is stamped again on every save.
        // A rule that moved anything on the second pass would drift a task's completion time
        // forward once per edit.
        val once = TaskCompletion.stamp(task(done = true), now)
        val twice = TaskCompletion.stamp(once, now + 60_000L)

        assertThat(twice).isEqualTo(once)
    }

    @Test
    fun `every output is well formed, from every input shape`() {
        val inputs = listOf(
            task(done = false, completedAt = null),
            task(done = false, completedAt = now),
            task(done = true, completedAt = null),
            task(done = true, completedAt = now - 1),
        )

        val outputs = inputs.map { TaskCompletion.stamp(it, now) }

        assertThat(outputs.map { TaskCompletion.isWellFormed(it) })
            .containsExactly(true, true, true, true)
        // And the predicate is not vacuous — two of those four inputs were malformed going in.
        assertThat(inputs.map { TaskCompletion.isWellFormed(it) })
            .containsExactly(true, false, false, true)
            .inOrder()
    }

    @Test
    fun `nothing but the completion pair is touched`() {
        val before = task(done = true)

        val after = TaskCompletion.stamp(before, now)

        assertThat(after).isEqualTo(before.copy(completedAtEpochMillis = now))
    }

    // ── What the readers downstream actually do with it ──────────────
    //
    // These drive the REAL use cases rather than re-typing their filters. Both are pure and
    // take a no-arg constructor, so there is no reason to test a copy — and a copy is exactly
    // what would stay green if the predicate upstream changed shape.

    private val goal = Goal(id = "g-1", title = "Run a half marathon", lifeAreaIds = listOf("a-1"))
    private val area = LifeArea(id = "a-1", name = "Health")

    @Test
    fun `the weekly summary counts a born-done task, and misses it unstamped`() {
        val windowStart = now - 7 * 86_400_000L
        val unstamped = task(done = true)
        val stamped = TaskCompletion.stamp(unstamped, now)
        val summarise = BuildSummaryUseCase()

        val missed = summarise(SummaryPeriod.WEEKLY, listOf(goal), listOf(unstamped), windowStart, now)
        val counted = summarise(SummaryPeriod.WEEKLY, listOf(goal), listOf(stamped), windowStart, now)

        // The failure is not that a field is missing — it is that the summary reports ZERO
        // tasks and ZERO points for work the projection function has already paid for.
        assertThat(missed.completedTasks).isEqualTo(0)
        assertThat(missed.totalPoints).isEqualTo(0L)
        assertThat(counted.completedTasks).isEqualTo(1)
        assertThat(counted.totalPoints).isEqualTo(15L)
    }

    @Test
    fun `the time-allocation chart sees a born-done task, and drops it unstamped`() {
        val window = TimeWindow(startMillis = now - 1_000L, endMillisExclusive = now + 1_000L)
        val allocate = TimeAllocationUseCase()

        val missed = allocate(window, listOf(area), listOf(goal), listOf(task(done = true)))
        val seen = allocate(window, listOf(area), listOf(goal), listOf(TaskCompletion.stamp(task(done = true), now)))

        assertThat(missed.isEmpty).isTrue()
        assertThat(seen.isEmpty).isFalse()
        assertThat(seen.totalMinutes).isEqualTo(45)
    }
}
