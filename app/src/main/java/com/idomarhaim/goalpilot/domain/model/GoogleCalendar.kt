package com.idomarhaim.goalpilot.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * §2.7's **incremental authorization** table, as three values
 * ([`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61)).
 *
 * > | Trigger | Scope asked |
 * > |---|---|
 * > | sign-in | `calendar.app.created` + the calendar **list** |
 * > | ticking calendars to avoid | `calendar.events.freebusy` |
 * > | first setting one calendar to **Full** | `calendar.readonly` |
 *
 * ### It is an enum because the restraint has to be visible in *which call is made*
 *
 * §2.7: *"Google cannot enforce that split (scopes are per-scope, never per-calendar) … so it
 * is a promise GoalPilot keeps … and the restraint is visible in **which call is made**,
 * rather than as a filter after the fact."* A single omnibus scope requested at sign-in would
 * make every later restraint a client-side filter over data the app had already been handed —
 * which is exactly the shape that sentence rejects. So each call names the scope it needs, and
 * a scope the user never triggers is never asked for.
 *
 * Everything in this file is free of Android and of Google's wire format, for the reason
 * [TasksConsent]'s KDoc already gives: `feature → domain → data` is the layering, and the
 * screens that render a consent state or a disappearance are `feature/`. The one thing that
 * genuinely cannot cross that line — the platform consent screen, which is an `Intent` — is
 * carried behind [ConsentRecovery], an interface the domain declares and only `data/calendar`
 * implements.
 */
enum class CalendarScope(val url: String) {

    /**
     * `calendar.app.created` — **the whole feature**, and the only scope the write path uses.
     *
     * §2.6: it *"creates the calendar, writes its events and colours it"*, and §2.7 adds
     * *"two-way, at no extra scope — `calendar.app.created` already reads back what it
     * wrote"*. Its blast radius is calendars **this app created**: it cannot see, move or
     * delete anything else Ido owns, which is what makes the two-way sync safe to run
     * unattended.
     */
    APP_CREATED("https://www.googleapis.com/auth/calendar.app.created"),

    /**
     * `calendar.events.freebusy` — busy/free windows with **no titles**, Google-enforced.
     *
     * Asked when Ido ticks calendars for the agent to avoid (§2.4's *"unless the slot is free
     * on every calendar Ido has chosen to share"*). The enforcement is Google's, not ours: the
     * endpoint returns intervals and nothing else, so a promise not to read titles is one the
     * app is structurally incapable of breaking.
     */
    FREE_BUSY("https://www.googleapis.com/auth/calendar.events.freebusy"),

    /**
     * `calendar.readonly` — the one that reads other calendars' contents.
     *
     * §2.7: asked only on *"first setting one calendar to Full"*, **with that calendar named
     * in the sentence**. This is the scope Google cannot narrow to one calendar, so it is the
     * one place the promise is ours to keep. If Ido never uses Full, it is never asked for and
     * the promise is never made.
     */
    READONLY("https://www.googleapis.com/auth/calendar.readonly");

    /** The same scope in the `oauth2:` form `GoogleAuthUtil.getToken` expects. */
    val oauth2: String get() = "oauth2:$url"
}

/**
 * Whether the signed-in account holds a given [CalendarScope].
 *
 * The same three cases as [TasksConsent] and deliberately a separate type: a user can hold the
 * Tasks scope and not the calendar one, and collapsing them would make *"you declined the
 * calendar"* readable off a Tasks grant. §2.6 makes the split load-bearing — *"the `View your
 * tasks` consent checkbox arrives unchecked (#36), so sign-in can succeed while granting
 * nothing"* — and the calendar checkbox arrives the same way.
 */
enum class CalendarConsent {
    /** The scope is held; the call can run. */
    GRANTED,

    /** Signed in, and this scope is not granted. Normal, not exceptional (§2.6). */
    MISSING,

    /** No Google account is cached, so nothing was asked and nothing was declined. */
    NOT_SIGNED_IN,
}

/**
 * An opaque handle to the platform's own consent screen for a scope that was refused.
 *
 * The domain must not name `android.content.Intent`, and the UI must be able to launch one.
 * This interface is the seam: `data/calendar` wraps the `Intent` that
 * `UserRecoverableAuthException` hands back, the domain passes the wrapper through untouched,
 * and only the launcher unwraps it. `GoogleTasksClient` could not do this — its result types
 * carry the `Intent` directly and therefore had to live in `data/`.
 */
interface ConsentRecovery

/**
 * The outcome of one call against Google Calendar.
 *
 * ### One generic type here, where Google Tasks deliberately has two concrete ones
 *
 * `TasksImportResult` / `TaskListsResult` are two hand-written copies, and their KDoc gives
 * the reason: *"the two calls are consumed by different screens, and a generic sealed
 * interface behind a typealias reads worse at both call sites than four extra lines here."*
 * That argument inverts here. This client has **six** calls — ensure, insert, patch, cancel,
 * list events, list calendars — and every one of them is consumed by the *same* sync path,
 * which threads them together and has to branch on the same three cases each time. Six
 * hand-written triples would be eighteen classes that only `SyncCalendarUseCase` reads.
 *
 * [NeedsConsent] is **not a failure**: §2.6 says a partial grant is the normal case, and every
 * caller degrades rather than blocking.
 */
sealed interface CalendarCall<out T> {

    data class Success<out T>(val value: T) : CalendarCall<T>

    /**
     * The account exists and has not granted [scope]. [recovery] is the platform's own consent
     * screen where one is available, and `null` where the failure was not recoverable that way.
     */
    data class NeedsConsent(
        val scope: CalendarScope,
        val recovery: ConsentRecovery? = null,
    ) : CalendarCall<Nothing>

    data class Failure(val message: String) : CalendarCall<Nothing>

    /**
     * The payload, or `null` for either non-success.
     *
     * `Success` is declared `out` so this needs no cast that the compiler cannot check — the
     * unchecked-cast warning an invariant `Success<T>` would produce here is not cosmetic, it
     * is the compiler saying it cannot prove the thing this property asserts.
     */
    val valueOrNull: T? get() = (this as? Success<T>)?.value
}

/**
 * **What GoalPilot asks Google to store** — the write half of §2.7's *"times in both
 * directions and state in neither"*.
 *
 * ### There is no state field here, and that is the design rather than an omission
 *
 * §2.7: *"A Google event has a start, an end and a title, and **no field for
 * `MISSED`/`OVERDUE`/`EXPIRED`/`PROVISIONAL`** … Every alternative ends in an encoding — a ✓
 * in a title, a colour meaning late — that nothing else respects and the user can destroy by
 * typing."* So this type has four fields and must not grow a fifth: if you find yourself
 * wanting to put an [OccurrenceState] on an event, the answer is that Google is not where that
 * lives.
 *
 * ### All-day and timed are one type with a flag, not two subclasses
 *
 * Unlike [Occurrence], which is sealed precisely so that *"an `ALL_DAY` with an end time is
 * unrepresentable"*, this is a **wire shape** and its two forms differ by which JSON key the
 * same two instants are written under (`date` vs `dateTime`). Making it sealed would buy an
 * unrepresentable state that the REST layer has to re-flatten one line later.
 */
data class CalendarEventDraft(
    val title: String,
    /** Inclusive start. For an all-day draft the time-of-day is ignored on the wire. */
    val start: LocalDateTime,
    /**
     * **Exclusive** end, the same half-open convention [Occurrence.closesAt] uses — and the
     * same one Google's own API uses for all-day events, where `end.date` is the day *after*
     * the last day.
     */
    val end: LocalDateTime,
    val allDay: Boolean,
) {
    /** The first day the event covers. */
    val startDate: LocalDate get() = start.toLocalDate()

    /** The last day the event covers, **inclusive** — [end] is exclusive. */
    val endDateInclusive: LocalDate
        get() = end.toLocalDate().minusDays(1).coerceAtLeast(startDate)

    /**
     * Whether [other] describes the same *when*, **ignoring the title**.
     *
     * §2.7: *"Titles are written but never read back."* So the comparison that decides *"has
     * Google drifted from us?"* must not include the title, or a Google-side rename would
     * present as a drift and be patched back — writing over what the user typed, which is
     * precisely the silent overwrite that clause exists to prevent.
     */
    fun sameTimesAs(other: CalendarEventDraft): Boolean =
        allDay == other.allDay && start == other.start && end == other.end
}

/**
 * **What Google says an event is now** — the read half of the sync.
 *
 * [cancelled] rather than an absence, because a pull asks for deleted events explicitly
 * (`showDeleted=true`): §2.8 puts a deletion in Google's 30-day trash, and an event merely
 * *dropped from the response* would be indistinguishable from one whose window the query
 * happened to miss. That distinction is what `CalendarSync.pullPlan`'s judged-window argument
 * exists to keep.
 */
data class RemoteEvent(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    /** **Exclusive**, as [CalendarEventDraft.end] is. */
    val end: LocalDateTime,
    val allDay: Boolean,
    /** True for Google's `status: "cancelled"` — trashed, restorable for 30 days (§2.8). */
    val cancelled: Boolean = false,
) {
    /** This event as a draft, so it can be compared with what we meant to write. */
    fun asDraft(): CalendarEventDraft =
        CalendarEventDraft(title = title, start = start, end = end, allDay = allDay)
}

/**
 * **The name and colour of the calendar §2.6 creates.**
 *
 * The calendar is **Ido's, not the app's** (§2.6), so its name is what he will see in a list
 * beside calendars he made himself — and the app must not create a second one on every launch,
 * which is what [SUMMARY] being a constant is for.
 */
object GoalPilotCalendar {

    /** The calendar's `summary`. Changing this strands the calendar already created. */
    const val SUMMARY = "GoalPilot"

    /**
     * Shown under the name in Google Calendar's own settings — the only place the app can
     * explain itself to someone looking at a calendar they do not recognise.
     */
    const val DESCRIPTION =
        "Created by GoalPilot. Blocks, deadlines and spans you confirmed in the app appear " +
            "here. Deleting an event here asks GoalPilot what to do with it. This calendar is " +
            "yours: signing out of GoalPilot does not remove it."

    /**
     * The `colorId` for the calendar, from Google's fixed calendar palette.
     *
     * §2.6 says the scope *"colours it"*, and a colour is the cheapest way for one calendar in
     * a list of eight to be recognisable. It is a **calendar** colour and never a per-event
     * one: §2.7 rules out *"a colour meaning late"*, and an event-level colour is exactly the
     * encoding that clause forbids.
     */
    const val COLOR_ID = "11"

    /** How a [Deadline]'s time-of-day is written into the banner's title. */
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * §2.7's banner title — *"`Due 23:59 · Submit report`"*.
     *
     * The time is **in the words** because the event itself is all-day and therefore carries
     * none: *"the Google event does not remind (§2.5's local notification does), so its only
     * job is to be **seen**, which a banner does and a 23:59 marker does not."* Dropping the
     * hour entirely would make the banner say less than the app knows, which §0.4 forbids as
     * much as it forbids an outright silence.
     */
    fun deadlineTitle(at: LocalDateTime, taskTitle: String): String =
        "Due ${TIME.format(at.toLocalTime())} · $taskTitle"
}
