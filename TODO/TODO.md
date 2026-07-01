# GoalPilot — Backlog

Index of outstanding work, in priority order. Detailed items live in the
`TODO_MUST/`, `TODO_OPTIONAL/`, and `TODO_FUTURE/` subfolders as
`<Area>.TODO.<priority>.md`.

## 🔴 MUST (before the app is "done")
- **Wire real credentials** and verify end-to-end on a device — see [docs/SETUP.md](../docs/SETUP.md).
  Firebase project, `google-services.json`, `GOOGLE_WEB_CLIENT_ID`, debug SHA-1,
  GROQ key, deploy rules + functions.

## 🟡 OPTIONAL (nice-to-have / bonus tiers — architected, not fully built)
- [Integrations.TODO.optional.md](TODO_OPTIONAL/Integrations.TODO.optional.md)
  - Health Connect (fitness/sleep) — `data/health/HealthConnectManager` stub.
  - Google Tasks import — `data/tasks/GoogleTasksClient` stub.
  - Competitive challenges — model + rules + preview screen exist; create/join/standings logic pending.
  - LLM task→goal classification UI (function + client are ready; add an "auto-sort task" action).

## 🟢 FUTURE (polish / production hardening)
- Move points/level computation to a Firestore-triggered Cloud Function (anti-cheat).
- Cascade-delete a goal's tasks/progress (Cloud Function or batched client delete).
- Migrate Google Sign-In → Credential Manager + Sign in with Google.
- Replace `createdAt` client timestamps with `@ServerTimestamp`.
- Instrumented E2E tests against the Firebase Emulator Suite.
