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
 * a language switch raises is *"did it actually change the words?"*, and §0.8's
 * *a design is not finished until it has been seen in Hebrew* is only checkable
 * if seeing it costs one tap.
 *
 * A vertical list rather than [SkinPicker]'s side-by-side tiles, because these
 * options are words of very different widths in two scripts, and a
 * three-across row would either clip "System" or waste the row on "עברית".
 * Modelled as a radio group so TalkBack announces "selected, 2 of 3".
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
        AppLanguage.entries.forEach { language ->
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
 * **Only `SYSTEM` is translated.** The other two are *endonyms* — a language's
 * name in itself — and translating them defeats the point: someone who has
 * landed in a script they cannot read needs to recognise their own language's
 * name to get out, and "אנגלית" is no help to a reader of English. `SYSTEM` is
 * not a language name but ordinary chrome, so it follows the picker like every
 * other word in the app.
 */
@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    else -> language.endonym
}
