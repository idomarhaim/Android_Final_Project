package com.idomarhaim.goalpilot.data.security

import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import com.idomarhaim.goalpilot.domain.model.AiKeyFailure
import com.idomarhaim.goalpilot.domain.repository.AiProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every decision `C13` makes, over an [AiCredentialStore] that holds the bytes
 * (#54, decided in #32).
 *
 * The split is what makes #54's exit criterion testable against the shipping
 * code: this class needs no `Context`, no Keystore and no device, so *no key →
 * proxy · key set → provider · key deleted → proxy again* is a plain JVM test
 * of the real routing rather than of a hand-written fake. See
 * [AiCredentialStore].
 *
 * ## Three pieces of state, and only one of them is persisted
 *
 * | state | lives | why |
 * |---|---|---|
 * | [credential] | the encrypted file | it is the setting |
 * | [lastAnswer] | memory | a fact about *this run*; persisting it would let Settings assert something about a call made days ago on a key since replaced |
 * | [deadKeyUnannounced] | memory | §5's latch is *"one message at the point of use"*, and a message owed across a process death is a message about a call the user cannot remember |
 */
@Singleton
class DefaultAiProviderRepository @Inject constructor(
    private val store: AiCredentialStore,
) : AiProviderRepository {

    private val _credential = MutableStateFlow(store.read())
    override val credential: StateFlow<AiCredential?> = _credential.asStateFlow()

    private val _lastAnswer = MutableStateFlow<AiAnswer?>(null)
    override val lastAnswer: StateFlow<AiAnswer?> = _lastAnswer.asStateFlow()

    private val _deadKeyUnannounced = MutableStateFlow(false)
    override val deadKeyUnannounced: StateFlow<Boolean> = _deadKeyUnannounced.asStateFlow()

    override fun save(credential: AiCredential) {
        store.write(credential)
        _credential.value = credential
        // §5: the latch clears when the key is EDITED. Without this, a user who
        // fixes a mistyped key carries its "rejected" message into the next
        // screen that speaks and is told about a key that no longer exists.
        _deadKeyUnannounced.value = false
        // ⚠️ And so does the last answer — the status line is a sentence about
        // THIS credential, and an answer recorded before this credential existed
        // says nothing about it.
        //
        // `Observed:` 2026-08-20 on a device, which is the only place it could
        // be seen. The dashboard makes a feed call at launch; against the
        // currently deployed (pre-C13) function that comes back with no echo, so
        // `lastAnswer` is `Proxy(null)`. Adding a key then rendered *"GoalPilot's
        // free model answered, not your OpenAI key"* — true word by word, and it
        // reads as though the key had been tried and passed over, about a call
        // made before the key was typed. Reset, it reads *"OpenAI is set.
        // Nothing has been asked yet"*, which is what actually happened.
        //
        // `clear()` deliberately does NOT do this, and the asymmetry is the
        // point: after a clear there is no credential for the line to be about,
        // so the null-credential branch answers and the stale value is never
        // rendered. Here it is.
        _lastAnswer.value = null
    }

    override fun clear() {
        store.clear()
        _credential.value = null
        _deadKeyUnannounced.value = false
        // _lastAnswer is deliberately NOT reset. What answered the last call is a
        // fact about a call that really happened, and deleting a key does not
        // un-happen it. The next call overwrites it with Proxy, which is the
        // truthful moment for the status line to change.
    }

    override fun recordAnswer(answer: AiAnswer) {
        _lastAnswer.value = answer
        when {
            // "any call to that provider succeeds" — the second half of §5's
            // reset rule, and the half that lets a self-healing failure stop
            // nagging without a timer.
            answer is AiAnswer.UserKey -> _deadKeyUnannounced.value = false

            // Only DEAD latches. QUOTA and TRANSIENT ride the fallback silently
            // (§5's table) — a transient 429 that spoke every time would be the
            // recovery-masks-failure trap wearing the opposite costume.
            //
            // Keyed on the FAILURE and not on the rung, which is why this is a
            // `when {}` rather than a `when (answer)`: a dead key is just as
            // dead when the free model failed after it, and that combination is
            // the one where the app is furthest from working. See AiAnswer.
            answer.keyFailure == AiKeyFailure.DEAD -> _deadKeyUnannounced.value = true

            // Nothing was learned about the key: neither set nor clear. Reached
            // when no key is set at all, and when the call died before any
            // provider saw it.
            else -> Unit
        }
    }

    override fun markDeadKeyAnnounced() {
        _deadKeyUnannounced.value = false
    }
}
