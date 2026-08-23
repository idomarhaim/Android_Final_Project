package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R
import com.idomarhaim.goalpilot.core.util.bidiIsolated
import com.idomarhaim.goalpilot.domain.usecase.NextStepOffer
import com.idomarhaim.goalpilot.domain.usecase.NoNextStepGoal
import com.idomarhaim.goalpilot.domain.usecase.SuccessFailureRun
import com.idomarhaim.goalpilot.domain.usecase.SuccessRange
import com.idomarhaim.goalpilot.domain.usecase.SuccessWindow
import com.idomarhaim.goalpilot.domain.usecase.WindowOutcome
import java.time.format.DateTimeFormatter

/**
 * **`C19`'s success/failure run** — `docs/PRODUCT_v0.3.md` §4.7,
 * [`#64`](https://github.com/idomarhaim/Android_Final_Project/issues/64), drawn against the
 * prototype at `docs/prototypes/2026-08-13-area-success-failure/` (revision 5).
 *
 * ## One component, two placements, and that is the point
 *
 * §4.7: *"**One component, two placements:** above the goal list on the life-area screen, and
 * beside the time donut on analytics."* It lives in `ui/components/` for that reason and not
 * for tidiness — two copies would be two answers to *"how am I doing here?"*, which is §0.3's
 * most-repeated finding with the numbers agreeing only by accident.
 *
 * The single thing that differs between the placements is [showAsymmetryNote], and §4.7 says
 * where it goes: *"beside the time donut on analytics — where the asymmetry sentence lives
 * **and nowhere else**"*. Revision 1 of the prototype printed it on every area frame, which
 * said the same thing twice and never where the two numbers actually meet.
 *
 * ## Outcome state never rides on hue
 *
 * §4.7's material contract, verbatim:
 *
 * ```
 * kept          filled
 * missed        hollow
 * still-owed    dashed with a centre pip     <- the one state that must NOT read as a failure
 * nothing-due   dotted
 * no next step  a dashed ring carrying a +   <- an invitation, not an outcome
 * ```
 *
 * So [WindowDot] distinguishes by **form** — fill, stroke, dash, pip — and the accent is one
 * colour used at four weights rather than four colours. The run therefore reads in dark neo, in
 * greyscale, and to a colour-blind eye.
 *
 * ⚠️ **This paragraph used to say *"there is no red in this file"* flatly, and `#67` made that
 * false — so it is narrowed here rather than left to rot.** §4.7's ban is on **outcome state**
 * riding on hue: no window, no dot, no count and no run may be tinted, and nothing below has
 * changed. What `#67` added is `Let it go`, which is a **destructive control** and not an
 * outcome — the same class as every `Delete` in the app, drawn the same way. The invariant that
 * survives, and the one to check an edit against, is *no colour here carries information about
 * how the person is doing*; a button that ends a goal is not saying anything about that.
 *
 * ## Two numbers, never a rate
 *
 * The pair is [SuccessFailureRun.kept] and [SuccessFailureRun.missed], which are counts over
 * the same windows the dots draw — so the two halves of this card cannot disagree. Nothing here
 * divides one by the other, and §4.7 forbids adding it *"even as a subtitle"*.
 *
 * ## The sentence under the run is not a caption
 *
 * §4.7: *"**What a window is** is answered **on the screen**, under the run… The numbers are
 * meaningless without it, so it is not spec-only text."* It renders unconditionally whenever
 * there is a run to explain.
 */
@Composable
fun SuccessFailureRunCard(
    run: SuccessFailureRun,
    onSelectRange: (SuccessRange) -> Unit,
    /** Opens the goal an offer is about — `C8`'s and `C9a`'s existing surfaces, not a new one. */
    onOpenGoal: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** The area's colour on the life-area screen; the theme's primary on analytics. */
    accent: Color = MaterialTheme.colorScheme.primary,
    /** §4.7: the asymmetry sentence belongs beside the time donut **and nowhere else**. */
    showAsymmetryNote: Boolean = false,
    /**
     * §4.7's `Let it go`, wired — `C19`'s third offer, delivered by
     * [`#67`](https://github.com/idomarhaim/Android_Final_Project/issues/67).
     *
     * **`null` means the host cannot perform it, and then the button is absent** rather than
     * present and inert. That is not defensive plumbing: `#64` shipped this card *without*
     * `Let it go` on exactly that reasoning — *"a button proposing a goal is over while doing
     * nothing is worse than the honest silence"* — so a default that drew a dead control would
     * reintroduce the thing the omission was protecting against.
     */
    onLetGo: ((NoNextStepGoal) -> Unit)? = null,
) {
    GpCard(modifier = modifier.fillMaxWidth().testTag(TAG_RUN_CARD)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.components_run_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            RangeChips(selected = run.range, onSelect = onSelectRange)

            if (run.hasRecord) {
                KeptMissedPair(kept = run.kept, missed = run.missed, accent = accent)
                WindowRun(windows = run.windows, accent = accent)
            } else {
                // Nothing has ever been due. A row of empty dots over `0 kept · 0 missed` is
                // technically true and reads as a verdict, which is the one thing §4.7 spends
                // its whole length avoiding.
                Text(
                    text = stringResource(R.string.components_run_empty_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.components_run_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showAsymmetryNote) {
                Text(
                    text = stringResource(R.string.components_run_asymmetry),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TAG_ASYMMETRY),
                )
            }

            if (run.noNextStep.isNotEmpty()) {
                NoNextStepSection(
                    goals = run.noNextStep,
                    accent = accent,
                    onOpenGoal = onOpenGoal,
                    onLetGo = onLetGo,
                )
            }
        }
    }
}

/** §4.7's `30 days · 8 weeks · 6 months`, default 8 weeks — a filter, never decay. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeChips(
    selected: SuccessRange,
    onSelect: (SuccessRange) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuccessRange.entries.forEach { range ->
            val on = range == selected
            FilterChip(
                selected = on,
                onClick = { onSelect(range) },
                label = { Text(range.label()) },
                modifier = Modifier.testTag(rangeTag(range)),
            )
        }
    }
}

/**
 * The pair — and there is nothing between them.
 *
 * `missed` is drawn in the foreground ink at reduced emphasis rather than in any warning
 * colour: it is a count, not an accusation, and §4.7 puts no red on this screen.
 */
@Composable
private fun KeptMissedPair(kept: Int, missed: Int, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BigNumber(
            value = kept,
            label = stringResource(R.string.components_run_kept),
            color = accent,
            tag = TAG_KEPT,
            modifier = Modifier.weight(1f),
        )
        BigNumber(
            value = missed,
            label = stringResource(R.string.components_run_missed),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
            tag = TAG_MISSED,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BigNumber(
    value: Int,
    label: String,
    color: Color,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 13.dp, vertical = 11.dp)
            .testTag(tag),
    ) {
        Text(
            // §4.8: a bare number inside a paragraph is reordered by the bidi algorithm. It is
            // isolated here rather than at the call site so no caller has to remember.
            text = "$value".bidiIsolated(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The run itself — `C5` §4's record, oldest window first.
 *
 * The legend names only the states actually present, exactly as the prototype does: a legend
 * entry for something not on screen is a word the user has to rule out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WindowRun(windows: List<SuccessWindow>, accent: Color) {
    val present = windows.map { it.outcome }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.testTag(TAG_DOTS),
        ) {
            windows.forEach { window -> WindowDot(window = window, accent = accent) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            WindowOutcome.entries.filter { it in present }.forEach { outcome ->
                LegendEntry(outcome = outcome, accent = accent)
            }
        }
        Text(
            // Not a caption, and not optional: §4.7 says the numbers are meaningless without it.
            text = stringResource(R.string.components_run_window_is),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TAG_WINDOW_IS),
        )
    }
}

@Composable
private fun LegendEntry(outcome: WindowOutcome, accent: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutcomeDot(outcome = outcome, accent = accent, diameter = 11)
        Text(
            text = stringResource(outcome.legendRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One window, as a dot — and it says what it is out loud.
 *
 * A screen reader has no dot to look at, so each carries its outcome **and its dates**. The same
 * duty `UnmeasuredMarker`'s single string discharges for a component that is otherwise silent.
 */
@Composable
private fun WindowDot(window: SuccessWindow, accent: Color) {
    val span = window.spanLabel()
    val description = stringResource(window.outcome.descriptionRes, span)
    Box(
        modifier = Modifier.semantics { this.contentDescription = description },
    ) {
        OutcomeDot(outcome = window.outcome, accent = accent, diameter = 17)
    }
}

/**
 * §4.7's five shapes, drawn by **form** and never by hue.
 *
 * Each branch is one line of the spec's own table, and they are deliberately distinguishable
 * with the colour removed: filled, hollow, dashed-with-a-pip, dotted.
 */
@Composable
private fun OutcomeDot(outcome: WindowOutcome, accent: Color, diameter: Int) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(diameter.dp)
            .drawBehind {
                val stroke = (size.minDimension * 0.13f).coerceAtLeast(1.2f)
                val inset = stroke / 2f
                val ring = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                when (outcome) {
                    // Filled: the only solid dot on the screen, and the only thing that means
                    // the window was honoured.
                    WindowOutcome.KEPT -> drawCircle(color = accent)

                    // Hollow. Not red, not filled with anything -- the absence IS the reading.
                    WindowOutcome.MISSED -> drawOval(
                        color = ink,
                        topLeft = topLeft,
                        size = ring,
                        style = Stroke(width = stroke),
                    )

                    // Dashed with a centre pip. Revision 1 drew it as a dashed ring alone and
                    // it was nearly invisible at this size, so the one state that must NOT read
                    // as a failure read as one. The pip makes it the only dot with an inside
                    // and an edge.
                    WindowOutcome.STILL_OWED -> {
                        drawOval(
                            color = accent,
                            topLeft = topLeft,
                            size = ring,
                            style = Stroke(
                                width = stroke * 1.25f,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(stroke * 1.6f, stroke * 1.4f),
                                ),
                            ),
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.55f),
                            radius = size.minDimension * 0.22f,
                        )
                    }

                    // Dotted, and quieter than everything else: nothing happened here.
                    WindowOutcome.NOTHING_DUE -> drawOval(
                        color = ink.copy(alpha = 0.5f),
                        topLeft = topLeft,
                        size = ring,
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(stroke * 0.7f, stroke * 1.3f),
                            ),
                        ),
                    )
                }
            },
    )
}

/**
 * §4.7's table: the goals with **no next step**, each offered the step it is actually missing.
 *
 * ⚠️ **Neither offer is a new AI surface.** §4.7 marks *Break it into steps* as `C8`'s existing
 * feature, and both buttons open the goal — where tasks are added and dated today. The offers
 * differ in the **sentence**, which is the part that is about this goal; inventing a second
 * route would be re-speccing a feature the ticket says to reuse.
 *
 * ✅ **`Let it go` is here now** — `#67`, and it is a **command**, which is the whole condition
 * `#64` left it waiting on.
 *
 * §4.7 draws it beside the offer and says it *"stays a command, never an inference"* — `C4`
 * forbids the app asserting an intrinsic edge by itself. `#64` shipped without it for a reason
 * that has since expired: *"there is no command behind it, and a button proposing a goal is over
 * while doing nothing is worse than the honest silence."* `#67` gave every entity a reachable
 * delete, so there is a command behind it, and this is the goal's own instance of it.
 *
 * ⚠️ **Nothing about `C4` is relaxed by its arrival.** The button says `Let it go` and the app
 * still never *suggests* that a goal is over: the row it sits on says `no next step`, which is a
 * statement about what is scheduled and not about whether the goal matters. `#67` is explicit
 * that this decision stays taken — *"the button may exist; the app may never suggest that a goal
 * is over, and no copy anywhere may imply it."*
 *
 * ⚠️ **It is last in the row and it is not the offer.** The two `NextStepOffer` buttons are what
 * §4.7 puts here; `Let it go` is the escape from them, and drawing it with equal weight would
 * make *give up* look like one of two equally recommended next steps. It is a text button in the
 * error colour, after the offer — and the **only** thing in this file that is not the accent, at
 * one deliberate cost: §4.7's *"there is no red on this screen at all"* is about **outcome
 * state**, the four window forms, and this is a destructive **control**, which every other
 * screen in the app already draws that way.
 *
 * ## ⚠️ The row is TWO lines, and the one-line version was a real defect
 *
 * Until 2026-08-24 the goal's title, the offer and `Let it go` shared one `Row`,
 * with the title column carrying `weight(1f)` and the two buttons carrying no
 * weight at all. That is the arrangement everybody writes and it has a failure
 * mode with teeth: a Compose `Row` measures its **unweighted** children first,
 * against the full incoming constraints, and only then hands what is left to the
 * weighted ones. Two buttons reading *Schedule the first one* and *Let it go*
 * will happily take the entire width, and the title column — the thing the row
 * is **about** — gets whatever remains.
 *
 * `Observed:` 2026-08-24, on Ido's Galaxy S25 Ultra. The two buttons rendered at
 * full size and every goal title was squeezed into a column roughly one glyph
 * wide, wrapping vertically: `i` `d` `l` `e` down the screen, one letter per
 * line, for eight goals. The same build on the `Pixel_10_Pro_XL` emulator looked
 * fine — which is exactly why it shipped. The discriminator is available width in
 * **dp** against **font scale**, and the emulator this was written on is wider
 * and at scale 1.0.
 *
 * **What was rejected.** *Weight the buttons too* — then the button labels
 * truncate instead, and a control reading `Schedule the fi…` is worse than a
 * wrapped title. *Make them icons* — §0.8's *form and words before iconography*
 * forbids it, and it is the same rule that sent the `#` marker to a word in this
 * same commit. *Truncate the title harder* — it was already `maxLines = 1` and
 * ellipsised, and that did nothing, because the column was never given the width
 * to ellipsise **into**.
 *
 * So the actions get their own line. It costs one line of height per goal and it
 * cannot be got wrong at any font scale, on any width — there is no longer a
 * competition to lose. `maxLines` on the title goes 1 → 2 for the same reason:
 * it now has a full row to itself, so a long goal name is legible rather than
 * cut at the first fold.
 */
@Composable
private fun NoNextStepSection(
    goals: List<NoNextStepGoal>,
    accent: Color,
    onOpenGoal: (String) -> Unit,
    onLetGo: ((NoNextStepGoal) -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = pluralStringResource(
                R.plurals.components_run_no_next_step_footer,
                goals.size,
                "${goals.size}".bidiIsolated(),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TAG_NO_NEXT_STEP_FOOTER),
        )
        goals.forEach { goal ->
            // Two lines, and that is the whole fix for Ido's S25 Ultra. See this
            // function's KDoc: the goal is what the row is ABOUT, so it may not be
            // the thing that gives way when the buttons need room.
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    NoNextStepMark(accent = accent)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title.ifBlank {
                                stringResource(R.string.components_goal_untitled)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = goal.metaLine(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // End-aligned, so the eye reads title-then-offer down the card
                // rather than hunting for two buttons in a ragged left column.
                // The offer is still first and `Let it go` still last: the order
                // this function's KDoc defends is about sequence, not about which
                // line they sit on.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onOpenGoal(goal.goalId) },
                        modifier = Modifier.testTag(offerTag(goal.offer)),
                    ) {
                        Text(
                            text = stringResource(goal.offer.labelRes),
                            maxLines = 1,
                        )
                    }
                    onLetGo?.let { letGo ->
                        TextButton(
                            onClick = { letGo(goal) },
                            modifier = Modifier.testTag(letGoTag(goal.goalId)),
                        ) {
                            Text(
                                text = stringResource(R.string.components_run_let_it_go),
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * §4.7's fifth shape: **a dashed ring carrying a `+`** — *"deliberately unlike all four, because
 * it is an invitation and not an outcome"*.
 *
 * A circle, where `UnmeasuredMarker` is a square. Both are dashed invitations and they can share
 * a screen, so §0.8's *one chip may not carry two axes* applies: every circle in this app's
 * language is an occurrence or an outcome, and a number slot is a square. That distinction was
 * bought by a render — see `UnmeasuredMarker`'s KDoc — and this is the other half of it.
 */
@Composable
private fun NoNextStepMark(accent: Color) {
    // 20.dp with a SMALL plus inside, and both numbers are a render finding rather than taste.
    // The first draft drew it at 17.dp with arms at 0.24 of the diameter and a plus as thick as
    // the ring: the mark filled itself in and read as a dense blob -- closest on the screen to
    // the still-owed pip, which is the one dot it must not be confused with. §0.8's *one chip
    // may not carry two axes* is the rule it was breaking, and only the frame showed it
    // (`docs/render-passes/2026-08-23-64-area-success-failure/`).
    Box(
        modifier = Modifier
            .size(20.dp)
            .drawBehind {
                val stroke = (size.minDimension * 0.11f).coerceAtLeast(1.2f)
                val inset = stroke / 2f
                drawOval(
                    color = accent,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(stroke * 1.8f, stroke * 1.6f),
                        ),
                    ),
                )
                // Air between the plus and the ring is what makes it read as an invitation
                // rather than as a filled mark, so the arm is a fifth of the radius short of
                // the stroke and the plus is thinner than the ring that holds it.
                val arm = size.minDimension * 0.17f
                val centre = Offset(size.width / 2f, size.height / 2f)
                val pen = stroke * 0.8f
                drawLine(accent, centre.copy(x = centre.x - arm), centre.copy(x = centre.x + arm), pen)
                drawLine(accent, centre.copy(y = centre.y - arm), centre.copy(y = centre.y + arm), pen)
            },
        contentAlignment = Alignment.Center,
    ) {}
}

// ── words ────────────────────────────────────────────────────────────────────────────────

/**
 * The range's label.
 *
 * A `when` over the enum rather than a label on it: `ComponentStrings.kt` gives the reason for
 * every enum in this package — *a language switch cannot reach a constructor argument*, which is
 * the fourth idiom in `kb/dev/untranslatable-idioms.md`.
 */
@Composable
private fun SuccessRange.label(): String = stringResource(
    when (this) {
        SuccessRange.THIRTY_DAYS -> R.string.components_run_range_30_days
        SuccessRange.EIGHT_WEEKS -> R.string.components_run_range_8_weeks
        SuccessRange.SIX_MONTHS -> R.string.components_run_range_6_months
    },
)

private val WindowOutcome.legendRes: Int
    get() = when (this) {
        WindowOutcome.KEPT -> R.string.components_run_legend_kept
        WindowOutcome.MISSED -> R.string.components_run_legend_missed
        WindowOutcome.STILL_OWED -> R.string.components_run_legend_owed
        WindowOutcome.NOTHING_DUE -> R.string.components_run_legend_nothing_due
    }

private val WindowOutcome.descriptionRes: Int
    get() = when (this) {
        WindowOutcome.KEPT -> R.string.components_run_window_kept
        WindowOutcome.MISSED -> R.string.components_run_window_missed
        WindowOutcome.STILL_OWED -> R.string.components_run_window_owed
        WindowOutcome.NOTHING_DUE -> R.string.components_run_window_nothing_due
    }

private val NextStepOffer.labelRes: Int
    get() = when (this) {
        NextStepOffer.BREAK_IT_INTO_STEPS -> R.string.components_run_offer_break_down
        NextStepOffer.SCHEDULE_THE_FIRST_ONE -> R.string.components_run_offer_schedule
    }

/**
 * `"no next step · idle 4 months"` — §4.7's row, verbatim.
 *
 * The idle half is dropped entirely when nothing ever happened on the goal, because
 * `idle 0 days` claims activity that never took place.
 */
@Composable
private fun NoNextStepGoal.metaLine(): String {
    val head = stringResource(R.string.components_run_no_next_step)
    val days = idleDays ?: return head
    val idle = when {
        days < 14 -> pluralStringResource(
            R.plurals.components_run_idle_days,
            days.toInt(),
            "$days".bidiIsolated(),
        )
        days < 60 -> (days / 7).let { weeks ->
            pluralStringResource(
                R.plurals.components_run_idle_weeks,
                weeks.toInt(),
                "$weeks".bidiIsolated(),
            )
        }
        else -> (days / 30).let { months ->
            pluralStringResource(
                R.plurals.components_run_idle_months,
                months.toInt(),
                "$months".bidiIsolated(),
            )
        }
    }
    return "$head · $idle"
}

/**
 * `"1–7 Aug"` for a window wider than a day, and one date for a day.
 *
 * §4.8: a date range is the canonical shape the bidi algorithm reorders — `09:00–12:00` renders
 * as `12:00–09:00` in an RTL paragraph — so the whole label is isolated as one unit. This is a
 * `contentDescription` today rather than visible text, and it is isolated anyway: the rule is
 * about the string, and a string that becomes visible later must not have to be found again.
 */
private fun SuccessWindow.spanLabel(): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    val label = if (from == to) from.format(fmt) else "${from.format(fmt)}–${to.format(fmt)}"
    return label.bidiIsolated()
}

// ── test handles ─────────────────────────────────────────────────────────────────────────

// camelCase, and that is not a style preference. `ui/components` is a SWEPT package, so
// `AnalyticsLiteralSweepTest` reads every literal in this file and calls anything with two or
// more alphabetic words user-facing prose -- which `"success_failure_kept"` is, to a regex.
// `MaterialPicker`'s tags in this same package are the idiom (`"materialTile_" + id`), and a
// concatenation rather than an interpolation keeps the literal down to one word.
const val TAG_RUN_CARD = "successRunCard"
const val TAG_KEPT = "successRunKept"
const val TAG_MISSED = "successRunMissed"
const val TAG_DOTS = "successRunDots"
const val TAG_WINDOW_IS = "successRunWindowIs"
const val TAG_ASYMMETRY = "successRunAsymmetry"
const val TAG_NO_NEXT_STEP_FOOTER = "successRunNoNextStep"

fun rangeTag(range: SuccessRange): String = "successRunRange_" + range.name

fun offerTag(offer: NextStepOffer): String = "successRunOffer_" + offer.name

/** `#67`'s `Let it go`, per goal, so a test can name the row it is letting go of. */
fun letGoTag(goalId: String): String = "successRunLetGo_" + goalId
