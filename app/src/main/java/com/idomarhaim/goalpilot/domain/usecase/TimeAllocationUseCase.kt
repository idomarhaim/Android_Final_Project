package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.util.TimeBucket
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import javax.inject.Inject

/**
 * One slice of the time-allocation pie: how much of the window went into one life
 * area.
 *
 * [areaId] is null for the "Unassigned" bucket, which collects completed tasks
 * with no goal, goals with no area, and goals pointing at an area that no longer
 * exists.
 */
data class TimeSlice(
    val areaId: String?,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val minutes: Int,
    val taskCount: Int,
    /** Share of [TimeAllocation.totalMinutes], 0f..1f. */
    val fraction: Float,
) {
    val percent: Int get() = Math.round(fraction * 100f)
    val hours: Float get() = minutes / 60f
}

/**
 * The whole answer for one window: the slices plus the totals the header shows.
 *
 * [estimatedTaskCount] counts tasks whose duration came from the LLM rather than
 * the local fallback. The screen states it, because "you spent 62 % on Health" is
 * a very different claim depending on whether the numbers were measured or guessed.
 */
data class TimeAllocation(
    val slices: List<TimeSlice> = emptyList(),
    val totalMinutes: Int = 0,
    val completedTasks: Int = 0,
    val estimatedTaskCount: Int = 0,
) {
    val isEmpty: Boolean get() = slices.isEmpty()
    val totalHours: Float get() = totalMinutes / 60f
}

/**
 * One life area's row in the trend chart. Carries the name and colour so a column
 * can be drawn without going back to the area list, and so the trend's stacking
 * order is literally the pie's slice order.
 */
data class TrendSeries(
    val areaId: String?,
    val name: String,
    val colorHex: String,
)

/**
 * One column of the trend chart. [minutes] is positional — index *i* is the time
 * that went into [TimeTrend.series] *i* — rather than a map, because the chart
 * draws the segments in series order and a `List<Int>` compares by value, which
 * the chart's animation key depends on.
 */
data class TrendBucket(
    val label: String,
    val minutes: List<Int>,
) {
    val totalMinutes: Int get() = minutes.sum()
}

/**
 * The same window as [TimeAllocation], cut into consecutive buckets: what the pie
 * cannot say, which is whether an area is growing or shrinking.
 *
 * Its totals are the pie's totals redistributed, never recomputed from a different
 * rule — [TimeAllocationUseCase.trend] takes the finished allocation as input, so
 * "the columns add up to the donut" is a property of the design and not a
 * coincidence that has to be re-checked whenever either changes.
 */
data class TimeTrend(
    val series: List<TrendSeries> = emptyList(),
    val buckets: List<TrendBucket> = emptyList(),
) {
    val isEmpty: Boolean get() = buckets.none { it.totalMinutes > 0 }

    /** Tallest column, and so the one every other column is drawn relative to. */
    val maxBucketMinutes: Int get() = buckets.maxOfOrNull { it.totalMinutes } ?: 0

    val totalMinutes: Int get() = buckets.sumOf { it.totalMinutes }

    /**
     * The heaviest bucket, for the one-line summary under the chart. Null when
     * nothing was tracked — "your busiest day was Monday, with nothing on it" is
     * not a sentence worth showing.
     */
    val busiest: TrendBucket? get() = buckets.filter { it.totalMinutes > 0 }.maxByOrNull { it.totalMinutes }
}

/**
 * Splits the time a user spent in a window across their life areas
 * (spec §6 Bonus: rich analytics).
 *
 * The chain is `completed task → its goal → that goal's life areas`, weighted by
 * the task's LLM-estimated duration. Pure — no Android, no I/O, no clock — so
 * `TimeAllocationUseCaseTest` can exercise every branch on the JVM.
 *
 * Since `PRODUCT_v0.3` §1.2 a goal reaches many areas, and §4.7 fixes the
 * asymmetry that follows: **a completion counts in full in every area it serves,
 * while its minutes divide between them.** A goal with one area — which is every
 * goal the pre-§1.2 backfill produced — reads exactly as it did before.
 */
class TimeAllocationUseCase @Inject constructor() {

    operator fun invoke(
        window: TimeWindow,
        lifeAreas: List<LifeArea>,
        goals: List<Goal>,
        tasks: List<Task>,
    ): TimeAllocation {
        val completed = tasks.filter { task ->
            val completedAt = task.completedAtEpochMillis
            task.isDone && completedAt != null && window.contains(completedAt)
        }
        if (completed.isEmpty()) return TimeAllocation()

        val goalsById = goals.associateBy { it.id }
        val areasById = lifeAreas.associateBy { it.id }

        // Minutes and completions per area id, with null standing for the
        // unassigned bucket. An archived area still gets its own slice: the time
        // was really spent there, and folding it into "Unassigned" would silently
        // rewrite the user's past.
        val minutesByArea = mutableMapOf<String?, Int>()
        val tasksByArea = mutableMapOf<String?, Int>()
        for (task in completed) {
            val minutes = TaskDuration.minutesOf(task)
            for ((areaId, share) in task.splitAcrossAreas(minutes, goalsById, areasById)) {
                minutesByArea[areaId] = (minutesByArea[areaId] ?: 0) + share
                tasksByArea[areaId] = (tasksByArea[areaId] ?: 0) + 1
            }
        }

        val total = minutesByArea.values.sum()
        if (total <= 0) return TimeAllocation()

        val slices = minutesByArea
            .map { (areaId, minutes) ->
                val area = areaId?.let(areasById::get)
                TimeSlice(
                    areaId = areaId,
                    name = area?.name?.takeIf { it.isNotBlank() } ?: UNASSIGNED_NAME,
                    colorHex = area?.colorHex ?: LifeAreaPalette.DEFAULT_HEX,
                    iconKey = area?.iconKey ?: "flag",
                    minutes = minutes,
                    taskCount = tasksByArea[areaId] ?: 0,
                    fraction = minutes.toFloat() / total,
                )
            }
            // Biggest first, so the pie starts at 12 o'clock with the area that
            // actually dominates; ties by name keep the order stable across
            // recompositions instead of shuffling as the map iterates.
            .sortedWith(compareByDescending<TimeSlice> { it.minutes }.thenBy { it.name })

        return TimeAllocation(
            slices = slices,
            totalMinutes = total,
            completedTasks = completed.size,
            estimatedTaskCount = completed.count { (it.estimatedMinutes ?: 0) > 0 },
        )
    }

    /**
     * The same time, distributed over [buckets] instead of summed into one pie.
     *
     * Takes the finished [allocation] rather than the life areas, for two reasons
     * that are really one: its slices already carry every name and colour a column
     * needs, and its ordering — biggest area first — becomes the stacking order,
     * so the legend under the donut reads the trend as well. It also means an area
     * that has no slice can have no segment, which is how a task filed under a
     * deleted area lands in "Unassigned" here without repeating the rule that
     * decided so.
     *
     * Assumes [buckets] tile the window [allocation] was computed for, which is
     * what `AnalyticsRange.buckets()` guarantees. Anything outside them is simply
     * not counted, exactly as the pie does not count it.
     */
    fun trend(
        buckets: List<TimeBucket>,
        allocation: TimeAllocation,
        goals: List<Goal>,
        tasks: List<Task>,
    ): TimeTrend {
        if (allocation.isEmpty || buckets.isEmpty()) return TimeTrend()

        val series = allocation.slices.map { TrendSeries(it.areaId, it.name, it.colorHex) }
        val indexByArea: Map<String?, Int> =
            series.withIndex().associate { (index, s) -> s.areaId to index }
        val goalsById = goals.associateBy { it.id }
        val completed = tasks.filter { it.isDone && it.completedAtEpochMillis != null }

        // The slices already decided which areas exist here, so the split runs
        // against *them* rather than the life-area list: an area the pie folded
        // into "Unassigned" must not reappear as a column of its own.
        val sliceAreas = allocation.slices.mapNotNull { it.areaId }.associateWith { it }

        val rows = buckets.map { bucket ->
            val minutes = IntArray(series.size)
            for (task in completed) {
                if (!bucket.window.contains(task.completedAtEpochMillis!!)) continue
                val shares =
                    task.splitAcrossAreas(TaskDuration.minutesOf(task), goalsById, sliceAreas)
                for ((areaId, share) in shares) {
                    // An area with no slice cannot have a segment; it falls to the
                    // unassigned row, which the pie must already have created for it.
                    val index = indexByArea[areaId] ?: indexByArea[null] ?: continue
                    minutes[index] += share
                }
            }
            TrendBucket(label = bucket.label, minutes = minutes.toList())
        }

        return TimeTrend(series = series, buckets = rows)
    }

    /**
     * This task's [minutes] shared out over the life areas its goal serves, as
     * `areaId to share`. A null key is the unassigned bucket, which is where a
     * task with no goal, a goal with no areas, and a goal whose every area has
     * been deleted all land.
     *
     * The shares are integers that **sum back to [minutes] exactly** — the
     * remainder goes to the first areas rather than being dropped — because the
     * donut's fractions are taken over this total and a lost minute would make
     * them add to less than one.
     */
    private fun <T> Task.splitAcrossAreas(
        minutes: Int,
        goalsById: Map<String, Goal>,
        knownAreas: Map<String, T>,
    ): List<Pair<String?, Int>> {
        val areaIds = goalId?.let(goalsById::get)
            ?.lifeAreaIds
            ?.filter(knownAreas::containsKey)
            .orEmpty()
        if (areaIds.isEmpty()) return listOf(null to minutes)

        val base = minutes / areaIds.size
        val remainder = minutes % areaIds.size
        return areaIds.mapIndexed { index, id ->
            id to base + if (index < remainder) 1 else 0
        }
    }

    companion object {
        const val UNASSIGNED_NAME = "Unassigned"
    }
}
