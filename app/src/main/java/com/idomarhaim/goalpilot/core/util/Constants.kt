package com.idomarhaim.goalpilot.core.util

/** Centralised, stable string keys shared across the data layer. */
object FirestorePaths {
    const val USERS = "users"
    const val GOALS = "goals"
    const val TASKS = "tasks"
    const val PROGRESS = "progress"
    const val SUMMARIES = "summaries"

    /** Public, world-readable projection used to build the friends leaderboard. */
    const val PUBLIC_PROFILES = "publicProfiles"

    /** Friend edges: publicProfiles/{uid}/friends/{friendUid}. */
    const val FRIENDS = "friends"

    /** Shared achievement/summary feed items. */
    const val SHARES = "shares"

    /** Competitive challenges. */
    const val CHALLENGES = "challenges"
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
