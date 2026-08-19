# 50b-transaction-guard — the build now refuses the deletion, and the negative control found the guard wrong on its first draft

> **Summary:** One JVM source-reading unit test now goes red if `ConnectivityMonitor` or the
> `connectivity.isOnline()` pre-check is deleted while `TaskRepositoryImpl.setDone` is still
> `firestore.runTransaction` — and reports **skipped, not passed**, the day that stops being true.
> The three-direction verification the brief demanded was not a formality: **direction 2 failed the
> first time**, and the guard as briefed passed with the pre-check fully commented out.

**Session:** `50b-transaction-guard` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE`
**Branch:** `main` · Executes [`sessions/done/50b-transaction-guard.md`](../../sessions/done/50b-transaction-guard.md),
written by [`50-offline-stamps` round 2](50-offline-stamps-r2.md)
**Singletons:** `#gradle-daemon` held for five JVM unit runs, released with this commit. **No device
touched, no emulator started, nothing signed in or out.**

---

## Why this exists

[#50](https://github.com/idomarhaim/Android_Final_Project/issues/50) §5 authorises deleting
`core/net/ConnectivityMonitor.kt` on the stated premise that *"`C20` removes the transaction"*.
**That premise is false at `HEAD`,** re-verified in this working tree before the board was claimed:

```
app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt:98
    firestore.runTransaction { txn ->
```

A Firestore transaction cannot be served from cache, so `setDone` is still server-only, the offline
pre-check is still load-bearing, and deleting it re-opens closed
[#3](https://github.com/idomarhaim/Android_Final_Project/issues/3) — the **7.9 s** optimistic tick
that is then taken back (measured on a device;
[`CHANGELOG/2026-08-08/fix-task-completion-feedback.md`](../2026-08-08/fix-task-completion-feedback.md)).

`50-offline-stamps` had already annotated the spec and argued it on the issue. The deliverable here
is the half that cannot be skimmed.

## What shipped

**One file**, `app/src/test/java/com/idomarhaim/goalpilot/guards/OfflineWriteGuardTest.kt` *(new
package `guards/`, deliberately outside `domain/` and `feature/`)*. It reads three source files and:

1. **Skips** — via `assumeTrue`, not an early return — if `runTransaction` has left
   `TaskRepositoryImpl.kt`. That skip **is** the signal that #50 item 5 is unblocked.
2. Otherwise asserts `core/net/ConnectivityMonitor.kt` exists and `GoalDetailViewModel.kt` still
   calls `connectivity.isOnline()`, with the whole argument in the failure message.

Source-reading is the established idiom here, not an invention: `resources/AnalyticsLiteralSweepTest`
already does it, and this file reuses its `stripComments` helper **byte-for-byte**.

## 🧪 Tests

**Layers.** JVM unit only, which is the whole of this unit — the test reads files off disk and has no
Android, UI or database surface, so **instrumented, endpoint, database, client-page and UI E2E layers
are not applicable and were not run**. The cloud instrumented workflow fires on push anyway
(`.github/workflows/instrumented-tests.yml`, `push:` on `app/**`); that is expected and free.

**Full JVM unit suite, at the committed state:**

```
suites=45  tests=420  skipped=0  failures=0  errors=0
./gradlew :app:testDebugUnitTest → BUILD SUCCESSFUL
```

`419/44` before this unit, `420/45` after — one new suite, one new case, nothing else moved.

### The three-direction verification, verbatim

Counts read from `app/build/test-results/testDebugUnitTest/TEST-…OfflineWriteGuardTest.xml`, never
from the console summary.

**1 — green as-is.**

```xml
<testsuite name="…OfflineWriteGuardTest" tests="1" skipped="0" failures="0" errors="0" time="0.086">
  <testcase name="while setDone is a transaction, the offline pre-check must survive" …/>
```

**2 — red when the thing it guards is removed.** Two shapes, and **the first one failed.**

*(2a, first attempt — the guard exactly as the brief wrote it.)* `connectivity.isOnline()` commented
out in `GoalDetailViewModel.toggleTask`, everything else untouched:

```xml
<testsuite … tests="1" skipped="0" failures="0" errors="0" time="0.074">   ← FALSE PASS
```

**The guard could not fail on the input it most exists for.** The brief's body compared the *raw*
file text, and a commented-out call still contains the string `connectivity.isOnline()`. Commenting
a check out is the commonest way one is disabled in practice, and this is the standing *check the
instrument on the hardest input it exists for* failure exactly: it degraded silently on the case
that motivates it, while every other input kept saying it worked.

**Fix:** both source checks now read `code(path)` = `stripComments(source(path))`, the identical
regex pair `AnalyticsLiteralSweepTest` already uses. Its known crudeness (a `//` inside a string
literal is also stripped) can only make the test fire when it should *not* — loud, and the safe
direction.

*(2a, re-run after the fix.)*

```xml
<testsuite … tests="1" skipped="0" failures="1" errors="0" time="0.197">
  <failure message="TaskRepositoryImpl.setDone is still firestore.runTransaction, which cannot be
   served from the Firestore cache. Offline it fails after a measured 7.9 s, … Deleting this
   re-opens closed #3. …" type="com.google.common.truth.AssertionErrorWithFacts"/>
```

That run also showed Truth dumping all 240 lines of `GoalDetailViewModel` beneath the message, so the
second assertion now tests a `Boolean` rather than the file text — the `why` is the part a reader
needs, and it was buried.

*(2b — `ConnectivityMonitor.kt` renamed to `NetworkMonitor.kt`.* Kotlin does not require the file name
to match the class name, so this compiles cleanly and isolates the file-existence check.*)*

```xml
<testsuite … tests="1" skipped="0" failures="1" errors="0" time="0.226">
```

**3 — skipped, not passed, when the premise flips.** `runTransaction` removed from
`TaskRepositoryImpl.kt` **compile-safely**, via a throwaway `FirebaseFirestore.runTxn` alias in a
temporary file, so the literal left the file under test without gutting `setDone`:

```xml
<testsuite name="…OfflineWriteGuardTest" tests="1" skipped="1" failures="0" errors="0" time="0.091">
  <testcase name="while setDone is a transaction, the offline pre-check must survive" …>
    <skipped/>
  </testcase>
```

`skipped="1"`, `failures="0"`, and a real `<skipped/>` element. **The expiry mechanism is not
decorative** — this was the direction the brief called most likely to be skipped and most likely to
be lying, and it holds.

**Every control was reverted and each revert verified by `git status --porcelain`**, which returned
only the new untracked test directory afterwards. The temporary alias file was deleted.

## Two deviations from the brief, both deliberate

1. **The test body is not verbatim.** `stripComments` / `code()` were added and the last assertion
   was made a `Boolean`. Both came out of running the verification the brief itself prescribed; the
   briefed body would have shipped a guard that passes when the guard is commented out.
2. **The KDoc gained two sections** — why the checks are comment-stripped, and why this is not
   `GoalDetailViewModelTest` twice.

## Why this is not the same test twice

`GoalDetailViewModelTest.an offline tap is refused outright and never fakes a tick` *does* go red
when the pre-check is removed. It is not a substitute: **#50 §5 authorises removing the pre-check**,
so whoever executes that ticket deletes that case in the same breath and is right to. Only this file
ties the permission to a **checkable condition** instead of to a sentence in a ticket, and it is the
one test whose own deletion has a stated precondition — the skip.

**What it still cannot see:** a call left in place but made unreachable, or moved out of
`toggleTask`. Not chased on purpose — a guard that tries to know which branch runs is a guard nobody
can predict, which is why `AnalyticsLiteralSweepTest` stays crude too.

## Scope held

- **Nothing was deleted.** This session prevents a deletion and performs none.
- `guards/` was **not** added to `SWEPT_PACKAGES` — #51 owns that sweep, and these strings are
  assertion messages read by developers, not user-facing copy.
- `C20`'s build half is still unbuilt and still untracked by any issue; the ready-to-paste body is in
  [`50-offline-stamps-r2.md`](50-offline-stamps-r2.md). **That is the thing that unblocks #50 item
  5**, and it is nobody's yet.

## 🕐 One note on dates

The machine clock read `2026-08-19T22:0x` during these runs while the harness date is `2026-08-20`;
the XML timestamps above are quoted as the machine wrote them. Folder and board dates follow the repo
convention already set by the two sessions that committed today.
