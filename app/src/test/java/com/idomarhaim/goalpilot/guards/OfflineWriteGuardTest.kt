package com.idomarhaim.goalpilot.guards

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * **`setDone` is a server-only transaction, so the offline pre-check may not be deleted yet.**
 *
 * #50 §5 authorises deleting `core/net/ConnectivityMonitor.kt` and
 * `GoalDetailViewModel`'s `connectivity.isOnline()` guard, on the stated premise that *"`C20`
 * removes the transaction"*. At the time #50 was executed that premise was **false**, and
 * acting on it would have re-opened closed `#3`: offline, `runTransaction` cannot touch the
 * cache, so the optimistic tick stands for a **measured 7.9 s** before being taken back.
 *
 * The premise lives in three committed documents and in the ticket itself. Prose does not stop
 * this — a ticket that grants permission to delete is exactly where the human always-ask gate
 * has already been spent. This test is the half that cannot be skimmed.
 *
 * ### It expires on its own, and says so
 *
 * When `C20`'s build half ships and `setDone` stops being a transaction, the assumption below
 * stops holding and this test reports **skipped**, not passed — a skip is visible in the report
 * where a vacuous pass is not. That skip is the signal that **#50 item 5 is now unblocked**:
 * delete `ConnectivityMonitor`, its `GoalDetailViewModel` pre-check, `OFFLINE_MESSAGE`, and
 * then delete this file.
 *
 * ### Why every check reads comment-stripped source
 *
 * The first draft compared the raw file text and **passed with the pre-check fully commented
 * out** — verified 2026-08-20, negative control run before this was fixed. Commenting a check
 * out is the commonest way one is disabled in practice, so that was the single input this guard
 * most exists for, and it degraded silently on it. [stripComments] is the same crude regex
 * `AnalyticsLiteralSweepTest` already uses here: it also strips a `//` inside a string literal,
 * which can only ever make this test fire when it should not — loud, and the safe direction.
 *
 * What it still cannot see: a call left in place but made unreachable, or moved out of
 * `toggleTask`. Deliberately not chased — a guard that tries to know which branch runs is a
 * guard nobody can predict, which is the reason `AnalyticsLiteralSweepTest` stays crude too.
 * `GoalDetailViewModelTest` covers the behaviour; this file covers the *deletion*.
 *
 * ### Why that is not the same test twice
 *
 * `GoalDetailViewModelTest.an offline tap is refused outright and never fakes a tick` does go
 * red if the pre-check is removed — but #50 §5 authorises removing the pre-check, so whoever
 * executes it deletes that case in the same breath and is right to. Only this file ties the
 * permission to a **checkable condition** rather than to a sentence in a ticket, and it is the
 * one test whose own deletion has a stated precondition: the skip below.
 */
class OfflineWriteGuardTest {

    private val sourceRoot = listOf(
        File("src/main/java/com/idomarhaim/goalpilot"),
        File("app/src/main/java/com/idomarhaim/goalpilot"),
    ).firstOrNull { it.isDirectory }
        ?: error("source root not found from ${File(".").absolutePath}")

    private fun source(path: String): String {
        val file = File(sourceRoot, path)
        assertWithMessage("$path should exist").that(file.isFile).isTrue()
        return file.readText()
    }

    /** [source] with comments removed, so a commented-out guard reads as absent. */
    private fun code(path: String): String = stripComments(source(path))

    private fun stripComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")

    @Test
    fun `while setDone is a transaction, the offline pre-check must survive`() {
        val transactional = "runTransaction" in code("data/firestore/TaskRepositoryImpl.kt")

        // Not an early return: a skip is visible in the test report, a vacuous
        // pass is not, and this test's whole job is to be noticed when it stops
        // applying. See the KDoc — the skip is what unblocks #50 item 5.
        assumeTrue(
            "setDone is no longer a transaction: #50 item 5 is unblocked. Delete " +
                "ConnectivityMonitor, GoalDetailViewModel's pre-check, OFFLINE_MESSAGE, " +
                "and this test.",
            transactional,
        )

        val why = "TaskRepositoryImpl.setDone is still firestore.runTransaction, which cannot " +
            "be served from the Firestore cache. Offline it fails after a measured 7.9 s, so " +
            "the optimistic tick in GoalDetailViewModel.toggleTask is a lie for eight seconds " +
            "unless the pre-check refuses the tap up front. Deleting this re-opens closed #3. " +
            "#50 §5 authorises the deletion on the premise that C20 removed the transaction — " +
            "C20 (#42) is a DECISION issue whose build half has never shipped. Build that " +
            "first; see docs/PRODUCT_v0.3.md §5.3 §5, annotated 2026-08-20."

        assertWithMessage(why)
            .that(File(sourceRoot, "core/net/ConnectivityMonitor.kt").isFile).isTrue()
        // `.that(text).contains(...)` would dump the whole 240-line file into the
        // failure; the `why` above is the part a reader needs.
        assertWithMessage(why)
            .that("connectivity.isOnline()" in code("feature/goals/GoalDetailViewModel.kt"))
            .isTrue()
    }
}
