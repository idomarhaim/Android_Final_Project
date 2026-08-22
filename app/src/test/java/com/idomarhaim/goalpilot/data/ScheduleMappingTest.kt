package com.idomarhaim.goalpilot.data

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.firestore.dto.OccurrenceDto
import com.idomarhaim.goalpilot.data.firestore.dto.RepeatRuleDto
import com.idomarhaim.goalpilot.data.firestore.dto.TaskDto
import com.idomarhaim.goalpilot.data.firestore.dto.toDomain
import com.idomarhaim.goalpilot.data.firestore.dto.toDto
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.RepeatEnd
import com.idomarhaim.goalpilot.domain.model.RepeatRule
import com.idomarhaim.goalpilot.domain.model.RepeatUnit
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * §2.1's rule and its occurrence documents **across the wire** (`#63`) — and what a document
 * written before them reads as, which is the half a round trip does not cover.
 *
 * `TaskOccurrenceMappingTest` makes the same argument for `#56`'s four fields on the task, and
 * this is deliberately its sibling rather than an extension of it: the two tickets migrate
 * independently, and a task with a rule but no occurrence document is a state that has to read
 * correctly on its own.
 *
 * The case worth reading first is the **one bound**: `RepeatEnd` is a sealed type with exactly
 * one of three answers and the wire cannot nest one, so the encoding is a discriminator beside
 * two payload fields. That is precisely the shape the domain type exists to prevent, and the
 * only thing keeping the guarantee true after a round trip is that each payload is written by
 * the kind that owns it and read only when that kind names it.
 */
class ScheduleMappingTest {

    private val day: LocalDate = LocalDate.of(2026, 8, 17)

    private fun roundTrip(rule: RepeatRule?): RepeatRule? =
        Task(id = "t1", title = "x", repeatRule = rule).toDto().toDomain().repeatRule

    private fun roundTrip(o: ScheduledOccurrence): ScheduledOccurrence? = o.toDto().toDomain()

    // ── The rule ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `every unit survives a round trip unchanged`() {
        RepeatUnit.entries.forEach { unit ->
            assertThat(roundTrip(RepeatRule(unit = unit, interval = 3)))
                .isEqualTo(RepeatRule(unit = unit, interval = 3))
        }
    }

    @Test
    fun `each of the three bounds survives, and no round trip produces two of them`() {
        val never = RepeatRule(RepeatUnit.WEEK)
        val onDate = RepeatRule(RepeatUnit.WEEK, end = RepeatEnd.OnDate(day.plusMonths(3)))
        val count = RepeatRule(RepeatUnit.WEEK, end = RepeatEnd.AfterCount(10))

        assertThat(roundTrip(never)?.end).isEqualTo(RepeatEnd.Never)
        assertThat(roundTrip(onDate)?.end).isEqualTo(RepeatEnd.OnDate(day.plusMonths(3)))
        assertThat(roundTrip(count)?.end).isEqualTo(RepeatEnd.AfterCount(10))

        // The payload the kind does not own is never written, so nothing downstream ever has to
        // adjudicate between a stored date and a stored count.
        val dto = onDate.let { Task(id = "t1", title = "x", repeatRule = it).toDto().repeatRule!! }
        assertThat(dto.endKind).isEqualTo("ON_DATE")
        assertThat(dto.endCount).isNull()
        val counted = Task(id = "t1", title = "x", repeatRule = count).toDto().repeatRule!!
        assertThat(counted.endDate).isNull()
    }

    @Test
    fun `a stray payload beside the wrong discriminator is ignored, not adjudicated`() {
        val confused = TaskDto(
            id = "t1",
            repeatRule = RepeatRuleDto(
                unit = "WEEK",
                endKind = "AFTER_COUNT",
                endCount = 4,
                endDate = "2026-12-31",
            ),
        )

        assertThat(confused.toDomain().repeatRule?.end).isEqualTo(RepeatEnd.AfterCount(4))
    }

    @Test
    fun `weekdays are written for a weekly rule and for nothing else`() {
        val weekly = RepeatRule(
            unit = RepeatUnit.WEEK,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        )
        val monthly = weekly.copy(unit = RepeatUnit.MONTH)

        assertThat(roundTrip(weekly)?.weekdays)
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        // A stored weekday set on a monthly rule can never contradict a unit that has none,
        // the same contract `occurrencePlacement` has with `occurrenceRung`.
        assertThat(Task(id = "t1", title = "x", repeatRule = monthly).toDto().repeatRule?.weekdays)
            .isEmpty()
    }

    @Test
    fun `a task written before this ticket has no rule and is not paused, with no backfill`() {
        val legacy = TaskDto(id = "t1", title = "Water the flowers")

        val task = legacy.toDomain()

        assertThat(task.repeatRule).isNull()
        assertThat(task.pausedUntil).isNull()
        // And it still reads exactly as `#56` left it -- the migration is additive.
        assertThat(task.occurrence).isNull()
        assertThat(task.isDone).isFalse()
    }

    @Test
    fun `an unrecognised unit reads as no rule rather than as an invented recurrence`() {
        val alien = TaskDto(id = "t1", repeatRule = RepeatRuleDto(unit = "FORTNIGHTLY"))

        // Guessing would put windows in front of the user on days nobody chose.
        assertThat(alien.toDomain().repeatRule).isNull()
    }

    @Test
    fun `an unreadable bound reads as Never rather than as a truncated series`() {
        val badDate = TaskDto(
            id = "t1",
            repeatRule = RepeatRuleDto(unit = "DAY", endKind = "ON_DATE", endDate = "soon"),
        )
        val missingCount = TaskDto(
            id = "t1",
            repeatRule = RepeatRuleDto(unit = "DAY", endKind = "AFTER_COUNT", endCount = null),
        )
        val alienKind = TaskDto(
            id = "t1",
            repeatRule = RepeatRuleDto(unit = "DAY", endKind = "UNTIL_I_SAY_STOP"),
        )

        // "It kept going" is visible; "it stopped in March" is not.
        assertThat(badDate.toDomain().repeatRule?.end).isEqualTo(RepeatEnd.Never)
        assertThat(missingCount.toDomain().repeatRule?.end).isEqualTo(RepeatEnd.Never)
        assertThat(alienKind.toDomain().repeatRule?.end).isEqualTo(RepeatEnd.Never)
    }

    @Test
    fun `a task whose recurrence is removed writes null over the whole rule`() {
        // `upsertTask` is a whole-document `set()`, so a surviving rule beside a cleared one is
        // §0.3's second answer. Nesting makes the clear all-or-nothing.
        val cleared = Task(id = "t1", title = "x", repeatRule = null, pausedUntil = null).toDto()

        assertThat(cleared.repeatRule).isNull()
        assertThat(cleared.pausedUntil).isNull()
    }

    @Test
    fun `pausedUntil survives as the instant it is`() {
        val paused = Task(id = "t1", title = "x", pausedUntil = 1_755_000_000_000L)

        assertThat(paused.toDto().toDomain().pausedUntil).isEqualTo(1_755_000_000_000L)
    }

    // ── The occurrence document ──────────────────────────────────────────────────────────

    @Test
    fun `every rung survives a round trip through the occurrence document`() {
        // The same codec `TaskDto` uses -- §7.1's migration is "the four task fields become the
        // first occurrence in it", which is a copy only while both spell it the same way.
        listOf(
            AllDay(day),
            Deadline(day.atTime(6, 0)),
            Block(day.atTime(9, 0), day.atTime(10, 30), BlockPlacement.PROVISIONAL),
            Span(day, day.plusDays(4)),
        ).forEach { when_ ->
            val o = ScheduledOccurrence(id = "o1", taskId = "t1", occurrence = when_)
            assertThat(roundTrip(o)?.occurrence).isEqualTo(when_)
        }
    }

    @Test
    fun `the task's four fields and the occurrence document encode a rung identically`() {
        // Asserted rather than assumed, because §7.1's migration is a field-for-field copy and
        // a divergence would show up on one rung, months later, as a window on the wrong day.
        val when_ = Block(day.atTime(9, 0), day.atTime(10, 30), BlockPlacement.SILENT)
        val onTask = Task(id = "t1", title = "x", occurrence = when_).toDto()
        val onDoc = ScheduledOccurrence(taskId = "t1", occurrence = when_).toDto()

        assertThat(onDoc.rung).isEqualTo(onTask.occurrenceRung)
        assertThat(onDoc.start).isEqualTo(onTask.occurrenceStart)
        assertThat(onDoc.end).isEqualTo(onTask.occurrenceEnd)
        assertThat(onDoc.placement).isEqualTo(onTask.occurrencePlacement)
    }

    @Test
    fun `the series date survives and is not confused with the start once moved`() {
        val moved = ScheduledOccurrence(
            id = "o1",
            taskId = "t1",
            occurrence = AllDay(day.plusDays(2)),
            seriesDate = day,
        )

        val back = roundTrip(moved)!!
        assertThat(back.seriesDate).isEqualTo(day)
        assertThat(back.occurrence).isEqualTo(AllDay(day.plusDays(2)))
        assertThat(back.isSeriesInstance).isTrue()
    }

    @Test
    fun `each outcome survives, and a stamp is written only by the two that have one`() {
        val planned = ScheduledOccurrence(taskId = "t1", occurrence = AllDay(day))
        val done = planned.copy(outcome = OccurrenceOutcome.Done(1_755_000_000_000L))
        val skipped = planned.copy(outcome = OccurrenceOutcome.Skipped(1_755_000_000_001L))

        assertThat(roundTrip(planned)?.outcome).isEqualTo(OccurrenceOutcome.Planned)
        assertThat(roundTrip(done)?.outcome).isEqualTo(OccurrenceOutcome.Done(1_755_000_000_000L))
        assertThat(roundTrip(skipped)?.outcome)
            .isEqualTo(OccurrenceOutcome.Skipped(1_755_000_000_001L))
        // A re-planned occurrence loses its stamp in the same write, so a stamp can never
        // outlive the outcome it belonged to.
        assertThat(planned.toDto().outcomeAt).isNull()
    }

    @Test
    fun `a stamped outcome with no stamp reads as planned rather than as 1970`() {
        val halfFact = OccurrenceDto(
            id = "o1",
            taskId = "t1",
            rung = "ALL_DAY",
            start = "2026-08-17",
            outcome = "DONE",
            outcomeAt = null,
        )

        // Half a fact is not a fact -- `CompletionFact`'s rule at the wire boundary. A review
        // counting when things happened would rather see nothing than see 1 January 1970.
        assertThat(halfFact.toDomain()?.outcome).isEqualTo(OccurrenceOutcome.Planned)
    }

    @Test
    fun `a document with no readable when is dropped rather than defaulted`() {
        // Loses one instance of a series rather than the screen -- the caller's `mapNotNull`.
        assertThat(OccurrenceDto(id = "o1", taskId = "t1", rung = "BLOCK", start = null).toDomain())
            .isNull()
        assertThat(OccurrenceDto(id = "o1", taskId = "t1", rung = "SPACE", start = "2026-08-17").toDomain())
            .isNull()
        // Half a window: a block with no end.
        assertThat(
            OccurrenceDto(id = "o1", taskId = "t1", rung = "BLOCK", start = "2026-08-17T09:00").toDomain(),
        ).isNull()
    }

    @Test
    fun `a blank googleEventId reads as absent, so a cleared link cannot look like a link`() {
        // §2.7: a disappearance "keeps its date, clears its googleEventId". A stored empty
        // string would be a link to an event that does not exist.
        val blank = OccurrenceDto(
            id = "o1", taskId = "t1", rung = "ALL_DAY", start = "2026-08-17", googleEventId = "",
        )

        assertThat(blank.toDomain()?.googleEventId).isNull()
    }
}
