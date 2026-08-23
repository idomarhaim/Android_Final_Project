package com.idomarhaim.goalpilot.feature.calendar

import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.startDate
import java.time.LocalDate
import java.time.LocalDateTime

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

    /** A task occurrence — the only kind the user can tick, drag or reschedule. */
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
