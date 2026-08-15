package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import kotlinx.coroutines.flow.Flow

/** CRUD + observation for the current user's goals (spec §6 Core: define goals). */
interface GoalRepository {

    fun observeGoals(includeArchived: Boolean = false): Flow<List<Goal>>

    fun observeGoal(goalId: String): Flow<Goal?>

    /** Creates or updates a goal; returns the goal id. */
    suspend fun upsertGoal(goal: Goal): Resource<String>

    /** Adds [delta] to the goal's current value (clamped at the target), for progress logging. */
    suspend fun addProgress(goalId: String, delta: Double): Resource<Unit>

    /**
     * Files a goal under [lifeAreaIds], or unfiles it with the empty list.
     * Separate from [upsertGoal] so the life-areas screens can re-file a stack of
     * goals without loading and re-writing each one whole — a read-modify-write of
     * a goal the user is editing on another screen would clobber their edit.
     *
     * The list replaces whatever the goal carried; it is not a union, so a caller
     * adding one area sends the areas the goal already had alongside it.
     */
    suspend fun setLifeAreas(goalId: String, lifeAreaIds: List<String>): Resource<Unit>

    suspend fun setArchived(goalId: String, archived: Boolean): Resource<Unit>

    suspend fun deleteGoal(goalId: String): Resource<Unit>
}
