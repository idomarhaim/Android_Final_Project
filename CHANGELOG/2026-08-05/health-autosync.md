# Changes — 05/08/2026 — session `health-autosync`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** Health Connect now syncs by itself every time the app comes forward — at most once per fifteen minutes — and writes every reading that is not in Firestore yet, with no review sheet. The day-level dedupe that made a manual sync safe had to become a *value* comparison for that to work, because today is still growing.

Requested as two sentences: sync on entering the app unless fifteen minutes have
not passed, and stop asking which readings to sync. The second half is a deletion;
the first half is where the work was.

## 🔁 The sync itself

`domain/usecase/SyncHealthDataUseCase.kt` *(new)* owns the whole path — throttle,
availability, read, dedupe, write — as an app-scoped singleton, because the two
callers must not race each other into the same write:

- **`ui/root/GoalPilotRoot.kt`** fires it on `Lifecycle.Event.ON_START`, inside the
  signed-in branch. `ON_START` rather than `ON_RESUME`: a permission dialog or the
  app switcher pauses the activity, and coming back from those is not "opening the
  app". A lifecycle already past STARTED replays the event to a new observer, so
  cold start and the moment sign-in completes are both covered.
- **The dashboard card** fires it with `MANUAL`, which skips the throttle. The user
  pressed a button; "you synced eight minutes ago" is not an answer.

The trigger sits at the root rather than on the dashboard because returning to the
app on the profile tab is still returning to the app.

**The throttle stamp is persisted per uid** (`health_last_sync_<uid>` in the
existing `goalpilot_ui_prefs`). In memory it would reset on every cold start —
which is precisely when the sync fires — so the window would never once apply. Per
uid so that signing in on a shared device does not inherit the other account's
clock. A stamp in the future (clock correction, timezone edit) is treated as
expired rather than as fresh, or the feature would park itself for up to an hour.

## ➕ Why the dedupe had to change

The old rule was "skip any day that already has an entry". That was correct when a
sync was a deliberate act once a day. At one sync per app entry it is actively
wrong: the first sync of the morning writes 2,000 steps, every later sync sees the
day and skips it, and the day ends recorded as 2,000. **The reading that survives
is whichever one happened at breakfast.**

So `alreadyLogged` went from `Set<String>` to `Map<String, Double>` — how much each
day has been *credited*, not whether it was seen — and a day is topped up by the
difference. `hc:steps:2026-08-05` is deliberately no longer unique per entry: a day
carries one entry per top-up and their sum is what the next sync compares against.
Consequences, all tested:

- A day that has shrunk (Health Connect dropped a duplicate record, or the user
  logged by hand) yields a negative delta and is **skipped** — this never subtracts.
- A change below `HealthMetric.minimumDelta` (1 step, 0.1 hour) is not worth a row;
  without that floor, sleep recomputed from 7.49 to 7.50 would litter the history.
- The note distinguishes the two: `Health Connect · +9,000 steps (11,000 total) ·
  Wed, Aug 5` versus `Health Connect · 8,432 steps · Sat, Aug 1`. A bare "+2,300" on
  a day the user knows they walked eleven thousand reads like a bug.

## 🗑️ What was deleted, and what replaced it as the safety net

`HealthSyncDialog` and its proposal plumbing (`toggleHealthProposal`,
`confirmHealthSync`, `HealthLogProposal.selected`, `HealthSyncState.proposals` /
`skippedCount` / `isVisible`) are gone. Approved explicitly before deleting.

With no human in the loop, **the two Firestore lookups the sync depends on stopped
being optional.** The old code tolerated a timed-out dedupe lookup because the user
still reviewed every row; now a timeout on either the goals read or the entries read
returns `Failed` and writes nothing. Proceeding blind would create a second "Weekly
steps" goal or log Tuesday twice, and nobody would catch it.

Auto-sync also **never raises the permission dialog** — a system prompt appearing by
itself on every launch is an ambush. It stands down silently at
`PermissionsRequired`; the card's button is where permission is granted, and the
throttle is deliberately *not* stamped in that case, so granting and coming back
syncs immediately instead of waiting out a window in which nothing happened.

## 🖼️ The card

Kept, and re-pointed at what it is now for: a feature that writes to your goals
without ever being visible is worse than one you have to press. It shows
availability, "Synced 4 minutes ago" (or "Not synced on this device yet"), and a
"Sync now" button. A snackbar appears only when a sync actually **wrote**
something; a manual sync additionally reports nothing-to-do and failures, an
automatic one stays quiet about both.

## 🧪 Tests

| Layer | Result |
|---|---|
| JVM unit (`:app:testDebugUnitTest`) | **197 passed**, 0 failed — was 175 |
| Instrumented (`:app:connectedDebugAndroidTest`, `Pixel_10_Pro_XL`, API 37) | **29 passed**, 0 failed |
| Firestore rules (`firestore-tests/`) | **not run** — no rule changed; the sync writes to `users/{uid}/…`, already covered |
| Live device behaviour | see below |

New: `HealthSyncTest` (18 tests) covers the throttle in both directions, the
per-account stamp, the backwards clock, the empty store still starting the window,
top-up arithmetic including three entries summing to one day's credit, manual
entries (no `sourceKey`) staying out of the dedupe, goal creation once per metric,
and the three refusals — a goals lookup that never returns, a dedupe lookup that
never returns, and missing permissions. `HealthProposalsTest` grew five cases for
delta, shrinkage, the minimum delta and both note shapes; it lost
`everything starts selected`, which described a field that no longer exists.

**Verified on the emulator**, against the real app rather than a fake:

1. Fresh install → the card reports permissions required, and **no dialog appeared
   by itself** on launch.
2. "Connect Health Connect" → Health Connect's own screen → Allow all → the app
   synced, the card flipped to "Synced just now", snackbar *"Health Connect is
   already up to date"* (the emulator's store is empty).
3. `health_last_sync_cTmjUK6…` appeared in `goalpilot_ui_prefs.xml`, keyed by uid.
4. Force-stop → cold start → **stamp unchanged after 20s**: throttled.
5. Stamp rewound 20 minutes on disk → cold start → **stamp moved to now**: the
   window expired and the sync ran. Both directions proven against the device's own
   record rather than against the UI.

## 🐛 One defect, found by running it

The first build crashed on launch — `NullPointerException` on
`MutableStateFlow.getValue()` inside `DashboardViewModel.<init>`. Collecting the
sync's `status` from the ViewModel's top `init` block reaches `_healthSync`, which
is declared further down: property initialisers run in source order, and a
`StateFlow` hands over its current value *synchronously* on
`Dispatchers.Main.immediate`, so the collector ran against a field that was still
null. Fixed by moving that collector into a second `init` placed below the property.
Neither unit test layer could have caught it — both halves are individually fine.
Recorded in `AGENTS.md`.

## ⚠️ Still unproven

The same gap as before, unchanged by this session: **no reading has ever been
written from real data.** The emulator carries the Health Connect provider but its
store is empty, so every run above exercised the read, the throttle and the
refusals — never the write. The top-up path in particular has only ever run against
fakes. That is the physical-phone follow-up already on the board.

## 📁 Files

**New**
- `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SyncHealthDataUseCase.kt`
- `app/src/test/java/com/idomarhaim/goalpilot/domain/HealthSyncTest.kt`
- `CHANGELOG/2026-08-05/health-autosync.md`

**Modified**
- `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildHealthProposalsUseCase.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/domain/repository/AppPreferencesRepository.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/data/prefs/AppPreferencesRepositoryImpl.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/ui/root/RootViewModel.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/ui/root/GoalPilotRoot.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt`
- `app/src/test/java/com/idomarhaim/goalpilot/domain/HealthProposalsTest.kt`
- `AGENTS.md`, `README.md`, `SESSIONS.md`, `TODO/TODO.md`,
  `TODO/TODO_OPTIONAL/Integrations.TODO.optional.md`,
  `CHANGELOG/CHANGELOG_README.md`
