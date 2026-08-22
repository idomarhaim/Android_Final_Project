package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.GpEntrance
import com.idomarhaim.goalpilot.ui.components.LocalGpEntrance
import com.idomarhaim.goalpilot.ui.components.SectionHeader
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for `#57` d — **not a test, a camera**, exactly as
 * [ChartVolumeRenderPass] and [MaterialRenderPass] are.
 *
 * ## Why a *strip* rather than a frame
 *
 * The other two passes photograph a settled screen, because what they check is a
 * still property: a colour, a shadow, a wall. This brief's property is **motion**,
 * and a settled frame of an entrance animation is indistinguishable from a build
 * with no animation in it — which is exactly the state `#57` d was opened about.
 * So the clock is frozen and one frame is written per instant: reading the strip
 * in order *is* watching the arrival, at a resolution no screen recording of this
 * emulator gives (and `ffmpeg` is not installed on this machine, so an `.mp4`
 * cannot be turned into frames here at all).
 *
 * ## What to look for in the strip
 *
 * - **`000ms`** — an empty page. The blocks are not merely faint, they are
 *   absent, which is the `both` in `animation: rise .34s both`.
 * - **`032ms` … `144ms`** — the wave. Each frame should show the top card
 *   further along than the one below it: more opaque *and* higher. A frame where
 *   every card is at the same opacity means the stagger is gone.
 * - **`800ms`** — the settled dashboard, which is what every other pass in this
 *   folder photographs and the only frame that proves nothing on its own.
 *
 * ## Run it without destroying the device's sign-in
 *
 * `connectedDebugAndroidTest` **uninstalls the app when it finishes**, taking
 * every PNG below with it (`kb/dev/android-device-verification.md` §8). Install
 * both APKs and drive the runner directly:
 *
 * ```
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.EntranceRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/render-pass
 * ```
 */
class EntranceRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDashboardColumnArriving() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GoalPilotTheme(
                skin = AppSkin.AURORA,
                background = AppBackground.MATCH,
                darkTheme = false,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .gpPage(
                                spec = MaterialTheme.gpMaterial,
                                background = MaterialTheme.colorScheme.background,
                            ),
                    ) {
                        CompositionLocalProvider(
                            LocalGpEntrance provides GpEntrance(motion = true),
                        ) {
                            ArrivingColumn()
                        }
                    }
                }
            }
        }

        val outDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "render-pass",
        )
        outDir.mkdirs()

        val written = mutableListOf<File>()
        var elapsed = 0L
        FRAMES_AT_MS.forEach { instant ->
            composeRule.mainClock.advanceTimeBy(instant - elapsed)
            elapsed = instant

            val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
            // Half size, [MaterialRenderPass]'s reason: a full-resolution PNG of
            // this AVD is ~4 Mpx. A 33-px rise survives the downscale.
            val small = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
            val stem = "entrance-" + instant.toString().padStart(3, '0') + "ms"
            val file = File(outDir, "$stem.png")
            file.outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 90, it) }
            small.recycle()
            written += file
        }

        assertWithMessage("one frame per instant").that(written).hasSize(FRAMES_AT_MS.size)
        written.forEach { file ->
            assertWithMessage("${file.name} was written").that(file.isFile).isTrue()
            assertWithMessage("${file.name} is not empty").that(file.length()).isGreaterThan(1_000L)
        }
    }

    private companion object {
        /**
         * Dense through the first 150 ms because that is where the whole thing
         * happens: the curve reaches nine tenths of the way in a fifth of its
         * time, so frames spread evenly across 340 ms would all look settled.
         */
        val FRAMES_AT_MS = listOf(0L, 32L, 48L, 64L, 96L, 144L, 208L, 800L)
    }
}

/**
 * A dashboard-shaped column: the cards Ido actually looks at, in the order the
 * real screen stacks them, with the section header that sits between them.
 */
@Composable
private fun ArrivingColumn() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Block(title = "Level 4", body = "620 pts · 80 pts to level 5")
        Block(title = "Overall progress", body = "62% averaged across 9 goals")
        Block(title = "Smart add a task", body = "Describe anything you want to do.")
        SectionHeader(title = "Your goals")
        Block(title = "Run 5 km", body = "3 of 4 tasks done this week")
        Block(title = "Read 12 books", body = "7 of 12 finished")
    }
}

@Composable
private fun Block(title: String, body: String) {
    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
