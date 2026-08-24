package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.ScoreSource
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.feature.challenges.ChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.StandingsList
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for `C14` / `#23`'s provenance badge — **not a test, a camera.**
 *
 * `ChallengesUiTest` beside this file proves the badge says the right words and that a
 * derived row says none. It cannot answer the question this feature is actually risky
 * for, because that question is not a string:
 *
 * > **Does a badged row read as *information*, or as an *accusation*?**
 *
 * That matters more here than on most surfaces. This is a claim about **another user**,
 * rendered to everyone in the challenge, on the one screen in the app people argue about.
 * `C4`'s register is the one to match — *the app never asserts an intrinsic edge by
 * itself* — and a sentence can be perfectly factual and still read as a finger pointed.
 * Only a picture settles it, so this walks the three sources side by side and writes
 * frames.
 *
 * ## What each frame is composed to expose
 *
 * The standings deliberately mix all three sources **in one list**, because the design's
 * whole claim is that the absence of a badge is the honest default: a frame with only
 * badged rows would prove nothing about how the unbadged ones read beside them. The
 * badged row is **not** last and **not** bottom-ranked, so the picture also shows that a
 * typed score sorts on its value like any other.
 *
 * One row carries a long display name, because `ReportedBadge` builds its sentence by
 * **concatenation** and that is the shape which breaks first — the frame is where an
 * ellipsis-versus-wrap decision becomes visible.
 *
 * ## No sign-in, on purpose
 *
 * It composes the real [StandingsList] and [MyChallengeCard] directly rather than
 * driving the app, so it needs no account and cannot be invalidated by a wiped emulator
 * (`kb/dev/android-device-verification.md` §8).
 *
 * ## Running it without uninstalling the app
 *
 * `connectedDebugAndroidTest` uninstalls, which takes the Google account with it. Install
 * both APKs and drive the runner directly:
 *
 * ```
 * adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.ChallengeProvenanceRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/challenge-provenance
 * ```
 */
class ChallengeProvenanceRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    private val challenge = com.idomarhaim.goalpilot.domain.model.Challenge(
        id = "c1",
        title = "August Steps Race",
        description = "Whoever walks the most this month.",
        measure = Measure(MeasureKind.COUNT, "steps"),
        ownerUid = "me",
    )

    /**
     * All three sources in one list, ranked on value alone.
     *
     * `Ann` is second on a **typed** number and `Ido` (the viewer) is third on a derived
     * one, so the badge appears above an unbadged row rather than under everything — the
     * arrangement that would make a punitive-looking badge most obvious.
     */
    private fun standings() = listOf(
        ChallengeParticipant(
            uid = "a",
            displayName = "Yonatan Ben-Shimon",
            score = 11_400.0,
            source = ScoreSource.DERIVED,
        ),
        ChallengeParticipant(
            uid = "b",
            displayName = "Ann",
            score = 8_200.0,
            source = ScoreSource.REPORTED,
            // Relative to the run, not a fixed epoch. `DateTimeUtils.relative` falls back
            // to a full date past its window, so a hard-coded stamp renders "Aug 24, 2025"
            // -- correct behaviour, and the wrong frame: it photographs the RARE case and
            // hides the one a competitor actually sees. `Observed:` the third frame of
            // this pass, where a year-old stamp spent a third of the line on the year.
            reportedAtEpochMillis = System.currentTimeMillis() - 26L * 60 * 60 * 1000,
        ),
        ChallengeParticipant(
            uid = "me",
            displayName = "Ido",
            score = 6_050.0,
            source = ScoreSource.DERIVED,
        ),
        ChallengeParticipant(uid = "d", displayName = "Noa", source = ScoreSource.NONE),
    )

    private fun card(linkedGoalId: String) = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = challenge,
            standings = standings().rankedByScore(currentUid = "me"),
            isOwner = true,
            hasJoined = true,
            myLinkedGoalId = linkedGoalId,
        ),
        phase = ChallengePhase.ACTIVE,
    )

    /**
     * Every frame, from ONE `setContent`.
     *
     * `AndroidComposeTestRule` throws *"Cannot call setContent twice per test"*, so a
     * frame-per-`@Test` would need six near-identical methods. The matrix is driven by
     * state instead — the same shape `CategoryPaletteRenderPass` uses — and the whole
     * thing is one pass over the emulator.
     */
    @Test
    fun everyFrame() {
        val dark = mutableStateOf(false)
        val frame = mutableStateOf(Frame.STANDINGS)

        composeRule.setContent {
            // ⚠️ THE `Surface` IS NOT DECORATION — WITHOUT IT THE DARK FRAME LIES.
            //
            // `StandingsList` and `MyChallengeCard` paint no background of their own; in
            // the app they sit inside a sheet and a screen that do. Rendered bare, the
            // theme swaps the FOREGROUND colours to their dark values and leaves the host
            // window's light background behind them.
            //
            // `Observed:` 2026-08-24, the fourth frame of this pass — `standings-dark.png`
            // came back with a pale background, near-invisible secondary text and washed
            // out rank numbers. Read as a product defect that is "the badge is unreadable
            // in dark mode", filed against `ReportedBadge`, and false: the app never draws
            // it on that background. A render pass that photographs a composable out of
            // its container measures the container's absence, not the component.
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
                written += capture("${f.id}-${if (isDark) "dark" else "light"}")
            }
        }

        assertWithMessage("three frames in both brightnesses").that(written).hasSize(6)
    }

    /** One frame's composable. Split out only so the `Surface` above wraps every case. */
    @androidx.compose.runtime.Composable
    private fun RenderFrame(frame: Frame) {
        when (frame) {
            // `StandingsList`, not `StandingsSheet`: the sheet renders in a
            // window of its own and cannot be photographed at all -- see that
            // composable's KDoc, and the two blank full-screen frames that got
            // this far before it was split out.
            Frame.STANDINGS -> StandingsList(card = card(linkedGoalId = "g1"))

            Frame.CARD_LINKED -> MyChallengeCard(
                card = card(linkedGoalId = "g1"),
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

            Frame.CARD_UNLINKED -> MyChallengeCard(
                card = card(linkedGoalId = ""),
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

    private enum class Frame(val id: String) {
        STANDINGS("standings"),
        CARD_LINKED("card-linked"),
        CARD_UNLINKED("card-unlinked"),
        ;

        companion object {
            val entries = listOf(STANDINGS, CARD_LINKED, CARD_UNLINKED)
        }
    }

    /**
     * Photographs the root that actually holds the content, and writes one PNG.
     *
     * See the comment inside for why both obvious selectors are wrong — and for the
     * blank full-screen frame that passed every size assertion before this was fixed.
     */
    private fun capture(name: String): File {
        // THE ROOT THAT CONTAINS THE CONTENT, never the tallest and never `onRoot()`.
        //
        // Both of the obvious selectors are wrong here, and neither says so:
        //
        //  * `onRoot()` refuses outright while a sheet is up -- "expected exactly 1 node
        //    but found 2 that satisfy (isRoot)" -- because
        //    `AppModalBottomSheet` renders in a window of its own
        //    (`CalendarSurfaceUiTest` documents this).
        //  * the TALLEST root is the HOST root, which is full-screen and, once the sheet
        //    has taken the content into its own window, completely EMPTY.
        //
        // `Observed:` 2026-08-24, this file's second run: `standings-light.png` came back
        // 1344x2992 of flat #d3e3fb and PASSED every floor below -- big enough, wide
        // enough, 22 kB on disk. A blank screen that satisfies a size assertion is
        // exactly the failure `kb/dev/look-at-your-own-output.md` is about, and the only
        // thing that caught it was opening the file and looking at it.
        val target = composeRule.onNode(isRoot() and hasAnyDescendant(hasText(challenge.title)))
        val bitmap = target.captureToImage().asAndroidBitmap()
        // Not just "big enough" -- big enough AND not one flat colour. See below.

        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "challenge-provenance",
        )
        dir.mkdirs()
        val out = File(dir, "$name.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // The floor `DurationBoxRenderTest` records: a render pass that photographed three
        // empty boxes proved nothing. A picture of a blank screen is not evidence -- and on
        // this file's first run it was exactly what stopped a 71 px strip being filed as a
        // standings sheet.
        assertWithMessage("$name was written").that(out.length()).isGreaterThan(1_000L)
        assertWithMessage("$name has width").that(bitmap.width).isGreaterThan(300)
        assertWithMessage("$name has height").that(bitmap.height).isGreaterThan(300)

        // ⚠️ THE SIZE FLOOR IS NOT ENOUGH, AND THAT IS THE FINDING THIS FILE COST.
        //
        // `DurationBoxRenderTest`'s floor -- big enough, wide enough, non-empty on disk --
        // is what every render pass here inherits, and a 1344x2992 rectangle of flat
        // #d3e3fb satisfies all three. `Observed:` 2026-08-24, twice: a blank host window
        // filed as a standings sheet, 22 kB on disk, every assertion green. The only thing
        // that caught it was a person opening the PNG.
        //
        // So the floor is raised to the property a blank frame CANNOT have: more than one
        // colour. Sampled on a coarse grid rather than every pixel -- this runs six times
        // and 4 M pixels each is a waste for a question a few hundred answers.
        val seen = HashSet<Int>()
        var y = 0
        while (y < bitmap.height && seen.size < 8) {
            var x = 0
            while (x < bitmap.width && seen.size < 8) {
                seen += bitmap.getPixel(x, y)
                x += 37
            }
            y += 37
        }
        assertWithMessage(
            "$name is one flat colour -- a blank window, not a screenshot. " +
                "See the selector comment above.",
        ).that(seen.size).isGreaterThan(1)
        return out
    }
}
