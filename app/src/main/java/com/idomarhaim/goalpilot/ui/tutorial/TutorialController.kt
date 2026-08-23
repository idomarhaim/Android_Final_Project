package com.idomarhaim.goalpilot.ui.tutorial

import com.idomarhaim.goalpilot.domain.repository.AppPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the tour is showing right now.
 *
 * ## `pendingAction` is not `step.action`, and the difference is the Back button
 *
 * [TutorialStep.GOALS_TAB] asks the user to open the Goals tab, and once they
 * have, it must stop asking. If the card read its imperative straight off the
 * step, then stepping *backwards* into it from step five would demand the tap
 * again — from a screen where the tab is already open, so the tap changes no
 * route, so nothing completes and the tour is stuck with no Next button on it.
 * That is a dead end reachable by pressing Back once, which is the sort of thing
 * a tour is never forgiven for.
 *
 * So *has the user done this yet* is state, and `pendingAction` is the answer:
 * `null` means either the step never asked or the user has already obliged, and
 * either way the card shows Next.
 *
 * ## A non-null `pendingAction` does NOT mean *no Next*
 *
 * Since 2026-08-24 an action carries [TutorialAction.required]. A **required**
 * one withholds Next; an **invited** one only opens the spotlight's hole so the
 * control underneath genuinely works. Both are `pendingAction`, and every
 * reader of this field has to ask which — the two places that matter are
 * [TutorialController.next] and the overlay's card.
 */
data class TutorialUiState(
    val step: TutorialStep,
    /** The imperative still outstanding on this step, or `null` if there is none left. */
    val pendingAction: TutorialAction?,
    /**
     * The route the tour wants the user on, or **`null` for *wherever they
     * are*** — which is the whole of the 2026-08-24 fix and is not the same
     * thing as [TutorialStep.route].
     *
     * ## Why a step's route is not enough
     *
     * `TutorialHost` herds: any frame where the current route is not the step's
     * route, it navigates. That is right while the tour is talking and wrong the
     * instant the user has just been invited to press something.
     *
     * `Observed:` 2026-08-24, on the emulator at Ido's own geometry, in the
     * first build that made [TutorialStep.CALENDAR] pressable. Tapping the
     * spotlighted Calendar tab did open the Calendar and did advance the tour —
     * and step seven lives on the dashboard, so the host navigated **straight
     * back to Home**. The calendar was on screen for about one frame. Ido's
     * complaint was *"I did not see it open what I pressed on"*, and a tour that
     * opens the thing and then closes it fails that sentence exactly as
     * completely as one that never opened it. The first fix was not wrong, it
     * was half.
     *
     * So performing an **invited** action does not advance the step. It clears
     * the imperative, puts Next on the card, and sets this to `null` — the tour
     * stops steering and waits, with its explanation still on screen, while the
     * user looks at the thing they just opened. Next moves on, and the next
     * step's own route takes over the steering.
     *
     * A **required** action still advances, and needs none of this: the step
     * after [TutorialStep.GOALS_TAB] lives on the very route that tap reaches,
     * which is why that one never showed the defect.
     */
    val route: String?,
)

/**
 * The guided tour's state machine: which step is showing, and what moves it.
 *
 * ## Why this is a `@Singleton` and not a `ViewModel`
 *
 * The tour is the one thing in this app that **outlives the screen it is drawn
 * on**. Step four sends the user from the dashboard to Goals, step seven brings
 * them back, and the replay control lives on a third screen entirely — so a
 * state holder scoped to any of them is reset by the very navigation the tour
 * performs. A `ViewModel` on the nav graph's root back-stack entry would survive
 * that, at the cost of every consumer needing the same entry to look it up; a
 * singleton is the same lifetime with none of that ceremony, and the tour
 * genuinely is one app-wide thing rather than a per-screen one.
 *
 * It holds no Android types and no Compose types, which is what lets the whole
 * sequence be tested on the JVM (`TutorialControllerTest`) rather than only on a
 * device.
 *
 * ## What it deliberately does not do
 *
 * It does not navigate. It says which step is current and which route that step
 * belongs on, and [TutorialHost] — which owns a `NavController` — decides what to
 * do about it. Putting navigation here would make the tour untestable without a
 * nav graph, and would give a singleton the power to move the user's screen from
 * anywhere in the app.
 */
@Singleton
class TutorialController @Inject constructor(
    private val appPreferences: AppPreferencesRepository,
) {

    private val _state = MutableStateFlow<TutorialUiState?>(null)

    /** The step being shown, or `null` — overwhelmingly the common case — for *no tour running*. */
    val state: StateFlow<TutorialUiState?> = _state.asStateFlow()

    /**
     * Steps whose [TutorialAction] the user has already performed.
     *
     * Cleared on every start, not accumulated across runs: a replay is a replay,
     * and a second run that silently skipped its one interactive moment would be
     * a different tour from the one the user asked to see again.
     */
    private var performed: Set<TutorialStep> = emptySet()

    val isRunning: Boolean get() = _state.value != null

    /**
     * Start the tour if this install has never seen this version of it.
     *
     * Called on every entry into the signed-in app, and answers *no* almost
     * always. Idempotent in both directions: an install that has seen the tour is
     * not started, and one that is **already running** it is not restarted —
     * which matters because the caller is a `LaunchedEffect` whose composition is
     * recreated on rotation, and the alternative throws the user back to step one
     * for turning their phone.
     */
    fun startIfFirstRun() {
        if (isRunning) return
        if (appPreferences.tutorialSeenVersion.value >= TUTORIAL_VERSION) return
        begin()
    }

    /**
     * Start the tour because the user asked for it, from Settings.
     *
     * Unconditional — that is the difference from [startIfFirstRun], and the
     * reason the two are separate functions rather than one with a flag. A caller
     * that has to pass `force = true` to get the behaviour its own button
     * promises is a caller that can pass `false` by accident.
     */
    fun restart() = begin()

    /**
     * Advance, or finish if this was the last step.
     *
     * Ignored while the current step is still waiting on a **required**
     * [TutorialAction], so a stray tap cannot skip past the one thing the tour
     * asks the user to do.
     *
     * ⚠️ **`required` is checked, not mere presence** — an *invited* action
     * (2026-08-24: [TutorialStep.CALENDAR]) is an offer, so its step keeps its
     * Next button and this must move on when it is pressed. Reading
     * `pendingAction != null` here would leave that step with neither a Next
     * that works nor a demand that says why, which is the dead end this class's
     * KDoc spends a paragraph avoiding one case earlier.
     */
    fun next() {
        val current = _state.value ?: return
        if (current.pendingAction?.required == true) return
        advanceFrom(current.step)
    }

    /**
     * Go back one step, or do nothing on the first.
     *
     * Nothing on the first rather than dismissing, because the system back
     * gesture also lands here: *back on step one* means **close this**, and
     * [TutorialHost] is where that is decided. Deciding it here would make one
     * method mean two things depending on a number.
     */
    fun back() {
        val current = _state.value ?: return
        if (current.step.isFirst) return
        show(TutorialStep.entries[current.step.ordinal - 1])
    }

    /**
     * The user did the thing the current step asked for — or offered. Records
     * it, then moves on.
     *
     * Recording is what stops the step demanding it again when the user steps
     * back into it; see [TutorialUiState]. It applies to an *invited* action
     * too: having actually opened the Calendar once, being invited to again on
     * the way back is noise.
     */
    fun completeAction() {
        val current = _state.value ?: return
        val action = current.pendingAction ?: return
        performed = performed + current.step
        if (action.required) {
            advanceFrom(current.step)
        } else {
            // Stay put and stop steering. See [TutorialUiState.route] for the
            // frame-long calendar this exists to prevent.
            _state.value = current.copy(pendingAction = null, route = null)
        }
    }

    /**
     * The user asked to stop, and does not want to be asked again.
     *
     * Skipping **records the tour as seen**, which is the choice worth stating:
     * the alternative — re-offering it on the next launch — reads as the app
     * ignoring an explicit no, and is the behaviour that makes people dislike
     * onboarding. What makes it safe is that the tour is never lost, only put
     * away: Settings carries a replay control whether or not the step that names
     * it was ever reached.
     */
    fun skip() = finish()

    /** The user reached the end, or skipped. Same effect, and the same record. */
    fun finish() {
        _state.value = null
        appPreferences.setTutorialSeenVersion(TUTORIAL_VERSION)
    }

    private fun begin() {
        performed = emptySet()
        show(TutorialStep.entries.first())
    }

    private fun advanceFrom(current: TutorialStep) {
        if (current.isLast) finish() else show(TutorialStep.entries[current.ordinal + 1])
    }

    private fun show(step: TutorialStep) {
        _state.value = TutorialUiState(
            step = step,
            pendingAction = step.action?.takeIf { step !in performed },
            // Every step starts by steering. Only performing an invited action
            // turns that off, and only for as long as that step is showing.
            route = step.route,
        )
    }
}
