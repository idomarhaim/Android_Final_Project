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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.prefs.AppPreferencesRepositoryImpl
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalStructure
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureBasis
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
import com.idomarhaim.goalpilot.domain.model.ProposalOrigin
import com.idomarhaim.goalpilot.domain.model.TargetSource
import com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase
import com.idomarhaim.goalpilot.feature.goals.MeasureOffer
import com.idomarhaim.goalpilot.feature.goals.MeasureProposalTags
import com.idomarhaim.goalpilot.feature.goals.UnmeasuredNote
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * §1.3's two surfaces on a real device — spec §1.3, §3.4 (`C22`
 * [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
 * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)).
 *
 * Three things, and only the first is an ordinary UI test:
 *
 *  1. **The behaviour** — the note stands above the offer, the two buttons exist,
 *     and a `USER` proposal says the number is the user's rather than showing a
 *     blank.
 *  2. **The permanence** — §1.3's *dismissal is permanent, not snoozed*, proved
 *     against a real `SharedPreferences` file re-read through a **fresh repository
 *     instance**. That is what process death is from this feature's point of view:
 *     the object is gone, the file is not. It cannot be a JVM test — this project
 *     has no Robolectric, and faking the store would test the fake.
 *  3. **The render pass** — a PNG a human opens, because #65's acceptance criterion
 *     is *tone*, and no assertion has ever caught a nag.
 *
 * ⚠️ **Run it with `install -r` + `am instrument`, never
 * `./gradlew :app:connectedDebugAndroidTest`**, which uninstalls the app and takes
 * the external files dir — and any Firebase sign-in — with it:
 *
 * ```bash
 * adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
 * adb -s <serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb -s <serial> shell am instrument -w -e class \
 *     com.idomarhaim.goalpilot.ui.MeasureProposalUiTest \
 *     com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb -s <serial> pull \
 *     /storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/issue-65-measure-proposal.png
 * ```
 *
 * §0.8 is suspended for this pass by #65's brief: **English only**.
 */
class MeasureProposalUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun proposal(
        source: TargetSource,
        target: Double?,
        origin: ProposalOrigin = ProposalOrigin.MECHANICAL,
        word: String = "steps",
        basis: MeasureBasis = MeasureBasis.OUTCOME,
    ) = MeasureProposal(
        goalId = "g1",
        kind = MeasureKind.COUNT,
        word = word,
        basis = basis,
        targetSource = source,
        target = target,
        origin = origin,
    )

    // ── 1 · Behaviour ───────────────────────────────────────────────────────────

    @Test
    fun theAbsenceIsStatedBeforeAnythingIsOffered() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Column {
                        UnmeasuredNote()
                        MeasureOffer(
                            proposal = proposal(TargetSource.STEPS, 8.0),
                            onAccept = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }

        // The line that stops the offer reading as a correction of a deliberate
        // choice. Both sentences, because the second is the load-bearing one.
        composeRule.onNodeWithText("No number on this one.").assertIsDisplayed()
        composeRule
            .onNodeWithText("That is a choice, and it stays one — nothing here is incomplete.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MeasureProposalTags.NOTE).assertIsDisplayed()
        composeRule.onNodeWithTag(MeasureProposalTags.OFFER).assertIsDisplayed()
    }

    @Test
    fun theOfferCarriesExactlyTwoActionsAndNeitherIsASnooze() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    MeasureOffer(proposal = proposal(TargetSource.STEPS, 8.0), onAccept = {}, onDismiss = {})
                }
            }
        }

        composeRule.onNodeWithTag(MeasureProposalTags.ACCEPT).assertIsDisplayed()
        composeRule.onNodeWithTag(MeasureProposalTags.DISMISS).assertIsDisplayed()
        composeRule.onNodeWithText("Use this").assertIsDisplayed()
        composeRule.onNodeWithText("Not for this goal").assertIsDisplayed()
        // §1.3 makes dismissal permanent, not snoozed. The prototype's copy table
        // carries a "Not now" string its own frames never render, and this is the
        // assertion that keeps it unrendered here.
        composeRule.onNodeWithText("Not now").assertDoesNotExist()
    }

    @Test
    fun aUserSourcedProposalSaysTheNumberIsYoursRatherThanShowingABlank() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    MeasureOffer(
                        proposal = proposal(
                            TargetSource.USER,
                            target = null,
                            origin = ProposalOrigin.MODEL,
                            word = "kg lost",
                        ),
                        onAccept = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // §3.3 E forbids the model supplying a target. Saying so plainly is what
        // turns a missing number from a gap into a policy the user can agree with.
        composeRule
            .onNodeWithText(
                "Nothing to compute from, so the target is yours to set. " +
                    "The app will not invent one, and the model is not asked for one.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun noProposalRendersNoOfferWhileTheNoteStillStands() {
        // The prototype's frame 5: the app saying nothing, on a real screen, which
        // is the only way to see whether it reads as deliberate or as broken.
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Column {
                        UnmeasuredNote()
                        MeasureOffer(proposal = null, onAccept = {}, onDismiss = {})
                    }
                }
            }
        }

        composeRule.onNodeWithTag(MeasureProposalTags.NOTE).assertIsDisplayed()
        composeRule.onNodeWithTag(MeasureProposalTags.OFFER).assertDoesNotExist()
    }

    @Test
    fun pressingEitherButtonReportsItExactlyOnce() {
        var accepted = 0
        var dismissed = 0
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    MeasureOffer(
                        proposal = proposal(TargetSource.STEPS, 8.0),
                        onAccept = { accepted++ },
                        onDismiss = { dismissed++ },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(MeasureProposalTags.ACCEPT).performClick()
        composeRule.onNodeWithTag(MeasureProposalTags.DISMISS).performClick()
        composeRule.waitForIdle()

        assertThat(accepted).isEqualTo(1)
        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun theMarkerAppearsOnAnUnmeasuredRowAndNotOnAMeasuredOne() {
        composeRule.setContent {
            GoalPilotTheme {
                Surface {
                    Column {
                        GoalCard(goal = Goal(id = "g1", title = "Get fit", measure = null), onClick = {})
                        GoalCard(
                            goal = Goal(
                                id = "g2",
                                title = "Drink water",
                                measure = Measure(MeasureKind.VOLUME, "L"),
                                targetValue = 4.0,
                            ),
                            onClick = {},
                        )
                    }
                }
            }
        }

        // The marker's only words are its TalkBack line, so that is what identifies
        // it — and asserting there is exactly ONE is the half that matters: a marker
        // on the measured row would be the app stating something false.
        val markers = composeRule
            .onAllNodesWithContentDescription("No number yet", substring = true)
            .fetchSemanticsNodes()
        assertThat(markers).hasSize(1)
    }

    // ── 2 · Dismissal is permanent, across process death ────────────────────────

    @Test
    fun aDismissedGoalNeverOffersAgain_evenAfterTheProcessDies() {
        // A prefs file of this test's own, so it cannot inherit or corrupt the real
        // one. It is cleared first, because a previous run of this test is itself a
        // "previous process" and would make the assertion pass for the wrong reason.
        context.getSharedPreferences("goalpilot_ui_prefs", Context.MODE_PRIVATE)
            .edit().remove("measure_proposal_dismissed").commit()

        val before = AppPreferencesRepositoryImpl(context)
        assertThat(before.isMeasureProposalDismissed("g1")).isFalse()
        before.dismissMeasureProposal("g1")
        assertThat(before.isMeasureProposalDismissed("g1")).isTrue()

        // THE assertion. A brand-new repository instance holds none of the first
        // one's memory — every byte it reports comes off disk, which is exactly what
        // survives process death. A snooze would have needed a timestamp to expire,
        // and there is no field here that could hold one.
        val afterRestart = AppPreferencesRepositoryImpl(context)
        assertThat(afterRestart.isMeasureProposalDismissed("g1")).isTrue()

        // Per goal, not global: dismissing one must not silence the rest.
        assertThat(afterRestart.isMeasureProposalDismissed("g2")).isFalse()

        // And a second goal joins rather than replacing — the set only grows.
        afterRestart.dismissMeasureProposal("g2")
        val third = AppPreferencesRepositoryImpl(context)
        assertThat(third.isMeasureProposalDismissed("g1")).isTrue()
        assertThat(third.isMeasureProposalDismissed("g2")).isTrue()

        context.getSharedPreferences("goalpilot_ui_prefs", Context.MODE_PRIVATE)
            .edit().remove("measure_proposal_dismissed").commit()
    }

    @Test
    fun eligibilityDoesNotDependOnTheDismissalStore() {
        // The two are deliberately separate questions (see ProposeMeasureUseCase):
        // a dismissed goal is still *eligible*, it is simply never asked about. Were
        // they folded together, removing a measure would silently un-dismiss.
        val goal = Goal(id = "g1", title = "Get fit", measure = null)
        val structure = GoalStructure(openStepCount = 3, totalStepCount = 3)
        val prefs = AppPreferencesRepositoryImpl(context)
        prefs.dismissMeasureProposal("g1")

        assertThat(ProposeMeasureUseCase.isEligible(goal, structure)).isTrue()
        assertThat(prefs.isMeasureProposalDismissed("g1")).isTrue()

        context.getSharedPreferences("goalpilot_ui_prefs", Context.MODE_PRIVATE)
            .edit().remove("measure_proposal_dismissed").commit()
    }

    // ── 3 · The render pass ─────────────────────────────────────────────────────

    /**
     * **Two PNGs, not one, and the split is a finding rather than a preference.**
     *
     * The first attempt put all five states in one column and captured
     * `onRoot()` — which is the activity window, so the bottom of the column was
     * simply **not in the bitmap**. What fell off was frame 5, *the app offering
     * nothing*, which is the single frame §10.1's argument turns on: an offer
     * looks perfectly reasonable alone and can still look pushy beside the app
     * minding its own business. A capture that silently drops the control frame is
     * worse than no capture, because it reports green.
     *
     * So each PNG holds a comparison that fits, and the second one carries the
     * silent state **beside** a live offer rather than on its own.
     */
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
    private fun Page(dark: Boolean = false, content: @Composable () -> Unit) {
        GoalPilotTheme(darkTheme = dark) {
            Surface {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { content() }
            }
        }
    }

    private fun capture(name: String, minHeight: Int) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File(context.getExternalFilesDir(null), name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(out.length()).isGreaterThan(1_000L)
        assertThat(bitmap.width).isGreaterThan(300)
        assertThat(bitmap.height).isGreaterThan(minHeight)
    }

    @Test
    fun theMarkerAndTheTwoMechanicalOffers_writtenToAPng() {
        composeRule.setContent {
            Page {
                Framed("1 · The marker, in a list — no words, no buttons") {
                    GoalCard(goal = Goal(id = "g1", title = "Get fit", measure = null), onClick = {})
                }
                Framed("2 · Mechanical · steps — no model behind it") {
                    UnmeasuredNote()
                    MeasureOffer(
                        proposal = proposal(TargetSource.STEPS, 8.0),
                        onAccept = {},
                        onDismiss = {},
                    )
                }
                Framed("3 · Mechanical · schedule — a leading indicator") {
                    MeasureOffer(
                        proposal = proposal(
                            TargetSource.SCHEDULE,
                            3.0,
                            word = "a week",
                            basis = MeasureBasis.LEADING,
                        ),
                        onAccept = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // A floor under the capture: the two states that must never be confused
        // have to be really on it, and the leading-indicator tag with them.
        composeRule.onNodeWithText("Count the steps you already listed").assertExists()
        composeRule.onNodeWithText("Count the occurrences you already schedule").assertExists()
        composeRule.onNodeWithText("leading indicator").assertExists()

        capture("issue-65-measure-proposal-a.png", minHeight = 400)
    }

    @Test
    fun theModelOfferBesideTheSilentState_writtenToAPng() {
        composeRule.setContent {
            Page {
                Framed("4 · Model · no target to compute — the number is yours") {
                    MeasureOffer(
                        proposal = proposal(
                            TargetSource.USER,
                            null,
                            origin = ProposalOrigin.MODEL,
                            word = "kg lost",
                        ),
                        onAccept = {},
                        onDismiss = {},
                    )
                }
                Framed("5 · Nothing to offer — the app says only that it is legal") {
                    UnmeasuredNote()
                    MeasureOffer(proposal = null, onAccept = {}, onDismiss = {})
                }
            }
        }
        composeRule.waitForIdle()

        // `substring`, and NOT the whole sentence: the word is wrapped in bidi
        // isolate characters (U+2068/U+2069) because it is user-visible content of
        // unknown script (§4.8), so the rendered string is *not* the one this file
        // spells. An exact match here failed on the first run and read as *the
        // title never rendered*. The stable half is the frame, which carries no
        // isolates; the word itself is asserted by the JVM suite where it is a
        // plain value rather than a rendered one.
        composeRule.onNodeWithText("Measure it in", substring = true).assertExists()
        // THE assertion this whole split exists for: the silent state is on the
        // bitmap, so the offer above it can be judged against the app saying nothing.
        composeRule.onNodeWithText("No number on this one.").assertIsDisplayed()
        composeRule.onNodeWithTag(MeasureProposalTags.OFFER).assertIsDisplayed()

        capture("issue-65-measure-proposal-b.png", minHeight = 300)
    }

    /**
     * The marker on a dark ground, because this file's own KDoc claims it survives
     * there and a claim about a *render* is not settled by reading the source.
     *
     * The claim is specifically that it is distinguished by **form** and not by
     * hue — a dashed square where every other marker in this language is a circle —
     * so the thing to look at is whether the square still reads as a square and
     * whether the dashes survive at 16 dp against a dark surface. Both markers are
     * put beside a measured row, so the comparison is the one a list actually
     * offers: a row with a number slot and a row without.
     */
    @Test
    fun theMarkerOnADarkGround_writtenToAPng() {
        composeRule.setContent {
            Page(dark = true) {
                Framed("Dark · unmeasured, then measured") {
                    GoalCard(goal = Goal(id = "g1", title = "Get fit", measure = null), onClick = {})
                    GoalCard(
                        goal = Goal(
                            id = "g2",
                            title = "Drink water",
                            measure = Measure(MeasureKind.VOLUME, "L"),
                            targetValue = 4.0,
                            currentValue = 1.0,
                        ),
                        onClick = {},
                    )
                }
                Framed("Dark · the note and an offer") {
                    UnmeasuredNote()
                    MeasureOffer(
                        proposal = proposal(TargetSource.STEPS, 8.0),
                        onAccept = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Exactly one marker, on the unmeasured row only — the same assertion the
        // light test makes, because a theme must not change what is *true*.
        assertThat(
            composeRule
                .onAllNodesWithContentDescription("No number yet", substring = true)
                .fetchSemanticsNodes(),
        ).hasSize(1)

        capture("issue-65-measure-proposal-dark.png", minHeight = 300)
    }
}
