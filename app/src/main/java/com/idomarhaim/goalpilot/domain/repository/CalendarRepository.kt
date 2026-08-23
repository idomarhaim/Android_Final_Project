package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.domain.model.CalendarCall
import com.idomarhaim.goalpilot.domain.model.CalendarConsent
import com.idomarhaim.goalpilot.domain.model.CalendarEventDraft
import com.idomarhaim.goalpilot.domain.model.CalendarScope
import com.idomarhaim.goalpilot.domain.model.RemoteEvent
import java.time.LocalDate

/**
 * §2.6's Google Calendar, behind an interface —
 * [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61).
 *
 * ### Every method is a call GoalPilot actually makes, and there are deliberately no others
 *
 * §2.7 requires the restraint to be *"visible in **which call is made**, rather than as a
 * filter after the fact"*. An interface with a general `get(url)` on it, or one that could
 * read another calendar's events without saying so in its own name, would put that promise
 * back into the implementation where nobody can see it. So there is no `readCalendar`, no
 * `search`, and [freeBusy] returns intervals because that is all the endpoint behind it can
 * return — the promise is Google-enforced there, not ours to keep.
 *
 * ### Nothing here decides anything
 *
 * The rules are `CalendarSync`'s, and they are pure. This interface only performs what that
 * produced — the same split `OccurrenceRepository` already makes, for the same reason: *"a rule
 * that can only be exercised against a live Firestore is a rule whose branches do not all get
 * tested"*, and a live Google account is harder to reach than a live Firestore.
 *
 * ### It never deletes a calendar
 *
 * §2.7: *"**Sign-out does not delete** the calendar Ido owns. An account switch reads as *not
 * mirrored*, not as events to patch."* There is no `deleteCalendar` on this interface and one
 * must not be added: the calendar belongs to Ido (§2.6), so removing it is his to do in
 * Google's own UI.
 */
interface CalendarRepository {

    /**
     * Whether the signed-in account holds [scope], read from the cached sign-in with no network
     * call.
     *
     * The same cheap up-front probe `GoogleTasksClient.consentState` is, and with the same
     * honest limit: the authoritative answer is whatever the next call returns, because a scope
     * granted through a recovery screen need not appear in the cached account.
     */
    suspend fun consentState(scope: CalendarScope = CalendarScope.APP_CREATED): CalendarConsent

    /**
     * The GoalPilot calendar's id, **creating it if it is not there** — §2.6's *"created
     * client-side"*.
     *
     * Idempotent by construction: an implementation looks for a calendar this app already
     * created before making one, so calling it on every sync costs a lookup and never a second
     * calendar.
     */
    suspend fun ensureCalendar(): CalendarCall<String>

    /** Creates one event and returns Google's id for it, which becomes `googleEventId`. */
    suspend fun insertEvent(calendarId: String, draft: CalendarEventDraft): CalendarCall<String>

    /**
     * Patches an event's **times and title**, and nothing else.
     *
     * A patch rather than a whole-event write, so a colour, a reminder or a guest the user
     * added by hand in Google's own UI survives a sync — §2.7's *"an event Ido creates by hand
     * … is left alone"* applies with equal force to something he added *to* one of ours.
     */
    suspend fun updateEvent(
        calendarId: String,
        eventId: String,
        draft: CalendarEventDraft,
    ): CalendarCall<Unit>

    /**
     * Cancels an event — §2.8's *"deletion is cancellation"*, into Google's 30-day trash.
     *
     * Named for what it does to the user's data rather than for the HTTP verb behind it: the
     * event is recoverable for thirty days, which is what makes *"a wrong deletion is
     * recoverable twice"* true.
     */
    suspend fun cancelEvent(calendarId: String, eventId: String): CalendarCall<Unit>

    /**
     * Every event on the GoalPilot calendar between [from] and [to] inclusive, **including
     * cancelled ones**.
     *
     * The cancelled ones are the point: §2.7's disappearance is detected by an event coming
     * back `cancelled`, and a query that hid them would leave a delete indistinguishable from a
     * window the range happened to miss. Callers must query wider than the range they are
     * willing to judge — see `CalendarSync.pullPlan`.
     */
    suspend fun listEvents(
        calendarId: String,
        from: LocalDate,
        to: LocalDate,
    ): CalendarCall<List<RemoteEvent>>

    /**
     * The names and ids of the user's calendars — §2.7's sign-in row, *"`calendar.app.created`
     * + the calendar **list**"*.
     *
     * Names only. It is what lets the settings surface offer *"which calendars should the agent
     * avoid?"* without having read a single event out of any of them.
     */
    suspend fun listCalendars(): CalendarCall<List<CalendarSummary>>

    /**
     * Busy intervals across [calendarIds] — §2.4's *"unless the slot is free on every calendar
     * Ido has chosen to share"*.
     *
     * **No titles ever reach the app**, and that is Google's enforcement rather than ours: the
     * `freeBusy` endpoint returns intervals and nothing else. The scope asked for is
     * [CalendarScope.FREE_BUSY], which is the narrowest thing that answers the question.
     */
    suspend fun freeBusy(
        calendarIds: List<String>,
        from: LocalDate,
        to: LocalDate,
    ): CalendarCall<List<BusyInterval>>
}

/** One of the user's calendars, by name. Deliberately carries nothing from inside it. */
data class CalendarSummary(
    val id: String,
    val name: String,
    /** True for the calendar this app created, so the settings surface can say which is ours. */
    val createdByThisApp: Boolean = false,
)

/**
 * A window in which the user is busy on some calendar they chose to share.
 *
 * It has no title, no id and no calendar attached, because the endpoint that produces it has
 * none to give. §2.4 only needs *"is 09:00 taken?"*, and this is exactly that and no more.
 */
data class BusyInterval(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)
