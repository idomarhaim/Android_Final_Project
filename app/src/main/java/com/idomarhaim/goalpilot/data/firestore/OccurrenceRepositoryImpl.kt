package com.idomarhaim.goalpilot.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.FirestorePaths
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.data.auth.uidFlow
import com.idomarhaim.goalpilot.data.firestore.dto.OccurrenceDto
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.SchedulePlan
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * §2.1's occurrences, at `users/{uid}/occurrences`
 * ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * ### The collection needed no `firestore.rules` change, and that is a finding rather than an
 * omission
 *
 * `users/{uid}/{document=**}` already matches every per-user subcollection with an owner-only
 * rule, so a new one under that path is a client-side change — the same result life areas
 * produced and AGENTS.md records. Nothing here is server-owned: the projection function does
 * not read occurrences, and `googleEventId` is written by the client, because §2.6 buys
 * `calendar.app.created` **client-side** and §2.7 says outright that *"there is no credential
 * for a background sync and cannot be one"*. A field-level condition would therefore have had
 * nothing to protect. What is owed instead is a **test**, and `firestore-tests/` has one — the
 * wildcard is correct today and a future narrowing of it would expose this collection silently,
 * with no client error and no failing Kotlin test.
 *
 * ### One batch, for the reason `#55` already established
 *
 * A scoped edit writes the task **and** its occurrence documents — moving a series materialises
 * its past in the same breath as it moves the anchor — and half of that applied is worse than
 * none of it: the past would exist twice, once as documents and once as the rule regenerating
 * it from the moved anchor. `TaskRepositoryImpl.upsertTask` reaches for a `WriteBatch` for the
 * same reason and the same non-reason: a batch reaches the offline cache synchronously exactly
 * as a single write does, where a transaction cannot reach it at all (`C20`, closed #3).
 *
 * That is also why [apply] refuses a [SchedulePlan.TooLarge] rather than chunking it. Chunking
 * would buy a plan that can be half-applied, which is the one property the batch is here for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class OccurrenceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OccurrenceRepository {

    private fun userDoc(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid)

    private fun occurrencesCol(uid: String): CollectionReference =
        userDoc(uid).collection(FirestorePaths.OCCURRENCES)

    override fun observeOccurrences(taskId: String?): Flow<List<ScheduledOccurrence>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                val col = occurrencesCol(uid)
                val query = if (taskId != null) col.whereEqualTo("taskId", taskId) else col
                query.snapshotsFlow().map { snap ->
                    // A document with no readable *when* is dropped rather than defaulted --
                    // `OccurrenceDto.toDomain` says why. Sorting is by the occurrence's own
                    // start, not by `seriesDate`, because a moved instance belongs where the
                    // user moved it.
                    snap.toObjects(OccurrenceDto::class.java)
                        .mapNotNull { it.toDomain() }
                        .sortedBy { it.occurrence.opensAt }
                }
            }
        }

    override fun observeSchedule(taskId: String): Flow<TaskSchedule> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf<TaskSchedule?>(null)
            } else {
                combine(
                    userDoc(uid).collection(FirestorePaths.TASKS).document(taskId)
                        .snapshotsFlow(),
                    observeOccurrences(taskId),
                ) { taskSnap, occurrences ->
                    // No task, no schedule. A `TaskSchedule` around a blank task would carry a
                    // null anchor and generate nothing, which reads as "this task has no
                    // when" -- a true sentence about a task that does not exist, and therefore
                    // the wrong one to say.
                    taskSnap.toObject(TaskDto::class.java)
                        ?.toDomain()
                        ?.let { TaskSchedule(task = it, stored = occurrences) }
                }
            }
        }.filterNotNull()

    override suspend fun apply(plan: SchedulePlan): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        // Legal, but never silent (§0.4). Both numbers are in the message because the only
        // useful next question is "how far over?", and a caller that cannot see it can only
        // guess at which edit to offer instead.
        val writes = when (plan) {
            is SchedulePlan.TooLarge -> return@withContext Resource.Error(
                "That change would write ${plan.required} documents at once, " +
                    "and the limit is ${plan.limit}.",
            )
            is SchedulePlan.Writes -> plan
        }
        try {
            val col = occurrencesCol(uid)
            val batch = firestore.batch()
            batch.set(
                userDoc(uid).collection(FirestorePaths.TASKS).document(writes.task.id),
                writes.task.toDto(),
            )
            writes.upserts.forEach { occurrence ->
                // A blank id is a document this plan is creating -- `ScheduleEdits` is pure and
                // mints none, exactly as `upsertTask` allocates the task's id here rather than
                // in the domain.
                val ref = if (occurrence.id.isBlank()) col.document() else col.document(occurrence.id)
                batch.set(ref, occurrence.copy(id = ref.id, taskId = writes.task.id).toDto())
            }
            writes.deletes.forEach { batch.delete(col.document(it)) }
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not change the schedule", e)
        }
    }

    override suspend fun setOutcome(
        occurrenceId: String,
        outcome: OccurrenceOutcome,
    ): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            // A field update, not a `set()`: the occurrence's *when* and its `googleEventId`
            // belong to other writers, and a whole-document write here would carry whatever
            // this caller happened to be holding over the top of them.
            occurrencesCol(uid).document(occurrenceId).update(
                mapOf(
                    "outcome" to when (outcome) {
                        is OccurrenceOutcome.Planned -> "PLANNED"
                        is OccurrenceOutcome.Done -> "DONE"
                        is OccurrenceOutcome.Skipped -> "SKIPPED"
                    },
                    // Cleared, never left behind: a stamp outliving the outcome it belonged to
                    // would re-read as a fact the next time `decodeOutcome` ran.
                    "outcomeAt" to when (outcome) {
                        is OccurrenceOutcome.Planned -> null
                        is OccurrenceOutcome.Done -> outcome.atEpochMillis
                        is OccurrenceOutcome.Skipped -> outcome.atEpochMillis
                    },
                ),
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not record what happened", e)
        }
    }

    override suspend fun linkGoogleEvent(
        occurrenceId: String,
        googleEventId: String?,
    ): Resource<Unit> = withContext(io) {
        val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
        try {
            occurrencesCol(uid).document(occurrenceId)
                .update(mapOf("googleEventId" to googleEventId)).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not link the calendar event", e)
        }
    }
}
