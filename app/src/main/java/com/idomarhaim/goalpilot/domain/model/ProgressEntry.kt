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
    /**
     * Identity of the external reading this entry came from, e.g.
     * `hc:steps:2026-08-01` for a Health Connect sync. Null for a manual log.
     *
     * It exists so an importer can recognise its own earlier writes exactly. Steps
     * are cumulative — re-syncing a day already logged would silently double it —
     * and matching on [note] text is the guesswork the Google Tasks import is
     * stuck with because its rows carry no such handle.
     */
    val sourceKey: String? = null,
)
