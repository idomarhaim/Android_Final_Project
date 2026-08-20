package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.domain.model.AiAnswer
import com.idomarhaim.goalpilot.domain.model.AiCredential
import kotlinx.coroutines.flow.StateFlow

/**
 * Which credential answers a model call, and which one actually did — `C13`'s
 * provider abstraction (#54, decided in #32).
 *
 * ## `null` is the default, and it is load-bearing
 *
 * [credential] is `null` on every install until somebody opens the AI section
 * and types a key. In that state this app behaves exactly as it did before
 * `C13`: every model call goes through the Cloud Function proxy on GoalPilot's
 * own GROQ key, no extra field travels, and nothing on any screen changes.
 * #54 states it as a requirement — *"a user who never opens this section should
 * not notice it exists"* — and it is also #32's permanent design constraint:
 * **a user key may only ever make things better, never make anything possible
 * that was impossible without it.**
 *
 * ## Why it is device-local and beside the theme rather than in Firestore
 *
 * #32 §1 rejected Firestore although it was the cheaper option and would have
 * needed **no rules change at all** — `users/{uid}/{document=**}` is already
 * owner-only — and would have followed the user to a second device. A
 * third-party secret at rest in a database that is backed up, exportable and
 * readable by any future admin script is a different posture from one in the
 * Keystore, and cross-device convenience is not worth it for an audience of
 * one. The key is re-entered on a new device, deliberately.
 *
 * That places it beside [AppPreferencesRepository]'s skin and language on
 * `docs/PRODUCT_v0.3.md` §4.9's sign-out test: **it belongs to this phone, not
 * to the account.**
 *
 * ## Why the answer lives here too
 *
 * §5 pairs the point-of-use message with a **permanent status line on the key's
 * row in Settings**, so *"what is stored"* and *"what answered"* are read
 * together by one screen and written by one call path. Splitting them would
 * create a second place for the two to disagree, which is the failure the
 * status line exists to prevent.
 *
 * Every flow is a hot [StateFlow] for the same reason
 * [AppPreferencesRepository]'s are: Settings renders the stored value on its
 * first composition, and a cold flow would show one frame of *"no key set"* to
 * a user who has one.
 */
interface AiProviderRepository {

    /** What Settings holds. `null` — the default — means the Cloud Function proxy answers. */
    val credential: StateFlow<AiCredential?>

    /**
     * Who answered the most recent model call, or `null` before the first one
     * this process has made.
     *
     * In-memory only, and that is the honest scope: it is a fact about *this
     * run of the app*, and persisting it would let Settings assert something
     * about a call made days ago on a key that has since been replaced.
     */
    val lastAnswer: StateFlow<AiAnswer?>

    /**
     * `true` while a dead-key message is **owed at the point of use** — §5's
     * latch, and the only thing in `C13` that makes the app speak unprompted.
     *
     * Set when a call comes back with [com.idomarhaim.goalpilot.domain.model.AiKeyFailure.DEAD].
     * Cleared when the key is **edited** or when **any call to that provider
     * succeeds**, so a key that is fixed and later re-broken speaks again — the
     * reset rule *"once"* needs in order not to fail in both directions.
     */
    val deadKeyUnannounced: StateFlow<Boolean>

    /**
     * Stores a key. Overwrites whatever was there, and clears the dead-key
     * latch — an edit is the user's answer to the message.
     */
    fun save(credential: AiCredential)

    /**
     * A **real delete** (#54): the stored provider, model and key are removed
     * from the encrypted file, not blanked, and the app returns to the proxy
     * path with no restart.
     */
    fun clear()

    /** Called once per model call, by whoever made it. */
    fun recordAnswer(answer: AiAnswer)

    /** Called by the screen that showed §5's one message, so it is not shown twice. */
    fun markDeadKeyAnnounced()
}
