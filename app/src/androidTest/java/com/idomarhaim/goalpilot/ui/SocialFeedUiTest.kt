package com.idomarhaim.goalpilot.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.SharedItem
import com.idomarhaim.goalpilot.feature.social.DeletePostDialog
import com.idomarhaim.goalpilot.feature.social.FeedCard
import com.idomarhaim.goalpilot.feature.social.FullScreenPhotoDialog
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI (Compose) layer test for the Social feed card — issues `#4` and `#5`.
 *
 * Both bugs were reported the same way: the accessibility tree showed **zero
 * interactive nodes in the entire feed card**, and the screen was pixel-identical
 * after every tap. So the first test below is deliberately the crudest one in the
 * repo — it counts clickable nodes — because that count is the literal text of
 * both reproductions, and a fix that leaves it at zero has not landed whatever
 * else it changed.
 *
 * The card is driven straight from a [SharedItem], so this needs no Firebase and
 * no Hilt — the same shape as [ChallengesUiTest].
 */
class SocialFeedUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun item(
        imageUrl: String? = "https://example.com/photo.jpg",
        isMine: Boolean = true,
        authorName: String = "Ido",
    ) = SharedItem(
        id = "share1",
        authorUid = "me",
        authorName = authorName,
        headline = "Weekly progress",
        message = "Earned 120 pts • 4 tasks done",
        points = 120,
        completedTasks = 4,
        imageUrl = imageUrl,
        createdAtEpochMillis = 1_700_000_000_000L,
        isMine = isMine,
    )

    private fun card(
        item: SharedItem = item(),
        onOpenPhoto: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeRule.setContent {
            GoalPilotTheme {
                FeedCard(item = item, onOpenPhoto = onOpenPhoto, onDelete = onDelete)
            }
        }
    }

    // ── The reproduction, inverted ───────────────────────────────────

    @Test
    fun theCardIsNoLongerAWallOfNothing() {
        card()

        // Was 0 in both reports. Now: the photo, the overflow button.
        val clickable = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertThat(clickable).hasSize(2)
    }

    // ── #4: the photo opens, and is announced ────────────────────────

    @Test
    fun thePhotoIsAnnouncedToAScreenReader() {
        card()

        // It used to pass contentDescription = null, the API's way of saying
        // "decorative" — so the post was announced with the picture missing.
        composeRule.onNodeWithContentDescription("Photo shared by Ido").assertIsDisplayed()
    }

    @Test
    fun anAuthorWithNoNameStillGivesThePhotoAUsableLabel() {
        card(item = item(authorName = ""))

        composeRule.onNodeWithContentDescription("Photo shared by GoalPilot user")
            .assertIsDisplayed()
    }

    @Test
    fun tappingThePhotoOpensIt() {
        var opened = 0
        card(onOpenPhoto = { opened++ })

        composeRule.onNodeWithContentDescription("Photo shared by Ido").performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun aPostWithNoPhotoHasNoPhotoToOpen() {
        card(item = item(imageUrl = null))

        // Only the overflow button remains — no phantom tap target where the
        // image would have been.
        assertThat(composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()).hasSize(1)
    }

    // ── #5: deleting your own post ───────────────────────────────────

    @Test
    fun yourOwnPostOffersToDeleteItself() {
        var deleted = 0
        card(onDelete = { deleted++ })

        composeRule.onNodeWithContentDescription("Post options").performClick()
        composeRule.onNodeWithText("Delete post").performClick()

        assertThat(deleted).isEqualTo(1)
    }

    @Test
    fun someoneElsesPostDoesNotOfferADeleteTheRulesWouldRefuse() {
        card(item = item(isMine = false))

        // firestore.rules scopes delete to the author, so a menu here would offer
        // an action the backend is guaranteed to deny.
        composeRule.onAllNodesWithContentDescription("Post options").assertCountEquals(0)
    }

    @Test
    fun theConfirmationSaysThePhotoGoesToo() {
        composeRule.setContent {
            GoalPilotTheme {
                DeletePostDialog(item = item(), onConfirm = {}, onDismiss = {})
            }
        }

        // "Delete" on a text post does not imply the image is deleted with it.
        composeRule.onNodeWithText("attached photo", substring = true).assertIsDisplayed()
    }

    @Test
    fun aTextOnlyPostIsNotPromisedAPhotoDeletion() {
        composeRule.setContent {
            GoalPilotTheme {
                DeletePostDialog(item = item(imageUrl = null), onConfirm = {}, onDismiss = {})
            }
        }

        composeRule.onAllNodesWithText("attached photo", substring = true).assertCountEquals(0)
        composeRule.onNodeWithText("This cannot be undone.", substring = true).assertIsDisplayed()
    }

    // ── The destination the photo opens into ─────────────────────────

    @Test
    fun theFullScreenViewerCanBeClosedAndKeepsTheLabel() {
        var dismissed = 0
        composeRule.setContent {
            GoalPilotTheme {
                FullScreenPhotoDialog(
                    imageUrl = "https://example.com/photo.jpg",
                    contentDescription = "Photo shared by Ido",
                    onDismiss = { dismissed++ },
                )
            }
        }

        // The viewer must not re-commit #4's second fault on its own image.
        composeRule.onNodeWithContentDescription("Photo shared by Ido").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close photo").performClick()

        assertThat(dismissed).isEqualTo(1)
    }
}
