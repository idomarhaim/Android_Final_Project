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
  - app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/**
  - app/src/test/java/com/idomarhaim/goalpilot/feature/challenges/ChallengesViewModelTest.kt
  - app/src/test/java/com/idomarhaim/goalpilot/domain/ChallengeStandingsTest.kt
  - CHANGELOG/<the day you run>/challenge-health-gate.md
  - kb-candidates/<the day you run>-challenge-health-gate.md
  - sessions/challenge-health-gate.md
singletons:
  - Gradle daemon
  - a device, for the join flow and the Health Connect route
created: 2026-08-24 by challenge-scoring
---

# Joining a challenge: the Health Connect gate, and the goal it is supposed to create

**Repo** `c:\Dev\Android_Final_Project`, branch `main` · **Mode** `auto`

## Two of §6's bullets, and they are one unit because they are the same moment

> **Joining links or creates a goal**, so a challenge hands you tracking you did not have.

> **No Health Connect connection → you cannot join a health-sourced challenge.** Ido's own
> call, honoured verbatim: **comparability over inclusion**, with the gate made **a route
> rather than a dead end**.

Both fire when somebody taps *Join*, and building either without the other produces a
half-answer: a gate with no route is the dead end Ido explicitly ruled out, and a *create*
with no gate creates a goal nothing will ever feed.

## ⚠️ Step 0 — a decision this cannot start without

**Once `ChallengeType` is deleted, what makes a challenge "health-sourced"?**

`challenge-scoring` deleted the enum on 2026-08-24, per §6 (*"purely presentational; nothing
branched on it to source a score"*). It was also the only thing that ever said *this challenge
is about steps*. So the gate's own subject no longer exists, and the obvious substitute does
not work: **a measure kind is not it** — a `COUNT` of books and a `COUNT` of steps are the same
kind, and gating book races on Health Connect would be absurd.

Three candidates, and none is free:

| candidate | what it means | what it costs |
|---|---|---|
| the owner ticks *"this is a health challenge"* | one boolean on the challenge document | a self-asserted flag, and the owner may be wrong or careless |
| derive it from the **linked goals** | a challenge is health-sourced if participants link goals carrying a `healthSourceKey` | it is not knowable at **join** time, which is the only moment the gate has |
| don't gate at all | anybody joins, links whatever goal they have | contradicts Ido's own recorded call |

**Put this to Ido as a situation, not a mechanism** — the ❓ rule's own remedy for a question
about a mechanism: *"you make a Steps Race and a friend has never connected Health Connect.
Should they be able to join and type their steps in by hand, or should the app send them to
connect first?"* Derive the field from his answer.

## What is already true

`Observed:` in `challenge-scoring` (2026-08-24) — read
`CHANGELOG/2026-08-24/challenge-scoring.md` before assuming any of it:

- `joinChallenge` writes a participant row and a mirror edge. It asks nothing and gates
  nothing.
- **Linking is built.** `linkGoal(challengeId, goalId)` writes `{ goalId, linkedAt }` to
  `users/{uid}/challengeReports/{challengeId}`, and two triggers keep the standing summed
  from that goal's progress. `Challenge.canBeScoredFrom(goal)` matches on the measure
  **kind**, never the word, and excludes archived goals.
- **Creating is not.** A user with no goal of the right kind gets a first-class message naming
  the kind to make and a trip to the Goals screen. Honest, and one screen short of §6's
  promise — `GoalLinkSheet`'s empty branch is where the *create* belongs.
- `HealthMetric` (`domain/usecase/`) already maps a health source to a `MeasureKind`, and
  `GoalDto.resolvedMeasure` already uses `healthSourceKey` to classify a legacy goal. That is
  the machinery a *create* would reuse; do not build a second one.

## Task

1. Settle Step 0 with Ido and record the answer verbatim in the changelog.
2. Build the gate as a **route**: a participant who cannot join is told what to connect and
   taken there, never shown a disabled button with no explanation.
3. Build *joining creates a goal*: from the challenge's own measure, pre-titled from the
   challenge, linked in the same act, so *Join* leaves the user with tracking they did not
   have.
4. `firestore.rules` is **untouched** by all of this if you keep the gate client-side. If you
   decide it must be enforced server-side, say why — a joiner writes only their own row, so
   the rule would have to read a document it does not currently see.

## Read first

1. `AGENTS.md`
2. `docs/PRODUCT_v0.3.md` §6, §1.3, and §4.6
3. `CHANGELOG/2026-08-24/challenge-scoring.md` — §2.3 and §2.5
4. [`#23`](https://github.com/idomarhaim/Android_Final_Project/issues/23)'s resolution comment,
   which is where *comparability over inclusion* is recorded as Ido's own word
5. [`#59`](https://github.com/idomarhaim/Android_Final_Project/issues/59) — Health Connect
   steps credited to an unrelated goal. Closed, but it is the shape of what goes wrong when a
   health source and a goal are matched loosely

## Carries over

- `app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/ChallengeDialogs.kt` —
  `GoalLinkSheet`'s empty-state branch, which names the *create* it does not do
- `CHANGELOG/2026-08-24/challenge-scoring.md` §5, which lists both halves as owed

## Out of scope

- The measure-approval flow (`sessions/challenge-measure-approval.md`)
- Re-opening how a challenge is scored. Linking, the window and the provenance badge all
  shipped and are not this brief's to revisit.

## Exit

Green at every layer this touches, a device run of the join flow in **both** branches (with
and without a Health Connect connection), a
`CHANGELOG/<day>/challenge-health-gate.md`, and commit on approval. Ido's Step 0 answer goes
in that changelog **verbatim** — it is a product decision and must not live only in a chat
log.
