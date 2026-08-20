/**
 * `C13`'s four provider adapters — the server half of `#54`, decided in `#32`.
 *
 * No emulator and no Firebase, deliberately: `src/providers.ts` imports nothing, so this
 * runs as plain `node --test` in milliseconds. Same arrangement as `classify.test.mjs`,
 * and the same reason — the questions here are about *request shapes* and *failure
 * classification*, neither of which needs a network to answer.
 *
 * ⚠️ **Every key in this file is obviously fake and marked as such** (`#54`'s last hard
 * requirement). They are `FAKE-*-NOT-A-REAL-KEY` strings: nothing here resembles a
 * provider's real key format, so no scanner, screenshot or copy-paste of this file can
 * ever produce a working credential.
 *
 * The suite is organised around the two properties the ticket rests on:
 *
 * 1. **A credential is validated, never trusted.** An unknown provider, a blank key or a
 *    non-string field means *no credential*, and the call falls to the project's own key —
 *    the same "absent is the honest answer" rule `AiProvider.fromId` applies on the client.
 * 2. **A failure carries a class and a status, and nothing else.** `#32` found a live
 *    defect where a provider's raw error body reached Cloud Logging; the adapters are
 *    written so there is no body to log, and the tests check the response body is never
 *    even read.
 */
import test from "node:test";
import assert from "node:assert/strict";

// The compiled output, not the source: `npm test` runs `tsc` first (package.json), so
// these tests exercise the exact JavaScript that gets deployed.
import {
  buildRequest,
  callUserProvider,
  classifyStatus,
  credentialFromRequest,
  extractJson,
  ProviderError,
} from "../lib/providers.js";

// Obviously fake, and deliberately shaped like nothing any provider issues.
const FAKE_KEY = "FAKE-KEY-NOT-A-REAL-KEY-0000";

const cred = (provider, model = "") => ({
  provider,
  model: model || credentialFromRequest({ aiProvider: provider, aiKey: FAKE_KEY }).model,
  key: FAKE_KEY,
});

// ── 1 · the credential is validated, never trusted ──────────────────────────

test("no credential fields at all means the proxy answers", () => {
  assert.equal(credentialFromRequest({ taskTitle: "run" }), null);
  assert.equal(credentialFromRequest({}), null);
  assert.equal(credentialFromRequest(undefined), null);
});

test("an unknown provider id is refused rather than guessed", () => {
  // The dangerous alternative is defaulting to GROQ, which would send an OpenAI
  // key to GROQ. Absent is the honest answer and it lands on the proxy path.
  assert.equal(credentialFromRequest({ aiProvider: "together", aiKey: FAKE_KEY }), null);
  assert.equal(credentialFromRequest({ aiProvider: "GROQ", aiKey: FAKE_KEY }), null);
});

test("a blank or whitespace-only key is no key", () => {
  assert.equal(credentialFromRequest({ aiProvider: "groq", aiKey: "" }), null);
  assert.equal(credentialFromRequest({ aiProvider: "groq", aiKey: "   " }), null);
});

test("non-string credential fields are refused", () => {
  assert.equal(credentialFromRequest({ aiProvider: 1, aiKey: FAKE_KEY }), null);
  assert.equal(credentialFromRequest({ aiProvider: "groq", aiKey: { k: FAKE_KEY } }), null);
});

test("a blank model falls back to the provider's default, per provider", () => {
  const defaults = {
    groq: "openai/gpt-oss-20b",
    openai: "gpt-4o-mini",
    anthropic: "claude-opus-5",
    gemini: "gemini-2.5-flash",
  };
  for (const [provider, expected] of Object.entries(defaults)) {
    const c = credentialFromRequest({ aiProvider: provider, aiKey: FAKE_KEY, aiModel: "  " });
    assert.equal(c.model, expected, `${provider} default`);
  }
});

test("a supplied model wins over the default, and is trimmed", () => {
  const c = credentialFromRequest({ aiProvider: "openai", aiKey: FAKE_KEY, aiModel: " gpt-4o " });
  assert.equal(c.model, "gpt-4o");
});

test("the key is trimmed — a pasted trailing newline is a 401 nobody can see the cause of", () => {
  const c = credentialFromRequest({ aiProvider: "groq", aiKey: `${FAKE_KEY}\n` });
  assert.equal(c.key, FAKE_KEY);
});

// ── 2 · each adapter's wire format ──────────────────────────────────────────

test("GROQ and OpenAI share a wire format but never a URL", () => {
  const g = buildRequest(cred("groq"), "sys", "usr");
  const o = buildRequest(cred("openai"), "sys", "usr");
  assert.match(g.url, /api\.groq\.com/);
  assert.match(o.url, /api\.openai\.com/);
  for (const r of [g, o]) {
    assert.equal(r.headers.Authorization, `Bearer ${FAKE_KEY}`);
    // #32 §7: the JSON shape is enforced NATIVELY by every adapter, because with
    // four providers a model swap is the normal case.
    assert.deepEqual(r.body.response_format, { type: "json_object" });
    assert.equal(r.body.messages[0].role, "system");
  }
});

test("Anthropic uses forced tool use, which is its native structured-output mechanism", () => {
  const r = buildRequest(cred("anthropic"), "sys", "usr");
  assert.equal(r.headers["x-api-key"], FAKE_KEY);
  assert.equal(r.headers["anthropic-version"], "2023-06-01");
  assert.equal(r.body.system, "sys");
  assert.deepEqual(r.body.tool_choice, { type: "tool", name: "answer" });
  assert.equal(r.body.tools[0].input_schema.type, "object");
  // The key must NOT be in an Authorization header for Anthropic — that is a
  // different vendor's shape, and sending it there is a silent 401.
  assert.equal(r.headers.Authorization, undefined);
});

test("Gemini enforces JSON by mime type, and the key never reaches the query string", () => {
  const r = buildRequest(cred("gemini"), "sys", "usr");
  assert.equal(r.headers["x-goog-api-key"], FAKE_KEY);
  assert.equal(r.body.generationConfig.responseMimeType, "application/json");
  // A query string is the one part of a URL that reliably lands in access logs.
  // Google's own quickstarts use `?key=`; #54's first hard rule forbids it here.
  assert.ok(!r.url.includes(FAKE_KEY), "the key must not appear in the URL");
  assert.ok(!r.url.includes("?"), "no query string at all");
});

test("the model id travels to every provider", () => {
  assert.equal(buildRequest(cred("groq", "m1"), "s", "u").body.model, "m1");
  assert.equal(buildRequest(cred("openai", "m2"), "s", "u").body.model, "m2");
  assert.equal(buildRequest(cred("anthropic", "m3"), "s", "u").body.model, "m3");
  // Gemini puts it in the path, not the body — so a naive `body.model` check
  // would pass on an adapter that dropped it entirely.
  assert.match(buildRequest(cred("gemini", "m4"), "s", "u").url, /models\/m4:generateContent/);
});

// ── 3 · pulling the answer back out ─────────────────────────────────────────

test("each provider's envelope yields the same object", () => {
  const want = { points: 12 };
  assert.deepEqual(
    extractJson("openai", { choices: [{ message: { content: JSON.stringify(want) } }] }),
    want,
  );
  assert.deepEqual(
    extractJson("anthropic", { content: [{ type: "tool_use", input: want }] }),
    want,
  );
  assert.deepEqual(
    extractJson("gemini", {
      candidates: [{ content: { parts: [{ text: JSON.stringify(want) }] } }],
    }),
    want,
  );
});

test("Anthropic falls back to a text block when the model ignored the forced tool", () => {
  const r = extractJson("anthropic", { content: [{ type: "text", text: '{"points":7}' }] });
  assert.deepEqual(r, { points: 7 });
});

test("an unusable reply is an empty object, never a throw", () => {
  // The client already treats an empty answer as "the model said nothing usable"
  // and lands on spec §8's deterministic fallback. Throwing here would turn a bad
  // reply into a dead-key message about a key that worked perfectly.
  assert.deepEqual(extractJson("openai", { choices: [{ message: { content: "sorry!" } }] }), {});
  assert.deepEqual(extractJson("openai", {}), {});
  assert.deepEqual(extractJson("gemini", { candidates: [] }), {});
  // A JSON *array* is valid JSON and is not an object; every caller indexes by
  // key, so an array is as unusable as prose.
  assert.deepEqual(extractJson("openai", { choices: [{ message: { content: "[1,2]" } }] }), {});
});

// ── 4 · #32 §5's failure table ──────────────────────────────────────────────

test("only the user can fix a 401, a 403 or a 402 — those are dead", () => {
  assert.equal(classifyStatus(401), "dead");
  assert.equal(classifyStatus(403), "dead");
  // Billing does not self-heal by waiting, which is the property that decides
  // this column — so 402 sits with 401 rather than with 429.
  assert.equal(classifyStatus(402), "dead");
});

test("429 is quota and self-heals; everything else is transient", () => {
  assert.equal(classifyStatus(429), "quota");
  assert.equal(classifyStatus(500), "transient");
  assert.equal(classifyStatus(503), "transient");
  assert.equal(classifyStatus(404), "transient");
  assert.equal(classifyStatus(0), "transient");
});

test("a failed call throws a class and a status, and the body is never read", async () => {
  let bodyRead = false;
  const fakeFetch = async () => ({
    ok: false,
    status: 401,
    // If the adapter ever reads this, the flag flips — which is the whole point.
    // #32's defect was a provider's raw error body reaching Cloud Logging, and
    // the fix is that there is no body in hand to log.
    text: async () => {
      bodyRead = true;
      return "Incorrect API key provided: sk-abc...xyz";
    },
    json: async () => {
      bodyRead = true;
      return {};
    },
  });

  await assert.rejects(
    () => callUserProvider(cred("openai"), "s", "u", fakeFetch),
    (e) => {
      assert.ok(e instanceof ProviderError);
      assert.equal(e.status, 401);
      assert.equal(e.failureClass, "dead");
      assert.equal(e.provider, "openai");
      // The message is what reaches a logger. It must carry no body and no key.
      assert.ok(!e.message.includes(FAKE_KEY));
      assert.ok(!e.message.includes("Incorrect API key"));
      assert.equal(e.body, undefined);
      return true;
    },
  );
  assert.equal(bodyRead, false, "the error body must not be read");
});

test("a network rejection is transient with status 0, and swallows the cause", async () => {
  // A fetch rejection can carry the request it failed on, and that request holds
  // the key — so the caught value is discarded rather than wrapped.
  const fakeFetch = async () => {
    throw new Error(`connect ETIMEDOUT while sending ${FAKE_KEY}`);
  };
  await assert.rejects(
    () => callUserProvider(cred("anthropic"), "s", "u", fakeFetch),
    (e) => {
      assert.ok(e instanceof ProviderError);
      assert.equal(e.status, 0);
      assert.equal(e.failureClass, "transient");
      assert.ok(!e.message.includes(FAKE_KEY));
      return true;
    },
  );
});

test("a successful call returns the parsed object", async () => {
  const fakeFetch = async (url, init) => {
    // The key travels in a header, and the body is JSON — assert both here so a
    // regression in either shows up as a failing call rather than a silent 401.
    assert.equal(JSON.parse(init.body).model, "openai/gpt-oss-20b");
    assert.equal(init.headers.Authorization, `Bearer ${FAKE_KEY}`);
    assert.match(url, /api\.groq\.com/);
    return {
      ok: true,
      status: 200,
      json: async () => ({ choices: [{ message: { content: '{"points":30}' } }] }),
    };
  };
  assert.deepEqual(await callUserProvider(cred("groq"), "s", "u", fakeFetch), { points: 30 });
});
