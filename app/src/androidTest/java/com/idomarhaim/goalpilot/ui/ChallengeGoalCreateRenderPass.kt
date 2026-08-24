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
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.feature.challenges.GoalLinkContent
import com.idomarhaim.goalpilot.feature.challenges.GoalLinkState
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for §2 — *"joining links **or creates** a goal"* — and **not a test, a
 * camera.**
 *
 * `ChallengesViewModelTest` proves the created goal copies the challenge's measure, that
 * creating and linking are one act, and that a half-done create says both halves. What it
 * cannot answer is the question this surface is risky for:
 *
 * > **Does a form appearing where a message used to be read as *help*, or as *paperwork*?**
 *
 * The user arrived here wanting to join a race, not to fill in a goal editor. Three
 * controls is the whole design argument — the measure is the challenge's and is deliberately
 * **not** a field — and whether three is few enough to feel like a shortcut rather than a
 * detour is only visible in a picture.
 *
 * ## What each frame is composed to expose
 *
 * - **`goal-create`** — the empty branch as a user first meets it: title seeded from the
 *   challenge, target blank. The blank target is a decision (a challenge names a unit,
 *   never a finish line) and the frame is where *"does an empty field read as unfinished
 *   or as a question?"* becomes answerable.
 * - **`goal-create-filled`** — target typed, so the unit suffix is beside a real number.
 * - **`goal-link-picker`** — the **non**-empty branch, unchanged by §2 and photographed to
 *   prove it: this session edited the composable those rows live in, and a frame is the
 *   only thing that shows the picker still reads as a picker.
 *
 * ## Running it without uninstalling the app
 *
 * ```
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.ChallengeGoalCreateRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/challenge-goal-create
 * ```
 */
class ChallengeGoalCreateRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    private val measure = Measure(MeasureKind.COUNT, "steps")

    private fun state(
        eligible: List<Goal> = emptyList(),
        target: String = "",
    ) = GoalLinkState(
        isVisible = true,
        challengeId = "c1",
        challengeTitle = "August Steps Race",
        measure = measure,
        metricWord = "steps",
        kindLabel = "Count",
        eligible = eligible,
        createTitle = "August Steps Race",
        createTarget = target,
    )

    @Test
    fun everyFrame() {
        val dark = mutableStateOf(false)
        val frame = mutableStateOf(Frame.CREATE)

        composeRule.setContent {
            // The `Surface` is load-bearing -- without it the dark frames render dark
            // foreground on the host window's light background and read as a defect the
            // app never draws. See `ChallengeProvenanceRenderPass` for the frame that
            // proved it on 2026-08-24.
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

        assertWithMessage("three frames in both brightnesses").that(written).hasSize(6)
    }

    @androidx.compose.runtime.Composable
    private fun RenderFrame(frame: Frame) {
        // `GoalLinkContent`, not `GoalLinkSheet`: the sheet renders in a window of its own
        // and cannot be photographed at all. That seam was cut in the same commit as this
        // pass, which is the rule this repo learned on 2026-08-24 rather than a style.
        val s = when (frame) {
            Frame.CREATE -> state()
            Frame.CREATE_FILLED -> state(target = "300000")
            Frame.PICKER -> state(
                eligible = listOf(
                    Goal(id = "g1", title = "Walk every day", measure = measure),
                    Goal(id = "g2", title = "10k steps a day", measure = measure),
                ),
            )
        }
        GoalLinkContent(
            state = s,
            onLink = {},
            onCreateTitle = {},
            onCreateTarget = {},
            onCreate = {},
        )
    }

    private enum class Frame(val id: String, val anchor: String) {
        CREATE("goal-create", "Create and start scoring"),
        CREATE_FILLED("goal-create-filled", "Create and start scoring"),
        PICKER("goal-link-picker", "Walk every day"),
        ;

        companion object {
            val entries = listOf(CREATE, CREATE_FILLED, PICKER)
        }
    }

    private fun capture(name: String, frame: Frame): File {
        val target = composeRule.onNode(
            isRoot() and hasAnyDescendant(hasText(frame.anchor, substring = true)),
        )
        val bitmap = target.captureToImage().asAndroidBitmap()

        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "challenge-goal-create",
        )
        dir.mkdirs()
        val out = File(dir, "$name.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertWithMessage("$name is too small to be the content").that(bitmap.height)
            .isAtLeast(200)
        assertWithMessage("$name is too narrow to be the content").that(bitmap.width)
            .isAtLeast(400)

        // Size alone passes on a full-screen rectangle of one flat colour, twice, which is
        // exactly what happened here on 2026-08-24. Count distinct sampled colours too.
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
