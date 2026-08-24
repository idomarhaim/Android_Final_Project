package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.BlockPlacement
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Difficulty
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.GoalFiling
import com.idomarhaim.goalpilot.domain.model.GoalPlan
import com.idomarhaim.goalpilot.domain.model.PlanStep
import com.idomarhaim.goalpilot.domain.model.PlanStepKind
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import com.idomarhaim.goalpilot.domain.model.TaskSchedule
import com.idomarhaim.goalpilot.domain.model.TaskSource
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.domain.usecase.ApplyGoalPlanUseCase
import com.idomarhaim.goalpilot.domain.usecase.GoalPlanOutcome
import com.idomarhaim.goalpilot.domain.usecase.SyncCalendarUseCase
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * §3.7's plan, on the client side — `C8`
 * [#24](https://github.com/idomarhaim/Android_Final_Project/issues/24), Ido 2026-08-24.
 *
 * ## What this suite covers, and where the other half is
 *
 * The **wire** half — which fields survive, what a container may not carry, that no id and no
 * date are ever authored — is `functions/test/plan.test.mjs`, because §3.4 puts validation in the
 * Cloud Function **singly**. There is deliberately no Kotlin twin of that validator: two
 * implementations of the same rule are two answers that drift the first time either is edited.
 *
 * What is here is everything the **app** decides and the function cannot:
 *
 * 1. **Which rung a step becomes**, which §3.7 says *falls out* of the label rather than being a
 *    fifth thing the model chose.
 * 2. **Which date it lands on**, resolved from an offset against the day the plan is applied —
 *    the reason the model answers in offsets at all.
 * 3. **What is written**, which is the kept steps and nothing else. That is the draft gate.
 * 4. **That the calendar needs no second write**, proved by asking `TaskSchedule` — the same type
 *    the app's calendar surface and `SyncCalendarUseCase` both read — what it sees.
 *
 * Everything is pure: no Firestore, no device, and no clock read (`appliedOn` is a parameter, so
 * a test can pin the day the offsets resolve against).
 */
class GoalPlanTest {

    private val monday = LocalDate.of(2026, 8, 24)

    /**
     * A [SyncCalendarUseCase] that does nothing.
     *
     * Relaxed rather than stubbed because **whether the sync ran is not what this suite is
     * about**: the plan's contract is the tasks it writes, and `ApplyGoalPlanUseCase` deliberately
     * discards the sync's outcome (a user who never granted the calendar scope gets
     * `NeedsConsent` on every pass, which is §2.6's ordinary state and not a fault of this
     * feature). Asserting on it here would freeze a behaviour the KDoc says is not one.
     */
    private fun noSync(): SyncCalendarUseCase = mockk(relaxed = true)

    private fun work(
        index: Int = 0,
        title: String = "Run 5 km",
        minutes: Int? = 40,
        offset: Int? = 2,
        at: LocalTime? = null,
        difficulty: Difficulty = Difficulty.ROUTINE,
        keep: Boolean = true,
    ) = PlanStep(
        index = index,
        title = title,
        kind = PlanStepKind.WORK,
        difficulty = difficulty,
        estimatedMinutes = minutes,
        dayOffset = offset,
        timeOfDay = at,
        keep = keep,
    )

    private fun milestone(index: Int = 0, offset: Int? = 45, keep: Boolean = true) = PlanStep(
        index = index,
        title = "Comfortable at 10 km",
        kind = PlanStepKind.MILESTONE,
        dayOffset = offset,
        keep = keep,
    )

    // ── 1 · The rung falls out of the label (§3.7, §2.2) ─────────────────────────

    @Test
    fun `work with a slot becomes a BLOCK spanning its estimated minutes`() {
        val occurrence = work(minutes = 45, at = LocalTime.of(7, 30)).occurrenceOn(monday)

        assertThat(occurrence).isInstanceOf(Block::class.java)
        val block = occurrence as Block
        assertThat(block.start).isEqualTo(monday.plusDays(2).atTime(7, 30))
        assertThat(block.end).isEqualTo(monday.plusDays(2).atTime(8, 15))
    }

    @Test
    fun `an agent-placed block is PROVISIONAL, so a slot it missed is silent`() {
        val block = work(at = LocalTime.of(7, 30)).occurrenceOn(monday) as Block

        // §2.4: a BLOCK needs confirmation because 09:00 may already be taken; §2.3 makes an
        // unconfirmed block that lapses EXPIRED and silent. This is the first code in the app
        // that writes PROVISIONAL, and it is why an accepted-then-ignored plan does not
        // manufacture a wall of failures against the user.
        assertThat(block.placement).isEqualTo(BlockPlacement.PROVISIONAL)
        assertThat(block.placement.isEndorsed).isFalse()
    }

    @Test
    fun `a block with no estimate still has a length, and it is the app's default`() {
        val block = work(minutes = null, at = LocalTime.of(9, 0)).occurrenceOn(monday) as Block

        // Nothing is invented about the DURATION -- `estimatedMinutes` stays absent on the task
        // (see the write test below). What is defaulted is the length of the SLOT, because a
        // zero-length block is not a window at all.
        assertThat(block.end).isEqualTo(
            block.start.plusMinutes(TaskDuration.DEFAULT_MINUTES.toLong()),
        )
    }

    @Test
    fun `work with no slot becomes an ALL_DAY, not a block at midnight`() {
        val occurrence = work(at = null).occurrenceOn(monday)

        assertThat(occurrence).isEqualTo(AllDay(monday.plusDays(2)))
    }

    @Test
    fun `a milestone becomes a DEADLINE, whose miss is OVERDUE rather than a failure`() {
        val occurrence = milestone(offset = 45).occurrenceOn(monday)

        assertThat(occurrence).isInstanceOf(Deadline::class.java)
        // Not midnight: a deadline at 00:00 is owed before the day it names has begun, which
        // reads as a day late to anyone looking at it.
        assertThat((occurrence as Deadline).at).isEqualTo(monday.plusDays(45).atTime(18, 0))
    }

    @Test
    fun `a step with no offset has no when at all, which is a legal task`() {
        assertThat(work(offset = null).occurrenceOn(monday)).isNull()
        assertThat(milestone(offset = null).occurrenceOn(monday)).isNull()
    }

    // ── 2 · The offset resolves against the day the plan is APPLIED ──────────────

    @Test
    fun `the same draft applied on a later day lands on later dates`() {
        val step = work(offset = 3, at = null)

        val onMonday = step.occurrenceOn(monday) as AllDay
        val onFriday = step.occurrenceOn(monday.plusDays(4)) as AllDay

        // §3.7 says a draft persists "exactly as left … no expiry". An absolute date from the
        // model would have pinned this to the day the plan was DRAWN, and nothing on screen
        // would have said so. This is the whole reason `dayOffset` is the wire's field.
        assertThat(onMonday.date).isEqualTo(LocalDate.of(2026, 8, 27))
        assertThat(onFriday.date).isEqualTo(LocalDate.of(2026, 8, 31))
    }

    @Test
    fun `offset zero is today, and is not confused with saying nothing`() {
        assertThat((work(offset = 0, at = null).occurrenceOn(monday) as AllDay).date)
            .isEqualTo(monday)
    }

    // ── 3 · The draft gate: kept steps, and nothing else ─────────────────────────

    @Test
    fun `kept is exactly what the user left ticked`() {
        val plan = GoalPlan(
            goalId = "g1",
            steps = listOf(work(index = 0), work(index = 1, keep = false), milestone(index = 2)),
        )

        assertThat(plan.kept.map { it.index }).containsExactly(0, 2).inOrder()
    }

    @Test
    fun `applying a plan writes only the kept steps`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())
        val plan = GoalPlan(
            goalId = "g1",
            steps = listOf(
                work(index = 0, title = "kept"),
                work(index = 1, title = "dropped", keep = false),
            ),
        )

        val outcome = useCase(plan, appliedOn = monday)

        assertThat(repo.written.map { it.title }).containsExactly("kept")
        assertThat(outcome.written).isEqualTo(1)
        assertThat(outcome.scheduled).isEqualTo(1)
        assertThat(outcome.isCompleteSuccess).isTrue()
    }

    @Test
    fun `a plan with nothing kept writes nothing and reports nothing`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        val outcome = useCase(
            GoalPlan(goalId = "g1", steps = listOf(work(keep = false))),
            appliedOn = monday,
        )

        assertThat(repo.written).isEmpty()
        assertThat(outcome).isEqualTo(
            GoalPlanOutcome(),
        )
    }

    @Test
    fun `a half-written plan reports both halves rather than failing whole`() = runTest {
        val repo = RecordingTaskRepository(failTitles = setOf("second"))
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        val outcome = useCase(
            GoalPlan(
                goalId = "g1",
                steps = listOf(work(index = 0, title = "first"), work(index = 1, title = "second")),
            ),
            appliedOn = monday,
        )

        // The step that landed is real. Reporting "the plan failed" would send the user looking
        // for a task that is already in their list.
        assertThat(outcome.written).isEqualTo(1)
        assertThat(outcome.failed).isEqualTo(1)
        assertThat(outcome.isCompleteSuccess).isFalse()
        assertThat(outcome.message).isNotNull()
    }

    // ── 4 · What a written step actually is ──────────────────────────────────────

    @Test
    fun `a written step is filed on its goal and priced on the model's judgement`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        useCase(
            GoalPlan(goalId = "g1", steps = listOf(work(minutes = 60, difficulty = Difficulty.DEMANDING))),
            appliedOn = monday,
        )

        val task = repo.written.single()
        assertThat(task.goalId).isEqualTo("g1")
        assertThat(task.estimatedMinutes).isEqualTo(60)
        assertThat(task.difficulty).isEqualTo(Difficulty.DEMANDING)
        // §1.4: the app computes the currency from the two inputs. Nothing named a point value.
        assertThat(task.points).isEqualTo(com.idomarhaim.goalpilot.domain.model.TaskScoring.pointsFor(60, Difficulty.DEMANDING))
    }

    @Test
    fun `an estimated duration is marked AI, never USER`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        useCase(GoalPlan(goalId = "g1", steps = listOf(work(minutes = 40))), appliedOn = monday)

        // USER is sticky and exempts the task from re-estimation forever (`#9`). A model's guess
        // marked USER would be a duration the user never typed, permanently frozen.
        assertThat(repo.written.single().durationSource).isEqualTo(DurationSource.AI)
    }

    @Test
    fun `a step the model did not price carries no duration and no source`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        useCase(GoalPlan(goalId = "g1", steps = listOf(work(minutes = null))), appliedOn = monday)

        val task = repo.written.single()
        assertThat(task.estimatedMinutes).isNull()
        assertThat(task.durationSource).isEqualTo(DurationSource.UNKNOWN)
    }

    @Test
    fun `a milestone is never priced, and the source enum is not lied to`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        useCase(GoalPlan(goalId = "g1", steps = listOf(milestone())), appliedOn = monday)

        val task = repo.written.single()
        assertThat(task.estimatedMinutes).isNull()
        assertThat(task.difficulty).isEqualTo(Difficulty.ROUTINE)
        // `TaskSource` answers "did this come from Google Tasks?" and has two values. A planned
        // step did not, so MANUAL is the honest answer to the question the enum asks.
        assertThat(task.source).isEqualTo(TaskSource.MANUAL)
    }

    // ── 5 · The calendar needs no second write ───────────────────────────────────

    @Test
    fun `a written step is visible to the same schedule the calendar and the sync read`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        useCase(
            GoalPlan(goalId = "g1", steps = listOf(work(offset = 2, at = LocalTime.of(7, 30)))),
            appliedOn = monday,
        )

        // `TaskSchedule.occurrencesIn` is what `SyncCalendarUseCase.entriesIn` walks and what
        // §4.3's calendar surface reads. Its fourth source is "the anchor itself, when the task
        // has neither a rule nor any stored document" -- exactly the shape written here. So the
        // plan reaches both calendars without this feature writing a calendar event at all,
        // which is the point: the occurrence IS the fact and the event is a mirror of it.
        val schedule = TaskSchedule(task = repo.written.single(), stored = emptyList())
        val visible = schedule.occurrencesIn(
            from = monday,
            to = monday.plusDays(7),
            zone = ZoneId.of("Asia/Jerusalem"),
        )

        assertThat(visible).hasSize(1)
        assertThat(visible.single().occurrence).isInstanceOf(Block::class.java)
    }

    @Test
    fun `a step with no date is on the list and not on the calendar`() = runTest {
        val repo = RecordingTaskRepository()
        val useCase = ApplyGoalPlanUseCase(repo, noSync())

        val outcome = useCase(
            GoalPlan(goalId = "g1", steps = listOf(work(offset = null))),
            appliedOn = monday,
        )

        assertThat(outcome.written).isEqualTo(1)
        assertThat(outcome.scheduled).isEqualTo(0)
        assertThat(repo.written.single().occurrence).isNull()
    }

    // ── 6 · GoalFiling's absences ────────────────────────────────────────────────

    @Test
    fun `a filing that said nothing is empty, and an empty filing is never applied`() {
        assertThat(GoalFiling().isEmpty).isTrue()
        assertThat(GoalFiling(lifeAreaId = "a1").isEmpty).isFalse()
        assertThat(
            GoalFiling(category = com.idomarhaim.goalpilot.domain.model.GoalCategory.FITNESS).isEmpty,
        ).isFalse()
    }

    @Test
    fun `an unrecognised label reads as WORK, which is the recoverable direction`() {
        assertThat(PlanStepKind.fromName(null)).isEqualTo(PlanStepKind.WORK)
        assertThat(PlanStepKind.fromName("nonsense")).isEqualTo(PlanStepKind.WORK)
        assertThat(PlanStepKind.fromName("state_you_reach")).isEqualTo(PlanStepKind.MILESTONE)
    }
}

/**
 * A [TaskRepository] that records what was written and can be told to fail named titles.
 *
 * Only `upsertTask` is reachable from the code under test; the rest throw rather than returning
 * a plausible empty value, so a use case that started calling one would fail loudly here instead
 * of silently passing on a fake that agreed with it.
 */
private class RecordingTaskRepository(
    private val failTitles: Set<String> = emptySet(),
) : TaskRepository {

    val written = mutableListOf<Task>()

    override suspend fun upsertTask(task: Task): Resource<String> =
        if (task.title in failTitles) {
            Resource.Error("offline")
        } else {
            written += task.copy(id = "t${written.size}")
            Resource.Success("t${written.size - 1}")
        }

    override fun observeTasks(goalId: String?): Flow<List<Task>> = flowOf(written.toList())
    override suspend fun setDone(taskId: String, done: Boolean): Resource<Unit> =
        throw UnsupportedOperationException("not reachable from ApplyGoalPlanUseCase")
    override suspend fun deleteTask(taskId: String): Resource<Unit> =
        throw UnsupportedOperationException("not reachable from ApplyGoalPlanUseCase")
}
