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
}
