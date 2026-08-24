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
import com.idomarhaim.goalpilot.domain.model.ChallengeInvite
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.InviteCandidate
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.feature.challenges.ChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.ChallengeInviteRow
import com.idomarhaim.goalpilot.feature.challenges.InviteList
import com.idomarhaim.goalpilot.feature.challenges.InviteState
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for §1's invite — **not a test, a camera.**
 *
 * `ChallengesViewModelTest` proves who may be invited, that a second invite is refused,
 * and that declining says nothing. None of that answers the question this feature is
 * actually risky for, because the question is not a string:
 *
 * > **Does an inbound invite row read as an *offer*, or as an *obligation*?**
 *
 * It must read as an offer. Ido asked for the ability to invite a friend; he did not ask
 * for a screen that starts making demands of the friend. Every choice in
 * [ChallengeInviteRow] is aimed at that one criterion — a filled `Join` beside a text
 * `Dismiss`, no badge, no count, no confirmation on declining — and a sentence can
 * satisfy every one of those rules and still land as a summons. Only a picture settles
 * it.
 *
 * ## What each frame is composed to expose
 *
 * - **`invite-row`** — the ordinary case, the one a real user meets.
 * - **`invite-row-long`** — the sentence is built by **concatenation** of a display name
 *   and a challenge title, so a long pair is the shape that breaks first. This is where
 *   a wrap-versus-ellipsis decision becomes visible instead of theoretical.
 * - **`invite-sheet`** — the sending half, deliberately mixing an invitable friend, one
 *   who is **already in**, and one **already invited**. A frame of three identical live
 *   rows would prove nothing about how a greyed row reads beside a live one, and greying
 *   rather than filtering is the decision most worth looking at.
 * - **`invite-sheet-empty`** — no friends at all. The fix is on a different screen, and
 *   the frame is how you check the sentence says so.
 * - **`card-with-invite`** — the affordance itself. Ido's complaint was that he could not
 *   *find* a way to invite anybody, so *"is the icon discoverable beside the overflow?"*
 *   is a first-class question and it is only answerable by looking at the card.
 *
 * ## No sign-in, on purpose
 *
 * Every frame composes the real composables directly with hand-built state, so this needs
 * no account and cannot be invalidated by a wiped emulator
 * (`kb/dev/android-device-verification.md` §8).
 *
 * ## Running it without uninstalling the app
 *
 * `connectedDebugAndroidTest` uninstalls, which takes the Google account with it. Build
 * **both** APKs, install **both**, and drive the runner directly:
 *
 * ```
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.ChallengeInviteRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/challenge-invite
 * ```
 */
class ChallengeInviteRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    private val challenge = Challenge(
        id = "c1",
        title = "August Steps Race",
        description = "Whoever walks the most this month.",
        measure = Measure(MeasureKind.COUNT, "steps"),
        ownerUid = "me",
    )

    private val card = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = challenge,
            standings = listOf(
                ChallengeParticipant(uid = "me", displayName = "Ido", score = 6_050.0),
                ChallengeParticipant(uid = "f2", displayName = "Boaz", score = 4_100.0),
            ).rankedByScore(currentUid = "me"),
            isOwner = true,
            hasJoined = true,
            myLinkedGoalId = "g1",
        ),
        phase = ChallengePhase.ACTIVE,
    )

    /** One inbound invite, at the length a real one usually is. */
    private val invite = ChallengeInvite(
        id = "i1",
        challengeId = "c1",
        challengeTitle = "August Steps Race",
        fromUid = "f1",
        fromName = "Ann",
        toUid = "me",
        createdAtEpochMillis = 1_760_000_000_000L,
    )

    /**
     * The shape that breaks the sentence.
     *
     * [ChallengeInviteRow] concatenates a display name and a challenge title into one
     * line, and neither is bounded by anything the app controls — a display name comes
     * from a Google account and a title from whoever made the challenge. A frame of the
     * short case alone would photograph the version that always looks fine.
     */
    private val longInvite = invite.copy(
        id = "i2",
        fromName = "Yonatan Ben-Shimon-Halevi",
        challengeTitle = "Whoever walks the most steps between now and the end of August",
    )

    private fun sheet(candidates: List<InviteCandidate>) = InviteState(
        isVisible = true,
        challengeId = "c1",
        challengeTitle = "August Steps Race",
        candidates = candidates,
    )

    /**
     * Every frame, from ONE `setContent`.
     *
     * `AndroidComposeTestRule` throws *"Cannot call setContent twice per test"*, so a
     * frame-per-`@Test` would need ten near-identical methods. The matrix is driven by
     * state instead — the shape `ChallengeProvenanceRenderPass` beside this file uses.
     */
    @Test
    fun everyFrame() {
        val dark = mutableStateOf(false)
        val frame = mutableStateOf(Frame.INVITE_ROW)

        composeRule.setContent {
            // ⚠️ THE `Surface` IS NOT DECORATION — WITHOUT IT THE DARK FRAME LIES.
            //
            // These composables paint no background of their own; in the app they sit
            // inside a sheet and a screen that do. Rendered bare, the theme swaps the
            // FOREGROUND colours to their dark values and leaves the host window's light
            // background behind them -- which reads as a real product defect ("the text
            // is unreadable in dark mode") and is not one, because the app never draws it
            // on that background. `Observed:` 2026-08-24 on the pass beside this file.
            GoalPilotTheme(darkTheme = dark.value) {
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

        assertWithMessage("five frames in both brightnesses").that(written).hasSize(10)
    }

    /** One frame's composable. Split out only so the `Surface` above wraps every case. */
    @androidx.compose.runtime.Composable
    private fun RenderFrame(frame: Frame) {
        when (frame) {
            Frame.INVITE_ROW -> ChallengeInviteRow(
                invite = invite,
                isBusy = false,
                onJoin = {},
                onDismiss = {},
            )

            Frame.INVITE_ROW_LONG -> ChallengeInviteRow(
                invite = longInvite,
                isBusy = false,
                onJoin = {},
                onDismiss = {},
            )

            // `InviteList`, not `InviteSheet`: the sheet renders in a window of its own
            // and cannot be photographed at all. See that composable's KDoc, and the
            // 71 px strip and the 1344x2992 blank that got filed before it was understood.
            Frame.INVITE_SHEET -> InviteList(
                state = sheet(
                    listOf(
                        InviteCandidate(uid = "f1", displayName = "Ann"),
                        InviteCandidate(
                            uid = "f2",
                            displayName = "Boaz",
                            isParticipant = true,
                        ),
                        InviteCandidate(
                            uid = "f3",
                            displayName = "Yonatan Ben-Shimon-Halevi",
                            isInvited = true,
                        ),
                    ),
                ),
                onInvite = {},
            )

            Frame.INVITE_SHEET_EMPTY -> InviteList(state = sheet(emptyList()), onInvite = {})

            Frame.CARD_WITH_INVITE -> MyChallengeCard(
                card = card,
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
        }
    }

    /**
     * Each frame knows a string that must be **inside** the root it is photographed from.
     *
     * Not decoration either: it is how `capture` finds the right root, and it is what
     * stopped a full-screen blank being filed as a standings sheet on 2026-08-24.
     */
    private enum class Frame(val id: String, val anchor: String) {
        INVITE_ROW("invite-row", "Ann invited you to"),
        INVITE_ROW_LONG("invite-row-long", "Yonatan Ben-Shimon-Halevi invited you to"),
        INVITE_SHEET("invite-sheet", "Invite a friend"),
        INVITE_SHEET_EMPTY("invite-sheet-empty", "You have not added anyone yet"),
        CARD_WITH_INVITE("card-with-invite", "August Steps Race"),
        ;

        companion object {
            val entries = listOf(
                INVITE_ROW,
                INVITE_ROW_LONG,
                INVITE_SHEET,
                INVITE_SHEET_EMPTY,
                CARD_WITH_INVITE,
            )
        }
    }

    /**
     * Photographs the root that actually holds the content, and writes one PNG.
     *
     * Both obvious selectors are wrong and neither says so — see
     * `ChallengeProvenanceRenderPass.capture` for the two frames that proved it. The
     * floors below are the ones that file records: big enough, **and more than one
     * colour**, because a 1344x2992 rectangle of flat background passed every size
     * assertion twice before anybody opened the file.
     */
    private fun capture(name: String, frame: Frame): File {
        val target = composeRule.onNode(
            isRoot() and hasAnyDescendant(hasText(frame.anchor, substring = true)),
        )
        val bitmap = target.captureToImage().asAndroidBitmap()

        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "challenge-invite",
        )
        dir.mkdirs()
        val out = File(dir, "$name.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertWithMessage("$name is too small to be the content").that(bitmap.height)
            .isAtLeast(MIN_HEIGHT_PX)
        assertWithMessage("$name is too narrow to be the content").that(bitmap.width)
            .isAtLeast(MIN_WIDTH_PX)

        // THE FLOOR THAT ACTUALLY CATCHES A BLANK. Size alone does not: a full-screen
        // rectangle of one flat colour is both tall and wide. Sampling a grid and
        // counting distinct colours is what distinguishes a rendered surface from an
        // empty window, and it is the assertion that would have caught 2026-08-24's two
        // false greens without anybody opening a file.
        val distinct = buildSet {
            for (x in 0 until bitmap.width step (bitmap.width / SAMPLES).coerceAtLeast(1)) {
                for (y in 0 until bitmap.height step (bitmap.height / SAMPLES).coerceAtLeast(1)) {
                    add(bitmap.getPixel(x, y))
                }
            }
        }
        assertWithMessage("$name is one flat colour — it photographed an empty window")
            .that(distinct.size).isAtLeast(MIN_DISTINCT_COLOURS)

        return out
    }

    private companion object {
        const val MIN_HEIGHT_PX = 120
        const val MIN_WIDTH_PX = 400
        const val SAMPLES = 24
        const val MIN_DISTINCT_COLOURS = 3
    }
}
