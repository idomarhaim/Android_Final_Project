package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AppSkin
import org.junit.Test
import java.io.File

/**
 * `values/` and `values-night/` `widget_colors.xml` is a **projection** of `WidgetPalette`'s own
 * arithmetic. This is what stops it drifting away from the thing it projects.
 *
 * The projection exists because a `RemoteViews` is inflated later, by the
 * launcher, so the only way a tile can follow the device's dark-mode switch is
 * to ship both answers as a resource and let the host resolve it (`Observed:`
 * 2026-08-16 — the runtime-computed version stayed light in dark mode, through a
 * forced update). The cost of that fix is a second copy of every colour, in a
 * format Kotlin cannot compute. A second copy nobody checks is a copy that goes
 * wrong quietly — §4.1's *a skin picker no material reads looks correct in
 * source*, one layer down — so it is checked here.
 *
 * The XML is parsed as text rather than through Android resources on purpose:
 * this stays a JVM test with no Robolectric, and reading the file is exactly
 * what catches a hand-edit.
 */
class WidgetPaletteResourceTest {

    private val light = parse("src/main/res/values/widget_colors.xml")
    private val night = parse("src/main/res/values-night/widget_colors.xml")

    @Test
    fun `every declared colour equals the arithmetic it was derived from`() {
        val mismatches = mutableListOf<String>()
        for (skin in AppSkin.entries) {
            for ((isDark, declared) in listOf(false to light, true to night)) {
                val computed = com.idomarhaim.goalpilot.ui.widget.WidgetPalette.computed(skin, isDark)
                val prefix = "gp_widget_${skin.id}_"
                fun check(name: String, expected: Int) {
                    val actual = declared["$prefix$name"]
                        ?: return mismatches.plusAssign("$prefix$name missing (dark=$isDark)")
                    if (actual != expected) {
                        mismatches += "$prefix$name dark=$isDark declared=${hex(actual)} computed=${hex(expected)}"
                    }
                }
                check("ground", computed.ground)
                check("on_surface", computed.onSurface)
                check("on_surface_variant", computed.onSurfaceVariant)
                check("accent", computed.accent)
            }
        }
        assertThat(mismatches).isEmpty()
    }

    @Test
    fun `both files declare exactly the same names`() {
        // A name present in one and not the other resolves to the light value in
        // dark mode, which is the original bug wearing a different hat.
        assertThat(light.keys).isEqualTo(night.keys)
    }

    @Test
    fun `every skin has a full set, so adding a skin cannot half-land`() {
        for (skin in AppSkin.entries) {
            val names = light.keys.filter { it.startsWith("gp_widget_${skin.id}_") }
            assertThat(names).hasSize(4)
        }
        assertThat(light).hasSize(AppSkin.entries.size * 4)
    }

    private fun hex(argb: Int) = "#%08X".format(argb)

    /**
     * Resolved against the module directory, which is where Gradle runs a unit
     * test from — not the repo root. Both are tried so the suite also passes
     * when run from an IDE configured the other way.
     */
    private fun parse(path: String): Map<String, Int> {
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.exists() }
            ?: error("missing $path — tried ./ and ./app/ from ${File(".").absolutePath}")
        return Regex("""<color name="([^"]+)">#([0-9A-Fa-f]{8})</color>""")
            .findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2].toLong(16).toInt() }
    }
}
