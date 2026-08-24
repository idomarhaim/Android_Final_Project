package com.idomarhaim.goalpilot.feature.calendar

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.AllDay
import com.idomarhaim.goalpilot.domain.model.Block
import com.idomarhaim.goalpilot.domain.model.Deadline
import com.idomarhaim.goalpilot.domain.model.Span
import java.time.LocalDate
import org.junit.Test

/**
 * The narrow chip's time line, and the arithmetic that decides a chip is narrow.
 *
 * ## Why this test exists at all
 *
 * `Observed:` 2026-08-24, on Ido's Galaxy S25 Ultra — 384 dp wide, `3 days`, so
 * each lane is ~101 dp. `WideChip` needs **74 dp of chrome before the title gets
 * anything** (a 42 dp time column, a 4 dp spacer, a 20 dp tick, 8 dp of chip
 * padding), so the title got 125 px — **44 dp** — and *Write the project book
 * chapter* rendered as `Write` / `the p…` down three lines.
 *
 * The chip was chosen by **zoom**: anything that was not `WEEK` got `WideChip`.
 * That lumps `AGENDA`, which is one column the full width of the screen, together
 * with `THREE_DAYS`, which is a third of it. The enum was standing in for width,
 * and it is a proxy that is right on a wide phone and wrong on a narrow one.
 *
 * ## What a JVM test can and cannot hold here
 *
 * It cannot lay anything out — the choice is made inside a `BoxWithConstraints`
 * against a real measurement, and only a device or a Compose rule sees that. What
 * it *can* hold are the two halves that are ordinary values:
 *
 * 1. **The threshold is above the widths that were broken** and below the ones
 *    that were fine. That is arithmetic over `WIDE_CHIP_MIN_DP`, and it is the
 *    part that would silently rot if someone "tidied" the constant.
 * 2. **Every rung produces a legible one-line time**, which is what the narrow
 *    form replaced the 42 dp column with.
 *
 * `Untested:` that the composable actually picks the right body at a given width.
 * That is `CalendarSurfaceUiTest`'s layer and it needs a device.
 */
class CalendarChipWidthTest {

    private val monday: LocalDate = LocalDate.of(2026, 8, 24)

    // ── The decision itself, exercised rather than mirrored ─────────────────

    @Test
    fun `three days on Ido's phone is NARROW, which is the case that shipped broken`() {
        // 384 dp at 450 dpi, font scale 1.15 -- his Galaxy S25 Ultra, read off the
        // device with `dumpsys window displays` rather than assumed.
        assertWithMessage(
            "if this is not NARROW, the defect Ido reported is still shipping",
        ).that(chipFormFor(CalendarZoom.THREE_DAYS, IDO_PHONE_DP)).isEqualTo(ChipForm.NARROW)
    }

    @Test
    fun `three days is NARROW on every phone this app supports`() {
        // The zoom-based check was right on nothing and wrong on everything in this
        // range; the point of the fix is that the range is now covered rather than
        // that one phone is.
        listOf(320, 360, 384, 393, 412, 440).forEach { dp ->
            assertWithMessage("three days at $dp dp must not use the wide chip")
                .that(chipFormFor(CalendarZoom.THREE_DAYS, dp)).isEqualTo(ChipForm.NARROW)
        }
    }

    @Test
    fun `three days becomes WIDE on a tablet, which is what the enum could never do`() {
        // The behaviour the old zoom check was silently promising and could not
        // deliver. A 600 dp lane is 195 dp per day; there is room for the time
        // column and the tick, so they come back.
        assertThat(chipFormFor(CalendarZoom.THREE_DAYS, 600)).isEqualTo(ChipForm.WIDE)
        assertThat(chipFormFor(CalendarZoom.THREE_DAYS, 800)).isEqualTo(ChipForm.WIDE)
    }

    @Test
    fun `agenda is WIDE at every width, because it is one column and not a grid`() {
        // AGENDA is a LIST -- it never divides the screen, so `laneWidthDp` does not
        // apply to it. Pushing it into the narrow form would move the time off a row
        // that has ~370 dp for a title, which is a regression dressed as a fix.
        listOf(320, 384, 600).forEach { dp ->
            assertWithMessage("agenda at $dp dp is a full-width row")
                .that(chipFormFor(CalendarZoom.AGENDA, dp)).isEqualTo(ChipForm.WIDE)
        }
    }

    @Test
    fun `week is STACKED at every width, and the fix did not touch it`() {
        // 46 dp holds neither a title nor a time range on any phone, which is what
        // `StackedChip` was built for. This is the regression guard on the half that
        // was already right.
        listOf(320, 384, 600, 800).forEach { dp ->
            assertThat(chipFormFor(CalendarZoom.WEEK, dp)).isEqualTo(ChipForm.STACKED)
        }
    }

    @Test
    fun `the lane arithmetic matches what the phone actually reported`() {
        // `laneWidthDp` is DERIVED from the paddings `DayColumns` applies, so it can
        // drift from them silently. The check is the measurement: a chip on the S25
        // Ultra reported bounds spanning 285 px at 2.8125 px/dp, or ~101 dp. The
        // derived number is the lane BEFORE the chip's own padding, so it should sit
        // a little above that and nowhere near the threshold.
        val derived = laneWidthDp(CalendarZoom.THREE_DAYS, IDO_PHONE_DP)

        assertWithMessage("derived $derived dp against a measured ~101 dp chip")
            .that(derived).isIn(100..130)
        assertWithMessage("the margin below the threshold is too thin to survive another phone")
            .that(WIDE_CHIP_MIN_DP - derived).isAtLeast(25)
    }

    @Test
    fun `the threshold leaves the title a usable share, which is what it is FOR`() {
        // The wide chip's chrome is fixed and known: 42 (time column) + 4 (spacer)
        // + 20 (tick) + 8 (chip padding) = 74.
        val titleAtThreshold = WIDE_CHIP_MIN_DP - (42 + 4 + 20 + 8)

        assertWithMessage(
            "at the threshold the title gets $titleAtThreshold dp; below ~70 the wide chip is " +
                "being chosen for a lane that cannot carry it, which is the whole defect",
        ).that(titleAtThreshold).isAtLeast(70)
    }

    // ── The one-line time, per rung ──────────────────────────────────────────

    @Test
    fun `a block reads as a range`() {
        val entry = entry(Block(monday.atTime(7, 0), monday.atTime(9, 30)))

        assertThat(narrowTimeLabel(entry)).isEqualTo("07:00–09:30")
    }

    @Test
    fun `a deadline says due, so a bare time cannot be read as a start`() {
        val entry = entry(Deadline(monday.atTime(20, 0)))

        // "20:00" alone is indistinguishable from the start of a block, and the
        // 42 dp column solved that by stacking the word above the time. One line
        // has to carry it in front instead.
        assertThat(narrowTimeLabel(entry)).isEqualTo("due 20:00")
    }

    @Test
    fun `a span counts days, and one day is singular`() {
        val oneDay = entry(Span(monday, monday))
        val threeDays = entry(Span(monday, monday.plusDays(2)))

        assertThat(narrowTimeLabel(oneDay)).isEqualTo("1 day")
        // "1 days" is the defect the prototype's revision 2 fixed in both
        // languages; a new surface must not reintroduce it.
        assertThat(narrowTimeLabel(threeDays)).doesNotContain("1 days")
        assertThat(narrowTimeLabel(threeDays)).endsWith("days")
    }

    @Test
    fun `an all-day entry says so in words`() {
        val entry = entry(AllDay(monday))

        assertThat(narrowTimeLabel(entry)).isEqualTo("all-day")
    }

    @Test
    fun `every rung produces something short enough to be worth putting on one line`() {
        // The narrow form exists to give the title room. A time label that is
        // itself long would spend what it just saved.
        val all = listOf(
            entry(Block(monday.atTime(7, 0), monday.atTime(9, 30))),
            entry(Deadline(monday.atTime(20, 0))),
            entry(Span(monday, monday.plusDays(9))),
            entry(AllDay(monday)),
        )

        all.forEach { e ->
            val label = narrowTimeLabel(e)
            assertWithMessage("$label is too long for a ~74 dp line")
                .that(label.length).isAtMost(12)
        }
    }

    private fun entry(occurrence: com.idomarhaim.goalpilot.domain.model.Occurrence) = CalendarEntry(
        key = "k",
        title = "Write the project book chapter",
        kind = EntryKind.TASK,
        occurrence = occurrence,
    )

    private companion object {
        /**
         * `Observed:` 2026-08-24 on Ido's Galaxy S25 Ultra (`SM_S938B`) — 384 dp
         * wide at 450 dpi with `font_scale` 1.15, read off the device over
         * wireless debugging. A `3 days` chip there reported `bounds` spanning
         * 285 px, or ~101 dp, and the title inside it got 125 px — 44 dp.
         */
        const val IDO_PHONE_DP = 384
    }
}
