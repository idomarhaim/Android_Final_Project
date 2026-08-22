package com.idomarhaim.goalpilot.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idomarhaim.goalpilot.R

/**
 * The **marker** — spec §1.3 (`C22`
 * [#44](https://github.com/idomarhaim/Android_Final_Project/issues/44),
 * [#65](https://github.com/idomarhaim/Android_Final_Project/issues/65)).
 *
 * > **It is two surfaces, not one.** A **marker** — a dashed **square**, no words,
 * > no buttons — wherever the goal is listed; and the **offer** itself **only on
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
 * placement, and it is why this file has no `onClick`, no label and no action.
 *
 * ## It is a SQUARE, and the shape is load-bearing
 *
 * Revision 2 of the prototype drew it as a dashed **circle** carrying `#`, and
 * beside `C19`'s dashed circle carrying `+` on the row below, the two invitations
 * read as the same thing at a glance — one chip carrying two axes. Every circle
 * in this app's language is an occurrence or an outcome, so a number slot is a
 * square: **distinguished by form, not by hue**, which is what keeps it legible
 * in dark mode, in every material, and in greyscale. Caught only once rendered.
 *
 * ## Dashed and hollow, always
 *
 * §1.3: the offer *"is drawn dashed and hollow throughout, so it can never borrow
 * the visual language of an outcome."* The filled accent means **kept** on
 * `C19`'s screen and must not appear on something the app has not done. There is
 * deliberately no filled variant of this composable and no colour parameter that
 * could make one.
 *
 * ## The one string, and why a silent component has one
 *
 * A screen reader has no square to see. So the marker's two visual claims —
 * *there is no number yet* and *nothing is owed* — are carried by
 * `components_goal_unmeasured`, which is TalkBack-only and never rendered.
 * Without it the marker is genuinely invisible to the one user who most needs a
 * list to be legible, and with a bare `"#"` it announces a punctuation mark.
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
            .drawBehind {
                // Compose's Modifier.border has no dashed form, so the stroke is drawn
                // here. `dashPathEffect` needs a Stroke rather than a Fill, which is
                // also what guarantees the hollow centre structurally: there is no
                // draw call in this component that fills anything.
                val stroke = 1.5.dp.toPx()
                val radius = 4.dp.toPx()
                val inset = stroke / 2f
                drawRoundRect(
                    color = edge,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        this.size.width - stroke,
                        this.size.height - stroke,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2.5.dp.toPx(), 2.dp.toPx()),
                        ),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // Not a translatable string: `#` is a glyph standing for *a number
            // slot*, the same way `+` does on C19's marker beside it. Sending it to
            // res/ would invite a translation of a symbol, and the sweep test's own
            // definition of prose (two or more alphabetic words) correctly ignores it.
            text = "#",
            color = edge.copy(alpha = 0.85f),
            fontSize = (size.value * 0.62f).sp,
            fontWeight = FontWeight.ExtraBold,
            // The square already carries the whole meaning for a sighted reader and
            // the semantics block above carries it for everyone else; letting the
            // glyph speak too would have TalkBack read "number sign".
            modifier = Modifier.semantics { contentDescription = "" },
        )
    }
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
    size: Dp = 16.dp,
) {
    if (measureIsAbsent) UnmeasuredMarker(modifier = modifier, size = size)
}
