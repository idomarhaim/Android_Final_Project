# Integrations — OPTIONAL backlog (nice-to-have / bonus)

These tiers are **architected and scaffolded** (models, DI, stubs, navigation) but
not fully implemented, to keep the Core build rock-solid. Each item lists exactly
how to activate it.

## ~~Health Connect — fitness & sleep (spec §5, §6 nice-to-have)~~ — DONE 2026-08-02, automatic since 2026-08-05
Reads the last seven days of steps and sleep and files each day against a fitness
or sleep goal. Originally a "Sync health data" button with a review sheet; since
2026-08-05 it runs **on every app foreground**, at most once per fifteen minutes,
and writes everything unsynced without asking. See `CHANGELOG/2026-08-02.md` and
`CHANGELOG/2026-08-05/health-autosync.md`.

**Read-only by design** — only `READ_STEPS` / `READ_SLEEP` are declared, so the
user is never asked for write access and GoalPilot can never modify the device's
health store.

Implementation notes worth keeping:
- **The client version is pinned by the toolchain, not by preference.**
  `connect-client` is on `1.1.0-beta01`; stable `1.1.0` and every `1.1.0-rc*`
  require **compileSdk 36 + AGP 8.9.1**, and this project is on compileSdk 35 /
  AGP 8.7.3 / Gradle 8.10.2. Don't "upgrade to stable" without doing the
  toolchain bump first — `checkDebugAarMetadata` fails immediately. There is no
  stable `1.0.0`; that line ends at `1.0.0-alpha11`.
- **Sleep is filed under the day the user woke up.** A session crossing midnight
  has two candidate dates; waking day is what "last night" means.
- **Overlapping sleep sessions are merged, not summed.** Health Connect
  aggregates every app on the device, so a watch plus a sleep tracker reports the
  same night twice — summing claims sixteen hours.
- **Steps use `aggregateGroupByPeriod`, not `readRecords`.** The provider
  de-duplicates across source apps during aggregation; raw reads do not.
- `ProgressEntry.sourceKey` (e.g. `hc:steps:2026-08-01`) makes re-sync dedupe
  **exact** — steps are cumulative, so logging a day twice silently doubles it.
  Since the sync became automatic that key is no longer unique per entry: a day
  can carry several entries, and their **sum** is what it has been credited. That
  is what lets today be topped up by the difference instead of being frozen at
  the first reading of the morning, which is what "skip any day already seen"
  would have done once the app started syncing every time it opened.
- **The throttle stamp is per uid and lives in SharedPreferences.** In memory it
  would reset on every cold start, which is exactly when the sync fires — the
  fifteen-minute window would then never apply.
- The rationale activity is **mandatory**: Health Connect refuses permissions to
  an app that handles neither `ACTION_SHOW_PERMISSIONS_RATIONALE` nor, on 14+,
  the `VIEW_PERMISSION_USAGE` alias.

**Still worth doing:** verify a sync on a **physical phone with real step data**.
The emulator carries the Health Connect apex but its store is empty, so the
proposal → Firestore write path has never run against real readings.

## ~~Google Tasks import (spec §5, §6 nice-to-have)~~ — DONE 2026-07-31
Shipped as the **"Import from Google Tasks"** card on the dashboard. Verified
end-to-end against a real account: 7 Hebrew tasks imported, each classified into
a correctly-categorised goal, one of them matched onto an *existing* goal rather
than duplicating it.

**The plan above was deliberately not followed.** It called for three Google API
libraries; none were added. `GoogleAuthUtil` (already present in
`play-services-auth`) mints an access token from the existing sign-in, and two
`HttpURLConnection` GETs hit the Tasks REST API directly, parsed with the
`kotlinx.serialization` already on the classpath. Several megabytes and a second
auth stack, avoided, for what is two HTTP calls.

Implementation notes worth keeping:
- `GoogleAuthClient` requests `tasks.readonly` at sign-in, but accounts that
  signed in *before* the scope existed hit `UserRecoverableAuthException`.
  `TasksImportResult.NeedsConsent` carries Google's own consent `Intent` up to
  the screen, which launches it and retries — no sign-out required.
- Import is **capped at 15 tasks per run**: each costs a `classifyTask` call and
  GROQ's free tier allows 30 requests/minute.
- Re-import is **deduped by title** against existing tasks. Firestore tasks have
  no external id, so title is the only handle — storing `externalId` on `Task`
  would make this exact.
- Titles are **clamped to 120 characters**. Google Tasks titles are unbounded and
  people paste whole messages into them; an unclamped one broke the review
  dialog layout during testing and would have become a giant goal name.

**Requires** the `tasks.readonly` scope on the OAuth consent screen, and — while the
project was in `Testing` — the account listed under Test users.

> ⚠️ **This line used to end** *"and publishing status **Testing** — an unverified app
> in production hard-blocks sensitive scopes with no override."* **That was never tested
> and it is false**, disproven 09/08/2026 by [#33](https://github.com/idomarhaim/Android_Final_Project/issues/33):
> production shows a warning with an `Advanced` override, and the import then works.
> The project has been **in production** since, so the Test users list no longer gates
> anything. Evidence: [`docs/research/2026-08-09-oauth-production-test/`](../../docs/research/2026-08-09-oauth-production-test/README.md).
>
> One live trap remains, and it is not about publishing status: the consent screen's
> `View your tasks` checkbox **arrives unchecked**. Sign-in succeeds without it and the
> import silently has no permission.

## ~~Competitive challenges (spec §6 nice-to-have, §7)~~ — DONE 05/08/2026
Shipped as a real **Challenges** screen, reached from both Social and Profile:
your challenges with live standings, a Discover list, join / leave, score
reporting, and a create flow. Built over two sessions — see
`CHANGELOG/2026-08-04/challenges.md` (rules + domain + data) and
`CHANGELOG/2026-08-05/challenges-ui.md` (ViewModel + screen).

**The plan above was deliberately not followed on point 3.** Standings are
computed **client-side**, not in a Cloud Function: a function would have put that
session in `functions/src/index.ts`, which the concurrent `time-insights` session
also needed, and that is the one file two sessions cannot share. Moving them
server-side is covered by the existing anti-cheat item in TODO → FUTURE, which
should move points and scores together.

Implementation notes worth keeping:
- **Participation is not a field on `Challenge`.** The rules allow writes to the
  challenge document only to its owner, so a `participantUids` array or a
  `standings` list stored there would deny every join. Participants are one
  self-owned document each under `challenges/{id}/participants/{uid}`, with a
  mirror edge at `users/{uid}/challenges/{id}` for "my challenges". The KDoc says
  so; do not re-add the fields.
- **A subcollection is not covered by its parent's `match`** — that is precisely
  what makes the participants subcollection the way to let a non-owner join
  something they cannot edit.
- **Standings use standard competition ranking** (joint ranks, 1-1-3), unlike the
  leaderboard's `rankedByPoints`. On a leaderboard an arbitrary tiebreak is
  cosmetic; on the thing people compete over it is wrong.
- **`endAt` is an exclusive bound**, so a challenge running *through* the chosen
  day ends at the **following** local midnight — storing the chosen day's own
  midnight ends a one-day challenge before it begins.
- **The Material date picker returns UTC midnight**, which is the previous
  calendar day in any zone west of Greenwich. `ChallengeDates.kt` extracts the day
  in UTC and re-anchors it to local midnight; both conversions are JVM-tested.
- **`reportScore` replaces the total, it does not add to it** — the dialog says
  so, because guessing wrong is a competitor's whole standing.
- Known limitations, deliberate and unchanged: deleting a challenge orphans its
  participant rows (Firestore does not cascade), and there is no "kick a
  participant" — granting the owner that power needs a `get()` on the parent
  inside the rule, billing a document read on every evaluation.

**~~Still to do~~ — both done 05/08/2026** in the `submission` session, in one
sitting as intended. See `CHANGELOG/2026-08-05/submission.md`.
1. [x] **Deploy the rules** — `firebase deploy --only firestore:rules` ran
   against live `goalpilot-56e30`. Release `cloud.firestore` moved from ruleset
   `8f80b66d-0970-4f72-80b1-59dcbd37ff80` (31/07) to
   `d38c7248-f5c4-464a-b01b-d7edba01ce6b`, and the deployed copy was read back
   over the Firebase Rules API to confirm it carries the `participants` block.
   The delta was purely additive; the 16 `firestore-tests` cases were re-run
   against that exact file first.
2. [x] **Verify a *non-owner* join end-to-end.** Account A created "August Steps
   Race" on `Pixel_10_Pro_XL`; account B joined it from `Pixel_10_Pro_XL_B`,
   reported a score of 8200, and A's screen re-ranked itself to #2 with no
   interaction. Each step confirmed in Firestore over REST.

   Worth keeping: the **owner auto-join is itself proof the deploy landed** —
   it writes `challenges/{id}/participants/{ownerUid}`, which under the old
   ruleset matched no rule at all, since a subcollection is not covered by the
   parent `match`.

## ~~LLM task→goal classification action (spec §6 bonus)~~ — DONE 2026-07-31
Shipped as the **"Smart add a task"** card on the dashboard
(`DashboardViewModel.classifyForSmartAdd` / `confirmSmartAdd`, `SmartAddCard` +
`SmartAddDialog` in `DashboardScreen`). Type a task in plain language → the
`classifyTask` Cloud Function picks the goal (or proposes a new one) and
estimates points → the user confirms before anything is written.

`scoreTask` is likewise no longer dead code: the ✨ button on the add-task row in
`GoalDetailScreen` calls `RecommendationRepository.scoreTask` and fills the
points field.

Both degrade to local heuristics when the function or key is unavailable (spec §8).
