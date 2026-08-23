package com.idomarhaim.goalpilot.docs

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * `docs/` makes assertions about this code, and until now nothing re-ran them.
 *
 * ## The drift this exists for
 *
 * `Observed:` 2026-08-24, auditing all six files under `docs/` against `HEAD`.
 * `docs/ARCHITECTURE.md` had not been touched since 2026-08-04 and carried roughly fifteen
 * false claims — among them *"completing a task is a Firestore transaction"* (deleted by `#55`),
 * a `publicProfiles` schema still listing `level` (deleted by §5.2), *"three callables"* against
 * four, a bottom bar naming a tab that no longer exists and missing the one that does, and a
 * data-model block with four collections absent. `docs/OPERATIONS.md` stated **92 JVM + 12
 * instrumented** tests against a tree holding **1084 + 319**, and contradicted itself on Health
 * Connect in two adjacent sections. Nothing in the repo could go red for any of it.
 *
 * That is the same mechanism `kb/dev/product-copy-describes-code.md` records one layer over —
 * *product copy is an assertion about other code, and nothing re-runs it* — arriving in the
 * documentation instead of on a screen. The remedy is the same one: **recompute the claim and
 * diff it**, rather than asking a human to re-read prose they have read before.
 *
 * ## ⚠️ WHAT THIS GUARD DOES NOT COVER — read this before trusting a green run
 *
 * Every assertion here is a **presence check over an enumeration**: the code contains a thing,
 * so the doc must name it. That catches the *omission* half of the drift and **none** of the
 * *false-assertion* half. Of the fifteen findings above, this class would have caught eight; the
 * ones that actually mislead a reader — a transaction that no longer exists, points described as
 * client-side when a trigger writes them, life areas called un-reorderable two hundred lines
 * below the use case that reorders them — are sentences that are simply **wrong**, and no
 * presence check reaches a wrong sentence.
 *
 * So **a green run here does not mean the docs are current.** It means nothing has been *added*
 * to the code that the docs never heard of. Naming that limit in the class that could otherwise
 * be read as a currency guarantee is deliberate: the false reassurance is a real cost, and it was
 * the strongest argument raised against building this at all.
 *
 * ## Why these four and not the six that were drafted
 *
 * Two drafted assertions were **dropped after being run**, which is the only reason the drop is
 * trustworthy:
 *
 * - *every package under `feature/` is named in ARCHITECTURE.md* — the oracle is "does this word occur
 *   in a 245-line file", and it is weak in **both** directions. Case-sensitive it fired on
 *   `social` (the doc says *Social*); case-insensitive it went silent on `health` (the doc says
 *   *Health/Tasks stubs*, describing a data-layer stub, not the feature package). An enumeration
 *   the document never promised to make cannot be asserted against.
 * - *the test counts in OPERATIONS.md match the tree* — this one is worse than useless. The count
 *   changes on **every commit that adds a test**, so guarding it taxes the commonest action in the
 *   repo to protect a number nobody makes a decision from. The right fix is to delete the number
 *   from the document, not to guard it. Same verdict for CLOUD-DEVICE.md's *"the 15 instrumented
 *   tests"*, which is stale by a factor of twenty.
 *
 * The four that survive all assert against enumerations the documents **explicitly make** — a
 * fenced collection tree, a named list of callables, a named bottom bar, a quoted shell command.
 *
 * ## ⚠️ This guard reads files Gradle does not associate with a test
 *
 * `functions/src/index.ts`, `gradle.properties`, `README.md` and `docs/` are outside everything
 * `testDebugUnitTest` tracks, so without explicit `inputs` declarations the task answers
 * **UP-TO-DATE** and this class reports green having never executed — the failure
 * [ReleaseNotesGuardTest] records from 2026-08-22, where a mutation check "passed" in 2 s having
 * run nothing. They are declared in `app/build.gradle.kts` beside that test's inputs. The two
 * Kotlin sources parsed below need no declaration: `inputs.dir("src/main/java")` already covers
 * them as text. **If this class grows a fifth input, declare it.**
 */
class DocsCurrencyTest {

    /** Anchored on `settings.gradle.kts`, which is at the root and nowhere else. */
    private val repoRoot: File = listOf(File("."), File(".."))
        .map { it.canonicalFile }
        .firstOrNull { File(it, "settings.gradle.kts").isFile }
        ?: error("repo root not found from ${File(".").canonicalPath}")

    private val architecture = File(repoRoot, "docs/ARCHITECTURE.md")
    private val functionsIndex = File(repoRoot, "functions/src/index.ts")
    private val constants = File(
        repoRoot,
        "app/src/main/java/com/idomarhaim/goalpilot/core/util/Constants.kt",
    )
    private val destinations = File(
        repoRoot,
        "app/src/main/java/com/idomarhaim/goalpilot/ui/navigation/Destinations.kt",
    )

    @Test
    fun `every callable the backend exports is named in ARCHITECTURE`() {
        val exported = Regex("""export const (\w+) = onCall""")
            .findAll(functionsIndex.readText())
            .map { it.groupValues[1] }
            .toList()
        check(exported.isNotEmpty()) { "no onCall exports found; this guard is stale" }

        val doc = architecture.readText()
        val missing = exported.filterNot { doc.contains(it) }

        assertWithMessage(
            "functions/src/index.ts exports ${exported.size} callables and docs/ARCHITECTURE.md " +
                "never names $missing. The LLM-flow section enumerates the callables and states a " +
                "count in prose, so a callable added without a doc edit leaves that section " +
                "quietly wrong -- which is how it came to say \"three\" against four.",
        ).that(missing).isEmpty()
    }

    @Test
    fun `every Firestore collection the client writes is named in ARCHITECTURE`() {
        // Scoped to the FirestorePaths object: StoragePaths and CloudFunctions live in the same
        // file and are not collections. Reading the whole file fired on `progress_images` and a
        // callable name, which is a false positive that trains people to ignore the guard.
        val body = constants.readText()
            .substringAfter("object FirestorePaths {")
            .substringBefore("\nobject ")
        val collections = Regex("""const val \w+ = "(\w+)"""")
            .findAll(body)
            .map { it.groupValues[1] }
            .toList()
        check(collections.isNotEmpty()) { "FirestorePaths parsed empty; this guard is stale" }

        val doc = architecture.readText()
        val missing = collections.filterNot { doc.contains(it) }

        assertWithMessage(
            "FirestorePaths declares ${collections.size} collections and the data-model block in " +
                "docs/ARCHITECTURE.md never names $missing. That block is a fenced tree of the " +
                "whole model -- a reader takes its absence as \"this collection does not exist\", " +
                "which is exactly what happened to completionFacts and occurrences.",
        ).that(missing).isEmpty()
    }

    @Test
    fun `every bottom-bar tab is named in ARCHITECTURE`() {
        val tabs = Regex("""\w+\(Routes\.\w+, "([^"]+)"""")
            .findAll(destinations.readText())
            .map { it.groupValues[1] }
            .toList()
        check(tabs.isNotEmpty()) { "TopLevelTab parsed empty; this guard is stale" }

        val doc = architecture.readText()
        val missing = tabs.filterNot { doc.contains(it) }

        assertWithMessage(
            "TopLevelTab declares the bar as $tabs and docs/ARCHITECTURE.md never names " +
                "$missing. The navigation section writes the bar out tab by tab, so a swapped tab " +
                "leaves a sentence that is confidently and specifically wrong.",
        ).that(missing).isEmpty()
    }

    @Test
    fun `every JDK path quoted in the docs is the one gradle properties pins`() {
        // Deliberately NOT "does this directory exist". That passes on Ido's machine and fails on
        // every CI runner, so it would be either skipped (not a gate) or deleted within a week.
        // Agreement with the pin is machine-independent and catches the same defect.
        val pinned = Regex("""org\.gradle\.java\.home=.*[/\\]([^/\\\r\n]+)""")
            .find(File(repoRoot, "gradle.properties").readText())
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?: error("gradle.properties no longer pins org.gradle.java.home; this guard is stale")

        // The separator class must accept a real backslash. Written '[\\/]' in Kotlin source so the
        // regex sees [\\/]; the naive '[\/]' collapses to '[/]' and silently matches only the
        // forward-slash spellings -- which is how the first draft of this check reported SILENT on
        // three documents that were telling people to export a JAVA_HOME that does not exist.
        val quoted = Regex("""Eclipse Adoptium[\\/]([\w.\-]+)""")

        val offenders = docFiles()
            .flatMap { file ->
                quoted.findAll(file.readText())
                    .map { it.groupValues[1] }
                    .filter { it != pinned }
                    .map { "${file.relativeTo(repoRoot).path.replace('\\', '/')} -> $it" }
            }
            .distinct()
            .sorted()

        assertWithMessage(
            "gradle.properties pins $pinned, and these documents tell a reader to export a " +
                "different JDK: $offenders. jdk-21.0.11.10-hotspot is not merely out of date -- it " +
                "is a known wreck with an orphaned lib/ and no bin/java.exe, so following those " +
                "instructions produces \"JAVA_HOME is set to an invalid directory\" and reads as a " +
                "broken machine rather than a stale document.",
        ).that(offenders).isEmpty()
    }

    @Test
    fun `the guard is not vacuous`() {
        // Every case above passes against a missing file, an empty doc, or a renamed property.
        // This is the complement: the inputs are all really there and really have content.
        for (f in listOf(architecture, functionsIndex, constants, destinations)) {
            assertWithMessage("${f.relativeTo(repoRoot).path} is missing; this guard is stale")
                .that(f.isFile)
                .isTrue()
        }
        assertWithMessage("docs/ has no markdown for the JDK check to read")
            .that(docFiles())
            .isNotEmpty()
        assertWithMessage("ARCHITECTURE.md no longer contains its data-model block")
            .that(architecture.readText())
            .contains("publicProfiles/{uid}")
    }

    /** Every document a person follows instructions from: the markdown under `docs/` plus the root README. */
    private fun docFiles(): List<File> =
        (File(repoRoot, "docs").listFiles().orEmpty().filter { it.extension == "md" } +
            File(repoRoot, "README.md"))
            .filter { it.isFile }
            .sortedBy { it.path }
}
