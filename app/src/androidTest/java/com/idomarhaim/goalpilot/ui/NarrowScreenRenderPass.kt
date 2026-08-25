package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * **The camera for Ido's S25 defect — and the only instrument that actually verifies it.**
 *
 * 2026-08-25, his photograph: the challenge card's `Standings` button rendered as a blue
 * column of single letters. The repair is `FlowRow` instead of `Row`.
 *
 * `NarrowScreenGuardTest` beside this file was written to hold that repair and **measured
 * not to**: with `maxLines = 1` in place a crushed button truncates rather than stacks, and
 * truncation is invisible to height, to width, and to Compose's semantics. Reverting the
 * `FlowRow` leaves that suite green. It says so in its own KDoc.
 *
 * So this pass is not decoration on a tested thing — **it is the test**, in the sense this
 * repo has used since 2026-08-24: a defect that is a *relationship* between elements is
 * settled by looking, and by nothing else.
 *
 * ### The geometry, and why it has to be constructed
 *
 * 384 dp at font 1.15 is his phone; **352** is what the card actually gets, because
 * `ChallengesScreen`'s list spends 16 dp on each side first. Getting that subtraction wrong
 * is what made the guard's first mutation test pass — at the full 384 the three buttons fit
 * and there is no defect to see. The emulator this runs on is wider than either number, so
 * without constructing the geometry the frame photographs a card that was never broken.
 *
 * ### The frames
 *
 * - **`card-at-his-width`** — the exact surface from the photograph: a linked challenge, so
 *   the widest action row the card can show (*Change goal · Type a score · Standings*).
 *   **What to look for: three complete words.** Any letter-per-line column, any `Standin…`,
 *   is the defect back.
 * - **`card-with-pending`** — the same card carrying §3's approval banner, which adds two
 *   more controls below the actions. More buttons is where a width problem shows first.
 *
 * ### Running it
 *
 * ```
 * adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
 * adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb -s emulator-5554 shell am instrument -w -e class com.idomarhaim.goalpilot.ui.NarrowScreenRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb -s emulator-5554 pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/narrow-screen
 * ```
 *
 * ⚠️ **`-s emulator-5554` is not optional.** Ido's real phone is attached to this machine, so
 * a bare `adb` command is ambiguous at best and reaches his handset at worst.
 */
class NarrowScreenRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    private val base = Challenge(
        id = "c1",
        title = "August Steps Race",
        measure = Measure(MeasureKind.COUNT, "steps"),
        ownerUid = "me",
    )

    private fun card(pending: Boolean) = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = if (pending) {
                base.copy(
                    pendingChangeId = "chg-1",
                    pendingMeasure = Measure(MeasureKind.DISTANCE, "km"),
                )
            } else {
                base
            },
            standings = listOf(
                ChallengeParticipant(uid = "me", displayName = "Ido", score = 71_137.0),
                ChallengeParticipant(uid = "b", displayName = "Rachil", score = 4_100.0),
            ).rankedByScore(currentUid = "me"),
            isOwner = true,
            hasJoined = true,
            // Linked: the widest the action row ever gets, and what he photographed.
            myLinkedGoalId = "g1",
            approvals = listOf("", ""),
        ),
        phase = ChallengePhase.ACTIVE,
    )

    @Test
    fun everyFrame() {
        val dark = mutableStateOf(false)
        val pending = mutableStateOf(false)

        composeRule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, FONT_SCALE)) {
                GoalPilotTheme(darkTheme = dark.value) {
                    Surface {
                        Box(Modifier.width(CARD_DP.dp)) { TheCard(pending.value) }
                    }
                }
            }
        }

        val written = mutableListOf<File>()
        listOf(false, true).forEach { withPending ->
            listOf(false, true).forEach { isDark ->
                pending.value = withPending
                dark.value = isDark
                composeRule.waitForIdle()
                val name = if (withPending) "card-with-pending" else "card-at-his-width"
                written += capture("$name-${if (isDark) "dark" else "light"}")
            }
        }
        assertWithMessage("two frames in both brightnesses").that(written).hasSize(4)
    }

    @androidx.compose.runtime.Composable
    private fun TheCard(pending: Boolean) {
        MyChallengeCard(
            card = card(pending),
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

    private fun capture(name: String): File {
        val bitmap = composeRule
            .onNode(isRoot() and hasAnyDescendant(hasText("Standings", substring = true)))
            .captureToImage()
            .asAndroidBitmap()

        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "narrow-screen",
        )
        dir.mkdirs()
        val out = File(dir, "$name.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertWithMessage("$name is too small").that(bitmap.height).isAtLeast(150)
        assertWithMessage("$name is too narrow").that(bitmap.width).isAtLeast(300)
        // Size alone passes on a flat rectangle -- two such frames were filed on 2026-08-24
        // before anybody opened them.
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

    private companion object {
        /**
         * What the CARD gets on his phone: 384 dp screen minus the list's 16 dp each side.
         *
         * The subtraction is the whole point — see this class's KDoc. Photographing at 384
         * shows a card that fits and proves nothing.
         */
        const val CARD_DP = 384 - 32
        const val FONT_SCALE = 1.15f
    }
}
