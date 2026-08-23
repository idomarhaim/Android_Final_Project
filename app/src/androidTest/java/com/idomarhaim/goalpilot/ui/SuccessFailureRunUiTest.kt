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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.usecase.NextStepOffer
import com.idomarhaim.goalpilot.domain.usecase.NoNextStepGoal
import com.idomarhaim.goalpilot.domain.usecase.SuccessFailureRun
import com.idomarhaim.goalpilot.domain.usecase.SuccessRange
import com.idomarhaim.goalpilot.domain.usecase.SuccessWindow
import com.idomarhaim.goalpilot.domain.usecase.WindowOutcome
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.SuccessFailureRunCard
import com.idomarhaim.goalpilot.ui.components.TAG_ASYMMETRY
import com.idomarhaim.goalpilot.ui.components.TAG_KEPT
import com.idomarhaim.goalpilot.ui.components.TAG_MISSED
import com.idomarhaim.goalpilot.ui.components.TAG_NO_NEXT_STEP_FOOTER
import com.idomarhaim.goalpilot.ui.components.TAG_RUN_CARD
import com.idomarhaim.goalpilot.ui.components.TAG_WINDOW_IS
import com.idomarhaim.goalpilot.ui.components.offerTag
import com.idomarhaim.goalpilot.ui.components.rangeTag
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * `#64` — **`C19`'s success/failure run**, on a device, in both placements.
 *
 * ## What only a device can answer here
 *
 * The counting rules are JVM tests (`SuccessFailureRunTest`) and belong there. What is left
 * for this file is the half a pure function cannot hold:
 *
 * - **The negatives that define the component.** *Two numbers, **never a rate*** and *there is
 *   **no red** on this screen* are both statements about what is **absent**, and a negative is
 *   what a screenshot is worst at — a human scanning a PNG for something that should not be
 *   there is the reading that fails.
 * - **Where each sentence lives.** §4.7 puts the asymmetry note beside the time donut *"and
 *   nowhere else"*. That is a claim about **two placements at once**, so no single render
 *   shows it; it needs the same component asserted twice with the flag both ways.
 * - **That the run reads without colour.** §4.7's material contract distinguishes the four
 *   outcomes by **form**. Only a rendered frame can show whether it worked, which is what the
 *   PNGs are for and why Ido's eye is the instrument they are aimed at.
 *
 * ⚠️ **Run it with `install -r` + `am instrument`, never
 * `./gradlew :app:connectedDebugAndroidTest`**, which uninstalls the app and takes the external
 * files dir — and any Firebase sign-in — with it:
 *
 * ```bash
 * adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
 * adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb -s <serial> shell am instrument -w -e class \
 *     com.idomarhaim.goalpilot.ui.SuccessFailureRunUiTest \
 *     com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb -s <serial> pull \
 *     /storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/issue-64-area-light.png
 * ```
 *
 * §0.8 is suspended by this ticket's brief: **English only**.
 */
class SuccessFailureRunUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val today: LocalDate = LocalDate.of(2026, 8, 17)

    /**
     * The run is built here rather than through `BuildSuccessFailureRunUseCase`.
     *
     * The use case's arithmetic has its own 22 JVM tests; driving it from here would make every
     * assertion below depend on a second thing being right, and a frame that renders the wrong
     * data still renders. The component's contract is `SuccessFailureRun` in, pixels out, so
     * that is what is fed to it.
     */
    private fun run(
        pattern: String,
        range: SuccessRange = SuccessRange.EIGHT_WEEKS,
        noNextStep: List<NoNextStepGoal> = emptyList(),
    ): SuccessFailureRun {
        val windows = pattern.mapIndexed { index, char ->
            val to = today.minusWeeks((pattern.length - 1 - index).toLong())
            val outcome = when (char) {
                'k' -> WindowOutcome.KEPT
                'm' -> WindowOutcome.MISSED
                'o' -> WindowOutcome.STILL_OWED
                else -> WindowOutcome.NOTHING_DUE
            }
            SuccessWindow(
                from = to.minusDays(6),
                to = to,
                outcome = outcome,
                dueCount = if (outcome == WindowOutcome.NOTHING_DUE) 0 else 1,
                keptCount = if (outcome == WindowOutcome.KEPT) 1 else 0,
            )
        }
        return SuccessFailureRun(range = range, windows = windows, noNextStep = noNextStep)
    }

    /** The prototype's own patterns, so the frames are the ones `#41` was resolved against. */
    private val goodShape = "kkkmkkko"
    private val badShape = "mmmmmkmm"
    private val quietThenRhythm = "nmmkkkkk"

    @Composable
    private fun Card(
        run: SuccessFailureRun,
        showAsymmetryNote: Boolean = false,
        onOpenGoal: (String) -> Unit = {},
        onSelectRange: (SuccessRange) -> Unit = {},
    ) {
        SuccessFailureRunCard(
            run = run,
            onSelectRange = onSelectRange,
            onOpenGoal = onOpenGoal,
            showAsymmetryNote = showAsymmetryNote,
        )
    }

    // ── Two numbers, never a rate ─────────────────────────────────────────────────────────

    @Test
    fun thePairIsTwoNumbersAndThereIsNoRateAnywhere() {
        composeRule.setContent { GoalPilotTheme { Surface { Card(run(goodShape)) } } }

        // The positive half FIRST, and it is what stops the negative below passing vacuously.
        // Every number in this card goes through `bidiIsolated()` (§4.8), which wraps it in
        // U+2068/U+2069 -- so an exact match fails on a card that renders perfectly, and a
        // NEGATIVE exact match passes whether or not the thing is there. `substring = true`
        // throughout, and the positive control proves the matcher still fires.
        composeRule.onNodeWithTag(TAG_KEPT).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_MISSED).assertIsDisplayed()
        composeRule.onAllNodesWithText("kept", substring = true).onFirst().assertIsDisplayed()

        // §4.7: "Two numbers, never a rate ... Do not add one, even as a subtitle." A per-cent
        // sign is what one would look like, and there is not one on this card.
        composeRule.onAllNodesWithText("%", substring = true).assertCountEquals(0)
        // Nor the other shape a rate takes -- `6 of 8`, `6/8`.
        composeRule.onAllNodesWithText(" of ", substring = true).assertCountEquals(0)
    }

    // ── The sentence under the run is on the screen, not only in the spec ─────────────────

    @Test
    fun whatAWindowIsIsAnsweredOnTheScreen() {
        composeRule.setContent { GoalPilotTheme { Surface { Card(run(goodShape)) } } }

        // §4.7: "The numbers are meaningless without it, so it is not spec-only text."
        composeRule.onNodeWithTag(TAG_WINDOW_IS).assertIsDisplayed()
        composeRule
            .onNodeWithText("everything due in it was done", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun theLegendSaysStillOwedIsNotAFailure() {
        // The one state that must NOT read as a failure says so in words, because the shape
        // alone cannot carry a disclaimer.
        composeRule.setContent { GoalPilotTheme { Surface { Card(run(goodShape)) } } }

        composeRule.onNodeWithText("not a failure", substring = true).assertIsDisplayed()
    }

    @Test
    fun theLegendNamesOnlyTheStatesThatAreActuallyOnScreen() {
        // A legend entry for something absent is a word the user has to rule out. `badShape`
        // has no still-owed window and no nothing-due one.
        composeRule.setContent { GoalPilotTheme { Surface { Card(run(badShape)) } } }

        composeRule.onAllNodesWithText("missed", substring = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("not a failure", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("nothing due", substring = true).assertCountEquals(0)
    }

    // ── Every window says what it is out loud ─────────────────────────────────────────────

    @Test
    fun everyWindowCarriesItsOutcomeForAScreenReader() {
        // A dot has nothing for a screen reader to read, and §4.7 distinguishes the four by
        // FORM -- which is exactly what does not survive being read aloud. Counts are asserted
        // against `goodShape`: 6 kept, 1 missed, 1 still owed, 0 nothing-due.
        composeRule.setContent { GoalPilotTheme { Surface { Card(run(goodShape)) } } }

        composeRule.onAllNodesWithContentDescription("Kept,", substring = true)
            .assertCountEquals(6)
        composeRule.onAllNodesWithContentDescription("Missed,", substring = true)
            .assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription("Still owed,", substring = true)
            .assertCountEquals(1)
    }

    // ── The asymmetry sentence lives beside the donut AND NOWHERE ELSE ────────────────────

    @Test
    fun theAsymmetryNoteIsOnAnalyticsAndNotOnTheLifeAreaPlacement() {
        // §4.7's clause is about two placements at once, so it takes two renders of the same
        // component. Revision 1 of the prototype printed the note on every area frame, which
        // said the same thing twice and never where the two numbers actually meet.
        composeRule.setContent {
            GoalPilotTheme { Surface { Card(run(goodShape), showAsymmetryNote = false) } }
        }
        composeRule.onNodeWithTag(TAG_ASYMMETRY).assertDoesNotExist()
        composeRule.onAllNodesWithText("Only its minutes", substring = true).assertCountEquals(0)
    }

    @Test
    fun theAsymmetryNoteIsPresentOnTheAnalyticsPlacement() {
        composeRule.setContent {
            GoalPilotTheme { Surface { Card(run(goodShape), showAsymmetryNote = true) } }
        }

        composeRule.onNodeWithTag(TAG_ASYMMETRY).assertIsDisplayed()
        composeRule.onNodeWithText("in full in both", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Only its minutes", substring = true).assertIsDisplayed()
    }

    // ── A goal with nothing due is missing a step, not failing ────────────────────────────

    @Test
    fun eachNoNextStepGoalIsOfferedTheStepItIsActuallyMissing() {
        // §4.7's table: open work = 0 wants "Break it into steps"; work without dates wants
        // "Schedule the first one". The two rows are rendered together, because the frame that
        // matters is the one where they DIFFER -- rev 5 of the prototype deleted a duplicate
        // line precisely to stop these two being pushed off the bottom of the screen.
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Card(
                        run(
                            quietThenRhythm,
                            noNextStep = listOf(
                                NoNextStepGoal("g1", "Learn to cook", NextStepOffer.BREAK_IT_INTO_STEPS, 124),
                                NoNextStepGoal("g2", "Read 12 books this year", NextStepOffer.SCHEDULE_THE_FIRST_ONE, 35),
                            ),
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Break it into steps").assertIsDisplayed()
        composeRule.onNodeWithText("Schedule the first one").assertIsDisplayed()
        composeRule.onNodeWithText("Learn to cook", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("no next step", substring = true).onFirst()
            .assertIsDisplayed()
        // "idle 4 months" / "idle 5 weeks" -- the row's second half, and the unit is chosen by
        // size: 124 days reads in months, 35 in weeks.
        //
        // ⚠️ Counts, not `assertIsDisplayed`, and the reason is worth keeping. THE RANGE CHIPS
        // ARE ON THIS CARD TOO -- `6 months` and `8 weeks` -- so each word matches twice and a
        // single-node assertion fails on the ambiguity rather than on the thing it is checking.
        // Found by the run; invisible in the source, where the two live 200 lines apart.
        composeRule.onAllNodesWithText("months", substring = true).assertCountEquals(2)
        composeRule.onAllNodesWithText("weeks", substring = true).assertCountEquals(2)
        // Both rows carry an idle clause -- the half that would vanish if the unit picker
        // silently returned an empty string for one of them.
        composeRule.onAllNodesWithText("idle", substring = true).assertCountEquals(2)
    }

    @Test
    fun theFooterSaysTheseGoalsAreCountedInNeitherNumber() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Card(
                        run(
                            quietThenRhythm,
                            noNextStep = listOf(
                                NoNextStepGoal("g1", "Learn to cook", NextStepOffer.BREAK_IT_INTO_STEPS, null),
                            ),
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(TAG_NO_NEXT_STEP_FOOTER).assertIsDisplayed()
        composeRule.onNodeWithText("nothing can be missed", substring = true).assertIsDisplayed()
        // §4.7: "Let it go stays a command, never an inference." There is no such button here,
        // and the positive assertions above are what stop this negative passing vacuously.
        composeRule.onAllNodesWithText("Let it go", substring = true).assertCountEquals(0)
    }

    @Test
    fun anOfferOpensTheGoalItIsAbout() {
        var opened: String? = null
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Card(
                        run(
                            quietThenRhythm,
                            noNextStep = listOf(
                                NoNextStepGoal("g-cook", "Learn to cook", NextStepOffer.BREAK_IT_INTO_STEPS, 124),
                            ),
                        ),
                        onOpenGoal = { opened = it },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(offerTag(NextStepOffer.BREAK_IT_INTO_STEPS)).performClick()
        composeRule.runOnIdle { assertThat(opened).isEqualTo("g-cook") }
    }

    @Test
    fun anIdleCountIsOmittedEntirelyWhenNothingEverHappened() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Card(
                        run(
                            quietThenRhythm,
                            noNextStep = listOf(
                                NoNextStepGoal("g1", "Learn to cook", NextStepOffer.BREAK_IT_INTO_STEPS, null),
                            ),
                        ),
                    )
                }
            }
        }

        // `idle 0 days` would claim activity that never happened, so the half is dropped.
        composeRule.onAllNodesWithText("no next step", substring = true).onFirst()
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("idle", substring = true).assertCountEquals(0)
    }

    // ── The window is a filter you pick ───────────────────────────────────────────────────

    @Test
    fun allThreeWindowsAreOfferedAndTheDefaultIsEightWeeks() {
        var picked: SuccessRange? = null
        composeRule.setContent {
            GoalPilotTheme {
                Surface { Card(run(goodShape), onSelectRange = { picked = it }) }
            }
        }

        SuccessRange.entries.forEach { range ->
            composeRule.onNodeWithTag(rangeTag(range)).assertIsDisplayed()
        }
        composeRule.onNodeWithText("8 weeks").assertIsDisplayed()
        composeRule.onNodeWithText("30 days").assertIsDisplayed()
        composeRule.onNodeWithText("6 months").assertIsDisplayed()

        composeRule.onNodeWithTag(rangeTag(SuccessRange.SIX_MONTHS)).performClick()
        composeRule.runOnIdle { assertThat(picked).isEqualTo(SuccessRange.SIX_MONTHS) }
    }

    @Test
    fun anAreaWhereNothingWasEverDueSaysSoRatherThanShowingZeroAndZero() {
        // A run of dotted windows above `0 kept · 0 missed` is true and reads as a verdict.
        composeRule.setContent {
            GoalPilotTheme { Surface { Card(run("nnnnnnnn")) } }
        }

        composeRule.onNodeWithText("Nothing has been due here yet").assertIsDisplayed()
        // And the pair is not drawn at all -- the tags are the check, because a `0` would be
        // isolated and a text assertion for it is the vacuous kind.
        composeRule.onNodeWithTag(TAG_KEPT).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_MISSED).assertDoesNotExist()
        // The card itself is still there, so the assertion above is not passing on an empty
        // tree.
        composeRule.onNodeWithTag(TAG_RUN_CARD).assertIsDisplayed()
    }

    // ── the render pass ───────────────────────────────────────────────────────────────────

    @Composable
    private fun AreaFrames() {
        Framed("1 · LIFE AREA · good shape — two numbers, never a rate; the run is the record") {
            Card(run(goodShape))
        }
        Framed("2 · LIFE AREA · bad shape — same component, NO RED, and kept is still filled") {
            Card(run(badShape))
        }
    }

    @Composable
    private fun QuietFrame() {
        Framed("3 · LIFE AREA · almost nothing ever scheduled — the missing STEP, two offers") {
            Card(
                run(
                    quietThenRhythm,
                    noNextStep = listOf(
                        NoNextStepGoal("g1", "Learn to cook", NextStepOffer.BREAK_IT_INTO_STEPS, 124),
                        NoNextStepGoal("g2", "Read 12 books this year", NextStepOffer.SCHEDULE_THE_FIRST_ONE, 35),
                    ),
                ),
            )
        }
    }

    @Composable
    private fun AnalyticsFrame() {
        Framed("4 · ANALYTICS · the counterpart to the donut — C17's asymmetry, stated here only") {
            Card(run(goodShape), showAsymmetryNote = true)
        }
    }

    @Test
    fun theTwoAreaFrames_light() {
        composeRule.setContent { Page(dark = false) { AreaFrames() } }
        capture("issue-64-area-light.png", lastFrameProbe = "everything due in it was done")
    }

    @Test
    fun theTwoAreaFrames_dark() {
        // Dark is not a duplicate. §4.7's whole material argument is that outcome state is
        // carried by FORM and not hue, and a dark frame is where that claim is actually tested
        // -- `#65`'s defect was found in a dark render, and `C12`'s before it.
        composeRule.setContent { Page(dark = true) { AreaFrames() } }
        capture("issue-64-area-dark.png", lastFrameProbe = "everything due in it was done")
    }

    @Test
    fun theQuietAreaFrame_light() {
        composeRule.setContent { Page(dark = false) { QuietFrame() } }
        capture("issue-64-quiet-light.png", lastFrameProbe = "Schedule the first one")
    }

    @Test
    fun theQuietAreaFrame_dark() {
        composeRule.setContent { Page(dark = true) { QuietFrame() } }
        capture("issue-64-quiet-dark.png", lastFrameProbe = "Schedule the first one")
    }

    @Test
    fun theAnalyticsFrame_light() {
        composeRule.setContent { Page(dark = false) { AnalyticsFrame() } }
        capture("issue-64-analytics-light.png", lastFrameProbe = "Only its minutes are divided")
    }

    @Test
    fun theAnalyticsFrame_dark() {
        composeRule.setContent { Page(dark = true) { AnalyticsFrame() } }
        capture("issue-64-analytics-dark.png", lastFrameProbe = "Only its minutes are divided")
    }

    @Test
    fun theWidestWindow_light() {
        // 30 dots, which is the case that decides whether the row wraps legibly or turns into
        // a grey band. Nothing in the source says which; only the frame does.
        composeRule.setContent {
            Page(dark = false) {
                Framed("5 · 30 DAYS · the widest run, and the only test of whether it wraps") {
                    Card(run("kkmkkkkokkkmkkkkkkonkkkmkkkkk", range = SuccessRange.THIRTY_DAYS))
                }
            }
        }
        capture("issue-64-thirty-days-light.png", lastFrameProbe = "everything due in it was done")
    }

    @Composable
    private fun Framed(caption: String, content: @Composable () -> Unit) {
        GpCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(caption, style = MaterialTheme.typography.labelSmall)
                content()
            }
        }
    }

    @Composable
    private fun Page(dark: Boolean, content: @Composable () -> Unit) {
        GoalPilotTheme(darkTheme = dark) {
            Surface {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { content() }
            }
        }
    }

    /**
     * `onRoot()` captures the activity window, so anything below the fold is simply not in the
     * bitmap and the file is honest about itself while silent about its subject.
     *
     * [lastFrameProbe] names something from the **last** frame on the page, so a capture that
     * clipped anything fails here instead of shipping a PNG whose bottom third is missing —
     * `kb/dev/look-at-your-own-output.md` §4g, and `UnmeasuredPercentRenderTest` records the
     * same helper catching the same defect one ticket ago.
     */
    private fun capture(name: String, lastFrameProbe: String) {
        composeRule.onAllNodesWithText(lastFrameProbe, substring = true).onLast()
            .assertIsDisplayed()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File(context.getExternalFilesDir(null), name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(700)
    }
}
