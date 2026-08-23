package com.idomarhaim.goalpilot.resources

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * There is **one** release-notes file, and both release routes name it.
 *
 * ## The defect this exists for, which shipped twice before anyone noticed
 *
 * ⚠️ **THIS CLASS ASSERTED THE RESOLUTION RULE BACKWARDS UNTIL 2026-08-24, AND SO DID THE FIX
 * IT SHIPPED WITH.** What stood here — *"that path is resolved relative to the `app` module"* — is
 * **false**, and it was never run against a real upload. `Observed:` 2026-08-24, the first
 * `./gradlew :app:appDistributionUploadRelease` since:
 *
 * ```
 * Failed to read file "C:/Dev/Android_Final_Project/release-notes.txt"
 * ```
 *
 * naming the **repo root**, with `app/release-notes.txt` sitting there the whole time. The plugin
 * resolves the property against the **root project**, not the module.
 *
 * Three things follow, and the middle one is the expensive one:
 *
 * 1. The `Inferred:` claim that stood here — that `20f3b7e` and `67c21e5` shipped testers the
 *    *module* file's placeholder — is **refuted**. The plugin was reading the root file, which is
 *    the one people were editing, so those releases shipped the notes intended for them.
 * 2. **Deleting the stray root file on 2026-08-22 killed the local upload route**, and nothing went
 *    red for two days because nobody ran it. The deletion was reasoned from this KDoc; the KDoc was
 *    wrong; the guard written to stop the next person making a bad reading encoded the bad reading.
 * 3. Assertion three — *the two routes name the same file* — **passed while the two routes named
 *    different files**, because it applied this same wrong rule to the Gradle side. Two errors
 *    cancelling inside a test is worse than no test, and it is why this class now resolves the
 *    declared path exactly the way the plugin does.
 *
 * The property now reads `"app/release-notes.txt"`, which names the same file under **either**
 * reading of the rule. That is the form that cannot rot back — not a comment saying which reading
 * is right.
 *
 * ### The original defect, which is still real
 *
 * `CHANGELOG/2026-08-20/c13-key-store.md` records a session creating a second notes file at the
 * root from reading the bare property as repo-relative. It **was** repo-relative, so that reading
 * was correct and the file it made was the live one; what was wrong was having two. One file, named
 * unambiguously, is the fix to both.
 *
 * ## Why a test rather than deleting the stray file and moving on
 *
 * The deletion fixes today. It does not touch what caused it, which is that the property **reads
 * like a repo-root path** — so the next person to add or move a notes file makes the same reading,
 * and the failure is silent in the one direction nobody checks: the build succeeds, the upload
 * succeeds, and the wrong words arrive on somebody else's phone. Nothing in this repo could have
 * gone red. Now something can.
 *
 * ## ⚠️ This guard reads files Gradle does not associate with a test
 *
 * Both of its inputs — the notes file and `.github/workflows/release.yml` — are outside anything
 * `testDebugUnitTest` tracks, so without an explicit `inputs.file` declaration the task answers
 * **UP-TO-DATE** and the guard reports green on the previous run. `Observed:` 2026-08-22, in the
 * mutation check written to prove this class is not vacuous: the workflow was edited to name a
 * different file and the task finished in 2 s having executed nothing. The mutation would have been
 * written up as *"the guard did not catch it"*, when the guard had never run. Both inputs are
 * declared in `app/build.gradle.kts`; if this class grows a third file, declare that one too.
 *
 * ## The third assertion is the one worth having
 *
 * The first two are hygiene. The third is that **the two release routes name the same file** —
 * `.github/workflows/release.yml` (tag → CI) writes and passes one path, `app/build.gradle.kts`
 * (local `appDistributionUploadRelease`) names another. Nothing makes them agree, they are edited by
 * different people for different reasons, and if they ever diverge the two routes ship *different
 * release notes for the same build* with no error anywhere.
 */
class ReleaseNotesGuardTest {

    /**
     * The repo root, from wherever Gradle put this test's working directory.
     *
     * Two candidates for the same reason `DialogLocaleGuardTest` has two: the module directory when
     * run from the `app` project, the repo root when run from elsewhere. Anchored on
     * `settings.gradle.kts`, which exists at the root and nowhere else.
     */
    private val repoRoot: File = listOf(File("."), File(".."))
        .map { it.canonicalFile }
        .firstOrNull { File(it, "settings.gradle.kts").isFile }
        ?: error("repo root not found from ${File(".").canonicalPath}")

    private val moduleBuildFile = File(repoRoot, "app/build.gradle.kts")
    private val workflow = File(repoRoot, ".github/workflows/release.yml")

    @Test
    fun `the release notes file the plugin names actually exists, and says something`() {
        val declared = declaredReleaseNotesPath()
        // Resolved against the REPO ROOT, because that is what the plugin does -- measured under a
        // real upload on 2026-08-24, not read off the property. See this class's KDoc.
        val resolved = File(repoRoot, declared)

        assertWithMessage(
            "app/build.gradle.kts declares releaseNotesFile = \"$declared\", which resolves to " +
                "${resolved.path} and is not there. The plugin resolves it relative to the REPO " +
                "ROOT, not the app module -- so the value needs its \"app/\" prefix.",
        ).that(resolved.isFile).isTrue()

        // An empty notes file is not a failure the plugin reports — testers just get nothing.
        assertWithMessage("The release notes are empty; testers would see a blank release.")
            .that(resolved.readText().trim())
            .isNotEmpty()
    }

    @Test
    fun `no second release-notes file exists anywhere else in the repo`() {
        val declared = declaredReleaseNotesPath()
        val canonical = File(repoRoot, declared).canonicalFile

        val strays = repoRoot.walkTopDown()
            .onEnter { it.name !in IGNORED_DIRS }
            .filter { it.isFile && it.name == canonical.name }
            .filter { it.canonicalFile != canonical }
            .map { it.relativeTo(repoRoot).path }
            .toList()

        assertWithMessage(
            "A second file with this name exists, and it is the one somebody will edit — it is " +
                "what `ls` shows at the repo root, while the file the release actually reads is " +
                "one directory down. That is how two releases shipped placeholder notes; see this " +
                "test's KDoc. Delete the stray, or point releaseNotesFile at it.",
        ).that(strays).isEmpty()
    }

    @Test
    fun `the tag route and the local route name the same file`() {
        // The assertion that is not hygiene. These two paths are written by different people at
        // different times and nothing makes them agree; diverged, a tag release and a local release
        // ship different notes for the same build, silently.
        val fromGradle = File(repoRoot, declaredReleaseNotesPath())
            .canonicalFile

        val fromWorkflow = Regex("""--release-notes-file\s+(\S+)""")
            .find(workflow.readText())
            ?.groupValues
            ?.get(1)
            ?: error("release.yml no longer passes --release-notes-file; this guard is stale")

        assertThat(File(repoRoot, fromWorkflow).canonicalFile).isEqualTo(fromGradle)
    }

    @Test
    fun `the guard is not vacuous`() {
        // Every case above passes in a repo that declares no release notes at all, or whose
        // workflow no longer uploads anything. This is the complement: both routes still exist.
        assertThat(moduleBuildFile.isFile).isTrue()
        assertThat(workflow.isFile).isTrue()
        assertThat(workflow.readText()).contains("appdistribution:distribute")
    }

    /** The literal value of `releaseNotesFile` in the module's build file. */
    private fun declaredReleaseNotesPath(): String =
        Regex("""releaseNotesFile\s*=\s*"([^"]+)"""")
            .find(moduleBuildFile.readText())
            ?.groupValues
            ?.get(1)
            ?: error("app/build.gradle.kts no longer declares releaseNotesFile; this guard is stale")

    private companion object {
        /** Build output and dependency trees hold thousands of files and none of them is ours. */
        val IGNORED_DIRS = setOf(".git", "build", "node_modules", ".gradle", ".idea")
    }
}
