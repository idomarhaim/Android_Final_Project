---
repo: c:\Dev\Android_Final_Project
branch: main
mode: normal
status: ready
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardScreen.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/dashboard/DashboardViewModel.kt
  - app/src/test/java/com/idomarhaim/goalpilot/domain/UnmeasuredPercentTest.kt
  - CHANGELOG/2026-08-23/69-verify-dashboard-average.md
  - sessions/69-verify-dashboard-average.md
created: 2026-08-23 by 66-unmeasured-percent
---

# Run the one thing `f25cca5` could not: the dashboard's overall-progress card

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `normal`

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`CHANGELOG/2026-08-23/66-unmeasured-percent.md`](../CHANGELOG/2026-08-23/66-unmeasured-percent.md)
§ *Second revision, 15:05* — which is the whole specification of this session and already lists what
is expected to fail.

**Task:** run `:app:testDebugUnitTest`, then look at the dashboard's *Overall progress* card in the
three states below. Nothing here is a design decision; it is the run that `f25cca5` owed and could
not take.

## Why this is its own session and not a loose end

`f25cca5` shipped **unverified**, and said so in its commit message, its changelog section and its
board note. `68-drag-to-move` was live, declares the **Gradle daemon** as its singleton, and was
actively building — `.gradle/file-system.probe` 41 s old, two JVMs at `+2.2 s` and `+2.3 s` CPU over
a 15 s sample, still busy on a re-check. Its uncommitted calendar work sits in this shared tree, so a
run then would have compiled **its** sources and reported about **its** tree
(`kb/dev/look-at-your-own-output.md` §4p).

⚠️ **So the first thing this session does is check whose tree it is running in.** `git status
--short` before the suite, not after a red. A failure in `feature/calendar/**` is not this session's.

## What is being verified, and what it replaced

`#66` moved `DerivedProgress.overallCompletionOf` to average **measured goals only**, and could not
reach the screen that renders it — so `main` carried *"Averaged across all your goals"* over a mean
across a subset for eleven hours. `f25cca5` fixes the caption and the ring.

| State | Expected |
|---|---|
| every goal has a measure | **unchanged** — `Averaged across all your goals`, ring as before |
| some goals unmeasured | `Averaged across the N goals that have a number`, ring over the measured ones |
| **no** goal has a measure | **no ring at all** — `UnmeasuredMarker` at 56 dp — and `No goal has a number yet` |

## Exit

- **JVM:** `:app:testDebugUnitTest` green, including the three new
  `UnmeasuredPercentTest` cases that import `DashboardUiState`. Expect **1018**.
- **A look at the card**, in the three states above, **light and dark**. §0.8 is suspended:
  **English only.** The thing no assertion covers is the third state's geometry — the marker is 56 dp
  inside 18 dp of padding, chosen to match the 92 dp ring it replaces, and **if that arithmetic is
  off the card changes height**. That is the one thing this session exists to look at.
- ⚠️ `adb install -r` + `am instrument`, **never** `connectedDebugAndroidTest` — it uninstalls the
  app and takes the Firebase sign-in with it. **No sign-in is needed** for the render itself.
- `CHANGELOG/2026-08-23/69-verify-dashboard-average.md`, and this brief moved to `sessions/done/`
  with `status: done` in the same commit.

## Out of scope

- **Re-deciding what the card should say.** `f25cca5` decided it, per `#66`'s own precedent in
  `SocialRepositoryImpl` (*"across N goals with a number"*). If the render says the wording is wrong,
  that is a finding to report — not a redesign to run here.
- **`#66` itself**, which is closed and shipped. Its eight sites are done and tested.
- **Anything in `feature/calendar/**`**, which is `68-drag-to-move`'s.
