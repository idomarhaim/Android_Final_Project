package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.domain.model.MINUTES_PER_HOUR
import com.idomarhaim.goalpilot.domain.model.OccurrenceDraft
import com.idomarhaim.goalpilot.ui.locale.AppDatePickerDialog
import com.idomarhaim.goalpilot.ui.locale.AppTimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The add-task row's ***when*** control — §2.2's two typed rungs, in one line (`#56`).
 *
 * ## What it can express, and why that is two rungs and not four
 *
 * Pick a **day** and the task is an `ALL_DAY`; add a **time** and it becomes a `DEADLINE`, the
 * rung whose reminder is computed backwards from how long the work takes. `BLOCK` and `SPAN`
 * are fully modelled and fully reminded, and have no author here on purpose — see
 * [OccurrenceDraft]. `#56` is explicit that a full scheduling surface *"needs its own
 * decision"*, so this is the smallest control that makes the model reachable rather than a
 * calendar built in passing.
 *
 * ## The rung is shown, not just the date
 *
 * The chip reads *"Fri, 22 Aug"* for an all-day and *"Fri, 22 Aug, 06:00"* once a time is
 * added, and the time button is what moves between them. That is deliberate: the two differ by
 * **what a miss means** (§2.2), and a control that hid the difference would let someone choose
 * *late, still owed* when they meant *the day passed* without ever seeing the choice.
 *
 * ## Both dialogs go through `ui/locale`
 *
 * `AppDatePickerDialog` / `AppTimePickerDialog`, never the raw Material ones — a dialog
 * composes into its own window, so `AppLocale`'s language override stops at the boundary while
 * RTL mirroring still crosses it, and the result renders in the device language while *looking*
 * perfectly localized. `DialogLocaleGuardTest` enforces it; the reasoning is in its KDoc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WhenPicker(
    draft: OccurrenceDraft,
    onChange: (OccurrenceDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AssistChip(
            onClick = { pickingDate = true },
            label = { Text(draft.label()) },
            leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null) },
            modifier = Modifier.testTag(WHEN_CHIP_TAG),
        )

        // Only offered once there is a day to put a time on. A time with no date is not a
        // deadline and OccurrenceDraft refuses to make one, so offering the control first
        // would be offering a tap that does nothing.
        if (draft.isSet) {
            IconButton(
                onClick = { if (draft.time == null) pickingTime = true else onChange(draft.withoutTime()) },
                modifier = Modifier.testTag(WHEN_TIME_TAG),
            ) {
                // ⚠️ OUTLINED when there is no time and FILLED-and-tinted when there is, and
                // this was found by LOOKING at the render pass rather than by a test. Both
                // states were the same outlined clock, so one glyph in one position carried
                // two opposite actions -- *add a time* and *remove it* -- distinguished only by
                // a content description nobody sees. That is §0.8's surviving sub-rule (one
                // chip may not carry two axes), and it is the same defect the duration box's
                // KDoc records one screen over. Outlined reads as an invitation, filled as a
                // state that is on; the chip's own text corroborates which.
                Icon(
                    if (draft.time == null) Icons.Outlined.Schedule else Icons.Filled.Schedule,
                    contentDescription = if (draft.time == null) ADD_TIME_LABEL else REMOVE_TIME_LABEL,
                    tint = if (draft.time == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            IconButton(
                onClick = { onChange(draft.cleared()) },
                modifier = Modifier.testTag(WHEN_CLEAR_TAG),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = CLEAR_WHEN_LABEL)
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = draft.date?.let { it.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() },
        )
        AppDatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onChange(draft.withDate(it.toUtcLocalDate())) }
                        pickingDate = false
                    },
                ) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (pickingTime) {
        AppTimePickerDialog(
            initialMinutesOfDay = DEFAULT_DEADLINE_MINUTES,
            confirmLabel = "Set",
            dismissLabel = "Cancel",
            title = { Text("Due at") },
            onDismissRequest = { pickingTime = false },
            onConfirm = { minutes ->
                onChange(
                    draft.withTime(
                        LocalTime.of(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR),
                    ),
                )
                pickingTime = false
            },
        )
    }
}

/**
 * `DatePicker` hands back **UTC midnight** for the day the user tapped, whatever zone they are
 * in — that is its documented contract, not an accident — so it is read back in UTC and never
 * in the system zone.
 *
 * Reading it as local time is the classic off-by-one here: east of UTC it lands on the previous
 * day, so a task filed for Friday quietly becomes Thursday and, being an occurrence, is missed
 * a day early. The rest of the occurrence model is deliberately zone-free (see
 * `TaskDto.occurrenceStart`); this is the one boundary where a zone appears, and it is pinned.
 */
private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()

/** *"When?"* until a day is chosen, then the day, then the day and the time. */
@Composable
private fun OccurrenceDraft.label(): String {
    val day = date ?: return NO_WHEN_LABEL
    val dayText = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(day)
    val at = time ?: return dayText
    return dayText + ", " + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(at)
}

/**
 * What the time picker opens on when a deadline is first added: **09:00**.
 *
 * A neutral hour inside `WakingHours.DEFAULT` (07:00 to 23:00), so the very first deadline
 * anyone sets produces a reminder that is not immediately clamped for sleep. It is only the
 * dialog's starting position, never a stored value.
 */
private const val DEFAULT_DEADLINE_MINUTES = 9 * MINUTES_PER_HOUR

internal const val WHEN_CHIP_TAG = "add-task-when"
internal const val WHEN_TIME_TAG = "add-task-when-time"
internal const val WHEN_CLEAR_TAG = "add-task-when-clear"

/** Shared with the tests, so an assertion cannot pass against a stale copy of the wording. */
internal const val NO_WHEN_LABEL = "When?"
internal const val ADD_TIME_LABEL = "Add a time, making it a deadline"
internal const val REMOVE_TIME_LABEL = "Remove the time, making it an all-day task"
internal const val CLEAR_WHEN_LABEL = "Clear when"
