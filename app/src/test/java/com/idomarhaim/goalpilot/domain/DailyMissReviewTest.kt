package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceState
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.DailyMissReview
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * §2.5's **daily review**: *"Misses meet Ido once, in a daily review on app open — never as a
 * push saying he failed"* (`#56`).
 *
 * Each clause is a test below. The one that is easiest to get wrong is **once** — a review that
 * re-shows everything every morning is indistinguishable from one that works, until the day
 * somebody scrolls it.
 */
class DailyMissReviewTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val tuesdayMorning: LocalDateTime = monday.plusDays(1).atTime(9, 0)

    private var nextId = 0

    private fun task(
        occurrence: Occurrence?,
        title: String = "Task ${nextId++}",
        done: Boolean = false,
    ) = Task(
        id = title,
        title = title,
        occurrence = occurrence,
        completion = if (done) CompletionFact(completedAtEpochMillis = 1L, minutes = 30) else null,
    )

    // ── What is a miss, and what is not ────────────────────────────────────────────────────

    @Test
    fun `all four of the table's miss meanings reach the review`() {
        val tasks = listOf(
            task(AllDay(monday), "day"),
            task(Deadline(monday.atTime(18, 0)), "deadline"),
            task(Block(monday.atTime(9, 0), monday.atTime(10, 0)), "block"),
            task(Span(monday, monday), "span"),
        )

        val review = DailyMissReview.of(tasks, tuesdayMorning)

        assertThat(review.map { it.state }).containsExactly(
            OccurrenceState.DAY_PASSED,
            OccurrenceState.OVERDUE,
            OccurrenceState.MISSED,
            OccurrenceState.WINDOW_CLOSED,
        )
    }

    @Test
    fun `an unconfirmed block that expired is not in the review at all`() {
        val tasks = listOf(
            task(
                Block(monday.atTime(9, 0), monday.atTime(10, 0), BlockPlacement.PROVISIONAL),
                "provisional",
            ),
        )

        // §2.3: it "counts for nothing, silently". Showing it is exactly the over-eager agent
        // manufacturing a failure the user never agreed to.
        assertThat(DailyMissReview.of(tasks, tuesdayMorning)).isEmpty()
    }

    @Test
    fun `nothing scheduled or underway is a miss`() {
        val tasks = listOf(
            task(AllDay(monday.plusDays(5)), "later"),
            task(AllDay(monday.plusDays(1)), "today"),
            task(Deadline(monday.plusDays(2).atTime(12, 0)), "ahead"),
        )

        assertThat(DailyMissReview.of(tasks, tuesdayMorning)).isEmpty()
    }

    @Test
    fun `a task completed after its deadline passed is not a miss`() {
        val tasks = listOf(task(Deadline(monday.atTime(18, 0)), "late but done", done = true))

        // It was DONE, not missed. The review and §2.5's fire-time re-check ask the same
        // question of the same field, which is what stops the two surfaces disagreeing.
        assertThat(DailyMissReview.of(tasks, tuesdayMorning)).isEmpty()
    }

    @Test
    fun `a task with no occurrence is never in the review`() {
        assertThat(DailyMissReview.of(listOf(task(null, "unscheduled")), tuesdayMorning)).isEmpty()
    }

    // ── "once" ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a miss already met in a previous review does not come back`() {
        val tasks = listOf(task(AllDay(monday), "day"))
        val firstReview = monday.plusDays(1).atTime(8, 0)

        assertThat(DailyMissReview.of(tasks, firstReview)).hasSize(1)
        // Next morning, having met it yesterday.
        assertThat(DailyMissReview.of(tasks, monday.plusDays(2).atTime(8, 0), since = firstReview))
            .isEmpty()
    }

    @Test
    fun `an overdue deadline comes back, because it is the one state that keeps reminding`() {
        val tasks = listOf(task(Deadline(monday.atTime(18, 0)), "still owed"))
        val firstReview = monday.plusDays(1).atTime(8, 0)

        assertThat(DailyMissReview.of(tasks, firstReview)).hasSize(1)

        // §2.3: a passed deadline is "late, and still owed", so it is still true tomorrow. A
        // missed block is not, which is what makes the OVERDUE/MISSED split earn its keep.
        val second = DailyMissReview.of(tasks, monday.plusDays(2).atTime(8, 0), since = firstReview)
        assertThat(second).hasSize(1)
        assertThat(second.single().stillOwed).isTrue()
    }

    @Test
    fun `a miss that closed after the last review is new, even on the same day`() {
        val lunchtimeReview = monday.atTime(12, 0)
        // A block that lapsed at 15:00, after this morning's review.
        val tasks = listOf(task(Block(monday.atTime(14, 0), monday.atTime(15, 0)), "afternoon"))

        assertThat(
            DailyMissReview.of(tasks, monday.atTime(16, 0), since = lunchtimeReview),
        ).hasSize(1)
    }

    @Test
    fun `the first ever review shows everything, because nothing has been met`() {
        val tasks = listOf(
            task(AllDay(monday.minusDays(30)), "ancient"),
            task(AllDay(monday), "recent"),
        )

        assertThat(DailyMissReview.of(tasks, tuesdayMorning, since = null)).hasSize(2)
    }

    // ── Order ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `the most recently closed miss is first`() {
        val tasks = listOf(
            task(AllDay(monday.minusDays(10)), "old"),
            task(AllDay(monday), "newest"),
            task(AllDay(monday.minusDays(3)), "middle"),
        )

        // A review that opens on something that lapsed a fortnight ago buries the thing the
        // user still has a decision to make about.
        assertThat(DailyMissReview.of(tasks, tuesdayMorning).map { it.task.title })
            .containsExactly("newest", "middle", "old")
            .inOrder()
    }

    // ── isDue ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a review is due once per calendar day, not once per twenty-four hours`() {
        assertThat(DailyMissReview.isDue(lastShownOn = null, today = monday)).isTrue()
        assertThat(DailyMissReview.isDue(lastShownOn = monday, today = monday)).isFalse()
        // 23:50 then 00:10 is two days and two reviews; twice in an afternoon is one.
        assertThat(DailyMissReview.isDue(lastShownOn = monday, today = monday.plusDays(1))).isTrue()
    }
}
