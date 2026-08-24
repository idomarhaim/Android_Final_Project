package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.feature.challenges.ChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.MeasureChangeContent
import com.idomarhaim.goalpilot.feature.challenges.MeasureChangeState
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for §3 — the measure-change approval flow — and **not a test, a camera.**
 *
 * `ChallengesViewModelTest` and `functions/test/measureChange.test.mjs` prove the quorum
 * arithmetic, and `firestore-tests/rules.test.mjs` proves the owner can no longer edit the
 * live measure at all. The question none of them can answer:
 *
 * > **Does "every score restarts at zero" arrive in time to stop somebody doing it by
 * > accident?**
 *
 * That is the whole risk of this feature. A measure change is the one action in the app
 * that destroys other people's numbers, and the design's answer is *say the consequence
 * before anybody is asked, in the error colour, in full sentences*. Whether that actually
 * reads as a warning — or as more grey text under a form — is only visible in a picture.
 *
 * ## What each frame is composed to expose
 *
 * - **`banner-reset`** — what a **participant** sees: somebody else has asked, and agreeing
 *   costs them their score. The alarming case, on the card, with the Agree button beside
 *   it. If the red sentence does not outweigh the button, the design has failed.
 * - **`banner-relabel-agreed`** — the **owner's** view of a harmless change they have
 *   already agreed to: no button, a Withdraw, and the reassuring wording. Photographed
 *   beside the first so the two are legibly different — a banner that looked the same
 *   either way would train people to ignore the red one.
 * - **`dialog-reset`** — the proposer's side of the same moment, before anyone is asked.
 *
 * ## Running it without uninstalling the app
 *
 * ```
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.ChallengeMeasureChangeRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/challenge-measure-change
 * ```
 */
class ChallengeMeasureChangeRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    private val base = Challenge(
        id = "c1",
        title = "August Steps Race",
        measure = Measure(MeasureKind.COUNT, "steps"),
        ownerUid = "owner",
    )

    private fun card(
        pendingKind: MeasureKind,
        pendingWord: String,
        isOwner: Boolean,
        myVote: String,
    ) = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = base.copy(
                ownerUid = if (isOwner) "me" else "owner",
                pendingChangeId = "chg-1",
                pendingMeasure = Measure(pendingKind, pendingWord),
                pendingProposedAtEpochMillis = 1_760_000_000_000L,
            ),
            standings = listOf(
                ChallengeParticipant(uid = "me", displayName = "Ido", score = 6_050.0),
                ChallengeParticipant(uid = "b", displayName = "Boaz", score = 4_100.0),
                ChallengeParticipant(uid = "c", displayName = "Ann", score = 2_400.0),
            ).rankedByScore(currentUid = "me"),
            isOwner = isOwner,
            hasJoined = true,
            myLinkedGoalId = "g1",
            approvals = listOf(myVote, "chg-1", ""),
            myApprovedChangeId = myVote,
        ),
        phase = ChallengePhase.ACTIVE,
    )

    private val dialogState = MeasureChangeState(
        isVisible = true,
        challengeId = "c1",
        challengeTitle = "August Steps Race",
        currentKind = MeasureKind.COUNT,
        currentWord = "steps",
        kind = MeasureKind.DISTANCE,
        word = "km",
        participantCount = 3,
    )

    @Test
    fun everyFrame() {
        val dark = mutableStateOf(false)
        val frame = mutableStateOf(Frame.BANNER_RESET)

        composeRule.setContent {
            GoalPilotTheme(darkTheme = dark.value) {
                // The `Surface` is for the two CARD frames, which paint no background of
                // their own. The dialog frame brings its own container and does not need
                // it, and is unharmed by it. See `ChallengeProvenanceRenderPass` for the
                // dark frame that lied without one.
                Surface { RenderFrame(frame.value) }
            }
        }

        val written = mutableListOf<File>()
        Frame.entries.forEach { f ->
            listOf(false, true).forEach { isDark ->
                frame.value = f
                dark.value = isDark
                composeRule.waitForIdle()
                written += capture("${f.id}-${if (isDark) "dark" else "light"}", f)
            }
        }

        assertWithMessage("three frames in both brightnesses").that(written).hasSize(6)
    }

    @androidx.compose.runtime.Composable
    private fun RenderFrame(frame: Frame) {
        when (frame) {
            // A participant, not the owner, who has NOT voted: the Agree button is live and
            // the red sentence is the thing that has to outweigh it.
            Frame.BANNER_RESET -> MyChallengeCard(
                card = card(MeasureKind.DISTANCE, "km", isOwner = false, myVote = ""),
                onOpenStandings = {},
                onReportScore = {},
                onLinkGoal = {},
                onInvite = {},
                onChangeMeasure = {},
                onApproveMeasure = {},
                onWithdrawMeasure = {},
                isApproving = false,
                onLeave = {},
                onDelete = {},
            )

            // The owner's own harmless change, already agreed: no Agree button, a Withdraw,
            // and wording that does not shout.
            Frame.BANNER_RELABEL_AGREED -> MyChallengeCard(
                card = card(MeasureKind.COUNT, "paces", isOwner = true, myVote = "chg-1"),
                onOpenStandings = {},
                onReportScore = {},
                onLinkGoal = {},
                onInvite = {},
                onChangeMeasure = {},
                onApproveMeasure = {},
                onWithdrawMeasure = {},
                isApproving = false,
                onLeave = {},
                onDelete = {},
            )

            // The proposer's side -- `MeasureChangeContent`, NOT `MeasureChangeDialog`.
            //
            // ⚠️ AN `AppAlertDialog` RENDERS IN A WINDOW OF ITS OWN AND THE SHEET SELECTOR
            // DOES NOT RESCUE IT. The first draft of this pass composed the whole dialog
            // and captured `isRoot() and hasAnyDescendant(hasText(...))`, which is exactly
            // what works for a sheet's content -- and got back a SINGLE FLAT COLOUR, the
            // scrim. `Observed:` 2026-08-25, `dialog-reset-light`, "1 distinct colour,
            // expected at least 3". The frame was full-screen, 1344 px wide and weighed
            // something on disk; only the more-than-one-colour floor said it was empty.
            // The body was split out of the dialog in the same commit, which is the rule
            // this repo learned on 2026-08-24 -- a surface that cannot be photographed
            // cannot be reviewed.
            Frame.DIALOG_RESET -> MeasureChangeContent(
                state = dialogState,
                onKind = {},
                onWord = {},
            )
        }
    }

    private enum class Frame(val id: String, val anchor: String) {
        BANNER_RESET("banner-reset", "Every score restarts at zero"),
        BANNER_RELABEL_AGREED("banner-relabel-agreed", "Only the wording changes"),
        DIALOG_RESET("dialog-reset", "The measure is the unit everyone"),
        ;

        companion object {
            val entries = listOf(BANNER_RESET, BANNER_RELABEL_AGREED, DIALOG_RESET)
        }
    }

    private fun capture(name: String, frame: Frame): File {
        val target = composeRule.onNode(
            isRoot() and hasAnyDescendant(hasText(frame.anchor, substring = true)),
        )
        val bitmap = target.captureToImage().asAndroidBitmap()

        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "challenge-measure-change",
        )
        dir.mkdirs()
        val out = File(dir, "$name.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertWithMessage("$name is too small to be the content").that(bitmap.height)
            .isAtLeast(200)
        assertWithMessage("$name is too narrow to be the content").that(bitmap.width)
            .isAtLeast(400)
        val distinct = buildSet {
            for (x in 0 until bitmap.width step (bitmap.width / 24).coerceAtLeast(1)) {
                for (y in 0 until bitmap.height step (bitmap.height / 24).coerceAtLeast(1)) {
                    add(bitmap.getPixel(x, y))
                }
            }
        }
        assertWithMessage("$name is one flat colour — it photographed an empty window")
            .that(distinct.size).isAtLeast(3)
        return out
    }
}
