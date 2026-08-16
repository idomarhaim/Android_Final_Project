package com.idomarhaim.goalpilot.resources

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The app-wide Hebrew resource contract (issue #51, spec §4.8 / §5.1 / §0.8).
 *
 * ### 1 · The bucket is `values-iw`, and `values-he` is a silent no-op
 *
 * `Observed:` 2026-08-16 by session `widget-pack` on a Samsung SM-S938B with
 * system locale `he-IL` — the widget rendered **English while its layout
 * mirrored correctly to RTL**. That split is the whole tell: the host knew the
 * device was Hebrew and the string lookup did not.
 *
 * **AAPT2 stores Hebrew under the legacy qualifier `iw` and does not fold
 * `values-he` into it** — the APK carries *both* buckets, and every lookup falls
 * through to the default. Proved on the device by logging the compiled id
 * (`0x7f0f0042`, `gp_widget_level`, context locale `he_IL`, value `"Level"`)
 * while `aapt2 dump` of that same on-device APK showed `(he) "רמה"` for that
 * exact id.
 *
 * ⚠️ **Not because `Locale` says `iw` — it does not.** That explanation is the
 * one everybody gives and it is false on current Android: `Observed:` API 37,
 * 2026-08-16, `Locale.forLanguageTag("he").language` returns **`"he"`**, and
 * `values-iw/` still resolves (`AppLocaleInstrumentedTest`). The bucket is a
 * fact about the resource system alone. This matters here because the folk
 * explanation invites exactly the check that now says "rename it to
 * `values-he`" — which is the defect, not the fix.
 *
 * **A `values-he` copy is worse than no copy**: it looks like the translation is
 * done, reviews as done, and does nothing. `WidgetHebrewResourceTest` guards the
 * widget's own file; this guards the *directory*, for every file anyone adds
 * next — which is the accident worth guarding, since `values-he` is what every
 * tutorial writes and what the IDE's translation editor offers.
 *
 * ### 2 · Parity, in both directions
 *
 * A key present in `values/` and missing from `values-iw/` falls back to English
 * on a Hebrew device — the same silent half-translation in a different disguise,
 * and the one this ticket's sweep creates most easily. A key present only in
 * `values-iw/` is dead weight that no render can reach.
 *
 * ### 3 · No Hebrew literal survives into an English render (§4.8)
 *
 * Asserting this **absolutely** is what caught three instances beyond the one
 * Ido spotted, so it is asserted absolutely here: no Hebrew codepoint may appear
 * anywhere under `values/`.
 *
 * ### 4 · An untranslated copy is not a translation
 *
 * A Hebrew string byte-identical to its English one is almost always a
 * copy-paste that was never translated. The two real exceptions are exempted by
 * what makes them exceptions, not by being listed: a string carrying **no
 * letters at all** (`%1$d%%`) is untranslatable by construction, and a string
 * that carries words but is still language-independent (the brand name) is what
 * aapt's own `translatable="false"` is for — which also, correctly, excuses it
 * from the parity check above.
 *
 * Deliberately file-existence and file-content tests rather than rendering
 * tests: the failure is in resource *packing* and resource *authoring*, neither
 * of which a JVM test can exercise through the runtime — and the packing half
 * took the device four build cycles to pin down.
 */
class HebrewLocaleResourceTest {

    private val res = listOf(File("src/main/res"), File("app/src/main/res"))
        .firstOrNull { it.isDirectory }
        ?: error("res/ not found from ${File(".").absolutePath}")

    private val default = File(res, "values")
    private val hebrew = File(res, "values-iw")

    @Test
    fun `no values-he directory exists anywhere in res`() {
        val strays = res.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.split("-").contains("he") }
        assertWithMessage(
            "A `he` resource qualifier silently does nothing on a Hebrew device — " +
                "Java asks for the `iw` bucket. Rename to `values-iw`; see this test's KDoc.",
        ).that(strays.map { it.name }).isEmpty()
    }

    @Test
    fun `the hebrew bucket exists and carries strings`() {
        assertThat(hebrew.isDirectory).isTrue()
        assertThat(stringFiles(hebrew)).isNotEmpty()
    }

    @Test
    fun `every translatable english string has a hebrew counterpart`() {
        val missing = mutableListOf<String>()
        stringFiles(default).forEach { file ->
            val counterpart = File(hebrew, file.name)
            val english = translatableNames(file)
            if (english.isEmpty()) return@forEach
            val translated = if (counterpart.exists()) names(counterpart) else emptySet()
            (english - translated).forEach { missing += "${file.name}:$it" }
        }
        assertWithMessage(
            "These keys fall back to English on a Hebrew device. Add them to " +
                "res/values-iw/, or mark them translatable=\"false\" if they are " +
                "genuinely language-independent.",
        ).that(missing.sorted()).isEmpty()
    }

    @Test
    fun `no hebrew string is orphaned`() {
        val orphans = mutableListOf<String>()
        stringFiles(hebrew).forEach { file ->
            val counterpart = File(default, file.name)
            val english = if (counterpart.exists()) names(counterpart) else emptySet()
            (names(file) - english).forEach { orphans += "${file.name}:$it" }
        }
        assertWithMessage(
            "No English key matches these, so no render can reach them.",
        ).that(orphans.sorted()).isEmpty()
    }

    @Test
    fun `no hebrew literal appears in the default resources`() {
        val offenders = stringFiles(default)
            .filter { HEBREW.containsMatchIn(it.readText()) }
            .map { it.name }
        assertWithMessage(
            "§4.8: no Hebrew literal may survive into an English render.",
        ).that(offenders).isEmpty()
    }

    @Test
    fun `no hebrew string is an untranslated copy of its english original`() {
        val copies = mutableListOf<String>()
        stringFiles(hebrew).forEach { file ->
            val counterpart = File(default, file.name)
            if (!counterpart.exists()) return@forEach
            val english = values(counterpart)
            values(file).forEach { (key, value) ->
                if (english[key] == value && carriesWords(value)) copies += "${file.name}:$key"
            }
        }
        assertWithMessage(
            "Identical to the English. Translate it, or — if it carries no words " +
                "of its own — mark it translatable=\"false\" in res/values/.",
        ).that(copies.sorted()).isEmpty()
    }

    /**
     * True when [value] has any letter left once its format specifiers are gone.
     *
     * The strip is the whole point: `%1$d%%` is untranslatable by construction,
     * but its conversion character `d` **is** a letter, so asking `\p{L}` of the
     * raw string calls every numeric format a missing translation.
     */
    private fun carriesWords(value: String): Boolean =
        LETTER.containsMatchIn(SPECIFIER.replace(value, ""))

    // ------------------------------------------------------------------ helpers

    private fun stringFiles(dir: File): List<File> =
        dir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "xml" }
            .filter { DECLARATION.containsMatchIn(it.readText()) }
            .sortedBy { it.name }

    private fun names(file: File): Set<String> = declarations(file).map { it.second }.toSet()

    /**
     * Names minus `translatable="false"`, which is aapt's own way of saying "this
     * one is not language-dependent" and is therefore not owed a translation.
     */
    private fun translatableNames(file: File): Set<String> =
        declarations(file).filterNot { it.first.contains("translatable=\"false\"") }
            .map { it.second }
            .toSet()

    private fun declarations(file: File): List<Pair<String, String>> =
        DECLARATION.findAll(file.readText())
            .map { it.value to it.groupValues[2] }
            .toList()

    /** Key → its text content, for the untranslated-copy check. `<plurals>` are skipped. */
    private fun values(file: File): Map<String, String> =
        STRING_WITH_BODY.findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2].trim() }

    private companion object {
        /**
         * Group 1 is the whole attribute run, group 2 the `name`.
         *
         * `(?![-\w])` rather than `\b`: `\b` matches between `string` and the
         * hyphen of `<string-array`, so the simpler pattern silently treats a
         * string-array as a string and then mis-pairs its body in [values].
         */
        val DECLARATION = Regex("""<(?:string|plurals)(?![-\w])([^>]*?)name="([^"]+)"([^>]*)>""")

        val STRING_WITH_BODY =
            Regex("""<string\b[^>]*?name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

        val HEBREW = Regex("""\p{IsHebrew}""")

        /**
         * Any letter, in any script — applied only *after* [SPECIFIER] is
         * stripped, per [carriesWords].
         *
         * A string with nothing left — `%1$d%%`, `%1$s · %2$s` — carries no
         * words, so it is untranslatable *by construction* and its two copies
         * are identical for a reason no reviewer needs to re-confirm. That is a
         * rule rather than an allowlist, so the next format-only string does not
         * have to be remembered. Strings that *do* carry words and still stay
         * identical (a brand name) are handled by aapt's own
         * `translatable="false"`, which also keeps them out of parity above.
         */
        val LETTER = Regex("""\p{L}""")

        /** A Java format specifier, `%%` included. */
        val SPECIFIER = Regex("""%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]""")
    }
}
