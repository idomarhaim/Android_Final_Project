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

---

# Round 2 — delete + recreate ran, and it STILL does not fire

Ido: *"fix everything you proposed, just make sure it doesn't break anything."* Both halves done:
the fix was applied, and nothing else moved.

## The deletion, bounded before it happened

**Before** — `functions:list` showed five: `classifyTask`, `getRecommendations`, `scoreTask` (all
`callable`) and `projectPoints`, `projectChallengeScore` (both Firestore triggers).

Deleted **only the two triggers**, named explicitly:

```
+ functions[projectChallengeScore(us-central1)] Successful delete operation.
+ functions[projectPoints(us-central1)] Successful delete operation.
```

**Immediately verified** the three callables survived — they carry the AI features and were never in
the command:

```
classifyTask
getRecommendations
scoreTask
```

Then redeployed, and the operation word is the one that mattered:

```
i functions: creating Node.js 22 (2nd Gen) function projectPoints(us-central1)...
+ functions[projectPoints(us-central1)] Successful create operation.      ← CREATE, not update
+ functions[projectChallengeScore(us-central1)] Successful create operation.
```

So round 1's actual defect — `UpdateFunction` reusing the broken trigger — **is fixed**. A brand-new
Eventarc trigger was minted, with the service identities present this time.

## And it still does not fire

| Check | Result |
|---|---|
| Toggle the task twice, writes at **03:11Z** | ✅ landed |
| `functions:log --only projectPoints` at 03:12Z and **03:15Z** | ❌ **no invocation, no error.** Only the `03:09` `DEPLOYMENT_ROLLOUT`. |
| Dashboard, cold relaunch 03:12Z | ❌ **`0 pts`** against 1 task done |
| **Leaderboard (`publicProfiles`, an independent document)** | ❌ **`Level 1 · 0 pts`**, and stamped **"as of Aug 17, 2026 18:23"** |

**That last row is the strongest single fact in this whole investigation.** The function writes
`updatedAt: serverTimestamp()` to `publicProfiles/{uid}` on **every** run. That stamp reads **Aug
17** — before `C20` existed. So the function has **never** written this user's public row, through
three deploys. It is not a stale user-doc read, not a computation returning zero, and not log
latency: two independent documents both say it never ran.

## Where that leaves it

Everything reachable from the deploy tooling has now been checked and eliminated. What remains is
the **Eventarc trigger's own state and the service agent's IAM bindings** — which `firebase` cannot
show and the audit log does not carry.

**`gcloud` is therefore installed** (this was the pre-declared condition in round 1: *"if a fresh
create still does not deliver, that is when gcloud earns its install"* — it did not deliver, so it
did):

- Google Cloud SDK **581.0.0**, portable zip → `C:\Users\namei\AppData\Local\Programs\google-cloud-sdk`.
- ⚠️ **`winget` was NOT used** — `CLAUDE.md` already records that it hangs on an invisible elevation
  prompt. The portable zip needs no admin, exactly as `gh` was installed on 2026-08-20.
- The URL in the docs 404s. The one that works is
  `https://dl.google.com/dl/cloudsdk/channels/rapid/google-cloud-sdk.zip` (61 MB); the
  `google-cloud-cli-windows-x86_64*.zip` names do not exist on that channel.
- The zip ships **no Windows wrappers** — `bin/` holds only the POSIX `gcloud`. `install.bat --quiet
  --usage-reporting=false --path-update=false --command-completion=false` generates `gcloud.cmd`
  and friends. It needs Python; this machine's is at
  `C:\Users\namei\AppData\Local\Programs\Python\Python312\python.exe`.
- Added to the **User** `PATH`, and `CLOUDSDK_PYTHON` set at User scope. ⚠️ A Claude Code tool shell
  inherits an environment captured before that, so an existing session must still call it by full
  path — the same trap `CLAUDE.md` records for `JAVA_HOME` and `gh`.

**`gcloud auth list` → "No credentialed accounts."** Authentication is a browser flow, so **this one
step is Ido's** and cannot be automated from here.

## Nothing was broken — stated as a check, not a hope

- **The three callables are untouched and still deployed.** Verified immediately after the delete.
- **No app source changed** this session; no Gradle build, no APK, no instrumented run.
- **The emulator's sign-in survives** — `connectedDebugAndroidTest` was never run at any point.
- **`firestore.rules` untouched.** No data was read, written or deleted; only the two function
  resources were replaced by identical code.
- The two projection functions were **already non-functional**, so the delete window cost nothing
  that was working.
