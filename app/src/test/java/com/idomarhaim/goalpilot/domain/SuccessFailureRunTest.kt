package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalEdge
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.RepeatEnd
import com.idomarhaim.goalpilot.domain.model.RepeatRule
import com.idomarhaim.goalpilot.domain.model.RepeatUnit
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.usecase.BuildSuccessFailureRunUseCase
import com.idomarhaim.goalpilot.domain.usecase.NextStepOffer
import com.idomarhaim.goalpilot.domain.usecase.SuccessRange
import com.idomarhaim.goalpilot.domain.usecase.WindowOutcome
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * §4.7's counting rules, asserted **one test per sentence** — `C19`
 * ([`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64)).
 *
 * The brief for this ticket says the rules *are* the feature, and it lists them: `MISSED`
 * counts and `OVERDUE` does not, `EXPIRED` counts for nothing, a success counts whole in each
 * area while minutes divide, the window is a filter over history rather than decay of it, and
 * a goal with nothing due lands in **neither** number. Each has its own test below, named after
 * the sentence rather than after the function, so a rule that is quietly dropped fails a test
 * whose name says what was lost.
 *
 * Everything runs on the JVM with an explicit clock: §2.3's derivation is a pure function of an
 * occurrence and an instant, and `BuildSuccessFailureRunUseCase` takes both as arguments for
 * exactly this reason.
 */
class SuccessFailureRunTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    /** A Monday, so a weekly window's arithmetic is readable in the failures. */
    private val today: LocalDate = LocalDate.of(2026, 8, 17)
    private val now: LocalDateTime = today.atTime(12, 0)

    private val health = Goal(id = "g-health", title = "Run 20 km a week")
    private val career = Goal(id = "g-career", title = "Apply to 2 roles a week")

    private fun task(
        id: String,
        goalId: String = health.id,
        occurrence: Occurrence? = null,
        rule: RepeatRule? = null,
        completion: CompletionFact? = null,
        extraGoalId: String? = null,
    ) = Task(
        id = id,
        title = id,
        goalEdges = listOfNotNull(GoalEdge(goalId), extraGoalId?.let { GoalEdge(it) }),
        occurrence = occurrence,
        repeatRule = rule,
        completion = completion,
    )

    private fun run(
        goals: List<Goal> = listOf(health),
        tasks: List<Task> = emptyList(),
        occurrences: List<ScheduledOccurrence> = emptyList(),
        range: SuccessRange = SuccessRange.EIGHT_WEEKS,
        at: LocalDateTime = now,
        on: LocalDate = today,
    ) = BuildSuccessFailureRunUseCase(
        goals = goals,
        tasks = tasks,
        occurrences = occurrences,
        range = range,
        today = on,
        now = at,
        zone = zone,
    )

    /** The window a date falls in, so a test can assert about *that* window and no other. */
    private fun com.idomarhaim.goalpilot.domain.usecase.SuccessFailureRun.windowOn(
        date: LocalDate,
    ) = windows.single { !date.isBefore(it.from) && !date.isAfter(it.to) }

    // ── The vocabulary: MISSED counts, OVERDUE does not, EXPIRED counts for nothing ────────

    @Test
    fun `a missed block makes its window missed`() {
        val lapsed = today.minusWeeks(2).atTime(9, 0)
        val result = run(
            tasks = listOf(
                task("t", occurrence = Block(lapsed, lapsed.plusHours(1), BlockPlacement.CONFIRMED)),
            ),
        )

        assertThat(result.windowOn(lapsed.toLocalDate()).outcome).isEqualTo(WindowOutcome.MISSED)
        assertThat(result.missed).isEqualTo(1)
    }

    @Test
    fun `an overdue deadline is still owed and is NOT missed`() {
        // §2.3: "OVERDUE ... NOT a failure", and §4.7's material note calls it "the one state
        // that must NOT read as a failure".
        val due = today.minusWeeks(2).atTime(9, 0)
        val result = run(tasks = listOf(task("t", occurrence = Deadline(due))))

        assertThat(result.windowOn(due.toLocalDate()).outcome).isEqualTo(WindowOutcome.STILL_OWED)
        assertThat(result.missed).isEqualTo(0)
        assertThat(result.stillOwed).isEqualTo(1)
    }

    @Test
    fun `an expired block counts for nothing and leaves its window with nothing due`() {
        // §2.3: an unconfirmed block whose time passed "counts for nothing, silently" -- so it
        // is not a miss, and it is also not a window that held something.
        val lapsed = today.minusWeeks(2).atTime(9, 0)
        val result = run(
            tasks = listOf(
                task(
                    "t",
                    occurrence = Block(lapsed, lapsed.plusHours(1), BlockPlacement.PROVISIONAL),
                ),
            ),
        )

        assertThat(result.windowOn(lapsed.toLocalDate()).outcome)
            .isEqualTo(WindowOutcome.NOTHING_DUE)
        assertThat(result.missed).isEqualTo(0)
        assertThat(result.hasRecord).isFalse()
    }

    @Test
    fun `a skipped window counts for nothing -- a skip is not a miss`() {
        // §2.1's skip, and OccurrenceOutcome.Skipped's KDoc: counting a window the person chose
        // to drop is "an over-eager agent manufactures failures" from the other direction.
        val day = today.minusWeeks(2)
        val result = run(
            tasks = listOf(task("t", occurrence = AllDay(day))),
            occurrences = listOf(
                ScheduledOccurrence(
                    id = "o",
                    taskId = "t",
                    occurrence = AllDay(day),
                    outcome = OccurrenceOutcome.Skipped(0L),
                ),
            ),
        )

        assertThat(result.windowOn(day).outcome).isEqualTo(WindowOutcome.NOTHING_DUE)
        assertThat(result.missed).isEqualTo(0)
    }

    @Test
    fun `a passed all-day and a closed span both count as missed`() {
        // The derivation this ticket had to make: #56 named DAY_PASSED and WINDOW_CLOSED
        // because they are not "a block whose slot has gone", NOT because they are not misses
        // -- `meetsUserInDailyReview` already shows all four to the user as misses. Excluding
        // them would say `0 missed` about windows the daily review had just named, and would
        // make the number structurally always zero, since `OccurrenceDraft` can only produce
        // ALL_DAY and DEADLINE.
        val day = today.minusWeeks(3)
        val spanFrom = today.minusWeeks(5)
        val result = run(
            tasks = listOf(
                task("all-day", occurrence = AllDay(day)),
                task("span", occurrence = Span(spanFrom, spanFrom.plusDays(2))),
            ),
        )

        assertThat(result.windowOn(day).outcome).isEqualTo(WindowOutcome.MISSED)
        assertThat(result.windowOn(spanFrom).outcome).isEqualTo(WindowOutcome.MISSED)
        assertThat(result.missed).isEqualTo(2)
    }

    // ── "A window counts as kept when everything due in it was done" ───────────────────────

    @Test
    fun `a window is kept only when everything due in it was done`() {
        val day = today.minusWeeks(2)
        val done = OccurrenceOutcome.Done(day.atTime(10, 0).atZone(zone).toInstant().toEpochMilli())
        val kept = (1..3).map { n ->
            ScheduledOccurrence(
                id = "kept-$n",
                taskId = "kept-$n",
                occurrence = AllDay(day),
                outcome = done,
            )
        }
        val tasks = (1..3).map { task("kept-$it", occurrence = AllDay(day)) } +
            task("slipped", occurrence = AllDay(day))

        val allKept = run(tasks = tasks.dropLast(1), occurrences = kept)
        assertThat(allKept.windowOn(day).outcome).isEqualTo(WindowOutcome.KEPT)

        // One miss beside three kept makes the window missed. "Everything" is the word, and
        // softening it to a majority would be the rate §4.7 refuses, per window.
        val oneSlipped = run(tasks = tasks, occurrences = kept)
        assertThat(oneSlipped.windowOn(day).outcome).isEqualTo(WindowOutcome.MISSED)
        assertThat(oneSlipped.windowOn(day).dueCount).isEqualTo(4)
        assertThat(oneSlipped.windowOn(day).keptCount).isEqualTo(3)
    }

    @Test
    fun `a still-owed window beside a kept one is still owed, not kept`() {
        val day = today.minusWeeks(2)
        val result = run(
            tasks = listOf(
                task("kept", occurrence = AllDay(day), completion = CompletionFact(1L)),
                task("owed", occurrence = Deadline(day.atTime(9, 0))),
            ),
        )

        assertThat(result.windowOn(day).outcome).isEqualTo(WindowOutcome.STILL_OWED)
    }

    @Test
    fun `a completed task with no occurrence documents has its window kept`() {
        // §7.1's *stored* leg: with no occurrences and no rule, the completion fact under
        // completionFacts/{taskId} is the answer -- which is every task written before #63.
        val day = today.minusWeeks(2)
        val result = run(
            tasks = listOf(task("t", occurrence = AllDay(day), completion = CompletionFact(1L))),
        )

        assertThat(result.windowOn(day).outcome).isEqualTo(WindowOutcome.KEPT)
        assertThat(result.kept).isEqualTo(1)
        assertThat(result.missed).isEqualTo(0)
    }

    @Test
    fun `a legacy completion flag never marks a whole repeating series kept`() {
        // The narrow scope of the branch above: with a rule, Doneness is Derived or
        // Unanswerable, so the stored flag does not reach the instances.
        val start = today.minusWeeks(4)
        val result = run(
            tasks = listOf(
                task(
                    "t",
                    occurrence = AllDay(start),
                    rule = RepeatRule(unit = RepeatUnit.WEEK, end = RepeatEnd.AfterCount(4)),
                    completion = CompletionFact(1L),
                ),
            ),
        )

        assertThat(result.kept).isEqualTo(0)
        assertThat(result.missed).isAtLeast(1)
    }

    @Test
    fun `work that is not yet due does not make today's window missed`() {
        // The newest window ends today and is still being written. Judging it on work that has
        // not happened would mark today missed every morning.
        val result = run(tasks = listOf(task("t", occurrence = Deadline(now.plusHours(3)))))

        assertThat(result.windowOn(today).outcome).isEqualTo(WindowOutcome.NOTHING_DUE)
        assertThat(result.missed).isEqualTo(0)
    }

    // ── A success counts in full in every area; only its minutes divide ────────────────────

    @Test
    fun `a task serving two areas counts whole under each of them`() {
        // §4.7: "A success counts in full in every area the task serves, while its minutes
        // divide." The use case takes the goals it should count and nothing else, so there is
        // no share to divide -- counted whole under each is what it can only do.
        val day = today.minusWeeks(2)
        val shared = task(
            "shared",
            goalId = health.id,
            extraGoalId = career.id,
            occurrence = AllDay(day),
            completion = CompletionFact(1L),
        )

        val healthOnly = run(goals = listOf(health), tasks = listOf(shared))
        val careerOnly = run(goals = listOf(career), tasks = listOf(shared))
        val both = run(goals = listOf(health, career), tasks = listOf(shared))

        assertThat(healthOnly.kept).isEqualTo(1)
        assertThat(careerOnly.kept).isEqualTo(1)
        // And on the screen that holds both, it is still ONE window -- the areas share it, they
        // do not each contribute one. That is what makes the analytics totals differ from the
        // sum of the area screens, which §4.7 says is deliberate.
        assertThat(both.kept).isEqualTo(1)
    }

    // ── Nothing ages out: the window is a filter over history, not decay of it ─────────────

    @Test
    fun `a miss outside the chosen range is filtered out, not forgotten`() {
        val old = today.minusWeeks(20)
        val tasks = listOf(task("t", occurrence = AllDay(old)))

        assertThat(run(tasks = tasks, range = SuccessRange.THIRTY_DAYS).missed).isEqualTo(0)
        assertThat(run(tasks = tasks, range = SuccessRange.EIGHT_WEEKS).missed).isEqualTo(0)
        // Same records, wider filter: it is still there.
        assertThat(run(tasks = tasks, range = SuccessRange.SIX_MONTHS).missed).isEqualTo(1)
    }

    @Test
    fun `every range draws its own number of windows and the last one ends today`() {
        SuccessRange.entries.forEach { range ->
            val result = run(range = range)
            assertThat(result.windows).hasSize(range.windowCount)
            assertThat(result.windows.last().to).isEqualTo(today)
            // Oldest first, contiguous, no gap and no overlap: the run reads left to right as
            // time passing, and a gap would hide a miss between two dots.
            result.windows.zipWithNext { earlier, later ->
                assertThat(later.from).isEqualTo(earlier.to.plusDays(1))
            }
        }
    }

    @Test
    fun `there is no rate anywhere -- the pair is a tally of the run itself`() {
        // §4.7: "Two numbers, never a rate." Asserted structurally: every window falls into
        // exactly one of the four counts, so the pair cannot be computed independently of the
        // run and therefore cannot disagree with it (§0.3).
        val day = today.minusWeeks(2)
        val result = run(
            tasks = listOf(
                task("kept", occurrence = AllDay(day.minusWeeks(1)), completion = CompletionFact(1L)),
                task("missed", occurrence = AllDay(day)),
                task("owed", occurrence = Deadline(day.plusWeeks(1).atTime(9, 0))),
            ),
        )

        assertThat(result.kept + result.missed + result.stillOwed + result.nothingDue)
            .isEqualTo(result.windows.size)
        assertThat(result.kept).isEqualTo(1)
        assertThat(result.missed).isEqualTo(1)
        assertThat(result.stillOwed).isEqualTo(1)
    }

    // ── A goal with nothing due is missing a step, not failing ─────────────────────────────

    @Test
    fun `a goal with no open work is offered break it into steps and counts in neither number`() {
        val result = run(goals = listOf(health), tasks = emptyList())

        assertThat(result.noNextStep.map { it.goalId }).containsExactly(health.id)
        assertThat(result.noNextStep.single().offer)
            .isEqualTo(NextStepOffer.BREAK_IT_INTO_STEPS)
        assertThat(result.kept).isEqualTo(0)
        assertThat(result.missed).isEqualTo(0)
        assertThat(result.hasRecord).isFalse()
    }

    @Test
    fun `a goal with undated open work is offered schedule the first one`() {
        val result = run(tasks = listOf(task("t", occurrence = null)))

        assertThat(result.noNextStep.single().offer)
            .isEqualTo(NextStepOffer.SCHEDULE_THE_FIRST_ONE)
    }

    @Test
    fun `a goal whose every date is in the past has no next step either`() {
        // "Having a next step" is open work with a date still ahead of it. A goal that WAS
        // scheduled and no longer is has no next step, and offering to schedule the first one
        // is the honest sentence about it -- while its past miss still counts.
        val result = run(tasks = listOf(task("t", occurrence = AllDay(today.minusWeeks(2)))))

        assertThat(result.noNextStep.single().offer)
            .isEqualTo(NextStepOffer.SCHEDULE_THE_FIRST_ONE)
        assertThat(result.missed).isEqualTo(1)
    }

    @Test
    fun `a goal with work still ahead of it is not listed as having no next step`() {
        val result = run(tasks = listOf(task("t", occurrence = AllDay(today.plusDays(3)))))

        assertThat(result.noNextStep).isEmpty()
    }

    @Test
    fun `idle days count from the last thing that happened, and are absent when nothing has`() {
        val neverTouched = run(goals = listOf(health), tasks = emptyList())
        assertThat(neverTouched.noNextStep.single().idleDays).isNull()

        val lapsed = today.minusDays(40)
        val idle = run(tasks = listOf(task("t", occurrence = AllDay(lapsed))))
        // The all-day's window closes at midnight on the following day, which is the day the
        // last thing that happened, happened.
        assertThat(idle.noNextStep.single().idleDays).isEqualTo(39)
    }

    @Test
    fun `a goal in a different area is neither counted nor listed`() {
        val result = run(
            goals = listOf(health),
            tasks = listOf(task("elsewhere", goalId = career.id, occurrence = AllDay(today.minusWeeks(1)))),
        )

        assertThat(result.missed).isEqualTo(0)
        assertThat(result.noNextStep.map { it.goalId }).containsExactly(health.id)
    }

    // ── A repeating task is a run of windows, not one ──────────────────────────────────────

    @Test
    fun `a weekly rule contributes one window per instance it generated`() {
        val start = today.minusWeeks(5)
        val result = run(
            tasks = listOf(
                task(
                    "weekly",
                    occurrence = AllDay(start),
                    rule = RepeatRule(unit = RepeatUnit.WEEK, end = RepeatEnd.AfterCount(4)),
                ),
            ),
        )

        // Four instances, at weeks -5, -4, -3 and -2, all lapsed and none done.
        assertThat(result.missed).isEqualTo(4)
        assertThat(result.nothingDue).isEqualTo(4)
    }

    @Test
    fun `a stored Done outcome on one instance keeps that window and only that one`() {
        val start = today.minusWeeks(5)
        val keptDate = start.plusWeeks(1)
        val result = run(
            tasks = listOf(
                task(
                    "weekly",
                    occurrence = AllDay(start),
                    rule = RepeatRule(unit = RepeatUnit.WEEK, end = RepeatEnd.AfterCount(4)),
                ),
            ),
            occurrences = listOf(
                ScheduledOccurrence(
                    id = "o",
                    taskId = "weekly",
                    occurrence = AllDay(keptDate),
                    seriesDate = keptDate,
                    outcome = OccurrenceOutcome.Done(1L),
                ),
            ),
        )

        assertThat(result.windowOn(keptDate).outcome).isEqualTo(WindowOutcome.KEPT)
        assertThat(result.kept).isEqualTo(1)
        assertThat(result.missed).isEqualTo(3)
    }
}
