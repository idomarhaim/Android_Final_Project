package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * ***"This occurrence, or all future ones?"*** — §2.1's question, and the reason the whole
 * shape exists ([`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63)).
 *
 * §2.1 states the failure both halves have alone:
 *
 * > *"a field-only model always answers **just this one**; a rule-only model always answers
 * > **all of them**."*
 *
 * So the assertions below are pairs: the same edit, on the same instance, under each scope, and
 * they must come out different. A change that makes them agree has deleted the feature while
 * leaving the types in place, which is exactly the failure a type check cannot see.
 *
 * The subtle case is **`THIS_AND_FUTURE` with a move**. The rule generates from the task's own
 * occurrence, so moving that anchor moves the *past* as well — and §2.3 forbids it: *"a missed
 * occurrence is never edited — it is history."* The past is therefore written down before the
 * anchor moves, and the spent half of an `AfterCount` bound goes with it.
 */
class ScheduleEditsTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val now = 1_755_000_000_000L

    private fun weekly(
        end: RepeatEnd = RepeatEnd.Never,
        stored: List<ScheduledOccurrence> = emptyList(),
    ) = TaskSchedule(
        task = Task(
            id = "task_1",
            title = "Water the flowers",
            occurrence = AllDay(monday),
            repeatRule = RepeatRule(unit = RepeatUnit.WEEK, interval = 2, end = end),
        ),
        stored = stored,
    )

    private fun writes(plan: SchedulePlan): SchedulePlan.Writes =
        plan as? SchedulePlan.Writes ?: error("expected Writes, got $plan")

    // ── THIS_OCCURRENCE — one document, the rule untouched ───────────────────────────────

    @Test
    fun `moving this occurrence writes one document and leaves the rule alone`() {
        val third = monday.plusWeeks(4)

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(),
                seriesDate = third,
                edit = ScheduleEdit.MoveTo(AllDay(third.plusDays(2))),
                scope = EditScope.THIS_OCCURRENCE,
                nowEpochMillis = now,
            ),
        )

        assertThat(plan.task.repeatRule).isEqualTo(weekly().task.repeatRule)
        assertThat(plan.task.occurrence).isEqualTo(AllDay(monday))
        assertThat(plan.deletes).isEmpty()
        assertThat(plan.upserts).hasSize(1)
        // The identity stays the date the RULE produced, so the next expansion recognises this
        // as still being that instance rather than generating it again beside the moved one.
        assertThat(plan.upserts.single().seriesDate).isEqualTo(third)
        assertThat(plan.upserts.single().occurrence).isEqualTo(AllDay(third.plusDays(2)))
    }

    @Test
    fun `moving this occurrence keeps what was already on the document`() {
        val existing = ScheduledOccurrence(
            id = "occ_1",
            taskId = "task_1",
            occurrence = AllDay(monday.plusWeeks(2)),
            seriesDate = monday.plusWeeks(2),
            outcome = OccurrenceOutcome.Done(now - 5),
            googleEventId = "gcal_abc",
        )

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(stored = listOf(existing)),
                seriesDate = monday.plusWeeks(2),
                edit = ScheduleEdit.MoveTo(AllDay(monday.plusWeeks(2).plusDays(1))),
                scope = EditScope.THIS_OCCURRENCE,
                nowEpochMillis = now,
            ),
        )

        val written = plan.upserts.single()
        assertThat(written.id).isEqualTo("occ_1")
        assertThat(written.googleEventId).isEqualTo("gcal_abc")
        assertThat(written.outcome).isEqualTo(OccurrenceOutcome.Done(now - 5))
    }

    @Test
    fun `skipping this occurrence records a skip and nothing else`() {
        val second = monday.plusWeeks(2)

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(),
                seriesDate = second,
                edit = ScheduleEdit.Skip,
                scope = EditScope.THIS_OCCURRENCE,
                nowEpochMillis = now,
            ),
        )

        assertThat(plan.task.repeatRule).isNotNull()
        assertThat(plan.upserts.single().outcome).isEqualTo(OccurrenceOutcome.Skipped(now))
        assertThat(plan.upserts.single().seriesDate).isEqualTo(second)
        // The rest of the series is untouched: the instance after it is still generated.
        val after = TaskSchedule(plan.task, plan.upserts)
            .occurrencesIn(second.plusDays(1), second.plusWeeks(3), java.time.ZoneId.of("UTC"))
        assertThat(after.map { it.occurrence.startDate }).contains(monday.plusWeeks(4))
    }

    // ── THIS_AND_FUTURE — the rule moves ─────────────────────────────────────────────────

    @Test
    fun `skipping this and all future ends the series the day before`() {
        val third = monday.plusWeeks(4)

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(),
                seriesDate = third,
                edit = ScheduleEdit.Skip,
                scope = EditScope.THIS_AND_FUTURE,
                nowEpochMillis = now,
            ),
        )

        assertThat(plan.task.repeatRule?.end).isEqualTo(RepeatEnd.OnDate(third.minusDays(1)))
        // Two instances survive -- the ones before the skip -- and nothing after it is generated.
        val remaining = TaskSchedule(plan.task).occurrencesIn(
            monday, monday.plusYears(1), java.time.ZoneId.of("UTC"),
        )
        assertThat(remaining.map { it.occurrence.startDate })
            .containsExactly(monday, monday.plusWeeks(2)).inOrder()
    }

    @Test
    fun `the same skip under the other scope keeps every other instance`() {
        // §2.1's sentence, executable: the two answers must not agree.
        val third = monday.plusWeeks(4)
        val one = writes(
            ScheduleEdits.apply(weekly(), third, ScheduleEdit.Skip, EditScope.THIS_OCCURRENCE, now),
        )
        val future = writes(
            ScheduleEdits.apply(weekly(), third, ScheduleEdit.Skip, EditScope.THIS_AND_FUTURE, now),
        )

        assertThat(one.task.repeatRule?.end).isEqualTo(RepeatEnd.Never)
        assertThat(future.task.repeatRule?.end).isEqualTo(RepeatEnd.OnDate(third.minusDays(1)))
    }

    @Test
    fun `skipping this and all future from the first instance takes the when with it`() {
        // A rule ending the day before its own start is legal, inert, and reads as a bug.
        val plan = writes(
            ScheduleEdits.apply(weekly(), monday, ScheduleEdit.Skip, EditScope.THIS_AND_FUTURE, now),
        )

        assertThat(plan.task.repeatRule).isNull()
        assertThat(plan.task.occurrence).isNull()
    }

    @Test
    fun `skipping this and all future deletes the stored instances from that date on`() {
        val kept = ScheduledOccurrence(
            id = "occ_past", taskId = "task_1",
            occurrence = AllDay(monday), seriesDate = monday,
            outcome = OccurrenceOutcome.Done(now - 10),
        )
        val doomed = ScheduledOccurrence(
            id = "occ_future", taskId = "task_1",
            occurrence = AllDay(monday.plusWeeks(6)), seriesDate = monday.plusWeeks(6),
        )

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(stored = listOf(kept, doomed)),
                seriesDate = monday.plusWeeks(4),
                edit = ScheduleEdit.Skip,
                scope = EditScope.THIS_AND_FUTURE,
                nowEpochMillis = now,
            ),
        )

        assertThat(plan.deletes).containsExactly("occ_future")
    }

    // ── THIS_AND_FUTURE with a move — the past is written down first ─────────────────────

    @Test
    fun `moving this and all future writes the past down before it moves the anchor`() {
        // §2.3: "a missed occurrence is never edited -- it is history." The rule generates from
        // the task's occurrence, so moving that would move the two instances that already
        // happened. They become documents on the days they actually fell on.
        val third = monday.plusWeeks(4)

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(),
                seriesDate = third,
                edit = ScheduleEdit.MoveTo(AllDay(third.plusDays(3))),
                scope = EditScope.THIS_AND_FUTURE,
                nowEpochMillis = now,
            ),
        )

        assertThat(plan.task.occurrence).isEqualTo(AllDay(third.plusDays(3)))
        assertThat(plan.upserts.map { it.seriesDate })
            .containsExactly(monday, monday.plusWeeks(2)).inOrder()
        assertThat(plan.upserts.map { it.occurrence })
            .containsExactly(AllDay(monday), AllDay(monday.plusWeeks(2))).inOrder()
    }

    @Test
    fun `a past instance the user already touched is not materialised twice`() {
        val touched = ScheduledOccurrence(
            id = "occ_done", taskId = "task_1",
            occurrence = AllDay(monday), seriesDate = monday,
            outcome = OccurrenceOutcome.Done(now - 10),
        )

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(stored = listOf(touched)),
                seriesDate = monday.plusWeeks(4),
                edit = ScheduleEdit.MoveTo(AllDay(monday.plusWeeks(4).plusDays(1))),
                scope = EditScope.THIS_AND_FUTURE,
                nowEpochMillis = now,
            ),
        )

        // Only the untouched second instance is written; the completed first one is left as it
        // is, or the write would carry a fresh Planned outcome over a Done one.
        assertThat(plan.upserts.map { it.seriesDate }).containsExactly(monday.plusWeeks(2))
        assertThat(plan.deletes).isEmpty()
    }

    @Test
    fun `after the move the series continues from the new when`() {
        val third = monday.plusWeeks(4)
        val plan = writes(
            ScheduleEdits.apply(
                weekly(), third, ScheduleEdit.MoveTo(AllDay(third.plusDays(3))),
                EditScope.THIS_AND_FUTURE, now,
            ),
        )

        val after = TaskSchedule(plan.task, plan.upserts).occurrencesIn(
            monday, monday.plusWeeks(9), java.time.ZoneId.of("UTC"),
        )

        assertThat(after.map { it.occurrence.startDate }).containsExactly(
            monday,                       // materialised past
            monday.plusWeeks(2),          // materialised past
            third.plusDays(3),            // the new anchor
            third.plusDays(3).plusWeeks(2),
            third.plusDays(3).plusWeeks(4),
        ).inOrder()
    }

    @Test
    fun `an AfterCount bound is spent by the instances the move wrote down`() {
        // Without this, "every other week, ten times", moved after three, would run for ten
        // more -- a series that silently grants itself a second life.
        val third = monday.plusWeeks(4)

        val plan = writes(
            ScheduleEdits.apply(
                schedule = weekly(end = RepeatEnd.AfterCount(10)),
                seriesDate = third,
                edit = ScheduleEdit.MoveTo(AllDay(third.plusDays(1))),
                scope = EditScope.THIS_AND_FUTURE,
                nowEpochMillis = now,
            ),
        )

        assertThat(plan.upserts).hasSize(2)
        assertThat(plan.task.repeatRule?.end).isEqualTo(RepeatEnd.AfterCount(8))
    }

    @Test
    fun `a task with no rule moves as one occurrence whichever scope is chosen`() {
        // Both answers are the same answer here, and saying so is better than pretending the
        // question was meaningful.
        val plain = TaskSchedule(
            task = Task(id = "task_1", title = "Renew passport", occurrence = AllDay(monday)),
        )

        val one = writes(
            ScheduleEdits.apply(
                plain, monday, ScheduleEdit.MoveTo(AllDay(monday.plusDays(4))),
                EditScope.THIS_OCCURRENCE, now,
            ),
        )
        val future = writes(
            ScheduleEdits.apply(
                plain, monday, ScheduleEdit.MoveTo(AllDay(monday.plusDays(4))),
                EditScope.THIS_AND_FUTURE, now,
            ),
        )

        assertThat(future.task.occurrence).isEqualTo(AllDay(monday.plusDays(4)))
        assertThat(one.upserts.single().occurrence).isEqualTo(AllDay(monday.plusDays(4)))
    }

    // ── Legal, but never silent (§0.4) ───────────────────────────────────────────────────

    @Test
    fun `a move that would exceed one batch is refused, with both numbers`() {
        // A daily task moved after two years: the past does not fit in one `WriteBatch`, and
        // the alternatives were a half-applicable plan or silently dropped history.
        val daily = TaskSchedule(
            task = Task(
                id = "task_1",
                title = "Take the pills",
                occurrence = AllDay(monday),
                repeatRule = RepeatRule(unit = RepeatUnit.DAY),
            ),
        )

        val plan = ScheduleEdits.apply(
            schedule = daily,
            seriesDate = monday.plusDays(700),
            edit = ScheduleEdit.MoveTo(AllDay(monday.plusDays(701))),
            scope = EditScope.THIS_AND_FUTURE,
            nowEpochMillis = now,
        )

        assertThat(plan).isInstanceOf(SchedulePlan.TooLarge::class.java)
        val refused = plan as SchedulePlan.TooLarge
        assertThat(refused.limit).isEqualTo(ScheduleEdits.MAX_BATCH_WRITES)
        assertThat(refused.required).isEqualTo(701)
    }

    @Test
    fun `a move just inside the batch limit is allowed`() {
        val daily = TaskSchedule(
            task = Task(
                id = "task_1",
                title = "Take the pills",
                occurrence = AllDay(monday),
                repeatRule = RepeatRule(unit = RepeatUnit.DAY),
            ),
        )

        val plan = writes(
            ScheduleEdits.apply(
                daily, monday.plusDays(499), ScheduleEdit.MoveTo(AllDay(monday.plusDays(500))),
                EditScope.THIS_AND_FUTURE, now,
            ),
        )

        assertThat(plan.writeCount).isEqualTo(ScheduleEdits.MAX_BATCH_WRITES)
    }
}
