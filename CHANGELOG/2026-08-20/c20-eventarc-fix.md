# `c20-eventarc-fix` — 2026-08-20

> **Summary:** the redeploy did **not** fix `projectPoints`, and the audit log says exactly why — it was an `UpdateFunction` that **reused the original Eventarc trigger** (`projectpoints-956857`), the one created while the service agent lacked permission. A trigger born broken is not repaired by updating the function around it; the functions must be **deleted and recreated**, which is a deletion and therefore Ido's call.

**Session:** `c20-eventarc-fix` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE`
**Predecessor:** `50-finish` r3, which diagnosed the cause and could not deploy.

---

## What Ido did

Added `Bash(firebase:*)` / `PowerShell(firebase:*)` to `C:\Users\namei\.claude\settings.json` and ran
the deploy himself:

```
+  functions[projectPoints(us-central1)] Successful update operation.
+  functions[projectChallengeScore(us-central1)] Successful update operation.
+  Deploy complete!
```

## Why `Deploy complete!` was again not the answer

The brief said not to trust it, and the brief was right for the **second** time on the same defect.

| Check | Result |
|---|---|
| Toggle the task twice (untick → re-tick), final state DONE, at **02:56Z** | ✅ writes landed — ring 1%, box ticked |
| `functions:log --only projectPoints` at 02:58Z and 02:59Z | ❌ **no invocation, and no error.** Last lines are the `02:53` `DEPLOYMENT_ROLLOUT`. |
| Dashboard after cold relaunch, 02:59Z | ❌ **still `0 pts`** against **1** task done |

## The cause, now pinned exactly

Parsed out of the deploy's own audit log:

```
methodName : google.cloud.functions.v2.FunctionService.UpdateFunction   ← not CreateFunction
trigger    : projects/goalpilot-56e30/locations/us-central1/triggers/projectpoints-956857
state      : ACTIVE
```

**`projectpoints-956857` is the same trigger id the `01:08` deploy created** — the deploy that ran
minutes after `01:01:31Z` failed with *"Permission denied while using the Eventarc Service Agent"*.

So: `firebase deploy` on an existing function issues **`UpdateFunction`**, which updates the
function's code and configuration and **leaves the existing Eventarc trigger in place**. Generating
the service identities (which the deploy did, visibly) does not retro-fix a trigger that was created
before they existed. `state: ACTIVE` is the **function's** state and says nothing about whether the
trigger delivers — which is why it read healthy through three separate checks.

**`Observed:`** same trigger id across both deploys, `UpdateFunction` both times, zero invocations.
**`Inferred:`** the trigger must be **recreated**, not updated. **`Untested:`** confirmed only by
doing it.

## Ruled out first, so nobody re-treads them

Beyond `50-finish` r3's five (path, region, deployed, code loads, log latency), this session also
killed:

- **A silently-zero computation.** `derived.ts` `TaskFact` is `{done?: boolean, points?: number}`
  and `pointsFromTasks` sums `points` where `done === true`; `TaskDto` stores exactly `done: Boolean`
  and `points: Int`, and the task in question renders `+30`. **The shapes match** — a firing function
  would have written 30, not 0. So this is not a quiet mis-computation dressed as a no-fire.
- **A function that fires and throws.** There is no error entry either. It does not run at all.

## What is needed next — and it is a deletion, so it is Ido's

```powershell
cd C:\Dev\Android_Final_Project
firebase functions:delete projectPoints projectChallengeScore --project goalpilot-56e30 --force
$env:FUNCTIONS_DISCOVERY_TIMEOUT = "120"
firebase deploy --only functions:projectPoints,functions:projectChallengeScore --project goalpilot-56e30
```

Deleting forces the next deploy to issue **`CreateFunction`**, which mints a **new** Eventarc trigger
— now with the service identities present.

**Risk, stated honestly:** low. Both functions are already non-functional, so the deletion window
costs nothing that is currently working; the source is in git and the redeploy recreates them in
~2 minutes. But it is a deletion of live infrastructure and therefore always-ask in both modes.

**Verify afterwards the same way, and still not by the headline:** tick a task, then
`firebase functions:log --only projectPoints -n 5` must show an info line with
`{uid, points, factCount}`. Expected `points: 30`.

**If a fresh create still does not deliver** — that is when `gcloud` finally earns its install, to
read the Eventarc trigger's own state and the service agent's IAM bindings. Deliberately still not
installed: every question so far has been answerable from the audit log, and the remaining one is
not yet open.

## 🧪 Tests

| Layer | Result |
|---|---|
| **Live behaviour** | ❌ the defect **reproduces** after the redeploy — this session's finding |
| **JVM unit / build** | not re-run; no app source was touched this session |
| **Functions unit** (`functions/test`) | not run — the code is not in question, the trigger wiring is. `c20-build-half`'s 9/9 pass and are irrelevant here: they run against the local emulator, which does not exercise Eventarc at all. |
