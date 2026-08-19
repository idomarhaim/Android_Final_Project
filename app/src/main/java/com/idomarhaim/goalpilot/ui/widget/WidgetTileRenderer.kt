package com.idomarhaim.goalpilot.ui.widget

import android.content.Context
import android.content.Intent
import android.util.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.idomarhaim.goalpilot.MainActivity
import com.idomarhaim.goalpilot.domain.model.WidgetArea
import com.idomarhaim.goalpilot.domain.model.WidgetDestination
import com.idomarhaim.goalpilot.domain.model.WidgetEffortRow
import com.idomarhaim.goalpilot.domain.model.WidgetRingRow
import com.idomarhaim.goalpilot.domain.model.WidgetSize
import com.idomarhaim.goalpilot.domain.model.WidgetTileBody
import com.idomarhaim.goalpilot.domain.model.WidgetTileContent

/**
 * Draws a finished [WidgetTileContent] as a Glance tree.
 *
 * **This file decides nothing.** Which rows survive, which sentence the
 * disclosure shrinks to, what the headline says — all of that is
 * `BuildWidgetTileUseCase`, on the other side of a seam, where it can be tested
 * without a launcher. What is left here is layout, and layout is the part a test
 * could not have checked anyway.
 *
 * Three constraints §4.5 confirmed, visible in every function below:
 *
 * - **Nothing animates.** Android renders a snapshot and refreshes on a
 *   schedule, so `rememberChartProgress` and `ui/components/ChartAnimation.kt`
 *   have no meaning here. Charts arrive at their finished value.
 * - **The launcher decides the real dp**, so every dimension is derived from
 *   [LocalSize] rather than assumed from the cell count that was requested.
 * - **A tap can only open the app at a destination** — never a dialog — so the
 *   whole tile is one click target and it carries where it goes.
 */
@Composable
fun WidgetTile(content: WidgetTileContent, palette: WidgetPalette) {
    val size = LocalSize.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.groundProvider)
            // Rounded on Android 12+, where the platform also rounds the host's
            // own frame; below 31 Glance drops it and the tile is square, which
            // is what every widget on those versions looks like anyway.
            .cornerRadius(20.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(openApp(LocalContext.current, content.destination)),
    ) {
        Header(content, palette)
        Spacer(GlanceModifier.height(6.dp))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Body(content, palette, size.width, size.height - CHROME_HEIGHT)
        }
        Footer(content, palette)
    }
}

/** Header, body padding and footer, so the body knows what it really has. */
private val CHROME_HEIGHT = 62.dp

// ── chrome ───────────────────────────────────────────────────────

@Composable
private fun Header(content: WidgetTileContent, palette: WidgetPalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // The lozenge is the tile's only ornament and it is doing work: it is the
        // per-tile accent the prototype keys each card to, and it is a shape
        // rather than a glyph because a glyph would need an icon vocabulary the
        // user has to learn — the same reason §4.3 keeps the rung off the chip.
        Box(
            modifier = GlanceModifier
                .size(width = 4.dp, height = 14.dp)
                .background(palette.accentProvider)
                .cornerRadius(2.dp),
            content = {},
        )
        Spacer(GlanceModifier.width(7.dp))
        Text(
            text = content.header,
            maxLines = 1,
            style = TextStyle(
                color = palette.onSurfaceProvider,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * The disclosure and the as-of stamp, on one line.
 *
 * They share a line because they are the same kind of claim — *here is what this
 * number is not telling you* — and because a widget that spends two lines on
 * footnotes has spent them on the wrong thing.
 */
@Composable
private fun Footer(content: WidgetTileContent, palette: WidgetPalette) {
    val parts = listOf(content.disclosure, content.asOf).filter { it.isNotBlank() }
    if (parts.isEmpty()) return
    Spacer(GlanceModifier.height(6.dp))
    Text(
        text = parts.joinToString(" · "),
        maxLines = if (content.size == WidgetSize.LARGE) 3 else 2,
        style = TextStyle(
            color = palette.onSurfaceVariantProvider,
            fontSize = 9.5f.sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}

// ── body ─────────────────────────────────────────────────────────

@Composable
private fun Body(content: WidgetTileContent, palette: WidgetPalette, width: Dp, height: Dp) {
    when (val body = content.body) {
        is WidgetTileBody.Message -> Text(
            text = body.text,
            maxLines = 3,
            style = TextStyle(
                color = palette.onSurfaceVariantProvider,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
        )


        is WidgetTileBody.Ring -> RingBody(body, palette, content.size, width, height)
        is WidgetTileBody.RingRows -> RingRowsBody(body, palette, content.size)
        is WidgetTileBody.Donut -> DonutBody(body, palette, content.size, width, height)
        is WidgetTileBody.Columns -> ColumnsBody(body, palette, width, height)
        is WidgetTileBody.EffortRows -> EffortBody(body, palette, content.size, width)
    }
}

@Composable
private fun RingBody(
    body: WidgetTileBody.Ring,
    palette: WidgetPalette,
    size: WidgetSize,
    width: Dp,
    height: Dp,
) {
    val context = LocalContext.current
    val diameter = ringDiameter(size, width, height)
    val ink = palette.chartInk()
    val bitmap = WidgetCharts.ring(
        sizePx = context.px(diameter),
        fraction = body.fraction,
        colorInt = ink.resolve(body.colorHex),
        ink = ink,
    )

    val ring: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(bitmap),
                // The number is right there in the tree as text, so repeating it
                // here would have a screen reader say it twice.
                contentDescription = null,
                modifier = GlanceModifier.size(diameter),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = body.centre,
                maxLines = 1,
                style = TextStyle(
                    color = palette.onSurfaceProvider,
                    fontSize = (diameter.value * 0.28f).sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }

    val lines: @Composable () -> Unit = {
        Column {
            body.lines.forEachIndexed { index, line ->
                Text(
                    text = line,
                    maxLines = 2,
                    style = TextStyle(
                        color = if (index == 0) palette.onSurfaceProvider else palette.onSurfaceVariantProvider,
                        fontSize = if (index == 0) 13.sp else 11.sp,
                        fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                        textAlign = if (size.isWide) TextAlign.Start else TextAlign.Center,
                    ),
                )
            }
        }
    }

    // Side by side once there is width for it, stacked otherwise — the ring is
    // the subject either way and never shrinks below legibility to make room.
    if (size == WidgetSize.WIDE || size == WidgetSize.LARGE) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ring()
            Spacer(GlanceModifier.width(12.dp))
            Box(GlanceModifier.defaultWeight()) { lines() }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ring()
            Spacer(GlanceModifier.height(8.dp))
            lines()
        }
    }
}

private fun ringDiameter(size: WidgetSize, width: Dp, height: Dp): Dp {
    val available = minOf(
        if (size.isWide) width * 0.42f else width,
        height,
    )
    return available.value.coerceIn(48f, 128f).dp
}

@Composable
private fun RingRowsBody(body: WidgetTileBody.RingRows, palette: WidgetPalette, size: WidgetSize) {
    val diameter = if (size == WidgetSize.LARGE) 42.dp else 36.dp
    // A 4×2 puts two goals side by side rather than stacking two into half the
    // height each; a 32 dp ring is a dot, and a dot is not a measure.
    if (size == WidgetSize.WIDE) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            body.rows.forEachIndexed { index, row ->
                if (index > 0) Spacer(GlanceModifier.width(10.dp))
                Box(GlanceModifier.defaultWeight()) { RingRow(row, palette, diameter) }
            }
        }
    } else {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            body.rows.forEachIndexed { index, row ->
                if (index > 0) Spacer(GlanceModifier.height(6.dp))
                RingRow(row, palette, diameter)
            }
        }
    }
}

@Composable
private fun RingRow(row: WidgetRingRow, palette: WidgetPalette, diameter: Dp) {
    val context = LocalContext.current
    val ink = palette.chartInk()
    val bitmap = WidgetCharts.ring(
        sizePx = context.px(diameter),
        fraction = row.fraction,
        colorInt = ink.resolve(row.colorHex),
        ink = ink,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.size(diameter),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = "${row.percent}",
                maxLines = 1,
                style = TextStyle(
                    color = palette.onSurfaceProvider,
                    fontSize = (diameter.value * 0.30f).sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.width(9.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = row.title,
                maxLines = 1,
                style = TextStyle(
                    color = palette.onSurfaceProvider,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            // Blank when the goal's unit is the "%" placeholder — the ring
            // already carries that number and a second copy teaches the eye to
            // read one fact as two.
            if (row.measure.isNotBlank()) {
                Text(
                    text = row.measure,
                    maxLines = 1,
                    style = TextStyle(color = palette.onSurfaceVariantProvider, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun DonutBody(
    body: WidgetTileBody.Donut,
    palette: WidgetPalette,
    size: WidgetSize,
    width: Dp,
    height: Dp,
) {
    val context = LocalContext.current
    val diameter = donutDiameter(size, width, height)
    val bitmap = WidgetCharts.donut(context.px(diameter), body.slices, palette.chartInk())

    val donut: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.size(diameter),
                contentScale = ContentScale.Fit,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = body.centre,
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.onSurfaceProvider,
                        fontSize = (diameter.value * 0.17f).coerceIn(11f, 20f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (body.centreCaption.isNotBlank()) {
                    Text(
                        text = body.centreCaption,
                        maxLines = 1,
                        style = TextStyle(color = palette.onSurfaceVariantProvider, fontSize = 9.sp),
                    )
                }
            }
        }
    }

    if (body.legend.isEmpty()) {
        donut()
        return
    }

    // §4.4: the area is written in words beside its swatch, never left to the
    // colour. Dark neo collapses six categorical hues into one ramp, so a legend
    // that is only colour is a legend that stops working on a dark home screen.
    val legend: @Composable () -> Unit = {
        Column {
            body.legend.forEachIndexed { index, area ->
                if (index > 0) Spacer(GlanceModifier.height(3.dp))
                LegendRow(area, palette)
            }
        }
    }

    if (size == WidgetSize.WIDE) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            donut()
            Spacer(GlanceModifier.width(10.dp))
            Box(GlanceModifier.defaultWeight()) { legend() }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            donut()
            Spacer(GlanceModifier.height(6.dp))
            legend()
        }
    }
}

private fun donutDiameter(size: WidgetSize, width: Dp, height: Dp): Dp {
    val available = minOf(if (size == WidgetSize.WIDE) width * 0.44f else width, height)
    return available.value.coerceIn(56f, 148f).dp
}

@Composable
private fun LegendRow(area: WidgetArea, palette: WidgetPalette) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Box(
            modifier = GlanceModifier
                .size(9.dp)
                .background(palette.providerFor(area.colorHex))
                .cornerRadius(3.dp),
            content = {},
        )
        Spacer(GlanceModifier.width(7.dp))
        Text(
            text = area.name,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = palette.onSurfaceProvider, fontSize = 11.sp),
        )
        Text(
            text = "${area.percent}%",
            maxLines = 1,
            style = TextStyle(color = palette.onSurfaceVariantProvider, fontSize = 10.sp),
        )
    }
}

@Composable
private fun ColumnsBody(body: WidgetTileBody.Columns, palette: WidgetPalette, width: Dp, height: Dp) {
    val context = LocalContext.current
    val rtl = context.isRtl()
    // §4.8's own example: *in Hebrew the week runs right to left — a drawn axis
    // does not mirror itself*. The label Row below DOES mirror, because Glance
    // hands it to RemoteViews which honours the layout direction. So the bitmap
    // is drawn in reverse instead, and the two agree. Reversing the labels
    // instead would have produced a chart that is right twice and wrong once.
    val columns = if (rtl) body.columns.reversed() else body.columns

    val labelHeight = if (body.showLabels) 14.dp else 0.dp
    val chartHeight = (height - labelHeight).value.coerceIn(28f, 160f).dp

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Image(
            provider = ImageProvider(
                WidgetCharts.columns(
                    widthPx = context.px(width),
                    heightPx = context.px(chartHeight),
                    series = body.series,
                    days = columns,
                    ink = palette.chartInk(),
                ),
            ),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxWidth().height(chartHeight),
            // FillBounds, not Fit: the bitmap is drawn at exactly this aspect and
            // Fit would letterbox it away from the labels it has to line up with.
            contentScale = ContentScale.FillBounds,
        )
        if (body.showLabels) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                body.columns.forEach { day ->
                    Text(
                        text = day.label,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = palette.onSurfaceVariantProvider,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EffortBody(
    body: WidgetTileBody.EffortRows,
    palette: WidgetPalette,
    size: WidgetSize,
    width: Dp,
) {
    // 2×2 is the headline alone. §4.4's honest sentence is the whole point of
    // this card, and a sentence is the one thing that survives at any width.
    if (body.rows.isEmpty()) {
        Text(
            text = body.headline,
            maxLines = 4,
            style = TextStyle(
                color = palette.onSurfaceProvider,
                fontSize = 12.5f.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        return
    }

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = body.headline,
            maxLines = 2,
            style = TextStyle(color = palette.onSurfaceVariantProvider, fontSize = 10.5f.sp),
        )
        Spacer(GlanceModifier.height(5.dp))
        body.rows.forEachIndexed { index, row ->
            if (index > 0) Spacer(GlanceModifier.height(5.dp))
            EffortRow(row, palette, size, width)
        }
    }
}

@Composable
private fun EffortRow(row: WidgetEffortRow, palette: WidgetPalette, size: WidgetSize, width: Dp) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.name,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = palette.onSurfaceProvider,
                    fontSize = 11.5f.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = row.effort,
                maxLines = 1,
                style = TextStyle(color = palette.onSurfaceVariantProvider, fontSize = 10.sp),
            )
        }
        Spacer(GlanceModifier.height(3.dp))
        Image(
            provider = ImageProvider(
                WidgetCharts.bar(
                    widthPx = context.px(width),
                    heightPx = context.px(6.dp),
                    fraction = row.effortFraction,
                    colorInt = palette.chartInk().resolve(row.colorHex),
                    ink = palette.chartInk(),
                ),
            ),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
            contentScale = ContentScale.FillBounds,
        )
        // The outcomes are NAMED, never ranked — §4.4's forced form. An area with
        // none prints nothing, which is the honest empty: it is not a zero.
        if (size != WidgetSize.SMALL) {
            row.outcomes.forEach {
                Text(
                    text = it,
                    maxLines = 1,
                    style = TextStyle(color = palette.onSurfaceVariantProvider, fontSize = 10.sp),
                )
            }
        }
    }
}

// ── plumbing ─────────────────────────────────────────────────────

/**
 * A tap opens the app, carrying where it wanted to go.
 *
 * `MainActivity` is named directly rather than through the nav graph: routes
 * live in `ui/navigation/Destinations.kt`, and a widget that hard-codes one
 * breaks silently on a rename — silently, because nobody re-tests a home screen
 * after a refactor. Until the activity reads the extra, every tile still opens
 * the app, which is the half of §4.5's contract that must never fail.
 */
private fun openApp(context: Context, destination: WidgetDestination) = actionStartActivity(
    Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(WidgetDestination.INTENT_EXTRA, destination.extra),
)

private fun Context.px(dp: Dp): Int = (dp.value * resources.displayMetrics.density).toInt()

private fun Context.isRtl(): Boolean =
    resources.configuration.layoutDirection == LayoutDirection.RTL
