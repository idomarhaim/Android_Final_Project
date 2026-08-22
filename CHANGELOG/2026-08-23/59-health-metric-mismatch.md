# 59-health-metric-mismatch — the fallback that credited a step count to a goal measured out of 100

> **Summary:** [`#59`](https://github.com/idomarhaim/Android_Final_Project/issues/59)'s code half is fixed: `BuildHealthProposalsUseCase.match()` no longer falls back to *any* unpinned goal in the metric's category, so a Health Connect reading is only ever filed against a goal whose **unit agrees**, and otherwise proposes a new one. Four tests written red first, then the one-branch deletion that turns them green; **790 JVM unit tests, 0 failures**. The **data half is held for Ido and is the half that matters on his account** — the fix cannot reach a goal that is already pinned, so `Strength Training` keeps climbing until the pin is cleared.

**Date:** 2026-08-23 · **Session:** `59-health-metric-mismatch` · **Mode:** AUTO · **Issue:** [#59](https://github.com/idomarhaim/Android_Final_Project/issues/59)

## The change, in one line

```kotlin
// before — two rules, and the second fires exactly where the first was needed
val candidates = active.filter { it.healthSourceKey == null && it.category == metric.category }
return candidates.firstOrNull { it.measureWord.equals(metric.unit, ignoreCase = true) }
    ?: candidates.firstOrNull()

// after
return active.firstOrNull {
    it.healthSourceKey == null &&
        it.category == metric.category &&
        it.measureWord.equals(metric.unit, ignoreCase = true)
}
```

Nothing else in `main` changed. The pinned-first branch above it is untouched, and
`SyncHealthDataUseCase` is untouched — `#47`'s pinning was never the bug, it was being handed the
wrong goal.

## Why this and not the other candidate the brief offered

The brief named a magnitude sanity check on `Goal.targetValue` as a possible second line of
defence. **Left out, deliberately, and recorded so it is not re-proposed as new:** it needs a
threshold nobody can derive, and it rejects a goal like *"walk 100 steps up the stairs"* — which
agrees on the unit and is genuinely the right home for the reading. Requiring the unit to agree is
not a weaker version of it; it is the check that has a principle behind it.

**Also considered and rejected: matching on `MeasureKind` as well as the word.** `HealthMetric`
declares its kind (`COUNT`, `DURATION`), so the comparison was available for free. It would reject
a goal a user classified as `DISTANCE` while measuring it in steps — a reasonable thing for a person
to think — and buys nothing the word does not already give.

**Empty is not a wildcard, and it did not need special-casing.** A goal written before §1.3 carries
`measureWord == ""`, and no metric's unit is empty, so it simply agrees with nothing. There is a
test for that rather than a comment, because it is the commonest shape of the defect.

## 🧪 Tests

**Layers this change touches:** JVM unit only. No UI, no ViewModel, no repository and no resource
changed, so there is no instrumented, endpoint or database layer to run — and **no
`connectedDebugAndroidTest` was run**, deliberately: it would have uninstalled the app and taken the
device's signed-in account with it, for a change that cannot be observed on a device without Ido's
live data anyway.

**Red first, and the run is recorded because a test written after the fix proves nothing about it.**

```
> Task :app:testDebugUnitTest

HealthProposalsTest > sleep is not credited to a sleep goal that is not measured in hours FAILED
    com.google.common.truth.AssertionErrorWithFacts at HealthProposalsTest.kt:202

HealthProposalsTest > a goal carrying no measure at all is not taken either FAILED
    com.google.common.truth.AssertionErrorWithFacts at HealthProposalsTest.kt:217

HealthProposalsTest > steps are not credited to a category-mate whose unit says nothing about steps FAILED
    com.google.common.truth.AssertionErrorWithFacts at HealthProposalsTest.kt:188

HealthSyncTest > a category-mate with the wrong unit is left alone and a new goal is made instead FAILED
    java.lang.AssertionError at HealthSyncTest.kt:281

44 tests completed, 4 failed

> Task :app:testDebugUnitTest FAILED
BUILD FAILED in 39s
```

The task **executed** rather than reporting `UP-TO-DATE` on a previous run — checked, because this
repo has now hit that trap twice (`kb/dev/look-at-your-own-output.md` §4c).

Green after the fix, whole suite:

```
BUILD SUCCESSFUL in 56s
32 actionable tasks: 11 executed, 21 up-to-date
```

⚠️ **That run measures the tree as it was at the time, and the tree has moved since.** Two sibling
sessions — `63-occurrences-and-recurrence` and `65-measure-proposal` — began writing into
`app/src/main` **after** it, and `63`'s board row claims the **Gradle daemon** as a singleton. So
the suite was **not** re-run at commit time: doing so would have contended for a claimed singleton
to re-measure files this commit does not carry. What the red→green bracket establishes is unchanged
— it is the same three files before and after the one-branch deletion — and this commit contains
those three files and nothing else.

Counts read out of `app/build/test-results/testDebugUnitTest/*.xml` rather than off the console,
which prints no total on success:

```
suites=74 tests=790 failures=0 errors=0 skipped=0
HealthProposalsTest tests=22 skipped=0 failures=0 errors=0
HealthSyncTest      tests=22 skipped=0 failures=0 errors=0
```

### The four new cases

| test | file | what it pins down |
|---|---|---|
| `steps are not credited to a category-mate whose unit says nothing about steps` | `HealthProposalsTest` | the defect as found — `Strength Training`, Fitness, `x/100` |
| `sleep is not credited to a sleep goal that is not measured in hours` | `HealthProposalsTest` | its second victim, so the fix is shown to cover both |
| `a goal carrying no measure at all is not taken either` | `HealthProposalsTest` | `measureWord == ""` reads as *nothing to compare*, never as a wildcard |
| `a category-mate with the wrong unit is left alone and a new goal is made instead` | `HealthSyncTest` | the goal is not **pinned** either — the half that made it permanent |

### Two existing tests were leaning on the fallback and now say what they claim

- `steps go to a fitness goal and sleep to a sleep goal` matched through the fallback (both goals
  carried an empty unit). Units stated; the category is still what keeps the two readings apart.
- `archived goals are never proposed` gave its goal no unit, so after this change it would have
  passed for `#59`'s reason instead of its own. Unit added, so archiving is the only thing that can
  be rejecting it.

## ⚠️ The fix does not repair Ido's account, and that is not a caveat — it is the live half

`match()` checks `healthSourceKey` **first** and returns outright. `Strength Training` already
carries `hc:goal:steps`, written by an earlier sync, so the corrected matcher never gets a vote on
it: **every future sync still tops it up**, exactly as before.

**An automatic un-pin was considered and rejected**, because it re-breaks the ticket the pin exists
for. [`#47`](https://github.com/idomarhaim/Android_Final_Project/issues/47) makes the pin
authoritative **over** category and unit precisely so a user edit cannot orphan a synced goal;
clearing the key whenever the unit disagrees would un-pin any user who renames their unit and hand
them a duplicate goal on the next sync. A cache built to outrank its inputs cannot be repaired by
re-reading its inputs.

So the repair is out-of-band and it is **Ido's call in both modes** — it writes to a live account's
history, and one of the three options deletes his progress entries. Put to him with the fix; nothing
was touched.

| option | what it does | cost |
|---|---|---|
| **unpin only** (recommended) | clear `healthSourceKey` on the mismatched goals; the next sync then proposes *Weekly steps* / *Weekly sleep* instead | stops the growth immediately; leaves the inflated totals in place until he edits them |
| **unpin + purge** | also delete every `ProgressEntry` whose `sourceKey` starts `hc:steps:` / `hc:sleep:` on those goals | correct history, and a **deletion of his data** |
| **do nothing** | ship the fix alone | the number keeps climbing on his account |

`Untested:` no repair path was built or rehearsed — deciding *whether* comes first, and the three
differ in what they would need.

### What this means for `#62`

[`#62`](https://github.com/idomarhaim/Android_Final_Project/issues/62) — re-record the tour — is
listed as blocked on `#59`. **It stays blocked.** The camera does not read `match()`, it reads the
Goals screen, and the Goals screen still shows `245613/100` until the goal is unpinned. Shipping the
code half does not clear that dependency; the data decision above is what clears it.

## Files

- `app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/BuildHealthProposalsUseCase.kt` — the guard, and a KDoc that now says why the fallback is gone and what was rejected instead of it.
- `app/src/test/java/com/idomarhaim/goalpilot/domain/HealthProposalsTest.kt` — three new cases, two existing ones un-leaned.
- `app/src/test/java/com/idomarhaim/goalpilot/domain/HealthSyncTest.kt` — the pinning case.
- `kb-candidates/2026-08-23-59-health-metric-mismatch.md` — written, drained, and **removed in this
  commit**. Both entries were promoted, which is the one deletion `rules/derivable-decision.md` §1
  permits without asking. It never reached history (it was created and drained inside one session),
  so the tie between candidate and page is the journal entry in
  [`kb/log/2026-08-23.md`](https://github.com/idomarhaim/JARVIS) — which names this repo and this
  file by name, exactly as the cross-repo case requires.

## 📥 KB

Two candidates, both drained this session into `C:\Dev\JARVIS` — commit
[`93c2c0c`](https://github.com/idomarhaim/JARVIS/commit/93c2c0c), pushed, `Check-KbLinks` clean over
113 pages:

1. **A "prefer X" fallback fires only on the inputs X was written to reject** — new page. `firstOrNull { quality } ?: firstOrNull()` is not a preference, it is two rules whose input ranges partition cleanly, and the second one owns exactly the population the first was protecting. The tell is a comment on the preferring line that names a hazard: it is describing the line **below** it, which is what happened here verbatim.
2. **Fixing a heuristic does not un-pin what it already mismatched** — a second instance of `stored-state-is-an-entry-point.md`, arriving through a different door (the heuristic's own cached answer rather than a user preference), plus the argument above for why an automatic repair is forbidden.

## 🧭 Siblings

`63-occurrences-and-recurrence` (claimed `74613cc`) and `65-measure-proposal` (row written, not yet
committed at the time of writing) are both live in this tree. Neither overlaps this session's paths:
`63` owns `data/firestore/**`, `Occurrence.kt`, `Task.kt` and the rules tests; `65` owns the measure
proposal surface and `functions/**`. This commit names its three source paths explicitly and touches
`SESSIONS.md` not at all — `65`'s row is sitting uncommitted in that file and is theirs to publish.

