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

---

# Round 2 — the offline tap, observed

**Ido, 2026-08-20:** *"do the required fixes, if you need SIGN IN use the emulator"*, then push.
Round 1's one open `unverified` is now closed by observation.

## 📱 No sign-in was needed from Ido

`Pixel_10_Pro_XL_B` (`emulator-5554`) was **already running**, with GoalPilot debug installed and
the device Google account `rachil751@gmail.com` present and the app already authenticated. Claimed
on the board (`58b4d97`) before the first device command. **`connectedDebugAndroidTest` was
deliberately NOT run** — it uninstalls the app and would have destroyed the very session this round
needed.

## What was observed, in order

| # | Step | Result |
|---|---|---|
| 1 | `:app:installDebug` of the post-deletion build | installed on `Pixel_10_Pro_XL_B(AVD) - 15` |
| 2 | Baseline: goal *"Be Slim, Pretty, and Just"*, one unticked task, ring **0%**, dashboard **0 tasks done** | — |
| 3 | `cmd connectivity airplane-mode enable` + `svc wifi disable` + `svc data disable` | `airplane_mode_on=1`, **`Active default network: none`** |
| 4 | **Tap the task, offline** | ✅ box ticked, title struck through, ring **0% → 1%**, `0/100` → `1/100`. **Instant. No refusal, no `OFFLINE_MESSAGE` snackbar** — which is exactly what the deleted pre-check used to produce. |
| 5 | Wait **12 s** (the old transaction took the tick back at a measured **7.9 s**) | ✅ still ticked, still 1% — **not** taken back |
| 6 | **Force-stop the app and cold-relaunch, still in airplane mode** | ✅ **still ticked.** This is the decisive one: a process kill destroys the in-memory `_pendingToggles` overlay, so the surviving tick proves the write is in **Firestore's local cache**, not in UI state. |
| 7 | Airplane off, reconnect (`Active default network: 105`) | ✅ tick survives — the server accepted it, no rollback |
| 8 | Dashboard after sync | ✅ **Tasks done 0 → 1**, **This week 0 → 1** |

**So the `Untested:` from round 1 is now `Observed:` 2026-08-20.** The optimistic tick does not
merely go un-refused offline — it lands, persists across a process death, and syncs. Step 6 is the
evidence round 1 could not produce by reading code.

Screenshots: `…/scratchpad/shots/01-launch.png` … `10-points-recheck.png` (session scratchpad, not
committed).

## ⚠️ A defect found on the way, and it is NOT this session's

**`projectPoints` does not fire on a real task write.** Points stayed at **0 pts** through step 8,
and were still **0** after a further ~12 minutes and a second cold relaunch, while *Tasks done*
correctly read **1**.

Evidence, gathered rather than assumed:

- `firebase functions:list` — `projectPoints` is **deployed and ACTIVE**, `v2`,
  `google.cloud.firestore.document.v1.written`, filter
  `document = users/{uid}/tasks/{taskId}` (match-path-pattern).
- The client writes to exactly that path — `TaskRepositoryImpl.tasksCol()` is
  `users/{uid}` → `.collection("tasks")`, `FirestorePaths.USERS = "users"`, `TASKS = "tasks"`. **The
  filter and the write path match**, so this is not a path typo.
- `firebase functions:log --only projectPoints` — the **only** execution lines are
  `2026-08-20T01:09:14Z` and `01:09:17Z`, both `DEPLOYMENT_ROLLOUT`. The tick landed at
  **~02:12Z**; the check ran at **02:26Z**. **No invocation in between.**
- Earlier in that log: the first deploy attempt failed with *"Permission denied while using the
  Eventarc Service Agent"* (`01:01:31Z`), which `c20-build-half` r2 retried successfully at
  `01:08`. That is the likeliest lead.

`Observed:` points did not move. `Inferred:` the trigger is not being delivered. `Untested:`
whether the cause is Eventarc delivery, the function body, or log latency — **not diagnosed here,
because it is `c20-build-half`'s deliverable and diagnosing it inside this unit would be the same
"one session both grants and spends" mistake the guard exists to prevent.**

**It does not touch this unit.** #50 item 5 removed a *client-side connectivity pre-check*; the
projection is a *server-side trigger*. The defect was already present when `941d6a8` was made — the
deploy is from `01:09`, before it. Handing it on rather than adopting it.

## 🧪 Tests — re-run from scratch, not read off a cache

| Layer | Result |
|---|---|
| **JVM unit** (`--rerun-tasks`) | **422 tests / 45 suites / 0 failures / 0 errors / 0 skipped**. Forced re-run, **2m33s** wall clock — deliberately not an `UP-TO-DATE` report, per `kb/dev/look-at-your-own-output.md` §4c, whose whole incident was a suite reporting green in 1s having executed nothing. |
| **Build** (`:app:assembleDebug`) | green, same forced run |
| **Real device** | the 8 steps above, on `Pixel_10_Pro_XL_B` |
| **Instrumented** (`connectedDebugAndroidTest`) | **deliberately not run** — it uninstalls the app and would destroy the signed-in session this round required. Unchanged from round 1: no instrumented test touches `toggleTask`, `setDone` or connectivity. |
| **Rules / functions** | not run — this unit touches neither. |
