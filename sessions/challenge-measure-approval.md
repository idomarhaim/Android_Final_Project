---
repo: c:\Dev\Android_Final_Project
branch: main
mode: auto
status: ready
issue: 23
owns:
  - app/src/main/java/com/idomarhaim/goalpilot/domain/model/Challenge.kt
  - app/src/main/java/com/idomarhaim/goalpilot/domain/repository/ChallengeRepository.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/ChallengeRepositoryImpl.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Dtos.kt
  - app/src/main/java/com/idomarhaim/goalpilot/data/firestore/dto/Mappers.kt
  - app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/**
  - functions/src/**
  - functions/test/**
  - firestore.rules
  - firestore-tests/rules.test.mjs
  - app/src/test/java/com/idomarhaim/goalpilot/domain/ChallengeStandingsTest.kt
  - app/src/test/java/com/idomarhaim/goalpilot/feature/challenges/ChallengesViewModelTest.kt
  - CHANGELOG/<the day you run>/challenge-measure-approval.md
  - kb-candidates/<the day you run>-challenge-measure-approval.md
  - sessions/challenge-measure-approval.md
singletons:
  - Gradle daemon
  - the Firestore + Functions emulators (`firestore-tests/`, `functions/ test:emulator`)
created: 2026-08-24 by challenge-scoring
---

# Changing a challenge's measure needs every participant's approval — §6's last unbuilt bullet

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

## What §6 decided, verbatim

> **Changing the measure needs every participant's approval** — `C7` said this had nowhere to
> live, and it is **representable in the existing rules partition unchanged**: the owner writes
> `pendingMeasure` on the challenge document, **each participant writes `approvedChangeId` in
> the one document they are permitted to write**, and the Function applies it when every row
> agrees.

That shape is the decision and it is not yours to re-open. It exists because of Ido's own
answer on `C7` §5: the owner is offered **reset** *or* **an adaptation of the existing
history**, and *"if it is a shared challenge of 2 users, then the second user also has to
approve."*

## Why it matters, in one sentence

A challenge's measure is **the unit every participant's score is expressed in**, so changing
it mid-flight silently re-denominates other people's numbers — the same trust problem `#23`
already owned for typed scores, arriving from a direction nobody had listed.

## State of the ground, as of `challenge-scoring` (2026-08-24)

**Built and shipped**, so you inherit all of it:

- `Challenge.measure: Measure?` (`measureKind` + `measureWord` on the document), with a
  read-migration off the deleted `type` / `metricUnit` pair.
- `challenges/{id}` is owner-only; `participants/{uid}` is written by that participant alone,
  with `score`, `scoreSource` and `reportedAt` **pinned** by `serverOwns([...])` — which now
  takes a list, so adding a field to the pin is one array element.
- The projection is `republishStanding(uid, challengeId)` in `functions/src/projection.ts`,
  behind two triggers. **It already reads the challenge document** (for `startAt`/`endAt`), so
  the Function that applies an approved change has a natural home beside it.
- 17 real-trigger cases in `functions/test/triggers.emulator.mjs` and 55 rules cases.

⚠️ **Nothing today permits or prevents the change.** An owner may edit `measureKind` outright
and every participant's score is silently re-denominated. `firestore.rules` says so in a
comment naming this brief, rather than implying otherwise — **delete that comment when you
ship, and do not leave it half-true.**

## The three things this has to settle

1. **Reset or adapt.** Ido's words offer the owner both. *Reset* is easy — the scores go to
   zero. *Adapt* is the hard half and it is not obviously well-defined: a `DERIVED` score is a
   sum over the linked goal's entries, so re-denominating it means either converting the sum
   (which needs a conversion this app deliberately does not perform — `Measure`'s KDoc says
   the dimensional model was rejected) or **re-linking**, which is really *every participant
   picks a new goal*. Decide which of those *adapt* means, and say why.
2. **What happens while the change is pending.** Does the challenge keep scoring in the old
   unit? (It should — a half-approved change must not stop the race.) Does the standings row
   say a change is pending, and in whose words?
3. **Leaving during a pending change.** A participant who leaves has no row to write
   `approvedChangeId` in. Is *everyone who is still here* the quorum, or does leaving block
   it forever? This is the case that turns a clean rule into a stuck challenge.

## Read first

1. `AGENTS.md`
2. `docs/PRODUCT_v0.3.md` §6, and §1.3 for what a measure is
3. `CHANGELOG/2026-08-24/challenge-scoring.md` — §2.1 and §2.2 are the shapes you build on
4. [`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23), the comment of
   2026-08-10 09:39, which is where this clause was handed to `C14` in the first place

## Carries over

- `firestore.rules` — the comment on `match /challenges/{challengeId}` naming this brief
- `functions/src/projection.ts` — `republishStanding`, which already reads the challenge doc
- `CHANGELOG/2026-08-24/challenge-scoring.md` §5, which lists this as owed

## Out of scope

- The Health Connect join gate (`sessions/challenge-health-gate.md`)
- The device render pass (`sessions/challenge-scoring-render-pass.md`)
- Re-opening §6's mechanism. Owner writes `pendingMeasure`, participants write
  `approvedChangeId`, the Function applies it — that is decided.

## Exit

Green at every layer this touches — JVM, `functions/` arithmetic **and** its emulator trigger
suite, `firestore-tests/` — plus a `CHANGELOG/<day>/challenge-measure-approval.md`, and commit
on approval. **`#23` stays open or closed on its own merits**: check
`sessions/` and `sessions/done/` for siblings naming `issue: 23` before deciding, and note
that `#23` is already closed as a *decision* ticket — this is build work under it, so the
tracker write owed here is a comment, not a re-close.
