package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.BarItem
import com.idomarhaim.goalpilot.ui.components.DonutChart
import com.idomarhaim.goalpilot.ui.components.DonutSlice
import com.idomarhaim.goalpilot.ui.components.HorizontalBarChart
import com.idomarhaim.goalpilot.ui.components.ProgressRing
import com.idomarhaim.goalpilot.ui.components.StackedColumn
import com.idomarhaim.goalpilot.ui.components.StackedColumnChart
import com.idomarhaim.goalpilot.ui.components.StackedSegment
import com.idomarhaim.goalpilot.ui.components.localizedLabel
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for `#57` c — **not a test, a camera**, exactly as
 * [MaterialRenderPass] is.
 *
 * The brief says so in as many words:
 *
 * > **Seen** on a device — this is a *look* feature, so a passing test proves
 * > almost nothing.
 *
 * `ThemePaletteTest` can assert that a body's stops never invert and that raised
 * is no-op on nothing. It cannot see a wall landing on a neighbour's face, a
 * grain tile seaming, a sheen that has become a white stripe, or a donut that no
 * longer fits its hole. Those are what these frames are for.
 *
 * ## All four chart types in one frame, on purpose
 *
 * [MaterialRenderPass] photographs one screen per cell. This one composes the
 * **donut, the ring, the columns and the bars together**, because the property
 * being checked is that they are lit from *the same place*: four separate frames
 * would each look plausible while the set disagreed about where the light is.
 * Sixteen frames — 4 materials × flat/raised × 2 brightnesses — on one skin.
 *
 * ## The wrapper is [MaterialRenderPass]'s, and it has to be
 *
 * `gpPage` is the only thing in the app that draws a ground, and it is called at
 * two sites, neither of them a screen. That file records the two days its frames
 * showed glass panels floating on a flat colour the app never renders; the same
 * copy is here for the same reason. A chart's cast shadow lands *on the ground*,
 * so a pass without one is photographing half the layer.
 *
 * ## Run it without destroying the device's sign-in
 *
 * `connectedDebugAndroidTest` **uninstalls the app when it finishes**, taking
 * every PNG below with it (`kb/dev/android-device-verification.md` §8). Install
 * both APKs and drive the runner directly:
 *
 * ```
 * adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.ChartVolumeRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/render-pass
 * ```
 *
 * ⚠️ **Wake and unlock first**, and ⚠️ **reinstall the MAIN apk, not only the
 * test one** — `#57` b lost three passes to byte-identical output because only
 * the androidTest APK was being replaced while the change under test lived in
 * the other. Both warnings are [MaterialRenderPass]'s, paid for there.
 */
class ChartVolumeRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aurora_everyMaterialFlatAndRaised() = capture(AppSkin.AURORA)

    /**
     * One skin per method, [MaterialRenderPass]'s rule: a hang in the second
     * leaves the first skin's frames on the device rather than nothing at all,
     * which is also why the assertions are about files existing.
     */
    @Test
    fun blossom_everyMaterialFlatAndRaised() = capture(AppSkin.BLOSSOM)

    private fun capture(skin: AppSkin) {
        val material = mutableStateOf(AppMaterial.NEO)
        val relief = mutableStateOf(AppRelief.FLAT)
        val brightness = mutableStateOf(AppBrightness.LIGHT)

        composeRule.setContent {
            GoalPilotTheme(
                skin = skin,
                material = material.value,
                // MATCH, so each material is shown on the ground it was designed
                // for. The ground is `#57` b's axis and it is not what varies
                // here; holding it fixed is what makes the pair of frames a
                // comparison of RELIEF and of nothing else.
                background = AppBackground.MATCH,
                relief = relief.value,
                darkTheme = brightness.value == AppBrightness.DARK,
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
                        ChartGallery()
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
        AppMaterial.entries.forEach { m ->
            AppRelief.entries.forEach { r ->
                listOf(AppBrightness.LIGHT, AppBrightness.DARK).forEach { b ->
                    material.value = m
                    relief.value = r
                    brightness.value = b
                    // The charts ANIMATE -- every one of them grows out of zero
                    // over `DEFAULT_DURATION_MS`. `waitForIdle` returns as soon
                    // as composition settles, which on an infinite-clock rule is
                    // long before the sweep finishes, so without this the pass
                    // photographs half-drawn bodies and every frame is a
                    // different half.
                    composeRule.waitForIdle()
                    composeRule.mainClock.advanceTimeBy(SETTLE_MS)
                    composeRule.waitForIdle()

                    val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
                    // Half size, [MaterialRenderPass]'s reason: a full-resolution
                    // PNG of this AVD is ~4 Mpx and sixteen of them are slow to
                    // encode and too heavy to commit. A wall and a sheen both
                    // survive the downscale.
                    val small = Bitmap.createScaledBitmap(
                        full,
                        full.width / 2,
                        full.height / 2,
                        true,
                    )
                    val stem = "charts-" + skin.id + "-" + m.id + "-" + r.id + "-" + b.id
                    val file = File(outDir, "$stem.png")
                    file.outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 90, it) }
                    small.recycle()
                    written += file
                }
            }
        }

        val expected = AppMaterial.entries.size * AppRelief.entries.size * 2
        assertWithMessage("$skin — the matrix is $expected frames")
            .that(written).hasSize(expected)
        written.forEach { file ->
            assertWithMessage("${file.name} was written").that(file.isFile).isTrue()
            assertWithMessage("${file.name} is not empty").that(file.length()).isGreaterThan(1_000L)
        }
    }
}

/**
 * The four chart primitives, one above the other, on the app's real palette.
 *
 * Numbers are the prototype's own week (`docs/prototypes/2026-08-10-charts-presentation/`)
 * so a frame here can be held against a frame there — which is the comparison
 * Ido made when he opened `#57`.
 */
@androidx.compose.runtime.Composable
private fun ChartGallery() {
    val areas = listOf(
        GoalCategory.LEARNING to 420,
        GoalCategory.HEALTH to 260,
        GoalCategory.CAREER to 210,
        GoalCategory.RELATIONSHIPS to 120,
        GoalCategory.FINANCE to 60,
        GoalCategory.OTHER to 40,
    )
    val total = areas.sumOf { it.second }.toFloat()
    val hues = areas.associate { it.first to it.first.defaultColorHex.toGoalAccent() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DonutChart(
                slices = areas.map { (category, minutes) ->
                    DonutSlice(
                        id = category.name,
                        // `#53` draws this now, so it has to be the word a user
                        // reads rather than the enum constant.
                        label = category.localizedLabel(),
                        fraction = minutes / total,
                        color = hues.getValue(category),
                    )
                },
                size = 180.dp,
                thickness = 30.dp,
            ) {
                Text("26 h", style = MaterialTheme.typography.titleMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressRing(progress = 0.8f, size = 84.dp, strokeWidth = 12.dp) {
                    Text("80%", style = MaterialTheme.typography.labelLarge)
                }
                ProgressRing(
                    progress = 0.42f,
                    size = 84.dp,
                    strokeWidth = 12.dp,
                    color = hues.getValue(GoalCategory.HEALTH),
                ) {
                    Text("42%", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        StackedColumnChart(
            columns = WEEK.map { (label, values) ->
                StackedColumn(
                    label = label,
                    segments = values.map { (category, minutes) ->
                        // `#53`'s `.tag`: the label is required now, and the render
                        // pass is a place where the words have to be visible.
                        StackedSegment(
                            id = category.name,
                            label = category.localizedLabel(),
                            color = hues.getValue(category),
                            value = minutes,
                        )
                    },
                )
            },
            maxValue = WEEK.maxOf { it.second.sumOf { pair -> pair.second } },
            height = 128.dp,
        )

        HorizontalBarChart(
            items = areas.take(4).map { (category, minutes) ->
                BarItem(
                    label = category.label,
                    fraction = minutes / 420f,
                    color = hues.getValue(category),
                    trailing = "${minutes / 60}h",
                )
            },
        )
    }
}

/** The prototype's week, bottom-first per column so the stack matches the donut. */
private val WEEK: List<Pair<String, List<Pair<GoalCategory, Int>>>> = listOf(
    "Sun" to listOf(
        GoalCategory.LEARNING to 90, GoalCategory.HEALTH to 40, GoalCategory.CAREER to 30,
    ),
    "Mon" to listOf(
        GoalCategory.LEARNING to 60, GoalCategory.HEALTH to 45, GoalCategory.CAREER to 60,
        GoalCategory.FINANCE to 20,
    ),
    "Tue" to listOf(
        GoalCategory.LEARNING to 80, GoalCategory.HEALTH to 35, GoalCategory.CAREER to 40,
        GoalCategory.RELATIONSHIPS to 30,
    ),
    "Wed" to listOf(
        GoalCategory.LEARNING to 70, GoalCategory.HEALTH to 50, GoalCategory.CAREER to 20,
    ),
    "Thu" to listOf(
        GoalCategory.LEARNING to 60, GoalCategory.HEALTH to 40, GoalCategory.CAREER to 60,
        GoalCategory.RELATIONSHIPS to 30,
    ),
    "Fri" to listOf(
        GoalCategory.LEARNING to 40, GoalCategory.HEALTH to 20, GoalCategory.RELATIONSHIPS to 60,
    ),
    "Sat" to listOf(
        GoalCategory.LEARNING to 20, GoalCategory.HEALTH to 30, GoalCategory.FINANCE to 20,
    ),
)

/**
 * Long enough for every chart's growth to finish.
 *
 * `DEFAULT_DURATION_MS` is 900 and `HorizontalBarChart` staggers its rows by up
 * to `STAGGER_MAX_MS` = 420 on top of that, so the last bar lands at 1320 ms.
 * 2000 clears it with margin — a frame taken mid-sweep is not *wrong*, it is
 * *arbitrary*, and an arbitrary frame is the one thing a comparison cannot use.
 */
private const val SETTLE_MS = 2_000L
