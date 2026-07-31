# Integrations — OPTIONAL backlog (nice-to-have / bonus)

These tiers are **architected and scaffolded** (models, DI, stubs, navigation) but
not fully implemented, to keep the Core build rock-solid. Each item lists exactly
how to activate it.

## Health Connect — fitness & sleep (spec §5, §6 nice-to-have)
File: `app/.../data/health/HealthConnectManager.kt` (stub).
1. Add dep: `androidx.health.connect:connect-client:<latest stable/rc>` to
   `gradle/libs.versions.toml` + `app/build.gradle.kts`.
2. Manifest: declare health permissions, the Health Connect `<queries>` entry,
   and a `PermissionsRationaleActivity`.
3. Replace the stub with `HealthConnectClient.getOrCreate(context)` reads
   (`StepsRecord`, `SleepSessionRecord`) over a time range.
4. Feed results into `ProgressRepository` (e.g. auto-log a "Steps"/"Sleep" goal).

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

**Requires** the `tasks.readonly` scope on the OAuth consent screen, the account
listed under Test users, and publishing status **Testing** — an unverified app
*in production* hard-blocks sensitive scopes with no override.

## Competitive challenges (spec §6 nice-to-have, §7)
Files: `domain/model/Challenge.kt`, `feature/challenges/ChallengesScreen.kt`
(preview), `challenges` Firestore rules.
1. Add `ChallengeRepository` (interface + Firestore impl) for create/join and
   standings.
2. Replace the preview screen's sample data with live challenges; add a create flow.
3. Compute standings from tasks/Health Connect metrics (Cloud Function recommended).

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
