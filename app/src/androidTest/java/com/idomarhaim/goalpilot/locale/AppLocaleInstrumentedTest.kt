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
        //
        // Enumerated by reflection over R.string rather than from a hand-kept
        // list, so a package swept next week is covered the day it lands and
        // nobody has to remember to extend this. `#51`'s sweep is incremental by
        // design, which makes a hand-kept list wrong by default.
        val hebrew = context.localizedFor(AppLanguage.HEBREW)
        val english = context.localizedFor(AppLanguage.ENGLISH)

        val untranslated = R.string::class.java.fields
            .mapNotNull { field ->
                val id = runCatching { field.getInt(null) }.getOrNull() ?: return@mapNotNull null
                field.name to id
            }
            .filter { (name, _) -> OWNED_PREFIXES.any { name.startsWith(it) } }
            .filterNot { (name, _) -> name in LANGUAGE_INDEPENDENT }
            .filter { (_, id) ->
                val he = runCatching { hebrew.getString(id) }.getOrNull() ?: return@filter false
                val en = runCatching { english.getString(id) }.getOrNull() ?: return@filter false
                he == en
            }
            .map { it.first }

        assertWithMessage(
            "These resolved to IDENTICAL text in Hebrew and English on-device, so either " +
                "the Hebrew bucket was not reached for them or they were never translated. " +
                "If a key is genuinely language-independent, add it to LANGUAGE_INDEPENDENT " +
                "with a reason.",
        ).that(untranslated.sorted()).isEmpty()
    }

    @Test
    fun aQuotedResourceKeepsTheWhitespaceAtItsEdges() {
        // aapt strips leading/trailing whitespace from an UNQUOTED value.
        // `Observed:` 2026-08-16 — analytics_a11y_separator was authored as `, `
        // and resolved as `,`, so TalkBack read the life-area list as
        // "לימודים 67%,בריאות" with no pause. Invisible in the XML, invisible on
        // screen (this string is only ever spoken), and caught only by dumping
        // the rendered content-description off the device.
        //
        // Asserted at the runtime rather than on the file, because the file is
        // what already looked correct: `HebrewLocaleResourceTest` guards the
        // authoring, and this guards what aapt actually produced from it.
        listOf(AppLanguage.ENGLISH, AppLanguage.HEBREW).forEach { language ->
            val separator = context.localizedFor(language)
                .getString(R.string.analytics_a11y_separator)
            assertWithMessage(
                "The separator lost its trailing space in $language — wrap the resource " +
                    "value in double quotes.",
            ).that(separator).isEqualTo(", ")
        }
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
        /**
         * Keys whose Hebrew is legitimately identical to their English, by name.
         *
         * Each needs a reason, and "we haven't translated it yet" is not one —
         * that is what this test exists to catch.
         */
        val LANGUAGE_INDEPENDENT = setOf(
            // The brand. Also `translatable="false"`, which is why the authoring
            // parity check skips it; the runtime cannot see that attribute.
            "app_name",
            // Pure format patterns and separators — no words to translate.
            // `%1$d%%` is the same string in every language by construction; the
            // authoring-side rule in HebrewLocaleResourceTest reaches the same
            // conclusion by stripping specifiers and finding no letters left.
            "analytics_percent",
            "gp_widget_percent",
            "analytics_a11y_slice",
            "analytics_a11y_bucket",
            "analytics_a11y_separator",
        )

        /**
         * Prefixes of resources this app actually owns.
         *
         * `R.string` also carries Compose, Material3 and AndroidX resources —
         * many untranslated for Hebrew upstream, none of them this app's to fix.
         * An allowlist of *ours* is the honest filter: a library adding a string
         * cannot make this test fail, and **a new key of ours cannot escape it**,
         * because every file in `res/values/` uses one of these prefixes.
         *
         * A new package's sweep adds its prefix here in the same commit.
         */
        val OWNED_PREFIXES = listOf("app_", "analytics_", "settings_", "tasks_consent_", "gp_widget_")
    }
}
