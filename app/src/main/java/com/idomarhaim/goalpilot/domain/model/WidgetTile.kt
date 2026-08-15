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
         * The dp at which a widget stops being two cells and starts being four.
         *
         * A launcher cell is nominally 70 dp with 30 dp of total margin, so two
         * cells land near 110 dp and four near 250 dp. 180 dp sits between them
         * with room on both sides, which matters because a `3×3` — legal on
         * every launcher and impossible to design for — has to resolve to
         * *something*, and resolving it to the larger layout is the choice that
         * cannot clip: a wide layout in a narrow cell truncates text, a narrow
         * layout in a wide cell only wastes space.
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
