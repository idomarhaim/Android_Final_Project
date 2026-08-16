package com.idomarhaim.goalpilot.resources

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Guards issue #51's literal sweep for `feature/analytics/` — the first package
 * swept, and the template for the rest.
 *
 * ### Why a test and not a review note
 *
 * A sweep is not a state, it is an event: the package is clean the day it is
 * done and drifts the first time somebody adds a `Text("Try again")` in a hurry.
 * Nothing else notices — the app compiles, the English render is perfect, and
 * the defect is visible only to a Hebrew reader, who is the one person not
 * reviewing the diff. `HebrewLocaleResourceTest` cannot see it either: a literal
 * that never reached `res/` has no key to be missing a translation for.
 *
 * ### What counts as prose
 *
 * A literal with **two or more alphabetic words**. That deliberately ignores
 * format patterns (`%1$s %2$s`), separators (`", "`), keys
 * (`"__unassigned__"`) and single technical tokens, which are not speech and
 * have no business in `res/`. The rule is crude on purpose: a precise one would
 * need to know which argument of which composable reaches a screen, and a guard
 * nobody can predict is a guard people route around.
 *
 * ### Extending it
 *
 * Add a package to [SWEPT_PACKAGES] the moment its sweep lands. A package absent
 * from that list is *unswept*, not exempt — the list is a record of progress,
 * and the changelog says which packages are still owed.
 */
class AnalyticsLiteralSweepTest {

    private val sourceRoot = listOf(
        File("src/main/java/com/idomarhaim/goalpilot"),
        File("app/src/main/java/com/idomarhaim/goalpilot"),
    ).firstOrNull { it.isDirectory }
        ?: error("source root not found from ${File(".").absolutePath}")

    @Test
    fun `no user-facing prose literal survives in a swept package`() {
        val offenders = mutableListOf<String>()

        SWEPT_PACKAGES.forEach { pkg ->
            val dir = File(sourceRoot, pkg)
            assertWithMessage("swept package $pkg does not exist").that(dir.isDirectory).isTrue()

            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                stringLiterals(stripComments(file.readText()))
                    .filter { it.isProse() }
                    .forEach { offenders += "${file.name}: \"$it\"" }
            }
        }

        assertWithMessage(
            "These are user-facing English literals in a package that has been swept. " +
                "Move each to res/values/<package>_strings.xml with its Hebrew in " +
                "res/values-iw/ — see issue #51 and AnalyticsStrings.kt.",
        ).that(offenders.sorted()).isEmpty()
    }

    @Test
    fun `the swept package resolves its words through resources`() {
        // The complement of the test above: absence of literals could also mean
        // the screen renders nothing. This checks the words actually went to
        // res/ rather than being deleted.
        val dir = File(sourceRoot, "feature/analytics")
        val usages = dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sumOf { file ->
                Regex("""\b(stringResource|pluralStringResource)\(""")
                    .findAll(file.readText()).count()
            }
        assertWithMessage("feature/analytics should read its copy from res/")
            .that(usages).isAtLeast(30)
    }

    // ------------------------------------------------------------------ helpers

    /** Block and line comments, so a KDoc sentence is never mistaken for copy. */
    private fun stripComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")

    /**
     * String literals, triple-quoted ones excluded.
     *
     * Raw strings in this codebase are regexes, and a regex is not speech.
     */
    private fun stringLiterals(source: String): List<String> {
        val withoutRaw = source.replace(Regex("\"\"\".*?\"\"\"", RegexOption.DOT_MATCHES_ALL), "")
        return Regex("""(?<!\\)"((?:[^"\\\n]|\\.)*)"""").findAll(withoutRaw)
            .map { it.groupValues[1] }
            .toList()
    }

    /** Two or more alphabetic words of at least two letters each. */
    private fun String.isProse(): Boolean =
        Regex("""\p{L}{2,}""").findAll(this).count() >= 2

    private companion object {
        /**
         * Packages whose literal sweep has landed. Absent = unswept, not exempt.
         *
         * - `feature/analytics` — 2026-08-16, session `51b-sweep-analytics`.
         */
        val SWEPT_PACKAGES = listOf("feature/analytics")
    }
}
