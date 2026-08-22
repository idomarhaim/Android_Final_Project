package com.idomarhaim.goalpilot.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.ui.navigation.Routes
import com.idomarhaim.goalpilot.ui.root.navigateForTutorial
import org.junit.Rule
import org.junit.Test

/**
 * The move the guided tour makes to put a step's own screen in front of the user.
 *
 * ### Why this test exists, and why it is instrumented
 *
 * `Observed:` 2026-08-22, on `emulator-5554`. Replaying the tour from Settings
 * started the tour and left the app **on Settings**, with step one's overlay
 * drawn over it. The host's own logging showed the decision being taken
 * correctly and `navigate()` returning without throwing — the navigation simply
 * did not happen, silently, and only from that one origin. The identical call
 * from the Goals tab worked, which is what made it survive a forwards walk of
 * the whole tour: every step passed, and the entry point a user is most likely
 * to reach it from was broken.
 *
 * Nothing on the JVM can catch that. It is `NavController`'s own behaviour under
 * one particular set of `NavOptions`, so the only test that means anything is one
 * that builds a real graph and asks where it ended up.
 *
 * ### It deliberately does not pin the broken behaviour
 *
 * The obvious second case is *and the tab move fails here*, which would prove the
 * guard is not vacuous. It is left out: that behaviour is androidx's, not this
 * app's, and a test asserting it would start failing the day the library fixed
 * it — reporting a regression on the release that removed the bug. The account of
 * the defect belongs in `navigateForTutorial`'s KDoc and in the changelog; this
 * file holds the requirement.
 */
class TutorialNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController

    private fun setContent() {
        composeRule.setContent {
            navController = rememberNavController()
            // The shape that matters: two top-level tabs and one screen pushed
            // above them, with the tour's own start destination first.
            NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
                composable(Routes.DASHBOARD) { Text("dashboard") }
                composable(Routes.GOALS) { Text("goals") }
                composable(Routes.SETTINGS) { Text("settings") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun currentRoute(): String? =
        composeRule.runOnIdle { navController.currentBackStackEntry?.destination?.route }

    private fun go(route: String) {
        composeRule.runOnUiThread { navController.navigateForTutorial(route) }
        composeRule.waitForIdle()
    }

    @Test
    fun itReachesTheStartDestinationFromAScreenPushedAboveIt() {
        // The case that was broken. Settings is not a tab; it is pushed on top of
        // the dashboard, which is exactly where Settings' Replay control lives.
        setContent()
        composeRule.runOnUiThread { navController.navigate(Routes.SETTINGS) }
        composeRule.waitForIdle()
        assertThat(currentRoute()).isEqualTo(Routes.SETTINGS)

        go(Routes.DASHBOARD)

        assertThat(currentRoute()).isEqualTo(Routes.DASHBOARD)
    }

    @Test
    fun itReachesTheStartDestinationFromAnotherTab() {
        // The case that always worked, kept because it is the one the tour takes
        // between steps six and seven — and because a fix for the case above that
        // broke this one would otherwise ship.
        setContent()
        go(Routes.GOALS)
        assertThat(currentRoute()).isEqualTo(Routes.GOALS)

        go(Routes.DASHBOARD)

        assertThat(currentRoute()).isEqualTo(Routes.DASHBOARD)
    }

    @Test
    fun itReachesAnotherTabFromTheStartDestination() {
        setContent()

        go(Routes.GOALS)

        assertThat(currentRoute()).isEqualTo(Routes.GOALS)
    }

    @Test
    fun itDoesNotPileTheSameScreenUp() {
        // The tour's navigation runs from an effect keyed on the step and the
        // route, so a step whose route is already current must be a no-op rather
        // than a second copy. Otherwise a user pressing Back after the tour walks
        // out through a stack of dashboards.
        setContent()
        go(Routes.GOALS)

        go(Routes.GOALS)
        go(Routes.GOALS)

        assertThat(currentRoute()).isEqualTo(Routes.GOALS)
        val depth = composeRule.runOnIdle { navController.currentBackStack.value.size }
        go(Routes.GOALS)
        assertThat(composeRule.runOnIdle { navController.currentBackStack.value.size })
            .isEqualTo(depth)
    }
}
