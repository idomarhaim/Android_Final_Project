package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskCompletion
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocationUseCase
import org.junit.Test

/**
 * `#7`'s invariant and `#55`'s answer to it.
 *
 * ### What this suite used to be, and why it is shorter now
 *
 * A completion was two fields, `isDone` and `completedAtEpochMillis`, and half of them was a
 * real and dangerous state: `pointsFromTasks` counted `done`, while the weekly summary, the
 * dashboard's done-this-week count and the time chart all required the stamp — so a done task
 * with no stamp **awarded points and was invisible everywhere the user could go and check**.
 * `TaskCompletion.stamp` existed to normalise the four states into the two that are legal,
 * and most of this file asserted that it did.
 *
 * §1.4 moved the completion into [CompletionFact], one object that either exists or does not.
 * `isDone` is `completion != null`. **The malformed state has no representation**, so there is
 * nothing left to normalise and the cases that asserted the repair have been deleted rather
 * than rewritten — they now assert a property of the Kotlin type system.
 *
 * ### What is left, and why each of the three is still worth running
 *
 * 1. **Minting** — [TaskCompletion.of] is the one place a fact is created, called by the tick
 *    and by the born-done create alike (§1.4's *"emit that same fact, not a second pipe"*).
 *    What it banks, and what it refuses to re-bank, is the whole of §1.4's re-pricing defence.
 * 2. **The unrepresentability itself**, asserted through the constructor rather than assumed.
 * 3. **The readers downstream**, driven as the REAL use cases rather than as re-typed filters
 *    — the same argument the old suite made, and the one part of it that did not depend on
 *    the two-field shape.
 */
class TaskCompletionTest {

    private val now = 1_755_000_000_000L

    private fun task(
        done: Boolean = false,
        completedAt: Long? = null,
        minutes: Int? = 45,
        difficulty: Difficulty = Difficulty.ROUTINE,
    ) = Task(
        id = "t-1",
        goalEdges = goalEdgesOf("g-1", contribution = 1.0),
        title = "Run 5 km",
        difficulty = difficulty,
        estimatedMinutes = minutes,
        durationSource = DurationSource.USER,
        createdAtEpochMillis = now - 10_000L,
        completion = if (done) {
            CompletionFact(
                completedAtEpochMillis = completedAt ?: now,
                minutes = minutes ?: 30,
                difficulty = difficulty,
            )
        } else {
            null
        },
    )

    // ── 1 · Minting ──────────────────────────────────────────────────

    @Test
    fun `a task created done gets its stamp from the write that created it`() {
        // `#7` itself: the quick-add "Already done" chip produces exactly this input — a task
        // carrying a placeholder fact with no timestamp, because only the write knows `now`.
        val fact = TaskCompletion.of(task().copy(completion = CompletionFact()), now)

        assertThat(fact.completedAtEpochMillis).isEqualTo(now)
    }

    @Test
    fun `the fact banks the inputs as they stand at the tick`() {
        // §1.4's "points are banked as their inputs". The fact has to be complete on its own:
        // `functions/src/projection.ts` totals the collection without reading a single task.
        val fact = TaskCompletion.of(
            task(minutes = 90, difficulty = Difficulty.DEMANDING),
            now,
        )

        assertThat(fact.minutes).isEqualTo(90)
        assertThat(fact.difficulty).isEqualTo(Difficulty.DEMANDING)
        assertThat(fact.points).isEqualTo(45) // round(90/3) = 30, ×1.5
    }

    @Test
    fun `a task with no duration banks the resolved fallback, not a null`() {
        // `TaskDuration.minutesOf` rather than `estimatedMinutes`, so a reader of
        // `completionFacts` never has to go and find the task to know what it was worth.
        val fact = TaskCompletion.of(task(minutes = null), now)

        assertThat(fact.minutes).isEqualTo(30)
        assertThat(fact.points).isEqualTo(10)
    }

    @Test
    fun `a completion that already happened is never re-dated or re-priced`() {
        // AnalyticsViewModel's duration backfill re-saves completed tasks routinely. Moving
        // the timestamp would hop the task into whichever week the correction was made; and
        // re-banking today's estimate is the re-pricing §1.4 banks the inputs to prevent —
        // tick at 10, correct the duration, and the completion must still be worth 10.
        val yesterday = now - 86_400_000L
        val banked = CompletionFact(
            completedAtEpochMillis = yesterday,
            minutes = 30,
            difficulty = Difficulty.ROUTINE,
        )
        val corrected = task(minutes = 300).copy(completion = banked)

        val fact = TaskCompletion.of(corrected, now)

        assertThat(fact).isEqualTo(banked)
        assertThat(fact.points).isEqualTo(10)
    }

    @Test
    fun `minting is idempotent`() {
        // Every write goes through `upsertTask`, so a done task is minted again on every save.
        // A rule that moved anything on the second pass would drift a completion forward once
        // per edit — which is the defect the case above describes, arriving by repetition.
        val once = TaskCompletion.of(task().copy(completion = CompletionFact()), now)
        val twice = TaskCompletion.of(task().copy(completion = once), now + 60_000L)

        assertThat(twice).isEqualTo(once)
    }

    // ── 2 · The malformed state has no representation ────────────────

    @Test
    fun `isDone and the stamp cannot disagree, because they are one field`() {
        // The invariant the deleted half of this suite used to repair. There is no way to
        // construct the state it repaired: `isDone` READS `completion`, so a done task with
        // no stamp would require a fact with no fact in it.
        val open = task(done = false)
        val done = task(done = true, completedAt = now)

        assertThat(open.isDone).isFalse()
        assertThat(open.completedAtEpochMillis).isNull()
        assertThat(done.isDone).isTrue()
        assertThat(done.completedAtEpochMillis).isEqualTo(now)

        // And clearing one clears the other, in one assignment rather than by convention.
        assertThat(done.copy(completion = null).isDone).isFalse()
        assertThat(done.copy(completion = null).completedAtEpochMillis).isNull()
    }

    @Test
    fun `an open task is priced live and a done task is priced from what it banked`() {
        val open = task(done = false, minutes = 60)
        val done = task(done = true, minutes = 60)

        assertThat(open.points).isEqualTo(20)
        assertThat(done.points).isEqualTo(20)
        // Correct the estimate afterwards: the open task re-prices, the completed one does not.
        assertThat(open.copy(estimatedMinutes = 300).points).isEqualTo(100)
        assertThat(done.copy(estimatedMinutes = 300).points).isEqualTo(20)
    }

    // ── 3 · What the readers downstream actually do with it ──────────
    //
    // These drive the REAL use cases rather than re-typing their filters. Both are pure and
    // take a no-arg constructor, so there is no reason to test a copy — and a copy is exactly
    // what would stay green if the predicate upstream changed shape.

    private val goal = Goal(id = "g-1", title = "Run a half marathon", lifeAreaIds = listOf("a-1"))
    private val area = LifeArea(id = "a-1", name = "Health")

    @Test
    fun `the weekly summary counts a completed task and ignores an open one`() {
        val windowStart = now - 7 * 86_400_000L
        val summarise = BuildSummaryUseCase()

        val open = summarise(SummaryPeriod.WEEKLY, listOf(goal), listOf(task()), windowStart, now)
        val counted =
            summarise(SummaryPeriod.WEEKLY, listOf(goal), listOf(task(done = true)), windowStart, now)

        assertThat(open.completedTasks).isEqualTo(0)
        assertThat(open.totalPoints).isEqualTo(0L)
        assertThat(counted.completedTasks).isEqualTo(1)
        // 45 minutes at ROUTINE: round(45/3) = 15.
        assertThat(counted.totalPoints).isEqualTo(15L)
        // §1.4: the per-goal companion number is EFFORT, and points are not a property of an
        // objective at all. `GoalProgress.points` was deleted with this ticket.
        assertThat(counted.goals.single { it.goalId == "g-1" }.effortMinutes).isEqualTo(45)
    }

    @Test
    fun `the time-allocation chart sees a completed task and drops an open one`() {
        val window = TimeWindow(startMillis = now - 1_000L, endMillisExclusive = now + 1_000L)
        val allocate = TimeAllocationUseCase()

        val open = allocate(window, listOf(area), listOf(goal), listOf(task()))
        val seen = allocate(window, listOf(area), listOf(goal), listOf(task(done = true)))

        assertThat(open.isEmpty).isTrue()
        assertThat(seen.isEmpty).isFalse()
        assertThat(seen.totalMinutes).isEqualTo(45)
    }
}
