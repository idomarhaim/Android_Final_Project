package com.idomarhaim.goalpilot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsProperties
import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiKeyFailure
import com.idomarhaim.goalpilot.domain.model.AiProvider
import com.idomarhaim.goalpilot.feature.settings.AiCard
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_ADD
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_CONSEQUENCE
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_DELETE
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_DELETE_CONFIRM
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_EDITOR
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_KEY_FIELD
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_KEY_MASK
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_MODEL
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_MODEL_FIELD
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_PROVIDER
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_REPLACE
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_SAVE
import com.idomarhaim.goalpilot.feature.settings.TAG_AI_STATUS
import com.idomarhaim.goalpilot.feature.settings.providerTag
import com.idomarhaim.goalpilot.ui.theme.GoalPilotTheme
import org.junit.Rule
import org.junit.Test

/**
 * §4.9's AI section on a device — **the masked field and the replace action**,
 * which is `#54`'s instrumented exit criterion.
 *
 * ### Why these need a device and the routing tests do not
 *
 * `AiProviderRoutingTest` answers *which credential is chosen*, on the JVM,
 * against the shipping repository. What it cannot answer is whether the key is
 * ever **on the screen**. That is a rendering fact, and the two ways it can go
 * wrong are both invisible to a JVM test:
 *
 * * a `TextField` that shows the stored key when you open the editor to replace
 *   it — the shape *"Reveal is a feature request, not a default"* forbids;
 * * a masked row that renders the mask correctly and is read out in full by the
 *   accessibility tree, which is what `assertKeyIsNowhereInTheTree` checks.
 *
 * The second is the one nothing else catches. A screenshot looks perfect.
 *
 * ⚠️ Every key here is obviously fake and clearly marked (`#54`).
 */
class AiSectionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeKey = "FAKE-KEY-NOT-A-REAL-KEY-9876"

    private fun setContent(
        initial: AiCredential? = null,
        lastAnswer: AiAnswer? = null,
    ) {
        composeRule.setContent {
            var credential by remember { mutableStateOf(initial) }
            GoalPilotTheme {
                AiCard(
                    credential = credential,
                    lastAnswer = lastAnswer,
                    onSave = { credential = it },
                    onClear = { credential = null },
                )
            }
        }
    }

    /**
     * The rendered text of a tagged node.
     *
     * `SemanticsProperties.Text` is a **list** of `AnnotatedString`, so the
     * `.first { it.key.name == "Text" }.value.toString()` idiom next door in
     * `SettingsScreenTest` yields `[the text]` — brackets and all. Every
     * assertion there is a `contains`, so it has never mattered; here the
     * assertions are `isEqualTo` and `endsWith`, and it did. Unwrapped properly
     * rather than trimmed, so the mask's own characters are what is compared.
     */
    private fun textOf(tag: String): String =
        composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString("") { it.text }

    /**
     * The key must not appear **anywhere** in the semantics tree — not as text,
     * not as a content description, not in an editable field's value.
     *
     * `printToString` with an unbounded depth is what makes this a real sweep
     * rather than a check of the nodes this test happened to think of, and the
     * accessibility tree is the surface a screenshot cannot show.
     */
    private fun assertKeyIsNowhereInTheTree() {
        // onAllNodes(isRoot()), not onRoot(): a ModalBottomSheet and a Dialog
        // each render in their OWN WINDOW, so while the editor is open there are
        // two roots. `onRoot()` throws on that, which is how this was caught —
        // but the dangerous version is the one that picks a root and passes,
        // because the sheet is precisely the surface a key could be shown on.
        val tree = composeRule.onAllNodes(isRoot(), useUnmergedTree = true)
            .printToString(maxDepth = Int.MAX_VALUE)
        assertThat(tree).doesNotContain(fakeKey)
        // Not even a fragment long enough to be useful. The mask keeps the last
        // four characters on purpose, so the assertion is on the head.
        assertThat(tree).doesNotContain("FAKE-KEY-NOT-A-REAL")
    }

    // ── with no key: one action, and a row that still speaks ───────

    @Test
    fun withNoKeyTheSectionOffersOneActionAndNoInertControls() {
        setContent()

        composeRule.onNodeWithTag(TAG_AI_ADD).assertIsDisplayed()
        // A provider picker with no key behind it would be a control that
        // changes nothing — the exact thing #48 refused to render, and #53 had
        // to ship before Appearance could offer material tiles.
        composeRule.onNodeWithTag(TAG_AI_PROVIDER).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_AI_MODEL).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_AI_KEY_MASK).assertDoesNotExist()

        assertThat(textOf(TAG_AI_STATUS)).contains("free model")
        assertThat(textOf(TAG_AI_CONSEQUENCE)).contains("works without a key")
    }

    // ── adding a key ───────────────────────────────────────────────

    @Test
    fun addingAKeyMasksItImmediatelyAndNeverShowsIt() {
        setContent()

        composeRule.onNodeWithTag(TAG_AI_ADD).performClick()
        composeRule.onNodeWithTag(TAG_AI_EDITOR).assertIsDisplayed()
        composeRule.onNodeWithTag(providerTag(AiProvider.ANTHROPIC)).performClick()
        composeRule.onNodeWithTag(TAG_AI_KEY_FIELD).performTextInput(fakeKey)
        composeRule.onNodeWithTag(TAG_AI_SAVE).performClick()

        composeRule.onNodeWithTag(TAG_AI_KEY_MASK).assertIsDisplayed()
        val mask = textOf(TAG_AI_KEY_MASK)
        assertThat(mask).endsWith("9876")
        assertThat(mask).doesNotContain("FAKE")
        assertThat(textOf(TAG_AI_PROVIDER)).isEqualTo(AiProvider.ANTHROPIC.displayName)

        assertKeyIsNowhereInTheTree()
    }

    @Test
    fun aBlankModelRendersTheProvidersDefaultRatherThanNothing() {
        setContent()

        composeRule.onNodeWithTag(TAG_AI_ADD).performClick()
        composeRule.onNodeWithTag(providerTag(AiProvider.GEMINI)).performClick()
        composeRule.onNodeWithTag(TAG_AI_KEY_FIELD).performTextInput(fakeKey)
        composeRule.onNodeWithTag(TAG_AI_SAVE).performClick()

        assertThat(textOf(TAG_AI_MODEL)).isEqualTo(AiProvider.GEMINI.defaultModel)
    }

    /**
     * The regression test for the defect this suite found: **Save must be on
     * screen the moment the editor opens.**
     *
     * It was at `y=3033px` on a `2992px` screen, so pressing it hit the scrim,
     * dismissed the sheet and discarded the typed key — with no exception
     * anywhere and a screenshot of the sheet's top that looked perfect. The
     * assertion is `assertIsDisplayed`, which is exactly the property that was
     * false: the node existed and had a size the whole time.
     */
    @Test
    fun theSaveButtonIsOnScreenAsSoonAsTheEditorOpens() {
        setContent()

        composeRule.onNodeWithTag(TAG_AI_ADD).performClick()

        composeRule.onNodeWithTag(TAG_AI_SAVE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_AI_KEY_FIELD).assertIsDisplayed()
    }

    @Test
    fun saveIsRefusedWithNoKeyTyped() {
        setContent()
        composeRule.onNodeWithTag(TAG_AI_ADD).performClick()

        // A blank key is not a credential, and #54's "a real delete" has its own
        // action — saving nothing must not become a way to half-delete.
        composeRule.onNodeWithTag(TAG_AI_SAVE).assertIsNotEnabled()
    }

    // ── the replace action ─────────────────────────────────────────

    @Test
    fun replaceOpensAnEmptyFieldRatherThanTheStoredKey() {
        setContent(AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey))

        composeRule.onNodeWithTag(TAG_AI_REPLACE).performClick()
        composeRule.onNodeWithTag(TAG_AI_EDITOR).assertIsDisplayed()

        // The property #54 states as *"Reveal is a feature request, not a
        // default"*: there is no code path in this app that puts the stored key
        // into a TextField, so the editor cannot be a reveal in disguise.
        assertKeyIsNowhereInTheTree()
    }

    @Test
    fun replacingTheKeyChangesTheMaskedTail() {
        setContent(AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey))
        assertThat(textOf(TAG_AI_KEY_MASK)).endsWith("9876")

        composeRule.onNodeWithTag(TAG_AI_REPLACE).performClick()
        composeRule.onNodeWithTag(TAG_AI_KEY_FIELD).performTextInput("FAKE-KEY-NOT-A-REAL-KEY-1111")
        composeRule.onNodeWithTag(TAG_AI_SAVE).performClick()

        // The tail is the whole reason four characters survive the mask: it is
        // what lets a user tell "the key I pasted" from "the key I meant to".
        assertThat(textOf(TAG_AI_KEY_MASK)).endsWith("1111")
    }

    @Test
    fun editingOnlyTheModelKeepsTheKeyWithoutRetypingIt() {
        setContent(AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey))

        composeRule.onNodeWithTag(TAG_AI_REPLACE).performClick()
        composeRule.onNodeWithTag(TAG_AI_MODEL_FIELD).performTextInput("gpt-4o")
        composeRule.onNodeWithTag(TAG_AI_SAVE).performClick()

        assertThat(textOf(TAG_AI_MODEL)).contains("gpt-4o")
        // Still set, still masked, still never rendered.
        assertThat(textOf(TAG_AI_KEY_MASK)).endsWith("9876")
        assertKeyIsNowhereInTheTree()
    }

    @Test
    fun switchingProviderClearsTheModelSoOneProvidersIdCannotBeSentToAnother() {
        setContent(AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey))

        composeRule.onNodeWithTag(TAG_AI_REPLACE).performClick()
        composeRule.onNodeWithTag(providerTag(AiProvider.GROQ)).performClick()
        composeRule.onNodeWithTag(TAG_AI_SAVE).performClick()

        // A GROQ id sent to Anthropic is a silent failure, which is exactly what
        // #32 §3's four named adapters exist to make impossible.
        assertThat(textOf(TAG_AI_PROVIDER)).isEqualTo(AiProvider.GROQ.displayName)
        assertThat(textOf(TAG_AI_MODEL)).isEqualTo(AiProvider.GROQ.defaultModel)
    }

    // ── the delete ─────────────────────────────────────────────────

    @Test
    fun removingTheKeyIsConfirmedAndReturnsToTheProxyPath() {
        setContent(AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey))

        composeRule.onNodeWithTag(TAG_AI_DELETE).performClick()
        // Unrecoverable here, and the user may not have the key anywhere else.
        composeRule.onNodeWithText("Remove your API key?").assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_AI_DELETE_CONFIRM).performClick()

        composeRule.onNodeWithTag(TAG_AI_KEY_MASK).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_AI_ADD).assertIsDisplayed()
        assertThat(textOf(TAG_AI_STATUS)).contains("not added a key")
        assertKeyIsNowhereInTheTree()
    }

    @Test
    fun keepingTheKeyLeavesItExactlyWhereItWas() {
        setContent(AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey))

        composeRule.onNodeWithTag(TAG_AI_DELETE).performClick()
        composeRule.onNodeWithText("Keep it").performClick()

        assertThat(textOf(TAG_AI_KEY_MASK)).endsWith("9876")
    }

    // ── §5's permanent status row, on the device ───────────────────

    @Test
    fun theStatusRowNamesADeadKeyRatherThanGoingQuiet() {
        setContent(
            AiCredential(AiProvider.OPENAI, "", fakeKey),
            lastAnswer = AiAnswer.Proxy(AiKeyFailure.DEAD),
        )

        // The whole reason §5 required a permanent row: a revoked key silently
        // riding the free model is indistinguishable from one that works.
        val status = textOf(TAG_AI_STATUS)
        assertThat(status).contains("OpenAI")
        assertThat(status).contains("rejected")
    }

    @Test
    fun theStatusRowNamesTheProviderThatAnswered() {
        setContent(
            AiCredential(AiProvider.GEMINI, "", fakeKey),
            lastAnswer = AiAnswer.UserKey(AiProvider.GEMINI),
        )

        assertThat(textOf(TAG_AI_STATUS)).isEqualTo("Answered by your Google Gemini key.")
    }
}
