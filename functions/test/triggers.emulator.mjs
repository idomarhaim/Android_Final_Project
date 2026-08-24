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
