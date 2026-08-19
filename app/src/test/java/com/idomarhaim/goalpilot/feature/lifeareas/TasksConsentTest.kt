package com.idomarhaim.goalpilot.feature.lifeareas

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.tasks.GoogleTasksClient
import com.idomarhaim.goalpilot.data.tasks.TaskListsResult
import com.idomarhaim.goalpilot.domain.model.TasksConsent
import com.idomarhaim.goalpilot.domain.repository.GoalRepository
import com.idomarhaim.goalpilot.domain.repository.LifeAreaRepository
import com.idomarhaim.goalpilot.domain.usecase.BuildLifeAreaProposalsUseCase
import com.idomarhaim.goalpilot.domain.usecase.ReorderLifeAreasUseCase
import com.idomarhaim.goalpilot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Issue #36 — a declined Google Tasks scope has to read as *declined*.
 *
 * Google's granular consent screen shows *"View your tasks"* **unticked**
 * (spec §2.6, observed live on 2026-08-09), so sign-in can succeed while
 * granting nothing. The recovery path was too good to notice it: a declined
 * scope and a first-ever run both landed on the same generic grant prompt, and
 * only *after* an import had already failed.
 *
 * These tests pin the state machine that fixes it. What they deliberately do
 * **not** cover is [GoogleTasksClient.consentState] itself — it is three lines
 * over the static `GoogleSignIn.hasPermissions`, needs a real `Context` and a
 * Play Services cache, and there is no Robolectric in this project. That is
 * exactly why it is only the *cheap up-front probe* here and never the
 * authority: every assertion below is about a caller correcting it.
 *
 * The `LifeAreas` screen is the subject because it reads the **same**
 * `tasks.readonly` scope as the dashboard import, on a ViewModel small enough
 * to construct honestly. `DashboardViewModel` carries the identical wiring.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksConsentTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val lifeAreaRepository = mockk<LifeAreaRepository>(relaxed = true)
    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val googleTasksClient = mockk<GoogleTasksClient>(relaxed = true)

    private fun viewModel(): LifeAreasViewModel {
        every { lifeAreaRepository.observeLifeAreas() } returns flowOf(emptyList())
        every { goalRepository.observeGoals() } returns flowOf(emptyList())
        return LifeAreasViewModel(
            lifeAreaRepository = lifeAreaRepository,
            goalRepository = goalRepository,
            googleTasksClient = googleTasksClient,
            buildProposals = BuildLifeAreaProposalsUseCase(),
            reorderAreas = ReorderLifeAreasUseCase(),
        )
    }

    @Test
    fun `consent is unknown until something has actually looked`() = runTest {
        val vm = viewModel()

        // Null, not MISSING. Telling someone they declined a scope nobody has
        // checked is the same class of lie as the generic prompt this replaces
        // — and the card renders its ordinary self while this is null.
        assertThat(vm.tasksConsent.value).isNull()
        coVerify(exactly = 0) { googleTasksClient.consentState() }
    }

    @Test
    fun `an unticked box is read up front, without waiting for an import to fail`() = runTest {
        // The whole of #36's in-scope half: this answer is available straight
        // after sign-in, from the cached account, with no network call.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.MISSING
        val vm = viewModel()

        vm.refreshTasksConsent()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.MISSING)
    }

    @Test
    fun `no google account reads as not-signed-in, never as declined`() = runTest {
        // §0.4 licenses speech about a failure the user can act on. "You left the
        // box unticked" is not something an account that was never shown the box
        // can act on, so the two states stay distinct all the way to the card.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.NOT_SIGNED_IN
        val vm = viewModel()

        vm.refreshTasksConsent()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.NOT_SIGNED_IN)
    }

    @Test
    fun `re-entering the screen corrects a reading that has gone stale`() = runTest {
        // The bug this replaces a once-per-ViewModel guard to avoid: grant the
        // scope on the *other* surface, navigate back here, and a guarded probe
        // would keep the card accusing a user who has already complied. The
        // composable is recreated on back-navigation; this ViewModel is not.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.MISSING
        val vm = viewModel()
        vm.refreshTasksConsent()
        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.MISSING)

        coEvery { googleTasksClient.consentState() } returns TasksConsent.GRANTED
        vm.refreshTasksConsent()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.GRANTED)
        coVerify(exactly = 2) { googleTasksClient.consentState() }
    }

    @Test
    fun `a refused token overrides a probe that said granted`() = runTest {
        // The cached GoogleSignInAccount is written by the sign-in flow, so it can
        // be wrong in both directions. Google refusing to mint a token is the
        // authoritative answer and it wins.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.GRANTED
        coEvery { googleTasksClient.fetchTaskLists() } returns
            TaskListsResult.NeedsConsent(mockk<Intent>(relaxed = true))
        val vm = viewModel()
        vm.refreshTasksConsent()
        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.GRANTED)

        vm.syncFromGoogleTasks()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.MISSING)
    }

    @Test
    fun `a minted token clears a stale probe that said missing`() = runTest {
        // The failure this prevents: the user grants the scope through Google's
        // own recovery screen, which need not write itself back into the cached
        // sign-in account — so the probe keeps saying MISSING and the card keeps
        // accusing a user who has just complied. A successful call proves the
        // scope is held whatever the cache thinks.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.MISSING
        coEvery { googleTasksClient.fetchTaskLists() } returns
            TaskListsResult.Success(emptyList())
        val vm = viewModel()
        vm.refreshTasksConsent()
        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.MISSING)

        vm.syncFromGoogleTasks()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.GRANTED)
    }

    @Test
    fun `backing out of the consent screen is recorded as a decline`() = runTest {
        // Before #36 this branch only cleared the intent, so cancelling Google's
        // screen returned the card to the state it had before the user refused —
        // the refusal left no trace anywhere in the app.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.GRANTED
        val vm = viewModel()
        vm.refreshTasksConsent()

        vm.onConsentDeclined()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.MISSING)
        assertThat(vm.consentIntent.value).isNull()
    }

    @Test
    fun `an ordinary failure says nothing about consent`() = runTest {
        // A timeout is not a refusal. Only NeedsConsent and a minted token move
        // this state; anything else leaves the last known reading alone.
        coEvery { googleTasksClient.consentState() } returns TasksConsent.GRANTED
        coEvery { googleTasksClient.fetchTaskLists() } returns
            TaskListsResult.Failure("Could not read your Google Tasks lists")
        val vm = viewModel()
        vm.refreshTasksConsent()

        vm.syncFromGoogleTasks()

        assertThat(vm.tasksConsent.value).isEqualTo(TasksConsent.GRANTED)
        assertThat(vm.sync.value.error).isEqualTo("Could not read your Google Tasks lists")
    }
}
