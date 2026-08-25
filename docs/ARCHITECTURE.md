# GoalPilot — Architecture

## Overview

GoalPilot is a single-module Android app in **Kotlin + Jetpack Compose (Material 3)**
following **MVVM with a clean-ish layering** and **Hilt** for dependency injection.
It uses **Firebase** (Auth, Firestore, Storage, Cloud Functions, App Distribution) for
the backend, and an **LLM behind a Cloud Function** so no API key ever ships in the app.

```
┌──────────────────────────────────────────────────────────────┐
│  feature/*   Compose screens + ViewModels (StateFlow<UiState>)│
│  ui/*        theme · components · navigation · tutorial ·     │
│              widget · locale · root auth gate                 │
│  notifications/  channels · reminder workers · deep links     │
├──────────────────────────────────────────────────────────────┤
│  domain/     models · repository INTERFACES · use cases       │ <- no Android/Firebase
├──────────────────────────────────────────────────────────────┤
│  data/       firestore/ · auth/ · storage/ · prefs/ ·         │
│              security/ (encrypted AI keys) · tasks/ ·         │
│              calendar/ · health/ · widget/ · remote/          │
├──────────────────────────────────────────────────────────────┤
│  Firebase (Auth · Firestore · Storage · Functions)            │
│  Google Tasks · Google Calendar · Health Connect              │
│  LLM: GROQ by default, or the user's own OpenAI / Anthropic / │
│       Gemini key -- always via functions/, never from the app │
└──────────────────────────────────────────────────────────────┘
        di/ (Hilt) binds interfaces -> impls, provides Firebase singletons
```

**Twelve feature packages at `HEAD`:** `analytics`, `auth`, `calendar`, `challenges`,
`dashboard`, `goals`, `health`, `lifeareas`, `profile`, `settings`, `social`, `sync`.

⚠️ **This line said *eleven* and omitted `sync` until 2026-08-25.** `feature/sync/` is not a
screen — it is the Google Tasks / Health Connect import surface (`SyncCards`, `SyncSection`,
`SyncViewModel`) that both the Settings screen and the dashboard host, which is exactly why it
kept being missed by a reader counting screens. `DocsCurrencyTest` cannot catch this: the
*"every feature package is named"* assertion was drafted, **run**, and dropped because the
oracle is "does this word occur in the file" and it fired on `social` while going silent on
`health`. The count is maintained by hand and by nothing else.

### Layer rules
- **domain** depends on nothing Android/Firebase — trivially unit-testable (`BuildSummaryUseCase`, `Leveling`).
- **data** implements domain interfaces using Firebase; maps DTOs ↔ domain models.
- **feature** ViewModels depend only on domain interfaces (mockable in tests).
- **ui** holds seven sub-packages, not one: `theme/` (the colour authority), `components/`
  (~24 reusable composables -- `GpCard`, `ProgressRing`, `DonutChart`, `StackedColumnChart`,
  `SuccessFailureRun`, `Avatar`, the pickers), `navigation/`, `root/` (the auth gate),
  `tutorial/` (the in-app guided tour), `widget/` (the home-screen widget) and `locale/`.
- **notifications** is its own top-level package: channels, the permission policy, deep links,
  and two `WorkManager` workers (`OccurrenceReminderWorker`, `PlanTomorrowWorker`).
- **`feature/sync/` is a shared surface, not a screen.** The Google Tasks import and the Health
  Connect connection are the same shape — availability, consent, a reviewable proposal list — so
  they live once and are hosted twice, by Settings and by the dashboard. It reaches into
  `feature/dashboard` for the one sentence that names a proposed duration rather than copying it;
  the comment there records why the tidier fix (a helper in `ui/components`) is blocked by
  `AnalyticsLiteralSweepTest`'s swept-package list.

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

- **Appearance is seven independent axes, not one skin.** Each is a pure enum in
  `domain/model/` with a persisted id and no Compose types, the same split as
  `GoalCategory.iconKey`:

  | Axis | Values |
  |---|---|
  | `AppSkin` | `AURORA` (default), `BLOSSOM` |
  | `AppBrightness` | `SYSTEM`, `LIGHT`, `DARK` |
  | `AppBackground` | `MATCH`, `GLOW`, `SPECTRUM`, `PLAIN` |
  | `AppMaterial` | `GLASS`, `LIQUID_GLASS`, `NEO`, `DARK_NEO` |
  | `AppRelief` | `FLAT`, `RAISED` |
  | `AppLanguage` | `SYSTEM`, `ENGLISH`, `HEBREW` |
  | `AppRegion` | the user's week-start / formatting region |

  They are chosen on the Settings screen and are **device-local**, not synced to
  Firestore. `PaletteTransform` composes them into the scheme actually rendered.
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
  tasks/{taskId}                  TaskDto  (goalEdges[] + goalId projection, points,
                                            difficulty, repeatRule, estimatedMinutes)
  completionFacts/{taskId}        TaskCompletion  — the banked completion (§1.4, #55)
  occurrences/{id}                Occurrence  (taskId, when) — only the instances touched (§7.1, #63)
  lifeAreas/{areaId}              LifeAreaDto  (name, colorHex, iconKey, sortOrder, googleListId)
  summaries/{summaryId}           ProgressSummary
  challengeReports/{challengeId}  what the participant measured — private, owner-written (§5.2)
  challenges/{challengeId}        { }  — the private MIRROR EDGE: "which challenges am I in?"
                                  in one query. `challenges/` above is the world-readable
                                  document; this is the same id under the owner's own tree
  friends/{friendUid}             { addedAt }                                [private]

publicProfiles/{uid}              { displayName, photoUrl, points, friendCode } [world-readable]
shares/{shareId}                  SharedItemDto  (authorUid, period, message, imageUrl)
challenges/{challengeId}          Challenge  (owner, measure, startAt/endAt, pendingMeasureChange)
  participants/{uid}              { score, joinedAt, linkedGoalId, approvedChangeId } —
                                  score is PROJECTED, never client-written (§5.2)
challengeInvites/{inviteId}       ChallengeInvite  (challengeId, fromUid, toUid) — the offer, readable by both sides only (#23)
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

## LLM flow — GROQ by default, the user's own key optionally

```
DashboardViewModel → RecommendationRepository → FirebaseFunctions.callable("getRecommendations")
GoalDetailViewModel →       "                 →            "        .callable("scoreTask")
DashboardViewModel  →       "                 →            "        .callable("classifyTask")
AddEditGoalViewModel →      "                 →            "        .callable("proposeMeasure")
AddEditGoalViewModel →      "                 →            "        .callable("fileGoal")
AddEditGoalViewModel →      "                 →            "        .callable("planGoal")
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
- All **six** callables are surfaced in the UI: `getRecommendations` → the AI coach
  card, `scoreTask` → the ✨ button on the add-task row, `classifyTask` → the
  "Smart add a task" card on the dashboard (spec §6 Bonus), `proposeMeasure` → the
  measure proposal on the goal editor, and — added by `ai-goal-onboarding` —
  `fileGoal` → the silent life-area and category filing a new goal gets on Create,
  and `planGoal` → the work plan offered straight after it.

  📌 **`fileGoal`, `planGoal` and `challengeInvites` were name-only entries until 2026-08-25.**
  They shipped on 2026-08-24 without a doc edit, `DocsCurrencyTest` went **red on `main`** and
  blocked the release guard, and `visual-parity` — which owns none of them — pasted the three
  names in from `FirestorePaths` and `functions/src/index.ts` to unblock a distribution Ido had
  asked for. That was the right call and it is why the guard is a presence check: it bought the
  *names*, and could not buy the prose. The prose is now written — the two callables above, and
  `challengeInvites` under **Challenges → Invites**.
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

## Scheduling, occurrences and the calendar

`Task.repeatRule` (`domain/model/RepeatRule`) describes *when* a task recurs; instances are
**generated, not stored**. One document exists in `users/{uid}/occurrences/{id}` per instance the
user has actually touched — moved, completed, skipped — which is what stops a fortnightly task
becoming 26 documents a year.

- `feature/calendar/` — the §4.3 calendar surface, on its own bottom-bar tab. `CalendarBuilder`
  expands rules into a window; `CalendarModel` is the pure shape the screen renders.
- `domain/usecase/OccurrenceReminders` + `notifications/OccurrenceReminderWorker` — a
  `WorkManager` job per due reminder. `PlanTomorrowWorker` is the evening nudge.
- `domain/usecase/SyncCalendarUseCase` + `data/calendar/GoogleCalendarClient` — two-way sync with
  Google Calendar, on the same token route as the Google Tasks import.

## Challenges — the subsystem that stores no score of its own

`feature/challenges/` + `functions/src/challenges.ts`, `measureChange.ts`, `derived.ts` and the
two projection triggers. **Written 2026-08-25**; this document described challenges only as four
lines of the collection tree until then, which is why every reader had to go to `PRODUCT_v0.3.md`
§6 for the shape.

**The one sentence that explains the rest: a challenge scores from nothing of its own — it scores
from each participant's own goal.** Put a goal and a challenge side by side and they are the same
object (a title, a measure, a start, a current value), so the app keeps **one** representation and
the challenge reads it. Everything that already feeds a goal — Health Connect, a completed task, a
manual log — feeds the challenge for free, and `ProgressEntry.sourceKey` already stops a re-sync
counting twice.

- **A challenge carries a `Measure` (`kind` + `word`), never free text.** `metricUnit` and
  `ChallengeType` are both **deleted**: the free-text unit defaulted to `"points"`, which ranks by
  *time logged* rather than by the thing being raced, and the type was purely presentational —
  nothing ever branched on it to source a score. `MeasureKind` covers the part that meant
  something (`STEPS` is `COUNT`, `SLEEP` is `DURATION`, `RUNNING` is `DISTANCE`).
- **The score is movement in the linked goal over the scoring window**, summed server-side from
  timestamped `progress` entries rather than stored as a delta — which is what makes relink,
  unlink, backfill and dedup fall out for free instead of each needing code.
- **`score` is server-owned.** The client writes the *fact* to `users/{uid}/challengeReports/{id}`,
  which the people reading the standings cannot read; `projectChallengeScore` and
  `projectChallengeScoreOnProgress` project it onto `challenges/{id}/participants/{uid}.score`, and
  `firestore.rules` enforces the split. The honest residual, stated in the spec and true here: this
  stops a win being **typed**, not a reading being **forged** — a typed score is labelled
  `REPORTED` on the standings row with who, what and when.
- **Changing the measure needs every participant's approval.** The owner writes
  `pendingMeasureChange` on the challenge document, each participant writes `approvedChangeId` in
  the one document they are permitted to write, and `applyMeasureChangeOnApproval` /
  `applyMeasureChangeOnProposal` apply it when every row agrees. No new rules partition was needed.

### Two things Ido changed on 2026-08-25, both overriding §6

Both are recorded here rather than only in `PRODUCT_v0.3.md`, because §6 is a decision record and
this is the description of what runs.

1. **Health Connect is a first-class choice in a challenge — pick it and you author no goal.**
   §6 said a challenge scores only from a goal you wrote. It still *does*, mechanically, and it has
   to: **the scoring Function runs in the cloud and cannot read Health Connect**, which is an
   on-device API — the only path a reading has ever taken to Firestore is `SyncHealthDataUseCase`
   writing a `ProgressEntry` against a goal. A literally separate pipe would write the same steps
   twice under two summers that can disagree, which is the exact defect `#23` was filed for. So
   `LinkChallengeToHealthUseCase` **find-or-creates** the canonical `healthSourceKey` goal and links
   it; the user picks *"Steps"* and authors nothing. The offer is matched on measure `kind` alone
   and **says so on the row** — a `COUNT` challenge might be counting steps or books, and matching
   the measure *word* would be a string match over user content that shuts a Hebrew user's
   `"צעדים"` out of an English steps race.
2. **A challenge can be retroactive.** `derived.ts#scoringWindow` — **the copy that decides the
   winner, because scoring runs there and not on the device** — now lets the dates on the tin win:
   `joinedAt` bounds an **open-ended** challenge only. It was `max(joined, startAt)`
   unconditionally, which scored a race for last week as **zero for everybody**, the lower bound
   landing past the challenge's own `endAt`. What §6's `joinedAt` protected (*joining with a
   year-old goal imports a year of history nobody raced for*) can only happen when there is no
   start date to bound the window. `Challenge.scoringWindowFor` is the Kotlin mirror and the two
   must agree, or the standings and the card disagree about the same race.

### Invites

`challengeInvites/{inviteId}` is **top-level, like `shares/`, and for the same reason**: it is one
document two different users must each reach on their own account, and everywhere tidier is
unreachable — `users/{friendUid}/{document=**}` is `isOwner(friendUid)`, and so is their
participant row.

⚠️ **Its read rule inspects `resource.data`, which constrains every QUERY and not only every get.**
A listener here must always carry `whereEqualTo("toUid", myUid)` or `whereEqualTo("fromUid", myUid)`;
an unconstrained collection listener fails with `PERMISSION_DENIED` and the message does not say
why. `firestore-tests/rules.test.mjs` asserts both directions, so the next person finds out from a
test rather than from a device.

## Layout that does not assume a screen width

**Written 2026-08-25**, from Ido's photograph of his S25 Ultra rendering the challenge card's
`Standings` button as a column of single letters.

A `Row` gives its last child whatever width is left over. When that is less than one word the text
has nowhere to go but downwards, so the label renders one character per line. A mechanical sweep
found **seven** such rows in five files, written by different sessions months apart.

- **Action rows are `FlowRow`s, not `Row`s** — a row that will not fit *wraps* instead of crushing
  its last child. `ui/components/FillButtonRow.kt` had documented this in as many words (*"a `Row`
  would clip the last one rather than wrap it"*); the other six simply never got it.
- **Every action label additionally carries `maxLines = 1`**, so a single over-wide button cannot
  stack even when it is alone on its line.

⚠️ **`NarrowScreenGuardTest` does not catch the regression it was written for, and says so in its
own KDoc.** `maxLines = 1` means a crushed button **truncates** instead of stacking, and truncation
is invisible to height, to width and to Compose semantics — reverting the `FlowRow` leaves the
guard green. It guards the *other* half: a label added later without `maxLines`, a raised font
scale, a long translation. **`NarrowScreenRenderPass` at a real card width is what verifies the
fix**, and a render pass taken at AVD width shows nothing at all — which is why the session that
predicted this defect hours earlier measured the margin wrong and shipped anyway.

## Settings, and why it is reachable signed out

`feature/settings/` owns everything that belongs to the **device** rather than the account: the
seven appearance axes above, language and region, notification permission, the sync cards, and the
user's own AI key. It is registered in **both** navigation graphs — beside the sign-in screen as
well as under the tabs — and that is the point: **Profile is the account, Settings is the device**,
and being reachable with no account is what proves the split.

## Bring-your-own AI key

`#54` made the model provider a user choice. `domain/model/AiProvider` names exactly four adapters
— GROQ, OpenAI, Anthropic, Gemini — and a fifth would be a Cloud Function deploy, not a settings
edit. That cost was accepted deliberately over "any OpenAI-compatible endpoint, three text fields",
which would have been free but would have let an **untested wire format** run.

- `data/security/EncryptedAiCredentialStore` holds the key on-device, encrypted; it is never sent
  to Firestore and never leaves the phone except to `functions/`.
- `domain/model/AiAnswer` carries **whose credential paid** for an answer, which is what the
  settings screen shows.
- Model ids are **free text with a per-provider default**, not a curated list — a curated list
  baked into a deploy rots identically, and now in four providers at once.

## The home-screen widget

`ui/widget/` is a Glance app widget. `domain/usecase/BuildWidgetSnapshotUseCase` and
`BuildWidgetTileUseCase` are pure; `data/widget/WidgetSnapshotStore` persists the last snapshot so
the widget renders instantly and offline rather than waiting on Firestore. `WidgetPalette` mirrors
the theme axes above, because a widget cannot read `MaterialTheme`.

**The panel is translucent, at `WIDGET_PANEL_ALPHA = 0.78`** (2026-08-24). An opaque widget panel
is a rectangle pasted onto the launcher wallpaper; the app's own glass surfaces can afford alpha
0.13 because they sit on a page the app controls, and a widget controls nothing behind it. **0.72
is the measured minimum** at which `onSurface` still clears the contrast floor over an arbitrary
wallpaper, and 0.78 is the shipped value — the gap is deliberate margin.
`WidgetPanelContrastTest` (JVM, free) asserts the floor against the shipped constant rather than a
copy of it: a floor proved against a duplicate of the number is a floor proved against nothing.

⚠️ `Untested:` the widgets have not been seen on a real launcher. The translucency is held by
measurement, not by eye.

## The in-app guided tour

`ui/tutorial/` drives a spotlight overlay on first run. Its steps name **anchors** applied by the
screens, and `TutorialStepsTest` asserts every named anchor really exists — the link is not checked
at compile time.

⚠️ **What that test cannot check is whether a step's sentence is still true.** A tour step is a
claim about how the rest of the app is arranged, and when the arrangement moves the step keeps
rendering perfectly and starts lying. It has happened here. Anyone moving a navigation destination
owes the tour a grep. See `kb/dev/product-copy-describes-code.md` in the JARVIS KB.

## Hebrew and RTL

`ui/locale/AppLocale` applies the chosen `AppLanguage`; `core/util/Bidi` isolates interpolated
values so a Latin number inside a Hebrew sentence cannot reorder the line.
`LocaleAwareWindows` exists because a `Dialog` and a `Popup` render into their **own** window,
which does not inherit the activity's locale configuration.

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
- ~~Deleting a goal doesn't cascade-delete its `tasks`/`progress` subcollections.~~ **Done.**
  `GoalRepositoryImpl.deleteGoal` batch-deletes the `progress` subcollection and **unlinks**
  rather than deletes tasks — a task may hang off several goals through `goalEdges`, so the
  edge is removed and the indexed `goalId` projection follows it. Firestore still has no
  server-side cascade; the client does it in batches.
- The time-allocation chart measures **estimated** effort, not clocked time: there
  is no timer in the app, so a task's minutes come from the LLM or from its point
  value. The analytics card says which, per window, rather than implying precision
  it does not have.
- ~~Life areas are not reorderable in the UI.~~ **Done** — the drag handle shipped with
  `ReorderLifeAreasUseCase`, described two sections above. This bullet contradicted the
  same document from 2026-08-04 until 2026-08-24.
