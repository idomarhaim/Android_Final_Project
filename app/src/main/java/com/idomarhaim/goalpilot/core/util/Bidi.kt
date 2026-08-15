package com.idomarhaim.goalpilot.core.util

/**
 * Direction isolation for strings that mix scripts (spec §4.8).
 *
 * The Unicode bidi algorithm resolves a run's direction from the *paragraph* it
 * lands in, not from the run itself. So a Latin-digit run dropped into an RTL
 * paragraph is re-ordered: `09:00–12:00` renders as `12:00–09:00`, `3.2 / 4 km`
 * loses its slash to the wrong side, and `2 of 8` reverses. The bug is invisible
 * in an English render and appears the moment the device locale is Hebrew —
 * which `android:supportsRtl="true"` makes reachable today, before the app has
 * any Hebrew of its own.
 *
 * The fix is the Unicode isolate pair — `FSI` … `PDI` — which tells the
 * algorithm to resolve the enclosed run's direction on its own and treat the
 * whole thing as one neutral object in its parent. That is exactly what
 * `<bdi>` does on the web and what `BidiFormatter.unicodeWrap` does on Android;
 * this is the same three characters with no Android dependency, so the tile
 * builders that produce these strings stay pure and stay testable on the JVM.
 *
 * Deliberately *not* `LRM`/`RLM` marks: those nudge one boundary and leave the
 * run's interior to the algorithm, which is why the half-fixes for this class of
 * bug keep re-appearing at the next punctuation mark.
 *
 * `ui/components/BidiText.kt` (session `d2-life-area-route`, spec §4.8) is the
 * Compose-side companion to this; when it lands, this file is the string half it
 * should call rather than a second implementation.
 */
object Bidi {

    /** `U+2068 FIRST STRONG ISOLATE` — open an independently-resolved run. */
    const val FSI = '⁨'

    /** `U+2069 POP DIRECTIONAL ISOLATE` — close it. */
    const val PDI = '⁩'

    /**
     * Wraps [text] so its direction is resolved from its own content and it
     * behaves as one neutral atom inside whatever paragraph it lands in.
     *
     * Blank input is returned untouched: an isolate around nothing is two
     * invisible characters that can still take up a line-break opportunity.
     * Already-isolated input is returned untouched too, so composing two
     * builders that both isolate cannot nest the marks.
     */
    fun isolate(text: String): String =
        if (text.isEmpty() || text.isWholeIsolate()) text else "$FSI$text$PDI"

    /**
     * True when the string is *one* isolate spanning its whole length.
     *
     * Not `first() == FSI && last() == PDI`: that is also true of
     * `⁨a⁩ · ⁨b⁩`, which is **two** isolates with text between them. A
     * composite assembled from two already-isolated parts would then be left
     * un-isolated while looking isolated — which is §4.8's defect surviving the
     * fix for it. So the depth is walked, and it must reach zero only at the end.
     */
    private fun String.isWholeIsolate(): Boolean {
        if (first() != FSI) return false
        var depth = 0
        forEachIndexed { index, ch ->
            when (ch) {
                FSI -> depth++
                PDI -> depth--
            }
            if (depth == 0 && index != lastIndex) return false
        }
        return depth == 0
    }

    /** Strips every isolate mark — for tests and for logging, never for display. */
    fun strip(text: String): String = text.filterNot { it == FSI || it == PDI }
}

/** Shorthand for [Bidi.isolate]. */
fun String.bidiIsolated(): String = Bidi.isolate(this)
