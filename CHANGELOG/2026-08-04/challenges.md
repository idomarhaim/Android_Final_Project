# Changes — 04/08/2026 · session `challenges`

> **Branch:** `feat/goalpilot-implementation`
> **Summary:** Groundwork for competitive challenges (spec §6 nice-to-have, §7).

Groundwork for competitive challenges (spec §6 nice-to-have, §7). No UI yet:
this is the security-rules change that makes joining possible at all, plus the
test layer that proves it.

## 🔐 Joining was impossible, and the rules were the reason

`firestore.rules` allowed `update` on a challenge only to its owner. Joining, as
the code was modelled, meant a participant appending their own uid to
`Challenge.participantUids` — an update to the challenge document. A joiner has
no write access to that document, so **every join would have been denied**. The
preview screen hid this because it never wrote anything.

Participation now lives in `challenges/{id}/participants/{uid}`, one row per
person, writable only by that person. A subcollection is not covered by the
parent `match`, so before this change those documents matched no rule at all and
were denied by default.

**This is a data-model change, not only a rules change.** Two fields on
`Challenge` are dead on arrival and must come from the subcollection instead:

- `participantUids` — a joiner cannot maintain it; see above.
- `standings` — same document, same problem.

And "which challenges am I in?" can no longer be `array-contains` on
`participantUids`. It becomes a mirror edge at `users/{uid}/challenges/{id}`,
which needs **no further rules change** (the existing owner-only
`users/{uid}/{document=**}` match already covers it) and mirrors the shape the
app already uses for friends (`users/{uid}/friends/{friendUid}`). The
alternative — a collection-group query over `participants` — would have needed
its own index and its own rules line for no gain.

**Standings are computed client-side**, against the TODO's recommendation of a
Cloud Function. A function would have put this session in
`functions/src/index.ts`, which `sessions/time-insights.md` also needs, and that
is the one file two sessions cannot share. Recorded on `SESSIONS.md` so a later
session moving standings server-side knows what it collides with.

Scores stay client-written, exactly as `publicProfiles.points` already is. That
is not a new hole — it is the same posture, and the existing `TODO_FUTURE`
anti-cheat item covers moving both server-side together.

**Known limitation, deliberate:** there is no "kick a participant". Only the
participant can write their own row. Granting the owner that power needs a
`get()` on the parent challenge inside the rule, which bills a document read on
every evaluation.

## 🧪 Tests

**New layer: `firestore-tests/`** — `@firebase/rules-unit-testing` against the
local Firestore emulator under project id `demo-goalpilot`. The `demo-` prefix
is load-bearing: the emulator treats such ids as offline-only, so a rules test
can never reach live `goalpilot-56e30`. Run with `npm test` in that folder.

This is the project's **first database/security-rules test layer**; before this
change no layer existed that could test `firestore.rules` at all.

- **16 tests, 16 pass, 0 fail** against the fixed rules.
- **Control run against the pre-fix rules: 12 pass, 4 fail** — and the four that
  fail are exactly the four that should (join, update own score, leave, read
  standings). Without this control the suite would have been worthless; see
  below.
- Kotlin/JVM and instrumented layers: **not run and not affected** — no app code
  was touched in this change.

### Why the control run is in this entry

The first verification attempt was `firebase emulators:exec`, which came back
clean. It was then run against a **deliberately broken** rules file — undefined
function, missing brace — and came back clean too, exit 0. The emulator does not
load rules until a client connects, so that check detects nothing and the first
"pass" meant nothing. The rules-unit-testing harness loads the ruleset
explicitly, which is why it can fail.

Worth keeping: the negative tests ("a user cannot write another user's row",
"a signed-out visitor cannot join") pass under **both** rulesets. Under the old
rules they pass vacuously — everything was denied because no rule matched. A
negative test alone could not have caught this bug.

## 📁 Modified / added
- `firestore.rules` — participants subcollection
- `firestore-tests/package.json`, `firestore-tests/rules.test.mjs` *(new)*
- `AGENTS.md` — commands + layout lines for the new test folder
- `CHANGELOG/CHANGELOG_README.md`, this entry *(new)*
- `SESSIONS.md` — claim row

## 🧭 Board
Claimed `challenges` before the first write (`9954822`). Claim extended twice as
scope became clear — `firestore-tests/`, then the changelog/AGENTS paths — rather
than writing outside the row. Still active; not released.

## 🏗️ Domain + data layer

The model no longer describes something the rules forbid. `participantUids` and
`standings` are gone from `Challenge`; `ChallengeParticipant` is one self-owned
row and `ChallengeWithStandings` is what a screen actually needs. The KDoc on
`Challenge` says why, so they do not get re-added.

**Standings use standard competition ranking, deliberately unlike the
leaderboard.** `rankedByPoints` stamps `index + 1`, so two people on the same
score render as #1 and #2. On a leaderboard an arbitrary tiebreak is cosmetic;
on the thing people compete over it is wrong. `rankedByScore` gives joint ranks
with the next skipping — 1, 1, 3.

`phaseAt` reads a challenge with no dates as **active, not expired in 1970**, and
uses half-open bounds so an instant belongs to exactly one phase — matching
`AnalyticsRange`.

`ChallengeRepositoryImpl` — three collections, each write aimed at whichever one
the rules actually permit:
- **Create and join commit in one batch.** An owner missing from their own
  standings reads as a bug the first time anyone else joins.
- **`reportScore` uses `update()`, not a merging `set()`.** A merge would happily
  create a participant row with no mirror edge, leaving someone scoring in a
  challenge that never appears in their own list.
- **`joinChallenge` checks the challenge exists first** — the same guard
  `addFriend` already uses against edges that point at nothing.
- `observeMyChallenges` walks the mirror edges and opens one pair of listeners
  per challenge rather than a chunked `whereIn`: the participants listeners are
  needed for standings anyway, so the query would have merged only the other half.

**Two limitations left visible rather than hidden.** Deleting a challenge
orphans its participant rows (Firestore does not cascade — the existing
`TODO_FUTURE` cascade-delete item covers it), and there is no "kick a
participant": granting the owner that power needs a `get()` on the parent inside
the rule, billing a document read on every evaluation.

### 🧪 Tests
- `:app:testDebugUnitTest` — **106 tests, 0 failed, 0 skipped** (was 92).
  New: `ChallengeStandingsTest`, 14 cases covering ordering, joint ranks, a
  three-way tie, snapshot-stable tie ordering, current-user flagging, zero-score
  participants, and all five phase boundaries.
- Instrumented layer **not run**: no composable changed in this pass.
- First Gradle run died on the documented Windows KSP lock
  (`Could not delete .../generated/ksp/debug/classes`). Cleared with
  `rm -rf app/build/generated/ksp`; not a code error.

## ⚠️ Still open
- **Rules are not deployed.** Live `goalpilot-56e30` still has the old ruleset,
  so joining is still impossible against the real backend. Deploy is deliberately
  held until the client code exists, so the whole path can be verified in one
  pass: `firebase deploy --only firestore:rules`.
- **The UI.** `ChallengesViewModel`, and replacing `sampleChallenges` in
  `ChallengesScreen` with live data, a create flow, join/leave and score
  reporting. The repository contract it consumes is committed and stable.
