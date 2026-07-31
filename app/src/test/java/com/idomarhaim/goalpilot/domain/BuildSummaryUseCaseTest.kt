package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.BuildSummaryUseCase
import org.junit.Test

class BuildSummaryUseCaseTest {

    private val useCase = BuildSummaryUseCase()

    private val goals = listOf(
        Goal(id = "g1", title = "Run", currentValue = 50.0, targetValue = 100.0),
        Goal(id = "g2", title = "Read", currentValue = 20.0, targetValue = 100.0),
        Goal(id = "g3", title = "Old", isArchived = true),
    )

    @Test
    fun `counts only completed tasks inside the window and sums their points`() {
        val windowStart = 1_000L
        val tasks = listOf(
            Task(id = "t1", goalId = "g1", points = 10, isDone = true, completedAtEpochMillis = 1_500L),
            Task(id = "t2", goalId = "g1", points = 5, isDone = true, completedAtEpochMillis = 2_000L),
            Task(id = "t3", goalId = "g2", points = 8, isDone = true, completedAtEpochMillis = 500L), // before window
            Task(id = "t4", goalId = "g2", points = 8, isDone = false, completedAtEpochMillis = null),
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
        assertThat(summary.goals.first { it.goalId == "g1" }.points).isEqualTo(15)
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
