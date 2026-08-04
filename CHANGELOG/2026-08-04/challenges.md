# Changes — 04/08/2026 · session `challenges`

> **Branch:** `feat/goalpilot-implementation`

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

## ⚠️ Still open
- **Rules are not deployed.** Live `goalpilot-56e30` still has the old ruleset,
  so joining is still impossible against the real backend. Deploy is deliberately
  held until the client code exists, so the whole path can be verified in one
  pass: `firebase deploy --only firestore:rules`.
- The feature itself: `ChallengeRepository` + Firestore impl, the DI binding, and
  replacing `sampleChallenges` in `ChallengesScreen` with live data and a create
  flow. `Challenge.participantUids` / `Challenge.standings` need removing as part
  of that.
