package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.data.firestore.dto.CompletionFactDto
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * **The one place a task document becomes a `Task`** — `#55`, `docs/PRODUCT_v0.3.md` §1.4.
 *
 * ### Why this file exists, and the defect that put it here
 *
 * §1.4 moved the completion out of the task document into
 * `users/{uid}/completionFacts/{taskId}`, so reading a task now means reading **two**
 * collections and joining them. Two repositories read the tasks collection —
 * [TaskRepositoryImpl] for the task list and [GoalRepositoryImpl] for the completed-task half
 * of a goal's derived progress — and the join went into the first one only.
 *
 * The result was a task that was **done on one screen and open on another**. `Observed:` on a
 * device, 2026-08-21: a completed task re-ticked after the migration showed its checkbox
 * ticked and its `+35` in the task list, while the goal ring above it read **0% / 0 of 100**
 * where it had read 1% a moment earlier. The task list had the join; the goal's arithmetic
 * did not, so it saw a task document with no `done` field and counted it as unfinished.
 *
 * That is §0.3's *second number that quietly disagrees*, and the honest fix is not to copy the
 * join into the second repository — it is to make there be **one** place that knows how a task
 * is assembled. A copied join is two things to keep in step, which is the same shape as the
 * defect. `#49` made exactly this argument about `currentValue` and landed on `DerivedProgress`;
 * this is that argument at the read boundary.
 *
 * **`Untested:` whether any third reader appears later.** What makes that safe rather than
 * hopeful is mechanical: `TaskDto.toDomain()` produces a task whose completion is only ever
 * the *legacy* one, so a reader that skips this seam is wrong about every task written after
 * the migration — visibly, on the first tick, exactly as above.
 */
internal object TaskStream {

    /**
     * Every task under [userDoc], with its banked completion joined on, optionally narrowed
     * to one goal.
     *
     * The narrowing applies to the **tasks** query only. A completion fact carries no goal —
     * it is a record of effort, not of filing — so the fact collection is always read whole,
     * and the join is a map lookup rather than a per-task `get()`. That matters offline: N
     * round trips on a screen that must render from the cache is not a cost, it is a failure.
     */
    fun observe(userDoc: DocumentReference, goalId: String? = null): Flow<List<Task>> {
        val tasks: CollectionReference = userDoc.collection(FirestorePaths.TASKS)
        val query = if (goalId != null) tasks.whereEqualTo("goalId", goalId) else tasks
        return combine(
            query.snapshotsFlow(),
            userDoc.collection(FirestorePaths.COMPLETION_FACTS).snapshotsFlow(),
        ) { taskSnap, factSnap ->
            val facts: Map<String, CompletionFact> = factSnap
                .toObjects(CompletionFactDto::class.java)
                .associate { it.id to it.toDomain() }
            taskSnap.toObjects(TaskDto::class.java).map { dto ->
                val task = dto.toDomain()
                // A real fact wins over the legacy reconstruction `toDomain()` supplies for a
                // pre-`#55` document. Where there is neither, `completion` is already null.
                facts[task.id]?.let { task.copy(completion = it) } ?: task
            }
        }
    }
}
