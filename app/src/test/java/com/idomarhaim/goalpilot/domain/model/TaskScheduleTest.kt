package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The aggregate that answers §2.1's questions — **the rule and the documents together**
 * ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * Three things are asserted here and each is a claim the spec makes:
 *
 * 1. **A task with no rule and no documents reads exactly as `#56` left it** — §7.1's migration
 *    posture is *"additive with a readable half-way state"*, and this is what makes day one
 *    identical for every task in the database.
 * 2. **A stored document overrides the instance the rule generated for it**, including one
 *    moved into or out of the range being asked about. That is the sentence §2.1 says a
 *    rule-only model cannot say.
 * 3. **`isDone` splits three ways** (§7.1), and the third way is *no answer*.
 */
class TaskScheduleTest {

    private val zone: ZoneId = ZoneId.of("Asia/Jerusalem")
    private val monday: LocalDate = LocalDate.of(2026, 8, 17)

    private fun taskWith(
        occurrence: Occurrence? = AllDay(monday),
        rule: RepeatRule? = null,
        pausedUntil: Long? = null,
        completion: CompletionFact? = null,
    ) = Task(
        id = "task_1",
        title = "Water the flowers",
        occurrence = occurrence,
        repeatRule = rule,
        pausedUntil = pausedUntil,
        completion = completion,
    )

    private fun TaskSchedule.week(from: LocalDate = monday, days: Long = 6) =
        occurrencesIn(from, from.plusDays(days), zone)

    // ── 1 · The task that has not repeated: `#56`'s shape, unchanged ──────────────────────

    @Test
    fun `a task with no rule and no documents yields exactly its own occurrence`() {
        val schedule = TaskSchedule(task = taskWith())

        val week = schedule.week()

        assertThat(week).hasSize(1)
        assertThat(week.single().occurrence).isEqualTo(AllDay(monday))
        assertThat(week.single().seriesDate).isNull()
    }

    @Test
    fun `a task with no when at all yields nothing`() {
        assertThat(TaskSchedule(task = taskWith(occurrence = null)).week()).isEmpty()
    }

    @Test
    fun `a rule with no anchor generates nothing rather than guessing a start`() {
        val schedule = TaskSchedule(
            task = taskWith(occurrence = null, rule = RepeatRule(RepeatUnit.DAY)),
        )

        assertThat(schedule.week()).isEmpty()
    }

    // ── 2 · Rule plus documents ───────────────────────────────────────────────────────────

    @Test
    fun `a rule generates every instance in the range and stores none of them`() {
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.DAY, interval = 2)),
        )

        val week = schedule.week()

        assertThat(week.map { it.occurrence.startDate }).containsExactly(
            monday, monday.plusDays(2), monday.plusDays(4), monday.plusDays(6),
        ).inOrder()
        assertThat(week.map { it.seriesDate }).doesNotContain(null)
        assertThat(week.all { it.id.isBlank() }).isTrue()
    }

    @Test
    fun `a stored document replaces the instance the rule generated for its series date`() {
        val moved = ScheduledOccurrence(
            id = "occ_1",
            taskId = "task_1",
            occurrence = AllDay(monday.plusDays(1)),
            seriesDate = monday,
        )
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.WEEK)),
            stored = listOf(moved),
        )

        val week = schedule.week()

        // One instance this week, and it is on Tuesday -- not Monday, and not both.
        assertThat(week).hasSize(1)
        assertThat(week.single().id).isEqualTo("occ_1")
        assertThat(week.single().occurrence.startDate).isEqualTo(monday.plusDays(1))
        assertThat(week.single().seriesDate).isEqualTo(monday)
    }

    @Test
    fun `an instance moved out of the range leaves it, and one moved in appears`() {
        // The sharp end of `seriesDate`: identity is the date the RULE produced, so an instance
        // can be asked about in one week and render in another.
        val pushedOut = ScheduledOccurrence(
            id = "occ_out",
            taskId = "task_1",
            occurrence = AllDay(monday.plusDays(10)),
            seriesDate = monday,
        )
        val pulledIn = ScheduledOccurrence(
            id = "occ_in",
            taskId = "task_1",
            occurrence = AllDay(monday.plusDays(2)),
            seriesDate = monday.plusWeeks(3),
        )
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.WEEK)),
            stored = listOf(pushedOut, pulledIn),
        )

        val week = schedule.week()

        assertThat(week.map { it.id }).containsExactly("occ_in")
    }

    @Test
    fun `a one-off document belongs to no series and still shows up`() {
        val extra = ScheduledOccurrence(
            id = "occ_extra",
            taskId = "task_1",
            occurrence = AllDay(monday.plusDays(3)),
            seriesDate = null,
        )
        val schedule = TaskSchedule(task = taskWith(), stored = listOf(extra))

        // No rule: the task's own anchor is superseded by the documents, per
        // `occurrencesIn`'s fourth source -- so this is the one occurrence in the week.
        assertThat(schedule.week().map { it.id }).containsExactly("occ_extra")
    }

    @Test
    fun `pausedUntil suppresses generated instances and never stored ones`() {
        val done = ScheduledOccurrence(
            id = "occ_done",
            taskId = "task_1",
            occurrence = AllDay(monday),
            seriesDate = monday,
            outcome = OccurrenceOutcome.Done(1L),
        )
        // Paused through Wednesday: Monday is stored (and survives), Tuesday and Wednesday are
        // generated (and do not), Thursday onwards resume.
        val pauseEnd = monday.plusDays(3).atStartOfDay(zone).toInstant().toEpochMilli()
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.DAY), pausedUntil = pauseEnd),
            stored = listOf(done),
        )

        val week = schedule.week()

        assertThat(week.map { it.occurrence.startDate }).containsExactly(
            monday, monday.plusDays(3), monday.plusDays(4), monday.plusDays(5), monday.plusDays(6),
        ).inOrder()
        assertThat(week.first().outcome).isEqualTo(OccurrenceOutcome.Done(1L))
    }

    @Test
    fun `the results are ordered by when the window opens, not by the rule`() {
        val late = ScheduledOccurrence(
            id = "occ_late",
            taskId = "task_1",
            occurrence = Deadline(monday.atTime(23, 0)),
            seriesDate = monday.plusDays(4),
        )
        val schedule = TaskSchedule(
            task = taskWith(occurrence = Deadline(monday.atTime(8, 0)), rule = RepeatRule(RepeatUnit.DAY)),
            stored = listOf(late),
        )

        val opens = schedule.week().map { it.occurrence.opensAt }

        assertThat(opens).isInOrder()
    }

    // ── 3 · §7.1's three-way `isDone` ────────────────────────────────────────────────────

    @Test
    fun `with no occurrences the answer is stored, and it is the completion fact`() {
        val open = TaskSchedule(task = taskWith()).doneness
        val ticked = TaskSchedule(
            task = taskWith(completion = CompletionFact(completedAtEpochMillis = 1L)),
        ).doneness

        assertThat(open).isEqualTo(Doneness.Stored(false))
        assertThat(ticked).isEqualTo(Doneness.Stored(true))
        assertThat(ticked.isDone).isTrue()
    }

    @Test
    fun `with occurrences the answer is derived from them, not from the task`() {
        // The task's own stored completion says nothing here, which is exactly the split: a
        // task with windows is done when its windows are.
        val schedule = TaskSchedule(
            task = taskWith(completion = CompletionFact(completedAtEpochMillis = 1L)),
            stored = listOf(
                ScheduledOccurrence(id = "a", occurrence = AllDay(monday), outcome = OccurrenceOutcome.Done(1L)),
                ScheduledOccurrence(id = "b", occurrence = AllDay(monday.plusDays(1))),
            ),
        )

        assertThat(schedule.doneness).isEqualTo(Doneness.Derived(completed = 1, total = 2))
        assertThat(schedule.doneness.isDone).isFalse()
    }

    @Test
    fun `a bounded series is done when every window it has is`() {
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.DAY, end = RepeatEnd.AfterCount(2))),
            stored = listOf(
                ScheduledOccurrence(id = "a", occurrence = AllDay(monday), seriesDate = monday, outcome = OccurrenceOutcome.Done(1L)),
                ScheduledOccurrence(id = "b", occurrence = AllDay(monday.plusDays(1)), seriesDate = monday.plusDays(1), outcome = OccurrenceOutcome.Done(2L)),
            ),
        )

        assertThat(schedule.doneness).isEqualTo(Doneness.Derived(completed = 2, total = 2))
        assertThat(schedule.doneness.isDone).isTrue()
    }

    @Test
    fun `a skipped window is not outstanding, and a task of nothing but skips is not done`() {
        val allSkipped = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.DAY, end = RepeatEnd.AfterCount(2))),
            stored = listOf(
                ScheduledOccurrence(id = "a", occurrence = AllDay(monday), seriesDate = monday, outcome = OccurrenceOutcome.Skipped(1L)),
                ScheduledOccurrence(id = "b", occurrence = AllDay(monday.plusDays(1)), seriesDate = monday.plusDays(1), outcome = OccurrenceOutcome.Skipped(2L)),
            ),
        )

        assertThat(allSkipped.doneness).isEqualTo(Doneness.Derived(completed = 0, total = 0))
        // Nothing was completed. Reporting `done` here would be inventing an achievement.
        assertThat(allSkipped.doneness.isDone).isFalse()
    }

    @Test
    fun `one skip beside one completion leaves the task done`() {
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.DAY, end = RepeatEnd.AfterCount(2))),
            stored = listOf(
                ScheduledOccurrence(id = "a", occurrence = AllDay(monday), seriesDate = monday, outcome = OccurrenceOutcome.Done(1L)),
                ScheduledOccurrence(id = "b", occurrence = AllDay(monday.plusDays(1)), seriesDate = monday.plusDays(1), outcome = OccurrenceOutcome.Skipped(2L)),
            ),
        )

        assertThat(schedule.doneness).isEqualTo(Doneness.Derived(completed = 1, total = 1))
        assertThat(schedule.doneness.isDone).isTrue()
    }

    @Test
    fun `an unbounded series has no answer at all, even with completed windows`() {
        // §7.1's third way. The stored document would derive a confident `1 of 1`, which is why
        // `doneness` checks the rule's bound FIRST -- an infinite series cannot be finished, so
        // no boolean about it is true.
        val schedule = TaskSchedule(
            task = taskWith(rule = RepeatRule(RepeatUnit.WEEK)),
            stored = listOf(
                ScheduledOccurrence(id = "a", occurrence = AllDay(monday), seriesDate = monday, outcome = OccurrenceOutcome.Done(1L)),
            ),
        )

        assertThat(schedule.doneness).isEqualTo(Doneness.Unanswerable)
        assertThat(schedule.doneness.isDone).isNull()
    }

    @Test
    fun `a bounded rule is recurring and still has an answer`() {
        // The reading `Doneness.Unanswerable` records: §7.1 says "absent on a recurring task",
        // and this constant fires on UNBOUNDED recurrence alone, because a series that ends has
        // a complete set of windows and therefore an answer.
        val schedule = TaskSchedule(
            task = taskWith(
                rule = RepeatRule(
                    unit = RepeatUnit.WEEK,
                    weekdays = setOf(DayOfWeek.MONDAY),
                    end = RepeatEnd.AfterCount(3),
                ),
            ),
        )

        assertThat(schedule.doneness).isInstanceOf(Doneness.Derived::class.java)
        assertThat((schedule.doneness as Doneness.Derived).total).isEqualTo(3)
        assertThat(schedule.doneness.isDone).isFalse()
    }
}
