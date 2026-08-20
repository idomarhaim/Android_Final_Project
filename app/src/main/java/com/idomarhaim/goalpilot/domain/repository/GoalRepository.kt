package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.DeclaredBy
import com.idomarhaim.goalpilot.domain.model.Goal
import kotlinx.coroutines.flow.Flow

/**
 * CRUD + observation for the current user's goals (spec §6 Core: define goals).
 *
 * There is no `addProgress`, and its absence is the design (#49, spec §5.2). A
 * goal's `currentValue` is a sum over its progress entries and its completed
 * tasks, computed on the way out of [observeGoals] / [observeGoal]; nothing in
 * this interface advances it, because a derived number whose reader can reach its
 * own inputs is owed no stored writer. Logging progress means writing a
 * `ProgressEntry`; completing a task means setting `done`. Both are facts, and the
 * number follows them.
 */
interface GoalRepository {

    /** Goals with `currentValue` already derived from the facts. */
    fun observeGoals(includeArchived: Boolean = false): Flow<List<Goal>>

    /** One goal with `currentValue` already derived, or null while it is absent. */
    fun observeGoal(goalId: String): Flow<Goal?>

    /**
     * Creates or updates a goal; returns the goal id.
     *
     * `currentValue` is not persisted — whatever the passed [Goal] carries in that
     * field is ignored, since the document no longer has one.
     */
    suspend fun upsertGoal(goal: Goal): Resource<String>

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

    /**
     * Rules on §1.1's intrinsic marker: [DeclaredBy.USER] keeps a goal the sorter proposed,
     * `null` **demotes** it — drops the marker, leaving the object and every edge intact.
     *
     * Separate from [upsertGoal] for [setLifeAreas]' reason, and one more that is specific to
     * this field: keeping or demoting a suggestion is a one-tap act on a **list** screen, which
     * does not hold the whole goal, and a read-modify-write from there would clobber whatever
     * the user is editing on the goal's own screen. The demotion has to be lossless, and a
     * write that can lose a concurrent edit is not.
     */
    suspend fun setDeclaredBy(goalId: String, declaredBy: DeclaredBy?): Resource<Unit>

    suspend fun setArchived(goalId: String, archived: Boolean): Resource<Unit>

    suspend fun deleteGoal(goalId: String): Resource<Unit>
}
