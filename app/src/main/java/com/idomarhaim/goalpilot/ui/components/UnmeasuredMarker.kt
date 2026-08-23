package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idomarhaim.goalpilot.R

/**
 * The **marker** — spec §1.3 (`C22`
 * [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
 * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)).
 *
 * > **It is two surfaces, not one.** A **marker** — a dashed **square**, no
 * > buttons — wherever the goal is listed; and the **offer** itself **only on
 * > the goal's own screen**, because **opening the goal is the consent** §0.7
 * > requires for intrinsic structure.
 *
 * ## Why this one may be everywhere and the offer may not
 *
 * §0.7: *intrinsic structure needs consent; instrumental structure does not.* A
 * measure defines what counts as progress on a goal, which is intrinsic — so the
 * **offer** may not be pushed into a list being scanned for something else. But
 * **stating a fact asserts nothing**, and this states one: *no number yet.* That
 * is the whole reason the ticket's answer was two objects rather than one
 * placement, and it is why this file has no `onClick` and no action.
 *
 * ## ⚠️ There is no `#` in it any more, and that was a real defect
 *
 * Until 2026-08-24 this square carried a bold `#` glyph, standing for *a number
 * slot* the way `+` does on `C19`'s marker beside it. `Observed:` 2026-08-24 —
 * Ido, looking at his own app on two devices, reported it as *"a picture of `#`
 * that I did not understand what it is supposed to express"*. He is the person
 * the app was built for and he could not read it, which is the only test a glyph
 * has to pass.
 *
 * §0.8 already had the answer and this file was on the wrong side of it: **form
 * and words before iconography.** So the two shapes now split by what is beside
 * them, and neither of them is a symbol:
 *
 * - [UnmeasuredMarker] — the hero sizes (the goal header at 72.dp, the
 *   dashboard's hero ring slot at 56.dp). **An empty dashed square, and nothing
 *   inside it.** Both call sites already print the whole sentence next to it —
 *   *No number yet — nothing logged* — so the object's job is to be a visibly
 *   **empty slot**, which every reader already knows how to read. A glyph there
 *   was never carrying meaning the sentence did not; it was competing with it.
 * - [UnmeasuredChip] — the inline list slot, where the marker **replaces a
 *   percentage** and there is no sentence in the same glance. It carries the
 *   words. A 16.dp square with a symbol in it was being asked to say *no number*
 *   in a slot the reader had just been trained to read as `45%`, and a two-word
 *   answer is what a percentage's slot can hold.
 *
 * **Rejected:** *keep the glyph and explain it in a tooltip* — a mark that needs
 * a tooltip has already failed, and there is no hover on a phone. *A `?` or a
 * dash instead* — the same class of guess, one symbol swapped for another, and a
 * dash additionally reads as *zero*, which is the one thing §1.3 exists to stop
 * the app claiming.
 *
 * ## Dashed and hollow, always
 *
 * §1.3: the offer *"is drawn dashed and hollow throughout, so it can never borrow
 * the visual language of an outcome."* The filled accent means **kept** on
 * `C19`'s screen and must not appear on something the app has not done. There is
 * deliberately no filled variant of this composable and no colour parameter that
 * could make one.
 *
 * ## It is a SQUARE, and the shape is load-bearing
 *
 * Revision 2 of the prototype drew it as a dashed **circle**, and beside `C19`'s
 * dashed circle carrying `+` on the row below, the two invitations read as the
 * same thing at a glance — one chip carrying two axes. Every circle in this
 * app's language is an occurrence or an outcome, so a number slot is a square:
 * **distinguished by form, not by hue**, which is what keeps it legible in dark
 * mode, in every material, and in greyscale. Caught only once rendered.
 *
 * ## The one string, and why a silent component has one
 *
 * A screen reader has no square to see. So the marker's two visual claims —
 * *there is no number yet* and *nothing is owed* — are carried by
 * `components_goal_unmeasured`, which is TalkBack-only and never rendered.
 * Without it the marker is genuinely invisible to the one user who most needs a
 * list to be legible.
 */
@Composable
fun UnmeasuredMarker(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    // onSurfaceVariant and not the goal's accent: an accent is how this app says
    // *this thing is yours and it is going somewhere*. An absence is neither.
    val edge = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.components_goal_unmeasured)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description }
            .dashedSlot(edge),
    )
}

/**
 * The marker with its words on it, for a row that has no room for a sentence.
 *
 * The same dashed hollow rectangle as [UnmeasuredMarker] — same rule, same
 * colour, same corner — sized by the text inside it rather than by a `Dp`. It
 * goes in the trailing slot of a goal row, which is where the percentage would
 * be if the goal had one, and that adjacency is the whole design: the reader's
 * eye arrives at the place a number lives and finds, in words, that there is not
 * one.
 *
 * `maxLines = 1` with an ellipsis rather than wrapping. A trailing slot that
 * grows to two lines pushes the goal's title around at large font scales, which
 * is the same defect `SuccessFailureRun`'s `NoNextStepSection` was fixed for in
 * this same commit — and the words are short enough that the ellipsis is
 * unreachable in practice.
 */
@Composable
fun UnmeasuredChip(modifier: Modifier = Modifier) {
    val edge = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.components_goal_unmeasured)

    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .dashedSlot(edge),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.components_goal_no_number),
            style = MaterialTheme.typography.labelMedium,
            color = edge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // The dashed outline already carries the whole meaning for a sighted
            // reader and the semantics block above carries it for everyone else;
            // letting the words speak too would have TalkBack read them twice.
            modifier = Modifier
                .semantics { contentDescription = "" }
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * The dashed hollow rectangle both shapes are drawn from.
 *
 * One function rather than two copies, because the whole claim §1.3 makes is
 * that these are **the same object at two sizes** — and two `drawBehind` blocks
 * drift the moment one of them is tuned. `Modifier.border` has no dashed form,
 * which is why this is a draw call at all; `dashPathEffect` needs a `Stroke`
 * rather than a `Fill`, which is also what guarantees the hollow centre
 * structurally: there is no draw call here that fills anything.
 */
private fun Modifier.dashedSlot(edge: Color): Modifier =
    this.drawBehind {
        val stroke = 1.5.dp.toPx()
        val radius = 4.dp.toPx()
        val inset = stroke / 2f
        drawRoundRect(
            color = edge,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(2.5.dp.toPx(), 2.dp.toPx()),
                ),
            ),
        )
    }

/**
 * The marker, or nothing at all.
 *
 * The condition in one place rather than at every list that renders a goal —
 * §1.3's population is `measure == null`, which is *absence is the default*, and
 * deliberately **not** [com.idomarhaim.goalpilot.domain.model.Goal.hasMeasure].
 * A goal carrying a word and a zero target has already answered this question;
 * marking it unmeasured would state something false.
 *
 * It renders the [UnmeasuredChip], not the bare square: every caller is a **list
 * row**, which is precisely the placement that has no sentence beside it. The
 * two hero call sites reach for [UnmeasuredMarker] directly and always did.
 *
 * Eligibility for the **offer** is a different and stricter question
 * ([com.idomarhaim.goalpilot.domain.usecase.ProposeMeasureUseCase.isEligible]) and
 * is not asked here on purpose: the marker reports the absence whether or not the
 * app has anything to suggest about it, which is what the prototype's own caption
 * says — *"nothing is owed and nothing is offered here."*
 */
@Composable
fun UnmeasuredMarkerIfNeeded(
    measureIsAbsent: Boolean,
    modifier: Modifier = Modifier,
) {
    if (measureIsAbsent) UnmeasuredChip(modifier = modifier)
}
