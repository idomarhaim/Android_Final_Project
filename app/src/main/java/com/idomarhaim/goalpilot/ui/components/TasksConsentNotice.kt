package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R

/**
 * *"Google Tasks access was not granted"* — the sentence a declined scope owes
 * the user (issue #36, spec §2.6 / §0.4).
 *
 * Shared rather than written twice because **two** surfaces read the same
 * `tasks.readonly` scope — the dashboard's import card and the life-areas sync
 * card — and a sentence duplicated across two screens is a sentence that will
 * be reworded on one of them.
 *
 * Both strings are resources, not Kotlin literals: this is app speech, so §5.1
 * and §4.8 make Hebrew part of it. `values-he/` carries these three and only
 * these three; the rest of the resource set is issue #51.
 *
 * Callers render this **instead of** their ordinary pitch paragraph, and pair it
 * with [R.string.tasks_consent_grant_action] on their own button — the action
 * differs by surface (import vs sync) even though the sentence does not.
 */
@Composable
fun TasksConsentNotice(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.tasks_consent_missing_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.tasks_consent_missing_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
