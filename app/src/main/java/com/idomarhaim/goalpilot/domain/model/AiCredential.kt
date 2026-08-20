package com.idomarhaim.goalpilot.domain.model

/**
 * A user's own model credential — the **only** third-party secret this app
 * holds (`C13` #54, decided in #32 §1).
 *
 * `null` wherever an `AiCredential?` appears means *no key is set*, which is
 * the default state and the one the whole app has been in until now: the Cloud
 * Function proxy answers on GoalPilot's own GROQ key. **A user who never opens
 * the AI section must not be able to tell this type exists.**
 *
 * ## Why [toString] is overridden
 *
 * This is a `data class`, and a `data class` prints every property. The
 * generated `toString()` is the single likeliest route from an encrypted store
 * to a log line — `Log.d(TAG, "cred=$credential")` in a debugging session, an
 * exception message that interpolates a request object, a crash reporter
 * serialising a field. #54's hard requirement is that *"a key never reaches a
 * log, a crash report, an analytics event, or a changelog"*, and the cheapest
 * way to make that true is to make the unsafe rendering **unreachable** rather
 * than to remember not to write it. `AiCredentialTest` asserts it.
 *
 * `equals`/`hashCode` still include [key] — that is what makes *"is this a
 * different key from the one stored?"* answerable, and neither leaks anything.
 */
data class AiCredential(
    val provider: AiProvider,
    /** Free text; blank means [AiProvider.defaultModel] (#32 §*Also settled*). */
    val model: String,
    val key: String,
) {

    /** What actually travels — the typed model, or the provider's default when blank. */
    val effectiveModel: String
        get() = model.ifBlank { provider.defaultModel }

    /**
     * What Settings renders. Never the key.
     *
     * Last four characters survive, which is what lets a user tell *"the key I
     * pasted"* from *"the key I meant to paste"* without a reveal action.
     * #54 rules that **"Reveal" is a feature request, not a default**, and the
     * app has no reveal at all — the editor's field starts empty on a replace
     * rather than pre-filled, so the stored key has no render path.
     *
     * A key of four characters or fewer is masked entirely: on a string that
     * short the last four *is* the whole secret.
     */
    val maskedKey: String
        get() = if (key.length <= MASK_TAIL) {
            MASK_CHAR.toString().repeat(key.length.coerceAtLeast(MASK_MIN))
        } else {
            MASK_CHAR.toString().repeat(MASK_MIN) + key.takeLast(MASK_TAIL)
        }

    /** Masked. See the class KDoc — this override is a safety property, not tidiness. */
    override fun toString(): String =
        "AiCredential(provider=${provider.id}, model=$effectiveModel, key=$maskedKey)"

    companion object {
        private const val MASK_CHAR = '•'
        private const val MASK_TAIL = 4
        private const val MASK_MIN = 8

        /**
         * A key with surrounding whitespace stripped, or `null` if nothing is left.
         *
         * Paste from a provider console routinely carries a trailing newline, and
         * a key with one is a key that fails with a `401` the user cannot see the
         * cause of — the worst possible first experience of this feature, since
         * §5 classes `401` as *dead, revoked or mistyped*.
         */
        fun of(provider: AiProvider, model: String, key: String): AiCredential? {
            val trimmed = key.trim()
            return if (trimmed.isEmpty()) null else AiCredential(provider, model.trim(), trimmed)
        }
    }
}
