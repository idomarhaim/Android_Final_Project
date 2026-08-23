package com.idomarhaim.goalpilot.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * §2.2's four rungs — **discriminated by what a miss means**, which is the only thing that
 * makes them four rather than one nullable date
 * ([#56](https://github.com/idomarhaim/Android_Final_Project/issues/56)).
 *
 * | Rung | What it is | A miss means |
 * |---|---|---|
 * | [ALL_DAY] | a day with no slot | the day passed |
 * | [DEADLINE] | a moment you owe something by | late, still owed |
 * | [BLOCK] | a span of time you are inside | the slot is gone |
 * | [SPAN] | days, not hours | the window closed |
 *
 * ⚠️ **`core/util/AnalyticsRange.kt` also carries the letters `BLOCK`, and it is a different
 * concept.** Its `DAY_BLOCKS` / `DAY_BLOCK_HOURS` are how a day is cut into six chart columns;
 * nothing there is a task rung, and there is no `ALL_DAY` in that file at all. The two must not
 * be unified: one is an analytics window over completions that already happened, the other is a
 * commitment about work that has not.
 */
enum class OccurrenceRung {
    ALL_DAY,
    DEADLINE,
    BLOCK,
    SPAN;

    companion object {
        /** Unknown, misspelled and absent all read as `null` — *this task has no occurrence*. */
        fun fromName(name: String?): OccurrenceRung? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * Whether a [Block]'s slot was endorsed, and by what — §2.3's placement states, §2.4's rule.
 *
 * **It lives on [Block] alone, and that is the decision, not an omission.** §2.4: `ALL_DAY`,
 * `DEADLINE` and `SPAN` *"occupy no slot and cannot collide → the agent sets them silently"*,
 * while *"a `BLOCK` needs confirmation, because 09:00 may already be taken"*. So a placement
 * state on the other three rungs would be a field with exactly one legal value — and §2.3's
 * `EXPIRED` (*an unconfirmed block whose time passed*) is a state only a block can be in.
 * Hanging it off the one rung it discriminates makes the other three unrepresentable rather
 * than merely unwritten.
 */
enum class BlockPlacement {

    /** Agent-placed, not yet confirmed. Its miss is [OccurrenceState.EXPIRED] — silent. */
    PROVISIONAL,

    /**
     * Agent-placed and **already confirmed, because the slot was visibly free** (§2.4).
     *
     * §2.3: *"`SILENT` and `PROVISIONAL` sit on the same day on purpose — they differ by
     * whether the app could **see** the slot, not by how confident it is."* So this is a
     * confirmed block for every purpose below, and its miss is a real [OccurrenceState.MISSED].
     */
    SILENT,

    /** Endorsed — by the user, or by a batch sheet they approved. */
    CONFIRMED;

    /** Whether a passed slot in this state is a miss ([SILENT], [CONFIRMED]) or silence. */
    val isEndorsed: Boolean get() = this != PROVISIONAL

    companion object {
        /**
         * Absent reads as [CONFIRMED], which is the honest value for **everything the app can
         * create today**: a block a person typed is one they endorsed by typing it. The agent
         * that would write [PROVISIONAL] is §3.7's proposed plan (`#24`) and does not exist,
         * so no stored absence can already mean *unconfirmed*.
         */
        fun fromName(name: String?): BlockPlacement =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CONFIRMED
    }
}

/**
 * §2.3's **derived** temporal state — *"no sweep, nothing deployed, nothing to go stale"*.
 *
 * ### Four rungs mean four meanings of a miss, and §2.3's vocabulary names two of them
 *
 * §2.3 lists `MISSED`, `OVERDUE` and `EXPIRED`; §2.2 requires **four** distinct miss meanings
 * and calls its table the spec. [DAY_PASSED] and [WINDOW_CLOSED] are those two remaining
 * meanings given names here, because the alternative is folding them into `MISSED` — and
 * `MISSED` is defined as *"a block whose slot has gone"* and is marked **a failure**, which
 * neither of them is. `Inferred:` the gap is in §2.3's list, not in §2.2's table, and this
 * enum resolves it toward §2.2 because §2.2 is the one that says what a miss *means*.
 *
 * ### Nothing here is stored, and the whole point is that it cannot be
 *
 * Every constant is a function of an [Occurrence] and a `now`. §2.3 rejected the available
 * `onSchedule` sweep because it *"buys only a stored field that can disagree with the
 * dates"*, so this enum has no wire form, no DTO field and no mapper. If you find yourself
 * writing one, the thing you actually want is a different [Occurrence].
 */
enum class OccurrenceState {

    /** Its window has not opened. Nothing is owed yet. */
    SCHEDULED,

    /**
     * You are inside it.
     *
     * Reachable for [AllDay], [Block] and [Span], and **never** for [Deadline]: a deadline's
     * window is a single instant, so the half-open test `opensAt <= now < closesAt` cannot
     * hold for it. That is the unrepresentability, not a branch.
     */
    UNDERWAY,

    /** §2.2 `ALL_DAY`: **the day passed**. */
    DAY_PASSED,

    /**
     * §2.2 `DEADLINE`: **late, and still owed**.
     *
     * §2.3: *"`OVERDUE` split from `MISSED` earns its keep twice"* — it is **not a failure**,
     * and it is **the one state that keeps reminding**. Both halves are properties below
     * rather than knowledge each caller re-derives.
     */
    OVERDUE,

    /** §2.2 `BLOCK`, endorsed: **the slot is gone**. §2.3 marks this one a failure. */
    MISSED,

    /**
     * §2.3: *"an unconfirmed block whose time passed — counts for nothing, silently"*.
     *
     * Without it *"an over-eager agent manufactures failures"*: a block the app guessed at and
     * nobody endorsed must not become a black mark when it lapses. So it is past, it is not a
     * failure, and it never reaches the daily review.
     */
    EXPIRED,

    /** §2.2 `SPAN`: **the window closed**. */
    WINDOW_CLOSED;

    /** Whether the window has closed — true for every miss state, false for the other two. */
    val isPast: Boolean
        get() = this != SCHEDULED && this != UNDERWAY

    /**
     * Whether this counts against the user — **[MISSED] alone**.
     *
     * §2.3 says so of exactly one constant (*"`MISSED` … → a failure"*), says the opposite of
     * [OVERDUE] (*"NOT a failure"*), and says [EXPIRED] *"counts for nothing"*.
     *
     * ⚠️ **The reader this was written for arrived, and does not use it.** This KDoc said
     * *"§4.7's per-life-area failure surface (`C19` #41) is the reader it is written for"*;
     * that surface shipped as `BuildSuccessFailureRunUseCase`
     * ([#64](https://github.com/idomarhaim/Android_Final_Project/issues/64)) and counts
     * [DAY_PASSED] and [WINDOW_CLOSED] as misses too. The sentence is corrected rather than
     * left standing, because a stale pointer here sends the next reader to the wrong property.
     *
     * The property is **not wrong** — it answers a narrower question. §2.3's three-word
     * vocabulary predates `#56`'s split, so *"`MISSED` is a failure"* is a sentence about the
     * **block rung**; §4.7 counts *windows*, and all four rungs have one.
     * [meetsUserInDailyReview] is the property that already groups the four, and
     * `DailyMissReview` already shows all four to the user as misses — so a run built on this
     * one would say `0 missed` about windows the daily review had just named. The full
     * derivation, with the third reason (`OccurrenceDraft` can only produce `ALL_DAY` and
     * `DEADLINE`, so this set is structurally always empty in the shipped app), is in
     * `BuildSuccessFailureRunUseCase.outcomeOf`.
     *
     * `Observed:` nothing in `app/src/main` **reads** this property as of `#64` — checked
     * mechanically over the source tree, not by eye. A grep for the name returns three hits
     * outside this file and all three are **prose** in that use case's KDoc, which is worth
     * saying here so the next person running the same grep does not read it as a
     * contradiction. `OccurrenceTest` pins the set over the whole enum and is unchanged.
     */
    val countsAsFailure: Boolean get() = this == MISSED

    /**
     * Whether a reminder for this state should keep arriving — **[OVERDUE] alone** (§2.3).
     *
     * A missed block goes silent *"because its slot is gone"*: there is nothing left to do at
     * the time it was about. A passed deadline is still owed, so it is the one thing worth
     * saying again.
     */
    val keepsReminding: Boolean get() = this == OVERDUE

    /**
     * Whether this miss meets the user in §2.5's **daily review** — the four of §2.2's table,
     * and never [EXPIRED], which §2.3 requires to pass in silence.
     */
    val meetsUserInDailyReview: Boolean
        get() = this == DAY_PASSED || this == OVERDUE || this == MISSED || this == WINDOW_CLOSED
}

/**
 * **One occurrence of a task: the *when*, and nothing else** — §2.2, `#56`.
 *
 * ### Why this is a sealed hierarchy and not a rung plus two nullable dates
 *
 * The flat shape — `rung` + `start` + `end?` — admits an `ALL_DAY` with an end time, a
 * `BLOCK` with no end, and a `SPAN` that finishes before it starts. Every one of those is a
 * state some reader then has to normalise, which is the shape `#55` deleted twice over (a
 * `done` flag beside a stamp; a point value beside its inputs). Here each rung carries exactly
 * the fields its miss semantics need and no others, so the normaliser has nothing to do and the
 * tests it would have needed do not exist.
 *
 * ### What `#56` deliberately left out, and where it now lives (`#63`)
 *
 * §2.1 wants *both* a rule on the task **and** occurrence documents, so that
 * *"this occurrence, or all future ones?"* is askable. `#56` built the occurrence and not the
 * rule, and said so: a task had at most **one** occurrence, so nothing here could express
 * `R18`'s fortnightly flowers, a moved instance, a skip, or a Google event id.
 *
 * **`#63` built the other half, and the prediction held: nothing below changed.** [RepeatRule]
 * is the rule, [ScheduledOccurrence] is the stored instance, and [TaskSchedule] is where the two
 * meet — every one of them built *on top of* this file, because the rungs, their miss semantics
 * and their reminders were per-occurrence already. A repeating task's instances are this
 * template moved to another day ([onDate]), which is why each still carries §2.2's meaning of a
 * miss rather than a date with a rung bolted back on.
 *
 * ### Everything temporal is derived from [stateAt]
 *
 * §2.3: temporal state is derived, never stored. There is no `isMissed`, no `status`, nothing a
 * sweep would have to keep true. Ask [stateAt] with the clock you have; two callers asking at the
 * same instant cannot disagree, because there is no second copy to disagree with.
 */
sealed interface Occurrence {

    val rung: OccurrenceRung

    /** When the window opens — **inclusive**. */
    val opensAt: LocalDateTime

    /**
     * When the window closes — **exclusive**, so an occurrence and its successor never both
     * contain the same instant. The same half-open convention `core/util/TimeWindow` uses, and
     * for the same reason.
     *
     * Equal to [opensAt] for [Deadline], whose window is a single instant.
     */
    val closesAt: LocalDateTime

    /** What a miss of *this* rung means (§2.2) — the state [stateAt] lands on once past. */
    val missState: OccurrenceState

    /**
     * The moment §2.5's reminder is timed **against** for this rung — the thing the reminder
     * arithmetic computes backwards from, or lands on.
     *
     * Not the same as [opensAt] for [AllDay] and [Span], whose window opens at midnight while
     * the user is asleep. See `OccurrenceReminders` for what each rung does with it.
     */
    val remindAgainst: LocalDateTime

    /**
     * §2.3's derivation, in one place: **where [now] sits relative to the window**.
     *
     * Half-open on both ends, which is what keeps [Deadline] out of [OccurrenceState.UNDERWAY]
     * without a rung check: its window has zero width, so no instant is inside it.
     */
    fun stateAt(now: LocalDateTime): OccurrenceState = when {
        now.isBefore(opensAt) -> OccurrenceState.SCHEDULED
        now.isBefore(closesAt) -> OccurrenceState.UNDERWAY
        else -> missState
    }
}

/** §2.2 `ALL_DAY` — **a day with no slot**; a miss means the day passed. */
data class AllDay(val date: LocalDate) : Occurrence {
    override val rung: OccurrenceRung get() = OccurrenceRung.ALL_DAY
    override val opensAt: LocalDateTime get() = date.atStartOfDay()
    override val closesAt: LocalDateTime get() = date.plusDays(1).atStartOfDay()
    override val missState: OccurrenceState get() = OccurrenceState.DAY_PASSED

    /**
     * Midnight, which is **not** when to remind and is not used as such: it is the anchor
     * `OccurrenceReminders` moves forward to the day's first waking minute. A reminder at
     * `00:00` for a day's work is a reminder nobody sees.
     */
    override val remindAgainst: LocalDateTime get() = opensAt
}

/**
 * §2.2 `DEADLINE` — **a moment you owe something by**; a miss means late and still owed.
 *
 * The one rung whose reminder is computed **backwards from how long the work takes** (§2.5),
 * which is the app's stated differentiator. See `OccurrenceReminders.planFor`.
 */
data class Deadline(val at: LocalDateTime) : Occurrence {
    override val rung: OccurrenceRung get() = OccurrenceRung.DEADLINE
    override val opensAt: LocalDateTime get() = at
    override val closesAt: LocalDateTime get() = at
    override val missState: OccurrenceState get() = OccurrenceState.OVERDUE
    override val remindAgainst: LocalDateTime get() = at
}

/**
 * §2.2 `BLOCK` — **a span of time you are inside**; a miss means the slot is gone.
 *
 * [placement] is what splits that miss in two (§2.3): an endorsed block that lapses is
 * [OccurrenceState.MISSED] and counts as a failure, while an unconfirmed one is
 * [OccurrenceState.EXPIRED] and counts for nothing. Nothing else in the model branches on it.
 */
data class Block(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val placement: BlockPlacement = BlockPlacement.CONFIRMED,
) : Occurrence {
    override val rung: OccurrenceRung get() = OccurrenceRung.BLOCK
    override val opensAt: LocalDateTime get() = start

    /**
     * The block's end, **never before its start**.
     *
     * Coerced rather than rejected: the constructor's inputs come off the wire, and an
     * inverted pair stored by some future writer should give a zero-length block the user can
     * see and fix, not an exception between a snapshot and a frame. A zero-length block is
     * already meaningful here — it is `SCHEDULED`, then its miss state, and never `UNDERWAY`.
     */
    override val closesAt: LocalDateTime get() = if (end.isBefore(start)) start else end

    override val missState: OccurrenceState
        get() = if (placement.isEndorsed) OccurrenceState.MISSED else OccurrenceState.EXPIRED

    /** The block's start: the reminder lands on the slot, which already holds the work. */
    override val remindAgainst: LocalDateTime get() = start
}

/**
 * §2.2 `SPAN` — **days, not hours**; a miss means the window closed.
 *
 * ⚠️ §2.2: *"Spans contribute nothing to the time-allocation chart"*, or one week-long
 * renovation swamps every life area. Nothing in this ticket feeds the chart from an occurrence
 * — `TimeAllocationUseCase` reads `estimatedMinutes`, which is **effort** and is untouched by
 * how many days a span covers — so that clause holds by construction today. It is written here
 * because the first thing that reads a span's elapsed dates is the first thing that can break it.
 */
data class Span(val from: LocalDate, val to: LocalDate) : Occurrence {
    override val rung: OccurrenceRung get() = OccurrenceRung.SPAN
    override val opensAt: LocalDateTime get() = from.atStartOfDay()

    /**
     * The day *after* the last day, so the whole final day is inside the window. Never before
     * [from], for the reason [Block.closesAt] gives.
     */
    override val closesAt: LocalDateTime
        get() = (if (to.isBefore(from)) from else to).plusDays(1).atStartOfDay()

    override val missState: OccurrenceState get() = OccurrenceState.WINDOW_CLOSED

    /** Midnight on the first day — moved to that day's first waking minute, as [AllDay] is. */
    override val remindAgainst: LocalDateTime get() = opensAt
}

/**
 * The **add-task row's** *when* control, as data — the occurrence half of what `DurationEntry`
 * is for the duration box (`#56`).
 *
 * ### Why the entry state is in the domain and not in a `remember`
 *
 * `DurationEntry`'s KDoc gives the reason and it applies unchanged: *"the only interesting
 * logic in the box"* is a rule, and a rule that can only be exercised on a running device is a
 * rule whose branches do not all get tested. The rule here is [toOccurrence]'s — **a date alone
 * is an `ALL_DAY`, a date with a time is a `DEADLINE`** — and it decides which of §2.2's miss
 * semantics the user gets, which is the most consequential thing this row does.
 *
 * ### Two rungs, not four, and that is a scope decision rather than an omission
 *
 * `BLOCK` and `SPAN` are modelled, stored, derived, reminded and reviewed end to end; what they
 * do not have is a way to *type* one. §2.4 says a block *"needs confirmation, because 09:00 may
 * already be taken"* through a batch sheet — that is §3.7's proposed plan (`#24`) — and a span
 * is a range that belongs to §4.3's calendar surface (`#26`). Both are out of `#56`'s scope,
 * which is explicit that a full scheduling surface *"needs its own decision"*. So this row
 * offers the two rungs a person types directly, and the other two wait for their own author.
 */
data class OccurrenceDraft(
    val date: LocalDate? = null,
    /** `null` = no time of day, which is what makes it an [AllDay] rather than a [Deadline]. */
    val time: LocalTime? = null,
) {

    /** Whether the row has a *when* at all. */
    val isSet: Boolean get() = date != null

    /**
     * What gets written to the task, or `null` for *no occurrence at all* — the default, and
     * the honest state for the majority of tasks, which are simply on the list.
     *
     * A time with **no date is not a `DEADLINE`**; it is nothing, and it reads as nothing. A
     * time of day with no day is not a moment, and inventing *today* for it would silently
     * make a task overdue this evening because someone tapped the wrong control.
     */
    fun toOccurrence(): Occurrence? {
        val day = date ?: return null
        return time?.let { Deadline(day.atTime(it)) } ?: AllDay(day)
    }

    /** Sets the day, keeping any time already chosen. */
    fun withDate(date: LocalDate): OccurrenceDraft = copy(date = date)

    /** Promotes an all-day to a deadline. */
    fun withTime(time: LocalTime): OccurrenceDraft = copy(time = time)

    /**
     * Demotes a deadline back to an all-day, keeping the date.
     *
     * Its own transition rather than `copy(time = null)` at the call site, because it is a
     * change of **rung** — the task stops being *late, still owed* when it lapses and starts
     * being *the day passed* — and that is worth a name.
     */
    fun withoutTime(): OccurrenceDraft = copy(time = null)

    /** Back to no *when* at all. */
    fun cleared(): OccurrenceDraft = OccurrenceDraft()
}
