package com.idomarhaim.goalpilot.domain.usecase

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
 * Splits the time a user spent in a window across their life areas
 * (spec §6 Bonus: rich analytics).
 *
 * The chain is `completed task → its goal → that goal's life area`, weighted by
 * the task's LLM-estimated duration. Pure — no Android, no I/O, no clock — so
 * `TimeAllocationUseCaseTest` can exercise every branch on the JVM.
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

        // Group by area id, with null standing for the unassigned bucket. An
        // archived area still gets its own slice: the time was really spent there,
        // and folding it into "Unassigned" would silently rewrite the user's past.
        val byArea: Map<String?, List<Task>> = completed.groupBy { task ->
            val goal = task.goalId?.let(goalsById::get)
            goal?.lifeAreaId?.takeIf { areasById.containsKey(it) }
        }

        val minutesByArea = byArea.mapValues { (_, list) -> list.sumOf { TaskDuration.minutesOf(it) } }
        val total = minutesByArea.values.sum()
        if (total <= 0) return TimeAllocation()

        val slices = minutesByArea
            .map { (areaId, minutes) ->
                val area = areaId?.let(areasById::get)
                val list = byArea.getValue(areaId)
                TimeSlice(
                    areaId = areaId,
                    name = area?.name?.takeIf { it.isNotBlank() } ?: UNASSIGNED_NAME,
                    colorHex = area?.colorHex ?: LifeAreaPalette.DEFAULT_HEX,
                    iconKey = area?.iconKey ?: "flag",
                    minutes = minutes,
                    taskCount = list.size,
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

    companion object {
        const val UNASSIGNED_NAME = "Unassigned"
    }
}
