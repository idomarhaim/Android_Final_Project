/**
 * `measure`'s validator — `docs/PRODUCT_v0.3.md` §3.3 E, §3.4 (`C7` #14, `C22` #44, #65).
 *
 * §3.4 makes the Cloud Function the **single** site of this validation, so this suite is the
 * only place these rules are checked at all — there is no Kotlin twin to cross-check against,
 * which is the point of putting them here once.
 *
 * No emulator and no Firebase: `src/measure.ts` imports nothing, so this runs as plain
 * `node --test` in milliseconds, exactly like `classify.test.mjs`.
 *
 * ## The property the whole suite is organised around
 *
 * **A proposal is whole or it does not exist.** `classify`'s fields each stand or fall alone;
 * `measure` drops an element entire, because a kind with no word is not a degraded measure —
 * it is not a measure. Every test below spoils exactly one field of a known-good element and
 * asserts the element is gone, which is the shape that would catch a validator that
 * "helpfully" substituted instead.
 */
import test from "node:test";
import assert from "node:assert/strict";

// The compiled output, not the source: `npm test` runs `tsc` first, so this exercises the
// exact JavaScript that gets deployed.
import {
  validateProposals,
  listsFromMeasureRequest,
  asBasis,
  MEASURE_KINDS,
  BASES,
  TARGET_SOURCES,
  MAX_WORD,
  MAX_PROPOSALS,
} from "../lib/measure.js";

const LISTS = { goalIds: ["g1", "g2"] };

/** A proposal with every field valid, so a test can spoil exactly one of them. */
const GOOD = {
  goalId: "g1",
  measureKind: "COUNT",
  word: "runs a week",
  basis: "LEADING",
  targetSource: "SCHEDULE",
};

const wrap = (...proposals) => ({ proposals });

test("a whole, valid proposal survives with every field intact", () => {
  assert.deepEqual(validateProposals(wrap(GOOD), LISTS), [GOOD]);
});

// ── The one measured failure class ──────────────────────────────────────────────
// `C11a`: across 248 live calls the ONLY two failures were semantic — a plausible id that
// was not real. This is the check that exists because of a measurement, not a worry.

test("a goalId that was not in the request is dropped whole", () => {
  const impostor = { ...GOOD, goalId: "g3" };
  assert.deepEqual(validateProposals(wrap(impostor), LISTS), []);
});

test("a non-member id does not cost the valid proposals beside it", () => {
  const out = validateProposals(wrap({ ...GOOD, goalId: "g3" }, { ...GOOD, goalId: "g2" }), LISTS);
  assert.deepEqual(out.map((p) => p.goalId), ["g2"]);
});

test("an empty membership list drops everything, which is the safe direction", () => {
  assert.deepEqual(validateProposals(wrap(GOOD), { goalIds: [] }), []);
});

// ── §3.3 E: missing kind, word or targetSource is NOT a proposal ────────────────

for (const field of ["measureKind", "word", "targetSource"]) {
  test(`a missing ${field} drops the element whole`, () => {
    const spoiled = { ...GOOD };
    delete spoiled[field];
    assert.deepEqual(validateProposals(wrap(spoiled), LISTS), []);
  });

  test(`an out-of-range ${field} drops the element whole`, () => {
    const spoiled = { ...GOOD, [field]: field === "word" ? "" : "NOT_A_MEMBER" };
    assert.deepEqual(validateProposals(wrap(spoiled), LISTS), []);
  });
}

test("every one of §1.3's seven kinds is accepted", () => {
  for (const kind of MEASURE_KINDS) {
    const out = validateProposals(wrap({ ...GOOD, measureKind: kind }), LISTS);
    assert.equal(out.length, 1, `${kind} should survive`);
    assert.equal(out[0].measureKind, kind);
  }
});

test("all three targetSources are accepted, USER included", () => {
  for (const source of TARGET_SOURCES) {
    const out = validateProposals(wrap({ ...GOOD, targetSource: source }), LISTS);
    assert.equal(out.length, 1, `${source} should survive`);
    assert.equal(out[0].targetSource, source);
  }
  // USER is a real answer meaning *ask him*, not a failure — the point of asserting it
  // explicitly is that a validator treating it as "no answer" would still pass the loop
  // above if it silently dropped only that one.
  assert.ok(TARGET_SOURCES.includes("USER"));
});

test("enums are accepted case-insensitively and normalised upward", () => {
  const out = validateProposals(wrap({ ...GOOD, measureKind: "count", targetSource: "steps" }), LISTS);
  assert.equal(out[0].measureKind, "COUNT");
  assert.equal(out[0].targetSource, "STEPS");
});

// ── `word` is CONTENT, so it is never truncated ─────────────────────────────────

test("a word at the cap survives", () => {
  const word = "x".repeat(MAX_WORD);
  assert.equal(validateProposals(wrap({ ...GOOD, word }), LISTS)[0].word, word);
});

test("a word over the cap drops the element rather than truncating it", () => {
  const out = validateProposals(wrap({ ...GOOD, word: "x".repeat(MAX_WORD + 1) }), LISTS);
  // The assertion that matters is NOT `length === 0` on its own — it is that no proposal
  // came back carrying a shortened word, because §3.5 makes this field his the moment he
  // accepts it and a cut-off unit is something he never chose.
  assert.deepEqual(out, []);
});

test("a whitespace-only word is blank and drops the element", () => {
  assert.deepEqual(validateProposals(wrap({ ...GOOD, word: "   " }), LISTS), []);
});

test("a word is trimmed but otherwise passed through verbatim, Hebrew included", () => {
  const out = validateProposals(wrap({ ...GOOD, word: "  ריצות בשבוע  " }), LISTS);
  assert.equal(out[0].word, "ריצות בשבוע");
});

// ── `basis` is the one field that substitutes ───────────────────────────────────

test("an absent or unusable basis becomes OUTCOME rather than dropping the proposal", () => {
  const missing = { ...GOOD };
  delete missing.basis;
  assert.equal(validateProposals(wrap(missing), LISTS)[0].basis, "OUTCOME");
  assert.equal(validateProposals(wrap({ ...GOOD, basis: "SIDEWAYS" }), LISTS)[0].basis, "OUTCOME");
});

test("asBasis accepts both of §3.3 E's values and nothing else", () => {
  for (const b of BASES) assert.equal(asBasis(b), b);
  assert.equal(asBasis(null), "OUTCOME");
  assert.equal(asBasis(7), "OUTCOME");
});

// ── One goal, one offer ─────────────────────────────────────────────────────────

test("a duplicate goalId is dropped, keeping the first", () => {
  const first = { ...GOOD, word: "runs a week" };
  const second = { ...GOOD, word: "kilometres" };
  const out = validateProposals(wrap(first, second), LISTS);
  assert.equal(out.length, 1);
  assert.equal(out[0].word, "runs a week");
});

test("a flood of valid repeats cannot exceed the cap", () => {
  const many = Array.from({ length: MAX_PROPOSALS + 20 }, () => ({ ...GOOD }));
  assert.ok(validateProposals(wrap(...many), LISTS).length <= MAX_PROPOSALS);
});

// ── Nothing usable in, nothing out ──────────────────────────────────────────────

test("a malformed response yields an empty list rather than throwing", () => {
  for (const raw of [null, undefined, 7, "proposals", {}, { proposals: null }, { proposals: {} }]) {
    assert.deepEqual(validateProposals(raw, LISTS), [], `${JSON.stringify(raw)}`);
  }
});

test("non-object elements inside the array are skipped, not fatal", () => {
  const out = validateProposals({ proposals: [null, 3, "x", GOOD] }, LISTS);
  assert.deepEqual(out, [GOOD]);
});

// ── The membership list comes off the request ───────────────────────────────────

test("listsFromMeasureRequest reads goals[].id and tolerates rubbish", () => {
  assert.deepEqual(
    listsFromMeasureRequest({ goals: [{ id: "g1" }, { id: "" }, null, 5, { noId: 1 }, { id: "g2" }] }),
    { goalIds: ["g1", "g2"] },
  );
});

test("a malformed goals array yields an EMPTY list, so every proposal fails membership", () => {
  // The safe direction, and worth its own test because the tolerant direction is the
  // tempting one: an app that offered a measure for a goal that does not exist is worse
  // than an app that offered nothing and fell back to arithmetic.
  for (const data of [undefined, {}, { goals: "g1" }, { goals: null }]) {
    assert.deepEqual(listsFromMeasureRequest(data), { goalIds: [] });
  }
});

// ── The schema itself ───────────────────────────────────────────────────────────

test("there is no numeric field anywhere in a validated proposal (§3.3 E)", () => {
  // The whole design in one assertion. `C11a` measured free numbers swinging 2x
  // run-to-run, so the target is computed by the client from structure it already holds
  // and NOTHING the model says can carry a number. A future field that broke this would
  // pass every other test in this file.
  const out = validateProposals(wrap({ ...GOOD, target: 3, targetValue: 3 }), LISTS)[0];
  for (const [key, value] of Object.entries(out)) {
    assert.equal(typeof value, "string", `${key} should be a string, got ${typeof value}`);
  }
  assert.deepEqual(Object.keys(out).sort(), ["basis", "goalId", "measureKind", "targetSource", "word"]);
});
