package com.idomarhaim.goalpilot.domain.model

/**
 * The four model providers `C13` may route a call through
 * ([#32](https://github.com/idomarhaim/Android_Final_Project/issues/32) §3,
 * built in [#54](https://github.com/idomarhaim/Android_Final_Project/issues/54)).
 *
 * ## Four named adapters, and nothing else
 *
 * §3 overturned a cheaper proposal — *"any OpenAI-compatible endpoint, three
 * text fields"* — which would have covered Together, DeepSeek, Fireworks,
 * OpenRouter and a local Ollama for free, because GROQ's endpoint already *is*
 * `…/openai/v1/chat/completions`. It was rejected in favour of the stronger
 * guarantee that **no untested wire format can ever run**, which is what §7's
 * schema contract turns on. The accepted cost is that a fifth provider is a
 * Cloud Function deploy rather than a settings edit.
 *
 * ## GROQ appears twice, and that is not a bug
 *
 * GROQ is the *project's* free default — every AI call in this app has always
 * gone through the Cloud Function on GoalPilot's own GROQ key — **and** one of
 * the four a user may bring a key for. The two are not the same route: the
 * difference is whose credential pays, which is exactly the distinction
 * [AiAnswer] exists to name on screen.
 *
 * ## The model ids rot, which is why they are only defaults
 *
 * `AGENTS.md` records that *"the GROQ model id rots — GROQ retires models on a
 * rolling schedule and a retired id fails **silently**"*. §*Also settled* drew
 * the consequence: the model id is **free text with a per-provider default**,
 * not a curated list, because a curated list baked into a deploy rots
 * identically and now in four providers at once. So [defaultModel] is a
 * starting value the user can overwrite, never a constraint.
 *
 * `Observed:` [GROQ]'s default is the one `functions/src/index.ts` already
 * pins, so the free path and a user's own GROQ key answer with the same model
 * unless the user says otherwise. `Untested:` the other three defaults are
 * plausible current ids checked against vendor documentation at authoring time
 * and **not** exercised against a live key by this session — the field exists
 * precisely so a rotted default costs one edit rather than a deploy.
 */
enum class AiProvider(
    /** Stable storage + wire id. Never the enum name — renaming must not orphan a stored key. */
    val id: String,
    /** What the user sees in Settings. */
    val displayName: String,
    /** Prefilled when the model field is left blank; see the class KDoc on rot. */
    val defaultModel: String,
    /** Where a user goes to mint a key, shown as chrome under the key field. */
    val keyOrigin: String,
) {
    GROQ("groq", "GROQ", "openai/gpt-oss-20b", "console.groq.com"),
    OPENAI("openai", "OpenAI", "gpt-4o-mini", "platform.openai.com"),
    ANTHROPIC("anthropic", "Anthropic", "claude-opus-5", "console.anthropic.com"),
    GEMINI("gemini", "Google Gemini", "gemini-2.5-flash", "aistudio.google.com"),
    ;

    companion object {
        /**
         * The stored provider, or `null` when the id names nothing.
         *
         * Deliberately nullable rather than defaulting to [GROQ]: a stored id
         * this build does not recognise means the credential cannot be routed,
         * and silently rewriting it to GROQ would send a user's **OpenAI** key
         * to GROQ. Absent is the honest answer, and it lands on the proxy path.
         */
        fun fromId(id: String?): AiProvider? = entries.firstOrNull { it.id == id }
    }
}
