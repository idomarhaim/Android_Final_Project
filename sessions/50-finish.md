---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: active
issue: 50
created: 2026-08-20
---

# `#50` item 5 — delete `ConnectivityMonitor`, and let the guard tell you it is allowed

**The smallest session in the plan: ~10 lines deleted, one test file removed. Closes #50.**
Needs the **Gradle daemon**; needs **no device**.

> **Runs alone, and first among the wave-2 pair.** It edits `GoalDetailViewModel.toggleTask`, which
> `7-quickadd-complete` also reaches. Ten minutes; do not overlap it with anything.

## ⛔ Precondition — one check, and it is mechanical

Run this first:

```bash
./gradlew :app:testDebugUnitTest --tests "com.idomarhaim.goalpilot.guards.OfflineWriteGuardTest"
grep -c "<skipped/>" app/build/test-results/testDebugUnitTest/TEST-*OfflineWriteGuardTest.xml
```

- **`<skipped/>` present → GO.** `setDone` is no longer a transaction; `c20-build-half` has landed
  and this deletion is authorised *by the code*, not by a sentence in a ticket.
- **Test passes green → STOP.** `setDone` is still `firestore.runTransaction`, the offline
  pre-check is still load-bearing, and deleting it re-opens closed
  [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3) — a **measured 7.9 s**
  optimistic tick that is then taken back. Run `/kickoff c20-build-half` instead.

**Do not read the console summary for this.** A skip and a pass look alike there; the XML element
is the evidence. That is the whole reason this brief exists as a separate unit.

## Why the ticket alone is not enough

#50 §5 authorises this deletion on the premise *"`C20` removes the transaction"*. That premise was
**false for six days** and nothing downstream would have caught it — a ticket-granted deletion is
exactly where the human always-ask gate has already been spent. The mechanism is written up at
`C:\Dev\JARVIS\kb\dev\decision-map-charting.md` **§12a**, with this ticket as the worked example.
`OfflineWriteGuardTest` is the half that cannot be skimmed.

## Task — four deletions, in this order

1. `app/src/main/java/com/idomarhaim/goalpilot/core/net/ConnectivityMonitor.kt` — whole file.
2. `feature/goals/GoalDetailViewModel.kt` — the `if (!connectivity.isOnline())` block in
   `toggleTask` (~`:173`), the `connectivity: ConnectivityMonitor` constructor parameter, and the
   import.
3. `feature/goals/GoalDetailViewModel.kt` — `OFFLINE_MESSAGE` in the companion (~`:243`).
   **`SAVE_FAILED_MESSAGE` stays** — a write can still fail for reasons that are not connectivity.
4. `app/src/test/java/com/idomarhaim/goalpilot/guards/OfflineWriteGuardTest.kt` — whole file. Its
   own KDoc names this moment as its expiry.

Then `GoalDetailViewModelTest`'s offline case (`an offline tap is refused outright and never fakes a
tick`, ~`:190`) and its `connectivity` mock (~`:53`) go too — that case asserts behaviour this unit
deliberately removes. **Deleting it is correct here and only here**, because the premise it
encoded has actually changed.

Check for other injection sites before you finish: `grep -rn ConnectivityMonitor app/src`.

## Verify — the optimistic tick must now be true, not merely un-blocked

The point of C20 is that an offline tick **works**, not that the refusal was removed. So:

- **JVM unit green**, and say what the count moved from and to.
- **Instrumented**: assert the offline path end-to-end if a test can reach it; if it cannot, say so
  explicitly rather than silently.
- **Say plainly whether an offline tap was actually observed succeeding**, and by what means. If
  nothing in this session observed it, mark it `Untested:` with what would check it — the cloud
  emulator can toggle airplane mode via `adb`, and that is the cheapest real check.

## Out of scope

- Anything in `functions/`, `firestore.rules`, or the DTOs — that is `c20-build-half`'s.
- #7's create-and-complete affordance, which rides the same C20 change but is its own ticket.

## Exit

- JVM unit green · `assembleDebug` green · layers that do not apply stated explicitly.
- **Post the #50 close comment** naming the guard's skip as the evidence the premise flipped.
  `gh` works with no `gh auth login` — see [`CLAUDE.md`](../CLAUDE.md) for the `git credential
  fill` recipe. **Ido's permission for the write is still required; ask.**
- `CHANGELOG/<today>/50-finish.md` · board row released · this brief closed to `sessions/done/`
  with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Seven conditions, exactly one heading, per `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` §🚥.
Name `/kickoff 7-quickadd-complete` — it is the next thing C20 unblocks.
