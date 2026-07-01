# GoalPilot — Architecture

## Overview

GoalPilot is a single-module Android app in **Kotlin + Jetpack Compose (Material 3)**
following **MVVM with a clean-ish layering** and **Hilt** for dependency injection.
It uses **Firebase** (Auth, Firestore, Storage, Cloud Functions) for the backend and
**GROQ** (an LLM) for recommendations, proxied through a Cloud Function so the API
key never ships in the app.

```
┌────────────────────────────────────────────────────────────┐
│  feature/*   Compose screens + ViewModels (StateFlow<UiState>)│
├────────────────────────────────────────────────────────────┤
│  domain/     models · repository INTERFACES · use cases      │  ← no Android/Firebase
├────────────────────────────────────────────────────────────┤
│  data/       AuthRepositoryImpl · *RepositoryImpl (Firestore) │
│              StorageRepositoryImpl · RecommendationRepository  │
│              GoogleAuthClient · Health/Tasks stubs            │
├────────────────────────────────────────────────────────────┤
│  Firebase (Auth · Firestore · Storage · Functions) · GROQ    │
└────────────────────────────────────────────────────────────┘
        di/ (Hilt) binds interfaces → impls, provides Firebase singletons
```

### Layer rules
- **domain** depends on nothing Android/Firebase — trivially unit-testable (`BuildSummaryUseCase`, `Leveling`).
- **data** implements domain interfaces using Firebase; maps DTOs ↔ domain models.
- **feature** ViewModels depend only on domain interfaces (mockable in tests).
- **ui** holds the theme, reusable components (`ProgressRing`, `GoalCard`, `HorizontalBarChart`, `Avatar`), navigation, and the root auth gate.

## Navigation & auth gate

`MainActivity` → `GoalPilotRoot` observes `RootViewModel.authState`:
- `Loading` → spinner, `SignedOut` → `SignInScreen`, `SignedIn` → `MainScaffold`.

`MainScaffold` hosts a bottom bar (**Home / Goals / Social / Profile**) and a
`NavHost`. Signing in/out simply flips the auth-state flow, which re-routes the
whole tree — screens never navigate on auth changes themselves.

## State pattern

Each ViewModel exposes one immutable `StateFlow<...UiState>` built with
`combine(...).stateIn(viewModelScope, WhileSubscribed(5s), initial)`. Screens are
stateless: they `collectAsStateWithLifecycle()` and call ViewModel functions or
navigation lambdas. Repositories return cold `Flow`s from Firestore snapshot
listeners (`snapshotsFlow()`), and suspend functions return `Resource<T>`.

## Firestore data model

```
users/{uid}                       UserDto  (points, email, displayName, …)  [private]
  goals/{goalId}                  GoalDto
    progress/{entryId}            ProgressDto  (value, note, imageUrl)
  tasks/{taskId}                  TaskDto  (goalId, points, done, completedAt)
  friends/{friendUid}             { addedAt }                                [private]

publicProfiles/{uid}              { displayName, photoUrl, points, level }   [world-readable]
shares/{shareId}                  SharedItemDto  (authorUid, period, message, imageUrl)
challenges/{challengeId}          Challenge  (owner, participants, standings) [nice-to-have]
```

- **Completing a task is a Firestore transaction** (`TaskRepositoryImpl.setDone`):
  it flips `done`, awards/rescinds points on `users/{uid}` **and** the
  `publicProfiles/{uid}` leaderboard projection, recomputes the level, and moves
  the linked goal's `currentValue` — all atomically.
- The **leaderboard** reads `publicProfiles` ordered by points; **friends-only**
  filters client-side against `users/{uid}/friends`.
- Security rules: `firestore.rules` / `storage.rules`. Private data is owner-only;
  `publicProfiles`/`shares` are readable by any signed-in user.

## LLM (GROQ) flow

```
DashboardViewModel → RecommendationRepository → FirebaseFunctions.callable("getRecommendations")
                                                        │
                              functions/src/index.ts ──┘  → GROQ chat completions (JSON)
```
- Key lives only in `functions/.env` (spec §5).
- Every call **degrades gracefully**: on any error the client returns
  deterministic local guidance (`fallbackRecommendations` / `fallbackClassification`),
  so the UI never blocks or crashes (spec §8).

## Gamification

`Leveling` uses a quadratic curve `pointsForLevel(n) = 50·(n-1)·n`
(L1:0, L2:100, L3:300, …). `User.level`, `levelProgress`, `pointsToNextLevel` are
derived; the dashboard/profile show a level bar; the `ProgressRing` and bar charts
animate.

## <a name="emulator"></a>Using the Firebase Emulator (optional, debug only)

To run without a live project, point the SDKs at the emulator in `FirebaseModule`
(guard with `BuildConfig.DEBUG`):

```kotlin
if (BuildConfig.DEBUG) {
    auth.useEmulator("10.0.2.2", 9099)
    firestore.useEmulator("10.0.2.2", 8080)
    storage.useEmulator("10.0.2.2", 9199)
    functions.useEmulator("10.0.2.2", 5001)
}
```
`10.0.2.2` is the host loopback from the Android emulator. Then
`firebase emulators:start`.

## Known limitations (course scope)

- Points/level are written client-side (transaction) — a determined user could
  edit `publicProfiles`. Production would compute them in a Cloud Function trigger.
- Deleting a goal doesn't cascade-delete its `tasks`/`progress` subcollections
  (Firestore has no server-side cascade); acceptable for the demo.
- Health Connect & Google Tasks are compiling stubs — see [TODO/](../TODO/TODO.md).
