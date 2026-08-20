package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.data.security.AiCredentialStore
import com.idomarhaim.goalpilot.data.security.DefaultAiProviderRepository
import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCallEnvelope
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiKeyFailure
import com.idomarhaim.goalpilot.domain.model.AiProvider
import org.junit.Test

/**
 * `C13`'s provider selection, in **all three directions** — #54's exit
 * criterion, and the reason `AiCredentialStore` exists as a seam.
 *
 * > *no key → proxy; key set → provider; key deleted → proxy again.*
 * > **All three directions, because the one you skip is the one that breaks.**
 *
 * The subject is [DefaultAiProviderRepository] itself — the class that ships —
 * with only the Keystore substituted for a map. A hand-written fake repository
 * would have made this suite assert against a second implementation of §5's
 * latch rather than against the one that runs.
 *
 * The **third** direction is the one that would rot silently: an app that never
 * cleared its in-memory credential after a delete would keep sending a key that
 * is no longer on disk, look completely fine until the next process start, and
 * then start answering differently for no visible reason.
 */
class AiProviderRoutingTest {

    /**
     * The seam, and the whole substitution. Obviously-fake keys throughout — #54
     * requires any test fixture key to be clearly marked as such, and nothing
     * here resembles a real credential from any of the four providers.
     */
    private class MemoryStore(var stored: AiCredential? = null) : AiCredentialStore {
        var clearCalls = 0
        override fun read() = stored
        override fun write(credential: AiCredential) {
            stored = credential
        }

        override fun clear() {
            clearCalls++
            stored = null
        }
    }

    private val fakeKey = "FAKE-KEY-NOT-A-REAL-KEY-0000"
    private fun key(provider: AiProvider = AiProvider.OPENAI, model: String = "") =
        AiCredential(provider, model, fakeKey)

    // ── direction 1 · no key → proxy ───────────────────────────────

    @Test
    fun `with no key the credential is absent and no field travels`() {
        val repo = DefaultAiProviderRepository(MemoryStore())

        assertThat(repo.credential.value).isNull()
        // The payload must be byte-identical to what this app sent before C13:
        // a user who never opens the AI section should not be able to tell it
        // exists, and an extra field on the wire is exactly how they could.
        assertThat(AiCallEnvelope.credentialFields(repo.credential.value)).isEmpty()
    }

    // ── direction 2 · key set → provider ───────────────────────────

    @Test
    fun `saving a key routes to that provider and puts three fields on the wire`() {
        val store = MemoryStore()
        val repo = DefaultAiProviderRepository(store)

        repo.save(key(AiProvider.ANTHROPIC, "some-model"))

        assertThat(repo.credential.value?.provider).isEqualTo(AiProvider.ANTHROPIC)
        assertThat(AiCallEnvelope.credentialFields(repo.credential.value)).containsExactly(
            AiCallEnvelope.FIELD_PROVIDER, "anthropic",
            AiCallEnvelope.FIELD_MODEL, "some-model",
            AiCallEnvelope.FIELD_KEY, fakeKey,
        )
        // It reached the store, not just the flow — otherwise the key survives
        // this process and nothing else.
        assertThat(store.stored).isNotNull()
    }

    @Test
    fun `a blank model travels as the provider's default, never blank`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key(AiProvider.GEMINI, model = ""))

        assertThat(AiCallEnvelope.credentialFields(repo.credential.value))
            .containsEntry(AiCallEnvelope.FIELD_MODEL, AiProvider.GEMINI.defaultModel)
    }

    @Test
    fun `a stored provider id this build does not know is refused, not defaulted`() {
        // Reading back an id from a future or older release. Defaulting to GROQ
        // would send the user's OpenAI key to GROQ, which is worse than not
        // routing at all — so `fromId` is nullable and this lands on the proxy.
        assertThat(AiProvider.fromId("together")).isNull()
        assertThat(AiProvider.fromId(null)).isNull()
        assertThat(AiProvider.fromId("groq")).isEqualTo(AiProvider.GROQ)
    }

    // ── direction 3 · key deleted → proxy again ────────────────────

    @Test
    fun `clearing the key returns to the proxy path in memory and on disk`() {
        val store = MemoryStore()
        val repo = DefaultAiProviderRepository(store)
        repo.save(key())

        repo.clear()

        assertThat(repo.credential.value).isNull()
        assertThat(AiCallEnvelope.credentialFields(repo.credential.value)).isEmpty()
        // A REAL delete (#54): the store was told, not merely the flow.
        assertThat(store.clearCalls).isEqualTo(1)
        assertThat(store.stored).isNull()
    }

    @Test
    fun `a repository built over a store holding a key starts on that key`() {
        // The other half of direction 3: the state a fresh process starts in is
        // the store's, so a delete that only cleared memory would silently come
        // back on the next launch.
        val repo = DefaultAiProviderRepository(MemoryStore(key(AiProvider.GROQ)))
        assertThat(repo.credential.value?.provider).isEqualTo(AiProvider.GROQ)
    }

    // ── §5's latch, in both directions ─────────────────────────────

    @Test
    fun `a dead key latches one message, and a success clears it`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key())
        assertThat(repo.deadKeyUnannounced.value).isFalse()

        repo.recordAnswer(AiAnswer.Proxy(AiKeyFailure.DEAD))
        assertThat(repo.deadKeyUnannounced.value).isTrue()

        repo.markDeadKeyAnnounced()
        assertThat(repo.deadKeyUnannounced.value).isFalse()

        // "the latch clears when ... any call to that provider succeeds", so a
        // fixed-then-rebroken key speaks again.
        repo.recordAnswer(AiAnswer.Proxy(AiKeyFailure.DEAD))
        repo.recordAnswer(AiAnswer.UserKey(AiProvider.OPENAI))
        assertThat(repo.deadKeyUnannounced.value).isFalse()
    }

    @Test
    fun `quota and transient failures never speak`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key())

        repo.recordAnswer(AiAnswer.Proxy(AiKeyFailure.QUOTA))
        assertThat(repo.deadKeyUnannounced.value).isFalse()

        repo.recordAnswer(AiAnswer.Proxy(AiKeyFailure.TRANSIENT))
        assertThat(repo.deadKeyUnannounced.value).isFalse()
    }

    @Test
    fun `a dead key still latches when the free model failed behind it`() {
        // The combination the app is furthest from working in, and the one an
        // earlier draft of AiAnswer silently dropped by hanging keyFailure off
        // Proxy alone.
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key())

        repo.recordAnswer(AiAnswer.Local(AiKeyFailure.DEAD))

        assertThat(repo.deadKeyUnannounced.value).isTrue()
    }

    @Test
    fun `editing the key clears an owed message`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key())
        repo.recordAnswer(AiAnswer.Proxy(AiKeyFailure.DEAD))

        repo.save(key(AiProvider.OPENAI, "other-model"))

        // Otherwise a user who fixes a mistyped key is told about a key that no
        // longer exists, on the next screen that speaks.
        assertThat(repo.deadKeyUnannounced.value).isFalse()
    }

    @Test
    fun `deleting the key clears an owed message`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key())
        repo.recordAnswer(AiAnswer.Proxy(AiKeyFailure.DEAD))

        repo.clear()

        assertThat(repo.deadKeyUnannounced.value).isFalse()
    }

    @Test
    fun `saving a key forgets an answer recorded before it existed`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        // The app asks the model at launch, long before anybody opens Settings.
        repo.recordAnswer(AiAnswer.Proxy())

        repo.save(key())

        // Otherwise the status line reports that call as though this key had
        // been tried and passed over. Found on a device, 2026-08-20.
        assertThat(repo.lastAnswer.value).isNull()
    }

    @Test
    fun `deleting the key does not rewrite what answered the last call`() {
        val repo = DefaultAiProviderRepository(MemoryStore())
        repo.save(key())
        repo.recordAnswer(AiAnswer.UserKey(AiProvider.OPENAI))

        repo.clear()

        // That call really did happen on that key. The NEXT call overwrites it,
        // which is the truthful moment for the status line to change.
        assertThat(repo.lastAnswer.value).isEqualTo(AiAnswer.UserKey(AiProvider.OPENAI))
    }
}
