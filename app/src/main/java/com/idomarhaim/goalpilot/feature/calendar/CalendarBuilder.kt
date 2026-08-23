package com.idomarhaim.goalpilot.feature.calendar

import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * **One column of §4.3's calendar** — a day, its three lanes, and its load.
 *
 * The lanes are separate lists rather than one list plus a filter at every draw site, because the
 * split *is* the design: §4.3's untimed strip exists as a distinct surface (*"without which the
 * calendar quietly lies about the day's real workload"*) and a deadline is *"only ever a banner in
 * the all-day strip"*. A single list would let a renderer put either one anywhere.
 */
data class CalendarDay(
    val date: LocalDate,
    /** [CalendarLane.GRID]: blocks, and nothing else. Sorted by start. */
    val timed: List<CalendarEntry>,
    /** [CalendarLane.ALL_DAY]: deadlines, spans, challenge windows, goal deadlines, carried entries. */
    val allDay: List<CalendarEntry>,
    /** [CalendarLane.UNTIMED]: §4.3's *work due today that was never given a time*. */
    val untimed: List<CalendarEntry>,
    val load: DayLoad,
    val isToday: Boolean,
) {
    /** Every entry on this column, whatever lane. For counts and for the agenda list. */
    val all: List<CalendarEntry> get() = timed + allDay + untimed

    val isEmpty: Boolean get() = timed.isEmpty() && allDay.isEmpty() && untimed.isEmpty()
}

/**
 * **Assembles §4.3's surface from what the repositories already stream** — `#60`.
 *
 * Pure: a date range, a clock, and four lists in; a list of columns out. No Firestore, no zone it
 * invented, no `now` it read off the system. That is the property that lets every rule §4.3 states
 * be tested on the JVM — the load bar, the ring, carry-forward, and *a `DEADLINE` is never a timed
 * box* — which the brief names as where most of this ticket's value is.
 */
object CalendarBuilder {

    /**
     * How far back the carry-forward sweep looks for still-owed work.
     *
     * ⚠️ **This is a bound, not a truth, and it is named rather than buried for that reason.** §4.3
     * gives no horizon — an overdue deadline *"keeps reminding"* with no stated expiry — so any
     * number here caps something the spec does not cap. What rules out *unbounded* is cost: the
     * sweep expands §2.1's repeat rules over the window, and a daily task swept back to its
     * creation is an unbounded expansion on the main thread every time the screen recomposes.
     *
     * Ninety days is where the two meet: three months of still-owed work is more backlog than any
     * daily review ever shows, and it is ~90 generated instances per repeating task. `Untested:`
     * whether anything is ever missed by it — nothing in this repo has a deadline older than that,
     * so the cap has never yet excluded a real row.
     */
    const val DEFAULT_CARRY_BACK_DAYS: Long = 90

    /**
     * @param range the days to draw, in order. [CalendarZoom.dayCount] long.
     * @param today the column carried-forward work lands on. Separate from [now] because a test
     *   that fixes one and not the other cannot say what it is testing.
     * @param schedules every task paired with its stored occurrences (§2.1's aggregate).
     * @param external §4.3's hand-made Google events. **Empty until `#61`** — see [EntryKind.EXTERNAL].
     * @param zone the zone [com.idomarhaim.goalpilot.domain.model.Task.pausedUntil] and the
     *   challenge epochs are read in. Passed rather than defaulted, for
     *   [TaskSchedule.occurrencesIn]'s reason: a hidden `systemDefault()` puts a whole class of
     *   off-by-one-day defect somewhere nobody can see it.
     */
    fun build(
        range: List<LocalDate>,
        today: LocalDate,
        now: LocalDateTime,
        schedules: List<TaskSchedule>,
        goals: List<Goal>,
        lifeAreas: List<LifeArea>,
        challenges: List<Challenge>,
        external: List<CalendarEntry> = emptyList(),
        waking: WakingHours = WakingHours.DEFAULT,
        zone: ZoneId = ZoneId.systemDefault(),
        carryBackDays: Long = DEFAULT_CARRY_BACK_DAYS,
    ): List<CalendarDay> {
        if (range.isEmpty()) return emptyList()
        val from = range.first()
        val to = range.last()
        // Carry-forward is about work whose own day has PASSED, so the sweep has to see days the
        // calendar is not drawing. Loading only `from..to` finds nothing to carry whenever the
        // overdue thing is older than the first visible column -- which is the ordinary case, since
        // the first column is usually today. `Observed:` this is what
        // `CalendarBuilderTest.an overdue deadline from before the range…` caught.
        val loadFrom = if (today in range) minOf(from, today.minusDays(carryBackDays)) else from

        val chipByGoal = lifeAreaChips(goals, lifeAreas)
        val entries = taskEntries(schedules, chipByGoal, loadFrom, to, zone) +
            goalDeadlineEntries(goals, chipByGoal, loadFrom, to, zone) +
            challengeEntries(challenges, from, to, zone) +
            external

        // Computed over the whole loaded window, then landed on ONE column. Per-column would need
        // every column to see the others, and landing it on more than one would demand the same
        // piece of work two or three times over.
        val carried = if (today in range) CarryForward.onto(today, entries, now) else emptyList()

        return range.map { date ->
            val onDay = entries.filter { it.covers(date) } + carried.filter { date == today }
            val timed = onDay.filter { it.lane == CalendarLane.GRID }
                .sortedWith(compareBy({ it.occurrence.opensAt }, { it.title }))
            val allDay = onDay.filter { it.lane == CalendarLane.ALL_DAY }
                .sortedWith(compareBy({ !it.carriedForward }, { it.occurrence.opensAt }, { it.title }))
            val untimed = onDay.filter { it.lane == CalendarLane.UNTIMED }
                .sortedBy { it.title }
            CalendarDay(
                date = date,
                timed = timed,
                allDay = allDay,
                untimed = untimed,
                load = DayLoad.of(date, onDay, waking),
                isToday = date == today,
            )
        }
    }

    /** The days one zoom shows, anchored at [anchor] — its first column. */
    fun daysFor(anchor: LocalDate, zoom: CalendarZoom): List<LocalDate> =
        (0 until zoom.dayCount).map { anchor.plusDays(it.toLong()) }

    /**
     * A task's occurrences in range, as entries.
     *
     * [TaskSchedule.occurrencesIn] does the whole of §2.1's rule-plus-documents expansion, including
     * the pause, the moved instance and the one-off. Nothing here re-derives any of it — this maps
     * what it returns.
     */
    private fun taskEntries(
        schedules: List<TaskSchedule>,
        chipByGoal: Map<String, LifeAreaChip>,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): List<CalendarEntry> = schedules.flatMap { schedule ->
        val task = schedule.task
        val chip = task.goalId?.let(chipByGoal::get)
        schedule.occurrencesIn(from, to, zone).map { stored ->
            CalendarEntry(
                // A generated instance has no document and therefore no id, so the key is built
                // from the two things that do identify it: the task, and the day the rule put it
                // on. `id` alone would collapse every generated instance of one task onto one key.
                key = stored.id.ifBlank { "${task.id}@${stored.occurrence.opensAt}" },
                title = task.title,
                kind = EntryKind.TASK,
                occurrence = stored.occurrence,
                lifeArea = chip,
                taskId = task.id,
                occurrenceId = stored.id.ifBlank { null },
                outcome = stored.outcome,
                // The two fields `#68`'s edits are identified by. Neither is derivable from the
                // row: `seriesDate` is the day the RULE produced this instance on, which a moved
                // instance no longer sits on, and `isRepeating` is a property of the task, which
                // an entry otherwise carries nothing of. See `CalendarEntry.isEditable`.
                seriesDate = stored.seriesDate,
                isRepeating = task.repeatRule != null,
            )
        }
    }

    /**
     * §4.3's *goal deadlines*, as [Deadline] occurrences so they land in the all-day strip through
     * the same rule everything else does.
     *
     * Archived and already-complete objectives are dropped: a deadline you have already met is not
     * a thing that needs a banner, and §4.4's *"a card with nothing to say hides itself"* is the
     * same instinct one screen over.
     *
     * ⚠️ **"Complete" is asked only of a goal that counts something, and skipping that guard makes
     * a deadline VANISH.** [Goal.isComplete] is `progressFraction >= 1f`, and for an
     * [Goal.isUnmeasured] objective that fraction is computed against a `targetValue` **nobody
     * set** — so an unmeasured goal whose logged entries happen to sum past its default target
     * reads as finished and silently loses its banner, on the one surface whose whole job is to
     * say when things are due. `Observed:` reported on `SESSIONS.md` 2026-08-23 by
     * `66-unmeasured-percent`, which found it while sweeping §1.3's *absence is the default* and
     * left it for this session rather than editing another row's file. It is `#66`'s seventh site
     * and it was written the same day the other six were being removed.
     */
    private fun goalDeadlineEntries(
        goals: List<Goal>,
        chipByGoal: Map<String, LifeAreaChip>,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): List<CalendarEntry> = goals
        .filterNot { it.isArchived || (!it.isUnmeasured && it.isComplete) }
        .mapNotNull { goal ->
            val at = goal.deadlineEpochMillis?.let { millisToLocal(it, zone) } ?: return@mapNotNull null
            val date = at.toLocalDate()
            if (date.isBefore(from) || date.isAfter(to)) return@mapNotNull null
            CalendarEntry(
                key = "goal:${goal.id}",
                title = goal.title,
                kind = EntryKind.GOAL_DEADLINE,
                occurrence = Deadline(at),
                lifeArea = chipByGoal[goal.id],
            )
        }

    /**
     * §4.3's *challenge windows*, as [Span]s — *days, not hours*, which is exactly what a challenge
     * window is and is why it needs no rung of its own.
     *
     * A window that merely **overlaps** the range is included and clipped to it. A challenge running
     * all month is the case that matters: keyed on its start date alone it would be invisible on
     * every column except the first, which is the opposite of what a window is for.
     */
    private fun challengeEntries(
        challenges: List<Challenge>,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): List<CalendarEntry> = challenges.mapNotNull { challenge ->
        val start = millisToLocal(challenge.startAtEpochMillis, zone).toLocalDate()
        val end = millisToLocal(challenge.endAtEpochMillis, zone).toLocalDate()
        if (end.isBefore(from) || start.isAfter(to)) return@mapNotNull null
        CalendarEntry(
            key = "challenge:${challenge.id}",
            title = challenge.title,
            kind = EntryKind.CHALLENGE_WINDOW,
            occurrence = Span(
                from = if (start.isBefore(from)) from else start,
                to = if (end.isAfter(to)) to else end,
            ),
        )
    }

    /**
     * Goal id → the chip its life area draws.
     *
     * §1.2's edges are many-to-many and [Goal.lifeAreaIds] is a list, but the chip carries **one**
     * colour and one name. The first is taken rather than a second chip drawn, for §0.8's surviving
     * sub-rule: two chips on one row is the same *"one pill carrying two axes"* failure that
     * deleted the rung glyphs, arriving through the door marked *completeness*.
     */
    private fun lifeAreaChips(goals: List<Goal>, lifeAreas: List<LifeArea>): Map<String, LifeAreaChip> {
        val byId = lifeAreas.associateBy { it.id }
        return goals.mapNotNull { goal ->
            val area = goal.lifeAreaIds.firstNotNullOfOrNull { byId[it] } ?: return@mapNotNull null
            goal.id to LifeAreaChip(id = area.id, name = area.name, colorHex = area.colorHex)
        }.toMap()
    }

    private fun millisToLocal(millis: Long, zone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
}
