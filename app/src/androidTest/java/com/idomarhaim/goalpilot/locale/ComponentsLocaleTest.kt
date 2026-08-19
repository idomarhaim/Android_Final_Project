package com.idomarhaim.goalpilot.locale

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.idomarhaim.goalpilot.core.util.Bidi
import com.idomarhaim.goalpilot.domain.model.AppLanguage
import com.idomarhaim.goalpilot.domain.model.AppSkin
import com.idomarhaim.goalpilot.domain.model.Goal
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.GoalCard
import com.idomarhaim.goalpilot.ui.components.SkinPicker
import com.idomarhaim.goalpilot.ui.locale.AppLocale
import org.junit.Rule
import org.junit.Test

/**
 * Renders `ui/components/` in Hebrew and reads what came out — issue #51's
 * sweep for the shared package.
 *
 * ### Why this is instrumented and not a JVM test
 *
 * `AnalyticsLiteralSweepTest` proves no English literal is *left in the source*
 * and `HebrewLocaleResourceTest` proves the two resource buckets *match*. Both
 * are satisfied by a sweep that moved every string to `res/` and then wired the
 * wrong key, or that isolated nothing. Neither can see a rendered frame, and the
 * two defects this package actually had are visible only there.
 *
 * ### The trap this class is built around
 *
 * `AppLocaleDialogTest` pinned the rule that **correct RTL mirroring is not
 * evidence that the strings are localized** — direction and language ride
 * different rails. So no test here concludes anything from direction alone:
 * [aMirroredCardIsNotEvidenceOfATranslatedOne] measures both halves off the same
 * frame, and every other test asserts on the words.
 *
 * A second, quieter half of the same trap belongs to this package specifically:
 * a card can be in perfect Hebrew *and* still be wrong, because a Latin-digit
 * run inside an RTL paragraph is re-ordered by the bidi algorithm — `5/10`
 * renders as `10/5`. That is not a missing translation and no amount of reading
 * the resource file shows it, so [theMeasureRunIsIsolated] asserts the isolate
 * marks are present in the composed output rather than in the resource.
 */
class ComponentsLocaleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val goal = Goal(
        id = "g1",
        title = "",                       // blank, so the fallback copy renders
        category = GoalCategory.LEARNING,
        currentValue = 5.0,
        targetValue = 10.0,
        unit = "hours",                   // user-authored (§8): never translated
    )

    // ------------------------------------------------------------------ Hebrew

    @Test
    fun goalCardSpeaksHebrew() {
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                GoalCard(goal = goal, onClick = {})
            }
        }
        composeRule.waitForIdle()

        val texts = renderedTexts()

        assertWithMessage("the blank-title fallback, and it must say יעד not מטרה")
            .that(texts).contains(HEBREW_UNTITLED)
        assertWithMessage("the category label, resolved through ComponentStrings")
            .that(texts.any { it.contains(HEBREW_LEARNING) }).isTrue()
        assertWithMessage("no English may survive into a Hebrew render")
            .that(texts.none { it.contains("Untitled") || it.contains("Learning") })
            .isTrue()
    }

    @Test
    fun theMeasureRunIsIsolated() {
        // §4.8, and the reason this cannot be checked by reading the XML: the
        // isolate marks are added by the CALL SITE, not by the resource. The
        // ratio must be ONE isolate — isolating the two numbers separately would
        // still let the slash migrate.
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                GoalCard(goal = goal, onClick = {})
            }
        }
        composeRule.waitForIdle()

        val meta = renderedTexts().firstOrNull { Bidi.strip(it).contains("5/10") }
        assertWithMessage("no rendered text carried the measure at all").that(meta).isNotNull()

        assertWithMessage(
            "the current/target run is not direction-isolated, so `5/10` renders as " +
                "`10/5` on a Hebrew device — see core/util/Bidi.kt",
        ).that(meta).contains("${Bidi.FSI}5/10${Bidi.PDI}")

        assertWithMessage("the user-authored unit is isolated too — its script is unknown here")
            .that(meta).contains("${Bidi.FSI}hours${Bidi.PDI}")
    }

    @Test
    fun thePercentageIsIsolated() {
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                GoalCard(goal = goal, onClick = {})
            }
        }
        composeRule.waitForIdle()

        assertThat(renderedTexts()).contains("${Bidi.FSI}50%${Bidi.PDI}")
    }

    @Test
    fun skinPickerSpeaksHebrew() {
        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                SkinPicker(selected = AppSkin.DEFAULT, onSelect = {})
            }
        }
        composeRule.waitForIdle()

        val texts = renderedTexts()

        assertWithMessage("skin names come from res/ now, not from the AppSkin enum")
            .that(texts).containsAtLeast(HEBREW_AURORA, HEBREW_BLOSSOM)
        assertWithMessage("the enum's old English constructor arguments must be gone")
            .that(texts.none { it.contains("Aurora") || it.contains("Ocean blue") })
            .isTrue()
    }

    @Test
    fun aMirroredCardIsNotEvidenceOfATranslatedOne() {
        // The 51d rule, re-asserted at this layer: both halves off one frame,
        // because it is their COMBINATION that misleads a reviewer. Here they
        // agree — which is the point. The assertion that matters is the second;
        // the first is what a reader would have stopped at.
        var direction: LayoutDirection? = null

        composeRule.setContent {
            AppLocale(language = AppLanguage.HEBREW) {
                direction = LocalLayoutDirection.current
                GoalCard(goal = goal, onClick = {})
            }
        }
        composeRule.waitForIdle()

        assertWithMessage("the card mirrors — the half that proves nothing")
            .that(direction).isEqualTo(LayoutDirection.Rtl)
        assertWithMessage("…and speaks Hebrew — the half that had to be measured separately")
            .that(renderedTexts()).contains(HEBREW_UNTITLED)
    }

    // ----------------------------------------------------------------- English

    @Test
    fun englishIsUnaffected() {
        // A sweep that breaks the language nobody was worried about is still a
        // regression, and no Hebrew assertion above would notice.
        composeRule.setContent {
            AppLocale(language = AppLanguage.ENGLISH) {
                GoalCard(goal = goal, onClick = {})
            }
        }
        composeRule.waitForIdle()

        val texts = renderedTexts()
        assertThat(texts).contains("Untitled goal")
        assertThat(texts.any { it.contains("Learning") }).isTrue()
        assertWithMessage("§4.8: no Hebrew literal may survive into an English render")
            .that(texts.none { HEBREW.containsMatchIn(it) }).isTrue()
    }

    @Test
    fun theIsolatesAreInvisibleToTheReader() {
        // Isolate marks are zero-width formatting characters, so the English
        // render must be byte-identical to the unisolated string once they are
        // stripped. This is what stops the fix from becoming a visible artefact.
        composeRule.setContent {
            AppLocale(language = AppLanguage.ENGLISH) {
                GoalCard(goal = goal, onClick = {})
            }
        }
        composeRule.waitForIdle()

        val meta = renderedTexts().first { Bidi.strip(it).contains("5/10") }
        assertThat(Bidi.strip(meta)).isEqualTo("Learning • 5/10 hours")
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Every string the composition actually produced — `Text` content and
     * `contentDescription` alike.
     *
     * Read off the semantics tree rather than asserted with `onNodeWithText`,
     * because the isolate marks are part of the string: a matcher spelled with
     * them is unreadable, and one spelled without them silently passes on the
     * unfixed output, which is the defect.
     */
    private fun renderedTexts(): List<String> {
        val out = mutableListOf<String>()
        fun walk(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { out += it }
            node.children.forEach(::walk)
        }
        walk(composeRule.onRoot().fetchSemanticsNode())
        return out
    }

    private companion object {
        const val HEBREW_UNTITLED = "יעד ללא שם"
        const val HEBREW_LEARNING = "לימודים"
        const val HEBREW_AURORA = "זוהר"
        const val HEBREW_BLOSSOM = "פריחה"
        val HEBREW = Regex("""\p{IsHebrew}""")
    }
}
