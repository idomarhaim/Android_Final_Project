/**
 * `C20`'s projection: the **only** writer of derived state in GoalPilot.
 *
 * One projection, two trigger registrations (`docs/PRODUCT_v0.3.md` §5.2):
 *
 * 1. **completion facts -> `users/{uid}.points` and `publicProfiles/{uid}.points`** — two
 *    registrations since #55, one on `completionFacts` (the live one) and one on `tasks`
 *    (deletes, and the pre-#55 completions that have not been migrated by a write yet)
 * 2. **report facts -> `challenges/{challengeId}/participants/{uid}.score`**
 *
 * ### Why the client stopped writing these
 *
 * §5.2's rule is *a derived number gets a stored writer if and only if somebody who cannot
 * read its inputs has to read it*, and `firestore.rules` already draws that boundary at
 * `isOwner(uid)`. The owner reads their own tasks, so their own points need no stored
 * writer at all — they are summed on the device, offline, from the cache. A **leaderboard
 * reader** cannot read those tasks, so the public copy does need one, and it is here.
 *
 * The offline win falls out of the same move: `TaskRepositoryImpl.setDone` used to hold all
 * three writes in one `firestore.runTransaction`, which cannot be served from the Firestore
 * cache and failed after a measured 7.9 s with the radio off (closed #3). Reduced to the one
 * write that is a *fact*, it is an ordinary single-document write that the cache takes
 * immediately. Nothing here is on the completion path — the tick does not wait for it.
 *
 * ### Reading the whole fact set is the design, not a cost oversight
 *
 * Each run re-reads every task and writes the total. That is what makes it **structurally**
 * idempotent: a redelivered event, a retried invocation and a manual re-run all write the
 * same number, because no prior value is an input. §5.2 rejected `FieldValue.increment` on
 * exactly this ground — `increment` *is* the accumulator, and it makes double-crediting
 * something the function has to be *careful* about rather than something it cannot do.
 *
 * ### What is deliberately absent
 *
 * - **`publicProfiles.level` is not written.** It was a stored function of `points` in the
 *   same document, so every reader of it could already compute it; §5.2 deleted it and the
 *   client now derives it through `Leveling` at the point of use.
 * - **No participant row is ever created here.** `projectChallengeScore` uses `update()`, so
 *   a report from somebody who has not joined (or who has left) writes nothing. A merging
 *   `set()` would resurrect a participant row with no mirror edge — the exact bug
 *   `ChallengeRepositoryImpl.reportScore` documents in its own comment.
 *
 * The arithmetic itself is in `derived.ts`, with no Firebase imports, so `test/` can run it
 * against `shared-fixtures/derived-state.json` — the same fixture the JVM suite reads.
 */
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import {
  CompletionFact,
  LegacyTaskFact,
  pointsFromFacts,
  scoreFromReport,
} from "./derived";

/** Mirrors `core/util/FirestorePaths` on the client — one name, two languages. */
const USERS = "users";
const TASKS = "tasks";
/** `FirestorePaths.COMPLETION_FACTS` — where a completion has lived since #55, §1.4. */
const COMPLETION_FACTS = "completionFacts";
const PUBLIC_PROFILES = "publicProfiles";
const CHALLENGES = "challenges";
const PARTICIPANTS = "participants";
const CHALLENGE_REPORTS = "challengeReports";
/** `FirestoreExt.UPDATED_AT` — the server-set as-of stamp both cross-boundary rows carry. */
const UPDATED_AT = "updatedAt";

/**
 * Re-total one user's points and publish them. Shared by both point triggers.
 *
 * ### It reads two collections, and the second one is a migration
 *
 * Since #55 a completion is its own document under `users/{uid}/completionFacts/{taskId}`,
 * banking the **minutes and difficulty** as they stood at the tick. That is the live source.
 * The tasks collection is read as well because a task completed **before** #55 carries
 * `done` + `points` on its own document and has no fact — and #55 writes no backfill, so
 * those totals would vanish on deploy if this only read the new collection. `pointsFromFacts`
 * keys both by task id, so a task that has been re-ticked since is counted once.
 *
 * ### Reading the whole fact set is the design, not a cost oversight
 *
 * Each run re-reads everything and writes the total. That is what makes it **structurally**
 * idempotent: a redelivered event, a retried invocation and a manual re-run all write the
 * same number, because no prior value is an input. §5.2 rejected `FieldValue.increment` on
 * exactly this ground — `increment` *is* the accumulator, and it makes double-crediting
 * something the function has to be *careful* about rather than something it cannot do.
 */
async function republishPoints(uid: string, trigger: string): Promise<void> {
  const db = getFirestore();
  const userRef = db.collection(USERS).doc(uid);

  const [factSnap, taskSnap] = await Promise.all([
    userRef.collection(COMPLETION_FACTS).get(),
    userRef.collection(TASKS).get(),
  ]);
  const facts: Record<string, CompletionFact> = {};
  for (const d of factSnap.docs) facts[d.id] = d.data() as CompletionFact;
  const legacyTasks: Record<string, LegacyTaskFact> = {};
  for (const d of taskSnap.docs) legacyTasks[d.id] = d.data() as LegacyTaskFact;
  const points = pointsFromFacts(facts, legacyTasks);

  // The private doc is the owner's own copy. They can compute it themselves from the
  // cache, so this write is a convenience for anything reading the user document
  // directly — never the source of truth the tick waits on.
  await userRef.set({ points }, { merge: true });

  // The public row is the one that had to exist. `updatedAt` rides only this write —
  // the write that moves the number — so a reader on another device is told when the
  // score was last touched rather than whether their own radio is on (#50, §5.3).
  await db
    .collection(PUBLIC_PROFILES)
    .doc(uid)
    .set({ points, [UPDATED_AT]: FieldValue.serverTimestamp() }, { merge: true });

  logger.info("projectPoints", {
    uid,
    points,
    trigger,
    factCount: factSnap.size,
    legacyTaskCount: taskSnap.size,
  });
}

/**
 * **Trigger 1a — a completion fact moved.**
 *
 * The live trigger since #55: a tick writes `completionFacts/{taskId}`, an untick deletes it,
 * and either way the total has to follow.
 */
export const projectPoints = onDocumentWritten(
  USERS + "/{uid}/" + COMPLETION_FACTS + "/{taskId}",
  async (event) => republishPoints(event.params.uid, COMPLETION_FACTS),
);

/**
 * **Trigger 1b — a task moved.**
 *
 * Still needed, and for two reasons that outlive the migration. A **delete** of a done task
 * removes its fact in the same batch, but a task that was completed before #55 has no fact to
 * delete — only its own `done` field, which this trigger sees and the other one never would.
 * And the migrating write that clears those legacy fields lands on the *task*, so the total
 * has to be recomputed when it does.
 *
 * It does not try to work out whether anything relevant changed. Re-running on an irrelevant
 * edit is free of consequence, which is the property projecting from whole fact sets buys.
 */
export const projectPointsOnTaskWrite = onDocumentWritten(
  USERS + "/{uid}/" + TASKS + "/{taskId}",
  async (event) => republishPoints(event.params.uid, TASKS),
);

/**
 * **Trigger 2 — a report fact moved.**
 *
 * The participant's own measurement lives at `users/{uid}/challengeReports/{challengeId}`,
 * which they own and can write offline like any other fact. This projects it onto the
 * public standing, which they no longer may write themselves — `firestore.rules` now pins
 * `score` on the participant row, and that is the first field-level condition in the file.
 */
export const projectChallengeScore = onDocumentWritten(
  USERS + "/{uid}/" + CHALLENGE_REPORTS + "/{challengeId}",
  async (event) => {
    const { uid, challengeId } = event.params;
    const score = scoreFromReport(event.data?.after?.data());

    // `null` means *write nothing* — see `scoreFromReport`. Removing the fact must not
    // zero a standing other people are reading.
    if (score === null) {
      logger.info("projectChallengeScore: no report, row left alone", { uid, challengeId });
      return;
    }

    const ref = getFirestore()
      .collection(CHALLENGES)
      .doc(challengeId)
      .collection(PARTICIPANTS)
      .doc(uid);

    try {
      await ref.update({ score, [UPDATED_AT]: FieldValue.serverTimestamp() });
      logger.info("projectChallengeScore", { uid, challengeId, score });
    } catch (e) {
      // NOT_FOUND is the ordinary case, not a failure: reporting into a challenge you
      // never joined, or one you have since left. `update()` is what makes it a no-op
      // instead of a resurrected participant row with no mirror edge.
      logger.info("projectChallengeScore: no participant row, nothing written", {
        uid,
        challengeId,
        reason: String(e),
      });
    }
  },
);
