package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AppBrightness
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.PaletteTransform
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
    fun `the default is neo`() {
        // §4.9's defaults table, and its reason: the only material with BOTH a
        // light and a dark scheme AND no blur under it.
        assertThat(AppMaterial.DEFAULT).isEqualTo(AppMaterial.NEO)
        assertThat(AppMaterial.DEFAULT.isBrightnessLocked).isFalse()
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
