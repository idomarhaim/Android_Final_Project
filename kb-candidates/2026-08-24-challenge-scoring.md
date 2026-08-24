# KB candidates — `challenge-scoring`, 2026-08-24

Written per `rules/memory-promotion.md`. Each entry stands alone: no entry may be
reconstructed from this session's transcript, which is machine-local and invisible to
whoever drains this file.

---

## 1 · A tool's *warning* line can turn a whole suite red, and the failures name the wrong cause

**Claim.** `firebase emulators:exec --only firestore,functions` prints **one warning** when
the functions analyzer exceeds its 10-second discovery budget —

```
!! functions: Failed to load function definition from source: FirebaseError: User code
   failed to load. Cannot determine backend specification. Timeout after 10000.
```

— and then **runs the suite anyway with no functions registered**. Every trigger assertion
times out. `Observed:` 2026-08-24, GoalPilot: **15 of 17 failed**, including every
pre-existing test the session had not touched, and the only two that passed were the two
that assert a trigger does *nothing*. The fix is `FUNCTIONS_DISCOVERY_TIMEOUT=120` in the
environment of the `emulators:exec` process.

**Why.** The failure mode is not "a tool errored". It is **a warning that changes the
meaning of every subsequent result**, and the results themselves are individually plausible:
a session reading them bottom-up sees its own new trigger among the failures and concludes it
broke the old ones. The refutation is one command and takes two seconds —

```
node -e "const t=Date.now();const m=require('./lib/index.js');console.log(Date.now()-t,'ms',Object.keys(m))"
```

`Observed:` **212 ms**, all eight exports listed, immediately after that failure. So the
general lesson is the interesting half: **when a suite fails wholesale — including tests the
change could not have reached — read the runner's own startup output before reading a single
assertion.** A pattern of failures that is *too broad for the change* is evidence about the
harness, not about the code. Related to, but distinct from, `look-at-your-own-output.md` §4k
(*print `wc -l` beside every count*): there the instrument returned a wrong number silently;
here it returned an honest one after announcing, once, that it was measuring nothing.

**Rejected:** raising the per-test deadline (treats the symptom and makes a real regression
take longer to report); bisecting the change (would have cost an hour and found nothing,
because nothing in the change was wrong).

**Destination.** `kb/dev/` — extend `look-at-your-own-output.md` with a clause on
*wholesale* failure, or a short page of its own on reading a harness's startup line. **And a
project-local half**: `C:\Dev\Android_Final_Project\CLAUDE.md` already records
`FUNCTIONS_DISCOVERY_TIMEOUT=120` for `firebase deploy --only functions` and says its error
"names the wrong cause" — it does not say the trap also bites `emulators:exec`, which is the
form a *test* session meets first.

**Anchors.** `functions/test/run-emulator-tests.mjs` (header + the env line, which now sets
it and prints it); `CHANGELOG/2026-08-24/challenge-scoring.md` §3.1;
`C:\Dev\Android_Final_Project\CLAUDE.md`, the `firebase deploy` bullet.

**Supersedes.** Nothing. It **extends** the CLAUDE.md deploy bullet to a second command.

**Status.** Ready. The project-local half is a one-bullet edit to a `CLAUDE.md` this session
does not own a claim on; the KB half is `/kb-ingest`'s.

---

## 2 · A shared fixture that pins two implementations must not be fed a one-sided case

**Claim.** GoalPilot's `shared-fixtures/derived-state.json` exists because `C20`/§5.2 moved
derived-state arithmetic to the server, leaving it implemented in **Kotlin and TypeScript**;
the fixture is read by both suites so neither can be edited into agreement with itself. This
session added arithmetic — the challenge-scoring window and its sum — that has **no Kotlin
twin**, because §6 makes the challenge score server-owned and the client only renders it. The
cases went into `functions/test/projection.test.mjs` as a local table, **not** into the
fixture, with the reason written where the next person will look.

**Why.** The shape of the fixture invites the mistake: it has named sections, adding one is
easy, and a session that adds `challengeScoreCases` has done something that *looks* like
following the convention. But writing a Kotlin mirror purely so the fixture has two readers
**manufactures the second implementation the fixture exists to police** — the duplication
becomes real, and it is now load-bearing for a test rather than for a product. The general
rule: **a cross-language fixture is a contract between two existing implementations, never a
reason to create the second one.** The honest move is a local table plus a note saying what
would move it into the fixture (here: the day a client computes a challenge score).

**Rejected:** adding the cases to the fixture and writing a thin Kotlin mirror (creates the
duplication); adding a fixture section only one side reads (quietly falsifies the file's own
stated contract, *"one copy read from two sides"*).

**Destination.** `kb/dev/` — a page or clause on shared/golden fixtures across languages.
Plausibly belongs beside whatever records `C20`'s honest-residual pattern.

**Anchors.** `shared-fixtures/derived-state.json` (its `$comment` block states the contract);
`functions/test/projection.test.mjs`, the §6 block and the comment above it;
`app/src/test/java/com/idomarhaim/goalpilot/derived/DerivedStateFixtureTest.kt`.

**Supersedes.** Nothing.

**Status.** Ready.

---

## 3 · A label about a person is a different artifact from a label about a number

**Claim.** Ido asked (2026-08-24, `#23`) that a manually reported challenge score *"say there
who performed the update and what they updated."* Built, and the design decisions that made
it safe are reusable: **(a)** the badge draws **only on the exception** — a derived score and
an unscored row draw nothing, so absence is the honest default rather than missing data;
**(b)** it never re-ranks — a typed score sorts exactly where its value puts it; **(c)** it
carries no warning icon, error colour or word like *unverified*; **(d)** the field it reads is
**server-written and rules-pinned**, because a participant who could write their own label
could type a number and mark it derived, and the label would then assert the one thing it
exists to deny; **(e)** the KDoc, the rules comment and the arithmetic all say in the same
words that it is a **label, not an attestation** — it says what this person *did*, never what
is *true about the walk*.

**Why.** The whole class — provenance badges, "edited" markers, AI-generated flags, source
attributions — shares one failure: the moment a factual label is rendered next to a person's
name in a competitive context, readers infer an accusation the product never made. (d) and (e)
are the two that are easy to skip and expensive to retrofit: a self-asserted label is worse
than no label, because it looks like evidence.

**Rejected:** ranking a typed score below a derived one (the app asserting something it
cannot know); badging every row with its source (makes the ordinary case look remarkable and
buries the exception); publishing the *linked goal's title* on the world-readable row (leaks a
goal's identity for a badge that only needed to say *derived*).

**Destination.** `kb/product/` or `kb/design/` — a page on provenance labels in social
surfaces. The register rule it instantiates (`C4`: *the app never asserts an intrinsic edge by
itself*) is GoalPilot's, but the pattern is not.

**Anchors.** `app/src/main/java/com/idomarhaim/goalpilot/domain/model/Challenge.kt`
(`ScoreSource`, `ChallengeParticipant`'s KDoc);
`.../feature/challenges/ChallengeDialogs.kt` (`ReportedBadge`); `firestore.rules`, the
participants block; `CHANGELOG/2026-08-24/challenge-scoring.md` §2.4.

**Supersedes.** Nothing.

**Status.** Ready.

---

## 4 · Two board rows can claim one device under two different names

**Claim.** GoalPilot's board had `62-tour-assembly` holding `emulator-5554` while
`s25-verify-on-real-phone` had taken, used and **released** `Pixel_10_Pro_XL_B` — and a 🚨
note on the board established that those are **the same serial**. So the board's two most
recent statements about one device disagreed, and neither session was wrong on its own terms.
Resolved by *unresolved counts as live*: the device was left alone, the two device-dependent
test layers were reported `unverified` rather than skipped, an addressed note was left below
the holder's row, and the owed work became its own brief.

**Why.** The singleton column is claimed **by name**, and a device has at least three names —
the AVD name, the adb serial, and whatever the session calls it in prose. `Claim paths, not
themes` has an unwritten sibling: **claim a singleton by the identifier the tool uses**, which
for a device is the `adb` serial, and record the mapping when you first resolve it
(`adb -s <serial> emu avd name`). Without that, two sessions can hold one machine while both
believe they are being careful.

**Why it matters more than tidiness:** the holder's row also carried a pending *revert of demo
data on Ido's live account*. Taking the device on the reasoning that the other session looked
finished would have risked a live-account operation, which is exactly the blast radius the
singleton rule exists for.

**Rejected:** treating a 4-hour-quiet row as released (§5.3's absent-row branch does not apply
to a row that is *present*); asking Ido (the fork turns on a board fact, not on his values);
using the device and mentioning it afterwards (a reply is not a gate).

**Destination.** `rules/agent-topology-and-model-routing.md` §5 — the singleton clause.
⚠️ **`rules/` is always-ask in both modes** (`memory-promotion.md`), and the 🎬
walkthrough rule owns it. **Do not drain this entry without Ido.**

**Anchors.** `SESSIONS.md`, the `challenge-scoring` release note and the note addressed to
`62-tour-assembly`; `CHANGELOG/2026-08-24/challenge-scoring.md` §6;
`sessions/challenge-scoring-render-pass.md`.

**Supersedes.** Nothing. It is a **narrowing** of §5's *"Shared singletons are exclusive …
Check the board's singleton column"* — the column is not enough if the name in it is not the
tool's.

**Status.** ⛔ **Always-ask — destination `rules/`.** Stays on this list until Ido rules.
