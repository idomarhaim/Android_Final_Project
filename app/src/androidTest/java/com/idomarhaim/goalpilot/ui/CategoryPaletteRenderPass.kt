package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.DonutChart
import com.idomarhaim.goalpilot.ui.components.DonutSlice
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.components.toGoalInk
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for `#57` a's category palette — **not a test, a camera.**
 *
 * `ThemePaletteTest` can prove the set is separable, in gamut and readable, and
 * it proves all three over fourteen schemes without an emulator. What it cannot
 * do is answer the question the ticket was actually opened about: *do ten of
 * these sitting side by side in one donut hold together, or do they read as
 * crayons?* That is not a number, so this walks the matrix and writes frames.
 *
 * It renders the **real** [DonutChart] with the **real** ten categories through
 * the **real** [GoalPilotTheme], plus a legend row per category showing the three
 * things a category actually paints: the fill as a dot, the fill as an icon tint,
 * and the derived ink as type. Ten slices, deliberately equal, because equal
 * sixths is the arrangement that gives the eye no size cue and therefore the
 * hardest colour comparison.
 *
 * ## No sign-in, on purpose
 *
 * This composes the components directly rather than driving the app, so it needs
 * no account and cannot be invalidated by a wiped emulator. That matters here:
 * `#58` left this AVD's Firebase auth store gone, and a palette is exactly the
 * kind of thing that should not have to wait on a human to log in.
 *
 * ## Running it without uninstalling the app
 *
 * `connectedDebugAndroidTest` uninstalls, which would take these PNGs with it
 * (`kb/dev/android-device-verification.md` §8). Install both APKs and drive the
 * runner directly — and wake the device first, or `captureToImage` blocks
 * forever on a screen that never produces a frame:
 *
 * ```
 * adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.CategoryPaletteRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/palette-pass
 * ```
 */
class CategoryPaletteRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aurora_everyMaterialInBothBrightnesses() = capture(AppSkin.AURORA)

    @Test
    fun blossom_everyMaterialInBothBrightnesses() = capture(AppSkin.BLOSSOM)

    private fun capture(skin: AppSkin) {
        val material = mutableStateOf(AppMaterial.GLASS)
        val brightness = mutableStateOf(AppBrightness.LIGHT)

        composeRule.setContent {
            // LIGHT/DARK explicitly rather than SYSTEM, for the reason
            // MaterialRenderPass gives: the matrix has to be the one this file
            // names and not the one the emulator happens to be in.
            GoalPilotTheme(
                skin = skin,
                material = material.value,
                darkTheme = brightness.value == AppBrightness.DARK,
            ) {
                PaletteBoard()
            }
        }

        val outDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "palette-pass",
        )
        outDir.mkdirs()

        val written = mutableListOf<File>()
        AppMaterial.entries.forEach { m ->
            listOf(AppBrightness.LIGHT, AppBrightness.DARK).forEach { b ->
                material.value = m
                brightness.value = b
                composeRule.waitForIdle()

                val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
                val small = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
                val file = File(outDir, "${skin.id}-${m.id}-${b.id}.png")
                file.outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 90, it) }
                small.recycle()
                written += file
            }
        }

        assertWithMessage("$skin — the matrix is 8 frames").that(written).hasSize(8)
        written.forEach { file ->
            assertWithMessage("${file.name} was written").that(file.isFile).isTrue()
            assertWithMessage("${file.name} is not empty").that(file.length()).isGreaterThan(0L)
        }
    }
}

@Composable
private fun PaletteBoard() {
    val categories = GoalCategory.entries.toList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GpCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DonutChart(
                    slices = categories.map {
                        DonutSlice(
                            id = it.name,
                            label = it.label,
                            // Equal slices: no size cue, so the only thing
                            // separating two wedges is their colour.
                            fraction = 1f / categories.size,
                            color = it.defaultColorHex.toGoalAccent(),
                        )
                    },
                    size = 190.dp,
                    thickness = 30.dp,
                )
            }
        }
        GpCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                categories.forEachIndexed { index, category ->
                    LegendRow(category, percent = 4 + index * 9)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(category: GoalCategory, percent: Int) {
    val fill = category.defaultColorHex.toGoalAccent()
    val ink = category.defaultColorHex.toGoalInk()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(12.dp).clip(CircleShape).background(fill))
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = iconForKey(category.iconKey),
            contentDescription = null,
            tint = fill,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ink,
        )
    }
}
