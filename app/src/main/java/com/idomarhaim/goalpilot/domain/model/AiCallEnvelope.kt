package com.idomarhaim.goalpilot.domain.model

/**
 * The wire contract between the client and the model proxy — `C13`'s provider
 * abstraction, reduced to two pure functions (#54 piece 2, #32 §2).
 *
 * ## Why the key is on the wire at all
 *
 * #32 §2 decided **yes — per call, held nowhere**: the client sends
 * `provider · model · key` in the callable payload, the Cloud Function uses it
 * in place of `process.env.GROQ_API_KEY`, and discards it. Nothing is cached,
 * and no key is written to Firestore or to a log.
 *
 * The deciding argument is **mechanism count, not security**. A client that
 * called a provider directly when a key is present would need a second copy of
 * every prompt and every `C11b` schema, and the copy that drifts is the
 * *enhanced* path — exercised only when a key is present, which for an audience
 * of one is exactly when nobody is watching. The security trade runs the other
 * way and is accepted with its eyes open: the key transits TLS and lives
 * briefly in the memory of a Google-run container.
 *
 * **The consequence the client keeps:** it gains **no** outbound path to any
 * model provider. GoalPilot's only network peers are still Firebase and Google.
 *
 * ## Why this is an object of pure functions and not a repository method
 *
 * Both halves are the ones `AiCallEnvelopeTest` has to be able to run without
 * a Keystore, a Firebase graph or a device — and [answeredBy] in particular
 * carries a property no integration test would ever reach.
 */
object AiCallEnvelope {

    // ── Request ────────────────────────────────────────────────────
    const val FIELD_PROVIDER = "aiProvider"
    const val FIELD_MODEL = "aiModel"
    const val FIELD_KEY = "aiKey"

    // ── Response ───────────────────────────────────────────────────
    const val FIELD_ANSWERED_BY = "answeredBy"
    const val FIELD_KEY_ERROR = "keyError"

    const val ANSWERED_BY_USER = "user"
    const val ANSWERED_BY_PROXY = "proxy"

    /**
     * The function ran and **nothing answered** — its own GROQ call failed too.
     *
     * Distinct from the field being *absent*, and the distinction is the whole
     * point: absent means an old deployment that never knew about `C13` and
     * really did answer on the project key, while `"none"` is a current
     * deployment reporting the third rung. Collapsing them would make an
     * outage read as *"the free model answered"*.
     */
    const val ANSWERED_BY_NONE = "none"

    /**
     * The three fields #32 §2 puts in a callable payload — **empty when no key
     * is set**, which is the default and must stay indistinguishable from the
     * app as it was before `C13`.
     *
     * Returning an empty map rather than nulls is deliberate: a
     * `HashMap<String, Any>` handed to `getHttpsCallable().call()` is serialised
     * whole, and a `null` on the wire is a field the function has to defend
     * against. Absent is unambiguous.
     */
    fun credentialFields(credential: AiCredential?): Map<String, String> {
        if (credential == null) return emptyMap()
        return mapOf(
            FIELD_PROVIDER to credential.provider.id,
            FIELD_MODEL to credential.effectiveModel,
            FIELD_KEY to credential.key,
        )
    }

    /**
     * Who answered, read off the callable's own reply.
     *
     * ## The absent-echo rule, which is the whole reason this function exists
     *
     * A response that carries **no** [FIELD_ANSWERED_BY] is read as
     * [AiAnswer.Proxy] with no failure — *"the free model answered"* — and that
     * is not a defensive default, it is the **true** reading. A deployed
     * function that predates `C13` ignores the credential fields entirely and
     * calls GROQ on `process.env.GROQ_API_KEY`; the free model really did
     * answer. The status line is therefore honest against an old deployment,
     * a rolled-back deployment, and a partially-rolled-out one, without the
     * client knowing which it is talking to.
     *
     * Reading it the other way — *"we sent a key, so the key must have
     * answered"* — is the failure this whole piece exists to prevent: it would
     * put a provider's name on screen for a call that provider never saw.
     *
     * @param sent what [credentialFields] was called with, so a `"user"` echo
     *   can name the provider. A `"user"` echo with nothing sent is impossible
     *   from a correct function and is read as [AiAnswer.Proxy] — the client
     *   never invents a provider it did not choose.
     */
    fun answeredBy(data: Any?, sent: AiCredential?): AiAnswer {
        val root = data as? Map<*, *> ?: return AiAnswer.Proxy()
        val failure = keyFailureOf(root[FIELD_KEY_ERROR])
        return when (root[FIELD_ANSWERED_BY] as? String) {
            ANSWERED_BY_USER ->
                // A "user" echo with nothing sent is impossible from a correct
                // function, and the client never invents a provider it did not
                // choose — so it degrades to the proxy reading rather than
                // guessing which of four names to print.
                if (sent != null) AiAnswer.UserKey(sent.provider) else AiAnswer.Proxy(failure)
            ANSWERED_BY_NONE -> AiAnswer.Local(failure)
            // Both `"proxy"` and ABSENT land here, by the rule above.
            else -> AiAnswer.Proxy(failure)
        }
    }

    /**
     * The failure class the function reported, or `null`.
     *
     * **A status code and a class, never a body.** #32 found a live defect while
     * grounding this: `functions/src/index.ts` threw the provider's raw error
     * body into `logger.error`, which is harmless on the project's own key and
     * is a third-party provider's error text in Google's logs the moment a user
     * key is in play. The spec line — *on a user-key call, never log a provider
     * error body verbatim* — is enforced on the function side; this end simply
     * has nowhere to put one, because the only field it reads is a class name.
     */
    private fun keyFailureOf(raw: Any?): AiKeyFailure? {
        val m = raw as? Map<*, *> ?: return null
        return AiKeyFailure.fromId(m["class"] as? String)
    }
}
