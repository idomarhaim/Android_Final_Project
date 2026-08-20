package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCallEnvelope
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiKeyFailure
import com.idomarhaim.goalpilot.domain.model.AiProvider
import org.junit.Test

/**
 * The wire contract, and one property that no integration test could reach:
 * **what the client believes when the deployed function is older than the
 * client** (#54, #32 §2).
 *
 * That case is not hypothetical. `functions/` deploys separately from the APK,
 * and this session ships both halves while the deploy is Ido's to run — so
 * between now and then every install is a `C13` client talking to a pre-`C13`
 * function. The client must be **truthful** in that window, not merely
 * non-crashing, because the status line's whole reason for existing is that a
 * user cannot otherwise tell which credential paid.
 *
 * Every fixture key here is obviously fake (#54).
 */
class AiCallEnvelopeTest {

    private val fakeKey = "FAKE-KEY-NOT-A-REAL-KEY-0000"
    private val sent = AiCredential(AiProvider.OPENAI, "gpt-4o-mini", fakeKey)

    // ── the request half ───────────────────────────────────────────

    @Test
    fun `no credential adds nothing to the payload`() {
        assertThat(AiCallEnvelope.credentialFields(null)).isEmpty()
    }

    @Test
    fun `a credential adds exactly three fields`() {
        assertThat(AiCallEnvelope.credentialFields(sent)).containsExactly(
            "aiProvider", "openai",
            "aiModel", "gpt-4o-mini",
            "aiKey", fakeKey,
        )
    }

    // ── the absent-echo rule ───────────────────────────────────────

    @Test
    fun `a response with no answeredBy field means the free model answered`() {
        // The load-bearing case. A deployment that predates C13 ignores the
        // credential fields entirely and calls GROQ on the project key — so the
        // free model REALLY answered, and this is the true reading rather than a
        // defensive default.
        val old = mapOf("recommendations" to emptyList<Any>())

        assertThat(AiCallEnvelope.answeredBy(old, sent)).isEqualTo(AiAnswer.Proxy())
    }

    @Test
    fun `the client never names a provider a call did not reach`() {
        // Reading it the other way — "we sent a key, so the key answered" — is
        // the failure the whole status line exists to prevent.
        val answer = AiCallEnvelope.answeredBy(mapOf("points" to 10), sent)

        assertThat(answer).isNotInstanceOf(AiAnswer.UserKey::class.java)
    }

    @Test
    fun `a non-map response is read as the proxy rather than crashing`() {
        assertThat(AiCallEnvelope.answeredBy(null, sent)).isEqualTo(AiAnswer.Proxy())
        assertThat(AiCallEnvelope.answeredBy("nonsense", sent)).isEqualTo(AiAnswer.Proxy())
        assertThat(AiCallEnvelope.answeredBy(listOf(1, 2), sent)).isEqualTo(AiAnswer.Proxy())
    }

    // ── the three rungs ────────────────────────────────────────────

    @Test
    fun `a user echo names the provider that was sent`() {
        val data = mapOf("answeredBy" to "user", "points" to 10)

        assertThat(AiCallEnvelope.answeredBy(data, sent))
            .isEqualTo(AiAnswer.UserKey(AiProvider.OPENAI))
    }

    @Test
    fun `a user echo with nothing sent degrades to the proxy`() {
        // Impossible from a correct function. The client still does not invent a
        // provider name — it has no way to choose which of four to print.
        val data = mapOf("answeredBy" to "user")

        assertThat(AiCallEnvelope.answeredBy(data, null)).isEqualTo(AiAnswer.Proxy())
    }

    @Test
    fun `a proxy echo carries the key failure class`() {
        val data = mapOf(
            "answeredBy" to "proxy",
            "keyError" to mapOf("class" to "dead", "status" to 401),
        )

        assertThat(AiCallEnvelope.answeredBy(data, sent))
            .isEqualTo(AiAnswer.Proxy(AiKeyFailure.DEAD))
    }

    @Test
    fun `none is the third rung, and is not the same as an absent field`() {
        val none = mapOf("answeredBy" to "none")
        val absent = mapOf("points" to 10)

        // Collapsing these would make an outage read as "the free model
        // answered", which is the one sentence it must not read as.
        assertThat(AiCallEnvelope.answeredBy(none, sent)).isEqualTo(AiAnswer.Local())
        assertThat(AiCallEnvelope.answeredBy(absent, sent)).isEqualTo(AiAnswer.Proxy())
    }

    @Test
    fun `a dead key survives the free model failing behind it`() {
        val data = mapOf(
            "answeredBy" to "none",
            "keyError" to mapOf("class" to "dead", "status" to 403),
        )

        assertThat(AiCallEnvelope.answeredBy(data, sent))
            .isEqualTo(AiAnswer.Local(AiKeyFailure.DEAD))
    }

    @Test
    fun `an unknown failure class is no class, not a guess`() {
        val data = mapOf(
            "answeredBy" to "proxy",
            "keyError" to mapOf("class" to "combusted"),
        )

        assertThat(AiCallEnvelope.answeredBy(data, sent)).isEqualTo(AiAnswer.Proxy(null))
    }

    @Test
    fun `every class the function can send round-trips`() {
        // The two halves of this contract live in different languages and deploy
        // separately, so the id strings are the only thing holding them together.
        // `providers.ts` emits exactly these three words.
        for ((id, expected) in listOf(
            "dead" to AiKeyFailure.DEAD,
            "quota" to AiKeyFailure.QUOTA,
            "transient" to AiKeyFailure.TRANSIENT,
        )) {
            val data = mapOf("answeredBy" to "proxy", "keyError" to mapOf("class" to id))
            assertThat(AiCallEnvelope.answeredBy(data, sent))
                .isEqualTo(AiAnswer.Proxy(expected))
        }
    }
}
