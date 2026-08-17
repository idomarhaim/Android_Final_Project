package com.idomarhaim.goalpilot.domain.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.util.Locale

/**
 * [AppLanguage] — and a deliberate record of the one fact this file may **not**
 * assert.
 *
 * Issue #51 rests on Hebrew resources living in `res/values-iw/`. The obvious
 * unit test for *why* — `assertThat(Locale.forLanguageTag("he").language)` — is
 * a **trap**, and running it is how the real mechanism was found:
 *
 * ```
 * forLanguageTag("he")  language=he      // JDK 21.0.12
 * new Locale("iw")      language=he      // every constructor agrees
 * ```
 *
 * JDK 17 flipped `java.locale.useOldISOCodes` to `false` and dropped the
 * `he→iw` mapping. The obvious next assumption — *"but Android kept it"* — was
 * then measured too, and is **also false**: API 37 returns `"he"` as well
 * (`AppLocaleInstrumentedTest`), while `values-iw/` keeps resolving correctly.
 *
 * So the bucket is decided by the **resource system**, not by [Locale], and no
 * assertion about `getLanguage()` belongs anywhere: it is true, irrelevant, and
 * actively misleading, because reading `"he"` off it is what tempts a future
 * session into renaming the directory and silently killing every Hebrew string.
 *
 * The fact is guarded where it is checkable — `HebrewLocaleResourceTest` on the
 * bucket name, `AppLocaleInstrumentedTest` on the real on-device lookup — and
 * this class stays with what is genuinely platform-independent.
 */
class AppLanguageTest {

    @Test
    fun `hebrew's language code is not load-bearing, whatever this runtime reports`() {
        // Not an assertion about which answer is right — an assertion that the
        // answer must never be load-bearing. It passes on either, because
        // AppLanguage reads neither: the resource bucket is decided by AAPT2.
        assertWithMessage(
            "Nothing in AppLanguage may branch on this — see the class KDoc. The bucket " +
                "is res/values-iw/ regardless, proved by AppLocaleInstrumentedTest.",
        ).that(Locale.forLanguageTag("he").language).isAnyOf("he", "iw")
    }

    @Test
    fun `direction is declared on the constant, not derived from the locale`() {
        // The regression this guards: re-deriving isRtl from locale.language
        // against a set of RTL codes. That expression yields "he" here and "iw"
        // on the device, so a table holding only one spelling silently returns
        // false and the app stops mirroring.
        assertThat(AppLanguage.HEBREW.isRtl).isTrue()
        assertThat(AppLanguage.ENGLISH.isRtl).isFalse()
    }

    @Test
    fun `system imposes neither a locale nor a direction`() {
        // Both are "not this setting's business" rather than a default value:
        // the platform already resolved them from the device.
        assertThat(AppLanguage.SYSTEM.locale).isNull()
        assertThat(AppLanguage.SYSTEM.isRtl).isNull()
    }

    @Test
    fun `the default follows the device`() {
        assertThat(AppLanguage.DEFAULT).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun `every id round-trips`() {
        AppLanguage.entries.forEach { assertThat(AppLanguage.fromId(it.id)).isEqualTo(it) }
    }

    @Test
    fun `an unknown or absent id falls back to the default rather than throwing`() {
        assertThat(AppLanguage.fromId(null)).isEqualTo(AppLanguage.DEFAULT)
        assertThat(AppLanguage.fromId("")).isEqualTo(AppLanguage.DEFAULT)
        assertThat(AppLanguage.fromId("klingon")).isEqualTo(AppLanguage.DEFAULT)
        // "iw" is the legacy spelling of a language we DO support, but it is not
        // an id we persist, so it must not silently resolve to HEBREW.
        assertThat(AppLanguage.fromId("iw")).isEqualTo(AppLanguage.DEFAULT)
    }

    @Test
    fun `ids are stable, because they are persisted`() {
        // A rename here silently resets every existing install to SYSTEM via the
        // tolerant fromId above, so the strings are pinned.
        assertThat(AppLanguage.entries.map { it.id })
            .containsExactly("system", "en", "he")
            .inOrder()
    }

    @Test
    fun `each language is labelled in the language it selects`() {
        // The escape hatch for a user stranded in a script they cannot read.
        assertThat(AppLanguage.HEBREW.endonym).isEqualTo("עברית")
        assertThat(AppLanguage.ENGLISH.endonym).isEqualTo("English")
    }

    // ---- The #51 deferral (Ido, 2026-08-17). See AGENTS.md § "§0.8 is suspended". ----

    @Test
    fun `hebrew is withheld from the picker but still exists`() {
        // Both halves matter and they pull opposite ways. Absent from OFFERED is
        // the freeze; present in entries is what keeps values-iw/, isRtl, locale
        // and the whole locale/ suite compiling and meaningful, so #51 resumes by
        // widening one list rather than re-implementing Hebrew.
        assertThat(AppLanguage.OFFERED).doesNotContain(AppLanguage.HEBREW)
        assertThat(AppLanguage.entries).contains(AppLanguage.HEBREW)
    }

    @Test
    fun `what the picker offers is exactly system and english`() {
        assertThat(AppLanguage.OFFERED)
            .containsExactly(AppLanguage.SYSTEM, AppLanguage.ENGLISH)
            .inOrder()
    }

    @Test
    fun `system clamps a device language the app does not offer to english`() {
        // The door a picker fix does not reach: DEFAULT is SYSTEM, so a Hebrew
        // phone would open a two-of-ten-swept UI without anyone touching a
        // setting. Spelled both ways on purpose — "he" and "iw" are the same
        // language to Locale, and a clamp that only knew one spelling would let
        // the other straight through on whichever runtime reports it.
        assertThat(AppLanguage.clampToOffered(Locale.forLanguageTag("he")).language)
            .isEqualTo("en")
        assertThat(AppLanguage.clampToOffered(Locale("iw")).language).isEqualTo("en")
        assertThat(AppLanguage.clampToOffered(Locale.forLanguageTag("fr-FR")).language)
            .isEqualTo("en")
    }

    @Test
    fun `system leaves an offered device locale alone, region and all`() {
        // §5.1 decouples Region from Language: en-GB must not be flattened to
        // bare "en", or a British device silently loses its date order and week
        // start to the clamp.
        val british = Locale.forLanguageTag("en-GB")
        assertThat(AppLanguage.clampToOffered(british)).isEqualTo(british)
        assertThat(AppLanguage.clampToOffered(british).country).isEqualTo("GB")
    }

    @Test
    fun `a hebrew preference stored before the freeze reads back as the default`() {
        // The third door: "he" is already in SharedPreferences on any device that
        // used the picker while Hebrew was offered, and fromId hands it back
        // faithfully. Only the persistence read path is clamped.
        assertThat(AppLanguage.offeredFromId("he")).isEqualTo(AppLanguage.DEFAULT)
        assertThat(AppLanguage.fromId("he")).isEqualTo(AppLanguage.HEBREW)
    }

    @Test
    fun `an offered preference still reads back unchanged`() {
        // The clamp must be invisible to everyone it does not apply to.
        assertThat(AppLanguage.offeredFromId("en")).isEqualTo(AppLanguage.ENGLISH)
        assertThat(AppLanguage.offeredFromId("system")).isEqualTo(AppLanguage.SYSTEM)
        assertThat(AppLanguage.offeredFromId(null)).isEqualTo(AppLanguage.DEFAULT)
        assertThat(AppLanguage.offeredFromId("klingon")).isEqualTo(AppLanguage.DEFAULT)
    }
}
