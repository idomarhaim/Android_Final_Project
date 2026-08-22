package com.idomarhaim.goalpilot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.ui.theme.VolumeArc
import com.idomarhaim.goalpilot.ui.theme.drawVolumeArcs
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** One wedge of a [DonutChart]. [fraction] is its share of the whole, 0f..1f. */
data class DonutSlice(
    val id: String,
    val label: String,
    val fraction: Float,
    val color: Color,
)

/**
 * An animated, tappable donut chart.
 *
 * Three things make it worth ~150 lines of Canvas instead of a bar list:
 *
 * - **It draws itself.** The ring sweeps clockwise out of 12 o'clock over
 *   [rememberChartProgress]'s window, so the eye reads the proportions being laid
 *   down rather than being handed a finished picture.
 * - **It answers questions.** Tapping a wedge selects it: the wedge thickens and
 *   the rest fade back, and the caller renders the detail in [center]. Tapping the
 *   hole (or the selected wedge again) clears the selection.
 * - **It has volume.** `#57` c: every wedge is a body, not a fill — a three-stop
 *   gradient lit from one direction for the whole chart, a sheen along the lit
 *   edge, a cast shadow and a grain pass. Set the relief to raised and the same
 *   wedges become solids with real side walls and end caps. None of that is
 *   decided here: it arrives in `MaterialTheme.gpMaterial.volume`, so this file
 *   still has no idea which material it is drawing in.
 * - **It stays honest.** Butt caps, not round: a rounded cap would add a couple of
 *   degrees to every wedge and quietly inflate the small ones. The inter-wedge gap
 *   is taken *out of* each wedge for the same reason, and is dropped entirely when
 *   there is only one wedge, which would otherwise show a notch in a full circle.
 * - **It says what each wedge is.** §4.1's `.tag` rule — *a category is written in
 *   words beside its dot, because dark neo collapses the six categorical hues into
 *   one ramp*. [DonutSlice.label] arrived here from the first commit and was never
 *   drawn; the legend under the chart carried the words instead, which is the
 *   failure the rule names rather than a form of it: at ten life areas the legend's
 *   last rows are off-screen from the wedge they name, so the reader is asked to
 *   hold ten colour-to-word pairs in their head — and under dark neo there are no
 *   ten colours left to hold. So every wedge that can carry its own word does, and
 *   [wedgeLabelFits] is what decides. See its KDoc for what happens to the ones
 *   that cannot.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    thickness: Dp = 34.dp,
    selectedId: String? = null,
    onSelect: (String?) -> Unit = {},
    contentDescription: String? = null,
    center: @Composable () -> Unit = {},
) {
    val sweepProgress by rememberChartProgress(key = slices)
    // One shared "is something selected" value: the wedge that owns it grows, the
    // others recede. Animating per-wedge would fight itself when selection moves.
    val selection by animateFloatAsState(
        targetValue = if (selectedId != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "donutSelection",
    )

    val density = LocalDensity.current
    val volume = MaterialTheme.gpMaterial.volume
    val trackColor = Color.Gray.copy(alpha = 0.12f)
    // `.tag`: the wedge labels. Measured rather than guessed, because whether a
    // word fits inside the band is a property of the FONT the device is rendering
    // at the size the user chose -- a hard-coded character budget is wrong on a
    // large-text device, which is the one it matters most on.
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    // Captured before the semantics lambda: inside it, `contentDescription` is the
    // SemanticsPropertyReceiver's own write-only property, not this parameter.
    val description = contentDescription

    Box(
        modifier = modifier
            .size(size)
            // `this.` is load-bearing: the composable's own `contentDescription`
            // parameter would otherwise shadow the semantics property.
            .semantics { description?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(slices, selectedId) {
                    detectTapGestures { offset ->
                        val hit = sliceAt(
                            offset = offset,
                            canvasSize = this.size.width.toFloat() to this.size.height.toFloat(),
                            ringThicknessPx = with(density) { thickness.toPx() } * SELECTED_SCALE,
                            slices = slices,
                        )
                        // Tapping the selected wedge again clears it, so the chart
                        // can always be returned to its "everything" state without
                        // hunting for the hole.
                        onSelect(if (hit == null || hit == selectedId) null else hit)
                    }
                },
        ) {
            val baseStroke = with(density) { thickness.toPx() }
            val maxStroke = baseStroke * SELECTED_SCALE
            // Inset by the *largest* stroke any wedge can reach, so a wedge does
            // not clip against the canvas edge at the moment it is selected.
            val inset = maxStroke / 2f
            val arcSize = Size(this.size.width - maxStroke, this.size.height - maxStroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = baseStroke, cap = StrokeCap.Butt),
            )

            val gap = if (slices.size > 1) GAP_DEGREES else 0f
            val drawnSoFar = 360f * sweepProgress
            var cursor = 0f
            // Read out here, not inside `buildList`: in there `this` is the list
            // builder and `this.size` is the list's length -- which compiles for
            // `size.width` only because Kotlin then hunts for an extension, and
            // that is a diagnostic worth avoiding rather than one worth reading.
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val labelMidRadius = (this.size.minDimension - baseStroke) / 2f
            val labelPaddingPx = with(density) { LABEL_PADDING.toPx() }
            // Collected in the same pass as the bodies but drawn AFTER them: the
            // volume pass ends with a grain tile over the whole ring, and a word
            // under it is a word behind a texture.
            val labels = mutableListOf<WedgeLabel>()

            // Collected first and drawn in ONE call, because two of the volume
            // layers are properties of the whole ring rather than of a wedge:
            // walls run before faces across the entire set (or a neighbour's wall
            // lands on a face), and the grain is one pass (or its tile seams
            // show). See `drawVolumeArcs`.
            val bodies = buildList {
                slices.forEach { slice ->
                    val full = slice.fraction.coerceIn(0f, 1f) * 360f
                    // Clip this wedge to however much of the circle has been drawn.
                    val visible = (minOf(cursor + full, drawnSoFar) - cursor).coerceAtLeast(0f)
                    if (visible > 0f) {
                        val isSelected = slice.id == selectedId
                        val stroke = if (isSelected) {
                            baseStroke + (maxStroke - baseStroke) * selection
                        } else {
                            baseStroke
                        }
                        add(
                            VolumeArc(
                                startAngle = -90f + cursor,
                                sweepAngle = max(
                                    visible - gap,
                                    MIN_SWEEP_DEGREES.coerceAtMost(visible),
                                ),
                                color = slice.color,
                                thickness = stroke,
                                // Selection dims the OTHERS, and it dims the whole
                                // body -- walls, caps and face together -- rather
                                // than the fill only, which would leave a receded
                                // wedge with a fully-lit rim.
                                alpha = if (selectedId == null || isSelected) {
                                    1f
                                } else {
                                    1f - DIMMED_BY * selection
                                },
                            ),
                        )
                    }

                    // Placed on the FULL wedge, and only once that wedge has
                    // finished sweeping. Positioning on the visible arc instead
                    // would make every word crawl round the ring for 900 ms and
                    // land somewhere else, which reads as a rendering fault.
                    if (visible >= full && full > 0f && slice.label.isNotBlank()) {
                        val midAngle = -90f + cursor + full / 2f
                        val room = wedgeLabelRoom(
                            sweepDegrees = full - gap,
                            midRadiusPx = labelMidRadius,
                            paddingPx = labelPaddingPx,
                        )
                        // Constrained to the arc and ellipsized, so a long name is
                        // SHORTENED rather than dropped -- see `wedgeLabelFits`.
                        // The measure is what applies the constraint; the fit test
                        // below judges whether there was enough room to be worth
                        // shortening into.
                        val measured = textMeasurer.measure(
                            text = slice.label,
                            style = labelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            constraints = Constraints(
                                maxWidth = room.toInt().coerceAtLeast(0),
                            ),
                        )
                        if (
                            wedgeLabelFits(
                                roomPx = room,
                                thicknessPx = baseStroke,
                                textHeightPx = measured.size.height.toFloat(),
                                paddingPx = labelPaddingPx,
                            )
                        ) {
                            val radians = midAngle * PI.toFloat() / 180f
                            val anchor = Offset(
                                canvasWidth / 2f + cos(radians) * labelMidRadius,
                                canvasHeight / 2f + sin(radians) * labelMidRadius,
                            )
                            labels += WedgeLabel(
                                layout = measured,
                                anchor = anchor,
                                // The tangent, so the word runs along the band --
                                // and half a turn more on the lower half, or every
                                // word from 3 round to 9 on the clock is upside
                                // down.
                                rotation = tangentRotation(midAngle),
                                // topLeft, so the measured box is centred on the
                                // anchor rather than starting at it.
                                topLeft = Offset(
                                    anchor.x - measured.size.width / 2f,
                                    anchor.y - measured.size.height / 2f,
                                ),
                                // The ink is solved against the wedge's own fill,
                                // the same solver `String.toGoalInk` uses -- so a
                                // word on a pale wedge goes dark and a word on a
                                // deep one goes light, without a second table.
                                color = slice.color.asInkOn(listOf(slice.color)),
                                alpha = if (selectedId == null || slice.id == selectedId) {
                                    1f
                                } else {
                                    1f - DIMMED_BY * selection
                                },
                            )
                        }
                    }
                    cursor += full
                }
            }
            drawVolumeArcs(
                volume = volume,
                // The WHOLE canvas, not the ring's box: one light for one scene.
                bounds = Rect(Offset.Zero, this.size),
                center = Offset(this.size.width / 2f, this.size.height / 2f),
                radius = (this.size.minDimension - maxStroke) / 2f,
                // The channel is what the track cuts, and the extrusion is spent
                // INSIDE it -- so a raised donut occupies exactly the ring a flat
                // one does and nothing can reach past the track's walls.
                channel = maxStroke,
                arcs = bodies,
            )

            labels.forEach { label ->
                rotate(degrees = label.rotation, pivot = label.anchor) {
                    drawText(
                        textLayoutResult = label.layout,
                        color = label.color,
                        topLeft = label.topLeft,
                        alpha = label.alpha,
                    )
                }
            }
        }
        center()
    }
}

/** One wedge's word, already measured, rotated and placed. */
private data class WedgeLabel(
    val layout: TextLayoutResult,
    /** The point on the band the word is centred on, and the pivot it turns about. */
    val anchor: Offset,
    val rotation: Float,
    val topLeft: Offset,
    val color: Color,
    val alpha: Float,
)

/**
 * The rotation that lays a word along the band at [midAngleDegrees], never upside
 * down.
 *
 * The tangent at a point is 90 degrees off the radius, so that is the base. The
 * flip is the part worth naming: from 3 round to 9 on the clock the tangent points
 * back up the page and a word drawn on it reads bottom-to-top. Adding half a turn
 * there costs nothing, because a rotated text box is symmetric about its own
 * centre and the centre is the pivot.
 *
 * @param midAngleDegrees canvas convention. 0 is 3 on the clock, positive
 *   clockwise, which is what `drawArc` and this file's `-90f + cursor` use.
 */
internal fun tangentRotation(midAngleDegrees: Float): Float {
    // Wrapped into (-180, 180] first. Testing `tangent > 90 && tangent < 270` on a
    // 0..360 value looks equivalent and is not: it leaves 270..360 alone, which is
    // already the right side up but reads as a three-quarter turn, and the first
    // version of this shipped that -- caught by the JVM test asserting the whole
    // ring, which is why that test walks 5 degrees at a time rather than checking
    // two representative angles.
    var tangent = (midAngleDegrees + 90f) % 360f
    if (tangent > 180f) tangent -= 360f
    if (tangent <= -180f) tangent += 360f
    return if (tangent > 90f) tangent - 180f else if (tangent < -90f) tangent + 180f else tangent
}

/**
 * How much of the band's length a word on this wedge has to live in.
 *
 * The arc at the **middle** of the band, less the breathing room it keeps from
 * its neighbours. Mid rather than outer because the inner edge is the tighter of
 * the two and the word is centred, so the mid is the honest average.
 *
 * @param sweepDegrees the wedge's drawn sweep, inter-wedge gap already removed.
 */
internal fun wedgeLabelRoom(sweepDegrees: Float, midRadiusPx: Float, paddingPx: Float): Float =
    2f * PI.toFloat() * midRadiusPx * (sweepDegrees / 360f) - paddingPx

/**
 * Whether a wedge can carry a word at all, given the [wedgeLabelRoom] it has.
 *
 * ## Why a fit test and not a leader line
 *
 * 4.1's `.tag` rule wants the word *beside the dot*, and the two ways to do that
 * on a donut are a callout in a gutter outside the ring, or the word in the band
 * itself. The gutter was rejected: it costs the chart about a third of its width
 * on a phone, which shrinks the ring the same rule is trying to make readable, and
 * at ten life areas the callouts collide and need a de-collision pass with its own
 * failure modes. The band is free.
 *
 * ## Why the word is rotated, which is why no angle reaches this function
 *
 * The first version drew the word **horizontally** and projected its box onto the
 * wedge's local radial and tangential axes. That is exact arithmetic and it
 * produced a bad chart, which the JVM test caught before the emulator did: a
 * horizontal six-letter word is about 32 dp wide and the band is 34 dp, so it fits
 * where it lies **along** the band (12 and 6 on the clock) and not where it lies
 * **across** it (3 and 9). Labels that appear only at the top and bottom of a ring
 * do not read as a rule; they read as a bug.
 *
 * Rotating the word to the tangent removes the angle from the problem. The band's
 * **thickness** then always faces the word's height and the **arc** always faces
 * its width, so the answer depends only on how big the wedge is. [DonutChart]
 * rotates by [tangentRotation], which also flips words on the lower half so none
 * is upside down.
 *
 * ## Why the word is shortened rather than dropped
 *
 * The second version drew only words that fitted **whole**, and the render pass is
 * what refused it: on ten equal wedges it labelled **six** and left Nutrition,
 * Relationships, Projects and Learning blank — the four longest names, and under
 * dark neo the four wedges nothing else tells apart. A rule that goes quiet
 * exactly where it is needed is not a rule, and no amount of padding arithmetic
 * fixes it, because the variable was never the padding: it was that a long word
 * was being treated as an unlabellable wedge.
 *
 * So the caller measures with `TextOverflow.Ellipsis` against [wedgeLabelRoom],
 * and this function judges the **room**, not the finished string. *Relation...*
 * carries identity; a blank wedge does not.
 *
 * ## What is still left unlabelled, and why that is the right answer
 *
 * A word cut below about four glyphs stops being a word, so the floor is
 * [MIN_WORD_HEIGHTS] times the type's own height — expressed in the type's
 * height rather than in dp so it follows the device's font scale instead of
 * fighting it. At the analytics donut's geometry — 220 dp across, a 34 dp band —
 * that leaves out wedges under about **18.5 degrees**, a 5% share, which
 * `CategoryTagTest` pins from both sides so this sentence cannot rot. Those keep the two paths that were already
 * there and are **not** the legend: the chart is tappable, and a tapped wedge
 * writes its full name into the hole (`DonutCenter`). So the rule degrades to
 * *the word is one tap away*, never to *the word is somewhere below*.
 *
 * Free of Compose types on purpose, exactly as [sliceAt] and
 * `StackedColumnChart.labelStride` are: a fit rule is arithmetic, and arithmetic
 * belongs in a JVM test.
 */
internal fun wedgeLabelFits(
    roomPx: Float,
    thicknessPx: Float,
    textHeightPx: Float,
    paddingPx: Float,
): Boolean {
    if (textHeightPx <= 0f) return false
    return textHeightPx + paddingPx <= thicknessPx && roomPx >= textHeightPx * MIN_WORD_HEIGHTS
}

/**
 * The shortest a shortened word may get, in multiples of its own **height**.
 *
 * Height rather than dp so the floor follows the device's font scale instead of
 * fighting it — but height is not width, and the first value here got that
 * backwards. A glyph's advance at these sizes is roughly **half** a line height,
 * so `3f` demanded room for about six glyphs and refused a 40 dp stacked column
 * outright. `2f` is four or five glyphs plus the ellipsis: enough for *Relat...*
 * to name a life area, not enough for *R...* to pretend to.
 */
private const val MIN_WORD_HEIGHTS = 2f

/**
 * Which wedge a tap landed on, or null for the hole / outside the ring.
 *
 * Kept out of the composable and free of Compose types so the geometry can be
 * reasoned about (and tested) on its own: angles run clockwise from 12 o'clock,
 * which is where the chart starts drawing.
 */
internal fun sliceAt(
    offset: Offset,
    canvasSize: Pair<Float, Float>,
    ringThicknessPx: Float,
    slices: List<DonutSlice>,
): String? {
    val (width, height) = canvasSize
    val cx = width / 2f
    val cy = height / 2f
    val outer = minOf(width, height) / 2f
    val inner = (outer - ringThicknessPx).coerceAtLeast(0f)

    val dx = offset.x - cx
    val dy = offset.y - cy
    val radius = hypot(dx, dy)
    if (radius < inner || radius > outer) return null

    // atan2 gives -180..180 measured from 3 o'clock counter-clockwise; the chart
    // measures clockwise from 12 o'clock.
    val degrees = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f

    var cursor = 0f
    slices.forEach { slice ->
        val sweep = slice.fraction.coerceIn(0f, 1f) * 360f
        if (degrees >= cursor && degrees < cursor + sweep) return slice.id
        cursor += sweep
    }
    return null
}

/** How much thicker the selected wedge gets. */
private const val SELECTED_SCALE = 1.22f

/** Alpha removed from the wedges that are not selected. */
private const val DIMMED_BY = 0.6f

/** Breathing room between wedges, in degrees. */
private const val GAP_DEGREES = 1.6f

/** A wedge narrower than the gap still gets a hairline, so it is not invisible. */
private const val MIN_SWEEP_DEGREES = 0.8f

/** Breathing room a `.tag` word keeps from the band's own edges. */
private val LABEL_PADDING = 6.dp
