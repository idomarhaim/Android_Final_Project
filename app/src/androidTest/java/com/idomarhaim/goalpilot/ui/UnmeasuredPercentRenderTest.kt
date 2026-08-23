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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.feature.analytics.AnalyticsUiState
import com.idomarhaim.goalpilot.feature.analytics.ProgressByGoalCard
import com.idomarhaim.goalpilot.feature.goals.GoalHeaderCard
import com.idomarhaim.goalpilot.feature.lifeareas.AreaGoalCard
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * `#66` — **the row that used to state a percentage it did not have**, on a device.
 *
 * ## What is actually being checked, and why a screenshot alone would not do it
 *
 * The defect `#65`'s render pass found is a *contradiction between two things on one
 * row*: the dashed-square marker meaning **no number yet** printed beside `0%` and
 * `0/100`. So the assertion that matters is a **negative** one — the percentage is
 * not there — and a negative is the half a PNG cannot carry, because a human
 * scanning a screenshot for something that should be missing is exactly the reading
 * that failed the first time round.
 *
 * The tests therefore assert the absence, and the PNG exists for the half assertions
 * are bad at: whether a row with its trailing figure removed reads as **deliberate**
 * or as **broken**. That judgement is Ido's and is why the file is written at all.
 *
 * ## The measured control is not padding
 *
 * Every frame below pairs an unmeasured goal with a measured one. A change that
 * removed the percentage from *every* row would pass a test that only looked at the
 * unmeasured case, and it would look fine in a screenshot of that case alone.
 *
 * ⚠️ **Run it with `install -r` + `am instrument`, never
 * `./gradlew :app:connectedDebugAndroidTest`**, which uninstalls the app and takes
 * the external files dir — and any Firebase sign-in — with it:
 *
 * ```bash
 * adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
 * adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb -s <serial> shell am instrument -w -e class \
 *     com.idomarhaim.goalpilot.ui.UnmeasuredPercentRenderTest \
 *     com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb -s <serial> pull \
 *     /storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/issue-66-light.png
 * ```
 *
 * §0.8 is suspended for this pass by the brief: **English only**.
 */
class UnmeasuredPercentRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** §1.3's default: a goal that says nothing about what it counts (`E6`). */
    private fun unmeasured(id: String, title: String, current: Double = 0.0) =
        Goal(id = id, title = title, currentValue = current, loggedEntryCount = 0)

    private fun measured(id: String, title: String, current: Double) = Goal(
        id = id,
        title = title,
        currentValue = current,
        targetValue = 100.0,
        measure = Measure(MeasureKind.COUNT, "sessions"),
    )

    // ------------------------------------------------------- the assertions

    /**
     * ⚠️ **Every matcher below is `substring = true`, and that is not a style choice.**
     *
     * `percentText()` and the meta line wrap their interpolated runs in
     * `bidiIsolated()` (§4.8), which inserts **U+2068** and **U+2069** — so the node
     * holds `⁨0%⁩`, which is byte-different from `"0%"` and pixel-identical to it.
     * An exact-match assertion therefore fails on a row that renders perfectly, and
     * `kb/dev/look-at-your-own-output.md` §5.4 records that exact failure in this
     * repo one ticket ago.
     *
     * **And a NEGATIVE assertion fails the other way, which is worse.**
     * `onAllNodesWithText("0%").assertCountEquals(0)` passes whether or not the
     * percentage is there, because the isolates defeat the match either way — a
     * guard that can never fire, reporting green. §5.4's incident was a positive
     * assertion, so it failed loudly; this direction is the silent one, which is
     * why each negative below is **paired with the same matcher succeeding on a
     * measured control in the same tree**. If the matcher stops working, the
     * positive half fails and the test says so.
     */
    @Test
    fun anUnmeasuredRowShowsNoPercentageWhileAMeasuredOneStillDoes() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Column {
                        GoalCard(
                            goal = unmeasured("g1", "Get fit").copy(loggedEntryCount = 11),
                            onClick = {},
                        )
                        // 55, NOT 40 — and the digits are load-bearing. `"40%"`
                        // CONTAINS `"0%"`, so a substring negative for the
                        // unmeasured row's `0%` would match the control and fail
                        // for the wrong reason. `55` shares no digit with `0`.
                        GoalCard(goal = measured("g2", "Drink 2 L a day", 55.0), onClick = {})
                    }
                }
            }
        }

        // ⚠️ **The whole claim in one line, and it cannot pass vacuously.**
        // Exactly ONE node in this tree carries a per-cent sign: the measured row.
        // If the unmeasured row starts printing one again the count is 2 and this
        // fails; if the fix over-reached and stripped the measured row too, the
        // count is 0 and this fails. A bare `assertCountEquals(0)` on the
        // unmeasured row could do neither.
        composeRule.onAllNodesWithText("%", substring = true).assertCountEquals(1)
        composeRule.onNodeWithText("55%", substring = true).assertIsDisplayed()

        // The meta line's ratio, the second figure `#65` found beside the marker.
        // `"55/100"` does not contain `"0/100"`, so the same care applies.
        composeRule.onNodeWithText("55/100", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("0/100", substring = true).assertCountEquals(0)
    }

    @Test
    fun theHonestCountStandsWhereThePercentageWas() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    GoalCard(
                        goal = unmeasured("g1", "Get fit").copy(loggedEntryCount = 11),
                        onClick = {},
                    )
                }
            }
        }

        // The count itself is isolated, so `"11 entries logged"` is NOT a substring
        // of what the node holds — `no number — ⁨11⁩ entries logged` is. Assert the
        // invariant frame here and the number itself on the JVM, where it is still a
        // plain value (`UnmeasuredPercentTest`). §5.4's rule, both halves.
        composeRule.onNodeWithText("no number", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("entries logged", substring = true).assertIsDisplayed()
    }

    @Test
    fun theMarkerIsStillThereAndCarriesTheClaimForAScreenReader() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface { GoalCard(goal = unmeasured("g1", "Get fit"), onClick = {}) }
            }
        }

        // The marker's only words: TalkBack-only, never rendered, and not isolated —
        // so this one is an exact match on purpose. It is also the check that a
        // screen reader still hears the claim the missing digit used to sit beside.
        composeRule
            .onNodeWithContentDescription("No number yet — nothing is owed here")
            .assertIsDisplayed()
    }

    @Test
    fun aGoalWithNothingLoggedSaysSoRatherThanCountingZero() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface { GoalCard(goal = unmeasured("g1", "Sleep before midnight"), onClick = {}) }
            }
        }

        // Deliberately not the plural's zero case: *"no number — 0 entries logged"*
        // states the absence twice and reads as a reproach, which is the opposite of
        // §1.3's *nothing is owed here*.
        //
        // The two strings are discriminated by their FRAME rather than by a number,
        // which is what makes the negative half able to fire: the unlogged copy is
        // `• no number yet` and the counted copy is `• no number — <n> entries
        // logged`, so `"logged"` appears in one and not the other.
        composeRule.onNodeWithText("no number yet", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("logged", substring = true).assertCountEquals(0)
    }

    @Test
    fun aChosenPercentMeasureKeepsItsPercentage() {
        // §7.1 keeps *chosen* and *defaulted* apart, and this is the row that proves
        // the fix branched on the right predicate: `"%"` as a deliberate measure is a
        // fact about the goal, and only `"%"` as a DEFAULT was ever the fiction.
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    GoalCard(
                        goal = Goal(
                            id = "g3",
                            title = "Renovate the flat",
                            currentValue = 45.0,
                            measure = Measure(MeasureKind.PERCENT, "%"),
                        ),
                        onClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("45%", substring = true).assertIsDisplayed()
        // …and it states it ONCE. The row used to read `45%` on the right and
        // `Other · 45/100 %` below — the same claim twice, which is what the
        // render pass showed while every assertion in this file passed. A
        // per-node query cannot see a relation between two marks, so the
        // assertion that pins it has to name the mark that should be absent.
        composeRule.onAllNodesWithText("45/100", substring = true).assertCountEquals(0)
    }

    // ------------------------------------------------------- the render pass

    // ------------------------- the other three surfaces the brief's Exit named

    /**
     * The **goal header**, the **life-area row** and the **analytics bar** — the
     * three surfaces `#66`'s brief asked the render pass to cover beside the list
     * row, and the three the first pass did not reach.
     *
     * All three were `private` in their own files. Widening them to `internal` is
     * the smallest honest way to render them: the alternatives were to drive the
     * real screens (Hilt, Firebase, seeded data — a different test) or to rebuild
     * approximations here, which would **exhibit something that is not the thing
     * under test** and is the failure `kb/dev/describing-is-not-exhibiting.md`
     * names. `GoalDetailScreen.kt` already exposes `AddTaskRow` this way.
     */
    @Composable
    private fun HeaderUnmeasured() {
        Framed("5 · GOAL HEADER · no measure — the marker at ring size, no ring") {
            GoalHeaderCard(
                isUnmeasured = true,
                restatesPercent = false,
                loggedEntryCount = 11,
                percent = 0,
                fraction = 0f,
                accentHex = "#4C6FFF",
                categoryLabel = "Other",
                lifeAreaNames = listOf("Health"),
                current = "11",
                target = "100",
                unit = "",
                description = "",
                fillAmounts = emptyList(),
                currentValue = 11.0,
                targetValue = 100.0,
                onFill = {},
                onLogProgress = {},
            )
        }
    }

    @Composable
    private fun HeaderMeasured() {
        Framed("6 · GOAL HEADER · CONTROL · measured, so the ring and the ratio stay") {
            GoalHeaderCard(
                isUnmeasured = false,
                restatesPercent = false,
                loggedEntryCount = 4,
                percent = 40,
                fraction = 0.4f,
                accentHex = "#4C6FFF",
                categoryLabel = "Other",
                lifeAreaNames = listOf("Health"),
                current = "40",
                target = "100",
                unit = "sessions",
                description = "",
                fillAmounts = emptyList(),
                currentValue = 40.0,
                targetValue = 100.0,
                onFill = {},
                onLogProgress = {},
            )
        }
    }

    @Composable
    private fun AreaRowAndChart() {
        Framed("7 · LIFE-AREA ROW · no measure above, measured below") {
            AreaGoalCard(
                goal = unmeasured("a1", "Get fit").copy(loggedEntryCount = 11),
                accent = MaterialTheme.colorScheme.primary,
                ink = MaterialTheme.colorScheme.primary,
                onClick = {},
                onRemove = {},
            )
            AreaGoalCard(
                goal = measured("a2", "Drink 2 L a day", 40.0),
                accent = MaterialTheme.colorScheme.primary,
                ink = MaterialTheme.colorScheme.primary,
                onClick = {},
                onRemove = {},
            )
        }
        Framed("8 · ANALYTICS · two unmeasured goals excluded, and the chart says so") {
            ProgressByGoalCard(
                state = AnalyticsUiState(
                    isLoading = false,
                    goals = listOf(
                        measured("b1", "Drink 2 L a day", 40.0),
                        measured("b2", "Read 12 books", 75.0),
                        unmeasured("b3", "Get fit"),
                        unmeasured("b4", "Sleep before midnight"),
                    ),
                    lifeAreas = listOf(LifeArea(id = "h", name = "Health")),
                ),
            )
        }
    }

    // ⚠️ **ONE header per capture, and the split is a finding rather than a
    // preference — the same finding `MeasureProposalUiTest` records one ticket
    // earlier, arrived at the same way.** Both headers on one page put the
    // control's `40/100 sessions` below the fold, and `onRoot()` captures the
    // window: the PNG simply would not have contained it. The difference this
    // time is that **the run said so** — `capture`'s subject probe failed on its
    // first execution, where the three container assertions under it all passed.
    // §4g's remedy, firing on §4g's defect.
    @Test
    fun theGoalHeaderWithNoMeasure_light() {
        composeRule.setContent { Page(dark = false) { HeaderUnmeasured() } }
        capture("issue-66-header-none-light.png", lastFrameProbe = "11 entries logged")
    }

    @Test
    fun theGoalHeaderWithNoMeasure_dark() {
        composeRule.setContent { Page(dark = true) { HeaderUnmeasured() } }
        capture("issue-66-header-none-dark.png", lastFrameProbe = "11 entries logged")
    }

    @Test
    fun theGoalHeaderControl_light() {
        composeRule.setContent { Page(dark = false) { HeaderMeasured() } }
        capture("issue-66-header-measured-light.png", lastFrameProbe = "40 / 100 sessions")  // spaces: the HEADER renders
            // `"$current / $target $unit"`, where the list ROW renders `40/100`.
            // Two formats, one ticket — the first draft of this probe copied the
            // row's and the run said 'component is not displayed' on a card that was
            // rendering perfectly, which is §5-family: recompute the string the
            // consumer sees, never the one you remember writing.
    }

    @Test
    fun theGoalHeaderControl_dark() {
        composeRule.setContent { Page(dark = true) { HeaderMeasured() } }
        capture("issue-66-header-measured-dark.png", lastFrameProbe = "40 / 100 sessions")  // spaces: the HEADER renders
            // `"$current / $target $unit"`, where the list ROW renders `40/100`.
            // Two formats, one ticket — the first draft of this probe copied the
            // row's and the run said 'component is not displayed' on a card that was
            // rendering perfectly, which is §5-family: recompute the string the
            // consumer sees, never the one you remember writing.
    }

    @Test
    fun theAreaRowAndTheChart_light() {
        composeRule.setContent { Page(dark = false) { AreaRowAndChart() } }
        capture("issue-66-area-chart-light.png", lastFrameProbe = "no number yet and are not charted")
    }

    @Test
    fun theAreaRowAndTheChart_dark() {
        composeRule.setContent { Page(dark = true) { AreaRowAndChart() } }
        capture("issue-66-area-chart-dark.png", lastFrameProbe = "no number yet and are not charted")
    }

    @Test
    fun theAnalyticsChartNamesTheGoalsItLeftOut() {
        // The footnote is the half that stops the exclusion being silent, and it
        // is the one thing on that card a screenshot alone would not confirm is
        // *correct* -- the count has to match the goals that were dropped.
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    ProgressByGoalCard(
                        state = AnalyticsUiState(
                            isLoading = false,
                            goals = listOf(
                                measured("b1", "Drink 2 L a day", 40.0),
                                unmeasured("b3", "Get fit"),
                                unmeasured("b4", "Sleep before midnight"),
                            ),
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithText("goals have no number yet", substring = true).assertIsDisplayed()
        // The charted goal is still charted -- the control that stops the whole
        // card silently emptying and this test passing on nothing.
        composeRule.onNodeWithText("Drink 2 L a day", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Get fit", substring = true).assertCountEquals(0)
    }

    @Test
    fun theRowsSideBySide_light() {
        composeRule.setContent { Page(dark = false) { Frames() } }
        capture("issue-66-light.png", lastFrameProbe = "Renovate the flat")
    }

    @Test
    fun theRowsSideBySide_dark() {
        // Dark is not a duplicate here: `#65`'s defect was FOUND in the dark render,
        // and the marker's whole argument is that it is distinguished by **form**
        // rather than hue, so a dark frame is where that claim is actually tested.
        composeRule.setContent { Page(dark = true) { Frames() } }
        capture("issue-66-dark.png", lastFrameProbe = "Renovate the flat")
    }

    @Composable
    private fun Frames() {
        Framed("1 · No measure, nothing logged — the marker carries the whole row") {
            GoalCard(goal = unmeasured("f1", "Sleep before midnight"), onClick = {})
        }
        Framed("2 · No measure, 11 entries — the honest count, where 0% used to be") {
            GoalCard(
                goal = unmeasured("f2", "Get fit").copy(loggedEntryCount = 11),
                onClick = {},
            )
        }
        Framed("3 · CONTROL · a measured goal, unchanged") {
            GoalCard(goal = measured("f3", "Drink 2 L a day", 40.0), onClick = {})
        }
        Framed("4 · CONTROL · percent CHOSEN as the measure, so it keeps its number") {
            GoalCard(
                goal = Goal(
                    id = "f4",
                    title = "Renovate the flat",
                    currentValue = 45.0,
                    measure = Measure(MeasureKind.PERCENT, "%"),
                ),
                onClick = {},
            )
        }
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
     * `onRoot()` captures the activity window, so anything below the fold is simply
     * not in the bitmap — `MeasureProposalUiTest` lost its control frame that way
     * and reported green. Four rows fit; the height assertion is what would notice
     * if a fifth were added and the page silently started clipping.
     */
    private fun capture(name: String, lastFrameProbe: String) {
        // ⚠️ `lastFrameProbe` is the floor, and the three lines under it are not.
        // `file.length()`, `width` and `height` all describe the CONTAINER, and
        // `kb/dev/look-at-your-own-output.md` §4g records this exact helper shape
        // passing against a capture missing two of the five states it existed to
        // show — an unscrollable `Column` renders its overflow at zero height, so
        // the bitmap is honest about itself and silent about the subject. The
        // probe names something from the **last** frame on the page, so a capture
        // that clipped anything fails here instead of shipping a PNG whose
        // bottom third is missing.
        composeRule.onNodeWithText(lastFrameProbe, substring = true).assertIsDisplayed()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File(context.getExternalFilesDir(null), name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(700)
    }
}
