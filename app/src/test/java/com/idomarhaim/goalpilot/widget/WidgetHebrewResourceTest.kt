package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The widget pack's Hebrew strings must live in `values-iw/`, not `values-he/`.
 *
 * This looks like pedantry and is not. `Observed:` 2026-08-16, Samsung SM-S938B,
 * system locale `he-IL` — the tile rendered **English** while its layout mirrored
 * correctly to RTL. That split is the tell: the host knew the device was Hebrew,
 * and the string lookup did not.
 *
 * Java reports Hebrew with the legacy code **`iw`**, so the runtime asks the
 * resource table for the `iw` bucket. AAPT2 does **not** fold `values-he` into
 * it — the built APK carried *both* buckets, with these strings in `he` and
 * AndroidX's in `iw`, so every lookup fell through to the default. Proved on the
 * device by logging the compiled id: `0x7f0f0042`, name `gp_widget_level`,
 * context locale `he_IL`, value `"Level"` — while `aapt2 dump` of that same
 * on-device APK showed `(he) "רמה"` for that exact id.
 *
 * A `values-he` copy is worse than no copy: it looks like the translation is
 * done, reviews as done, and does nothing. So this fails if one comes back —
 * which is the likely accident, since `values-he` is what every tutorial writes
 * and what an IDE offers.
 *
 * Deliberately a file-existence test rather than a rendering test: the failure is
 * in resource *packing*, which no JVM test can exercise and which the device
 * needed four builds to pin down. Cheap, and it guards the exact regression.
 */
class WidgetHebrewResourceTest {

    private val res = listOf(File("src/main/res"), File("app/src/main/res"))
        .firstOrNull { it.isDirectory }
        ?: error("res/ not found from ${File(".").absolutePath}")

    @Test
    fun `the widget's Hebrew strings are in the iw bucket`() {
        assertThat(File(res, "values-iw/widget_strings.xml").exists()).isTrue()
    }

    @Test
    fun `no values-he copy of the widget strings exists`() {
        val stray = File(res, "values-he/widget_strings.xml")
        assertWithMessage(
            "values-he/widget_strings.xml is back — it will silently do nothing " +
                "on a Hebrew device; see this test's KDoc",
        ).that(stray.exists()).isFalse()
    }

    @Test
    fun `every English widget string has a Hebrew counterpart`() {
        // A missing key falls back to English on a Hebrew device, which is the
        // same silent half-translation in a different disguise.
        val english = names(File(res, "values/widget_strings.xml"))
        val hebrew = names(File(res, "values-iw/widget_strings.xml"))
        assertThat(english).isNotEmpty()
        assertThat(hebrew).containsExactlyElementsIn(english)
    }

    private fun names(file: File): Set<String> {
        check(file.exists()) { "missing ${file.path}" }
        val text = file.readText()
        return (
            Regex("""<string name="([^"]+)"""").findAll(text) +
                Regex("""<plurals name="([^"]+)"""").findAll(text)
            ).map { it.groupValues[1] }.toSet()
    }
}
