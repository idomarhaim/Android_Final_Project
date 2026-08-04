<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 🗂️ TODO Index — GoalPilot

Backlog index. Per-area files live under three priority subfolders next to this index:

- `TODO_MUST/` — `<Area>.TODO.must.md`
- `TODO_OPTIONAL/` — `<Area>.TODO.optional.md`
- `TODO_FUTURE/` — `<Area>.TODO.future.md`

## 🚦 Priority order
1. Finish every `must` item across all areas first.
2. Then tackle `optional` items.
3. `future` is roadmap — only worked on after explicit user go-ahead.

## ✅ Status legend
`[ ]` todo · `[~]` in-progress · `[x]` done · `[-]` deferred

## 📋 Per-area files

### 🔴 MUST — [`TODO_MUST/`](TODO_MUST/)
- [x] **Wire real credentials** and verify end-to-end on a device — done 31/07/2026.
  Firebase project `goalpilot-56e30` on Blaze; Firestore + Storage + auth + rules
  + all three Cloud Functions live in `us-central1`. Verified on the emulator:
  sign-in → goal → task → completion transaction → photo upload → GROQ, each
  confirmed against Firestore/Storage/function logs rather than the UI. See
  `CHANGELOG/2026-07-31.md`.
- [Submission.TODO.must.md](TODO_MUST/Submission.TODO.must.md) — the two items
  that block handing the project in. Both are largely **manual**: an agent cannot
  sign into Google as a second account, nor supply your name/ID/course number.
  - [ ] **Demo the sharing requirement (spec §7) with two Google accounts.**
    Both accounts are already OAuth test users; friend code `NDXVJC` makes the
    add-friend step a 6-character type-in.
  - [ ] **Fill in the spec title page** — it still reads
    `[Full name & ID] · [Course number]`.

### 🟡 OPTIONAL — [`TODO_OPTIONAL/`](TODO_OPTIONAL/)
- [Integrations.TODO.optional.md](TODO_OPTIONAL/Integrations.TODO.optional.md)
  - [x] **Health Connect (fitness/sleep)** — shipped 02/08/2026 as the "Sync
    health data" card. Read-only, review-before-write, dedupe via
    `ProgressEntry.sourceKey`. One follow-up: verify on a **physical phone with
    real step data** — the emulator's Health Connect store is empty, so the
    write path has never run against real readings.
  - [x] Google Tasks import — shipped 31/07/2026.
  - [ ] **Competitive challenges** — model + rules + preview screen exist;
    create/join/standings logic pending. **The last remaining §6 nice-to-have.**
  - [x] LLM task→goal classification UI — shipped as the "Smart add a task" card
    on the dashboard; `scoreTask` is wired to the ✨ button on the add-task row.
  - [x] **Life areas + time-allocation analytics** — shipped **and verified**;
    closed 04/08/2026 on your confirmation, with every follow-up done. What
    shipped: user-defined areas synced from Google Tasks list names, LLM duration
    estimates on tasks, and the interactive "Where your time goes" donut at
    day / week / month / quarter / year. See `CHANGELOG/2026-08-03/lifeareas.md`.
    The four follow-ups it deliberately left out, all landed 04/08/2026:
    - [x] **Reordering life areas** — drag handle plus accessibility actions,
      writing only the `sortOrder` values that actually moved.
      See `CHANGELOG/2026-08-04/lifearea-polish.md`.
    - [x] **Show the life area on the goals list** — the list is banded by area
      rather than carrying another chip per row.
      See `CHANGELOG/2026-08-04/lifearea-polish.md`.
    - [x] **Back-fill durations for old tasks** — a "Re-estimate N durations"
      action on the analytics card, capped at 15 per run like the Tasks import,
      review-before-write. Verified against the live model: it returned 105
      minutes for a five-word title, which neither the client heuristic (ceiling
      60 for five words) nor the Cloud Function's flat 30 can produce, and the
      card moved from "1 of 2 durations estimated by AI" to "all 2".
      See `CHANGELOG/2026-08-04/time-insights.md`.
    - [x] **A trend chart over time** — "How it moves", a stacked column per
      bucket, with buckets that tile the selected range exactly (days in a week,
      weeks in a month or quarter, months in a year, four-hour blocks in a day).
      See `CHANGELOG/2026-08-04/time-insights.md`.

### 🟢 FUTURE — [`TODO_FUTURE/`](TODO_FUTURE/)
- **Bump the build toolchain, then take Health Connect `1.1.0` stable.** The app
  is pinned to `connect-client:1.1.0-beta01` purely because stable `1.1.0` (and
  every `1.1.0-rc*`) requires **compileSdk 36 + AGP 8.9.1+**, while this project
  is on compileSdk 35 / AGP 8.7.3 / Gradle 8.10.2. The three move together —
  AGP 8.9.1 also needs Gradle 8.11.1+. SDK Platform 36 is already installed on
  this machine, so the blocker is risk, not tooling: it is a whole-build change
  and was deliberately kept out of the Health Connect feature.
- Move points/level computation to a Firestore-triggered Cloud Function (anti-cheat).
- Cascade-delete a goal's tasks/progress (Cloud Function or batched client delete).
- **Migrate Google Sign-In → Credential Manager + Sign in with Google.** This is
  now the *only* remaining source of deprecation warnings in the build
  (9 warnings from `data/auth/GoogleAuthClient.kt`). Google has said the legacy
  `com.google.android.gms.auth.api.signin` package will be removed from
  play-services-auth "in a future release". **Its stated blocker is now gone** —
  a real Firebase project exists and legacy sign-in is verified working against
  it, so the migration can be attempted and regression-tested whenever you give
  the go-ahead. Still `future` tier: it replaces the entire sign-in flow, which
  is currently the one part of the app proven end-to-end.
- Replace `createdAt` client timestamps with `@ServerTimestamp`.
- Instrumented E2E tests against the Firebase Emulator Suite.

## ✍️ Conventions
- When you complete a task, **wait for user confirmation** before flipping `[ ]` → `[x]`.
- Closing a task requires a matching entry in today's `CHANGELOG/YYYY-MM-DD.md`.
- Adding a new task: place the file under the matching priority subfolder (`TODO_MUST/`, `TODO_OPTIONAL/`, `TODO_FUTURE/`); create a new `<Area>.TODO.<priority>.md` if no area fits, and link it from this index.
