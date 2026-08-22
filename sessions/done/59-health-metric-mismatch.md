---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 59
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildHealthProposalsUseCase.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SyncHealthDataUseCase.kt
  - app/src/test/java/com/idomarhaim/goalpilot/domain/HealthProposalsTest.kt
  - app/src/test/java/com/idomarhaim/goalpilot/domain/HealthSyncTest.kt
  - kb-candidates/2026-08-23-59-health-metric-mismatch.md
  - CHANGELOG/2026-08-23/59-health-metric-mismatch.md
  - sessions/59-health-metric-mismatch.md
created: 2026-08-23
result: |
  Both halves done. Code fix in a014e36 (pushed): match() requires the unit to agree,
  4 new tests red-first, 790 JVM unit tests green. Data repair run on Ido's explicit
  approval -- both mispaired goals unpinned and their 83 hc:* progress entries deleted
  (245612 steps, 165.5 hours; zero hand-logged entries were at risk), verified by
  re-reading. KB drained to C:\Dev\JARVIS 93c2c0c. #59 closed.
---

# `#59` — a step count is being credited to a goal that has nothing to do with steps

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Read first:** [`AGENTS.md`](../AGENTS.md), then
[`#59`](https://github.com/idomarhaim/Android_Final_Project/issues/59), then
[`BuildHealthProposalsUseCase.kt`](../app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildHealthProposalsUseCase.kt)
— specifically `match()` and the KDoc above it.

**Task:** stop Health Connect metrics being pinned to an unrelated goal, and decide with Ido
what to do about the data already written.

## The evidence, so nothing has to be re-found

`Observed:` 2026-08-22, `emulator-5554`, live account. `Strength Training` — a **Fitness**
goal measured `x/100` — reads **`245613/100`** on the Goals screen, and the number **grew
while a session watched it**: `245358` → `245612` → `245613` over about two hours.
`Sleep 7 hours` reads `165.5/100`. It is on camera in
`C:\Users\namei\Videos\GoalPilot-Tour\GoalPilot-full-tour.mp4` at **`1:23`**.

## The cause is four lines, and the file predicts it

```kotlin
val candidates = active.filter { it.healthSourceKey == null && it.category == metric.category }
return candidates.firstOrNull { it.measureWord.equals(metric.unit, ignoreCase = true) }
    ?: candidates.firstOrNull()          // <-- unguarded last resort
```

The **preferred** branch matches on the unit and is correct. The **fallback** takes any
unpinned active goal in the metric's category — so a daily step count in the thousands is
credited to a Fitness goal measured out of 100.

**The KDoc directly above already names the hazard:**

> *"preferring one whose unit already agrees so steps do not get added to a "workouts" goal
> and inflate it by four thousand"*

The preference exists; the fallback is unguarded, and the fallback fires whenever no goal
in that category happens to be measured in `steps`. `SyncHealthDataUseCase` then **pins**
the result via `healthSourceKey`, so the wrong pairing becomes permanent and every later
sync tops it up. That is why it climbs.

⚠️ **The pinning is not the bug.** `#47`'s design is working exactly as intended — it is
being handed the wrong goal. Do not change the pinning to work around the matcher.

## What to build

**1 · Guard the fallback.** Cheapest first, and the first is probably right: **require the
unit to agree**, and when nothing matches, propose a **new** goal rather than corrupting an
existing one. `HealthMetric` already carries `defaultGoalTitle` (`"Weekly steps"`) for
exactly this case, so the machinery exists. A magnitude sanity check on `target` is a
second line of defence, not a substitute — a plausible-looking wrong pairing is still wrong.

**2 · The data already written is Ido's call, and it is always-ask.** The bad entries are
identifiable: `ProgressEntry.sourceKey == "hc:steps:<date>"`. Three options to put to him,
recommendation first:
- **unpin the goal and leave history alone** — clears `healthSourceKey` so it stops growing,
  costs no data, and leaves one goal reading oddly until he edits it;
- **remove the `hc:steps:*` entries on that goal** — correct, and a deletion of his data;
- **do nothing** beyond the fix, and let him tidy it by hand.
Do **not** pick for him. Deleting a user's progress entries is always-ask in both modes.

## Exit

- `HealthSyncTest` covers `match()` at the JVM layer already — extend it with the failing
  case: a Fitness goal measured out of 100, a `STEPS` metric, and **no** unit agreement.
  The test must fail before the fix and pass after; a test written after the fix proves
  nothing about the fix.
- Green at every layer the change touches (JVM unit; no UI change is expected, so no
  instrumented run and **no `connectedDebugAndroidTest`**).
- `CHANGELOG/2026-08-23/59-health-metric-mismatch.md` written, with the test output verbatim.
- Commit on the auto-mode trigger; the data-repair half waits for Ido regardless of mode.

## Carries over

- The defect, the reproduction and the growing number — [`CHANGELOG/2026-08-22/tour-video.md`](../CHANGELOG/2026-08-22/tour-video.md) § *Defect observed, not caused*.
- The full analysis — [`#59`](https://github.com/idomarhaim/Android_Final_Project/issues/59).

## Out of scope

Health Connect permissions, the proposals UI, `#47`'s pinning design, and the
`Sleep 7 hours` goal's own `165.5/100` — check whether the same fix covers it and say so,
but do not widen into a sleep-measure redesign.
