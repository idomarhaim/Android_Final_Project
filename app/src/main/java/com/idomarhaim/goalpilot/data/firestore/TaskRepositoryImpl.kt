package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.CompletionFactDto
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskCompletion
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tasks live under `users/{uid}/tasks`, and **a completion is its own document** under
 * `users/{uid}/completionFacts/{taskId}` (`#55`, spec §1.4).
 *
 * ### What `#55` moved, and why the tick is still one commit
 *
 * The completion used to be two fields on the task document, `done` + `completedAt`. §1.4
 * banks its **inputs** instead — the minutes and the difficulty as they stood at the tick —
 * so that correcting an estimate afterwards cannot re-price what was already earned. Those
 * inputs cannot live on the task, because `upsertTask` is a whole-document `set()` and an
 * ordinary retitle would overwrite them; they get their own document, keyed by the task id.
 *
 * That is two documents, and §1.4 is explicit that a create-and-complete must *"emit that
 * same fact, not a second pipe"*. Both are satisfied by a **`WriteBatch`**: one commit, one
 * failure mode, no window in which half the fact exists. A batch is applied to the Firestore
 * cache synchronously exactly as a single write is — the offline win `C20` bought by deleting
 * `runTransaction` (a transaction cannot reach the cache at all and failed after a measured
 * 7.9 s, closed #3) is untouched, because a batch is not a transaction. And there is one
 * minting function, [TaskCompletion.of], called by the tick and by the born-done create with
 * the same arguments; the pipe is single because the *fact* has one author, not because the
 * write has one document.
 *
 * ### Three consequences worth knowing before editing this file
 *
 * 1. **`observeTasks` is a join.** The tasks collection carries no completion any more, so
 *    the flow combines it with `completionFacts`. Both are cache-served snapshot listeners,
 *    so this costs a second listener and no round trip.
 * 2. **A pre-`#55` document still reads correctly, with no backfill.** `TaskDto.toDomain`
 *    reconstructs a fact from `done`/`completedAt`/`points`, losslessly — a legacy point
 *    value `p` becomes `3p` minutes at `ROUTINE`, which prices back at `p`. The fact
 *    document, when one exists, wins over that reconstruction.
 * 3. **Points do not move until the projection function runs — including the owner's own.**
 *    Nothing in this app sums these facts on the device: `AuthRepositoryImpl.authState()`
 *    reads the stored `users/{uid}.points` and `UserDto.toDomain()` passes it straight
 *    through, so Dashboard, Profile and the widget all render the **stored** number. The tick
 *    itself still works offline and instantly, because it is a fact; the totals derived from
 *    it wait for the server. That is why the projection writes `users/{uid}.points` as well
 *    as the public row. **`Observed:` 2026-08-20**, by reading the four call sites.
 *
 * It also does not advance the linked goal (#49, spec §5.2): the goal sums the **declared
 * contribution of each edge** its completed tasks point at it with (§1.5), so the tick alone
 * carries the progress and there is no derived number stored anywhere to fall out of step.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TaskRepository {

    private fun userDoc(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid)

    private fun tasksCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.TASKS)

    private fun factsCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.COMPLETION_FACTS)

    /**
     * §2.1's occurrence documents — read here **only to delete them with their task** (`#67`).
     *
     * Everything else about this collection belongs to `OccurrenceRepositoryImpl`, and nothing
     * about that split changes: a task's deletion is the one write that has to reach both
     * collections, for the same reason it already reaches `completionFacts`, and routing it
     * through the other repository would make a delete depend on an interface whose whole
     * contract is about scheduling.
     */
    private fun occurrencesCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.OCCURRENCES)

    /**
     * The task as the database currently holds it, completion joined on.
     *
     * Two `get()`s rather than one, and both are cache-served on the path that matters: the
     * row was just rendered from that cache. Reading the fact as well is what makes
     * `TaskCompletion.of` able to see an ALREADY-BANKED completion and leave it alone --
     * without it, re-saving a completed task would re-date and re-price it, which is the one
     * thing §1.4 banks the inputs to prevent.
     */
    private suspend fun readTask(uid: String, taskId: String): Task? {
        val dto = tasksCol(uid).document(taskId).get().await()
            .toObject(TaskDto::class.java) ?: return null
        val fact = factsCol(uid).document(taskId).get().await()
            .toObject(CompletionFactDto::class.java)
        return dto.toDomain().copy(id = taskId, completion = fact?.toDomain() ?: dto.toDomain().completion)
    }

    override fun observeTasks(goalId: String?): Flow<List<Task>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                // The join lives in `TaskStream` and not here, because `GoalRepositoryImpl`
                // needs the same one and a copy of it is two things to keep in step -- see
                // that file for the device-observed defect that moved it.
                TaskStream.observe(userDoc(uid), goalId)
                    // Sort client-side to avoid requiring a composite Firestore index.
                    .map { it.sortedWith(compareBy({ t -> t.isDone }, { t -> -t.createdAtEpochMillis })) }
            }
        }

    override suspend fun upsertTask(task: Task): Resource<String> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            val col = tasksCol(uid)
            val ref = if (task.id.isBlank()) col.document() else col.document(task.id)
            val now = System.currentTimeMillis()
            val toSave = task.copy(
                id = ref.id,
                createdAtEpochMillis = if (task.createdAtEpochMillis == 0L) {
                    now
                } else {
                    task.createdAtEpochMillis
                },
            )
            // ONE COMMIT, AND IT CARRIES THE COMPLETION IF THERE IS ONE (`#7`, `#55`, §1.4).
            //
            // This is what makes a task **born done** legal: the create carries its fact, so
            // there is no `setDone` after this and therefore no window in which the task
            // exists un-completed. The tempting shape -- upsert, then tick -- is two commits
            // and two failure modes, and its second one is the write that has to succeed for
            // the fact to be whole.
            //
            // Note which function mints the fact: `TaskCompletion.of`, the same one `setDone`
            // calls. That is §1.4's "emit that same fact, not a second pipe" -- one author,
            // whatever surface the completion came from.
            //
            // An already-banked fact is re-written unchanged rather than skipped, because a
            // batch must describe the whole intended state: `TaskCompletion.of` returns the
            // existing fact untouched, so `AnalyticsViewModel`'s duration backfill re-saving a
            // completed task cannot move its stamp or re-price it.
            val batch = firestore.batch()
            batch.set(ref, toSave.toDto())
            val factRef = factsCol(uid).document(ref.id)
            if (toSave.isDone) {
                batch.set(factRef, TaskCompletion.of(toSave, now).toDto(ref.id))
            } else {
                batch.delete(factRef)
            }
            batch.commit().await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not save task", e)
        }
    }

    override suspend fun setDone(taskId: String, done: Boolean): Resource<Unit> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                val taskRef = tasksCol(uid).document(taskId)
                val factRef = factsCol(uid).document(taskId)
                if (done) {
                    // The fact has to be minted from the task, and the task is not in hand:
                    // `toggleTask` passes an id.
                    val task = readTask(uid, taskId)
                        ?: return@withContext Resource.Error("Task no longer exists")
                    val batch = firestore.batch()
                    batch.set(factRef, TaskCompletion.of(task, System.currentTimeMillis()).toDto(taskId))
                    batch.update(taskRef, LEGACY_COMPLETION_CLEARED)
                    batch.commit().await()
                } else {
                    // AN UNTICK REMOVES EXACTLY THE FACT THE TICK ADDED (§1.4).
                    //
                    // A delete of a known path -- no read, no arithmetic, nothing to
                    // double-subtract. The defect `C20` fixed (tick at 10, re-score to 30,
                    // untick, lose 30) cannot return by this route: nothing here reads a
                    // point value at all, and the fact it removes is the one that was banked.
                    //
                    // The legacy clear is not optional. A pre-`#55` document still carries
                    // `done: true`, which `TaskDto.legacyCompletion()` reads as a completion;
                    // deleting the fact without clearing it would leave a task that ticks
                    // back on by itself.
                    val batch = firestore.batch()
                    batch.delete(factRef)
                    batch.update(taskRef, LEGACY_COMPLETION_CLEARED)
                    batch.commit().await()
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not update task", e)
            }
        }

    /**
     * Removes the task, its completion fact, **and its occurrence documents** (`#67`).
     *
     * ### The occurrences were orphaned, and that was invisible
     *
     * `Observed:` 2026-08-23, reading this method against `OccurrenceRepositoryImpl`. Before
     * `#67` this deleted two documents and left every row in `users/{uid}/occurrences` whose
     * `taskId` was this one. Nothing rendered them afterwards — every consumer joins the
     * collection back to the task list (`CalendarViewModel.schedules`,
     * `BuildSuccessFailureRunUseCase`), so a document whose task is gone is dropped from every
     * count and every lane. That is exactly what made it hard to notice: no number was wrong,
     * no screen misbehaved, and the rows accumulated in Firestore for the life of the account
     * with no reader and no way to remove them.
     *
     * The fact's own comment already carries the argument, and it is the same one: *"an orphan
     * fact would add points the user cannot see, find or remove."* An orphan occurrence adds no
     * points, and is otherwise the same object — storage the person is paying for, about a task
     * they deleted, reachable by nobody.
     *
     * ### §2.3 is not violated by this, and it is worth saying why
     *
     * *"A missed occurrence is never edited — it is history"* governs the **scoped verbs**:
     * `EditScope` deliberately offers no `ALL` reaching backwards, so no move and no skip can
     * rewrite what already happened. A delete is not one of those verbs — it is the person
     * saying the task should not exist — and `DeletionImpact.OfTask` is what makes the app say
     * so first, naming how many of the windows going with it had already happened.
     *
     * ### Chunked rather than refused, unlike `OccurrenceRepository.apply`
     *
     * That method returns `SchedulePlan.TooLarge` rather than splitting a plan across batches,
     * because a half-applied *schedule* is worse than none: the past would exist twice, once as
     * documents and once as the rule regenerating it. A half-applied **deletion** has neither
     * property. It is idempotent — re-running removes what is left — and it converges, because
     * every commit strictly shrinks the set. So a task with more occurrence documents than one
     * batch holds is deleted in several, and the task document itself goes **last**, so an
     * interrupted run leaves a task that is still listed and still deletable rather than an
     * invisible task with a tail of documents.
     */
    override suspend fun deleteTask(taskId: String): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            val mine = occurrencesCol(uid).whereEqualTo(OCCURRENCE_TASK_ID, taskId).get().await()
            mine.documents.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            // The fact goes with the task. It is tempting to keep it -- §1.4 wants a level
            // that can never fall -- but that clause is about **re-pricing**, which banking
            // the inputs already settles. Deleting a task is a deliberate act about a thing
            // that should not have been counted, and an orphan fact would add points the user
            // cannot see, find or remove.
            val batch = firestore.batch()
            batch.delete(tasksCol(uid).document(taskId))
            batch.delete(factsCol(uid).document(taskId))
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not delete task", e)
        }
    }

    private companion object {
        /**
         * The pre-`#55` completion fields, removed rather than nulled.
         *
         * `FieldValue.delete()` and not `null` because `TaskDto.done` is `Boolean?`: a stored
         * `null` and an absent key both read as "not done", so either would work — but the
         * document is being migrated, and leaving a superseded key present is the shape
         * §0.3 keeps naming. `toDto()` writes them as `null` for a different reason: a
         * whole-document `set()` cannot express a deletion.
         */
        val LEGACY_COMPLETION_CLEARED = mapOf(
            "done" to FieldValue.delete(),
            "completedAt" to FieldValue.delete(),
        )

        /**
         * `OccurrenceDto.taskId`, as a query key.
         *
         * The same literal `OccurrenceRepositoryImpl.observeOccurrences` filters on. Named here
         * rather than typed twice, because a rename of that field would otherwise leave this
         * query matching nothing — and a delete that finds no occurrences looks **exactly** like
         * a task that had none.
         */
        const val OCCURRENCE_TASK_ID = "taskId"

        /**
         * Firestore's cap is 500 writes per batch; this leaves room for the two that follow.
         *
         * A repeating task accumulates one document per window the user actually touched, so
         * reaching this at all takes years of use — the chunking is here because the failure
         * mode if it were ever reached is a thrown exception on a delete the person has already
         * confirmed, not because it is expected.
         */
        const val BATCH_LIMIT = 450
    }
}
