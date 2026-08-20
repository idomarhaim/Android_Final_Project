package com.idomarhaim.goalpilot.domain.model

/**
 * **Who actually answered the last model call** — `C13`'s honesty half
 * (#54 piece 3, #32 §5).
 *
 * Once a key exists the app has two credentials and three rungs, and the user
 * has no way to tell which one ran. That is `docs/PRODUCT_v0.3.md` §0.3's
 * *second number that quietly disagrees* wearing a different hat: a dead key
 * that silently rides the fallback looks exactly like a key that is working.
 *
 * #32 §5's ladder, which **never degrades below where the app stands today**:
 *
 * ```
 * user key  →  free model (GROQ, project key)  →  local fallback (spec §8)
 * ```
 *
 * This type is the report of *which rung spoke*, not the decision of which to
 * try — the decision is [AiCredential]'s presence, and the fallback is the
 * Cloud Function's and then the repository's. It is written on every call, so
 * it is a fact rather than an inference.
 */
sealed interface AiAnswer {

    /**
     * Why the user's key did not answer, or `null`.
     *
     * **On the interface rather than on one case**, because it is orthogonal to
     * which rung spoke: a dead key can be followed by the free model answering
     * ([Proxy]) *or* by the free model failing too ([Local]), and the user needs
     * the same message either way. Hanging it off [Proxy] alone made a dead key
     * that coincided with an outage silently un-report itself — the one
     * combination where the app is furthest from working and says least.
     *
     * `null` on [UserKey] by construction: a key that answered did not fail.
     */
    val keyFailure: AiKeyFailure?

    /**
     * The Cloud Function answered on **GoalPilot's own** GROQ key.
     *
     * This is the state of the whole app before `C13` and of every install that
     * never sets a key, so it is the honest default in every ambiguous case —
     * see [AiCallEnvelope.answeredBy]. A non-null [keyFailure] here is the only
     * thing distinguishing *"you have no key"* from *"your key did not work"*.
     */
    data class Proxy(override val keyFailure: AiKeyFailure? = null) : AiAnswer

    /** The user's own key answered, on [provider]. */
    data class UserKey(val provider: AiProvider) : AiAnswer {
        override val keyFailure: AiKeyFailure? = null
    }

    /**
     * **No model spoke at all** — neither credential was reached, and
     * `RecommendationRepositoryImpl`'s deterministic heuristics answered
     * (spec §8). Rung three.
     *
     * Named rather than folded into [Proxy] because a status line that called
     * this *"the free model answered"* would be saying something false. It is
     * reachable two ways: the callable itself failed (no network, a cold start
     * that timed out), or it ran and reported that nothing answered.
     */
    data class Local(override val keyFailure: AiKeyFailure? = null) : AiAnswer
}

/**
 * #32 §5's failure classes — the table that decides whether the app **speaks**.
 *
 * | class | meaning | behaviour |
 * |---|---|---|
 * | [DEAD] (`401` / `403`) | revoked or mistyped — **only the user can fix it** | one message at the point of use |
 * | [QUOTA] (`429`) | out of quota — self-heals | silent, rides the fallback |
 * | [TRANSIENT] (`5xx`, timeout) | the provider's problem | silent, rides the fallback |
 *
 * The split exists because *"tell me at the point of use, **once**"* needs a
 * failure class or it fails in both directions: once-ever means a key you fix
 * and re-break never speaks again, once-per-open means a transient `429` nags
 * until you learn to ignore it. Only [DEAD] latches; see
 * `AiProviderRepository.deadKeyUnannounced`.
 *
 * **All three still show on the permanent status row**, including the silent
 * ones. §5's improvement on the chosen option was that a one-time message alone
 * leaves a dead key with no standing indicator — the message tells you now, the
 * row tells you later. `C7`'s house rule: **legal, but never silent.**
 */
enum class AiKeyFailure(val id: String) {
    DEAD("dead"),
    QUOTA("quota"),
    TRANSIENT("transient"),
    ;

    companion object {
        fun fromId(id: String?): AiKeyFailure? = entries.firstOrNull { it.id == id }
    }
}
