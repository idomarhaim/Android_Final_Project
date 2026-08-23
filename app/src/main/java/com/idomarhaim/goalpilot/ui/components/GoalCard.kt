package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.Goal

/** Compact card summarising one goal's progress; tapping opens the detail. */
@Composable
fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = goal.colorHex.toGoalAccent()
    val ink = goal.colorHex.toGoalInk()
    GpCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Squircle rather than a circle: it sits better against the card's
            // own rounded rectangle and gives the icon more optical weight.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.10f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = goal.category.icon(),
                    contentDescription = goal.category.localizedLabel(),
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = goal.title.ifBlank {
                            stringResource(R.string.components_goal_untitled)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    // §1.3's MARKER (`C22` #44, #65) — a dashed square, no words and
                    // no buttons, wherever the goal is listed. It may be here, and
                    // the offer may not, because §0.7 needs consent for intrinsic
                    // structure and **stating a fact asserts nothing**. Opening the
                    // goal is that consent, which is where the offer lives.
                    //
                    // It REPLACES the percentage rather than sitting beside it
                    // (`#66`). It used to do the latter, and the two then made
                    // opposite claims on one row: the marker said there is no
                    // number and the row printed `0%` and `0/100` next to it —
                    // both computed against `targetValue`'s 100.0 default, a
                    // target nobody set. §0.3's *second number that quietly
                    // disagrees*, in the one place §1.3 had just denied it.
                    UnmeasuredMarkerIfNeeded(measureIsAbsent = goal.isUnmeasured)
                    if (goal.isComplete && !goal.isUnmeasured) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.components_goal_complete),
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (!goal.isUnmeasured) {
                        Text(
                            text = percentText(goal.progressPercent),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                        )
                    }
                }
                // The BAR goes with the digit, and that is not tidiness: it is the
                // same fiction drawn instead of printed. #11's live example —
                // `Health · 1/100 %` — fills this bar to 1% of a target nobody set,
                // and a reader who never reads the digit still reads the fill.
                if (!goal.isUnmeasured) {
                    GpLinearProgress(
                        progress = goal.progressFraction,
                        color = accent,
                        height = 8.dp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Text(
                    // One resource with three arguments, not a four-part
                    // concatenation: word order is a property of the language
                    // and no resource file can reorder a Kotlin `+`. The ratio
                    // is isolated as ONE run — `5/10` reverses to `10/5` in an
                    // RTL paragraph otherwise — and `unit` is isolated because
                    // it is user-authored (§8) and its script is unknown here.
                    //
                    // The unmeasured branch states the honest count instead of the
                    // ratio, which is what the `C22` prototype draws — `no number
                    // — 11 sessions logged` on its own life-area frame.
                    text = if (goal.isUnmeasured) {
                        unmeasuredMetaText(
                            categoryLabel = goal.category.localizedLabel(),
                            loggedEntryCount = goal.loggedEntryCount,
                        )
                    } else if (goal.restatesPercent) {
                        // The category alone. A goal that CHOSE percent already
                        // states its number in the trailing slot above, and
                        // `45/100 %` under it is the same claim a second time —
                        // §0.3's two numbers that do not even disagree. The widget
                        // has dropped this label since `#11`; the row had not.
                        // Caught in `#66`'s own render pass, by looking: every
                        // assertion passed, because the defect is a RELATION
                        // between two marks and no per-node query ranges over one.
                        goal.category.localizedLabel()
                    } else {
                        stringResource(
                            R.string.components_goal_meta,
                            goal.category.localizedLabel(),
                            "${goal.currentValue.trimNumber()}/${goal.targetValue.trimNumber()}"
                                .bidiIsolated(),
                            goal.measureWord.bidiIsolated(),
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Renders `5.0` as `"5"`, `5.5` as `"5.5"`, and `0.25` as `"0.25"`.
 *
 * **It used to be `"%.1f"`, and that was wrong twice over** — found by #11, whose
 * fill-button ladder produces `0.25` on §1.3's own worked example. One decimal
 * renders that as `0.3`, so the water goal would have read `0.3 / 4 L` after a
 * 250 ml tap: a number the app made up, on the one screen the ticket exists to
 * fix. And `String.format` without an explicit `Locale` follows the *default*
 * locale, so the separator and even the digits move with the device — a display
 * rule has no business varying by device language.
 *
 * Three decimals is above anything the ladder can produce and below anything a
 * person types, and rounding there is also what absorbs binary-representation
 * noise: `currentValue` is a **sum** over entries (§4.6), and a sum of thirds
 * arrives as `0.30000000000000004` without it.
 */
fun Double.trimNumber(): String {
    if (!isFinite()) return toString()
    val rounded = Math.round(this * 1000.0) / 1000.0
    if (rounded == Math.floor(rounded)) return rounded.toLong().toString()
    return rounded.toString().trimEnd('0').trimEnd('.')
}
