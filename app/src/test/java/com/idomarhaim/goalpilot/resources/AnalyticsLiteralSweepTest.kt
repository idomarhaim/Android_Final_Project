package com.idomarhaim.goalpilot.resources

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Guards issue #51's literal sweep for **every package swept so far** — see
 * [SWEPT_PACKAGES].
 *
 * The name is historical: it was written for `feature/analytics/`, the first
 * package swept, and now covers `ui/components/` too. Kept rather than renamed
 * because the file is referenced by name from the changelog, the issue and
 * `AnalyticsStrings.kt`, and a rename buys a tidier name for a fistful of stale
 * pointers.
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
    fun `every swept package resolves its words through resources`() {
        // The complement of the test above: absence of literals could also mean
        // the screen renders nothing. This checks the words actually went to
        // res/ rather than being deleted.
        //
        // Loops over SWEPT_PACKAGES rather than naming one package, which it did
        // until `ui/components` was swept. That shape is worth noting: adding a
        // package to the list above extended the offender scan automatically but
        // left this half silently covering only analytics — a guard that grows on
        // one side and not the other reports green for a package it never read.
        SWEPT_PACKAGES.forEach { pkg ->
            val floor = RESOURCE_FLOOR.getValue(pkg)
            val usages = File(sourceRoot, pkg).walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .sumOf { file ->
                    Regex("""\b(stringResource|pluralStringResource)\(""")
                        .findAll(file.readText()).count()
                }
            assertWithMessage("$pkg should read its copy from res/")
                .that(usages).isAtLeast(floor)
        }
    }

    @Test
    fun `the prose rule fires on copy and stays silent on code`() {
        // The instrument, checked on the hardest inputs it exists for. Loosening
        // isProse to ignore interpolations is exactly the kind of change that
        // silently stops the guard firing, and every other input keeps saying it
        // works — so the silent half is asserted as hard as the loud half.
        val copy = listOf(
            "Goal complete",
            "Untitled goal",
            "Completed \${n} of \${m} tasks",   // words survive the strip
            "Nothing to chart yet",
            "\$count tasks remaining",
        )
        val notCopy = listOf(
            "\${goal.currentValue.trimNumber()}/\${goal.targetValue.trimNumber()}",
            "\${Math.round(it * progress)}\${item.countSuffix}",
            "\${goal.progressPercent}%",
            "\${a.veryLongIdentifierName} \${b.anotherLongOne}",
            "%1\$s %2\$s",
            ", ",
            "__unassigned__",
            "favorite",                          // one word: a key, not a sentence
        )

        assertWithMessage("these carry user-facing words and must be caught")
            .that(copy.filterNot { it.isProse() }).isEmpty()
        assertWithMessage("these are code or punctuation and must not be flagged")
            .that(notCopy.filter { it.isProse() }).isEmpty()
    }

    @Test
    fun `every swept package declares a resource floor`() {
        // Otherwise the getValue above throws a bare NoSuchElementException and
        // the next sweeper has to read this file to find out why.
        assertWithMessage("add the package to RESOURCE_FLOOR when you add it to SWEPT_PACKAGES")
            .that(RESOURCE_FLOOR.keys).containsAtLeastElementsIn(SWEPT_PACKAGES)
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

    /**
     * Two or more alphabetic words of at least two letters each, counted over
     * the literal's **copy** — its interpolations removed first.
     *
     * The contents of a `${…}` are code, not speech: `"${a.currentValue}/${b}"`
     * has no words in it, but `currentValue` and `trimNumber` are alphabetic
     * runs, so counting them made the guard report a pure-punctuation template
     * as user-facing prose. `Observed:` 2026-08-17 on two literals in
     * `ui/components` that the sweep had already fixed — a **false positive
     * that fires precisely on the remedy**, which is the shape that gets a guard
     * routed around rather than obeyed.
     *
     * It does not weaken the check. Copy *between* the interpolations survives
     * the strip, so `"Completed ${n} of ${m} tasks"` still counts three words
     * and still fails; what disappears is only the identifiers. The one thing it
     * cannot see is prose nested inside an interpolation
     * (`"${if (x) "all done" else "…"}"`), and [stringLiterals]' regex already
     * terminates at that inner quote, so it was never visible here anyway.
     */
    private fun String.isProse(): Boolean =
        Regex("""\p{L}{2,}""").findAll(withoutInterpolations()).count() >= 2

    /**
     * Drops `${…}` (brace-matched, so a lambda inside one does not end it early)
     * and bare `$identifier`.
     */
    private fun String.withoutInterpolations(): String {
        val out = StringBuilder()
        var i = 0
        while (i < length) {
            val ch = this[i]
            if (ch == '$' && i + 1 < length && this[i + 1] == '{') {
                var depth = 0
                var j = i + 1
                while (j < length) {
                    if (this[j] == '{') depth++
                    if (this[j] == '}' && --depth == 0) break
                    j++
                }
                i = if (j < length) j + 1 else length
            } else if (ch == '$' && i + 1 < length && (this[i + 1].isLetter() || this[i + 1] == '_')) {
                var j = i + 1
                while (j < length && (this[j].isLetterOrDigit() || this[j] == '_')) j++
                i = j
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    private companion object {
        /**
         * Packages whose literal sweep has landed. Absent = unswept, not exempt.
         *
         * - `feature/analytics` — 2026-08-16, session `51b-sweep-analytics`.
         * - `ui/components` — 2026-08-17, session `51e-sweep-components`.
         *
         * ### 🛑 Frozen at two of ten — **deferred by decision, not forgotten**
         *
         * Ido deferred `#51` on **2026-08-17**: all functionality must work before
         * Hebrew. The eight packages still owed — `auth`, `challenges`,
         * `dashboard`, `goals`, `health`, `lifeareas`, `profile`, `social` — are
         * listed with the order to take them in
         * `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md`, and `#51` stays
         * **OPEN**.
         *
         * **This freeze is documentation, not a behaviour change, and the reason
         * is that this guard is opt-in.** It reads only what is in this list, so
         * an absent package is *unswept*, not failing: nothing here had to be
         * relaxed, disabled or `@Ignore`d to park the sweep, and the two packages
         * that are listed stay guarded exactly as strictly as before.
         *
         * So **do not add your package here as a favour** while writing a feature.
         * Adding a name opts that package into work that is deliberately parked,
         * and the build will then fail on plain English literals AGENTS.md
         * currently tells you to write. A name goes in when its sweep lands —
         * which is what resuming `#51` means.
         */
        val SWEPT_PACKAGES = listOf("feature/analytics", "ui/components")

        /**
         * Fewest `stringResource` call sites a swept package must still have.
         *
         * A floor rather than a count: it exists to catch copy being *deleted*
         * instead of moved, and an exact number would fail on every ordinary
         * edit afterwards. `ui/components`'s is low because most of this package
         * takes its words as **parameters** from the screen rendering it — that
         * is the honest number, not a weaker standard.
         */
        val RESOURCE_FLOOR = mapOf(
            "feature/analytics" to 30,
            "ui/components" to 8,
        )
    }
}
