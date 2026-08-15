package com.idomarhaim.goalpilot.ui.widget

import android.content.Context
import android.text.format.DateFormat
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetTile
import com.idomarhaim.goalpilot.domain.usecase.WidgetStrings
import java.util.Date

/**
 * [WidgetStrings] backed by `R.string`, and the only place the widget pack knows
 * what a resource is.
 *
 * The [context] handed in must be the one Glance is rendering with, not the
 * application context: a widget is inflated by the **launcher**, and on Android
 * 13+ a per-app language override lives on the context rather than on the
 * process. Reading strings off the application context would render the widget
 * in the system language while the app itself is in the user's — the same class
 * of split §4.9 names when it puts language on the device rather than the
 * account.
 */
class AndroidWidgetStrings(private val context: Context) : WidgetStrings {

    override fun header(tile: WidgetTile, size: WidgetSize): String = context.getString(
        when (tile) {
            WidgetTile.GOALS ->
                if (size == WidgetSize.SMALL) R.string.gp_widget_goal else R.string.gp_widget_goals

            WidgetTile.WEEK ->
                if (size == WidgetSize.LARGE) R.string.gp_widget_week_large else R.string.gp_widget_week

            WidgetTile.TREND -> R.string.gp_widget_trend

            WidgetTile.EFFORT ->
                if (size == WidgetSize.SMALL) R.string.gp_widget_effort_short else R.string.gp_widget_effort

            WidgetTile.LEVEL ->
                if (size == WidgetSize.LARGE) R.string.gp_widget_level_large else R.string.gp_widget_level
        },
    )

    override fun asOf(epochMillis: Long): String =
        context.getString(R.string.gp_widget_as_of, asOfShort(epochMillis))

    /**
     * The clock alone, in the device's own 12/24-hour setting.
     *
     * [DateFormat.getTimeFormat] rather than a pattern string: a hard-coded
     * `HH:mm` renders 13:00 to a user whose phone says 1 PM, which is the kind
     * of small wrongness a home screen makes permanent.
     */
    override fun asOfShort(epochMillis: Long): String =
        DateFormat.getTimeFormat(context).format(Date(epochMillis))

    override val signedOut: String get() = context.getString(R.string.gp_widget_signed_out)
    override val neverOpened: String get() = context.getString(R.string.gp_widget_never_opened)
    override val nothingTracked: String get() = context.getString(R.string.gp_widget_nothing_tracked)
    override val noGoals: String get() = context.getString(R.string.gp_widget_no_goals)

    override fun duration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        return if (h > 0) {
            context.getString(R.string.gp_widget_duration_hm, h, m)
        } else {
            context.getString(R.string.gp_widget_duration_m, m)
        }
    }

    override fun percent(value: Int): String = context.getString(R.string.gp_widget_percent, value)

    override fun points(value: Long): String =
        context.getString(R.string.gp_widget_points, formatCount(value))

    override val goalsRingMeaning: String
        get() = context.getString(R.string.gp_widget_goals_ring_meaning)

    override fun goalsWithoutMeasure(count: Int): String =
        context.resources.getQuantityString(R.plurals.gp_widget_goals_without_measure, count, count)

    override fun dividedMinutes(size: WidgetSize): String = context.getString(
        when (size) {
            WidgetSize.SMALL -> R.string.gp_widget_divided_small
            WidgetSize.WIDE, WidgetSize.TALL -> R.string.gp_widget_divided_mid
            WidgetSize.LARGE -> R.string.gp_widget_divided_large
        },
    )

    override val tracked: String get() = context.getString(R.string.gp_widget_tracked)

    override fun trendDisclosure(size: WidgetSize): String = context.getString(
        if (size == WidgetSize.SMALL) R.string.gp_widget_trend_small else R.string.gp_widget_trend_mid,
    )

    override fun busiestDay(label: String, duration: String): String =
        context.getString(R.string.gp_widget_busiest, label, duration)

    override fun effortHeadline(area: String): String =
        context.getString(R.string.gp_widget_effort_headline, area)

    override val effortNoMeasure: String
        get() = context.getString(R.string.gp_widget_effort_no_measure)

    override fun effortLead(goal: String, percent: String): String =
        context.getString(R.string.gp_widget_effort_lead, goal, percent)

    override fun effortDisclosure(size: WidgetSize): String = context.getString(
        when (size) {
            WidgetSize.SMALL, WidgetSize.WIDE -> R.string.gp_widget_effort_small
            else -> R.string.gp_widget_effort_large
        },
    )

    override fun toNextLevel(points: Long): String =
        context.getString(R.string.gp_widget_to_next_level, formatCount(points))

    override fun pointsDisclosure(size: WidgetSize): String = context.getString(
        if (size == WidgetSize.SMALL) R.string.gp_widget_points_small else R.string.gp_widget_points_large,
    )

    override fun trackedThisWeek(duration: String): String =
        context.getString(R.string.gp_widget_tracked_this_week, duration)

    /**
     * Grouped in the locale's own digits and separator — `1,240` in English,
     * `1,240` in Hebrew (which uses Latin digits and a comma), and correct
     * wherever it does not.
     */
    private fun formatCount(value: Long): String =
        java.text.NumberFormat.getIntegerInstance(
            androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)[0]
                ?: java.util.Locale.getDefault(),
        ).format(value)
}
