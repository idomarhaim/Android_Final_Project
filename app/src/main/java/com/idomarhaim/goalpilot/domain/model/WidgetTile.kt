package com.idomarhaim.goalpilot.domain.model

/**
 * The cards §4.5 ships as home-screen widgets.
 *
 * §4.5 names **seven**. Five are here; `decisions` and `today` are not, and the
 * reason is data rather than design: both are schedule surfaces, and §2
 * scheduling is unbuilt — `Task` carries no due time at all, and §7.2 already
 * records that `GoogleTasksClient.kt:145` parses Google's `due` into a field no
 * other line in the repo reads. A "4 decisions waiting" tile computed from
 * nothing would be the map's most-repeated finding (§0.3, *a second number that
 * quietly disagrees*) manufactured on purpose, so the two are filed in
 * `TODO/TODO_OPTIONAL/Presentation.TODO.optional.md` rather than faked.
 *
 * [derivesNumbers] carries §4.5's size rule. Revision 1 banned a chart whose
 * honesty depends on a footnote; Ido overturned that and it was re-cut as
 * *the disclosure shrinks to the smallest true sentence the tile can hold, and
 * no size ships without one*. The test is not "is this a chart" but **does this
 * tile show a derived or divided number** — divided minutes on [WEEK], [TREND]
 * and [EFFORT]; points, which are minutes scored, on [LEVEL]. [GOALS] shows a
 * goal's own progress against its own target, which is neither, so it owes a
 * disclosure only at the sizes where it hides a goal it could not draw.
 * `WidgetDisclosureTest` is what keeps that honest.
 */
enum class WidgetTile(
    val key: String,
    val derivesNumbers: Boolean,
) {
    /** Rings against each goal's own target. */
    GOALS("goals", derivesNumbers = false),

    /** The time donut: where the week went, across life areas. */
    WEEK("week", derivesNumbers = true),

    /** The same minutes as [WEEK], cut into days. */
    TREND("trend", derivesNumbers = true),

    /** Effort against outcome — minutes ranked, everything else named (§4.4). */
    EFFORT("effort", derivesNumbers = true),

    /** Level and points (§4.4: the points hero demoted to a ring). */
    LEVEL("level", derivesNumbers = true),
    ;

    companion object {
        fun fromKey(key: String?): WidgetTile? = entries.firstOrNull { it.key == key }
    }
}

/**
 * The four size classes §4.5 ships, named by shape rather than by cell count
 * because **the launcher decides the real dp** a `2×2` or `4×4` occupies and it
 * varies by device and launcher. So the tile picks its layout from the space it
 * was actually given — see [of] — and every layout has to survive being smaller
 * than it was drawn.
 */
enum class WidgetSize(val cells: String) {
    /** `2×2` — one number, or one ring. */
    SMALL("2x2"),

    /** `4×2` — a number beside a short list, or a chart beside a legend. */
    WIDE("4x2"),

    /** `2×4` — a narrow column: a chart over a list. */
    TALL("2x4"),

    /** `4×4` — the full card, and the only size that gets the full sentence. */
    LARGE("4x4"),
    ;

    val isWide: Boolean get() = this == WIDE || this == LARGE
    val isTall: Boolean get() = this == TALL || this == LARGE

    companion object {
        /**
         * The midpoint between the two candidate widths the pack declares.
         *
         * This is **not** a guess about real launcher cells, and the difference
         * matters because the first version was exactly that guess and it was
         * wrong. `Observed:` 2026-08-16, `Pixel_10_Pro_XL`, API 37 — a nominal
         * `2×2` measured about **190 dp**, past the old 180 dp threshold, so the
         * smallest tile drew the largest layout and the four-size ladder
         * collapsed to one rung. §4.5 says plainly that dp per cell *varies by
         * device and launcher*, so no fixed number could have been right.
         *
         * `GoalPilotWidget` now uses `SizeMode.Responsive` and declares its four
         * design sizes (110 / 250 dp per axis). The launcher does the matching
         * against whatever it really granted, and [LocalSize] hands back one of
         * those four exactly — so this only has to separate 110 from 250, which
         * a midpoint does with 70 dp of margin on either side.
         */
        const val WIDE_THRESHOLD_DP = 180

        fun of(widthDp: Int, heightDp: Int): WidgetSize {
            val wide = widthDp >= WIDE_THRESHOLD_DP
            val tall = heightDp >= WIDE_THRESHOLD_DP
            return when {
                wide && tall -> LARGE
                wide -> WIDE
                tall -> TALL
                else -> SMALL
            }
        }
    }
}
