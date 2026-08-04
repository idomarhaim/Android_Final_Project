package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import javax.inject.Inject

/**
 * One band of the goals list: an area and the goals filed under it, or the
 * unfiled remainder when [area] is null.
 */
data class GoalGroup(val area: LifeArea?, val goals: List<Goal>)

/**
 * Splits the goals list into life-area bands, so "my goals belong to areas" is
 * visible where the user actually looks at their goals (spec §1) and not only on
 * a goal's own screen.
 *
 * The rules it encodes, all of which exist to stop a header appearing where it
 * says nothing:
 *
 * - **No areas defined → one nameless band.** A lone "No life area" header over
 *   the whole list is pure noise for a user who has not adopted the feature; the
 *   screen renders a single null-area band exactly as it always rendered the flat
 *   list.
 * - **Empty areas get no band.** An area with no goals belongs on the life-areas
 *   screen, not as a header with nothing under it.
 * - **Unfiled goes last**, and only when it is non-empty.
 * - **A dangling `lifeAreaId` counts as unfiled** — same rule as the life-areas
 *   screen. An area deleted on another device must not take its goals off the
 *   list with it.
 *
 * Bands come out in the order [areas] arrives in, which is the repository's
 * `(sortOrder, name)` — so the drag order the user chose is the order their goals
 * are grouped in.
 */
class GroupGoalsByLifeAreaUseCase @Inject constructor() {

    operator fun invoke(goals: List<Goal>, areas: List<LifeArea>): List<GoalGroup> {
        if (goals.isEmpty()) return emptyList()
        if (areas.isEmpty()) return listOf(GoalGroup(area = null, goals = goals))

        val known = areas.associateBy { it.id }
        val byAreaId = goals.groupBy { goal -> goal.lifeAreaId?.let { known[it]?.id } }
        val filed = areas.mapNotNull { area ->
            byAreaId[area.id]?.let { GoalGroup(area = area, goals = it) }
        }
        val unfiled = byAreaId[null].orEmpty()

        return if (unfiled.isEmpty()) filed else filed + GoalGroup(area = null, goals = unfiled)
    }
}
