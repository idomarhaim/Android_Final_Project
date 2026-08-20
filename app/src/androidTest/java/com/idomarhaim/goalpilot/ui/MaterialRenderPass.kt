package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.feature.settings.SettingsContent
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The render pass for `C12` #53 — **not a test, a camera.**
 *
 * The brief for this unit is explicit that the thing being shipped cannot be
 * checked by assertions:
 *
 * > **This cannot be verified by tests.** Four materials × two brightnesses ×
 * > two skins is a visual matrix, and every one of the three "found late" items
 * > is invisible to a green suite. Render them and **look**, and put the images
 * > in the changelog.
 *
 * So this walks the matrix and writes one PNG per cell into the app's external
 * files directory, for `adb pull`. It asserts only that the files were
 * *written* — a render pass that silently produced nothing is the one failure
 * mode that would leave a green run standing in for a look nobody took.
 *
 * ## Run it without destroying the device's Google account
 *
 * `connectedDebugAndroidTest` **uninstalls the app when it finishes**, which
 * takes the signed-in account *and* every PNG below with it
 * (`kb/dev/android-device-verification.md` §8). Install both APKs and drive the
 * runner directly instead — and note the runner is **this app's**, not the
 * generic one, or `am` answers *"Unable to find instrumentation info"*:
 *
 * ```
 * adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e class com.idomarhaim.goalpilot.ui.MaterialRenderPass \
 *   com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner
 * adb pull /sdcard/Android/data/com.idomarhaim.goalpilot.debug/files/render-pass
 * ```
 *
 * ⚠️ **Wake and unlock the device first — that line is not decoration.**
 * `captureToImage` goes through `PixelCopy`, which waits for the window to
 * produce a frame. A screen-off emulator produces none, so the call blocks
 * **forever at 0% CPU** and takes `adb shell` and `logcat` down with it — a
 * hang that looks exactly like a slow render and needs the AVD killed.
 * `Observed:` 2026-08-20, first attempt at this pass.
 *
 * ## Two methods, not one
 *
 * One skin each, so a hang in the second leaves the first skin's frames on the
 * device instead of nothing at all — the same reason the assertions are about
 * files existing rather than about pixels.
 *
 * ## What the frames do and do not prove
 *
 * They show §4.9's Appearance section drawn by the real `GoalPilotTheme`, so
 * the palette transform, the `GpMaterialSpec` surface, the page backdrop and
 * every `GpCard` on that screen are all exercised. They do **not** show the
 * rest of the app: this is one screen, chosen because it is the screen the
 * material contract was built to make possible and the one that shows all four
 * materials at once in its own tiles.
 */
class MaterialRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aurora_everyMaterialInBothBrightnesses() = capture(AppSkin.AURORA)

    @Test
    fun blossom_everyMaterialInBothBrightnesses() = capture(AppSkin.BLOSSOM)

    private fun capture(skin: AppSkin) {
        val material = mutableStateOf(AppMaterial.NEO)
        val brightness = mutableStateOf(AppBrightness.LIGHT)
        val region = mutableStateOf(AppRegion.SYSTEM)
        val schedule = mutableStateOf(DaySchedule.DEFAULT)

        composeRule.setContent {
            GoalPilotTheme(
                skin = skin,
                material = material.value,
                // LIGHT/DARK explicitly rather than SYSTEM: the matrix has to be
                // the one this file names, not the one the emulator happens to
                // be in -- and dark neo overriding it is the thing being shown.
                darkTheme = brightness.value == AppBrightness.DARK,
            ) {
                SettingsContent(
                    skin = skin,
                    onSkin = {},
                    brightness = brightness.value,
                    onBrightness = { brightness.value = it },
                    material = material.value,
                    onMaterial = { material.value = it },
                    language = AppLanguage.ENGLISH,
                    onLanguage = {},
                    region = region.value,
                    onRegion = { region.value = it },
                    schedule = schedule.value,
                    onWakingHours = { schedule.value = schedule.value.copy(waking = it) },
                    onPlanningOverrideMinutes = {
                        schedule.value = schedule.value.copy(planningOverrideMinutes = it)
                    },
                    onBack = {},
                    onOpenProfile = {},
                )
            }
        }

        val outDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "render-pass",
        )
        outDir.mkdirs()

        val written = mutableListOf<File>()
        AppMaterial.entries.forEach { m ->
            listOf(AppBrightness.LIGHT, AppBrightness.DARK).forEach { b ->
                material.value = m
                brightness.value = b
                composeRule.waitForIdle()

                val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
                // Downscaled before encoding: a full-resolution PNG of this AVD
                // is ~4 Mpx, and 16 of them is both slow to encode and too heavy
                // to commit. Half size still shows a shadow pair and a rim.
                val small = Bitmap.createScaledBitmap(
                    full,
                    full.width / 2,
                    full.height / 2,
                    true,
                )
                val file = File(outDir, "${skin.id}-${m.id}-${b.id}.png")
                file.outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 90, it) }
                small.recycle()
                written += file
            }
        }

        // 4 materials x 2 brightnesses. Dark neo's two frames are expected to be
        // IDENTICAL -- that pair is the brightness lock, seen.
        assertWithMessage("$skin — the matrix is 8 frames").that(written).hasSize(8)
        written.forEach { file ->
            assertWithMessage("${file.name} was written").that(file.isFile).isTrue()
            assertWithMessage("${file.name} is not empty").that(file.length()).isGreaterThan(1_000L)
        }
    }
}
