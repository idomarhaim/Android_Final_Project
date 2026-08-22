package com.idomarhaim.goalpilot.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppRelief
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.toGoalAccent
import com.idomarhaim.goalpilot.ui.theme.VolumeArc
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import com.idomarhaim.goalpilot.ui.theme.drawVolumeArcs
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
import com.idomarhaim.goalpilot.ui.theme.materialSpecFor

/**
 * Spec §4.9's Appearance section, the relief — `#57` c.
 *
 * ## Why a picker with previews and not a switch
 *
 * The same argument `BackgroundPicker` makes, and it is stronger here: a body is
 * judged by **looking at it**. A `Switch` labelled *"3D charts"* asks the user to
 * imagine the answer; two tiles each carrying a real donut, drawn by the real
 * [drawVolumeArcs] in the real current material, hand it to them.
 *
 * ## The tiles draw the CURRENT material, on the CURRENT ground
 *
 * Relief is what varies here, so everything else is held fixed — that is what
 * makes a row of tiles a comparison rather than a gallery. It also puts the one
 * fact the overturned decision was about directly in front of the user: pick
 * Glass, and the raised tile visibly does something, which is the claim *"it is
 * a no-op on glass and liquid"* denied on screen rather than in a comment.
 *
 * ## Plain English literals, deliberately
 *
 * `feature/settings` is **unswept** — absent from
 * `AnalyticsLiteralSweepTest.SWEPT_PACKAGES` — and AGENTS.md §0.8's suspension
 * permits plain English literals there while `#51` is deferred. Its siblings in
 * the same card are already written this way; see `BackgroundPicker`'s note.
 */
@Composable
fun ReliefPicker(
    selected: AppRelief,
    material: AppMaterial,
    background: AppBackground,
    skin: AppSkin,
    brightnessIsDark: Boolean,
    onSelect: (AppRelief) -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewDark = material.resolveDark(brightnessIsDark)
    val previewScheme = colorSchemeFor(skin, material, brightnessIsDark)
    val current = MaterialTheme.gpMaterial
    // `#57` a's authored category set, resolved exactly the way the analytics
    // donut resolves it -- so a tile cannot show a palette the real chart does
    // not have. `toGoalAccent` is @ReadOnlyComposable, so it is read here and
    // never inside the DrawScope below.
    val hues = GoalCategory.entries.take(PREVIEW_SLICES).map { it.defaultColorHex.toGoalAccent() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppRelief.entries.forEach { option ->
            val optionSpec = materialSpecFor(
                material = material,
                background = background,
                scheme = previewScheme,
                dark = previewDark,
                relief = option,
            )
            val isSelected = option == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .testTag(reliefTileTag(option)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 92 rather than the background tiles' 62, and that is
                        // not decoration. The thing being compared is a HEIGHT of
                        // about a third of the ring's channel, so the whole
                        // difference between the two tiles scales with the donut
                        // -- at 78 dp the extrusion came out around 5 dp and the
                        // two tiles read as the same picture. `Observed:` the
                        // first settings render pass, 2026-08-22.
                        .height(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .gpPage(optionSpec, previewScheme.background)
                        .then(
                            if (isSelected) {
                                // The current material's accent, not the tile's --
                                // the ring speaks to the screen around it, exactly
                                // as the material and background tiles' rings do.
                                Modifier.border(
                                    BorderStroke(2.dp, current.accent),
                                    RoundedCornerShape(16.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(10.dp),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val bounds = Rect(Offset.Zero, size)
                        // 0.32 + 0.32 puts the outer edge at 0.48 of the
                        // shorter side, so the ring fills the tile with a hair to
                        // spare. The channel is as wide as the radius on purpose:
                        // a thin ring has nothing to extrude INTO, and the height
                        // is a fraction of the channel.
                        val radius = size.minDimension * 0.32f
                        val channel = size.minDimension * 0.32f
                        val centre = Offset(size.width / 2f, size.height / 2f)
                        // The track, so the tile reads as a donut rather than as a
                        // broken ring -- the same ghost the real chart draws under
                        // its wedges.
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.12f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(centre.x - radius, centre.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                            style = Stroke(width = channel * 0.66f, cap = StrokeCap.Butt),
                        )
                        val sweep = 360f / PREVIEW_SLICES
                        drawVolumeArcs(
                            volume = optionSpec.volume,
                            bounds = bounds,
                            center = centre,
                            radius = radius,
                            channel = channel,
                            arcs = hues.mapIndexed { index, hue ->
                                VolumeArc(
                                    startAngle = -90f + index * sweep,
                                    sweepAngle = sweep - PREVIEW_GAP_DEGREES,
                                    color = hue,
                                    thickness = channel * 0.66f,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = reliefLabel(option),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Enough wedges to show walls and end caps; few enough to stay legible at 78 dp. */
private const val PREVIEW_SLICES = 4

/** The tile's own gap. Wider than the chart's, because the tile is much smaller. */
private const val PREVIEW_GAP_DEGREES = 5f

/**
 * What this relief is, in the app's own words.
 *
 * *"Flat"* rather than *"Off"*: the flat body still carries volume — the
 * three-stop fill, the sheen, the cast and the grain — so *"Off"* would promise
 * a plainness the app does not have and never had. [AppRelief.FLAT]'s own doc
 * makes the same point one layer down.
 */
internal fun reliefLabel(relief: AppRelief): String = when (relief) {
    AppRelief.FLAT -> "Flat"
    AppRelief.RAISED -> "Raised 3D"
}

/**
 * What the choice costs, live — §4.9's consequence-line pattern, the same one
 * the brightness lock and the background use.
 *
 * Written against the **material** as well as the relief, because the one thing
 * a user is owed here is the sentence the overturned decision was about: raised
 * glass is not a no-op, and it is also not the same object glass otherwise is.
 * Saying so before it is picked is the whole point of a consequence line.
 */
internal fun reliefConsequence(relief: AppRelief, material: AppMaterial): String {
    val soft = material == AppMaterial.NEO || material == AppMaterial.DARK_NEO
    return when {
        relief == AppRelief.FLAT ->
            "Chart bodies are painted: a graded fill, a sheen along the lit edge " +
                "and a shadow underneath. They still have volume — they just have no height."

        soft ->
            "Wedges and bars become solids with real side walls, lit from the top left. " +
                "This is the shape soft surfaces were designed around, so it costs nothing here."

        else ->
            "Wedges and bars become solids with real side walls, lit from the top left. " +
                "On a glass material that is a solid object sitting on a translucent panel, " +
                "which is a deliberate combination rather than the material's own look."
    }
}

/**
 * Stable per-relief tag for instrumented tests.
 *
 * camelCase and a single token, matching `materialTileTag` and
 * `backgroundTileTag` — a tag shape that differs between three controls in one
 * card is a trap for the next person to move one of them.
 */
fun reliefTileTag(relief: AppRelief): String = "reliefTile_" + relief.id
