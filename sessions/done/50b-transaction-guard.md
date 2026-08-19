---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 50
created: 2026-08-20
---

# #50b — Make the `ConnectivityMonitor` deletion *impossible to get wrong*, not merely documented

**Short session. One test file, one commit.** Needs the **Gradle daemon**; needs **no device**.

## Why this exists

`50-offline-stamps` shipped #50 items 1–4 and **held item 5** — deleting
`core/net/ConnectivityMonitor.kt` — because the ticket authorises that deletion on a premise
that is **false at `HEAD`**:

> §5: *"its whole premise was `setDone` being a **server-only transaction** … `C20` removes the
> transaction."*

`TaskRepositoryImpl.setDone` is still `firestore.runTransaction`. #49 removed the *goal* write
from **inside** the transaction, not the transaction itself. A Firestore transaction cannot be
served from cache, so `setDone` is still server-only, the offline pre-check is still
load-bearing, and deleting it re-opens closed **#3** — a **measured 7.9 s** optimistic tick that
is then taken back.

**Three committed artifacts state the false premise**, and one of them is a *ticket that grants
permission to delete*. That is the dangerous shape: a ticket-granted deletion is exactly where
the always-ask gate has already been spent, so nothing downstream catches it.

`50-offline-stamps` annotated the spec (`docs/PRODUCT_v0.3.md` §5.3 §5, dated 2026-08-20) and
put the argument on issue #50. **Prose is not enough.** The next session reads the ticket, not
the annotation, and the whole failure mode of this class is that *prose gets skimmed and each
restatement reads as corroboration*. See `C:\Dev\JARVIS\kb\dev\decision-map-charting.md` §12.

> **The deliverable is a red test, not a paragraph.** Make the build refuse the mistake.

## Task

Add **one** JVM unit test that fails if the offline pre-check is removed while `setDone` is
still a transaction. Source-reading, exactly as `resources/AnalyticsLiteralSweepTest.kt`
already does in this repo — that is the established idiom here, not an invention.

Suggested home: `app/src/test/java/com/idomarhaim/goalpilot/guards/OfflineWriteGuardTest.kt`
*(new package — deliberately not under `domain/` or `feature/`, both of which
`48-settings-surface` claimed on 2026-08-19)*.

### The body, so it is not re-derived

```kotlin
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

    @Test
    fun `while setDone is a transaction, the offline pre-check must survive`() {
        val transactional = "runTransaction" in source("data/firestore/TaskRepositoryImpl.kt")

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
        assertWithMessage(why)
            .that(source("feature/goals/GoalDetailViewModel.kt"))
            .contains("connectivity.isOnline()")
    }
}
```

### Verify it in both directions before committing — the positive control is the point

A guard that cannot fail is worse than no guard. Prove both:

1. **Green as-is** — `./gradlew testDebugUnitTest`.
2. **Red when the thing it guards is removed** — temporarily comment out
   `connectivity.isOnline()` in `GoalDetailViewModel.toggleTask`, re-run, **watch it fail**,
   then revert. Do the same for renaming `ConnectivityMonitor.kt`.
3. **Skipped when the premise flips** — temporarily replace `runTransaction` in
   `TaskRepositoryImpl` with a placeholder, re-run, confirm the test reports **skipped and not
   passed**, then revert.

Step 3 is the one most likely to be skipped and the one that matters: if `assumeTrue` silently
reports a pass in this project's reporter, the expiry mechanism is decorative and the KDoc is
lying. Check the actual XML in `app/build/test-results/testDebugUnitTest/` for a `<skipped/>`
element — do not trust the console summary.

## Carries over

- **`48-settings-surface` held the Gradle daemon and `Pixel_10_Pro_XL` on 2026-08-19.** Check
  `SESSIONS.md` before your first build; this session needs the daemon and no device.
- **Do not delete anything.** This session *prevents* a deletion; it performs none.
- **`guards/` is a new package** and is **not** in `SWEPT_PACKAGES` — #51 owns that sweep. The
  strings here are assertion messages read by developers, not user-facing copy, so they stay
  plain Kotlin literals and the package is not added to the list.
- Touching `app/src/**` means the push fires the **cloud-emulator** workflow
  (`.github/workflows/instrumented-tests.yml`, `push:` on `app/**`). That is free and expected.

## Out of scope

- **Building `C20`'s projection function.** That is its own unfiled issue — see
  `CHANGELOG/2026-08-20/50-offline-stamps-r2.md`, which carries a ready-to-paste body for it.
- **Deleting `ConnectivityMonitor`.** Blocked until C20 ships, by construction.
- **The #51 literal sweep.** `OFFLINE_MESSAGE` stays a hardcoded English literal.

## Exit

- JVM unit green, with the three-direction verification above **recorded verbatim** in
  `CHANGELOG/<today>/50b-transaction-guard.md` — including the `<skipped/>` XML check.
- Board row released; this brief closed to `sessions/done/` with `status: done` in the same commit.
- Commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Per `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` §🚥, seven conditions, exactly one heading.
If `48-settings-surface` is still live when you finish, say so on the line — it holds the daemon
and the emulator, and the next kickoff needs to know.
