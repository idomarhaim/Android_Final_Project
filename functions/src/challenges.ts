/**
 * §6's measure-change approval quorum — the **trigger** half.
 *
 * The arithmetic is in `measureChange.ts`, with no Firebase imports, so `test/` can run it
 * as plain `node --test`; this file is only the part that needs Firestore. Same split as
 * `derived.ts` / `projection.ts`, and for the same reason.
 *
 * ### Why this is a Function at all, when §1's invites deliberately were not
 *
 * §1 rejected a Cloud Function for invites because the rules partition already modelled the
 * problem — two named parties, one document each can reach. This one is the opposite case
 * and it is the case `C20` scoped the Function layer to: **the write has to see documents
 * the writer cannot.** Deciding that a change is unanimous means reading *every*
 * participant's row and then writing the *challenge* document, and no single client is
 * permitted both. A participant can read the rows but may not write the challenge; the
 * owner may write the challenge but has no way to be sure they read every row at the
 * instant they did it.
 *
 * ### Two registrations, because either side can complete the quorum
 *
 * The last act before a change applies is sometimes a **participant approving** and
 * sometimes the **owner proposing** — a solo challenge, or one whose members all approved a
 * previous proposal and had their rows cleared. Registering only the first would leave a
 * one-person challenge's proposal waiting forever for an approval that already exists.
 *
 * Both funnel into [applyIfUnanimous], which re-reads everything and is therefore
 * **structurally idempotent** in `projection.ts`'s sense: a redelivered event, a retried
 * invocation and both triggers firing at once all compute the same answer from the same
 * documents, because no prior value is an input.
 */
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import {
  ApprovalRow,
  challengeUpdate,
  consequenceOf,
  participantUpdate,
  pendingFrom,
  quorum,
} from "./measureChange";

/** Mirrors `core/util/FirestorePaths` on the client — one name, two languages. */
const USERS = "users";
const CHALLENGES = "challenges";
const PARTICIPANTS = "participants";
const CHALLENGE_REPORTS = "challengeReports";

/**
 * Applies the pending measure change on `challengeId` if — and only if — every participant
 * row that exists right now names it in `approvedChangeId`.
 *
 * Everything is written in **one batch**. A challenge whose measure had changed but whose
 * standings still held the old unit's numbers would be the exact falsification §6 is
 * about, and it would be visible for however long the second write took.
 */
async function applyIfUnanimous(challengeId: string): Promise<void> {
  const db = getFirestore();
  const challengeRef = db.collection(CHALLENGES).doc(challengeId);
  const snap = await challengeRef.get();
  if (!snap.exists) return;

  const data = snap.data() as Record<string, unknown> | undefined;
  const pending = pendingFrom(data);
  if (!pending) return;

  const participants = await challengeRef.collection(PARTICIPANTS).get();
  const rows: ApprovalRow[] = participants.docs.map((d) => ({
    uid: d.id,
    approvedChangeId: (d.data() as Record<string, unknown>).approvedChangeId as
      | string
      | undefined,
  }));

  const state = quorum(pending, rows);
  if (!state.reached) {
    logger.debug("measure change waiting", {
      challengeId,
      changeId: pending.changeId,
      approved: state.approved,
      total: state.total,
    });
    return;
  }

  const consequence = consequenceOf(
    {
      kind: ((data?.measureKind as string | null) ?? null),
      word: ((data?.measureWord as string | null) ?? null),
    },
    pending,
  );

  const batch = db.batch();
  batch.update(challengeRef, challengeUpdate(pending));
  const rowUpdate = participantUpdate(consequence);
  participants.docs.forEach((d) => batch.update(d.ref, rowUpdate));
  await batch.commit();

  // THE LINKS GO SEPARATELY, AND ONLY ON A RESET.
  //
  // A participant's link lives at `users/{uid}/challengeReports/{challengeId}` -- their own
  // document, in their own space, which only the Admin SDK can reach from here. After a
  // KIND change it points at a goal of the wrong kind: `Challenge.canBeScoredFrom` would
  // refuse to offer that goal today, and leaving the fact in place would have the
  // projection keep summing steps into a race now measured in kilometres. Deleting it is
  // what "reset" means -- everybody re-links, or creates a goal for the new unit through
  // §2's form, which is the only non-converting way to adapt.
  //
  // Not batched with the above on purpose: these are other people's documents in another
  // part of the tree, and a failure here must not roll back a measure change that every
  // participant has already agreed to. It is re-runnable -- the next write on any of these
  // paths comes back through here with no pending change and does nothing.
  if (consequence === "RESET") {
    await Promise.all(
      rows.map((r) =>
        db
          .collection(USERS)
          .doc(r.uid)
          .collection(CHALLENGE_REPORTS)
          .doc(challengeId)
          .delete()
          .catch((e) =>
            logger.warn("could not clear a link after a measure reset", {
              challengeId,
              uid: r.uid,
              error: String(e),
            }),
          ),
      ),
    );
  }

  logger.info("measure change applied", {
    challengeId,
    changeId: pending.changeId,
    consequence,
    participants: state.total,
  });
}

/**
 * **Registration 1 — a participant approved (or left).**
 *
 * Leaving fires this too, and that is wanted rather than tolerated: the quorum is *everyone
 * who is still here*, so the departure of the last hold-out is exactly the event that
 * completes it. Without this registration, one person walking away would leave a change
 * that now has unanimous consent sitting unapplied until somebody else happened to write.
 */
export const applyMeasureChangeOnApproval = onDocumentWritten(
  CHALLENGES + "/{challengeId}/" + PARTICIPANTS + "/{uid}",
  async (event) => applyIfUnanimous(event.params.challengeId),
);

/**
 * **Registration 2 — the owner proposed.**
 *
 * The proposal itself can be the last act: a solo challenge has one row, the owner's, and
 * `ChallengeRepositoryImpl.proposeMeasureChange` writes their approval in the same batch,
 * so nothing further will ever be written to a participant row. Registration 1 alone would
 * leave that proposal waiting forever.
 */
export const applyMeasureChangeOnProposal = onDocumentWritten(
  CHALLENGES + "/{challengeId}",
  async (event) => applyIfUnanimous(event.params.challengeId),
);
