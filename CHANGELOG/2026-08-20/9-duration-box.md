# `9-duration-box` — 2026-08-20

> **Summary:** `#9` did **not** start — `11-fill-buttons` is live and its board row owns five of the files the duration box needs, plus the Gradle daemon. The "verified independent" line in **both** briefs is **false**, and for a reproducible reason: the conflict map was run at **symbol** level (`unit` vs `looksLikeFallback`) while the two tickets collide at **file** level. Wave 3's original *"strictly sequential, one working set"* was right and `505f083` overturned it on bad evidence. Nothing under `app/` was touched. The design and the migration decision are derived and written down, so the next run starts at the keyboard.

**Session:** `9-duration-box` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Brief:** [`sessions/9-duration-box.md`](../../sessions/9-duration-box.md)
**Blocked by:** `11-fill-buttons` (`8eb37b9`, claimed 2026-08-20)

---

## Why nothing was built

`/kickoff 9-duration-box` opened while `11-fill-buttons` held a live claim. Liveness was
established, not assumed:

| Check | Result |
|---|---|
| Row present in `## 🔒 Active claims` | ✅ `11-fill-buttons`, claimed 2026-08-20 |
| Their paths dirty in the working tree | ✅ ` M SESSIONS.md`, ` M sessions/11-fill-buttons.md` at first read |
| Re-read one tool call later | ✅ tree clean, `8eb37b9 11-fill-buttons: claim before the first write` on `main` — they committed **between my two calls** |

`Observed:` 2026-08-20. That is §5.3(c)'s *positive* reading — live, stop and ask. No transcript
escalation was needed, because the working tree answered first.

### The overlap, file by file

| File `#9` needs | On `11-fill-buttons`'s row? |
|---|---|
| `domain/model/TaskEstimate.kt` | free |
| `domain/model/Task.kt` | free |
| `domain/usecase/BackfillDurationsUseCase.kt` | free |
| `feature/analytics/AnalyticsViewModel.kt` | free |
| `feature/dashboard/DashboardViewModel.kt` | free |
| **`feature/goals/GoalDetailScreen.kt`** — *the duration box itself* | ❌ `feature/goals/` |
| **`feature/goals/GoalDetailViewModel.kt`** | ❌ `feature/goals/` |
| **`data/firestore/dto/Dtos.kt`** | ❌ named |
| **`data/firestore/dto/Mappers.kt`** | ❌ named |
| **`data/remote/RecommendationRepositoryImpl.kt`** | ❌ named |
| `app/src/test/`, `app/src/androidTest/` | ❌ "(new suites)" — partial |
| **Gradle daemon** | ❌ singleton — no build, no JVM tests |

`#9`'s deliverable is a box inside `AddTaskRow`, which lives in `GoalDetailScreen.kt`. There is no
half of this ticket that ships without a contested file.

**Doing the free half anyway was considered and rejected on a fact, not on caution.**
`TaskEstimate.minutes` must become **nullable** (§3.4 — a field that fails validation is *absent*,
never a guess), and that break propagates straight into `RecommendationRepositoryImpl.kt:101` and
`GoalDetailViewModel.kt:143`, both theirs. Landing it would put a **non-compiling tree** under a
sibling who is building in it.

## The finding: the conflict map under-detected, and it is reproducible

Both briefs carry the same assurance, and both are wrong:

- `sessions/9-duration-box.md` — *"no file mentions both (`grep -rl unit … | xargs grep -l looksLikeFallback` returns empty)"*
- `sessions/11-fill-buttons.md` — *"verified 2026-08-20: no file touching `Goal.unit` also touches `TaskEstimate`"*

Both are **true as stated and useless as a conflict test.** They ask *does one file mention both
symbols today* — a question about the code as it stands. A conflict is about the **edits a ticket is
about to make**: `#11` adds a per-goal input mode to `GoalDetailScreen.kt`, `#9` adds a duration box
to the same file, and neither symbol ever has to appear beside the other for the two sessions to
collide there. The grep returns empty and nothing in its output says the width was wrong.

`TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` Wave 3 already had the right answer — *"they all
edit the same add-flow and the same ViewModel, so they are **one working set** and cannot be
parallelised however many emulators are free"* — and `505f083` (*"#9 and #11 turn out to be parallel,
not one wave"*) overturned it using the symbol grep. **The roadmap was right and stands.**

`Inferred:` the general shape — *a symbol grep answers a question about the current code and is
silent about the edits a ticket is about to make; the unit that collides is the **file**, and for a
Compose screen the file is the whole surface.* Flagged as a KB candidate.

## What `#9` should build — derived during the block, so the next run starts at the keyboard

None of this needed a build.

### 1. Provenance is a stored enum, three values

```kotlin
enum class DurationSource { USER, AI, UNKNOWN }
```

on `Task` (`durationSource: DurationSource = UNKNOWN`) and on `TaskDto` as a string.
`TaskScoring.looksLikeFallback` and `SERVER_FALLBACK` are **deleted** — the repository knows, at the
point of production, whether a model answered, so nothing downstream has to reconstruct it.

A fourth value (`NONE`, *no duration at all*) was considered and collapsed: `estimatedMinutes == null`
already says that, and two fields that can disagree is §0.3's defect rather than its fix.

### 2. `TaskEstimate.minutes` becomes nullable

`null` means **the model did not supply one** (§3.4). `scoreTask` stops calling
`fallbackMinutes(points)` on both the missing-minutes path and the `catch` path — that is the
concrete site of *"the app never guesses a duration from a word count"*. `points` keeps its
heuristic: the §1.4 inversion (`points = round(minutes/3) × difficulty`, the `difficulty` enum, the
`5..50` cap deletion, completion facts) is `C1` [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)
and **not this ticket**, however much §1.4 discusses both in one paragraph.

### 3. The precedence rule, as a pure JVM-testable object

```kotlin
data class DurationEntry(val minutes: Int? = null, val source: DurationSource = UNKNOWN) {
    val isTyped: Boolean           get() = source == DurationSource.USER
    val showsEstimateIcon: Boolean get() = !isTyped        // R8, exactly as worded
    fun withEstimate(m: Int?): DurationEntry = if (isTyped) this else …  // unconditional, no threshold
    fun withRetitle(): DurationEntry         = if (isTyped) this else DurationEntry()
    fun resolve(): Pair<Int, DurationSource> = …           // DEFAULT_MINUTES + UNKNOWN when skipped
}
```

This is what makes the Exit's *"both directions"* JVM test possible without an emulator, and it keeps
`AddTaskRow` thin — the composable holds one `DurationEntry`, not the `aiMinutes` reconstruction at
`GoalDetailScreen.kt:332` it replaces. A **retitle no longer clears a typed duration**, which is the
precedence rule made visible and is the cheapest thing for the instrumented test to watch.

`BackfillDurationsUseCase.invoke` gains an explicit `.filter { it.durationSource != DurationSource.USER }`
— redundant today (a typed value implies non-null minutes, which the existing filter already excludes)
but it **is** the rule, and §3.3 A requires it be structural rather than a check on what comes back.

### 4. The migration decision — settled by a fact, not a preference

The brief calls this the trap: legacy tasks carry no stored provenance, and backfilling them by
running `looksLikeFallback` one last time re-imports the exact guess this ticket deletes.

**Decision: legacy rows read as `UNKNOWN`, no backfill write runs at all, and `UNKNOWN` is not sticky.**

`Observed:` 2026-08-20 — **no code path lets a person type a duration today.** Every write of
`Task.estimatedMinutes` under `app/src/main` is model- or fallback-derived:

```
Mappers.kt:83,96                          round-trip
RecommendationRepositoryImpl.kt:139       classify response
RecommendationRepositoryImpl.kt:207,220   fallbackMinutes(10)
AnalyticsViewModel.kt:205                 backfill proposal
DashboardViewModel.kt:246,497             classify / Google-Tasks import proposal
GoalDetailViewModel.kt:115                AddTaskRow's computed `aiMinutes ?: fallbackMinutes(points)`
```

and a grep for a minutes `TextField` / `onValueChange` across `feature/` and `ui/` returns
**nothing**. So the one value stickiness exists to protect **provably cannot exist yet**, which is
what makes a non-sticky `UNKNOWN` safe rather than merely convenient. The migration is therefore a
**read**, not a write: the mapper yields `UNKNOWN` for an absent field and no row is touched.

`Untested:` the UI half of all four — nothing was compiled or run.

## 🧪 Tests

**None run, at any layer, and no code changed.** The Gradle daemon is claimed by `11-fill-buttons`.

Layers this ticket will owe when it runs: **JVM unit** (`DurationEntry` precedence in both
directions; `BackfillDurationsUseCase`'s candidate filter), **instrumented** (the icon appearing and
disappearing), and a **looked-at render pass** — the icon is the deliverable and a green test is not
a look. No layer is being skipped silently; all three are deferred with the ticket.

## 📥 KB

📥 **Ingested:** *a disjointness check run at symbol granularity is not a conflict test* →
`kb/dev/agent-topology-and-routing.md` § *The disjointness check has to be run at the granularity the
claim is made in*, in `C:\Dev\JARVIS` (`f621889` claim → `cd7f9b6` content). Index row and
`kb/log/2026-08-20.md` updated, board row released with the note, `Check-KbLinks` **CLEAN** (94
pages). Neither always-ask gate opened: destination is `kb/dev/`, not `rules/`, and the entry is
additive to a paragraph it agrees with. The candidate file was written, fully drained and deleted in
this unit, per the drained-candidate carve-out.

It sits third among three findings on that page that all say *path partitioning is necessary and not
sufficient*, at three different moments: *paths-not-themes* is about the **claim**, *the singleton
nobody can claim* is about resources no partition covers, and this one is about the **pre-flight
check** that decides whether two tickets are ever offered in parallel at all.

## Files

- **Edited:** this file *(new)*, `sessions/9-duration-box.md`,
  `kb-candidates/2026-08-20-9-duration-box.md` *(new, drained and deleted in this unit)*
- **In `C:\Dev\JARVIS`:** `kb/dev/agent-topology-and-routing.md`, `kb/index.md`,
  `kb/log/2026-08-20.md`, `SESSIONS.md` — claimed, written, released.
- **`app/` untouched**, deliberately — see above.
- **`SESSIONS.md` untouched.** `11-fill-buttons` holds it and was mid-write during this session; a
  blocked session that lands no work in `app/` has nothing to claim, and the pathspec commit remedy
  explicitly cannot cover a file both sessions write.
