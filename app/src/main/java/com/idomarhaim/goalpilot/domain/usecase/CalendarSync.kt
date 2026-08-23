package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.CalendarEventDraft
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.GoalPilotCalendar
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.RemoteEvent
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.startDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** A task paired with one of its occurrences — what a single Google event mirrors. */
data class CalendarEntry(val task: Task, val occurrence: ScheduledOccurrence) {

    /** The Google event mirroring this occurrence, or `null` for one that never reached it. */
    val eventId: String? get() = occurrence.googleEventId

    /** The *when*, without the document around it. */
    val when_: Occurrence get() = occurrence.occurrence
}

/** One write this sync wants to make against the GoalPilot calendar. */
sealed interface CalendarPush {

    val entry: CalendarEntry

    /** This occurrence has never reached Google. Create the event and store its id. */
    data class Insert(
        override val entry: CalendarEntry,
        val draft: CalendarEventDraft,
    ) : CalendarPush

    /** Google's copy disagrees with ours about *when*. Patch the times; never the state. */
    data class Update(
        override val entry: CalendarEntry,
        val eventId: String,
        val draft: CalendarEventDraft,
    ) : CalendarPush

    /**
     * The occurrence should no longer be on the calendar and its window has not closed.
     *
     * §2.8: *"Deletion is cancellation — Google's trash, restorable 30 days"*, and *"every
     * destructive effect splits by tense: future events cancel, past events stay as the record
     * of time actually spent."* Nothing in this file ever produces a `Cancel` for a past
     * window; that is the tense split, and it is a test.
     */
    data class Cancel(
        override val entry: CalendarEntry,
        val eventId: String,
    ) : CalendarPush
}

/**
 * What to assume about a linked event we have **no fresh remote copy of**.
 *
 * The two callers genuinely differ, which is why this is an argument rather than a constant.
 * §2.7: *"Pull is foreground + the shipped 15-minute per-uid throttle; **push is not
 * throttled** (a write must not lag the user)."*
 */
enum class UnknownRemote {

    /**
     * A full sync, which pulled first — so an id missing from the remote map was genuinely not
     * returned, and re-patching it would be a write against something we know nothing about.
     */
    LEAVE_ALONE,

    /**
     * The user just changed this occurrence and is waiting. There is no pull to consult, and
     * one unnecessary `PATCH` costs a request where a skipped necessary one costs a calendar
     * that silently disagrees with the app.
     */
    ASSUME_STALE,
}

/** What Google now says about the events GoalPilot wrote, and what follows from it. */
data class CalendarPullPlan(
    /** Google moved it. Take the new *when*, keep the rung, keep the title (§2.7). */
    val retimed: List<Retimed> = emptyList(),
    /** It is gone from the GoalPilot calendar, and we cannot tell why (§2.7). */
    val disappeared: List<CalendarEntry> = emptyList(),
    /** An event Ido made by hand inside the GoalPilot calendar. **Left alone** (§2.7). */
    val external: List<RemoteEvent> = emptyList(),
) {
    val isEmpty: Boolean
        get() = retimed.isEmpty() && disappeared.isEmpty() && external.isEmpty()

    /** One occurrence whose times Google changed, and the [Occurrence] it becomes. */
    data class Retimed(val entry: CalendarEntry, val moved: Occurrence)
}

/**
 * §2.7's three answers to a disappearance, asked in the daily-review batch sheet.
 *
 * *"A move-out is indistinguishable from a delete (both read as `cancelled`, and we see only
 * our own calendar), so the occurrence **keeps its date, clears its `googleEventId`**, and the
 * ambiguity is asked … at the one moment Ido is holding the phone."*
 *
 * The clearing has **already happened** by the time any of these is chosen — that is what makes
 * the sheet safe to ignore. A user who never answers keeps every occurrence, on its original
 * date, unmirrored.
 */
enum class DisappearanceChoice {

    /** It is still a plan; it just is not in Google any more. Nothing further happens. */
    KEEP,

    /** He meant to drop it. Records [OccurrenceOutcome.Skipped] — a decision, not a miss. */
    CANCEL,

    /** He deleted it by accident, or moved it out and wants it back. Re-inserts the event. */
    PUT_BACK,
}

/**
 * **The sync rules of §2.6–§2.8, as pure functions** —
 * [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61).
 *
 * ### Why every decision is here and not in `SyncCalendarUseCase`
 *
 * The same argument `ScheduleEdits` and `DailyMissReview` already make in this codebase: *"a
 * rule that can only be exercised against a live Firestore is a rule whose branches do not all
 * get tested"* — and here the live thing is worse than a Firestore, because it is a Google
 * account, a consent screen and a network. Every clause below is a function of an occurrence, a
 * remote event and a clock, so the whole of §2.7 is a JVM test rather than a device and a
 * sign-in.
 *
 * ### The three invariants this file exists to hold
 *
 * 1. **State syncs in neither direction.** Nothing here reads or writes an
 *    [com.idomarhaim.goalpilot.domain.model.OccurrenceState], and [retimed] cannot change a
 *    [Block]'s [com.idomarhaim.goalpilot.domain.model.BlockPlacement]. §2.7: a Google event
 *    *"has no field for `MISSED`/`OVERDUE`/`EXPIRED`/`PROVISIONAL`"*.
 * 2. **Titles are written but never read back.** [draftFor] composes a title; [retimed] never
 *    touches one, and [CalendarEventDraft.sameTimesAs] excludes it from the drift comparison.
 *    *"A Google-side rename would silently replace a task title with no undo."*
 * 3. **A disappearance never deletes and never re-creates.** [pullPlan] reports it; the caller
 *    clears the link and keeps the date. Nothing here produces a re-insert for it — only
 *    [DisappearanceChoice.PUT_BACK], which is a person answering.
 */
object CalendarSync {

    /**
     * How long a pull counts as fresh — **the same fifteen minutes** `SyncHealthDataUseCase`
     * already ships, referenced rather than re-typed.
     *
     * The brief for this work says *"honouring the shipped 15-minute per-uid throttle"*, and
     * two constants both meaning *fifteen minutes* is one edit away from disagreeing. The
     * reasoning is the health sync's own and transfers unchanged: short enough that a slot
     * moved at lunch is in the app by the time it is looked at, long enough that flicking
     * between apps is not a request per switch.
     */
    const val PULL_THROTTLE_MILLIS: Long = SyncHealthDataUseCase.THROTTLE_MILLIS

    /**
     * Whether a pull may run now.
     *
     * A **manual** pull is never throttled — the user asked. A foreground one is, and the
     * `elapsed >= 0` half is the health sync's guard against a clock that has gone backwards
     * (a timezone edit, an NTP correction), which would otherwise park the next pull up to
     * fifteen minutes in the future.
     *
     * **Push has no equivalent and must not grow one** (§2.7: *"a write must not lag the
     * user"*). That asymmetry is the whole reason this function is about pulling and is named
     * so.
     */
    fun mayPull(manual: Boolean, lastPullAtMillis: Long, nowMillis: Long): Boolean {
        if (manual) return true
        val elapsed = nowMillis - lastPullAtMillis
        return elapsed < 0 || elapsed >= PULL_THROTTLE_MILLIS
    }

    /**
     * **What this occurrence should look like in Google**, or `null` if it does not belong
     * there at all.
     *
     * ### The four rungs, and the one that is not what it looks like
     *
     * - [AllDay] → an all-day event on its day.
     * - [Span] → an all-day event across its days.
     * - [Block] → a real timed event; it is the only rung that occupies a slot.
     * - [Deadline] → **an all-day banner**, titled `Due 23:59 · Submit report`.
     *
     * §2.7 gives one criterion for that last one and it is worth restating where the code is:
     * *"the Google event **does not remind** (§2.5's local notification does), so its only job
     * is to be **seen**, which a banner does and a 23:59 marker does not. A timed event would
     * occupy a slot the app cannot check and collapse `DEADLINE` into `BLOCK`."* The banner
     * **may be paired with a real `BLOCK`** — the obligation and the plan, as two events —
     * which costs nothing here, because those are two occurrences and each gets its own draft.
     *
     * ### What returns `null`, and why each is not a deletion
     *
     * - A **skipped** occurrence. §2.1 calls a skip *"a decision"*, and a decided-against
     *   window is not a plan.
     * - A **[com.idomarhaim.goalpilot.domain.model.BlockPlacement.PROVISIONAL]** block. §2.3
     *   says outright: *"agent-placed, not yet confirmed, **not synced to Google**"*, and §2.7
     *   that *"only **confirmed** occurrences reach Google"*.
     *   [com.idomarhaim.goalpilot.domain.model.BlockPlacement.SILENT] is confirmed — §2.3:
     *   *"agent-placed and **already confirmed**, because the slot was visibly free"* — so it
     *   syncs, and reading `isEndorsed` rather than `== CONFIRMED` is what keeps that true.
     * - A task with a **blank title**, which would create an untitled event nobody can act on.
     *
     * `null` is *"not a plan Google should hold"*, never *"delete it"*. What happens to an
     * event already there is [pushPlan]'s tense split, not this function's.
     */
    fun draftFor(entry: CalendarEntry): CalendarEventDraft? {
        if (entry.occurrence.outcome is OccurrenceOutcome.Skipped) return null
        val title = entry.task.title.trim()
        if (title.isEmpty()) return null
        return when (val o = entry.when_) {
            is AllDay -> CalendarEventDraft(
                title = title,
                start = o.opensAt,
                end = o.closesAt,
                allDay = true,
            )

            is Span -> CalendarEventDraft(
                title = title,
                start = o.opensAt,
                end = o.closesAt,
                allDay = true,
            )

            is Deadline -> CalendarEventDraft(
                title = GoalPilotCalendar.deadlineTitle(o.at, title),
                // The banner covers the day the deadline falls on. Its `end` is the next
                // midnight because Google's all-day `end.date` is exclusive, exactly as
                // `AllDay.closesAt` already is.
                start = o.at.toLocalDate().atStartOfDay(),
                end = o.at.toLocalDate().plusDays(1).atStartOfDay(),
                allDay = true,
            )

            is Block -> if (!o.placement.isEndorsed) {
                null
            } else {
                CalendarEventDraft(
                    title = title,
                    start = o.start,
                    end = o.closesAt,
                    allDay = false,
                )
            }
        }
    }

    /**
     * **The writes to make against the GoalPilot calendar.**
     *
     * @param remote what the last pull said, keyed by Google event id. Empty is legitimate —
     *   see [unknownRemote].
     * @param unknownRemote what to do about a linked occurrence [remote] says nothing about.
     *
     * ### A cancelled remote copy is left strictly alone
     *
     * §2.7: *"a disappearance never deletes and never re-creates."* An event Google has
     * trashed is a disappearance, and [pullPlan] is what reports it. If this function patched
     * it the user's deletion would silently undo itself, and if it inserted a replacement they
     * would have deleted the same thing twice. So it does neither — which is one branch, and
     * the one worth a test.
     */
    fun pushPlan(
        entries: List<CalendarEntry>,
        remote: Map<String, RemoteEvent>,
        now: LocalDateTime,
        unknownRemote: UnknownRemote,
    ): List<CalendarPush> = entries.mapNotNull { entry ->
        val draft = draftFor(entry)
        val eventId = entry.eventId
        val known = eventId?.let { remote[it] }
        when {
            // Trashed in Google. Neither re-create it nor patch over the trashing.
            known != null && known.cancelled -> null

            // A window that has already closed is history, and history is preserved rather
            // than created. §2.8 keeps a past event that is ALREADY there; it does not make
            // one. Without this the first sync after granting the scope backfills every
            // occurrence in the look-back window, which is a calendar full of things nobody
            // can act on.
            draft != null && eventId == null ->
                if (entry.when_.closesAt.isAfter(now)) CalendarPush.Insert(entry, draft) else null

            draft != null && eventId != null -> when {
                known != null ->
                    if (known.asDraft().sameTimesAs(draft)) null
                    else CalendarPush.Update(entry, eventId, draft)

                unknownRemote == UnknownRemote.ASSUME_STALE ->
                    CalendarPush.Update(entry, eventId, draft)

                else -> null
            }

            // No longer a plan Google should hold. §2.8's tense split decides what that means.
            draft == null && eventId != null ->
                if (entry.when_.closesAt.isAfter(now)) CalendarPush.Cancel(entry, eventId)
                else null

            else -> null
        }
    }

    /**
     * **What Google's copy says has happened to the events GoalPilot wrote.**
     *
     * @param judgedFrom / @param judgedTo the days this call is willing to call a
     *   disappearance on. **The caller must have queried Google over a wider range than this**
     *   — see below.
     *
     * ### Absence only counts as a disappearance inside the judged window
     *
     * A `cancelled` event is unambiguous: Google was asked for deleted events and returned one.
     * A *missing* one is not — it could have been dragged a fortnight out and simply fall
     * outside the range we asked about. Reading that as a delete would clear a perfectly live
     * link and put a question in front of the user about something nobody touched. So an
     * occurrence is only judged when its own date is inside [judgedFrom]…[judgedTo], and the
     * query that produced [remote] is expected to reach further on both sides.
     *
     * This is **not** the ambiguity §2.7 says cannot be resolved. That one is *"a move-out is
     * indistinguishable from a delete"* — moved to **another calendar**, which we cannot see at
     * all. Moved to another *date* on the same calendar is visible, and losing it to a narrow
     * query would be an ambiguity we manufactured.
     *
     * ### An event with no occurrence behind it is `EXTERNAL`, and is never touched
     *
     * §2.7: *"An event Ido creates **by hand** inside the GoalPilot calendar is **left
     * alone**."* §2.8 adds why nothing sweeps them up: *"orphaned events are surfaced there and
     * never auto-deleted — silent cleanup is the one operation with no undo affordance."*
     */
    fun pullPlan(
        entries: List<CalendarEntry>,
        remote: List<RemoteEvent>,
        judgedFrom: LocalDate,
        judgedTo: LocalDate,
    ): CalendarPullPlan {
        val byId = remote.associateBy { it.id }
        val retimed = mutableListOf<CalendarPullPlan.Retimed>()
        val disappeared = mutableListOf<CalendarEntry>()

        entries.forEach { entry ->
            val eventId = entry.eventId ?: return@forEach
            val event = byId[eventId]
            val date = entry.when_.startDate
            val judged = !date.isBefore(judgedFrom) && !date.isAfter(judgedTo)
            when {
                event == null -> if (judged) disappeared += entry
                event.cancelled -> disappeared += entry
                else -> {
                    val moved = retimed(entry.when_, event)
                    if (moved != entry.when_) {
                        retimed += CalendarPullPlan.Retimed(entry, moved)
                    }
                }
            }
        }

        val mirrored = entries.mapNotNull { it.eventId }.toSet()
        val external = remote.filter { !it.cancelled && it.id !in mirrored }

        return CalendarPullPlan(
            retimed = retimed,
            disappeared = disappeared,
            external = external,
        )
    }

    /**
     * **The occurrence Google's copy implies** — the *when* moves, the **rung does not**.
     *
     * ### Keeping the rung is the decision, and it is not conservatism
     *
     * A pull that let Google change a rung would let a drag in someone else's UI change what a
     * *miss means* (§2.2): drop an all-day onto 09:00 and `DAY_PASSED` silently becomes
     * `MISSED`, which §2.3 counts as a failure. §2.8 is explicit that a rung change is the
     * app's own operation — *"a rung change `BLOCK` → `DEADLINE` is cancel-and-recreate, not a
     * patch"* — so it cannot arrive as a side effect of a pull.
     *
     * The consequence is that each rung takes from the remote event only what it can express:
     *
     * | Local rung | Remote all-day | Remote timed |
     * |---|---|---|
     * | [AllDay] | the new day | the new day; the time is dropped |
     * | [Deadline] | the new day, **keeping the hour we already knew** | the new instant |
     * | [Block] | the new day, keeping our times of day and its length | the new start and end |
     * | [Span] | the new range | the new range, read as whole days |
     *
     * The [Deadline] row is the one that matters: its banner is all-day *by construction*
     * ([draftFor]), so a pull can never learn its hour back from Google and must not invent
     * one. Taking `00:00` would make every synced deadline due at midnight.
     *
     * [Block.placement] is carried across untouched — that is invariant 1 in this object's
     * KDoc, and there is nowhere in a Google event it could have come from anyway.
     */
    fun retimed(occurrence: Occurrence, event: RemoteEvent): Occurrence {
        val startDay = event.start.toLocalDate()
        return when (occurrence) {
            is AllDay -> AllDay(startDay)

            is Deadline -> Deadline(
                if (event.allDay) startDay.atTime(occurrence.at.toLocalTime()) else event.start,
            )

            is Block -> if (event.allDay) {
                val length = ChronoUnit.DAYS.between(
                    occurrence.start.toLocalDate(),
                    occurrence.closesAt.toLocalDate(),
                )
                Block(
                    start = startDay.atTime(occurrence.start.toLocalTime()),
                    end = startDay.plusDays(length).atTime(occurrence.closesAt.toLocalTime()),
                    placement = occurrence.placement,
                )
            } else {
                Block(start = event.start, end = event.end, placement = occurrence.placement)
            }

            is Span -> Span(
                from = startDay,
                to = lastDayOf(event).coerceAtLeast(startDay),
            )
        }
    }

    /**
     * The last day an event covers. Google's all-day `end.date` is **exclusive**, a timed
     * event's `end.dateTime` is the instant it stops — so the two need different arithmetic and
     * getting it wrong shortens or lengthens every synced span by a day.
     */
    private fun lastDayOf(event: RemoteEvent): LocalDate =
        if (event.allDay) event.end.toLocalDate().minusDays(1) else event.end.toLocalDate()
}
