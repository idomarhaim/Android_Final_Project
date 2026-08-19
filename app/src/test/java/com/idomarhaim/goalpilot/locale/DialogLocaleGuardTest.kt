package com.idomarhaim.goalpilot.locale

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Every window this app opens must go through `ui/locale/LocaleAwareWindows.kt`.
 *
 * ### Why this is a test and not a convention
 *
 * A `Dialog`, `ModalBottomSheet` or `DropdownMenu` composes into its own
 * `AbstractComposeView`, which re-provides `LocalContext` from **its** window —
 * so `AppLocale`'s language override stops at the boundary and everything
 * inside renders in the *device* language.
 *
 * **And the tell is a lie.** `LocalLayoutDirection` *does* cross that boundary
 * (Compose copies it onto the new window's `View`), so a broken dialog mirrors
 * right-to-left perfectly while speaking English. It looks *more* finished than
 * a half-done job. `Observed:` 2026-08-16, session `51c-analytics-render` — and
 * the identical inference had already failed one layer down, when the widget's
 * `values-he` bucket mirrored beautifully and resolved nothing.
 *
 * So: **correct RTL mirroring is not evidence that the strings are localized.**
 * Nobody catches this by looking, which is why it is guarded here instead.
 *
 * ### What the guard actually buys
 *
 * Not the fix — the fix is three lines. It buys the *fifty-odd* places the fix
 * has to be applied and can be silently forgotten: an `AlertDialog` has four
 * content slots and this app has fourteen of them, and the eight feature
 * packages still owed a literal sweep under #51 will each be editing exactly
 * those lambdas. A wrapper missed on one slot fails silently, in Hebrew only.
 *
 * ### Honest limit
 *
 * This reads source text, so it catches the shapes people actually write —
 * including a fully-qualified `androidx.compose.material3.AlertDialog(`, which
 * `DashboardScreen` really did contain. It would **not** catch an import alias
 * (`import …AlertDialog as Foo`). Nothing here pretends otherwise; the test
 * closes the accident, not the determined workaround.
 */
class DialogLocaleGuardTest {

    private val sourceRoot = listOf(
        File("src/main/java/com/idomarhaim/goalpilot"),
        File("app/src/main/java/com/idomarhaim/goalpilot"),
    ).firstOrNull { it.isDirectory }
        ?: error("source root not found from ${File(".").absolutePath}")

    @Test
    fun `no window is opened outside ui-locale`() {
        val offenders = mutableListOf<String>()

        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.isTheWrapperPackage() }
            .forEach { file ->
                val source = stripComments(file.readText())
                source.lineSequence().forEachIndexed { index, line ->
                    WINDOW_CONSTRUCTORS.forEach { name ->
                        if (rawCallOf(name).containsMatchIn(line)) {
                            offenders += "${file.name}:${index + 1}  $name("
                        }
                    }
                }
            }

        assertWithMessage(
            "These open a window that will NOT inherit AppLocale's language — it will " +
                "render in the device language while mirroring RTL correctly, which is " +
                "why looking at it does not catch it. Use the App* wrapper from " +
                "ui/locale/LocaleAwareWindows.kt instead (AppAlertDialog, AppDialog, " +
                "AppDropdownMenu, AppModalBottomSheet, AppDatePickerDialog), or wrap the " +
                "content in InheritAppLocale { } and add the new window type there.",
        ).that(offenders.sorted()).isEmpty()
    }

    @Test
    fun `the guard is not vacuous`() {
        // The test above also passes in an app that opens no windows at all, and
        // would keep passing if the wrappers were deleted along with their call
        // sites. This is the complement: the app really does open windows, and
        // they really do go through ui/locale/.
        val wrapperUses = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.isTheWrapperPackage() }
            .sumOf { file ->
                Regex("""\bApp(AlertDialog|Dialog|DropdownMenu|ModalBottomSheet|DatePickerDialog)\(""")
                    .findAll(stripComments(file.readText())).count()
            }

        assertWithMessage(
            "The app should still be opening its windows through ui/locale/. If this " +
                "dropped, either the wrappers were bypassed or the guard above is now " +
                "guarding nothing.",
        ).that(wrapperUses).isAtLeast(20)
    }

    // ------------------------------------------------------------------ helpers

    /**
     * A call to [name] that is **not** one of our wrappers.
     *
     * The lookbehind does the work: `AppAlertDialog(` is preceded by `p`, so it
     * cannot match, while `androidx.compose.material3.AlertDialog(` is preceded
     * by `.` and does. It is also what keeps `AlertDialog(` from registering as
     * a bare `Dialog(`, and `DropdownMenuItem(` from registering as a
     * `DropdownMenu(` — the `(` must follow the name immediately.
     */
    private fun rawCallOf(name: String) = Regex("""(?<!\w)$name\s*\(""")

    /**
     * `ui/locale/` — the one package allowed to touch a raw constructor, because
     * it is the wrapper.
     *
     * Matched on **both** path segments, not just the leaf: a future
     * `data/locale/` or `core/locale/` would otherwise exempt itself from this
     * guard by its name alone, which is exactly the kind of accident the guard
     * exists to catch.
     */
    private fun File.isTheWrapperPackage(): Boolean =
        parentFile?.name == "locale" && parentFile?.parentFile?.name == "ui"

    /** So a KDoc sentence naming `AlertDialog(` is never read as a call. */
    private fun stripComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")

    private companion object {
        /**
         * Composables that host their content in a **new window**, and therefore
         * lose `LocalContext`. Add to this list, and to `LocaleAwareWindows.kt`,
         * whenever a new one is adopted — `TooltipBox` and `ExposedDropdownMenu`
         * are the two most likely next.
         */
        val WINDOW_CONSTRUCTORS = listOf(
            "AlertDialog",
            "BasicAlertDialog",
            "DatePickerDialog",
            "TimePickerDialog",
            "Dialog",
            "DropdownMenu",
            "ModalBottomSheet",
            "Popup",
        )
    }
}
