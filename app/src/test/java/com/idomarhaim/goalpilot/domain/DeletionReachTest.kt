package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.CompletionFact
import com.idomarhaim.goalpilot.domain.model.Deletion
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.OccurrenceOutcome
import com.idomarhaim.goalpilot.domain.model.ScheduledOccurrence
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.goalEdgesOf
import org.junit.Test
import java.time.LocalDate

/**
 * **`#67`'s rules** — who can be reached, and what a deletion takes with it.
 *
 * ### Why the reach half is tested at all
 *
 * The founding defect of `#67` is a task that no screen lists, and *"no screen lists it"* is
 * not a property any screen can assert about itself: each one is individually correct, and the
 * gap is between them. `Deletion.unreachableTasks` is that gap written down as a predicate over
 * the same two facts the screens filter on — is it filed under a goal that exists, and does it
 * have a *when* — so the case can be exercised without a device, and so the dashboard's card
 * cannot quietly start showing a different population from the one it claims.
 *
 * ### Why the impact half is tested
 *
 * Each count on a [com.idomarhaim.goalpilot.domain.model.DeletionImpact] is a **claim about a
 * write** the confirm dialog then says out loud. A claim that drifts from its repository is
 * invisible in an English render — the dialog composes, the sentence scans, and the number is
 * wrong. These are the assertions that make the drift break something.
 */
class DeletionReachTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 23)

    private fun task(
        id: String,
        goalId: String? = null,
        occurrence: com.idomarhaim.goalpilot.domain.model.Occurrence? = null,
        done: Boolean = false,
        minutes: Int = 30,
    ) = Task(
        id = id,
        title = "task $id",
        goalEdges = goalEdgesOf(goalId),
        occurrence = occurrence,
        estimatedMinutes = minutes,
        difficulty = Difficulty.ROUTINE,
        completion = if (done) {
            CompletionFact(minutes = minutes, difficulty = Difficulty.ROUTINE)
        } else {
            null
        },
    )

    private fun goal(id: String, areaIds: List<String> = emptyList()) =
        Goal(id = id, title = "goal $id", lifeAreaIds = areaIds)

    // ── reach ───────────────────────────────────────────────────────────

    @Test
    fun `a quick-add filed under nothing and dated nothing is on no screen`() {
        // Exactly what `DashboardViewModel.classifyForSmartAdd` writes for a
        // `FilingDecision.NoGoal`: no edge, no occurrence. This is the defect.
        val orphan = task("orphan")

        val found = Deletion.unreachableTasks(tasks = listOf(orphan), goals = emptyList())

        assertThat(found.map { it.id }).containsExactly("orphan")
    }

    @Test
    fun `a task filed under a goal is reachable, because the goal screen lists it`() {
        val filed = task("filed", goalId = "g")

        val found = Deletion.unreachableTasks(tasks = listOf(filed), goals = listOf(goal("g")))

        assertThat(found).isEmpty()
    }

    @Test
    fun `an unfiled task with a date is reachable, because the calendar draws it`() {
        val dated = task("dated", occurrence = AllDay(today))

        val found = Deletion.unreachableTasks(tasks = listOf(dated), goals = emptyList())

        assertThat(found).isEmpty()
    }

    @Test
    fun `an unfiled task with a stored occurrence and no anchor is still on the calendar`() {
        // `TaskSchedule.occurrencesIn` emits a document with no `seriesDate` as a one-off, on
        // its own date, whether or not the task carries an anchor. So the document is reach.
        val loose = task("loose")
        val stored = ScheduledOccurrence(id = "o", taskId = "loose", occurrence = AllDay(today))

        val found = Deletion.unreachableTasks(
            tasks = listOf(loose),
            goals = emptyList(),
            occurrences = listOf(stored),
        )

        assertThat(found).isEmpty()
    }

    @Test
    fun `an edge to a goal that no longer exists is not reach`() {
        // The second shape of the same defect, and the one that reads as filed. Before `#67`
        // `deleteGoal` left every edge pointing at the document it had just removed.
        val dangling = task("dangling", goalId = "deleted-goal")

        val found = Deletion.unreachableTasks(
            tasks = listOf(dangling),
            goals = listOf(goal("some-other-goal")),
        )

        assertThat(found.map { it.id }).containsExactly("dangling")
    }

    @Test
    fun `an archived goal still counts as reach`() {
        // Archiving is 1_1's reversible verb and the goal is still openable. Treating an
        // archived goal as gone would hide its tasks in the unreachable list and invite the
        // person to delete work that is one un-archive away from being visible again.
        val filed = task("filed", goalId = "g")
        val archived = goal("g").copy(isArchived = true)

        val found = Deletion.unreachableTasks(tasks = listOf(filed), goals = listOf(archived))

        assertThat(found).isEmpty()
    }

    @Test
    fun `a blank goal id on either side never matches`() {
        // A blank id is *absent*, not a value, and two absences are not the same goal. Without
        // this, every unfiled task would look filed under every id-less goal.
        val blankEdge = task("t", goalId = "")
        val blankGoal = goal("")

        val found = Deletion.unreachableTasks(tasks = listOf(blankEdge), goals = listOf(blankGoal))

        assertThat(found.map { it.id }).containsExactly("t")
    }

    // ── what a goal's deletion takes ────────────────────────────────────

    @Test
    fun `deleting a goal unfiles its tasks and takes its log`() {
        val g = goal("g")
        val tasks = listOf(task("a", goalId = "g"), task("b", goalId = "g"), task("c", goalId = "other"))

        val impact = Deletion.ofGoal(goal = g, tasks = tasks, entryCount = 4)

        assertThat(impact.unfiledTaskCount).isEqualTo(2)
        assertThat(impact.entryCount).isEqualTo(4)
        assertThat(impact.subjectName).isEqualTo("goal g")
    }

    // ── what a task's deletion takes ────────────────────────────────────

    @Test
    fun `deleting a task takes every occurrence it has, and says how many already happened`() {
        val t = task("t", occurrence = AllDay(today))
        val occurrences = listOf(
            ScheduledOccurrence(id = "1", taskId = "t", occurrence = AllDay(today.minusDays(2)), outcome = OccurrenceOutcome.Done(1L)),
            ScheduledOccurrence(id = "2", taskId = "t", occurrence = AllDay(today.minusDays(1)), outcome = OccurrenceOutcome.Skipped(1L)),
            ScheduledOccurrence(id = "3", taskId = "t", occurrence = AllDay(today.plusDays(1))),
            ScheduledOccurrence(id = "4", taskId = "elsewhere", occurrence = AllDay(today)),
        )

        val impact = Deletion.ofTask(task = t, occurrences = occurrences)

        assertThat(impact.occurrenceCount).isEqualTo(3)
        // Done and Skipped are settled; Planned is not. A skip is a decision that was made, so
        // it is part of what the person is about to lose, exactly as a completion is.
        assertThat(impact.settledOccurrenceCount).isEqualTo(2)
    }

    @Test
    fun `an open task has banked no points, so its deletion takes none`() {
        // `Task.points` prices an OPEN task from its current minutes so the add row can show
        // what it would be worth. That number was never awarded, and naming it here would tell
        // the person they are about to lose something they never had.
        val open = task("open", minutes = 30)
        assertThat(open.points).isGreaterThan(0)

        val impact = Deletion.ofTask(task = open, occurrences = emptyList())

        assertThat(impact.bankedPoints).isEqualTo(0)
    }

    @Test
    fun `a done task takes the points its completion fact banked`() {
        val done = task("done", minutes = 30, done = true)

        val impact = Deletion.ofTask(task = done, occurrences = emptyList())

        assertThat(impact.bankedPoints).isEqualTo(done.points)
        assertThat(impact.bankedPoints).isGreaterThan(0)
    }

    // ── what a life area's deletion takes ───────────────────────────────

    @Test
    fun `deleting a life area keeps its goals and unfiles them`() {
        val area = LifeArea(id = "a", name = "Health")
        val goals = listOf(
            goal("g1", areaIds = listOf("a")),
            goal("g2", areaIds = listOf("a", "b")),
            goal("g3", areaIds = listOf("b")),
        )

        val impact = Deletion.ofLifeArea(area = area, goals = goals)

        assertThat(impact.unfiledGoalCount).isEqualTo(2)
        assertThat(impact.subjectName).isEqualTo("Health")
    }
}
