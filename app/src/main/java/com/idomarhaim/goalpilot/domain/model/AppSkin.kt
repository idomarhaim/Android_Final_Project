package com.idomarhaim.goalpilot.domain.model

/**
 * A user-selectable colour skin for the whole app.
 *
 * Pure domain on purpose: this carries **only the persisted [id]**. The Compose
 * colour schemes that render each skin live in `ui/theme/Palettes.kt` and the
 * words the picker shows live in `res/values/components_strings.xml`, resolved
 * by `ui/components/ComponentStrings.kt` — the same split [GoalCategory] uses
 * for its `iconKey`, which keeps Android/Compose types out of `domain/`.
 *
 * ⚠️ **The label and tagline used to be constructor arguments here, and that was
 * a localization defect** (issue #51, `kb/dev/untranslatable-idioms.md` §1):
 * *a language switch cannot reach a constructor argument*, so the picker went on
 * saying "Ocean blue & evergreen" on a Hebrew device while the layout around it
 * mirrored correctly. `AppSkinTest` now asserts that this enum declares no
 * instance member but [id], so the copy cannot come back the same way.
 */
enum class AppSkin(
    val id: String,
) {
    /** Default. Deep ocean blue → teal → evergreen. */
    AURORA(id = "aurora"),

    /** Warm sunset rose → coral → amber. */
    BLOSSOM(id = "blossom"),
    ;

    companion object {
        val DEFAULT: AppSkin = AURORA

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppSkin =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
