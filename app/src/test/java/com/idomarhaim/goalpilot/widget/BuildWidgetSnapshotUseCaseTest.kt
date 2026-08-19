package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetSnapshotUseCase
import com.idomarhaim.goalpilot.domain.usecase.TimeAllocation
import com.idomarhaim.goalpilot.domain.usecase.TimeSlice
import com.idomarhaim.goalpilot.domain.usecase.TimeTrend
import com.idomarhaim.goalpilot.domain.usecase.TrendBucket
import com.idomarhaim.goalpilot.domain.usecase.TrendSeries
import org.junit.Test

/**
 * The one seam where widget code meets a domain model (`SESSIONS.md`,
 * 2026-08-15). Everything the tiles render is decided here, so everything the
 * tiles could get wrong about the model is testable here.
 */
class BuildWidgetSnapshotUseCaseTest {

    private val build = BuildWidgetSnapshotUseCase()

    private val user = User(uid = "u1", displayName = "Ido", points = 1_240L)

    private fun allocation(vararg slices: TimeSlice) = TimeAllocation(
        slices = slices.toList(),
        totalMinutes = slices.sumOf { it.minutes },
        completedTasks = slices.sumOf { it.taskCount },
    )

    private fun slice(id: String?, name: String, minutes: Int, total: Int) = TimeSlice(
        areaId = id,
        name = name,
        colorHex = "#0F6FCB",
        iconKey = "flag",
        minutes = minutes,
        taskCount = 1,
        fraction = minutes.toFloat() / total,
    )

    @Test
    fun `no signed-in user yields a snapshot that says so and nothing else`() {
        val snapshot = build(
            capturedAtEpochMillis = 1_000L,
            user = null,
            goals = listOf(Goal(id = "g", title = "Run", targetValue = 4.0, unit = "km")),
            allocation = allocation(slice("a", "Health", 60, 60)),
            trend = TimeTrend(),
        )

        assertThat(snapshot.signedIn).isFalse()
        // Stamped even so: the tile must be able to say when it last looked, and
        // "never" is a different state from "looked, and nobody was signed in".
        assertThat(snapshot.isEmpty).isFalse()
        assertThat(snapshot.goals).isEmpty()
        assertThat(snapshot.areas).isEmpty()
    }

    @Test
    fun `a never-captured snapshot reports empty`() {
        assertThat(build(0L, user, emptyList(), TimeAllocation(), TimeTrend()).isEmpty).isTrue()
    }

    @Test
    fun `the percent placeholder unit is not a measure`() {
        // Goal.unit defaults to "%", which §4.6 records as the map's most-repeated
        // finding at its first site — it labelled the log dialog "Amount (%)" and
        // made a whole feature read as "changing the percentage myself". A goal
        // still carrying it is a goal with nothing counted, so it gets no ring.
        val snapshot = build(
            capturedAtEpochMillis = 1_000L,
            user = user,
            goals = listOf(
                Goal(id = "a", title = "Finish the project", targetValue = 100.0, unit = "%"),
                Goal(id = "b", title = "Run 4 km", targetValue = 4.0, currentValue = 3.2, unit = "km"),
            ),
            allocation = TimeAllocation(),
            trend = TimeTrend(),
        )

        assertThat(snapshot.goals.map { it.id }).containsExactly("b")
        assertThat(snapshot.goalsWithoutMeasure).isEqualTo(1)
    }

    @Test
    fun `a zero target is not a measure either`() {
        // progressFraction already returns 0f there — a number that means nothing
        // rather than a number that means zero, which is exactly what a ring at
        // 0% would assert.
        val snapshot = build(
            1_000L,
            user,
            listOf(Goal(id = "a", title = "Read", targetValue = 0.0, unit = "books")),
            TimeAllocation(),
            TimeTrend(),
        )
        assertThat(snapshot.goals).isEmpty()
        assertThat(snapshot.goalsWithoutMeasure).isEqualTo(1)
    }

    @Test
    fun `an archived goal counts in neither number`() {
        val snapshot = build(
            1_000L,
            user,
            listOf(Goal(id = "a", title = "Old", targetValue = 4.0, unit = "km", isArchived = true)),
            TimeAllocation(),
            TimeTrend(),
        )
        assertThat(snapshot.goals).isEmpty()
        assertThat(snapshot.goalsWithoutMeasure).isEqualTo(0)
    }

    @Test
    fun `the measure reads in the goal's own unit, isolated, without a trailing zero`() {
        val snapshot = build(
            1_000L,
            user,
            listOf(Goal(id = "a", title = "Run", targetValue = 4.0, currentValue = 3.25, unit = "km")),
            TimeAllocation(),
            TimeTrend(),
        )

        val measure = snapshot.goals.single().measure
        assertThat(Bidi.strip(measure)).isEqualTo("3.25 / 4 km")
        assertThat(measure.first()).isEqualTo(Bidi.FSI)
    }

    @Test
    fun `goals are ordered by progress so the size that shows one shows the one with news`() {
        val snapshot = build(
            1_000L,
            user,
            listOf(
                Goal(id = "low", title = "A", targetValue = 10.0, currentValue = 1.0, unit = "km"),
                Goal(id = "high", title = "B", targetValue = 10.0, currentValue = 8.0, unit = "km"),
            ),
            TimeAllocation(),
            TimeTrend(),
        )
        assertThat(snapshot.goals.map { it.id }).containsExactly("high", "low").inOrder()
    }

    @Test
    fun `the trend stays positional over the areas it was built from`() {
        // "The columns add up to the donut" has to be structural. TimeTrend's
        // series ARE the allocation's slices in order, so the snapshot copies the
        // index rather than re-deriving a mapping that could disagree.
        val learning = slice("l", "Learning", 420, 680)
        val health = slice("h", "Health", 260, 680)
        val trend = TimeTrend(
            series = listOf(TrendSeries("l", "Learning", "#8B39C4"), TrendSeries("h", "Health", "#CF3636")),
            buckets = listOf(
                TrendBucket("Sun", listOf(90, 40)),
                TrendBucket("Mon", listOf(330, 220)),
            ),
        )

        val snapshot = build(1_000L, user, emptyList(), allocation(learning, health), trend)

        assertThat(snapshot.areas.map { it.name }).containsExactly("Learning", "Health").inOrder()
        assertThat(snapshot.days.map { it.label }).containsExactly("Sun", "Mon").inOrder()
        assertThat(snapshot.busiestDay?.label).isEqualTo("Mon")
        assertThat(snapshot.days.first().minutes).containsExactly(90, 40).inOrder()
    }

    @Test
    fun `level and points come from the user's own curve, not a second one`() {
        val snapshot = build(1_000L, user, emptyList(), TimeAllocation(), TimeTrend())
        assertThat(snapshot.points).isEqualTo(user.points)
        assertThat(snapshot.level).isEqualTo(user.level)
        assertThat(snapshot.pointsToNextLevel).isEqualTo(user.pointsToNextLevel)
        assertThat(snapshot.levelProgress).isEqualTo(user.levelProgress)
    }

    @Test
    fun `the busiest area is the one that took most of the window`() {
        val snapshot = build(
            1_000L,
            user,
            emptyList(),
            allocation(slice("l", "Learning", 420, 680), slice("h", "Health", 260, 680)),
            TimeTrend(),
        )
        assertThat(snapshot.busiestArea?.name).isEqualTo("Learning")
        assertThat(snapshot.trackedMinutes).isEqualTo(680)
    }
}
