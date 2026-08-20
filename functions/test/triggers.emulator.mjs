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

const UID = "uid_runner";
const CHALLENGE = "challenge_run_streak";
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
  ]) {
    const snap = await db.collection(path).get();
    await Promise.all(snap.docs.map((d) => d.ref.delete()));
  }
  await db.doc(`users/${UID}`).delete();
  await db.doc(`publicProfiles/${UID}`).delete();
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
