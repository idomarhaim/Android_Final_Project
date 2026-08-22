package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.SchedulePlan
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import kotlinx.coroutines.flow.Flow

/**
 * §2.1's occurrence documents — `users/{uid}/occurrences`
 * ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * ### Why this is its own repository and not four methods on `TaskRepository`
 *
 * `TaskRepository` writes a task with a whole-document `set()`, which is precisely why `#55`
 * put the completion fact in its own collection: *"banked values living on the task would be
 * overwritten by an ordinary edit"*. An occurrence carries a `googleEventId` and an outcome —
 * both things an ordinary retitle must not touch — so it needs the same structural separation,
 * and a separate collection wants a separate interface for the same reason `ProgressRepository`
 * has one.
 *
 * ### Everything that decides anything is in the domain, not here
 *
 * The interesting question — *"this occurrence, or all future ones?"* — is answered by
 * `ScheduleEdits.apply`, which is pure and takes a clock as an argument. This interface only
 * commits what that produced. That split is deliberate: a rule that can only be exercised
 * against a live Firestore is a rule whose branches do not all get tested, which is the same
 * argument `OccurrenceDraft`'s KDoc already makes for the add-task row.
 */
interface OccurrenceRepository {

    /**
     * Every stored occurrence for the signed-in user, or just one task's.
     *
     * Deliberately **not** range-filtered. The collection holds one document per *when the user
     * touched*, not one per instance, so it stays small by construction — and a whole-collection
     * snapshot listener is cache-served, exactly as the `completionFacts` join in `TaskStream`
     * already is. §4.3's calendar range is applied in the domain by
     * [TaskSchedule.occurrencesIn], which has to expand the rule anyway and therefore cannot be
     * replaced by a query.
     */
    fun observeOccurrences(taskId: String? = null): Flow<List<ScheduledOccurrence>>

    /**
     * A task paired with its stored occurrences — the aggregate every scheduling question is
     * asked of.
     *
     * Emits nothing while the task does not exist: an absent task has no schedule, and a
     * [TaskSchedule] built around a blank [com.idomarhaim.goalpilot.domain.model.Task] would
     * generate instances from an anchor nobody set.
     */
    fun observeSchedule(taskId: String): Flow<TaskSchedule>

    /**
     * Commits a plan from `ScheduleEdits.apply` — the task and its occurrence documents, in one
     * `WriteBatch`.
     *
     * A [SchedulePlan.TooLarge] is returned as an error naming both numbers rather than
     * silently chunked: chunking would trade the single failure mode for a half-applied edit,
     * and truncating would drop history. **Legal, but never silent** (§0.4).
     */
    suspend fun apply(plan: SchedulePlan): Resource<Unit>

    /**
     * Records what happened to one window — done, skipped, or back to planned.
     *
     * ⚠️ This banks **no points**: §1.4's completion fact is a separate document with its own
     * inputs, and points per occurrence is `#64`'s. See [OccurrenceOutcome.Done].
     */
    suspend fun setOutcome(occurrenceId: String, outcome: OccurrenceOutcome): Resource<Unit>

    /**
     * Links this occurrence to the Google Calendar event mirroring it, or clears the link
     * (§2.7, `#61`).
     *
     * Clearing is a first-class operation because §2.7 requires it: *"a disappearance never
     * deletes and never re-creates"* — the occurrence *"keeps its date, clears its
     * `googleEventId`"*, and the ambiguity is asked in the daily review rather than resolved by
     * a write.
     */
    suspend fun linkGoogleEvent(occurrenceId: String, googleEventId: String?): Resource<Unit>
}
