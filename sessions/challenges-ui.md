---
repo: c:\Dev\Android_Final_Project
branch: feat/goalpilot-implementation
mode: normal
status: ready
created: 2026-08-04
---

# Challenges: a real screen, and the rules deploy that makes it work

**Repo** — `c:\Dev\Android_Final_Project`, branch `feat/goalpilot-implementation`

**Mode** — `normal`. (The session that built the data layer ran interactively and
asked before each commit; that was scoped to it. Say `AUTO MODE` if you want this
one to commit and push without asking.)

**Read first** — [`AGENTS.md`](../AGENTS.md), then
[`CHANGELOG/2026-08-04/challenges.md`](../CHANGELOG/2026-08-04/challenges.md) —
which is the design record for everything below — then the OPTIONAL block in
[`TODO/TODO.md`](../TODO/TODO.md).

**Task** — finish **Competitive challenges**, the last remaining §6 nice-to-have
in `TODO/TODO_OPTIONAL/Integrations.TODO.optional.md`. The domain, data and DI
layers are built, tested and committed (`8117368`); the security rules are
written and tested but **not deployed** (`1e56ee3`). What is left:

1. **`ChallengesViewModel`** over `ChallengeRepository`, in the house style — a
   single `StateFlow<UiState>`, screens stateless and driven by lambdas.
2. **Replace `sampleChallenges`** in `feature/challenges/ChallengesScreen.kt`
   with live data: the user's challenges with standings, a discoverable list,
   join / leave, and reporting a score.
3. **A create flow** — title, description, type, metric unit, optional start/end.
4. **Deploy the rules** and verify the whole path in one pass.

**Carries over**

- **The repository contract is committed and stable** —
  `app/src/main/java/com/idomarhaim/goalpilot/domain/repository/ChallengeRepository.kt`.
  Every method's doc says which documents it writes and why.
- **Why `Challenge` has no `participantUids` and no `standings`**: the KDoc on
  `app/src/main/java/com/idomarhaim/goalpilot/domain/model/Challenge.kt`. The
  challenge document is owner-only, so a joiner could never maintain a field on
  it. Do not re-add them — the join would silently fail for everyone but the owner.
- **Standings are already ranked.** `rankedByScore` returns
  `ChallengeStanding`s with ranks stamped, using joint ranks for ties (1, 1, 3),
  and flags the current user. Do not re-sort or re-rank in the ViewModel;
  `ChallengeWithStandings.myStanding` is already there.
- **`ChallengePhase` / `phaseAt(now)`** decides UPCOMING / ACTIVE / ENDED and
  should drive what the UI offers — no join button on an ended challenge. Dates
  are half-open and a challenge with no dates is open-ended, not expired.
- **Navigation is already wired** — `Routes.CHALLENGES`, and
  `ui/root/GoalPilotRoot.kt` already routes to `ChallengesScreen`, reached from
  both `SocialScreen` and `ProfileScreen`. A detail screen, if you add one, is
  the only new destination needed.
- **The rules and what proves them**: the `participants` block in
  `firestore.rules`, and `firestore-tests/` (`npm test` in that folder, 16 cases).
  Re-run it after any rules edit — and read the two pitfalls it produced in
  `AGENTS.md`, especially that `firebase emulators:exec` does **not** validate
  rules and that pure negative tests pass vacuously.
- **Standings stay client-side.** A Cloud Function would put this session in
  `functions/src/index.ts`, which `sessions/time-insights.md` also needs — the one
  file two sessions cannot share. Reasoning on `SESSIONS.md` and in the changelog.
- **Compose traps, if you animate anything**: the `AGENTS.md` pitfalls about
  `animateFloatAsState` initialising *at* its target (use `rememberChartProgress`)
  and about `BarItem`'s restart key needing stable structural equality.
- Existing card/screen idiom to match: `ui/components/GpCard.kt` and the
  dashboard's card composables.

**Out of scope**

- Moving standings or points to a Cloud Function — see above, and the existing
  anti-cheat item in `TODO/TODO.md` → FUTURE.
- Cascade-deleting a challenge's participant rows, and "kick a participant".
  Both are recorded limitations, not oversights; the changelog says why.
- The two MUST items (two-account demo, spec title page) — except see Exit below,
  where they overlap on purpose.
- `sessions/lifearea-polish.md` and `sessions/time-insights.md`. Disjoint paths,
  but all three touch composables and so contend for the emulator.

**Exit**

- `:app:testDebugUnitTest` green (106 at handover), and
  `:app:connectedDebugAndroidTest` green — you will be changing composables, so
  the emulator `Pixel_10_Pro_XL` is in play and is an **exclusive singleton**:
  claim it on [`SESSIONS.md`](../SESSIONS.md) before your first device command,
  not just before your first edit.
- **Deploy the rules**: `firebase deploy --only firestore:rules`. This is a live
  change to `goalpilot-56e30` — run `firestore-tests` first, and ask Ido before
  deploying. Until it lands, joining fails against the real backend no matter how
  correct the client is.
- **Verify a join against Firestore, not the UI.** Creating a challenge
  auto-joins the owner, so a single account cannot prove the part that was
  broken. A *non-owner* join needs the second account — which is the two-account
  demo MUST item. Either pair the two in one sitting, or state plainly in your
  changelog that non-owner join was verified only by `firestore-tests` and not
  against the live backend. Do not report it as verified end-to-end if it wasn't.
- Your own `CHANGELOG/YYYY-MM-DD/<session-label>.md`, written before the commit
  and used verbatim as the commit message.
- Commit on approval; flip the TODO checkboxes only once Ido confirms.
- Release your row on `SESSIONS.md`, and move this file to `sessions/done/` with
  the commit hash.
