# c14-challenge-scoring — a challenge scores from nothing of its own

> **Summary:** a challenge scores from nothing of its own

**Session:** `c14-challenge-scoring` · **Date:** 2026-08-10 · **Mode:** `AUTO MODE` (from Ido's first message)
**Branch:** `feat/goalpilot-implementation` · **Ticket:** [#23 · `C14`](https://github.com/idomarhaim/Android_Final_Project/issues/23) on map [#12](https://github.com/idomarhaim/Android_Final_Project/issues/12)

## What this session did

`/wayfinder 12` invoked **bare**, so the frontier pick was the agent's. Frontier re-derived
out of the GitHub dependencies API per open child of `#12` — **four** takeable tickets, not
the one the board's prose still implied: `#21 · C5`, `#23 · C14`, `#28 · C9e`, `#31 · C12`.
Claimed `#23` because it is the only one whose subject lives in a **different subsystem**
than every live sibling; the other three were each declined for a different reason, all
recorded on `SESSIONS.md`.

Ido answered one of four picker questions and handed the other three back with his standing
instruction — *take the highest-quality answer and improve it* — so the pick is the agent's
and on the record, exactly as `C3` recorded earlier the same day. The one he answered
himself (**no data source → you cannot join**) is honoured verbatim in §6 of the resolution.

## The decision

**A challenge scores from each participant's own goal, and from nothing of its own.**

- **The enumeration needed no adjudication.** `(a) goal progress · (b) task count · (c) a
  health metric · (d) a typed number · (e) per `ChallengeType`` are not five rivals: **(a) is
  the answer and (b), (c), (d) are how a goal is fed** — decided elsewhere on this map.
- **`R1` is not missing wiring.** Put a goal and a challenge side by side after `C4` and `C7`
  and they are the same object. *"August Steps Race: #2 · 0 steps"* beside steps flowing into
  goals is **two representations of the same walk**; the fix deletes one rather than building
  a second pipe. `SyncHealthDataUseCase` already writes a `ProgressEntry` against a goal and
  `ProgressEntry.sourceKey` already dedupes a re-sync — the whole left-hand side existed.
- **Score = movement since you joined**, not the goal's current value. Only possible because
  `C3` §3 landed `start` hours earlier; without it a weight-loss race ranks by who is
  heaviest. Summed from timestamped entries rather than stored as a delta, so relink,
  unlink, backfill and dedup fall out for free.
- **A challenge has no optional measure** (`C7` §2's optionality does not carry — there is
  nothing to compare without a shared unit), **`points` may never be one** (`C3` §1 — that
  ranks by time logged), and `metricUnit = "points"` is deleted rather than re-homed.
- **Joining links or creates a goal**, so a challenge hands you tracking you did not have.
- **`score` becomes server-owned** — client writes the fact, a Firestore trigger owns the
  derived number. Third site of [#34](https://github.com/idomarhaim/Android_Final_Project/issues/34)'s
  pattern, and [`firestore.rules:53`](../../firestore.rules) already records the coupling to
  `publicProfiles.points` in its own comment.
- **`C7` §5's *"changing a measure needs every participant's approval"* — which `C7` said had
  nowhere to live — is representable in the existing rules partition unchanged.** Owner
  writes `pendingMeasure` on the challenge document; each participant writes
  `approvedChangeId` in the one document they are permitted to write; the Function applies it
  when every row agrees. No new privilege, no new collection, no rules restructuring.
- **`ChallengeType` is deleted**, with a lossless migration onto `kind` + `word`.

## Artifacts

- Resolution comment on [#23](https://github.com/idomarhaim/Android_Final_Project/issues/23#issuecomment-5244202318); ticket **closed**.
- Hand-off to [#19 · `C1`](https://github.com/idomarhaim/Android_Final_Project/issues/19#issuecomment-5244218338) — the trust verdict, posted rather than decided, since `#19` is unclaimed and still blocked behind `#39`.
- Hand-off to [#31 · `C12`](https://github.com/idomarhaim/Android_Final_Project/issues/31#issuecomment-5244221145) — standings are a ranked list in one shared unit, overshoot is legal, and a score can now fall.
- `#12` map body: **one** Decisions-so-far line appended, the **"what becomes of `ChallengeType`"** fog patch cleared (answered, not graduated), and one new fog patch — *who owns derived state, now that three places want the same answer*. Re-fetched immediately before writing and verified byte-identical afterwards; net **+3 / −1** lines.
- Filed **no** new tickets: nothing this resolution surfaced was sharp enough to phrase as a question, which is the map's own fog test.

## 🧪 Tests

**No test layer applies, and this is stated rather than skipped.** This session changed **no
code** — it resolved a decision ticket. The repo's layers (server unit, endpoints, database
rules via `firestore-tests/rules.test.mjs`, client component, UI E2E) all exist but have
nothing to exercise here; no Gradle, `adb` or Firebase singleton was claimed or touched.

The resolution **specifies** two test obligations for the build session that implements it,
recorded here so they are not lost: the `score`-is-not-client-writable rule belongs in the
existing `firestore-tests/rules.test.mjs` harness, and the movement-summing Function needs
unit coverage for the falling-series case (`current` moving toward a `target` below `start`),
which is the one `reportScore`'s deleted `if (score < 0)` guard used to reject.

## Verification of claims

Every code claim in the resolution was read, not recalled:

- `firestore.rules:42-59` — the owner/participant write partition, and the line-53 comment tying scores to `publicProfiles.points`.
- `ChallengeRepositoryImpl.kt` — `createChallenge`, `joinChallenge`, `leaveChallenge`, `reportScore` (single writer of `score`), and `myEdgesCol` = `users/{uid}/challenges/{challengeId}` (the mirror-edge path was **corrected** before posting; it had been written from inference).
- `Challenge.kt` — `metricUnit = "points"` default, `ChallengeType` presentational, `rankedByScore` standard competition ranking.
- `ProgressEntry.kt` — `sourceKey` dedup; `ProgressRepositoryImpl.kt:40-43` — `users/{uid}/goals/{goalId}/progress`.
- `functions/src/index.ts` — three `onCall` handlers, no Firestore trigger yet.
- Two `#issuecomment-` URLs in the board row had been written from memory and were **wrong**; replaced with the API's before committing.

## Board

`SESSIONS.md` claim written **after** assigning `#23` on GitHub and **before** the first
write, as a pure insertion (64 added, 0 deleted) with `c9b-calendar-surface` live and
uncommitted in the same tree; staged by explicit path. Released on completion.
