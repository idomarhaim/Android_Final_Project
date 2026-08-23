package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.RepeatRule
import com.idomarhaim.goalpilot.domain.model.RepeatUnit
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.WakingHours
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * §4.3's surface, assembled — the columns, the three lanes, and what each of the four
 * [EntryKind]s contributes ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * The expansion of §2.1's rule-plus-documents is `#63`'s and is tested there; what is tested here
 * is everything this ticket added on top of it — the lane split, the goal deadlines, the challenge
 * windows clipped to the visible range, the load per column, and carry-forward landing on exactly
 * one day.
 */
class CalendarBuilderTest {

    private val zone: ZoneId = ZoneId.of("Asia/Jerusalem")
    private val monday: LocalDate = LocalDate.of(2026, 8, 17)
    private val threeDays: List<LocalDate> = CalendarBuilder.daysFor(monday, CalendarZoom.THREE_DAYS)
    private val noonOnMonday: LocalDateTime = monday.atTime(12, 0)

    private val health = LifeArea(id = "la-health", name = "Health", colorHex = "#22C55E")
    private val fitness = Goal(id = "g-fit", title = "Run 100 km", lifeAreaIds = listOf(health.id))

    private fun task(id: String, title: String, occurrence: Occurrence, goalId: String? = null) =
        Task(
            id = id,
            title = title,
            occurrence = occurrence,
            goalEdges = goalId?.let { listOf(GoalEdge(goalId = it)) } ?: emptyList(),
        )

    private fun build(
        schedules: List<TaskSchedule> = emptyList(),
        goals: List<Goal> = emptyList(),
        lifeAreas: List<LifeArea> = listOf(health),
        challenges: List<Challenge> = emptyList(),
        external: List<CalendarEntry> = emptyList(),
        range: List<LocalDate> = threeDays,
        today: LocalDate = monday,
        now: LocalDateTime = noonOnMonday,
    ) = CalendarBuilder.build(
        range = range,
        today = today,
        now = now,
        schedules = schedules,
        goals = goals,
        lifeAreas = lifeAreas,
        challenges = challenges,
        external = external,
        waking = WakingHours.DEFAULT,
        zone = zone,
    )

    // ── The zoom decides the columns ─────────────────────────────────────────────────────

    @Test
    fun `each zoom produces its own number of columns, and three days is the default`() {
        assertThat(CalendarZoom.DEFAULT).isEqualTo(CalendarZoom.THREE_DAYS)
        assertThat(CalendarBuilder.daysFor(monday, CalendarZoom.AGENDA)).hasSize(1)
        assertThat(CalendarBuilder.daysFor(monday, CalendarZoom.THREE_DAYS)).hasSize(3)
        assertThat(CalendarBuilder.daysFor(monday, CalendarZoom.WEEK)).hasSize(7)
        assertThat(CalendarBuilder.daysFor(monday, CalendarZoom.WEEK).last()).isEqualTo(monday.plusDays(6))
    }

    @Test
    fun `an empty calendar still produces one column per day`() {
        val days = build()

        assertThat(days).hasSize(3)
        assertThat(days.map { it.date }).containsExactlyElementsIn(threeDays).inOrder()
        assertThat(days.all { it.isEmpty }).isTrue()
        assertThat(days.single { it.isToday }.date).isEqualTo(monday)
    }

    // ── The three lanes ──────────────────────────────────────────────────────────────────

    @Test
    fun `a block lands in the grid and an all-day lands in the untimed strip`() {
        val schedules = listOf(
            TaskSchedule(task("t1", "Gym", Block(monday.atTime(9, 0), monday.atTime(10, 0)))),
            TaskSchedule(task("t2", "Call the bank", AllDay(monday))),
        )

        val today = build(schedules = schedules).first()

        assertThat(today.timed.map { it.title }).containsExactly("Gym")
        assertThat(today.untimed.map { it.title }).containsExactly("Call the bank")
        assertThat(today.allDay).isEmpty()
    }

    @Test
    fun `a task deadline lands in the all-day strip and never in the grid`() {
        // 4.3's rule, asserted through the builder as well as through the lane table -- the two
        // could in principle disagree, and this is where a filter bug would put it back in the grid.
        val schedules = listOf(TaskSchedule(task("t", "Tax return", Deadline(monday.atTime(17, 0)))))

        val today = build(schedules = schedules).first()

        assertThat(today.timed).isEmpty()
        assertThat(today.allDay.map { it.title }).containsExactly("Tax return")
    }

    @Test
    fun `timed entries are ordered by start`() {
        val schedules = listOf(
            TaskSchedule(task("b", "Later", Block(monday.atTime(16, 0), monday.atTime(17, 0)))),
            TaskSchedule(task("a", "Earlier", Block(monday.atTime(8, 0), monday.atTime(9, 0)))),
        )

        val today = build(schedules = schedules).first()

        assertThat(today.timed.map { it.title }).containsExactly("Earlier", "Later").inOrder()
    }

    // ── The chip carries the life area, through the goal ──────────────────────────────────

    @Test
    fun `a task filed under a goal carries that goal's life area as its chip`() {
        val schedules = listOf(
            TaskSchedule(task("t", "Run 5k", Block(monday.atTime(7, 0), monday.atTime(8, 0)), goalId = fitness.id)),
        )

        val entry = build(schedules = schedules, goals = listOf(fitness)).first().timed.single()

        assertThat(entry.lifeArea).isEqualTo(LifeAreaChip(health.id, "Health", "#22C55E"))
    }

    @Test
    fun `a task filed nowhere has no chip rather than a blank one`() {
        // 0.4: legal, but never silent. A chip with an empty name and a default grey would read as
        // a life area called nothing, which is a claim; absence is the honest answer.
        val schedules = listOf(TaskSchedule(task("t", "Errand", Block(monday.atTime(9, 0), monday.atTime(10, 0)))))

        assertThat(build(schedules = schedules).first().timed.single().lifeArea).isNull()
    }

    // ── Goal deadlines ───────────────────────────────────────────────────────────────────

    @Test
    fun `a goal deadline in range is drawn as a banner`() {
        val deadline = monday.plusDays(1).atTime(23, 0)
        val goal = fitness.copy(deadlineEpochMillis = deadline.atZone(zone).toInstant().toEpochMilli())

        val days = build(goals = listOf(goal))

        assertThat(days[0].allDay).isEmpty()
        assertThat(days[1].allDay.map { it.kind }).containsExactly(EntryKind.GOAL_DEADLINE)
        assertThat(days[1].allDay.single().title).isEqualTo("Run 100 km")
    }

    @Test
    fun `a goal deadline outside the range is not drawn`() {
        val far = monday.plusDays(30).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

        val days = build(goals = listOf(fitness.copy(deadlineEpochMillis = far)))

        assertThat(days.flatMap { it.all }).isEmpty()
    }

    @Test
    fun `an unmeasured goal's deadline is drawn however far past its default target it reads`() {
        // #66's seventh site, found by that session sweeping 1.3 and reported on SESSIONS.md
        // rather than edited into this file. `isComplete` is `progressFraction >= 1f`, and for a
        // goal that counts NOTHING that fraction is measured against a targetValue nobody set --
        // so logged entries summing past the default would silently delete the banner, on the one
        // surface whose whole job is to say when things are due.
        val at = monday.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        val unmeasured = Goal(
            id = "g-unmeasured",
            title = "Read more",
            deadlineEpochMillis = at,
            currentValue = 250.0,
            targetValue = 100.0,
        )

        assertThat(unmeasured.isUnmeasured).isTrue()
        assertThat(unmeasured.isComplete).isTrue()
        assertThat(build(goals = listOf(unmeasured)).first().allDay.map { it.title })
            .containsExactly("Read more")
    }

    @Test
    fun `an archived or completed goal's deadline is not drawn`() {
        // 4.4's instinct one screen over: a card with nothing to say hides itself. A deadline you
        // have already met is not a thing that needs a banner.
        val at = monday.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        val archived = fitness.copy(id = "a", deadlineEpochMillis = at, isArchived = true)
        // `met` carries a real measure, or the assertion below would be passing for the reason the
        // case above exists to forbid rather than because the goal is finished.
        val met = fitness.copy(
            id = "b",
            deadlineEpochMillis = at,
            measure = Measure(word = "km", kind = MeasureKind.DISTANCE),
            targetValue = 10.0,
            currentValue = 10.0,
        )

        assertThat(met.isUnmeasured).isFalse()
        assertThat(met.isComplete).isTrue()
        assertThat(build(goals = listOf(archived, met)).flatMap { it.all }).isEmpty()
    }

    // ── Challenge windows ────────────────────────────────────────────────────────────────

    @Test
    fun `a challenge window overlapping the range is drawn and clipped to it`() {
        // The case that matters: a challenge running all month, keyed on its start date alone,
        // would be invisible on every column but the first -- which is the opposite of a window.
        val challenge = Challenge(
            id = "c1",
            title = "August streak",
            startAtEpochMillis = monday.minusDays(10).atStartOfDay(zone).toInstant().toEpochMilli(),
            endAtEpochMillis = monday.plusDays(10).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val days = build(challenges = listOf(challenge))

        assertThat(days.map { it.allDay.size }).containsExactly(1, 1, 1)
        val entry = days.first().allDay.single()
        assertThat(entry.kind).isEqualTo(EntryKind.CHALLENGE_WINDOW)
        assertThat(entry.occurrence.opensAt.toLocalDate()).isEqualTo(monday)
    }

    @Test
    fun `a challenge that ended before the range is not drawn`() {
        val past = Challenge(
            id = "c",
            title = "July",
            startAtEpochMillis = monday.minusDays(40).atStartOfDay(zone).toInstant().toEpochMilli(),
            endAtEpochMillis = monday.minusDays(10).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        assertThat(build(challenges = listOf(past)).flatMap { it.all }).isEmpty()
    }

    // ── #61's slot: built, and empty ─────────────────────────────────────────────────────

    @Test
    fun `no google events appear with no google account, and the slot still works when fed`() {
        // The brief's boundary, both halves in one case: nothing produces an EXTERNAL entry today
        // (#61 has not shipped), and the lane it will land in is not a stub -- feed it one and it
        // draws in the grid beside the app's own blocks.
        assertThat(build().flatMap { it.all }.map { it.kind }).doesNotContain(EntryKind.EXTERNAL)

        val handMade = CalendarEntry(
            key = "gcal:1",
            title = "Dentist",
            kind = EntryKind.EXTERNAL,
            occurrence = Block(monday.atTime(15, 0), monday.atTime(16, 0)),
        )

        val today = build(external = listOf(handMade)).first()

        assertThat(today.timed.map { it.title }).containsExactly("Dentist")
        assertThat(today.timed.single().isTickable).isFalse()
    }

    // ── The load, per column ─────────────────────────────────────────────────────────────

    @Test
    fun `each column carries its own load`() {
        val schedules = listOf(
            TaskSchedule(task("t1", "Long day", Block(monday.atTime(9, 0), monday.atTime(17, 0)))),
            TaskSchedule(task("t2", "Short", Block(monday.plusDays(1).atTime(9, 0), monday.plusDays(1).atTime(10, 0)))),
        )

        val days = build(schedules = schedules)

        assertThat(days[0].load.bookedMinutes).isEqualTo(8 * 60)
        assertThat(days[1].load.bookedMinutes).isEqualTo(60)
        assertThat(days[2].load.bookedMinutes).isEqualTo(0)
        assertThat(days[2].load.freeMinutes).isEqualTo(16 * 60)
    }

    @Test
    fun `a challenge window does not book any of the day it covers`() {
        // A span contributes nothing (2.2) -- and a challenge window is a span, so the clause
        // reaches it through the same rule rather than through a second exception.
        val challenge = Challenge(
            id = "c",
            title = "Streak",
            startAtEpochMillis = monday.atStartOfDay(zone).toInstant().toEpochMilli(),
            endAtEpochMillis = monday.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        assertThat(build(challenges = listOf(challenge)).first().load.bookedMinutes).isEqualTo(0)
    }

    // ── Carry-forward, through the builder ───────────────────────────────────────────────

    @Test
    fun `an overdue deadline from before the range is carried onto today only`() {
        val overdue = TaskSchedule(task("t", "Rent", Deadline(monday.minusDays(3).atTime(9, 0))))

        val days = build(schedules = listOf(overdue))

        // It sits outside the drawn range entirely, so its own column does not exist -- and it must
        // still reach the user, which is the whole argument for carrying it.
        assertThat(days[0].allDay.map { it.title }).containsExactly("Rent")
        assertThat(days[0].allDay.single().carriedForward).isTrue()
        assertThat(days[1].all).isEmpty()
        assertThat(days[2].all).isEmpty()
    }

    @Test
    fun `carried work appears before the day's own banners`() {
        val schedules = listOf(
            TaskSchedule(task("t1", "Rent", Deadline(monday.minusDays(3).atTime(9, 0)))),
            TaskSchedule(task("t2", "Today's paper", Deadline(monday.atTime(18, 0)))),
        )

        val allDay = build(schedules = schedules).first().allDay

        assertThat(allDay.map { it.title }).containsExactly("Rent", "Today's paper").inOrder()
    }

    @Test
    fun `nothing is carried when today is not on screen`() {
        // Scrolled a week ahead, the calendar is answering "when does it fit", not "what needs me".
        // Landing today's overdue work on a future Thursday would be a demand dated wrongly.
        val overdue = TaskSchedule(task("t", "Rent", Deadline(monday.minusDays(3).atTime(9, 0))))
        val nextWeek = CalendarBuilder.daysFor(monday.plusDays(7), CalendarZoom.THREE_DAYS)

        val days = build(schedules = listOf(overdue), range = nextWeek)

        assertThat(days.flatMap { it.all }).isEmpty()
    }

    // ── #63's expansion reaches the calendar unchanged ───────────────────────────────────

    @Test
    fun `a repeating task draws one instance per generated day`() {
        // The rule-plus-documents expansion is TaskSchedule.occurrencesIn's and is tested in #63.
        // What this asserts is that the calendar reads it rather than re-deriving it: 4.3's range
        // is applied in the domain, and this surface is the first caller that needs it to be.
        val daily = task("t", "Meditate", Block(monday.atTime(7, 0), monday.atTime(7, 30)))
            .copy(repeatRule = RepeatRule(unit = RepeatUnit.DAY))

        val days = build(schedules = listOf(TaskSchedule(daily)))

        assertThat(days.map { it.timed.size }).containsExactly(1, 1, 1)
        assertThat(days.map { it.load.bookedMinutes }).containsExactly(30, 30, 30)
        assertThat(days[1].timed.single().occurrence.opensAt).isEqualTo(monday.plusDays(1).atTime(7, 0))
    }

    @Test
    fun `generated instances of one task get distinct keys`() {
        // A generated instance has no document and therefore no id. Keyed on the blank id they
        // would all collapse onto one key, which in a LazyColumn is a recycled row that draws the
        // wrong day's work.
        val daily = task("t", "Meditate", Block(monday.atTime(7, 0), monday.atTime(7, 30)))
            .copy(repeatRule = RepeatRule(unit = RepeatUnit.DAY))

        val keys = build(schedules = listOf(TaskSchedule(daily))).flatMap { it.timed }.map { it.key }

        assertThat(keys).containsNoDuplicates()
    }
}
