package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import org.junit.Test

/**
 * §4.8: *every time, date, number and range string owes direction isolation* —
 * a defect class, found independently on four screens, not a mockup artefact.
 */
class BidiTest {

    @Test
    fun `isolation wraps the run in the isolate pair`() {
        val out = "3.2 / 4 km".bidiIsolated()
        assertThat(out.first()).isEqualTo(Bidi.FSI)
        assertThat(out.last()).isEqualTo(Bidi.PDI)
        assertThat(Bidi.strip(out)).isEqualTo("3.2 / 4 km")
    }

    @Test
    fun `isolating twice does not nest`() {
        // Tile builders compose: a measure is isolated when the snapshot is built
        // and may be isolated again inside a longer row. Nesting would be
        // invisible on screen and would double the character count of every
        // string in a subsystem whose whole problem is that space is scarce.
        val once = "09:00–12:00".bidiIsolated()
        assertThat(once.bidiIsolated()).isEqualTo(once)
    }

    @Test
    fun `a composite of two isolated parts is itself isolated`() {
        // The guard used to be `first() == FSI && last() == PDI`, which is true of
        // this string — two isolates with text between them — so the composite was
        // left un-isolated while LOOKING isolated. That is §4.8's own defect
        // surviving the fix for it, and it is invisible in an English render.
        val composite = "3.2 / 4 km".bidiIsolated() + " · " + "80%".bidiIsolated()
        val isolated = composite.bidiIsolated()

        assertThat(isolated).isNotEqualTo(composite)
        assertThat(isolated.first()).isEqualTo(Bidi.FSI)
        assertThat(isolated.last()).isEqualTo(Bidi.PDI)
        assertThat(Bidi.strip(isolated)).isEqualTo("3.2 / 4 km · 80%")
    }

    @Test
    fun `an empty string is left alone`() {
        // An isolate around nothing is two invisible characters that still take a
        // line-break opportunity — which is how a blank measure pushes a title
        // onto a second line it did not need.
        assertThat("".bidiIsolated()).isEmpty()
    }

    @Test
    fun `strip is the inverse and leaves ordinary text untouched`() {
        assertThat(Bidi.strip("plain")).isEqualTo("plain")
        assertThat(Bidi.strip("לרוץ 4 ק״מ".bidiIsolated())).isEqualTo("לרוץ 4 ק״מ")
    }
}
