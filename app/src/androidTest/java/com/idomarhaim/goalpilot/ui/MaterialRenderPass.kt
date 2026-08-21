package com.idomarhaim.goalpilot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRegion
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.DaySchedule
import com.idomarhaim.goalpilot.feature.settings.SettingsContent
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
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
 * ## Two methods, then two more (`#57` b)
 *
 * One skin each, so a hang in the second leaves the first skin's frames on the
 * device instead of nothing at all — the same reason the assertions are about
 * files existing rather than about pixels.
 *
 * `#57` b added the **ground** as a third axis, and it is a visual change by
 * construction, so it gets the same treatment: [aurora_everyGroundUnderEveryMaterial]
 * and [blossom_everyGroundUnderEveryMaterial] walk 4 materials × 4 grounds ×
 * 2 brightnesses. The frame that has to be looked at hardest is
 * **`*-neo-glow-*`**: `AppBackground` promises in words that neumorphism cannot
 * survive a lit ground and becomes a translucent plate with an edge, and this is
 * the only place that promise can be checked. `*-glass-plain-*` is the same
 * claim in the other direction — a glass panel with nothing to be transparent
 * about.
 *
 * ## What the frames do and do not prove
 *
 * They show §4.9's Appearance section drawn by the real `GoalPilotTheme`, so
 * the palette transform, the `GpMaterialSpec` surface, the page backdrop and
 * every `GpCard` on that screen are all exercised. They do **not** show the
 * rest of the app: this is one screen, chosen because it is the screen the
 * material contract was built to make possible and the one that shows all four
 * materials at once in its own tiles.
 *
 * ⚠️ **The sentence above claimed "the page backdrop" for two days and was
 * false, and `#57` b's first pass is what caught it.** This harness used to put
 * `SettingsContent` straight under `GoalPilotTheme`, while the real app wraps it
 * in `MainActivity`'s `Surface` + `Box.gpPage(...)` — and `gpPage` is the *only*
 * thing that draws a ground. So every frame this pass had ever produced showed
 * glass and liquid glass panels floating on a flat colour the app never renders,
 * which is the one look those two materials are defined against. The wrapper
 * below is a copy of `MainActivity`'s, and it is the reason the ground is
 * visible at all.
 *
 * **How it went unnoticed is the useful half:** a render pass is checked by
 * *looking*, and a frame that is missing a background still looks like a
 * perfectly good screenshot of a settings screen. Nothing in it says a layer is
 * absent. The gap only became obvious once a control existed whose entire
 * subject was that layer.
 */
class MaterialRenderPass {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aurora_everyMaterialInBothBrightnesses() = capture(AppSkin.AURORA)

    @Test
    fun blossom_everyMaterialInBothBrightnesses() = capture(AppSkin.BLOSSOM)

    /** `#57` b — the combination grid, one skin per method for the reason above. */
    @Test
    fun aurora_everyGroundUnderEveryMaterial() =
        capture(AppSkin.AURORA, grounds = AppBackground.entries.toList())

    @Test
    fun blossom_everyGroundUnderEveryMaterial() =
        capture(AppSkin.BLOSSOM, grounds = AppBackground.entries.toList())

    /**
     * @param grounds which backgrounds to walk. The default is [AppBackground.MATCH]
     *   alone — i.e. the per-material grounds these frames showed before `#57` b —
     *   so the original two methods keep producing the original eight filenames and
     *   a diff against the previous pass stays readable.
     */
    private fun capture(
        skin: AppSkin,
        grounds: List<AppBackground> = listOf(AppBackground.MATCH),
    ) {
        val material = mutableStateOf(AppMaterial.NEO)
        val background = mutableStateOf(AppBackground.MATCH)
        val brightness = mutableStateOf(AppBrightness.LIGHT)
        val region = mutableStateOf(AppRegion.SYSTEM)
        val schedule = mutableStateOf(DaySchedule.DEFAULT)

        composeRule.setContent {
            GoalPilotTheme(
                skin = skin,
                material = material.value,
                background = background.value,
                // LIGHT/DARK explicitly rather than SYSTEM: the matrix has to be
                // the one this file names, not the one the emulator happens to
                // be in -- and dark neo overriding it is the thing being shown.
                darkTheme = brightness.value == AppBrightness.DARK,
            ) {
                // A COPY of MainActivity's wrapper, and it has to be: `gpPage`
                // is what draws the ground, it is called in exactly two places
                // in the app, and neither of them is a screen. Without this the
                // pass photographs the screen and not the page it sits on.
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
                SettingsContent(
                    skin = skin,
                    onSkin = {},
                    brightness = brightness.value,
                    onBrightness = { brightness.value = it },
                    material = material.value,
                    onMaterial = { material.value = it },
                    // #57 b's third axis. Explicit rather than defaulted, for the
                    // same reason the AI state is: a default lets a real screen
                    // forget the control and render one that silently does nothing.
                    background = background.value,
                    onBackground = { background.value = it },
                    language = AppLanguage.ENGLISH,
                    onLanguage = {},
                    region = region.value,
                    onRegion = { region.value = it },
                    schedule = schedule.value,
                    onWakingHours = { schedule.value = schedule.value.copy(waking = it) },
                    onPlanningOverrideMinutes = {
                        schedule.value = schedule.value.copy(planningOverrideMinutes = it)
                    },
                    // C13 (#54) added §4.9's fifth section. Passed explicitly
                    // rather than defaulted: a default would let a real screen
                    // forget them and render an AI section that silently does
                    // nothing, which is the one thing that section must not be.
                    // This file is about the MATERIAL contract, so the AI state
                    // is the app's default — no key, nothing asked yet.
                    aiCredential = null,
                    aiLastAnswer = null,
                    onAiCredential = {},
                    onClearAiCredential = {},
                    onBack = {},
                    onOpenProfile = {},
                )
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
            grounds.forEach { g ->
                listOf(AppBrightness.LIGHT, AppBrightness.DARK).forEach { b ->
                    material.value = m
                    background.value = g
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
                    // The ground goes in the filename only when there is more than
                    // one of them, so the original eight keep their original names
                    // and a before/after comparison is a plain `diff` of two pulls.
                    val stem = if (grounds.size > 1) {
                        skin.id + "-" + m.id + "-" + g.id + "-" + b.id
                    } else {
                        skin.id + "-" + m.id + "-" + b.id
                    }
                    val file = File(outDir, "$stem.png")
                    file.outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 90, it) }
                    small.recycle()
                    written += file
                }
            }
        }

        // materials x grounds x brightnesses. Dark neo's brightness pairs are
        // expected to be IDENTICAL -- that pair is the brightness lock, seen --
        // and so are its MATCH and PLAIN columns, because PLAIN is what MATCH
        // resolves to for it.
        val expected = AppMaterial.entries.size * grounds.size * 2
        assertWithMessage("$skin — the matrix is $expected frames")
            .that(written).hasSize(expected)
        written.forEach { file ->
            assertWithMessage("${file.name} was written").that(file.isFile).isTrue()
            assertWithMessage("${file.name} is not empty").that(file.length()).isGreaterThan(1_000L)
        }
    }
}
