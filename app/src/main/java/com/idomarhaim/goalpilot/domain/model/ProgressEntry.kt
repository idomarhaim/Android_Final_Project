package com.idomarhaim.goalpilot.domain.model

/**
 * A single manual progress log against a goal (spec §6 Core: "Lightweight
 * manual progress logging"). May carry an uploaded image (spec §6 Core: image
 * upload to Firebase Storage).
 */
data class ProgressEntry(
    val id: String = "",
    val goalId: String = "",
    val value: Double = 0.0,
    val note: String = "",
    val imageUrl: String? = null,
    val createdAtEpochMillis: Long = 0L,
)
