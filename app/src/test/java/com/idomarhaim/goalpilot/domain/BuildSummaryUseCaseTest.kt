package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import org.junit.Test

class BuildSummaryUseCaseTest {

    private val useCase = BuildSummaryUseCase()

    private val goals = listOf(
        Goal(id = "g1", title = "Run", currentValue = 50.0, targetValue = 100.0),
        Goal(id = "g2", title = "Read", currentValue = 20.0, targetValue = 100.0),
        Goal(id = "g3", title = "Old", isArchived = true),
    )

    /** A completed task worth [minutes] at ROUTINE — `round(minutes / 3)` points (§1.4). */
    private fun done(id: String, goalId: String, minutes: Int, at: Long) = Task(
        id = id,
        goalEdges = goalEdgesOf(goalId),
        estimatedMinutes = minutes,
        completion = CompletionFact(completedAtEpochMillis = at, minutes = minutes),
    )

    @Test
    fun `counts only completed tasks inside the window and sums their points`() {
        val windowStart = 1_000L
        val tasks = listOf(
            done("t1", "g1", minutes = 30, at = 1_500L), // 10 points
            done("t2", "g1", minutes = 15, at = 2_000L), // 5 points
            done("t3", "g2", minutes = 24, at = 500L), // 8 points, before the window
            Task(id = "t4", goalEdges = goalEdgesOf("g2"), estimatedMinutes = 24),
        )

        val summary = useCase(
            period = SummaryPeriod.WEEKLY,
            goals = goals,
            tasks = tasks,
            windowStartMillis = windowStart,
            nowMillis = 3_000L,
        )

        assertThat(summary.completedTasks).isEqualTo(2)
        assertThat(summary.totalPoints).isEqualTo(15)
        assertThat(summary.activeGoals).isEqualTo(2) // archived goal excluded
        // §1.4: "Points are never rendered as a property of an objective ... the goal
        // header's companion number becomes effort." This asserted `.points == 15` until
        // `#55` deleted the field; the quantity underneath it is 30 + 15 = 45 MINUTES.
        assertThat(summary.goals.first { it.goalId == "g1" }.effortMinutes).isEqualTo(45)
    }

    @Test
    fun `a completed task banks its effort against every objective it serves`() {
        // §1.5: minutes are POOLED and divided only where the slices must sum to the time
        // that passed — the life-area pie. A per-goal companion number is not that: "4h 20m
        // of work logged toward this" is true of the whole run for each goal it served, and
        // halving it would make a goal's effort depend on how many other goals it shares.
        val shared = Task(
            id = "t1",
            goalEdges = listOf(GoalEdge("g1"), GoalEdge("g2")),
            estimatedMinutes = 60,
            completion = CompletionFact(completedAtEpochMillis = 1_500L, minutes = 60),
        )

        val summary = useCase(
            period = SummaryPeriod.WEEKLY,
            goals = goals,
            tasks = listOf(shared),
            windowStartMillis = 1_000L,
            nowMillis = 3_000L,
        )

        assertThat(summary.goals.first { it.goalId == "g1" }.effortMinutes).isEqualTo(60)
        assertThat(summary.goals.first { it.goalId == "g2" }.effortMinutes).isEqualTo(60)
        // The person's own currency is still counted once — 60 minutes happened once.
        assertThat(summary.totalPoints).isEqualTo(20L)
    }

    @Test
    fun `the total is what was banked, not what the task is estimated at today`() {
        // §1.4's re-pricing defence, at the reader that publishes a shareable number.
        val banked = Task(
            id = "t1",
            goalEdges = goalEdgesOf("g1"),
            estimatedMinutes = 300,
            completion = CompletionFact(completedAtEpochMillis = 1_500L, minutes = 30),
        )

        val summary = useCase(
            period = SummaryPeriod.WEEKLY,
            goals = goals,
            tasks = listOf(banked),
            windowStartMillis = 1_000L,
            nowMillis = 3_000L,
        )

        assertThat(summary.totalPoints).isEqualTo(10L)
        assertThat(summary.goals.first { it.goalId == "g1" }.effortMinutes).isEqualTo(30)
    }

    @Test
    fun `averageProgress averages active goal fractions`() {
        val summary = useCase(
            period = SummaryPeriod.DAILY,
            goals = goals,
            tasks = emptyList(),
            windowStartMillis = 0L,
        )
        // g1 = 0.5, g2 = 0.2 -> average 0.35
        assertThat(summary.averageProgress).isWithin(0.001f).of(0.35f)
    }
}
