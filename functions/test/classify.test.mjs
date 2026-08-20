/**
 * `classify`'s validator — the TypeScript half of `#6` (`docs/PRODUCT_v0.3.md` §3.3 D, §3.4).
 *
 * §3.4 makes the Cloud Function the **single** site of this validation, so this suite is the
 * only place the rules are checked at all: there is no Kotlin twin to cross-check against,
 * which is the whole point of putting them here once.
 *
 * No emulator and no Firebase, deliberately — `src/classify.ts` imports nothing, so this runs
 * as plain `node --test` in milliseconds. The emulator has a different question to answer
 * (*does a transport failure reach the client as a failure?*), and it is asked in
 * `triggers.emulator.mjs`.
 *
 * The suite is organised around the one property the whole ticket rests on: **presence is the
 * signal.** The client picks its branch by asking whether `suggestedGoalId` is there, so a
 * test that only checked values would pass against a validator that substituted a plausible
 * one — which is exactly the bug §3.4 is written against.
 */
import test from "node:test";
import assert from "node:assert/strict";

// The compiled output, not the source: `npm test` runs `tsc` first (package.json), so these
// tests exercise the exact JavaScript that gets deployed.
import { validateClassification, listsFromRequest, CATEGORIES } from "../lib/classify.js";

const LISTS = { goalIds: ["g1", "g2"], lifeAreaIds: ["a1"] };

/** A response with every field valid, so a test can spoil exactly one of them. */
const GOOD = {
  suggestedGoalId: "g1",
  suggestedNewGoalTitle: "Run a half marathon",
  suggestedLifeAreaId: "a1",
  suggestedCategory: "FITNESS",
  confidence: 0.82,
  rationale: "Matches your running goal.",
  estimatedPoints: 20,
  estimatedMinutes: 45,
};

// ── The branch the ticket turns on ────────────────────────────────────────────

test("a member goal id survives — this is the silent branch", () => {
  const out = validateClassification(GOOD, LISTS);
  assert.equal(out.suggestedGoalId, "g1");
});

test("a non-member goal id is ABSENT, not null — this is the branch that speaks", () => {
  const out = validateClassification({ ...GOOD, suggestedGoalId: "g-does-not-exist" }, LISTS);
  assert.equal("suggestedGoalId" in out, false);
  // The distinction the client branches on. `null` would read as an answer.
  assert.equal(out.suggestedGoalId, undefined);
});

test("an id the model adapted rather than echoed is still a non-member", () => {
  // C11a's observed failure mode was *silent id corruption*, not invention from nothing:
  // a near-miss is the realistic shape, and a substring/prefix check would let it through.
  const out = validateClassification({ ...GOOD, suggestedGoalId: "g1 " }, LISTS);
  assert.equal("suggestedGoalId" in out, false);
});

test("an explicit null goal id is absent, and takes the new-goal branch", () => {
  const out = validateClassification({ ...GOOD, suggestedGoalId: null }, LISTS);
  assert.equal("suggestedGoalId" in out, false);
});

test("no goals were sent at all — every id fails, so every task takes the new-goal branch", () => {
  const out = validateClassification(GOOD, { goalIds: [], lifeAreaIds: [] });
  assert.equal("suggestedGoalId" in out, false);
  assert.equal("suggestedLifeAreaId" in out, false);
});

// ── Omit, never substitute (§3.3) ─────────────────────────────────────────────

test("no field is ever emitted as null", () => {
  const junk = {
    suggestedGoalId: 7,
    suggestedNewGoalTitle: "   ",
    suggestedLifeAreaId: {},
    suggestedCategory: "SPORTS",
    confidence: "high",
    rationale: 12,
    estimatedPoints: "20",
    estimatedMinutes: NaN,
  };
  const out = validateClassification(junk, LISTS);
  assert.deepEqual(out, {}, "every field failed, so the object is empty");
  for (const v of Object.values(out)) assert.notEqual(v, null);
});

test("a response that is not an object validates to nothing rather than throwing", () => {
  for (const raw of [null, undefined, "{}", 42, []]) {
    assert.deepEqual(validateClassification(raw, LISTS), {});
  }
});

test("the returned object is new — `raw` is never mutated", () => {
  const raw = { ...GOOD, suggestedGoalId: "nope" };
  validateClassification(raw, LISTS);
  assert.equal(raw.suggestedGoalId, "nope");
});

// ── The estimate group is validated INDEPENDENTLY (§3.3 D) ────────────────────

test("a nonsense duration does not cost you the goal id", () => {
  const out = validateClassification({ ...GOOD, estimatedMinutes: 9000 }, LISTS);
  assert.equal(out.suggestedGoalId, "g1");
  assert.equal("estimatedMinutes" in out, false);
});

test("a non-member goal id does not cost you the duration", () => {
  const out = validateClassification({ ...GOOD, suggestedGoalId: "nope" }, LISTS);
  assert.equal("suggestedGoalId" in out, false);
  assert.equal(out.estimatedMinutes, 45);
});

test("minutes are kept at the boundaries and dropped outside them — never clamped", () => {
  assert.equal(validateClassification({ estimatedMinutes: 5 }, LISTS).estimatedMinutes, 5);
  assert.equal(validateClassification({ estimatedMinutes: 480 }, LISTS).estimatedMinutes, 480);
  assert.equal("estimatedMinutes" in validateClassification({ estimatedMinutes: 4 }, LISTS), false);
  assert.equal("estimatedMinutes" in validateClassification({ estimatedMinutes: 481 }, LISTS), false);
  // Clamping would report 480 minutes nobody said. The failure is the answer's, not ours.
  assert.equal(validateClassification({ estimatedMinutes: 481 }, LISTS).estimatedMinutes, undefined);
});

test("points are kept at the boundaries and dropped outside them", () => {
  assert.equal(validateClassification({ estimatedPoints: 5 }, LISTS).estimatedPoints, 5);
  assert.equal(validateClassification({ estimatedPoints: 50 }, LISTS).estimatedPoints, 50);
  assert.equal("estimatedPoints" in validateClassification({ estimatedPoints: 51 }, LISTS), false);
  assert.equal("estimatedPoints" in validateClassification({ estimatedPoints: 0 }, LISTS), false);
});

test("a fractional number is rounded, not rejected — the range is the contract, not the type", () => {
  assert.equal(validateClassification({ estimatedMinutes: 44.6 }, LISTS).estimatedMinutes, 45);
});

// ── The remaining fields ──────────────────────────────────────────────────────

test("confidence keeps both boundaries, and 0 is an answer rather than a silence", () => {
  assert.equal(validateClassification({ confidence: 0 }, LISTS).confidence, 0);
  assert.equal(validateClassification({ confidence: 1 }, LISTS).confidence, 1);
  assert.equal("confidence" in validateClassification({ confidence: 1.01 }, LISTS), false);
  assert.equal("confidence" in validateClassification({ confidence: -0.1 }, LISTS), false);
});

test("every declared category survives, and nothing else does", () => {
  for (const c of CATEGORIES) {
    assert.equal(validateClassification({ suggestedCategory: c }, LISTS).suggestedCategory, c);
  }
  assert.equal("suggestedCategory" in validateClassification({ suggestedCategory: "fitness" }, LISTS), false);
});

test("a new goal title is trimmed, and a blank or runaway one is absent", () => {
  assert.equal(
    validateClassification({ suggestedNewGoalTitle: "  Learn Spanish  " }, LISTS).suggestedNewGoalTitle,
    "Learn Spanish",
  );
  assert.equal("suggestedNewGoalTitle" in validateClassification({ suggestedNewGoalTitle: "   " }, LISTS), false);
  // Content is never silently truncated (§3.3 D): a paragraph is not a title, and the
  // client falls back to the words the user actually typed.
  const paragraph = "x".repeat(121);
  assert.equal("suggestedNewGoalTitle" in validateClassification({ suggestedNewGoalTitle: paragraph }, LISTS), false);
  assert.equal(validateClassification({ suggestedNewGoalTitle: "x".repeat(120) }, LISTS).suggestedNewGoalTitle.length, 120);
});

test("a Hebrew title survives untouched — it is content, authored once", () => {
  const title = "לרוץ חצי מרתון";
  assert.equal(validateClassification({ suggestedNewGoalTitle: title }, LISTS).suggestedNewGoalTitle, title);
});

// ── The membership lists come from the request ────────────────────────────────

test("listsFromRequest reads the ids the client actually sent", () => {
  const lists = listsFromRequest({
    goals: [{ id: "g1", title: "Run" }, { id: "g2", title: "Read" }],
    lifeAreas: [{ id: "a1", name: "Health" }],
  });
  assert.deepEqual(lists, { goalIds: ["g1", "g2"], lifeAreaIds: ["a1"] });
});

test("a malformed payload yields empty lists, which fails safe toward the new-goal branch", () => {
  for (const data of [undefined, null, {}, { goals: "g1" }, { goals: [null, 3, {}, { id: "" }] }]) {
    assert.deepEqual(listsFromRequest(data), { goalIds: [], lifeAreaIds: [] });
  }
});

test("the lists ignore everything but the ids", () => {
  const lists = listsFromRequest({ goals: [{ id: "g1", title: "Run", category: "FITNESS" }] });
  assert.deepEqual(lists.goalIds, ["g1"]);
});
