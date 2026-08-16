package com.idomarhaim.goalpilot.resources

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The two wording rules every `values-iw/` file obeys, as a test (#51).
 *
 * ### Why a test and not a header comment
 *
 * `analytics_strings.xml` already stated both rules in its header, and the widget
 * pack — written afterwards, in the same `res/` tree — still shipped **eight**
 * wrong resources: six on rule 1, three on rule 2, one on both. This is the same
 * argument [AnalyticsLiteralSweepTest] makes about sweeps: a translation is clean
 * the day it lands and drifts on the next string somebody adds in a hurry, and
 * **the one person who would notice is the Hebrew reader, who is not reviewing
 * the diff.**
 *
 * The brief that commissioned the fix listed nine defective lines and missed
 * `gp_widget_effort_lead`, which carries rule 2's defect twice. That is the
 * argument for a regex over a reading, not a claim about the brief: a careful
 * human enumeration of one 80-line file was already incomplete.
 *
 * Neither defect is reachable from any other layer. A wrong noun compiles, packs,
 * and renders; `WidgetHebrewResourceTest` compares resource *names* and is blind
 * to values by construction; and rule 2's failure is a property of the Unicode
 * bidi algorithm at draw time, which no JVM test can exercise. What *is* checkable
 * is the string that causes it, so that is what this checks.
 *
 * ### Rule 1 — the `Goal` entity is יעד, never מטרה
 *
 * Spec §5.1 / `E1`, ratified by Ido on 2026-08-16. `Product and UX Reviews/`
 * `2026-08-09-entity-model-brief.md` `E1` is the record: *"the more correct
 * wording is probably **יעד** … so from now on I will say **יעד**, and you need
 * to know that when I said **מטרה** earlier I meant the same thing."*
 *
 * Comments are stripped before the check, because three headers legitimately
 * contain the word in order to forbid it — and a guard that fires on its own
 * documentation is a guard people delete.
 *
 * ### Rule 2 — no Hebrew prefix attached to a format argument (§4.8)
 *
 * `ל־%1$d`, `ב־%2$s`, `ה%1$s`: the Unicode bidi algorithm resolves a Latin or
 * digit run's direction from the *paragraph*, so an RTL prefix bonded to the
 * front of one lands against the run's **last** word instead of its first —
 * `מ‑Health Connect` renders `Health Connect‑מ`. Isolating the run (`Bidi.kt`)
 * does not help, and cannot: the prefix is outside the isolate, which is the
 * whole point of one.
 *
 * The remedy is always the same shape — give the prefix a Hebrew word to attach
 * to (`נכון לשעה %1$s`) or use a space-separated preposition (`עבור %1$d`,
 * `עומד על %2$s`) — so the rule is stated as *never bond a Hebrew letter to a
 * format specifier*, which is mechanical and has no false positives in this
 * codebase.
 */
class HebrewTerminologyTest {

    private val res = listOf(File("src/main/res"), File("app/src/main/res"))
        .firstOrNull { it.isDirectory }
        ?: error("res/ not found from ${File(".").absolutePath}")

    private val hebrewFiles: List<File>
        get() = File(res, "values-iw")
            .listFiles { f -> f.isFile && f.extension == "xml" }
            .orEmpty()
            .sortedBy { it.name }

    @Test
    fun `the Goal entity is never called מטרה`() {
        val offenders = hebrewFiles.flatMap { file ->
            stripComments(file.readText()).lineSequence()
                .filter { it.contains("מטרה") || it.contains("מטרות") }
                .map { "${file.name}: ${it.trim()}" }
        }

        assertWithMessage(
            "The `Goal` entity is יעד, never מטרה (spec §5.1 / E1, ratified " +
                "2026-08-16). Note the gender changes with the noun: יעד is " +
                "masculine where מטרה is feminine, so any verb agreeing with it " +
                "moves too.",
        ).that(offenders).isEmpty()
    }

    @Test
    fun `no Hebrew prefix is bonded to a format argument`() {
        val offenders = hebrewFiles.flatMap { file ->
            stripComments(file.readText()).lineSequence()
                .filter { BONDED_PREFIX.containsMatchIn(it) }
                .map { "${file.name}: ${it.trim()}" }
        }

        assertWithMessage(
            "§4.8: a Hebrew prefix bonded to a format argument renders on the " +
                "far side of the run it prefixes, because the argument may hold " +
                "Latin text or digits. Give the prefix a Hebrew noun to attach " +
                "to (`נכון לשעה %1\$s`) or use a space-separated preposition " +
                "(`עבור %1\$d`, `עומד על %2\$s`).",
        ).that(offenders).isEmpty()
    }

    @Test
    fun `the widget pack still says יעד`() {
        // The complement of the first test: zero occurrences of מטרה is also what
        // a deleted file looks like. This is the same guard as
        // AnalyticsLiteralSweepTest's `resolves its words through resources`.
        val widget = File(res, "values-iw/widget_strings.xml")
        assertThat(stripComments(widget.readText())).contains("יעד")
    }

    private companion object {

        /**
         * A Hebrew letter — optionally followed by a maqaf `־` — immediately
         * against a format specifier, with no space between.
         *
         * The maqaf is optional because both spellings fail identically: `ב%1$s`
         * is the same defect as `ב־%1$s` with one less character. `\p{InHebrew}`
         * is avoided in favour of the explicit range so the final forms
         * (`ךםןףץ`, all inside `05D0..05EA`) are visibly included.
         */
        val BONDED_PREFIX = Regex("[א-ת]־?%\\d\\$")
    }

    /** So a header that names a forbidden word in order to forbid it is not an offender. */
    private fun stripComments(xml: String): String =
        xml.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
}
