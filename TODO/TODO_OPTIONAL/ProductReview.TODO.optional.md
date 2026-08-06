# Product review — actionable backlog

Everything from the 2026-08-06 product/UX brief that is **already answerable** —
defects to fix and single-session UX work. Source text, numbered `R1`–`R28`:
[`Product and UX Reviews/2026-08-06-brief-review.md`](../../Product%20and%20UX%20Reviews/2026-08-06-brief-review.md).

The items that are *not* answerable — the product-model questions where fixing
one without deciding the others would be guesswork — live in
[`TODO_FUTURE/ProductModel.TODO.future.md`](../TODO_FUTURE/ProductModel.TODO.future.md)
and are bound for a `/wayfinder` map.

**None of this blocks submission.** Spec §6 and §7 are built and verified; this is
a v2 backlog.

> ⚠️ **Repro status is static only.** The notes below were derived by reading the
> source, not by driving the app. "Confirmed statically" means the code cannot do
> the thing; it does not mean the observed symptom has the cause named here. The
> device pass — reproduce each defect against a real build, on the emulator and
> for the Health Connect one a physical phone — is still owed, and this repo has
> been burned by a stale backlog premise before (`TODO_MUST/Submission.TODO.must.md`
> §1). Nothing here graduates to a GitHub issue until it has been reproduced.

---

## 🐞 Defects

- [ ] **D1 · Shared challenge does not sync with my tasks or Health Connect** (`R1`)
  Progress inside a challenge has to be typed in by hand; completing the tasks or
  walking the steps that the challenge is nominally about moves nothing.
  *Not verified statically* — `ChallengeRepositoryImpl` and `SyncHealthDataUseCase`
  need reading together to say whether this is a missing wiring or a deliberate
  scope line from the challenges session. **Start here in the device pass**: it is
  the only defect whose expected behaviour is not self-evident, and it may turn out
  to be a design question (what *should* a challenge score from?) rather than a bug.
  Touches: `data/firestore/ChallengeRepositoryImpl.kt`,
  `domain/usecase/SyncHealthDataUseCase.kt`, `feature/challenges/`.

- [ ] **D2 · No route from a life area into its goals** (`R2`)
  **Confirmed statically.** `feature/lifeareas/LifeAreasScreen.kt` contains no
  navigation call at all — the areas screen is a management surface (rename,
  reorder, propose), and nothing on it is a way in to the goals it groups. The
  goals list is already banded by area (`lifearea-polish`, 2026-08-04), so the
  destination exists; only the entry point is missing.
  Touches: `feature/lifeareas/LifeAreasScreen.kt`, `ui/navigation/Destinations.kt`,
  `feature/goals/GoalsScreen.kt` (needs to accept an area to scroll/filter to).

- [ ] **D3 · Completing a task takes too long to show the checkbox and the graph** (`R13`)
  Reported as latency. Needs measuring before it is fixed — a Firestore round-trip
  that the UI waits on rather than an optimistic local update is the likely shape,
  but that is a guess until timed.
  Touches: `feature/goals/GoalDetailViewModel.kt`, `data/firestore/TaskRepositoryImpl.kt`
  (`setDone`), `ui/components/ChartAnimation.kt`.

- [ ] **D4 · A shared photo cannot be opened** (`R27a`)
  **Confirmed statically.** `feature/social/SocialScreen.kt:269-271` renders the
  image with `AsyncImage` inside the feed card and attaches no click handler —
  there is no full-screen or zoom destination for it to open into.
  Touches: `feature/social/SocialScreen.kt`.

- [ ] **D5 · A user cannot delete a share they made** (`R27b`)
  **Confirmed statically, and it is not UI-only.**
  `domain/repository/SocialRepository.kt` exposes `observeFeed` and `shareSummary`
  and no delete of any kind, so this needs a repository method, a `firestore.rules`
  clause restricting deletion to the author, a rules test, and the UI affordance.
  Treat it as a small feature, not a one-liner.
  Touches: `domain/repository/SocialRepository.kt`,
  `data/firestore/SocialRepositoryImpl.kt`, `firestore.rules`, `firestore-tests/`,
  `feature/social/SocialScreen.kt`, `data/storage/StorageRepositoryImpl.kt`
  (the uploaded image should go with the share).

- [ ] **D6 · ~~Task scores do not match goal percentages~~ → reclassified, not a defect** (`R12`)
  **Do not open this as a bug.** The two numbers are independent by construction:
  `Task.points` is a gamification currency the LLM sets (default `10`), while a
  goal's percentage is `currentValue / targetValue`, advanced by
  `Task.progressContribution` — which defaults to `1.0` and is surfaced nowhere in
  the UI. So every completed task moves its goal by one unit regardless of what the
  task was worth, and the "book whose tasks cover the whole thing" is that default
  showing through, not a scoring error. Fixing the symptom without deciding the
  model would harden the wrong one. Moved to `ProductModel.TODO.future.md` **C3**.

## ✨ Single-session UX work

- [ ] **U1 · Smart add should not ask for approval every time** (`R3`)
  Today `SmartAddCard` always opens `SmartAddDialog` for confirmation
  (`feature/dashboard/DashboardScreen.kt:180,239`). Wanted: a preference —
  default **off**, i.e. file it silently — with the dialog kept as the opt-in.
  `AppPreferencesRepository` already exists to hold it. Pairs naturally with **U3**:
  silent filing is only safe if the user is told when the sorter improvised.

- [ ] **U2 · Complete a task from within quick add** (`R6`)
  A task you are entering because you already did it should not need a second trip
  to the goal detail screen.

- [ ] **U3 · Notify when the sorter invents a new goal** (`R5`)
  In-app **and** a system notification. The app currently ships no notification
  channel at all, so this item carries the whole notification substrate with it —
  channel, permission request (API 33+), and a tap-through destination. Size it
  accordingly; it is the largest item in this section.

- [ ] **U4 · Duration box: AI estimate by default, manual override, placeholder icon** (`R8`)
  `Task.estimatedMinutes` is already nullable-with-fallback and the LLM already
  fills it, so this is a UI item — except for the part that decides *when* a
  hand-typed value wins over a re-estimation, which is **C1**'s call.

- [ ] **U5 · Widget pack** (`R24`)
  No widget exists today. Which widgets, and what each shows, is a design question
  the map should answer (**C12** covers presentation); the build itself is
  ordinary work once that is decided.

- [ ] **U6 · Repeat-tappable fill buttons for quantity tasks** (`R25`)
  250 ml / 500 ml / 750 ml / 1 L for "drink 4 litres a day". **Blocked on C7** —
  what a unit *is* has to be settled first, because these buttons are increments in
  a unit and `Goal.unit` is currently free text.

---

## 🔍 Additions from the agent's own pass — **static half only**

Found by reading the code and the spec, not by using the app. Kept separate from
Ido's items on purpose: these are the agent's, and they have not been seen on a
screen.

- [ ] **A1 · The app has no Hebrew and no RTL support** — `app/src/main/res` holds
  `values` and `values-night` only. Its author writes Hebrew, its goal titles will
  be Hebrew, and Compose will lay them out left-to-right in an English chrome. This
  is not in Ido's document and is arguably the largest product gap in the app.
  Decide it as scope (is this app Hebrew-facing?) before it is priced.

- [ ] **A2 · Nothing in the domain model expresses recurrence** — neither `Task`
  nor `Goal` carries a repeat, cadence, or next-due field. `R18`/`R20` therefore
  are not UI features; they are a data-model change with a Firestore migration
  behind them. Noted here so the map (**C5**) prices it honestly.

- [ ] **A3 · `Goal.unit` is free-text `String`, defaulted to `"%"`** — so nothing
  can validate a unit, convert one, or reason about it, which is exactly why the
  UNIT picker reads as opaque (`R15`). The fix is a domain concept, not a better
  dropdown label. Feeds **C7**.

- [ ] **A4 · Tasks have no archive and goals have no cascade** — `Goal.isArchived`
  exists, `Task` has no equivalent, and deleting a goal leaves its tasks and
  progress behind (already on the FUTURE list as a cascade-delete item). Worth
  seeing as one story rather than two.

> **Still owed: the device half of this pass.** Onboarding and empty states, first-run
> comprehension, tap targets, dark mode across both skins, error and offline states,
> and whether the dashboard's information order matches what a user opens the app
> for. None of that can be assessed from source. It wants the emulator and a
> deliberate walk through the app.
