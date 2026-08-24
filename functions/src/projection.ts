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
  ProgressFact,
  ReportFact,
  linkedGoalId,
  pointsFromFacts,
  scoringWindow,
  standingFromFact,
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
/** `FirestorePaths.GOALS` / `.PROGRESS` -- where a goal's timestamped entries live (#49). */
const GOALS = "goals";
const PROGRESS = "progress";
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
 * Re-project one participant's standing. Shared by both challenge triggers.
 *
 * ### Why this reads four documents and stores no baseline
 *
 * §6's score is **movement in the linked goal since you joined**, not the goal's current
 * value — without which a weight-loss race would rank by *who is heaviest*. That could have
 * been a stored delta with a baseline captured at join time; §6 chose a **sum over
 * timestamped entries** instead, and the reason is that all four of the awkward cases stop
 * being cases at all: re-linking a different goal re-sums a different list, unlinking sums
 * nothing, a backfilled entry lands inside or outside the window on its own timestamp, and
 * `ProgressEntry.sourceKey` already stopped a Health Connect re-sync writing a second entry
 * for the same day. A baseline would have had to be repaired in every one of them.
 *
 * So: the **fact** says which goal (or which typed number), the **participant row** says
 * when they joined, the **challenge** says when it runs, and the **progress collection** is
 * the movement. The arithmetic itself is in `derived.ts` with no Firebase imports, so
 * `test/projection.test.mjs` runs it with no emulator.
 *
 * ### Reading the whole entry set is the design, not a cost oversight
 *
 * Same property as points, for the same reason: no prior score is an input, so a redelivered
 * event, a retried invocation and a manual re-run all write the same number. §5.2 rejected
 * `FieldValue.increment` on exactly this ground.
 */
async function republishStanding(
  uid: string,
  challengeId: string,
  trigger: string,
): Promise<void> {
  const db = getFirestore();
  const factRef = db
    .collection(USERS)
    .doc(uid)
    .collection(CHALLENGE_REPORTS)
    .doc(challengeId);
  const rowRef = db
    .collection(CHALLENGES)
    .doc(challengeId)
    .collection(PARTICIPANTS)
    .doc(uid);

  const [factSnap, challengeSnap, rowSnap] = await Promise.all([
    factRef.get(),
    db.collection(CHALLENGES).doc(challengeId).get(),
    rowRef.get(),
  ]);

  const report = (factSnap.exists ? factSnap.data() : null) as ReportFact | null;
  const goalId = linkedGoalId(report);

  let progress: ProgressFact[] = [];
  if (goalId !== null) {
    // The whole collection, filtered in `derived.ts` rather than by a `where` clause. It is
    // one user's entries for one goal, the arithmetic has to be testable without an
    // emulator, and a range query here would need a composite index for a bound that costs
    // nothing to apply in memory.
    const entries = await db
      .collection(USERS)
      .doc(uid)
      .collection(GOALS)
      .doc(goalId)
      .collection(PROGRESS)
      .get();
    progress = entries.docs.map((d) => d.data() as ProgressFact);
  }

  const window = scoringWindow(
    challengeSnap.exists ? (challengeSnap.data() as { startAt?: number; endAt?: number }) : null,
    rowSnap.exists ? (rowSnap.data() as { joinedAt?: number }).joinedAt : null,
  );
  const standing = standingFromFact(report, progress, window, report?.reportedAt as number);

  // `null` means *write nothing* — see `standingFromFact`. Removing the fact must not zero a
  // standing other people are reading.
  if (standing === null) {
    logger.info("projectChallengeScore: no report, row left alone", {
      uid,
      challengeId,
      trigger,
    });
    return;
  }

  try {
    await rowRef.update({
      score: standing.score,
      // Written by the server and pinned by `firestore.rules`, because a participant who
      // could write their own label could type a number and mark it DERIVED — the label
      // would then assert the one thing it exists to deny (Ido's third ask on #23).
      scoreSource: standing.scoreSource,
      reportedAt: standing.reportedAt,
      [UPDATED_AT]: FieldValue.serverTimestamp(),
    });
    logger.info("projectChallengeScore", {
      uid,
      challengeId,
      trigger,
      score: standing.score,
      scoreSource: standing.scoreSource,
      entryCount: progress.length,
    });
  } catch (e) {
    // NOT_FOUND is the ordinary case, not a failure: reporting into a challenge you never
    // joined, or one you have since left. `update()` is what makes it a no-op instead of a
    // resurrected participant row with no mirror edge.
    logger.info("projectChallengeScore: no participant row, nothing written", {
      uid,
      challengeId,
      reason: String(e),
    });
  }
}

/**
 * **Trigger 2a — a participant's own fact moved.**
 *
 * The participant's fact lives at `users/{uid}/challengeReports/{challengeId}`, which they
 * own and can write offline like any other fact. Since §6 it carries one of **two** shapes,
 * never both — a `goalId` link, or a typed `value` — and this fires for either.
 */
export const projectChallengeScore = onDocumentWritten(
  USERS + "/{uid}/" + CHALLENGE_REPORTS + "/{challengeId}",
  async (event) =>
    republishStanding(event.params.uid, event.params.challengeId, CHALLENGE_REPORTS),
);

/**
 * **Trigger 2b — progress was logged against a goal, so every challenge linked to it moves.**
 *
 * This is the trigger that makes `R1` go away: *"a shared challenge does not sync with my
 * tasks or with my Health Connect."* It never mentions Health Connect, and that is the whole
 * point — `SyncHealthDataUseCase` writes a `ProgressEntry` against a **goal**, a completed
 * task writes one, a manual log writes one, and all three arrive here identically. Building a
 * Health-Connect-to-challenge path instead would be the second representation of the same
 * walk that §6 exists to delete.
 *
 * ### The fan-out is over the user's own facts, and is bounded by their challenges
 *
 * One `where(goalId ==)` over `users/{uid}/challengeReports` — a handful of documents for a
 * real user, and a collection they own, so no cross-boundary read is involved. A user in no
 * challenge, or one whose challenges point at other goals, does one empty query per logged
 * entry and writes nothing.
 *
 * ### It does not try to work out whether the entry was relevant
 *
 * A delete, an edit and a create all re-sum the same set to the same number, so there is
 * nothing to be careful about — the property projecting from whole fact sets buys. An entry
 * that falls outside the scoring window is filtered by the arithmetic, not by this trigger,
 * so a backfilled entry with an old timestamp correctly changes nothing.
 */
export const projectChallengeScoreOnProgress = onDocumentWritten(
  USERS + "/{uid}/" + GOALS + "/{goalId}/" + PROGRESS + "/{entryId}",
  async (event) => {
    const { uid, goalId } = event.params;
    const linked = await getFirestore()
      .collection(USERS)
      .doc(uid)
      .collection(CHALLENGE_REPORTS)
      .where("goalId", "==", goalId)
      .get();
    if (linked.empty) return;
    await Promise.all(
      linked.docs.map((d) => republishStanding(uid, d.id, PROGRESS)),
    );
  },
);
