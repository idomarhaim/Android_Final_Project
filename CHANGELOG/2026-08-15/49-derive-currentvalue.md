# `49-derive-currentvalue` — 2026-08-15

`/implement #49` · [`#49` — logProgress is non-atomic: a crash or a retry leaves goal.currentValue permanently wrong](https://github.com/idomarhaim/Android_Final_Project/issues/49)
· branch `feat/goalpilot-implementation` · mode `AUTO MODE`

Builds against `docs/PRODUCT_v0.3.md` **§4.6** (LOG PROGRESS — *`currentValue` stops being a
stored aggregate and becomes a sum over entries*), **§5.2** (*who owns a derived number*),
**§1.5** (progress arithmetic and the four clamps) and **§7.1 / §7.2** (the delta and the
sites).

## What was wrong

`ProgressRepositoryImpl.logProgress` wrote the progress entry and *then* advanced
`goal.currentValue`, as two independent awaits inside one `try`:

```kotlin
ref.set(dto).await()                                   // :83  the fact
if (entry.value != 0.0) {
    goalRepository.addProgress(entry.goalId, entry.value)   // :87  the counter
}
```

Two ways that corrupts stored data, and only the first needs a crash:

1. **The crash window.** Anything ending the process between `:83` and `:87` — a kill, the
   app backgrounded and reaped — leaves the entry recorded and the counter not. Nothing
   reconciles the pair: no sweep, no checksum, and no screen showing the two side by side.
   The goal reads low **forever**, and every later log adds to the wrong base, so the error
   is carried forward rather than washed out.
2. **The retry path, which needs no crash at all.** If the counter update threw, the
   `catch` returned `Resource.Error` — *after* the entry had already been committed. The
   user was told the log failed and did the obvious thing: logged it again. Two entries,
   one counter movement, and now the numbers disagree in the *other* direction.

And the field had **two** client writers, not one: `TaskRepositoryImpl.setDone` moved the
same number from a completely different path.

## What shipped

### 1 · The counter is deleted, not made atomic

A Firestore transaction over the pair was the obvious repair and is the wrong one. §5.2
decides it, and it is checkable against `firestore.rules` rather than a matter of taste:

> A derived number gets a stored writer **if and only if** somebody who cannot read its
> inputs has to read it.

`users/{uid}/goals` is read under `isOwner(uid)`, and that same owner can read the progress
entries. **The reader is the writer**, so `goal.currentValue` is owed to nobody and needs no
stored writer at all. With no stored aggregate there is no second number left to disagree
with the facts — the corruption is not repaired, it is made **unrepresentable** (§0.2, §0.3).

`domain/model/GoalProgress.kt` *(new)* is the whole arithmetic, and it is pure:

```
currentValue(goal) = Σ progressEntries[goal].value
                   + Σ { t.progressContribution | t.goalId == goal.id ∧ t.isDone }
```

`withDerivedProgress` is the single seam that puts that number onto a `Goal`, so every
existing reader — `GoalCard`, the dashboard, the analytics screen, the widget,
`LifeAreaDetailScreen` — keeps reading `goal.currentValue` unchanged. The field survives as
a **view** of the facts; only its source moved. Making every screen call a use case instead
would have spread one arithmetic decision across a dozen files for nothing.

### 2 · Both client writers are gone (§5.2, §7.2)

| Site | Was | Now |
|---|---|---|
| `ProgressRepositoryImpl.kt:83-88` | write the entry, then `addProgress` | writes the entry and stops. There is no step 3 |
| `GoalRepositoryImpl.addProgress` | a read-modify-write transaction on `currentValue` | **deleted whole**, with `GoalRepository.addProgress` |
| `TaskRepositoryImpl.setDone:135-141` | credit/retract the goal inside the points transaction | **deleted**, which also takes the goal document out of the transaction's read set (§7.2's unbounded multi-document transaction) |

**A ticked task still moves its goal** — it is summed rather than written. Deleting the
writer without the task half of the sum would have quietly stopped tasks advancing goals at
all, so the two land together or not at all.

Making the tick **emit a progress entry** instead was considered and rejected: it re-creates
the exact defect one layer down — a second write that has to agree with the first, plus an
untick that then has to find and undo it. Summing over `isDone` is idempotent
*structurally*, which is the property §5.2 chose `C1`'s shape for. `FieldValue.increment` is
rejected for the reason §5.2 already gives: **`increment` *is* the accumulator.**

### 3 · The four clamps §1.5 deletes

Overshoot is legal and shown — past the target the app stops speaking in percent and uses
`C7`'s word. All four sites the spec names are gone:

| # | Site | Clamp |
|---|---|---|
| 1 | `Goal.progressFraction` | `.coerceIn(0.0, 1.0)` |
| 2 | `TaskRepositoryImpl.kt:135-141` | `.coerceIn(0.0, target)` at the write |
| 3 | `GoalDetailViewModel.kt:278` | `.coerceIn(0.0, targetValue)` on the optimistic preview |
| 4 | `GoalRepositoryImpl.kt:91` | `.coerceIn(0.0, target)` — the one that made overshoot unreachable on the only screen a human writes to |

`progressFraction` keeps its name and its `Float` type, so no call site changed; it can now
simply exceed `1f`. `isComplete` and `progressPercent` follow it and are the better for it —
120% of a goal now reads as 120, not 100.

### 4 · No migration, and the corrupted documents self-heal

A goal with no entries and no completed tasks sums to `0.0`, which is exactly what its
stored `currentValue` defaulted to, so **existing documents read identically on day one**.
A document already carrying a wrong number from the old two-step write is not repaired —
its stored field simply stops being consulted, which is the same outcome for less risk.
The write side stops persisting it, so nothing re-introduces a stale copy.

## Deliberately not in this ticket

- **`TaskRepositoryImpl.kt:120-127`** — the points accumulator and its `.coerceAtLeast(0)`
  (§10 defect 2). Same shape, different field; §7.2 assigns it to the session that makes
  `points` derived (`C1` [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)).
- **`DashboardViewModel.kt:104`** — a plain mean of `progressFraction`, which §7.2 notes is
  "broken once overshoot is legal". True, and now reachable, but it is a *display* decision
  §4.4 owns and the file is another session's. Flagged, not silently changed.
- **`start`, and `(current − start)/(target − start)`** — §1.5's origin field is `C7`'s
  measure work, not this ticket's; #49's scope is the stored aggregate and the clamps.

## Concurrency

Four sessions were live in this tree. This unit could not be designed clear of the others
the way `widget-pack` was — the two writers and the four clamps live in files
`d2-life-area-route` held — so the disjoint half (`GoalProgress.kt`,
`ProgressRepositoryImpl.kt`, `TaskRepositoryImpl.kt`, `ProgressRepository.kt`) was built
first and the shared files were taken only after that session released. The board note
records the one change of this unit that reached theirs: `progressFraction` can now exceed
`1f`.

## 🔍 Review

`/adversarial-review`, all three passes (the change deletes a field from live
documents, so it is architecture-class). Two findings fixed, three accepted:

- **fixed** — the summing rule existed twice (`currentValueOf` had its own loop
  beside `currentValues`). Two copies of the invariant this change exists to enforce
  is the same defect one level up; `currentValueOf` now delegates.
- **fixed** — `summing twice gives the same number` was tautological: `f(x) == f(x)`
  cannot fail for any implementation. Replaced with `f(f(x)) == f(x)` over a `Goal`,
  which fails if the seam ever accumulates instead of replacing, plus an
  order-independence case the repository genuinely relies on (the per-goal listeners
  flatten in `combine`'s order).
- **accepted** — `combine` widens the failure surface: any of the N progress
  listeners erroring now kills the goal-list flow, where only the goals query could
  before. All four consumers (`Goals`, `Dashboard`, `Analytics`, `LifeAreas`) were
  read and all four already `.catch` into a screen-level error.
- **accepted, flagged** — `SyncHealthDataUseCase:173` reads `observeGoals().first()`
  under a 5 s `withTimeoutOrNull` and never reads `currentValue`, so it now waits on
  N+1 listeners for a number it discards. `Observed:` the sync reads only
  `healthSourceKey` / `category` / `targetValue` (grepped). `Inferred:` the risk is
  low, because Firestore raises cached snapshots immediately rather than blocking.
  `Untested:` not measured on a device. It fails **closed** — a timeout returns
  `Failed`, which the sync already treats as *do not proceed* — so the worst case is
  a skipped sync, never a duplicated goal or a double-logged day.
- **accepted** — `GoalDetailViewModel` derives via `observeGoal` and then adds
  in-flight deltas on top. Two layers, but idempotent, and the overlay exists for an
  unrelated reason (a server-only transaction leaves nothing for the snapshot
  listener to render).

**Attacked and not broken:** blank and empty goal ids, duplicate ids in the fan-out,
the empty goal list, archived goals (filtered before deriving, so no listener is
opened for them), negative and overshooting sums, unlinked tasks, and every drawing
call site — `GpProgress.kt:46` and `ProgressRing.kt:42` both clamp internally, which
was read rather than assumed.

## 🧪 Tests

| Layer | Exists? | Result |
|---|---|---|
| **JVM unit** (`:app:testDebugUnitTest`) | yes | **312 passed, 0 failed, 0 skipped**, 31 classes |
| **APK build** (`:app:assembleDebug`) | yes | **BUILD SUCCESSFUL** |
| **Security rules** (`firestore-tests`) | yes | **30 passed, 0 failed** |
| **Instrumented** (`:app:connectedDebugAndroidTest`) | yes | **not run** — see below |
| **Cloud Functions** | **no test layer at all** | nothing to run; §7.2 already records `functions/` as having no `test/` dir and no `test` script |

New and changed suites:

- `progress/DerivedProgressTest` — **16 new tests**: the sum itself (entries, completed
  tasks, cross-goal isolation, unlinked tasks), the unclamped cases in both directions,
  idempotence as a fixed point, order-independence, the batch form, and the crash
  window expressed as a test — *an entry recorded with no counter update is already
  counted*.
- `domain/GoalTest` — 4 → **6 tests**. Two assertions are **inverted on purpose**: the
  clamp cases now assert `1.5f` and `-0.05f` where they asserted `1f` and `0f`.
- `feature/goals/GoalDetailViewModelTest` — 16 tests, one inverted: *optimistic progress
  overshoots the target* replaces *…is clamped at the target*. The preview has to agree
  with the derived number, and neither clamps now.

**Instrumented tests were not run, and that is a decision rather than an omission.**
The emulator is an exclusive singleton with three other live sessions in this tree, and
the suite is UI-component tests (charts, icons, empty states, skin picker, social feed,
consent notice) — `AnimatedBarChartUiTest` builds `BarItem` fixtures directly and never
touches a `Goal`, so no androidTest source reaches any line this change alters. Booting
an AVD would contend a singleton to re-prove eleven tests the diff cannot affect.

**Two environment traps hit on the way, both already documented and both re-confirmed:**
`firestore-tests` failed first with *"firebase-tools no longer supports Java version
before 21"* because the machine `JAVA_HOME` in a fresh shell reads
`jdk-21.0.11.10-hotspot` — the **broken** install, not the `jdk-21.0.12.8-hotspot` that
`AGENTS.md` records as current. Exporting the good path fixed it. And KSP needed
`rm -rf app/build/generated/ksp` once, as `widget-pack`'s board note advises reaching
for early. One trap was self-inflicted and is worth recording: deleting
`app/build/tmp/kotlin-classes` — which the documented remedy does **not** name — broke
Gradle's incremental state and produced *"New files were found"* on `clean` itself.
