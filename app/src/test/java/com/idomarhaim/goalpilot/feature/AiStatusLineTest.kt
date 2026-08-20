package com.idomarhaim.goalpilot.feature

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiKeyFailure
import com.idomarhaim.goalpilot.domain.model.AiProvider
import com.idomarhaim.goalpilot.feature.settings.aiConsequenceLine
import com.idomarhaim.goalpilot.feature.settings.aiStatusLine
import org.junit.Test

/**
 * §4.9's AI status line, in every state it can reach (#54 piece 3, #32 §5).
 *
 * `SettingsContent`'s KDoc states the rule this suite obeys: §4.9's consequence
 * lines are **arithmetic rendered as a sentence**, and *"the only way to catch
 * one that silently stops moving with its setting is to move the setting and
 * read the sentence"*. So every test here moves an input and reads the output.
 *
 * The property that matters most is **negative** and is asserted as such: no
 * state may claim that something answered when it did not. A line that
 * over-claims is worse than no line, because the whole reason §5 required a
 * permanent row is that a user otherwise cannot tell a working key from a dead
 * one riding the fallback.
 */
class AiStatusLineTest {

    private val key = AiCredential(AiProvider.OPENAI, "", "FAKE-KEY-NOT-A-REAL-KEY-0000")

    // ── no key: the default state of almost every install ──────────

    @Test
    fun `with no key the line says the free model answers and no key is set`() {
        val line = aiStatusLine(credential = null, answer = null)

        assertThat(line).contains("free model")
        assertThat(line).contains("not added a key")
    }

    @Test
    fun `with no key an offline call is reported as offline, not as the free model`() {
        val line = aiStatusLine(credential = null, answer = AiAnswer.Local())

        assertThat(line).contains("could not be reached")
        assertThat(line).contains("offline")
    }

    @Test
    fun `with no key no provider name is ever printed`() {
        // There is no user provider to name, so naming one would be pure
        // invention. Checked against all four rather than the one in `key`.
        for (answer in listOf(null, AiAnswer.Proxy(), AiAnswer.Local())) {
            val line = aiStatusLine(credential = null, answer = answer)
            for (provider in AiProvider.entries) {
                assertThat(line).doesNotContain(provider.displayName)
            }
        }
    }

    // ── a key is set ───────────────────────────────────────────────

    @Test
    fun `a key that has not been used yet does not claim to have answered`() {
        val line = aiStatusLine(key, answer = null)

        assertThat(line).contains("OpenAI")
        assertThat(line).contains("not answered yet")
        // The over-claim this exists to prevent.
        assertThat(line).doesNotContain("Answered by")
    }

    @Test
    fun `a successful user call names the provider that answered`() {
        val line = aiStatusLine(key, AiAnswer.UserKey(AiProvider.OPENAI))

        assertThat(line).isEqualTo("Answered by your OpenAI key.")
    }

    @Test
    fun `each failure class produces a different sentence`() {
        val dead = aiStatusLine(key, AiAnswer.Proxy(AiKeyFailure.DEAD))
        val quota = aiStatusLine(key, AiAnswer.Proxy(AiKeyFailure.QUOTA))
        val transient = aiStatusLine(key, AiAnswer.Proxy(AiKeyFailure.TRANSIENT))

        // Three classes, three sentences. The §5 table's whole point is that
        // they mean different things to the user, so collapsing any two of them
        // into one sentence would lose the only information the row carries.
        assertThat(setOf(dead, quota, transient)).hasSize(3)

        // Only the class the user can act on asks them to act.
        assertThat(dead).contains("rejected")
        assertThat(dead).contains("Check the key")
        assertThat(quota).contains("quota")
        assertThat(quota).doesNotContain("Check the key")
        assertThat(transient).doesNotContain("Check the key")
    }

    @Test
    fun `an old deployment reports the free model without accusing the key`() {
        // `Proxy(null)` is what an absent `answeredBy` becomes — a function that
        // predates C13 and ignored the credential. The free model really did
        // answer, and the key was never tried, so the line must not suggest it
        // failed.
        val line = aiStatusLine(key, AiAnswer.Proxy(null))

        assertThat(line).contains("free model answered")
        assertThat(line).doesNotContain("rejected")
        assertThat(line).doesNotContain("quota")
    }

    @Test
    fun `both rungs failing still reports the dead key first`() {
        val line = aiStatusLine(key, AiAnswer.Local(AiKeyFailure.DEAD))

        // Two facts. The one only the user can act on leads.
        assertThat(line.indexOf("rejected")).isLessThan(line.indexOf("free model"))
        assertThat(line).contains("offline guidance")
        assertThat(line).contains("Check the key")
    }

    @Test
    fun `an outage with a healthy key does not blame the key`() {
        val line = aiStatusLine(key, AiAnswer.Local())

        assertThat(line).contains("was not the problem")
        assertThat(line).doesNotContain("rejected")
    }

    @Test
    fun `no line ever says the free model answered when nothing did`() {
        // The single sentence that must never appear on the third rung, in any
        // of its states — it is the §0.3 failure the whole status line exists
        // against, one layer down.
        for (failure in listOf(null) + AiKeyFailure.entries) {
            val line = aiStatusLine(key, AiAnswer.Local(failure))
            assertThat(line).doesNotContain("free model answered")
        }
    }

    @Test
    fun `the provider name follows the credential, not the answer`() {
        val gemini = AiCredential(AiProvider.GEMINI, "", "FAKE-KEY-NOT-A-REAL-KEY-0000")

        assertThat(aiStatusLine(gemini, AiAnswer.Proxy(AiKeyFailure.DEAD)))
            .contains(AiProvider.GEMINI.displayName)
        assertThat(aiStatusLine(gemini, AiAnswer.Proxy(AiKeyFailure.DEAD)))
            .doesNotContain(AiProvider.OPENAI.displayName)
    }

    // ── the consequence line: #32 §6's "quality only" ──────────────

    @Test
    fun `the consequence line promises no new capability`() {
        val without = aiConsequenceLine(null)
        val with = aiConsequenceLine(key)

        // §6 was DERIVED, not asked: the free model is a permanent design
        // constraint, so a key may only ever make things better and never make
        // anything possible that was impossible without it. Saying so on the
        // screen is what stops the section reading like a paywall.
        assertThat(without).contains("works without a key")
        assertThat(without).contains("not what")
        assertThat(with).contains("Same features")
        assertThat(with).contains("better answerer")
    }

    @Test
    fun `the consequence line names the fallback, so the ladder is visible`() {
        assertThat(aiConsequenceLine(key)).contains("free model answers instead")
    }
}
