package com.idomarhaim.goalpilot.domain.model

import kotlinx.serialization.Serializable

/**
 * Everything the widget pack renders, flattened into one value.
 *
 * **This is the seam, and it is load-bearing in two directions.**
 *
 * *Outward:* §4.5 confirms a widget is **not a live screen** — Android renders a
 * snapshot and refreshes on a schedule. Modelling that as a snapshot rather than
 * as repositories the tiles reach into keeps the honest constraint visible in
 * the type: [capturedAtEpochMillis] is a fact about the picture on the home
 * screen, and the tiles state it rather than implying the numbers are live.
 *
 * *Inward:* it is the only place widget code meets a domain model.
 * `BuildWidgetSnapshotUseCase` reads [Goal], [LifeArea], [User] and the time
 * allocation; nothing under `ui/widget/` reads any of them. That was chosen
 * deliberately while `d2-life-area-route` was mid-rename of
 * `Goal.lifeAreaId` → `lifeAreaIds` (`SESSIONS.md`, 2026-08-15): a model change
 * lands in one function instead of across five tiles and twenty layouts.
 *
 * Every field is a finished, presentable quantity. There is no arithmetic left
 * to do downstream, because arithmetic done in a renderer is arithmetic nobody
 * can test.
 */
@Serializable
data class WidgetSnapshot(
    /** When these numbers were read. Zero means "never" — see [isEmpty]. */
    val capturedAtEpochMillis: Long = 0L,
    /** False when nobody is signed in on this device; every tile shows its sign-in state. */
    val signedIn: Boolean = false,

    // ── Level and points (§4.4) ──────────────────────────────────
    val level: Int = 1,
    val points: Long = 0L,
    /** 0f..1f through the current level. */
    val levelProgress: Float = 0f,
    val pointsToNextLevel: Long = 0L,

    // ── The week (§4.4, via TimeAllocationUseCase) ───────────────
    /** Total minutes the window accounted for, across every area. */
    val trackedMinutes: Int = 0,
    /** Biggest first — the donut's slice order and the trend's stacking order. */
    val areas: List<WidgetArea> = emptyList(),
    /** One per day of the window; [WidgetDay.minutes] is positional over [areas]. */
    val days: List<WidgetDay> = emptyList(),

    // ── Goals (§4.4) ─────────────────────────────────────────────
    /** Measured goals only, furthest along first. */
    val goals: List<WidgetGoal> = emptyList(),
    /**
     * Live goals this snapshot could not draw a ring for, because they carry no
     * measure to draw one against (`C7` permits exactly that). The count is what
     * makes the `goals` tile's disclosure true rather than decorative.
     */
    val goalsWithoutMeasure: Int = 0,
) {
    /** Nothing has ever been captured — the tile shows its "open GoalPilot" state. */
    val isEmpty: Boolean get() = capturedAtEpochMillis <= 0L

    /** The area that took most of the window, or null when nothing was tracked. */
    val busiestArea: WidgetArea? get() = areas.maxByOrNull { it.minutes }

    /** The heaviest day, for the trend tile's one-line summary. */
    val busiestDay: WidgetDay? get() = days.filter { it.totalMinutes > 0 }.maxByOrNull { it.totalMinutes }

    /**
     * The goals filed under one area — **all** of them, including a goal that
     * also serves another.
     *
     * §4.7 is explicit that a success counts *in full* in every area the work
     * serves while only its **minutes** divide, and that the asymmetry is the
     * point rather than an accident. So this is not `first()`: a goal serving
     * Health and Career is named under both, and only the effort bar beside it
     * is split.
     */
    fun goalsIn(areaId: String?): List<WidgetGoal> = goals.filter {
        if (areaId == null) it.areaIds.isEmpty() else areaId in it.areaIds
    }
}

/**
 * One life area's share of the window.
 *
 * Carries its own name and colour so neither the donut nor the trend has to go
 * back to a list to draw itself — the same reason `TrendSeries` does, and what
 * makes "the columns add up to the donut" structural instead of coincidental.
 */
@Serializable
data class WidgetArea(
    val id: String?,
    val name: String,
    val colorHex: String,
    val minutes: Int,
    /** Share of [WidgetSnapshot.trackedMinutes], 0f..1f. */
    val fraction: Float,
) {
    val percent: Int get() = Math.round(fraction * 100f)
}

/**
 * One column of the trend tile. [minutes] is positional over
 * [WidgetSnapshot.areas] rather than a map, so the stacking order is literally
 * the donut's slice order and cannot drift from it.
 */
@Serializable
data class WidgetDay(
    val label: String,
    val minutes: List<Int>,
) {
    val totalMinutes: Int get() = minutes.sum()
}

/**
 * One goal, already reduced to what a tile can draw.
 *
 * [measure] is the goal's own words for where it stands — `"3.2 / 4 km"` — and
 * is **blank when the unit is the `"%"` placeholder**, because then the ring's
 * own percentage already says it and a second copy of one number is §0.3's
 * finding in miniature. It is bidi-isolated at construction (§4.8): it mixes
 * Latin digits with a unit that may be Hebrew, which is precisely the run the
 * bidi algorithm reverses inside an RTL paragraph.
 */
@Serializable
data class WidgetGoal(
    val id: String,
    val title: String,
    /**
     * Every life area this goal serves — plural since §1.2 made the edge plural.
     * Empty is *unfiled*, and it is a real state rather than a missing one.
     */
    val areaIds: List<String>,
    val colorHex: String,
    /** 0..100, already rounded. */
    val percent: Int,
    val measure: String,
) {
    val fraction: Float get() = (percent / 100f).coerceIn(0f, 1f)
}
