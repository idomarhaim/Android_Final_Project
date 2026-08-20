/**
 * `C13`'s four provider adapters — the server half of
 * [#54](https://github.com/idomarhaim/Android_Final_Project/issues/54), decided
 * in [#32](https://github.com/idomarhaim/Android_Final_Project/issues/32).
 *
 * ## Why the adapters are here and not on the client
 *
 * #32 §2 decided the user's key travels **to this function, per call, held
 * nowhere**: the client sends `provider · model · key` in the callable payload,
 * an adapter below uses it in place of `process.env.GROQ_API_KEY`, and it is
 * discarded when the request ends. Nothing is cached and nothing is written to
 * Firestore or to a log.
 *
 * The deciding argument was **mechanism count, not security.** A client that
 * called a provider directly when a key is present would need a second copy of
 * every prompt and every `C11b` schema — and the copy that drifts is the
 * *enhanced* one, exercised only when a key is present, which for an audience
 * of one is exactly when nobody is watching. The security trade runs the other
 * way and is accepted with open eyes: the key transits TLS and lives briefly in
 * the memory of a Google-run container. What the app keeps in exchange is that
 * it gains **no outbound path to any model provider at all**.
 *
 * ## Four named adapters, and nothing else
 *
 * #32 §3 overturned a cheaper *"any OpenAI-compatible endpoint"* proposal in
 * favour of the stronger guarantee that **no untested wire format can ever
 * run**. Adding a fifth provider is a deploy, not a settings edit, and that is
 * the accepted cost.
 *
 * ## Errors carry a class and a status, never a body
 *
 * #32 found a live defect while grounding this: `index.ts` threw the provider's
 * **raw error body** into `logger.error`, which is harmless while the only key
 * is the project's own and is a third-party provider's error text in Google's
 * logs the moment a user key is in play. The four providers do not fail alike,
 * so there is no safe body to keep. [classifyStatus] reduces every failure to
 * one of three words the client can act on, and [ProviderError] carries nothing
 * else — see `AiKeyFailure` on the Kotlin side for the table.
 *
 * This module imports nothing, so `test/providers.test.mjs` runs it under plain
 * `node --test` with no emulator and no Firebase — the same arrangement
 * `classify.ts` and `derived.ts` use, and for the same reason.
 */

/** The four ids the client may send. Anything else is not a provider we have. */
export type ProviderId = "groq" | "openai" | "anthropic" | "gemini";

/** #32 §5's failure classes, as the client reads them. */
export type FailureClass = "dead" | "quota" | "transient";

/** The credential fields a callable payload may carry (`AiCallEnvelope` on the client). */
export interface UserCredential {
  provider: ProviderId;
  model: string;
  key: string;
}

/**
 * A provider failure, reduced to what is safe to return and to log.
 *
 * **No `body` field, deliberately.** There is nowhere to put a provider's error
 * text, which is what makes the spec line *never log a provider error body
 * verbatim* structural rather than a rule someone has to remember.
 */
export class ProviderError extends Error {
  constructor(
    readonly provider: ProviderId,
    readonly status: number,
    readonly failureClass: FailureClass,
  ) {
    super(`${provider} HTTP ${status} (${failureClass})`);
    this.name = "ProviderError";
  }
}

/**
 * #32 §5's table, and the only place it is decided.
 *
 * | status | class | what the app does |
 * |---|---|---|
 * | `401`, `403` | `dead` | revoked or mistyped — **one message at the point of use** |
 * | `429` | `quota` | self-heals — silent, rides the fallback |
 * | anything else | `transient` | the provider's problem — silent, rides the fallback |
 *
 * `402` is `dead` too: a provider that says *payment required* is telling the
 * user something only they can fix, which is the property that decides this
 * column. Quota that self-heals and billing that does not are different
 * answers to *"will waiting help?"*, and only the user can answer the second.
 */
export function classifyStatus(status: number): FailureClass {
  if (status === 401 || status === 403 || status === 402) return "dead";
  if (status === 429) return "quota";
  return "transient";
}

/**
 * The credential the client sent, or `null` when it sent none — which is the
 * default and the state every install is in until somebody adds a key.
 *
 * Validated rather than trusted: an unknown provider id, a blank key or a
 * non-string field means **no credential**, and the call falls to the project's
 * own GROQ key. That is the same *"absent is the honest answer"* rule
 * `AiProvider.fromId` applies on the client — an id this deploy does not know
 * cannot be routed, and guessing would send one provider's key to another.
 */
export function credentialFromRequest(data: any): UserCredential | null {
  const provider = data?.aiProvider;
  const key = data?.aiKey;
  const model = data?.aiModel;
  if (typeof provider !== "string" || typeof key !== "string") return null;
  if (!PROVIDERS.has(provider as ProviderId)) return null;
  if (key.trim().length === 0) return null;
  return {
    provider: provider as ProviderId,
    model: typeof model === "string" && model.trim().length > 0
      ? model.trim()
      : DEFAULT_MODELS[provider as ProviderId],
    key: key.trim(),
  };
}

const PROVIDERS = new Set<ProviderId>(["groq", "openai", "anthropic", "gemini"]);

/**
 * Per-provider fallbacks for a blank model field.
 *
 * These mirror `AiProvider.defaultModel` on the client, and the client normally
 * resolves the blank itself — this is the second line, for a payload that
 * carries a provider and no model. **Two copies of a list that rots** is a real
 * cost and it was taken deliberately: the alternative is a round trip to ask
 * the function what its default is, on every render of a settings screen.
 */
const DEFAULT_MODELS: Record<ProviderId, string> = {
  groq: "openai/gpt-oss-20b",
  openai: "gpt-4o-mini",
  anthropic: "claude-opus-5",
  gemini: "gemini-2.5-flash",
};

/** What each adapter builds. Kept as data so the request shapes are testable without a network. */
export interface ProviderRequest {
  url: string;
  headers: Record<string, string>;
  body: unknown;
}

/**
 * Builds one provider's HTTP request for a JSON-object answer.
 *
 * ## Every adapter enforces the JSON shape natively (#32 §7)
 *
 * §7 is the one requirement that is the same decision as §3 seen twice: with
 * four providers a model swap is the normal case, so *"a guarantee that
 * survives a model swap"* stops being a footnote and becomes the requirement.
 * Each adapter therefore uses its own vendor mechanism —
 *
 * | provider | mechanism |
 * |---|---|
 * | GROQ, OpenAI | `response_format: { type: "json_object" }` |
 * | Anthropic | forced tool use — `tool_choice` pinned to one tool whose schema is the answer |
 * | Gemini | `responseMimeType: "application/json"` |
 *
 * — and the **app-side validation stays**, because the two catch different
 * failures. Structural failures (`"type": "BANANA"`, a missing field) are what
 * native enforcement catches; the one failure `C11a` actually measured in 248
 * live calls was **semantic** — `8xKq2mN4vRt7pLwZaB1c` came back
 * `8xKq2mN4vRt7pLwZaB`, a truncated id of the right type and a plausible
 * length, passing every structural check there is. No schema catches that at
 * any provider or any model size; only `classify.ts`'s membership check does.
 */
export function buildRequest(
  cred: UserCredential,
  system: string,
  user: string,
): ProviderRequest {
  switch (cred.provider) {
    // GROQ's endpoint IS OpenAI's, which is why one branch serves both. They are
    // still two entries in ProviderId: a shared wire format is not a shared
    // credential, and #32 §3 named four providers rather than one shape.
    case "groq":
    case "openai":
      return {
        url: cred.provider === "groq"
          ? "https://api.groq.com/openai/v1/chat/completions"
          : "https://api.openai.com/v1/chat/completions",
        headers: {
          "Authorization": `Bearer ${cred.key}`,
          "Content-Type": "application/json",
        },
        body: {
          model: cred.model,
          temperature: 0.7,
          response_format: { type: "json_object" },
          messages: [
            { role: "system", content: system },
            { role: "user", content: user },
          ],
        },
      };

    case "anthropic":
      return {
        url: "https://api.anthropic.com/v1/messages",
        headers: {
          "x-api-key": cred.key,
          "anthropic-version": "2023-06-01",
          "Content-Type": "application/json",
        },
        body: {
          model: cred.model,
          max_tokens: 2048,
          system,
          messages: [{ role: "user", content: user }],
          // Forced tool use is Anthropic's structured-output mechanism, and the
          // schema is deliberately permissive: the SHAPE each callable wants is
          // described in its own `system` prompt and checked by classify.ts, so
          // pinning field names here would put a third copy of every schema in
          // this file. What the tool buys is the guarantee that the reply is a
          // JSON OBJECT rather than prose containing one — which is the failure
          // `JSON.parse` hits, and the only one this layer can prevent.
          tools: [
            {
              name: "answer",
              description: "Return the answer as a JSON object.",
              input_schema: { type: "object", additionalProperties: true },
            },
          ],
          tool_choice: { type: "tool", name: "answer" },
        },
      };

    case "gemini":
      return {
        // The key goes in a header, not the `?key=` query parameter Google's
        // own quickstarts use. A query string is the one part of a URL that
        // reliably ends up in access logs and error messages, and #54's first
        // hard rule is that the key reaches neither.
        url: `https://generativelanguage.googleapis.com/v1beta/models/${
          encodeURIComponent(cred.model)
        }:generateContent`,
        headers: {
          "x-goog-api-key": cred.key,
          "Content-Type": "application/json",
        },
        body: {
          systemInstruction: { parts: [{ text: system }] },
          contents: [{ role: "user", parts: [{ text: user }] }],
          generationConfig: { responseMimeType: "application/json" },
        },
      };
  }
}

/**
 * Pulls the JSON object out of whichever envelope the provider wrapped it in.
 *
 * Returns `{}` rather than throwing on a shape it does not recognise: an empty
 * object is what every caller in `index.ts` already treats as *"the model said
 * nothing usable"*, and it lands on the same deterministic fallback a network
 * failure does (spec §8). Throwing here would turn a bad reply into a `dead
 * key` message about a key that worked perfectly.
 */
export function extractJson(provider: ProviderId, data: any): any {
  let text: string | undefined;
  switch (provider) {
    case "groq":
    case "openai":
      text = data?.choices?.[0]?.message?.content;
      break;
    case "anthropic": {
      // The forced tool call carries the object already parsed, so there is
      // nothing to JSON.parse. Fall through to any text block only if the model
      // answered without using the tool it was pinned to.
      const toolUse = (data?.content ?? []).find((b: any) => b?.type === "tool_use");
      if (toolUse?.input && typeof toolUse.input === "object") return toolUse.input;
      text = (data?.content ?? []).find((b: any) => b?.type === "text")?.text;
      break;
    }
    case "gemini":
      text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
      break;
  }
  if (typeof text !== "string") return {};
  try {
    const parsed = JSON.parse(text);
    // `!Array.isArray` is not tidiness: `typeof [] === "object"`, so an array
    // sails through the object check, and every caller in index.ts then reads
    // `json?.points` / `json?.recommendations` off it and gets `undefined` —
    // a "the model answered" path that produces the fallback values silently.
    // Caught by `providers.test.mjs` on the first run of this file.
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

/**
 * Calls a user's provider and returns the JSON object it answered with.
 *
 * @throws {ProviderError} with a status and a class, and **never** a body.
 */
export async function callUserProvider(
  cred: UserCredential,
  system: string,
  user: string,
  fetchImpl: typeof fetch = fetch,
): Promise<any> {
  const req = buildRequest(cred, system, user);
  let res: Response;
  try {
    res = await fetchImpl(req.url, {
      method: "POST",
      headers: req.headers,
      body: JSON.stringify(req.body),
    });
  } catch {
    // A DNS failure, a TLS failure or a timeout. No status to report, and the
    // caught value is discarded rather than inspected: a fetch rejection can
    // carry the request it failed on, and that request holds the key.
    throw new ProviderError(cred.provider, 0, "transient");
  }
  if (!res.ok) {
    // The body is NOT read. See the module KDoc: there is no safe body to keep,
    // and reading one only creates something to accidentally log.
    throw new ProviderError(cred.provider, res.status, classifyStatus(res.status));
  }
  return extractJson(cred.provider, await res.json());
}
