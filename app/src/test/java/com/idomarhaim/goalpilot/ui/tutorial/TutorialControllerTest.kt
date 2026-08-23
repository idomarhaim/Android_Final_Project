package com.idomarhaim.goalpilot.ui.tutorial

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.testing.FakeAppPreferences
import com.idomarhaim.goalpilot.ui.navigation.Routes
import org.junit.Test

/**
 * The guided tour's sequence, driven on the JVM.
 *
 * This is the layer that would otherwise only be testable by installing the app
 * on a phone, signing in, and watching — which is to say, not testable at all
 * for the cases that matter: *what happens on the second launch*, *what happens
 * when the user steps backwards into the one interactive step*, *what happens
 * when the tour is replayed*. Every one of those is a state the controller can
 * be put into in three lines here, and none of them is reachable by hand
 * without wiping app data between attempts.
 */
class TutorialControllerTest {

    private val preferences = FakeAppPreferences()
    private val controller = TutorialController(preferences)

    // ── First run, and the second one ────────────────────────────────────

    @Test
    fun `a fresh install opens on the first step`() {
        controller.startIfFirstRun()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.entries.first())
    }

    @Test
    fun `an install that has seen this version is not shown it again`() {
        preferences.setTutorialSeenVersion(TUTORIAL_VERSION)

        controller.startIfFirstRun()

        assertThat(controller.state.value).isNull()
    }

    @Test
    fun `an install that saw an older version is shown the new one`() {
        // The whole reason the preference is an Int. With a boolean this case is
        // indistinguishable from the one above, and every existing user misses
        // every step ever added.
        preferences.setTutorialSeenVersion(TUTORIAL_VERSION - 1)

        controller.startIfFirstRun()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.entries.first())
    }

    @Test
    fun `a running tour is not restarted by a second start`() {
        // The caller is a LaunchedEffect whose composition is recreated on
        // rotation. Without this, turning the phone sideways mid-tour throws the
        // user back to step one.
        controller.startIfFirstRun()
        controller.next()
        val afterOneStep = controller.state.value?.step

        controller.startIfFirstRun()

        assertThat(controller.state.value?.step).isEqualTo(afterOneStep)
    }

    // ── Moving through it ────────────────────────────────────────────────

    @Test
    fun `next walks forward one step at a time`() {
        controller.startIfFirstRun()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.WELCOME)
        controller.next()
        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.PROGRESS)
        controller.next()
        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.QUICK_ADD)
    }

    @Test
    fun `back walks it in reverse, and stops at the first step`() {
        controller.startIfFirstRun()
        controller.next()

        controller.back()
        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.WELCOME)

        // Not a dismissal: TutorialHost decides that a back gesture on step one
        // means "stop", and it decides it there so that one method does not mean
        // two things depending on a number.
        controller.back()
        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.WELCOME)
    }

    @Test
    fun `finishing the last step ends the tour and records the version`() {
        controller.restart()
        repeat(TutorialStep.count) { advanceOneStep() }

        assertThat(controller.state.value).isNull()
        assertThat(preferences.tutorialSeenVersion.value).isEqualTo(TUTORIAL_VERSION)
    }

    @Test
    fun `skipping records the version, so it is not offered again`() {
        controller.startIfFirstRun()

        controller.skip()

        assertThat(controller.state.value).isNull()
        assertThat(preferences.tutorialSeenVersion.value).isEqualTo(TUTORIAL_VERSION)

        controller.startIfFirstRun()
        assertThat(controller.state.value).isNull()
    }

    // ── The one interactive step ─────────────────────────────────────────

    @Test
    fun `an action step will not advance on next`() {
        controller.restart()
        walkTo(TutorialStep.GOALS_TAB)

        controller.next()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.GOALS_TAB)
    }

    @Test
    fun `an action step advances when the user reaches its route`() {
        controller.restart()
        walkTo(TutorialStep.GOALS_TAB)

        controller.completeAction()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.NEW_GOAL)
    }

    @Test
    fun `an INVITED action does not advance -- it stops the tour steering`() {
        // The 2026-08-24 defect, as an assertion. Ido tapped the spotlighted
        // Calendar tab; it opened, the tour advanced, and step seven's route
        // sent him straight back to Home. The calendar was up for one frame,
        // which fails *"I want to see the result of what I press"* exactly as
        // completely as never opening it.
        controller.restart()
        walkTo(TutorialStep.CALENDAR)
        assertThat(controller.state.value?.pendingAction).isNotNull()

        controller.completeAction()

        val after = controller.state.value
        assertWithMessage("performing an invited action must not move the step on")
            .that(after?.step).isEqualTo(TutorialStep.CALENDAR)
        assertWithMessage("the imperative is spent, so the card must now offer Next")
            .that(after?.pendingAction).isNull()
        assertWithMessage(
            "a null route is what tells TutorialHost to stop navigating -- with the " +
                "step's own route here, the host steers the user off the screen they " +
                "just opened",
        ).that(after?.route).isNull()
    }

    @Test
    fun `a REQUIRED action still advances on the spot`() {
        controller.restart()
        walkTo(TutorialStep.GOALS_TAB)

        controller.completeAction()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.NEW_GOAL)
        // It steers again immediately, and that is right: this step has not been
        // performed, so the tour is talking rather than waiting.
        assertThat(controller.state.value?.route).isEqualTo(TutorialStep.NEW_GOAL.route)
    }

    @Test
    fun `Next past a resolved invitation moves on and steers again`() {
        controller.restart()
        walkTo(TutorialStep.CALENDAR)
        controller.completeAction()

        controller.next()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.WHERE_SETTINGS)
        assertThat(controller.state.value?.route).isEqualTo(TutorialStep.WHERE_SETTINGS.route)
    }

    @Test
    fun `stepping back into a performed action step offers Next instead of the imperative`() {
        // The dead end this exists to prevent: the step asks for a tap on the
        // Goals tab, and stepping back into it lands the user on the Goals
        // screen — where the tab is already open, so the tap changes no route,
        // so nothing completes and the card has no Next button on it.
        controller.restart()
        walkTo(TutorialStep.GOALS_TAB)
        controller.completeAction()

        controller.back()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.GOALS_TAB)
        assertThat(controller.state.value?.pendingAction).isNull()

        controller.next()
        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.NEW_GOAL)
    }

    @Test
    fun `a replay asks for the action again`() {
        // A replay is a replay. A second run that silently skipped its one
        // interactive moment would be a different tour from the one the user
        // asked to see again.
        controller.restart()
        walkTo(TutorialStep.GOALS_TAB)
        controller.completeAction()
        controller.skip()

        controller.restart()
        walkTo(TutorialStep.GOALS_TAB)

        assertThat(controller.state.value?.pendingAction).isNotNull()
        assertThat(controller.state.value?.pendingAction?.completedOnRoute).isEqualTo(Routes.GOALS)
    }

    @Test
    fun `completing an action nothing asked for changes nothing`() {
        controller.restart()

        controller.completeAction()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.WELCOME)
    }

    @Test
    fun `nothing moves while the tour is not running`() {
        controller.next()
        controller.back()
        controller.completeAction()

        assertThat(controller.state.value).isNull()
        // In particular, none of those recorded the tour as seen — a stray call
        // must not be able to consume a first run that never happened.
        assertThat(preferences.tutorialSeenVersion.value).isEqualTo(0)
    }

    // ── Replay ───────────────────────────────────────────────────────────

    @Test
    fun `restart runs the tour even for an install that has seen it`() {
        preferences.setTutorialSeenVersion(TUTORIAL_VERSION)

        controller.restart()

        assertThat(controller.state.value?.step).isEqualTo(TutorialStep.entries.first())
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * One **step** forward, whichever kind of step it is.
     *
     * ⚠️ That can take two calls into the controller, and the asymmetry is the
     * behaviour rather than a wrinkle in this helper. Performing an *invited*
     * action does not advance: it clears the imperative and stops the tour
     * steering, so the user can look at the thing they just opened. Next is what
     * moves on from there. A *required* action advances on the spot, as before.
     * See `TutorialUiState.route`.
     */
    private fun advanceOneStep() {
        val before = controller.state.value?.step
        if (controller.state.value?.pendingAction != null) controller.completeAction()
        if (controller.state.value != null && controller.state.value?.step == before) {
            controller.next()
        }
    }

    private fun walkTo(step: TutorialStep) {
        var guard = 0
        while (controller.state.value?.step != step) {
            check(guard++ < TutorialStep.count) { "never reached $step" }
            advanceOneStep()
        }
    }
}
