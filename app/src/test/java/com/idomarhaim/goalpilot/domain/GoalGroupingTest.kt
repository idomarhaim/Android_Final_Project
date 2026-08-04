package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.usecase.GroupGoalsByLifeAreaUseCase
import org.junit.Test

/**
 * Banding the goals list by life area. Every case here is a header that must or
 * must not appear — the whole feature is "make the filing visible", and a header
 * over nothing, or a missing one, is the only way it can fail.
 */
class GoalGroupingTest {

    private val group = GroupGoalsByLifeAreaUseCase()

    private val health = LifeArea(id = "health", name = "בריאות", sortOrder = 0)
    private val study = LifeArea(id = "study", name = "לימודים", sortOrder = 1)

    private fun goal(id: String, areaId: String? = null) =
        Goal(id = id, title = id, lifeAreaId = areaId)

    @Test
    fun `goals are banded in the order the areas arrive in`() {
        val goals = listOf(goal("g1", "study"), goal("g2", "health"), goal("g3", "study"))

        val groups = group(goals, listOf(health, study))

        assertThat(groups.map { it.area?.id }).containsExactly("health", "study").inOrder()
        assertThat(groups.first().goals.map { it.id }).containsExactly("g2")
        assertThat(groups.last().goals.map { it.id }).containsExactly("g1", "g3").inOrder()
    }

    @Test
    fun `an area with no goals gets no header`() {
        val groups = group(listOf(goal("g1", "health")), listOf(health, study))

        assertThat(groups.map { it.area?.id }).containsExactly("health")
    }

    @Test
    fun `unfiled goals come last, in their own band`() {
        val goals = listOf(goal("g1"), goal("g2", "health"))

        val groups = group(goals, listOf(health, study))

        assertThat(groups.map { it.area?.id }).containsExactly("health", null).inOrder()
        assertThat(groups.last().goals.map { it.id }).containsExactly("g1")
    }

    @Test
    fun `no life areas at all means one nameless band, not a lone Unassigned header`() {
        val goals = listOf(goal("g1"), goal("g2"))

        val groups = group(goals, areas = emptyList())

        assertThat(groups).hasSize(1)
        assertThat(groups.single().area).isNull()
        assertThat(groups.single().goals).hasSize(2)
    }

    @Test
    fun `every goal unfiled while areas exist still collapses to one band`() {
        val groups = group(listOf(goal("g1"), goal("g2")), listOf(health, study))

        assertThat(groups).hasSize(1)
        assertThat(groups.single().area).isNull()
    }

    @Test
    fun `a goal pointing at a deleted area is unfiled, not lost`() {
        // The area was deleted on another device; the goal must stay on the list.
        val goals = listOf(goal("g1", "gone"), goal("g2", "health"))

        val groups = group(goals, listOf(health))

        assertThat(groups.flatMap { it.goals }.map { it.id }).containsExactly("g2", "g1")
        assertThat(groups.last().area).isNull()
    }

    @Test
    fun `no goals means no bands at all`() {
        assertThat(group(emptyList(), listOf(health, study))).isEmpty()
    }
}
