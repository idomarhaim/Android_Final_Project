package com.idomarhaim.goalpilot.core.util

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * [AppDateFormatters], and the defect it exists to close.
 *
 * Spec §5.1 files it as a defect rather than a wish — *"all ten date formatters
 * are process-scoped `val`s no switch can move"* — and the tests below are
 * written against the **failure**, not the API: the interesting question is not
 * "does `of()` return a formatter" but "does a language switch actually reach
 * a formatter that was already used once".
 */
class AppDateFormattersTest {

    private lateinit var original: Locale

    @Before
    fun setUp() {
        original = Locale.getDefault()
        AppDateFormatters.clear()
    }

    @After
    fun tearDown() {
        Locale.setDefault(original)
        AppDateFormatters.clear()
    }

    @Test
    fun `a formatter follows a locale switch that happens after its first use`() {
        val date = LocalDate.of(2026, 8, 16)

        Locale.setDefault(Locale.ENGLISH)
        val english = AppDateFormatters.of("MMMM").format(date)

        Locale.setDefault(Locale.forLanguageTag("he"))
        val hebrew = AppDateFormatters.of("MMMM").format(date)

        assertWithMessage(
            "The month name did not move. This is the exact §5.1 defect: a formatter " +
                "that resolved Locale.getDefault() before the switch keeps the old locale.",
        ).that(hebrew).isNotEqualTo(english)
        assertThat(english).isEqualTo("August")
    }

    @Test
    fun `the naive implementation this replaces would fail the test above`() {
        // Kept as an executable statement of the bug, so a future "simplification"
        // back to a stored val is caught by a test that explains itself rather
        // than by a mysterious month name on a Hebrew phone.
        Locale.setDefault(Locale.ENGLISH)
        val frozen: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

        Locale.setDefault(Locale.forLanguageTag("he"))
        val stillEnglish = frozen.format(LocalDate.of(2026, 8, 16))

        assertThat(stillEnglish).isEqualTo("August")
    }

    @Test
    fun `the same pattern and locale yields the identical cached instance`() {
        Locale.setDefault(Locale.ENGLISH)
        assertThat(AppDateFormatters.of("MMM d")).isSameInstanceAs(AppDateFormatters.of("MMM d"))
    }

    @Test
    fun `two locales of one pattern are cached separately rather than overwriting`() {
        val english = AppDateFormatters.of("MMMM", Locale.ENGLISH)
        val hebrew = AppDateFormatters.of("MMMM", Locale.forLanguageTag("he"))

        assertThat(english).isNotSameInstanceAs(hebrew)
        // And the first is still the English one — a cache keyed on pattern alone
        // would have evicted it, which is the other way to reintroduce the defect.
        assertThat(english.format(LocalDate.of(2026, 8, 16))).isEqualTo("August")
    }

    @Test
    fun `an explicit locale overrides the process default`() {
        Locale.setDefault(Locale.forLanguageTag("he"))
        assertThat(AppDateFormatters.of("MMMM", Locale.ENGLISH).format(LocalDate.of(2026, 8, 16)))
            .isEqualTo("August")
    }

    @Test
    fun `DateTimeUtils formats through the switchable path`() {
        // The integration that matters: the object's own `get()` accessor, not
        // AppDateFormatters directly. A `val` reintroduced there would fail here
        // and nowhere else.
        val epochMillis = LocalDate.of(2026, 8, 16)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        Locale.setDefault(Locale.ENGLISH)
        val english = DateTimeUtils.formatDay(epochMillis)

        Locale.setDefault(Locale.forLanguageTag("he"))
        val hebrew = DateTimeUtils.formatDay(epochMillis)

        assertThat(english).contains("Aug")
        assertWithMessage(
            "DateTimeUtils.dayFormatter is frozen again — check it is `get()` and not `val`.",
        ).that(hebrew).isNotEqualTo(english)
    }
}
