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
| _none_ | — | — | — | — |

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
| Gradle daemon / `.gradle` locks | Two `gradlew` runs contend; one blocks or dies mid-write |
| The git index | Never `git add -A` — stage explicit paths |
| Emulator `Pixel_10_Pro_XL` (`adb`) | One screen, one driver. Installing/driving the app is exclusive |
| Firebase project `goalpilot-56e30` | Live Firestore/Storage/Functions — concurrent writes are attributable to nobody |

## 🗂️ Unclaimed work

Where to look, in order: [`TODO/TODO.md`](TODO/TODO.md) (MUST → OPTIONAL →
FUTURE), then open issues. `/claim` reads both and proposes a fit.

Currently unclaimed and ready:
- **Two-account demo + spec title page** — the two remaining MUST items, and the
  only things still blocking submission. Mostly manual: no agent can sign in as
  the second Google account or supply a name, ID and course number.
- **Health Connect on a physical phone** — small follow-up to the shipped feature.
  The emulator carries the provider but its store is empty, so the
  proposal → Firestore write path has never run against real step data.
- Three written briefs, each a session: `/kickoff challenges-ui`,
  `/kickoff lifearea-polish`, `/kickoff time-insights` (`sessions/`).

**Disjointness of the three briefs**, checked 2026-08-04 so the next session does
not have to re-derive it:
- Their **paths** are disjoint — `feature/challenges/`, `feature/lifeareas/` +
  `feature/goals/`, and `feature/analytics/` + `functions/src/index.ts`
  respectively. Two can be written concurrently.
- Their **verification is not.** All three touch composables, so all three want
  `:app:connectedDebugAndroidTest`, and the emulator is one exclusive singleton.
  They queue at the device even when their edits never meet.
- `challenges-ui` stays clear of `functions/src/index.ts` only because standings
  are computed client-side. A later session moving them server-side collides with
  `time-insights`.
- All three add files under `app/src/test/` — different files in the same
  directory, which is fine. Stage explicit paths and it stays fine.

## 📓 Recently released

| Session | Task | Released | Landed in |
|---|---|---|---|
| `backend` | Live Firebase backend, E2E verification, Google Tasks import, JARVIS §5 governance | 2026-07-31 | `6e4a184`, `79ce624`, `1ebb178`, `53c2afb`, `64802e5`, PR #1 |
| `launchers` | One-click run scripts; made the emulator singleton self-enforcing | 2026-08-01 | `dc1c06e` + follow-up (pending) |
| `health` | Health Connect integration — steps & sleep, read-only, review-before-write | 2026-08-02 | see `CHANGELOG/2026-08-02.md`; emulator released |
| `theming` | Selectable app skins (Aurora/Blossom) + UI/UX pass | 2026-08-02 | `e31ac9d`, `a413485`, `c30709e`; emulator released |
| `scaffold` | Template-library upgrade — `AGENTS.md` v8→v10, `general.instructions.md` v10→v12, this file v1→v2 | 2026-08-03 | see `CHANGELOG/2026-08-03.md` |
| `lifeareas` | Life areas (user-defined + synced from Google Tasks list names), LLM task durations, interactive time-allocation analytics at day/week/month/quarter/year | 2026-08-03 | `fe9f61d`; see `CHANGELOG/2026-08-03/lifeareas.md`. Emulator released. |
| `challenges` | Competitive challenges: the `participants` security rule that makes joining possible, `firestore-tests/` (the repo's first rules test layer), and the domain + data + DI layers | 2026-08-04 | `1e56ee3`, `8117368`; see `CHANGELOG/2026-08-04/challenges.md`. **Rules written and tested but NOT deployed.** UI continues in `sessions/challenges-ui.md`. Emulator never claimed in practice; Gradle daemon released. |

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
