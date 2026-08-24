/**
 * §6's measure-change approval quorum — the arithmetic, with no emulator.
 *
 * `src/measureChange.ts` has no Firebase imports precisely so this can run as plain
 * `node --test` in a few milliseconds, the same arrangement `derived.ts` /
 * `projection.test.mjs` uses. What needs a real Firestore — that the rules let a
 * participant write `approvedChangeId` and refuse the owner the live measure — is
 * `../firestore-tests/rules.test.mjs`, a different question in a different layer.
 *
 * The cases below are the three things §6 left for this session to settle, plus the two
 * replay defences that make the quorum mean anything.
 */
import test from "node:test";
import assert from "node:assert/strict";
import {
  challengeUpdate,
  consequenceOf,
  participantUpdate,
  pendingFrom,
  quorum,
} from "../lib/measureChange.js";

const change = { changeId: "chg-1", kind: "DISTANCE", word: "km" };
const row = (uid, approvedChangeId) => ({ uid, approvedChangeId });

// ── Reading a proposal off the challenge document ────────────────────

test("no pending fields is no proposal", () => {
  assert.equal(pendingFrom({ measureKind: "COUNT", measureWord: "steps" }), null);
});

test("a HALF-written proposal is not a proposal", () => {
  // Applying one would write a measure with a missing half onto a live challenge, and
  // every reader of that challenge would then see a unit with no word or a word with no
  // kind -- neither of which any client can render honestly.
  assert.equal(
    pendingFrom({ pendingChangeId: "chg-1", pendingMeasureKind: "DISTANCE" }),
    null,
  );
  assert.equal(
    pendingFrom({ pendingChangeId: "chg-1", pendingMeasureWord: "km" }),
    null,
  );
  assert.equal(
    pendingFrom({ pendingMeasureKind: "DISTANCE", pendingMeasureWord: "km" }),
    null,
  );
});

test("whitespace is not a value", () => {
  assert.equal(
    pendingFrom({
      pendingChangeId: "chg-1",
      pendingMeasureKind: "DISTANCE",
      pendingMeasureWord: "   ",
    }),
    null,
  );
});

test("a complete proposal reads back", () => {
  assert.deepEqual(
    pendingFrom({
      pendingChangeId: " chg-1 ",
      pendingMeasureKind: "DISTANCE",
      pendingMeasureWord: "km",
    }),
    change,
  );
});

// ── The consequence is DERIVED, never chosen ─────────────────────────
//
// `C7` §5 offered the owner "reset or adapt". Working out what ADAPT could mean is what
// settles the design: a KIND change cannot be adapted without a unit conversion this app
// deliberately does not perform, so it IS a reset; a WORD-only change needs no adapting
// because no arithmetic depends on the word. There is no third answer and therefore no
// choice to offer.

test("a WORD-only change is a relabel -- every number and every link survives", () => {
  const c = consequenceOf({ kind: "DISTANCE", word: "km" }, { ...change, word: "kilometres" });
  assert.equal(c, "RELABEL");
});

test("a KIND change is a reset -- the only non-converting way to adapt is to re-link", () => {
  assert.equal(consequenceOf({ kind: "COUNT", word: "steps" }, change), "RESET");
});

test("a challenge that never HAD a kind is gaining one, not changing one", () => {
  // A pre-§6 document whose `metricUnit` was the "points" default. There is nothing scored
  // in an old unit to invalidate -- `Challenge.isUnmeasured` blocks linking entirely -- so
  // treating this as a reset would zero numbers the new kind does not disagree with.
  assert.equal(consequenceOf({ kind: null, word: null }, change), "RELABEL");
});

// ── The quorum ───────────────────────────────────────────────────────

test("everybody agreeing applies it", () => {
  const q = quorum(change, [row("a", "chg-1"), row("b", "chg-1")]);
  assert.deepEqual(q, { total: 2, approved: 2, reached: true });
});

test("one hold-out blocks it", () => {
  const q = quorum(change, [row("a", "chg-1"), row("b", null)]);
  assert.deepEqual(q, { total: 2, approved: 1, reached: false });
});

test("an approval of a DIFFERENT change does not count", () => {
  // The replay defence, and the reason `changeId` exists at all. Without it the owner
  // could withdraw a proposal, propose something else, and inherit consent nobody gave to
  // the second one.
  const q = quorum(change, [row("a", "chg-1"), row("b", "chg-0")]);
  assert.equal(q.reached, false);
});

test("the quorum is everyone who is STILL HERE", () => {
  // §6's third open question. A participant who leaves has no row to write
  // `approvedChangeId` in, so counting them would let one person walking away freeze the
  // challenge forever. Two rows, both agreeing, applies -- whoever else used to be here.
  const q = quorum(change, [row("a", "chg-1"), row("b", "chg-1")]);
  assert.equal(q.reached, true);
});

test("an EMPTY participant list never reaches quorum", () => {
  // Vacuously "everybody agrees", and that is exactly the reading to refuse: it is not a
  // challenge whose every member consented, it is one the trigger caught mid-delete.
  assert.deepEqual(quorum(change, []), { total: 0, approved: 0, reached: false });
});

// ── What gets written when it applies ────────────────────────────────

test("applying clears the proposal in the SAME write that carries it out", () => {
  const u = challengeUpdate(change);
  assert.equal(u.measureKind, "DISTANCE");
  assert.equal(u.measureWord, "km");
  // Left behind, these would have every client render a second change still waiting for
  // approval -- the one it has just finished applying.
  assert.equal(u.pendingChangeId, null);
  assert.equal(u.pendingMeasureKind, null);
  assert.equal(u.pendingMeasureWord, null);
  assert.equal(u.pendingProposedAt, null);
});

test("a relabel clears the approval and touches no number", () => {
  const u = participantUpdate("RELABEL");
  assert.deepEqual(u, { approvedChangeId: null });
});

test("a reset zeroes the three SERVER-owned score fields, and only those", () => {
  const u = participantUpdate("RESET");
  assert.equal(u.approvedChangeId, null);
  assert.equal(u.score, 0);
  assert.equal(u.scoreSource, "NONE");
  assert.equal(u.reportedAt, 0);
  // Never the identity fields. Those are the participant's, and `firestore.rules` says so
  // in the other direction -- this function must not be the one to break the symmetry.
  assert.equal("displayName" in u, false);
  assert.equal("photoUrl" in u, false);
  assert.equal("joinedAt" in u, false);
});

test("the approval is cleared EITHER way", () => {
  // It named a proposal that no longer exists. A stale value is what would make the next
  // proposal inherit consent nobody gave -- the same defect the `changeId` check above
  // guards from the other side.
  assert.equal(participantUpdate("RELABEL").approvedChangeId, null);
  assert.equal(participantUpdate("RESET").approvedChangeId, null);
});
