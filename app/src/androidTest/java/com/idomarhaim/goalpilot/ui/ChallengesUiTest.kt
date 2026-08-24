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
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ScoreSource
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.feature.challenges.ChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.DiscoverChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.DiscoverableChallenge
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.ReportedBadge
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
        measure: Measure? = Measure(MeasureKind.DISTANCE, "km"),
        description: String = "",
    ) = Challenge(
        id = "c1",
        title = title,
        description = description,
        measure = measure,
        ownerUid = "me",
    )

    private fun card(
        phase: ChallengePhase,
        standings: List<ChallengeParticipant> = emptyList(),
        isOwner: Boolean = true,
        linkedGoalId: String = "",
    ) = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = challenge(),
            standings = standings.rankedByScore(currentUid = "me"),
            isOwner = isOwner,
            hasJoined = true,
            myLinkedGoalId = linkedGoalId,
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
                    onLinkGoal = {},
                onInvite = {},
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
                    onLinkGoal = {},
                onInvite = {},
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
                    onLinkGoal = {},
                onInvite = {},
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
                    onLinkGoal = {},
                onInvite = {},
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
                        metricWord = "km",
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
                        metricWord = "km",
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

    // ── §6: the card says how this challenge is scored ───────────────

    @Test
    fun myCard_offersScoringFromAGoalAndSaysItIsNotLinkedYet() {
        composeRule.setContent {
            GoalPilotTheme {
                MyChallengeCard(
                    card = card(ChallengePhase.ACTIVE),
                    onOpenStandings = {},
                    onReportScore = {},
                    onLinkGoal = {},
                onInvite = {},
                    onLeave = {},
                    onDelete = {},
                )
            }
        }

        // §6's path is the primary action; typing stays available for somebody with no
        // goal of the right kind yet.
        composeRule.onNodeWithText("Score from a goal").assertIsDisplayed()
        composeRule.onNodeWithText("Type a score").assertIsDisplayed()
        composeRule.onNodeWithText("Not linked yet — you are typing this score.")
            .assertIsDisplayed()
    }

    @Test
    fun myCard_saysWhenTheChallengeIsScoringItself() {
        composeRule.setContent {
            GoalPilotTheme {
                MyChallengeCard(
                    card = card(ChallengePhase.ACTIVE, linkedGoalId = "g1"),
                    onOpenStandings = {},
                    onReportScore = {},
                    onLinkGoal = {},
                onInvite = {},
                    onLeave = {},
                    onDelete = {},
                )
            }
        }

        // A linked challenge moves silently by design, so the one place it has to be
        // legible is the card of the person whose score it is.
        composeRule.onNodeWithText("Scoring itself from your linked goal.").assertIsDisplayed()
        composeRule.onNodeWithText("Change goal").assertIsDisplayed()
    }

    // ── §6 / Ido's third ask: a typed score says so, a derived one does not ──

    @Test
    fun standingsRow_saysWhoTypedTheScoreAndWhat() {
        composeRule.setContent {
            GoalPilotTheme {
                ReportedBadge(
                    standing = ChallengeParticipant(
                        uid = "other",
                        displayName = "Ann",
                        score = 8200.0,
                        source = ScoreSource.REPORTED,
                        reportedAtEpochMillis = 1_756_000_000_000L,
                    ).let { listOf(it).rankedByScore(currentUid = "me").single() },
                    metricWord = "steps",
                )
            }
        }

        // Factual and unarguable -- WHO, WHAT, WHEN -- and nothing that reads as an
        // accusation. `C4`'s register: the app never asserts an intrinsic edge by itself.
        composeRule.onNodeWithText("Reported by Ann", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("8200 steps", substring = true).assertIsDisplayed()
    }

    @Test
    fun standingsRow_saysNothingAtAllWhenTheScoreCameFromAGoal() {
        composeRule.setContent {
            GoalPilotTheme {
                ReportedBadge(
                    standing = ChallengeParticipant(
                        uid = "other",
                        displayName = "Ann",
                        score = 8200.0,
                        source = ScoreSource.DERIVED,
                    ).let { listOf(it).rankedByScore(currentUid = "me").single() },
                    metricWord = "steps",
                )
            }
        }

        // THE ABSENCE OF A BADGE IS THE HONEST DEFAULT. A challenge is meant to score
        // itself, so a derived row is the ordinary case; a badge on every row would be
        // noise, and only the exception speaks.
        composeRule.onNodeWithText("Reported by", substring = true).assertDoesNotExist()
    }
}
