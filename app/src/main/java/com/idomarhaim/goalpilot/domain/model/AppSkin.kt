package com.idomarhaim.goalpilot.domain.model

/**
 * A user-selectable colour skin for the whole app.
 *
 * Pure domain on purpose: this carries only the persisted [id] and the copy the
 * picker shows. The Compose colour schemes that render each skin live in
 * `ui/theme/Palettes.kt` — the same split [GoalCategory] uses for its `iconKey`,
 * which keeps Android/Compose types out of `domain/`.
 */
enum class AppSkin(
    val id: String,
    val label: String,
    val tagline: String,
) {
    /** Default. Deep ocean blue → teal → evergreen. */
    AURORA(id = "aurora", label = "Aurora", tagline = "Ocean blue & evergreen"),

    /** Warm sunset rose → coral → amber. */
    BLOSSOM(id = "blossom", label = "Blossom", tagline = "Sunset pink & orange"),
    ;

    companion object {
        val DEFAULT: AppSkin = AURORA

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppSkin =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
