package com.idomarhaim.goalpilot.data.calendar

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.domain.model.CalendarCall
import com.idomarhaim.goalpilot.domain.model.CalendarConsent
import com.idomarhaim.goalpilot.domain.model.CalendarEventDraft
import com.idomarhaim.goalpilot.domain.model.CalendarScope
import com.idomarhaim.goalpilot.domain.model.ConsentRecovery
import com.idomarhaim.goalpilot.domain.model.GoalPilotCalendar
import com.idomarhaim.goalpilot.domain.model.RemoteEvent
import com.idomarhaim.goalpilot.domain.repository.BusyInterval
import com.idomarhaim.goalpilot.domain.repository.CalendarRepository
import com.idomarhaim.goalpilot.domain.repository.CalendarSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The `Intent` half of [ConsentRecovery] — Google's own consent screen for a refused scope.
 *
 * The wrapper exists so the domain never names `android.content.Intent`. Whoever launches it
 * casts back to this; nothing between here and there has to know what is inside.
 */
data class IntentConsentRecovery(val intent: Intent) : ConsentRecovery

/**
 * §2.6's Google Calendar, over REST —
 * [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61).
 *
 * ### Why REST and not `google-api-services-calendar`
 *
 * `GoogleTasksClient`'s reason, unchanged and now weighed a second time: the official client
 * *"pulls in several megabytes and its own auth flow, when all this needs is a few requests
 * against an endpoint we already have credentials for"*. This one makes six kinds of request
 * rather than two, which is still nowhere near the weight of a second auth stack — and the six
 * are listed on [CalendarRepository] precisely so that the set stays small and visible.
 *
 * ### The scope is asked for per call, and that is §2.7's promise in code
 *
 * Every method below mints its token for **one** [CalendarScope]. §2.7: *"the restraint is
 * visible in **which call is made**, rather than as a filter after the fact"* — so
 * [freeBusy] cannot accidentally run on a `calendar.readonly` token it happened to have,
 * because it never asks for one.
 *
 * ### A refused scope is not a failure
 *
 * §2.6: *"sign-in can succeed while granting nothing … Every calendar feature therefore
 * degrades legibly and none gates the app."* So a missing grant comes back as
 * [CalendarCall.NeedsConsent] carrying the recovery screen, exactly as
 * [com.idomarhaim.goalpilot.data.tasks.TasksImportResult.NeedsConsent] does, and every caller
 * is expected to carry on without the feature rather than stop.
 */
@Singleton
class GoogleCalendarClient @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CalendarRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    /**
     * The zone every `dateTime` is written in and read back into.
     *
     * Read once, at the edge, and never inside the sync rules: `CalendarSync` takes its dates
     * as `LocalDate`/`LocalDateTime` and never asks what zone they are in, which is the same
     * discipline `TaskSchedule.occurrencesIn` enforces by taking a `ZoneId` argument rather
     * than defaulting one. A device that changes zone changes what a timed event means, and
     * that is Google's semantics too — an event at 09:00 is at 09:00 where it was written.
     */
    private val zone: ZoneId get() = ZoneId.systemDefault()

    override suspend fun consentState(scope: CalendarScope): CalendarConsent = withContext(io) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext CalendarConsent.NOT_SIGNED_IN
        if (GoogleSignIn.hasPermissions(account, Scope(scope.url))) {
            CalendarConsent.GRANTED
        } else {
            CalendarConsent.MISSING
        }
    }

    /**
     * §2.6's *"created **client-side**"*, and the whole of *"a service-account owner is
     * actively wrong"* in one method: the calendar is created by the signed-in user's own
     * token, so **Ido owns it**.
     *
     * Looks before it creates. `calendar.app.created` scopes `calendarList` to calendars this
     * app made, so an entry whose summary matches [GoalPilotCalendar.SUMMARY] is ours — and if
     * the list call is refused or empty, creating is still correct, because a calendar we
     * cannot see is one we cannot write to either.
     */
    override suspend fun ensureCalendar(): CalendarCall<String> = withContext(io) {
        when (val existing = listCalendars()) {
            is CalendarCall.Success -> existing.value
                .firstOrNull { it.createdByThisApp }
                ?.let { return@withContext CalendarCall.Success(it.id) }

            is CalendarCall.NeedsConsent -> return@withContext existing
            is CalendarCall.Failure -> Unit // Fall through and create; see the KDoc.
        }
        request(
            method = "POST",
            url = "$BASE/calendars",
            scope = CalendarScope.APP_CREATED,
            body = json.encodeToString(
                CalendarWriteDto(
                    summary = GoalPilotCalendar.SUMMARY,
                    description = GoalPilotCalendar.DESCRIPTION,
                ),
            ),
        ).mapCatching { body ->
            val id = json.decodeFromString<CalendarDto>(body).id
            if (id.isBlank()) throw IOException("Google created a calendar with no id")
            colour(id)
            id
        }
    }

    /**
     * §2.6's *"and colours it"*.
     *
     * A failure here is **swallowed on purpose**: the calendar exists and is writable, and
     * refusing to return its id because it came out the wrong colour would gate the whole
     * feature on decoration. It is a `calendarList` patch rather than a `calendars` one because
     * colour is a property of *this user's subscription* to a calendar, not of the calendar.
     */
    private fun colour(calendarId: String) {
        runCatching {
            request(
                method = "PATCH",
                url = "$BASE/users/me/calendarList/${calendarId.enc()}",
                scope = CalendarScope.APP_CREATED,
                body = json.encodeToString(CalendarColourDto(colorId = GoalPilotCalendar.COLOR_ID)),
            )
        }
    }

    override suspend fun insertEvent(
        calendarId: String,
        draft: CalendarEventDraft,
    ): CalendarCall<String> = withContext(io) {
        request(
            method = "POST",
            url = "$BASE/calendars/${calendarId.enc()}/events",
            scope = CalendarScope.APP_CREATED,
            body = json.encodeToString(draft.toWireDto()),
        ).mapCatching { body ->
            val id = json.decodeFromString<EventDto>(body).id
            if (id.isBlank()) throw IOException("Google created an event with no id")
            id
        }
    }

    override suspend fun updateEvent(
        calendarId: String,
        eventId: String,
        draft: CalendarEventDraft,
    ): CalendarCall<Unit> = withContext(io) {
        request(
            method = "PATCH",
            url = "$BASE/calendars/${calendarId.enc()}/events/${eventId.enc()}",
            scope = CalendarScope.APP_CREATED,
            body = json.encodeToString(draft.toWireDto()),
        ).mapCatching { }
    }

    /**
     * §2.8's *"deletion is cancellation"*. `DELETE` on the Calendar API is not a purge: the
     * event moves to the trash and stays restorable for thirty days, which is what the method
     * name says and the verb does not.
     */
    override suspend fun cancelEvent(
        calendarId: String,
        eventId: String,
    ): CalendarCall<Unit> = withContext(io) {
        request(
            method = "DELETE",
            url = "$BASE/calendars/${calendarId.enc()}/events/${eventId.enc()}",
            scope = CalendarScope.APP_CREATED,
            // A DELETE on an event already gone is a 410, and that is the state we wanted.
            treatAsSuccess = setOf(HttpURLConnection.HTTP_GONE, HttpURLConnection.HTTP_NOT_FOUND),
        ).mapCatching { }
    }

    override suspend fun listEvents(
        calendarId: String,
        from: LocalDate,
        to: LocalDate,
    ): CalendarCall<List<RemoteEvent>> = withContext(io) {
        val timeMin = from.atStartOfDay(zone).toOffsetDateTime().format(RFC3339)
        val timeMax = to.plusDays(1).atStartOfDay(zone).toOffsetDateTime().format(RFC3339)
        request(
            method = "GET",
            url = "$BASE/calendars/${calendarId.enc()}/events" +
                "?timeMin=${timeMin.enc()}&timeMax=${timeMax.enc()}" +
                // Both flags are load-bearing. `showDeleted` is how §2.7's disappearance is
                // seen at all; `singleEvents` expands Google-side recurrence, so a series
                // someone created by hand arrives as instances we can compare with ours
                // rather than as one master event on a date none of them fall on.
                "&showDeleted=true&singleEvents=true&maxResults=$MAX_EVENTS",
            scope = CalendarScope.APP_CREATED,
        ).mapCatching { body ->
            json.decodeFromString<EventsResponse>(body).items.mapNotNull { it.toRemoteEvent() }
        }
    }

    override suspend fun listCalendars(): CalendarCall<List<CalendarSummary>> = withContext(io) {
        request(
            method = "GET",
            url = "$BASE/users/me/calendarList?maxResults=250&showHidden=true",
            scope = CalendarScope.APP_CREATED,
        ).mapCatching { body ->
            json.decodeFromString<CalendarListResponse>(body).items
                .filter { it.id.isNotBlank() }
                .map { entry ->
                    CalendarSummary(
                        id = entry.id,
                        name = entry.summary.orEmpty(),
                        // `Inferred:` under `calendar.app.created` this list holds only
                        // calendars this app made, so the name match is a second belt rather
                        // than the only one. It is here because the scope's exact listing
                        // behaviour is not something this repo has measured, and creating a
                        // duplicate calendar is the expensive way to find out.
                        createdByThisApp = entry.summary == GoalPilotCalendar.SUMMARY,
                    )
                }
        }
    }

    override suspend fun freeBusy(
        calendarIds: List<String>,
        from: LocalDate,
        to: LocalDate,
    ): CalendarCall<List<BusyInterval>> = withContext(io) {
        if (calendarIds.isEmpty()) return@withContext CalendarCall.Success(emptyList())
        val body = json.encodeToString(
            FreeBusyRequest(
                timeMin = from.atStartOfDay(zone).toOffsetDateTime().format(RFC3339),
                timeMax = to.plusDays(1).atStartOfDay(zone).toOffsetDateTime().format(RFC3339),
                items = calendarIds.map { FreeBusyItem(it) },
            ),
        )
        request(
            method = "POST",
            url = "$BASE/freeBusy",
            // The narrow scope, and the reason §2.7 can promise no titles are read: this
            // endpoint has none to return.
            scope = CalendarScope.FREE_BUSY,
            body = body,
        ).mapCatching { response ->
            json.decodeFromString<FreeBusyResponse>(response).calendars.values
                .flatMap { it.busy }
                .mapNotNull { period ->
                    val start = runCatching { OffsetDateTime.parse(period.start) }.getOrNull()
                    val end = runCatching { OffsetDateTime.parse(period.end) }.getOrNull()
                    if (start == null || end == null) null
                    else BusyInterval(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
                }
        }
    }

    // ── Transport ─────────────────────────────────────────────────────────

    /**
     * One HTTP call, with the token for [scope] minted first.
     *
     * Returns the response body as a string, or the reason it could not. A
     * [UserRecoverableAuthException] becomes [CalendarCall.NeedsConsent] rather than an error,
     * because §2.6 says a missing grant is the normal case.
     */
    private fun request(
        method: String,
        url: String,
        scope: CalendarScope,
        body: String? = null,
        treatAsSuccess: Set<Int> = emptySet(),
    ): CalendarCall<String> {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: return CalendarCall.Failure("Not signed in with Google")
        val token = try {
            GoogleAuthUtil.getToken(context, account, scope.oauth2)
        } catch (e: UserRecoverableAuthException) {
            return CalendarCall.NeedsConsent(
                scope = scope,
                recovery = e.intent?.let(::IntentConsentRecovery),
            )
        } catch (e: Exception) {
            return CalendarCall.Failure(e.message ?: "Could not obtain a Google Calendar token")
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        return try {
            body?.let { connection.outputStream.use { out -> out.write(it.toByteArray()) } }
            val code = connection.responseCode
            when {
                code in 200..299 ->
                    CalendarCall.Success(
                        connection.inputStream.bufferedReader().use { it.readText() },
                    )

                code in treatAsSuccess -> CalendarCall.Success("")

                // 403 here is a granted-scope call the account is not allowed to make (a
                // calendar it does not own); 401 is a token the server rejected. Neither is
                // recoverable by the consent screen, so neither pretends to be.
                else -> {
                    val detail = connection.errorStream?.bufferedReader()
                        ?.use { it.readText() }.orEmpty().take(200)
                    CalendarCall.Failure("Google Calendar returned HTTP $code $detail".trim())
                }
            }
        } catch (e: Exception) {
            CalendarCall.Failure(e.message ?: "Could not reach Google Calendar")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Maps a successful body, turning a decode failure into [CalendarCall.Failure] rather than
     * an exception crossing the repository boundary. `NeedsConsent` passes through untouched —
     * it is not an error and must not be flattened into one.
     */
    private inline fun <T> CalendarCall<String>.mapCatching(
        transform: (String) -> T,
    ): CalendarCall<T> = when (this) {
        is CalendarCall.Success -> try {
            CalendarCall.Success(transform(value))
        } catch (e: Exception) {
            CalendarCall.Failure(e.message ?: "Could not read Google Calendar's reply")
        }

        is CalendarCall.NeedsConsent -> this
        is CalendarCall.Failure -> this
    }

    private fun String.enc(): String = URLEncoder.encode(this, "UTF-8")

    // ── Wire format (only the fields we produce or consume) ────────────────

    private fun CalendarEventDraft.toWireDto(): EventWriteDto = EventWriteDto(
        summary = title,
        start = if (allDay) {
            EventTimeDto(date = startDate.toString())
        } else {
            EventTimeDto(dateTime = start.format(LOCAL_DATE_TIME), timeZone = zone.id)
        },
        end = if (allDay) {
            // Google's all-day `end.date` is exclusive, and so is the draft's `end`.
            EventTimeDto(date = end.toLocalDate().toString())
        } else {
            EventTimeDto(dateTime = end.format(LOCAL_DATE_TIME), timeZone = zone.id)
        },
    )

    /**
     * A Google event as [RemoteEvent], or `null` for one this app cannot make sense of.
     *
     * A cancelled event legitimately arrives with **no `start` at all** — Google keeps the id
     * and the status and drops the rest — so the times fall back to a zero-width window at the
     * epoch. Nothing reads them: `CalendarSync.pullPlan` branches on `cancelled` before it
     * looks at any time. Dropping such an event instead would turn every deletion into an
     * absence, which is the one reading §2.7 needs to keep separate.
     */
    private fun EventDto.toRemoteEvent(): RemoteEvent? {
        if (id.isBlank()) return null
        val cancelled = status == STATUS_CANCELLED
        val start = this.start?.toLocalDateTime()
        val end = this.end?.toLocalDateTime()
        if ((start == null || end == null) && !cancelled) return null
        val allDay = this.start?.date != null
        return RemoteEvent(
            id = id,
            title = summary.orEmpty(),
            start = start ?: EPOCH,
            end = end ?: EPOCH,
            allDay = allDay,
            cancelled = cancelled,
        )
    }

    private fun EventTimeDto.toLocalDateTime(): LocalDateTime? = when {
        date != null -> runCatching { LocalDate.parse(date).atStartOfDay() }.getOrNull()
        dateTime != null -> runCatching {
            OffsetDateTime.parse(dateTime).atZoneSameInstant(zone).toLocalDateTime()
        }.getOrNull()

        else -> null
    }

    private companion object {
        const val BASE = "https://www.googleapis.com/calendar/v3"
        const val TIMEOUT_MS = 15_000
        const val STATUS_CANCELLED = "cancelled"

        /**
         * Ceiling on one range query. §4.3's calendar surface shows a week at a time and this
         * syncs a few weeks either side, so 2,500 is far beyond anything one user's GoalPilot
         * calendar holds — and a cap is still owed, because the alternative to a cap is a
         * pagination loop nobody has a test for.
         */
        const val MAX_EVENTS = 2500

        val RFC3339: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val EPOCH: LocalDateTime = LocalDate.of(1970, 1, 1).atStartOfDay()
    }
}

@Serializable
private data class CalendarWriteDto(val summary: String, val description: String? = null)

@Serializable
private data class CalendarColourDto(val colorId: String)

@Serializable
private data class CalendarDto(val id: String = "")

@Serializable
private data class CalendarListEntryDto(val id: String = "", val summary: String? = null)

@Serializable
private data class CalendarListResponse(val items: List<CalendarListEntryDto> = emptyList())

@Serializable
private data class EventTimeDto(
    val date: String? = null,
    val dateTime: String? = null,
    val timeZone: String? = null,
)

@Serializable
private data class EventWriteDto(
    val summary: String,
    val start: EventTimeDto,
    val end: EventTimeDto,
)

@Serializable
private data class EventDto(
    val id: String = "",
    val summary: String? = null,
    val start: EventTimeDto? = null,
    val end: EventTimeDto? = null,
    val status: String? = null,
)

@Serializable
private data class EventsResponse(val items: List<EventDto> = emptyList())

@Serializable
private data class FreeBusyItem(val id: String)

@Serializable
private data class FreeBusyRequest(
    val timeMin: String,
    val timeMax: String,
    val items: List<FreeBusyItem>,
)

@Serializable
private data class FreeBusyPeriod(val start: String, val end: String)

@Serializable
private data class FreeBusyCalendar(val busy: List<FreeBusyPeriod> = emptyList())

@Serializable
private data class FreeBusyResponse(
    val calendars: Map<String, FreeBusyCalendar> = emptyMap(),
)
