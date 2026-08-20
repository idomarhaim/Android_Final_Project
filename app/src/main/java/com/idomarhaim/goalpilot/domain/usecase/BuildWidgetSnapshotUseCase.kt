package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.User
import com.idomarhaim.goalpilot.domain.model.WidgetArea
import com.idomarhaim.goalpilot.domain.model.WidgetDay
import com.idomarhaim.goalpilot.domain.model.WidgetGoal
import com.idomarhaim.goalpilot.domain.model.WidgetSnapshot
import javax.inject.Inject

/**
 * Reduces the live model to the flat [WidgetSnapshot] the tiles render.
 *
 * **This is the only file under the widget pack that reads a domain model**, and
 * that is deliberate rather than tidy. `SESSIONS.md` records why: it was written
 * while `d2-life-area-route` was renaming `Goal.lifeAreaId` → `lifeAreaIds`, and
 * a rename that reaches one function is a different thing from one that reaches
 * five tiles and twenty layouts.
 *
 * Pure — no Android, no I/O, no clock. [capturedAtEpochMillis] is passed in for
 * the same reason `TimeAllocationUseCase` takes its window: a use case that
 * reads the clock cannot be tested at a boundary.
 *
 * It takes the **finished** [TimeAllocation] and [TimeTrend] rather than tasks,
 * so the widget's donut is the analytics screen's donut by construction and not
 * by two implementations agreeing. That is the same move `TimeAllocationUseCase.trend`
 * makes for its own columns, and it is why "the trend adds up to the donut" is a
 * property here too rather than something to re-check.
 */
class BuildWidgetSnapshotUseCase @Inject constructor() {

    operator fun invoke(
        capturedAtEpochMillis: Long,
        user: User?,
        goals: List<Goal>,
        allocation: TimeAllocation,
        trend: TimeTrend,
    ): WidgetSnapshot {
        if (user == null) {
            return WidgetSnapshot(
                capturedAtEpochMillis = capturedAtEpochMillis,
                signedIn = false,
            )
        }

        val areas = allocation.slices.map { slice ->
            WidgetArea(
                id = slice.areaId,
                name = slice.name,
                colorHex = slice.colorHex,
                minutes = slice.minutes,
                fraction = slice.fraction,
            )
        }

        // Positional over `areas`, which is exactly what TimeTrend already
        // guarantees: its series ARE the allocation's slices, in order. Re-deriving
        // the mapping here would be a second rule that could disagree with the first.
        val days = trend.buckets.map { WidgetDay(label = it.label, minutes = it.minutes) }

        val live = goals.filterNot { it.isArchived }
        val measured = live.filter { it.hasMeasure }

        return WidgetSnapshot(
            capturedAtEpochMillis = capturedAtEpochMillis,
            signedIn = true,
            level = user.level,
            points = user.points,
            levelProgress = user.levelProgress,
            pointsToNextLevel = user.pointsToNextLevel,
            trackedMinutes = allocation.totalMinutes,
            areas = areas,
            days = days,
            goals = measured
                // Furthest along first: a tile that can only show one goal should
                // show the one with something to report, and ties by title keep the
                // order stable between two refreshes of the same data.
                .sortedWith(compareByDescending<Goal> { it.progressFraction }.thenBy { it.title })
                .map { it.toWidgetGoal() },
            goalsWithoutMeasure = live.size - measured.size,
        )
    }

    private fun Goal.toWidgetGoal() = WidgetGoal(
        id = id,
        title = title,
        // Plural, and carried through plural. `d2-life-area-route` adapted this
        // line to `lifeAreaIds.firstOrNull()` to keep the tree compiling through
        // its §1.2 rename, which was the right call at the time and the wrong
        // shape to keep: §4.7 says a success counts IN FULL in every area the
        // work serves, and only its minutes divide. Taking the first area would
        // have made a goal serving Health and Career vanish from one of them on
        // the effort tile, silently and only for the users who file that way.
        areaIds = lifeAreaIds,
        colorHex = colorHex,
        percent = progressPercent,
        measure = measureLabel(),
    )

    /**
     * The goal's own words for where it stands, or blank.
     *
     * Blank when the goal has no measure — which, since #11, is what a goal that
     * merely *defaulted* to `"%"` reads as. It used to be blank on the literal
     * string `"%"`, for the same reason and one layer lower down: printing `45%`
     * beside a ring reading 45 is §0.3's *second number that quietly disagrees*
     * in miniature — here it would not even disagree, which is worse, because it
     * trains the eye to read two numbers as two facts.
     *
     * A goal that *chose* `PERCENT` still gets a label, and that is the point of
     * §7.1 keeping chosen and defaulted apart.
     *
     * Isolated (§4.8) because it mixes Latin digits with a word the user may have
     * typed in Hebrew, and `3.2 / 4 ק״מ` is exactly the run the bidi algorithm
     * reverses inside an RTL paragraph.
     */
    private fun Goal.measureLabel(): String {
        if (!hasMeasure) return ""
        // A goal that genuinely chose PERCENT still belongs on the tile, but its
        // label would restate the ring digit for digit — so the goal stays and
        // the label goes. This is the surviving half of the old `unit != "%"`
        // rule, kept at the site the reasoning was always about: the ring.
        if (measure?.kind == MeasureKind.PERCENT) return ""
        return "${currentValue.trim()} / ${targetValue.trim()} $measureWord".trim().bidiIsolated()
    }
}

/** `4.0` → `"4"`, `3.25` → `"3.25"` — a trailing `.0` is noise on a tile this size. */
private fun Double.trim(): String =
    if (this == Math.floor(this) && !isInfinite()) toLong().toString() else toString()
