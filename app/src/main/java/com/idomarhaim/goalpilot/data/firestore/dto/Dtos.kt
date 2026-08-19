package com.idomarhaim.goalpilot.data.firestore.dto

import com.google.firebase.firestore.DocumentId

/**
 * Firestore data-transfer objects. They are mutable [var]s with defaults and a
 * no-arg constructor so Firestore's reflective (de)serialization works reliably.
 * [DocumentId] auto-populates the id from the document path on read and is
 * excluded from the written payload.
 */

data class GoalDto(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "OTHER",
    /**
     * **Legacy, read-only.** The single life-area id goals carried before
     * `PRODUCT_v0.3` §1.2 made the edge plural. Kept on the DTO purely so an
     * un-migrated document still resolves to `[current]` on read; every write
     * emits null here and the id in [lifeAreaIds] instead, so a document is
     * migrated the first time it is saved and the two can never disagree.
     */
    var lifeAreaId: String? = null,
    /**
     * Ids of the user's [com.idomarhaim.goalpilot.domain.model.LifeArea]s this
     * goal serves; absent on documents written before §1.2, where [lifeAreaId]
     * backfills it. Empty means unfiled.
     */
    var lifeAreaIds: List<String>? = null,
    /**
     * Automatic data source this goal belongs to, e.g. `"hc:goal:steps"`; absent on
     * older docs and on goals nobody syncs into. Additive — a document written
     * before #47 simply reads back null and gets stamped on the next sync.
     */
    var healthSourceKey: String? = null,
    var targetValue: Double = 100.0,
    // No `currentValue`. It stopped being a stored aggregate in #49 (§7.1: "stops
    // being stored — a sum over entries"), and the field is absent rather than
    // retained-and-ignored on purpose: `upsertGoal` writes with `set()`, so a
    // retained field would be re-persisted on every save and a *kept* stale number
    // is exactly the second number that quietly disagrees. Firestore ignores
    // document fields a DTO has no property for, so documents written before this
    // still deserialize; their orphaned `currentValue` is dropped the next time the
    // goal is saved and read by nobody in the meantime.
    var unit: String = "%",
    var colorHex: String = "",
    var deadline: Long? = null,
    var archived: Boolean = false,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
)

data class TaskDto(
    @DocumentId var id: String = "",
    var goalId: String? = null,
    var title: String = "",
    var points: Int = 10,
    var done: Boolean = false,
    var source: String = "MANUAL",
    var progressContribution: Double = 1.0,
    /** LLM duration estimate in minutes; absent on tasks created before it existed. */
    var estimatedMinutes: Int? = null,
    var createdAt: Long = 0L,
    var completedAt: Long? = null,
)

data class LifeAreaDto(
    @DocumentId var id: String = "",
    var name: String = "",
    var colorHex: String = "",
    var iconKey: String = "flag",
    var sortOrder: Int = 0,
    /** Google Tasks list this area mirrors, if it was synced from one. */
    var googleListId: String? = null,
    var archived: Boolean = false,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
)

data class ProgressDto(
    @DocumentId var id: String = "",
    var goalId: String = "",
    var value: Double = 0.0,
    var note: String = "",
    var imageUrl: String? = null,
    var createdAt: Long = 0L,
    /** Identity of an imported reading (e.g. `hc:steps:2026-08-01`); null if logged by hand. */
    var sourceKey: String? = null,
)

data class UserDto(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var email: String = "",
    var photoUrl: String? = null,
    var points: Long = 0L,
    var friendCode: String = "",
    var createdAt: Long = 0L,
)

data class PublicProfileDto(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var photoUrl: String? = null,
    var points: Long = 0L,
    var level: Int = 1,
    /** Mirrored from the private user doc so friends can look this profile up. */
    var friendCode: String = "",
)

data class SharedItemDto(
    @DocumentId var id: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var authorPhotoUrl: String? = null,
    var period: String = "WEEKLY",
    var headline: String = "",
    var message: String = "",
    var points: Long = 0L,
    var completedTasks: Int = 0,
    var imageUrl: String? = null,
    var createdAt: Long = 0L,
)

data class ChallengeDto(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var type: String = "CUSTOM",
    var metricUnit: String = "points",
    var ownerUid: String = "",
    var startAt: Long = 0L,
    var endAt: Long = 0L,
    var createdAt: Long = 0L,
)

/**
 * `challenges/{challengeId}/participants/{uid}` — the document id **is** the
 * participant's uid, which is what the security rule matches on. Never write
 * this to a path whose id differs from the signed-in user; the rule rejects it.
 */
data class ChallengeParticipantDto(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var photoUrl: String? = null,
    var score: Double = 0.0,
    var joinedAt: Long = 0L,
)
