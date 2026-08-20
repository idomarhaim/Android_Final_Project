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
  pointsFromTasks,
  levelForPoints,
  pointsForLevel,
  scoreFromReport,
} from "../lib/derived.js";

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
    const points = pointsFromTasks(testCase.facts.tasks);
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

test("projecting twice writes the same number — idempotence, not carefulness", () => {
  // The property §5.2 chose this shape for, and the reason `FieldValue.increment` stays
  // rejected. Asserted as a property rather than as a fixture row because it is about
  // running the function twice, which a facts -> expected table cannot express.
  for (const testCase of fixture.pointsCases) {
    const once = pointsFromTasks(testCase.facts.tasks);
    const twice = pointsFromTasks(testCase.facts.tasks);
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
