package com.idomarhaim.goalpilot.domain.model

/**
 * One person asking one other person to join one challenge —
 * `challengeInvites/{inviteId}`.
 *
 * ### Why this is a top-level collection and not something tidier
 *
 * Ido, 2026-08-24: *"in the version I have on my phone I can create a CHALLENGE
 * but I cannot invite a friend I have in the app to the CHALLENGE"*. He was
 * exactly right — before this there was no invite mechanism anywhere in the app,
 * and a friend could only reach a challenge through **Discover**, which lists
 * every challenge in the database to every signed-in user. So joining was never
 * impossible; there was simply no way to say *"join mine"* and no way for the
 * other person to be told.
 *
 * The two homes that first come to mind are both **unreachable**, and that is
 * what dictates the shape here rather than a preference:
 *
 *  * **Not `users/{friendUid}/…`.** `firestore.rules` gives
 *    `users/{uid}/{document=**}` to `isOwner(uid)` alone, so you cannot write
 *    into your friend's space at all.
 *  * **Not their participant row.** `challenges/{id}/participants/{uid}` is
 *    `isOwner(uid)` too — and a row conjured for somebody who never joined is
 *    the exact bug `ChallengeRepositoryImpl` already documents, because the
 *    standings would then show a competitor who never agreed to compete.
 *  * **Not a Cloud Function with the Admin SDK.** It would work, and it is the
 *    wrong reach: it buys a deploy, a second write path and a trigger for
 *    something the rules partition already models. The Function layer here is
 *    for **derived numbers** (`C20` #42), which is a different job.
 *
 * What is left is the shape `shares/{shareId}` already uses — a **top-level
 * document that two different users can each reach on their own account**. The
 * rule names both of them and nobody else: an invite is not world-readable, it
 * is readable by its two parties.
 *
 * ### An invite is an offer, never an obligation
 *
 * There is deliberately **no badge count, no red dot and no notification**. It is
 * a row at the top of the Challenges screen that is there, and then is not. The
 * notification system in this app schedules reminders for the user's *own* work;
 * an inbound request from another person is a different class with a different
 * consent story, and `C9a` §6 does not cover it. Named as owed, not built.
 *
 * ### Nothing is ever written to the invitee
 *
 * Accepting runs the ordinary `joinChallenge` under the invitee's own uid and
 * deletes the invite in the same batch. Joining stays their own act, which is
 * what keeps the whole participants partition honest.
 *
 * @property challengeTitle Copied at send time on purpose. The invitee can read
 *   `challenges/{id}` — every challenge is readable to signed-in users — but a
 *   row that has to resolve a second document to render its own sentence would
 *   flash a title-less line first, and an invite to a challenge the owner has
 *   since deleted would render as a blank offer instead of a stale one. The copy
 *   is a **caption**, not a source of truth: `acceptInvite` reads the real
 *   challenge and fails honestly if it is gone.
 */
data class ChallengeInvite(
    val id: String = "",
    val challengeId: String = "",
    val challengeTitle: String = "",
    val fromUid: String = "",
    /** The sender's display name at send time — a caption, like [challengeTitle]. */
    val fromName: String = "",
    val fromPhotoUrl: String? = null,
    val toUid: String = "",
    val createdAtEpochMillis: Long = 0L,
) {
    /** What the row says, with a fallback for a sender who never set a name. */
    val senderLabel: String get() = fromName.ifBlank { "Someone" }
}

/**
 * One friend, as the invite sheet offers them — a friend plus **why they cannot
 * be invited**, if they cannot.
 *
 * The two blocked states are shown rather than filtered out, and that is a
 * decision worth its line. A friend who silently disappears from the list reads
 * as *the app does not know them*, which is the one thing the user is certain is
 * false; a greyed row saying *"Already in"* answers the question they actually
 * had. It also makes the sheet's length stable, so inviting three people in a row
 * does not make the list jump under the finger.
 */
data class InviteCandidate(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    /** They are already a participant — there is nothing to invite them to. */
    val isParticipant: Boolean = false,
    /** They already hold an invite to this challenge from this user. */
    val isInvited: Boolean = false,
) {
    /** Whether the row's action is live. A second invite is noise, not emphasis. */
    val canInvite: Boolean get() = !isParticipant && !isInvited

    /** A display name, or an honest stand-in for a friend who never set one. */
    val label: String get() = displayName.ifBlank { "GoalPilot user" }
}
