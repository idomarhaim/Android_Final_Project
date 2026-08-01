<!-- SOURCE: user-template v4; do not edit in-project, edit user-level then re-sync -->

# AGENTS.md — GoalPilot

Cross-agent entry point. Read this first. GitHub Copilot also loads `.github/copilot-instructions.md` (a thin pointer to this file).

## 📚 Authoritative docs (link, don't restate)

- **[docs/OPERATIONS.md](docs/OPERATIONS.md) — start here if you are a new session.**
  Live project ids, environment traps, what's left, and how to verify it.
- [SESSIONS.md](SESSIONS.md) — session claim board. **Read before your first edit**
  if anyone else may be working here; claim before your first write.
- [README.md](README.md) — features, quick-start.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — layered design, data model, data flow.
- [docs/SETUP.md](docs/SETUP.md) — Firebase / GROQ / OAuth credentials + debug SHA-1.
- [TODO/TODO.md](TODO/TODO.md) — backlog index (MUST → OPTIONAL → FUTURE).
- [CHANGELOG/CHANGELOG_README.md](CHANGELOG/CHANGELOG_README.md) — one file per date.
- [.github/authoring-instructions.md](.github/authoring-instructions.md) — how to write instruction files.
- [.github/instruction-file-catalog.md](.github/instruction-file-catalog.md) — which instruction file lives where.

## 🗺️ Where things live

- `app/src/main/java/com/idomarhaim/goalpilot/`
  - `core/` — `Resource` wrapper, date utils, Firestore/Storage/Functions constants.
  - `domain/` — models, repository **interfaces**, use cases. **No Android/Firebase types here.**
  - `data/` — Firebase + GROQ implementations (`auth/`, `firestore/`, `storage/`, `remote/`) and integration stubs (`health/`, `tasks/`).
  - `di/` — Hilt modules (`FirebaseModule`, `DispatchersModule`, `RepositoryModule`).
  - `ui/` — `theme/`, reusable `components/`, `navigation/`, `root/` (auth gate + scaffold).
  - `feature/` — one package per screen: `auth`, `goals`, `dashboard`, `social`, `profile`, `analytics`, `challenges`.
- `functions/` — GROQ proxy Cloud Functions (TypeScript).
- `firestore.rules`, `storage.rules`, `firebase.json`, `.firebaserc` — backend config.
- `scripts/` — one-click launchers (emulator/phone → build → install → launch) so
  the project never has to be opened in Android Studio. See `scripts/README.md`.
- `docs/`, `TODO/`, `CHANGELOG/` — documentation & backlog.

## 🔧 Common commands

```powershell
# One-click: device up -> build -> install -> launch. Sets JAVA_HOME itself.
.\scripts\run-goalpilot.ps1             # phone if plugged in, else the emulator
.\scripts\run-goalpilot.ps1 -Target device -Logcat
.\scripts\run-goalpilot.ps1 -Target emulator -SkipInstall   # just boot the AVD

# JDK 21 is pinned in gradle.properties; set it if calling gradlew from a fresh shell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"

.\gradlew :app:assembleDebug            # build the APK
.\gradlew :app:testDebugUnitTest        # JVM unit tests
.\gradlew :app:installDebug             # install on device/emulator
.\gradlew :app:connectedDebugAndroidTest # instrumented tests (needs emulator)

# Cloud Functions
cd functions; npm install; npm run build
firebase emulators:start                # local Auth/Firestore/Storage/Functions
firebase deploy --only firestore:rules,storage,functions
```

## ⚠️ Pitfalls

- **JDK:** the machine's `JAVA_HOME` is JDK 25, which AGP rejects. Gradle is pinned to JDK 21 via `org.gradle.java.home` in `gradle.properties`. Don't remove that line on this machine.
- **`google-services.json` is a placeholder.** The app compiles but sign-in/Firestore only work after you drop in the real file — see [docs/SETUP.md](docs/SETUP.md).
- **Web client id** comes from `local.properties` → `GOOGLE_WEB_CLIENT_ID` → generated `R.string.gp_web_client_id`. Do **not** reference `R.string.default_web_client_id` (only exists with a real json and would clash).
- **Firestore DTOs** must stay mutable (`var`) with defaults + `@DocumentId` for reflective (de)serialization. Keep the domain models immutable and separate.
- **Windows file locks:** KSP/`.gradle` occasionally fail with "Could not delete/move …". Re-run, or `rm -rf app/build/generated/ksp` and rebuild.
- **The emulator is an exclusive singleton** ([SESSIONS.md](SESSIONS.md)). Two sessions driving one AVD corrupts its quickboot snapshot and yields `InstallException: device offline`. `scripts/run-goalpilot.ps1` enforces this — it claims the AVD for the duration of a run and refuses if another session holds it. Never blanket-kill `qemu-system-x86_64` to unstick an emulator; that takes down other sessions' AVDs too. Use `-Recover`, which is scoped to one AVD.
- **GROQ key never ships in the app** — it lives only in `functions/.env` (spec §5). The client always has a local fallback (spec §8).
- **The GROQ model id rots.** GROQ retires models on a rolling schedule and a retired id fails *silently* — the client just serves local fallback tips, so the AI looks bland rather than broken. `DEFAULT_MODEL` in `functions/src/index.ts` is the single pin; check it against <https://console.groq.com/docs/deprecations> before any demo.
- **Instrumented tests need Espresso ≥ 3.7.0** on modern emulators. 3.6.1 reflects on the private `InputManager.getInstance()`, removed in Android 15+, and every test dies with `NoSuchMethodException` before its body runs.
- **Never re-enable Material You dynamic colour.** `GoalPilotTheme` used to take
  `dynamicColor = true`, which meant the wallpaper decided every colour and the
  brand palette was dead code on Android 12+. Colour now comes from the selected
  `AppSkin` (`ui/theme/Palettes.kt`); the two cannot coexist.
- **Colours are test-guarded.** `ThemePaletteTest` asserts WCAG contrast for all
  four skin/brightness schemes and pairwise distinctness of the `GoalCategory`
  palette. Editing a hex without running `:app:testDebugUnitTest` will bite.
- **Repository snapshot flows must be built on `FirebaseAuth.uidFlow()`** (`data/auth/AuthExt.kt`), never a one-shot `auth.currentUser` read — a `Flow` is constructed at ViewModel-creation time, so a one-shot read pins the account that was signed in then.

## 🧱 Conventions

- Generic conventions (English, changelogs, docs, TODOs, safety) live in `.github/instructions/general.instructions.md`.
- Kotlin/Compose/Firebase rules live in `.github/instructions/android.instructions.md`.
- Test layering lives in `.github/instructions/testing.instructions.md`.
- Architecture: **Clean-ish layering** — `feature (Compose + ViewModel)` → `domain (interfaces + models)` → `data (Firebase/GROQ)`, wired by Hilt. ViewModels expose a single `StateFlow<UiState>`; screens are stateless and driven by lambdas from the nav graph.

## 🤖 Model routing & agent topology

- **Route by decision density & blast radius:** frontier models (Fable/Opus-class) for architecture, planning, cross-cutting refactors, hairy debugging, review; mid tier (Sonnet-class) for well-specced implementation; fast tier (Haiku/Flash-class) for mechanical bulk edits and summarization. Escalate a tier after repeated failure — never loop a flailing cheap model.
- **Topology ladder:** one agent, one session (default) → split sessions at verification/commit boundaries (handoff files + committed docs, never chat memory) → subagents only for read-only fan-out or truly independent parallel chunks.
- **Concurrent sessions — claim by path, or don't run them at once.** Two live sessions in one working tree is **opt-in, not the default**, and legitimate only when the working sets are disjoint. Board: [SESSIONS.md](SESSIONS.md) — read before your first edit, claim before your first write, release when done. Claim **paths**, not themes. Singletons here are exclusive: the Gradle daemon, the git index, emulator `Pixel_10_Pro_XL`, and the live Firebase project `goalpilot-56e30`; **never `git add -A`** beside a live sibling session. The agent recommends, the user assigns: append `🧭 **Claim:** <candidate> → owns <paths>; conflicts: <none|paths>` and wait — never self-assign. Entry point `/claim`. Rule: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.
- **Subagent gate:** before spawning ANY subagent — explain why, which model, why that model, and get user approval (no read-only exemption).
- **Derivable decisions:** when committed principles fully determine a decision's answer, proceed and log "decision X taken per principle Y" instead of asking. ALWAYS ask for: deletions, outward-facing actions, relocating/rewriting anything that predates JARVIS. Rule: `C:\Dev\JARVIS\rules\derivable-decision.md`.
- **Surfaces:** Copilot = inline completions, quick in-editor edits, PR review; Claude Code = orchestrated multi-step local work; otherwise token economics decide.
- Full policy: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md`.

## 🔒 Frozen / off-limits

- `GoalPilot_spec_EN.docx` — the course spec; do not edit.
- `gradle/wrapper/gradle-wrapper.jar` and `gradlew*` — generated wrapper; regenerate via `gradle wrapper`, don't hand-edit.
