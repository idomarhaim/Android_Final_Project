package com.idomarhaim.goalpilot.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * **This screen's one new component** — spec §4.9.
 *
 * > Every control that feeds arithmetic elsewhere states that arithmetic under
 * > itself, inside the same card, dimmer and smaller, **with live values, in the
 * > app's own words.**
 *
 * ## Why a component and not a `Text` per card
 *
 * §0.3 is the product map's most-repeated finding — *a second number that
 * quietly disagrees* — and **a settings screen is where that defect is
 * manufactured**: a control here moves a number on a screen nobody is looking
 * at. The remedy is not documentation, it is that the number moves *in front of
 * the person moving it*. Making it one component is what stops the next control
 * shipping without one, because the omission is then visible in a diff.
 *
 * ## Why never a tooltip and never a help link
 *
 * §4.9 rules both out for one reason: **both must be asked for, and nobody asks
 * about a setting they believe they understand.** A consequence line is read by
 * the user who was confident and wrong, which is the only user it is for.
 *
 * ## Two things it must not become
 *
 * - **Not a description of the control.** "Choose when you wake up" is chrome;
 *   "16 h awake — the load bar reddens past 12 h" is a consequence. If the
 *   sentence would still be true with the setting on any other value, it is the
 *   wrong sentence.
 * - **Not an icon.** §0.8's surviving sub-rule is *form and words before
 *   iconography*, and a ⓘ here would be a tooltip by another name.
 */
@Composable
fun ConsequenceLine(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}
