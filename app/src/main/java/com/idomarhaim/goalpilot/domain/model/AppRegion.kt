package com.idomarhaim.goalpilot.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Where the user is, for formatting purposes — spec §5.1's **Region**, the
 * second of the three settings that one word usually hides.
 *
 * §5.1's table, and this type is the middle row:
 *
 * | Setting | Owns | Default | Stored |
 * |---|---|---|---|
 * | Language | every word | device language | per-device ([AppLanguage]) |
 * | **Region** | **first day of week, date order** | **device country** | **per-device, decoupled from Language** |
 * | Direction | RTL/LTR | — | derived from Language, never Region |
 *
 * **Decoupled from [AppLanguage] deliberately, and that is Ido's call** — the
 * one recorded on [AppLanguage] against a proposal to pin the week to Sunday.
 * *Israelis in hi-tech often work in English yet still start the week on
 * Sunday*, so **English + Israel** must stay left-to-right with a Sunday week
 * start. Deriving one from the other makes that combination unreachable.
 *
 * ## Week start is not stored here, and that is the point
 *
 * §4.9 inherited week start from `C15` as a setting and demoted it: it is
 * **derived from Region and read out** ([firstDayOfWeek]), never stored beside
 * it. Storing it would manufacture §0.3's *second number that quietly
 * disagrees* inside the very screen built to prevent it — two fields that can
 * differ, in a store with no rule saying which wins.
 *
 * **The boundary, stated rather than assumed:** week start graduates to its own
 * control the first time something needs it to disagree with date order.
 * Nothing in v0.3 does.
 *
 * ## Only the read-out is wired, and that is #51's line, not an omission
 *
 * The app's ten process-scoped date formatters (`core/util/AppDateFormatters.kt`)
 * are `#51`'s problem and `#51` is deferred, so this setting is **stored and
 * stated**, and nothing else on any screen moves yet. [sampleDate] is what makes
 * that honest: the Settings screen shows the user what their choice *means*
 * using this type's own arithmetic, rather than asserting a change the rest of
 * the app has not made.
 */
data class AppRegion(
    /** ISO 3166-1 alpha-2, or `null` for *follow the device*. */
    val countryCode: String?,
) {

    /** The persisted id. [SYSTEM] stores the sentinel rather than an empty string. */
    val id: String get() = countryCode ?: SYSTEM_ID

    /**
     * The locale whose **region** rules apply, given what the device reports.
     *
     * Language is deliberately not part of this: see [formattingLocale], which
     * is the composition of both settings and the one the UI actually formats
     * with.
     */
    fun resolve(deviceLocale: Locale): Locale =
        countryCode?.let { Locale.Builder().setRegion(it).build() } ?: deviceLocale

    /**
     * The first day of the week this region uses — §4.9's derived read-out.
     *
     * `WeekFields.of(locale)` rather than a hand-written table of countries:
     * a table is the same shape of defect as the hand-spelled RTL lookup
     * [AppLanguage.isRtl] warns about — it is wrong for every country nobody
     * thought of, and silently.
     *
     * `Observed:` the JVM's answers for the three regions that decide this are
     * asserted in `AppRegionTest`; the on-device answer for one of them is
     * asserted in `SettingsScreenTest`, because Android resolves this through
     * ICU and the JVM through its own CLDR copy, and the two agreeing is a
     * measurement rather than a deduction.
     */
    fun firstDayOfWeek(deviceLocale: Locale): DayOfWeek =
        WeekFields.of(resolve(deviceLocale)).firstDayOfWeek

    /**
     * How a date reads here — §4.9's other half of the Region consequence line.
     *
     * Rendered from a real date rather than described in words ("day/month/year"),
     * because the ordering is only half of what changes: separators, zero-padding
     * and two-vs-four-digit years all move too, and a user recognises their own
     * date format on sight far faster than they parse a description of it.
     */
    fun sampleDate(language: AppLanguage, deviceLocale: Locale, date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(formattingLocale(language, deviceLocale))
            .format(date)

    /**
     * §5.1's two settings composed: **words from Language, order from Region.**
     *
     * This is the one place the decoupling becomes a concrete `Locale`, and it
     * is built rather than picked — taking either setting's locale whole would
     * silently import the other half of it, which is exactly the coupling §5.1
     * forbids.
     */
    fun formattingLocale(language: AppLanguage, deviceLocale: Locale): Locale {
        val languageTag = language.locale?.language ?: deviceLocale.language
        val regionTag = countryCode ?: deviceLocale.country
        val builder = Locale.Builder()
        if (languageTag.isNotBlank()) builder.setLanguage(languageTag)
        if (regionTag.isNotBlank()) builder.setRegion(regionTag)
        return builder.build()
    }

    companion object {
        private const val SYSTEM_ID = "system"

        /** Follow the device. The default, per §5.1 and §4.9's defaults table. */
        val SYSTEM: AppRegion = AppRegion(null)

        val DEFAULT: AppRegion = SYSTEM

        /**
         * Tolerant lookup for the persistence read path: anything that is not a
         * country this platform knows falls back to [DEFAULT] rather than
         * throwing. A stored code can outlive a platform's ISO table.
         */
        fun fromId(id: String?): AppRegion {
            val trimmed = id?.trim()?.uppercase(Locale.ROOT)
            if (trimmed.isNullOrEmpty() || trimmed.equals(SYSTEM_ID, ignoreCase = true)) return SYSTEM
            return if (trimmed in isoCountries) AppRegion(trimmed) else DEFAULT
        }

        /**
         * Every country the platform knows, sorted by how its name reads **in
         * the language the app is currently speaking**.
         *
         * The whole ISO list rather than a curated shortlist, because a
         * shortlist is an enumeration somebody has to be in — and the person
         * missing from it has no way to say so. Sorting is per-call because the
         * order depends on the display locale, and the app's language is a
         * setting on the very screen that shows this list.
         */
        fun offered(displayIn: Locale): List<AppRegion> =
            isoCountries
                .map { AppRegion(it) }
                .sortedBy { it.displayName(displayIn) }

        private val isoCountries: Set<String> = Locale.getISOCountries().toSet()
    }
}

/**
 * The country's name, in [displayIn]'s words. Falls back to the raw code for a
 * country the display locale has no name for, which is better than a blank row.
 */
fun AppRegion.displayName(displayIn: Locale): String {
    val code = countryCode ?: return ""
    return Locale.Builder().setRegion(code).build()
        .getDisplayCountry(displayIn)
        .ifBlank { code }
}
