package com.idomarhaim.goalpilot.core.util

import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Locale-aware [DateTimeFormatter]s — spec §5.1's second filed defect, fixed.
 *
 * §5.1 records it as a defect rather than a wish:
 *
 * > all ten date formatters are **process-scoped `val`s** no switch can move.
 *
 * The bug is subtle because the code reads correctly. A `private val f =
 * DateTimeFormatter.ofPattern(p, Locale.getDefault())` resolves
 * `Locale.getDefault()` **once**, when its enclosing class is initialised —
 * typically during the first frame, before the user has ever opened settings.
 * Every later switch updates the process default and every one of those
 * formatters keeps the locale it was born with, so the app's words turn Hebrew
 * and its month names stay English. Nothing about the call sites changes, which
 * is why this survives review.
 *
 * [of] re-reads the default **per call** and caches on `(pattern, locale)`, so
 * the switch lands while the cost stays a map lookup rather than a pattern
 * re-parse. The cache is unbounded on purpose: its key space is the handful of
 * literal patterns in this module times the two or three locales a device will
 * ever hold.
 *
 * ### Using it correctly
 *
 * ```kotlin
 * private val fullDay get() = AppDateFormatters.of("MMM d, yyyy")   // ✅ re-reads
 * private val fullDay =     AppDateFormatters.of("MMM d, yyyy")     // ❌ same bug
 * ```
 *
 * The `get()` is the whole fix — a `val` here re-freezes the locale one layer
 * further in and reintroduces exactly the defect this exists to remove.
 * `AppDateFormattersTest` fails on a stale formatter rather than trusting it.
 */
object AppDateFormatters {

    private val cache = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()

    /**
     * A formatter for [pattern] in [locale], defaulting to the process locale
     * **at the moment of the call** — which [com.idomarhaim.goalpilot.ui.locale.AppLocale]
     * keeps in step with the user's Language setting.
     */
    fun of(pattern: String, locale: Locale = Locale.getDefault()): DateTimeFormatter =
        cache.getOrPut(pattern to locale) { DateTimeFormatter.ofPattern(pattern, locale) }

    /** Visible for tests: the cache must not be a source of cross-test bleed. */
    internal fun clear() = cache.clear()
}
