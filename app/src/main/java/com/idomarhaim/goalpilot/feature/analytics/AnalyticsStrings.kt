package com.idomarhaim.goalpilot.feature.analytics

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.AnalyticsRange
import com.idomarhaim.goalpilot.core.util.bidiIsolated

/**
 * The presentation half of [AnalyticsRange] — issue #51's literal sweep.
 *
 * Modelled on `iconForKey`, which maps a `GoalCategory` to its icon rather than
 * storing one on the enum: the identity lives in `core/`, the words live in
 * `res/`, and neither knows about the other. Before #51 these strings were
 * constructor arguments on the enum, which made them unreachable by a language
 * switch.
 */
@Composable
@ReadOnlyComposable
fun AnalyticsRange.label(): String = stringResource(labelRes)

/**
 * The mid-sentence form — "nothing completed in this **week** yet".
 *
 * A separate resource rather than `label().lowercase()`, deliberately. Case is a
 * property of *English*, not of the string: Hebrew has none, and a locale where
 * case-mapping is not per-character (Turkish dotted/dotless i) turns the
 * transformation into a bug. The two forms are identical in Hebrew, which is the
 * clearest possible statement that the difference belongs to the translator.
 */
@Composable
@ReadOnlyComposable
fun AnalyticsRange.labelInline(): String = stringResource(labelInlineRes)

/** The unit one column of the trend chart covers, for this range. */
@Composable
@ReadOnlyComposable
fun AnalyticsRange.bucketNoun(): String = stringResource(bucketNounRes)

@get:StringRes
private val AnalyticsRange.labelRes: Int
    get() = when (this) {
        AnalyticsRange.DAY -> R.string.analytics_range_day
        AnalyticsRange.WEEK -> R.string.analytics_range_week
        AnalyticsRange.MONTH -> R.string.analytics_range_month
        AnalyticsRange.QUARTER -> R.string.analytics_range_quarter
        AnalyticsRange.YEAR -> R.string.analytics_range_year
    }

@get:StringRes
private val AnalyticsRange.labelInlineRes: Int
    get() = when (this) {
        AnalyticsRange.DAY -> R.string.analytics_range_day_inline
        AnalyticsRange.WEEK -> R.string.analytics_range_week_inline
        AnalyticsRange.MONTH -> R.string.analytics_range_month_inline
        AnalyticsRange.QUARTER -> R.string.analytics_range_quarter_inline
        AnalyticsRange.YEAR -> R.string.analytics_range_year_inline
    }

@get:StringRes
private val AnalyticsRange.bucketNounRes: Int
    get() = when (this) {
        AnalyticsRange.DAY -> R.string.analytics_bucket_day
        AnalyticsRange.WEEK -> R.string.analytics_bucket_week
        AnalyticsRange.MONTH -> R.string.analytics_bucket_month
        AnalyticsRange.QUARTER -> R.string.analytics_bucket_quarter
        AnalyticsRange.YEAR -> R.string.analytics_bucket_year
    }

/**
 * A number on its way into a sentence — **always** through here.
 *
 * §4.8: the Unicode bidi algorithm resolves a neutral run's direction from the
 * *paragraph* it lands in, so a Latin-digit run inside Hebrew prose is
 * re-ordered. `2 of 8` reverses; `09:00–12:00` becomes `12:00–09:00`. Isolating
 * at the point the number becomes text means no call site has to remember.
 *
 * Calls `core/util/Bidi.kt` rather than reimplementing it: session
 * `d2-life-area-route` wrote a second Compose-side helper and deleted it again
 * in favour of that one, and a third would be the same mistake.
 */
fun Int.isolated(): String = toString().bidiIsolated()

/** As [isolated], for text that is already formatted (a duration, a range). */
fun String.isolated(): String = bidiIsolated()

/**
 * A percentage, isolated.
 *
 * The `%` sign's placement is the translator's — it is a resource, not a Kotlin
 * `"$n%"` — and the whole thing is one isolate so the sign cannot migrate to the
 * far side of the digits in an RTL paragraph.
 */
@Composable
@ReadOnlyComposable
fun percentText(percent: Int): String =
    stringResource(R.string.analytics_percent, percent).bidiIsolated()
