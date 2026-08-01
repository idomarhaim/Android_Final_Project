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
  - Health Connect (fitness/sleep) — `data/health/HealthConnectManager` stub.
  - Google Tasks import — `data/tasks/GoogleTasksClient` stub.
  - Competitive challenges — model + rules + preview screen exist; create/join/standings logic pending.
  - [x] LLM task→goal classification UI — shipped as the "Smart add a task" card
    on the dashboard; `scoreTask` is wired to the ✨ button on the add-task row.

### 🟢 FUTURE — [`TODO_FUTURE/`](TODO_FUTURE/)
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
