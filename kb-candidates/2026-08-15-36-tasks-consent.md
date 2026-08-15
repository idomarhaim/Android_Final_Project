# KB candidates — `36-tasks-consent`, 2026-08-15

Session: `/implement #36` (Google Tasks consent legibility), branch
`feat/goalpilot-implementation`, `AUTO MODE`.

Every entry below stands alone. No entry cites this session's transcript.

---

## 1 · A shared working tree makes *compilability* a singleton no board row can partition

**Claim.** In a single-module build (this repo: one Gradle module, `:app`), the compile state of the
main source set is a **shared exclusive resource**, exactly like the daemon or the emulator — but
unlike those it has no name, no claim column, and nobody notices it until it is red. One session's
half-finished rename makes **every** sibling's test run impossible, including sessions whose own
working set is disjoint and whose own code is correct. Path-disjointness, which is the entire
precondition for running sessions in parallel, does **not** buy verification-independence.

**Observed**, 2026-08-15, GoalPilot, three concurrent sessions:

- `d2-life-area-route` renamed `Goal.lifeAreaId: String?` → `lifeAreaIds: List<String>` (#2).
- `widget-pack` (#10) claimed **all-new paths** — genuinely disjoint from everyone — and its
  `BuildWidgetSnapshotUseCase.kt` read the old singular field.
- `36-tasks-consent` (#36) shared three files with `d2` but its own code compiled clean.

`:app:compileDebugKotlin` reduced to **one** error, at the intersection of the two *other* sessions,
and it blocked all three from running a single test. `widget-pack` had declared *"I go last on the
daemon"*, so it was not building and had no reason to know its file was the tree's sole blocker.
The block cleared only when a session that was not blocked by it chose to look.

**Why (and what was rejected).** The board partitions **territory** and names **singletons**
(`AGENTS.md`: Gradle daemon, git index, two AVDs, the live Firebase project). Both mechanisms are
about *who may write*, and the compile state is not written by anyone — it is *emergent* from
everyone's writes together. Rejected: adding `#compile` as a claimable singleton. A claim excludes,
and excluding sessions from compiling is worse than the problem; the tree is red for everyone
regardless of who holds a token. Also rejected: "just build less often", which is what produced the
silent blockage here. The remedy that actually fits is **notification, not exclusion** — a session
that finds the tree red should identify the owner and say so on the board, the same shape §5.4
already prescribes for unpublished work. That worked: a note naming the file, the error and the
owner cost one edit.

**Rule implication, and it is Ido's call, not this session's.** This looks like a gap in
`rules/agent-topology-and-model-routing.md` §5 — the parallel-sessions precondition is stated as
*disjoint working sets*, and this is a case where disjoint working sets were **not sufficient**. If
that sentence should gain a clause, it is a change to how sessions behave and therefore
🎬-walkthrough territory. **Flagged, not drafted.**

**Destination.** `kb/dev/` — a new page, or a section on the parallel-sessions flow page if one
exists. The rule implication above is a **separate, always-ask** item.
**Anchors.** `SESSIONS.md` claim rows and the three 📣 notes of 2026-08-15;
`CHANGELOG/2026-08-15/36-tasks-consent.md` § Concurrency.
**Supersedes.** Nothing.
**Status.** Pending. The `kb/dev/` half is ingestable under `AUTO MODE`; the `rules/` half is not.

---

## 2 · A ViewModel-scoped `ensureX()` guard behind `LaunchedEffect(Unit)` means *once ever*, not *once per screen entry*

**Claim.** In Navigation Compose, navigating away disposes a destination's composable but **keeps its
`ViewModel` alive in the back stack**. So `LaunchedEffect(Unit) { vm.ensureX() }` re-fires on
back-navigation while a `private var xChecked = false` field inside the ViewModel does not reset.
The pair reads as *once per screen entry* and behaves as *once per ViewModel lifetime*. The KDoc in
this codebase says the former (`"Loads AI recommendations once per screen entry"`) and the code does
the latter — which is **correct** for an expensive network call and **wrong** for a cheap local read,
because the local read is the one whose answer can change while you are on another screen.

**Observed**, 2026-08-15, GoalPilot #36. The bug this caught before it shipped: the Google Tasks
scope is granted from **two** surfaces (dashboard import card, life-areas sync card). Grant it on
Life areas, press Back, and the Dashboard's surviving ViewModel still held `MISSING` — so the card
went on telling a user who had just complied that they had not. The defect was **introduced by the
fix**: before #36 the card said nothing about consent, so it could not be wrong about it.

**Why (and what was rejected).** The guard was copied from the neighbouring `ensureRecommendations`
on the same ViewModel — consistency with local convention, which is normally right and was wrong
here. The discriminator is not *how often* but **what kind of read**: a guard is for expense, and
where the read is cheap and the answer is owned outside the screen, the guard buys nothing and costs
correctness. Rejected: a shared `StateFlow` of consent on the client that both ViewModels observe —
correct, and more machinery than a re-read of a cached value costs. Rejected: refreshing on
`ON_RESUME` — the same answer with a lifecycle dependency that unit tests cannot reach.

Second-order finding worth keeping with it: **the unit test written alongside pinned the defect.**
`the probe runs once per screen entry` asserted the guard rather than any user-visible contract, so
it would have gone green forever on the wrong behaviour — the implementation-coupled anti-pattern,
found by `/adversarial-review` and not by the suite.

**Destination.** `kb/dev/` — Compose/Android page on screen-entry effects and ViewModel lifetime.
**Anchors.** `feature/dashboard/DashboardViewModel.kt` `refreshTasksConsent()` and its KDoc;
`feature/lifeareas/LifeAreasViewModel.kt`; `app/src/test/java/…/feature/lifeareas/TasksConsentTest.kt`
(`re-entering the screen corrects a reading that has gone stale`).
**Supersedes.** Nothing. Does **not** contradict the existing `ensureRecommendations` guard, which
stays correct on its own terms.
**Status.** Pending.

---

## Not this session's — reported, not drained

Four `kb-candidates/` files were already in the folder at session start and belong to other
sessions; `backlog-triage` reported the same four on 2026-08-15 and also did not drain them:

- `2026-08-13-c15b-stored-ai-text.md`
- `2026-08-13-c2-task-type.md`
- `2026-08-15-c23-goal-category.md`
- `2026-08-15-c24-settings-surface.md`
