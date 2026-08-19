# Changes — 05/08/2026 · session `challenges-ui`

> **Branch:** `feat/goalpilot-implementation`

Competitive challenges become a real screen. The `challenges` session built the
domain, data and DI layers and left the rules written but undeployed
(`1e56ee3`, `8117368`); this is the ViewModel, the live screen, and the create
flow on top of them. Spec §6 nice-to-have, §7 — the last remaining OPTIONAL item.

## 🎯 The preview screen is gone

`sampleChallenges` — three hard-coded rows with a chip that did nothing — is
replaced by two live lists: **Your challenges**, each with its standings and your
own rank, and **Discover**, which the repository has already filtered down to
challenges you are *not* in, so no Join button is ever a no-op.

`ChallengesViewModel` follows the house shape: one `StateFlow<ChallengesUiState>`
combining `observeMyChallenges()` and `observeDiscoverable()`, with the screen
stateless and driven by lambdas. The dialogs get their own small flows
(`editor`, `scoreEntry`, `detailId`, `message`), the same split
`LifeAreasViewModel` uses.

**Standings are passed through, never re-derived.** `rankedByScore` already
stamps joint ranks for ties (1, 1, 3) and flags the current user, so the
ViewModel maps and does not sort. A test asserts the 1, 1, 3 survives, because
re-ranking from list position is exactly the mistake the domain layer was written
to prevent.

## ⏳ Phase decides what the screen offers

`ChallengePhase` drives the UI rather than being decoration: no Join button on an
ended challenge, no score reporting before one starts — and in both cases a line
of text saying *why*, because a disabled button with no explanation reads as a
bug.

Phases are stamped from a clock at emission time, with **no ticker**.
`WhileSubscribed(5_000)` restarts the upstream flows when the screen is re-entered
after a pause, so every return re-derives them; a challenge crossing a boundary
while you watch is rarer than the recomposition-per-minute a ticker would cost.
The clock is an overridable property purely so phase boundaries are assertable
without waiting for one.

**The end date needed an off-by-one fixing, twice over.**

- `phaseAt` treats `endAt` as an **exclusive** bound (`now >= endAt` → ENDED). So
  a challenge that should run *through* the day the user picked ends at the
  **following** local midnight. Storing the picked day's own midnight would have
  ended a one-day challenge before it began.
- The Material date picker reports **UTC midnight** for the tapped day. Used
  as-is, that instant is the *previous* calendar day in any zone west of
  Greenwich. The day is extracted in UTC and re-anchored to local midnight.

Both live in `ChallengeDates.kt` — deliberately outside the Compose file, because
they are pure and each encodes an off-by-one that is only visible once. Three
JVM tests pin them, including that a same-day challenge is ACTIVE at noon on its
one day.

## 📝 Creating and scoring

The create dialog takes title, description, type, metric unit and two optional
dates. Picking a type re-suggests the unit (RUNNING → km, SLEEP → hours) **only
until the user types one** — the same `touched` rule the life-area editor uses
for its icon, so a chosen unit is never swapped out from under them. Both dates
empty means open-ended, which the model already reads as active rather than
expired in 1970.

Scores are entered in the challenge's own unit, and **a comma decimal is
accepted**: a keypad on a Hebrew or European locale offers one, and `12,5`
parsing to null would read as the app rejecting a real number. The dialog says
outright that a score *replaces* the total rather than adding to it — the
repository uses `update()`, and guessing wrong there is a competitor's whole
standing.

Validation is duplicated between the dialog and the repository on purpose: the
dialog is where the two dates were picked, so it is where the complaint belongs.

## ➕ Beyond the brief's four items

**Delete, for an owner.** The brief listed create / join / leave / report, but
`deleteChallenge` was already on the committed contract and an owner otherwise
could not remove a challenge they made. It is behind a confirm dialog that says
it disappears for everyone. Its two known limitations are unchanged and still
recorded: orphaned participant rows, and no "kick a participant".

**A standings sheet, not a nav destination.** The detail view is a
`ModalBottomSheet` inside the screen, so `ui/root/GoalPilotRoot.kt` was not
touched at all — navigation was already wired, and the sheet looks its challenge
up from the live list by id, so a score someone else reports moves the rows while
it is open.

## 🧪 Tests

- **`:app:testDebugUnitTest` — 175 tests, 0 failed, 0 skipped** (was 150).
  25 new in `ChallengesViewModelTest`: phase→affordance mapping, the exclusive
  end bound, rank pass-through, comma/negative/non-numeric score parsing,
  pre-fill behaviour, both create validations, the unit-suggestion `touched`
  rule, error text surfacing, sheet-closing on leave, stream failure, and the two
  date conversions.
- **`:app:connectedDebugAndroidTest` — 29 tests, 0 failed** (was 20). 9 new in
  `ChallengesUiTest`, driving the cards directly with no Hilt and no Firebase,
  the shape `LifeAreaReorderUiTest` established.
- **`firestore-tests` — 16 tests, 16 pass, 0 fail.** Re-run unchanged; this
  session edited no rules.
- First instrumented run died twice on the documented Windows KSP lock
  (`Could not delete .../generated/ksp`). Cleared with `rm -rf`; not a code error.

### What the UI test caught

`discoverCard_stillShowsAnEndedChallengeButWillNotJoinIt` failed on its first run
with *"expected 1 node but found 2"* for the text `Ended` — the phase chip said
it and the disabled button said it too. That is a real redundancy, not a test
artefact: one card carrying the same word twice reads as two states. The button
keeps its own verb (`Join`, disabled) and the phase is said once, on the chip.
**The fix went into the UI, not the assertion.**

## ⚠️ Still open

- **The rules are still not deployed — held deliberately, on Ido's call.**
  `firestore.rules` is unchanged and its 16 tests pass, but live
  `goalpilot-56e30` still carries the old ruleset, so a non-owner join fails
  against the real backend no matter how correct this client is.
  `firebase deploy --only firestore:rules` is the whole of it. It is paired with
  the two-account session on purpose: that is the only sitting in which the
  deploy can actually be *proven* rather than merely performed.
- **Non-owner join is verified by `firestore-tests` only, not end-to-end against
  the live backend.** Creating a challenge auto-joins the owner, so one account
  cannot exercise the path that was broken — that needs the second account, which
  is the two-account demo MUST item. Not reported as verified end-to-end,
  because it isn't.

## 📁 Modified / added
- `app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/ChallengesViewModel.kt` *(new)*
- `app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/ChallengeDialogs.kt` *(new)*
- `app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/ChallengeDates.kt` *(new)*
- `app/src/main/java/com/idomarhaim/goalpilot/feature/challenges/ChallengesScreen.kt` — live data
- `app/src/test/java/com/idomarhaim/goalpilot/feature/challenges/ChallengesViewModelTest.kt` *(new)*
- `app/src/androidTest/java/com/idomarhaim/goalpilot/ui/ChallengesUiTest.kt` *(new)*
- `CHANGELOG/CHANGELOG_README.md`, this entry *(new)*
- `SESSIONS.md`, `sessions/challenges-ui.md`

## 🧭 Board
Claimed `challenges-ui` before the first write (`02e7f98`) against an empty board.
Emulator `Pixel_10_Pro_XL` and the Gradle daemon were used and released; the live
Firebase project was **not** touched — the deploy is Ido's call.
