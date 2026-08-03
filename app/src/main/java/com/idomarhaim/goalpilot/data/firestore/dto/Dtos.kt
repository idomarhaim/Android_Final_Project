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
    /** Id of the user's [com.idomarhaim.goalpilot.domain.model.LifeArea]; absent on older docs. */
    var lifeAreaId: String? = null,
    var targetValue: Double = 100.0,
    var currentValue: Double = 0.0,
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
