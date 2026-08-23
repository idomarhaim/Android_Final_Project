package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.OccurrenceRung
import com.idomarhaim.goalpilot.domain.model.Span
import org.junit.Test
import java.time.LocalDate

/**
 * §4.3's two presentation rules, asserted over **every** rung rather than over the ones that came
 * to mind ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)):
 *
 * - ***"A `DEADLINE` is only ever a banner in the all-day strip, never a timed box."***
 * - ***"The rung is carried by the form of the leading time column, never by a glyph on the
 *   chip"*** — the chip carries **only** the life area.
 *
 * Both are written as sweeps over [OccurrenceRung.entries], which is the shape that survives a
 * fifth rung being added: a test naming four rungs by hand passes silently on the day a fifth
 * appears with no lane at all.
 */
class CalendarLaneTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)

    private fun entry(occurrence: Occurrence, kind: EntryKind = EntryKind.TASK) =
        CalendarEntry(key = "k", title = "t", kind = kind, occurrence = occurrence)

    private fun sampleOf(rung: OccurrenceRung): Occurrence = when (rung) {
        OccurrenceRung.ALL_DAY -> AllDay(monday)
        OccurrenceRung.DEADLINE -> Deadline(monday.atTime(18, 0))
        OccurrenceRung.BLOCK -> Block(monday.atTime(9, 0), monday.atTime(11, 0))
        OccurrenceRung.SPAN -> Span(monday, monday.plusDays(3))
    }

    // ── "A DEADLINE is only ever a banner in the all-day strip, never a timed box" ────────

    @Test
    fun `a deadline is never drawn in the grid`() {
        assertThat(RungPresentation.laneOf(OccurrenceRung.DEADLINE)).isEqualTo(CalendarLane.ALL_DAY)
        assertThat(entry(Deadline(monday.atTime(18, 0))).lane).isEqualTo(CalendarLane.ALL_DAY)
    }

    @Test
    fun `the grid holds blocks and nothing else`() {
        // Stated as an exhaustive sweep rather than as one assertion about deadlines, because the
        // rule that actually protects the design is "only a block occupies a slot" (2.4) -- of
        // which "a deadline is never a box" is the consequence 4.3 chose to spell out.
        val inGrid = OccurrenceRung.entries.filter { RungPresentation.laneOf(it) == CalendarLane.GRID }

        assertThat(inGrid).containsExactly(OccurrenceRung.BLOCK)
    }

    @Test
    fun `a goal deadline is a banner too, not a box`() {
        // 4.3 draws goal deadlines as well as task deadlines. They reach the lane through the same
        // rung, so the rule cannot be true for one kind and false for the other.
        val goalDeadline = entry(Deadline(monday.atTime(23, 0)), kind = EntryKind.GOAL_DEADLINE)

        assertThat(goalDeadline.lane).isEqualTo(CalendarLane.ALL_DAY)
    }

    @Test
    fun `no kind of entry can put a deadline in the grid`() {
        val offenders = EntryKind.entries
            .map { entry(Deadline(monday.atTime(12, 0)), kind = it) }
            .filter { it.lane == CalendarLane.GRID }
            .map { it.kind.name }

        assertWithMessage(
            "4.3: a DEADLINE is only ever a banner in the all-day strip, never a timed box. " +
                "These kinds put one in the grid.",
        ).that(offenders).isEmpty()
    }

    // ── Every rung has a lane, and 4.3's untimed strip has an occupant ────────────────────

    @Test
    fun `every rung has exactly one lane`() {
        val lanes = OccurrenceRung.entries.associateWith { RungPresentation.laneOf(it) }

        assertThat(lanes).hasSize(OccurrenceRung.entries.size)
        assertThat(lanes.values).containsNoneIn(listOf<CalendarLane?>(null))
    }

    @Test
    fun `the untimed strip holds work that was given a day but never a time`() {
        // 4.3: "a strip for work due today that was never given a time -- without which the
        // calendar quietly lies about the day's real workload". That is exactly ALL_DAY, which
        // 2.2 defines as "a day with no slot".
        val untimed = OccurrenceRung.entries.filter { RungPresentation.laneOf(it) == CalendarLane.UNTIMED }

        assertThat(untimed).containsExactly(OccurrenceRung.ALL_DAY)
    }

    // ── The time column carries the rung, in form ─────────────────────────────────────────

    @Test
    fun `each rung gets its own time-column form`() {
        // The prototype's rev-2 table verbatim. It matters that these are four DISTINCT forms:
        // two rungs sharing one form is the chip-glyph failure moved into the time column, where
        // it would be no more readable.
        val forms = OccurrenceRung.entries.map(RungPresentation::timeColumnFormOf)

        assertThat(forms).containsNoDuplicates()
        assertThat(RungPresentation.timeColumnFormOf(OccurrenceRung.BLOCK)).isEqualTo(TimeColumnForm.RAIL)
        assertThat(RungPresentation.timeColumnFormOf(OccurrenceRung.DEADLINE)).isEqualTo(TimeColumnForm.POINT)
        assertThat(RungPresentation.timeColumnFormOf(OccurrenceRung.SPAN)).isEqualTo(TimeColumnForm.CAPSULE)
        assertThat(RungPresentation.timeColumnFormOf(OccurrenceRung.ALL_DAY)).isEqualTo(TimeColumnForm.WORDS)
    }

    @Test
    fun `an entry's form follows its own rung whatever lane it lands in`() {
        OccurrenceRung.entries.forEach { rung ->
            assertWithMessage("$rung")
                .that(entry(sampleOf(rung)).timeColumnForm)
                .isEqualTo(RungPresentation.timeColumnFormOf(rung))
        }
    }

    // ── The chip carries the life area and nothing else ───────────────────────────────────

    @Test
    fun `the chip carries only a colour and a name`() {
        // 0.8's surviving sub-rule -- one chip may not carry two axes -- as a structural check:
        // LifeAreaChip has no field a rung, a state or an outcome could be written into, so the
        // failure the prototype's rev 2 found cannot be reintroduced by adding a parameter at a
        // call site. It would take a change to the type, which is where it should be argued.
        //
        // `$`-prefixed fields are dropped because the Compose compiler adds a static `$stable` to
        // every class it sees. Filtering by name rather than by `isSynthetic` is deliberate:
        // `$stable` is NOT marked synthetic, so the obvious filter passes it straight through.
        val fields = LifeAreaChip::class.java.declaredFields
            .map { it.name }
            .filterNot { it.startsWith("$") }

        assertThat(fields).containsExactly("id", "name", "colorHex")
    }

    // ── Booking time is the block's property, not a per-entry decision ────────────────────

    @Test
    fun `only a block books time`() {
        val booking = OccurrenceRung.entries.filter(RungPresentation::booksTime)

        assertThat(booking).containsExactly(OccurrenceRung.BLOCK)
    }

    @Test
    fun `the lane that books time is the lane that is drawn in the grid`() {
        // The two must agree, or a load bar counts something the grid never drew (or the reverse).
        // Derived from one `when` for exactly this reason; asserted so a second `when` cannot
        // quietly appear.
        OccurrenceRung.entries.forEach { rung ->
            assertWithMessage("$rung")
                .that(RungPresentation.booksTime(rung))
                .isEqualTo(RungPresentation.laneOf(rung) == CalendarLane.GRID)
        }
    }
}
