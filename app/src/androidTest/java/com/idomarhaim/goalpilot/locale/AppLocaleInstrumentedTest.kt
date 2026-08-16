package com.idomarhaim.goalpilot.locale

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * The half of issue #51 that **only a device can answer** — and the class that
 * corrected this ticket's own explanation of itself.
 *
 * `HebrewLocaleResourceTest` checks that the Hebrew strings are *authored* in
 * `res/values-iw/`. It cannot check that Android then **finds** them, because
 * the failure lives in resource packing and lookup. That is what this exercises.
 *
 * ### The explanation everyone gives is wrong, and this is where it died
 *
 * The reason usually given for `values-iw` — including in this ticket's first
 * draft, in `widget-pack`'s notes, and in every tutorial — is *"Java/Android
 * reports Hebrew with the legacy code `iw`, so ask for the `iw` bucket."*
 * Measured, that is **false on current Android**:
 *
 * | runtime | `Locale.forLanguageTag("he").language` |
 * |---|---|
 * | JDK 21 (unit tests) | `"he"` — JDK 17 flipped `java.locale.useOldISOCodes` to `false` |
 * | Android 17 / API 37 | `"he"` — `Observed:` on this emulator, 2026-08-16 |
 *
 * And yet the three resource tests below **pass**: `res/values-iw/` resolves
 * correctly on that same device. So the bucket is decided by the **resource
 * system** — AAPT2 stores Hebrew under the legacy qualifier and resolution maps
 * onto it — and *not* by what `Locale` reports. `getLanguage()` is a red
 * herring that happened to agree with the right answer on older Android.
 *
 * Why that distinction is worth a test rather than a comment: the folk
 * explanation is **self-defeating as it ages**. A session that checks it on a
 * modern device sees `"he"`, concludes the legacy wart is gone, renames the
 * directory to `values-he/`, and silently breaks every Hebrew string — with the
 * measurement that "proved" the rename sitting right there in its notes.
 *
 * Deliberately plain instrumentation with no Hilt and no Compose rule: the
 * subject is `Resources`, and every layer added above it is one more thing that
 * can fail for an unrelated reason.
 */
@RunWith(AndroidJUnit4::class)
class AppLocaleInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theResourceBucketDoesNotFollowWhateverLocaleReportsAsTheLanguage() {
        // `Observed:` API 37 (Android 17), 2026-08-16 — this returns "he", the
        // same as JDK 21. The widely-repeated explanation for values-iw ("Android
        // reports Hebrew as iw") is therefore FALSE on current Android, and a
        // session that checks it, finds "he", and renames the bucket to
        // values-he will break Hebrew while believing it fixed a legacy wart.
        //
        // Asserted as "either", because the point is that NOTHING may branch on
        // it: the bucket is fixed by the resource system, which the three tests
        // below exercise directly.
        assertWithMessage(
            "Whatever this is, it does not decide the bucket — see the tests below, " +
                "which resolve real strings out of res/values-iw/.",
        ).that(Locale.forLanguageTag("he").language).isAnyOf("he", "iw")
    }

    @Test
    fun aHebrewConfiguredContextResolvesTheHebrewString() {
        val hebrew = context.localizedFor(AppLanguage.HEBREW)
        val english = context.localizedFor(AppLanguage.ENGLISH)

        val translated = hebrew.getString(R.string.app_tagline)
        val original = english.getString(R.string.app_tagline)

        // The actual proof that res/values-iw/ is reachable. When the strings
        // lived in res/values-he/ this returned the English text — the exact
        // silent failure this ticket exists to remove.
        assertWithMessage(
            "The Hebrew tagline did not resolve. This is the values-he defect: the lookup " +
                "fell through to the default bucket. Check the directory is values-iw/.",
        ).that(translated).isNotEqualTo(original)
        assertThat(translated).contains("יעדי")
    }

    @Test
    fun everyHebrewStringDiffersFromItsEnglishOriginalOnDevice() {
        // The parity unit test compares authored XML; this compares what the
        // runtime actually hands back, which is the only thing a user sees.
        val hebrew = context.localizedFor(AppLanguage.HEBREW)
        val english = context.localizedFor(AppLanguage.ENGLISH)

        val untranslated = TRANSLATED_KEYS.filter { id ->
            hebrew.getString(id) == english.getString(id)
        }

        assertWithMessage(
            "These resolved to identical text on-device, so the Hebrew bucket was not " +
                "reached for them.",
        ).that(untranslated.map { context.resources.getResourceEntryName(it) }).isEmpty()
    }

    @Test
    fun hebrewLaysOutRightToLeftAndEnglishDoesNot() {
        assertThat(context.localizedConfiguration(AppLanguage.HEBREW).layoutDirection)
            .isEqualTo(android.view.View.LAYOUT_DIRECTION_RTL)
        assertThat(context.localizedConfiguration(AppLanguage.ENGLISH).layoutDirection)
            .isEqualTo(android.view.View.LAYOUT_DIRECTION_LTR)
    }

    // ------------------------------------------------------------------ helpers

    /** Mirrors what `ui/locale/AppLocale.kt` builds, so this tests the real path. */
    private fun android.content.Context.localizedConfiguration(language: AppLanguage): Configuration {
        val locale = requireNotNull(language.locale) { "SYSTEM imposes no locale" }
        return Configuration(resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    private fun android.content.Context.localizedFor(language: AppLanguage) =
        createConfigurationContext(localizedConfiguration(language)).resources

    private companion object {
        /** Every key #51 has translated so far. Grows with the literal sweep. */
        val TRANSLATED_KEYS = listOf(
            R.string.app_tagline,
            R.string.tasks_consent_missing_title,
            R.string.tasks_consent_missing_body,
            R.string.tasks_consent_grant_action,
            R.string.settings_appearance_title,
            R.string.settings_appearance_description,
            R.string.settings_language_title,
            R.string.settings_language_description,
            R.string.settings_language_system,
        )
    }
}
