package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.domain.model.HealthSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    /**
     * Smallest change worth writing a top-up entry for. A day is re-read every
     * time the app comes forward, so without a floor a sleep session recomputed
     * as 7.50 instead of 7.49 would add a 0.01-hour row to the goal's history.
     */
    val minimumDelta: Double,
) {
    STEPS(
        label = "Steps",
        unit = "steps",
        category = GoalCategory.FITNESS,
        defaultGoalTitle = "Weekly steps",
        defaultGoalTarget = 70_000.0,
        minimumDelta = 1.0,
    ),
    SLEEP(
        label = "Sleep",
        unit = "hours",
        category = GoalCategory.SLEEP,
        defaultGoalTitle = "Weekly sleep",
        defaultGoalTarget = 56.0,
        minimumDelta = 0.1,
    ),
}

/**
 * One day's reading, together with where it would be filed.
 *
 * [value] is the amount to write **now**, never the day's whole reading: today's
 * step count grows all day, so a day already partly logged is topped up by the
 * difference (see [BuildHealthProposalsUseCase]).
 *
 * Exactly one of [targetGoalId] (log against a goal that already exists) and
 * [newGoalTitle] (create one first) is set.
 */
data class HealthLogProposal(
    val sourceKey: String,
    val metric: HealthMetric,
    val epochDay: Long,
    /** The delta to write: the day's reading minus whatever is already logged. */
    val value: Double,
    /** What earlier syncs already wrote for this day; 0.0 the first time. */
    val alreadyLogged: Double = 0.0,
    val targetGoalId: String? = null,
    val targetGoalTitle: String? = null,
    val newGoalTitle: String? = null,
) {
    /** The day's full reading as Health Connect now sees it. */
    val total: Double get() = alreadyLogged + value

    /** True when this only adds the difference to a day that was already logged. */
    val isTopUp: Boolean get() = alreadyLogged > 0.0
}

/**
 * Turns a Health Connect reading into a list of progress-log proposals.
 *
 * Pure: no Android, no I/O, no clock. Every rule about which goal a reading lands
 * in — and how much of it is still owed — is decided here, so
 * `HealthProposalsTest` can exercise the whole thing on the JVM.
 */
class BuildHealthProposalsUseCase @Inject constructor() {

    /**
     * @param alreadyLogged how much has already been written for each
     *   [HealthLogProposal.sourceKey], summed across that day's entries. This is a
     *   *map*, not a set of keys, because today is still accumulating: the app
     *   syncs whenever it comes forward, so the first sync of a day sees 2,000
     *   steps and the last sees 11,000. Dropping a day the moment it appears once
     *   would freeze it at its earliest reading — the whole day would be recorded
     *   as whatever it was at breakfast. Writing the *difference* keeps the goal's
     *   running total honest without ever double-counting.
     *
     *   A day that has shrunk (Health Connect revised it down, or the user logged
     *   against the goal by hand) yields a negative delta and is simply skipped:
     *   this never subtracts.
     */
    operator fun invoke(
        snapshot: HealthSnapshot,
        goals: List<Goal>,
        alreadyLogged: Map<String, Double> = emptyMap(),
    ): List<HealthLogProposal> {
        val stepsGoal = goals.matchFor(HealthMetric.STEPS)
        val sleepGoal = goals.matchFor(HealthMetric.SLEEP)

        val stepProposals = snapshot.steps
            .filter { it.steps > 0 }
            .mapNotNull { day ->
                proposal(
                    metric = HealthMetric.STEPS,
                    epochDay = day.epochDay,
                    reading = day.steps.toDouble(),
                    goal = stepsGoal,
                    alreadyLogged = alreadyLogged,
                )
            }

        val sleepProposals = snapshot.sleep
            .filter { it.minutes > 0 }
            .mapNotNull { night ->
                proposal(
                    metric = HealthMetric.SLEEP,
                    epochDay = night.epochDay,
                    // Hours to one decimal: "7.5 hours" is the unit people think in,
                    // and an unrounded 7.483333 would show up in the goal's total.
                    reading = night.hours.roundToOneDecimal(),
                    goal = sleepGoal,
                    alreadyLogged = alreadyLogged,
                )
            }

        return (stepProposals + sleepProposals)
            .sortedWith(compareByDescending<HealthLogProposal> { it.epochDay }.thenBy { it.metric })
    }

    /** Null when this day owes nothing — already logged in full, or close enough. */
    private fun proposal(
        metric: HealthMetric,
        epochDay: Long,
        reading: Double,
        goal: Goal?,
        alreadyLogged: Map<String, Double>,
    ): HealthLogProposal? {
        val key = sourceKey(metric, epochDay)
        val logged = alreadyLogged[key] ?: 0.0
        val delta = (reading - logged).let {
            if (metric == HealthMetric.SLEEP) it.roundToOneDecimal() else it
        }
        if (delta < metric.minimumDelta) return null
        return HealthLogProposal(
            sourceKey = key,
            metric = metric,
            epochDay = epochDay,
            value = delta,
            alreadyLogged = logged,
            targetGoalId = goal?.id,
            targetGoalTitle = goal?.title,
            newGoalTitle = if (goal == null) metric.defaultGoalTitle else null,
        )
    }

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

    private fun Double.roundToOneDecimal(): Double = Math.round(this * 10.0) / 10.0

    companion object {
        /**
         * Stable identity for "this metric, this day", stored on the progress entry
         * as [com.idomarhaim.goalpilot.domain.model.ProgressEntry.sourceKey] so a
         * re-sync can recognise its own earlier writes exactly. The Google Tasks
         * import has to dedupe by title because its rows carry no such handle —
         * this one does not have to guess.
         *
         * Deliberately **not** unique per entry: a day topped up three times has
         * three entries under one key, and their sum is what that day has been
         * credited so far.
         */
        fun sourceKey(metric: HealthMetric, epochDay: Long): String =
            "hc:${metric.name.lowercase()}:${LocalDate.ofEpochDay(epochDay)}"
    }
}

private val healthDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/** e.g. "Sat, Aug 1" — the day the reading belongs to, not the day it was synced. */
fun HealthLogProposal.dayLabel(): String =
    healthDayFormatter.format(LocalDate.ofEpochDay(epochDay))

/** The amount being written now, in the metric's own unit. */
fun HealthLogProposal.valueLabel(): String = when (metric) {
    HealthMetric.STEPS -> "%,d steps".format(value.toLong())
    HealthMetric.SLEEP -> "%.1f hours".format(value)
}

/** The day's full reading, for a top-up's "of what" half. */
fun HealthLogProposal.totalLabel(): String = when (metric) {
    HealthMetric.STEPS -> "%,d".format(total.toLong())
    HealthMetric.SLEEP -> "%.1f".format(total)
}

/**
 * What gets written to the progress entry the user will read back later. A top-up
 * says so and names the day's running total, otherwise "+2,300 steps" on a day the
 * user knows they walked eleven thousand reads like a bug.
 */
fun HealthLogProposal.noteText(): String = if (isTopUp) {
    "Health Connect · +${valueLabel()} (${totalLabel()} total) · ${dayLabel()}"
} else {
    "Health Connect · ${valueLabel()} · ${dayLabel()}"
}
