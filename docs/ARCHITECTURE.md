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
- **ui** holds the theme, reusable components (`GpCard`, `GpLinearProgress`, `ProgressRing`, `GoalCard`, `HorizontalBarChart`, `IconChip`, `HeroSurface`, `Avatar`), navigation, and the root auth gate.

## Theming — skins

`ui/theme/` is the single colour authority. **Material You dynamic colour is
deliberately off**: it let the device wallpaper override the brand palette, and
it cannot coexist with a user-chosen skin — two colour authorities cannot both
win.

- `domain/model/AppSkin` — the pure enum (`AURORA` default, `BLOSSOM`) plus its
  persisted id. No Compose types, same split as `GoalCategory.iconKey`.
- `ui/theme/Palettes.kt` — `colorSchemeFor(skin, dark)` returns one of four full
  Material 3 `ColorScheme`s; `accentsFor(...)` returns the off-Material brand
  accents (`GpAccents.heroGradient`, `positive`) exposed as `MaterialTheme.gpAccents`.
- `domain/repository/AppPreferencesRepository` → `data/prefs/…Impl` — the chosen
  skin, in `SharedPreferences`, as a hot `StateFlow` so the first frame already
  has the right palette. Device-local: it is *not* synced to Firestore.
- `MainActivity` field-injects that repository, because the skin must be known
  outside `GoalPilotTheme` and every `hiltViewModel()` lives inside it.

Surfaces invert the Material default on purpose: the page is a tinted canvas and
`GpCard` fills with `surfaceContainerLowest`, so cards read as objects lifted off
the background rather than darker holes cut into it.

`app/src/test/.../ui/ThemePaletteTest` asserts WCAG contrast across all four
schemes and the distinctness of the ten `GoalCategory` colours, so a palette edit
that breaks legibility fails the build rather than shipping.

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
users/{uid}                       UserDto  (points, email, displayName, friendCode, …) [private]
  goals/{goalId}                  GoalDto
    progress/{entryId}            ProgressDto  (value, note, imageUrl)
  tasks/{taskId}                  TaskDto  (goalId, points, done, completedAt)
  friends/{friendUid}             { addedAt }                                [private]

publicProfiles/{uid}              { displayName, photoUrl, points, level, friendCode } [world-readable]
shares/{shareId}                  SharedItemDto  (authorUid, period, message, imageUrl)
challenges/{challengeId}          Challenge  (owner, participants, standings) [nice-to-have]
```

- **Completing a task is a Firestore transaction** (`TaskRepositoryImpl.setDone`):
  it flips `done`, awards/rescinds points on `users/{uid}` **and** the
  `publicProfiles/{uid}` leaderboard projection, recomputes the level, and moves
  the linked goal's `currentValue` — all atomically.
- The **leaderboard** has two modes. "Everyone" reads `publicProfiles` ordered by
  points, capped at 100. "Friends" instead fetches the friends' profiles *by
  document id* (`whereIn` on `documentId()`, chunked at Firestore's 30-value cap)
  — filtering the global top-100 client-side would drop any friend outside it.
  Ranking happens after filtering, in the pure `List<LeaderboardEntry>.rankedByPoints()`.
- **Friend codes:** the raw uid is 28 characters and unusable in a live demo, so
  `AuthRepositoryImpl.ensureProfile` allocates a 6-character `FriendCode` on first
  sign-in (back-filled for older accounts) and mirrors it onto `publicProfiles`
  so `addFriendByCode` can resolve it. `addFriend` rejects uids with no public
  profile, so a mistyped code can never write a dangling friend edge.
- **Snapshot flows are uid-reactive.** Repositories build their `Flow`s on
  `FirebaseAuth.uidFlow()` (`data/auth/AuthExt.kt`) and `flatMapLatest` on it. A
  `Flow` is constructed when its ViewModel is created, so reading
  `auth.currentUser` once would pin whichever account was signed in at that
  moment — and serve user A's goals to user B when demoing sharing (spec §7).
- Security rules: `firestore.rules` / `storage.rules`. Private data is owner-only;
  `publicProfiles`/`shares` are readable by any signed-in user.

## LLM (GROQ) flow

```
DashboardViewModel → RecommendationRepository → FirebaseFunctions.callable("getRecommendations")
GoalDetailViewModel →       "                 →            "        .callable("scoreTask")
DashboardViewModel  →       "                 →            "        .callable("classifyTask")
                                                        │
                              functions/src/index.ts ──┘  → GROQ chat completions (JSON)
```
- Key lives only in `functions/.env` (spec §5). The model id is pinned in
  `functions/src/index.ts` and overridable via `GROQ_MODEL` — GROQ retires models
  on a rolling schedule, see [SETUP.md](SETUP.md#groq-model--check-before-you-demo).
- All three callables are surfaced in the UI: `getRecommendations` → the AI coach
  card, `scoreTask` → the ✨ button on the add-task row, `classifyTask` → the
  "Smart add a task" card on the dashboard (spec §6 Bonus).
- Every call **degrades gracefully**: on any error the client returns
  deterministic local guidance (`fallbackRecommendations`, `fallbackClassification`,
  `fallbackPoints`), so the UI never blocks or crashes (spec §8).
- The LLM **proposes, never writes**: Smart add always shows a confirmation
  dialog, and a `suggestedGoalId` that matches no real goal is discarded rather
  than trusted.

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
