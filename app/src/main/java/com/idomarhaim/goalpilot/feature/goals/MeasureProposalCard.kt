package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.model.MeasureBasis
import com.idomarhaim.goalpilot.domain.model.MeasureProposal
import com.idomarhaim.goalpilot.domain.model.ProposalOrigin
import com.idomarhaim.goalpilot.domain.model.TargetSource
import com.idomarhaim.goalpilot.ui.components.trimNumber

/**
 * The **offer** — spec §1.3 (`C22`
 * [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
 * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)) — and the
 * one surface on this map §10.1 calls *the most easily-resented*.
 *
 * ## Tone is the whole ticket, and every rule below is a rule about tone
 *
 * §0.4 makes the offer **legal**. It does not make it **wanted**, and an offer
 * arriving on a goal Ido deliberately left unmeasured is a correction unless it
 * is drawn exactly as the prototype drew it. So:
 *
 *  1. **[UnmeasuredNote] comes first and is not an apology.** *"No number on this
 *     one. That is a choice, and it stays one — nothing here is incomplete."*
 *     Without that line the offer below it reads as fixing a mistake he made on
 *     purpose. It is also what makes the state where the app offers **nothing**
 *     read as deliberate rather than as a load failure — so it renders whether or
 *     not there is a proposal to put under it.
 *  2. **Dashed and hollow throughout.** The filled accent means *kept* elsewhere
 *     in this app and must not appear on something the app has not done. There is
 *     no filled variant here and no colour to make one.
 *  3. **The dismiss is a peer of the accept** — same row, same weight, no colour.
 *     Absence is the default (`E6`), so *not for this goal* is not the lesser
 *     branch. Drawn small it is a dark pattern; drawn red it says a legitimate
 *     choice was a mistake.
 *  4. **Two buttons, and neither of them is *Not now*.** The prototype's copy
 *     table carries a `later` string and its own frames never render it: §1.3
 *     makes dismissal **permanent, not snoozed**, *because a default that re-asks
 *     is not a default*. A third button would quietly reintroduce the snooze.
 *  5. **The provenance is on the screen, not asserted in a caption.** The number
 *     says where it came from — *your own schedule*, *the steps you listed*, or
 *     plainly that there is nothing to compute from and the app will not invent
 *     one.
 *
 * ## English literals are legal in this file
 *
 * `feature/goals` is not in `AnalyticsLiteralSweepTest.SWEPT_PACKAGES`, and
 * `AGENTS.md` §0.8's suspension block permits plain literals in an unswept
 * package — adding this package to the sweep as a favour is what it explicitly
 * forbids. #65's brief suspends §0.8 for the render pass on the same grounds.
 * `GoalMeasureStrings.kt` next door is where this copy joins when #51 reaches
 * this package.
 */

/** Test tags, so the instrumented suite names the same nodes the design does. */
object MeasureProposalTags {
    const val NOTE = "measure_absence_note"
    const val OFFER = "measure_offer"
    const val ACCEPT = "measure_offer_accept"
    const val DISMISS = "measure_offer_dismiss"
}

/**
 * §1.3's *"the absence is stated as legal before anything is offered"*.
 *
 * Rendered for **every** unmeasured goal, with or without a proposal under it.
 * That is not redundancy: with a proposal it is what stops the offer reading as a
 * correction, and without one it is the only thing on the screen saying the
 * silence is deliberate. The prototype's frame 5 exists to prove exactly that
 * second case — *the app saying nothing, on a real screen, which is the only way
 * to see whether it reads as deliberate or broken.*
 */
@Composable
fun UnmeasuredNote(modifier: Modifier = Modifier) {
    DashedPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MeasureProposalTags.NOTE),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "◇",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "No number on this one.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // The second sentence is the load-bearing one. "No number" is a
                    // fact; "that is a choice, and it stays one" is the app declining
                    // to treat the fact as a defect, which is the only thing that
                    // earns it the right to make an offer underneath.
                    text = "That is a choice, and it stays one — nothing here is incomplete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The offer itself, under [UnmeasuredNote] and above whatever it computed its
 * target from.
 *
 * Renders nothing when [proposal] is null — §3.4's silent row. The caller still
 * renders [UnmeasuredNote], so the screen says the absence is legal and then says
 * nothing further, which is the state frame 5 was drawn to test.
 *
 * @param onAccept applies the measure. §1.3: the offer **never auto-applies**, so
 *   this is the only path from a proposal to a goal, and it is a finger press.
 * @param onDismiss dismisses **permanently** for this goal (§1.3). Not a snooze;
 *   there is no un-dismiss, and the manual path through the goal editor is what
 *   keeps nothing unreachable.
 */
@Composable
fun MeasureOffer(
    proposal: MeasureProposal?,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (proposal == null) return

    DashedPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MeasureProposalTags.OFFER),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                // The same hollow glyph the marker carries in the list, so the offer
                // is visibly the same subject the row was flagged for rather than a
                // second unrelated invitation.
                Text(
                    text = "#",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = offerTitle(proposal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (proposal.basis == MeasureBasis.LEADING) {
                        Text(
                            // §1.3's leading indicator, said as a TAG and not as a
                            // sentence. It is a real claim — this number measures the
                            // behaviour that produces the result, not the result — and
                            // on a model-phrased proposal nothing else on the card
                            // carries it. Two words is what it can cost without the
                            // card turning back into a lecture.
                            text = "leading indicator",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = offerProvenance(proposal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            Text(
                // Says the two things a person weighing this actually needs: that
                // nothing happens on its own, and that saying no ends it. Both are
                // §1.3 guarantees, and an offer that does not state them is asking
                // the user to trust an unstated policy.
                text = "The app never applies this on its own. " +
                    "Dismiss once and this goal stops asking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Both OutlinedButton, both weight(1f): rule 3 above. A FilledButton
                // beside a TextButton would make the default branch look like the
                // mistake, and the default here is to have no number at all.
                OutlinedButton(
                    onClick = onAccept,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(MeasureProposalTags.ACCEPT),
                ) { Text("Use this") }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(MeasureProposalTags.DISMISS),
                ) { Text("Not for this goal") }
            }

            // NO second policy paragraph here, and its absence is a finding rather
            // than an omission. A draft carried §1.3's *"changing a kind is never
            // silent"* under the buttons; the render pass showed the card grown to
            // three stacked blocks of app-policy prose over a two-line offer, which
            // is what a nag looks like — the exact failure #65 exists to avoid. The
            // prototype's own `offer()` renders title, ONE `why` line and the two
            // buttons; its `changeable` string belongs to the confirm sheet, which
            // is where that promise is owed and where it is not competing with the
            // offer itself.
        }
    }
}

/**
 * The offer's headline — *what the app is proposing to count*, in the app's own
 * voice for a mechanical proposal and the model's word for a phrased one.
 *
 * §3.4: *"the surface is unchanged — same component, same two buttons, only the
 * wording is the app's rather than the model's."* So there is one function here
 * and not two components. The mechanical branch is the prototype's frame 4
 * verbatim, because the claim it makes is stronger than a model's: *these are
 * your steps, and this is arithmetic.*
 */
private fun offerTitle(proposal: MeasureProposal): String = when {
    proposal.origin == ProposalOrigin.MECHANICAL &&
        proposal.targetSource == TargetSource.STEPS ->
        "Count the steps you already listed"

    proposal.origin == ProposalOrigin.MECHANICAL &&
        proposal.targetSource == TargetSource.SCHEDULE ->
        "Count the occurrences you already schedule"

    // A model-phrased proposal names its own unit, and the unit is content — so it
    // is isolated rather than concatenated into an English frame that a Hebrew
    // word would reverse (§4.8).
    //
    // "Measure it in <word>" and not "Count <word>", which the render pass caught:
    // the model's word is a UNIT and need not be a plural noun, so the verb frame
    // has to survive "kg lost" and "a week" as readily as "pages". *Count kg lost*
    // is not English; *measure it in kg lost* is, for every word the schema allows.
    else -> "Measure it in ${proposal.word.bidiIsolated()}"
}

/**
 * Where the number came from, said on the screen.
 *
 * The `USER` branch is the one worth reading twice. It does **not** apologise and
 * it does not offer a guess: §3.3 E forbids the model supplying a target because
 * `C11a` measured free numbers swinging 2× run-to-run, and saying so plainly is
 * what turns a missing number from a gap into a policy the user can agree with.
 */
private fun offerProvenance(proposal: MeasureProposal): String {
    val target = proposal.target?.trimNumber()?.bidiIsolated()
    return when (proposal.targetSource) {
        TargetSource.SCHEDULE ->
            if (target == null) NOTHING_TO_COMPUTE_FROM
            else "$target a week are already set on this goal, so the target is " +
                "arithmetic and not a judgement."

        TargetSource.STEPS ->
            if (target == null) NOTHING_TO_COMPUTE_FROM
            else "$target steps are already listed here. Nothing was generated — the " +
                "steps are yours and the count is arithmetic, so this stands with no " +
                "model behind it."

        TargetSource.USER ->
            "Nothing to compute from, so the target is yours to set. The app will not " +
                "invent one, and the model is not asked for one."
    }
}

/**
 * Used where a `SCHEDULE` or `STEPS` source arrived with nothing behind it — a
 * model naming an arithmetic the goal cannot run.
 *
 * The proposal is still offered, because its kind and word are perfectly usable
 * and §3.3 E's whole point is that only the *number* was untrustworthy. What
 * changes is that the app says so instead of showing a blank.
 */
private const val NOTHING_TO_COMPUTE_FROM: String =
    "Nothing to compute a target from yet, so that number is yours to set."

/**
 * The one container both surfaces use: dashed border, no fill, no accent.
 *
 * A single private composable rather than a modifier on each, so §1.3's *"dashed
 * and hollow throughout"* is a property of the file rather than a discipline
 * anyone editing it has to remember. There is no `filled` parameter.
 */
@Composable
private fun DashedPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val edge = MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                val radius = 16.dp.toPx()
                val inset = stroke / 2f
                drawRoundRect(
                    color = edge,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - stroke,
                        size.height - stroke,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                        ),
                    ),
                )
            }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) { content() }
}
