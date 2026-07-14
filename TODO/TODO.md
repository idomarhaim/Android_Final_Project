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
- **Wire real credentials** and verify end-to-end on a device — see [docs/SETUP.md](../docs/SETUP.md).
  Firebase project, `google-services.json`, `GOOGLE_WEB_CLIENT_ID`, debug SHA-1,
  GROQ key, deploy rules + functions.

### 🟡 OPTIONAL — [`TODO_OPTIONAL/`](TODO_OPTIONAL/)
- [Integrations.TODO.optional.md](TODO_OPTIONAL/Integrations.TODO.optional.md)
  - Health Connect (fitness/sleep) — `data/health/HealthConnectManager` stub.
  - Google Tasks import — `data/tasks/GoogleTasksClient` stub.
  - Competitive challenges — model + rules + preview screen exist; create/join/standings logic pending.
  - LLM task→goal classification UI (function + client are ready; add an "auto-sort task" action).

### 🟢 FUTURE — [`TODO_FUTURE/`](TODO_FUTURE/)
- Move points/level computation to a Firestore-triggered Cloud Function (anti-cheat).
- Cascade-delete a goal's tasks/progress (Cloud Function or batched client delete).
- Migrate Google Sign-In → Credential Manager + Sign in with Google.
- Replace `createdAt` client timestamps with `@ServerTimestamp`.
- Instrumented E2E tests against the Firebase Emulator Suite.

## ✍️ Conventions
- When you complete a task, **wait for user confirmation** before flipping `[ ]` → `[x]`.
- Closing a task requires a matching entry in today's `CHANGELOG/YYYY-MM-DD.md`.
- Adding a new task: place the file under the matching priority subfolder (`TODO_MUST/`, `TODO_OPTIONAL/`, `TODO_FUTURE/`); create a new `<Area>.TODO.<priority>.md` if no area fits, and link it from this index.
