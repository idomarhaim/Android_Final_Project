package com.idomarhaim.goalpilot.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.ui.locale.AppModalBottomSheet
import com.idomarhaim.goalpilot.ui.locale.AppTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * **§4.3's author for `BLOCK` and `SPAN`** — the sheet a tapped slot or the FAB opens
 * ([#60](https://github.com/idomarhaim/Android_Final_Project/issues/60)).
 *
 * Every rule it enforces lives in [SlotDraft] and is JVM-tested there; this is the control surface
 * for those transitions and holds no logic of its own. That is `OccurrenceDraft`'s own argument for
 * where entry state belongs — *"a rule that can only be exercised on a running device is a rule
 * whose branches do not all get tested"* — and it is why a night block, a coerced span and a
 * carried duration are all checked with no emulator.
 *
 * ⚠️ **It goes through [AppModalBottomSheet], not a raw `ModalBottomSheet`.** §0.8's Hebrew freeze
 * is suspended but `DialogLocaleGuardTest` stays armed and app-wide: a window opened outside the
 * `ui/locale/` façades escapes the app's own locale, and that defect is **invisible in an English
 * render** — which is exactly why the guard is what catches it rather than a look at the screen.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SlotSheet(
    draft: SlotDraft,
    goals: List<Goal>,
    onChange: (SlotDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (SlotDraft) -> Unit,
) {
    var editing by remember { mutableStateOf<TimeField?>(null) }

    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag(TAG_SHEET),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "New on ${draft.date.format(DAY)}",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = draft.title,
                onValueChange = { onChange(draft.copy(title = it)) },
                label = { Text("What is it?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(TAG_TITLE),
            )

            // The rung switch. Two options, and the labels say what a MISS means rather than
            // naming the enum -- 2.2's whole discriminator is "what does missing this mean",
            // and "BLOCK" tells a user nothing about that.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AuthoredRung.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = draft.rung == option,
                        onClick = { onChange(draft.withRung(option)) },
                        shape = SegmentedButtonDefaults.itemShape(index, AuthoredRung.entries.size),
                        modifier = Modifier.testTag(rungTag(option)),
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            when (draft.rung) {
                AuthoredRung.BLOCK -> BlockTimes(draft) { editing = it }
                AuthoredRung.SPAN -> SpanDays(draft, onChange)
            }

            if (goals.isNotEmpty()) {
                GoalPicker(draft = draft, goals = goals, onChange = onChange)
            }

            // FlowRow: Cancel + Save is two short words in English and can be two long
            // ones elsewhere, and a Row would crush the second rather than wrap it.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel", maxLines = 1) }
                Button(
                    onClick = { onSave(draft) },
                    enabled = draft.isValid,
                    modifier = Modifier.testTag(TAG_SAVE),
                ) {
                    Text("Add")
                }
            }
        }
    }

    editing?.let { field ->
        AppTimePickerDialog(
            initialMinutesOfDay = field.current(draft).let { it.hour * 60 + it.minute },
            confirmLabel = "Set",
            dismissLabel = "Cancel",
            title = { Text(field.label) },
            onDismissRequest = { editing = null },
            onConfirm = { minutes ->
                onChange(field.applyTo(draft, LocalTime.of(minutes / 60, minutes % 60)))
                editing = null
            },
        )
    }
}

/**
 * The block's two times, and the duration they come to.
 *
 * The duration is shown rather than left implicit because [SlotDraft.withEnd] takes an end before
 * the start as **the following morning** — a reading that is right for a night block and wrong for
 * a mis-tap, and the only thing that makes the difference visible at the moment it happens.
 */
@Composable
private fun BlockTimes(draft: SlotDraft, onEdit: (TimeField) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { onEdit(TimeField.START) },
            modifier = Modifier.weight(1f).testTag(TAG_START),
        ) {
            Text(draft.startTime.format(HH_MM))
        }
        Text("–", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = { onEdit(TimeField.END) },
            modifier = Modifier.weight(1f).testTag(TAG_END),
        ) {
            Text(draft.endTime.format(HH_MM))
        }
    }
    Text(
        text = buildString {
            append(durationLabel(draft.minutes))
            if (draft.crossesMidnight) append(" · ends next day")
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(TAG_DURATION),
    )
}

/** The span's length in days. A window is what it is; the last day is derived from the count. */
@Composable
private fun SpanDays(draft: SlotDraft, onChange: (SlotDraft) -> Unit) {
    val days = (java.time.temporal.ChronoUnit.DAYS.between(draft.date, draft.endDate) + 1).toInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { onChange(draft.withEndDate(draft.endDate.minusDays(1))) },
            enabled = days > 1,
            modifier = Modifier.testTag(TAG_SPAN_SHORTER),
        ) {
            Text("−")
        }
        Text(
            text = if (days == 1) "1 day" else "$days days",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).testTag(TAG_SPAN_DAYS),
        )
        OutlinedButton(
            onClick = { onChange(draft.withEndDate(draft.endDate.plusDays(1))) },
            modifier = Modifier.testTag(TAG_SPAN_LONGER),
        ) {
            Text("+")
        }
    }
    Text(
        text = "${draft.date.format(DAY)} – ${draft.endDate.format(DAY)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Which objective this is filed under — which is what gives the row its chip.
 *
 * Optional, and *unfiled* is a real answer rather than a blank: §1.5's edge *"declares its
 * contribution in the objective's own word, or contributes nothing"*, and a task nobody filed is
 * the ordinary case, not an omission to nag about.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalPicker(draft: SlotDraft, goals: List<Goal>, onChange: (SlotDraft) -> Unit) {
    Column {
        Text(
            text = "Towards",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        // The riskiest row in the file: every chip after the first is a GOAL TITLE, which
        // the user wrote and which nothing bounds. A Row fits what it can and crushes the
        // rest; this wraps.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { onChange(draft.copy(goalId = null)) }) {
                Text("Nothing", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            goals.take(MAX_GOAL_CHIPS).forEach { goal ->
                OutlinedButton(
                    onClick = { onChange(draft.copy(goalId = goal.id)) },
                    modifier = Modifier.testTag("calendar_goal_${goal.id}"),
                ) {
                    Text(goal.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

/** Which of the block's two times a picker is editing. */
private enum class TimeField(val label: String) {
    START("Starts at"),
    END("Ends at");

    fun current(draft: SlotDraft): LocalTime = if (this == START) draft.startTime else draft.endTime

    fun applyTo(draft: SlotDraft, time: LocalTime): SlotDraft =
        if (this == START) draft.withStart(time) else draft.withEnd(time)
}

/**
 * Keeps an open sheet across a rotation.
 *
 * A sheet that loses its half-typed title when the phone turns is the same class of loss as a
 * settings screen that forgets what you chose — and [SlotDraft] is plain data, so the saver is six
 * fields rather than a reason not to bother.
 */
val SlotDraftSaver: Saver<SlotDraft?, List<String>> = Saver(
    save = { draft ->
        draft?.let {
            listOf(
                it.date.toString(),
                it.title,
                it.rung.name,
                it.startTime.toString(),
                it.endTime.toString(),
                it.endDate.toString(),
                it.goalId.orEmpty(),
            )
        } ?: emptyList()
    },
    restore = { saved ->
        if (saved.size < SAVED_FIELDS) {
            null
        } else {
            SlotDraft(
                date = LocalDate.parse(saved[0]),
                title = saved[1],
                rung = AuthoredRung.valueOf(saved[2]),
                startTime = LocalTime.parse(saved[3]),
                endTime = LocalTime.parse(saved[4]),
                endDate = LocalDate.parse(saved[5]),
                goalId = saved[6].ifBlank { null },
            )
        }
    },
)

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

private const val SAVED_FIELDS = 7
private const val MAX_GOAL_CHIPS = 3

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

const val TAG_SHEET = "calendar_slot_sheet"
const val TAG_TITLE = "calendar_slot_title"
const val TAG_SAVE = "calendar_slot_save"
const val TAG_START = "calendar_slot_start"
const val TAG_END = "calendar_slot_end"
const val TAG_DURATION = "calendar_slot_duration"
const val TAG_SPAN_DAYS = "calendar_slot_span_days"
const val TAG_SPAN_LONGER = "calendar_slot_span_longer"
const val TAG_SPAN_SHORTER = "calendar_slot_span_shorter"

fun rungTag(rung: AuthoredRung): String = "calendar_rung_${rung.name}"
