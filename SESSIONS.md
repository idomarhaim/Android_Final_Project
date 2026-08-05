<!-- SOURCE: user-template v2; do not edit in-project, edit user-level then re-sync -->

# 🧭 Session claim board — GoalPilot

Who is working on what, **right now**. Read this before your first edit; claim
before your first write. Normative rule:
`C:\Dev\JARVIS\rules\agent-topology-and-model-routing.md` §5.

> Running more than one session at a time is **opt-in and not the default**. It
> is legitimate only when the working sets are disjoint and the user has
> assigned them. If two claims would overlap, it is one session's work — run it
> sequentially.

## 🔒 Active claims

| Session | Task | Owns (paths) | Singletons | Claimed |
|---|---|---|---|---|
| `submission` | **Two-account sharing demo (spec §7) + deploy `firestore.rules`**, the last MUST item and the only thing still blocking hand-in — see [`TODO/TODO_MUST/Submission.TODO.must.md`](TODO/TODO_MUST/Submission.TODO.must.md) and the two challenges follow-ups in [`TODO/TODO_OPTIONAL/Integrations.TODO.optional.md`](TODO/TODO_OPTIONAL/Integrations.TODO.optional.md). Deploy and demo ride together because a **non-owner** join is the path the deploy unblocks, and creating a challenge auto-joins the owner — one account cannot exercise it. Rider: correct the stale governance note in `Submission.TODO.must.md` (claims `AGENTS.md` is at template v4 vs library v7; it is at **v14** since `f7ae3dd`) | `firestore.rules` (deploy only, not edited) · `TODO/TODO_MUST/Submission.TODO.must.md` · `TODO/TODO_OPTIONAL/Integrations.TODO.optional.md` · `CHANGELOG/2026-08-05/submission.md` · `SESSIONS.md` | `#emulator-A` (`Pixel_10_Pro_XL`) · `#emulator-B` (`Pixel_10_Pro_XL_B`) · `#firebase` (`goalpilot-56e30`) — **all three held**; a sibling session must not build, drive a device, or write to the live project until this row is released | 2026-08-05 |

## 📏 Rules

0. **This file existing is the trigger.** Read it before your first edit — not
   "if someone else might be here". Whether they are is what this file tells you,
   so skipping it means you have no evidence you were allowed to skip it.
1. **Claim before writing.** Add your row, commit it, then work. If a row was
   written *for* you by another session from files it saw change, that is a
   report, not a claim — confirm and correct its path list before you continue.
2. **Never write outside your paths.** If you need a path another session owns,
   say so and let the user re-assign — do not "just quickly" edit it.
3. **Never blanket-stage** — `git add -A`, `git add .`, `git add --all`,
   `git commit -a`. Not "while another session is live": you cannot know that
   until you have read this board, and by then you have already staged. Explicit
   paths always; it costs nothing on the days you are alone.
4. **Singletons are exclusive.** Builds and device work serialise. Claim, use,
   release — and check the table below before your first **build or device
   command**, not only before your first edit.
5. **Release when done** — clear your row on `/handoff`, on finish, or when
   abandoning. A stale claim blocks work nobody is doing.
6. **The agent recommends, the user assigns.** A session that sees unclaimed work
   fitting its context emits one line —
   `🧭 **Claim:** <candidate> → owns <paths>; conflicts: <none|paths>` — and waits.
   It never self-assigns.

### Singletons in this repo

| Singleton | Why it matters here |
|---|---|
| Gradle daemon / `.gradle` locks | Two `gradlew` runs contend; one blocks or dies mid-write. **This, not the emulator, is what actually serialises two sessions** — see below |
| The git index | Never `git add -A` — stage explicit paths |
| Emulator `Pixel_10_Pro_XL` (`adb`) | One screen, one driver. Installing/driving the app is exclusive |
| Emulator `Pixel_10_Pro_XL_B` (`adb`) | Second device, added 2026-08-05 for the two-account demo. Same rule, claimed separately: `run-goalpilot.ps1 -Avd Pixel_10_Pro_XL_B` |
| Firebase project `goalpilot-56e30` | Live Firestore/Storage/Functions — concurrent writes are attributable to nobody |

**Two emulators do not buy two parallel verifications.** Both AVDs exist so that
*one* session can drive two signed-in accounts at once (the spec §7 sharing demo),
not so that two sessions can each run `:app:connectedDebugAndroidTest`. Those two
runs would still queue at the Gradle daemon above, and worse, each would build an
APK from the *other* session's uncommitted edits — one working tree, one
`app/build/`. Real parallel instrumented testing needs a second checkout, which
this repo has deliberately not adopted.

## 🗂️ Unclaimed work

Where to look, in order: [`TODO/TODO.md`](TODO/TODO.md) (MUST → OPTIONAL →
FUTURE), then open issues. `/claim` reads both and proposes a fit.

Currently unclaimed and ready:
- **Two-account demo + spec title page** — the two remaining MUST items, and the
  only things still blocking submission. Mostly manual: no agent can sign in as
  the second Google account or supply a name, ID and course number. **Take the
  challenges rules deploy in the same sitting**: `firebase deploy --only
  firestore:rules` is one command, and the second account is the only way to
  prove what it unblocks — a *non-owner* join, which creating a challenge
  (auto-joining the owner) cannot exercise.
- **Health Connect on a physical phone** — small follow-up to the shipped feature.
  The emulator carries the provider but its store is empty, so the
  proposal → Firestore write path has never run against real step data.
- ~~One written brief, its own session: `/kickoff challenges-ui`~~ — **done
  2026-08-05.** All three briefs are now in `sessions/done/`; there is no
  unworked brief left. What challenges left behind is folded into the two-account
  item above: **deploy `firestore.rules`**, then verify a *non-owner* join
  against the live backend. Both want the second account, so both belong to that
  sitting rather than to a session of their own.
- ~~One `time-insights` verification is still open~~ — **done 2026-08-04.** Both
  blocked checks ran once the AVD came free: `:app:connectedDebugAndroidTest` 20/20
  green, and a live re-estimation run that returned 105 minutes for a five-word
  title, which the client heuristic (ceiling 60 for five words) and the Cloud
  Function's flat 30 both cannot produce. One of eight candidates matched a
  fallback and was unticked automatically. See
  `CHANGELOG/2026-08-04/time-insights.md` → "Verified against the live model".

**Disjointness**, checked 2026-08-04 and left here so it need not be re-derived:
- The three briefs' **paths** were disjoint — `feature/challenges/`,
  `feature/lifeareas/` + `feature/goals/`, and `feature/analytics/` +
  `functions/src/index.ts`. Two ran concurrently and never collided.
- Their **verification was not**, and that is the part that actually bit. All three
  touch composables, so all three want `:app:connectedDebugAndroidTest`, and the
  emulator is one exclusive singleton — `time-insights` released with its
  instrumented layer unrun because `lifearea-polish` held the AVD. **Disjoint paths
  do not make sessions independent; the device does.** When two sessions both end
  at a composable, expect one of them to hand its device check to the other.
- The **Gradle daemon** turned out to be shareable by queueing, not by claiming: a
  build during a sibling's mid-edit fails on *their* half-written file, which reads
  as your own compile error until you look at the path.
- `challenges-ui` stays clear of `functions/src/index.ts` only because standings
  are computed client-side. A later session moving them server-side collides with
  what `time-insights` already landed.

## 📓 Recently released

| Session | Task | Released | Landed in |
|---|---|---|---|
| `template-sync-v16` | 🔁 **Mechanical template sweep**, driven from JARVIS by `Update-TemplateConsumers.ps1`: `general.instructions.md` **v14 → v16**, `new-changelog-entry.prompt.md` v3 → v4, `AGENTS.md` v12 → v14. Clears the v14 → v15 gap the 2026-08-04 sweep **correctly** refused while `challenges` held a dirty tree — that tree is clean now, so both versions landed in one pass. Verbatim projections only; no decision was taken in this repo, no Kotlin/Gradle/Firestore file touched, and neither `#emulator` nor `#gradle-daemon` was taken. **No Active row was claimed:** a single-commit mechanical sync into a clean, unclaimed tree, so a claim created and cleared in the same breath protects nothing (`C:\Dev\JARVIS\rules\scale-adaptive-ceremony.md`, *mechanical sync* row) | 2026-08-05 | this commit |
| `backend` | Live Firebase backend, E2E verification, Google Tasks import, JARVIS §5 governance | 2026-07-31 | `6e4a184`, `79ce624`, `1ebb178`, `53c2afb`, `64802e5`, PR #1 |
| `launchers` | One-click run scripts; made the emulator singleton self-enforcing | 2026-08-01 | `dc1c06e` + follow-up (pending) |
| `health` | Health Connect integration — steps & sleep, read-only, review-before-write | 2026-08-02 | see `CHANGELOG/2026-08-02.md`; emulator released |
| `theming` | Selectable app skins (Aurora/Blossom) + UI/UX pass | 2026-08-02 | `e31ac9d`, `a413485`, `c30709e`; emulator released |
| `scaffold` | Template-library upgrade — `AGENTS.md` v8→v10, `general.instructions.md` v10→v12, this file v1→v2 | 2026-08-03 | see `CHANGELOG/2026-08-03.md` |
| `lifeareas` | Life areas (user-defined + synced from Google Tasks list names), LLM task durations, interactive time-allocation analytics at day/week/month/quarter/year | 2026-08-03 | `fe9f61d`; see `CHANGELOG/2026-08-03/lifeareas.md`. Emulator released. |
| `challenges` | Competitive challenges: the `participants` security rule that makes joining possible, `firestore-tests/` (the repo's first rules test layer), and the domain + data + DI layers | 2026-08-04 | `1e56ee3`, `8117368`; see `CHANGELOG/2026-08-04/challenges.md`. **Rules written and tested but NOT deployed.** UI continues in `sessions/challenges-ui.md`. Emulator never claimed in practice; Gradle daemon released. |
| `second-avd` | Second emulator `Pixel_10_Pro_XL_B` for the two-account demo; `-Avd` made a demand so it never adopts the other session's screen | 2026-08-05 | see `CHANGELOG/2026-08-05/second-avd.md`. No app code touched, so no suite was run — verification was behavioural against both live emulators. Both AVDs left running; neither claim held. |
| `time-insights` | A stacked-column trend beside the time-allocation donut, and an AI re-estimation pass for tasks that never had a duration | 2026-08-04 | `342af48` + verification pass; see `CHANGELOG/2026-08-04/time-insights.md`. **All layers green and fully verified**: 150 JVM, 20 instrumented, and a live re-estimation run against GROQ that wrote 7 durations to `goalpilot-56e30`. Ran in two sittings — released once with the device checks blocked, re-claimed when the AVD came free. Emulator and Firebase project **released**. |
| `lifearea-polish` | Drag-to-reorder life areas (minimal `sortOrder` writes) and the goals list banded by life area | 2026-08-04 | `6f4a749`; see `CHANGELOG/2026-08-04/lifearea-polish.md`. Both layers green — 144 JVM, 20 instrumented. Emulator `Pixel_10_Pro_XL` recovered from a wedge and **released**; Gradle daemon released. |
| `challenges-ui` | Competitive challenges as a real screen: ViewModel, live standings, discover/join/leave, score reporting, create flow | 2026-08-05 | see `CHANGELOG/2026-08-05/challenges-ui.md`. All three layers green — 175 JVM, 29 instrumented, 16 rules. Ran against an empty board, uncontended. Emulator `Pixel_10_Pro_XL` and the Gradle daemon **released**. The live project `goalpilot-56e30` was **never touched** — the rules deploy is held for the two-account session on Ido's call, so a non-owner join is still proven by `firestore-tests` only. |

> **Post-mortem, recorded because the next session should not repeat it.** The
> `theming` session ran for two days without ever reading this board — it did not
> exist when that work started, and the `AGENTS.md` it read (template v4) had no
> pointer to it. Consequences: it used `git add -A` (the one thing rule 3 forbids
> by name), wrote to `feature/dashboard/DashboardScreen.kt` and
> `di/RepositoryModule.kt` while `health` owned them, and used the Gradle daemon
> and the AVD while `health` held both. Nothing was actually lost — the two
> sessions happened to edit different regions of `DashboardScreen.kt`, and the
> `add -A` landed in a window where the tree held no sibling work — but only by
> luck. The rule text and enforcement were tightened in JARVIS §5 as a result.
