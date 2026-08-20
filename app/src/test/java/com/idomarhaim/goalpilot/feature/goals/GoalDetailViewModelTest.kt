package com.idomarhaim.goalpilot.feature.goals

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Task
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.repository.ProgressRepository
import com.idomarhaim.goalpilot.domain.repository.RecommendationRepository
import com.idomarhaim.goalpilot.domain.repository.TaskRepository
import com.idomarhaim.goalpilot.ui.navigation.Routes
import com.idomarhaim.goalpilot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Issue #3 — completing a task was ~2 s of dead screen online and a silent no-op
 * offline.
 *
 * The two halves are tested together on purpose, because shipping either alone is
 * a regression: the optimistic tick without the undo turns the offline no-op into
 * a *silent lie* (box ticked, points raised, write never landed), and the undo
 * without the optimistic tick leaves the two dead seconds in place.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val progressRepository = mockk<ProgressRepository>(relaxed = true)
    private val recommendationRepository = mockk<RecommendationRepository>(relaxed = true)
    private val lifeAreaRepository = mockk<LifeAreaRepository>(relaxed = true)

    private val goalId = "g1"

    /** What the snapshot listener is currently reporting — the tests push into these. */
    private val observedGoal = MutableStateFlow<Goal?>(
        Goal(id = goalId, title = "Get fit", targetValue = 100.0, currentValue = 2.0),
    )
    private val observedTasks = MutableStateFlow(
        listOf(Task(id = "t1", goalId = goalId, title = "Run 5k", points = 10, isDone = false)),
    )

    private fun task() = observedTasks.value.single()

    /**
     * `uiState` is `WhileSubscribed`, so it computes nothing without a collector.
     * The collector lives on `backgroundScope` and the dispatcher is unconfined, so
     * `uiState.value` is up to date the instant anything upstream changes.
     */
    private fun TestScope.subscribedViewModel(): GoalDetailViewModel {
        every { goalRepository.observeGoal(goalId) } returns observedGoal
        every { taskRepository.observeTasks(goalId) } returns observedTasks
        every { progressRepository.observeEntries(goalId) } returns flowOf(emptyList())
        every { lifeAreaRepository.observeLifeAreas(any()) } returns flowOf(emptyList())

        val handle = mockk<SavedStateHandle>()
        every { handle.get<String>(Routes.ARG_GOAL_ID) } returns goalId

        val vm = GoalDetailViewModel(
            goalRepository = goalRepository,
            taskRepository = taskRepository,
            progressRepository = progressRepository,
            recommendationRepository = recommendationRepository,
            lifeAreaRepository = lifeAreaRepository,
            savedStateHandle = handle,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    // ── The speed half: the screen must not wait for the round trip ───────

    @Test
    fun `a tap ticks the box before Firestore has confirmed anything`() = runTest {
        // The write never comes back for the duration of this test, which is what
        // the 2.24 s of dead screen actually looked like from the UI's side.
        val neverReturns = CompletableDeferred<Resource<Unit>>()
        coEvery { taskRepository.setDone("t1", true) } coAnswers { neverReturns.await() }
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        assertThat(vm.uiState.value.tasks.single().isDone).isTrue()
    }

    @Test
    fun `the goal's progress travels with the checkbox rather than lagging it`() = runTest {
        val neverReturns = CompletableDeferred<Resource<Unit>>()
        coEvery { taskRepository.setDone(any(), any()) } coAnswers { neverReturns.await() }
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        // 2.0 + progressContribution(1.0) — the same arithmetic `GoalProgress`
        // performs once the tick lands, so the ring shows the number the repository
        // is about to derive.
        assertThat(vm.uiState.value.goal!!.currentValue).isEqualTo(3.0)
    }

    @Test
    fun `un-ticking a completed task moves the progress back down`() = runTest {
        observedTasks.value = listOf(task().copy(isDone = true))
        observedGoal.value = observedGoal.value!!.copy(currentValue = 3.0)
        val neverReturns = CompletableDeferred<Resource<Unit>>()
        coEvery { taskRepository.setDone(any(), any()) } coAnswers { neverReturns.await() }
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        assertThat(vm.uiState.value.tasks.single().isDone).isFalse()
        assertThat(vm.uiState.value.goal!!.currentValue).isEqualTo(2.0)
    }

    @Test
    fun `optimistic progress overshoots the target, exactly as the derived number will`() =
        runTest {
            observedGoal.value = Goal(id = goalId, targetValue = 2.0, currentValue = 2.0)
            val neverReturns = CompletableDeferred<Resource<Unit>>()
            coEvery { taskRepository.setDone(any(), any()) } coAnswers { neverReturns.await() }
            val vm = subscribedViewModel()

            vm.toggleTask(task())

            // 3.0, not 2.0. This assertion is inverted from what it was, and the
            // inversion is the point of #49: the preview used to clamp because the
            // write clamped, and both clamps hid a goal the user had beaten. The
            // sum over facts does not clamp, so neither may the preview — the two
            // have to agree, and now they agree on the true number.
            assertThat(vm.uiState.value.goal!!.currentValue).isEqualTo(3.0)
        }

    // ── The honesty half: a failure must undo the tick and say so ─────────

    @Test
    fun `a failed write takes the tick back and says so`() = runTest {
        // Exactly what an offline tap produces when the pre-check does not catch
        // it — a captive portal, or Firestore unreachable behind a live network.
        coEvery { taskRepository.setDone("t1", true) } returns
            Resource.Error("UNAVAILABLE: Unable to resolve host firestore.googleapis.com")
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        assertThat(vm.uiState.value.tasks.single().isDone).isFalse()
        assertThat(vm.action.value.message)
            .isEqualTo(GoalDetailViewModel.SAVE_FAILED_MESSAGE)
    }

    @Test
    fun `the raw gRPC failure text never reaches the user`() = runTest {
        // The device pass put "UNAVAILABLE: Unable to resolve host
        // firestore.googleapis.com" in a snackbar. Actionable repository text is
        // worth surfacing; a transport stack trace is not.
        coEvery { taskRepository.setDone(any(), any()) } returns
            Resource.Error("UNAVAILABLE: Unable to resolve host firestore.googleapis.com")
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        assertThat(vm.action.value.message).doesNotContain("UNAVAILABLE")
        assertThat(vm.action.value.message).doesNotContain("googleapis")
    }

    @Test
    fun `a failed write takes the goal's progress back with it`() = runTest {
        coEvery { taskRepository.setDone(any(), any()) } returns Resource.Error("boom")
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        // The whole point of pairing the two fixes: no orphaned progress left
        // behind by a write that never landed.
        assertThat(vm.uiState.value.goal!!.currentValue).isEqualTo(2.0)
    }

    // ── The overlay must retire cleanly, without flicker or double-count ──

    @Test
    fun `a successful write keeps the tick while the snapshot is still catching up`() = runTest {
        coEvery { taskRepository.setDone("t1", true) } returns Resource.Success(Unit)
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        // setDone has returned but observedTasks has NOT been updated yet — the
        // completion callback and the snapshot arrive on different channels.
        // Dropping the overlay here is what would make the row flicker back.
        assertThat(vm.uiState.value.tasks.single().isDone).isTrue()
    }

    @Test
    fun `once the snapshot catches up the overlay retires without double-counting`() = runTest {
        coEvery { taskRepository.setDone("t1", true) } returns Resource.Success(Unit)
        val vm = subscribedViewModel()

        vm.toggleTask(task())
        // The server confirms: the task is done and the goal moved 2.0 -> 3.0.
        observedTasks.value = listOf(task().copy(isDone = true))
        observedGoal.value = observedGoal.value!!.copy(currentValue = 3.0)

        assertThat(vm.uiState.value.tasks.single().isDone).isTrue()
        // 3.0, not 4.0 — the overlay must stop contributing once it agrees with
        // the observed data, or every completion counts twice.
        assertThat(vm.uiState.value.goal!!.currentValue).isEqualTo(3.0)
    }

    @Test
    fun `a completed task sorts below an open one, as the repository would return it`() = runTest {
        observedTasks.value = listOf(
            Task(id = "t1", goalId = goalId, title = "Run 5k", createdAtEpochMillis = 200),
            Task(id = "t2", goalId = goalId, title = "Stretch", createdAtEpochMillis = 100),
        )
        val neverReturns = CompletableDeferred<Resource<Unit>>()
        coEvery { taskRepository.setDone(any(), any()) } coAnswers { neverReturns.await() }
        val vm = subscribedViewModel()

        vm.toggleTask(vm.uiState.value.tasks.first { it.id == "t1" })

        // The optimistic list is a faithful preview of the confirmed one, so the
        // row settles into its final position now rather than jumping again later.
        assertThat(vm.uiState.value.tasks.map { it.id }).containsExactly("t2", "t1").inOrder()
    }

    // ── The rest of the sweep: results that were being discarded ──────────

    @Test
    fun `a failed delete surfaces its message instead of being discarded`() = runTest {
        coEvery { taskRepository.deleteTask("t1") } returns Resource.Error("Not signed in")
        val vm = subscribedViewModel()

        vm.deleteTask("t1")

        assertThat(vm.action.value.message).isEqualTo("Not signed in")
    }

    @Test
    fun `a failed add surfaces its message instead of being discarded`() = runTest {
        coEvery { taskRepository.upsertTask(any()) } returns Resource.Error("Could not save task")
        val vm = subscribedViewModel()

        vm.addTask("Swim", 10, 30)

        assertThat(vm.action.value.message).isEqualTo("Could not save task")
    }

    @Test
    fun `a failed archive surfaces a message instead of failing silently`() = runTest {
        coEvery { goalRepository.setArchived(goalId, true) } returns Resource.Error("offline")
        val vm = subscribedViewModel()

        vm.archiveGoal()

        assertThat(vm.action.value.message).isEqualTo("Could not archive goal")
    }

    @Test
    fun `a successful toggle says nothing - silence is the success signal`() = runTest {
        coEvery { taskRepository.setDone(any(), any()) } returns Resource.Success(Unit)
        val vm = subscribedViewModel()

        vm.toggleTask(task())

        // A snackbar on every tick would be worse than the bug: ticking a task is
        // the most repeated action in the app.
        assertThat(vm.action.value.message).isNull()
    }
}
