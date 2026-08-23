package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.CalendarCall
import com.idomarhaim.goalpilot.domain.model.ConsentRecovery
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.RemoteEvent
import com.idomarhaim.goalpilot.domain.model.SchedulePlan
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import com.idomarhaim.goalpilot.domain.repository.AuthRepository
import com.idomarhaim.goalpilot.domain.repository.CalendarRepository
import com.idomarhaim.goalpilot.domain.repository.OccurrenceRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Why a calendar sync is running, which decides whether the pull is throttled (§2.7). */
enum class CalendarSyncTrigger {
    /** The app came to the foreground. The **pull** is throttled; the push never is. */
    APP_FOREGROUND,

    /** The user asked. Nothing is throttled. */
    MANUAL,
}

/** What one sync attempt did. Facts only; the wording lives in the UI layer. */
sealed interface CalendarSyncOutcome {

    /** It ran. The counts are what actually changed, not what was considered. */
    data class Synced(
        val inserted: Int = 0,
        val updated: Int = 0,
        val cancelled: Int = 0,
        val retimed: Int = 0,
        val disappeared: Int = 0,
    ) : CalendarSyncOutcome {
        val isQuiet: Boolean
            get() = inserted == 0 && updated == 0 && cancelled == 0 &&
                retimed == 0 && disappeared == 0
    }

    /**
     * The calendar scope has not been granted. **Not a failure** (§2.6): sign-in can succeed
     * having granted nothing, so this is the ordinary state of a user who has not opted in, and
     * [recovery] is the screen that would change it.
     */
    data class NeedsConsent(val recovery: ConsentRecovery?) : CalendarSyncOutcome

    /** Nobody is signed in, so there is no calendar to be Ido's. */
    data object NotSignedIn : CalendarSyncOutcome

    /** Another sync is mid-flight; this one stood down rather than double-writing. */
    data object AlreadyRunning : CalendarSyncOutcome

    data class Failed(val message: String?) : CalendarSyncOutcome
}

/**
 * §2.7's two-way sync between GoalPilot's occurrences and Ido's own Google calendar —
 * [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61).
 *
 * ### It decides nothing; [CalendarSync] does
 *
 * Every rule this feature has — which rung becomes which kind of event, what a disappearance
 * means, when a cancel is allowed, whether a pull may run — is a pure function in
 * [CalendarSync] and is tested on the JVM. What is left here is I/O and its ordering, which is
 * the part a test could only fake. The split is `ScheduleEdits`/`OccurrenceRepository`'s,
 * repeated deliberately.
 *
 * ### Pull first, then push, and the order is the decision
 *
 * §2.7: *"last-write-wins is correct rather than a compromise"*, and *"the pull runs on
 * foreground, the window is minutes wide for one user"*. Pulling first means the push sees what
 * Google currently holds, so it patches only what genuinely drifted and — crucially — can tell
 * a **cancelled** event from an unknown one. Pushing first would re-create events the user had
 * just deleted, which §2.7 forbids outright.
 *
 * ### There is no background sync and there cannot be one
 *
 * §2.7: *"`GoogleAuthUtil` mints only short-lived tokens with no refresh token, and `C9d`
 * banned the service account, so **there is no credential for a background sync and cannot be
 * one**."* Nothing here schedules anything; the caller is the app coming forward, or a person
 * pressing a button. That is also why there is no conflict resolver: the two writers cannot
 * both be running while nobody is holding the phone.
 *
 * ### A singleton, for `SyncHealthDataUseCase`'s reason
 *
 * The throttle and the in-flight guard have to be shared: the root view model fires on
 * foreground and the daily-review sheet fires on an answer, and those two must not race each
 * other into the same set of writes.
 */
@Singleton
class SyncCalendarUseCase @Inject constructor(
    private val calendar: CalendarRepository,
    private val tasks: TaskRepository,
    private val occurrences: OccurrenceRepository,
    private val auth: AuthRepository,
    private val preferences: AppPreferencesRepository,
) {

    private val inFlight = Mutex()

    private val _disappearances = MutableStateFlow<List<CalendarEntry>>(emptyList())

    /**
     * Occurrences whose Google event has gone, awaiting §2.7's **Keep / Cancel / Put back**.
     *
     * Their links are **already cleared** by the time they appear here — the sheet asks what
     * Ido wants, it does not decide whether the data is safe. So a user who never opens it
     * keeps every occurrence, on its original date, simply unmirrored.
     */
    val disappearances: StateFlow<List<CalendarEntry>> = _disappearances.asStateFlow()

    /**
     * Runs one sync.
     *
     * @param zone the zone occurrence dates are read in. Passed rather than defaulted, for
     *   `TaskSchedule.occurrencesIn`'s reason: *"a day read in the wrong zone is the exact
     *   defect … and a hidden `systemDefault()` would put it back where nobody could see it."*
     */
    suspend operator fun invoke(
        trigger: CalendarSyncTrigger,
        now: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): CalendarSyncOutcome {
        val uid = auth.currentUid() ?: return CalendarSyncOutcome.NotSignedIn
        if (!inFlight.tryLock()) return CalendarSyncOutcome.AlreadyRunning
        return try {
            run(uid, trigger, now, zone, nowMillis)
        } catch (e: Exception) {
            CalendarSyncOutcome.Failed(e.message)
        } finally {
            inFlight.unlock()
        }
    }

    private suspend fun run(
        uid: String,
        trigger: CalendarSyncTrigger,
        now: LocalDateTime,
        zone: ZoneId,
        nowMillis: Long,
    ): CalendarSyncOutcome {
        val calendarId = when (val ensured = resolveCalendarId(uid)) {
            is CalendarCall.Success -> ensured.value
            is CalendarCall.NeedsConsent -> return CalendarSyncOutcome.NeedsConsent(ensured.recovery)
            is CalendarCall.Failure -> return CalendarSyncOutcome.Failed(ensured.message)
        }

        val today = now.toLocalDate()
        // Two ranges, and keeping them distinct is the whole of `pullPlan`'s absent-event
        // caveat: we ASK Google about a wider span than we are willing to JUDGE, so an event
        // dragged a fortnight out of the window is found rather than read as a delete.
        val windowFrom = today.minusDays(PAST_DAYS)
        val windowTo = today.plusDays(WINDOW_DAYS)
        val queryFrom = windowFrom.minusDays(QUERY_MARGIN_DAYS)
        val queryTo = windowTo.plusDays(QUERY_MARGIN_DAYS)

        val entries = entriesIn(windowFrom, windowTo, zone) ?: return CalendarSyncOutcome.Failed(
            "Could not read your schedule",
        )

        val manual = trigger == CalendarSyncTrigger.MANUAL
        val mayPull = CalendarSync.mayPull(
            manual = manual,
            lastPullAtMillis = preferences.calendarLastPullAt(uid),
            nowMillis = nowMillis,
        )

        var retimed = 0
        var disappeared = 0
        var remote = emptyMap<String, RemoteEvent>()

        if (mayPull) {
            when (
                val fetched = calendar.listEvents(
                    calendarId = calendarId,
                    from = queryFrom,
                    to = queryTo,
                )
            ) {
                is CalendarCall.NeedsConsent ->
                    return CalendarSyncOutcome.NeedsConsent(fetched.recovery)

                is CalendarCall.Failure -> return CalendarSyncOutcome.Failed(fetched.message)
                is CalendarCall.Success -> {
                    remote = fetched.value.associateBy { it.id }
                    // The read succeeded, so the throttle window starts now even if nothing
                    // changed -- `SyncHealthDataUseCase`'s reasoning, and for the same reason:
                    // otherwise an empty calendar is re-read on every single foreground.
                    preferences.setCalendarLastPullAt(uid, nowMillis)
                    val plan = CalendarSync.pullPlan(
                        entries = entries,
                        remote = fetched.value,
                        judgedFrom = windowFrom,
                        judgedTo = windowTo,
                    )
                    plan.retimed.forEach { move ->
                        val committed = commit(
                            move.entry,
                            move.entry.occurrence.copy(occurrence = move.moved),
                        )
                        if (committed) retimed++
                    }
                    // §2.7: keeps its date, clears its `googleEventId`, and the ambiguity is
                    // asked. The clear happens here; the asking is `disappearances`.
                    plan.disappeared.forEach { entry ->
                        if (clearLink(entry)) disappeared++
                    }
                    if (plan.disappeared.isNotEmpty()) {
                        _disappearances.update { held ->
                            val ids = held.map { it.occurrence.id }.toSet()
                            held + plan.disappeared.filter { it.occurrence.id !in ids }
                        }
                    }
                    // plan.external is deliberately unused: §2.7 leaves a hand-made event
                    // alone, and §2.8 forbids silent cleanup. Nothing to do is the behaviour.
                }
            }
        }

        // §2.7: push is not throttled -- a write must never lag the user. Re-read after the
        // pull, because a retime above changed the occurrences this list is built from.
        val afterPull = if (retimed > 0 || disappeared > 0) {
            entriesIn(windowFrom, windowTo, zone) ?: entries
        } else {
            entries
        }
        val pushes = CalendarSync.pushPlan(
            entries = afterPull,
            remote = remote,
            now = now,
            unknownRemote = if (mayPull) UnknownRemote.LEAVE_ALONE else UnknownRemote.ASSUME_STALE,
        )

        var inserted = 0
        var updated = 0
        var cancelled = 0
        for (push in pushes) {
            when (push) {
                is CalendarPush.Insert -> when (
                    val created = calendar.insertEvent(calendarId, push.draft)
                ) {
                    is CalendarCall.Success ->
                        if (link(push.entry, created.value)) inserted++

                    is CalendarCall.NeedsConsent ->
                        return CalendarSyncOutcome.NeedsConsent(created.recovery)

                    is CalendarCall.Failure -> Unit
                }

                is CalendarPush.Update -> when (
                    val patched = calendar.updateEvent(calendarId, push.eventId, push.draft)
                ) {
                    is CalendarCall.Success -> updated++
                    is CalendarCall.NeedsConsent ->
                        return CalendarSyncOutcome.NeedsConsent(patched.recovery)

                    is CalendarCall.Failure -> Unit
                }

                is CalendarPush.Cancel -> when (
                    val removed = calendar.cancelEvent(calendarId, push.eventId)
                ) {
                    is CalendarCall.Success -> if (clearLink(push.entry)) cancelled++
                    is CalendarCall.NeedsConsent ->
                        return CalendarSyncOutcome.NeedsConsent(removed.recovery)

                    is CalendarCall.Failure -> Unit
                }
            }
        }

        return CalendarSyncOutcome.Synced(
            inserted = inserted,
            updated = updated,
            cancelled = cancelled,
            retimed = retimed,
            disappeared = disappeared,
        )
    }

    /**
     * Answers §2.7's disappearance question for one occurrence.
     *
     * The entry leaves [disappearances] whichever answer it gets — including
     * [DisappearanceChoice.KEEP], which is *"nothing further happens"* and is therefore
     * complete the moment it is chosen.
     */
    suspend fun resolve(
        entry: CalendarEntry,
        choice: DisappearanceChoice,
        nowMillis: Long = System.currentTimeMillis(),
    ): CalendarSyncOutcome {
        val outcome = when (choice) {
            DisappearanceChoice.KEEP -> CalendarSyncOutcome.Synced()

            DisappearanceChoice.CANCEL -> {
                occurrences.setOutcome(
                    occurrenceId = entry.occurrence.id,
                    outcome = OccurrenceOutcome.Skipped(nowMillis),
                )
                CalendarSyncOutcome.Synced(cancelled = 1)
            }

            DisappearanceChoice.PUT_BACK -> {
                val draft = CalendarSync.draftFor(entry)
                    ?: return CalendarSyncOutcome.Failed("That is no longer something to put back")
                val uid = auth.currentUid() ?: return CalendarSyncOutcome.NotSignedIn
                when (val id = resolveCalendarId(uid)) {
                    is CalendarCall.NeedsConsent ->
                        return CalendarSyncOutcome.NeedsConsent(id.recovery)

                    is CalendarCall.Failure -> return CalendarSyncOutcome.Failed(id.message)
                    is CalendarCall.Success -> when (
                        val created = calendar.insertEvent(id.value, draft)
                    ) {
                        is CalendarCall.Success -> {
                            link(entry, created.value)
                            CalendarSyncOutcome.Synced(inserted = 1)
                        }

                        is CalendarCall.NeedsConsent ->
                            return CalendarSyncOutcome.NeedsConsent(created.recovery)

                        is CalendarCall.Failure ->
                            return CalendarSyncOutcome.Failed(created.message)
                    }
                }
            }
        }
        _disappearances.update { held -> held.filterNot { it.occurrence.id == entry.occurrence.id } }
        return outcome
    }

    /**
     * The calendar's id, from this install's memory of it or from Google.
     *
     * Cached **per uid**, so an account switch starts from nothing rather than writing the
     * previous account's events into whichever calendar id happened to be lying around. §2.7:
     * *"an account switch reads as **not mirrored**, not as events to patch."*
     */
    private suspend fun resolveCalendarId(uid: String): CalendarCall<String> {
        preferences.goalPilotCalendarId(uid)?.takeIf { it.isNotBlank() }?.let {
            return CalendarCall.Success(it)
        }
        val ensured = calendar.ensureCalendar()
        if (ensured is CalendarCall.Success) preferences.setGoalPilotCalendarId(uid, ensured.value)
        return ensured
    }

    /**
     * Every occurrence in the sync window, paired with its task.
     *
     * `null` — rather than an empty list — when a lookup did not come back. The distinction is
     * `SyncHealthDataUseCase`'s and matters for the same reason: an empty schedule and an
     * unread one are indistinguishable downstream, and treating the second as the first would
     * make the push plan cancel every future event on the calendar.
     */
    private suspend fun entriesIn(
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): List<CalendarEntry>? {
        val allTasks = withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { tasks.observeTasks().first() }
            ?: return null
        val stored = withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { occurrences.observeOccurrences().first() }
            ?: return null
        val byTask: Map<String, List<ScheduledOccurrence>> = stored.groupBy { it.taskId }
        return allTasks.flatMap { task ->
            TaskSchedule(task = task, stored = byTask[task.id].orEmpty())
                .occurrencesIn(from = from, to = to, zone = zone)
                .map { CalendarEntry(task = task, occurrence = it) }
        }
    }

    /**
     * Stores [eventId] against the occurrence, **creating its document if the instance was
     * still only a rule**.
     *
     * §2.1 lists exactly this as one of the four reasons a document comes into being: *"a
     * document exists for an instance the user touched — moved it, skipped it, did it, **or
     * gave it a Google event id**."* A generated instance has a blank id, so `linkGoogleEvent`
     * has nothing to update and the write goes through `apply`, which mints the id the same way
     * `upsertTask` does.
     */
    private suspend fun link(entry: CalendarEntry, eventId: String): Boolean =
        if (entry.occurrence.id.isBlank()) {
            commit(entry, entry.occurrence.copy(googleEventId = eventId))
        } else {
            occurrences.linkGoogleEvent(entry.occurrence.id, eventId) is
                Resource.Success
        }

    /** §2.7's *"keeps its date, clears its `googleEventId`"* — never a delete of the document. */
    private suspend fun clearLink(entry: CalendarEntry): Boolean =
        if (entry.occurrence.id.isBlank()) {
            true // Never linked in the first place; there is nothing stored to clear.
        } else {
            occurrences.linkGoogleEvent(entry.occurrence.id, null) is
                Resource.Success
        }

    /**
     * Writes one occurrence document, task included, through the batch `apply` already used by
     * every scoped edit. A blank id means *create*, which `OccurrenceRepositoryImpl.apply`
     * already handles.
     */
    private suspend fun commit(entry: CalendarEntry, updated: ScheduledOccurrence): Boolean =
        occurrences.apply(
            SchedulePlan.Writes(task = entry.task, upserts = listOf(updated)),
        ) is Resource.Success

    companion object {

        /**
         * How far ahead the sync mirrors. §4.3's surface shows a week; sixty days is far enough
         * that a deadline set for next month is in Google the moment it is set, and near enough
         * that one range query answers for it.
         */
        const val WINDOW_DAYS: Long = 60

        /**
         * How far **back** the sync still looks.
         *
         * Not zero, because §2.8 keeps past events — *"past events stay as the record of time
         * actually spent"* — and a slot moved yesterday evening in Google should still reach
         * the app. Not large either: `pushPlan` refuses to *create* an event for a window that
         * has already closed, so widening this buys re-reads and never a backfill.
         */
        const val PAST_DAYS: Long = 7

        /**
         * How much **wider than the judged window** the Google query reaches, on both sides.
         *
         * `CalendarSync.pullPlan` only calls an absent event a disappearance inside the judged
         * range, precisely so that an event dragged a few days out of it is *found* rather than
         * mistaken for a delete. That margin is this constant, and setting it to zero
         * reintroduces the bug: the two numbers are one decision and are written once here.
         */
        const val QUERY_MARGIN_DAYS: Long = 14

        /** Ceiling on each Firestore lookup before the sync gives up unfinished. */
        private const val LOOKUP_TIMEOUT_MS = 5_000L
    }
}
