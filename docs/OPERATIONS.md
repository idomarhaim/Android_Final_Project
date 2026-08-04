# Operating notes — GoalPilot

**Written 2026-07-31**, after the session that took this project from placeholder
config to a live, verified backend. Read [AGENTS.md](../AGENTS.md) first; this
file covers only what a *new session* needs that the other docs don't say.

> Not a `/handoff` document. `/handoff` writes session context to
> `%TEMP%\handoffs\` and is deliberately *not* committed
> (`C:\Dev\JARVIS\skills\handoff\SKILL.md`). This file is the opposite: durable
> project facts that belong in the repo, which is exactly where a handoff is
> required to have put them before a session split
> (`C:\Dev\JARVIS\rules\memory-promotion.md`).

---

## 1. Where the project actually is

The app is **feature-complete for spec §6 Core**, and every layer has been
verified against the real backend — not inferred from the UI.

| Area | State |
|---|---|
| Firebase backend | **Live.** Project `goalpilot-56e30`, Blaze plan |
| Firestore / Storage / Auth / rules | Deployed and exercised |
| Cloud Functions | `getRecommendations`, `classifyTask`, `scoreTask` — v2 callable, `us-central1`, `nodejs22` |
| GROQ | `openai/gpt-oss-20b`, key in git-ignored `functions/.env` |
| §6 Core | ✅ verified end-to-end |
| §6 Bonus — LLM classification | ✅ "Smart add a task" on the dashboard |
| §6 Bonus — life areas + time-allocation analytics | ✅ shipped 2026-08-03, verified on the emulator against the real Google Tasks account |
| §6 Nice-to-have — Google Tasks | ✅ shipped, verified on real Hebrew data |
| §6 Nice-to-have — Health Connect | ✅ shipped 2026-08-02 (this row said "compiling stub" until 2026-08-03; the emulator carries real synced step entries) |
| §6 Nice-to-have — Challenges | ⬜ preview screen with sample data |
| Tests | 92 JVM unit + 12 instrumented, all passing (2026-08-03) |

Full detail is in [`CHANGELOG/2026-07-31.md`](../CHANGELOG/2026-07-31.md) — it is
long, but it is the record of what was done and *why*.

---

## 2. Environment facts you will need

**These are not guessable and will waste your time if you don't know them.**

### Project identifiers

```
Firebase / GCP project   goalpilot-56e30      (project number 297750736036)
Android app (release)    1:297750736036:android:b5d15ee73b43a6fa058a31
Android app (debug)      1:297750736036:android:f428d22e3d58bdf5058a31
Storage bucket           goalpilot-56e30.firebasestorage.app
Region (everything)      us-central1
Debug SHA-1              F1:D0:96:4D:54:41:D5:99:86:7D:AE:83:0F:77:16:23:BB:64:DB:3F
```

### ⚠️ gcloud's default project is the WRONG one

`gcloud config` points at `neon-feat-461713-h9` ("My First Project"), a leftover.
**Pass `--project goalpilot-56e30` on every single gcloud/firebase command.** The
same trap exists in the Cloud console — its project picker defaults to "My First
Project", and an OAuth consent screen was once edited on the wrong project
because of it.

### Toolchain

- `JAVA_HOME` is set at **User** level to `jdk-21.0.11.10-hotspot`. The Machine-level
  value points at JDK 25, which AGP rejects; User scope overrides it. If a build
  dies with "JAVA_HOME is set to an invalid directory", that override is missing.
- `gcloud` and `firebase` CLIs are installed and authenticated as `name.iddo@gmail.com`.
- Emulator AVD `Pixel_10_Pro_XL` (API 37 / Android 17), a Google-APIs image — Play
  Services present, which Google Sign-In requires.
- Second AVD `Pixel_10_Pro_XL_B` (same image, leaner: 3 GB / 4 cores), added
  2026-08-05 so two accounts can be signed in at once. Boot it with
  `run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B`, or the *Run GoalPilot on Second
  Device* launcher. Details in [`scripts/README.md`](../scripts/README.md).

### Test accounts

```
name.iddo@gmail.com    project owner, signed in on the emulator, friend code NDXVJC
rachil751@gmail.com    second demo account, already an OAuth test user
```

---

## 3. What's left

### MUST (blocks submission)

1. **Two-account sharing demo (spec §7).** Everything is in place — both accounts
   are OAuth test users, and since 2026-08-05 the second device exists too:

   ```powershell
   .\scripts\run-goalpilot.ps1                              # emulator A, name.iddo@
   .\scripts\run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B       # emulator B, alongside
   ```

   Run them **one at a time** — the second waits for the first's Gradle build, and
   `-Avd` guarantees it boots B rather than adopting A. Then sign in on B as
   `rachil751@gmail.com`, add friend code `NDXVJC`, and exercise the leaderboard /
   friends / shared-feed flow with both screens visible.

   Keep the two emulators on **different accounts**: they share the live Firebase
   project, so two sessions of the same account produce writes attributable to
   nobody.
2. **Spec title page** still reads `[Full name & ID] · [Course number]`.
   `GoalPilot_spec_EN.docx` is marked frozen in AGENTS.md — confirm with the user
   before touching it.

### OPTIONAL — the two remaining §6 features

See [`TODO/TODO_OPTIONAL/Integrations.TODO.optional.md`](../TODO/TODO_OPTIONAL/Integrations.TODO.optional.md)
for both.

**Health Connect** — `data/health/HealthConnectManager.kt` is a stub. Needs the
`androidx.health.connect:connect-client` dependency, manifest permission
declarations plus the Health Connect `<queries>` entry and a
`PermissionsRationaleActivity`, then reads of `StepsRecord` / `SleepSessionRecord`
feeding `ProgressRepository`. No account or API key required. Built into Android
14+; older devices install it from Play.

> ⚠️ **Re-evaluate that TODO's dependency list before following it.** The Google
> Tasks entry told you to add three Google API libraries; none were needed — the
> whole feature turned out to be two REST calls using a token from
> `GoogleAuthUtil`, which `play-services-auth` already provides. Apply the same
> scrutiny here. Health Connect genuinely does need its client library, but check
> what else the plan assumes.

**Competitive challenges** — `domain/model/Challenge.kt` and the `challenges`
Firestore rules exist; `feature/challenges/ChallengesScreen.kt` renders sample
data. Needs a `ChallengeRepository` (interface + Firestore impl) for create/join
and standings, and the preview screen swapped onto live data. Standings are best
computed server-side.

---

## 4. Traps discovered the hard way

Each of these cost real time in the previous session.

### Verifying the LLM features

`RecommendationRepositoryImpl` **swallows every GROQ failure into a deterministic
local fallback** (spec §8, deliberate). A dead API and a working one look nearly
identical on screen. To prove a call really reached the model, compare against
what the fallback can produce:

- `getRecommendations` fallback only ever emits *"Start with one goal"*, *"Keep
  the streak alive"*, or *"Nudge: {goal}"*. Anything else came from GROQ.
- `scoreTask` has **two** fallback signatures, and knowing only the first is how a
  dead model gets reported as a working one:
  1. **Client-side**, when the call never left the device: `5 + 3×words` points
     clamped 5..50, and `3 × points` minutes.
  2. **Server-side**, when the call reached the function but GROQ did not answer:
     a flat **`10 points / 30 minutes`** from the `catch` in
     `functions/src/index.ts`. Note `5 + 3×words` can never equal 10, so this pair
     is *unreachable* by rule 1 and looks like a perfectly ordinary estimate.

  `TaskScoring.looksLikeFallback` encodes both; the duration back-fill uses it to
  keep either from being written as an AI estimate. If the `catch` in
  `functions/src/index.ts` ever returns different numbers, that constant must
  change with it.

Never report an LLM feature as working based on the UI alone.

### GROQ rate limits

Free tier is **30 requests/minute**. Any feature that fans out one LLM call per
item must be capped — the Google Tasks import caps at 15 per run for this reason.
Exceeding it doesn't error visibly; the calls just fall back.

That is also why `classifyTask` and `scoreTask` return the task's **duration**
alongside its points instead of a second callable: one sentence, one call.

### Verifying the time-allocation chart

The pie is built from `completed task → goal → life area`, weighted by minutes, so
an empty chart usually means one of the links is missing rather than the chart
being broken. In order: is anything **completed inside the selected window**
(a calendar day/week/month/quarter/year, not a rolling one)? do those tasks have a
**goal**? does that goal have a **life area**? A 100 % "Unassigned" slice is the
chart telling you the third link is missing — the card's "Fix" button jumps
straight to the screen that repairs it.

### OAuth and sensitive scopes

Plain sign-in uses `email`/`profile` (non-sensitive) and Just Works. Anything
sensitive — `tasks.readonly`, and Health Connect data if you route it through
Google — plays by different rules:

- Publishing status **must be Testing**. An unverified app *in production*
  returns `Error 403: access_denied` with **no override**.
- The account must be on the **Test users** list. Being project Owner grants
  nothing.
- **"Ineligible accounts not added" means the address is already on the list** —
  a duplicate rejection, not a permissions failure.
- Google appends the project number to unverified app names on the consent screen
  (`GoalPilot-297750736036`). That is anti-impersonation, not misconfiguration,
  and can't be removed without verification.

### Firestore layout

Progress entries are nested **under the goal**:
`users/{uid}/goals/{goalId}/progress/{id}` — not `users/{uid}/progress`. Querying
the wrong path returns empty and looks like a write failure.

### Provisioning

`gcloud storage buckets create` **cannot** create the Firebase default bucket —
it 403s because Google owns `.firebasestorage.app`. Use the Firebase Storage API:
`POST https://firebasestorage.googleapis.com/v1beta/projects/{project}/defaultBucket`
with `{"location":"us-central1"}` — the call the console's "Get started" makes.

### Driving the emulator with adb

- `adb shell input text` is **ASCII-only**. The app handles Hebrew perfectly; the
  tooling doesn't. Use English strings for automation.
- The **first tap outside a focused text field is consumed dismissing the IME**.
  Action buttons need tapping twice.
- The emulator's floating IME panel overlaps the **left edge** and can cover
  controls (e.g. task checkboxes). `adb shell ime disable <id>` clears it —
  **re-enable it afterwards**.
- `adb push` to `/sdcard/...` from Git Bash needs `MSYS_NO_PATHCONV=1`, or the
  path is rewritten to `C:/Program Files/Git/sdcard/...`.

### Windows / Gradle

- Pipe Gradle through `tail` only with `${PIPESTATUS[0]}` — the pipe's exit code
  is `tail`'s, so failures read as success.
- KSP occasionally fails with "Could not delete/move …" file locks. Re-run, or
  `rm -rf app/build/generated/ksp`. Not a code error.

---

## 5. Useful verification commands

```bash
# Firestore documents (read-only, no SDK needed)
T=$(gcloud auth print-access-token)
curl -s -H "Authorization: Bearer $T" \
  "https://firestore.googleapis.com/v1/projects/goalpilot-56e30/databases/(default)/documents/users"

# Did a callable function actually run?
firebase functions:log --only getRecommendations --project goalpilot-56e30

# What's in Storage?
gcloud storage ls -r gs://goalpilot-56e30.firebasestorage.app/

# Is the Google sign-in provider enabled?
curl -s -H "Authorization: Bearer $T" -H "x-goog-user-project: goalpilot-56e30" \
  "https://identitytoolkit.googleapis.com/admin/v2/projects/goalpilot-56e30/defaultSupportedIdpConfigs"
```

---

## 6. Working agreements

- **Normal (interactive) mode unless the user's message starts with `AUTO MODE`.**
  Ask `Commit this? — <one-line summary>` and wait.
- **CHANGELOG first, then commit** using that entry's text as the message —
  copy-paste, don't rewrite.
- **Never push without explicit OK**, and never to `main` without confirmation.
- **Wait for user confirmation before flipping a TODO `[ ]` → `[x]`.**
- Reply in **English** even when the user writes Hebrew.
- End every task wrap-up with three lists: files read, files edited, skills used.

---

## 7. Open item not yet resolved

`.github/copilot-instructions.md` has an uncommitted modification and
`.github/instructions/mermaid.instructions.md` is untracked — both from a JARVIS
tooling sync, unrelated to the app. They were deliberately left out of every
commit this session. Decide with the user whether they belong in their own commit.
