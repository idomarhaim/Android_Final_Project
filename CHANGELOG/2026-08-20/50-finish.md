# `50-finish` — 2026-08-20

> **Summary:** `#50` item 5 — `ConnectivityMonitor`, its `GoalDetailViewModel` pre-check and `OFFLINE_MESSAGE` are deleted, authorised by `OfflineWriteGuardTest` reporting `<skipped/>` rather than by the ticket's prose; JVM unit 425 → 422 (the skip retired with the guard), `assembleDebug` green, and the offline tick itself is `Untested:` because no instrumented test can reach it.

## What this session was

The smallest unit in the roadmap and the one with the most careful gate in front of it. #50 §5
authorises deleting `ConnectivityMonitor` on the stated premise *"`C20` removes the transaction"* —
a premise that was **false for six days**. A ticket-granted deletion is precisely where the human
always-ask gate has already been spent, so nothing downstream would have caught it. The gate this
session actually obeyed was mechanical.

## ⛔ The precondition, and what it returned

```
./gradlew :app:testDebugUnitTest --tests "…guards.OfflineWriteGuardTest"
```

```xml
<testsuite name="com.idomarhaim.goalpilot.guards.OfflineWriteGuardTest"
           tests="1" skipped="1" failures="0" errors="0" timestamp="2026-08-20T01:41:01" …>
  <testcase name="while setDone is a transaction, the offline pre-check must survive" …>
    <skipped/>
  </testcase>
```

**`<skipped/>` present → GO.** `c20-build-half` (`731961b`, `e5e0ef0`) made `setDone` a single
`update()` on one document; the guard detected that by itself and stood down. The authorisation
came from the code.

> ⚠️ **The brief's warning about the console was an understatement, and that is worth recording.**
> It said *"a skip and a pass look alike there"*. In fact the console prints **nothing at all**
> about the test — 25 lines of task status and `BUILD SUCCESSFUL in 7s`. There is no ambiguous
> line to misread; there is no line. The JUnit XML is the only surface that carries the signal.

## Deleted

| Path | What went |
|---|---|
| `core/net/ConnectivityMonitor.kt` | whole file — interface, `AndroidConnectivityMonitor`, `ConnectivityModule` Hilt binding. The `core/net/` package is now empty and gone. |
| `feature/goals/GoalDetailViewModel.kt` | the `if (!connectivity.isOnline())` pre-check, the `connectivity: ConnectivityMonitor` constructor parameter, the import, and `OFFLINE_MESSAGE`. |
| `guards/OfflineWriteGuardTest.kt` | whole file — its own KDoc named this moment as its expiry. The `guards/` package is now empty and gone. |
| `feature/goals/GoalDetailViewModelTest.kt` | the `connectivity` mock, its import, the constructor argument, and **two** obsolete test cases. |

**`SAVE_FAILED_MESSAGE` stays** — a write can still fail for reasons that are not connectivity,
and `toggleTask`'s undo path is still the thing that surfaces it.

### The brief's enumeration was one test short

It named one obsolete case (`an offline tap is refused outright and never fakes a tick`, `:190`).
There were **two**: `an offline tap never reaches the repository at all` (`:203`) asserts
`coVerify(exactly = 0) { taskRepository.setDone(…) }` — precisely the behaviour this unit removes.
Both are deleted. Found by re-running the grep rather than working the list; the brief itself
prescribes that for *source* (`grep -rn ConnectivityMonitor app/src`) and did not extend it to
tests.

## Three texts my own deletion falsified — repaired, not left standing

Not in the brief's four-item list. Each is a factual claim *about the mechanism this session
changed*, and each would have been false at `HEAD` the moment the deletion landed.

1. **`GoalDetailViewModel._pendingToggles` KDoc** said the overlay exists because `setDone` is a
   *server-only `runTransaction`* that *never touches the offline cache*. Both halves are now
   false. Rewritten with provenance: `Observed:` the single `update()` at `731961b`; `Inferred:`
   that the optimistic half is now largely redundant, since `inFlight` retires an entry as soon as
   the cached snapshot arrives — which is immediately; `Untested:` whether removing the overlay
   entirely is safe. **The overlay is kept.** Deleting it is a behaviour change and its own ticket.
   The **undo** half is still load-bearing: `update().await()` resolves on *server ack*, so a
   server-rejected write returns long after the tick is drawn.
2. **`uiState`'s retirement comment** referred to *"the transaction's completion callback"*. Now
   `setDone`'s own.
3. **`TaskRepositoryImpl`'s KDoc** pointed a future reader at `guards/OfflineWriteGuardTest.kt`
   *"watching for exactly this"* — a file this session deleted. Now records that the guard fired,
   was read, and was retired, and says plainly it no longer exists.

**`docs/PRODUCT_v0.3.md` §5** carried a dated ⚠️ annotation (added by `50-offline-stamps`) saying
`ConnectivityMonitor` *"therefore still exists"* and ending *"the sentence above becomes true the
day `C20`'s build half ships"*. That day came. Closed with a ✅ annotation naming the two commits
and the skip — **annotated, not rewritten**, matching `de696a6`: the design record was never wrong,
only undated.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | **422 tests, 0 failures, 0 errors, 0 skipped**, 45 suites — **was 425 / 46 / 1 skipped** (`CHANGELOG/2026-08-20/c20-build-half.md:166`). −3 tests, −1 suite; the lone skip retired with the guard that produced it. `GoalDetailViewModelTest` 14/14 green. |
| **Build** (`:app:assembleDebug`) | **green**, 51s. |
| **Instrumented** (`connectedDebugAndroidTest`) | **not run — and it could not have helped.** No test in `app/src/androidTest` touches `toggleTask`, `setDone`, task completion or connectivity; the 16 suites are locale, charts, pickers, empty states and consent notices. Verified by grep, not assumed. |
| **Security rules** (`firestore-tests/`) | not run — this unit touches no `.rules` file and no Firestore path. |
| **Cloud Functions** (`functions/`) | not run — explicitly out of scope (`c20-build-half`'s). |

### Was an offline tap observed succeeding? **No.**

`Untested:` **nothing in this session observed an offline tick landing.** What was established is
narrower and worth stating exactly: `setDone` is now
`tasksCol(uid).document(taskId).update(…).await()`, a single-document write, and Firestore applies
such a write to the local cache **synchronously** — so the snapshot listener has something to
render with the radio off. That is a reading of the code, not an observation of the app.

**What would check it:** the cloud emulator (`.github/workflows/instrumented-tests.yml`) with
`adb shell svc data disable` / airplane mode, tapping a task on Goal Detail and watching the tick
land, survive a recomposition, and sync on reconnect. **One caveat that makes this more than a
button press:** Goal Detail requires a signed-in account, and the cloud runner has none — so this
needs either a seeded test account or a local device pass. It is not a five-minute check, which is
why it is marked rather than done.

## Scope held

- Nothing in `functions/`, `firestore.rules` or the DTOs — `c20-build-half`'s.
- #7's create-and-complete affordance rides the same C20 change and is its own ticket.
- **The deletion was not extended by one file.** `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md`
  §Auto-mode: *"`#50`'s deletion of `ConnectivityMonitor` is authorised by its own ticket, not by
  this mode — name it in the reply and do not extend it by one file."* Named; not extended.

## 🔀 Push held, and what would ride along when it goes

**Held at precondition 2.** The range carries **two source deletions** —
`core/net/ConnectivityMonitor.kt` and `guards/OfflineWriteGuardTest.kt`. The one deletion carve-out
covers a fully-drained `kb-candidates/` file only, and these are not that. The *deletion* is
authorised by #50 itself; **publishing** it is a separate gate and it is Ido's.

The **rename** in the range does *not* hold it: `sessions/50-finish.md` →
`sessions/done/50-finish.md` lands in the same commit as that brief's `status: done`, which is
exactly the brief-close carve-out.

`Observed:` upstream had **not** moved as of `2026-08-20T02:03:04Z` — checked with `git fetch` +
`git log HEAD..@{u}`, empty. Held **and still unpublished as of that check**; a sibling's push
would publish these commits on their schedule with no gate of mine involved, so that timestamp is
the claim, not "held" on its own.

### Foreign commits in the range — adjudicated, not merely noticed

| Commit | Session | Paths | Verdict |
|---|---|---|---|
| `8be4b78` | `c20-build-half` *(round 3)* | `SESSIONS.md` (claim) | rides along |
| `1a1f1c9` | `c20-build-half` *(round 3)* | `CLAUDE.md`, `SESSIONS.md` | rides along |

**Why they ride:** that session **explicitly released** — its row moved to *Recently released* with
a full account in `1a1f1c9`, which is a positive signal written by that session about itself and
settles the question without a transcript check. Their paths are clean in the tree. Disjoint from
this session throughout: they held `CLAUDE.md`, this session held four `app/**` paths plus its own
brief and changelog, and their note records checking that this session's staged deletions survived
their commits intact.

**Two of their findings matter to what is still owed here:** `gh` is at
`C:\Program Files\GitHub CLI\gh.exe` (not the `%LOCALAPPDATA%` path `CLAUDE.md` used to give), and
**`gh` is now authenticated** — Ido ran `gh auth login --web` on 2026-08-20. So the #50 close
comment is mechanically unblocked; only the permission is missing.
