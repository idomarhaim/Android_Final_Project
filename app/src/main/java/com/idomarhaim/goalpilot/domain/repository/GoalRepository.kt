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
     * Files a goal under a life area, or unfiles it with null. Separate from
     * [upsertGoal] so the life-areas screen can re-file a stack of goals without
     * loading and re-writing each one whole — a read-modify-write of a goal the
     * user is editing on another screen would clobber their edit.
     */
    suspend fun setLifeArea(goalId: String, lifeAreaId: String?): Resource<Unit>

    suspend fun setArchived(goalId: String, archived: Boolean): Resource<Unit>

    suspend fun deleteGoal(goalId: String): Resource<Unit>
}
