package com.idomarhaim.goalpilot.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * One quiet line about what a **cross-boundary** read knows about itself — the
 * *as-of* caption and the *"Not loaded yet"* state (#50, spec §5.3).
 *
 * ### It is a caption, never a warning
 *
 * > Staleness is a property of the *data*, not of the *connection*.
 *
 * A leaderboard fetched forty minutes ago over perfect Wi-Fi is exactly as old as
 * one served from cache with the radio off, so this is drawn **identically online
 * and offline** and nothing on its path asks the OS about the radio. `#50` argues
 * down both alternatives and they are not to be reintroduced: no global
 * connectivity banner (it would assert that what sits below it is suspect, and
 * after `C20` the owner's own numbers are not), and no per-number "cached"
 * styling. Styled as ordinary secondary body text for the same reason — an error
 * colour would make a statement of fact read as a fault.
 *
 * ### Why it takes its words from the caller
 *
 * The two callers say different things — *"Leaderboard as of 09:14"* on
 * `feature/social`, *"Standings as of 09:14"* and *"Not loaded yet"* on
 * `feature/challenges` — and each names its own surface. Keeping the prose at the
 * call site also keeps it out of `ui/components/`, which `AnalyticsLiteralSweepTest`
 * holds to `stringResource`; both feature packages are still unswept, and #51 owns
 * that sweep.
 *
 * Only ever used for a collection somebody **other than the reader** writes.
 * Owner-side data is complete and correct offline after `C20` and has nothing to
 * be as-of about.
 */
@Composable
fun FreshnessNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
