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
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.usecase.HealthMetric
import com.idomarhaim.goalpilot.feature.challenges.GoalLinkContent
import com.idomarhaim.goalpilot.feature.challenges.GoalLinkState
import com.idomarhaim.goalpilot.feature.challenges.HealthLinkOption
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for Ido's 2026-08-25 ask — **not a test, a camera.**
 *
 * > *"if I make a steps competition, there should also be an option to pull the logs
 * > straight into the CHALLENGE and not only through a personal GOAL of mine"*
 *
 * `ChallengesViewModelTest` proves which metrics are offered, that an existing Health
 * Connect goal is reused rather than duplicated, and that an unavailable provider still
 * leaves the row standing. None of that answers the question this surface is risky for:
 *
 * > **Does "Health Connect" read as the obvious answer, or as one more thing to
 * > understand?**
 *
 * That is the whole ask. He did not want a new capability — a steps race could already be
 * scored from a steps goal — he wanted **not to have to think about a goal**. If the row is
 * one more item in a list of choices, the feature has technically shipped and actually
 * failed.
 *
 * ## What each frame is composed to expose
 *
 * - **`health-and-goals`** — the mixed case a real user meets: Health Connect at the top,
 *   two of their own goals underneath, and the *"…or score it from a goal of your own"*
 *   divider between. The frame is where *"is the top option obviously the answer?"* becomes
 *   answerable rather than asserted.
 * - **`health-creates-goal`** — the first-time case, where taking it **makes** the goal. The
 *   row says so before it happens; a new row appearing on the Goals screen unannounced is
 *   the one thing about this design a user could fairly call a surprise, so the sentence
 *   that prevents it has to be legible.
 * - **`health-unavailable`** — Health Connect missing from the phone. The option is
 *   deliberately **still there**, greyed, with the reason. A choice that vanishes teaches
 *   nothing, and this frame is how you check the greyed row reads as *"not set up"* rather
 *   than as *"broken"*.
 * - **`health-in-use`** — already the thing scoring this challenge, so the row reports
 *   rather than offers.
 *
 * ## Running it without uninstalling the app
 *
 * ```
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.ChallengeHealthSourceRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/challenge-health-source
 * ```
 */
class ChallengeHealthSourceRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    private val stepsMeasure = Measure(MeasureKind.COUNT, "steps")

    private fun state(
        eligible: List<Goal> = emptyList(),
        createsGoal: Boolean = false,
        isCurrent: Boolean = false,
        availability: HealthAvailability? = HealthAvailability.AVAILABLE,
    ) = GoalLinkState(
        isVisible = true,
        challengeId = "c1",
        challengeTitle = "August Steps Race",
        measure = stepsMeasure,
        metricWord = "steps",
        kindLabel = "Count",
        eligible = eligible,
        createTitle = "August Steps Race",
        // The computed sentence the sheet now shows instead of §6's stale prose.
        windowNote = "Everything you count between 18 Aug and 24 Aug — the same days " +
            "for everyone, whenever each of you joined.",
        healthOptions = listOf(
            HealthLinkOption(
                metric = HealthMetric.STEPS,
                isCurrent = isCurrent,
                createsGoal = createsGoal,
            ),
        ),
        healthAvailability = availability,
    )

    private val myGoals = listOf(
        Goal(id = "g1", title = "Walk every day", measure = stepsMeasure),
        Goal(id = "g2", title = "10k steps a day", measure = stepsMeasure),
    )

    @Test
    fun everyFrame() {
        val dark = mutableStateOf(false)
        val frame = mutableStateOf(Frame.HEALTH_AND_GOALS)

        composeRule.setContent {
            // The `Surface` is load-bearing: this composable paints no background of its
            // own, and without one the dark frames render dark foreground on the host
            // window's light ground and read as a defect the app never draws. See
            // `ChallengeProvenanceRenderPass` for the frame that proved it.
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

        assertWithMessage("four frames in both brightnesses").that(written).hasSize(8)
    }

    @androidx.compose.runtime.Composable
    private fun RenderFrame(frame: Frame) {
        // `GoalLinkContent`, never `GoalLinkSheet`: a modal sheet renders in a window of its
        // own and cannot be photographed at all. That seam already exists for exactly this.
        val s = when (frame) {
            Frame.HEALTH_AND_GOALS -> state(eligible = myGoals)
            Frame.HEALTH_CREATES_GOAL -> state(createsGoal = true)
            Frame.HEALTH_UNAVAILABLE ->
                state(eligible = myGoals, availability = HealthAvailability.NOT_SUPPORTED)

            Frame.HEALTH_IN_USE -> state(eligible = myGoals, isCurrent = true)
        }
        GoalLinkContent(
            state = s,
            onLink = {},
            onPickHealth = {},
            onCreateTitle = {},
            onCreateTarget = {},
            onCreate = {},
        )
    }

    private enum class Frame(val id: String, val anchor: String) {
        HEALTH_AND_GOALS("health-and-goals", "Straight from Health Connect"),
        HEALTH_CREATES_GOAL("health-creates-goal", "goal to hold them"),
        HEALTH_UNAVAILABLE("health-unavailable", "not set up on this phone"),
        HEALTH_IN_USE("health-in-use", "Already scoring this challenge"),
        ;

        companion object {
            val entries =
                listOf(HEALTH_AND_GOALS, HEALTH_CREATES_GOAL, HEALTH_UNAVAILABLE, HEALTH_IN_USE)
        }
    }

    private fun capture(name: String, frame: Frame): File {
        val target = composeRule.onNode(
            isRoot() and hasAnyDescendant(hasText(frame.anchor, substring = true)),
        )
        val bitmap = target.captureToImage().asAndroidBitmap()

        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "challenge-health-source",
        )
        dir.mkdirs()
        val out = File(dir, "$name.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        assertWithMessage("$name is too small").that(bitmap.height).isAtLeast(200)
        assertWithMessage("$name is too narrow").that(bitmap.width).isAtLeast(400)
        // Size alone passes on a full-screen rectangle of one flat colour -- which is how a
        // dialog photographs, and how two frames were filed on 2026-08-24 before anybody
        // opened them. Count distinct sampled colours as well.
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
