package com.idomarhaim.goalpilot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_BACK
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_CARD
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_CREATE_GOAL
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_HINT
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_NEXT
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_OVERLAY
import com.idomarhaim.goalpilot.ui.tutorial.TAG_TUTORIAL_SKIP
import com.idomarhaim.goalpilot.ui.tutorial.TutorialOverlay
import com.idomarhaim.goalpilot.ui.tutorial.TutorialStep
import org.junit.Rule
import org.junit.Test

/**
 * The coach mark as drawn, on a real device.
 *
 * `TutorialControllerTest` owns the sequence and `TutorialPlacementTest` owns
 * the arithmetic; both run on the JVM. What is left for a device is everything
 * about the *overlay as an obstacle*: which controls exist on which kind of
 * step, and — the one that matters most — whether the scrim really stops a tap
 * from reaching the app behind it, and really stops stopping it over the
 * spotlight. That is a hit-testing question, and there is no way to answer it
 * except by putting a finger on the glass.
 *
 * The overlay is stateless, so every case drives it directly rather than through
 * the controller. Deliberate: an assertion that fails here is about the drawing
 * and one that fails on the JVM is about the sequence, with no case that could
 * be either.
 */
class TutorialOverlayUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val stepState = mutableStateOf(TutorialStep.entries.first())
    private val targetState = mutableStateOf<Rect?>(null)
    private val pendingState = mutableStateOf(false)

    private var next = 0
    private var back = 0
    private var skip = 0
    private var createGoal = 0

    /** Taps that got past the overlay — the whole reason there is a screen behind it. */
    private var appTaps = 0

    private var composed = false

    /**
     * Put the overlay over a full-screen tap target.
     *
     * The target is the point of the fixture, not scenery: it is how *did the
     * tap reach the app* is asked at all. Composed once and then driven by state,
     * because `setContent` may only be called once per rule — and because a tour
     * that is torn down between steps would never exercise the spotlight moving.
     */
    private fun show(
        step: TutorialStep,
        target: Rect? = null,
        actionPending: Boolean = false,
    ) {
        if (!composed) {
            composed = true
            composeRule.setContent {
                GoalPilotTheme {
                    Box(Modifier.fillMaxSize()) {
                        val interaction = remember { MutableInteractionSource() }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                ) { appTaps++ },
                        ) {
                            Text("the app")
                        }
                        val current by stepState
                        val pending by pendingState
                        val spotlight by targetState
                        TutorialOverlay(
                            step = current,
                            pendingAction = current.action.takeIf { pending },
                            target = spotlight,
                            onNext = { next++ },
                            onBack = { back++ },
                            onSkip = { skip++ },
                            onCreateGoal = { createGoal++ },
                        )
                    }
                }
            }
        }
        composeRule.runOnUiThread {
            stepState.value = step
            targetState.value = target
            pendingState.value = actionPending
        }
        composeRule.waitForIdle()
    }

    // ── What is on the card ──────────────────────────────────────────────

    @Test
    fun firstStep_offersStartAndSkipButNotBack() {
        show(TutorialStep.entries.first())

        composeRule.onNodeWithTag(TAG_TUTORIAL_CARD).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_TUTORIAL_NEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_TUTORIAL_SKIP).assertIsDisplayed()
        // Back on step one would be a control that does nothing, which is worse
        // than no control: it teaches that the buttons here are decorative.
        composeRule.onNodeWithTag(TAG_TUTORIAL_BACK).assertDoesNotExist()
    }

    @Test
    fun theCounterSaysWhereTheUserIsAndHowMuchIsLeft() {
        show(TutorialStep.entries.first())

        composeRule.onNodeWithText("Step 1 of ${TutorialStep.count}").assertIsDisplayed()
    }

    @Test
    fun aLaterStep_offersBack() {
        show(TutorialStep.entries[1])

        composeRule.onNodeWithTag(TAG_TUTORIAL_BACK).performClick()

        assertThat(back).isEqualTo(1)
    }

    @Test
    fun lastStep_offersTheClosingAction() {
        show(TutorialStep.entries.last())

        composeRule.onNodeWithTag(TAG_TUTORIAL_CREATE_GOAL).performClick()

        assertThat(createGoal).isEqualTo(1)
    }

    @Test
    fun skipIsOnEveryStep() {
        // Not "on the first step". Skip is the promise that makes the whole
        // overlay acceptable, and a step that quietly lost it is a step the user
        // is trapped on.
        TutorialStep.entries.forEachIndexed { index, step ->
            show(step, actionPending = step.action != null)
            composeRule.onNodeWithTag(TAG_TUTORIAL_SKIP).performClick()
            assertThat(skip).isEqualTo(index + 1)
        }
    }

    // ── The interactive step ─────────────────────────────────────────────

    @Test
    fun aPendingActionReplacesNextWithAnImperative() {
        val step = TutorialStep.entries.first { it.action != null }

        show(step, actionPending = true)

        composeRule.onNodeWithTag(TAG_TUTORIAL_HINT).assertIsDisplayed()
        // Offering Next here would make the instruction optional, and an
        // optional instruction is one nobody follows.
        composeRule.onNodeWithTag(TAG_TUTORIAL_NEXT).assertDoesNotExist()
    }

    @Test
    fun aPerformedActionGivesTheStepItsNextButtonBack() {
        // The state a user reaches by pressing Back from the next step. Without
        // it the tour is a dead end one gesture away.
        val step = TutorialStep.entries.first { it.action != null }

        show(step, actionPending = false)

        composeRule.onNodeWithTag(TAG_TUTORIAL_NEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_TUTORIAL_HINT).assertDoesNotExist()
    }

    // ── The overlay as an obstacle ───────────────────────────────────────

    @Test
    fun anInformationalStepIsDismissedByTappingAnywhere() {
        show(TutorialStep.entries.first())

        composeRule.onNodeWithTag(TAG_TUTORIAL_OVERLAY)
            .performTouchInput { click(Offset(4f, 4f)) }

        assertThat(next).isEqualTo(1)
        // And the app behind it never saw the tap.
        assertThat(appTaps).isEqualTo(0)
    }

    @Test
    fun aPendingActionLetsTheTapReachTheSpotlitWidgetAndNothingElse() {
        // The single most important behaviour in this file. The step says "tap
        // Goals"; if the scrim swallows that tap the tour cannot be completed at
        // all, and if the scrim lets every tap through the user wanders off
        // mid-tour. Both failures are invisible on a screenshot.
        val step = TutorialStep.entries.first { it.action != null }
        val spotlight = Rect(200f, 400f, 500f, 700f)

        show(step, target = spotlight, actionPending = true)

        composeRule.onNodeWithTag(TAG_TUTORIAL_OVERLAY)
            .performTouchInput { click(Offset(350f, 550f)) }
        assertThat(appTaps).isEqualTo(1)

        composeRule.onNodeWithTag(TAG_TUTORIAL_OVERLAY)
            .performTouchInput { click(Offset(4f, 4f)) }
        assertThat(appTaps).isEqualTo(1)
        // ...and a blocked tap is not silently treated as Next either.
        assertThat(next).isEqualTo(0)
    }

    @Test
    fun anInformationalStepBlocksTheAppEvenOverItsOwnSpotlight() {
        // The hole is a hole in the DIM, not in the blocking, unless the step is
        // waiting for a tap. Otherwise one rectangle of the screen would behave
        // differently from the rest for no reason the user can see.
        show(TutorialStep.entries[1], target = Rect(200f, 400f, 500f, 700f))

        composeRule.onNodeWithTag(TAG_TUTORIAL_OVERLAY)
            .performTouchInput { click(Offset(350f, 550f)) }

        assertThat(appTaps).isEqualTo(0)
        // ⚠️ AND IT DOES NOT ADVANCE EITHER. This assertion read `isEqualTo(1)`
        // until 2026-08-24 and the change that broke it was deliberate.
        //
        // `Observed:` Ido, on his first run of the tour — *"when it marked me to
        // press certain buttons (for example the calendar), I did not see it open
        // what I pressed on."* A pulsing ring around a real control reads as
        // *press me*; the blockers covered the hole; and the tap landed on the
        // scrim, whose informational behaviour was **advance**. So pressing the
        // thing the tour was pointing at moved the tour on and never opened the
        // tab — an affordance that lied, and then swallowed the gesture it had
        // invited.
        //
        // Tap-anywhere-to-advance now fires **only where the step points at
        // nothing**. Where there is a ring, the ring means *look here* and Next
        // means *go on*, and the two no longer compete for one tap.
        assertThat(next).isEqualTo(0)
    }

    @Test
    fun aStepThatPointsAtNothingStillAdvancesOnATapAnywhere() {
        // The surviving half of the old behaviour, and the reason it survives:
        // with no ring on screen there is no control being pointed at, so a tap
        // cannot be mistaken for pressing one. It is the gesture people try
        // first and the welcome step is where they try it.
        val anchorless = TutorialStep.entries.first { it.anchor == null }

        show(anchorless, target = null)

        composeRule.onNodeWithTag(TAG_TUTORIAL_OVERLAY)
            .performTouchInput { click(Offset(350f, 550f)) }

        assertThat(appTaps).isEqualTo(0)
        assertThat(next).isEqualTo(1)
    }
}
