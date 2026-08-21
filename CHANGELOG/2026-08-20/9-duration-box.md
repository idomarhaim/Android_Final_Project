# `9-duration-box` — 2026-08-20

> **Summary:** `#9` ships. `R8`'s placeholder icon is **stored provenance** — a `DurationSource` on the task, written by whoever produced the duration — and `looksLikeFallback` + `SERVER_FALLBACK`, which reconstructed that answer by recomputing both silent fallbacks and comparing, are deleted. `TaskEstimate.minutes` is **nullable**: `scoreTask` no longer manufactures a duration out of a point score that is itself a word count. §1.4's hand-typed duration is sticky, unconditionally, enforced by **exclusion from the candidate set** rather than by a check on what comes back. The migration turned out to be settled by a fact — no code path let a person type a duration before this ticket — so legacy rows read as `UNKNOWN`, non-sticky, with **no backfill write at all**. JVM unit **481/0** (+22), instrumented **105/0** (+14), `assembleDebug` green, render pass looked at twice. **Round 1 of this session built nothing**: it opened while `11-fill-buttons` held five of these files, and the "verified independent" line in both briefs turned out to be false for a reproducible reason — the conflict map was run at **symbol** granularity while the tickets collide at **file** level.

**Session:** `9-duration-box` · **Date:** 2026-08-20 · **Mode:** `AUTO MODE` · **Brief:** [`sessions/9-duration-box.md`](../../sessions/9-duration-box.md)
**Rounds:** r1 blocked (`48e94bc`) · r2 shipped. **Unblocked by** `11-fill-buttons` releasing at `28edeb2`.

---

# Round 1 — blocked, and the finding that came out of it

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

---

# Round 2 — the ticket, built

`/kickoff 9-duration-box` was re-run once `11-fill-buttons` released. The block-clearance check the
round-1 correction block demanded was run first and is recorded in `1e42b5d`: zero active rows, clean
tree, `28edeb2` on `main` with `#11` closed to `sessions/done/`.

**Round 1's derived design survived contact.** `#11` had edited `GoalDetailScreen.kt`,
`GoalDetailViewModel.kt`, `Dtos.kt`, `Mappers.kt` and `RecommendationRepositoryImpl.kt` in between, so
all five were re-read at HEAD before anything was written. `AddTaskRow` itself was untouched by `#11`.

## What shipped

**1 · Provenance is stored, and the two reconstructors are gone.**
`DurationSource { USER, AI, UNKNOWN }` lives on `Task` and on `TaskDto` as a string.
`TaskScoring.looksLikeFallback` and its `SERVER_FALLBACK` sentinel are **deleted**: they existed to
recognise an estimate no model had produced by recomputing what each fallback *would* have returned
and comparing — *"evidence, not proof"* by their own KDoc, since a model may land on the same numbers
by agreement. The producer knows the answer at the moment it produces it.

**2 · `TaskEstimate.minutes` is nullable, and that is where the rule actually bites.**
`scoreTask` no longer reads `?: TaskDuration.fallbackMinutes(points)` on either the missing-minutes
path or the `catch` path, and `parseClassification` and both offline classification fallbacks now
report `null` too. That chain was the concrete site of *"the app never guesses a duration from a word
count"*: with no network, `points` is `heuristicPoints` = `5 + 3×words`, and `fallbackMinutes` turned
that word count into *how long your life took* — then stored it as though a model had said so.
`fallbackMinutes` survives, **narrowed to one caller**, `TaskDuration.minutesOf`, which decides what an
undated task is worth **to the chart** and writes nothing.

**3 · The precedence rule is a pure object.**
`DurationEntry(minutes, source)` carries every transition: `withEstimate` (a typed value returns
untouched — no comparison, so there is nothing to soften later), `withRetitle`, `typed`, `resolve`.
`AddTaskRow` holds one of these instead of the `aiMinutes` variable it replaced. Two consequences worth
naming: **a retitle no longer clears a typed duration**, and **clearing the box makes it estimable
again** rather than leaving it permanently sticky and the AI button apparently dead.

**4 · The structural half of §3.3 A.**
`BackfillDurationsUseCase.invoke` filters `durationSource != USER` explicitly. It is redundant today —
a typed value implies a stored one, which the existing null filter already excludes — and is kept
because the rule is about **provenance**, not nullness. A test pins both directions and a third pins it
against `limit = Int.MAX_VALUE`, so it cannot start passing for the wrong reason if the candidate set
ever widens.

**5 · The migration — decided on a fact, and it is a read.**
**Legacy rows read as `UNKNOWN`, no backfill write runs, and `UNKNOWN` is not sticky.** `Observed:`
2026-08-20 — no code path let a person type a duration before this ticket. Every `estimatedMinutes`
write under `app/src/main` was model- or fallback-derived, and a grep for a minutes `TextField` across
`feature/` and `ui/` returned nothing. So the one value stickiness protects **provably cannot exist
yet**, which is what makes a non-sticky `UNKNOWN` safe rather than merely convenient.

## A defect this ticket would otherwise have created

`TimeAllocation.estimatedTaskCount` counted **any stored duration**, while its own KDoc says *"tasks
whose duration came from the LLM"*. Those were the same set for as long as only the model could write
one — and `R8`'s box ends that. Without the fix, the analytics card's *"x of y durations estimated by
AI"* would have counted the user's own hand-typed number as the AI's. It now reads
`durationSource == AI`. Not in the brief; it is the brief's own §0.3 complaint arriving one file over.

## Two things found by looking, not by testing

**The icon was indistinguishable from the button beside it.** `R8`'s state marker shipped as a filled
`AutoAwesome` tinted `tertiary` — which is exactly what the **AI-estimate button** on the same row is.
Thirteen instrumented assertions passed, each true of its own node; the render pass put both in one
frame and the duplication was immediate. **No number of assertions would have caught it**: Compose
queries are per-node and the defect is a relation between two of them. Fixed by making the marker
**outlined and `onSurfaceVariant`**. Flagged as a KB candidate and ingested.

**The caption disagreed with what was stored.** Found in the pre-commit self-review: typing `0` left
`isTyped` true and `minutes` non-null, so the line under the box read *"You said about 0m"* while
`resolve()` correctly stored `30` as nobody's answer. A second number quietly disagreeing, one line
below the box built to end them. `durationCaption` now derives from `resolve()`, which makes the
disagreement unrepresentable rather than merely fixed.

## What `#7` inherits

`7-quickadd-complete` lands in this same `AddTaskRow`, and the brief predicted this exactly: the
`aiMinutes` reconstruction it would have built on is gone. Two concrete hand-overs — the quick-add and
Google-Tasks sheets now carry a **nullable** `minutes` and render *"no estimate · counts as 30m"*
rather than printing thirty minutes as an answer, and §3.4's *ask the user how long* is **that sheet's**
surface, deliberately not built here.

## Deliberately not built

The §1.4 **points inversion** — `points = round(minutes/3) × difficulty`, the `difficulty` enum, the
`5..50` cap deletion, points banked as completion facts — is `C1`
[#19](https://github.com/idomarhaim/Android_Final_Project/issues/19). §1.4 discusses it and #9's
precedence rule in one paragraph, and only one of the two is this ticket's. `heuristicPoints` therefore
survives; `C1` retires it.

## 🧪 Tests

| Layer | Result |
|---|---|
| **JVM unit** | **481 / 0**, 0 skipped (+22 on `#11`'s 459). New `DurationEntryTest` (16); `BackfillDurationsUseCaseTest` +4 and two rewritten; `TimeAllocationUseCaseTest` +1 |
| **Instrumented** | **105 / 0** on `Pixel_10_Pro_XL_B` (+14 on `#11`'s 91). New `DurationBoxUiTest` (13) and `DurationBoxRenderTest` (1) |
| **Build** | `assembleDebug` green |
| **Render pass** | **Looked at twice** — `docs/render-passes/2026-08-20-9-duration-box/duration-box.png`. The first look found the duplicate-icon defect; the second confirmed the fix |
| **`firestore.rules`** | **Not applicable, stated rather than skipped.** Task documents live under `users/{uid}` with no field allowlist, so a new field needs no rule change and `firestore-tests/` is unaffected |
| **`functions/`** | **No test layer exists** in this project; unchanged by this ticket |

**Two tests were rewritten rather than deleted, and it matters which way.** The two that existed only
to exercise `looksLikeFallback` now assert the **opposite**: an estimate matching the old fallback
numbers is *believed*. That was `looksLikeFallback`'s admitted false positive — a real 60-minute answer
rejected because the offline heuristic would also have said 60 — and it is now gone rather than
tolerated.

## 📥 KB — round 2

📥 **Ingested:** *a Compose assertion suite is structurally blind to a **relational** defect* →
`kb/dev/look-at-your-own-output.md` **§4e**, the sixth instrument failure and the first that is not a
weak instrument at all.
📥 **Ingested:** *a placeholder assertion tests focus, not emptiness* →
`kb/dev/android-device-verification.md` **§7a**.

Both in `C:\Dev\JARVIS` (`d07eec3` claim → `664d03d` content); index rows, `kb/log/2026-08-20.md` and
the board note updated, `Check-KbLinks` **CLEAN** (94 pages). Neither always-ask gate opened.

⚠️ **Noted rather than ingested:** `kb/dev/android-device-verification.md` **§8 already says** that
`connectedDebugAndroidTest`'s uninstall destroys device state and that `install -r` + `am instrument`
avoids it. This session re-derived it from scratch after the Gradle task deleted the render-pass PNG.
A **retrieval** failure, not a knowledge gap, so nothing in §8 changed.

## Files — round 2

- **Main:** `domain/model/TaskEstimate.kt` · `domain/model/Task.kt` · `domain/model/Recommendation.kt` ·
  `domain/usecase/BackfillDurationsUseCase.kt` · `domain/usecase/TimeAllocationUseCase.kt` ·
  `data/firestore/dto/Dtos.kt` · `data/firestore/dto/Mappers.kt` ·
  `data/remote/RecommendationRepositoryImpl.kt` · `feature/goals/GoalDetailScreen.kt` ·
  `feature/goals/GoalDetailViewModel.kt` · `feature/analytics/AnalyticsViewModel.kt` ·
  `feature/analytics/AnalyticsScreen.kt` · `feature/dashboard/DashboardViewModel.kt` ·
  `feature/dashboard/DashboardScreen.kt`
- **Tests:** `domain/DurationEntryTest.kt` *(new)* · `ui/DurationBoxUiTest.kt` *(new)* ·
  `ui/DurationBoxRenderTest.kt` *(new)* · `domain/BackfillDurationsUseCaseTest.kt` ·
  `domain/TimeAllocationUseCaseTest.kt` · `data/RecommendationRepositoryFallbackTest.kt` ·
  `feature/goals/GoalDetailViewModelTest.kt`
- **Other:** `docs/render-passes/2026-08-20-9-duration-box/duration-box.png` *(new)* ·
  `kb-candidates/2026-08-20-9-duration-box.md` *(new, drained and deleted in this unit)* ·
  `SESSIONS.md` · `sessions/9-duration-box.md`
- **In `C:\Dev\JARVIS`:** `kb/dev/look-at-your-own-output.md`, `kb/dev/android-device-verification.md`,
  `kb/index.md`, `kb/log/2026-08-20.md`, `SESSIONS.md`

---

# Round 3 — the retrieval failure had left a *wrong instruction* behind, and that is fixable

Ido asked what round 2's closing ⚠️ note meant and whether it needed fixing. Restating it did not
answer the question, so the note was re-read as a **finding** rather than as an apology — and it turns
out something concrete was broken after all.

**What the note actually said.** `./gradlew :app:connectedDebugAndroidTest` **uninstalls the app when
it finishes**, which deletes the app's external files dir — where the render test had just written its
PNG. So the capture was gone before `adb pull` could fetch it. The workaround (install both APKs with
`install -r`, then `adb shell am instrument` directly, which uninstalls nothing) was re-derived from
scratch, and `kb/dev/android-device-verification.md` **§8 already documented it** from
`48-settings-surface` on 2026-08-19. Nothing was broken in the KB and nothing in the code; the failure
was that the page was not consulted.

**But the failure left residue, and the residue is a defect.** Both render tests carried this KDoc:

> *The PNG lands in the app's external files dir; `adb pull` fetches it.*

**That instruction does not work**, and it is the one the next author reads: whoever writes render test
#3 opens render test #2. Following it after the Gradle task reports a path that does not exist, which
reads as *the test did not write anything* rather than *the task deleted it*. `AGENTS.md`'s command
block listed `connectedDebugAndroidTest` with no warning either.

Fixed in three places — both render tests' KDoc and the `AGENTS.md` command block — each carrying the
working sequence, the `.debug` `applicationIdSuffix` trap (the directory is not the one `applicationId`
suggests), `pm list instrumentation` for the runner rather than guessing at `AndroidJUnitRunner`, and a
pointer to §8.

`WaterGoalRenderTest.kt` is `11-fill-buttons`'s file. It is released on the board and the sentence is
identically wrong in both, so it is corrected here rather than left to mislead — a doc correction, not
an adoption of their ticket.

**Verified by execution, not by reading.** All four commands in the new KDoc are the ones this session
actually ran to recover the PNG: the two `install -r` calls, the `am instrument` line with
`com.idomarhaim.goalpilot.debug.test/com.idomarhaim.goalpilot.HiltTestRunner` as `pm list
instrumentation` reported it, and the pull from
`/storage/emulated/0/Android/data/com.idomarhaim.goalpilot.debug/files/`.

**No KB change.** §8 is correct as written and gains nothing from this; the gap was retrieval, and the
remedy that fits a retrieval gap is a pointer at the point of use, which is what shipped.

## 🧪 Tests — round 3

`:app:compileDebugAndroidTestKotlin` green. **No behaviour changed** — three comment blocks and one
markdown block — so no suite was re-run, stated rather than skipped silently.

## Files — round 3

`app/src/androidTest/.../DurationBoxRenderTest.kt` · `app/src/androidTest/.../WaterGoalRenderTest.kt` ·
`AGENTS.md` · this file · `SESSIONS.md`

---

# Round 4 — why the page was not read, and what actually changes

Ido asked the question round 3 did not answer: *why didn't you read the relevant file, and what can
you do so it doesn't repeat?*

## The answer is structural, and it is measurable

**There is no rule, skill or checklist anywhere that fires a KB *read*.**

`grep -rniE "consult the kb|read the kb|search the kb|retriev" rules/ user-rules/` in `C:\Dev\JARVIS`
returns **nothing about reading**. The single `retriev` hit, `derivable-decision.md:195`, is about
*deleting* a drained candidate file — the write side again.

Compare the other direction, which is specified to the point of redundancy: `/kb-flag` →
`kb-candidates/<date>-<session>.md` → `/kb-ingest`, firing at the commit trigger, on any 🔀 split
signal, on `/handoff`, on `/kickoff`, and backstopped by *"every session lists this folder before its
first unit of work"* so a forgotten drain is caught by the **next** session with nobody having to
remember.

**The pipeline is a funnel with no outlet.** And the second measurement makes the cost concrete:
**65 of 76** `kb/dev/` pages name GoalPilot, while this repo's `AGENTS.md` — the file every session is
*required* to read first — pointed at that bundle **once**, to cite graphify's *policy*. Nothing in it
pointed at `kb/index.md`, which is the lookup surface.

## The part that is actually damning, and it is not "I forgot to search"

**The KB was searched twice this session.** Once in round 1 (`grep` over `kb/dev/` for *conflict* and
*wrong width*) and once in round 2 (section headings of `android-device-verification.md`). **Both were
searches for a destination to write to** — *where does this candidate belong?* — and neither was a
search for an answer to read.

So the habit is not missing. It is **wired to the wrong verb**. That matters for the remedy: *be more
thorough* addresses a lapse that did not occur, and only a **trigger** addresses this.

## Why it survived five days and would have survived longer

Every other gap in this methodology announces itself. An unclaimed path collides. An undrained
`kb-candidates/` file is found by the next session's mandatory listing. A rotted brief fails its
`/kickoff` checks. **A page that is never read emits no signal at all** — the session solves the
problem anyway, slightly slower, and reports success, exactly as round 2 did.

That makes the failure rate **unobservable in principle**. This instance surfaced only because Ido
asked what a one-line footnote in a wrap-up meant.

## What shipped, and what deliberately did not

**Shipped — `AGENTS.md` now has a retrieval pointer.** In *Authoritative docs*, which every session
reads first by rule: `kb/index.md`, described as a **lookup surface rather than a reading list**, with
**three named moments** — before the first device/emulator/deploy/Firebase command, when something
surprises you, and before spending more than a few minutes working around anything — plus the six
pages this repo trips over most. The *why* is written into the entry, because a pointer whose reason
is invisible is the first thing a later editor trims.

**Not shipped — the `rules/` half, and that is Ido's call.** Giving retrieval a trigger the way
deposit has one is a change to how the agent behaves, so the 🎬 walkthrough rule owns it and it is
**parked**, not written, not synced. Candidate wording and rationale:
[`kb-candidates/2026-08-20-9-duration-box.md`](../../kb-candidates/2026-08-20-9-duration-box.md) entry
**4b**.

**Stated plainly: what shipped is weaker than what is parked.** `AGENTS.md` covers **one repo** and is
a **pointer, not a gate** — it works only if the file is read attentively, which is the same
assumption that just failed. The parked rule would attach to an **action** rather than to a document.
The only genuinely mechanical version is a `PreToolUse` hook on `adb` / `gradlew connected*` /
`firebase deploy` that prints the matching page, and that is a larger decision than the ticket which
found it.

## A correction caught before it shipped

The first draft of the `AGENTS.md` entry said **64 of 76**. Recounting mechanically
(`grep -rln … | wc -l` rather than eyeballing the space-separated list) gave **65**. Corrected before
the commit — the same *recompute, don't read* duty that this whole round is about.

## 🧪 Tests — round 4

**Not applicable and stated rather than skipped:** no code changed. One Markdown block in `AGENTS.md`,
one KB section, one candidate file.

## 📥 KB — round 4

📥 **Ingested:** *the funnel has no outlet* → `kb/dev/learning-pipeline.md` § *The funnel has no
outlet*, in `C:\Dev\JARVIS` (`73c81f5` claim → `2db56de` content). Index row and
`kb/log/2026-08-20.md` updated, board row released, `Check-KbLinks` **CLEAN** (94 pages). That page's
existing scope is *external sources → KB* — the funnel **in** — so the observation that it has no
outlet extends it rather than needing a page of its own.

⏸️ **Parked, always-ask:** the `rules/` half, per above.

## Files — round 4

`AGENTS.md` · `kb-candidates/2026-08-20-9-duration-box.md` *(new; entry 4a drained, 4b parked, so the
file **stays**)* · this file · `SESSIONS.md` · in `C:\Dev\JARVIS`: `kb/dev/learning-pipeline.md`,
`kb/index.md`, `kb/log/2026-08-20.md`, `SESSIONS.md`


---

## 📌 Pointer repair — `#55` built it *(appended 2026-08-21 by session `55-scoring-model`)*

**Appended, not edited.** Everything above is this session's own account of what it did and
is left exactly as it was written; what follows is the one fact a later reader needs and
cannot get from it.

Where this file defers §1.4's points inversion to **`C1` [#19]**, that pointer was already
stale when it was written: #19 is a **decision** ticket, `state_reason: completed`, closed
2026-08-10, and it was never going to build anything. `7-quickadd-complete` found that on
2026-08-20 and [#55](https://github.com/idomarhaim/Android_Final_Project/issues/55) was
opened as the carrier.

**`#55` is now built** — the inversion, the difficulty enum, the deleted `5..50` cap, the
retired `heuristicPoints`, the completion facts and §1.5's `goalEdges`. So the correct
forward pointer from every sentence above that says *"that is #19's"* is **`#55`**, and the
account of the work is `CHANGELOG/2026-08-21/55-scoring-model.md`.
