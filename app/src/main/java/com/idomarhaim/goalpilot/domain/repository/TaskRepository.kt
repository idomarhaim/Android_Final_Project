package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Task
import kotlinx.coroutines.flow.Flow

/** Tasks linked to goals + completion/points (spec §6 Core: associate tasks, point scoring). */
interface TaskRepository {

    /** Observe tasks; pass [goalId] to scope to a single goal, or null for all. */
    fun observeTasks(goalId: String? = null): Flow<List<Task>>

    suspend fun upsertTask(task: Task): Resource<String>

    /**
     * Toggle a task's completion. Implementations award/rescind [Task.points] to
     * the user and advance/retract the linked goal's progress atomically.
     */
    suspend fun setDone(taskId: String, done: Boolean): Resource<Unit>

    suspend fun deleteTask(taskId: String): Resource<Unit>
}
