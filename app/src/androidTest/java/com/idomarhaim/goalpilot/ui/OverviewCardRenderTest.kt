package com.idomarhaim.goalpilot.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.feature.dashboard.OverviewCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * `#70` — **the run `f25cca5` owed**: the dashboard's *Overall progress* card in the
 * three states `#66` gave it, on a device.
 *
 * ## Why this file exists when `UnmeasuredPercentTest` already covers the state
 *
 * The JVM suite asserts `DashboardUiState.measuredGoalCount`, which is the **input**
 * to this card. It says nothing about what the card draws from it, and the one thing
 * `f25cca5` shipped that no assertion anywhere covered is **geometry**:
 *
 * > `UnmeasuredMarker(size = 56.dp, modifier = Modifier.padding(18.dp))`
 *
 * replaces `ProgressRing(size = 92.dp)`, and `56 + 18 + 18 = 92`. But that sum is
 * only the node's height because `UnmeasuredMarker` writes `modifier.size(size)`,
 * which puts the caller's padding **outside** the sized box. Had it written
 * `Modifier.size(size).then(modifier)` — the same three numbers, one order apart —
 * the node would measure **56 dp** with 20 dp of usable middle, and the card would
 * lose 36 dp of height in exactly one of its three states. That is arithmetic whose
 * answer lives in a modifier order in **another file**, which is why reading it was
 * never going to settle it.
 *
 * ## The assertion is a comparison, never a constant
 *
 * [theCardKeepsItsHeightInAllThreeStates] pins the three heights **against each
 * other**. A hard-coded dp would encode this emulator's density and font scale and
 * then fail on the next device for a reason with nothing to do with the defect; the
 * claim actually being made is *the marker matches the ring it replaces*, and that
 * is a difference of zero however tall the card happens to be.
 *
 * ## The PNGs carry the half an assertion cannot
 *
 * Whether a card with a dashed square where its ring used to be reads as
 * **deliberate** or as **broken** is Ido's judgement and not a matcher's. Both
 * brightnesses are captured and neither is a duplicate: the marker is a 1.5 dp
 * dashed stroke at `onSurfaceVariant`, so it depends on **contrast** rather than
 * hue, and `#66`'s original defect was found in a dark render.
 *
 * §0.8 is suspended, so this is English only.
 */
class OverviewCardRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ----------------------------------------------------------- the assertions

    /**
     * **The one thing this session exists to check.** Three states, one page, one
     * comparison — a marker that does not match the ring it replaces shows up here
     * as a height that differs from the other two.
     */
    @Test
    fun theCardKeepsItsHeightInAllThreeStates() {
        composeRule.setContent { Page(dark = false) { ThreeStates() } }

        val all = composeRule.onNodeWithTag(TAG_ALL).getUnclippedBoundsInRoot().height
        val some = composeRule.onNodeWithTag(TAG_SOME).getUnclippedBoundsInRoot().height
        val none = composeRule.onNodeWithTag(TAG_NONE).getUnclippedBoundsInRoot().height

        // Both compared against the measured card, which is the one state
        // `f25cca5` did not touch — so a failure here always means the NEW arm
        // moved, never that the baseline drifted.
        //
        // ⚠️ **A tolerance, and it is not a softened assertion.** The first run of
        // this test used exact equality and reported
        // `none=236.33331` against `all=236.33334` — a disagreement of
        // **0.00003 dp**, which at this device's density is about one
        // ten-thousandth of a pixel and is simply the px→dp round trip landing on
        // a different float. The defect being hunted is the marker measuring
        // 56 dp where the ring measured 92: a **36 dp** gap. `0.05` is 700× smaller
        // than that and 1600× larger than the observed noise, so it cannot hide a
        // real mismatch and cannot fire on arithmetic that is exactly right.
        // Recorded rather than quietly relaxed, because a tolerance nobody can
        // justify is how a geometry test stops testing geometry.
        assertThat(some.value).isWithin(0.05f).of(all.value)
        assertThat(none.value).isWithin(0.05f).of(all.value)
    }

    /**
     * **The other axis, and the render is why it is here.** Height was the axis the
     * brief named; looking at the PNG raised the horizontal one, because the ring's
     * ink is *not* centred in its own box — a progress arc is open on the left, so
     * at 50 % it paints only the right half and reads as though it sits further
     * right than the dashed square that replaces it.
     *
     * It does not, and this is the check rather than the eye. `Overall progress` is
     * the first thing to the right of the ring/marker, so its left edge is exactly
     * where that node's width ends: identical across the three states means the
     * marker occupies the same horizontal box the ring did. Had the padding gone
     * *inside* the 56 dp instead of outside, this text would jump 36 dp left in one
     * card and the assertion above would catch nothing, because the card's height
     * is set by the text column in that case and would not move.
     */
    @Test
    fun theTextColumnStartsAtTheSamePlaceInAllThreeStates() {
        composeRule.setContent { Page(dark = false) { ThreeStates() } }

        val titles = composeRule.onAllNodesWithText("Overall progress", substring = true)
        titles.assertCountEquals(3)
        val lefts = (0..2).map { titles[it].getUnclippedBoundsInRoot().left.value }

        assertThat(lefts[1]).isWithin(0.05f).of(lefts[0])
        assertThat(lefts[2]).isWithin(0.05f).of(lefts[0])
    }

    /**
     * The caption names the population the number is a mean **over** — the defect
     * `f25cca5` fixed — asserted at the three counts that select its three branches.
     */
    @Test
    fun theCaptionNamesThePopulationTheAverageIsTakenOver() {
        composeRule.setContent { Page(dark = false) { ThreeStates() } }

        composeRule.onNodeWithText("Averaged across all your goals", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Averaged across the 1 goal that has a number", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("No goal has a number yet", substring = true)
            .assertIsDisplayed()

        // ⚠️ The negative that matters, and it is paired so it cannot pass
        // vacuously. What `#66` left behind was "all your goals" printed over a
        // mean taken across a subset; exactly ONE card on this page may say it,
        // and the positive assertion above is what proves the matcher still works
        // at all. A bare count-is-zero could report green on a broken matcher.
        composeRule.onAllNodesWithText("Averaged across all your goals", substring = true)
            .assertCountEquals(1)
    }

    /** Plural and singular are separate branches, and only one of them is grammatical. */
    @Test
    fun theCaptionCountsInWordsThatAgreeWithTheNumber() {
        composeRule.setContent {
            Page(dark = false) {
                Frame("2 of 3 measured") {
                    OverviewCard(
                        averageProgress = 0.4f,
                        goalCount = 3,
                        measuredGoalCount = 2,
                        doneTasks = 1,
                        completedThisWeek = 1,
                        onOpenAnalytics = {},
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Averaged across the 2 goals that have a number", substring = true)
            .assertIsDisplayed()
    }

    // -------------------------------------------------------------- the captures

    @Test
    fun theThreeStates_light() {
        composeRule.setContent { Page(dark = false) { ThreeStates() } }
        capture("issue-70-overview-light.png", lastFrameProbe = "No goal has a number yet")
    }

    @Test
    fun theThreeStates_dark() {
        // Not a duplicate of the light frame: the marker is a 1.5 dp dashed stroke
        // at `onSurfaceVariant` over the card's own surface, so whether it reads as
        // a deliberate placeholder is a contrast question, and `#66`'s own defect
        // was found in a dark render one ticket ago.
        composeRule.setContent { Page(dark = true) { ThreeStates() } }
        capture("issue-70-overview-dark.png", lastFrameProbe = "No goal has a number yet")
    }

    /**
     * **A fourth state the brief did not list, captured because it is reachable.**
     *
     * `DashboardScreen` renders `OverviewCard` unconditionally — the
     * `state.goals.isEmpty()` branch sits further down the same `LazyColumn` — so a
     * brand-new account with **no goals at all** takes the `measuredGoalCount == 0`
     * arm and is told *"No goal has a number yet"* beside a goal count of `0`.
     * Captured rather than argued about: re-deciding the wording is out of this
     * session's scope, and a PNG is what lets Ido decide whether it needs a ticket.
     */
    @Test
    fun theAccountWithNoGoalsAtAll() {
        composeRule.setContent {
            Page(dark = false) {
                Frame("no goals at all — reachable on a new account") {
                    OverviewCard(
                        averageProgress = 0f,
                        goalCount = 0,
                        measuredGoalCount = 0,
                        doneTasks = 0,
                        completedThisWeek = 0,
                        onOpenAnalytics = {},
                    )
                }
            }
        }
        capture("issue-70-overview-no-goals.png", lastFrameProbe = "No goal has a number yet")
    }

    // --------------------------------------------------------------- scaffolding

    /**
     * The three states of the brief's table, in its order, on one page.
     *
     * They share a page deliberately: the height claim is a **comparison**, and
     * three separate captures would each be individually plausible while differing
     * from one another. One bitmap is also what makes the difference visible to a
     * human without measuring anything.
     */
    @Composable
    private fun ThreeStates() {
        Frame("every goal has a number — unchanged") {
            Column(Modifier.testTag(TAG_ALL)) {
                OverviewCard(
                    averageProgress = 0.5f,
                    goalCount = 2,
                    measuredGoalCount = 2,
                    doneTasks = 4,
                    completedThisWeek = 2,
                    onOpenAnalytics = {},
                )
            }
        }
        Frame("some goals have none") {
            Column(Modifier.testTag(TAG_SOME)) {
                OverviewCard(
                    averageProgress = 0.4f,
                    goalCount = 2,
                    measuredGoalCount = 1,
                    doneTasks = 3,
                    completedThisWeek = 1,
                    onOpenAnalytics = {},
                )
            }
        }
        Frame("no goal has a number") {
            Column(Modifier.testTag(TAG_NONE)) {
                OverviewCard(
                    averageProgress = 0f,
                    goalCount = 2,
                    measuredGoalCount = 0,
                    doneTasks = 2,
                    completedThisWeek = 1,
                    onOpenAnalytics = {},
                )
            }
        }
    }

    @Composable
    private fun Frame(caption: String, content: @Composable () -> Unit) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(caption, style = MaterialTheme.typography.labelSmall)
            content()
        }
    }

    @Composable
    private fun Page(dark: Boolean, content: @Composable () -> Unit) {
        GoalPilotTheme(darkTheme = dark) {
            Surface {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { content() }
            }
        }
    }

    /**
     * `onRoot()` captures the activity window, so anything below the fold is simply
     * absent from the bitmap while `length()`, `width` and `height` stay perfectly
     * healthy — `kb/dev/look-at-your-own-output.md` §4g records this exact helper
     * shape passing over a capture that was missing the states it existed to show.
     * The probe names something from the **last** frame on the page, so a capture
     * that clipped anything fails here instead of shipping a PNG whose bottom third
     * is missing.
     */
    private fun capture(name: String, lastFrameProbe: String) {
        composeRule.onNodeWithText(lastFrameProbe, substring = true).assertIsDisplayed()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File(context.getExternalFilesDir(null), name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(700)
    }

    private companion object {
        const val TAG_ALL = "overview-all-measured"
        const val TAG_SOME = "overview-some-measured"
        const val TAG_NONE = "overview-none-measured"
    }
}
