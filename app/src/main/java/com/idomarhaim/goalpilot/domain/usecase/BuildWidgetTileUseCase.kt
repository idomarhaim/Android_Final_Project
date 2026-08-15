package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.WidgetDestination
import com.idomarhaim.goalpilot.domain.model.WidgetEffortRow
import com.idomarhaim.goalpilot.domain.model.WidgetRingRow
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetSnapshot
import com.idomarhaim.goalpilot.domain.model.WidgetTile
import com.idomarhaim.goalpilot.domain.model.WidgetTileBody
import com.idomarhaim.goalpilot.domain.model.WidgetTileContent
import javax.inject.Inject

/**
 * Turns a [WidgetSnapshot] into the finished tile for one [WidgetTile] at one
 * [WidgetSize] — which rows survive, which sentence the disclosure shrinks to,
 * what the headline says when the busiest area has nothing measured.
 *
 * **This is where §4.5's size rule lives**, and having it here rather than in the
 * renderer is the whole reason it can be checked. Revision 1 of `C12` banned a
 * chart whose honesty depends on a footnote; Ido overturned that and it was
 * re-cut as a size rule — *the disclosure shrinks to the smallest true sentence
 * the tile can hold, and no size ships without one*. A rule that exists only
 * inside a `RemoteViews` tree is a rule nobody can test, so every decision it
 * makes is a value returned from here and `WidgetTileTest` reads them back.
 *
 * Pure, and deliberately ignorant of Glance, `Context` and `R` — the words come
 * through [WidgetStrings].
 */
class BuildWidgetTileUseCase @Inject constructor() {

    operator fun invoke(
        snapshot: WidgetSnapshot,
        tile: WidgetTile,
        size: WidgetSize,
        strings: WidgetStrings,
    ): WidgetTileContent {
        // Two refusals before any tile-specific work, in the order they become
        // true. Each is a different fact and deserves its own sentence: "open
        // GoalPilot once" and "sign in" are not the same news, and a tile that
        // conflates them sends the user to the wrong place.
        val body: WidgetTileBody
        val disclosure: String
        when {
            snapshot.isEmpty -> {
                body = WidgetTileBody.Message(strings.neverOpened)
                disclosure = ""
            }

            !snapshot.signedIn -> {
                body = WidgetTileBody.Message(strings.signedOut)
                disclosure = ""
            }

            else -> {
                val built = when (tile) {
                    WidgetTile.GOALS -> goals(snapshot, size, strings)
                    WidgetTile.WEEK -> week(snapshot, size, strings)
                    WidgetTile.TREND -> trend(snapshot, size, strings)
                    WidgetTile.EFFORT -> effort(snapshot, size, strings)
                    WidgetTile.LEVEL -> level(snapshot, size, strings)
                }
                body = built.first
                // A tile with nothing to draw has no number to disclose about, and
                // a footnote under an empty state is a footnote about nothing.
                disclosure = if (built.first is WidgetTileBody.Message) "" else built.second
            }
        }

        return WidgetTileContent(
            tile = tile,
            size = size,
            header = strings.header(tile, size),
            body = body,
            disclosure = disclosure,
            asOf = when {
                body is WidgetTileBody.Message -> ""
                size == WidgetSize.SMALL -> strings.asOfShort(snapshot.capturedAtEpochMillis)
                else -> strings.asOf(snapshot.capturedAtEpochMillis)
            },
            destination = tile.destination,
        )
    }

    // ── goals ────────────────────────────────────────────────────

    /**
     * Rings against each goal's own target.
     *
     * The one tile whose numbers are neither derived nor divided, so its
     * disclosure is conditional rather than mandatory — and the condition is
     * *did this tile fail to draw something the user has*. At `2×2` the header
     * says **Goal**, singular, which is why showing one of four is not a
     * concealment there; from `4×2` up the tile claims the plural and owes the
     * count it could not draw.
     */
    private fun goals(
        snapshot: WidgetSnapshot,
        size: WidgetSize,
        strings: WidgetStrings,
    ): Pair<WidgetTileBody, String> {
        val goals = snapshot.goals
        if (goals.isEmpty()) {
            // Having goals but none of them measurable is different news from
            // having no goals at all, and the first tells the user what to do.
            val message =
                if (snapshot.goalsWithoutMeasure > 0) strings.goalsWithoutMeasure(snapshot.goalsWithoutMeasure)
                else strings.noGoals
            return WidgetTileBody.Message(message) to ""
        }

        val rows = goals.take(size.goalRowCount()).map {
            WidgetRingRow(
                title = it.title,
                measure = it.measure,
                percent = it.percent,
                colorHex = it.colorHex,
            )
        }

        val hidden = snapshot.goalsWithoutMeasure
        val disclosure = when {
            hidden > 0 -> strings.goalsWithoutMeasure(hidden)
            size == WidgetSize.SMALL || size == WidgetSize.WIDE -> ""
            else -> strings.goalsRingMeaning
        }

        val body = if (size == WidgetSize.SMALL) {
            val top = rows.first()
            WidgetTileBody.Ring(
                fraction = top.fraction,
                centre = strings.percent(top.percent),
                colorHex = top.colorHex,
                lines = listOf(top.title),
            )
        } else {
            WidgetTileBody.RingRows(rows)
        }
        return body to disclosure
    }

    private fun WidgetSize.goalRowCount(): Int = when (this) {
        WidgetSize.SMALL -> 1
        WidgetSize.WIDE -> 2
        WidgetSize.TALL -> 4
        WidgetSize.LARGE -> 4
    }

    // ── week ─────────────────────────────────────────────────────

    /**
     * The time donut. It divides shared minutes across areas, so `C17` §3's
     * disclosure is owed at **every** size — three words at `2×2`, a clause in
     * the middle, the whole sentence at `4×4`.
     */
    private fun week(
        snapshot: WidgetSnapshot,
        size: WidgetSize,
        strings: WidgetStrings,
    ): Pair<WidgetTileBody, String> {
        if (snapshot.areas.isEmpty() || snapshot.trackedMinutes <= 0) {
            return WidgetTileBody.Message(strings.nothingTracked) to ""
        }
        val body = WidgetTileBody.Donut(
            slices = snapshot.areas,
            centre = strings.duration(snapshot.trackedMinutes).bidiIsolated(),
            centreCaption = if (size == WidgetSize.LARGE) strings.tracked else "",
            legend = snapshot.areas.take(size.legendCount()),
        )
        return body to strings.dividedMinutes(size)
    }

    /**
     * `2×2` gets no legend at all — the donut has to stay big enough to read and
     * there is no room for both. §4.4 requires every area to be *named in words*
     * rather than left to a colour, and the way that survives here is that the
     * size which cannot name them does not claim to: it shows the total, not the
     * split, and the split is one tap away.
     */
    private fun WidgetSize.legendCount(): Int = when (this) {
        WidgetSize.SMALL -> 0
        WidgetSize.WIDE -> 4
        WidgetSize.TALL -> 5
        WidgetSize.LARGE -> 6
    }

    // ── trend ────────────────────────────────────────────────────

    private fun trend(
        snapshot: WidgetSnapshot,
        size: WidgetSize,
        strings: WidgetStrings,
    ): Pair<WidgetTileBody, String> {
        if (snapshot.days.none { it.totalMinutes > 0 }) {
            return WidgetTileBody.Message(strings.nothingTracked) to ""
        }
        val body = WidgetTileBody.Columns(
            series = snapshot.areas,
            columns = snapshot.days,
            // A day label under a bar needs roughly 24 dp of column. At 2×2 seven
            // columns cannot have it, and a label that renders as a smear is worse
            // than a chart that admits it has no axis.
            showLabels = size != WidgetSize.SMALL,
        )

        val base = strings.trendDisclosure(size)
        // The busiest day is the one thing the columns cannot say for themselves —
        // the eye finds the tallest bar but cannot read its total — so it is added
        // wherever the line has room for a second clause.
        val busiest = snapshot.busiestDay
        val disclosure = if (size.isTall && busiest != null) {
            "$base · ${strings.busiestDay(busiest.label, strings.duration(busiest.totalMinutes).bidiIsolated())}"
        } else {
            base
        }
        return body to disclosure
    }

    // ── effort ───────────────────────────────────────────────────

    /**
     * Effort against outcome (§4.4), in the form the data forced.
     *
     * A percentage is a fraction of *its own* target, so ranking areas by how
     * much they moved would partly rank how modest the user's goals are. The app
     * therefore orders the one quantity it may order — minutes — and **names**
     * the rest. The headline is the honest one §4.4 found: when the area that
     * took most of the week has nothing measured, the tile says so, and then
     * nothing on it claims that area did or did not progress.
     */
    private fun effort(
        snapshot: WidgetSnapshot,
        size: WidgetSize,
        strings: WidgetStrings,
    ): Pair<WidgetTileBody, String> {
        val busiest = snapshot.busiestArea
        if (busiest == null || snapshot.trackedMinutes <= 0) {
            return WidgetTileBody.Message(strings.nothingTracked) to ""
        }

        val lead = snapshot.goalsIn(busiest.id).firstOrNull()
        val headline = buildString {
            append(strings.effortHeadline(busiest.name))
            append(' ')
            append(
                if (lead == null) strings.effortNoMeasure
                else strings.effortLead(lead.title, strings.percent(lead.percent)),
            )
        }

        val top = snapshot.areas.take(size.effortRowCount())
        // Bars are drawn against the busiest row in view, not against the window
        // total: with six areas every bar would be a stub and the comparison the
        // row exists to make would be unreadable.
        val scale = top.maxOfOrNull { it.minutes }?.takeIf { it > 0 } ?: 1
        val rows = top.map { area ->
            WidgetEffortRow(
                name = area.name,
                colorHex = area.colorHex,
                effort = strings.duration(area.minutes).bidiIsolated(),
                effortFraction = area.minutes.toFloat() / scale,
                outcomes = snapshot.goalsIn(area.id)
                    .take(size.effortOutcomeCount())
                    .map { "${it.title} · ${strings.percent(it.percent)}".bidiIsolated() },
            )
        }

        return WidgetTileBody.EffortRows(rows = rows, headline = headline) to
            strings.effortDisclosure(size)
    }

    /** `2×2` carries the headline alone — one sentence is all it can hold honestly. */
    private fun WidgetSize.effortRowCount(): Int = when (this) {
        WidgetSize.SMALL -> 0
        WidgetSize.WIDE -> 2
        WidgetSize.TALL -> 3
        WidgetSize.LARGE -> 4
    }

    private fun WidgetSize.effortOutcomeCount(): Int = if (this == WidgetSize.LARGE) 2 else 1

    // ── level ────────────────────────────────────────────────────

    private fun level(
        snapshot: WidgetSnapshot,
        size: WidgetSize,
        strings: WidgetStrings,
    ): Pair<WidgetTileBody, String> {
        val lines = buildList {
            if (size != WidgetSize.SMALL) add(strings.points(snapshot.points).bidiIsolated())
            add(strings.toNextLevel(snapshot.pointsToNextLevel).bidiIsolated())
            if (size == WidgetSize.LARGE && snapshot.trackedMinutes > 0) {
                add(strings.trackedThisWeek(strings.duration(snapshot.trackedMinutes)).bidiIsolated())
            }
        }
        val body = WidgetTileBody.Ring(
            fraction = snapshot.levelProgress,
            centre = snapshot.level.toString(),
            colorHex = SKIN_ACCENT,
            lines = lines,
        )
        return body to strings.pointsDisclosure(size)
    }

    companion object {
        /**
         * The level ring is the one element that is **not** a life area, so it
         * takes the skin's own accent rather than a categorical hue — the same
         * reason §4.7 keeps outcome state off colour entirely.
         *
         * A sentinel rather than a hex because the accent is not knowable here:
         * it belongs to the selected `AppSkin`, and §4.1's enforcement note is
         * exactly that *a skin picker no material reads is a control that does
         * nothing and it looks correct in source*. `ui/widget/WidgetPalette.kt`
         * resolves it against the live skin, and `WidgetTileTest` asserts it is
         * never rendered as a colour.
         */
        const val SKIN_ACCENT = "@accent"
    }
}

/**
 * Where each tile's tap lands.
 *
 * Not a nav route: routes live in `ui/navigation/Destinations.kt` and a widget
 * that hard-codes one breaks silently when the route is renamed — and it is
 * silent because nobody re-tests a home screen after a refactor.
 */
private val WidgetTile.destination: WidgetDestination
    get() = when (this) {
        WidgetTile.GOALS -> WidgetDestination.GOALS
        WidgetTile.WEEK, WidgetTile.TREND, WidgetTile.EFFORT -> WidgetDestination.ANALYTICS
        WidgetTile.LEVEL -> WidgetDestination.HOME
    }
