package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AppLanguage

/**
 * Language chooser for spec §5.1's **Language** setting.
 *
 * Applies instantly, like [SkinPicker] beside it: the surrounding screen becomes
 * the preview, which matters far more here than for a colour — the one question
 * a language switch raises is *"did it actually change the words?"*.
 *
 * ⚠️ **Iterates [AppLanguage.OFFERED], not `entries`, and that is the `#51`
 * deferral.** Hebrew is withheld while `#51` is parked (Ido's decision,
 * 2026-08-17 — see AGENTS.md § *§0.8 is suspended*); the `HEBREW` entry itself
 * stays, because everything that implements Hebrew is written against it.
 * Iterating `entries` here is what made עברית tappable into a two-of-ten-swept
 * UI, so an `entries` loop is the regression, not a simplification.
 *
 * A vertical list rather than [SkinPicker]'s side-by-side tiles, because these
 * options are words of very different widths — and, when `#51` resumes, of two
 * scripts — so a three-across row would either clip "System" or waste the row on
 * "עברית". Modelled as a radio group, so TalkBack announces "selected, 1 of 2"
 * — a count that comes from [AppLanguage.OFFERED] and moves with it.
 */
@Composable
fun LanguagePicker(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        AppLanguage.OFFERED.forEach { language ->
            val isSelected = language == selected
            ListItem(
                modifier = Modifier.selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = { onSelect(language) },
                ),
                headlineContent = {
                    Text(languageLabel(language), style = MaterialTheme.typography.titleSmall)
                },
                leadingContent = {
                    // Null callback: the whole row already carries the click and
                    // the selectable role, so a separately-clickable control
                    // would be announced twice.
                    RadioButton(selected = isSelected, onClick = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

/**
 * The label for one option.
 *
 * **Only `SYSTEM` is translated.** Every other option is an *endonym* — a
 * language's name in itself — and translating them defeats the point: someone
 * who has landed in a script they cannot read needs to recognise their own
 * language's name to get out, and "אנגלית" is no help to a reader of English.
 * `SYSTEM` is not a language name but ordinary chrome, so it follows the picker
 * like every other word in the app.
 *
 * The `else` branch is reachable for exactly one entry today
 * ([AppLanguage.ENGLISH]) and stays a `when` because [AppLanguage.OFFERED] grows
 * back when `#51` resumes.
 */
@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    else -> language.endonym
}
