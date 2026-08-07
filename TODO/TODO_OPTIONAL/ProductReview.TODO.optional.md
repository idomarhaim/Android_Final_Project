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

> ✅ **Reproduced on a device, 2026-08-06** (`CHANGELOG/2026-08-06/product-device-pass.md`).
> Every `D` below now carries a verdict taken against a real debug build on
> `Pixel_10_Pro_XL`, signed in as Ido's own account with his real data — not a
> reading of the source. `D2`, `D3`, `D4`, `D5` are **confirmed**; `D1` is
> **not a defect** and moves to the decision backlog. The four confirmed ones have
> graduated to GitHub issues, which are now the place their state lives: per
> `/triage`, graduating is **one-way**, so the issue number is recorded here and
> issue state is never mirrored back into this file.

---

## 🐞 Defects

- [ ] **D1 · ~~Shared challenge does not sync with my tasks or Health Connect~~ → not a defect, it is an undecided model** (`R1`)
  **Do not open this as a bug.** The symptom is exactly as reported, and it is not
  a wiring that broke — it is a wiring that was **never specified**, so building one
  now would pick an answer by accident. Established 2026-08-06, statically and then
  on the device:
  - `ChallengeParticipant.score` has **exactly one writer in the whole codebase**:
    `ChallengeRepositoryImpl.reportScore`, reached only from the manual "Report
    score" dialog. Join initialises it to `0.0`; nothing else ever touches it.
  - `ChallengeType` — which carries `RUNNING`, `STEPS`, `SLEEP`, `WORKOUTS` — is
    **purely presentational**. Its only three uses are an icon (`ChallengesScreen`
    `iconFor`), a display label, and a default `metricUnit` **string**
    (`ChallengesViewModel.defaultUnitFor`). No code branches on it to source a score.
  - `SyncHealthDataUseCase` writes a `ProgressEntry` against a **`Goal`**. It does
    not import, inject, or mention challenges anywhere.
  - Device evidence: Ido's own **"August Steps Race"** is a `STEPS` challenge
    reading **"#2 · 0 steps"** with "Report score" as the only way to move it —
    while his steps are flowing into goals through Health Connect. Two parallel
    representations of the same walk, joined by nothing.

  So the open question is **what a challenge should score from** — a goal's
  progress, a task count, a raw health metric, or a free number — and that cannot
  be answered without `C7` (what a *unit* is), because `Challenge.metricUnit` is
  free text with the identical disease as `Goal.unit` (**A3**).
  **→ belongs in `TODO_FUTURE/ProductModel.TODO.future.md` as a new decision**,
  alongside `C7`. **Not moved, by Ido's call on 2026-08-07**: it goes to the
  concurrent `product-model-map` session as a **candidate `C14`**, to be charted
  with the other thirteen rather than bolted on afterwards. That session owns
  `TODO_FUTURE/`, so this session did not write there.

  <!-- HANDOFF: product-model-map may lift the block below verbatim into
       ProductModel.TODO.future.md. Reproduced on a device 2026-08-06; the
       evidence is above. -->

  ```markdown
  ## C14 · What should a challenge score from?

  Reported by Ido as a defect (`R1`: "a shared CHALLENGE does not sync with my
  tasks or with my Health Connect"), reclassified 2026-08-06 after reproducing it
  on a device: nothing is broken, the wiring was never specified.

  `ChallengeParticipant.score` has exactly one writer — `reportScore`, from a
  manual dialog. `ChallengeType` (`RUNNING`/`STEPS`/`SLEEP`/`WORKOUTS`) is purely
  presentational: an icon, a label, and a default `metricUnit` string. Nothing
  branches on it. `SyncHealthDataUseCase` writes a `ProgressEntry` against a
  **`Goal`** and never mentions challenges.

  **The decision:** does a challenge's score come from (a) a linked goal's
  progress, (b) a count of completed tasks, (c) a raw Health Connect metric,
  (d) a free number the user reports, or (e) a choice per `ChallengeType`?

  **Blocked on `C7`.** `Challenge.metricUnit` is free text with the identical
  disease as `Goal.unit`, so "8200 steps" and "8200" are the same value to the
  app. Whatever `C7` decides a unit *is* has to cover challenges too, or the two
  drift apart.

  **Also decide with it:** anti-cheat. If a score can be typed, it can be typed
  dishonestly — the existing server-side-scoring item in TODO → FUTURE should
  move points and challenge scores together.

  Evidence: `D1` in `TODO/TODO_OPTIONAL/ProductReview.TODO.optional.md`;
  `CHANGELOG/2026-08-06/product-device-pass.md`.
  ```

- [ ] **D2 · No route from a life area into its goals** (`R2`) — **CONFIRMED on device → [#2](https://github.com/idomarhaim/Android_Final_Project/issues/2)**
  `feature/lifeareas/LifeAreasScreen.kt` contains no navigation call at all — the
  areas screen is a management surface (rename, reorder, propose), and nothing on
  it is a way in to the goals it groups. The goals list is already banded by area
  (`lifearea-polish`, 2026-08-04), so the destination exists; only the entry point
  is missing.
  **Device evidence:** tapping the row body and tapping the "3 goals" count both
  leave the screen pixel-identical. The accessibility tree says why exactly — per
  area row the *only* nodes with `clickable="true"` are the two icon buttons
  `Edit <area>` and `Delete <area>`. The row, the area name and the goal count
  carry no click handler, so **the count reads as a link and is a label**.
  Touches: `feature/lifeareas/LifeAreasScreen.kt`, `ui/navigation/Destinations.kt`,
  `feature/goals/GoalsScreen.kt` (needs to accept an area to scroll/filter to).

- [ ] **D3 · Completing a task takes too long to show the checkbox and the graph** (`R13`) — **CONFIRMED and measured → [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)**
  **~2 seconds, and the guess in the original note was right but not sharp enough.**
  Measured from 60 fps screen recordings (`screenrecord` emits a frame only when
  the screen changes, so the gaps below are real dead time, not sampling):

  | | tap → checkbox changes | dead screen after the ripple ends |
  |---|---|---|
  | sample 1 (complete) | **2.24 s** | 1.20 s |
  | sample 2 (un-complete) | **1.94 s** | 0.88 s |

  The goal's own number moves at the same moment, not before: the donut read
  `2 / 100 %` right up to the last frame of the first burst and `3 / 100 %` on the
  first frame of the second. **The only feedback in those two seconds is the
  Material ripple, which itself finishes in ~0.7 s** — after that the screen is
  completely still, with the box still empty.

  **Root cause, and it is worse than a plain round-trip.** `TaskRepositoryImpl.setDone`
  is a **`firestore.runTransaction`** (it reads task + user + goal, then writes all
  three). A Firestore transaction is **server-only** — it cannot be served from the
  offline cache — so the UI is waiting on a full server read-then-write, and there
  is no local write to render early. On top of that
  `GoalDetailViewModel.toggleTask` is one line that **discards the `Resource` it
  gets back**:
  ```kotlin
  fun toggleTask(task: Task) {
      viewModelScope.launch { taskRepository.setDone(task.id, !task.isDone) }
  }
  ```
  — no `isSubmitting`, no error branch, unlike `logProgress` directly beneath it.
  So there is neither an optimistic update nor a failure path. **This is the same
  defect as `A5`** (offline it does nothing at all) and the two must be fixed
  together: adding an optimistic update *alone* would turn `A5` from a silent
  no-op into a silent lie.
  Touches: `feature/goals/GoalDetailViewModel.kt`, `data/firestore/TaskRepositoryImpl.kt`
  (`setDone`), `ui/components/ChartAnimation.kt`.

- [ ] **D4 · A shared photo cannot be opened** (`R27a`) — **CONFIRMED on device → [#4](https://github.com/idomarhaim/Android_Final_Project/issues/4)**
  `feature/social/SocialScreen.kt:269-271` renders the image with `AsyncImage`
  inside the feed card and attaches no click handler — there is no full-screen or
  zoom destination for it to open into.
  **Device evidence:** tapping the photo in Ido's own feed post leaves the screen
  **pixel-identical** below the status bar (PSNR ∞, MSE 0.00 against the frame
  before the tap). The accessibility tree has **no interactive node anywhere in the
  feed card** — the last `clickable="true"` node on the screen is the "Challenges"
  link above the feed. **Also**: the image carries **no content description**, so
  it is not merely un-openable, it is invisible to a screen reader — fix both in
  one pass.
  Touches: `feature/social/SocialScreen.kt`.

- [ ] **D5 · A user cannot delete a share they made** (`R27b`) — **CONFIRMED on device → [#5](https://github.com/idomarhaim/Android_Final_Project/issues/5)**
  **Device evidence:** on Ido's own post there is no overflow menu, no delete icon
  and no context menu — a 1.2 s long-press on the card leaves the screen
  **pixel-identical**, and the accessibility tree shows **zero interactive nodes in
  the entire feed card**. There is nothing to remove a share with, on your own post
  or anyone's.
  **Not UI-only.**
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

- [ ] **U1 · Smart add should not ask for approval every time** (`R3`) → [#6](https://github.com/idomarhaim/Android_Final_Project/issues/6)
  Today `SmartAddCard` always opens `SmartAddDialog` for confirmation
  (`feature/dashboard/DashboardScreen.kt:180,239`). Wanted: a preference —
  default **off**, i.e. file it silently — with the dialog kept as the opt-in.
  `AppPreferencesRepository` already exists to hold it. Pairs naturally with **U3**:
  silent filing is only safe if the user is told when the sorter improvised.

- [ ] **U2 · Complete a task from within quick add** (`R6`) → [#7](https://github.com/idomarhaim/Android_Final_Project/issues/7)
  A task you are entering because you already did it should not need a second trip
  to the goal detail screen.

- [ ] **U3 · Notify when the sorter invents a new goal** (`R5`) → [#8](https://github.com/idomarhaim/Android_Final_Project/issues/8)
  In-app **and** a system notification. The app currently ships no notification
  channel at all, so this item carries the whole notification substrate with it —
  channel, permission request (API 33+), and a tap-through destination. Size it
  accordingly; it is the largest item in this section.

- [ ] **U4 · Duration box: AI estimate by default, manual override, placeholder icon** (`R8`) → [#9](https://github.com/idomarhaim/Android_Final_Project/issues/9)
  `Task.estimatedMinutes` is already nullable-with-fallback and the LLM already
  fills it, so this is a UI item — except for the part that decides *when* a
  hand-typed value wins over a re-estimation, which is **C1**'s call.

- [ ] **U5 · Widget pack** (`R24`) → [#10](https://github.com/idomarhaim/Android_Final_Project/issues/10)
  No widget exists today. Which widgets, and what each shows, is a design question
  the map should answer (**C12** covers presentation); the build itself is
  ordinary work once that is decided.

- [ ] **U6 · Repeat-tappable fill buttons for quantity tasks** (`R25`) → [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11)
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

## 🔍 Additions from the agent's own pass — **device half**, 2026-08-06

Found by driving a real debug build on `Pixel_10_Pro_XL` signed in as Ido, not by
reading. Still the agent's rather than Ido's. Per the session brief these are
**recorded, not filed** — the only things that graduated to issues are the
confirmed `D` defects and `U1`–`U6`. `A5` is the exception and rides inside `D3`'s
issue, because they are one defect with two faces.

- [ ] **A5 · Completing a task while offline does nothing at all, and says nothing** → filed inside [#3](https://github.com/idomarhaim/Android_Final_Project/issues/3)
  Same root cause as **D3**, and the more serious half. Tap the checkbox with the
  network down and you get the ripple and **nothing else** — box still empty, title
  not struck through, points unchanged, no snackbar, no toast, no error. The screen
  is unchanged 60 s later. Logcat shows Firestore failing the write
  (`UNAVAILABLE … UnknownHostException: firestore.googleapis.com`), so the app knew.
  Two reasons it is silent, and both need fixing:
  1. `setDone` is a **`runTransaction`**, which is server-only — offline it cannot
     even apply locally, so unlike an ordinary `set()`/`update()` there is no cached
     write for the snapshot listener to render.
  2. `GoalDetailViewModel.toggleTask` **throws the `Resource` away**, so the
     `Resource.Error` that comes back is consumed by nobody.

  A user walking through a tunnel ticks four tasks, sees nothing happen, and has no
  way to tell whether the app is slow or broken. Verified as the *only* silently
  swallowed write? **No** — `deleteTask` on the line below has the same shape, and
  the other repositories were not audited. Worth a sweep when this is fixed.

- [ ] **A6 · The app never says it is offline, and cold-starts looking perfectly live**
  Force-stopped and relaunched with the network down, the dashboard renders in full
  from Firestore's local cache — "70 pts", "24% overall", "7 goals", "5 tasks done",
  the goal list, the feed — with **no banner, no badge, no "last updated", nothing**.
  Offline persistence is doing its job; the problem is that the UI presents cached
  numbers and live numbers identically, so a stale figure is indistinguishable from
  a current one. Pairs with **A5**: together they mean a user offline sees confident
  numbers *and* silently loses their writes.

- [ ] **A7 · The dashboard answers "how am I doing?" and never "what do I do now?"**
  In scroll order the home screen is: points/level banner → overall-progress donut →
  Smart add → **Import from Google Tasks** → **Connect Health Connect** → five
  generic AI-coach tips → *then* "Your goals" → share card. Ido's own goals are
  roughly **four screenfuls down**, behind two one-time setup cards that never
  retire and five tips that are not about his goals. And **no task appears on the
  dashboard at all** — not today's, not overdue, not next. Someone opening a goal
  app at 8 a.m. wants the list of what to do; the current order gives them a score.
  This is the `R24` widget question in miniature and should be decided with it.

- [ ] **A8 · One tap target below the 48 dp minimum**
  Every `clickable` node on Home, Goals, goal detail, Social and Challenges was
  measured from the accessibility tree against 48 dp (144 px at this device's
  480 dpi). **57 of 58 pass.** The one that fails is the **⋮ overflow on a row under
  "Goals with no area"** on the Life areas screen: `144 × 113 px` = **48 × 38 dp**,
  10 dp short vertically. Small, mechanical, and the kind of thing that never gets
  found by looking.

- [ ] **A9 · The shared photo has no content description** → filed inside [#4](https://github.com/idomarhaim/Android_Final_Project/issues/4)
  Recorded separately from **D4** because it survives D4's fix: making the image
  tappable does not give it a label. The Social screen's only content descriptions
  are "Add friend", "Remove friend" and the two leaderboard avatars; the posted
  image has none, so a screen reader announces the post with the picture missing.

### Checked and **not** defects — recorded so they are not re-raised

- **Both skins in dark mode are fine.** Aurora and Blossom were each walked in dark
  across Home, Goals, goal detail and Profile: text legible, progress bars visible,
  selected-nav pills readable, no invisible-on-invisible. Consistent with
  `ThemePaletteTest` asserting WCAG contrast for all four skin/brightness schemes.
  The goal-category icon colours stay constant across skins — that is deliberate
  (`AGENTS.md`: the `GoalCategory` palette is pairwise-distinct and test-guarded),
  not a skin bug.
- **The AI coach is live, not falling back.** The tips shown were "Start Small",
  "Track Your Wins", "Set SMART Goals", "Celebrate Progress". The local fallback in
  `RecommendationRepositoryImpl` can only ever emit "Start with one goal", "Keep the
  streak alive" or "Nudge: {goal}", so these came from the model — i.e. the GROQ
  model id in `functions/src/index.ts` has **not** rotted as of 2026-08-06. That is
  the check `AGENTS.md` demands before any demo, and it passes today.
- **The FAB does not cover the last card.** It overlaps mid-scroll, which is normal
  Material behaviour; at full scroll both the Goals and Life areas lists have
  enough bottom padding to clear it.
- **Hebrew content is already real in the app**, which bears on **A1** without
  changing its scope-decision status: the life areas are `בריאות`, `לימודים`,
  `קריירה`, `זוגיות`, tasks are titled in Hebrew (`אימון ריצה`), and the profile
  name is Hebrew. So A1 is not hypothetical — Hebrew strings are being laid out
  left-to-right in English chrome **today**, on Ido's own account.

- [ ] **A10 · A cold start with no cache is a blank screen and an 8-pixel dot**
  Found 2026-08-07 by `pm clear`-ing the app and signing back in — i.e. exactly
  what a user gets on a **new phone, a reinstall, or after clearing data**. Between
  sign-in completing and Firestore's first snapshot arriving, the dashboard is
  **empty**: the word "GoalPilot", the bottom nav, and a **single ~8 px blue dot**
  in the middle of an otherwise blank page. It held that state for ~10 seconds on
  a healthy connection.

  Nothing tells the user the app is fetching their data, and at that size the dot
  reads as a rendering artefact rather than a spinner. The same undersized
  indicator is used on the sign-in button. A skeleton, or simply a normally-sized
  spinner with a line of text, would cost very little.

  Note this is **not** the same as the empty state for a genuinely new account —
  see the box below.

> **Still owed: the true zero-data empty states.** `pm clear` was run with Ido's
> approval on 2026-08-07 and **did not** reach them, exactly as expected: signing
> back in restores every goal from Firestore, so the app was full again within
> seconds. What it *did* buy is `A10` above — the cold, cacheless first load.
>
> Every feature screen has an empty-state branch in code (Analytics, Challenges,
> Dashboard, AddEditGoal, GoalDetail ×2, LifeAreas, Social ×2), so the states
> exist; what has never been *read on a screen* is their copy, for a user with
> nothing at all. That needs a **throwaway Google account**, which is the only
> remaining route.
>
> The sign-in screen itself was seen twice: one sentence of explanation, a single
> "Sign in with Google" button, no preview, no way past it, and a ~4 px dot as its
> in-progress state.

> ⚙️ **Environment note, not a product finding.** Immediately after `pm clear`,
> Google Play Services on the emulator wedged: `SignInActivity` took focus and
> rendered nothing — a scrimmed screen and an empty accessibility tree — through
> two full retries and ~90 s of waiting, while logcat showed GMS cold-compiling its
> Chimera modules. `adb shell am force-stop com.google.android.gms` cleared it
> immediately and sign-in then worked first time. **This is the emulator, not
> GoalPilot** — recorded so the next session that clears app data does not spend
> twenty minutes diagnosing the app.
