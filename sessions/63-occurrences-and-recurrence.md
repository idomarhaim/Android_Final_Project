---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 63
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Occurrence.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Task.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/RepeatRule.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Mappers.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/**
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
