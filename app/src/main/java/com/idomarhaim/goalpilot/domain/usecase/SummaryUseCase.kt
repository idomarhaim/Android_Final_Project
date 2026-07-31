package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.util.SummaryPeriod
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalProgress
import com.idomarhaim.goalpilot.domain.model.ProgressSummary
import com.idomarhaim.goalpilot.domain.model.Task
import javax.inject.Inject

/**
 * Pure computation that turns the user's goals + tasks into a shareable
 * [ProgressSummary] for a time window (spec §7). Kept free of Android/Firebase
 * types so it is trivially unit-testable.
 */
class BuildSummaryUseCase @Inject constructor() {

    operator fun invoke(
        period: SummaryPeriod,
        goals: List<Goal>,
        tasks: List<Task>,
        windowStartMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProgressSummary {
        val activeGoals = goals.filterNot { it.isArchived }

        val tasksInWindow = tasks.filter {
            it.isDone && (it.completedAtEpochMillis ?: 0L) >= windowStartMillis
        }
        val completedCount = tasksInWindow.size
        val earnedPoints = tasksInWindow.sumOf { it.points.toLong() }

        val pointsByGoal = tasksInWindow
            .groupBy { it.goalId }
            .mapValues { (_, list) -> list.sumOf { it.points.toLong() } }

        val goalProgress = activeGoals.map { goal ->
            GoalProgress(
                goalId = goal.id,
                title = goal.title,
                category = goal.category,
                fraction = goal.progressFraction,
                points = pointsByGoal[goal.id] ?: 0L,
            )
        }

        return ProgressSummary(
            period = period,
            totalPoints = earnedPoints,
            completedTasks = completedCount,
            activeGoals = activeGoals.size,
            goals = goalProgress,
            generatedAtEpochMillis = nowMillis,
        )
    }
}
