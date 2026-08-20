package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.ui.components.trimNumber

/**
 * The repeat-tappable fill buttons and their running tally — `R25`,
 * [#11](https://github.com/idomarhaim/Android_Final_Project/issues/11), spec §1.3.
 *
 * > *"For the task 'drink 4 litres a day' I have several fill buttons I can tap
 * > more than once (250 ml, 500 ml, 750 ml, 1 L)."*
 *
 * Each tap logs one [ProgressEntry][com.idomarhaim.goalpilot.domain.model.ProgressEntry]
 * of that amount and nothing else. **There is no counter behind the tally**:
 * §4.6 makes `currentValue` a sum over entries, so what the row shows is the same
 * number every other screen shows, arrived at the same way. That is also why
 * there is no optimistic overlay here, unlike `toggleTask` — a log is an ordinary
 * `add()`, so Firestore applies it to the offline cache immediately and the
 * snapshot listener redraws the tally on the next frame, radio on or off.
 *
 * **The labels carry no `ml`.** [FillLadder][com.idomarhaim.goalpilot.domain.model.FillLadder]
 * says why: rendering `0.25 L` as `250 ml` needs the app to know the user's word
 * means litres, and §1.3 makes that word user content.
 *
 * **There is no `enabled` flag, and its absence is load-bearing.** The obvious
 * shape — disable the row while a write is in flight — is wrong here, and wrong
 * in the direction that destroys the feature. `Observed:` 2026-08-20 by reading
 * `ProgressRepositoryImpl.logProgress`: it ends in `ref.set(dto).await()`, and a
 * Firestore write task resolves on **server ack**, not on the cache write. So
 * offline, or on a slow radio, the first tap would disable all four buttons until
 * the network came back — on a control whose entire premise is *"I can tap it
 * more than once"* (`R25`), and on precisely the surface §5.3 says must keep
 * working offline. The cached write and the tally are both immediate, so there is
 * nothing for the disable to protect: a second tap is a second entry, which is
 * what the user meant.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillButtonRow(
    amounts: List<Double>,
    word: String,
    current: Double,
    target: Double,
    onLog: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (amounts.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        // FlowRow, not Row: four buttons whose labels are the user's own word can
        // be any width at all (`0.25 L` and `12500 שקלים` are the same component),
        // and a Row would clip the last one rather than wrap it.
        //
        // **Two per row, at equal width, and that is a render-pass finding.** Left
        // to flow freely the four buttons wrapped `3 + 1` at 360 dp — legible, and
        // it reads as a layout accident rather than a decision, which §0.8 makes a
        // defect in its own right. A 2x2 block is stable at every width and every
        // word length, and it degrades honestly: three rungs give `2 + 1`, one
        // gives a single half-width button.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            amounts.forEach { amount ->
                val label = amountLabel(amount, word)
                FilledTonalButton(
                    onClick = { onLog(amount) },
                    // The visible label is bidi-isolated for §4.8, which makes it
                    // an unreliable test and a11y handle; the description is the
                    // plain reading of the same amount.
                    modifier = Modifier
                        .weight(1f)
                        .semantics { this.contentDescription = "Log $label" },
                ) {
                    Text(label.bidiIsolated())
                }
            }
        }
        Text(
            text = tallyLabel(current, target, word).bidiIsolated(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { this.contentDescription = tallyLabel(current, target, word) },
        )
    }
}

/**
 * `0.25` + `"L"` → `"0.25 L"`; a goal with a word but no unit text still reads as
 * a bare number rather than as a number with a trailing space.
 */
internal fun amountLabel(amount: Double, word: String): String =
    "${amount.trimNumber()} ${word.trim()}".trim()

/**
 * The running tally: `"1.5 / 4 L"`.
 *
 * **Not a percentage**, deliberately. The whole defect #11 opens on is a goal
 * that reads `1/100 %` when it is four litres of water, and restating the ring's
 * percentage under the buttons would put the same second number back one row
 * lower (§0.3). Past the target it simply keeps counting — overshoot is legal and
 * shown (§1.5), and a tally that stopped at the target would be the fifth clamp.
 */
internal fun tallyLabel(current: Double, target: Double, word: String): String =
    "${current.trimNumber()} / ${amountLabel(target, word)}"
