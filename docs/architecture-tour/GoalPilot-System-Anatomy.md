# GoalPilot — System Anatomy

### A guided tour of the machine, organ by organ

> **This file is a source document for a Gemini / NotebookLM notebook.**
> Everything below is factual, drawn from the GoalPilot repository at version
> **v0.4.0 (versionCode 9)**, 2026-08-24. Nothing here is invented for effect: every
> file path, class name, collection name and number is real and can be opened.

---

## 0 · Brief for the notebook — read this first

**Produce a slide presentation titled _"GoalPilot — System Anatomy"_.**

**Audience.** Software people. Developers, a course examiner, a technical
reviewer. They already know what MVVM, a NoSQL document store, dependency
injection and a serverless function are. Do **not** explain those. Explain how
*this* system uses them, and why it made the choices it made.

**Format.**
- **22 content slides** — one per numbered section in Part 2 and Part 3 — plus a
  title slide and a closing slide.
- Every slide has the same three-part shape:
  1. **The organ** — one sentence of the body metaphor. This is the hook, not the content.
  2. **What it really is** — the engineering fact: names, files, mechanism.
  3. **Why it is built that way** — the decision, and the alternative that was rejected.
- Put **real identifiers on the slides**: `TaskRepositoryImpl.setDone`,
  `users/{uid}/completionFacts/{taskId}`, `projectPoints`. A software audience
  trusts a name it can grep for; it does not trust a box labelled "Data Layer".
- Where a section contains a diagram, reproduce it as a slide graphic.

**Tone.** The metaphor is a *map legend*, not a costume. One line of anatomy per
slide, then engineering. Never write "just like the human body, our data flows…" —
write "Firestore is the long-term memory" and then spend the slide on Firestore.

**Language.** Generate the deck in English. To get Hebrew instead, add to your
notebook prompt: *"Generate the deck in Hebrew, but keep every code identifier,
file path and collection name in English."*

**What to leave out.** Product marketing. The user's problem. Feature benefits.
This deck is about the machine, not the pitch.

---

## 1 · The body at a glance

### 1.1 Vital statistics

| Measure | Value |
|---|---|
| Application | GoalPilot, `com.idomarhaim.goalpilot`, v0.4.0 (versionCode 9) |
| Platform | Android, `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Client language | **Kotlin**, JVM target 17, built on **JDK 21** |
| UI toolkit | **Jetpack Compose** + Material 3 — no XML layouts anywhere |
| Backend language | **TypeScript** on Node 22 (Cloud Functions) |
| Rules language | **Firebase Security Rules v2** — a third language, with its own test suite |
| Client source | **222 Kotlin files, ~48,500 lines** under `app/src/main/` |
| Backend source | **6 TypeScript modules, ~1,540 lines** under `functions/src/` |
| JVM unit tests | **93 files, ~19,400 lines** under `app/src/test/` |
| Instrumented tests | **46 files, ~11,200 lines** under `app/src/androidTest/` |
| Security-rules tests | `firestore-tests/rules.test.mjs`, run against the local emulator |
| Firebase project | `goalpilot-56e30` (Blaze plan) |
| Deployed functions | **7** — 4 HTTPS callables + 3 Firestore triggers, region `us-central1` |
| Domain layer | 44 models · 14 repository interfaces · 17 use cases |
| Data layer | 25 implementation files |
| UI kit | 24 reusable composables in `ui/components/` |
| Feature packages | 11 screens + 1 non-screen (`feature/sync/`) |

**One thing to notice before anything else:** there are **30,600 lines of test
source** against 48,500 lines of production code. That ratio is not decoration —
Part 3 §19 shows tests in this project that read *the build file and the
documentation as text* and fail the build when they disagree with the code.

### 1.2 The anatomical chart

```
                            ┌────────── THE OUTSIDE WORLD ──────────┐
                            │  Google Sign-In · Google Tasks        │
                            │  Google Calendar · Health Connect     │
                            │  GROQ / OpenAI / Anthropic / Gemini   │
                            └───────────────┬───────────────────────┘
                                            │
  ── THE PHONE ─────────────────────────────┼──────────────────────────────────
                                            │
   SKIN        feature/*  11 Compose screens + ViewModels
               ui/        theme · 24 components · navigation · tutorial ·
                          widget · locale · root (the auth gate)
               notifications/  channels · 2 WorkManager workers · deep links
  ─────────────────────────────────────────────────────────────────────────────
   GENOME      domain/    44 models · 14 repository INTERFACES · 17 use cases
               ← contains no Android type and no Firebase type at all →
  ─────────────────────────────────────────────────────────────────────────────
   ORGANS      data/      firestore · auth · storage · prefs · security ·
                          tasks · calendar · health · widget · remote
               di/        Hilt binds interface → implementation, in one place
  ─────────────────────────────────────────────────────────────────────────────
                                            │
  ── THE CLOUD ─────────────────────────────┼──────────────────────────────────
                                            │
   MEMORY      Firestore  users/{uid}/… (private) · publicProfiles ·
                          shares · challenges
   IMMUNITY    firestore.rules · storage.rules      ← the membrane
   GLANDS      functions/ 4 callables (LLM proxy) + 3 triggers (projection)
   VAULT       Storage    progress photos
```

### 1.3 The one-line description of every layer

- **`domain/`** — the genome. Pure Kotlin. It knows nothing about Android, nothing
  about Firebase, nothing about the network. Everything in it can be tested on a
  laptop with no device in the room.
- **`data/`** — the organs that face outward. They speak Firestore, OAuth, HTTP and
  Health Connect, and translate all of it into the genome's vocabulary.
- **`feature/`** — the specialised tissue. One package per screen: a Compose screen
  and a ViewModel.
- **`ui/`** — the skin, the face and the reflexes: colour, shape, typography, the
  shared component kit, navigation, the first-run tutorial, the home-screen widget.
- **`di/`** — the circulatory system. Hilt, and nothing else, decides which
  implementation reaches which organ.
- **`functions/`** — the endocrine glands. Nothing calls them for a result the user
  is waiting on; they secrete derived state in the background.
- **`firestore.rules`** — the cell membrane. It decides what crosses, per document,
  per field.

---

# Part 2 — The organ tour

Each numbered section below is one slide.

---

## 2 · Slide 1 — The skeleton: how the body is assembled

**The organ.** A skeleton nobody looks at, which decides the shape of everything
attached to it.

**What it really is.** Gradle with the Kotlin DSL, one application module. Not a
multi-module project — a deliberate simplification at this size, where layer
boundaries are enforced by convention and by tests rather than by module visibility.

- `settings.gradle.kts` — the module list.
- `gradle/libs.versions.toml` — the **version catalog**. Every dependency version
  lives in one file and is referenced as `libs.androidx.material3`. No version
  literal is written in a build file.
- `app/build.gradle.kts` — plugins (Android, Kotlin, Compose, Serialization, **KSP**,
  **Hilt**, Google Services, Firebase App Distribution), the two build types, signing.
- `gradle.properties` — pins `org.gradle.java.home` to **JDK 21**, daemon at `-Xmx2560m`.

**The two build types are not the same app.**

| | `debug` | `release` |
|---|---|---|
| Application id | `…goalpilot.debug` | `…goalpilot` |
| Minification | off | **R8 on**, resources shrunk |
| Signing | debug key | the real keystore, or the upload task **fails** |
| App Distribution updater | a **no-op stub** | the real implementation |

That last row deserves a sentence on the slide. The Firebase App Distribution
dependency is split in two: `firebase-appdistribution-api` compiles into every
variant as a stub, and the real `firebase-appdistribution` is `releaseImplementation`
only. So `AppUpdateChecker` compiles everywhere and quietly does nothing in debug.
The build file says out loud: do not collapse this to one dependency.

**Why.** Secrets never enter the repository. `GOOGLE_WEB_CLIENT_ID`, the functions
region and all four release-signing values are read from `local.properties`
(git-ignored) or from environment variables on CI, then injected as `resValue` and
`buildConfigField`. A fresh clone still compiles, on a placeholder id.

---

## 3 · Slide 2 — The genome: the domain layer

**The organ.** DNA. It describes the organism completely and depends on no organ.

**What it really is.** `domain/` — **44 models, 14 repository interfaces, 17 use
cases** — and the rule that gives the layer its value:

> **No Android type and no Firebase type may appear in `domain/`.**

That single constraint is what makes the layer testable with plain JUnit: no
emulator, no Robolectric, no device. It is why 93 JVM test files exist.

**The three kinds of thing that live there.**

1. **Models** — `Goal`, `Task`, `GoalEdge`, `TaskCompletion`, `Occurrence`,
   `LifeArea`, `Measure`, `User`, `Freshness`, and the seven appearance enums.
2. **Repository interfaces** — `GoalRepository`, `TaskRepository`,
   `SocialRepository`, … 14 of them. A ViewModel is typed against the interface and
   has never seen `FirebaseFirestore`.
3. **Use cases** — pure functions over the model: `TimeAllocationUseCase`,
   `ReorderLifeAreasUseCase`, `BuildSummaryUseCase`, `CalendarSync`,
   `DailyMissReview`, `BuildWidgetSnapshotUseCase`.

**The deep fact for a software audience.** Look at `GoalEdge`:

```kotlin
data class GoalEdge(val goalId: String, val contribution: Double? = null)
```

Contribution used to be a field on `Task` — `progressContribution: Double = 1.0`.
The model changed because that number *cannot be true*: a 30-minute run is worth
`1` to "run 20 times", `5` to "run 100 km", and is meaningless to "lose 5 kg". The
quantity belongs to the **pair**, and an edge *is* the pair. And `null` is not
`0.0`: `0.0` declares that this work is worth nothing; `null` says nobody declared
a value at all.

That is domain modelling doing real work — the kind of thing this deck should show
rather than assert.

---

## 4 · Slide 3 — The circulatory system: Hilt and dependency injection

**The organ.** The bloodstream. Every organ is fed from one place and never goes
looking for its own supply.

**What it really is.** Hilt (Dagger) with the KSP compiler, in **three modules**:

- **`FirebaseModule`** — provides four Firebase singletons: `FirebaseAuth`,
  `FirebaseFirestore`, `FirebaseStorage`, and
  `FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION)`. That last one is
  why the region is a build-config field: the client and
  `setGlobalOptions({ region })` in `functions/src/index.ts` must name the same
  region, or every callable 404s.
- **`RepositoryModule`** — one `@Binds` per interface → implementation pair. This is
  the entire seam of the architecture, written out in ~130 lines: `AuthRepository` →
  `AuthRepositoryImpl`, `HealthRepository` → `HealthConnectManager`,
  `CalendarRepository` → `GoogleCalendarClient`, and eleven more.
- **`DispatchersModule`** — coroutine dispatchers, injected rather than referenced
  statically, so a test can substitute a deterministic one.

**Why.** Two payoffs, and usually only the second is mentioned.

1. A ViewModel constructor takes interfaces, so a unit test hands it a `mockk`.
2. **Swapping an implementation is a one-line edit in one file.** The row
   `bindHealthRepository(impl: HealthConnectManager): HealthRepository` is the only
   place in 48,500 lines that knows Health Connect is *how* health data arrives.

`GoalPilotApp` carries `@HiltAndroidApp`; `MainActivity` carries `@AndroidEntryPoint`.
Two objects Hilt cannot construct — the **Glance widget** and the **notification
workers** — reach the graph through explicit `EntryPoint` interfaces
(`WidgetEntryPoint`, `NotificationEntryPoint`), because the framework instantiates
them, not Hilt.

---

## 5 · Slide 4 — The nervous system: MVVM, coroutines, one-way signal flow

**The organ.** Afferent nerves carry sensation up, efferent nerves carry commands
down, and neither carries the other's traffic.

**What it really is.** Unidirectional data flow, identical in all 11 features:

```
Firestore snapshot listener
  → repository Flow<Model>                        (cold, uid-reactive)
    → ViewModel: combine(…).stateIn(viewModelScope, WhileSubscribed(5s), initial)
      → StateFlow<XxxUiState>                     (one immutable object)
        → Screen: collectAsStateWithLifecycle()
          → user event → ViewModel function → repository suspend fun
            → Resource<T> → back up the same path
```

Every screen in this app is **stateless**: it receives a UiState and emits events.
It never holds a variable the ViewModel does not know about.

**Three details that are load-bearing.**

1. **`WhileSubscribed(5_000)`** — the Firestore listener detaches five seconds after
   the last observer leaves. A rotation costs nothing; leaving the screen stops
   paying for the socket.
2. **`Resource<T>`** — the return type of every suspend repository call:
   `sealed interface Resource<out T> { Loading; Success<T>; Error }`. No thrown
   exception crosses a layer boundary in this codebase.
3. **Flows are uid-reactive, and that is a fixed bug, not a flourish.** Repositories
   build their flows on `FirebaseAuth.uidFlow()` (`data/auth/AuthExt.kt`) and
   `flatMapLatest` on it. Reading `auth.currentUser` once at construction pins
   whichever account was signed in *at that moment* — and then serves user A's goals
   to user B when you sign out and back in to demonstrate sharing.

---

## 6 · Slide 5 — The face: Compose, the component kit, the theme engine

**The organ.** Skin, face and expression — everything the outside world actually sees.

**What it really is.** 100% Jetpack Compose. There is no `.xml` layout in the project.

- **`ui/components/` — 24 reusable composables.** `GpCard`, `ProgressRing`,
  `DonutChart`, `SimpleBarChart`, `StackedColumnChart`, `SuccessFailureRun`,
  `Avatar`, `HeroSurface`, `GoalCard`, `DeleteConfirm`, `Entrance`, the pickers.
  Most of them **hold no text at all** — they take copy as parameters, which is why
  they came through the Hebrew localisation sweep untouched.
- **`ui/theme/` — the single colour authority.** `Palettes.kt`'s
  `colorSchemeFor(skin, dark)` returns one of four full Material 3 `ColorScheme`s;
  `accentsFor(…)` returns the off-Material brand accents, exposed as
  `MaterialTheme.gpAccents`.

**Appearance is seven independent axes, not one "theme".** Each is a pure enum in
`domain/model/` with a persisted id and no Compose type in it:

| Axis | Values |
|---|---|
| `AppSkin` | `AURORA` (default), `BLOSSOM` |
| `AppBrightness` | `SYSTEM`, `LIGHT`, `DARK` |
| `AppBackground` | `MATCH`, `GLOW`, `SPECTRUM`, `PLAIN` |
| `AppMaterial` | `GLASS`, `LIQUID_GLASS`, `NEO`, `DARK_NEO` |
| `AppRelief` | `FLAT`, `RAISED` |
| `AppLanguage` | `SYSTEM`, `ENGLISH`, `HEBREW` |
| `AppRegion` | week-start and formatting region |

`PaletteTransform` composes the seven into the scheme actually rendered. They are
**device-local** — `SharedPreferences`, never synced to Firestore — because they
describe this phone, not this account.

**Two decisions worth a line each.**

- **Material You dynamic colour is deliberately off.** It let the wallpaper override
  the brand palette, and it cannot coexist with a user-chosen skin: two colour
  authorities cannot both win.
- **Surfaces are inverted on purpose.** The page is a tinted canvas and `GpCard`
  fills with `surfaceContainerLowest`, so a card reads as an object lifted off the
  background rather than a darker hole cut into it.

**And the palette is tested.** `ThemePaletteTest` asserts WCAG contrast across all
four schemes and the mutual distinctness of the ten `GoalCategory` colours — so a
palette edit that breaks legibility fails the build instead of shipping.

---

## 7 · Slide 6 — The brain stem: startup, the auth gate, navigation

**The organ.** The brain stem. It does not think; it decides whether you are
conscious, and routes everything accordingly.

**What it really is.** A three-state gate, and the whole app hangs off it.

```
MainActivity  (@AndroidEntryPoint, singleTop, adjustResize)
 └─ GoalPilotTheme(skin from AppPreferencesRepository)      ← injected as a field
    └─ GoalPilotRoot ── observes RootViewModel.authState
          ├─ Loading   → spinner
          ├─ SignedOut → SignInScreen      (+ Settings, reachable from here)
          └─ SignedIn  → MainScaffold
                           ├─ bottom bar: Home · Goals · Calendar · Social
                           └─ NavHost — 13 destinations declared in `Routes`
```

**Profile is not on the bottom bar.** Five tabs read as a crowded bar, so Profile
moved to an avatar in Home's top-right and the freed tab went to Calendar.
`Routes.PROFILE` is still registered — the avatar sheet navigates to it.

**Why this shape.** Signing in or out flips one flow, and the whole tree re-routes.
**No screen ever navigates in response to an auth change** — which is what makes
sign-out impossible to get wrong.

**Two details a reviewer will ask about.**

- **`MainActivity` field-injects `AppPreferencesRepository`** rather than using
  `hiltViewModel()`. It has to: the skin must be known *outside* `GoalPilotTheme`,
  and every `hiltViewModel()` lives inside it. That is why the first frame already
  has the right palette instead of flashing the default one.
- **Settings is registered in both navigation graphs** — beside the sign-in screen
  as well as under the tabs. That is the point of the split: **Profile is the
  account, Settings is the device.** Being reachable with no account is what proves it.

---

## 8 · Slide 7 — Digestion: the data layer and the DTO boundary

**The organ.** The gut. It takes in something foreign, breaks it into molecules the
body can use, and lets nothing unbroken past.

**What it really is.** `data/` — **25 files** across ten packages, every one an
implementation of a `domain/` interface:

| Package | What it faces |
|---|---|
| `data/firestore/` | 7 repository impls + `dto/Dtos.kt` + `dto/Mappers.kt` |
| `data/auth/` | Firebase Auth, Google Sign-In, `uidFlow()` |
| `data/storage/` | Firebase Storage — progress photos |
| `data/remote/` | the Cloud Functions callables (the LLM route) |
| `data/security/` | the encrypted on-device AI key store |
| `data/prefs/` | `SharedPreferences` exposed as a hot `StateFlow` |
| `data/tasks/` | Google Tasks REST |
| `data/calendar/` | Google Calendar REST |
| `data/health/` | Health Connect |
| `data/widget/` | the widget's persisted snapshot |

**The DTO boundary is the slide's real content.** `GoalDto` is not `Goal`. The DTO
is the wire shape — nullable, permissive, and versioned by history. The mapper is
where a document written months ago is read correctly today:

- `GoalDto.resolvedMeasure` — a goal whose measure merely *defaulted* to `"%"` is
  read as **absent**, while a goal that genuinely *chose* `PERCENT` keeps it. The two
  cannot be told apart on the wire, and *absent* is the recoverable direction.
- `TaskDto.progressContribution` — documents written before the edge model are read
  **at their stored value**, not at `null`. Rewriting a stored number as a silence
  would be a migration that deletes data.
- `GoalDto.lifeAreaId` — the old singular field is back-filled into the plural
  `lifeAreaIds` on read.

There are **four JVM test files named `*MigrationTest`** whose only job is to assert
that old documents still read correctly. That is the honest cost of a schemaless
store, paid in the one place it can be paid.

---

## 9 · Slide 8 — Long-term memory: the Firestore document tree

**The organ.** Long-term memory. Structured, addressable, and — crucially — partly
private and partly public.

**What it really is.** One database, where the shape of the tree *is* the security model:

```
users/{uid}                      UserDto (points, email, displayName, friendCode)  PRIVATE
 ├─ goals/{goalId}               GoalDto      → lifeAreaIds[]
 │   └─ progress/{entryId}       ProgressDto  (value, note, imageUrl)
 ├─ tasks/{taskId}               TaskDto      (goalEdges[], points, difficulty,
 │                                             repeatRule, estimatedMinutes)
 ├─ completionFacts/{taskId}     TaskCompletion    ← the banked completion
 ├─ occurrences/{id}             Occurrence   (only instances actually touched)
 ├─ lifeAreas/{areaId}           LifeAreaDto  (name, colorHex, iconKey, sortOrder)
 ├─ summaries/{summaryId}        ProgressSummary
 ├─ challengeReports/{id}        what the participant measured   (owner-written)
 └─ friends/{friendUid}          { addedAt }

publicProfiles/{uid}             { displayName, photoUrl, points, friendCode }  WORLD-READABLE
shares/{shareId}                 SharedItemDto (authorUid, period, message, imageUrl)
challenges/{challengeId}         Challenge
 └─ participants/{uid}           { score }    ← projected, NOT client-written
```

**Three modelling decisions to put on the slide.**

1. **Recurring tasks do not become documents.** `Task.repeatRule` describes *when* a
   task recurs; instances are **generated, not stored**. Exactly one document exists
   in `occurrences/` per instance the user actually touched — moved, completed,
   skipped. That is what stops a fortnightly task becoming 26 documents a year.
2. **`publicProfiles` exists because private data cannot be read.** The leaderboard
   needs your points; your tasks are `isOwner(uid)`. So the one number a reader
   cannot compute for themselves gets a public copy — and only that one.
3. **The friend code is six characters because a uid is 28.**
   `AuthRepositoryImpl.ensureProfile` allocates a `FriendCode` on first sign-in and
   mirrors it onto `publicProfiles` so `addFriendByCode` can resolve it. A 28-character
   uid is unusable in a live demo.

**And one query detail with a real bug behind it.** The leaderboard has two modes.
"Everyone" reads `publicProfiles` ordered by points, capped at 100. "Friends"
instead fetches the friends' profiles **by document id** — `whereIn` on
`documentId()`, chunked at Firestore's 30-value cap — because filtering the global
top-100 client-side would silently drop any friend outside it.

---

## 10 · Slide 9 — The immune system: security rules as a membrane

**The organ.** The cell membrane, plus the immune system behind it. It decides what
crosses, in both directions, per molecule.

**What it really is.** `firestore.rules` and `storage.rules` — a third language in
this project, with its own runtime, its own semantics and its own test suite.

The base is four lines and covers most of the app:

```
match /users/{uid} {
  allow read, write: if isOwner(uid);
  match /{document=**} { allow read, write: if isOwner(uid); }
}
```

Everything private — goals, tasks, progress, occurrences, life areas, friends,
completion facts, challenge reports — is covered by that one recursive rule. When
life areas were added, **no rules change was needed at all**.

**Then it gets interesting: field-level ownership.**

```
function serverOwns(field) {
  return resource != null
    && !request.resource.data.diff(resource.data).affectedKeys().hasAny([field]);
}

match /publicProfiles/{uid} {
  allow read:   if isSignedIn();
  allow create: if isOwner(uid) && request.resource.data.points == 0;
  allow update: if isOwner(uid) && serverOwns('points');
}
```

You own your public profile — your display name, your photo, your friend code — but
you **cannot write your own `points`**. The server owns that field.

Three subtleties in nine lines, each worth naming on the slide:

- **`diff().affectedKeys()`** is what makes this expressible at all. It reports the
  keys that actually change between the stored document and the one the write would
  leave behind, so it catches an add, an edit **and** a removal, and it behaves the
  same whether the client sent a merge or a whole document.
- **The `resource != null` guard** exists because Firestore evaluates *both* the
  create and the update clause for a `set()`. On a document that does not exist yet
  there is no `resource`, and `diff()` errors — denying for the wrong reason and
  reading in the logs like a broken rule.
- **The projection function needs no exemption**, because it reaches Firestore through
  the Admin SDK, which bypasses rules entirely. There is no service-account uid to
  keep in step with a deployment.

**The rules decide the data model, not just guard it.** The challenge document is
owner-only — so *joining a challenge cannot be an edit to it*. Participation has to
live in a subcollection each user writes for themselves, and that is why
`challenges/{id}/participants/{uid}` exists at all. The `firestore-tests/` suite
contains two tests literally named "regression" and "the fix": that argument, made
executable.

**And this layer has no other test.** The Kotlin suites cannot reach the rules. The
only way to test them is `@firebase/rules-unit-testing` against the local emulator,
under the project id `demo-goalpilot` — where the `demo-` prefix is load-bearing:
the emulator treats such ids as offline-only and refuses to reach any real backend,
so a rules test can never touch the live project.

---

## 11 · Slide 10 — The endocrine system: Cloud Functions and derived state

**The organ.** Glands. Nothing calls a gland and waits for an answer; it secretes,
and the body adjusts.

**What it really is.** Three Firestore triggers in `functions/src/projection.ts`,
described in their own source as *"the only writer of derived state in GoalPilot."*

| Trigger | Fires on | Writes |
|---|---|---|
| `projectPoints` | `users/{uid}/completionFacts/{taskId}` | `publicProfiles/{uid}.points` |
| `projectPointsOnTaskWrite` | `users/{uid}/tasks/{taskId}` | the same total |
| `projectChallengeScore` | `users/{uid}/challengeReports/{id}` | `challenges/{id}/participants/{uid}.score` |

**The governing rule, in one sentence:**

> A derived number gets a stored writer **if and only if** somebody who cannot read
> its inputs has to read it.

`firestore.rules` already draws that boundary at `isOwner(uid)`. **You** can read
your own tasks, so **your own points need no stored writer at all** — the client
sums them on the device, from the offline cache. A **leaderboard reader** cannot
read your tasks, so the public copy does need one, and it lives here.

**What that rule deleted.** `publicProfiles.level` no longer exists. It was a stored
function of `points` **in the same document**, so every reader could already compute
it. The client now derives it through `Leveling` at the point of use.

**Idempotence by construction, not by care.** Each run re-reads the entire fact set
and writes the total. That is *why* it is safe: a redelivered event, a retried
invocation and a manual re-run all write the same number, because no prior value is
an input. `FieldValue.increment` was rejected on exactly this ground — `increment`
**is** the accumulator, so it makes double-crediting something the function has to
be careful about rather than something it cannot do.

**The offline win fell out of the same move,** and it is the single best story in
this codebase. `TaskRepositoryImpl.setDone` used to hold three writes in one
`firestore.runTransaction`. A transaction **cannot be served from the Firestore
cache**, so with the radio off, ticking a task hung for a measured **7.9 seconds**
and then failed.

Reduced to writing the thing that is a *fact* — `set` on `completionFacts/{taskId}`
for a tick, `delete` for an untick — it became an ordinary **`WriteBatch`**, which
the offline cache takes instantly. (A batch, not a single write: the same batch
clears the pre-migration `done` field on the task, or a legacy document would tick
itself back on.) The distinction that matters is **batch vs transaction**, not one
write vs two — a transaction needs a server round trip to read before it writes; a
batch does not. Nothing on the completion path waits for the function.

---

## 12 · Slide 11 — The consulted specialist: the AI, and why it never holds a scalpel

**The organ.** A specialist the body consults. It examines, it advises — it is never
allowed to operate.

**What it really is.** Four HTTPS callables in `functions/src/index.ts`:

```
DashboardViewModel   ─→ RecommendationRepository ─→ callable("getRecommendations")
GoalDetailViewModel  ─→          "               ─→ callable("scoreTask")
DashboardViewModel   ─→          "               ─→ callable("classifyTask")
AddEditGoalViewModel ─→          "               ─→ callable("proposeMeasure")
                                                          │
                        functions/src/index.ts ───────────┘ → provider chat completions (JSON)
```

**Why a proxy at all.** The API key lives only in `functions/.env`. **No key of any
kind ships in the APK.** That is the entire reason this tier exists.

**Bring your own key — four adapters, not a URL field.** `functions/src/providers.ts`
names exactly four: **GROQ** (the default), **OpenAI**, **Anthropic**, **Gemini**. A
fifth would be a Cloud Function deploy, not a settings edit. The cheaper design —
"any OpenAI-compatible endpoint, three text fields" — was rejected deliberately,
because it would let an **untested wire format** run. The user's own key is held in
`data/security/EncryptedAiCredentialStore` (Keystore-backed
`EncryptedSharedPreferences`) — never in Firestore, and never leaving the phone
except to `functions/`.

**The failure contract is the part worth a slide.** Every call **degrades
gracefully**: on any error the client returns deterministic local guidance —
`fallbackRecommendations`, `fallbackClassification`, `fallbackPoints`. The UI never
blocks, never spins forever, never crashes because a model was slow.

**And the model proposes, it never writes.**

- "Smart add a task" always shows a confirmation dialog before anything is saved.
- A `suggestedGoalId` that matches no real goal is **discarded**, not trusted.
- Every response is validated in a Firebase-free module — `classify.ts`, `measure.ts` —
  where **the list the prompt offers and the list the response is checked against are
  the same constant**, so the prompt cannot drift into offering a value the validator
  then silently drops.

**One economy detail.** `classifyTask` and `scoreTask` also return
`estimatedMinutes` — the duration the time-allocation chart weighs a completed task
by — riding on the *existing* call rather than a second one, because GROQ's free
tier allows 30 requests a minute and the Google Tasks import already spends one per
row.

---

## 13 · Slide 12 — The senses: Google Tasks, Google Calendar, Health Connect

**The organ.** Sense organs. They bring the outside world in; two of them are
read-only, and that is a design decision, not a limitation.

**What it really is.** Three integrations, one OAuth token route, all rendered as
cards inside Settings by `feature/sync/` — which is the one package under `feature/`
that is **not a screen**.

| Sense | Client | Direction | Notes |
|---|---|---|---|
| **Google Tasks** | `data/tasks/GoogleTasksClient` | read → in | list names become **life areas**; each task is filed under a goal by the LLM, deduped, reviewed before saving |
| **Google Calendar** | `data/calendar/GoogleCalendarClient` | **two-way** | Google holds the *when*; GoalPilot holds *what happened* |
| **Health Connect** | `data/health/HealthConnectManager` | **read-only** | steps and sleep; no `WRITE_*` permission is declared, so the user is never asked for one |

**Health Connect is the one to spend time on.** It reads on every foreground, at most
once every fifteen minutes, and logs against a fitness or sleep goal — creating one
if none exists.

- **A day is never counted twice**, and today is *topped up by the difference* as you
  walk, so the goal keeps up without the day being logged again.
- **`Goal.healthSourceKey`** exists because of a real defect. The sync used to answer
  *"does a goal for this metric already exist?"* by matching `category` — but the
  category is a **chip the user can edit**, so one edit orphaned the goal and the next
  sync created a duplicate. An identity the user cannot reach is the only safe key for
  that question. A display attribute is not an identity.
- The manifest carries a `<queries>` entry for `com.google.android.apps.healthdata`,
  without which Android 11+ package filtering hides the provider and `getSdkStatus()`
  reports `SDK_UNAVAILABLE` **on a device that has it installed**.
- Health Connect refuses to grant permissions to an app that cannot explain itself, so
  `HealthPermissionsRationaleActivity` is exported — plus an `<activity-alias>` for
  Android 14+, which moved the same rationale behind `VIEW_PERMISSION_USAGE`.

---

## 14 · Slide 13 — Circadian rhythm: scheduling, reminders and notifications

**The organ.** The body clock. Nothing external tells it when to act.

**What it really is.** All scheduling in this app is **local**, and that is a
consequence, not a preference: there is no credential for a background sync and
there cannot be one, so every reminder is scheduled on the device or not at all.

- **`WorkManager`, not `AlarmManager`.** `notifications/ReminderScheduler` schedules
  one job per due reminder. `OccurrenceReminderWorker` fires a task reminder;
  `PlanTomorrowWorker` is the evening nudge.
- **No `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, deliberately.** Nothing here needs
  the minute, and `USE_EXACT_ALARM` asserts to Play Store review that this is an
  alarm-clock app.
- **The permission is not requested at launch.** From Android 13 the system stops
  showing the notification dialog after two dismissals — so the first ask is very
  nearly the only ask. `NotificationPermissionPolicy` raises it at the first *filing
  outcome that has something to say*, not on a cold start before the user has seen
  the app do anything.
- **`MainActivity` is `singleTop`** so that tapping a second notification while the app
  is open **moves the running instance** instead of stacking a copy on itself. It
  arrives through `onNewIntent`, which is why both that and `onCreate` call
  `NotificationDeepLink.offer`.

**The scheduling model itself** is the same "generated, not stored" idea from §9.
`CalendarBuilder` expands a `RepeatRule` into a window on demand; `CalendarModel` is
the pure shape the screen renders; `domain/usecase/OccurrenceReminders` decides which
instances deserve a job.

---

## 15 · Slide 14 — The reflex outside the body: the Glance home-screen widget

**The organ.** A reflex arc. It runs in a different place from the brain, on a copy
of what the brain last knew, and it must work when the brain is asleep.

**What it really is.** `ui/widget/` — a **Glance** app widget, and the most
misunderstood component in the project.

> It is Compose-shaped, but **it does not render Compose.** The composable tree is
> compiled into a `RemoteViews` that the **launcher inflates in its own process.**

That process boundary is the whole slide. **Nothing from `ui/components/` crosses
it** — no `Canvas`, no `Modifier.blur`, no animation. So `ui/widget/WidgetCharts.kt`
draws its charts **into bitmaps** instead of reusing the app's chart components, and
`WidgetPalette` mirrors the theme axes by hand, because a widget cannot read
`MaterialTheme`.

**It also cannot wait for Firestore.** `data/widget/WidgetSnapshotStore` persists the
last snapshot, so the widget renders **instantly and offline** from local state.
`BuildWidgetSnapshotUseCase` and `BuildWidgetTileUseCase` are pure, and unit-tested
like everything else in `domain/`.

**One packaging detail worth knowing:** the launcher's widget picker lists
*receivers*, so the manifest declares one `<receiver>` per tile — five entries, five
names — rather than one receiver with five configurations.

---

## 16 · Slide 15 — Speech and handedness: Hebrew, RTL and bidirectional text

**The organ.** Handedness. The body is not symmetric, and building it as if it were
produces something that works perfectly until it is used the other way round.

**What it really is.** Full English/Hebrew support with real RTL — and three
mechanisms that exist because RTL is a **defect class**, not a translation task.

1. **`ui/locale/AppLocale`** applies the chosen `AppLanguage` to the whole tree.
2. **`ui/locale/LocaleAwareWindows`** exists because a `Dialog` and a `Popup` render
   into **their own window**, which does not inherit the activity's locale
   configuration. Without it, every dialog in the app silently reverts to the system
   language.
3. **`core/util/Bidi`** isolates interpolated values, so a Latin number inside a
   Hebrew sentence cannot reorder the line. `SimpleBarChart` direction-isolates its
   trailing labels for the same reason.

**And the boundary of what may be translated is drawn in the model.** `MeasureKind`
is a **closed list of seven** — `COUNT`, `DURATION`, `DISTANCE`, `VOLUME`, `MASS`,
`MONEY`, `PERCENT` — because the *kind* is app logic: it fixes the arithmetic, the
fill-button ladder and the rounding rule. Its companion, the **word** ("books",
"litres"), is user content and is **never translated**.

There is deliberately **no `UNKNOWN` member**: an eighth value would be an
untranslatable label inside a closed set. A goal whose kind nobody recorded is
expressed as `Measure(kind = null, …)` instead.

Four JVM tests guard this by reading `src/main/res` and `src/main/java` **as text**:
`HebrewLocaleResourceTest`, `HebrewTerminologyTest`, `AnalyticsLiteralSweepTest`,
`WidgetHebrewResourceTest`.

---

## 17 · Slide 16 — Metabolism without oxygen: offline, and the honesty of "as of"

**The organ.** Anaerobic metabolism. The body keeps working when supply is cut — and
knows the difference between what it has and what it merely remembers.

**What it really is.** Firestore's offline persistent cache is on by default on
Android, so goals and tasks are readable with no connection. That is free. **What is
not free is knowing how old somebody else's data is** — and this project models it
explicitly:

```kotlin
data class Freshness(
    val asOfEpochMillis: Long = 0L,   // the NEWEST updatedAt across the rows held
    val neverLoaded: Boolean = false, // came back empty AND from cache
)
```

**The governing sentence, from the model's own documentation:**

> Staleness is a property of the **data**, not of the **connection**.

A leaderboard fetched forty minutes ago over perfect Wi-Fi is exactly as old as one
served from cache with the radio off. So **nothing here asks the OS about the
network** — both facts come off the Firestore snapshot itself.

**Two refinements that show the care.**

- **Newest, not oldest.** "As of" claims *nothing here reflects a write after this
  time*. Each row's stamp says when its owner last wrote **the copy we hold**; it
  never promises they have not written since — which is exactly why the UI shows a
  caption and not a guarantee.
- **`neverLoaded` is not "empty".** If this device has never actually seen the
  collection, rendering *"No friends"* would be the app **stating a fact about
  someone else's data it has never read**. An empty list you own is a fact; an empty
  read you have never performed is not.

**And it rides only where it is needed.** `Freshness` wraps exactly the two
cross-boundary reads — `publicProfiles/{uid}` and
`challenges/{id}/participants/{uid}` — the two collections `firestore.rules` lets
somebody other than the owner read. Owner-side data is complete and correct offline,
and is deliberately not wrapped.

---

## 18 · Slide 17 — Growth: the gamification arithmetic

**The organ.** Growth. Monotonic, irreversible, and computed from what actually
happened rather than from a running tally somebody could corrupt.

**What it really is.** A quadratic level curve in `domain/model/User.kt`:

```
pointsForLevel(n) = 50 · (n − 1) · n        L1: 0 · L2: 100 · L3: 300 · L4: 600 …
```

`User.level`, `levelProgress` and `pointsToNextLevel` are all **derived** — none is
stored anywhere.

**The design principle underneath it is the one to teach:**

> **Points are banked as their inputs, never as a number.**

On completion, `minutes` and `difficulty` are stamped into a timestamped completion
fact at `users/{uid}/completionFacts/{taskId}`. The lifetime total is a **sum over
facts**.

**The defect that argument is aimed at:** tick a task worth 10 points, correct its
duration so it is now worth 30, then untick it. Any design that stores the *number*
at one moment and subtracts a *different* number at another lets a level fall.
Summing the facts means the arithmetic never branches, no stored number can
disagree with another, and **a level can never fall**.

**Where the minutes come from, and which way the arithmetic runs.** This was
**inverted**, and the inversion is the point:

```
points = round(minutes ÷ 3) × difficulty         ← TaskScoring
minutesOf(task) = task.estimatedMinutes ?: 30    ← TaskDuration
```

Minutes are the **input**; points are a **view** of them. A task with no stored
duration is a half-hour chore — not a number reconstructed backwards from what it
was scored. The old backwards path survives only as
`TaskDuration.legacyMinutesFromPoints`, narrowed from a live path to a **migration**
for tasks written before the completion-fact model.

One function, `TaskDuration.minutesOf`, decides for the whole app. And the analytics
card **states how many of the window's durations were the model's** rather than
implying they all were.

> ⚠️ **Presenter's note.** `docs/ARCHITECTURE.md` still describes the *old* direction
> ("`TaskDuration.fallbackMinutes(points)` — 3 minutes per difficulty point"). The
> code above is what actually ships. This block is correct; that line in
> ARCHITECTURE.md is stale.

---

# Part 3 — Systems, not organs

---

## 19 · Slide 18 — The immune memory: how this codebase defends itself

**The organ.** Acquired immunity. Every defect that got in once leaves behind an
antibody that recognises it instantly.

**What it really is.** Four test layers, and a fifth category that does not exist in
most projects.

| Layer | Where | Count | Runs on |
|---|---|---|---|
| JVM unit | `app/src/test/` | 93 files | laptop, no device |
| Instrumented / Compose UI | `app/src/androidTest/` | 46 files | emulator or phone |
| Security rules | `firestore-tests/` | 1 suite | Firebase emulator |
| Cloud Functions | `functions/test/` | 4 suites | plain `node --test` |

**The fifth category: tests that read the project as text.** These do not exercise
code at all — they open files and assert on their contents.

- **`DocsCurrencyTest`** — reads `docs/ARCHITECTURE.md` and asserts it still matches
  the code: that the callables it names are the callables `functions/src/index.ts`
  exports, that the bottom-bar tabs it lists are the tabs `Destinations.kt` declares,
  that the JDK version it quotes is the one `gradle.properties` pins. **Documentation
  that contradicts the code fails the build.**
- **`TutorialStepsTest`** — asserts every anchor named by a tutorial step really
  exists, since that link is not checked at compile time.
- **`ReleaseNotesGuardTest`** — reads the release notes *and the release workflow*.
- **`HebrewLocaleResourceTest` and friends** — read `res/` and `java/` as text.

**And here is the sharpest engineering lesson in the whole repository.** A test that
reads files off disk is invisible to Gradle's up-to-date check, whose declared inputs
for a test task are the test classes and the runtime classpath. So editing the
**value** of an existing string leaves every declared input byte-identical,
`testDebugUnitTest` reports `UP-TO-DATE`, and **the guard that exists to catch exactly
that edit never executes a single assertion.**

It fails in the flattering direction, and it fails precisely when it matters — a
resource-only change is what a localisation sweep *is*. The fix is eight declared
inputs in `app/build.gradle.kts`:

```kotlin
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/res"))
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
    inputs.dir(rootProject.layout.projectDirectory.dir("docs"))
    inputs.file(rootProject.layout.projectDirectory.file("README.md"))
    inputs.file(rootProject.layout.projectDirectory.file("functions/src/index.ts"))
    inputs.file(rootProject.layout.projectDirectory.file("gradle.properties"))
    inputs.file(layout.projectDirectory.file("build.gradle.kts"))
    // … and the shared fixture, the release notes, the release workflow
}
```

**One fixture, two languages.** `shared-fixtures/derived-state.json` is read by
`DerivedStateFixtureTest` (Kotlin) **and** `functions/test/projection.test.mjs`
(Node). The points arithmetic exists in two languages, and one file proves they
agree. Neither layer may own it — which is why it sits at the repository root.

---

## 20 · Slide 19 — Life support: CI, signing and delivery

**The organ.** Life support and the delivery ward. The parts of the system that
exist so the organism can leave the building.

**What it really is.** Three GitHub Actions workflows.

1. **`instrumented-tests.yml` — the cloud emulator.** Boots an Android emulator on
   GitHub's hardware, runs `app/src/androidTest/`, and **photographs the running
   app**. Artifacts: an HTML test report and PNGs plus `logcat.txt`. It exists for a
   concrete reason — the development machine measured **15.67 GB total RAM with
   0.85 GB free**, and build + emulate + edit do not fit on it at once. Run #1 was
   green in **12m 02s**, and it now runs on every app change.
2. **`release.yml` — tag-triggered.** Unit tests → `assembleRelease` → upload to
   **Firebase App Distribution**, notes from `app/release-notes.txt`.
3. **`backup-signing-key.yml`** — because losing the upload key is unrecoverable.

**The signing rule, and why it is a hard failure rather than a warning:**

> A debug-signed APK is a dead end. The moment a tester installs one, the properly
> signed successor can never update it — Android rejects a signature change, and the
> only way out is uninstall-and-lose-your-data.

So a local `assembleRelease` may fall back to the debug key (handy, and it never
leaves the machine), but every `appDistributionUpload*` task carries a `doFirst`
that **refuses to run** without the real key.

**And one silent trap the checklist exists for:** `versionCode` must move strictly
upward. App Distribution compares it, and Android refuses to install a lower one.
Forget it and the build succeeds, the upload succeeds, and **testers are simply never
prompted**.

---

## 21 · Slide 20 — A single heartbeat, traced end to end

**The organ.** One heartbeat, followed from the nerve that fires it to the last
capillary it reaches.

**Trace: the user ticks a task as done.**

```
 1. Screen           the tick: GoalDetailScreen's task row, or CalendarScreen's onTick
 2. ViewModel        GoalDetailViewModel.toggleTask / CalendarViewModel.setDone
                     → viewModelScope.launch
 3. Interface        TaskRepository.setDone(taskId, done)      ← domain, an interface
 4. Implementation   TaskRepositoryImpl.setDone
 5. Model            TaskCompletion.of(task, now) — minutes + difficulty, the INPUTS
 6. Write            a WriteBatch, not a transaction:
                       set()    users/{uid}/completionFacts/{taskId}   ← the fact
                       update() users/{uid}/tasks/{taskId}             ← clear legacy `done`
                     A batch IS servable from the offline cache; a transaction is not.
                     The UI is already updated.  (Untick = delete() + the same clear.)
 ─────────────────── the user's part of the story is over here ───────────────────
 7. Cache → server   Firestore syncs the write when there is a connection
 8. Rules            users/{uid}/{document=**} → isOwner(uid) → allowed
 9. Trigger          projectPoints fires on completionFacts/{taskId}
10. Projection       re-reads the WHOLE fact set, sums it (derived.ts, no Firebase)
11. Admin write      publicProfiles/{uid}.points = total     ← bypasses rules
12. Other devices    every leaderboard listener gets the new value
13. Their client     Leveling.levelForPoints(points) — the LEVEL is never stored
14. Own device       DerivedProgress recomputes the goal ring from the same facts
15. Widget           WidgetSnapshotStore updated; the launcher redraws in ITS process
```

**What to say over this slide.** Step 6 is one batch, and it completes against the
local cache. Steps 9 through 11 are the only place derived state is authored, they
are idempotent by construction, and **nothing in steps 1 through 6 waits for them**. The number the user sees is computed on their
own device from facts they own; the number *other people* see is projected by the
server, because they cannot read the inputs.

**Second trace, for contrast: "smart add a task".**

```
 1. Dashboard   the user types free text: "practise saxophone 20 minutes on Sunday"
 2. ViewModel   DashboardViewModel → RecommendationRepository
 3. Callable    functions/src/index.ts → classifyTask
                (the request carries the user's goals AND their life areas)
 4. Provider    GROQ by default, or the user's own key via providers.ts
 5. Validate    classify.ts — schema-checked against the SAME constant the prompt used
 6. Return      goal (or a proposed new one), points, estimatedMinutes, lifeAreaId
 7. Fallback    on ANY error: fallbackClassification — deterministic, local, no crash
 8. Confirm     a dialog. Nothing has been written.  ← the AI proposes, never writes
 9. Save        only now: a real Task document, with a real GoalEdge
```

---

## 22 · Slide 21 — The laws this body obeys

**The organ.** Homeostasis. The small number of rules the whole organism is arranged
around.

Five principles, each visible in code you can open:

1. **Derive, don't store.** A derived number gets a stored writer **if and only if**
   somebody who cannot read its inputs has to read it. This deleted
   `publicProfiles.level`, moved points to a projection, and made `Goal.currentValue`
   a view computed at the repository boundary rather than a field.
2. **Bank the inputs, not the number.** Completion stores `minutes` and `difficulty`,
   not points. So the arithmetic never branches and no two stored numbers can disagree.
3. **The AI judges; the app computes.** The model returns a judgement — a category, a
   difficulty, a duration estimate. Every arithmetic consequence is computed locally
   from it. And nothing the model returns is written without confirmation.
4. **One authority per concern.** One colour authority (`ui/theme/`). One duration
   function (`TaskDuration.minutesOf`). One writer of derived state
   (`functions/src/projection.ts`). One place that binds interfaces to implementations
   (`RepositoryModule`).
5. **Legal, but never silent.** Where the app cannot know something, it says so rather
   than guessing: the "Unassigned" pie slice for unresolvable links, the analytics
   caption naming how many durations were estimated, `neverLoaded` instead of
   "No friends", `Measure(kind = null)` instead of an invented default.

---

## 23 · Slide 22 — What a physician would flag

**The organ.** The honest note at the bottom of the chart.

Real, current limitations — worth a closing slide because a technical audience will
find them anyway, and finding them listed is a much better outcome:

- **The time chart measures *estimated* effort, not clocked time.** There is no timer
  in the app. A task's minutes come from the LLM or from its point value, and the
  analytics card says which, per window, rather than implying precision it does not have.
- **Firestore has no server-side cascade.** Deleting a goal batch-deletes its
  `progress` subcollection and **unlinks** its tasks (a task may hang off several goals
  through `goalEdges`, so the edge is removed and the indexed `goalId` projection
  follows it) — all of it done by the client, in batches.
- **Ordering is client-side.** `lifeAreas` are sorted by `(sortOrder, name)` on the
  device; a composite index for a collection holding a handful of documents would be
  ceremony.
- **Challenges are architected and scaffolded, not finished** — model, Firestore rules,
  projection trigger and a preview screen all exist.
- **A tutorial step is a claim about how the rest of the app is arranged.**
  `TutorialStepsTest` proves the anchor exists; nothing can prove the sentence is still
  true. When the arrangement moves, the step keeps rendering perfectly and starts lying.

---

## Appendix A — Every deployed backend symbol

| Symbol | Kind | Trigger / route |
|---|---|---|
| `getRecommendations` | HTTPS callable | AI coach card on the dashboard |
| `classifyTask` | HTTPS callable | "Smart add a task" |
| `scoreTask` | HTTPS callable | the ✨ button on the add-task row |
| `proposeMeasure` | HTTPS callable | measure proposal on the goal editor |
| `projectPoints` | Firestore trigger | `users/{uid}/completionFacts/{taskId}` |
| `projectPointsOnTaskWrite` | Firestore trigger | `users/{uid}/tasks/{taskId}` |
| `projectChallengeScore` | Firestore trigger | `users/{uid}/challengeReports/{challengeId}` |

Region `us-central1`, `maxInstances: 5`, Node 22. Supporting modules —
`derived.ts` (arithmetic), `classify.ts`, `measure.ts`, `providers.ts` — import **no
Firebase**, so all four run under plain `node --test` with no emulator.

## Appendix B — Every feature package

| Package | Screen | Notable |
|---|---|---|
| `feature/auth` | Sign-in | Google Sign-In; the only screen outside the tabs |
| `feature/dashboard` | Home | AI coach card, Smart add, avatar → profile |
| `feature/goals` | Goals list, detail, editor | banded by life area; `FillButtonRow`, `WhenPicker` |
| `feature/calendar` | Calendar tab | `CalendarBuilder`, `SlotSheet`, drag-to-move |
| `feature/social` | Social tab | leaderboard (two modes), shares feed, photo viewer |
| `feature/profile` | Profile | level bar, friend code, life areas entry |
| `feature/analytics` | Analytics | range picker, interactive `DonutChart`, two bar charts |
| `feature/lifeareas` | Life areas | drag-to-reorder, Google Tasks proposals |
| `feature/settings` | Settings | seven appearance axes, language, AI key, sync cards |
| `feature/challenges` | Challenges | scaffolded |
| `feature/health` | (no screen) | the exported Health Connect rationale activity |
| `feature/sync` | **not a screen** | Google Tasks + Health Connect, rendered inside Settings |

## Appendix C — Where to look for each claim in this document

| Claim | File |
|---|---|
| Layering and data model | `docs/ARCHITECTURE.md` |
| Product and modelling decisions | `docs/PRODUCT_v0.3.md` |
| Build, signing, variants | `app/build.gradle.kts` |
| Security model | `firestore.rules`, `storage.rules`, `firestore-tests/rules.test.mjs` |
| Derived state | `functions/src/projection.ts`, `functions/src/derived.ts` |
| LLM proxy and providers | `functions/src/index.ts`, `functions/src/providers.ts` |
| Offline semantics | `domain/model/Freshness.kt` |
| Points arithmetic | `domain/model/Task.kt`, `domain/model/User.kt` |
| Cloud device options | `docs/CLOUD-DEVICE.md` |
| Release process | `docs/RELEASING.md`, `.github/workflows/release.yml` |
