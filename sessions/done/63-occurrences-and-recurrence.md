---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
commit: 7c457c4
issue: 63
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Occurrence.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/RepeatRule.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/**
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Mappers.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/**
  - app/src/main/java/com/idomarhaim/goalpilot/di/RepositoryModule.kt
  - app/src/main/java/com/idomarhaim/goalpilot/core/FirestorePaths.kt
  - firestore.rules
  - firestore-tests/**
  - app/src/test/java/com/idomarhaim/goalpilot/domain/OccurrenceTest.kt
  - kb-candidates/2026-08-23-63-occurrences.md
  - CHANGELOG/2026-08-23/63-occurrences-and-recurrence.md
  - sessions/63-occurrences-and-recurrence.md
created: 2026-08-23
---

# `#63` — the `occurrences` collection, and the recurrence half `#56` did not build

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

**Read first:** [`AGENTS.md`](../AGENTS.md) ·
[`Occurrence.kt`](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Occurrence.kt) and
[`Task.kt`](../app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt) — **both already
describe what is missing, in their own KDoc** · then
[`docs/PRODUCT_v0.3.md` §7.1](../docs/PRODUCT_v0.3.md) and
[`#63`](https://github.com/idomarhaim/Android_Final_Project/issues/63).

**Task:** build the flat `occurrences` collection and the repeat rule, so *"this occurrence, or
all future ones?"* becomes askable.

## Why this exists as its own session

[`#56`](https://github.com/idomarhaim/Android_Final_Project/issues/56) shipped **four fields on
the task** (`occurrenceRung` / `Start` / `End` / `Placement`) — *at most one* `when` per task —
and said so:

> *"**At most one**, and §2.1 wants more: a **rule** on the task plus **occurrence documents**…
> `#56` builds the occurrence half; **recurrence is the other half and is not here.**"*

`Observed:` 2026-08-23 — `grep -rn occurrences` over `core/`, `data/firestore/` and
`firestore.rules` returns **zero hits**.

**It is a foundation, not a feature.**
[`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64) cannot start without it
(it counts **missed windows**, and a window *is* an occurrence), and
[`#61`](https://github.com/idomarhaim/Android_Final_Project/issues/61) wants `googleEventId` to
live on it. ⚠️ **[`#60`](https://github.com/idomarhaim/Android_Final_Project/issues/60) does NOT
block on this** — the calendar renders fine from the task's four fields for one-off work, and a
repeating task simply shows once until this lands. Do not let #60 wait for it.

## What to build

1. **`occurrences`** — flat, **one document per *when***, carrying `googleEventId`, the
   confirmation state and the outcome.
2. **`repeatRule`** on the task, and **`pausedUntil: Long?`**.
3. **Security rules**, tested in `firestore-tests/` — the **only** layer that can test
   `firestore.rules`; the Kotlin suites cannot reach them, and saying so is required by
   `testing.instructions.md` if a layer is skipped.
4. The **"this occurrence, or all future ones?"** question. It is the reason the shape exists;
   a build that lands the documents and not this has not finished.

## The sharp edge — read §7.1 before writing the migration

**`isDone` splits three ways:** *stored* with no occurrences, **derived** with them, and
**absent** on a recurring task. Everything that reads `isDone` today assumes the first.

## Naming is yours, and say what you chose

§7.1 is explicit that `occurrences`, `completionFacts` and `planDrafts` are **that file's own**
names, picked for readability: *"the **shape** is decided, the spelling is the build session's."*
Choose well, and record the choice in the changelog so the next three sessions use the same word.

## Exit

- **JVM tests** for the rung/state logic and the recurrence expansion — `Occurrence.stateAt` is
  pure and this is where the value is.
- **`firestore-tests/`** green for the new collection's rules, run against the local emulator.
- **No device work is required.** No `adb`, no Gradle instrumented run, no
  `connectedDebugAndroidTest`.
- `CHANGELOG/2026-08-23/63-occurrences-and-recurrence.md`, with counts verbatim and the naming
  decision stated.

## Carries over

- The two KDoc blocks that specify this — `Task.kt` around `val occurrence`, and `Occurrence.kt`
  (especially line ~346, which defers the range to §4.3's calendar surface).
- The delta table — [`docs/PRODUCT_v0.3.md` §7.1](../docs/PRODUCT_v0.3.md).

## Out of scope

The calendar surface (#60), Google sync (#61) and the success/failure run (#64). This ticket
builds the thing all three read; it renders nothing.

---

## Result — `status: done`, 2026-08-23, `7c457c4`

Every item in *What to build* shipped, and the two readings the spec left open are recorded
where the next session will hit them rather than only here:

1. **`occurrences`** — `users/{uid}/occurrences/{id}`, **flat and per-user**, the owning task as
   a `taskId` field. Carries `googleEventId`, the confirmation state (the rung's `placement`,
   unchanged from `#56`) and the outcome.
2. **`repeatRule`** — a nested map on the task, `{unit, interval, weekdays, endKind, endDate,
   endCount}` — plus **`pausedUntil: Long?`**.
3. **Security rules** — `firestore.rules` **did not change**, and that is the finding: the
   owner-only `users/{uid}/{document=**}` match already covers the collection, the same result
   life areas produced. Six cases in `firestore-tests/` assert it, **mutation-checked**: narrowing
   the wildcard fails exactly **1 of 6**, the owner-succeeds case. The other five pass vacuously.
4. **"This occurrence, or all future ones?"** — `EditScope`, `ScheduleEdit`, `SchedulePlan` and
   the pure `ScheduleEdits.apply`. 14 tests, several of them the same edit under both scopes
   asserting that the two answers differ.

**Naming, as the brief asked:** the spelling stayed `occurrences`; the rest is in
`CHANGELOG/2026-08-23/63-occurrences-and-recurrence.md` §2, one row per decision.

**The sharp edge:** `Task.isDone` was **left as a `Boolean`** — it is the *stored* leg, correct
for every task in the database, and widening it would have rewritten every screen for a case
none can be in. The three-way answer is `TaskSchedule.doneness`, and `Inferred:` §7.1's *"absent
on a recurring task"* was read as **unbounded** recurrence alone.

**Owed elsewhere, named rather than left to be found:** points **per occurrence**.
`completionFacts` is keyed `{taskId}`, so a repeating task banks its points once; the
occurrence's outcome records *the window was honoured* and banks nothing. Widening that key is a
migration on live data and belongs to `#64`. `docs/PRODUCT_v0.3.md` §7.1 carries the same note.

**Held at close** (neither is a defect in the work): the **push**, because
`59-health-metric-mismatch`'s two commits are in the range and its board row is live; and the
**KB drain** of `kb-candidates/2026-08-23-63-occurrences.md`, because `65-measure-proposal` holds
`kb/dev/look-at-your-own-output.md` and `kb/log/` on the JARVIS board, which is where two of the
three entries go. The candidate file is committed, so nothing is lost.
