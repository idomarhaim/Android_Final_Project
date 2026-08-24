/**
 * `plan`'s validator — `docs/PRODUCT_v0.3.md` §3.3 B, §3.7 (`C8` #24).
 *
 * §3.4 makes the Cloud Function the **single** site of this validation, so this suite is the
 * only place these rules are checked at all — there is no Kotlin twin to cross-check against,
 * which is exactly why they live here once.
 *
 * No emulator and no Firebase: `src/plan.ts` imports nothing, so this runs as plain
 * `node --test` in milliseconds, like `classify.test.mjs` and `measure.test.mjs`.
 *
 * ## The two properties the suite is organised around
 *
 * 1. **A container is stripped, never dropped.** §3.3 B's cross-field rule is a validation
 *    failure *of two fields*, not of the item — so the tests below spoil a milestone by pricing
 *    it and assert the milestone **survives** with the price gone. A validator that dropped it
 *    would pass a naive "invalid input is rejected" test and be wrong.
 * 2. **The model authors no id and no date.** Both are absences, and an absence is the one
 *    thing a schema test can miss by simply not asking. So there are explicit tests that an
 *    echoed `id` never reaches the output and that `dayOffset` is the only temporal field.
 */
import test from "node:test";
import assert from "node:assert/strict";

// The compiled output, not the source: `npm test` runs `tsc` first, so this exercises the
// exact JavaScript that gets deployed.
import {
  validatePlan,
  asTimeOfDay,
  LABELS,
  DIFFICULTIES,
  MIN_MINUTES,
  MAX_MINUTES,
  MAX_DAY_OFFSET,
  MAX_TITLE,
  MAX_ITEMS,
} from "../lib/plan.js";

/** A leaf with every field valid, so a test can spoil exactly one of them. */
const WORK = {
  title: "Run 5 km at an easy pace",
  label: "WORK_YOU_DO",
  difficulty: "ROUTINE",
  estimatedMinutes: 40,
  dayOffset: 2,
  timeOfDay: "07:30",
};

/** A container with every field valid. */
const STATE = {
  title: "Comfortable running 10 km",
  label: "STATE_YOU_REACH",
  dayOffset: 45,
};

const planOf = (...items) => validatePlan({ items });
const only = (item) => planOf(item).items[0];

// ---------------------------------------------------------------------------
// The happy path, and the shape of what comes back
// ---------------------------------------------------------------------------

test("a valid leaf survives with every field", () => {
  assert.deepEqual(only(WORK), WORK);
});

test("a valid container survives with no pricing fields", () => {
  assert.deepEqual(only(STATE), STATE);
});

test("order is preserved, because position is the step's identity (§3.3 B)", () => {
  const plan = planOf(
    { ...WORK, title: "first" },
    { ...STATE, title: "second" },
    { ...WORK, title: "third" },
  );
  assert.deepEqual(plan.items.map((i) => i.title), ["first", "second", "third"]);
});

test("an empty response is an empty plan, not a crash", () => {
  assert.deepEqual(validatePlan({}), { items: [], changeNotes: [] });
  assert.deepEqual(validatePlan(null), { items: [], changeNotes: [] });
  assert.deepEqual(validatePlan("nope"), { items: [], changeNotes: [] });
  assert.deepEqual(validatePlan({ items: "nope" }), { items: [], changeNotes: [] });
});

// ---------------------------------------------------------------------------
// What makes a step a step: title and label. Everything else stands or falls alone.
// ---------------------------------------------------------------------------

for (const bad of [undefined, null, "", "   ", 42, {}, "x".repeat(MAX_TITLE + 1)]) {
  test(`no title (${JSON.stringify(bad)}) drops the item whole`, () => {
    assert.deepEqual(planOf({ ...WORK, title: bad }).items, []);
  });
}

test("a title exactly at the limit survives -- the bound is inclusive", () => {
  const title = "x".repeat(MAX_TITLE);
  assert.equal(only({ ...WORK, title }).title, title);
});

test("a title is trimmed but never truncated", () => {
  assert.equal(only({ ...WORK, title: "  Buy shoes  " }).title, "Buy shoes");
});

for (const bad of [undefined, null, "", "MILESTONE", "state", 1, {}]) {
  test(`an unusable label (${JSON.stringify(bad)}) drops the item whole`, () => {
    assert.deepEqual(planOf({ ...WORK, label: bad }).items, []);
  });
}

test("a label is matched case-insensitively and stored upper-case", () => {
  assert.equal(only({ ...WORK, label: "work_you_do" }).label, "WORK_YOU_DO");
});

test("both labels in LABELS are accepted", () => {
  for (const label of LABELS) {
    assert.equal(only({ ...WORK, label }).label, label);
  }
});

// ---------------------------------------------------------------------------
// §3.3 B's cross-field rule: a container is STRIPPED, not dropped
// ---------------------------------------------------------------------------

test("a priced container keeps the item and loses the price", () => {
  const item = only({
    ...STATE,
    difficulty: "DEMANDING",
    estimatedMinutes: 120,
    timeOfDay: "09:00",
  });
  assert.equal(item.title, STATE.title);
  assert.equal(item.label, "STATE_YOU_REACH");
  assert.equal("difficulty" in item, false);
  assert.equal("estimatedMinutes" in item, false);
  assert.equal("timeOfDay" in item, false);
});

test("a container keeps its dayOffset -- a milestone still has a date", () => {
  assert.equal(only({ ...STATE, dayOffset: 90 }).dayOffset, 90);
});

// ---------------------------------------------------------------------------
// The estimate group, on leaves. Each field stands or falls alone.
// ---------------------------------------------------------------------------

for (const bad of [undefined, null, "hard", "ROUTINEISH", 3]) {
  test(`an unusable difficulty (${JSON.stringify(bad)}) is absent, and costs nothing else`, () => {
    const item = only({ ...WORK, difficulty: bad });
    assert.equal("difficulty" in item, false);
    assert.equal(item.estimatedMinutes, WORK.estimatedMinutes);
    assert.equal(item.title, WORK.title);
  });
}

test("every difficulty in DIFFICULTIES is accepted", () => {
  for (const difficulty of DIFFICULTIES) {
    assert.equal(only({ ...WORK, difficulty }).difficulty, difficulty);
  }
});

for (const bad of [MIN_MINUTES - 1, MAX_MINUTES + 1, 0, -30, NaN, Infinity, "40", null]) {
  test(`minutes out of range (${JSON.stringify(bad)}) is absent, never clamped`, () => {
    const item = only({ ...WORK, estimatedMinutes: bad });
    assert.equal("estimatedMinutes" in item, false);
    assert.equal(item.difficulty, WORK.difficulty);
  });
}

test("minutes at either bound survive -- both are inclusive", () => {
  assert.equal(only({ ...WORK, estimatedMinutes: MIN_MINUTES }).estimatedMinutes, MIN_MINUTES);
  assert.equal(only({ ...WORK, estimatedMinutes: MAX_MINUTES }).estimatedMinutes, MAX_MINUTES);
});

test("a fractional minute count is rounded, not rejected", () => {
  assert.equal(only({ ...WORK, estimatedMinutes: 40.4 }).estimatedMinutes, 40);
});

// ---------------------------------------------------------------------------
// The model authors no date. `dayOffset` is the only temporal field there is.
// ---------------------------------------------------------------------------

test("dayOffset 0 is today and is a legal answer, distinct from saying nothing", () => {
  assert.equal(only({ ...WORK, dayOffset: 0 }).dayOffset, 0);
});

test("dayOffset at the ceiling survives", () => {
  assert.equal(only({ ...WORK, dayOffset: MAX_DAY_OFFSET }).dayOffset, MAX_DAY_OFFSET);
});

for (const bad of [-1, MAX_DAY_OFFSET + 1, 20260901, NaN, "2", null, undefined]) {
  test(`an unusable dayOffset (${JSON.stringify(bad)}) leaves the step undated, not dropped`, () => {
    const item = only({ ...WORK, dayOffset: bad });
    assert.equal("dayOffset" in item, false);
    assert.equal(item.title, WORK.title);
  });
}

test("a calendar date in dayOffset is rejected -- that is what the ceiling is for", () => {
  assert.equal("dayOffset" in only({ ...WORK, dayOffset: 20260901 }), false);
});

test("no date field other than dayOffset ever reaches the output", () => {
  const item = only({ ...WORK, date: "2026-09-01", dueDate: "2026-09-01", when: "next week" });
  assert.deepEqual(Object.keys(item).sort(), Object.keys(WORK).sort());
});

// ---------------------------------------------------------------------------
// The model may not mint an id (§3.3 B) -- the truncation failure is unrepresentable
// ---------------------------------------------------------------------------

test("an echoed id is not carried through, whatever it says", () => {
  for (const id of ["step-1", "", null, 7]) {
    const item = only({ ...WORK, id });
    assert.equal("id" in item, false);
  }
});

// ---------------------------------------------------------------------------
// timeOfDay -- shape only, no taste
// ---------------------------------------------------------------------------

test("asTimeOfDay accepts a 24-hour HH:MM and normalises whitespace", () => {
  assert.equal(asTimeOfDay("07:30"), "07:30");
  assert.equal(asTimeOfDay("  23:59 "), "23:59");
  assert.equal(asTimeOfDay("00:00"), "00:00");
});

test("asTimeOfDay does not judge the hour -- 03:00 is a legal answer", () => {
  assert.equal(asTimeOfDay("03:00"), "03:00");
});

for (const bad of ["7:30", "24:00", "23:60", "07-30", "half seven", "07:30:00", 730, null]) {
  test(`asTimeOfDay rejects ${JSON.stringify(bad)}`, () => {
    assert.equal(asTimeOfDay(bad), undefined);
  });
}

test("an unusable timeOfDay costs the slot and never the step", () => {
  const item = only({ ...WORK, timeOfDay: "half seven" });
  assert.equal("timeOfDay" in item, false);
  assert.equal(item.dayOffset, WORK.dayOffset);
});

// ---------------------------------------------------------------------------
// Bounds on the plan itself
// ---------------------------------------------------------------------------

test(`at most ${MAX_ITEMS} items survive, and they are the first ones`, () => {
  const many = Array.from({ length: MAX_ITEMS + 5 }, (_, i) => ({ ...WORK, title: `step ${i}` }));
  const items = validatePlan({ items: many }).items;
  assert.equal(items.length, MAX_ITEMS);
  assert.equal(items[0].title, "step 0");
  assert.equal(items[MAX_ITEMS - 1].title, `step ${MAX_ITEMS - 1}`);
});

test("a spoiled step costs that step and nothing else", () => {
  const plan = planOf(WORK, { ...WORK, label: "NONSENSE" }, STATE);
  assert.deepEqual(plan.items.map((i) => i.title), [WORK.title, STATE.title]);
});

test("a plan whose every step failed is an empty plan -- the client reports it", () => {
  assert.deepEqual(planOf({ label: "WORK_YOU_DO" }, { title: "no label" }).items, []);
});

// ---------------------------------------------------------------------------
// changeNotes -- speech, and bounded
// ---------------------------------------------------------------------------

test("changeNotes survive as trimmed strings", () => {
  const plan = validatePlan({ items: [], changeNotes: ["  moved week 2 later ", "dropped a run"] });
  assert.deepEqual(plan.changeNotes, ["moved week 2 later", "dropped a run"]);
});

test("unusable changeNotes are dropped individually", () => {
  const plan = validatePlan({ items: [], changeNotes: ["kept", "", null, 7, "x".repeat(500)] });
  assert.deepEqual(plan.changeNotes, ["kept"]);
});

test("changeNotes that is not an array is an empty list", () => {
  assert.deepEqual(validatePlan({ items: [], changeNotes: "moved things" }).changeNotes, []);
});
