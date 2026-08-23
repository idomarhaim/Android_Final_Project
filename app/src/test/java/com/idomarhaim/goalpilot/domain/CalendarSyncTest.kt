package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.CalendarEventDraft
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.RemoteEvent
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.CalendarEntry
import com.idomarhaim.goalpilot.domain.usecase.CalendarPush
import com.idomarhaim.goalpilot.domain.usecase.CalendarSync
import com.idomarhaim.goalpilot.domain.usecase.SyncCalendarUseCase
import com.idomarhaim.goalpilot.domain.usecase.UnknownRemote
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §2.6–§2.8's sync rules — [`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61).
 *
 * ### Why the whole feature is testable here, with no device and no Google account
 *
 * Because every decision it makes is in `CalendarSync`, which takes an occurrence, a remote
 * event and a clock and returns what to do. The REST client below it performs; it does not
 * decide. That is the same split `ScheduleEdits` and `DailyMissReview` already have, and it is
 * worth more here than in either of them: the alternative to a JVM test for *"what happens when
 * Google says the event was deleted?"* is deleting an event in Google and watching.
 *
 * Each `§` in a test name is the clause the case is checking. Where two clauses could both
 * apply and disagree — a **skipped** occurrence whose window has already closed, say — there is
 * a test for the disagreement rather than for either clause alone.
 */
class CalendarSyncTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val mondayMorning: LocalDateTime = monday.atTime(9, 0)

    // A clock a week BEFORE everything below, so every fixture defaults to "in the future"
    // and a test about the past has to say so.
    private val now: LocalDateTime = monday.minusDays(7).atTime(12, 0)

    private var nextId = 0

    private fun entry(
        occurrence: Occurrence,
        title: String = "Submit report",
        eventId: String? = null,
        outcome: OccurrenceOutcome = OccurrenceOutcome.Planned,
        storedId: String = "o${nextId++}",
    ) = CalendarEntry(
        task = Task(id = "t-$storedId", title = title),
        occurrence = ScheduledOccurrence(
            id = storedId,
            taskId = "t-$storedId",
            occurrence = occurrence,
            outcome = outcome,
            googleEventId = eventId,
        ),
    )

    private fun remote(
        id: String,
        start: LocalDateTime,
        end: LocalDateTime,
        allDay: Boolean,
        title: String = "Submit report",
        cancelled: Boolean = false,
    ) = RemoteEvent(
        id = id,
        title = title,
        start = start,
        end = end,
        allDay = allDay,
        cancelled = cancelled,
    )

    // ── §2.7 · the four rungs, and what each becomes in Google ─────────────

    @Test
    fun `a DEADLINE becomes an all-day banner whose title carries the hour`() {
        // §2.7: "A DEADLINE is an all-day banner titled `Due 23:59 · Submit report`", because
        // "the Google event does not remind ... so its only job is to be SEEN, which a banner
        // does and a 23:59 marker does not".
        val draft = CalendarSync.draftFor(entry(Deadline(monday.atTime(23, 59))))

        assertThat(draft).isNotNull()
        assertThat(draft!!.allDay).isTrue()
        assertThat(draft.title).isEqualTo("Due 23:59 · Submit report")
        assertThat(draft.startDate).isEqualTo(monday)
        assertThat(draft.endDateInclusive).isEqualTo(monday)
    }

    @Test
    fun `a BLOCK is the one rung that occupies a real slot`() {
        // The contrast that makes the deadline banner a decision rather than an accident: a
        // timed deadline "would occupy a slot the app cannot check and collapse DEADLINE into
        // BLOCK" (§2.7). Only a block is allowed to be that.
        val draft = CalendarSync.draftFor(
            entry(Block(mondayMorning, mondayMorning.plusHours(2))),
        )

        assertThat(draft!!.allDay).isFalse()
        assertThat(draft.start).isEqualTo(mondayMorning)
        assertThat(draft.end).isEqualTo(mondayMorning.plusHours(2))
        assertThat(draft.title).isEqualTo("Submit report")
    }

    @Test
    fun `an ALL_DAY covers one day and a SPAN covers its range, both exclusive at the end`() {
        val allDay = CalendarSync.draftFor(entry(AllDay(monday)))!!
        val span = CalendarSync.draftFor(entry(Span(monday, monday.plusDays(4))))!!

        assertThat(allDay.allDay).isTrue()
        assertThat(allDay.endDateInclusive).isEqualTo(monday)
        // Google's all-day `end.date` is exclusive, so the wire value is the day after.
        assertThat(allDay.end.toLocalDate()).isEqualTo(monday.plusDays(1))

        assertThat(span.allDay).isTrue()
        assertThat(span.startDate).isEqualTo(monday)
        assertThat(span.endDateInclusive).isEqualTo(monday.plusDays(4))
    }

    // ── §2.3 / §2.7 · only confirmed occurrences reach Google ──────────────

    @Test
    fun `a PROVISIONAL block is not synced, and a SILENT one is`() {
        // §2.3: PROVISIONAL is "agent-placed, not yet confirmed, NOT SYNCED TO GOOGLE"; SILENT
        // is "agent-placed and ALREADY CONFIRMED, because the slot was visibly free". The two
        // "sit on the same day on purpose", so reading `isEndorsed` rather than `== CONFIRMED`
        // is the difference between a correct sync and one that hides half the plan.
        val provisional = entry(
            Block(mondayMorning, mondayMorning.plusHours(1), BlockPlacement.PROVISIONAL),
        )
        val silent = entry(
            Block(mondayMorning, mondayMorning.plusHours(1), BlockPlacement.SILENT),
        )

        assertThat(CalendarSync.draftFor(provisional)).isNull()
        assertThat(CalendarSync.draftFor(silent)).isNotNull()
    }

    @Test
    fun `a skipped occurrence is not something Google should hold`() {
        val skipped = entry(AllDay(monday), outcome = OccurrenceOutcome.Skipped(1_000L))

        assertThat(CalendarSync.draftFor(skipped)).isNull()
    }

    @Test
    fun `a blank task title produces no event, rather than an untitled one`() {
        assertThat(CalendarSync.draftFor(entry(AllDay(monday), title = "   "))).isNull()
    }

    // ── §2.7 · titles are written but never read back ──────────────────────

    @Test
    fun `a Google-side rename is not a drift, so it is never patched back`() {
        // §2.7: "Titles are written but never read back. A Google-side rename would silently
        // replace a task title with no undo; the reverse failure is visible and harmless."
        val e = entry(AllDay(monday), eventId = "g1")
        val renamedInGoogle = remote(
            id = "g1",
            start = monday.atStartOfDay(),
            end = monday.plusDays(1).atStartOfDay(),
            allDay = true,
            title = "something Ido typed instead",
        )

        val pushes = CalendarSync.pushPlan(
            entries = listOf(e),
            remote = mapOf("g1" to renamedInGoogle),
            now = now,
            unknownRemote = UnknownRemote.LEAVE_ALONE,
        )

        assertThat(pushes).isEmpty()
    }

    @Test
    fun `a rename in Google never reaches the task, even when the times did drift`() {
        val e = entry(AllDay(monday), eventId = "g1")
        val movedAndRenamed = remote(
            id = "g1",
            start = monday.plusDays(3).atStartOfDay(),
            end = monday.plusDays(4).atStartOfDay(),
            allDay = true,
            title = "not the task's title",
        )

        val plan = CalendarSync.pullPlan(
            entries = listOf(e),
            remote = listOf(movedAndRenamed),
            judgedFrom = monday.minusDays(7),
            judgedTo = monday.plusDays(30),
        )

        assertThat(plan.retimed).hasSize(1)
        // The *when* moved and nothing else did. There is no title on an Occurrence at all,
        // which is what makes this structural rather than a rule someone has to remember.
        assertThat(plan.retimed.single().moved).isEqualTo(AllDay(monday.plusDays(3)))
        assertThat(plan.retimed.single().entry.task.title).isEqualTo("Submit report")
    }

    // ── §2.7 · the pull moves times and never rungs ────────────────────────

    @Test
    fun `dragging an all-day into a slot moves the day and does not make it a BLOCK`() {
        // A rung change is "cancel-and-recreate, not a patch" (§2.8) and is the app's own
        // operation. Letting a drag in Google do it would turn DAY_PASSED -- which §2.3 does
        // not count as a failure -- into MISSED, which it does.
        val moved = CalendarSync.retimed(
            AllDay(monday),
            remote("g1", monday.plusDays(1).atTime(9, 0), monday.plusDays(1).atTime(10, 0), allDay = false),
        )

        assertThat(moved).isEqualTo(AllDay(monday.plusDays(1)))
    }

    @Test
    fun `a deadline banner moved to another day keeps the hour Google never knew`() {
        // The banner is all-day BY CONSTRUCTION, so Google holds no time of day for it and a
        // pull must not invent one. Taking the remote start would make every synced deadline
        // due at midnight.
        val moved = CalendarSync.retimed(
            Deadline(monday.atTime(23, 59)),
            remote(
                "g1",
                monday.plusDays(2).atStartOfDay(),
                monday.plusDays(3).atStartOfDay(),
                allDay = true,
            ),
        )

        assertThat(moved).isEqualTo(Deadline(monday.plusDays(2).atTime(23, 59)))
    }

    @Test
    fun `a block dragged to a new slot keeps its placement`() {
        // §2.7: "the sync carries times in both directions and state in neither". A Google
        // event has no field a placement could have come from, so carrying the local one over
        // is the only honest answer -- and losing it would silently confirm a provisional
        // block, or unconfirm a real one.
        val moved = CalendarSync.retimed(
            Block(mondayMorning, mondayMorning.plusHours(2), BlockPlacement.SILENT),
            remote("g1", monday.atTime(14, 0), monday.atTime(16, 0), allDay = false),
        )

        assertThat(moved).isEqualTo(
            Block(monday.atTime(14, 0), monday.atTime(16, 0), BlockPlacement.SILENT),
        )
    }

    @Test
    fun `a span moved in Google keeps its length, and the exclusive end is read once`() {
        val moved = CalendarSync.retimed(
            Span(monday, monday.plusDays(2)),
            remote(
                "g1",
                monday.plusDays(10).atStartOfDay(),
                monday.plusDays(13).atStartOfDay(),
                allDay = true,
            ),
        )

        // Google's `end.date` is exclusive; a Span's `to` is inclusive. Reading the two the
        // same way lengthens or shortens every synced span by a day.
        assertThat(moved).isEqualTo(Span(monday.plusDays(10), monday.plusDays(12)))
    }

    @Test
    fun `an unchanged event produces no retime at all`() {
        val e = entry(Block(mondayMorning, mondayMorning.plusHours(2)), eventId = "g1")

        val plan = CalendarSync.pullPlan(
            entries = listOf(e),
            remote = listOf(
                remote("g1", mondayMorning, mondayMorning.plusHours(2), allDay = false),
            ),
            judgedFrom = monday.minusDays(7),
            judgedTo = monday.plusDays(30),
        )

        assertThat(plan.isEmpty).isTrue()
    }

    // ── §2.7 · a disappearance never deletes and never re-creates ──────────

    @Test
    fun `a cancelled event is a disappearance, and the occurrence keeps its date`() {
        // §2.7: the occurrence "keeps its date, clears its googleEventId", and the ambiguity is
        // ASKED. Nothing in the plan touches the occurrence's when.
        val e = entry(AllDay(monday), eventId = "g1")

        val plan = CalendarSync.pullPlan(
            entries = listOf(e),
            remote = listOf(
                remote("g1", monday.atStartOfDay(), monday.plusDays(1).atStartOfDay(), true, cancelled = true),
            ),
            judgedFrom = monday.minusDays(7),
            judgedTo = monday.plusDays(30),
        )

        assertThat(plan.disappeared).containsExactly(e)
        assertThat(plan.retimed).isEmpty()
        assertThat(plan.disappeared.single().occurrence.occurrence).isEqualTo(AllDay(monday))
    }

    @Test
    fun `a cancelled event is neither re-created nor patched over`() {
        // Both halves of "never deletes and never re-creates". If the push re-inserted, the
        // user's deletion would undo itself; if it patched, the trashing would be reversed by
        // a write they never asked for.
        val e = entry(AllDay(monday.plusDays(4)), eventId = "g1")
        val trashed = remote(
            "g1",
            monday.atStartOfDay(),
            monday.plusDays(1).atStartOfDay(),
            allDay = true,
            cancelled = true,
        )

        val pushes = CalendarSync.pushPlan(
            entries = listOf(e),
            remote = mapOf("g1" to trashed),
            now = now,
            unknownRemote = UnknownRemote.ASSUME_STALE,
        )

        assertThat(pushes).isEmpty()
    }

    @Test
    fun `an event missing from the response is only a disappearance inside the judged window`() {
        // The caller queries Google WIDER than it judges, precisely so an event dragged a
        // fortnight out of the window is found rather than read as a delete. §2.7's real
        // ambiguity is a move to ANOTHER CALENDAR, which we cannot see; a move to another date
        // on the same calendar is visible, and losing it to a narrow query would be an
        // ambiguity we manufactured.
        val inside = entry(AllDay(monday), eventId = "g-inside")
        val outside = entry(AllDay(monday.plusDays(90)), eventId = "g-outside")

        val plan = CalendarSync.pullPlan(
            entries = listOf(inside, outside),
            remote = emptyList(),
            judgedFrom = monday.minusDays(7),
            judgedTo = monday.plusDays(30),
        )

        assertThat(plan.disappeared).containsExactly(inside)
    }

    // ── §2.7 / §2.8 · what happens to an event that should no longer be there

    @Test
    fun `a future window that stops being a plan is cancelled`() {
        val skippedFuture = entry(
            AllDay(monday.plusDays(3)),
            eventId = "g1",
            outcome = OccurrenceOutcome.Skipped(1_000L),
        )

        val pushes = CalendarSync.pushPlan(
            entries = listOf(skippedFuture),
            remote = emptyMap(),
            now = now,
            unknownRemote = UnknownRemote.LEAVE_ALONE,
        )

        assertThat(pushes).containsExactly(CalendarPush.Cancel(skippedFuture, "g1"))
    }

    @Test
    fun `a past window that stops being a plan is left alone`() {
        // §2.8: "every destructive effect splits by tense: future events cancel, PAST EVENTS
        // STAY as the record of time actually spent." Two clauses could apply to a skipped
        // past occurrence -- "a skip is not a plan" and "past events stay" -- and this is which
        // one wins.
        val skippedPast = entry(
            AllDay(now.toLocalDate().minusDays(3)),
            eventId = "g1",
            outcome = OccurrenceOutcome.Skipped(1_000L),
        )

        val pushes = CalendarSync.pushPlan(
            entries = listOf(skippedPast),
            remote = emptyMap(),
            now = now,
            unknownRemote = UnknownRemote.LEAVE_ALONE,
        )

        assertThat(pushes).isEmpty()
    }

    @Test
    fun `history is never created, only preserved`() {
        // The mirror of the tense split, and the one that bites on the very first sync: an
        // unlinked occurrence whose window has already closed must not be inserted, or granting
        // the scope backfills a week of things nobody can act on.
        val past = entry(AllDay(now.toLocalDate().minusDays(2)))
        val future = entry(AllDay(now.toLocalDate().plusDays(2)))

        val pushes = CalendarSync.pushPlan(
            entries = listOf(past, future),
            remote = emptyMap(),
            now = now,
            unknownRemote = UnknownRemote.LEAVE_ALONE,
        )

        assertThat(pushes.map { it.entry }).containsExactly(future)
        assertThat(pushes.single()).isInstanceOf(CalendarPush.Insert::class.java)
    }

    // ── §2.7 · last-write-wins, and the two readings of an unknown event ───

    @Test
    fun `a drift against a known remote copy is patched -- last-write-wins, and we wrote last`() {
        // §2.7: "the pull runs on foreground, the window is minutes wide for one user, and
        // LAST-WRITE-WINS IS CORRECT rather than a compromise". There is no merge and no
        // conflict resolver, because "there is no credential for a background sync and cannot
        // be one" -- so the two writers are never both live.
        val e = entry(Block(mondayMorning, mondayMorning.plusHours(2)), eventId = "g1")
        val stale = remote("g1", monday.atTime(15, 0), monday.atTime(16, 0), allDay = false)

        val pushes = CalendarSync.pushPlan(
            entries = listOf(e),
            remote = mapOf("g1" to stale),
            now = now,
            unknownRemote = UnknownRemote.LEAVE_ALONE,
        )

        val update = pushes.single() as CalendarPush.Update
        assertThat(update.eventId).isEqualTo("g1")
        assertThat(update.draft.start).isEqualTo(mondayMorning)
        assertThat(update.draft.end).isEqualTo(mondayMorning.plusHours(2))
    }

    @Test
    fun `an unknown remote copy is left alone after a pull and patched without one`() {
        // The asymmetry is §2.7's throttle clause in code. After a pull, silence means Google
        // did not return it and a blind patch is a write against something unknown. With no
        // pull -- the user just edited it and is waiting -- one unnecessary PATCH costs a
        // request, where a skipped necessary one costs a calendar that disagrees with the app.
        val e = entry(AllDay(monday.plusDays(2)), eventId = "g1")

        val afterPull = CalendarSync.pushPlan(
            listOf(e), emptyMap(), now, UnknownRemote.LEAVE_ALONE,
        )
        val userInitiated = CalendarSync.pushPlan(
            listOf(e), emptyMap(), now, UnknownRemote.ASSUME_STALE,
        )

        assertThat(afterPull).isEmpty()
        assertThat(userInitiated.single()).isInstanceOf(CalendarPush.Update::class.java)
    }

    // ── §2.7 · the throttle is on the pull, and only on the pull ───────────

    @Test
    fun `a foreground pull inside the window is throttled and a manual one never is`() {
        val lastPull = 1_000_000L
        val soon = lastPull + CalendarSync.PULL_THROTTLE_MILLIS - 1

        assertThat(CalendarSync.mayPull(manual = false, lastPullAtMillis = lastPull, nowMillis = soon))
            .isFalse()
        assertThat(CalendarSync.mayPull(manual = true, lastPullAtMillis = lastPull, nowMillis = soon))
            .isTrue()
        assertThat(
            CalendarSync.mayPull(
                manual = false,
                lastPullAtMillis = lastPull,
                nowMillis = lastPull + CalendarSync.PULL_THROTTLE_MILLIS,
            ),
        ).isTrue()
    }

    @Test
    fun `a clock that went backwards does not park the next pull in the future`() {
        // `SyncHealthDataUseCase`'s guard, and it transfers because the failure does: a
        // timezone edit or an NTP correction would otherwise suppress every pull for up to
        // fifteen minutes of wall time that has already happened.
        assertThat(
            CalendarSync.mayPull(manual = false, lastPullAtMillis = 5_000_000L, nowMillis = 1_000L),
        ).isTrue()
    }

    @Test
    fun `the throttle window is the one already shipped, not a second copy of fifteen minutes`() {
        assertThat(CalendarSync.PULL_THROTTLE_MILLIS)
            .isEqualTo(com.idomarhaim.goalpilot.domain.usecase.SyncHealthDataUseCase.THROTTLE_MILLIS)
    }

    @Test
    fun `a push is never throttled, because nothing in the push path can be`() {
        // A negative test with a positive form: `pushPlan` takes no clock-stamp and no
        // last-write time, so there is nowhere a throttle could be applied. If this stops
        // compiling because someone added one, that is the finding.
        val e = entry(AllDay(monday.plusDays(1)))
        val first = CalendarSync.pushPlan(listOf(e), emptyMap(), now, UnknownRemote.ASSUME_STALE)
        val immediatelyAgain =
            CalendarSync.pushPlan(listOf(e), emptyMap(), now, UnknownRemote.ASSUME_STALE)

        assertThat(first).isNotEmpty()
        assertThat(immediatelyAgain).isEqualTo(first)
    }

    // ── §2.7 · what the app must never do to Ido's own calendar ────────────

    @Test
    fun `an event with no occurrence behind it is external, and nothing happens to it`() {
        // §2.7: "An event Ido creates BY HAND inside the GoalPilot calendar is LEFT ALONE."
        // §2.8: "orphaned events are surfaced there and never auto-deleted -- silent cleanup is
        // the one operation with no undo affordance."
        val mine = entry(AllDay(monday), eventId = "g-mine")
        val his = remote("g-his", monday.atTime(18, 0), monday.atTime(19, 0), allDay = false, title = "Dentist")

        val plan = CalendarSync.pullPlan(
            entries = listOf(mine),
            remote = listOf(
                remote("g-mine", monday.atStartOfDay(), monday.plusDays(1).atStartOfDay(), true),
                his,
            ),
            judgedFrom = monday.minusDays(7),
            judgedTo = monday.plusDays(30),
        )

        assertThat(plan.external).containsExactly(his)
        assertThat(plan.disappeared).isEmpty()
        assertThat(plan.retimed).isEmpty()
    }

    @Test
    fun `an account switch reads as not mirrored, never as events to patch`() {
        // §2.7: "Sign-out does not delete the calendar Ido owns. An account switch reads as NOT
        // MIRRORED, not as events to patch." The new account has no occurrences and no links,
        // so this is what the sync sees on its first run -- and it must produce no writes at
        // all, rather than treating every event on the calendar as an orphan to tidy up.
        val whatIsAlreadyThere = listOf(
            remote("g1", monday.atStartOfDay(), monday.plusDays(1).atStartOfDay(), true),
            remote("g2", mondayMorning, mondayMorning.plusHours(1), allDay = false),
        )

        val plan = CalendarSync.pullPlan(
            entries = emptyList(),
            remote = whatIsAlreadyThere,
            judgedFrom = monday.minusDays(7),
            judgedTo = monday.plusDays(30),
        )
        val pushes = CalendarSync.pushPlan(emptyList(), emptyMap(), now, UnknownRemote.LEAVE_ALONE)

        assertThat(plan.external).containsExactlyElementsIn(whatIsAlreadyThere)
        assertThat(plan.disappeared).isEmpty()
        assertThat(pushes).isEmpty()
    }

    // ── The two constants that are one decision ────────────────────────────

    @Test
    fun `the query margin is not zero, or the judged window would be its own query window`() {
        // `pullPlan`'s absent-event rule is only safe because the caller asks wider than it
        // judges. Setting this to zero reintroduces the bug silently -- an event dragged just
        // past the window edge would come back absent and be called a delete -- so the two
        // numbers are asserted together rather than trusted apart.
        assertThat(SyncCalendarUseCase.QUERY_MARGIN_DAYS).isGreaterThan(0L)
        assertThat(SyncCalendarUseCase.PAST_DAYS).isAtLeast(0L)
        assertThat(SyncCalendarUseCase.WINDOW_DAYS).isGreaterThan(SyncCalendarUseCase.PAST_DAYS)
    }

    @Test
    fun `sameTimesAs ignores the title and nothing else`() {
        val a = CalendarEventDraft("one", mondayMorning, mondayMorning.plusHours(1), allDay = false)

        assertThat(a.sameTimesAs(a.copy(title = "two"))).isTrue()
        assertThat(a.sameTimesAs(a.copy(start = mondayMorning.plusMinutes(1)))).isFalse()
        assertThat(a.sameTimesAs(a.copy(end = mondayMorning.plusHours(2)))).isFalse()
        assertThat(a.sameTimesAs(a.copy(allDay = true))).isFalse()
    }
}
