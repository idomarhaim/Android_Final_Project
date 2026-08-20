package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.TimeBucket
import com.idomarhaim.goalpilot.core.util.TimeWindow
import com.idomarhaim.goalpilot.domain.model.DurationSource
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
        Goal(id = "g-run", title = "Run", lifeAreaIds = listOf("health")),
        Goal(id = "g-sleep", title = "Sleep", lifeAreaIds = listOf("health")),
        Goal(id = "g-thesis", title = "Thesis", lifeAreaIds = listOf("study")),
        Goal(id = "g-loose", title = "Unfiled goal"),
    )

    private val window = TimeWindow(startMillis = 1_000L, endMillisExclusive = 9_000L)

    private fun done(
        id: String,
        goalId: String?,
        at: Long,
        minutes: Int? = null,
        points: Int = 10,
        source: DurationSource = DurationSource.UNKNOWN,
    ) =
        Task(
            id = id,
            goalId = goalId,
            isDone = true,
            points = points,
            estimatedMinutes = minutes,
            durationSource = source,
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
        val goalsWithDanglingArea = goals + Goal(id = "g-ghost", lifeAreaIds = listOf("deleted-area"))

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
            done("estimated", "g-run", at = 2_000L, minutes = 45, source = DurationSource.AI),
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
    fun `the AI count reads provenance, not merely that a duration is stored`() {
        // Before #9 this counted every stored duration, which was the same set while
        // only the model could write one. R8's box breaks that: a task the user timed
        // by hand has a stored duration and did NOT come from the AI, and the card's
        // own line — "x of y durations estimated by AI" — would have claimed it did.
        val tasks = listOf(
            done("by-hand", "g-run", at = 2_000L, minutes = 45, source = DurationSource.USER),
            done("by-model", "g-sleep", at = 2_000L, minutes = 45, source = DurationSource.AI),
            // A row written before #9: a duration with no recorded origin. Not the
            // AI's on any evidence the app holds, so it is not counted as the AI's.
            done("legacy", "g-thesis", at = 2_000L, minutes = 45),
        )

        val result = useCase(window, areas, goals, tasks)

        assertThat(result.completedTasks).isEqualTo(3)
        assertThat(result.estimatedTaskCount).isEqualTo(1)
        // All three still contribute their minutes — provenance decides attribution,
        // never whether the time happened.
        assertThat(result.totalMinutes).isEqualTo(135)
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

    // ── Plural life areas (spec §1.2 / §4.7) ──────────────────────────

    @Test
    fun `a goal serving two areas divides its minutes and counts in both`() {
        // §4.7's asymmetry, and the reason both numbers sit on one screen: the
        // completion counts in full in every area, the minutes are shared out.
        val shared = goals + Goal(id = "g-both", lifeAreaIds = listOf("health", "study"))
        val tasks = listOf(done("t", "g-both", at = 2_000L, minutes = 60))

        val result = useCase(window, areas, shared, tasks)

        assertThat(result.totalMinutes).isEqualTo(60)
        assertThat(result.completedTasks).isEqualTo(1)
        assertThat(result.slices.map { it.minutes }).containsExactly(30, 30)
        assertThat(result.slices.map { it.taskCount }).containsExactly(1, 1)
    }

    @Test
    fun `an odd split loses no minute`() {
        // Integer division would drop the remainder and the fractions would then
        // add to less than one, which the donut renders as a gap.
        val shared = goals + Goal(id = "g-three", lifeAreaIds = listOf("health", "study", "money"))
        val withMoney = areas + LifeArea(id = "money", name = "Money")
        val tasks = listOf(done("t", "g-three", at = 2_000L, minutes = 100))

        val result = useCase(window, withMoney, shared, tasks)

        assertThat(result.totalMinutes).isEqualTo(100)
        assertThat(result.slices.sumOf { it.minutes }).isEqualTo(100)
        assertThat(result.slices.map { it.minutes }.sorted()).containsExactly(33, 33, 34).inOrder()
        assertThat(result.slices.map { it.fraction }.sum()).isWithin(0.0001f).of(1f)
    }

    @Test
    fun `a plural goal only divides between the areas that still exist`() {
        val shared = goals + Goal(id = "g-half", lifeAreaIds = listOf("health", "deleted-area"))
        val tasks = listOf(done("t", "g-half", at = 2_000L, minutes = 60))

        val result = useCase(window, areas, shared, tasks)

        // All 60 to Health: a dangling id is not a claim on the time, so this is
        // not "30 to Health and 30 to Unassigned".
        assertThat(result.slices).hasSize(1)
        assertThat(result.slices.single().areaId).isEqualTo("health")
        assertThat(result.slices.single().minutes).isEqualTo(60)
    }

    @Test
    fun `the trend divides a plural goal the same way the pie does`() {
        val shared = goals + Goal(id = "g-both", lifeAreaIds = listOf("health", "study"))
        val tasks = listOf(done("t", "g-both", at = 1_500L, minutes = 60))
        val allocation = useCase(window, areas, shared, tasks)

        val trend = useCase.trend(buckets, allocation, shared, tasks)

        assertThat(trend.totalMinutes).isEqualTo(allocation.totalMinutes)
        assertThat(trend.buckets.first().minutes).containsExactly(30, 30)
    }

    @Test
    fun `time spent under an archived area keeps its own slice`() {
        val archived = areas + LifeArea(id = "old", name = "Old area", isArchived = true)
        val goalsWithArchived = goals + Goal(id = "g-old", lifeAreaIds = listOf("old"))
        val tasks = listOf(
            done("x", "g-old", at = 2_000L, minutes = 40),
            done("y", "g-run", at = 2_000L, minutes = 60),
        )

        val result = useCase(window, archived, goalsWithArchived, tasks)

        assertThat(result.slices.map { it.name }).containsExactly("Health", "Old area")
    }

    // ── The trend: the same minutes, cut into buckets ─────────────────

    /** Four buckets tiling [window] exactly, as `AnalyticsRange.buckets()` would. */
    private val buckets = listOf(
        TimeBucket("a", TimeWindow(1_000L, 3_000L)),
        TimeBucket("b", TimeWindow(3_000L, 5_000L)),
        TimeBucket("c", TimeWindow(5_000L, 7_000L)),
        TimeBucket("d", TimeWindow(7_000L, 9_000L)),
    )

    @Test
    fun `the trend redistributes exactly the minutes the pie reports`() {
        val tasks = listOf(
            done("t1", "g-run", at = 1_500L, minutes = 60),
            done("t2", "g-thesis", at = 3_500L, minutes = 30),
            done("t3", "g-sleep", at = 8_999L, minutes = 30),
        )
        val allocation = useCase(window, areas, goals, tasks)

        val trend = useCase.trend(buckets, allocation, goals, tasks)

        assertThat(trend.totalMinutes).isEqualTo(allocation.totalMinutes)
        assertThat(trend.buckets.map { it.totalMinutes }).containsExactly(60, 30, 0, 30).inOrder()
        assertThat(trend.maxBucketMinutes).isEqualTo(60)
        assertThat(trend.busiest?.label).isEqualTo("a")
    }

    @Test
    fun `series are the pie's slices, in the pie's order`() {
        val tasks = listOf(
            done("t1", "g-run", at = 1_500L, minutes = 60),
            done("t2", "g-thesis", at = 3_500L, minutes = 30),
        )
        val allocation = useCase(window, areas, goals, tasks)

        val trend = useCase.trend(buckets, allocation, goals, tasks)

        assertThat(trend.series.map { it.name })
            .containsExactlyElementsIn(allocation.slices.map { it.name }).inOrder()
        // Positional: index 0 is Health, which is where bucket "a"'s 60 minutes went.
        assertThat(trend.buckets.first().minutes).containsExactly(60, 0).inOrder()
        assertThat(trend.buckets[1].minutes).containsExactly(0, 30).inOrder()
    }

    @Test
    fun `an area that lost its slice folds into Unassigned here too`() {
        val goalsWithDanglingArea = goals + Goal(id = "g-ghost", lifeAreaIds = listOf("deleted-area"))
        val tasks = listOf(
            done("real", "g-run", at = 1_500L, minutes = 60),
            done("dangling", "g-ghost", at = 3_500L, minutes = 40),
        )
        val allocation = useCase(window, areas, goalsWithDanglingArea, tasks)

        val trend = useCase.trend(buckets, allocation, goalsWithDanglingArea, tasks)

        val unassigned = trend.series.indexOfFirst { it.areaId == null }
        assertThat(unassigned).isAtLeast(0)
        assertThat(trend.buckets[1].minutes[unassigned]).isEqualTo(40)
        assertThat(trend.totalMinutes).isEqualTo(allocation.totalMinutes)
    }

    @Test
    fun `a completion outside every bucket counts in neither chart`() {
        val tasks = listOf(
            done("inside", "g-run", at = 1_500L, minutes = 60),
            done("after", "g-run", at = 9_000L, minutes = 999),
        )
        val allocation = useCase(window, areas, goals, tasks)

        val trend = useCase.trend(buckets, allocation, goals, tasks)

        assertThat(allocation.totalMinutes).isEqualTo(60)
        assertThat(trend.totalMinutes).isEqualTo(60)
    }

    @Test
    fun `an empty window has an empty trend rather than a row of zero columns`() {
        val allocation = useCase(window, areas, goals, tasks = emptyList())

        val trend = useCase.trend(buckets, allocation, goals, emptyList())

        assertThat(trend.isEmpty).isTrue()
        assertThat(trend.buckets).isEmpty()
        assertThat(trend.busiest).isNull()
    }

    @Test
    fun `a window with nothing in some buckets keeps those columns empty, not absent`() {
        val tasks = listOf(done("t1", "g-run", at = 1_500L, minutes = 60))
        val allocation = useCase(window, areas, goals, tasks)

        val trend = useCase.trend(buckets, allocation, goals, tasks)

        assertThat(trend.buckets).hasSize(4)
        assertThat(trend.isEmpty).isFalse()
        assertThat(trend.buckets.drop(1).map { it.totalMinutes }).containsExactly(0, 0, 0)
    }
}
