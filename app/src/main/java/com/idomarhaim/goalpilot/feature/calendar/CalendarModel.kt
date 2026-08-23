package com.idomarhaim.goalpilot.feature.calendar

import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.EditScope
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.onDate
import com.idomarhaim.goalpilot.domain.model.startDate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * **§4.3's calendar surface, as pure data** — the vocabulary every other file in this package
 * reads ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * ### Why the arithmetic lives in `feature/calendar/` and not in `domain/usecase/`
 *
 * Nothing in this file imports Android or Firebase, so `app/src/test/` exercises all of it on the
 * JVM with no device — which is the property that matters, and it is the property `domain/` exists
 * to guarantee. What keeps it here rather than one package up is that every type below answers a
 * question **only this screen asks**: which of three lanes a rung is drawn in, what shape its time
 * column takes, whether it is carried forward onto today. A `domain/usecase/` reader would find a
 * module whose every export names a visual decision.
 *
 * The boundary is real and worth stating, because the temptation runs the other way: the moment
 * something here is wanted by a **second** screen it belongs in `domain/`, and the first such
 * reader should move it rather than import it across features.
 */

/**
 * §4.2's zoom — *"agenda ⇄ 3 days ⇄ week"*, which is what collapsed three rival screens into one
 * control.
 *
 * §4.3 fixes the default and says why it is not taste: *"Seven columns on a 390 dp phone is ~46 dp
 * per day — no Hebrew title and no time range survives it… at 3 days a column is ~110 dp and both
 * fit."* So [THREE_DAYS] is [DEFAULT], and [WEEK] exists with the times stacked start-over-end,
 * *"the only thing that fits at 46 dp"*.
 */
enum class CalendarZoom(val dayCount: Int, val label: String) {

    /**
     * One day as a list rather than a grid — the prototype's variant `B`, which rev 5 found was
     * *"never a rival screen; it is the **agenda level** of this control"*.
     */
    AGENDA(dayCount = 1, label = "Agenda"),

    THREE_DAYS(dayCount = 3, label = "3 days"),

    WEEK(dayCount = 7, label = "Week");

    /** Whether this zoom draws an hour grid at all. [AGENDA] does not — it is a list. */
    val isGrid: Boolean get() = this != AGENDA

    companion object {
        /** §4.3's measured default. */
        val DEFAULT: CalendarZoom = THREE_DAYS
    }
}

/**
 * **Where one entry is drawn** — the three-way split that makes §4.3's *"a `DEADLINE` is only ever
 * a banner in the all-day strip, never a timed box"* true by construction rather than by a check.
 *
 * A rung maps to exactly one lane ([RungPresentation.laneOf]), so there is no state in which a
 * deadline has been placed in the grid and something has to notice.
 */
enum class CalendarLane {

    /** The hour grid. **[OccurrenceRung.BLOCK] alone** — the only rung that occupies a slot. */
    GRID,

    /**
     * The banner strip above the grid: deadlines, spans, challenge windows, goal deadlines, and
     * (once [#61](https://github.com/idomarhaim/Android_Final_Project/issues/61) ships) hand-made
     * Google events. Also where a carried-forward entry lands — see [CarryForward].
     */
    ALL_DAY,

    /**
     * §4.3's second strip: **work due that day that was never given a time**, which is
     * [OccurrenceRung.ALL_DAY].
     *
     * It is separate from [ALL_DAY] the lane and that is the whole point — *"without which the
     * calendar quietly lies about the day's real workload"*. Folded into the banner strip it would
     * read as scenery beside a challenge window instead of as work somebody owes.
     */
    UNTIMED,
}

/**
 * §4.3: ***"the rung is carried by the form of the leading time column, never by a glyph on the
 * chip"*** — the prototype's rev-2 table, as a type.
 *
 * §0.8's surviving sub-rule is what forced it: the chip was carrying **two unrelated axes**, the
 * rung (a property of the occurrence) and the life area (a property of the goal), and forced into
 * one pill the rung had to degrade into a symbol nobody could read. So the chip carries only the
 * life area and the rung is carried here, in **form**, with **no legend and no symbol vocabulary**.
 */
enum class TimeColumnForm {

    /** [OccurrenceRung.BLOCK]: start over end with a filled rail between — *a span you are inside*. */
    RAIL,

    /** [OccurrenceRung.DEADLINE]: `due` + the time, then a single point — *a moment, not a duration*. */
    POINT,

    /** [OccurrenceRung.SPAN]: a date range and a soft capsule — *days, not hours*. */
    CAPSULE,

    /** [OccurrenceRung.ALL_DAY]: the words **all-day**, and no time at all. */
    WORDS,
}

/**
 * The one table both presentation questions are answered from.
 *
 * They are derived from the same `when` deliberately: a lane and a form that disagreed — a
 * deadline drawn as a [TimeColumnForm.RAIL] in the [CalendarLane.GRID] — is exactly §0.3's *second
 * number that quietly disagrees*, and two separate `when`s is how that gets built.
 */
object RungPresentation {

    /** Which lane this rung is drawn in. Total over [OccurrenceRung], with no default branch. */
    fun laneOf(rung: OccurrenceRung): CalendarLane = when (rung) {
        OccurrenceRung.BLOCK -> CalendarLane.GRID
        OccurrenceRung.DEADLINE -> CalendarLane.ALL_DAY
        OccurrenceRung.SPAN -> CalendarLane.ALL_DAY
        OccurrenceRung.ALL_DAY -> CalendarLane.UNTIMED
    }

    /** What shape this rung's leading time column takes. */
    fun timeColumnFormOf(rung: OccurrenceRung): TimeColumnForm = when (rung) {
        OccurrenceRung.BLOCK -> TimeColumnForm.RAIL
        OccurrenceRung.DEADLINE -> TimeColumnForm.POINT
        OccurrenceRung.SPAN -> TimeColumnForm.CAPSULE
        OccurrenceRung.ALL_DAY -> TimeColumnForm.WORDS
    }

    /**
     * Whether this rung consumes minutes out of the waking day — **[OccurrenceRung.BLOCK] alone**.
     *
     * Derived rather than chosen. §2.4: `ALL_DAY`, `DEADLINE` and `SPAN` *"occupy no slot and
     * cannot collide"*, so none of them can book time; §2.2 says separately and emphatically that
     * *"spans contribute nothing"*, or one week-long renovation swamps the day. What is left is the
     * one rung that is *"a span of time you are inside"*.
     */
    fun booksTime(rung: OccurrenceRung): Boolean = rung == OccurrenceRung.BLOCK
}

/**
 * **What kind of thing this is** — because §4.3 draws four, and only one of them is a task.
 *
 * The distinction is not decoration: it decides the fill (grey for [EXTERNAL]), whether ticking it
 * is offered at all, and what a tap opens.
 */
enum class EntryKind {

    /**
     * A task occurrence — the only kind the user can tick, drag or skip.
     *
     * ⚠️ **This sentence used to name a capability that did not exist**, and it is corrected here
     * rather than deleted: until
     * [#68](https://github.com/idomarhaim/Android_Final_Project/issues/68) it read *"tick, **drag**
     * or **reschedule**"* while two of those three were unbuilt — the forward-pointer class
     * `kb/dev/retracting-a-copied-claim.md` §5 names. Drag is now real ([DragToMove]); *reschedule*
     * as a separate verb never was, and *skip* is what actually shipped beside it
     * ([ScheduleEdit.Skip], reachable from the entry menu).
     */
    TASK,

    /** §4.3's *goal deadlines*: an objective's own `deadlineEpochMillis`, drawn as a banner. */
    GOAL_DEADLINE,

    /** §4.3's *challenge windows*: a challenge's start..end, drawn as a span capsule. */
    CHALLENGE_WINDOW,

    /**
     * §4.3's *hand-made Google events in grey* — *"readable at no extra scope, and hiding them
     * would make the app's own calendar look empty"*.
     *
     * ⚠️ **Nothing produces one today, and that is `#60`'s stated boundary rather than an
     * oversight.** The reader is
     * [#61](https://github.com/idomarhaim/Android_Final_Project/issues/61), which has not
     * shipped; this surface has to be complete and useful with no Google account at all. The lane,
     * the fill and the chip wording exist and are tested — what is empty is the *source*.
     */
    EXTERNAL,
}

/**
 * **One thing drawn on the calendar.**
 *
 * Flat rather than sealed-per-kind, and the reason is the opposite of [Occurrence]'s: there, each
 * rung needed *different fields* and a flat shape would have admitted an `ALL_DAY` with an end
 * time. Here every kind carries the same five things — a when, a title, a life area, a state and a
 * lane — and a sealed hierarchy would buy four identical bodies plus a `when` at every call site.
 * What varies is [kind], which is a genuine enum: the fill, the tick affordance and the tap target.
 */
data class CalendarEntry(
    /** Stable within a rendered range — the occurrence id, or a synthesised one for a generated instance. */
    val key: String,
    val title: String,
    val kind: EntryKind,
    /** The *when*, as one of §2.2's four rungs. Its [Occurrence.rung] decides the lane and the form. */
    val occurrence: Occurrence,
    /** §4.3's chip, and **all** it carries: a colour and a name. `null` for work filed nowhere. */
    val lifeArea: LifeAreaChip? = null,
    /** The task this belongs to, or `null` for the three non-task kinds. */
    val taskId: String? = null,
    /** The stored occurrence document, when one exists — what a tick writes its outcome to. */
    val occurrenceId: String? = null,
    /** What happened to this window (§2.8) — [OccurrenceOutcome.Planned] for everything non-task. */
    val outcome: OccurrenceOutcome = OccurrenceOutcome.Planned,
    /**
     * The rule-generated date this entry stands in for, or `null` when it belongs to no series —
     * [ScheduledOccurrence.seriesDate], carried onto the surface.
     *
     * It is **not** [date]: a moved instance is drawn on the day it was moved to and keeps the day
     * the rule produced it on, and that second date is the only thing that identifies *which*
     * instance an edit is about. `ScheduleEdits.apply` takes it as a parameter, so without it here
     * no gesture on this surface could name the thing it is acting on.
     */
    val seriesDate: LocalDate? = null,
    /**
     * Whether the task behind this entry carries a [com.idomarhaim.goalpilot.domain.model.RepeatRule].
     *
     * **This is the whole of §2.1's *"this occurrence, or all future ones?"* trigger** — see
     * [MoveScope.isAsked]. It is a property of the *task*, not of the occurrence, which is why it
     * cannot be derived from anything else on this row: a series instance the user has never
     * touched and a one-off both arrive here as a bare [Occurrence].
     */
    val isRepeating: Boolean = false,
    /**
     * §2.3's *`AWAY`* — *"it left the GoalPilot calendar; indistinguishable from a delete"*.
     *
     * ⚠️ **It is a parameter and not a derivation, because nothing can derive it yet.** §2.7 says a
     * disappearance *"keeps its date, clears its `googleEventId`"* — so from stored data alone an
     * occurrence that lost its event is indistinguishable from one that never had one, and only
     * `#61`'s sync knows which. Wiring it to `googleEventId == null` would mark **every** occurrence
     * in the database `AWAY` on the day `#61` ships. It stays `false` until something can honestly
     * set it, and [CarryForward] is tested through both branches regardless.
     */
    val isAway: Boolean = false,
    /** Whether this entry is on today's column only because it needs action — see [CarryForward]. */
    val carriedForward: Boolean = false,
) {

    /**
     * Which lane this is drawn in.
     *
     * A carried-forward entry lands in [CalendarLane.ALL_DAY] whatever its rung: it is on today's
     * column precisely because its own slot is no longer available to it, so drawing it in the grid
     * would put it at an hour that means nothing.
     */
    val lane: CalendarLane
        get() = if (carriedForward) CalendarLane.ALL_DAY else RungPresentation.laneOf(occurrence.rung)

    val timeColumnForm: TimeColumnForm get() = RungPresentation.timeColumnFormOf(occurrence.rung)

    /** The day this sits on. For a carried entry that is still its **own** day, not today. */
    val date: LocalDate get() = occurrence.startDate

    /**
     * Whether this entry is drawn on [date]'s column — **every** day its window touches, not only
     * the day it starts.
     *
     * ⚠️ **Keying a column on [date] alone silently hides every multi-day thing after its first
     * day.** `Observed:` `CalendarBuilderTest.a challenge window overlapping the range…` — a
     * challenge running all month drew on Monday and vanished on Tuesday and Wednesday, which is
     * the exact opposite of what a *window* is for. A [com.idomarhaim.goalpilot.domain.model.Span]
     * and an overnight [com.idomarhaim.goalpilot.domain.model.Block] have the same shape and the
     * same failure.
     *
     * Half-open at the far end, matching [Occurrence.closesAt]: an
     * [com.idomarhaim.goalpilot.domain.model.AllDay] on Monday closes at Tuesday midnight and does
     * **not** reach Tuesday's column, while a block running to 02:00 does.
     */
    fun covers(date: LocalDate): Boolean =
        !date.isBefore(occurrence.startDate) && date.atStartOfDay() < occurrence.closesAt

    /** §2.3's derived temporal state. Nothing here is stored; ask with the clock you have. */
    fun stateAt(now: LocalDateTime): OccurrenceState = occurrence.stateAt(now)

    /** Whether the user has already settled this window — done or deliberately skipped. */
    val isSettled: Boolean get() = outcome.isSettled

    /** Whether §4.3's *tick to complete* is offered. Only a task's own window can be ticked. */
    val isTickable: Boolean get() = kind == EntryKind.TASK

    /**
     * Whether `ScheduleEdits.apply` can express an edit to **this** row — the gate on both
     * §4.3's *drag to move* and on `Skip`
     * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
     *
     * Three conditions, and each one is a different kind of *no*:
     *
     * 1. **It is a task.** A goal deadline, a challenge window and an `EXTERNAL` event have no
     *    occurrence document and no schedule behind them; there is nothing for an edit to write.
     * 2. **It is unsettled.** §2.3: *"a missed occurrence is never edited — it is history"*, and
     *    §2.8: *"past events stay as the record of time actually spent"*. A window the user has
     *    already ticked or skipped is that record. Moving it would say the work happened at an hour
     *    it did not, and skipping something already done is incoherent. **Un-ticking is what the
     *    tick box is for** — this is not the only route back.
     * 3. ⚠️ **It is either a series instance, or a one-off with no document yet.** This third
     *    condition guards a real hole in `#63`'s machinery rather than a rule of the product, and
     *    it is the one worth reading twice.
     *
     * ### The hole, named
     *
     * `ScheduleEdits.apply` finds the instance an edit is about with
     * `stored.firstOrNull { it.seriesDate == seriesDate }`, and its `seriesDate` parameter is a
     * **non-null** `LocalDate`. A one-off's document carries `seriesDate = null` by construction
     * ([ScheduledOccurrence.seriesDate] — *"`null` means this document is not part of a series at
     * all"*), so **that lookup can never find it**. Both scopes then go wrong, in opposite
     * directions and both silently:
     *
     * - `THIS_OCCURRENCE` falls through to `TaskSchedule.instanceOn`, whose result has a blank id,
     *   so the upsert **creates a second one-off document** and the calendar draws the entry twice.
     * - `THIS_AND_FUTURE` writes `Task.occurrence` and touches no document — but a one-off *with* a
     *   document is drawn **from that document**, so the move appears to do nothing at all.
     *
     * `Observed:` read out of `ScheduleEdits.apply`, `TaskSchedule.occurrencesIn` and
     * `CalendarViewModel.seriesDateOf` on 2026-08-23; `Untested:` on a device, because this
     * property is what stops the app reaching it. It is reachable two ways today — ticking a
     * one-off (`CalendarViewModel.setDone` writes `seriesDate = null`) and `#61` pushing one to
     * Google (`SyncCalendarUseCase` line 403) — and condition 2 already excludes the first, so
     * what this third condition actually buys is **the Google-linked one-off**. Fixing it properly
     * means widening that parameter to `LocalDate?`, which is a change to `ScheduleEdits`'
     * semantics and was out of `#68`'s scope by name.
     *
     * ✅ **It has a ticket:
     * [#69](https://github.com/idomarhaim/Android_Final_Project/issues/69).** When that lands, this
     * third condition comes off together with `DragToMoveTest.a one-off that already has a
     * document…`, and [MoveScope.seriesDateOf]'s KDoc — which explains why its `?: date` fallback
     * is safe *because* of this guard — needs re-reading rather than left standing.
     */
    val isEditable: Boolean
        get() = isTickable && !isSettled && (seriesDate != null || occurrenceId == null)

    /**
     * Whether §4.3's *drag to move* is offered on this row **by virtue of its lane**.
     *
     * [isEditable] plus a lane with somewhere to drop it. Only [CalendarLane.GRID] has a geometry —
     * a day per column, an hour per row — so only there does a finger's travel mean a date and a
     * time. The banner strip and the untimed strip are ordered lists: a row's position in them
     * carries no *when*, so a drag could only mean *reorder*, which is not a thing this app has.
     * Those rows still reach `Skip` through the entry menu, which needs no geometry.
     *
     * ⚠️ **This is a property of the entry, and the zoom is a property of the screen — so it is a
     * necessary condition and not a sufficient one.** [CalendarZoom.AGENDA] draws a timed block as
     * a row in a list rather than in a grid, and this returns `true` for it: the *rung* has not
     * changed, only what is drawn around it. `AgendaColumn` is what declines to offer the drag
     * there, and says so at that site. Written down because the two facts are three files apart,
     * and a reader who found only this one would conclude the agenda level is draggable.
     */
    val isDraggable: Boolean get() = isEditable && lane == CalendarLane.GRID
}

/** §4.3's chip, and the whole of it: **a colour dot and its name**, and no second axis. */
data class LifeAreaChip(val id: String, val name: String, val colorHex: String)

/**
 * ***"`OVERDUE` and `AWAY` are both carried forward from other days"*** — §4.3, and the one clause
 * the prototype's own checks caught as a **bug** rather than a preference.
 *
 * Rev 4: *"B carried `OVERDUE` items forward from other days but not `AWAY` ones, so an event that
 * vanished from Thursday's calendar would surface only when Thursday arrived — exactly too late to
 * put it back."* Both need action, and neither waits for you to navigate to its date.
 */
object CarryForward {

    /**
     * Whether an entry in this state is dragged onto today from whatever day it sits on.
     *
     * Deliberately **not** every past state. [OccurrenceState.MISSED] is a failure whose slot is
     * gone and §2.3 is explicit that *"a missed occurrence is never edited — it is history"*;
     * [OccurrenceState.EXPIRED] *"counts for nothing, silently"*; [OccurrenceState.DAY_PASSED] and
     * [OccurrenceState.WINDOW_CLOSED] closed on their own terms. Carrying those forward would turn
     * today's column into a backlog of everything that ever lapsed, which is the nag §4.2 says Home
     * must not become.
     */
    fun carries(state: OccurrenceState, isAway: Boolean): Boolean =
        isAway || state == OccurrenceState.OVERDUE

    /**
     * The entries from outside [today] that belong on today's column, marked as carried.
     *
     * @param entries every entry in the loaded window, whatever day it sits on.
     * @param today the day they are carried **onto** — one column, never all the visible ones. An
     *   overdue deadline repeated across three columns is three demands for one piece of work.
     * @param now the clock §2.3's states are derived against.
     */
    fun onto(today: LocalDate, entries: List<CalendarEntry>, now: LocalDateTime): List<CalendarEntry> =
        entries
            .filter { it.date != today }
            .filterNot { it.isSettled }
            .filter { carries(it.stateAt(now), it.isAway) }
            .map { it.copy(carriedForward = true) }
}

/**
 * ***"Fully actionable: … drag to move …"*** — §4.3's third verb, as arithmetic
 * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
 *
 * ### Why the whole gesture is a pure function of four numbers
 *
 * A drag handler is the easiest thing in a Compose screen to get subtly wrong and the hardest to
 * see wrong: it is off by a column at one zoom, or it snaps to the wrong quarter-hour, or it lets a
 * block start after the grid ends — and every one of those renders as *a block, somewhere*, which
 * is what the screen draws when it is right. `#68`'s brief names this exactly: *"a unit test of a
 * drag handler is a test of your own arithmetic"*, and the way out is to make the arithmetic
 * something other than the handler. So the handler accumulates a pixel delta and hands it here;
 * everything that decides **which day and which minute** is in this object, takes its geometry as
 * an argument, and is exercised on the JVM with no device.
 *
 * That is the same split [CalendarBuilder] already has with the lanes, and it is why the
 * instrumented test can be about *the gesture arriving* rather than about where it lands.
 */
object DragToMove {

    /**
     * Where a drag may land — **the drawn grid, and nothing outside it**.
     *
     * @param columnPitchPx the distance between one column's left edge and the next, in pixels.
     *   The **pitch**, not the drawn width: [DayColumns] gives every column 2 dp of padding on each
     *   side, so consecutive columns sit 4 dp further apart than they are wide, and measuring by
     *   width alone would make every drag land short of where the finger went — a systematic error
     *   that grows with the distance dragged and reads as *the calendar is sluggish*.
     * @param hourHeightPx the drawn height of one hour row.
     * @param days the drawn columns, in order. A drag is clamped **into** this list rather than
     *   allowed to leave it: the surface can only commit a move to a day it is showing, and a drag
     *   that fell off the edge silently doing nothing is the worse of the two answers.
     * @param hourFrom first drawn hour, inclusive.
     * @param hourTo last drawn hour, exclusive — the grid's own crop ([HourGrid]).
     * @param snapMinutes the grain a landing time is rounded to.
     */
    data class Geometry(
        val columnPitchPx: Float,
        val hourHeightPx: Float,
        val days: List<LocalDate>,
        val hourFrom: Int = HOUR_FROM,
        val hourTo: Int = HOUR_TO,
        val snapMinutes: Int = SNAP_MINUTES,
    ) {
        /** Whether this geometry has been measured yet. A zero pitch is a column not yet laid out. */
        val isMeasured: Boolean
            get() = columnPitchPx > 0f && hourHeightPx > 0f && days.isNotEmpty()
    }

    /** Where a drag landed: one of the drawn days, and a minute of that day. */
    data class Target(val date: LocalDate, val minuteOfDay: Int) {
        val time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    }

    /**
     * The grain a landing time is rounded to.
     *
     * Not a preference: the grid draws one hour per row, so a finger resolving finer than a
     * quarter of that is resolving finer than the thing it is pointing at, and a block starting at
     * 09:07 is a number nobody chose.
     */
    const val SNAP_MINUTES: Int = 15

    /**
     * How far a finger may wander and still count as *held, not moved*.
     *
     * One long press has to serve two verbs, because the chip has no room for a third control — at
     * three days a column is ~110 dp and §4.3's own measurement already spends it on the time
     * column, the title and the tick. So a long press **picks the row up**, and what happens on
     * release is decided here: put back where it started, it opens the entry menu; carried
     * somewhere else, it moves. A finger resting on a chip drifts a few pixels, which is what this
     * absorbs.
     */
    const val PRESS_SLOP_PX: Float = 24f

    /**
     * Where a drag of [dragXPx], [dragYPx] from [entry] lands — **clamped, then snapped, in that
     * order**.
     *
     * The order matters and is the one thing here worth a comment. Snapping first and clamping
     * afterwards can push a landing back off the grain — clamping to `hourFrom * 60` is exact, but
     * the last legal start is not a multiple of [Geometry.snapMinutes] in general — so a block
     * would land at 22:53 having been snapped to 23:00. Clamping first and snapping inside a range
     * whose ends are both handled cannot leave it, which is why the result is coerced once more.
     *
     * Returns `null` when the geometry has not been measured — a drag before the first layout pass
     * has no frame to be measured in, and inventing one would put the block on a day chosen by
     * arithmetic over a zero.
     */
    fun targetOf(entry: CalendarEntry, dragXPx: Float, dragYPx: Float, geometry: Geometry): Target? {
        if (!geometry.isMeasured) return null
        val fromIndex = geometry.days.indexOf(entry.date)
        // A drag on a row whose own day is not drawn cannot be placed relative to it. The only way
        // to be here is a carried-forward entry, which is drawn in the banner strip and therefore
        // never draggable -- but `isDraggable` is a property of the entry and this is a property of
        // the geometry, so neither can enforce the other's half.
        if (fromIndex < 0) return null

        val columns = Math.round(dragXPx / geometry.columnPitchPx)
        val date = geometry.days[(fromIndex + columns).coerceIn(geometry.days.indices)]

        val fromMinute = entry.occurrence.opensAt.let { it.hour * 60 + it.minute }
        val minutes = fromMinute + Math.round(dragYPx / geometry.hourHeightPx * 60f)
        val first = geometry.hourFrom * 60
        // The LAST legal start, not the last drawn minute: a block that opens on the grid's closing
        // edge is a row with no height, which reads as the drag having lost it.
        val last = geometry.hourTo * 60 - geometry.snapMinutes
        val clamped = minutes.coerceIn(first, last)
        val snapped = Math.round(clamped.toFloat() / geometry.snapMinutes) * geometry.snapMinutes

        return Target(date = date, minuteOfDay = snapped.coerceIn(first, last))
    }

    /**
     * [entry]'s occurrence, moved to [target] — **rung, duration and placement preserved**.
     *
     * `Occurrence.onDate` already moves a shape to another day keeping its time of day, which is
     * what a series expansion needs; a drag additionally moves the *time*, and only for the one
     * rung that has one in the grid. Everything else falls through to the day move alone, so a rung
     * that reaches the grid later cannot land here with an invented hour.
     */
    fun movedTo(entry: CalendarEntry, target: Target): Occurrence {
        val onDay = entry.occurrence.onDate(target.date)
        if (onDay !is Block) return onDay
        val start = target.date.atTime(target.time)
        return Block(
            start = start,
            end = start.plus(Duration.between(onDay.opensAt, onDay.closesAt)),
            placement = onDay.placement,
        )
    }

    /** Whether a drag travelled far enough to be a move rather than a press. See [PRESS_SLOP_PX]. */
    fun isMove(dragXPx: Float, dragYPx: Float): Boolean =
        kotlin.math.hypot(dragXPx, dragYPx) >= PRESS_SLOP_PX
}

/**
 * ***"This occurrence, or all future ones?"*** — §2.1's question, decided **before** it is asked
 * ([#68](https://github.com/idomarhaim/Android_Final_Project/issues/68)).
 */
object MoveScope {

    /**
     * Whether the scope sheet appears at all.
     *
     * **Only where a rule exists.** §2.1's question is what *"a field-only model always answers
     * just this one; a rule-only model always answers all of them"* is about — it is a question
     * about a **series**, and a task with no `RepeatRule` has no other instances for either answer
     * to differ over. Asking anyway would put a sheet in front of a decision with one outcome,
     * which teaches the person that the sheet means nothing on the day it means something.
     */
    fun isAsked(entry: CalendarEntry): Boolean = entry.isEditable && entry.isRepeating

    /**
     * The scope used when the question is **not** asked — and it is `THIS_AND_FUTURE`, which looks
     * like the wrong one.
     *
     * A one-off has exactly one instance, so *this occurrence* and *this and all future ones* name
     * the same window and any scope should do. They do not do the same thing, and the difference is
     * **which document the write lands in**:
     *
     * - `THIS_OCCURRENCE` writes an **occurrence document** carrying the new *when*. `#63` built
     *   that leg for a series instance, where a document is exactly what an override is.
     * - `THIS_AND_FUTURE` writes **`Task.occurrence`** — the anchor — through
     *   `ScheduleEdits.moveSeries`' no-rule branch.
     *
     * For a one-off, the anchor **is** the *when*: `CalendarViewModel.create` puts a new task's
     * occurrence there rather than in a document precisely because *"a document for a one-off with
     * no series would be the 26-duplicate-documents-a-year shape §2.1 rejects, arriving one at a
     * time"*. Every other surface that shows a task's *when* reads that field. So moving a one-off
     * through `THIS_OCCURRENCE` would manufacture the document §2.1 refuses **and** leave the
     * task's own anchor pointing at the old time — §0.3's *second number that quietly disagrees*,
     * on the field the rest of the app reads.
     *
     * `Skip` is unaffected either way: `ScheduleEdits.endSeries` says so in its own KDoc — *"a task
     * with no rule degenerates to the same thing"* — and takes the `THIS_OCCURRENCE` write for both
     * scopes.
     */
    val whenNotAsked: EditScope = EditScope.THIS_AND_FUTURE

    /**
     * The date `ScheduleEdits.apply` identifies this entry by.
     *
     * A series instance is named by the day the **rule** produced it on, which survives the
     * instance being moved away from it; a one-off has no such date and is named by the only day it
     * has. [CalendarEntry.isEditable] is what guarantees the second branch is reached only where
     * that day really is the task's anchor.
     */
    fun seriesDateOf(entry: CalendarEntry): LocalDate = entry.seriesDate ?: entry.date
}

/**
 * Something the surface has to **say** rather than draw — §0.4's *legal, but never silent*.
 *
 * Typed rather than a `String` because [TooLarge] carries two numbers the message is required to
 * name, and a pre-formatted sentence in a view model is a literal that escapes both the language
 * the screen is rendering in and the test that reads them.
 */
sealed interface CalendarNotice {

    /**
     * `SchedulePlan.TooLarge`, surfaced.
     *
     * Reachable from exactly one edit — `THIS_AND_FUTURE` + a move, on a series that has run long
     * enough that writing its past down exceeds Firestore's batch limit. `SchedulePlan.TooLarge`'s
     * own KDoc explains why refusing beats chunking or truncating; §0.4 is why the refusal is not
     * allowed to be a no-op the user watches happen.
     */
    data class TooLarge(val required: Int, val limit: Int) : CalendarNotice

    /** The write itself failed — offline, denied, or a batch that did not commit. */
    data object EditFailed : CalendarNotice
}
