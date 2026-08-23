package com.idomarhaim.goalpilot.feature.calendar

import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_DAY
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import java.time.LocalDate
import java.time.LocalTime

/**
 * **The two rungs nobody could type until now** — §4.3's *create by FAB or by tapping a slot*,
 * as data (`#60`).
 *
 * ### This is the thing `#56` said it was not building
 *
 * [com.idomarhaim.goalpilot.domain.model.OccurrenceDraft]'s KDoc is explicit that `BLOCK` and
 * `SPAN` are *"modelled, stored, derived, reminded and reviewed end to end; what they do not have
 * is a way to **type** one"*, and it hands each to its owner — the block to §3.7's proposed plan,
 * and the span to *"a range that belongs to §4.3's calendar surface (`#26`)"*. This is that
 * author, and it covers both: tapping a slot is what makes a block reachable for the first time in
 * the product's life, and the sheet it opens can switch rungs without switching screens.
 *
 * ### Why the entry state is in a data class and not in the composable's `remember`
 *
 * The same reason `OccurrenceDraft` and `DurationEntry` give, and it is the reason this file has
 * tests at all: *"the only interesting logic in the box is a rule, and a rule that can only be
 * exercised on a running device is a rule whose branches do not all get tested."* The rules here
 * are that a block's end follows its start when the start moves, that an end before the start is the
 * next morning rather than a negative, that a span's last day is never
 * before its first, and that switching rung preserves the day the user tapped — every one of which
 * is a branch, and none of which needs an emulator to check.
 *
 * ### It authors a `CONFIRMED` block, and that is not a default sliding through
 *
 * §2.4 requires confirmation for a block *"because 09:00 may already be taken"* — of an **agent's**
 * placement. A block the person typed is one they endorsed by typing it, which is exactly the
 * reading [BlockPlacement.fromName] already committed to. [BlockPlacement.PROVISIONAL] belongs to
 * §3.7's batch sheet and is still unreachable from any human-facing control.
 */
data class SlotDraft(
    /** The day the user tapped, and the block's start day / the span's first day. */
    val date: LocalDate,
    val title: String = "",
    val rung: AuthoredRung = AuthoredRung.BLOCK,
    /** [AuthoredRung.BLOCK] only — the slot's start. */
    val startTime: LocalTime = DEFAULT_START,
    /** [AuthoredRung.BLOCK] only — the slot's end. Earlier than [startTime] means [crossesMidnight]. */
    val endTime: LocalTime = DEFAULT_START.plusMinutes(TaskDuration.DEFAULT_MINUTES.toLong()),
    /** [AuthoredRung.SPAN] only — the window's last day, inclusive. Never before [date]. */
    val endDate: LocalDate = date,
    /** The objective this work is filed under, or `null`. Drives the chip, and nothing else. */
    val goalId: String? = null,
) {

    /** Whether this can be saved. A window with no name is a box on a calendar nobody can read. */
    val isValid: Boolean get() = title.isNotBlank()

    /**
     * Whether the block ends on the **following** day — an end wall-clock time earlier than its
     * start.
     *
     * ⚠️ **This is not a nicety; without it a tap near midnight silently produces a zero-length
     * block.** `LocalTime.plusMinutes` wraps: `23:45` plus the default half hour is `00:15`, which
     * as a *time on the same date* is fourteen and a half hours **before** the start, and
     * [Block.closesAt] then coerces the whole thing to zero. `Observed:` writing
     * `SlotDraftTest.a tap in the last hour of the day…` is what surfaced it — the draft compiled,
     * read perfectly, and produced a block you could not see on the grid.
     *
     * An end *equal* to the start is **not** a midnight crossing: it is zero, and [withEnd] pushes
     * it off. A twenty-four-hour block is not a thing anybody types on purpose.
     */
    val crossesMidnight: Boolean get() = rung == AuthoredRung.BLOCK && endTime < startTime

    /**
     * How long the authored block is, in minutes — `0` for a [AuthoredRung.SPAN], which books no
     * hours at all (§2.2).
     *
     * Modular, so a block crossing midnight reports the half hour it actually is rather than the
     * negative fourteen hours plain subtraction gives. Equal start and end read as zero.
     */
    val minutes: Int
        get() = when {
            rung != AuthoredRung.BLOCK -> 0
            endTime == startTime -> 0
            else -> Math.floorMod(
                java.time.Duration.between(startTime, endTime).toMinutes().toInt(),
                MINUTES_PER_DAY,
            )
        }

    /**
     * What gets written — one of §2.2's four rungs, and never `null`.
     *
     * Unlike [com.idomarhaim.goalpilot.domain.model.OccurrenceDraft.toOccurrence] there is no *no
     * occurrence at all* answer here, and that is the difference between the two authors: the
     * add-task row's *when* is optional and *"the honest state for the majority of tasks, which are
     * simply on the list"*, while this sheet is only ever opened **by** placing something on a
     * calendar. A draft with no when could not have been started.
     */
    fun toOccurrence(): Occurrence = when (rung) {
        AuthoredRung.BLOCK -> Block(
            start = date.atTime(startTime),
            end = (if (crossesMidnight) date.plusDays(1) else date).atTime(endTime),
            placement = BlockPlacement.CONFIRMED,
        )
        AuthoredRung.SPAN -> Span(from = date, to = if (endDate.isBefore(date)) date else endDate)
    }

    /**
     * Moves the start, **carrying the end with it** so the block keeps its length.
     *
     * A calendar that leaves the end where it was turns *"actually, start at 11"* into a
     * zero-length block the moment the new start passes the old end — and the user's next action is
     * to fix a duration they never changed. Dragging the pair is what every calendar does, and the
     * length is the thing the person actually chose.
     */
    fun withStart(time: LocalTime): SlotDraft {
        val kept = minutes.coerceAtLeast(TaskDuration.MIN_MINUTES)
        return copy(startTime = time, endTime = time.plusMinutes(kept.toLong()))
    }

    /**
     * Moves the end. An end **earlier** than the start means the following morning; an end
     * **equal** to it is pushed off.
     *
     * The two readings of *"end at 01:00 when you start at 23:00"* are a night block and a mistake,
     * and this takes the first. Rejecting it — pushing back to `start + MIN_MINUTES` — would make a
     * block across midnight **impossible to type**, which is a whole legitimate shape of evening
     * silently unavailable; and a mistaken long block is visible the instant it is made, in the
     * duration the sheet shows and in the bar it reddens. A wrong answer you can see beats a right
     * one you cannot reach.
     *
     * Equality is the one case with no such reading: a zero-length block is nobody's intent, and a
     * twenty-four-hour one is not either.
     */
    fun withEnd(time: LocalTime): SlotDraft =
        copy(endTime = if (time == startTime) time.plusMinutes(TaskDuration.MIN_MINUTES.toLong()) else time)

    /** Moves the span's last day, never before its first. */
    fun withEndDate(day: LocalDate): SlotDraft = copy(endDate = if (day.isBefore(date)) date else day)

    /**
     * Moves the first day, carrying the span's last day so the window keeps its length.
     *
     * [withStart]'s argument, one unit up: a span moved forward past its own end is a window that
     * has silently become one day long.
     */
    fun withDate(day: LocalDate): SlotDraft {
        val span = java.time.temporal.ChronoUnit.DAYS.between(date, endDate).coerceAtLeast(0)
        return copy(date = day, endDate = day.plusDays(span))
    }

    /**
     * Switches rung, **keeping the day** — which is the whole reason this is a transition with a
     * name rather than `copy(rung = …)` at the call site.
     *
     * The day is the thing the user chose by tapping; the times and the end day are defaults this
     * sheet supplied. Changing what a miss means (§2.2) while silently keeping either of those is
     * how a span quietly acquires a start time nobody typed.
     */
    fun withRung(next: AuthoredRung): SlotDraft = when (next) {
        rung -> this
        AuthoredRung.BLOCK -> copy(rung = next, startTime = DEFAULT_START, endTime = DEFAULT_START.plusMinutes(TaskDuration.DEFAULT_MINUTES.toLong()))
        AuthoredRung.SPAN -> copy(rung = next, endDate = date)
    }

    companion object {

        /** Where a draft with no tapped slot starts — 09:00, the first hour of an ordinary day. */
        val DEFAULT_START: LocalTime = LocalTime.of(9, 0)

        /**
         * §4.3's *create by tapping a slot* — the draft a tap on [date] at [hour] opens.
         *
         * The end is [TaskDuration.DEFAULT_MINUTES] later, which is the same *"half-hour chore"*
         * answer [TaskDuration.minutesOf] gives a task with no estimate. Two surfaces inventing two
         * different defaults for *how long is a thing* is §0.3 in miniature.
         */
        fun atSlot(date: LocalDate, hour: Int, minute: Int = 0): SlotDraft {
            val start = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            return SlotDraft(
                date = date,
                startTime = start,
                endTime = start.plusMinutes(TaskDuration.DEFAULT_MINUTES.toLong()),
            )
        }
    }
}

/**
 * The two rungs this sheet can author.
 *
 * Two and not four, and the omissions are the same scope decision `OccurrenceDraft` made in the
 * other direction: `ALL_DAY` and `DEADLINE` already have an author — the add-task row's *When?*
 * chip — and giving them a second one would be two controls writing the same field, which is where
 * the two disagree about what a blank time means.
 */
enum class AuthoredRung(val rung: OccurrenceRung, val label: String) {

    /** *A span of time you are inside.* A miss means the slot is gone. */
    BLOCK(OccurrenceRung.BLOCK, "Time block"),

    /** *Days, not hours.* A miss means the window closed. */
    SPAN(OccurrenceRung.SPAN, "Multi-day"),
}
