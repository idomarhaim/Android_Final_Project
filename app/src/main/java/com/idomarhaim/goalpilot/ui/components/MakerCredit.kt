package com.idomarhaim.goalpilot.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R

/**
 * The company credit — **Ido Mar-Chaim (IMC)**, the maker — never GoalPilot's own brand.
 *
 * Form is fixed by Ido (2026-10-05): the words `Made By:` and the logo directly beneath
 * them, centred. Placement and the never-in-an-identity-slot rule:
 * `C:\Dev\JARVIS\rules\company-branding.md`. The words stay English in every locale
 * (`translatable="false"` in `components_strings.xml`) — they are part of the credit.
 *
 * The app chooses light/dark **in-app** (`AppBrightness`), not through the
 * configuration's night mode, so a `drawable-night` qualifier would pick the wrong
 * variant whenever the in-app choice differs from the system. The variant is chosen
 * from the surface actually behind the credit instead.
 */
@Composable
fun MakerCredit(
    surface: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(TAG_MAKER_CREDIT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.components_maker_credit_label),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Image(
            painter = painterResource(imcLogoFor(surface)),
            contentDescription = stringResource(R.string.components_maker_credit_description),
            modifier = Modifier.width(150.dp),
        )
    }
}

/** Navy wordmark on light surfaces, near-white wordmark on dark ones. */
@DrawableRes
fun imcLogoFor(surface: Color): Int =
    if (surface.luminance() < 0.5f) R.drawable.imc_logo_dark else R.drawable.imc_logo_light

const val TAG_MAKER_CREDIT = "makerCredit"
