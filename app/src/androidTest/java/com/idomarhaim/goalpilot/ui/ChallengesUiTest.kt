package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengeType
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.feature.challenges.ChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.DiscoverChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.DiscoverableChallenge
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.ScoreEntryDialog
import com.idomarhaim.goalpilot.feature.challenges.ScoreEntryState
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the challenge cards.
 *
 * The cards are driven straight from a [ChallengeCard], so this needs no Firebase
 * and no Hilt — the same shape as [LifeAreaReorderUiTest]. What it pins is the
 * link between a challenge's *phase* and what the card lets you do about it: the
 * phase arithmetic itself is covered on the JVM by `ChallengeStandingsTest` and
 * `ChallengesViewModelTest`.
 */
class ChallengesUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun challenge(
        title: String = "Most km this week",
        metricUnit: String = "km",
        description: String = "",
    ) = Challenge(
        id = "c1",
        title = title,
        description = description,
        type = ChallengeType.RUNNING,
        metricUnit = metricUnit,
        ownerUid = "me",
    )

    private fun card(
        phase: ChallengePhase,
        standings: List<ChallengeParticipant> = emptyList(),
        isOwner: Boolean = true,
    ) = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = challenge(),
            standings = standings.rankedByScore(currentUid = "me"),
            isOwner = isOwner,
            hasJoined = true,
        ),
        phase = phase,
    )

    // ── Discover ─────────────────────────────────────────────────────

    @Test
    fun discoverCard_offersJoinWhileTheChallengeCanStillBeJoined() {
        var joined = 0
        composeRule.setContent {
            GoalPilotTheme {
                DiscoverChallengeCard(
                    entry = DiscoverableChallenge(challenge(), ChallengePhase.ACTIVE),
                    onJoin = { joined++ },
                )
            }
        }

        composeRule.onNodeWithText("Join").assertIsEnabled().performClick()

        assertThat(joined).isEqualTo(1)
    }

    @Test
    fun discoverCard_stillShowsAnEndedChallengeButWillNotJoinIt() {
        var joined = 0
        composeRule.setContent {
            GoalPilotTheme {
                DiscoverChallengeCard(
                    entry = DiscoverableChallenge(challenge(), ChallengePhase.ENDED),
                    onJoin = { joined++ },
                )
            }
        }

        // Disabled rather than absent: a card with no action reads as a broken row.
        // The phase is said once, on the chip — the button keeps its own verb, or
        // the card would carry the word "Ended" twice and read as two states.
        composeRule.onNodeWithText("Most km this week").assertIsDisplayed()
        composeRule.onNodeWithText("Ended").assertIsDisplayed()
        composeRule.onNodeWithText("Join").assertIsNotEnabled().performClick()
        assertThat(joined).isEqualTo(0)
    }

    @Test
    fun discoverCard_showsAnUpcomingChallengeAsJoinable() {
        composeRule.setContent {
            GoalPilotTheme {
                DiscoverChallengeCard(
                    entry = DiscoverableChallenge(challenge(), ChallengePhase.UPCOMING),
                    onJoin = {},
                )
            }
        }

        // Joining before the start is the point of an upcoming challenge.
        composeRule.onNodeWithText("Join").assertIsEnabled()
    }

    // ── Your challenges ──────────────────────────────────────────────

    @Test
    fun myCard_saysWhyScoringIsClosedBeforeTheChallengeStarts() {
        composeRule.setContent {
            GoalPilotTheme {
                MyChallengeCard(
                    card = card(ChallengePhase.UPCOMING),
                    onOpenStandings = {},
                    onReportScore = {},
                    onLeave = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Scores open when the challenge starts.").assertIsDisplayed()
    }

    @Test
    fun myCard_saysTheStandingsAreFinalOnceItIsOver() {
        composeRule.setContent {
            GoalPilotTheme {
                MyChallengeCard(
                    card = card(ChallengePhase.ENDED),
                    onOpenStandings = {},
                    onReportScore = {},
                    onLeave = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("This challenge is over — the standings are final.")
            .assertIsDisplayed()
    }

    @Test
    fun myCard_showsYourOwnRankAndScoreInTheChallengesUnit() {
        composeRule.setContent {
            GoalPilotTheme {
                MyChallengeCard(
                    card = card(
                        phase = ChallengePhase.ACTIVE,
                        standings = listOf(
                            ChallengeParticipant(uid = "me", displayName = "Ido", score = 12.5),
                            ChallengeParticipant(uid = "other", displayName = "Ann", score = 20.0),
                        ),
                    ),
                    onOpenStandings = {},
                    onReportScore = {},
                    onLeave = {},
                    onDelete = {},
                )
            }
        }

        // Second on 12.5 km — and 12.5 keeps its decimal where a whole number would not.
        composeRule.onNodeWithText("#2 · 12.5 km", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("2 people in", substring = true).assertIsDisplayed()
    }

    @Test
    fun myCard_opensTheStandingsFromItsOwnButton() {
        var opened = 0
        composeRule.setContent {
            GoalPilotTheme {
                MyChallengeCard(
                    card = card(ChallengePhase.ACTIVE),
                    onOpenStandings = { opened++ },
                    onReportScore = {},
                    onLeave = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Standings").performClick()

        assertThat(opened).isEqualTo(1)
    }

    // ── Reporting a score ────────────────────────────────────────────

    @Test
    fun scoreDialog_labelsTheFieldWithTheChallengesOwnUnit() {
        composeRule.setContent {
            GoalPilotTheme {
                ScoreEntryDialog(
                    state = ScoreEntryState(
                        isVisible = true,
                        challengeTitle = "Most km this week",
                        metricUnit = "km",
                        value = "12.5",
                    ),
                    onValue = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Total km").assertIsDisplayed()
        composeRule.onNodeWithText("12.5").assertIsDisplayed()
        // The one thing a competitor needs to know before typing.
        composeRule.onNodeWithText("This replaces your total, it does not add to it.")
            .assertIsDisplayed()
    }

    @Test
    fun scoreDialog_showsTheRefusalAndKeepsTheTypedValue() {
        composeRule.setContent {
            GoalPilotTheme {
                ScoreEntryDialog(
                    state = ScoreEntryState(
                        isVisible = true,
                        challengeTitle = "Most km this week",
                        metricUnit = "km",
                        value = "lots",
                        error = "Enter a number",
                    ),
                    onValue = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Enter a number").assertIsDisplayed()
        composeRule.onNodeWithText("lots").assertIsDisplayed()
    }
}
