/**
 * The TypeScript half of `C20`'s shared fixture (`docs/PRODUCT_v0.3.md` §5.2).
 *
 * §5.2 accepted one honest cost when it moved derived state to the server: the arithmetic
 * now exists in **Kotlin and TypeScript**, a second implementation that can disagree. It
 * accepted it on one condition — that both are *"pinned by a shared `facts -> expected
 * numbers` fixture both test layers run"*. This file is one of those two readers. The other
 * is `app/src/test/java/com/idomarhaim/goalpilot/derived/DerivedStateFixtureTest.kt`.
 *
 * **They read the same file, not the same numbers.** `../shared-fixtures/derived-state.json`
 * is resolved by walking up from each module directory, so neither suite owns it and neither
 * can be edited into agreement with itself. Adding a case there turns both suites red until
 * both languages produce it.
 *
 * No emulator and no Firebase: `src/derived.ts` has no Firebase imports precisely so this
 * can run as plain `node --test` in a couple of hundred milliseconds. What the emulator
 * *does* have to prove — that the rules now deny the client these fields — is
 * `firestore-tests/rules.test.mjs`, a different question in a different layer.
 */
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

// The compiled output, not the source: `npm test` runs `tsc` first (package.json). Importing
// lib/ rather than transpiling here means these tests exercise the exact JavaScript that
// gets deployed, which is the only version that can actually disagree with Kotlin.
import {
  pointsFromFacts,
  levelForPoints,
  pointsForLevel,
  scoreFromReport,
  scoreFromProgress,
  scoringWindow,
  standingFromFact,
  linkedGoalId,
} from "../lib/derived.js";

/**
 * The fixture gives arrays; the projection takes maps keyed by task id, because that is how
 * it tells a re-ticked task from two different ones (#55). Converting here rather than in the
 * fixture keeps the fixture readable as *what the user did*.
 */
function factsOf(facts) {
  const out = {};
  for (const f of facts.completionFacts ?? []) out[f.taskId] = f;
  return out;
}

function legacyOf(facts) {
  const out = {};
  for (const t of facts.tasks ?? []) out[t.id] = t;
  return out;
}

function project(facts) {
  return pointsFromFacts(factsOf(facts), legacyOf(facts));
}

const here = dirname(fileURLToPath(import.meta.url));
const fixturePath = resolve(here, "..", "..", "shared-fixtures", "derived-state.json");
const fixture = JSON.parse(readFileSync(fixturePath, "utf8"));

test("the shared fixture is where both suites expect it", () => {
  // A fixture silently resolving to nothing is the one failure mode that would make every
  // case below pass vacuously — the suite would report green having asserted nothing.
  assert.ok(fixture.pointsCases.length > 0, `no points cases at ${fixturePath}`);
  assert.ok(fixture.scoreCases.length > 0, `no score cases at ${fixturePath}`);
});

for (const testCase of fixture.pointsCases) {
  test(`points: ${testCase.name}`, () => {
    const points = project(testCase.facts);
    assert.equal(points, testCase.expected.points, "points projected from the fact set");
    assert.equal(
      levelForPoints(points),
      testCase.expected.level,
      "level computed from those points — never stored, see §5.2",
    );
  });
}

for (const testCase of fixture.scoreCases) {
  test(`score: ${testCase.name}`, () => {
    assert.equal(scoreFromReport(testCase.facts.report), testCase.expected.score);
  });
}

// ── The window, pinned in both languages (added 2026-08-25) ──────────
//
// `scoringWindow` decides WHICH entries a challenge counts, and it exists twice -- here and
// in `Challenge.scoringWindowFor`. It was pinned by nothing until today, while being the
// likeliest of all the shared rules to drift: the two are written in different SHAPES, an
// if/else against a nested ternary, so an edit to one has no mechanical reason to reach the
// other. The retroactive case below is a bug that actually shipped.
for (const testCase of fixture.windowCases) {
  test(`window: ${testCase.name}`, () => {
    const w = scoringWindow(testCase.challenge, testCase.joinedAt);
    assert.equal(w.from, testCase.expected.from, "lower bound");
    assert.equal(w.until, testCase.expected.until, "upper bound");
  });
}

test("projecting twice writes the same number — idempotence, not carefulness", () => {
  // The property §5.2 chose this shape for, and the reason `FieldValue.increment` stays
  // rejected. Asserted as a property rather than as a fixture row because it is about
  // running the function twice, which a facts -> expected table cannot express.
  for (const testCase of fixture.pointsCases) {
    const once = project(testCase.facts);
    const twice = project(testCase.facts);
    assert.equal(once, twice, testCase.name);
  }
});

test("the level curve is 50*(n-1)*n — L1:0, L2:100, L3:300, L4:600", () => {
  // Mirrors `Leveling.pointsForLevel`. The fixture pins the boundaries that matter; this
  // pins the formula they come from, so a curve edited in one language is caught here even
  // before it moves a fixture case.
  assert.deepEqual(
    [1, 2, 3, 4, 5].map(pointsForLevel),
    [0, 100, 300, 600, 1000],
  );
  assert.equal(pointsForLevel(0), 0, "clamped at level 1, as Kotlin coerceAtLeast(1) does");
});

// ─────────────────────────────────────────────────────────────────────────────
// §6 (`C14` #23): a challenge scores from each participant's GOAL.
//
// These cases are NOT in `shared-fixtures/derived-state.json`, and that is deliberate.
// The fixture exists to pin two implementations of one rule together — §5.2's honest
// residual is that the points arithmetic lives in Kotlin AND TypeScript and the two can
// disagree. This arithmetic has **no Kotlin twin**: §6 makes the challenge score
// server-owned, the client only ever renders what the projection wrote, and inventing a
// Kotlin mirror to satisfy the fixture's shape would manufacture exactly the duplication
// the fixture exists to police. If a client ever computes a challenge score, these move
// into the fixture the same day.
// ─────────────────────────────────────────────────────────────────────────────

const OPEN = { from: 0, until: null };

test("a fact carries a goal link or a typed number, never both", () => {
  assert.equal(linkedGoalId({ goalId: "g1" }), "g1");
  assert.equal(linkedGoalId({ goalId: "  g1  " }), "g1", "trimmed");
  assert.equal(linkedGoalId({ value: 12 }), null);
  assert.equal(linkedGoalId({ goalId: "" }), null, "an empty id is not a link");
  assert.equal(linkedGoalId({ goalId: "   " }), null);
  assert.equal(linkedGoalId(null), null);
});

test("a DATED challenge scores its own window; only an open-ended one uses joinedAt", () => {
  // ⚠️ THIS TEST ASSERTED THE OPPOSITE UNTIL 2026-08-25, and the line that changed is the
  // third one. It read `{ startAt: 100 }, 500 -> from: 500` -- the later of the two -- on
  // the reasoning that "joining an UPCOMING challenge credits it with what you did
  // beforehand". That reasoning was sound and the rule it produced was still wrong, because
  // it also made a RETROACTIVE challenge score zero for everybody: a race for last week,
  // accepted today, got a lower bound of today, which is past its own endAt.
  //
  // Ido asked for exactly that race on 2026-08-25, so the rule now splits on whether the
  // owner set a start date at all -- see `scoringWindow`'s KDoc for why that is the thing
  // §6's `joinedAt` was really doing. Kept as a rewritten assertion rather than deleted:
  // the case it used to protect is the FIRST line below, and it still passes.
  assert.deepEqual(scoringWindow({ startAt: 0, endAt: 0 }, 500), { from: 500, until: null });
  assert.deepEqual(scoringWindow({ startAt: 900, endAt: 0 }, 500), { from: 900, until: null });
  // The changed one: a start date wins even when the participant joined AFTER it.
  assert.deepEqual(scoringWindow({ startAt: 100, endAt: 0 }, 500), { from: 100, until: null });
  assert.deepEqual(scoringWindow({ startAt: 0, endAt: 2000 }, 500), { from: 500, until: 2000 });
  // And the case that was unreachable before: joined long after the race finished.
  assert.deepEqual(scoringWindow({ startAt: 100, endAt: 200 }, 9999), { from: 100, until: 200 });
});

test("a missing challenge or joinedAt yields a window, never NaN", () => {
  // `Math.max(NaN, x)` is NaN, and a NaN bound includes NOTHING -- which would read as
  // "this challenge scores zero" rather than as a failure. The guard is the difference
  // between a wrong number and no number.
  assert.deepEqual(scoringWindow(null, null), { from: 0, until: null });
  assert.deepEqual(scoringWindow(undefined, undefined), { from: 0, until: null });
  assert.deepEqual(scoringWindow({}, 500), { from: 500, until: null });
});

test("the score is the movement inside the window, not the goal's whole history", () => {
  const entries = [
    { value: 3000, createdAt: 100 }, // before joining
    { value: 4000, createdAt: 500 }, // the instant of joining -- inclusive
    { value: 1200, createdAt: 900 },
    { value: 999, createdAt: 2000 }, // the end bound -- exclusive
  ];
  assert.equal(scoreFromProgress(entries, { from: 500, until: 2000 }), 5200);
  // Joining with a year-old goal imports no history: that is the whole reason §6 sums
  // timestamped entries instead of storing a delta from a baseline.
  assert.equal(scoreFromProgress(entries, { from: 2001, until: null }), 0);
  assert.equal(scoreFromProgress(entries, OPEN), 9199);
});

test("an entry with no usable timestamp or value contributes nothing", () => {
  const entries = [
    { value: 10, createdAt: 100 },
    { value: 10 }, // no stamp -- cannot be placed in the window
    { createdAt: 100 }, // no value
    { value: "not a number", createdAt: 100 },
    null,
  ];
  assert.equal(scoreFromProgress(entries, OPEN), 10);
});

test("projecting a challenge score twice writes the same number", () => {
  // Structural idempotence, the property §5.2 rejected `FieldValue.increment` for. The
  // whole entry set is the input and no prior score is, so a redelivered event, a retry
  // and a manual re-run all agree.
  const entries = [
    { value: 5, createdAt: 10 },
    { value: 7, createdAt: 20 },
  ];
  assert.equal(scoreFromProgress(entries, OPEN), scoreFromProgress(entries, OPEN));
});

test("a downward goal floors at zero rather than sorting below somebody idle", () => {
  // A weight-loss goal logs downwards, so negative movement is real. §6 does not
  // adjudicate direction, and a negative score would put a participant BELOW somebody
  // who has done nothing while the standings offer no way to say why.
  assert.equal(scoreFromProgress([{ value: -4, createdAt: 10 }], OPEN), 0);
});

test("a linked fact projects a derived standing with no reported-at at all", () => {
  const standing = standingFromFact(
    { goalId: "g1" },
    [{ value: 8200, createdAt: 600 }],
    { from: 500, until: null },
    1_756_000_000_000,
  );
  assert.deepEqual(standing, { score: 8200, scoreSource: "DERIVED", reportedAt: 0 });
});

test("a typed fact projects a reported standing that says when", () => {
  const standing = standingFromFact({ value: 8200 }, [], OPEN, 1_756_000_000_000);
  assert.deepEqual(standing, {
    score: 8200,
    scoreSource: "REPORTED",
    reportedAt: 1_756_000_000_000,
  });
});

test("a reported standing with no stamp says nothing rather than inventing a date", () => {
  // A fact written before `reportedAt` existed. The badge drops the date rather than
  // borrowing the as-of stamp beside it, which would be a claim nobody made.
  assert.equal(standingFromFact({ value: 5 }, [], OPEN, undefined).reportedAt, 0);
  assert.equal(standingFromFact({ value: 5 }, [], OPEN, 0).reportedAt, 0);
});

test("a deleted fact writes NOTHING, and does not zero a standing others are reading", () => {
  // `null` is a real answer, not an error path -- undoing a report or leaving must not
  // silently blank a row on everybody else's screen. The last number stands until the
  // participant row itself is deleted.
  assert.equal(standingFromFact(null, [], OPEN, 0), null);
  assert.equal(standingFromFact(undefined, [], OPEN, 0), null);
  assert.equal(standingFromFact({}, [], OPEN, 0), null, "a fact with neither shape");
});

test("a link to a goal with no progress yet is a real zero, not a write-nothing", () => {
  // The one case where the two answers genuinely differ: linking is an act, so the row
  // should say DERIVED and 0 -- "this is scoring itself and has not moved" -- rather than
  // leaving whatever number was typed before the link standing there labelled REPORTED.
  assert.deepEqual(standingFromFact({ goalId: "g1" }, [], OPEN, 0), {
    score: 0,
    scoreSource: "DERIVED",
    reportedAt: 0,
  });
});
