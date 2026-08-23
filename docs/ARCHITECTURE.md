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
- **ui** holds the theme, reusable components (`GpCard`, `GpLinearProgress`, `ProgressRing`, `GoalCard`, `HorizontalBarChart`, `DonutChart`, `IconChip`, `HeroSurface`, `Avatar`), navigation, and the root auth gate.

## Life areas and the time-allocation chart

The user's goals hang off **life areas** — their own division of their life
("בריאות", "לימודים", "קריירה", …), not the fixed `GoalCategory` taxonomy. A goal
carries both: `category` drives colour, icon and the LLM's context; `lifeAreaId`
answers *which part of my life is this*, and is the unit the analytics pie reports
on.

```
completed Task ──(goalId)──▶ Goal ──(lifeAreaId)──▶ LifeArea
       │                                              │
       └── estimatedMinutes (LLM) ────────────────────┴──▶ slice of the pie
```

- `domain/model/LifeArea.kt` — the model plus `LifeAreaPalette` (ten categorical
  hues, twelve icon keys, and a **bilingual** name→icon guesser, because the
  Google Tasks lists it names areas after are in Hebrew).
- `domain/usecase/BuildLifeAreaProposalsUseCase` — Google Tasks lists → reviewable
  proposals (create / link / already-synced), pure and unit-tested.
- `domain/usecase/ReorderLifeAreasUseCase` — a drag `(from, to)` → the **minimal**
  `id → sortOrder` map. Positions outside the moved span keep their existing
  values, so one drag writes the span it crossed and not the collection. It falls
  back to renumbering 0..n-1 only when the existing values are not strictly
  increasing, because rotating values through a tie would land the card wherever
  the `(sortOrder, name)` tie-break decided rather than where it was dropped.
- `domain/usecase/GroupGoalsByLifeAreaUseCase` — goals + areas → the bands the
  goals list is grouped into. It is what stops a header appearing where it says
  nothing: empty areas get no band, unfiled goes last, and a user with no areas at
  all gets one nameless band (the flat list, as before) rather than a lone
  "No life area" header over everything.
- `domain/usecase/TimeAllocationUseCase` — the chain above, over a window, pure
  and unit-tested. Unresolvable links (no goal, no area, deleted area) fall into
  one honest "Unassigned" slice rather than being dropped.
- `core/util/AnalyticsRange` — day / week / month / quarter / year, **calendar
  aligned** (not rolling like `SummaryPeriod`) and locale-aware about which day the
  week starts on.
- `feature/lifeareas/` — define, edit, delete, reorder and sync areas; file loose
  goals. `LifeAreaRows.kt` holds the drag-to-reorder state holder and the rows as a
  `LazyListScope` extension, so they drop into the screen's existing list rather
  than nesting a second scrollable, and can be driven by a UI test with no
  Firebase in the room.
- `feature/goals/` — the goals list, **banded by life area**. Headers rather than a
  chip per card: the card's colour and meta line already belong to the goal's
  *category*, and a second differently-coloured token per row read as noise.
- `feature/analytics/` — the range picker, the interactive `DonutChart`, and the
  two goal-level bar charts.

**One order, three screens.** `sortOrder` is the user's own ordering and the
repository sorts by `(sortOrder, name)` client-side — a composite index for a
collection holding a handful of documents would be ceremony. Because the goals
list and the analytics chart both consume `observeLifeAreas`, dragging an area on
the life-areas screen reorders the goal bands too, with no write to the goals
collection at all.

**Durations.** Every completed task contributes minutes: the LLM's estimate when
there is one (`classifyTask` / `scoreTask` return `estimatedMinutes`), otherwise
`TaskDuration.fallbackMinutes(points)` — 3 minutes per difficulty point. One
function, `TaskDuration.minutesOf`, decides for the whole app, and the analytics
card states how many of the window's durations were the model's rather than
implying they all were.

**Chart animation.** `ui/components/ChartAnimation.kt` is the shared entry
animation. `animateFloatAsState` cannot do this job: it initialises *at* its
target, so a value that never changes never animates. An `Animatable` explicitly
started at zero is what makes bars grow and the donut sweep out of 12 o'clock.

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

`MainScaffold` hosts a bottom bar (**Home / Goals / Calendar / Social**) and a
`NavHost`. Signing in/out simply flips the auth-state flow, which re-routes the
whole tree — screens never navigate on auth changes themselves.

⚠️ **This line read *Home / Goals / Social / Profile* until 2026-08-24, and both
halves of that were wrong.** `#60` moved **Profile** off the bar to an avatar in Home's
top-right (five is a crowded bar) and gave the freed tab to **Calendar**. `Routes.PROFILE`
is still registered and must stay — the avatar sheet reaches it.

## State pattern

Each ViewModel exposes one immutable `StateFlow<...UiState>` built with
`combine(...).stateIn(viewModelScope, WhileSubscribed(5s), initial)`. Screens are
stateless: they `collectAsStateWithLifecycle()` and call ViewModel functions or
navigation lambdas. Repositories return cold `Flow`s from Firestore snapshot
listeners (`snapshotsFlow()`), and suspend functions return `Resource<T>`.

## Firestore data model

```
users/{uid}                       UserDto  (points, email, displayName, friendCode, …) [private]
  goals/{goalId}                  GoalDto  (…, lifeAreaId → lifeAreas/{id})
    progress/{entryId}            ProgressDto  (value, note, imageUrl)
  tasks/{taskId}                  TaskDto  (goalId, points, done, completedAt, estimatedMinutes)
  completionFacts/{taskId}        TaskCompletion  — the banked completion (§1.4, #55)
  occurrences/{id}                Occurrence  (taskId, when) — only the instances touched (§7.1, #63)
  lifeAreas/{areaId}              LifeAreaDto  (name, colorHex, iconKey, sortOrder, googleListId)
  summaries/{summaryId}           ProgressSummary
  challengeReports/{challengeId}  what the participant measured — private, owner-written (§5.2)
  friends/{friendUid}             { addedAt }                                [private]

publicProfiles/{uid}              { displayName, photoUrl, points, friendCode } [world-readable]
shares/{shareId}                  SharedItemDto  (authorUid, period, message, imageUrl)
challenges/{challengeId}          Challenge  (owner, standings)
  participants/{uid}              { score } — projected, NOT client-written (§5.2)
```

- **Completing a task is one document write, not a transaction** (`TaskRepositoryImpl.setDone`).
  It banks a fact in `completionFacts/{taskId}` — a `set` on a known path for a tick, a `delete`
  for an untick — and nothing else. `functions/src/projection.ts` then totals the collection onto
  `publicProfiles/{uid}.points`.

  ⚠️ **This bullet described a `runTransaction` until 2026-08-24, four weeks after `#55` deleted
  it.** The transaction is gone on purpose: it could not be served from the Firestore cache and
  failed after a measured 7.9 s with the radio off. `publicProfiles.level` went with it (§5.2) —
  it was a stored function of `points` in the same document, so every reader could already derive
  it, and the client now does at the point of use.
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
- **Deleting a life area unfiles its goals first** (`LifeAreaRepositoryImpl`
  batch-clears `lifeAreaId`, then deletes). In that order on purpose: a failure
  halfway leaves the area alive, which the user can retry, rather than goals
  holding an id nothing resolves.
- Security rules: `firestore.rules` / `storage.rules`. Private data is owner-only;
  `publicProfiles`/`shares` are readable by any signed-in user. `lifeAreas` needed
  **no rules change** — it is matched by the existing `users/{uid}/{document=**}`
  owner-only rule.

## LLM (GROQ) flow

```
DashboardViewModel → RecommendationRepository → FirebaseFunctions.callable("getRecommendations")
GoalDetailViewModel →       "                 →            "        .callable("scoreTask")
DashboardViewModel  →       "                 →            "        .callable("classifyTask")
AddEditGoalViewModel →      "                 →            "        .callable("proposeMeasure")
                                                        │
                              functions/src/index.ts ──┘  → the provider's chat completions (JSON)
```
- Key lives only in `functions/.env` (spec §5). The model id is pinned in
  `functions/src/index.ts` and overridable via `GROQ_MODEL` — GROQ retires models
  on a rolling schedule, see [SETUP.md](SETUP.md#groq-model--check-before-you-demo).
- **GROQ is the default route, not the only one.** `#54` added `functions/src/providers.ts`:
  a user may bring their own key for GROQ, OpenAI, Anthropic or Gemini, held in
  `data/security/EncryptedAiCredentialStore`. Four named adapters and no generic
  OpenAI-compatible endpoint, so no untested wire format can run.
- All four callables are surfaced in the UI: `getRecommendations` → the AI coach
  card, `scoreTask` → the ✨ button on the add-task row, `classifyTask` → the
  "Smart add a task" card on the dashboard (spec §6 Bonus), `proposeMeasure` → the
  measure proposal on the goal editor.
- `classifyTask` and `scoreTask` also return **`estimatedMinutes`** — the duration
  the time-allocation chart weighs a completed task by — and `classifyTask` takes
  the user's life areas so a goal it creates is filed straight away. Both facts
  ride on the *existing* call rather than a second one: GROQ's free tier allows 30
  requests/minute and the Google Tasks import already spends one per row.
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

- ~~Points/level are written client-side (transaction).~~ **Done, `#42`/`#55`.**
  `functions/src/projection.ts` is now the only writer of `publicProfiles.points`, and
  `firestore.rules` stops the client writing it. The client sums its *own* points on
  device from `completionFacts`, which is what makes a tick work offline.
- Deleting a goal doesn't cascade-delete its `tasks`/`progress` subcollections
  (Firestore has no server-side cascade); acceptable for the demo.
- The time-allocation chart measures **estimated** effort, not clocked time: there
  is no timer in the app, so a task's minutes come from the LLM or from its point
  value. The analytics card says which, per window, rather than implying precision
  it does not have.
- ~~Life areas are not reorderable in the UI.~~ **Done** — the drag handle shipped with
  `ReorderLifeAreasUseCase`, described two sections above. This bullet contradicted the
  same document from 2026-08-04 until 2026-08-24.
