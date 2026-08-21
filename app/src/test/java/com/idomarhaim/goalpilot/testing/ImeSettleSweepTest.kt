package com.idomarhaim.goalpilot.testing

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Guards issue #58: **no instrumented test may call `performTextInput`,
 * `performTextReplacement` or `performTextClearance` directly.** They call the
 * `…AndSettle` wrapper, which waits for the soft keyboard to finish moving the
 * layout first — see `androidTest/.../ui/ImeSettling.kt` for the mechanism and
 * the measurements.
 *
 * ### Why a test and not a review note
 *
 * The same reason [com.idomarhaim.goalpilot.resources.AnalyticsLiteralSweepTest]
 * gives, and more sharply. A raw `performTextInput` followed by a click is
 * **invisible at the call site**: it is the obvious spelling, it is what every
 * Compose tutorial shows, it passes when the class is run alone, and it fails
 * intermittently in the full suite — `Observed:` 2026-08-21, **1 run in 4** — in a
 * *different* test each time, because which one loses the race depends on where
 * the keyboard had got to. `#58` was opened from two *runs*, and reading the runs
 * could never have found it; the answer was in the geometry of a frame.
 *
 * So the guard has to fire at the moment the line is *written*, and the JVM layer
 * is where it is free: this test needs no device, no emulator and no minutes.
 *
 * ### Why the check is textual
 *
 * Nothing else can see it. A lint rule would be the tidier instrument and this
 * project has no custom-lint module; the compiler cannot help, because both
 * spellings type-check identically. The cost of the crude form is that a comment
 * mentioning the name would trip it, which is why comments are stripped first.
 *
 * ### The exemption
 *
 * [HELPER_FILE] is the one file that must call the real thing — it *is* the
 * wrapper. Exempting it by name rather than by a suppression comment keeps the
 * escape hatch to exactly one place that a reader can go and look at.
 */
class ImeSettleSweepTest {

    private val androidTestRoot = listOf(
        File("src/androidTest/java/com/idomarhaim/goalpilot"),
        File("app/src/androidTest/java/com/idomarhaim/goalpilot"),
    ).firstOrNull { it.isDirectory }
        ?: error("androidTest root not found from ${File(".").absolutePath}")

    @Test
    fun `no instrumented test touches a text field without waiting for the keyboard`() {
        val offenders = mutableListOf<String>()

        androidTestRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != HELPER_FILE }
            .forEach { file ->
                stripComments(file.readText())
                    .lineSequence()
                    .forEachIndexed { index, line ->
                        if (RAW_CALL.containsMatchIn(line)) {
                            offenders += "${file.name}:${index + 1}"
                        }
                    }
            }

        assertWithMessage(
            "These touch a text field without waiting for the keyboard. Focusing a " +
                "field raises the soft keyboard, and the window-inset animation that " +
                "follows moves the layout while Compose reports itself idle -- so the " +
                "NEXT click is aimed at where the target used to be, and is silently " +
                "lost. Call the ...AndSettle wrapper instead " +
                "(androidTest/.../ui/ImeSettling.kt); see issue #58.",
        ).that(offenders.sorted()).isEmpty()
    }

    /**
     * The sweep is worth nothing if it is looking at an empty directory or a
     * source root that moved, and both fail *silently* as a pass. So assert the
     * corpus is non-trivial and that the guarded call actually appears in it.
     */
    @Test
    fun `the sweep is actually reading the instrumented sources`() {
        val kotlinFiles = androidTestRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        assertWithMessage("androidTest source root looks empty -- the sweep would pass on nothing")
            .that(kotlinFiles.size).isAtLeast(20)

        val settled = kotlinFiles.count { SETTLED_CALL.containsMatchIn(stripComments(it.readText())) }
        assertWithMessage(
            "No file calls a ...AndSettle wrapper. Either every text-input test was " +
                "deleted, or the helpers were renamed and this guard is now watching " +
                "names nothing uses -- which reads as green forever.",
        ).that(settled).isAtLeast(1)
    }

    /**
     * Strips block and line comments so that *documenting* the banned call — as
     * `ImeSettling.kt` and this file's own KDoc both do — is not an offence.
     * String literals are not stripped: a test that builds the call name as a
     * string is doing something else entirely.
     */
    private fun stripComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)) { match ->
                // Keep the line count, so the reported line numbers stay usable.
                "\n".repeat(match.value.count { it == '\n' })
            }
            .replace(Regex("""//.*"""), "")

    private companion object {
        const val HELPER_FILE = "ImeSettling.kt"

        /**
         * The whole focus-raising family: `performTextInput`,
         * `performTextReplacement`, `performTextClearance`. All three call
         * `getNodeAndFocus` first, and focusing the field is what raises the
         * keyboard — clearance included, which is exactly why it is listed: it
         * is the one that does not look like typing.
         *
         * The `…AndSettle` wrappers do not match, and it is worth seeing why
         * rather than trusting it: the pattern requires optional whitespace and
         * then `(` immediately after the name, and what follows there is `A`.
         * `the sweep is actually reading the instrumented sources` is the test
         * that would notice if that ever stopped being true.
         */
        val RAW_CALL = Regex("""\bperformText(Input|Replacement|Clearance)\s*\(""")
        val SETTLED_CALL = Regex("""\bperformText(Input|Replacement|Clearance)AndSettle\s*\(""")
    }
}
