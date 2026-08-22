package com.idomarhaim.goalpilot.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.ui.components.ENTRANCE_WINDOW_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/**
 * The tour's one piece of wiring: it turns [TutorialController]'s step into a
 * drawn overlay, and turns the app's current route into the controller's input.
 *
 * ## Why navigation lives here and not in the controller
 *
 * The controller is a plain object with no Android in it, which is what lets the
 * whole sequence be tested on the JVM. Handing it a `NavController` would undo
 * that, and would also give an app-wide singleton the power to move the user's
 * screen from anywhere. So the split is: the controller says **which step and
 * which route that step belongs on**, and this composable — which is the only
 * thing here that already holds a `NavController` — decides what to do about the
 * difference.
 *
 * ## One effect, not two, and that is a bug fix rather than a tidy-up
 *
 * Completing an action step and navigating for an informational one are two
 * rules over the same two inputs, and as two `LaunchedEffect`s they race: the
 * frame the user taps *Goals*, one effect advances the step and the other —
 * still holding the old step, which says it belongs on the dashboard — navigates
 * straight back. Which one wins depends on composition order, so it would work
 * on the device it was written on. As one ordered effect the question does not
 * arise.
 */
@Composable
fun TutorialHost(
    anchors: TutorialAnchors,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: TutorialViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // After the dashboard's arrival wave, not before it: a coach mark
        // dropped on top of cards that are still rising reads as two animations
        // fighting, and the first thing the user sees of the app should be the
        // app. `ENTRANCE_WINDOW_MS` is the column's own answer to "when is this
        // screen finished arriving?", so it is borrowed rather than guessed at.
        delay(ENTRANCE_WINDOW_MS + START_GRACE_MS)
        viewModel.startIfFirstRun()
    }

    // Published for `Modifier.tutorialAnchor`, which uses it to scroll its own
    // widget into view. Written from an effect rather than during composition —
    // a composition that writes state another composable reads is a
    // recomposition loop waiting for a second reader.
    LaunchedEffect(state) {
        anchors.activeAnchor = state?.step?.anchor
    }

    LaunchedEffect(state, currentRoute) {
        val current = state ?: return@LaunchedEffect
        val pending = current.pendingAction
        if (pending != null && currentRoute == pending.completedOnRoute) {
            viewModel.completeAction()
            return@LaunchedEffect
        }
        if (currentRoute != current.step.route) onNavigate(current.step.route)
    }

    val current = state ?: return

    // Back means "one step back", and on the first step it means "stop" — which
    // is the same promise the Skip button makes, so it records the tour as seen
    // exactly as Skip does. Anything else and a user who reflexively swipes back
    // is asked again tomorrow.
    BackHandler {
        if (current.step.isFirst) viewModel.skip() else viewModel.back()
    }

    TutorialOverlay(
        step = current.step,
        pendingAction = current.pendingAction,
        target = current.step.anchor?.let { anchors[it] },
        onNext = viewModel::next,
        onBack = viewModel::back,
        onSkip = viewModel::skip,
        onCreateGoal = {
            viewModel.finish()
            onCreateGoal()
        },
    )
}

/**
 * The composition's window onto the app-wide [TutorialController].
 *
 * It holds no state of its own and exists for one reason: `hiltViewModel()` is
 * how a composable reaches the DI graph without an entry point, and the
 * alternative — threading the singleton down from `MainActivity` — would put a
 * tutorial parameter on every composable between here and there.
 */
@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val controller: TutorialController,
) : ViewModel() {

    val state: StateFlow<TutorialUiState?> = controller.state

    fun startIfFirstRun() = controller.startIfFirstRun()

    /** Settings' Replay control. Unconditional; see [TutorialController.restart]. */
    fun restart() = controller.restart()

    fun next() = controller.next()

    fun back() = controller.back()

    fun skip() = controller.skip()

    fun finish() = controller.finish()

    fun completeAction() = controller.completeAction()
}

/**
 * A little after the arrival wave, so the tour lands on a screen that has
 * finished moving rather than one that is still settling.
 */
private const val START_GRACE_MS = 250L
