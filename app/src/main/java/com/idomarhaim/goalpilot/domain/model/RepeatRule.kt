package com.idomarhaim.goalpilot.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * How often a [RepeatRule] comes round — the unit its `interval` counts in.
 *
 * Four units and no more, because every unit here has to answer *"what is the same day next
 * time?"* without ambiguity, and only these four do. A `FORTNIGHTLY` constant would be
 * [WEEK] with `interval = 2` wearing a second name, which is §0.3's *second number that
 * quietly disagrees* in enum form.
 */
enum class RepeatUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    companion object {
        /** Unknown, misspelled and absent all read as `null` — *this task does not repeat*. */
        fun fromName(name: String?): RepeatUnit? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * When a series stops — **exactly one answer, never two** (`#63`).
 *
 * ### Why this is a sealed type and not `until: LocalDate?` beside `count: Int?`
 *
 * Those two fields admit *both set* and then somebody has to decide which wins; RFC 5545 has
 * that wart and every consumer of it re-implements the same tie-break. The pair is also the
 * shape [Occurrence]'s own KDoc rejects for the rungs, and `#55` deleted twice over (a `done`
 * flag beside a stamp, a point value beside its inputs). One of three, and the disagreement is
 * unrepresentable rather than guarded.
 */
sealed interface RepeatEnd {

    /**
     * It does not stop.
     *
     * **This is the constant `docs/PRODUCT_v0.3.md` §7.1 means by *"`isDone` … **absent** on a
     * recurring task"***, and the reason is in [TaskSchedule.doneness]: a series with no last
     * instance has no *"are they all done?"* to answer, so the honest answer is that there is
     * no answer. A **bounded** series does have one — see that property for the reading, and
     * for why it resolves that way.
     */
    data object Never : RepeatEnd

    /** The last day the series may land on — **inclusive**, as a person means a date. */
    data class OnDate(val date: LocalDate) : RepeatEnd

    /**
     * A fixed number of instances, counting the first one.
     *
     * `AfterCount(1)` is a series with a single instance, which is a legitimate thing to say
     * and is *not* the same as no rule at all: the rule survives an edit that a bare
     * occurrence could not carry.
     */
    data class AfterCount(val count: Int) : RepeatEnd
}

/**
 * **The recurrence half of §2.1** — the rule that lives on the task, beside the occurrence
 * documents that live in their own collection
 * ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * ### Why a rule *and* documents, when either alone looks sufficient
 *
 * §2.1 answers this and the answer is the whole shape:
 *
 * > *"A date **on** `Task` gives one — so `R18`'s flowers become **26 duplicate documents a
 * > year**… A **rule alone**, computed and unstored, cannot hold a moved instance, a skip, or a
 * > Google event id. **Both, and that combination is what makes *"this occurrence, or all
 * > future ones?"* askable** — a field-only model always answers *just this one*; a rule-only
 * > model always answers *all of them*."*
 *
 * So this type generates the series, [ScheduledOccurrence] documents override individual
 * instances of it, and [ScheduleEdits] is where the two meet.
 *
 * ### The anchor is not stored here, and that is deliberate
 *
 * A rule says *how often*, never *starting when*. The start is [Task.occurrence] — §2.2's
 * rung, already shipped by `#56` and already carrying the rung, the time of day and the
 * duration that every generated instance copies. Storing a second start on the rule would be a
 * date that can disagree with the task's own, which is §0.3 exactly; [datesFrom] takes the
 * anchor as an argument instead, so there is nothing to keep in step.
 *
 * ### Nothing here is a `Sequence` by accident
 *
 * [RepeatEnd.Never] is genuinely infinite, so [datesFrom] returns a lazy sequence and **every
 * caller states its own bound**. A caller that forgets is a caller that hangs, which is loud;
 * a rule that silently capped itself at some number of years would be quiet and wrong, and the
 * wrongness would only show up on the one long-running series nobody tests.
 */
data class RepeatRule(
    val unit: RepeatUnit,
    /** How many [unit]s between instances. Coerced to at least 1 — see [step]. */
    val interval: Int = 1,
    /**
     * Which days of the week the series lands on — **[RepeatUnit.WEEK] only**, and empty means
     * *the anchor's own weekday*.
     *
     * Meaningless for the other three units and ignored there rather than rejected: the value
     * comes off the wire, and a stored weekday set on a monthly rule should read as a monthly
     * rule, not as an exception between a snapshot and a frame.
     */
    val weekdays: Set<DayOfWeek> = emptySet(),
    val end: RepeatEnd = RepeatEnd.Never,
) {

    /**
     * [interval], never below 1.
     *
     * A stored `0` would make [datesFrom] emit the anchor forever, which is an infinite loop
     * inside a lazy sequence — the one failure here that is neither loud nor recoverable.
     * Coerced rather than rejected for [Block.closesAt]'s reason: the inputs come off the wire.
     */
    val step: Int get() = if (interval < 1) 1 else interval

    /** Whether the series has no last instance — the one case [Doneness] cannot answer. */
    val isUnbounded: Boolean get() = end is RepeatEnd.Never

    /**
     * Every date this rule lands on, starting at [anchor] — **lazy, and infinite when
     * [isUnbounded]**.
     *
     * ### The week is measured from the anchor, not from Monday and not from the locale
     *
     * *"Every other week on Monday and Thursday"* needs a definition of *which* other week, and
     * the two available ones are the ISO week and the user's locale week — both of which put the
     * fortnight boundary somewhere the user did not choose, and the second of which moves when
     * they change region. Here week `k` is simply `[anchor + 7k, anchor + 7k + 6]`, so the
     * series is anchored to the day the user picked and reads the same in every locale.
     * `AppRegion`'s week start governs how a **calendar is drawn**; it has no business deciding
     * when a commitment recurs.
     *
     * ### A month that has no such day is skipped, never clamped
     *
     * `LocalDate.plusMonths` clamps: the 31st of January plus one month is the 28th of
     * February. For a *display* that is a kindness; for a **commitment** it silently moves the
     * thing to a day nobody chose, and then the next month moves it back. So a monthly rule
     * anchored on the 31st lands only in months that have one, and a yearly rule anchored on
     * 29 February lands only in leap years. That is the honest reading of *"the same day next
     * month"*, and it is the branch worth a test.
     */
    fun datesFrom(anchor: LocalDate): Sequence<LocalDate> = when (unit) {
        RepeatUnit.DAY -> generateSequence(anchor) { it.plusDays(step.toLong()) }
        RepeatUnit.WEEK -> weeklyDates(anchor)
        RepeatUnit.MONTH -> monthlyDates(anchor, months = true)
        RepeatUnit.YEAR -> monthlyDates(anchor, months = false)
    }.let { bounded(it, anchor) }

    /**
     * [datesFrom], stopped at [lastDate] — the form nearly every caller wants, and the one that
     * cannot hang on an unbounded rule.
     */
    fun datesUpTo(anchor: LocalDate, lastDate: LocalDate): List<LocalDate> =
        datesFrom(anchor).takeWhile { !it.isAfter(lastDate) }.toList()

    /**
     * Applies [end]. [RepeatEnd.AfterCount] counts **instances**, so it is a `take`;
     * [RepeatEnd.OnDate] bounds the **calendar**, so it is a `takeWhile`. A count below 1 is an
     * empty series rather than an error — an edit can legitimately spend a series down to
     * nothing (see [ScheduleEdits]).
     */
    private fun bounded(dates: Sequence<LocalDate>, anchor: LocalDate): Sequence<LocalDate> =
        when (val e = end) {
            is RepeatEnd.Never -> dates
            is RepeatEnd.OnDate ->
                if (e.date.isBefore(anchor)) emptySequence()
                else dates.takeWhile { !it.isAfter(e.date) }
            is RepeatEnd.AfterCount ->
                if (e.count < 1) emptySequence() else dates.take(e.count)
        }

    /** Week `k` is `[anchor + 7k, anchor + 7k + 6]`; see [datesFrom] for why. */
    private fun weeklyDates(anchor: LocalDate): Sequence<LocalDate> {
        if (weekdays.isEmpty()) {
            return generateSequence(anchor) { it.plusWeeks(step.toLong()) }
        }
        return generateSequence(0L) { it + 1 }.flatMap { week ->
            val weekStart = anchor.plusWeeks(week * step.toLong())
            (0L..6L).asSequence()
                .map { weekStart.plusDays(it) }
                .filter { it.dayOfWeek in weekdays }
                // Week 0 is a partial week: the anchor is its first day by construction, so
                // this only ever drops nothing. It is here because the reader's first question
                // is "what about days earlier in the anchor's week?" and the answer is that
                // there are none.
                .filter { !it.isBefore(anchor) }
        }
    }

    /**
     * Monthly and yearly share every line except the stride, so they share the function. See
     * [datesFrom] for why a month without the anchor's day is skipped rather than clamped.
     */
    private fun monthlyDates(anchor: LocalDate, months: Boolean): Sequence<LocalDate> {
        val day = anchor.dayOfMonth
        return generateSequence(0L) { it + 1 }
            .map { n ->
                val stride = n * step.toLong()
                if (months) anchor.withDayOfMonth(1).plusMonths(stride)
                else anchor.withDayOfMonth(1).plusYears(stride)
            }
            .filter { it.lengthOfMonth() >= day }
            .map { it.withDayOfMonth(day) }
    }
}

/**
 * The calendar day this occurrence **starts on** — its identity within a series.
 *
 * Taken from [Occurrence.opensAt] rather than branched per rung, so a rung added later needs no
 * edit here. For [Span] that is the first day, which is what *"the same instance, moved"* means
 * for a range.
 */
val Occurrence.startDate: LocalDate get() = opensAt.toLocalDate()

/**
 * The same occurrence, on [date] — **rung, time of day and duration preserved**.
 *
 * This is what makes a [RepeatRule] able to generate a series from one template: the task
 * carries a single [Task.occurrence] describing *what kind of when this is*, and every instance
 * is that shape moved to another day. A 09:00–10:30 block stays 90 minutes; a three-day span
 * stays three days; a deadline keeps its hour.
 *
 * `Block`'s duration is measured with [java.time.Duration] and `Span`'s with days, which is the
 * same distinction §2.2 draws between the two rungs that are about instants and the two that
 * are about days. Using days for a block would move it across a DST boundary by an hour.
 */
fun Occurrence.onDate(date: LocalDate): Occurrence = when (this) {
    is AllDay -> AllDay(date)
    is Deadline -> Deadline(date.atTime(at.toLocalTime()))
    is Block -> {
        val newStart = date.atTime(start.toLocalTime())
        Block(
            start = newStart,
            end = newStart.plus(java.time.Duration.between(start, closesAt)),
            placement = placement,
        )
    }
    is Span -> {
        // Measured against `closesAt`, not against `to`, so an inverted stored pair moves as
        // the zero-length span `Span.closesAt` already coerces it to rather than as a negative
        // one. Same reasoning as that property's, one layer out.
        val days = java.time.temporal.ChronoUnit.DAYS.between(opensAt.toLocalDate(), closesAt.toLocalDate()) - 1
        Span(from = date, to = date.plusDays(if (days < 0) 0 else days))
    }
}
