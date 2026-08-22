package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppBackground
import com.idomarhaim.goalpilot.domain.model.AppMaterial
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.ui.theme.colorSchemeFor
import com.idomarhaim.goalpilot.ui.theme.gpMaterial
import com.idomarhaim.goalpilot.ui.theme.gpPage
import com.idomarhaim.goalpilot.ui.theme.gpSurface
import com.idomarhaim.goalpilot.ui.theme.materialSpecFor

/**
 * Spec §4.9's Appearance section, first control: the four material tiles.
 *
 * ## The documented exception
 *
 * > **The material picker is the one control that must not obey the material.**
 * > Each tile paints itself; drawn against the contract, three of four options
 * > render as the fourth.
 *
 * So this is the only file in the app allowed to call [materialSpecFor] with a
 * material that is not the current one. Everywhere else that would be the
 * `when (material)` the contract exists to forbid; here it *is* the control.
 * (`feature/settings/BackgroundPicker.kt` calls it too, and does not break the
 * rule: it varies the **ground** and holds the material at the current one, so
 * it is the same exception one axis over rather than a second one.)
 *
 * ## And each tile paints itself in the current SKIN
 *
 * §4.1's enforcement note, which was found by a prototype shipping without it:
 *
 * > `AppSkin` must reach **every** material's accent, ground and ramp. […] a
 * > skin picker which no material reads is a control that does nothing, and it
 * > looks correct in source.
 *
 * A tile previewing a material in a *fixed* palette is the same bug one axis
 * over — the tiles would not move when the skin did, and the honest preview is
 * what this material looks like *with what you have chosen*.
 *
 * ## The lock is a word
 *
 * > **A lock is a word, never a dimming.** A 40%-opacity segment is a quiet
 * > no-op with extra steps.
 *
 * Dark neo carries `Dark only` on its tile — [AppMaterial.isBrightnessLocked],
 * so a second brightness-locked material gets the badge without an edit here.
 * The other half of the same disclosure lives on the brightness control, which
 * strikes its segments through and captions why.
 *
 * ## And on the currently chosen GROUND (`#57` b)
 *
 * The same argument a third time. `#57` b made the background its own axis, so
 * a tile previewing every material on that material's *native* ground would
 * show a combination the user has not chosen and cannot get to from here — and
 * the tile that changes most under a foreign ground is the one whose whole
 * definition depends on it (`AppBackground`'s note on neo). The honest preview
 * is *this material, on the ground you are actually running*.
 *
 * @param brightnessIsDark what the *brightness setting* resolved to. Each tile
 *   previews itself at this brightness unless its own lock overrides it — which
 *   is exactly the thing the badge is announcing.
 * @param background the selected ground. [AppBackground.MATCH] resolves per
 *   tile, which is what makes the default row show four *different* grounds.
 */
@Composable
fun MaterialPicker(
    selected: AppMaterial,
    skin: AppSkin,
    brightnessIsDark: Boolean,
    background: AppBackground,
    onSelect: (AppMaterial) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = AppMaterial.entries.chunked(2)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { material ->
                    MaterialTile(
                        material = material,
                        skin = skin,
                        brightnessIsDark = brightnessIsDark,
                        background = background,
                        isSelected = material == selected,
                        onSelect = { onSelect(material) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialTile(
    material: AppMaterial,
    skin: AppSkin,
    brightnessIsDark: Boolean,
    background: AppBackground,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The tile's OWN material, at the brightness that material would actually
    // render in -- resolveDark is what makes the dark-neo tile look dark while
    // the rest of the screen is light, which is the whole claim of the badge.
    val previewDark = material.resolveDark(brightnessIsDark)
    val previewScheme = colorSchemeFor(skin, material, brightnessIsDark)
    val previewSpec = materialSpecFor(material, background, previewScheme, previewDark)

    // The selection ring is the ONE thing drawn in the current material, not the
    // tile's: it says "this is the chosen one" to the screen around it, so it
    // has to be legible in the material the screen is wearing.
    val current = MaterialTheme.gpMaterial

    Column(
        modifier = modifier
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect)
            .testTag(materialTileTag(material)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(previewSpec.corner))
                .gpPage(previewSpec, previewScheme.background)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            BorderStroke(2.dp, current.accent),
                            RoundedCornerShape(previewSpec.corner),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(10.dp),
        ) {
            // A miniature of what the material does to a panel: the fill, the
            // depth and the edge, drawn by the same modifier every card uses.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .gpSurface(previewSpec, RoundedCornerShape(previewSpec.corner - 6.dp)),
            ) {
                // The accent, as the material paints it -- one colour for three
                // of them, a two-stop ramp for dark neo. This is the stripe that
                // makes Aurora and Blossom visibly different under dark neo.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .height(6.dp)
                        .fillMaxWidth(0.5f)
                        .clip(CircleShape)
                        .accentFill(previewSpec.accentStops),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(current.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = current.onAccent,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(material.label(), style = MaterialTheme.typography.titleSmall)
        // §4.1's own word for this material, under the word a user reads.
        // VISIBLE, not a contentDescription: the failure #53 filed is that a
        // reader of the spec cannot FIND the control, and a description nobody
        // sees fixes only the screen-reader half of that. Being a Text it lands
        // in the semantics tree anyway, so the one line covers both.
        //
        // Unconditional, including liquid glass, where the two vocabularies
        // happen to coincide and the line therefore repeats the label. A line
        // that appeared only where the words differ would be unlearnable -- a
        // reader could not tell "the same" from "not stated" -- and the
        // repetition is the honest report of a genuine coincidence.
        Text(
            text = material.specName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(materialSpecTag(material)),
        )
        if (material.isBrightnessLocked) {
            // §4.9: the word, on the tile. Not a hue, not a dimming.
            Text(
                text = stringResource(R.string.components_material_dark_only),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(materialLockTag(material)),
            )
        }
        Text(
            text = material.tagline(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Paint the material's accent, whichever shape it takes.
 *
 * `accentStops` is a list precisely because dark neo's accent **is** a gradient
 * while the other three are colours — so a call site that reached for a single
 * `Color` would silently render dark neo as its bright end alone and lose the
 * one thing that material is.
 */
private fun Modifier.accentFill(stops: List<Color>): Modifier = when (stops.size) {
    0 -> this
    1 -> this.background(stops.first())
    else -> this.background(Brush.horizontalGradient(stops))
}

/**
 * Stable per-material tag, so an instrumented test can name a tile without its
 * words — which is the point, since the words move when `#51` resumes.
 *
 * ⚠️ **camelCase, not the `settings_material_picker` snake_case used one package
 * over, and that is a constraint rather than a preference.** `ui/components` is
 * in `AnalyticsLiteralSweepTest.SWEPT_PACKAGES`, and that guard calls a literal
 * *prose* when it holds two alphabetic runs of two-plus letters. `Observed:`
 * 2026-08-20 — `"material_tile_${'$'}{material.id}"` strips to `material_tile_`,
 * counts **two** words, and fails the sweep. A single token is the shape the
 * guard already accepts for keys (its own passing example is `favorite`), so
 * the tag conforms to it instead of the guard being loosened to let prose
 * through.
 */
fun materialTileTag(material: AppMaterial): String = "materialTile_" + material.id

/** The `Dark only` badge — present only where [AppMaterial.isBrightnessLocked]. */
fun materialLockTag(material: AppMaterial): String = "materialLock_" + material.id

/**
 * The spec-name caption — present on **every** tile, which is what makes its
 * absence assertable. Same camelCase constraint as [materialTileTag] above.
 */
fun materialSpecTag(material: AppMaterial): String = "materialSpec_" + material.id
