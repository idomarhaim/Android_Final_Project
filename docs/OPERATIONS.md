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

The app is **feature-complete for spec §6 Core**, and every layer has been verified
against the real backend — not inferred from the UI.

| Area | State |
|---|---|
| Firebase backend | **Live.** Project `goalpilot-56e30`, Blaze plan |
| Firestore / Storage / Auth / rules | Deployed and exercised |
| Cloud Functions — callables | **six:** `getRecommendations`, `classifyTask`, `scoreTask`, `proposeMeasure`, `fileGoal`, `planGoal` — v2 callable, `us-central1`, `nodejs22` |
| Cloud Functions — triggers | **six:** `projectPoints`, `projectPointsOnTaskWrite`, `projectChallengeScore`, `projectChallengeScoreOnProgress` (`projection.ts`) · `applyMeasureChangeOnApproval`, `applyMeasureChangeOnProposal` (`challenges.ts`) — the **only** writers of derived state (§5.2) |
| LLM | GROQ `openai/gpt-oss-20b` by default; a user may bring an OpenAI / Anthropic / Gemini key (`#54`) |
| §6 Core | ✅ verified end-to-end |
| §6 Bonus — LLM classification | ✅ "Smart add a task" on the dashboard |
| §6 Bonus — life areas + time-allocation analytics | ✅ shipped, verified against the real Google Tasks account |
| §6 Nice-to-have — Google Tasks | ✅ shipped, verified on real Hebrew data |
| §6 Nice-to-have — Health Connect | ✅ shipped |
| §6 Nice-to-have — Challenges | ✅ **finished 2026-08-25** — `#23` closed. A typed `Measure` (`ChallengeType` and free-text `metricUnit` deleted), invite a friend, join links or creates a goal, server-owned scores, a measure change every participant must approve, **Health Connect as a first-class source**, and **retroactive** challenges that score a week already past |
| Beyond §6 | Calendar tab + recurrence, home-screen widget (translucent panel), notifications, guided tour, Settings surface, AI goal filing + work plan (`fileGoal` / `planGoal`), Hebrew/RTL |
| Shipped version | **v0.5.4, `versionCode` 15** — with both testers via App Distribution |
| Tests | JVM unit, instrumented, Firestore-rules (`firestore-tests/`) and functions (`functions/test/`) — all four layers green |

> **No test counts here on purpose.** This row read *"92 JVM unit + 12 instrumented"*
> for three weeks against a tree holding more than ten times that. A number in prose
> changes on every commit that adds a test and nobody makes a decision from it, so the
> layers are named and the digits are not. `gradlew :app:testDebugUnitTest` is the
> answer, and it is current by construction.

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

### ✅ Standing authorisation — Firebase actions that cost nothing

**Ido, 2026-08-21:** *"I already gave you authorisation to do any Firebase action that does not
require me to pay money — I want that written where it needs to be."*

This is the canonical record of that grant.

**And it is not an exception to the outward-action rule — it is that rule's own test, applied.**
`C:\Dev\JARVIS\rules\outward-action-governance.md` says outward autonomy is granted per task
and *"does not persist"*. Read carelessly, the grant below contradicts it. It does not, because
that rule states its own discriminator:

> *"Autonomy may persist where the blast radius is a repo; where it **reaches people**, it is
> re-granted per task or not at all."*

A deploy to **Ido's own Firebase project** reaches nobody. It is the same class as a push to
his own remote, which the global commit rule already says *"does not need `OUTWARD AUTO` and
never grants it"*. So the five sessions that stopped at this gate were not being careful — they
were applying a rule about **reaching people** to an action that does not, and `CLAUDE.md` said
in so many words that `outward-action-governance.md` *"is the gate"*. It was never the right
gate for a deploy. It remains exactly the right gate for the Firebase actions that **do** reach
people, which is why those are on the always-ask list below and not covered by anything here.

**Permitted without asking, in either mode:**

| Action | Why it is free |
|---|---|
| `firebase deploy --only functions` | deploying is not billed; Cloud Build and Artifact Registry usage for a project this size sits inside the free allowances |
| `firebase deploy --only firestore:rules` · `--only storage` | rules deploys are free outright |
| `firebase emulators:start`, `firestore-tests`, any local emulator run | never leaves the machine |
| `firebase functions:log`, `projects:list`, `functions:list`, any read | reads |

**Still always-ask, and none of these is a judgement call:**

- **anything that changes the billing plan, or *deliberately* enables a paid API** — that is
  the boundary the grant is drawn around, so a command that moves it cannot be covered by it.
  ⚠️ **Read that word `deliberately`, because without it this line cancels the grant.**
  `firebase deploy --only functions` prints, every single time:

  ```
  i  functions: ensuring required API run.googleapis.com is enabled...
  i  functions: ensuring required API eventarc.googleapis.com is enabled...
  i  functions: ensuring required API pubsub.googleapis.com is enabled...
  i  functions: ensuring required API storage.googleapis.com is enabled...
  ```

  A session reading the always-ask list literally would stop **at exactly the action this grant
  exists to permit** — a false stop, which is the failure being removed, arriving through the
  fix for it. `Observed:` in this project's own deploy log, 2026-08-21. Those four are the APIs
  v2 functions already run on; ensuring them is part of deploying, not a decision. What is
  always-ask is **turning on a paid service as the point of the command** — `gcloud services
  enable`, a console toggle, a new product;
- **creating a resource that bills by existing** rather than by use (a new region, a
  provisioned instance, a scheduled function that runs regardless of traffic);
- **deleting anything** — data, a function, a bucket, a rules ruleset. Deletions are
  always-ask on their own account, everywhere, and this grant does not touch that;
- **project settings, IAM, visibility, adding members**;
- **writing to production *data*** as an act rather than as a side effect of using the app.

**Permitted is not the same as *now*, and this grant does not decide sequencing.**
`Observed:` `CHANGELOG/2026-08-05/challenges-ui.md` — a `firestore.rules` deploy was held
**deliberately**, and the reason was not permission: *"it is paired with the two-account session
on purpose: that is the only sitting in which the deploy can actually be **proven** rather than
merely performed."* Under this grant alone a session would deploy immediately and lose the
verification. So: the grant removes the **asking**; it does not remove the judgement about
**when**, and it never overrides an explicit hold — Ido's, or a previous session's written one.
If a changelog or a brief says an action is being held for a reason, that reason still stands
and this page does not answer it.

**Honest limit, stated rather than smoothed over.** The project is on **Blaze** (v2 functions
require it), so *"costs nothing"* means *"inside the free allowance at this project's size"*,
not *"cannot be billed"*. A deploy pushes a container build; at one developer's cadence that is
free, and at a thousand deploys a day it would not be. If a session ever has reason to think a
Firebase action would leave the free tier, that is the always-ask list above, whatever this
table says.

**What this changed.** Before 2026-08-21 every deploy stopped and waited for Ido, including the
ones that only re-published code he had already approved — which is how `#55` shipped a client
that the deployed functions could not read (see that session's changelog §4.2). Capability was
never the gate; the asking was.

**How this wording was tested, since Ido waived the walkthrough.** It was run against **all five
recorded instances** of the failure it addresses — `challenges` (2026-08-04), `challenges-ui`
(2026-08-05), `social-share-bugs` (2026-08-15), `c13-key-store` (2026-08-20) and `#55`
(2026-08-21). It fires correctly on four. **On the fifth it fired where it should not have**,
which is what produced the sequencing clause above; the API-enablement clause came from the
same pass, reading a permitted command's own output. Neither was visible from the draft alone.
Account: `CHANGELOG/2026-08-21/55-scoring-model-r3.md`.

### OAuth consent screen — current state

```
Publishing status        In production    (unverified)   ← since 2026-08-09
User type                External
Test users               name.iddo@gmail.com, rachil751@gmail.com   (since 31/07;
                                                       irrelevant while in production)
```

**It was `Testing` until 2026-08-09**, read from the console that morning by the
`c9f-consent-screen-state` session and published the same day for
[#33](https://github.com/idomarhaim/Android_Final_Project/issues/33). The reason
matters more than the setting:

**In `Testing`, every authorization dies after seven days.** Verbatim —
*"authorizations by a test user will expire seven days from the time of consent."*
The clock is on the **grant**, not the access token, so `GoogleAuthUtil.getToken`
stops minting and Play Services throws `UserRecoverableAuthException`, which
`GoogleTasksClient` turns into `NeedsConsent(intent)` and the dashboard renders as an
ordinary "grant permission" button. **A weekly re-consent is indistinguishable from
normal first use**, so it had been happening silently for as long as the Tasks import
had shipped and nobody could have filed it by observation. Survivable for an import
you press; fatal for a calendar that is supposed to stay true while nobody is looking.

**In production, authorizations do not expire.** That is why this was changed.

**What production costs, tested on 2026-08-09, not assumed:** first consent on any
account now goes through *"Google hasn't verified this app"* → **Advanced** → **Go to
GoalPilot (unsafe)** — one extra tap, once per account — and the 100-new-user lifetime
counter for unverified production apps is now running. At an audience of two, noise.
If a clean demo screen is ever wanted (a course recording, say), switching back to
`Testing` restores the milder wording and is a 30-second job — at the price of the
clock returning. Full evidence:
[`docs/research/2026-08-09-oauth-production-test/`](research/2026-08-09-oauth-production-test/README.md).

**Publishing is reversible — and this took finding, so don't re-derive it.** Neither
[Manage app audience](https://support.google.com/cloud/answer/15549945) nor
[Submitting your app for verification](https://support.google.com/cloud/answer/13461325)
mentions the return trip at all, which reads like a one-way door. It isn't. The
statement lives on an unrelated page,
[Brand Approvals & Auto-Cancellations](https://support.google.com/cloud/answer/16868008):

> If you switched to Testing or Internal, when you switch back to In Production or
> External, public users will immediately be able to sign in and access the
> previously verified configuration.

What the round trip costs, from the same page: switching to Testing **auto-cancels a
pending verification request** but *"does not revoke your existing verification
status."* GoalPilot has neither, so both are free here. Two things Google does **not**
document, and neither should be assumed: whether the 100-new-user counter resets
(assume not — immaterial at two accounts), and whether a grant issued while in
production is re-clocked to seven days on the way back.

### Google APIs enabled for user data

```
tasks.googleapis.com            Google Tasks API      (since 31/07)
calendar-json.googleapis.com    Google Calendar API   (enabled 2026-08-08 by #33)
```

Calendar was **not** enabled until 2026-08-08. It is a separate toggle from Tasks;
missing it yields HTTP 403 `accessNotConfigured`, which does not read as a consent
problem. Re-enable elsewhere with:

```bash
gcloud services enable calendar-json.googleapis.com --project=goalpilot-56e30
```

### ⚠️ gcloud's default project is the WRONG one

`gcloud config` points at `neon-feat-461713-h9` ("My First Project"), a leftover.
**Pass `--project goalpilot-56e30` on every single gcloud/firebase command.** The
same trap exists in the Cloud console — its project picker defaults to "My First
Project", and an OAuth consent screen was once edited on the wrong project
because of it.

### Toolchain

- **The toolchain runs on JDK 21.** `gradle.properties` pins `org.gradle.java.home`
  to `C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot`, and that pin
  **overrides** `JAVA_HOME` — a build follows the pin whatever the environment says.
  *Observed 2026-08-19:* that is the only Adoptium JDK on this machine. If a build
  dies with "JAVA_HOME is set to an invalid directory", the pinned directory is
  missing — reinstall it (`winget install EclipseAdoptium.Temurin.21.JDK`) rather
  than repointing the pin (`0e52a66`). Tools that read `PATH` instead of `JAVA_HOME`
  — `firebase-tools` is one — can still disagree; see the JDK pitfall in
  [AGENTS.md](../AGENTS.md).
- `gcloud` and `firebase` CLIs are installed and authenticated as `name.iddo@gmail.com`.
- Emulator AVD `Pixel_10_Pro_XL` (API 37 / Android 17), a Google-APIs image — Play
  Services present, which Google Sign-In requires.
- Second AVD `Pixel_10_Pro_XL_B` (same image, leaner: 3 GB / 4 cores), added
  2026-08-05 so two accounts can be signed in at once. Boot it with
  `run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B`, or the *Run GoalPilot on Second
  Device* launcher. Details in [`scripts/README.md`](../scripts/README.md).

### ⚠️ Ido's real phone is routinely attached — pass `-s` on every `adb` call

`R5CY21NM30D` is **Ido's own Galaxy S25 Ultra**, and it is often plugged in alongside an AVD.
Two consequences, and the second is the dangerous one:

- A bare `adb` command fails with *more than one device/emulator*, which is merely annoying;
- a command that happens to resolve to one device can **reach his phone** — installing over the
  build he is actually using, or wiping a sign-in.

So **every** `adb` call names its target: `adb -s emulator-5554 …`, or `-s R5CY21NM30D` when the
phone is genuinely the target. `adb devices` first, always.

**It is also the only way to verify at his geometry.** His card width is **384 dp at font scale
1.15**; a render pass taken at AVD width shows nothing, which is how a button that renders one
letter per line on his screen passed every check here (see *Layout that does not assume a screen
width* in [ARCHITECTURE.md](ARCHITECTURE.md)). `scripts/mirror-phone.ps1` (and the *Mirror Phone*
launcher) puts the phone on screen; `scripts/README.md` §*Mirroring the phone* has the adb-version
trap it exists to avoid.

- **`connectedDebugAndroidTest` UNINSTALLS the app, and takes any Google sign-in with it.** Where
  a session needs both a signed-in device and the instrumented suite, use the data-preserving
  path instead — `adb -s <serial> install -r` for both the debug and the `androidTest` APK, then
  `adb shell am instrument -w <appId>.test/<runner>`. `Observed:` 2026-08-19, the full suite ran
  green and the app's Firebase auth store still held its user afterwards.

### Test accounts

```
name.iddo@gmail.com    project owner, signed in on the emulator, friend code NDXVJC
rachil751@gmail.com    second demo account, already an OAuth test user
```

---

## 3. What's left

> **This section deliberately does not list open work.** *(Decided 2026-08-24 by
> `docs-repair`; Ido delegated the choice — "pick the best solution for the system" — so
> the decision is **mine** and his to overturn.)*
>
> It used to, and by 2026-08-24 every item in it was false: Health Connect was called a
> stub after it shipped, challenges a preview screen after the repository landed, and a
> ⛔ box declared the whole §1.4/§1.5 points model absent from `HEAD` when `difficulty`,
> `completionFacts` and `goalEdges` were all in the code. **A second backlog beside the
> issue tracker drifts by construction** — the tracker is written to when work happens,
> this file only when somebody remembers.
>
> So the split is by **what rots**: the live list lives in
> [GitHub issues](https://github.com/idomarhaim/Android_Final_Project/issues), and what
> stays here is only what the tracker cannot hold — a judgement about submission, and a
> procedure. Neither changes when a ticket closes.

### The one thing that blocks submission

**Spec title page** still reads `[Full name & ID] · [Course number]`.
`GoalPilot_spec_EN.docx` is marked frozen in AGENTS.md — confirm with the user before
touching it.

### The two-account demo (spec §7) — a procedure, not a backlog item

Everything is in place; this is how to run it.

```powershell
.\scripts\run-goalpilot.ps1                              # emulator A, name.iddo@
.\scripts\run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B       # emulator B, alongside
```

Run them **one at a time** — the second waits for the first's Gradle build, and `-Avd`
guarantees it boots B rather than adopting A. Then sign in on B as
`rachil751@gmail.com`, add friend code `NDXVJC`, and exercise the leaderboard / friends
/ shared-feed flow with both screens visible.

Keep the two emulators on **different accounts**: they share the live Firebase project,
so two sessions of the same account produce writes attributable to nobody.

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

- The account must be on the **Test users** list. Being project Owner grants
  nothing. **This one was observed** — an `Error 403: access_denied` screen on
  31/07 disproved the "owners are implicitly allowed" theory
  ([`CHANGELOG/2026-08-01.md:252-259`](../CHANGELOG/2026-08-01.md#L252-L259)).
- ❌ **DISPROVEN 2026-08-09, by running it.** This file used to say *"Publishing
  status must be Testing — an unverified app in production returns `Error 403:
  access_denied` with **no override**."* Written as fact on 31/07, never tested,
  and repeated into two TODO files as a standing instruction. **It is false.** An
  unverified app in production shows *"Google hasn't verified this app"* with an
  **Advanced → Go to GoalPilot (unsafe)** override on the first screen, and
  `tasks.readonly` then works: a live import found 10 open tasks with no
  `UserRecoverableAuthException` and no 403. Screenshots and the full run:
  [`docs/research/2026-08-09-oauth-production-test/`](research/2026-08-09-oauth-production-test/README.md).
  The original was most likely a true fact about **restricted** scopes (Gmail,
  Drive) generalised to a **sensitive** one, which `tasks.readonly` is.
- ⚠️ **The granular consent checkbox arrives UNCHECKED.** On the *"Select what
  GoalPilot can access"* screen, `View your tasks` is off by default. Tap Continue
  without ticking it and sign-in **succeeds** while granting nothing — the Tasks
  import then has no permission, and it surfaces as an ordinary "grant permission"
  prompt rather than as "you declined this". Live on the shipped feature; observed
  2026-08-09. If an import ever returns nothing for an account that plainly has
  tasks, check this before checking the code.
- ⚠️ **`GoalPilot-297750736036` was not observed.** The claim below that Google
  appends the project number to unverified app names did not hold on any of the
  four consent screens captured 2026-08-09 — all four read plain `GoalPilot`.
  Left in place rather than deleted, because one run is weak evidence against it,
  but do not rely on it.
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

### The keyboard moves the layout, and the instrumented tests care too

The two IME bullets above are written for a **human** driving the emulator by
hand. The same physics reaches Compose tests, in a shape that produces no error
message at all, and that was issue
[#58](https://github.com/idomarhaim/Android_Final_Project/issues/58).

Focusing a text field raises the soft keyboard. On Android 11+ the keyboard
arrives as a **window inset animation**: the window is resized and everything in
it slides upward over a few hundred milliseconds. Compose's idling resource
tracks recompositions, its frame clock and pending measure/layout passes — a
*system* inset animation is none of those, so `waitForIdle()` returns while the
layout is still travelling. A `performClick` issued then reads its target's
bounds, injects a touch at their centre, and the target has moved by the time the
event lands. **The click is silently lost.**

- **In tests:** never call `performTextInput`, `performTextReplacement` or
  `performTextClearance` directly. Call the `…AndSettle` wrapper in
  `app/src/androidTest/.../ui/ImeSettling.kt`, which waits for the bounds to stop
  moving. `ImeSettleSweepTest` (JVM layer, free) fails the build if a raw one
  reappears.
- **Driving by hand:** nothing changed. The "tap twice" bullet above is a
  *different* mechanism — there the IME **window** consumes your tap; here the
  **layout** moves out from under an injected one. Same cause upstream (the
  keyboard), two distinct effects; don't reason from one to the other.

**No device or AVD setting is involved, deliberately.** Disabling the emulator's
soft keyboard was `#58`'s option 3 — `Untested:` never tried here, so whether it
works is unmeasured, and it persists on the AVD and silently changes the ground
under every other session sharing it. The wait needs nothing from the device, so
it holds on CI and for a human running `adb shell am instrument` by hand.

### Windows / Gradle

- Pipe Gradle through `tail` only with `${PIPESTATUS[0]}` — the pipe's exit code
  is `tail`'s, so failures read as success.
- ⚠️ **The same gate belongs on any build whose output you then *install*, and there the
  failure is worse than a misread exit code.** `gradlew assemble… | grep …` exits with
  **`grep`'s** status, so `&&` does not protect you — and **the previous APK is still sitting at
  the output path**, so `adb install -r` succeeds and the test run reports the *last* build's
  results. `Observed:` 2026-08-20 — a Kotlin compile error scrolled past inside a `grep` and the
  suite came back with the same 8 failures as the run before, which read as *"the fix did not
  work"*. It was never in the APK.
- **The artifact path is not evidence the artifact is current** — the general form of the same
  trap. Before uploading a release, check the APK rather than the clock:
  `aapt2 dump badging app/build/outputs/apk/release/app-release.apk` and read `versionCode` /
  `versionName` back. `Observed:` 2026-08-24, a stale `app-release.apk` from an earlier build sat
  at the output path while the real build was still running.
- KSP occasionally fails with "Could not delete/move …" file locks. Re-run, or
  `rm -rf app/build/generated/ksp`. Not a code error.

### ⚠️ The emulator's window-capture surface can wedge, and it looks exactly like a code regression

`Observed:` 2026-08-25. Six `EntranceAnimationUiTest` cases read *transparent* where they expect
red, and a render frame came back one flat colour — on code that had passed **331/331** an hour
before. Not the display (awake throughout), not the host window size (restoring it changed
nothing), not test ordering (it failed alone). **An emulator restart fixed it completely.**

If several pixel or screenshot tests go red **together**, restart the emulator before you read
your diff. The instinctive move — bisecting the change that "caused" it — costs an hour and finds
nothing, because nothing in the tree changed.

---

## 4a. Getting a build onto someone else's phone

See [`RELEASING.md`](RELEASING.md) — the release signing key, Firebase App
Distribution, the tag-triggered workflow, and the per-release checklist.

Two things from it are worth knowing even if you never cut a release:

- **The release signing key is permanent.** Once a tester installs a build,
  every later build must carry the same signature or Android refuses the update.
  `scripts/new-release-keystore.ps1` creates it; the `.jks` and its password are
  git-ignored and must be backed up off this machine.
- **`versionCode` is bumped by hand and forgetting is silent** — the build and
  the upload both succeed, and no tester is ever prompted.

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

## 7. Open items

**None as of 2026-08-25.** This section previously held one — `.github/copilot-instructions.md`
modified and `.github/instructions/mermaid.instructions.md` untracked, both from a JARVIS tooling
sync. Verified resolved: `git status .github/` is clean and no `mermaid.instructions.md` exists in
the tree.

> **Open *work* is deliberately not listed here**, for the reason §3 gives at length: a second
> backlog beside the issue tracker drifts by construction. The live list is
> [GitHub issues](https://github.com/idomarhaim/Android_Final_Project/issues). What belongs in
> this section is only a **loose end in the repo itself** — a dirty file, a half-applied sync, a
> credential that needs rotating — which no ticket would ever hold.
