package com.idomarhaim.goalpilot.core.util

/** Centralised, stable string keys shared across the data layer. */
object FirestorePaths {
    const val USERS = "users"
    const val GOALS = "goals"
    const val TASKS = "tasks"

    /**
     * Banked completions: `users/{uid}/completionFacts/{taskId}` (`#55`, spec §1.4).
     *
     * The document id **is** the task id, so a tick is a `set` and an untick a `delete`
     * of one known path — no query, no read-then-write, nothing to accumulate. Mirrored in
     * `functions/src/projection.ts`, which totals a user's points by reading this
     * collection: one name, two languages.
     */
    const val COMPLETION_FACTS = "completionFacts"

    /**
     * §2.1's occurrences: `users/{uid}/occurrences/{id}` (`#63`, spec §7.1).
     *
     * **Flat and per-user, not nested under the task**, for two reasons that point the same
     * way. §4.3's calendar surface asks *"what happens between these two dates?"* across every
     * task, which is one query here and a collection-group query under
     * `users/{uid}/tasks/{taskId}/occurrences`. And a per-user subcollection is already covered
     * by the owner-only `users/{uid}/{document=**}` match, so it needed **no `firestore.rules`
     * change at all** — the same finding life areas produced, and the reason `firestore-tests/`
     * gained a section asserting it rather than the rules file gaining a block.
     *
     * The owning task is the `taskId` field. One document exists per *when* the user has
     * actually touched; every other instance is generated from `Task.repeatRule` and is not
     * stored, which is what stops `R18`'s fortnightly flowers becoming 26 documents a year.
     */
    const val OCCURRENCES = "occurrences"

    /** User-defined life areas: users/{uid}/lifeAreas/{id}. */
    const val LIFE_AREAS = "lifeAreas"
    const val PROGRESS = "progress"
    const val SUMMARIES = "summaries"

    /** Public, world-readable projection used to build the friends leaderboard. */
    const val PUBLIC_PROFILES = "publicProfiles"

    /** Friend edges, private to the owner: users/{uid}/friends/{friendUid}. */
    const val FRIENDS = "friends"

    /** Shared achievement/summary feed items. */
    const val SHARES = "shares"

    /**
     * Competitive challenges. Used twice on purpose: the world-readable
     * `challenges/{id}`, and the private mirror edge `users/{uid}/challenges/{id}`
     * that answers "which challenges am I in?" in one query.
     */
    const val CHALLENGES = "challenges"

    /**
     * `challenges/{id}/participants/{uid}` — one self-owned row per member. The
     * document id must be the writer's uid; the security rule matches on it.
     *
     * **Its `score` is no longer writable from here** (`C20` #42, spec §5.2). The
     * client writes [CHALLENGE_REPORTS] instead and the projection function puts the
     * number on this row; `firestore.rules` enforces the split.
     */
    const val PARTICIPANTS = "participants"

    /**
     * `users/{uid}/challengeReports/{challengeId}` — **the fact behind a standing.**
     *
     * What the participant measured, private to them and written by them, one document
     * per challenge they are in. `challenges/{id}/participants/{uid}.score` is the
     * *projection* of it, written by `functions/src/projection.ts` because the people
     * reading the standings cannot read this path — which is exactly spec §5.2's test
     * for a derived number needing a stored writer, and the reason `score` was the one
     * quantity of seven that kept one.
     *
     * It lives under `users/{uid}` so it inherits `isOwner(uid)` and, with it, the
     * offline cache: reporting a score works with the radio off, like every other fact.
     */
    const val CHALLENGE_REPORTS = "challengeReports"

    /**
     * `challengeInvites/{inviteId}` — one person asking one other person to join one
     * challenge (Ido's own report, 2026-08-24: *"I cannot invite a friend I have in the
     * app to the CHALLENGE"*).
     *
     * **Top-level, like [SHARES], and for the same reason**: it is a document two
     * different users must each be able to reach on their own account. Everywhere tidier
     * is unreachable — `users/{friendUid}/{document=**}` is `isOwner(friendUid)`, and so
     * is their participant row. See `ChallengeInvite` for the full derivation.
     *
     * *(That path is spelt out in full rather than abbreviated on purpose: the short
     * form ends in a slash-star pair, which OPENS A NESTED BLOCK COMMENT in Kotlin and
     * takes the rest of the file with it. `Observed:` 2026-08-25, this very line — the
     * compiler reported `Missing '}'` at line 80 and `Unclosed comment` at the last line
     * of the file, naming neither the KDoc nor the token. Same family as the `--`-in-XML
     * and backslash-in-`.properties` traps this repo's `CLAUDE.md` already records.)*
     *
     * ⚠️ **Its read rule inspects `resource.data`, which constrains every QUERY and not
     * only every get.** Firestore rejects a query it cannot prove is inside the rule, so
     * a listener here must always carry `whereEqualTo("toUid", myUid)` or
     * `whereEqualTo("fromUid", myUid)`. An unconstrained `collection(CHALLENGE_INVITES)`
     * listener fails with `PERMISSION_DENIED` and the message does not say why —
     * `firestore-tests/rules.test.mjs` asserts both directions so the next person finds
     * out from a test rather than from a device.
     */
    const val CHALLENGE_INVITES = "challengeInvites"
}

/** Names of the Firebase Cloud Functions the client calls (callable HTTPS). */
object CloudFunctions {
    const val GET_RECOMMENDATIONS = "getRecommendations"
    const val CLASSIFY_TASK = "classifyTask"
    const val SCORE_TASK = "scoreTask"

    /**
     * §3.3 E's `measure` — a concrete measure for a goal that has none (`C22` #44, #65).
     *
     * `C7`'s fifth AI feature, and the one `C11b` never wrote a format for; §10.1 has the
     * account. The name matches `export const proposeMeasure` in `functions/src/index.ts`,
     * which is the only thing that makes the call resolve.
     */
    const val PROPOSE_MEASURE = "proposeMeasure"

    /**
     * Where a **goal** belongs, from its title alone (§3.3 D's schema, §0.7; Ido 2026-08-24).
     *
     * Reuses `classify`'s validator on the far side with an empty `goals[]` rather than adding
     * a sixth schema — see `RecommendationRepository.fileGoal`. The name matches
     * `export const fileGoal` in `functions/src/index.ts`, which is the only thing that makes
     * the call resolve.
     */
    const val FILE_GOAL = "fileGoal"

    /**
     * §3.3 B's `plan` — a proposed work plan for one goal (§3.7, `C8` #24).
     *
     * Matches `export const planGoal` in `functions/src/index.ts`.
     */
    const val PLAN_GOAL = "planGoal"
}

/** Firebase Storage folder layout. */
object StoragePaths {
    const val PROGRESS_IMAGES = "progress_images"
    const val SUMMARY_IMAGES = "summary_images"
}
