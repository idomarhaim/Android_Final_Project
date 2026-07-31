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
- [ ] **Wire real credentials** and verify end-to-end on a device — see [docs/SETUP.md](../docs/SETUP.md).
  Firebase project (**Blaze plan required** — Functions *and* Storage),
  `google-services.json`, `GOOGLE_WEB_CLIENT_ID`, debug SHA-1, GROQ key,
  deploy rules + functions. *(Firebase CLI is now installed locally.)*
- [ ] **Demo the sharing requirement (spec §7) with two Google accounts** — both
  added as OAuth test users. Friend codes now make this a 6-character type-in.
- [ ] **Fill in the spec title page** — it still reads `[Full name & ID] · [Course number]`.

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
  play-services-auth "in a future release". Deliberately left for after the
  credentials are wired: it replaces the whole sign-in flow, and it cannot be
  verified until there is a real Firebase project to sign in against.
- Replace `createdAt` client timestamps with `@ServerTimestamp`.
- Instrumented E2E tests against the Firebase Emulator Suite.

## ✍️ Conventions
- When you complete a task, **wait for user confirmation** before flipping `[ ]` → `[x]`.
- Closing a task requires a matching entry in today's `CHANGELOG/YYYY-MM-DD.md`.
- Adding a new task: place the file under the matching priority subfolder (`TODO_MUST/`, `TODO_OPTIONAL/`, `TODO_FUTURE/`); create a new `<Area>.TODO.<priority>.md` if no area fits, and link it from this index.
