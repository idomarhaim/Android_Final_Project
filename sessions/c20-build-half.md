---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 52
created: 2026-08-20
---

# `C20` build half — the projection function, its two triggers, and `setDone` reduced to one write

**This is the keystone of the remaining backlog.** #50 item 5, #7 and part of #11 are all waiting
on it. Needs the **Gradle daemon**, the **Firebase emulator**, and **no device**.

> **Written 2026-08-20 by `50b-transaction-guard`, which held `sessions/`.** `/kickoff
> c20-derived-state` failed correctly — that is a **past session label** (`CHANGELOG/2026-08-14/`
> and `2026-08-15/`), not a brief. This is the brief. The slug is **`c20-build-half`**.

## Why this exists

[#42](https://github.com/idomarhaim/Android_Final_Project/issues/42) (`C20`, *"Who owns derived
state, and in what shape?"*) closed **2026-08-14 as a decision**. Its build half was never written
and **no issue tracks it** — that is the *decided-read-as-built* failure now recorded at
`C:\Dev\JARVIS\kb\dev\decision-map-charting.md` §12, with this ticket as the worked example.

Design of record: [`docs/PRODUCT_v0.3.md`](../docs/PRODUCT_v0.3.md) **§5.2**. Its rule:

> **A derived number gets a stored writer if and only if somebody who cannot read its inputs has to
> read it.**

## ⚠️ Read this before you read §5.2 — one of its sentences is stale at HEAD

§5.2 says:

> *"Two client transactions already write `goal.currentValue` (`GoalRepositoryImpl.kt:87`,
> `TaskRepositoryImpl.kt:135`)"*

**Both are gone.** #49 removed them; `TaskRepositoryImpl.kt:33-36` records it in its own KDoc, and
`grep -n '"currentValue"'` returns **nothing** in either file. Those line numbers no longer hold a
write. `Observed:` 2026-08-20, verified in the working tree before this brief was written.

So the client-side remainder is **smaller and more specific than §5.2 implies**, and it is one
function.

## What actually still writes derived state — verified at HEAD

`TaskRepositoryImpl.setDone` (`app/src/main/java/com/idomarhaim/goalpilot/data/firestore/TaskRepositoryImpl.kt:94`)
runs one `firestore.runTransaction` containing **three** writes:

| Line | Write | Verdict |
|---|---|---|
| `:114` | `txn.update(taskRef, mapOf("done" …, "completedAt" …))` | **a fact.** Stays — this is the only write that survives |
| `:122` | `txn.update(userRef, "points", newPoints)` | **derived** — `points` is a sum over completion facts. **Goes to the server** |
| `:123` | `txn.set(publicDoc(uid), …)` | the leaderboard projection, incl. `level`. **Goes to the server** |

Reduce `setDone` to line `:114` alone and **the transaction has nothing left to be atomic about** —
it becomes a single-document write, which is what §5.3 needs and what unblocks everything below.

## What ships — §5.2, in its own words

> **One projection function, two trigger registrations, and zero client writers of derived state.**

Neither *"one Function"* nor *"three triggers"* is literally what ships; §5.2 killed that trichotomy
because **most derived numbers need no writer at all**. Of its seven quantities only
**`challengeParticipant.score`** survives as needing a stored writer, *because it crosses the
ownership boundary* — and `publicProfiles.points`, which a leaderboard reader cannot compute.

### 1 · The projection function — `functions/src/index.ts`

`functions/src/index.ts` today holds **three `onCall` callables and zero Firestore triggers**
(verified). Add one projection and register it on two paths:

- **completion facts → `users/{uid}.points` and `publicProfiles/{uid}.points`**
- **`challengeParticipant.score`**

**Project from facts; never recompute-and-store.** §5.2 decided this on #34's own stated risk:
*project from facts* is idempotent **structurally** — running it twice writes the same number —
while recompute-and-store makes double-crediting something the function must be *careful* about.
**`FieldValue.increment` stays rejected for the same reason: `increment` *is* the accumulator.**

### 2 · `publicProfiles.level` is deleted outright

A stored function of `points` **in the same document** — so nobody who reads it is unable to compute
it. Three sites:

- `data/firestore/dto/Dtos.kt:109` — `var level: Int = 1`
- `data/firestore/dto/Mappers.kt:198` — `resolvedLevel()`, whose fallback **can never fire** because
  both writers write ≥ 1
- `data/firestore/SocialRepositoryImpl.kt:137` — its only caller

`User.level` (`domain/model/User.kt:14`) is **already** a computed property and is §5.2's *worked
example*, not a site to change. Leave it alone.

### 3 · `firestore.rules` gets its first field-level condition

`firestore.rules:23` still carries the NOTE *"points/level are written by the client for this course
project"* — that sentence becomes false with this unit and goes with it. Line `:53` carries the same
claim for scores. The participant row keeps three writers with different rights, which is the one
place this genuinely makes the rules harder.

**There is already a test layer for this:** `firestore-tests/rules.test.mjs` runs against the
emulator via `firestore-tests/run-tests.mjs`. Extend it — a rules change with no rules test is the
half of this unit most likely to ship unverified.

### 4 · The shared fixture — §5.2 calls this the honest residual, so do not skip it

> *"the arithmetic now exists in **Kotlin and TypeScript** — a second implementation that can
> disagree. Accepted, because avoiding it costs the offline win entirely, and **pinned by a shared
> `facts → expected numbers` fixture both test layers run**."*

One fixture file, read by the JVM unit tests **and** by the functions tests. Not two copies of the
same numbers — that is the disagreement it exists to prevent.

## The thing that tells you you are done

`app/src/test/java/com/idomarhaim/goalpilot/guards/OfflineWriteGuardTest.kt` is watching this
change, deliberately:

- While `setDone` is still `runTransaction`, it **asserts** `ConnectivityMonitor` and
  `GoalDetailViewModel`'s `connectivity.isOnline()` still exist. Delete either one early and it goes
  **red** with the whole argument in the failure message.
- The moment `setDone` stops being a transaction, its `assumeTrue` stops holding and it reports
  **skipped, not passed** — visible in `app/build/test-results/testDebugUnitTest/` as a real
  `<skipped/>` element.

**That skip is the deliverable's own receipt.** When you see it, `#50` item 5 is unblocked.

## Out of scope — and this one matters

- **Do NOT delete `ConnectivityMonitor`, the pre-check, `OFFLINE_MESSAGE`, or that test file here.**
  That is `50-finish`'s unit and it closes **#50**. A build that also performs a deletion it
  authorised itself is indistinguishable from the failure the guard exists to prevent — which is
  the whole content of `decision-map-charting.md` §12a. Report the skip; let the next session act
  on it.
- **#7, #9, #11** — downstream, separately briefed.
- **`goal.currentValue`** — already done by #49. Nothing to remove.

## Carries over

- **`50b-transaction-guard` (round 3) holds `CLAUDE.md`, `docs/CLOUD-DEVICE.md`, `sessions/` and
  its own changelog.** Disjoint from every path here. Read `SESSIONS.md` and claim before your
  first write — that session's board note names what it owns.
- **`gh` is installed and usable — v2.97.0, and it needs no `gh auth login`.** Windows Credential
  Manager already holds a token with `repo` + `workflow` scope; read it per command rather than
  persisting a second copy:
  `GH_TOKEN=$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill | grep ^password= | cut -d= -f2-) gh …`
  Full note and the `winget` trap in [`CLAUDE.md`](../CLAUDE.md).
- **The issue is filed: [#52](https://github.com/idomarhaim/Android_Final_Project/issues/52)**
  *(2026-08-20)*, and #42 now carries a comment pointing at it. Its body already omits §5.2's
  stale `goal.currentValue` sentence — nothing to correct before you start.
- Touching `app/**` fires the cloud emulator workflow on push. **The screenshot job does not run on
  push** — `.github/workflows/instrumented-tests.yml:197` gates it on
  `github.event_name == 'workflow_dispatch' && inputs.capture-screenshots`. If you want screenshots,
  dispatch it manually.

## Exit

- `functions/` tests green · `firestore-tests/rules.test.mjs` green against the emulator · JVM unit
  green · `assembleDebug` green. State explicitly which layers did not apply.
- **`OfflineWriteGuardTest` reports `skipped`, evidenced by the `<skipped/>` element in the results
  XML** — not by the console summary. Quote it in the changelog.
- The shared fixture exists and **both** test layers read it.
- `CHANGELOG/<today>/c20-build-half.md` written · board row released · this brief closed to
  `sessions/done/` with `status: done` in the same commit · commit and push under AUTO MODE.

## 🚥 Hand-off line — mandatory, the last thing in your final reply

Per `TODO/TODO_MUST/Completion-Roadmap.TODO.must.md` §🚥, seven conditions, exactly one heading.
The slug you name is **`/kickoff 50-finish`** if that brief exists by then; if it does not, say so
and name writing it as the next step — it is a ~10-line deletion and it closes #50.
