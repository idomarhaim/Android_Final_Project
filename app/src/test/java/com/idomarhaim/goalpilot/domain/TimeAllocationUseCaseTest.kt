package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocationUseCase
import org.junit.Test

/**
 * The chain the headline analytics chart depends on:
 * completed task → its goal → that goal's life area, weighted by duration.
 */
class TimeAllocationUseCaseTest {

    private val useCase = TimeAllocationUseCase()

    private val areas = listOf(
        LifeArea(id = "health", name = "Health", colorHex = "#CF3636", iconKey = "favorite"),
        LifeArea(id = "study", name = "Studies", colorHex = "#0F6FCB", iconKey = "school"),
    )

    private val goals = listOf(
        Goal(id = "g-run", title = "Run", lifeAreaId = "health"),
        Goal(id = "g-sleep", title = "Sleep", lifeAreaId = "health"),
        Goal(id = "g-thesis", title = "Thesis", lifeAreaId = "study"),
        Goal(id = "g-loose", title = "Unfiled goal"),
    )

    private val window = TimeWindow(startMillis = 1_000L, endMillisExclusive = 9_000L)

    private fun done(id: String, goalId: String?, at: Long, minutes: Int? = null, points: Int = 10) =
        Task(
            id = id,
            goalId = goalId,
            isDone = true,
            points = points,
            estimatedMinutes = minutes,
            completedAtEpochMillis = at,
        )

    @Test
    fun `sums minutes per life area and reports each area's share`() {
        val tasks = listOf(
            done("t1", "g-run", at = 2_000L, minutes = 60),
            done("t2", "g-sleep", at = 3_000L, minutes = 30),
            done("t3", "g-thesis", at = 4_000L, minutes = 30),
        )

        val result = useCase(window, areas, goals, tasks)

        assertThat(result.totalMinutes).isEqualTo(120)
        assertThat(result.completedTasks).isEqualTo(3)
        // Biggest first: Health 90 of 120, Studies 30 of 120.
        assertThat(result.slices.map { it.name }).containsExactly("Health", "Studies").inOrder()
        assertThat(result.slices.first().minutes).isEqualTo(90)
        assertThat(result.slices.first().fraction).isWithin(0.001f).of(0.75f)
        assertThat(result.slices.first().percent).isEqualTo(75)
        assertThat(result.slices.first().taskCount).isEqualTo(2)
    }

    @Test
    fun `only completed tasks inside the window count`() {
        val tasks = listOf(
            done("inside", "g-run", at = 1_000L, minutes = 60), // inclusive start
            done("before", "g-run", at = 999L, minutes = 60),
            done("after", "g-run", at = 9_000L, minutes = 60), // exclusive end
            Task(
                id = "open",
                goalId = "g-run",
                isDone = false,
                estimatedMinutes = 60,
                completedAtEpochMillis = 2_000L,
            ),
            // Flagged done but never stamped — nothing to place it in a window with.
            Task(id = "stampless", goalId = "g-run", isDone = true, estimatedMinutes = 60),
        )

        val result = useCase(window, areas, goals, tasks)

        assertThat(result.completedTasks).isEqualTo(1)
        assertThat(result.totalMinutes).isEqualTo(60)
    }

    @Test
    fun `tasks with no goal, no area, or a dangling area land in Unassigned`() {
        val tasks = listOf(
            done("no-goal", null, at = 2_000L, minutes = 20),
            done("unfiled-goal", "g-loose", at = 2_000L, minutes = 20),
            done("dangling", "g-ghost", at = 2_000L, minutes = 20),
            done("real", "g-run", at = 2_000L, minutes = 60),
        )
        // A goal whose life area was deleted must not vanish from the chart.
        val goalsWithDanglingArea = goals + Goal(id = "g-ghost", lifeAreaId = "deleted-area")

        val result = useCase(window, areas, goalsWithDanglingArea, tasks)

        val unassigned = result.slices.first { it.areaId == null }
        assertThat(unassigned.name).isEqualTo(TimeAllocationUseCase.UNASSIGNED_NAME)
        assertThat(unassigned.minutes).isEqualTo(60)
        assertThat(unassigned.taskCount).isEqualTo(3)
        assertThat(result.totalMinutes).isEqualTo(120)
    }

    @Test
    fun `a task with no stored estimate still counts, using its points`() {
        val tasks = listOf(
            done("estimated", "g-run", at = 2_000L, minutes = 45),
            done("guessed", "g-thesis", at = 2_000L, minutes = null, points = 20),
        )

        val result = useCase(window, areas, goals, tasks)

        // 20 points × 3 = 60 minutes from the fallback.
        assertThat(result.slices.first { it.name == "Studies" }.minutes).isEqualTo(60)
        assertThat(result.totalMinutes).isEqualTo(105)
        assertThat(result.estimatedTaskCount).isEqualTo(1)
        assertThat(result.completedTasks).isEqualTo(2)
    }

    @Test
    fun `fractions add up to one`() {
        val tasks = listOf(
            done("a", "g-run", at = 2_000L, minutes = 33),
            done("b", "g-thesis", at = 2_000L, minutes = 67),
            done("c", null, at = 2_000L, minutes = 15),
        )

        val result = useCase(window, areas, goals, tasks)

        assertThat(result.slices.map { it.fraction }.sum()).isWithin(0.0001f).of(1f)
    }

    @Test
    fun `an empty window produces an empty allocation rather than a zero-slice pie`() {
        val result = useCase(window, areas, goals, tasks = emptyList())

        assertThat(result.isEmpty).isTrue()
        assertThat(result.totalMinutes).isEqualTo(0)
        assertThat(result.slices).isEmpty()
    }

    @Test
    fun `time spent under an archived area keeps its own slice`() {
        val archived = areas + LifeArea(id = "old", name = "Old area", isArchived = true)
        val goalsWithArchived = goals + Goal(id = "g-old", lifeAreaId = "old")
        val tasks = listOf(
            done("x", "g-old", at = 2_000L, minutes = 40),
            done("y", "g-run", at = 2_000L, minutes = 60),
        )

        val result = useCase(window, archived, goalsWithArchived, tasks)

        assertThat(result.slices.map { it.name }).containsExactly("Health", "Old area")
    }
}
