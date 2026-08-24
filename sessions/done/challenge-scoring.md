---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: done
issue: 23
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Challenge.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/ChallengeRepository.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/ChallengeRepositoryImpl.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/**
  - app/src/main/java/com/idomarhaim/goalpilot/domain/usecase/SyncHealthDataUseCase.kt
  - functions/src/**
  - firestore.rules
  - app/src/test/java/com/idomarhaim/goalpilot/feature/challenges/**
  - CHANGELOG/<the day you run>/challenge-scoring.md
  - kb-candidates/<the day you run>-challenge-scoring.md
  - sessions/challenge-scoring.md
singletons:
  - Gradle daemon
  - a device, for the standings render pass
created: 2026-08-24 by s25-verify-on-real-phone
result: |
  Built and committed 2026-08-24. Green at four layers (JVM 1125/1125, functions
  arithmetic 105/105, real emulator triggers 17/17, security rules 55/55).
  OWED, each with its own brief rather than as a line in a changelog:
    - sessions/challenge-scoring-render-pass.md  -- the instrumented suite and the
      device render pass, blocked because `emulator-5554` was claimed on the board.
    - sessions/challenge-measure-approval.md     -- section 6's every-participant
      approval for changing a measure. Nothing today permits OR prevents it.
    - sessions/challenge-health-gate.md          -- the Health Connect join gate, and
      section 6's "joining links OR CREATES a goal", of which only linking is built.
  Step 0 discharged on arrival: C7 (#14) and C3 (#18) are both closed, so the
  conditional note to Ido this brief made contingent on an open C7 was not owed.
  The brief's "needs C3's start" turned out to be about a GOAL's baseline, not this:
  since #49 the entry IS the progress, so movement-since-joining is a windowed sum
  over the same documents and needs no start field.
---

# `C14` / [`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23) — a challenge scores itself, and says who moved it

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

## Ido answered the open question, 2026-08-24

Verbatim, because this is a product decision and it must not live only in a chat log:

> *"I want the challenge to be synced to HEALTH CONNECT (according to the challenge
> type) and generally to tasks. If someone updated manually and it was not updated
> through HEALTH CONNECT, then it should say there who performed the update and what
> they updated."*

Three asks, and the third is new to this ticket:

1. **A challenge's number moves on its own**, from Health Connect where the challenge
   is a health kind, and from **tasks** otherwise.
2. **According to the challenge type** — `STEPS` reads steps, `SLEEP` reads sleep.
3. ⭐ **A manually reported score is labelled as one, with who and what.** This is
   not in `C14`'s enumeration and not in `PRODUCT_v0.3.md` §6. It is Ido's own
   addition and it is the most valuable part of the ask: on a shared leaderboard, a
   reading and a typed number are not the same claim, and today they are pixel
   identical.

## What is already decided, and how it reconciles

`docs/PRODUCT_v0.3.md` §6 adjudicated this ticket already:

> **A challenge scores from nothing of its own: it scores from each participant's goal.**

⚠️ **Read quickly, that looks like it contradicts Ido. It does not, and the
reconciliation is the whole design:** a goal is *already* the thing Health Connect
and task completions both feed. `SyncHealthDataUseCase` writes a `ProgressEntry`
against a goal; ticking a task writes against a goal. So *"score from the goal"* **is**
*"synced to Health Connect and to tasks"* — routed through the object that already
aggregates both, instead of a second pipe per source. §6's own words: the fix
*"deletes one rather than building a second pipe into it"*.

**Do not build a Health-Connect→challenge path.** It would be the second
representation of the same walk that §6 exists to remove.

The one genuine tension: §6 says **`ChallengeType` is deleted** ("purely
presentational; nothing branched on it to source a score"), while Ido says *"according
to the challenge type"*. Under §6 the *link to a goal* carries the unit, so the type
is not needed to source anything. **Resolve it by asking Ido**, and put the question
as a situation rather than a mechanism: *when you make a Steps Race, do you pick
"steps" from a list, or do you point it at your own steps goal?*

Everything else §6 fixes, carried here unchanged:

- **Score is *movement since you joined*, not the goal's current value** — otherwise a
  weight-loss race ranks by who is heaviest. Summed from timestamped entries, so
  relink / unlink / backfill / dedup all fall out and joining with a year-old goal
  imports no history. Needs `C3`'s `start`.
- **No optional measure** — nothing to compare without a shared unit.
- **`points` may never be a challenge metric** — it ranks by time logged.
  `metricUnit = "points"` is deleted rather than re-homed.
- **Joining links or creates a goal**, so a challenge hands you tracking you lacked.
- **`score` becomes server-owned** — the client writes the fact, a Cloud Function owns
  the derived number. Honest residual: this stops a win being *typed*, not a reading
  being *forged*.
- **No Health Connect connection → you cannot join a health-sourced challenge.** Ido's
  earlier call, honoured verbatim: comparability over inclusion, with the gate made a
  route rather than a dead end.
- **Changing the measure needs every participant's approval** — owner writes
  `pendingMeasure`, each participant writes `approvedChangeId` in the one document
  they may write, the Function applies it when every row agrees.

## Ask 3 — provenance on the standings row

Nothing in the spec covers this yet. What it needs, at minimum:

- `ChallengeParticipant` gains the provenance of its current score: **derived** (from
  the linked goal) or **reported** (typed), plus **who** and **what** — the uid /
  display name that wrote it, the value, and when.
- The standings row shows it. A derived row needs no badge; a **reported** one says so,
  and says who and what. Design it so the *absence* of a badge is the honest default
  and the badge is the exception, not the reverse.
- ⚠️ It is a claim about another user, rendered to everyone in the challenge, so keep
  it factual and unarguable — *"reported by Ido · 8,200 steps · 24 Aug"* — never
  anything that reads as an accusation. `C4`'s rule (the app never asserts an intrinsic
  edge by itself) is the register to match.
- `firestore.rules`: a participant writes only their own row, so provenance is
  self-asserted by construction. Say that plainly in the KDoc rather than implying the
  badge is proof of anything — it is a **label**, not an attestation.

## Blocked on, and this is the first thing to check

`C14` says the decision *"cannot be taken without `C7`"* — what a *unit* is — because
`Challenge.metricUnit` is free text with the identical disease as `Goal.unit` (`A3`).
§6 was written after that and assumes a measure model.

**Step 0: read `C7`'s state before writing any code.** If `C7` is still open, the
honest first move is a short note to Ido saying which of this brief is blocked on it,
not a half-built scoring path against free-text units.

## Exit

Green tests at every layer this touches — JVM for the scoring arithmetic and the
provenance model, `firestore-tests/` for any rules change, a device render pass for
the standings row — plus this session's own
`CHANGELOG/<day>/challenge-scoring.md`, and commit on approval.

## Out of scope

- The calendar, the tour, the sync cards, the analytics run. All shipped in v0.4.0
  and v0.4.1 and verified on Ido's own phone.
- `#62`'s film and the exam material. Other sessions hold those.
