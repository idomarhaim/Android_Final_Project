package com.idomarhaim.goalpilot.domain.model

import java.util.Locale

/**
 * The language the app *speaks* — one of spec §5.1's three independent settings.
 *
 * §5.1 splits what one word usually hides:
 *
 * | Setting | Owns | Default | Stored |
 * |---|---|---|---|
 * | **Language** | every word — chrome, AI text, the §8 fallback, month names | device language | **per-device, beside the skin** |
 * | **Region** | first day of week, date order | device country | decoupled from Language |
 * | **Direction** | RTL/LTR | — | **derived from Language, never Region** |
 *
 * This type is the first row only, and [isRtl] is the third derived from it.
 * **Region is deliberately absent and is not an oversight:** Ido's call, against
 * a proposal to pin the week to Sunday — *Israelis in hi-tech often work in
 * English yet still start the week on Sunday*, so English-in-Israel must stay
 * left-to-right with a Sunday week start. Deriving Region from Language would
 * make that combination unreachable.
 *
 * Stored **per-device beside the skin** rather than on the account, for the
 * reason [AppSkin] is: it must be known before the first frame, and the account
 * is not known until Auth resolves.
 *
 * Pure domain, like [AppSkin]: [Locale] is `java.util`, not Android. The Compose
 * side that turns this into a configuration lives in `ui/locale/`.
 */
enum class AppLanguage(
    val id: String,
    /**
     * The picker's label, in the language it selects — "עברית", not "Hebrew".
     *
     * An endonym is the one label that is legible to the person who needs it: a
     * user who has accidentally landed in a language they cannot read has to be
     * able to find their way out, and "Hebrew" written in Hebrew script would be
     * unreadable to exactly the person reaching for it.
     */
    val endonym: String,
    /**
     * Whether this language lays out right-to-left — §5.1's *Direction follows
     * Language, not Region*.
     *
     * **Declared, not derived from [locale], and that is deliberate.** The
     * obvious implementation — look `locale.language` up in a set of RTL codes —
     * is broken by the normalization split documented on [locale]: the same
     * expression yields `"he"` in a JVM unit test and `"iw"` on a device, so the
     * lookup table would have to carry both spellings of every language and
     * would still be one platform change away from silently returning `false`
     * and un-mirroring the app. Whether Hebrew reads right-to-left is known at
     * authorship and needs no computation.
     *
     * `null` for [SYSTEM], meaning *not this setting's business*: the platform
     * already derived a direction from the device locale, and overriding it with
     * a guess is how an unrelated device language ends up mirrored.
     */
    val isRtl: Boolean?,
) {
    /** Follow the device. The default, per §5.1. */
    SYSTEM(id = "system", endonym = "System", isRtl = null),

    ENGLISH(id = "en", endonym = "English", isRtl = false),

    HEBREW(id = "he", endonym = "עברית", isRtl = true),
    ;

    /**
     * The locale to impose, or `null` for [SYSTEM] — which imposes nothing and
     * lets the platform's own resolution stand.
     *
     * ⚠️ **Nothing may branch on `locale.language`, and the usual explanation of
     * why is wrong.**
     *
     * Hebrew's resources live in `res/values-iw/`, and the reason given
     * everywhere — *"Android reports Hebrew as `iw`, so ask for the `iw`
     * bucket"* — does not survive measurement:
     *
     * | runtime | `Locale.forLanguageTag("he").language` |
     * |---|---|
     * | JDK 21 (unit tests) | `"he"` — JDK 17 flipped `java.locale.useOldISOCodes` to `false` |
     * | Android 17 / API 37 | `"he"` — `Observed:` 2026-08-16 on the project emulator |
     *
     * Both say `"he"`, and `res/values-iw/` still resolves correctly on the
     * device (`AppLocaleInstrumentedTest`). The bucket is fixed by the
     * **resource system** — AAPT2 stores Hebrew under the legacy qualifier —
     * not by `Locale`. So this property is only ever handed to
     * `Configuration.setLocale`, and never inspected.
     *
     * The danger is specifically that the folk explanation *invites* a check
     * that now returns `"he"`, which reads as "the legacy wart is gone, rename
     * the directory" — and that rename silently kills every Hebrew string.
     * `HebrewLocaleResourceTest` fails if a `values-he/` bucket appears.
     */
    val locale: Locale? get() = if (this == SYSTEM) null else Locale.forLanguageTag(id)

    companion object {
        val DEFAULT: AppLanguage = SYSTEM

        /** Tolerant lookup: unknown/absent ids fall back to [DEFAULT] rather than throwing. */
        fun fromId(id: String?): AppLanguage =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT

        /**
         * The languages the app actually offers. **[HEBREW] is withheld while
         * `#51` is deferred** (Ido's decision, 2026-08-17 — functionality before
         * Hebrew; see AGENTS.md § *§0.8 is suspended*).
         *
         * **The [HEBREW] entry itself stays**, and deleting it is not the tidy-up
         * it looks like: `res/values-iw/`, [isRtl], [locale], `HebrewLocaleResourceTest`
         * and the whole `locale/` instrumented suite are written against it, and
         * `#51` resumes by putting one name back in this list. Withholding it
         * *here* is the entire deferral.
         *
         * There are **three** doors into a half-Hebrew app and this list closes
         * all three, which is why it is a domain value rather than a filter in
         * the picker:
         *
         * | Door | Closed by |
         * |---|---|
         * | the user taps עברית | `LanguagePicker` iterates this list |
         * | the *device* is Hebrew and the setting is [SYSTEM] | [clampToOffered] |
         * | `"he"` is already in SharedPreferences from before the freeze | [offeredFromId] |
         *
         * The second is the one a picker fix does not reach, and [DEFAULT] is
         * [SYSTEM], so it is also the door most users arrive through.
         */
        val OFFERED: List<AppLanguage> = listOf(SYSTEM, ENGLISH)

        /**
         * What [SYSTEM] should resolve `deviceLocale` to: itself when the device
         * speaks a language this app offers, [ENGLISH] otherwise.
         *
         * The device locale is returned **unchanged** when it is offered, region
         * and all — §5.1 decouples Region from Language, so `en-GB` must stay
         * `en-GB` rather than being flattened to bare `en`.
         *
         * ⚠️ **This does inspect `Locale.language`, which the [locale] KDoc warns
         * against — and it is safe for the reason that warning is really about.**
         * The hazard there is a lookup table of RTL codes written by hand, which
         * has to spell Hebrew both ways (`he` *and* `iw`) and silently returns
         * `false` when a platform changes which one it reports. Nothing is spelled
         * by hand here: both sides of the comparison come out of [Locale], so
         * whatever normalization a runtime applies, it applies to both. The only
         * literal involved is `"en"`, which no runtime has ever spelled twice.
         */
        fun clampToOffered(deviceLocale: Locale): Locale =
            if (deviceLocale.language in offeredLanguageCodes) deviceLocale else ENGLISH_LOCALE

        /**
         * [fromId] for the **persistence** read path, clamped to [OFFERED].
         *
         * A device that selected עברית before the freeze still has `"he"` in
         * SharedPreferences, and [fromId] would faithfully hand it back — so the
         * freeze would hold for every phone except the ones that had already used
         * the feature. Falls back to [DEFAULT], not [ENGLISH], because [SYSTEM] is
         * what an unset preference means and [clampToOffered] already makes it safe.
         *
         * [fromId] itself is deliberately **not** narrowed: it is the id round-trip
         * for every entry (`AppLanguageTest`), and `#51` needs it whole.
         */
        fun offeredFromId(id: String?): AppLanguage =
            fromId(id).takeIf { it in OFFERED } ?: DEFAULT

        private val ENGLISH_LOCALE: Locale = Locale.forLanguageTag(ENGLISH.id)

        /**
         * The language subtags of the concrete languages in [OFFERED] — [SYSTEM]
         * contributes none, having no locale of its own to compare against.
         */
        private val offeredLanguageCodes: Set<String> =
            OFFERED.mapNotNull { it.locale?.language }.toSet()
    }
}
