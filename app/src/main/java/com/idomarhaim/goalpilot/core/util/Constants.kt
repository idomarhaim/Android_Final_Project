package com.idomarhaim.goalpilot.core.util

/** Centralised, stable string keys shared across the data layer. */
object FirestorePaths {
    const val USERS = "users"
    const val GOALS = "goals"
    const val TASKS = "tasks"

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
}

/** Names of the Firebase Cloud Functions the client calls (callable HTTPS). */
object CloudFunctions {
    const val GET_RECOMMENDATIONS = "getRecommendations"
    const val CLASSIFY_TASK = "classifyTask"
    const val SCORE_TASK = "scoreTask"
}

/** Firebase Storage folder layout. */
object StoragePaths {
    const val PROGRESS_IMAGES = "progress_images"
    const val SUMMARY_IMAGES = "summary_images"
}
