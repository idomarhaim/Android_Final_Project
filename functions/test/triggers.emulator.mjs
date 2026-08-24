/**
 * **The two projections, executed as real Firestore triggers.**
 *
 * `projection.test.mjs` beside this file proves the *arithmetic* and runs in milliseconds with no
 * emulator. It cannot prove any of the things that actually go wrong with a trigger: that the path
 * pattern matches, that `event.params` is spelled the way the handler reads it, that
 * `event.data.after` is the shape it expects, that the Admin SDK write lands where it was aimed,
 * and that an `update()` on a missing document is the silent no-op the design depends on. Those
 * are answered only by writing a fact and watching a document change.
 *
 * Deliberately **not** named `*.test.mjs`: `npm test` globs that pattern and must stay emulator-
 * free. This file is run by `npm run test:emulator`, which starts `firestore` and `functions`
 * first — see `run-emulator-tests.mjs`.
 *
 * Project id is `demo-goalpilot`. The `demo-` prefix is load-bearing exactly as it is in
 * `firestore-tests/rules.test.mjs`: the emulator suite treats such ids as offline-only and refuses
 * to reach any real backend, so nothing here can touch the live project `goalpilot-56e30`.
 *
 * **Polling, not sleeping.** A trigger is asynchronous and its latency is not a constant, so every
 * assertion waits for a *condition* with a deadline rather than for a duration. A fixed sleep is
 * either flaky or slow, and usually both.
 */
import test, { before, after, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { initializeApp, deleteApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

// The one list the prompt offers and the validator checks against (#6).
import { CATEGORIES } from "../lib/classify.js";

const UID = "uid_runner";
const CHALLENGE = "challenge_run_streak";
/** §3 needs a SECOND participant, or every quorum is trivially unanimous. */
const OTHER = "uid_rival";
/** The goal a challenge is scored from (§6). One user, one goal, one progress collection. */
const GOAL = "goal_steps";
const DEADLINE_MS = 15_000;

let app;
let db;

before(() => {
  // FIRESTORE_EMULATOR_HOST is set by `firebase emulators:exec` for this child process; the Admin
  // SDK reads it on its own. If it is missing, this would silently try to reach a real backend —
  // so fail loudly instead, naming the runner that sets it.
  assert.ok(
    process.env.FIRESTORE_EMULATOR_HOST,
    "FIRESTORE_EMULATOR_HOST is unset: run this through `npm run test:emulator`, never directly.",
  );
  app = initializeApp({ projectId: "demo-goalpilot" });
  db = getFirestore(app);
});

after(async () => {
  await deleteApp(app);
});

beforeEach(async () => {
  // Each case starts from nothing, because `projectPoints` sums the WHOLE task collection and a
  // leftover fact from the previous case would be counted.
  for (const path of [
    `users/${UID}/tasks`,
    `users/${UID}/challengeReports`,
    `challenges/${CHALLENGE}/participants`,
    // §6 sums the WHOLE progress collection over a window, so a leftover entry from the
    // previous case is counted exactly as a leftover task fact would be.
    `users/${UID}/goals/${GOAL}/progress`,
  ]) {
    const snap = await db.collection(path).get();
    await Promise.all(snap.docs.map((d) => d.ref.delete()));
  }
  await db.doc(`users/${UID}`).delete();
  await db.doc(`publicProfiles/${UID}`).delete();
  await db.doc(`challenges/${CHALLENGE}`).delete();
  // §3's second participant and their link. Both are outside the loop above because they
  // belong to a different uid, and a leftover row would join the next case's quorum.
  await db.doc(`challenges/${CHALLENGE}/participants/${OTHER}`).delete();
  await db.doc(`users/${OTHER}/challengeReports/${CHALLENGE}`).delete();
});

/** Waits for `read()` to satisfy `ok`, and reports the last value seen when it never does. */
async function eventually(label, read, ok) {
  const started = Date.now();
  let last;
  while (Date.now() - started < DEADLINE_MS) {
    last = await read();
    if (ok(last)) return last;
    await new Promise((r) => setTimeout(r, 250));
  }
  assert.fail(`${label}: still ${JSON.stringify(last)} after ${DEADLINE_MS} ms`);
}

const publicPoints = async () => (await db.doc(`publicProfiles/${UID}`).get()).data()?.points;
const privatePoints = async () => (await db.doc(`users/${UID}`).get()).data()?.points;
const standing = async () =>
  (await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).get()).data();

// ── projectPoints ────────────────────────────────────────────────────

test("completing a task projects onto BOTH the private doc and the public row", async () => {
  await db.doc(`users/${UID}/tasks/t1`).set({ done: true, points: 40, title: "Run" });

  assert.equal(await eventually("publicProfiles.points", publicPoints, (v) => v === 40), 40);
  assert.equal(await privatePoints(), 40, "users/{uid}.points — what Dashboard and Profile read");
});

test("an undone task contributes nothing, and un-ticking takes the points back", async () => {
  await db.doc(`users/${UID}/tasks/t1`).set({ done: true, points: 40 });
  await eventually("first projection", publicPoints, (v) => v === 40);

  await db.doc(`users/${UID}/tasks/t1`).update({ done: false });
  await eventually("points come back down", publicPoints, (v) => v === 0);
});

test("deleting a done task removes the fact, and the total follows it down", async () => {
  // The case a trigger registered only on create/update would miss. `onDocumentWritten` covers
  // deletes, and re-reading the whole collection is what makes the total right afterwards.
  await db.doc(`users/${UID}/tasks/t1`).set({ done: true, points: 40 });
  await db.doc(`users/${UID}/tasks/t2`).set({ done: true, points: 35 });
  await eventually("both counted", publicPoints, (v) => v === 75);

  await db.doc(`users/${UID}/tasks/t2`).delete();
  await eventually("one removed", publicPoints, (v) => v === 40);
});

test("the public row carries a server-set updatedAt, and no level", async () => {
  await db.doc(`users/${UID}/tasks/t1`).set({ done: true, points: 100 });
  await eventually("projected", publicPoints, (v) => v === 100);

  const row = (await db.doc(`publicProfiles/${UID}`).get()).data();
  assert.ok(row.updatedAt, "#50's as-of stamp rides the write that moves the number");
  assert.equal(row.level, undefined, "C20 deleted publicProfiles.level outright (spec 5.2)");
});

test("the projection does not clobber the identity fields beside the number", async () => {
  // AuthRepositoryImpl owns displayName / photoUrl / friendCode on this row. The projection writes
  // with { merge: true }; a plain set() here would silently blank a user's name on every tick.
  await db.doc(`publicProfiles/${UID}`).set({ displayName: "Ido", friendCode: "ABC123", points: 0 });
  await db.doc(`users/${UID}/tasks/t1`).set({ done: true, points: 25 });
  await eventually("projected", publicPoints, (v) => v === 25);

  const row = (await db.doc(`publicProfiles/${UID}`).get()).data();
  assert.equal(row.displayName, "Ido");
  assert.equal(row.friendCode, "ABC123");
});

// ── projectChallengeScore ────────────────────────────────────────────

test("a report fact projects onto the participant row the reporter may not write", async () => {
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({
    displayName: "Ido",
    score: 0,
    joinedAt: 1,
  });

  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ value: 12.5, reportedAt: 1 });

  const row = await eventually("participant score", standing, (v) => v?.score === 12.5);
  assert.ok(row.updatedAt, "the as-of stamp rides this write too");
  assert.equal(row.displayName, "Ido", "update() leaves the identity fields alone");
  // §6 / Ido's third ask: a typed number is LABELLED as one, by the server, on a field the
  // participant may not write.
  assert.equal(row.scoreSource, "REPORTED");
  assert.equal(row.reportedAt, 1, "and it says when — that is the 'what they updated' half");
});

test("a negative report is clamped to zero rather than stored", async () => {
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({ displayName: "Ido", score: 9 });
  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ value: -3, reportedAt: 1 });

  await eventually("clamped", standing, (v) => v?.score === 0);
});

test("reporting into a challenge you never joined writes nothing at all", async () => {
  // The design's load-bearing no-op: `update()` on a missing document throws NOT_FOUND, which the
  // handler swallows. A merging `set()` would create a participant row with no mirror edge — a
  // user scoring in a challenge that never appears in their own list.
  await db.doc(`users/${UID}/challengeReports/never_joined`).set({ value: 99, reportedAt: 1 });

  await new Promise((r) => setTimeout(r, 3000)); // nothing to poll FOR; give it time to misbehave
  const created = await db.doc(`challenges/never_joined/participants/${UID}`).get();
  assert.equal(created.exists, false, "no participant row may be conjured by a report");
});

test("deleting the report leaves the last standing alone", async () => {
  // `scoreFromReport` returns null for "write nothing". Zeroing a standing other people are reading
  // because its owner tidied up a private fact would be a silent data loss across the boundary.
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({ displayName: "Ido", score: 0 });
  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ value: 7, reportedAt: 1 });
  await eventually("projected", standing, (v) => v?.score === 7);

  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).delete();
  await new Promise((r) => setTimeout(r, 3000));
  assert.equal((await standing()).score, 7, "the last reported number stands");
});

// ── projectChallengeScoreOnProgress — §6 (`C14` #23) ─────────────────
//
// THIS IS THE TRIGGER THAT MAKES `R1` GO AWAY: *"a shared CHALLENGE does not sync with my tasks
// or with my Health Connect."* Nothing below mentions Health Connect, and that is the point —
// `SyncHealthDataUseCase` writes a `ProgressEntry` against a GOAL, a completed task writes one,
// a manual log writes one, and all three arrive at this trigger identically. A
// Health-Connect-to-challenge path would be the second representation of the same walk that §6
// exists to delete.
//
// `projection.test.mjs` proves the arithmetic. Only this file can prove the path pattern
// matches a THREE-segment subcollection, that the `where(goalId ==)` fan-out finds the fact, and
// that the write lands on a row the writer may not touch.

/** Links this user's challenge to [GOAL] and gives them a participant row joined at `joinedAt`. */
async function joinAndLink(joinedAt, { startAt = 0, endAt = 0 } = {}) {
  await db.doc(`challenges/${CHALLENGE}`).set({ title: "Steps", startAt, endAt });
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({
    displayName: "Ido",
    score: 0,
    joinedAt,
  });
  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({
    goalId: GOAL,
    linkedAt: joinedAt,
  });
}

const logProgress = (id, value, createdAt) =>
  db.doc(`users/${UID}/goals/${GOAL}/progress/${id}`).set({ goalId: GOAL, value, createdAt });

test("logging progress against a linked goal moves the challenge on its own", async () => {
  await joinAndLink(1_000);
  await eventually("linked at zero", standing, (v) => v?.scoreSource === "DERIVED");

  await logProgress("p1", 4_000, 1_500);

  const row = await eventually("derived score", standing, (v) => v?.score === 4_000);
  assert.equal(row.scoreSource, "DERIVED");
  assert.equal(row.reportedAt, 0, "nothing was typed, so the badge has no date to show");
  assert.equal(row.displayName, "Ido", "update() leaves the identity fields alone");
});

test("a second entry adds to it, and a deleted one comes back off", async () => {
  // Re-summing the whole collection, not accumulating — so a delete is handled by the same
  // code path as an insert and needs no compensating write of its own.
  await joinAndLink(1_000);
  await logProgress("p1", 4_000, 1_500);
  await logProgress("p2", 1_200, 1_600);
  await eventually("summed", standing, (v) => v?.score === 5_200);

  await db.doc(`users/${UID}/goals/${GOAL}/progress/p2`).delete();
  await eventually("and back down", standing, (v) => v?.score === 4_000);
});

test("joining with a year-old goal imports no history", async () => {
  // §6's whole reason for summing timestamped entries rather than a stored delta. The entry
  // exists BEFORE the link, so this also proves the initial link projects the right number
  // without waiting for the next log.
  await logProgress("ancient", 999_999, 10);
  await joinAndLink(1_000);

  const row = await eventually("derived", standing, (v) => v?.scoreSource === "DERIVED");
  assert.equal(row.score, 0, "movement SINCE you joined, not the goal's whole history");
});

test("the challenge's own dates bound the window at both ends", async () => {
  // The lower bound is a DERIVATION from the phase model rather than §6's words — see
  // `ScoringWindow` in Kotlin. The upper bound stops a DERIVED score climbing after a
  // challenge has ended, which `canReportScore` already forbids for a typed one.
  await joinAndLink(500, { startAt: 1_000, endAt: 2_000 });
  await logProgress("early", 50, 700); // joined, but the challenge had not started
  await logProgress("inside", 30, 1_500);
  await logProgress("late", 70, 2_000); // the end bound is exclusive

  await eventually("only the middle one counts", standing, (v) => v?.score === 30);
});

test("progress against an UNLINKED goal moves nothing", async () => {
  // The fan-out is `where(goalId ==)`, so a user logging against a goal no challenge points at
  // must not cost a write. This is the common case for every user in no challenge at all.
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({
    displayName: "Ido",
    score: 3,
    joinedAt: 1_000,
  });
  await logProgress("p1", 4_000, 1_500);

  await new Promise((r) => setTimeout(r, 3000)); // nothing to poll FOR; give it time to misbehave
  assert.equal((await standing()).score, 3, "an unlinked challenge is untouched");
});

test("typing a number over a link takes the challenge off the goal, and says so", async () => {
  // The two shapes of one fact are mutually exclusive by construction: the client writes the
  // fact with an unmerged `set()`, so the `goalId` is GONE rather than shadowed. That is what
  // makes the badge answerable — there is never a tiebreak to report the outcome of.
  await joinAndLink(1_000);
  await logProgress("p1", 4_000, 1_500);
  await eventually("derived first", standing, (v) => v?.score === 4_000);

  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ value: 8_200, reportedAt: 77 });

  const row = await eventually("switched to typed", standing, (v) => v?.score === 8_200);
  assert.equal(row.scoreSource, "REPORTED");
  assert.equal(row.reportedAt, 77);
});

test("linking a goal over a typed number switches back, and clears the date", async () => {
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({
    displayName: "Ido",
    score: 0,
    joinedAt: 1_000,
  });
  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ value: 8_200, reportedAt: 77 });
  await eventually("typed first", standing, (v) => v?.scoreSource === "REPORTED");

  await logProgress("p1", 500, 1_500);
  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ goalId: GOAL, linkedAt: 2 });

  const row = await eventually("switched to derived", standing, (v) => v?.score === 500);
  assert.equal(row.scoreSource, "DERIVED");
  assert.equal(row.reportedAt, 0, "the old typed date must not survive the switch");
});

// ── classifyTask (#6, spec §3.3 D / §3.4) ────────────────────────────

/**
 * The callable's emulator URL. Port and region are `firebase.json`'s and `setGlobalOptions`'
 * respectively, and both are hard-coded here rather than read from the environment because
 * `emulators:exec` exports a host for Firestore but not for Functions — a wrong guess would
 * silently connect to nothing and report a network error as a test failure.
 */
const CLASSIFY_URL = "http://127.0.0.1:5001/demo-goalpilot/us-central1/classifyTask";

/** Invokes the callable the way the Android client does: `{ data: … }` in, `{ result: … }` out. */
async function callClassify(data) {
  const res = await fetch(CLASSIFY_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ data }),
  });
  return { status: res.status, body: await res.json().catch(() => null) };
}

test("classifyTask cannot echo a goal id when no goals were sent — and a failure stays a failure", async () => {
  // **Why an EMPTY goals list.** This case calls the real model through the emulator, so the one
  // thing it must not assert is what the model says. Membership against an empty list is the
  // assertion that holds whatever comes back: §3.4 requires `suggestedGoalId` to be a member of
  // the list that travelled with the request, and no string is a member of nothing. If the field
  // survives, the validator is not wired into the deployed handler — which is precisely the
  // failure `projection.test.mjs`-style pure tests cannot see.
  const { status, body } = await callClassify({
    taskTitle: "Run 5 km on Friday",
    goals: [],
    lifeAreas: [],
  });

  if (status !== 200) {
    // The other half of the change, and the branch this takes when GROQ has no key or is
    // unreachable. `classifyTask` used to answer a transport failure with a fabricated 200 —
    // `suggestedGoalId: null` plus a sliced title — which took the NEW-GOAL branch, the one
    // branch of §3.4's table that speaks. A dead network made the app announce a new goal.
    // A failure must now arrive as a failure, so the client runs spec §8's own fallback.
    assert.ok(body?.error, `a failed classify must carry an error, got ${JSON.stringify(body)}`);
    assert.equal(body.result, undefined, "a transport failure must not carry a result body");
    return;
  }

  const result = body?.result;
  assert.ok(result && typeof result === "object", "a 200 must carry a result object");
  assert.equal(
    "suggestedGoalId" in result, false,
    `no goal was offered, so none may be echoed — got ${JSON.stringify(result)}`,
  );
  assert.equal("suggestedLifeAreaId" in result, false, "no life area was offered either");
  // Omit, never substitute (§3.3): a null would be indistinguishable from an answer at the
  // one field the client picks its branch on.
  for (const [k, v] of Object.entries(result)) {
    assert.notEqual(v, null, `${k} came back as null; the contract is absence`);
  }
  if ("suggestedCategory" in result) {
    assert.ok(CATEGORIES.includes(result.suggestedCategory), "category must be a declared enum");
  }
});

// ── §3 · the measure-change approval quorum ──────────────────────────
//
// `measureChange.test.mjs` proves the arithmetic with no Firestore at all, and
// `../firestore-tests/rules.test.mjs` proves the client can no longer write the live
// measure. ONLY THIS FILE can prove the trigger is actually wired: that both registrations
// fire, that the batch reaches a challenge document the approving participant may not
// write, and that a RESET deletes links in another user's private space.

/**
 * A pending change with two participants, one of whom has already agreed.
 *
 * ⚠️ **THE SCORES ARE LET IN BY THE PROJECTION, NEVER WRITTEN BY HAND, AND THE FIRST DRAFT
 * OF THIS HELPER GOT THAT WRONG.**
 *
 * It seeded `score: 3000` directly on the rival's row and also gave them a `goalId` link.
 * Writing that link fires `projectChallengeScore`, which sums the rival's OWN progress
 * against that goal — of which there is none — and correctly republishes **0** over the
 * hand-written 3000. The RELABEL case below then failed with `0 !== 3000`, reading as a
 * bug in a code path that was behaving perfectly. `Observed:` 2026-08-25, twice, the second
 * time after a wrong fix aimed at a cascade from the previous test.
 *
 * So the fixture now writes a **typed report**, waits for the projection to publish it, and
 * only then proposes. Two things fall out of that, both wanted: the scores under test are
 * ones the system actually produced, and the wait removes any leftover projection from an
 * earlier case before the change is proposed.
 *
 * The general shape is worth remembering: **in a suite whose whole subject is a projection,
 * a hand-written derived value is a fixture the system is entitled to overwrite.**
 */
async function proposeWithTwo(pendingKind, pendingWord) {
  await db.doc(`challenges/${CHALLENGE}`).set({
    title: "Steps",
    ownerUid: UID,
    measureKind: "COUNT",
    measureWord: "steps",
  });
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({
    displayName: "Ido",
    score: 0,
    joinedAt: 1,
  });
  await db.doc(`challenges/${CHALLENGE}/participants/${OTHER}`).set({
    displayName: "Rival",
    score: 0,
    joinedAt: 1,
  });
  // Typed, not linked: a report carries its own number, so the projection publishes
  // something non-zero without either user needing a goal with progress on it.
  await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).set({ value: 5_000, reportedAt: 1 });
  await db.doc(`users/${OTHER}/challengeReports/${CHALLENGE}`).set({ value: 3_000, reportedAt: 1 });

  await eventually("both scores published", rivalRow, (v) => v?.score === 3_000);
  await eventually(
    "my score published",
    async () => (await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).get()).data(),
    (v) => v?.score === 5_000,
  );

  // Only now propose, so nothing from the seeding is still in flight.
  await db.doc(`challenges/${CHALLENGE}`).update({
    pendingChangeId: "chg-1",
    pendingMeasureKind: pendingKind,
    pendingMeasureWord: pendingWord,
    pendingProposedAt: 1,
  });
  await db
    .doc(`challenges/${CHALLENGE}/participants/${UID}`)
    .update({ approvedChangeId: "chg-1" });
}

const challengeDoc = async () => (await db.doc(`challenges/${CHALLENGE}`).get()).data();
const rivalRow = async () =>
  (await db.doc(`challenges/${CHALLENGE}/participants/${OTHER}`).get()).data();

test("one hold-out leaves the measure exactly as it was", async () => {
  await proposeWithTwo("DISTANCE", "km");
  // Give both registrations time to fire and decide against applying. Asserting a
  // NON-event needs a wait, not a poll -- there is nothing to converge to.
  await new Promise((r) => setTimeout(r, 3_000));
  const c = await challengeDoc();
  assert.equal(c.measureKind, "COUNT");
  assert.equal(c.measureWord, "steps");
  assert.equal(c.pendingChangeId, "chg-1");
});

test("the last approval applies it, and clears the proposal in the same write", async () => {
  await proposeWithTwo("DISTANCE", "km");
  await db
    .doc(`challenges/${CHALLENGE}/participants/${OTHER}`)
    .update({ approvedChangeId: "chg-1" });

  const c = await eventually(
    "measure applied",
    challengeDoc,
    (v) => v?.measureKind === "DISTANCE",
  );
  assert.equal(c.measureWord, "km");
  // Left behind, these would have every client render a change still waiting for approval
  // -- the one it has just applied.
  assert.equal(c.pendingChangeId, null);
  assert.equal(c.pendingMeasureKind, null);
  assert.equal(c.pendingMeasureWord, null);
});

test("a RESET zeroes every score and deletes every link, in both users' spaces", async () => {
  await proposeWithTwo("DISTANCE", "km");
  await db
    .doc(`challenges/${CHALLENGE}/participants/${OTHER}`)
    .update({ approvedChangeId: "chg-1" });

  await eventually("rival reset", rivalRow, (v) => v?.score === 0);
  const rival = await rivalRow();
  assert.equal(rival.scoreSource, "NONE");
  assert.equal(rival.approvedChangeId, null);
  // The identity fields are the participant's and must survive.
  assert.equal(rival.displayName, "Rival");

  // The link lives under `users/{uid}` -- a document only the Admin SDK can reach from the
  // function. Leaving it would have the projection keep summing steps into a race now
  // measured in kilometres, which is the falsification the whole feature exists to stop.
  await eventually(
    "links cleared",
    async () => ({
      mine: (await db.doc(`users/${UID}/challengeReports/${CHALLENGE}`).get()).exists,
      theirs: (await db.doc(`users/${OTHER}/challengeReports/${CHALLENGE}`).get()).exists,
    }),
    (v) => !v.mine && !v.theirs,
  );

  // ⚠️ LET THIS TEST'S OWN CASCADE FINISH BEFORE THE NEXT ONE STARTS.
  //
  // Deleting those two links fires `projectChallengeScore` twice, and that trigger
  // outlives the assertion above. `Observed:` 2026-08-25 -- without this wait, the RELABEL
  // case below read a rival score of 0 where it had just written 3000, because a
  // projection from THIS test landed after the next test's `beforeEach` had cleaned up and
  // re-seeded. The failure names the wrong test and looks like a product bug in a code
  // path that is behaving correctly.
  //
  // Contained here rather than in `beforeEach`, deliberately: a settle in `beforeEach`
  // taxes all 23 cases for a cascade only this one creates. Not a product race -- after a
  // reset a late projection writes zero onto a row that is already zero.
  await new Promise((r) => setTimeout(r, 2_000));
});

test("a RELABEL keeps every score and every link", async () => {
  await proposeWithTwo("COUNT", "paces");
  await db
    .doc(`challenges/${CHALLENGE}/participants/${OTHER}`)
    .update({ approvedChangeId: "chg-1" });

  await eventually("word applied", challengeDoc, (v) => v?.measureWord === "paces");
  const rival = await rivalRow();
  // THE DISCRIMINATING ASSERTION OF THE PAIR. `RESET` above asserts 0, which is also what
  // the projection would produce on its own from a deleted report -- so that case alone
  // could not tell a working batch from a no-op. This one can: 3000 survives only because
  // `participantUpdate("RELABEL")` deliberately writes nothing but `approvedChangeId`.
  assert.equal(rival.score, 3_000);
  assert.equal(rival.scoreSource, "REPORTED");
  assert.equal(rival.approvedChangeId, null);
  assert.equal(
    (await db.doc(`users/${OTHER}/challengeReports/${CHALLENGE}`).get()).exists,
    true,
  );
});

test("the OWNER'S OWN PROPOSAL applies it on a solo challenge", async () => {
  // The reason `applyMeasureChangeOnProposal` exists. One row, approved in the proposal's
  // own batch, so NOTHING further is ever written to a participant row -- the approval
  // registration alone would wait forever.
  await db.doc(`challenges/${CHALLENGE}/participants/${UID}`).set({
    displayName: "Ido",
    score: 5_000,
    joinedAt: 1,
    approvedChangeId: "chg-9",
  });
  await db.doc(`challenges/${CHALLENGE}`).set({
    title: "Steps",
    ownerUid: UID,
    measureKind: "COUNT",
    measureWord: "steps",
    pendingChangeId: "chg-9",
    pendingMeasureKind: "DURATION",
    pendingMeasureWord: "hours",
    pendingProposedAt: 1,
  });

  const c = await eventually(
    "solo change applied",
    challengeDoc,
    (v) => v?.measureKind === "DURATION",
  );
  assert.equal(c.measureWord, "hours");
});

test("an approval naming a DIFFERENT change is not consent to this one", async () => {
  await proposeWithTwo("DISTANCE", "km");
  await db
    .doc(`challenges/${CHALLENGE}/participants/${OTHER}`)
    .update({ approvedChangeId: "chg-0" });

  await new Promise((r) => setTimeout(r, 3_000));
  assert.equal((await challengeDoc()).measureKind, "COUNT");
});
