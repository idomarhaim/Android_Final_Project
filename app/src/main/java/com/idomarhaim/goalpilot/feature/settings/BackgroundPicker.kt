package com.idomarhaim.goalpilot.feature.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
import com.idomarhaim.goalpilot.ui.theme.gpSurface
import com.idomarhaim.goalpilot.ui.theme.materialSpecFor

/**
 * Spec §4.9's Appearance section, the ground — `#57` b.
 *
 * ## Why this is a picker over four grounds and not a two-state toggle
 *
 * [AppBackground]'s own doc carries the argument; the short version is that the
 * prototype's *shared / native canvas* toggle is two **values** of this axis
 * ([AppBackground.MATCH] and [AppBackground.GLOW]) rather than a rival design,
 * and Ido asked for *"combinations between the backgrounds and the blocks"*,
 * which two states are not.
 *
 * ## Each tile shows the COMBINATION, not the ground alone
 *
 * A tile that painted only the ground would be four swatches, and the thing
 * being chosen is not a swatch — it is what happens when *this* material sits
 * on *that* ground. So every tile draws the current material's own panel
 * (`gpSurface`, the modifier every card in the app uses) on top of the ground
 * it is offering. That is what makes the definitional cost **visible before it
 * is chosen**: pick a lit ground under Soft and the tile's panel gains an edge
 * on the spot.
 *
 * The same argument runs in the other direction one control up, where
 * `MaterialPicker` previews each material on the currently selected ground.
 * Between them the two controls show a row of the grid each way, which is as
 * much of a 4x4 as fits in a settings card.
 *
 * ## Plain English literals, deliberately
 *
 * `feature/settings` is **unswept** — it is absent from
 * `AnalyticsLiteralSweepTest.SWEPT_PACKAGES` — and AGENTS.md §0.8's suspension
 * permits plain English literals there while `#51` is deferred. Its siblings in
 * the same card (`"Brightness"`, `"Colour"`, `brightnessLabel`,
 * `materialConsequence`) are already written this way, so this control is
 * consistent with the card it joins rather than adding a new kind of debt.
 * `MaterialPicker` lives in the swept `ui/components` and therefore uses
 * `R.string`; that is a property of *where it lives*, not a standard this had
 * to meet.
 */
@Composable
fun BackgroundPicker(
    selected: AppBackground,
    material: AppMaterial,
    skin: AppSkin,
    brightnessIsDark: Boolean,
    onSelect: (AppBackground) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The tiles preview the CURRENT material -- the ground is what varies here,
    // so holding the material fixed is what makes the row a comparison.
    val previewDark = material.resolveDark(brightnessIsDark)
    val previewScheme = colorSchemeFor(skin, material, brightnessIsDark)
    val current = MaterialTheme.gpMaterial

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppBackground.entries.forEach { option ->
            val optionSpec = materialSpecFor(material, option, previewScheme, previewDark)
            val isSelected = option == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .testTag(backgroundTileTag(option)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .gpPage(optionSpec, previewScheme.background)
                        .then(
                            if (isSelected) {
                                // The ring is drawn in the CURRENT material's accent,
                                // not the tile's -- it speaks to the screen around it,
                                // exactly as the material tiles' ring does.
                                Modifier.border(
                                    BorderStroke(2.dp, current.accent),
                                    RoundedCornerShape(16.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(9.dp),
                ) {
                    // The panel, drawn by the same modifier every card uses. This is
                    // the half that makes a tile a COMBINATION rather than a swatch.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .gpSurface(optionSpec, RoundedCornerShape(10.dp)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = backgroundLabel(option),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * What this ground is, in the app's own words.
 *
 * [AppBackground.MATCH] is *"Match"* rather than *"Default"* because the axis it
 * names is a relationship — this ground is a **function of the material**, not a
 * fixed thing the material happens to start on — and "Default" would say the
 * opposite of that to the one user who most needs to know it: the one about to
 * change materials.
 */
internal fun backgroundLabel(background: AppBackground): String = when (background) {
    AppBackground.MATCH -> "Match"
    AppBackground.GLOW -> "Glow"
    AppBackground.SPECTRUM -> "Spectrum"
    AppBackground.PLAIN -> "Plain"
}

/**
 * What the **combination** costs, live — §4.9's consequence-line pattern, the
 * same one the brightness lock uses.
 *
 * Two of the reachable pairs change what the material *is*, and both are named
 * here rather than left to be discovered as a bug report:
 *
 * - a lit ground under a soft material is no longer neumorphism, and
 * - a plain ground under a glass material has nothing to be transparent about.
 *
 * Written against the **resolved** ground, so *Match* reports the same fact as
 * the value it currently means rather than staying silent — the user is running
 * the combination either way.
 */
internal fun backgroundConsequence(
    background: AppBackground,
    material: AppMaterial,
): String {
    val lit = background.isLit(material)
    val soft = material == AppMaterial.NEO || material == AppMaterial.DARK_NEO
    return when {
        soft && lit ->
            "Soft surfaces are defined by the panel being the same colour as the page, " +
                "and a lit ground has no single such colour. The panel becomes a " +
                "translucent plate instead, so it keeps its depth but gains an edge."

        !soft && !lit ->
            "Glass panels have nothing to be transparent about on a plain ground, " +
                "so they read as quiet flat cards. Their rim, shadow and edge are unchanged."

        lit ->
            "The page carries soft coloured lights, which is what the glass panels " +
                "above are transparent about."

        else ->
            "One flat tone behind everything, which is what makes the raised panels " +
                "read as pressed out of the page."
    }
}

/**
 * Stable per-ground tag for instrumented tests.
 *
 * camelCase and a single token, matching `materialTileTag` — see its note. That
 * guard reads `ui/components` only and this file is one package over, but a tag
 * shape that differs between two controls in the same card is a trap for the
 * next person to move one of them.
 */
fun backgroundTileTag(background: AppBackground): String = "backgroundTile_" + background.id
