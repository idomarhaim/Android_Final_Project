package com.idomarhaim.goalpilot.domain.model

/**
 * Whether the app renders light or dark — spec §4.9's **brightness**, the third
 * control in the Appearance section beside material and colour.
 *
 * A separate axis from [AppSkin], not a third skin: the skin decides *which*
 * palette, brightness decides *which end* of it. `ui/theme/Palettes.kt` has
 * always had both ends of every skin (`colorSchemeFor(skin, darkTheme)`); until
 * #48 the only thing allowed to pick between them was
 * `isSystemInDarkTheme()`, so half of every palette was unreachable from inside
 * the app.
 *
 * Device-local, beside the skin and the language, for the reason
 * [AppSkin] is: it must be known before the first frame, and the account is
 * not known until Auth resolves. Sign-out is §4.9's test and brightness
 * survives it.
 *
 * Pure domain: [isDark] takes the system's answer as an argument rather than
 * calling Compose's `isSystemInDarkTheme()`, which keeps the decision unit
 * testable and keeps Android types out of `domain/`.
 */
enum class AppBrightness(val id: String) {

    /** Follow the device. The default, per §4.9's defaults table. */
    SYSTEM(id = "system"),

    LIGHT(id = "light"),

    DARK(id = "dark"),
    ;

    /**
     * Whether to render dark, given what the device currently says.
     *
     * [systemIsDark] is consulted **only** by [SYSTEM] — the whole point of the
     * other two is that they override it.
     */
    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        val DEFAULT: AppBrightness = SYSTEM

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppBrightness =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
