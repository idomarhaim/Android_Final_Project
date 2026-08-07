---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: ready
issue: https://github.com/idomarhaim/Android_Final_Project/issues/3
created: 2026-08-07 by product-device-pass
---

# Fix issue #3 — task completion has no feedback online and no effect offline

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`. Say `AUTO MODE` at the start if you want it to commit and
push without asking.

**Read first** — [`AGENTS.md`](../AGENTS.md), then
[issue #3](https://github.com/idomarhaim/Android_Final_Project/issues/3) (the
brief for the work — it carries the measurements and the root cause), then
[`CHANGELOG/2026-08-06/product-device-pass.md`](../CHANGELOG/2026-08-06/product-device-pass.md)
(how it was measured, so you can re-measure the same way afterwards).

**Task** — make ticking a task feel instant and make its failure visible. Two
faults, one fix, and they must land together:

1. **~2 s of dead screen online.** Measured at 2.24 s and 1.94 s from tap to the
   checkbox changing.
2. **A silent no-op offline.** The same tap does nothing at all, and says nothing.

The cause is in two places and both need a decision:

- `TaskRepositoryImpl.setDone` is a **`firestore.runTransaction`**. A transaction
  is server-only, so it cannot apply to the offline cache — which is why offline
  there is not even a local write to render. It is a transaction for a real
  reason: ticking a task must move task, user points, user level and goal progress
  consistently. **Decide deliberately** whether to keep it (and fake the result in
  the UI) or replace it with a batched write plus reconciliation. Do not drop the
  transaction casually — read the `challenges` and `time-insights` changelogs on
  why consistency was wanted here.
- `GoalDetailViewModel.toggleTask` **discards the `Resource`** it gets back. Give
  it the shape `logProgress` already has a few lines below in the same file: a
  submitting flag and a surfaced message.

**The trap, and the reason this is one issue and not two:** adding an optimistic
update *on its own* turns the offline no-op into a **silent lie** — the box ticks,
the points rise, and the write never lands. The optimistic update needs a failure
path that undoes it and tells the user.

**Carries over**

- The measurements, the root cause and the reproduction method —
  [issue #3](https://github.com/idomarhaim/Android_Final_Project/issues/3) and
  [`CHANGELOG/2026-08-06/product-device-pass.md`](../CHANGELOG/2026-08-06/product-device-pass.md).
  Re-measure with the same technique (`screenrecord` is variable-frame-rate, so
  gaps in `ffprobe -show_entries frame=pts_time` are literal dead screen) — a fix
  claimed without a number is not this issue's standard.
- **`deleteTask` on the next line has the identical throw-away-the-result shape**,
  and no other repository was audited. Sweep, do not patch one call site.
- The emulator and Gradle-daemon rules —
  [`SESSIONS.md`](../SESSIONS.md); launch with `scripts/run-goalpilot.ps1`.
- A GMS wedge follows `pm clear` on this emulator; `am force-stop
  com.google.android.gms` clears it. See `ProductReview.TODO.optional.md`.

**Out of scope**

- Every other filed issue. #2, #4, #5 are independent; #9, #10, #11 are blocked on
  decisions the `/wayfinder` map is taking.
- `TODO/TODO_FUTURE/ProductModel.TODO.future.md` and anything the concurrent
  `product-model-map` session owns — **check `SESSIONS.md` before your first
  write**, that session may still be live.
- Redesigning the dashboard (`A7`) or adding an offline indicator (`A6`). Related,
  separately recorded, not this.

**Exit** — the checkbox responds immediately; an offline tap is either prevented
or visibly fails; the new latency is **re-measured and quoted**; JVM tests green
(`:app:testDebugUnitTest`) and instrumented green if a composable changed
(`:app:connectedDebugAndroidTest`), with a regression test for the failure path;
`CHANGELOG/YYYY-MM-DD/fix-task-completion-feedback.md` written; issue #3 closed by
the commit; claim released on [`SESSIONS.md`](../SESSIONS.md); commit on approval.
