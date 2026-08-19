package com.idomarhaim.goalpilot.widget

import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetTile
import com.idomarhaim.goalpilot.domain.usecase.WidgetStrings

/**
 * The English string table, near enough, with no `Context`.
 *
 * It returns the *shape* of each sentence rather than a marker like `"header"`,
 * because the thing under test is §4.5's size rule — *the disclosure shrinks to
 * the smallest true sentence the tile can hold* — and a fake that returns the
 * same token at every size would make the rule untestable while appearing to
 * test it.
 */
class FakeWidgetStrings : WidgetStrings {

    override fun header(tile: WidgetTile, size: WidgetSize): String = when {
        tile == WidgetTile.GOALS && size == WidgetSize.SMALL -> "Goal"
        tile == WidgetTile.GOALS -> "Your goals"
        tile == WidgetTile.WEEK && size == WidgetSize.LARGE -> "Where the week went"
        tile == WidgetTile.WEEK -> "This week"
        tile == WidgetTile.TREND -> "Day by day"
        tile == WidgetTile.EFFORT && size == WidgetSize.SMALL -> "Effort"
        tile == WidgetTile.EFFORT -> "Effort and outcome"
        else -> "Level"
    }

    override fun asOf(epochMillis: Long) = "as of ${clock(epochMillis)}"
    override fun asOfShort(epochMillis: Long) = clock(epochMillis)

    override val signedOut = "Sign in to GoalPilot"
    override val neverOpened = "Open GoalPilot once to fill this in"
    override val nothingTracked = "Nothing tracked this week yet"
    override val noGoals = "No goals yet"

    override fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    override fun percent(value: Int) = "$value%"
    override fun points(value: Long) = "$value points"

    override val goalsRingMeaning = "Each ring is progress toward that goal's own target."

    override fun goalsWithoutMeasure(count: Int) =
        "Only a goal with a measure can show a ring — $count of yours have none."

    override fun dividedMinutes(size: WidgetSize) = when (size) {
        WidgetSize.SMALL -> "shared time divided"
        WidgetSize.WIDE, WidgetSize.TALL ->
            "Shared time is divided across areas."
        WidgetSize.LARGE ->
            "A 40-minute run serving two areas counts 20 minutes in each, so this adds up to your real week."
    }

    override val tracked = "tracked"

    override fun trendDisclosure(size: WidgetSize) = when (size) {
        WidgetSize.SMALL -> "7 days · same divided minutes"
        else -> "The same divided minutes as the donut, cut into days."
    }

    override fun busiestDay(label: String, duration: String) = "Busiest: $label, $duration"

    override fun effortHeadline(area: String) = "$area took most of your week"
    override val effortNoMeasure = "and it has no measure."
    override fun effortLead(goal: String, percent: String) = "and $goal is at $percent."

    override fun effortDisclosure(size: WidgetSize) = when (size) {
        WidgetSize.SMALL, WidgetSize.WIDE -> "ranked by minutes"
        else -> "Ranked by minutes — the one quantity the app may order."
    }

    override fun toNextLevel(points: Long) = "$points to the next level"

    override fun pointsDisclosure(size: WidgetSize) = when (size) {
        WidgetSize.SMALL -> "points are minutes, scored"
        else -> "Points are your minutes, scored — not a separate score."
    }

    override fun trackedThisWeek(duration: String) = "$duration tracked this week"

    /** Fixed-zone so a test asserting on the stamp does not move with the runner. */
    private fun clock(epochMillis: Long): String {
        val minutesOfDay = ((epochMillis / 60_000L) % (24 * 60)).toInt()
        return "%02d:%02d".format(minutesOfDay / 60, minutesOfDay % 60)
    }
}
