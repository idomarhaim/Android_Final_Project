package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthSnapshot
import java.time.LocalDate
import javax.inject.Inject

/** The two things GoalPilot reads out of Health Connect. */
enum class HealthMetric(
    val label: String,
    val unit: String,
    val category: GoalCategory,
    /** Title for the goal proposed when the user has nothing suitable yet. */
    val defaultGoalTitle: String,
    /** Target for that proposed goal — one week at a widely-cited healthy rate. */
    val defaultGoalTarget: Double,
) {
    STEPS(
        label = "Steps",
        unit = "steps",
        category = GoalCategory.FITNESS,
        defaultGoalTitle = "Weekly steps",
        defaultGoalTarget = 70_000.0,
    ),
    SLEEP(
        label = "Sleep",
        unit = "hours",
        category = GoalCategory.SLEEP,
        defaultGoalTitle = "Weekly sleep",
        defaultGoalTarget = 56.0,
    ),
}

/**
 * One day's reading, together with where it would be filed. Nothing is written
 * until the user confirms — same policy as the Google Tasks import and smart add.
 *
 * Exactly one of [targetGoalId] (log against a goal that already exists) and
 * [newGoalTitle] (create one first) is set.
 */
data class HealthLogProposal(
    val sourceKey: String,
    val metric: HealthMetric,
    val epochDay: Long,
    val value: Double,
    val targetGoalId: String? = null,
    val targetGoalTitle: String? = null,
    val newGoalTitle: String? = null,
    val selected: Boolean = true,
) {
    val goalLabel: String get() = targetGoalTitle ?: "new goal “$newGoalTitle”"
}

/**
 * Turns a Health Connect reading into a reviewable list of progress-log proposals.
 *
 * Pure: no Android, no I/O, no clock. Every rule about which goal a reading lands
 * in — and which readings are skipped because they are already logged — is
 * decided here, so `HealthProposalsTest` can exercise the whole thing on the JVM.
 */
class BuildHealthProposalsUseCase @Inject constructor() {

    /**
     * @param alreadyLogged [HealthLogProposal.sourceKey]s already present on the
     *   candidate goals' progress entries. Re-syncing must not double-count a day:
     *   steps are cumulative, so logging Saturday twice silently doubles the week.
     */
    operator fun invoke(
        snapshot: HealthSnapshot,
        goals: List<Goal>,
        alreadyLogged: Set<String> = emptySet(),
    ): List<HealthLogProposal> {
        val stepsGoal = goals.matchFor(HealthMetric.STEPS)
        val sleepGoal = goals.matchFor(HealthMetric.SLEEP)

        val stepProposals = snapshot.steps
            .filter { it.steps > 0 }
            .map { day ->
                proposal(
                    metric = HealthMetric.STEPS,
                    epochDay = day.epochDay,
                    value = day.steps.toDouble(),
                    goal = stepsGoal,
                )
            }

        val sleepProposals = snapshot.sleep
            .filter { it.minutes > 0 }
            .map { night ->
                proposal(
                    metric = HealthMetric.SLEEP,
                    epochDay = night.epochDay,
                    // Hours to one decimal: "7.5 hours" is the unit people think in,
                    // and an unrounded 7.483333 would show up in the goal's total.
                    value = Math.round(night.hours * 10.0) / 10.0,
                    goal = sleepGoal,
                )
            }

        return (stepProposals + sleepProposals)
            .filter { it.sourceKey !in alreadyLogged }
            .sortedWith(compareByDescending<HealthLogProposal> { it.epochDay }.thenBy { it.metric })
    }

    private fun proposal(
        metric: HealthMetric,
        epochDay: Long,
        value: Double,
        goal: Goal?,
    ) = HealthLogProposal(
        sourceKey = sourceKey(metric, epochDay),
        metric = metric,
        epochDay = epochDay,
        value = value,
        targetGoalId = goal?.id,
        targetGoalTitle = goal?.title,
        newGoalTitle = if (goal == null) metric.defaultGoalTitle else null,
    )

    /**
     * Picks the goal a metric should be logged against: an active goal in the
     * matching category, preferring one whose unit already agrees so steps do not
     * get added to a "workouts" goal and inflate it by four thousand.
     */
    private fun List<Goal>.matchFor(metric: HealthMetric): Goal? {
        val candidates = filter { !it.isArchived && it.category == metric.category }
        return candidates.firstOrNull { it.unit.equals(metric.unit, ignoreCase = true) }
            ?: candidates.firstOrNull()
    }

    companion object {
        /**
         * Stable identity for "this metric, this day", stored on the progress entry
         * as [com.idomarhaim.goalpilot.domain.model.ProgressEntry.sourceKey] so a
         * re-sync can recognise its own earlier writes exactly. The Google Tasks
         * import has to dedupe by title because its rows carry no such handle —
         * this one does not have to guess.
         */
        fun sourceKey(metric: HealthMetric, epochDay: Long): String =
            "hc:${metric.name.lowercase()}:${LocalDate.ofEpochDay(epochDay)}"
    }
}
