package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.PaletteTransform
import java.io.File
import org.junit.Test

/**
 * Spec §4.1's material contract, at the layer where it is a **declaration**.
 *
 * Everything here is a claim `AppMaterial` makes about itself before a single
 * pixel is drawn — which transform it applies, whether it has both
 * brightnesses, and what it persists as. §4.1 says the two consequences that
 * *"would otherwise be found late"* are exactly of this kind, so they are
 * asserted here rather than left to a render pass that only looks at whichever
 * combination happened to be on screen.
 *
 * Mirrors `AppSkinTest`, including the no-copy-in-the-enum guard: a label in a
 * constructor argument is unreachable by a language switch
 * (`kb/dev/untranslatable-idioms.md` §1), and this enum was written after that
 * was already known.
 */
class AppMaterialTest {

    @Test
    fun `four materials ship, and metal is not one of them`() {
        // §4.1: "Four materials ship as a user-selectable skin — glassmorphism ·
        // liquid glass · neo · dark neo. Metal is deleted."
        assertThat(AppMaterial.entries.map { it.name })
            .containsExactly("GLASS", "LIQUID_GLASS", "NEO", "DARK_NEO")
            .inOrder()
    }

    @Test
    fun `the default is glassmorphism, and it is not brightness-locked`() {
        // CHANGED 2026-08-24 by `visual-parity`. This asserted NEO, on §4.9's
        // defaults table and its stated reason -- "the only material with BOTH a
        // light and a dark scheme AND no blur under it".
        //
        // The second half of that reason is FALSE about the code that shipped.
        // `#57` b drew glass and liquid glass as translucent panels over a
        // gradient backdrop precisely BECAUSE Compose has no backdrop filter, so
        // neither material uses `Modifier.blur` or `RenderEffect` at all -- see
        // `assert no material depends on an API-gated primitive` below, which is
        // the guard that makes this change safe rather than merely intended.
        //
        // What the old default cost: `AppBackground.MATCH` (also the default)
        // resolves neo to `PLAIN` -- "one flat tone, no lights at all" -- so a
        // fresh install was opaque, unlit and flat, and all four presentation
        // features `#57` shipped were off until the user found the picker. Ido
        // reported the app as not matching the prototypes on 2026-08-21 and
        // AGAIN on 2026-08-24, after all four had landed.
        assertThat(AppMaterial.DEFAULT).isEqualTo(AppMaterial.GLASS)

        // Unchanged and still the load-bearing half: a brightness-locked default
        // would make the light/dark switch silently do nothing on first run.
        assertThat(AppMaterial.DEFAULT.isBrightnessLocked).isFalse()
    }

    @Test
    fun `assert no material depends on an API-gated primitive`() {
        // The guard the default change above rests on, and the reason it is a
        // measurement rather than a promise.
        //
        // §4.9 justified defaulting to neo with "Glass and liquid glass are
        // `Modifier.blur` / `RenderEffect` -- API 31+, with a fallback below
        // that changes the look". `minSdk` is 26, so if that were true of the
        // shipped code, defaulting to glass would ship a different-looking app
        // to every device below API 31 and this session would be wrong.
        //
        // It is not true: `#57` b deliberately drew glass as a TRANSLUCENT panel
        // over `GpMaterialSpec.backdrop` rather than a blurred one, because
        // Compose has no backdrop filter in the first place. This asserts that,
        // so that anyone who later reaches for a real blur is told -- by a test,
        // in the same commit -- that they have just made the default unsafe.
        //
        // Reads the source the way `DocsCurrencyTest` and `MaterialVocabularyTest`
        // already do: this is a claim ABOUT the code, and there is no runtime
        // value that carries it.
        val repoRoot = listOf(File("."), File(".."))
            .map { it.canonicalFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("repo root not found from ${File(".").canonicalPath}")

        // Built by concatenation on purpose: writing the two-character comment
        // opener as a literal inside this file would open a NESTED Kotlin block
        // comment, whose failure is an "Unclosed comment" pointing at the last
        // line of the file (see CLAUDE.md).
        val blockComment = Regex("/" + "\\*" + "[\\s\\S]*?" + "\\*" + "/")
        val lineComment = Regex("//[^\\n]*")
        val gated = listOf("Modifier.blur", "RenderEffect", "BlurMaskFilter")

        val offenders = File(repoRoot, "app/src/main/java/com/idomarhaim/goalpilot/ui")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val code = file.readText()
                    .replace(blockComment, " ")
                    .replace(lineComment, " ")
                gated.filter { code.contains(it) }.map { "${file.name}: $it" }
            }
            .toList()

        assertWithMessage(
            "A material now depends on an API-gated primitive, so AppMaterial.DEFAULT = GLASS is " +
                "no longer safe on minSdk 26. Either gate it and restore a non-blurred default, " +
                "or raise minSdk. Offenders",
        ).that(offenders).isEmpty()
    }

    @Test
    fun `the default resolves to a lit ground rather than a flat one`() {
        // The regression this session actually exists to prevent, and it is NOT
        // expressible on `AppMaterial` alone -- which is why nothing caught it.
        // Neither default was wrong by itself; the PAIR was. Assert the pair.
        assertThat(AppBackground.DEFAULT.resolve(AppMaterial.DEFAULT))
            .isNotEqualTo(AppBackground.PLAIN)
        assertThat(AppBackground.DEFAULT.isLit(AppMaterial.DEFAULT)).isTrue()
    }

    @Test
    fun `every material declares a palette transform, and exactly one declares the ramp`() {
        // §4.1's first named consequence, in its general form: a material that
        // did not read the skin at all would satisfy the type and render Aurora
        // for every choice. The ramp is what carries the skin into dark neo.
        assertThat(AppMaterial.entries.map { it.paletteTransform }).doesNotContain(null)
        assertThat(AppMaterial.entries.filter { it.paletteTransform == PaletteTransform.SINGLE_ACCENT_RAMP })
            .containsExactly(AppMaterial.DARK_NEO)
    }

    @Test
    fun `the two translucent materials take the skin unchanged and neo mutes it`() {
        assertThat(AppMaterial.GLASS.paletteTransform).isEqualTo(PaletteTransform.IDENTITY)
        assertThat(AppMaterial.LIQUID_GLASS.paletteTransform).isEqualTo(PaletteTransform.IDENTITY)
        assertThat(AppMaterial.NEO.paletteTransform).isEqualTo(PaletteTransform.MUTE)
    }

    @Test
    fun `dark neo is the only brightness-locked material, and it locks to dark`() {
        // §4.1's second named consequence: "The product is ragged, not
        // rectangular. Dark neo has no light scheme, so a material must be able
        // to declare itself brightness-locked."
        assertThat(AppMaterial.entries.filter { it.isBrightnessLocked })
            .containsExactly(AppMaterial.DARK_NEO)
        assertThat(AppMaterial.DARK_NEO.lockedDark).isTrue()
    }

    @Test
    fun `a locked material ignores the brightness setting, an unlocked one obeys it`() {
        // The whole of the lock, in one place -- and the reason it is one
        // function rather than an `if` at each call site: the picker's caption
        // and the theme's rendering must not be able to disagree.
        assertThat(AppMaterial.DARK_NEO.resolveDark(requestedDark = false)).isTrue()
        assertThat(AppMaterial.DARK_NEO.resolveDark(requestedDark = true)).isTrue()

        AppMaterial.entries.filterNot { it.isBrightnessLocked }.forEach { material ->
            assertWithMessage(material.name)
                .that(material.resolveDark(requestedDark = false)).isFalse()
            assertWithMessage(material.name)
                .that(material.resolveDark(requestedDark = true)).isTrue()
        }
    }

    @Test
    fun `the lock beats every brightness setting, including follow-the-device`() {
        // SYSTEM is the one that would slip through a lock applied only to the
        // two explicit values -- it resolves to whatever the phone says, so a
        // device in light mode would render dark neo light.
        AppBrightness.entries.forEach { setting ->
            listOf(false, true).forEach { deviceIsDark ->
                val requested = setting.isDark(deviceIsDark)
                assertWithMessage("$setting on a ${if (deviceIsDark) "dark" else "light"} device")
                    .that(AppMaterial.DARK_NEO.resolveDark(requested))
                    .isTrue()
            }
        }
    }

    @Test
    fun `ids round-trip and are stable`() {
        // These strings are in SharedPreferences on real phones. Renaming one
        // silently resets that phone to the default.
        AppMaterial.entries.forEach { material ->
            assertWithMessage(material.name)
                .that(AppMaterial.fromId(material.id)).isEqualTo(material)
        }
        assertThat(AppMaterial.entries.map { it.id })
            .containsExactly("glass", "liquid", "neo", "darkneo")
    }

    @Test
    fun `an unknown or absent id falls back to the default rather than throwing`() {
        // "metal" is the deleted fifth material, and it is the id most likely to
        // be sitting in a preference file somewhere.
        assertThat(AppMaterial.fromId("metal")).isEqualTo(AppMaterial.DEFAULT)
        assertThat(AppMaterial.fromId(null)).isEqualTo(AppMaterial.DEFAULT)
        assertThat(AppMaterial.fromId("")).isEqualTo(AppMaterial.DEFAULT)
        assertThat(AppMaterial.fromId("NEO")).isEqualTo(AppMaterial.NEO)
    }

    @Test
    fun `the enum carries no user-facing copy`() {
        // Same guard as AppSkinTest: a language switch cannot reach a
        // constructor argument, so the words live in res/ and the enum carries
        // only what is persisted or declared.
        val allowed = setOf("id", "paletteTransform", "lockedDark", "isBrightnessLocked")
        val declared = AppMaterial::class.java.declaredMethods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 }
            .map { it.name.removePrefix("get").replaceFirstChar(Char::lowercaseChar) }
            .filterNot { it == "entries" || it == "\$VALUES" }
        assertWithMessage("AppMaterial must declare no copy — see AppSkin's KDoc")
            .that(declared.toSet() - allowed)
            .isEmpty()
    }
}
