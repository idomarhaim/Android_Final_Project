package com.idomarhaim.goalpilot.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiProvider
import com.idomarhaim.goalpilot.ui.components.GpCard
import com.idomarhaim.goalpilot.ui.locale.AppAlertDialog
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet

/**
 * §4.9's **AI** section — the fifth and last, and the one `#48` deliberately
 * left out (#54, decided in #32).
 *
 * `#48` shipped four of five sections and recorded why this one was missing, in
 * `SettingsScreen.kt`'s own table: *"its three controls are `C13`'s — an
 * `EncryptedSharedPreferences` key store, a provider abstraction, and a status
 * line naming which provider answered"*. All three now exist, so the section
 * does.
 *
 * ## What renders with no key, and why it is one button and not three controls
 *
 * `null` is the default and almost every install's state. In it this card shows
 * **the status line and one action**, not an inert provider picker over a model
 * field over an empty key field. A picker with no key behind it is a control
 * that changes nothing, which is the exact reason `#48` refused to render this
 * section as a dimmed row in the first place — and the reason `#53` had to ship
 * before Appearance could offer material tiles. `#54`'s requirement is stricter
 * still: *a user who never opens this section should not notice it exists*.
 *
 * ## The key is masked, replaced, and deleted — never revealed
 *
 * There is **no reveal action anywhere in this app**, and the stored key has no
 * render path at all: the editor's field starts **empty** even when a
 * credential exists, and leaving it empty keeps the stored key rather than
 * showing it. `#54`: *"Reveal is a feature request, not a default."* What the
 * user sees is [AiCredential.maskedKey] — eight bullets and the last four
 * characters, enough to tell *the key I pasted* from *the key I meant to
 * paste*.
 *
 * ## Delete is confirmed, because it is unrecoverable here
 *
 * `#54` requires *a real delete*, and [AiProviderRepository.clear] gives one —
 * the bytes go. Nothing in GoalPilot can bring the key back, and the user may
 * not have it anywhere else, so the confirmation is not ceremony. The dialog
 * states the consequence the app *will* have: the free model answers again.
 *
 * @param onSave stores the credential; the caller clears §5's dead-key latch.
 * @param onClear the real delete.
 */
@Composable
fun AiCard(
    credential: AiCredential?,
    lastAnswer: AiAnswer?,
    onSave: (AiCredential) -> Unit,
    onClear: () -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    GpCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingLabel("Who answers")
            SettingDescription(stringResource(R.string.settings_ai_description))

            // §5's permanent status row. Always rendered, in every state,
            // including the ones the point-of-use message stays silent about.
            Text(
                text = aiStatusLine(credential, lastAnswer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_AI_STATUS),
            )

            Spacer(Modifier.height(16.dp))

            if (credential == null) {
                OutlinedButton(
                    onClick = { editorOpen = true },
                    modifier = Modifier.testTag(TAG_AI_ADD),
                ) {
                    Text("Use your own API key")
                }
            } else {
                SettingLabel("Provider")
                ValueRow(
                    value = credential.provider.displayName,
                    onClick = { editorOpen = true },
                    testTag = TAG_AI_PROVIDER,
                )

                Spacer(Modifier.height(12.dp))

                SettingLabel("Model")
                ValueRow(
                    // effectiveModel, not model: a blank field means the
                    // provider's default, and showing the blank would make the
                    // row say the app has no opinion when it does.
                    value = credential.effectiveModel,
                    onClick = { editorOpen = true },
                    testTag = TAG_AI_MODEL,
                )

                Spacer(Modifier.height(12.dp))

                SettingLabel("Key")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = credential.maskedKey,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TAG_AI_KEY_MASK),
                    )
                    TextButton(
                        onClick = { editorOpen = true },
                        modifier = Modifier.testTag(TAG_AI_REPLACE),
                    ) {
                        Text("Replace")
                    }
                    TextButton(
                        onClick = { confirmingDelete = true },
                        modifier = Modifier.testTag(TAG_AI_DELETE),
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            ConsequenceLine(
                text = aiConsequenceLine(credential),
                modifier = Modifier.testTag(TAG_AI_CONSEQUENCE),
            )
        }
    }

    if (editorOpen) {
        AiKeyEditorSheet(
            existing = credential,
            onSave = {
                onSave(it)
                editorOpen = false
            },
            onDismiss = { editorOpen = false },
        )
    }

    if (confirmingDelete) {
        AppAlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Remove your API key?") },
            text = {
                Text(
                    "It is deleted from this phone and GoalPilot cannot get it back. " +
                        "The free model answers again — nothing stops working.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        confirmingDelete = false
                    },
                    modifier = Modifier.testTag(TAG_AI_DELETE_CONFIRM),
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Keep it") }
            },
        )
    }
}

/**
 * §4.9's three AI controls, in one window: **provider · model · key**.
 *
 * A sheet rather than three rows on the screen because the three are one
 * decision — a model id belongs to a provider, and a key belongs to both — and
 * because a key field permanently on a settings screen is a key field somebody
 * eventually screenshots.
 *
 * ## The key field starts empty even when a key exists
 *
 * That is the *replace* action, and it is what makes "never reveal the stored
 * key" true rather than aspirational: there is no code path in this app that
 * puts [AiCredential.key] into a `TextField`. Leaving it empty on save keeps
 * what is stored, so changing only the model does not force a re-paste — a
 * convenience that costs nothing, because nothing about the key is shown or
 * sent differently either way.
 *
 * ## The action row is pinned, and the sheet opens fully expanded
 *
 * ⚠️ **Both are fixes for a defect this file shipped with for one build**, found
 * by `AiSectionUiTest` on an emulator: the sheet opened *partially* expanded and
 * the form scrolled inside it, which put **Save at y=3033px on a 2992px
 * screen** — the primary action of the editor, off the bottom of the window the
 * moment it opened. A tap at that point does not miss harmlessly either: it
 * lands on the **scrim**, so the sheet closes and the key the user just typed is
 * silently discarded.
 *
 * It reads perfectly in a screenshot of the top of the sheet, and the JVM tests
 * cannot see geometry at all. `Observed:` 2026-08-20, `sdk_gphone64_x86_64`,
 * 1344×2992 — the test typed a key, pressed Save, and the card came back still
 * saying *"You have not added a key"* with no exception anywhere.
 *
 * The scrolling `Column` keeps its `weight(1f, fill = false)` so a short screen
 * with the keyboard up still scrolls the *form*; what may never scroll away is
 * the row that commits it.
 *
 * @param existing `null` when adding the first key.
 */
// AppModalBottomSheet wraps ModalBottomSheet, which is still experimental in
// Material 3 — the same opt-in SettingsContent carries for the region sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiKeyEditorSheet(
    existing: AiCredential?,
    onSave: (AiCredential) -> Unit,
    onDismiss: () -> Unit,
) {
    var provider by remember { mutableStateOf(existing?.provider ?: AiProvider.GROQ) }
    var model by remember { mutableStateOf(existing?.model.orEmpty()) }
    var key by remember { mutableStateOf("") }

    // Blank means "keep what is stored" only when there IS something stored;
    // with no credential a blank key is nothing to save.
    val canSave = key.isNotBlank() || existing != null

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        // A form that opens half-covered is a form whose primary action is not
        // on screen. See the KDoc above for what that cost.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp)
                .testTag(TAG_AI_EDITOR),
        ) {
            Text(
                text = if (existing == null) "Use your own API key" else "Your API key",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "It stays on this phone, encrypted. It is never saved to your account " +
                    "and never included in a backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // ── 1 · provider ──────────────────────────────────────
            SettingLabel("Provider")
            AiProvider.entries.forEach { candidate ->
                ListItem(
                    headlineContent = {
                        Text(candidate.displayName, style = MaterialTheme.typography.bodyLarge)
                    },
                    supportingContent = { Text(candidate.keyOrigin) },
                    trailingContent = if (candidate == provider) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            provider = candidate
                            // The model field is per-provider: a GROQ id sent to
                            // Anthropic is a silent failure, and #32's whole
                            // reason for four named adapters was that no untested
                            // wire format can ever run. Clearing it hands the new
                            // provider its own default rather than the old
                            // provider's id.
                            model = ""
                        }
                        .testTag(providerTag(candidate)),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── 2 · model ─────────────────────────────────────────
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                placeholder = { Text(provider.defaultModel) },
                // Free text with a per-provider default, never a curated list
                // (#32, "Also settled"): a list baked into a release rots exactly
                // as a hard-coded id does, and now in four providers at once.
                supportingText = {
                    Text("Leave blank for ${provider.defaultModel}. Model ids change — " +
                        "if answers stop improving, check the provider's current id.")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_AI_MODEL_FIELD),
            )

            Spacer(Modifier.height(12.dp))

            // ── 3 · key ───────────────────────────────────────────
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(if (existing == null) "API key" else "New API key") },
                // No reveal toggle, deliberately — see the KDoc. The
                // transformation is unconditional, so there is no state in which
                // this field renders a key in the clear.
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    Text(
                        if (existing == null) {
                            "Get one at ${provider.keyOrigin}."
                        } else {
                            "Leave blank to keep the key you already saved."
                        },
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_AI_KEY_FIELD),
            )

            Spacer(Modifier.height(20.dp))
        }

        // OUTSIDE the scrolling Column, deliberately — see the KDoc.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val secret = key.ifBlank { existing?.key.orEmpty() }
                    AiCredential.of(provider, model, secret)?.let(onSave)
                },
                enabled = canSave,
                modifier = Modifier.testTag(TAG_AI_SAVE),
            ) {
                Text("Save")
            }
        }
    }
}

/** Per-provider tag so a test can pick one of four without depending on row order. */
fun providerTag(provider: AiProvider): String = "settings_ai_provider_${provider.id}"

const val TAG_AI_STATUS = "settings_ai_status"
const val TAG_AI_CONSEQUENCE = "settings_ai_consequence"
const val TAG_AI_ADD = "settings_ai_add"
const val TAG_AI_PROVIDER = "settings_ai_provider"
const val TAG_AI_MODEL = "settings_ai_model"
const val TAG_AI_KEY_MASK = "settings_ai_key_mask"
const val TAG_AI_REPLACE = "settings_ai_replace"
const val TAG_AI_DELETE = "settings_ai_delete"
const val TAG_AI_DELETE_CONFIRM = "settings_ai_delete_confirm"
const val TAG_AI_EDITOR = "settings_ai_editor"
const val TAG_AI_MODEL_FIELD = "settings_ai_model_field"
const val TAG_AI_KEY_FIELD = "settings_ai_key_field"
const val TAG_AI_SAVE = "settings_ai_save"
