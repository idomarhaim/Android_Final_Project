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

    suspend fun setArchived(goalId: String, archived: Boolean): Resource<Unit>

    suspend fun deleteGoal(goalId: String): Resource<Unit>
}
