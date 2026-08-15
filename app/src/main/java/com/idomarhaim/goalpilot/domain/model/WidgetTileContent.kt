package com.idomarhaim.goalpilot.domain.model

/**
 * One tile, at one size, fully decided — every string final, every number
 * rounded, every colour chosen.
 *
 * The renderer under `ui/widget/` turns this into Glance composables and makes
 * no decisions of its own. That split is what lets the *design* of §4.5 — which
 * rows survive at `2×2`, which sentence the disclosure shrinks to, what the
 * headline says when the busiest area has no measure — be exercised by ordinary
 * JVM tests, on a subsystem whose real output is a `RemoteViews` bitmap on
 * somebody's launcher.
 */
data class WidgetTileContent(
    val tile: WidgetTile,
    val size: WidgetSize,
    /** The tile's own name, in the header row. */
    val header: String,
    val body: WidgetTileBody,
    /**
     * §4.5's size rule: the smallest true sentence this tile can hold at this
     * size. Blank is legal only where the tile shows nothing derived — see
     * [WidgetTile.derivesNumbers] — or where [body] is a
     * [WidgetTileBody.Message], which has no number to disclose about.
     */
    val disclosure: String,
    /**
     * When these numbers were read, as words — kept apart from [disclosure] so
     * §4.5's size rule can be tested on the sentence alone.
     *
     * Every tile carries it, at every size. A widget is a snapshot (§4.5) and
     * Android refreshes it on a schedule it is free to defer, so a tile showing
     * a number with no stamp is asserting a freshness nobody promised it — the
     * same reason §5.3 makes offline *an as-of stamp, not a connectivity story*.
     * This is the one line the prototype does not have, and it is here because
     * the prototype could not be stale.
     */
    val asOf: String,
    /** Where a tap lands. A widget cannot open a dialog (§4.5), only the app. */
    val destination: WidgetDestination,
)

/**
 * Where a tap on the tile opens the app.
 *
 * Deliberately an enum of *app surfaces* rather than a route string: the
 * nav-graph is `ui/navigation/Destinations.kt`, which belongs to another session
 * today, and a widget that hard-codes a route breaks silently when the route is
 * renamed. `MainActivity` resolves this to a route; until it does, every value
 * opens the app at its start destination, which is the honest degradation —
 * §4.5's contract is "a tap opens the app at a destination", and opening the app
 * is the half that must never fail.
 */
enum class WidgetDestination(val extra: String) {
    HOME("home"),
    GOALS("goals"),
    ANALYTICS("analytics"),
    ;

    companion object {
        const val INTENT_EXTRA = "com.idomarhaim.goalpilot.widget.DESTINATION"

        fun fromExtra(value: String?): WidgetDestination =
            entries.firstOrNull { it.extra == value } ?: HOME
    }
}

/** The shapes a tile body can take. One per rendering strategy, not one per tile. */
sealed interface WidgetTileBody {

    /**
     * Nothing to show — nobody signed in, or the snapshot has never been
     * captured, or the window really is empty.
     *
     * A tile with nothing to say says so in words rather than drawing an empty
     * chart, which is §4.4's *a card with nothing to say hides itself* as far as
     * a widget can honour it: a widget cannot remove itself from the launcher,
     * so the nearest true thing is to stop pretending it has data.
     */
    data class Message(val text: String) : WidgetTileBody


    /** A ring, its centre label, and supporting lines beside or under it. */
    data class Ring(
        val fraction: Float,
        val centre: String,
        val colorHex: String,
        val lines: List<String> = emptyList(),
    ) : WidgetTileBody

    /** A list of goals, each with its own ring. */
    data class RingRows(val rows: List<WidgetRingRow>) : WidgetTileBody

    /**
     * The time donut, with the legend §4.4 requires instead of a legend
     * round-trip: every entry is named in words and carries its percentage.
     * Empty [legend] means the size had no room and the donut stands alone.
     */
    data class Donut(
        val slices: List<WidgetArea>,
        val centre: String,
        val centreCaption: String,
        val legend: List<WidgetArea>,
    ) : WidgetTileBody

    /** The stacked trend. [series] indexes every column's `minutes` list. */
    data class Columns(
        val series: List<WidgetArea>,
        val columns: List<WidgetDay>,
        val showLabels: Boolean,
    ) : WidgetTileBody

    /**
     * Effort against outcome (§4.4).
     *
     * The form was **forced, not chosen**: a percentage is a fraction of its own
     * target, so ranking areas by movement would partly rank how modest the
     * user's goals are. So the rows are ordered by the one quantity the app may
     * order — minutes — and everything else is *named*.
     */
    data class EffortRows(
        val rows: List<WidgetEffortRow>,
        val headline: String,
    ) : WidgetTileBody
}

/** One goal in [WidgetTileBody.RingRows]. */
data class WidgetRingRow(
    val title: String,
    val measure: String,
    val percent: Int,
    val colorHex: String,
) {
    val fraction: Float get() = (percent / 100f).coerceIn(0f, 1f)
}

/**
 * One life area in [WidgetTileBody.EffortRows]: the minutes it took, drawn to
 * scale, and the goals it holds — named, never ranked.
 */
data class WidgetEffortRow(
    val name: String,
    val colorHex: String,
    /** Already formatted — `"7h 0m"`, bidi-isolated. */
    val effort: String,
    /** Share of the busiest row's minutes, 0f..1f, for the bar. */
    val effortFraction: Float,
    /** What stands where, in the area's own goals. Empty is a fact, not a gap. */
    val outcomes: List<String>,
)
