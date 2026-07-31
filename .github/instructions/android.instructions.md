---
applyTo: "app/**"
---

# Android / Kotlin / Compose conventions (GoalPilot)

Project-specific rules for the Android app. Generic rules are in
`general.instructions.md`; test layering is in `testing.instructions.md`.

## Architecture
- **Layering:** `feature/` (Compose + ViewModel) → `domain/` (interfaces + models + use cases) → `data/` (Firebase/GROQ). Wire with Hilt in `di/`.
- **domain is pure:** no Android, Firebase, or Compose imports in `domain/`. Keep models immutable `data class`es.
- ViewModels expose a single immutable `StateFlow<...UiState>` (build with `combine(...).stateIn(viewModelScope, WhileSubscribed(5_000), initial)`). Suspend repo calls return `Resource<T>`; streams return cold `Flow`.
- Screens are **stateless**: `collectAsStateWithLifecycle()`, then call ViewModel functions / navigation lambdas passed from the nav graph. No navigation inside ViewModels.

## Compose
- Material 3 only; theme + reusable widgets live in `ui/`. Prefer existing components (`ProgressRing`, `GoalCard`, `HorizontalBarChart`, `Avatar`, `EmptyState`, `LoadingBox`).
- When using `by animateFloatAsState`/`collectAsStateWithLifecycle`, import `androidx.compose.runtime.getValue`.
- Use `Icons.AutoMirrored.*` for directional icons.

## Firebase / data
- Firestore DTOs (`data/firestore/dto/`) are mutable `var` with defaults + `@DocumentId`; map to/from domain via `Mappers.kt`. Never expose DTOs above the data layer.
- Use `snapshotsFlow()` for realtime reads and `kotlinx.coroutines.tasks.await()` for one-shot writes; wrap writes in `try/catch` → `Resource`.
- Multi-document invariants (points + progress on task completion) go in a Firestore **transaction** with all reads before writes.
- **Secrets never in the app.** GROQ key lives in `functions/.env`; the Web client id comes from `local.properties` → `R.string.gp_web_client_id`.

## Build
- `compileSdk`/`targetSdk` 35, `minSdk` 26. Version catalog is `gradle/libs.versions.toml`.
- Keep the Core build dependency set lean; gate fragile/alpha libs (Health Connect, Google Tasks) behind their activation TODOs rather than adding them to Core.
