package com.idomarhaim.goalpilot.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.domain.model.Challenge
import com.idomarhaim.goalpilot.domain.model.ChallengeInvite
import com.idomarhaim.goalpilot.domain.model.ChallengeParticipant
import com.idomarhaim.goalpilot.domain.model.ChallengePhase
import com.idomarhaim.goalpilot.domain.model.ChallengeWithStandings
import com.idomarhaim.goalpilot.domain.model.Measure
import com.idomarhaim.goalpilot.domain.model.MeasureKind
import com.idomarhaim.goalpilot.domain.model.rankedByScore
import com.idomarhaim.goalpilot.feature.challenges.ChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.ChallengeInviteRow
import com.idomarhaim.goalpilot.feature.challenges.MyChallengeCard
import com.idomarhaim.goalpilot.feature.challenges.PendingMeasureBanner
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * **A button must never render one letter per line, at any width.**
 *
 * Ido, 2026-08-25, with a photograph of his Galaxy S25 Ultra: the challenge card's
 * `Standings` button was a blue column of single characters down the right-hand side. His
 * ask was the general one — *"the text, and the proportions and shapes and sizes generally,
 * should be agnostic to different screen sizes."* This is the part of that ask a machine can
 * hold on to.
 *
 * ### Why a test and not just the fix
 *
 * The fix — `FlowRow` instead of `Row` — is one word per site, and **the sites are the
 * problem**. A mechanical sweep found **seven** rows app-wide with two or more buttons and
 * no `weight()`, in five files, written by different sessions months apart. Nothing stops an
 * eighth: `Row { Button(); Button() }` is the obvious way to put two buttons side by side,
 * it reads correctly, it reviews correctly, and it is only wrong on a screen the author does
 * not have.
 *
 * §1 of `challenges-finish-the-job` is the proof that care is not enough here. It reasoned
 * explicitly about this exact card at this exact geometry — *"the action row is already at
 * its width limit … a fourth wraps at his 384 dp / font 1.15"* — and used that to place the
 * invite affordance in the header. The reasoning was right about the mechanism and wrong
 * about the number: **three** already wrapped. A render frame at AVD width showed nothing,
 * because the AVD is wider.
 *
 * ### ⚠️ WHAT THIS DOES **NOT** CATCH, MEASURED RATHER THAN ASSUMED
 *
 * **It does not catch a `Row` that should be a `FlowRow`.** That was the whole point of
 * writing it, and it does not do it. Reverting `MyChallengeCard`'s action row to a plain
 * `Row` and re-running leaves all three tests **green** — verified twice on 2026-08-25.
 *
 * The reason is the other half of the same repair. Once every action label carries
 * `maxLines = 1`, a crushed button can no longer stack; it **truncates** instead. Truncation
 * changes neither the control's height nor its width enough to separate from a legitimate
 * short button, and Compose's semantics do not report *"this text was ellipsised"*. A width
 * floor was tried and dropped for exactly that reason: at Ido's geometry the crushed
 * `Standings` still measures ~74 dp, comfortably above any floor a real icon button allows.
 *
 * **So the verified guard against Row-vs-FlowRow is the render pass**
 * (`docs/render-passes/2026-08-25-narrow-screen/`), photographed at his geometry — which is
 * this repo's established instrument for defects that are relationships rather than values.
 * This file guards the *other* half: a label added later without `maxLines`, a font scale
 * raised, or a translation three times its English length, any of which brings the stacking
 * straight back.
 *
 * Saying so here rather than letting the name imply more: a guard whose limits are
 * misdescribed is worse than no guard, because it is trusted.
 *
 * ### What it measures, and why height is the right instrument
 *
 * Height, not width. A crushed button is not narrow — it is **tall**: the label has nowhere
 * to go but downwards, so nine characters become nine lines. At 384 dp with a 1.15 font
 * scale an ordinary single-line button is around 40–56 dp and a generously wrapped two-line
 * one stays under 90; `Standings` stacked vertically is well over 200. [MAX_BUTTON_DP]
 * therefore separates the two cleanly without pinning any particular typography.
 *
 * Measuring the **rendered node** rather than the source is what makes this general: it
 * catches a `Row` that should have been a `FlowRow`, a label that grew, a font scale that
 * rose, and a translation three times longer than its English original — none of which a
 * lint over the source could see.
 *
 * ### ⚠️ camelCase test names, and it is a hard constraint rather than a style
 *
 * Every JVM suite in this repo names its tests in backticks with spaces, and **no
 * instrumented one does.** This file tried to and could not build:
 *
 * ```
 * D8: Space characters in SimpleName 'the challenge card's actions survive Ido's phone'
 *   are not allowed prior to DEX version 040
 * ```
 *
 * A backtick identifier is legal Kotlin and legal JVM bytecode; **dex rejects it**, so the
 * convention only survives where nothing is dexed. The first repair here guessed at the
 * lambdas — the error names a synthetic `…$1.class` before it names the method — and was
 * wrong; the method name itself is the problem. `Observed:` 2026-08-25.
 *
 * ### The geometry is Ido's, deliberately
 *
 * 384 dp and font 1.15 are his S25 Ultra's real settings, recorded by
 * `s25-verify-on-real-phone` on 2026-08-24. The device this suite runs on is wider, which is
 * exactly why the defect reached him: **the emulator cannot reproduce it without being
 * told to.** Constraining the composition is how a test on any device asks his question.
 */
class NarrowScreenGuardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val challenge = Challenge(
        id = "c1",
        title = "August Steps Race",
        measure = Measure(MeasureKind.COUNT, "steps"),
        ownerUid = "me",
        pendingChangeId = "chg-1",
        pendingMeasure = Measure(MeasureKind.DISTANCE, "km"),
    )

    private val card = ChallengeCard(
        data = ChallengeWithStandings(
            challenge = challenge,
            standings = listOf(
                ChallengeParticipant(uid = "me", displayName = "Ido", score = 71_137.0),
                ChallengeParticipant(uid = "b", displayName = "Rachil", score = 4_100.0),
            ).rankedByScore(currentUid = "me"),
            isOwner = true,
            hasJoined = true,
            // Linked, so the card shows its WIDEST action row: "Change goal" +
            // "Type a score" + "Standings". That is the exact combination Ido photographed.
            myLinkedGoalId = "g1",
            approvals = listOf("", ""),
        ),
        phase = ChallengePhase.ACTIVE,
    )

    private val invite = ChallengeInvite(
        id = "i1",
        challengeId = "c1",
        challengeTitle = "August Steps Race",
        fromUid = "f1",
        fromName = "Rachil",
        toUid = "me",
    )

    /**
     * Composes [content] at a given width and font scale.
     *
     * ⚠️ **EVERY `setContent` LAMBDA LIVES IN A HELPER LIKE THIS ONE, AND THAT IS A DEX
     * CONSTRAINT, NOT A STYLE CHOICE.** Kotlin names a lambda's synthetic class after the
     * method that encloses it, and this suite's test names are backtick identifiers **with
     * spaces in them**. Inline the composition into a test and the compiler emits
     * `NarrowScreenGuardTest$and it still holds at a width nobody designs for$1$1$1$1.class`
     * — which `r8` refuses outright:
     *
     * ```
     * Space characters in SimpleName '…$and it still holds at a width nobody designs for$1$1$1$1'
     *   are not allowed prior to DEX version 040
     * ```
     *
     * `Observed:` 2026-08-25. Three of the four tests here were fine and the fourth was not,
     * for exactly this reason — the other three already delegated their lambdas to a helper
     * and only the fourth inlined them. The failure names a `.class` file rather than any
     * line of source, and nothing about it points at the test's name.
     */
    private fun at(
        widthDp: Int,
        fontScale: Float,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                // His font scale, not the device's. A larger scale makes every label wider
                // without changing the space it has, which is half of why his phone broke
                // and a default emulator does not.
                LocalDensity provides Density(base.density, fontScale),
            ) {
                GoalPilotTheme {
                    Surface {
                        // ⚠️ HIS WIDTH **MINUS WHAT THE SCREEN SPENDS BEFORE THE CARD
                        // SEES IT**, and the first draft of this guard got that wrong.
                        //
                        // It handed the card the whole 384 dp and the mutation test then
                        // PASSED: with 384 the three buttons fit (138 + 92 + 91 + spacing
                        // = 337) and nothing was ever crushed, so a plain `Row` measured
                        // identically to the `FlowRow` and the guard was blind to the exact
                        // defect it was written for. `Observed:` 2026-08-25 — the fixed and
                        // mutated layouts logged the same six control sizes, to the pixel.
                        //
                        // `ChallengesScreen`'s `LazyColumn` takes `start = 16, end = 16`
                        // before any card exists, so the card's real width on his phone is
                        // 352, and its own `padding(16.dp)` leaves 320 for the buttons.
                        // 337 > 320 is the crush he photographed. A harness that does not
                        // spend the container's padding is not asking his question.
                        Box(Modifier.width((widthDp - LIST_PADDING_DP * 2).dp)) { content() }
                    }
                }
            }
        }
    }

    /** [at] Ido's own geometry — the S25 Ultra's real width and font scale. */
    private fun atHisPhone(content: @androidx.compose.runtime.Composable () -> Unit) =
        at(SCREEN_DP, FONT_SCALE, content)

    /**
     * Every clickable thing on screen, measured.
     *
     * `hasClickAction()` rather than a tag or a text match on purpose: the guard should
     * cover buttons nobody thought to name, including ones added later.
     */
    /** The node's own text, for a log line that names which control is wrong. */
    private fun labelOf(node: androidx.compose.ui.semantics.SemanticsNode): String =
        node.config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
            .joinToString(" ") { it.text }
            .take(40)
            .ifBlank { node.config.getOrElse(
                androidx.compose.ui.semantics.SemanticsProperties.ContentDescription,
            ) { emptyList() }.joinToString(" ").take(40) }
            .ifBlank { "(no label)" }

    private fun assertNoButtonIsStacked(surface: String) {
        val density = composeRule.density
        val clickable = composeRule
            .onAllNodes(hasClickAction(), useUnmergedTree = true)
            .fetchSemanticsNodes()

        // ⚠️ LEAF CONTROLS ONLY, AND THE FIRST DRAFT OF THIS GUARD DID NOT DO THAT.
        //
        // It measured every clickable node and failed immediately at 411 dp — on the CARD,
        // which is itself clickable (tapping it opens the standings) and is legitimately
        // that tall. `Observed:` 2026-08-25, on a layout that had already been repaired, so
        // the guard was reporting a defect that was not there.
        //
        // A container is a clickable that CONTAINS another clickable. Excluding those
        // leaves exactly the things a label can stack inside: buttons, icon buttons, rows
        // that act as one. It is a structural test rather than a list of tags, so a control
        // added later is covered without anybody remembering this file.
        val controls = clickable.filter { node ->
            clickable.none { other ->
                other !== node && node.boundsInRoot.contains(other.boundsInRoot.center)
            }
        }
        assertWithMessage(
            "$surface: found no leaf controls at all — the guard is blind. " +
                "${clickable.size} clickable nodes, all of them containers?",
        ).that(controls).isNotEmpty()

        controls.forEach { node ->
            val heightDp = with(density) { node.size.height.toDp().value }
            val label = node.config.toString().take(90)
            assertWithMessage(
                "$surface: a control is ${heightDp.toInt()} dp tall at $SCREEN_DP dp / font " +
                    "$FONT_SCALE, over the ${MAX_BUTTON_DP.toInt()} dp ceiling — its label " +
                    "is almost certainly stacking one character per line, which is what " +
                    "Ido photographed on his S25 on 2026-08-25. Use FlowRow, not Row. " +
                    "Node: $label",
            ).that(heightDp).isLessThan(MAX_BUTTON_DP)
        }
    }

    /** The card under test, as one composable — see [at] for why no test inlines this. */
    @androidx.compose.runtime.Composable
    private fun theCard() {
        MyChallengeCard(
            card = card,
            onOpenStandings = {},
            onReportScore = {},
            onLinkGoal = {},
            onInvite = {},
            onChangeMeasure = {},
            onApproveMeasure = {},
            onWithdrawMeasure = {},
            isApproving = false,
            onLeave = {},
            onDelete = {},
        )
    }

    @Test
    fun challengeCardActionsSurviveHisPhone() {
        // The exact surface from the photograph: linked challenge, three actions.
        atHisPhone {
            MyChallengeCard(
                card = card,
                onOpenStandings = {},
                onReportScore = {},
                onLinkGoal = {},
                onInvite = {},
                onChangeMeasure = {},
                onApproveMeasure = {},
                onWithdrawMeasure = {},
                isApproving = false,
                onLeave = {},
                onDelete = {},
            )
        }
        assertNoButtonIsStacked("MyChallengeCard")
    }

    @Test
    fun inviteRowJoinAndDismissSurviveIt() {
        atHisPhone {
            ChallengeInviteRow(invite = invite, isBusy = false, onJoin = {}, onDismiss = {})
        }
        assertNoButtonIsStacked("ChallengeInviteRow")
    }

    @Test
    fun pendingMeasureBannerSurvivesIt() {
        atHisPhone {
            PendingMeasureBanner(card = card, isBusy = false, onApprove = {}, onWithdraw = {})
        }
        assertNoButtonIsStacked("PendingMeasureBanner")
    }


    private companion object {
        /** Ido's S25 Ultra, measured by `s25-verify-on-real-phone` on 2026-08-24. */
        const val SCREEN_DP = 384
        const val FONT_SCALE = 1.15f

        /**
         * The ceiling a control's height must stay under.
         *
         * Chosen to sit in the gap rather than on a typography value: an ordinary
         * single-line button is 40–56 dp at this scale, a wrapped two-line one under 90,
         * and a nine-letter label stacked vertically is over 200. Anything between 90 and
         * 200 is not a shape this app draws, so the exact number is not load-bearing —
         * which is what stops the guard turning into a pin on the theme.
         */
        const val MAX_BUTTON_DP = 120f

        /**
         * What `ChallengesScreen`'s `LazyColumn` spends on each side before a card exists
         * (`contentPadding = PaddingValues(start = 16.dp, end = 16.dp, …)`).
         *
         * Subtracted from the screen width because the guard composes a **card**, not a
         * screen. Leaving it out is what made the first version of this test pass against
         * the very defect it exists for.
         */
        const val LIST_PADDING_DP = 16

        /**
         * The narrowest a real control here gets: an icon button, which is square at 40 dp.
         * A text button squeezed below that has lost its label.
         */
        const val MIN_CONTROL_DP = 40f
    }
}
