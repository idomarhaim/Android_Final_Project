package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.firestore.dto.GoalDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.Goal
import org.junit.Test

/**
 * `lifeAreaId` → `lifeAreaIds`, the additive half of spec §7.1.
 *
 * The migration posture the whole spec commits to is *"additive with a readable
 * half-way state"*, and this is the one place that claim can actually be checked:
 * a document written before the change must read back identically, and a document
 * written after it must stop carrying the old field at all. Without the second
 * half a goal would hold two answers to "which area?", which is the map's
 * most-repeated finding — a second number that quietly disagrees.
 */
class GoalLifeAreaMigrationTest {

    @Test
    fun `a pre-migration document backfills its single id`() {
        val legacy = GoalDto(id = "g1", title = "Run", lifeAreaId = "health")

        assertThat(legacy.toDomain().lifeAreaIds).containsExactly("health")
    }

    @Test
    fun `a pre-migration document with no area backfills the empty list`() {
        val legacy = GoalDto(id = "g1", title = "Run", lifeAreaId = null)

        assertThat(legacy.toDomain().lifeAreaIds).isEmpty()
    }

    @Test
    fun `a blank legacy id is unfiled, not an area named empty string`() {
        val legacy = GoalDto(id = "g1", title = "Run", lifeAreaId = "   ")

        assertThat(legacy.toDomain().lifeAreaIds).isEmpty()
    }

    @Test
    fun `the plural field wins outright over a stale singular one`() {
        // Only reachable if some other writer left both behind. The plural field
        // is the one this app writes, so it is the one that decides.
        val both = GoalDto(id = "g1", lifeAreaId = "stale", lifeAreaIds = listOf("health"))

        assertThat(both.toDomain().lifeAreaIds).containsExactly("health")
    }

    @Test
    fun `an explicitly empty plural field is unfiled, not backfilled from the legacy id`() {
        // The difference between "migrated, then unfiled" and "never migrated".
        // Reading the legacy id here would resurrect a filing the user removed.
        val unfiled = GoalDto(id = "g1", lifeAreaId = "health", lifeAreaIds = emptyList())

        assertThat(unfiled.toDomain().lifeAreaIds).isEmpty()
    }

    @Test
    fun `duplicates collapse, so one goal cannot be counted twice in one band`() {
        val doubled = GoalDto(id = "g1", lifeAreaIds = listOf("health", "health", "study"))

        assertThat(doubled.toDomain().lifeAreaIds).containsExactly("health", "study").inOrder()
    }

    @Test
    fun `writing a goal clears the legacy field, so the two can never disagree`() {
        val dto = Goal(id = "g1", lifeAreaIds = listOf("health", "study")).toDto()

        assertThat(dto.lifeAreaId).isNull()
        assertThat(dto.lifeAreaIds).containsExactly("health", "study").inOrder()
    }

    @Test
    fun `a round trip through Firestore's shape is lossless`() {
        val goal = Goal(id = "g1", title = "Run", lifeAreaIds = listOf("health", "study"))

        assertThat(goal.toDto().toDomain().lifeAreaIds).isEqualTo(goal.lifeAreaIds)
    }
}
