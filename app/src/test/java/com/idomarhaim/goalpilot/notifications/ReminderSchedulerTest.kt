package com.idomarhaim.goalpilot.notifications

import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

/**
 * [ReminderScheduler.nextOccurrence] — the delay arithmetic behind §2.5's nightly prompt.
 *
 * The one that matters is `strictly after`. [PlanTomorrowWorker] re-arms the chain from the
 * clock it woke at, so the very first caller of this function every night passes a `now` equal
 * to the target minute. A non-strict comparison returns that same instant, WorkManager enqueues
 * with a zero delay, the worker runs again immediately, and the app posts the plan-tomorrow
 * notification in a loop for a day. It is the kind of defect that cannot happen on a developer's
 * machine and cannot be missed on a user's phone.
 */
class ReminderSchedulerTest {

    private val tenPm = 22 * 60

    @Test
    fun `before the time today, it fires today`() {
        val next = ReminderScheduler.nextOccurrence(
            now = LocalDateTime.of(2026, 8, 20, 9, 30),
            minuteOfDay = tenPm,
        )
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 8, 20, 22, 0))
    }

    @Test
    fun `after the time today, it fires tomorrow`() {
        val next = ReminderScheduler.nextOccurrence(
            now = LocalDateTime.of(2026, 8, 20, 23, 30),
            minuteOfDay = tenPm,
        )
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 8, 21, 22, 0))
    }

    @Test
    fun `AT the time it fires tomorrow, never in zero milliseconds`() {
        // The re-arm case: a worker that woke exactly on time asks for its own successor.
        val next = ReminderScheduler.nextOccurrence(
            now = LocalDateTime.of(2026, 8, 20, 22, 0),
            minuteOfDay = tenPm,
        )
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 8, 21, 22, 0))
        assertThat(next).isGreaterThan(LocalDateTime.of(2026, 8, 20, 22, 0))
    }

    @Test
    fun `one second past the time still fires tomorrow`() {
        val next = ReminderScheduler.nextOccurrence(
            now = LocalDateTime.of(2026, 8, 20, 22, 0, 1),
            minuteOfDay = tenPm,
        )
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 8, 21, 22, 0))
    }

    @Test
    fun `midnight and the minute before it both resolve`() {
        assertThat(
            ReminderScheduler.nextOccurrence(LocalDateTime.of(2026, 8, 20, 23, 30), 0),
        ).isEqualTo(LocalDateTime.of(2026, 8, 21, 0, 0))

        assertThat(
            ReminderScheduler.nextOccurrence(LocalDateTime.of(2026, 8, 20, 12, 0), 23 * 60 + 59),
        ).isEqualTo(LocalDateTime.of(2026, 8, 20, 23, 59))
    }

    @Test
    fun `a stored minute outside the day is wrapped rather than throwing`() {
        // The value comes from a preference read; DaySchedule's own doc-comment takes the same
        // line -- a corrupt value should land on a wrong-but-usable time, not an exception.
        val next = ReminderScheduler.nextOccurrence(
            now = LocalDateTime.of(2026, 8, 20, 9, 0),
            minuteOfDay = 24 * 60 + 10 * 60,
        )
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0))
    }
}
