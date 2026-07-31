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

## Google Tasks import (spec §5, §6 nice-to-have)
File: `app/.../data/tasks/GoogleTasksClient.kt` (stub).
1. Add deps: `google-api-client-android`, `google-api-services-tasks`,
   `google-auth-library-oauth2-http`.
2. In `GoogleAuthClient`, request `TasksScopes.TASKS_READONLY` (a one-line
   `.requestScopes(...)`).
3. Build the Tasks service with `GoogleAccountCredential` from the signed-in
   account; list task lists/tasks.
4. Map to `domain.model.Task` with `source = GOOGLE_TASKS`; optionally run each
   through `RecommendationRepository.classifyTask` to auto-assign a goal.

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
