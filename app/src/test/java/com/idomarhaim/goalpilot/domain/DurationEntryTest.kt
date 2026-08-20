package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.DurationEntry
import com.idomarhaim.goalpilot.domain.model.DurationSource
import com.idomarhaim.goalpilot.domain.model.TaskDuration
import org.junit.Test

/**
 * `R8`'s duration box, as a state machine (#9, spec §1.4 and §3.4).
 *
 * Every rule this ticket ships is a transition here, which is the reason the box's
 * logic is in the domain at all: §1.4 calls the precedence rule *unconditional*, and
 * a rule that can only be exercised by typing into a running app is a rule whose two
 * directions do not both get tested. The instrumented suite watches the icon; this
 * one watches the rule.
 */
class DurationEntryTest {

    // ── §1.4: a hand-typed duration is sticky, both directions ──────

    @Test
    fun `an estimate does not overwrite a typed duration`() {
        val typed = DurationEntry().typed("45")

        val after = typed.withEstimate(90)

        assertThat(after.minutes).isEqualTo(45)
        assertThat(after.source).isEqualTo(DurationSource.USER)
    }

    @Test
    fun `the other direction - an untyped box DOES take the estimate`() {
        // The half that breaks silently if stickiness is written as "never replace a
        // non-null value". Then the AI button stops working and nothing says so.
        val empty = DurationEntry()

        val after = empty.withEstimate(90)

        assertThat(after.minutes).isEqualTo(90)
        assertThat(after.source).isEqualTo(DurationSource.AI)
    }

    @Test
    fun `an AI estimate is replaced by a later AI estimate`() {
        val estimated = DurationEntry().withEstimate(90)

        assertThat(estimated.withEstimate(20).minutes).isEqualTo(20)
    }

    @Test
    fun `stickiness has no threshold - even an absurd typed value survives`() {
        // §0.6, and the rule this decision explicitly rejected: "only re-estimate if
        // the typed value is wildly off". 480 against an estimate of 5 is as wildly
        // off as the storable range allows, and it still wins.
        val typed = DurationEntry().typed("480")

        assertThat(typed.withEstimate(5).minutes).isEqualTo(480)
    }

    @Test
    fun `a typed duration survives a retitle, an AI one does not`() {
        assertThat(DurationEntry().typed("45").withRetitle().minutes).isEqualTo(45)
        assertThat(DurationEntry().withEstimate(90).withRetitle().minutes).isNull()
    }

    // ── R8: the icon ────────────────────────────────────────────────

    @Test
    fun `the icon shows for as long as the person has not entered a number`() {
        assertThat(DurationEntry().showsEstimateIcon).isTrue()
        assertThat(DurationEntry().withEstimate(90).showsEstimateIcon).isTrue()
        assertThat(DurationEntry().typed("45").showsEstimateIcon).isFalse()
    }

    @Test
    fun `clearing the box brings the icon back and makes it estimable again`() {
        val cleared = DurationEntry().typed("45").typed("")

        assertThat(cleared.showsEstimateIcon).isTrue()
        assertThat(cleared.source).isEqualTo(DurationSource.UNKNOWN)
        // And the point of that: the AI button works again rather than appearing dead.
        assertThat(cleared.withEstimate(90).minutes).isEqualTo(90)
    }

    // ── §3.4: absent is absent, never a guess ───────────────────────

    @Test
    fun `an estimate with no minutes empties the box instead of filling it`() {
        val after = DurationEntry().withEstimate(null)

        assertThat(after.minutes).isNull()
        assertThat(after.source).isEqualTo(DurationSource.UNKNOWN)
        assertThat(after.text()).isEmpty()
    }

    @Test
    fun `a skipped box stores the default, and stores that nobody supplied it`() {
        val (minutes, source) = DurationEntry().resolve()

        assertThat(minutes).isEqualTo(TaskDuration.DEFAULT_MINUTES)
        assertThat(source).isEqualTo(DurationSource.UNKNOWN)
    }

    @Test
    fun `a typed value keeps its stamp through the clamp`() {
        // Clamping is what the database can hold, not a second opinion about the day,
        // so it must not quietly demote the provenance to UNKNOWN.
        val (minutes, source) = DurationEntry().typed("9999").resolve()

        assertThat(minutes).isEqualTo(TaskDuration.MAX_MINUTES)
        assertThat(source).isEqualTo(DurationSource.USER)
    }

    @Test
    fun `an AI value keeps its stamp too`() {
        val (minutes, source) = DurationEntry().withEstimate(90).resolve()

        assertThat(minutes).isEqualTo(90)
        assertThat(source).isEqualTo(DurationSource.AI)
    }

    // ── Typing ──────────────────────────────────────────────────────

    @Test
    fun `typing zero is not an answer of none - it stores the default, as nobody's`() {
        // Found by looking at the render pass, not by an assertion: the caption read
        // "You said about 0m" while this stored 30. The box keeps the character the
        // person typed, and `resolve` is the single place that decides what is
        // written — so the caption reads THAT rather than the field.
        val zero = DurationEntry().typed("0")

        val (minutes, source) = zero.resolve()
        assertThat(minutes).isEqualTo(TaskDuration.DEFAULT_MINUTES)
        assertThat(source).isEqualTo(DurationSource.UNKNOWN)
        // …and it is still shown, so the keystroke is not silently swallowed.
        assertThat(zero.text()).isEqualTo("0")
    }

    @Test
    fun `a typed value below the floor is clamped up, and stays the user's`() {
        val (minutes, source) = DurationEntry().typed("3").resolve()

        assertThat(minutes).isEqualTo(TaskDuration.MIN_MINUTES)
        assertThat(source).isEqualTo(DurationSource.USER)
    }

    @Test
    fun `only digits reach the value`() {
        assertThat(DurationEntry().typed("4a5").minutes).isEqualTo(45)
        assertThat(DurationEntry().typed("abc").minutes).isNull()
    }

    @Test
    fun `typed text is capped so a long paste cannot overflow`() {
        assertThat(DurationEntry().typed("99999999").minutes).isEqualTo(9999)
    }

    @Test
    fun `an out-of-range estimate is clamped rather than dropped`() {
        assertThat(DurationEntry().withEstimate(5_000).minutes).isEqualTo(TaskDuration.MAX_MINUTES)
        // Zero is not a duration; it reads as "no answer", not as an answer of none.
        assertThat(DurationEntry().withEstimate(0).source).isEqualTo(DurationSource.UNKNOWN)
    }

    @Test
    fun `the box renders empty rather than zero when there is nothing to show`() {
        assertThat(DurationEntry().text()).isEmpty()
        assertThat(DurationEntry().withEstimate(90).text()).isEqualTo("90")
    }
}
