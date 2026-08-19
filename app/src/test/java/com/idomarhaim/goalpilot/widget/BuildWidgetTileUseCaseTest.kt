package com.idomarhaim.goalpilot.widget

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.domain.model.WidgetArea
import com.idomarhaim.goalpilot.domain.model.WidgetDay
import com.idomarhaim.goalpilot.domain.model.WidgetDestination
import com.idomarhaim.goalpilot.domain.model.WidgetGoal
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetSnapshot
import com.idomarhaim.goalpilot.domain.model.WidgetTile
import com.idomarhaim.goalpilot.domain.model.WidgetTileBody
import com.idomarhaim.goalpilot.domain.usecase.BuildWidgetTileUseCase
import org.junit.Test

/**
 * §4.5's size rule, and the sentences it decides.
 *
 * Ido overturned revision 1's *a chart whose honesty depends on a footnote may
 * not be a widget* and it was re-cut as **the disclosure shrinks to the smallest
 * true sentence the tile can hold, and no size ships without one**. That rule is
 * the reason all seven cards were allowed to become tiles at all, so it is the
 * one thing here that must not be able to rot quietly.
 */
class BuildWidgetTileUseCaseTest {

    private val build = BuildWidgetTileUseCase()
    private val strings = FakeWidgetStrings()

    private val learning = WidgetArea("l", "Learning", "#8B39C4", 420, 0.62f)
    private val health = WidgetArea("h", "Health", "#CF3636", 260, 0.38f)

    private val full = WidgetSnapshot(
        capturedAtEpochMillis = 52_320_000L, // 14:32 on the fake clock
        signedIn = true,
        level = 6,
        points = 1_240L,
        levelProgress = 0.72f,
        pointsToNextLevel = 310L,
        trackedMinutes = 680,
        areas = listOf(learning, health),
        days = listOf(
            WidgetDay("Sun", listOf(90, 40)),
            WidgetDay("Mon", listOf(330, 220)),
        ),
        goals = listOf(
            WidgetGoal("g1", "Run 4 km a week", listOf("h"), "#CF3636", 80, "3.2 / 4 km"),
            WidgetGoal("g2", "Lose 5 kg", listOf("h"), "#CF3636", 50, "77.5 / 80 kg"),
        ),
        goalsWithoutMeasure = 0,
    )

    private fun all(snapshot: WidgetSnapshot = full) =
        WidgetTile.entries.flatMap { tile ->
            WidgetSize.entries.map { size -> build(snapshot, tile, size, strings) }
        }

    // ── the size rule ────────────────────────────────────────────

    @Test
    fun `every tile showing a derived or divided number carries a disclosure at every size`() {
        val offenders = all()
            .filter { it.tile.derivesNumbers && it.body !is WidgetTileBody.Message }
            .filter { it.disclosure.isBlank() }
            .map { "${it.tile.key}@${it.size.cells}" }

        assertThat(offenders).isEmpty()
    }

    @Test
    fun `the disclosure really does shrink — a smaller tile never gets a longer sentence`() {
        // The rule is not "has a footnote", it is "shrinks to the smallest true
        // sentence this size can hold". A fake that returned one constant would
        // pass the test above and prove nothing, so this is the half that bites.
        val order = listOf(WidgetSize.SMALL, WidgetSize.WIDE, WidgetSize.TALL, WidgetSize.LARGE)
        for (tile in WidgetTile.entries.filter { it.derivesNumbers }) {
            val small = build(full, tile, order.first(), strings).disclosure
            val large = build(full, tile, order.last(), strings).disclosure
            assertThat(small.length).isAtMost(large.length)
        }
    }

    @Test
    fun `goals owes a disclosure only where it hides something`() {
        // The one tile whose numbers are its own — a goal against its own target
        // is neither derived nor divided — so its footnote is conditional, and
        // the condition is "did this tile fail to draw something you have".
        assertThat(build(full, WidgetTile.GOALS, WidgetSize.SMALL, strings).disclosure).isEmpty()
        assertThat(build(full, WidgetTile.GOALS, WidgetSize.WIDE, strings).disclosure).isEmpty()

        val withUnmeasured = full.copy(goalsWithoutMeasure = 2)
        for (size in WidgetSize.entries) {
            val content = build(withUnmeasured, WidgetTile.GOALS, size, strings)
            assertThat(content.disclosure).contains("2")
        }
    }

    @Test
    fun `every tile carries an as-of stamp whenever it carries a number`() {
        // A widget is a snapshot and Android refreshes it on a schedule it is free
        // to defer, so a number with no stamp asserts a freshness nobody promised.
        val unstamped = all()
            .filter { it.body !is WidgetTileBody.Message }
            .filter { it.asOf.isBlank() }
        assertThat(unstamped).isEmpty()
    }

    @Test
    fun `the smallest size gets the shortest stamp`() {
        assertThat(build(full, WidgetTile.LEVEL, WidgetSize.SMALL, strings).asOf).isEqualTo("14:32")
        assertThat(build(full, WidgetTile.LEVEL, WidgetSize.LARGE, strings).asOf).isEqualTo("as of 14:32")
    }

    // ── empty and refusal states ─────────────────────────────────

    @Test
    fun `a never-captured snapshot says so on every tile, and discloses nothing`() {
        for (content in all(WidgetSnapshot())) {
            assertThat(content.body).isInstanceOf(WidgetTileBody.Message::class.java)
            // A footnote under an empty state is a footnote about nothing.
            assertThat(content.disclosure).isEmpty()
            assertThat(content.asOf).isEmpty()
        }
    }

    @Test
    fun `signed out is a different sentence from never opened`() {
        val signedOut = build(
            WidgetSnapshot(capturedAtEpochMillis = 1L, signedIn = false),
            WidgetTile.LEVEL,
            WidgetSize.SMALL,
            strings,
        )
        val neverOpened = build(WidgetSnapshot(), WidgetTile.LEVEL, WidgetSize.SMALL, strings)

        assertThat((signedOut.body as WidgetTileBody.Message).text)
            .isNotEqualTo((neverOpened.body as WidgetTileBody.Message).text)
    }

    @Test
    fun `having goals but none measurable is different news from having no goals`() {
        val none = full.copy(goals = emptyList(), goalsWithoutMeasure = 0)
        val unmeasured = full.copy(goals = emptyList(), goalsWithoutMeasure = 3)

        val a = build(none, WidgetTile.GOALS, WidgetSize.TALL, strings).body as WidgetTileBody.Message
        val b = build(unmeasured, WidgetTile.GOALS, WidgetSize.TALL, strings).body as WidgetTileBody.Message
        assertThat(a.text).isNotEqualTo(b.text)
        assertThat(b.text).contains("3")
    }

    @Test
    fun `a window with nothing tracked does not draw an empty donut`() {
        val bare = full.copy(trackedMinutes = 0, areas = emptyList(), days = emptyList())
        for (tile in listOf(WidgetTile.WEEK, WidgetTile.TREND, WidgetTile.EFFORT)) {
            for (size in WidgetSize.entries) {
                assertThat(build(bare, tile, size, strings).body)
                    .isInstanceOf(WidgetTileBody.Message::class.java)
            }
        }
    }

    // ── week ─────────────────────────────────────────────────────

    @Test
    fun `the smallest week tile shows the total and does not claim the split`() {
        val body = build(full, WidgetTile.WEEK, WidgetSize.SMALL, strings).body as WidgetTileBody.Donut
        assertThat(body.legend).isEmpty()
        assertThat(Bidi.strip(body.centre)).isEqualTo("11h 20m")
    }

    @Test
    fun `larger week tiles name every area they have room for`() {
        val wide = build(full, WidgetTile.WEEK, WidgetSize.WIDE, strings).body as WidgetTileBody.Donut
        assertThat(wide.legend.map { it.name }).containsExactly("Learning", "Health").inOrder()
        assertThat(wide.centreCaption).isEmpty()

        val large = build(full, WidgetTile.WEEK, WidgetSize.LARGE, strings).body as WidgetTileBody.Donut
        assertThat(large.centreCaption).isNotEmpty()
    }

    // ── trend ────────────────────────────────────────────────────

    @Test
    fun `the smallest trend tile drops its axis rather than smearing it`() {
        val small = build(full, WidgetTile.TREND, WidgetSize.SMALL, strings).body as WidgetTileBody.Columns
        assertThat(small.showLabels).isFalse()
        val wide = build(full, WidgetTile.TREND, WidgetSize.WIDE, strings).body as WidgetTileBody.Columns
        assertThat(wide.showLabels).isTrue()
    }

    @Test
    fun `the busiest day is named only where the line has room for it`() {
        assertThat(build(full, WidgetTile.TREND, WidgetSize.WIDE, strings).disclosure)
            .doesNotContain("Busiest")
        assertThat(build(full, WidgetTile.TREND, WidgetSize.LARGE, strings).disclosure)
            .contains("Busiest: Mon")
    }

    @Test
    fun `the trend stacks in the donut's own order`() {
        val body = build(full, WidgetTile.TREND, WidgetSize.LARGE, strings).body as WidgetTileBody.Columns
        assertThat(body.series.map { it.name }).containsExactly("Learning", "Health").inOrder()
    }

    // ── effort ───────────────────────────────────────────────────

    @Test
    fun `the headline says the busiest area has no measure when it has none`() {
        // §4.4's forced form: the app orders only minutes and NAMES the rest. When
        // the area that took the week has nothing measured, the honest headline is
        // that nothing here can say whether it moved.
        val body = build(full, WidgetTile.EFFORT, WidgetSize.WIDE, strings)
            .body as WidgetTileBody.EffortRows
        assertThat(body.headline).contains("Learning took most of your week")
        assertThat(body.headline).contains("no measure")
    }

    @Test
    fun `the headline names a goal instead when the busiest area has one`() {
        val snapshot = full.copy(
            goals = listOf(WidgetGoal("g", "Read 12 books", listOf("l"), "#8B39C4", 40, "5 / 12 books")),
        )
        val body = build(snapshot, WidgetTile.EFFORT, WidgetSize.WIDE, strings)
            .body as WidgetTileBody.EffortRows
        assertThat(body.headline).contains("Read 12 books")
        assertThat(body.headline).contains("40%")
        assertThat(body.headline).doesNotContain("no measure")
    }

    @Test
    fun `the smallest effort tile is the sentence alone`() {
        val body = build(full, WidgetTile.EFFORT, WidgetSize.SMALL, strings)
            .body as WidgetTileBody.EffortRows
        assertThat(body.rows).isEmpty()
        assertThat(body.headline).isNotEmpty()
    }

    @Test
    fun `effort bars are scaled against the busiest row in view`() {
        // Against the window total every bar would be a stub and the comparison
        // the row exists to make would be unreadable.
        val body = build(full, WidgetTile.EFFORT, WidgetSize.LARGE, strings)
            .body as WidgetTileBody.EffortRows
        assertThat(body.rows.first().effortFraction).isEqualTo(1f)
        assertThat(body.rows[1].effortFraction).isWithin(0.001f).of(260f / 420f)
    }

    @Test
    fun `a goal serving two areas is named under both`() {
        // §4.7: a success counts IN FULL in every area the work serves, and only
        // its minutes divide — the asymmetry is the point, not an accident. This
        // is the test that stops the plural edge quietly collapsing back to a
        // `first()` the next time something needs one colour.
        val shared = full.copy(
            goals = listOf(
                WidgetGoal("g", "Train for the 10k", listOf("h", "l"), "#CF3636", 60, ""),
            ),
        )
        val body = build(shared, WidgetTile.EFFORT, WidgetSize.LARGE, strings)
            .body as WidgetTileBody.EffortRows

        assertThat(body.rows.single { it.name == "Health" }.outcomes).hasSize(1)
        assertThat(body.rows.single { it.name == "Learning" }.outcomes).hasSize(1)
    }

    @Test
    fun `an unfiled goal belongs to no area, not to every area`() {
        val unfiled = full.copy(
            goals = listOf(WidgetGoal("g", "Read more", emptyList(), "#64748B", 20, "")),
        )
        val body = build(unfiled, WidgetTile.EFFORT, WidgetSize.LARGE, strings)
            .body as WidgetTileBody.EffortRows
        assertThat(body.rows.flatMap { it.outcomes }).isEmpty()
    }

    @Test
    fun `effort rows name their outcomes rather than ranking them`() {
        val body = build(full, WidgetTile.EFFORT, WidgetSize.LARGE, strings)
            .body as WidgetTileBody.EffortRows
        val healthRow = body.rows.single { it.name == "Health" }
        assertThat(healthRow.outcomes.map(Bidi::strip))
            .containsExactly("Run 4 km a week · 80%", "Lose 5 kg · 50%").inOrder()

        val learningRow = body.rows.single { it.name == "Learning" }
        assertThat(learningRow.outcomes).isEmpty()
    }

    // ── level ────────────────────────────────────────────────────

    @Test
    fun `the level ring defers its colour to the skin instead of picking one`() {
        // §4.1: a skin picker no material reads is a control that does nothing,
        // and it looks correct in source. This is the sentinel that keeps the
        // level ring from quietly hard-coding a hue past the picker.
        val body = build(full, WidgetTile.LEVEL, WidgetSize.SMALL, strings).body as WidgetTileBody.Ring
        assertThat(body.colorHex).isEqualTo(BuildWidgetTileUseCase.SKIN_ACCENT)
        assertThat(body.colorHex).doesNotContain("#")
    }

    @Test
    fun `the level tile grows a line at a time`() {
        fun lines(size: WidgetSize) =
            (build(full, WidgetTile.LEVEL, size, strings).body as WidgetTileBody.Ring).lines

        assertThat(lines(WidgetSize.SMALL)).hasSize(1)
        assertThat(lines(WidgetSize.WIDE)).hasSize(2)
        assertThat(lines(WidgetSize.LARGE)).hasSize(3)
        assertThat(Bidi.strip(lines(WidgetSize.LARGE).last())).contains("11h 20m")
    }

    // ── chrome ───────────────────────────────────────────────────

    @Test
    fun `every tap has somewhere to land`() {
        for (content in all()) {
            assertThat(content.destination).isNotNull()
            assertThat(content.header).isNotEmpty()
        }
        assertThat(build(full, WidgetTile.GOALS, WidgetSize.SMALL, strings).destination)
            .isEqualTo(WidgetDestination.GOALS)
        assertThat(build(full, WidgetTile.WEEK, WidgetSize.SMALL, strings).destination)
            .isEqualTo(WidgetDestination.ANALYTICS)
    }

    @Test
    fun `the smallest goals tile says Goal, not Your goals`() {
        // Showing one of four is not a concealment when the header is singular —
        // which is why that size is allowed to carry no disclosure.
        assertThat(build(full, WidgetTile.GOALS, WidgetSize.SMALL, strings).header).isEqualTo("Goal")
        assertThat(build(full, WidgetTile.GOALS, WidgetSize.WIDE, strings).header).isEqualTo("Your goals")
    }

    @Test
    fun `every string that mixes digits with words is isolated`() {
        // §4.8 is a defect class, not a mockup artefact: an un-isolated run
        // reverses inside an RTL paragraph and the bug is invisible in English.
        val large = build(full, WidgetTile.LEVEL, WidgetSize.LARGE, strings).body as WidgetTileBody.Ring
        for (line in large.lines) {
            assertThat(line.first()).isEqualTo(Bidi.FSI)
            assertThat(line.last()).isEqualTo(Bidi.PDI)
        }

        val effort = build(full, WidgetTile.EFFORT, WidgetSize.LARGE, strings)
            .body as WidgetTileBody.EffortRows
        for (row in effort.rows) {
            assertThat(row.effort.first()).isEqualTo(Bidi.FSI)
            row.outcomes.forEach { assertThat(it.first()).isEqualTo(Bidi.FSI) }
        }
    }
}
