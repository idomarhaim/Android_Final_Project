package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * [AppRegion] — spec §5.1's Region, and §4.9's *week start is not a setting*.
 *
 * ### What is actually at risk here
 *
 * Two things, and neither is the obvious one.
 *
 * 1. **The decoupling.** §5.1 splits Language from Region on Ido's explicit
 *    call — *English + Israel* must be left-to-right with a Sunday week start.
 *    That combination is unreachable the moment either setting is derived from
 *    the other, and the derivation is the tempting implementation, so it is
 *    pinned below in the one case that would catch it.
 * 2. **The week-start source.** Week start is derived, never stored, so a
 *    `WeekFields` answer that quietly changed would move a number no field
 *    records — nothing in the app would disagree with it, which is exactly
 *    §0.3's failure with the disagreement hidden.
 *
 * ⚠️ **`Observed:` on the JVM only.** These assertions read this runtime's CLDR
 * copy. Android resolves the same call through ICU, and the two agreeing is a
 * measurement rather than a deduction — `SettingsScreenTest` reads the
 * on-device answer for Israel, which is the case Ido asked for.
 */
class AppRegionTest {

    private val deviceLocale: Locale = Locale.forLanguageTag("en-GB")

    // ------------------------------------------------------------- week start

    @Test
    fun `Israel starts the week on Sunday`() {
        assertThat(AppRegion("IL").firstDayOfWeek(deviceLocale)).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `Britain and France start it on Monday`() {
        assertThat(AppRegion("GB").firstDayOfWeek(deviceLocale)).isEqualTo(DayOfWeek.MONDAY)
        assertThat(AppRegion("FR").firstDayOfWeek(deviceLocale)).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `the United States starts it on Sunday`() {
        assertThat(AppRegion("US").firstDayOfWeek(deviceLocale)).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `follow-the-device reads the device's country, not a hardcoded one`() {
        assertThat(AppRegion.SYSTEM.firstDayOfWeek(Locale.forLanguageTag("en-IL")))
            .isEqualTo(DayOfWeek.SUNDAY)
        assertThat(AppRegion.SYSTEM.firstDayOfWeek(Locale.forLanguageTag("en-GB")))
            .isEqualTo(DayOfWeek.MONDAY)
    }

    // ------------------------------------------- §5.1's decoupling, the point

    @Test
    fun `English in Israel keeps English words and Israeli week start`() {
        val region = AppRegion("IL")
        val locale = region.formattingLocale(AppLanguage.ENGLISH, deviceLocale)

        assertThat(locale.language).isEqualTo("en")
        assertThat(locale.country).isEqualTo("IL")
        assertThat(region.firstDayOfWeek(deviceLocale)).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `changing Region does not change the language, and vice versa`() {
        val english = AppLanguage.ENGLISH
        assertThat(AppRegion("IL").formattingLocale(english, deviceLocale).language)
            .isEqualTo(AppRegion("JP").formattingLocale(english, deviceLocale).language)

        // ...and the language does not reach into the region.
        assertThat(AppRegion("IL").formattingLocale(AppLanguage.HEBREW, deviceLocale).country)
            .isEqualTo("IL")
        assertThat(AppRegion("IL").formattingLocale(AppLanguage.ENGLISH, deviceLocale).country)
            .isEqualTo("IL")
    }

    @Test
    fun `SYSTEM language takes the device's language and leaves the chosen region alone`() {
        val locale = AppRegion("JP").formattingLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("en-GB"))
        assertThat(locale.language).isEqualTo("en")
        assertThat(locale.country).isEqualTo("JP")
    }

    // ------------------------------------------------------ the date read-out

    @Test
    fun `the sample date actually differs between regions`() {
        val date = LocalDate.of(2026, 8, 20)
        val british = AppRegion("GB").sampleDate(AppLanguage.ENGLISH, deviceLocale, date)
        val american = AppRegion("US").sampleDate(AppLanguage.ENGLISH, deviceLocale, date)

        // Not asserted as literal strings: the exact pattern is CLDR's to
        // change, and pinning it would fail on a data update that broke
        // nothing. What must hold is that the read-out is a function of the
        // setting -- a constant here is the defect worth catching.
        assertThat(british).isNotEqualTo(american)
        assertThat(british).isNotEmpty()
    }

    // -------------------------------------------------------- the store's I/O

    @Test
    fun `a country id round-trips through the persistence path`() {
        assertThat(AppRegion.fromId("IL")).isEqualTo(AppRegion("IL"))
        assertThat(AppRegion("IL").id).isEqualTo("IL")
    }

    @Test
    fun `ids are normalised, so a lowercase stored value is not a different region`() {
        assertThat(AppRegion.fromId("il")).isEqualTo(AppRegion("IL"))
        assertThat(AppRegion.fromId(" il ")).isEqualTo(AppRegion("IL"))
    }

    @Test
    fun `follow-the-device round-trips through its own sentinel`() {
        assertThat(AppRegion.SYSTEM.id).isEqualTo("system")
        assertThat(AppRegion.fromId(AppRegion.SYSTEM.id)).isEqualTo(AppRegion.SYSTEM)
        assertThat(AppRegion.fromId(null)).isEqualTo(AppRegion.SYSTEM)
        assertThat(AppRegion.fromId("")).isEqualTo(AppRegion.SYSTEM)
    }

    @Test
    fun `a country this platform does not know falls back rather than throwing`() {
        // A stored code can outlive a platform's ISO table -- ask for a region
        // and the answer must be a usable screen, not a crash before the frame.
        assertThat(AppRegion.fromId("ZZ")).isEqualTo(AppRegion.DEFAULT)
        assertThat(AppRegion.fromId("not-a-country")).isEqualTo(AppRegion.DEFAULT)
    }

    // ------------------------------------------------------------- the picker

    @Test
    fun `the offered list is the whole ISO table, not a shortlist`() {
        val offered = AppRegion.offered(Locale.ENGLISH)

        assertThat(offered).contains(AppRegion("IL"))
        assertThat(offered).contains(AppRegion("GB"))
        assertThat(offered).contains(AppRegion("JP"))
        // A curated list is an enumeration somebody has to be in; the person
        // missing from it has no way to say so. ~250 is the ISO table's size.
        assertThat(offered.size).isGreaterThan(200)
    }

    @Test
    fun `the offered list is sorted by the name the user actually reads`() {
        val names = AppRegion.offered(Locale.ENGLISH).map { it.displayName(Locale.ENGLISH) }
        assertThat(names).isInOrder()
    }

    @Test
    fun `a country's name follows the display locale`() {
        val germany = AppRegion("DE")
        assertThat(germany.displayName(Locale.ENGLISH)).isEqualTo("Germany")
        assertThat(germany.displayName(Locale.GERMAN)).isEqualTo("Deutschland")
    }

    @Test
    fun `follow-the-device has no name of its own to show`() {
        assertThat(AppRegion.SYSTEM.displayName(Locale.ENGLISH)).isEmpty()
    }
}
