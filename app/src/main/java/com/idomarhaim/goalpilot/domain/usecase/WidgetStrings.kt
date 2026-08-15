package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetTile

/**
 * Every word the widget pack says, behind an interface.
 *
 * [BuildWidgetTileUseCase] decides *which* sentence a tile shows at a size —
 * that is §4.5's size rule and it is the part worth testing — while the words
 * themselves come from `R.string` through the implementation in `ui/widget/`.
 * Without this split the use case would need a `Context` and the size rule could
 * only be checked on a device.
 *
 * It is also what makes §0.8 reachable: the same tile logic runs against a
 * Hebrew string table with no branch of its own, so *"no design is finished
 * until it has been seen in Hebrew"* costs a `values-he/` file rather than a
 * second implementation.
 */
interface WidgetStrings {

    // ── Chrome ───────────────────────────────────────────────────

    /**
     * The tile's name in its header. Takes the size because a `2×2` says
     * *Goal* where a `4×4` says *Your goals* — the header is the first thing
     * that has to survive being narrower than it was drawn.
     */
    fun header(tile: WidgetTile, size: WidgetSize): String

    /** "as of 14:32" — appended to every disclosure line. */
    fun asOf(epochMillis: Long): String

    /** Short form for `2×2`, where the sentence beside it needs the room. */
    fun asOfShort(epochMillis: Long): String

    // ── Empty states ─────────────────────────────────────────────

    val signedOut: String
    val neverOpened: String
    val nothingTracked: String
    val noGoals: String

    // ── Formatting ───────────────────────────────────────────────

    /** `"7h 0m"` / `"45m"`, bidi-isolated by the caller. */
    fun duration(minutes: Int): String

    fun percent(value: Int): String

    fun points(value: Long): String

    // ── goals ────────────────────────────────────────────────────

    /** Shown when the tile drew every goal it has. */
    val goalsRingMeaning: String

    /** Shown when [count] live goals carry no measure and so got no ring. */
    fun goalsWithoutMeasure(count: Int): String

    // ── week ─────────────────────────────────────────────────────

    /**
     * `C17` §3's disclosure, at the length this size can hold — three words at
     * `2×2`, a clause at `4×2`/`2×4`, the whole sentence at `4×4`. The tile is
     * dividing shared minutes across areas and must say so at every size.
     */
    fun dividedMinutes(size: WidgetSize): String

    /** Under the donut's total. */
    val tracked: String

    // ── trend ────────────────────────────────────────────────────

    fun trendDisclosure(size: WidgetSize): String

    fun busiestDay(label: String, duration: String): String

    // ── effort ───────────────────────────────────────────────────

    /** "Learning took most of your week". */
    fun effortHeadline(area: String): String

    /** "…and it has no measure" — the honest headline §4.4 says the data forced. */
    val effortNoMeasure: String

    /** "…and {goal} is at {percent}" when the area does have something measured. */
    fun effortLead(goal: String, percent: String): String

    fun effortDisclosure(size: WidgetSize): String

    // ── level ────────────────────────────────────────────────────

    fun toNextLevel(points: Long): String

    /**
     * "Points are your minutes, scored — not a separate score", at the length
     * this size can hold. Points are `round(minutes/3) × difficulty` (§4.4), so
     * this tile shows a derived number and owes the sentence at every size.
     */
    fun pointsDisclosure(size: WidgetSize): String

    /** "7h 0m tracked this week" — the largest size's third line. */
    fun trackedThisWeek(duration: String): String
}
