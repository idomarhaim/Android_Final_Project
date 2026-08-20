---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 42
created: 2026-08-20 by `50-finish` (round 3)
---

# `C20` — `projectPoints` never fires: the trigger exists, Eventarc never wired it

**The diagnosis is DONE. This session is the fix and its verification — likely one command and
ten minutes.** Needs **no device** and **no Gradle**. Needs the **live `goalpilot-56e30` project**.

## ⛔ Precondition — read this before you start, it is the whole reason this brief exists

**The fix is a `firebase deploy`, and the Claude Code auto-mode classifier REFUSES it.** `50-finish`
r3 hit this twice: once on the deploy, once on writing a `.claude/settings.json` that would have
allowed the deploy. **Do not try to route around it** — a gate on changing live infrastructure and
on an agent widening its own permissions is a gate that should hold.

So **step 1 is Ido**, and there is no point starting without it. One of:

- **(a) He runs it himself** — PowerShell, and note the syntax, because the bash form fails here:
  ```powershell
  cd C:\Dev\Android_Final_Project
  $env:FUNCTIONS_DISCOVERY_TIMEOUT = "120"
  firebase deploy --only functions:projectPoints,functions:projectChallengeScore --project goalpilot-56e30
  ```
- **(b) He adds the permission rule himself** and you retry. Project file
  `C:\Dev\Android_Final_Project\.claude\settings.json` (does not exist yet) or his existing
  `C:\Users\namei\.claude\settings.json`:
  ```json
  { "permissions": { "allow": ["Bash(firebase:*)", "PowerShell(firebase:*)"] } }
  ```
  ⚠️ **`Bash(firebase deploy:*)` is NOT enough** if the call carries an env-var prefix — the command
  string would not *begin* with `firebase`. And it is unproven that a permission rule overrides the
  **classifier** at all; that is a different mechanism. Treat (b) as worth one try, not as a plan.

**If neither has happened, say so and stop.** Do not do half of this.

## What is broken

`projectPoints` **does not fire on a real task write.** Points read `0 pts` while *Tasks done* reads
`1`. Observed end-to-end on `Pixel_10_Pro_XL_B`, 2026-08-20 — a task ticked at `~02:12Z`, checked at
`02:26Z` and `02:36Z`.

## What is already ruled OUT — do not re-derive these

| Hypothesis | How it was killed |
|---|---|
| Wrong document path | `FirestorePaths.USERS="users"` / `TASKS="tasks"`, so the client writes `users/{uid}/tasks/{taskId}`; the trigger filter is the **same string**. |
| Region mismatch *(the classic silent no-fire)* | `firebase firestore:databases:get "(default)"` → Location **`us-central1`**; trigger region **`us-central1`**. |
| Not deployed | `firebase functions:list` → `projectPoints` **v2, ACTIVE**, `google.cloud.firestore.document.v1.written`. |
| Code broken / won't load | `node -e "require('./lib/index.js')"` → **199 ms**, exports all five functions. |
| Cloud Logging latency *(my own instrument)* | Re-queried twice, 24 min after the tick. Genuinely absent. |

## The cause, and the evidence for it

**The Eventarc and Pub/Sub service agents did not exist when `c20-build-half` r2 first deployed, so
the trigger was created but never wired.**

A `--dry-run` on 2026-08-20 emitted:

```
i functions: generating the service identity for pubsub.googleapis.com...
i functions: generating the service identity for eventarc.googleapis.com...
```

A dry run that must **generate** those identities is saying they were absent. That is exactly what
r2's first deploy died on at `01:01:31Z`:

> `Validation failed for trigger …/triggers/projectpoints-764090: Invalid resource state for "":
> Permission denied while using the Eventarc Service Agent.`

r2 retried at `01:08` and the **function** was created (`ACTIVE`, new trigger id
`projectpoints-956857`) — so `Deploy complete!` was **truthful about the function and silent about
the trigger**.

`Inferred:` a trigger created before its service agent is granted does not self-heal. `Untested:`
confirmed only by redeploying now that the identities exist — which is this session.

> 📌 **Why r2's tests could not have caught this.** Its 9/9 trigger suite ran against the **local
> emulator**, which does not exercise Eventarc at all. The one component that broke is the one
> component that suite structurally cannot see. Same family as
> `C:\Dev\JARVIS\kb\dev\look-at-your-own-output.md` **§4c** — a green headline over something that
> never ran. **Worth a KB candidate of its own:** *an emulator suite's coverage boundary is a
> claim about the instrument, and it belongs next to the green it produces.*

## Task

1. Get past the precondition (Ido).
2. Redeploy both projection triggers. **Same code — change nothing in `functions/src/`.** If a source
   change turns out to be needed, that is a different finding: stop and re-scope.
3. `FUNCTIONS_DISCOVERY_TIMEOUT=120` is required on this machine. The default 10 s times out during
   codebase discovery even though the code loads in 199 ms — a local tooling flake, not a code fault.

## Verify — and `Deploy complete!` is NOT the verification

That headline is precisely what hid this for two hours. Do all three:

1. **Tick a task in the app** (`Pixel_10_Pro_XL_B` was left running, signed in as
   `rachil751@gmail.com`; the app is installed).
2. `firebase functions:log --only projectPoints -n 5` → expect an **info line carrying
   `{uid, points, factCount}`**, which `projection.ts:91` logs on every run. **Absence of that line
   means it still does not fire**, regardless of what the deploy said.
3. **Dashboard shows non-zero `pts`.** It currently reads `0 pts` against **1** task done, so any
   non-zero value is the signal.

**If step 2 is still empty after a successful deploy** — that is when `gcloud` earns its install
(`50-finish` r3 deliberately did not install it, because diagnosis was complete and the remedy
identified; it is the right tool for the *next* question, not this one). Inspect the Eventarc
trigger state and the service agent's IAM bindings directly.

## Out of scope

- **#50 item 5 is DONE and pushed** (`941d6a8`, `310b6f8`). `ConnectivityMonitor` is deleted and the
  offline tick is verified by observation. Nothing here touches it.
- Anything in `app/` — this is a backend/infra unit.
- The `firebase-functions` "outdated version" warning the CLI prints. Real, unrelated, its own ticket.

## Exit

- The three verification steps above, each reported with what it actually returned.
- `CHANGELOG/2026-08-20/c20-eventarc-fix.md` (or the day you run it) · board row released · this
  brief closed to `sessions/done/` with `status: done` in the same commit · commit + push under
  AUTO MODE.
- **Flag the KB candidate** noted above about emulator coverage boundaries.

## 🚥 Hand-off line — mandatory

Per `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` §🚥. `9-duration-box` and `11-fill-buttons` do
**not** depend on this and can run before or after it; `7-quickadd-complete` **does** — its whole
spec is the points pipeline — so name that dependency explicitly.
