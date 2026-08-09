<!-- SOURCE: user-template v15; do not edit in-project, edit user-level then re-sync -->

# AGENTS.md — GoalPilot

Cross-agent entry point. Read this first. GitHub Copilot also loads `.github/copilot-instructions.md` (a thin pointer to this file).

## 📚 Authoritative docs (link, don't restate)

- **[docs/OPERATIONS.md](docs/OPERATIONS.md) — start here if you are a new session.**
  Live project ids, environment traps, what's left, and how to verify it.
- **[SESSIONS.md](SESSIONS.md) — session claim board. If this file exists, read it
  before your first edit.** Not "if someone else may be working here": whether
  they are is what the board tells you. Claim before your first write.
- [README.md](README.md) — features, quick-start.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — layered design, data model, data flow.
- [docs/SETUP.md](docs/SETUP.md) — Firebase / GROQ / OAuth credentials + debug SHA-1.
- [TODO/TODO.md](TODO/TODO.md) — backlog index (MUST → OPTIONAL → FUTURE).
- [CHANGELOG/CHANGELOG_README.md](CHANGELOG/CHANGELOG_README.md) — index of days. Entries live at `CHANGELOG/YYYY-MM-DD/<session-label>.md`: **one folder per day, one file per session**, so parallel sessions never write the same file. `SUMMARY.md` in a day folder is written by the first session of a later day.
- [.github/authoring-instructions.md](.github/authoring-instructions.md) — how to write instruction files.
- [.github/instruction-file-catalog.md](.github/instruction-file-catalog.md) — which instruction file lives where.

## 🗺️ Where things live

- `app/src/main/java/com/idomarhaim/goalpilot/`
  - `core/` — `Resource` wrapper, date utils, Firestore/Storage/Functions constants.
  - `domain/` — models, repository **interfaces**, use cases. **No Android/Firebase types here.**
  - `data/` — Firebase + GROQ implementations (`auth/`, `firestore/`, `storage/`, `remote/`) plus the Google Tasks (`tasks/`) and Health Connect (`health/`) integrations.
  - `di/` — Hilt modules (`FirebaseModule`, `DispatchersModule`, `RepositoryModule`).
  - `ui/` — `theme/`, reusable `components/`, `navigation/`, `root/` (auth gate + scaffold).
  - `feature/` — one package per screen: `auth`, `goals`, `dashboard`, `social`, `profile`, `analytics`, `lifeareas`, `challenges`.
- `functions/` — GROQ proxy Cloud Functions (TypeScript).
- `firestore-tests/` — security-rules tests (`@firebase/rules-unit-testing`) against the local emulator. The **only** layer that can test `firestore.rules`; the Kotlin suites cannot reach them.
- `firestore.rules`, `storage.rules`, `firebase.json`, `.firebaserc` — backend config.
- `scripts/` — one-click launchers (emulator/phone → build → install → launch) so
  the project never has to be opened in Android Studio. See `scripts/README.md`.
- `docs/`, `TODO/`, `CHANGELOG/` — documentation & backlog.
- **`docs/pre-injested-docs/` — Ido's own source documents, as he wrote them**
  (`.docx`, Hebrew, free prose). Added 2026-08-09. **Treat everything here as
  unreadable to a future session:** it is binary, it is not in English, and it
  cannot be quoted in a ticket. Nothing downstream may cite a file in this folder
  directly — it cites the **transcription** instead.
- **`Product and UX Reviews/` — the transcriptions of those documents**, one
  Markdown file per source, named `YYYY-MM-DD-<topic>.md`, with stable per-item
  ids so tickets, TODOs and wayfinder tickets can point at a line that will not
  move. `2026-08-06-brief-review.md` (`R1`–`R28`, the product/UX observation
  backlog) and `2026-08-09-entity-model-brief.md` (`E1`–`E19`, the life
  area/goal/milestone/task entity definitions). **Source in
  `docs/pre-injested-docs/`, transcription here** — the two are deliberately
  split, because the transcription is linked from seven places and the source
  from one.

## 🔧 Common commands

```powershell
# One-click: device up -> build -> install -> launch. Sets JAVA_HOME itself.
.\scripts\run-goalpilot.ps1             # phone if plugged in, else the emulator
.\scripts\run-goalpilot.ps1 -Target device -Logcat
.\scripts\run-goalpilot.ps1 -Target emulator -SkipInstall   # just boot the AVD
.\scripts\run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B          # the second device (two-account demo)

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

# Security-rules tests — emulator-only, cannot reach live goalpilot-56e30
cd firestore-tests; npm install; npm test
```

## ⚠️ Pitfalls

- **JDK:** the machine's `JAVA_HOME` is JDK 25, which AGP rejects. Gradle is pinned to JDK 21 via `org.gradle.java.home` in `gradle.properties`. Don't remove that line on this machine.
- **`google-services.json` is the real config** for Firebase project `goalpilot-56e30` (since `79ce624`) — it is committed, which is normal for Android since the key is restricted by package name + SHA-1. `local.properties` and `functions/.env` hold the machine-local half and are git-ignored; a fresh clone needs both before sign-in or the AI coach work. See [docs/SETUP.md](docs/SETUP.md) and [docs/OPERATIONS.md](docs/OPERATIONS.md).
- **Web client id** comes from `local.properties` → `GOOGLE_WEB_CLIENT_ID` → generated `R.string.gp_web_client_id`. Do **not** reference `R.string.default_web_client_id` (only exists with a real json and would clash).
- **Firestore DTOs** must stay mutable (`var`) with defaults + `@DocumentId` for reflective (de)serialization. Keep the domain models immutable and separate.
- **Windows file locks:** KSP/`.gradle` occasionally fail with "Could not delete/move …". Re-run, or `rm -rf app/build/generated/ksp` and rebuild.
- **The emulator is an exclusive singleton** ([SESSIONS.md](SESSIONS.md)). Two sessions driving one AVD corrupts its quickboot snapshot and yields `InstallException: device offline`. `scripts/run-goalpilot.ps1` enforces this — it claims the AVD for the duration of a run and refuses if another session holds it. Never blanket-kill `qemu-system-x86_64` to unstick an emulator; that takes down other sessions' AVDs too. Use `-Recover`, which is scoped to one AVD.
- **GROQ key never ships in the app** — it lives only in `functions/.env` (spec §5). The client always has a local fallback (spec §8).
- **The GROQ model id rots.** GROQ retires models on a rolling schedule and a retired id fails *silently* — the client just serves local fallback tips, so the AI looks bland rather than broken. `DEFAULT_MODEL` in `functions/src/index.ts` is the single pin; check it against <https://console.groq.com/docs/deprecations> before any demo.
- **Instrumented tests need Espresso ≥ 3.7.0** on modern emulators. 3.6.1 reflects on the private `InputManager.getInstance()`, removed in Android 15+, and every test dies with `NoSuchMethodException` before its body runs.
- **Health Connect is pinned to `connect-client:1.1.0-beta01` by the toolchain.** Stable `1.1.0` and every `1.1.0-rc*` require **compileSdk 36 AND AGP 8.9.1+**; this project is compileSdk 35 / AGP 8.7.3 / Gradle 8.10.2, and `checkDebugAarMetadata` rejects them outright. Do not "upgrade to stable" as a tidy-up — it is a three-part toolchain bump (AGP + wrapper + compileSdk), tracked in TODO → FUTURE. There is no stable `1.0.0`; that line ends at `1.0.0-alpha11`.
- **Health Connect is read-only here, and that is load-bearing.** Only `READ_STEPS`/`READ_SLEEP` are declared. Adding a write permission — even temporarily, even to seed test data — contradicts what `HealthPermissionsRationaleActivity` promises the user on a screen the system itself surfaces. Seed real data on a phone instead.
- **The rationale activity is mandatory, and the 14+ entry point must be an `<activity-alias>`.** Health Connect refuses permissions to an app handling neither `ACTION_SHOW_PERMISSIONS_RATIONALE` nor `VIEW_PERMISSION_USAGE`. The alias exists because the system requires `START_VIEW_PERMISSION_USAGE` on the component it launches, and putting that permission on the activity itself would also gate the 13-and-below entry point, which may not be gated.
- **An activity outside `ui/root/` gets no insets for free.** `enableEdgeToEdge()` is on, and only `GoalPilotRoot`'s scaffold insets for it — a standalone activity needs `Modifier.safeDrawingPadding()` or its heading renders under the status bar.
- **Never re-enable Material You dynamic colour.** `GoalPilotTheme` used to take
  `dynamicColor = true`, which meant the wallpaper decided every colour and the
  brand palette was dead code on Android 12+. Colour now comes from the selected
  `AppSkin` (`ui/theme/Palettes.kt`); the two cannot coexist.
- **Colours are test-guarded.** `ThemePaletteTest` asserts WCAG contrast for all
  four skin/brightness schemes and pairwise distinctness of the `GoalCategory`
  palette. Editing a hex without running `:app:testDebugUnitTest` will bite.
- **`animateFloatAsState` cannot animate a chart into existence.** It initialises
  *at* its target on first composition, so a bar whose value never changes never
  moves — which is why the analytics screen used to appear fully formed. Entry
  animations go through `rememberChartProgress` (`ui/components/ChartAnimation.kt`),
  an `Animatable` explicitly started at 0f. Its `key` is the animation's restart
  handle, so anything it keys on must have stable structural equality: `BarItem`
  deliberately carries a `countSuffix: String` and **not** a formatter lambda,
  because two identical lambdas are not `equals` and every recomposition would
  restart the sweep.
- **A composable parameter named `contentDescription` shadows the semantics
  property.** Inside `Modifier.semantics { }` you must write
  `this.contentDescription = …`, or the compiler resolves the name to the (val)
  parameter and fails with "'val' cannot be reassigned". `DonutChart` does exactly
  this.
- **Life areas needed no `firestore.rules` change** — `users/{uid}/lifeAreas` is
  already covered by the owner-only `users/{uid}/{document=**}` match. Adding a new
  per-user subcollection is a client-side change only.
- **A subcollection is not covered by its parent's `match`.** `match /challenges/{id}`
  says nothing about `challenges/{id}/participants/{uid}` — without its own block that
  path matches no rule and is denied. This is what makes a participants subcollection
  the way to let a non-owner join something they cannot edit.
- **`firebase emulators:exec` does NOT validate `firestore.rules`.** It starts, runs
  your script and exits **0** even against a rules file with an undefined function and
  an unbalanced brace — confirmed by running it against a deliberately broken one. The
  emulator does not load rules until a client connects, so a clean run proves nothing.
  The only check that actually fails on bad rules is `firestore-tests/`, where
  `initializeTestEnvironment` loads the ruleset explicitly. And when adding rules tests,
  run the suite against the *old* rules too: pure negative tests ("X is denied") pass
  vacuously when nothing matches at all.
- **Repository snapshot flows must be built on `FirebaseAuth.uidFlow()`** (`data/auth/AuthExt.kt`), never a one-shot `auth.currentUser` read — a `Flow` is constructed at ViewModel-creation time, so a one-shot read pins the account that was signed in then.
- **The Health Connect sync is automatic and unreviewed, and the dedupe is the only thing keeping it honest.** `SyncHealthDataUseCase` runs on every app foreground (`LifecycleEventEffect(ON_START)` in `ui/root/`), throttled to 15 minutes by a per-uid SharedPreferences stamp. There is no review sheet, so the two Firestore lookups it depends on — the user's goals and the entries already logged — are **not optional**: on a timeout it returns `Failed` rather than proceeding, because proceeding blind creates a duplicate "Weekly steps" goal or logs a day twice, and no human is watching. Note that `sourceKey` is no longer unique per entry — a day carries one entry per top-up and their **sum** is what it has been credited, which is what lets today keep growing instead of freezing at the first reading of the morning.
- **A `StateFlow` collected from an `init` block runs before the properties declared below it.** Property initialisers execute in source order and `StateFlow` delivers its current value synchronously on `Dispatchers.Main.immediate`, so an `init` placed above the `MutableStateFlow` it updates dereferences a null field and the app dies before its first frame — `DashboardViewModel` collects the health sync's status from an `init` deliberately positioned *after* `_healthSync`. This never shows up in a unit test of either half.

## 🧱 Conventions

- Generic conventions (English, changelogs, docs, TODOs, safety) live in `.github/instructions/general.instructions.md`.
- Kotlin/Compose/Firebase rules live in `.github/instructions/android.instructions.md`.
- Test layering lives in `.github/instructions/testing.instructions.md`.
- Architecture: **Clean-ish layering** — `feature (Compose + ViewModel)` → `domain (interfaces + models)` → `data (Firebase/GROQ)`, wired by Hilt. ViewModels expose a single `StateFlow<UiState>`; screens are stateless and driven by lambdas from the nav graph.

<!-- JARVIS:BEGIN routing -->
## 🤖 Model routing & agent topology

- **Route by decision density & blast radius:** frontier models (Fable/Opus-class) for architecture, planning, cross-cutting refactors, hairy debugging, review; mid tier (Sonnet-class) for well-specced implementation; fast tier (Haiku/Flash-class) for mechanical bulk edits and summarization. Escalate a tier after repeated failure — never loop a flailing cheap model.
- **Topology ladder:** one agent, one session (default) → split sessions at verification/commit boundaries (handoff files + committed docs, never chat memory) → subagents only for read-only fan-out or truly independent parallel chunks.
- **Session hygiene — the agent flags the split, the user takes it.** On any of these triggers, append one line to the reply — `🔀 **Session:** <trigger> → suggest <exit>; carries over: <what>` — then keep working (it's a recommendation, not a pause): a compaction warning fired / files already read are being re-read / the working set moved to another subsystem; a commit boundary reached **and** the next unit is a different theme; the request plainly exceeds one session (→ `/wayfinder`, then a session per ticket); an answered side question left a long detour the remaining work doesn't need (→ `/compact`); a design question a throwaway would settle (→ `/prototype` here, fresh session to implement). **`/handoff`** = work continues elsewhere, promote durable facts to committed docs first; **`/compact`** = work continues here, transcript only, no durability guarantee. The agent can only recommend — `/handoff`, `/wayfinder`, `/compact` are all user-invoked. **The exit names the end of the session, not the artifact that carries the handover** — `/handoff` is Form A (a document, only when the user invokes it), and by default the agent instead writes committed `sessions/<slug>.md` briefs and ends the reply with `/kickoff <slug>`. Once those briefs exist the handover is **already complete**: recommending `/handoff` on top of them recommends an uncommitted duplicate of a committed file, and leaves the user invoking a skill with nothing left to do.
- **Next-session kickoff prompt — flagging the split is half the duty.** Whenever the suggested exit **opens a new session** (`/handoff`, `/wayfinder`, `/prototype`-then-implement — *not* `/compact`, where work continues here), the same reply ends with a **fenced block the user can paste verbatim as that session's first message**, in one of **two forms, decided by whether a handoff document exists**. **Form A** — the user invoked `/handoff`, so a doc exists — is four lines and no more: `Repo` (path + branch) · `Mode` (`normal` / `AUTO MODE`) · `Read first` (**the handoff doc's absolute path first**, then `AGENTS.md`) · `Task` (**named as the work plan or TODO item names it**). `Carries over` / `Out of scope` / `Exit` are required *sections of that document* and must **not** be repeated in the paste: a chat-text duplicate is uncommitted, unlinted, and free to rot away from the doc it was copied from. **Form B** — no doc — is **not pasted at all** (2026-08-03): the agent writes a committed brief per next session at `sessions/<slug>.md` (front matter `repo`/`branch`/`mode`/`status`/`issue`/`created`, then all seven fields), and the reply ends with one line each — `` `/kickoff <slug>` `` plus a one-line why. **One brief per session, never one parameterized over several targets.** [`/kickoff`](file:///C:/Dev/JARVIS/skills/kickoff/SKILL.md) loads it, checks it hasn't rotted, claims it on the board, and moves it to `sessions/done/` on completion. A file beats a tracker issue here because it is the *first* action of a fresh agent — `Read` needs no network, no auth, no `gh` — and because committed means diffable. Fall back to a pasted block only where a file can't be written or wouldn't be read (not a git repo, target repo absent, or the user asks inline). The agent **cannot** pick Form A by writing the doc itself: `/handoff` is `disable-model-invocation: true`, so absent the user's keystroke, Form B and a long block are the honest output. Three constraints hold for both: it must **stand alone** (the next agent has no transcript — "continue where we left off" is a defect), it must **carry no fact that isn't committed somewhere first** (the block *and* the handoff doc are throwaway), and it must **point at the current methodology rather than restate it** (restated governance ships stale). Unlike the split itself, this part is fully enforceable — it is just reply text. Full spec: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §4.1.
- **Concurrent sessions — claim by path, or don't run them at once.** Running two sessions in one working tree is opt-in, not the default, and legitimate only when the working sets are **disjoint**. The claim board is [`SESSIONS.md`](SESSIONS.md) at the repo root: **its existence is the trigger** — if the file is there, read it before your first edit, claim before your first write, release when done (a stale claim blocks work nobody is doing). Do not judge for yourself whether a sibling is live; that fact is *in* the board, so skipping it leaves you with no evidence you were allowed to. Claim **paths**, not themes — `ui/theme/`, not "the UI" — because only paths are checkable, and confirm your own row rather than inheriting one another session wrote for you from observed changes (that is a report, and it understates your footprint). Shared singletons (build daemon, the git index, device/emulator, live cloud env) need a named owner and are exclusive — check them before your first **build or device command**, not only before your first edit. **In a board repo, blanket staging is banned outright** — `git add -A`, `git add .`, `git commit -a` — not merely "while a sibling is live"; a machine-wide `PreToolUse` hook (`C:\Dev\JARVIS\scripts\Assert-SessionBoard.ps1`) turns one into a permission prompt. The agent recommends, the user assigns: on seeing unclaimed work that fits, append one line — `🧭 **Claim:** <candidate> → owns <paths>; conflicts: <none|paths>` — and wait; never self-assign. Entry point `/claim`. Full rule: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.
- **Lease the commons — wait, don't ask.** The board partitions *territory*; it has no answer for the files **every** session touches for ten seconds each (`AGENTS.md`, `SESSIONS.md`, the changelog index, `kb/log.md`), because a claim lasts hours and claiming those blocks everyone. Those are **leased**, not claimed: [`Lock-Path.ps1`](file:///C:/Dev/JARVIS/scripts/Lock-Path.ps1) writes one gitignored file per path under `.jarvis/locks/`, taken before the **first write of a unit** and held until that unit is **committed** — the unit is edit → stage → commit, with `#git-index` taken alongside the paths at stage time, because releasing between the last edit and the commit is what lets a sibling's half-written file ride into your commit. **Blocked? Do not ask the user:** reorder onto work that doesn't need the path, ask now any question that doesn't depend on the wait, then arm a background wait on the blocking lease file and let the turn end — the harness re-invokes the session when that file disappears, so the wait costs ~2 turns however long it lasts and the user sees nothing. **On waking, re-read before you write** — waiting fixes an *incidental* overlap and makes a *semantic* one worse, and the prompt that used to catch it is exactly what this removed. Leases expire (15 min default) so a crashed session can't wedge a path, and holding through the commit gate is safe for that reason: if the answer outlasts the lease, re-acquire and re-read before staging. **Shard before you lease** — a log shards by day, an index should be generated from what it indexes; what survives that pass is what the lease is for. Auto-resume is Claude-Code-only (a harness background task); Copilot degrades to checking on its next turn. Full rule: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.2.
- **KB candidates live in `kb-candidates/`, one file per session — and the folder is a session-start read.** The moment something durable lands, the `📌 **KB candidate:**` line in the reply is mirrored into `kb-candidates/YYYY-MM-DD-<session-label>.md` at the repo root: created on the first flag, appended to by every later one, riding the next commit of any kind. **Written in every mode** — `AUTO MODE` gates whether the list *drains* without asking, never whether it *exists*. **The drain never waits for the user to declare a unit finished**, because sessions end without anyone saying so: it fires at the commit trigger, on any 🔀 split signal, on `/handoff`, on `/kickoff` — and, the part that actually closes the hole, **every session lists this folder before its first unit of work and reports a non-empty result**, exactly as with `SESSIONS.md`, so a forgotten drain is caught by the *next* session with nobody having to remember. **Every entry stands alone, and another session's chat history is not a source** — a transcript is machine-local, outside git, invisible to the other agent and truncated by compaction, so it is missing precisely when it would be reached for. Each entry therefore carries Claim · Why (the reasoning, and what was rejected) · Destination · Anchors (`none` is legitimate) · Supersedes · Status, and one too thin to write a page from **stops and asks the user** rather than being reconstructed from a log. `/kb-ingest` deletes a fully-drained file and **rewrites a partly-drained one down to the survivors** (original numbering kept, reasons written into `Status`, always-ask entries moved to `## Standing — always-ask`); its journal entry names the source file **and its repo**, which is the candidate→page tie whenever the bundle lives in a different repo. Full rule: `C:\Dev\JARVIS\rules\memory-promotion.md`.
- **Subagent gate:** before spawning ANY subagent — explain why, which model, why that model, and get user approval (no read-only exemption). A harness fan-out default (fleet/workflow presets) does not lift the gate. A machine-wide Claude `PreToolUse` hook (`C:\Dev\JARVIS\scripts\Assert-SubagentGate.ps1`) turns every spawn into a permission prompt — an **early warning, not a guarantee**: Copilot has no equivalent and no git/CI layer can observe a spawn, so "the hook didn't fire" is not approval, and re-issuing a spawn to get past the prompt is the anti-pattern. Rule: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §6 + §6.1.
- **Derivable decisions:** when committed principles fully determine a decision's answer, proceed and log "decision X taken per principle Y" instead of asking. ALWAYS ask for: deletions, outward-facing actions, relocating/rewriting anything that predates JARVIS. Rule: `C:\Dev\JARVIS\rules\derivable-decision.md`.
- **Surfaces:** Copilot = inline completions, quick in-editor edits, PR review; Claude Code = orchestrated multi-step local work; otherwise token economics decide.
- Full policy: `C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md`.
<!-- JARVIS:END routing -->

**Singletons in this repo** — check them before your first *build or device command*, not only before your first edit: the **Gradle daemon**, the **git index**, emulators **`Pixel_10_Pro_XL`** and **`Pixel_10_Pro_XL_B`** (claimed separately; `_B` exists for the two-account demo, not to parallelise instrumented tests — those still queue at the one Gradle daemon), and the live Firebase project **`goalpilot-56e30`**. Blanket staging (`git add -A`, `git add .`, `git commit -a`) is banned outright here, not merely beside a live sibling. Board: [SESSIONS.md](SESSIONS.md).

## 🔒 Frozen / off-limits

- `GoalPilot_spec_EN.docx` — the course spec; do not edit.
- `gradle/wrapper/gradle-wrapper.jar` and `gradlew*` — generated wrapper; regenerate via `gradle wrapper`, don't hand-edit.
