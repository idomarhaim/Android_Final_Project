package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Occurrence
import com.idomarhaim.goalpilot.domain.model.Span
import com.idomarhaim.goalpilot.domain.model.WakingHours
import org.junit.Test
import java.time.LocalDate

/**
 * §4.3's **load bar and booked/free ring**, asserted as the arithmetic they are claimed to be
 * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * §4.3 calls them *"arithmetic not inference"* and that is the load-bearing claim: it is why they
 * cost nothing against §0.1's free-model rule. Arithmetic that is never checked is inference with
 * better manners, so every clause §4.3 states about them gets a case here — **spans contribute
 * nothing**, red **past 75% of waking hours**, and the denominator is **waking hours** rather than
 * the day.
 */
class DayLoadTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 17)

    /** §4.9's default: 07:00–23:00, a 16 h day, so the bar reddens at 12 h. */
    private val waking = WakingHours.DEFAULT

    private fun entry(occurrence: Occurrence, key: String = occurrence.toString()) =
        CalendarEntry(key = key, title = key, kind = EntryKind.TASK, occurrence = occurrence)

    /** `toHour = 24` means the following midnight — `LocalTime.of(24, 0)` does not exist. */
    private fun block(fromHour: Int, toHour: Int) = entry(
        Block(
            start = monday.atTime(fromHour, 0),
            end = if (toHour >= 24) monday.plusDays(1).atStartOfDay() else monday.atTime(toHour, 0),
        ),
        "block $fromHour-$toHour",
    )

    private fun loadOf(vararg entries: CalendarEntry, hours: WakingHours = waking) =
        DayLoad.of(monday, entries.toList(), hours)

    // ── The baseline ─────────────────────────────────────────────────────────────────────

    @Test
    fun `an empty day is wholly free and not overloaded`() {
        val load = loadOf()

        assertThat(load.bookedMinutes).isEqualTo(0)
        assertThat(load.freeMinutes).isEqualTo(16 * 60)
        assertThat(load.fraction).isEqualTo(0f)
        assertThat(load.isOverloaded).isFalse()
        assertThat(load.isEmpty).isTrue()
    }

    @Test
    fun `two disjoint blocks book their exact total`() {
        val load = loadOf(block(9, 11), block(14, 15))

        assertThat(load.bookedMinutes).isEqualTo(180)
        assertThat(load.freeMinutes).isEqualTo(16 * 60 - 180)
    }

    @Test
    fun `adjacent blocks touch without overlapping and book their exact total`() {
        // The half-open convention's payoff: 09:00-10:00 and 10:00-11:00 share the instant 10:00
        // and must still come to 120 minutes, not 119 and not 121.
        val load = loadOf(block(9, 10), block(10, 11))

        assertThat(load.bookedMinutes).isEqualTo(120)
    }

    // ── Overlap: the reason booked minutes are a union and not a sum ──────────────────────

    @Test
    fun `two overlapping blocks book the time once, not twice`() {
        // 09:00-11:00 and 10:00-12:00 are two commitments over three booked hours. Summing gives
        // four, which is a day reading more booked than the hours it contains.
        val load = loadOf(block(9, 11), block(10, 12))

        assertThat(load.bookedMinutes).isEqualTo(180)
    }

    @Test
    fun `a block wholly inside another adds nothing`() {
        val load = loadOf(block(9, 17), block(11, 12))

        assertThat(load.bookedMinutes).isEqualTo(8 * 60)
    }

    @Test
    fun `a day cannot book more than its waking hours however many blocks overlap`() {
        // The property the union buys, stated as a property: no arrangement of blocks can make
        // `booked` exceed the denominator, so `freeMinutes` can never be floored artificially and
        // `fraction` can never exceed 1 through double-counting alone.
        val load = loadOf(block(7, 23), block(8, 22), block(9, 21), block(10, 20))

        assertThat(load.bookedMinutes).isEqualTo(16 * 60)
        assertThat(load.freeMinutes).isEqualTo(0)
        assertThat(load.fraction).isEqualTo(1f)
    }

    // ── §2.2: "Spans contribute nothing" — and neither do the other two slotless rungs ────

    @Test
    fun `a span contributes nothing to the load`() {
        // §2.2, verbatim: "Spans contribute nothing to the time-allocation chart", or one
        // week-long renovation swamps every life area. The same sentence governs this bar.
        val load = loadOf(entry(Span(monday, monday.plusDays(6))))

        assertThat(load.bookedMinutes).isEqualTo(0)
        assertThat(load.isEmpty).isTrue()
    }

    @Test
    fun `a deadline contributes nothing to the load`() {
        // §2.4: a DEADLINE "occupies no slot and cannot collide". A moment you owe something by
        // books no hours -- what takes the hours is the block you place to do the work in.
        val load = loadOf(entry(Deadline(monday.atTime(18, 0))))

        assertThat(load.bookedMinutes).isEqualTo(0)
    }

    @Test
    fun `an all-day contributes nothing to the load`() {
        val load = loadOf(entry(AllDay(monday)))

        assertThat(load.bookedMinutes).isEqualTo(0)
    }

    @Test
    fun `a span beside a block leaves the block's own minutes standing`() {
        // The mixed case, which is the one a filter bug survives: a rung that contributes nothing
        // must not also suppress the one that does.
        val load = loadOf(block(9, 11), entry(Span(monday, monday.plusDays(3))))

        assertThat(load.bookedMinutes).isEqualTo(120)
    }

    // ── §4.3: red PAST 75% of waking hours ───────────────────────────────────────────────

    @Test
    fun `a day booked to exactly the threshold is not yet overloaded`() {
        // 12 h of a 16 h day is exactly WakingHours.loadBarRedMinutes. "Past 75%" is strict, and
        // integer truncation makes this boundary a value real days land on rather than a
        // measure-zero edge nobody reaches.
        val load = loadOf(block(7, 19))

        assertThat(load.bookedMinutes).isEqualTo(load.redAtMinutes)
        assertThat(load.isOverloaded).isFalse()
    }

    @Test
    fun `one minute past the threshold reddens the bar`() {
        val load = loadOf(entry(Block(monday.atTime(7, 0), monday.atTime(19, 1))))

        assertThat(load.bookedMinutes).isEqualTo(load.redAtMinutes + 1)
        assertThat(load.isOverloaded).isTrue()
    }

    @Test
    fun `the threshold moves when waking hours move`() {
        // The whole reason `loadBarRedMinutes` lives on WakingHours: a user who sets an 8 h day
        // reddens at 6 h, not at the 12 h the default happens to produce. Two places computing
        // "three quarters of the waking day" is one refactor away from being two numbers.
        val short = WakingHours(startMinutes = 9 * 60, endMinutes = 17 * 60)
        val load = loadOf(block(9, 16), hours = short)

        assertThat(load.redAtMinutes).isEqualTo(6 * 60)
        assertThat(load.bookedMinutes).isEqualTo(7 * 60)
        assertThat(load.isOverloaded).isTrue()
    }

    // ── The denominator is waking hours, so the numerator is clipped to them ──────────────

    @Test
    fun `a block outside waking hours books none of the waking day`() {
        // 03:00-05:00 with waking hours of 07:00-23:00. Counting it would put a numerator measured
        // over 24 h above a denominator measured over 16 -- two numbers about different windows,
        // whose ratio means nothing (0.3).
        val load = loadOf(block(3, 5))

        assertThat(load.bookedMinutes).isEqualTo(0)
        assertThat(load.freeMinutes).isEqualTo(16 * 60)
    }

    @Test
    fun `a block straddling the start of waking hours counts only its waking half`() {
        val load = loadOf(block(6, 8))

        assertThat(load.bookedMinutes).isEqualTo(60)
    }

    @Test
    fun `a block straddling the end of waking hours counts only its waking half`() {
        val load = loadOf(block(22, 24))

        assertThat(load.bookedMinutes).isEqualTo(60)
    }

    // ── Waking hours that wrap past midnight — a night shift, not an edge case ────────────

    @Test
    fun `waking hours wrapping past midnight are two intervals on one column`() {
        // WakingHours' own KDoc: "a night-shift user's waking hours are exactly that shape".
        // Read as one start..end range this span has a NEGATIVE length, which is the failure this
        // case exists to catch -- it would silently zero every load bar for such a user.
        val nightShift = WakingHours(startMinutes = 22 * 60, endMinutes = 6 * 60)

        val early = loadOf(block(2, 4), hours = nightShift)
        val late = loadOf(block(23, 24), hours = nightShift)
        val asleep = loadOf(block(12, 14), hours = nightShift)

        assertThat(nightShift.lengthMinutes).isEqualTo(8 * 60)
        assertThat(early.bookedMinutes).isEqualTo(120)
        assertThat(late.bookedMinutes).isEqualTo(60)
        assertThat(asleep.bookedMinutes).isEqualTo(0)
    }

    // ── The zero-length waking day, which the model explicitly permits ────────────────────

    @Test
    fun `a zero-length waking day reports a zero fraction rather than dividing by it`() {
        // WakingHours.lengthMinutes: a start equal to an end "reads as zero, not twenty-four
        // hours", and "a consumer dividing by it should see zero rather than a silently plausible
        // 1440". Dividing anyway yields NaN, which draws nothing and raises nothing.
        val none = WakingHours(startMinutes = 9 * 60, endMinutes = 9 * 60)
        val load = loadOf(block(9, 11), hours = none)

        assertThat(none.lengthMinutes).isEqualTo(0)
        assertThat(load.wakingMinutes).isEqualTo(0)
        assertThat(load.fraction).isEqualTo(0f)
        assertThat(load.fraction.isNaN()).isFalse()
        assertThat(load.freeMinutes).isEqualTo(0)
        assertThat(load.barFraction).isEqualTo(0f)
    }

    // ── Blocks that cross midnight belong to the columns they touch ───────────────────────

    @Test
    fun `a block running past midnight books only its own day's share`() {
        val overnight = entry(Block(monday.atTime(22, 0), monday.plusDays(1).atTime(2, 0)))

        val onMonday = DayLoad.of(monday, listOf(overnight), waking)
        val onTuesday = DayLoad.of(monday.plusDays(1), listOf(overnight), waking)

        // 22:00-23:00 is Monday's waking share; 00:00-02:00 on Tuesday is before 07:00 and books
        // nothing. Between them they never double-count the same hour on two columns.
        assertThat(onMonday.bookedMinutes).isEqualTo(60)
        assertThat(onTuesday.bookedMinutes).isEqualTo(0)
    }

    @Test
    fun `a zero-length block books nothing`() {
        // Block.closesAt coerces an inverted pair to zero length rather than throwing, so this is a
        // state that can arrive off the wire. It must book nothing rather than a negative.
        val load = loadOf(entry(Block(monday.atTime(9, 0), monday.atTime(9, 0))))

        assertThat(load.bookedMinutes).isEqualTo(0)
        assertThat(load.freeMinutes).isEqualTo(16 * 60)
    }

    @Test
    fun `an inverted block books nothing rather than a negative`() {
        val load = loadOf(entry(Block(monday.atTime(11, 0), monday.atTime(9, 0))))

        assertThat(load.bookedMinutes).isEqualTo(0)
    }

    // ── Carried-forward work books no hours of the day it is carried onto ─────────────────

    @Test
    fun `a carried-forward entry does not redden the day it lands on`() {
        // It is on today's column because its own slot is gone, not because it occupies one of
        // today's hours. Letting it book time would report work as scheduled that nobody scheduled.
        val carried = entry(Block(monday.minusDays(1).atTime(9, 0), monday.minusDays(1).atTime(17, 0)))
            .copy(carriedForward = true)

        val load = DayLoad.of(monday, listOf(carried), waking)

        assertThat(load.bookedMinutes).isEqualTo(0)
    }

    // ── The fraction, uncapped for truth and capped for drawing ───────────────────────────

    @Test
    fun `the fraction is uncapped and the bar fraction is capped`() {
        val eightHourDay = WakingHours(startMinutes = 9 * 60, endMinutes = 17 * 60)
        val load = loadOf(block(9, 17), hours = eightHourDay)

        assertThat(load.fraction).isEqualTo(1f)
        assertThat(load.barFraction).isEqualTo(1f)
        assertThat(load.freeMinutes).isEqualTo(0)
    }
}
