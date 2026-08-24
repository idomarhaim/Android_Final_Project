/**
 * GoalPilot Cloud Functions — a thin GROQ (LLM) proxy.
 *
 * The client calls these HTTPS-callable functions; the GROQ API key lives only
 * here (spec §5: "proxy the calls through Firebase Cloud Functions so the API
 * key is not exposed on the client side"). Every handler degrades gracefully:
 * if the key is missing or GROQ errors, it returns an empty/neutral result and
 * the Android client falls back to local heuristics (spec §8).
 */
import { onCall, CallableRequest } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";
import * as logger from "firebase-functions/logger";
import { initializeApp } from "firebase-admin/app";

// Region must match FirebaseFunctions.getInstance(region) on the client.
setGlobalOptions({ region: "us-central1", maxInstances: 5 });

// The Admin SDK, for the projection triggers re-exported below. It bypasses
// `firestore.rules` entirely, which is what lets those rules deny the client the two
// fields this function owns without the function needing an exemption written into them.
initializeApp();

/**
 * `C20`'s projection (`docs/PRODUCT_v0.3.md` §5.2) — one projection, two trigger
 * registrations, and zero client writers of derived state. Re-exported here because
 * `firebase deploy` reads the entry point named by `package.json#main` and deploys
 * exactly the symbols it exports.
 *
 * These are the first Firestore triggers in this file; everything above is an HTTPS
 * callable the client invokes directly.
 */
// `projectPointsOnTaskWrite` is #55's second registration and is NOT optional: a task
// DELETED while done removes its fact in the same batch, but a task completed BEFORE #55
// has no fact to delete -- only its own legacy `done` field, which the completionFacts
// trigger never sees. Omit it and those totals stop tracking their own deletions.
export {
  projectPoints,
  projectPointsOnTaskWrite,
  projectChallengeScore,
  projectChallengeScoreOnProgress,
} from "./projection";

// `classify`'s validator (spec §3.4), in its own Firebase-free module so
// `test/classify.test.mjs` can run it with no emulator — same arrangement as
// `derived.ts`. `CATEGORIES` lives there too: the list the prompt offers and the
// list the response is checked against must be one list, or the prompt can drift
// into offering a value the validator then silently drops.
import { CATEGORIES, listsFromRequest, validateClassification, asDifficulty } from "./classify";

// `measure`'s validator (spec §3.3 E, §3.4) -- `C7`'s fifth AI feature, which `C11b` never
// wrote a format for (§10.1). Firebase-free like `classify.ts`, so `test/measure.test.mjs`
// runs under plain `node --test`. `MEASURE_KINDS` lives there so the list the prompt OFFERS
// and the list the response is CHECKED against cannot drift into disagreeing.
import {
  MEASURE_KINDS,
  BASES,
  TARGET_SOURCES,
  MAX_WORD,
  listsFromMeasureRequest,
  validateProposals,
} from "./measure";

// `plan`'s validator (spec §3.3 B, §3.7) -- `C8` #24's schema, Firebase-free like the two
// above so `test/plan.test.mjs` runs under plain `node --test`. `LABELS`, `DIFFICULTIES` and
// the two bounds live there so the list the prompt OFFERS and the list the response is CHECKED
// against are one list, exactly as `CATEGORIES` and `MEASURE_KINDS` already are.
import {
  LABELS,
  DIFFICULTIES as PLAN_DIFFICULTIES,
  MAX_DAY_OFFSET,
  MAX_ITEMS,
  MIN_MINUTES as PLAN_MIN_MINUTES,
  MAX_MINUTES as PLAN_MAX_MINUTES,
  validatePlan,
} from "./plan";

// `C13`'s four provider adapters (#54, decided in #32). Firebase-free for the same
// reason `classify.ts` is: `test/providers.test.mjs` runs them under plain
// `node --test` with no emulator.
import {
  callUserProvider,
  credentialFromRequest,
  ProviderError,
  type FailureClass,
} from "./providers";

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
// GROQ deprecated llama-3.1-8b-instant on 2026-06-17 (shutdown 2026-08-16) and
// recommends openai/gpt-oss-20b as its replacement. Override per deployment with
// the GROQ_MODEL env var; check https://console.groq.com/docs/deprecations before
// pinning a new one.
const DEFAULT_MODEL = "openai/gpt-oss-20b";


/**
 * Calls GROQ's OpenAI-compatible chat endpoint on **the project's own key** and
 * parses a JSON object reply — rung two of `C13`'s ladder, and the only rung
 * this app had before `C13`.
 *
 * The error body is still interpolated into the thrown message, and that is
 * deliberate rather than an oversight: this key is GoalPilot's, its error text
 * is GoalPilot's, and losing it would make the free tier harder to debug for
 * nothing. #32's spec line is scoped to a **user-key** call, and a user's key
 * never reaches this function — `providers.ts` handles those and never reads a
 * response body at all.
 */
async function callGroqJson(system: string, user: string): Promise<any> {
  const key = process.env.GROQ_API_KEY;
  if (!key) throw new Error("GROQ_API_KEY is not configured");
  const model = process.env.GROQ_MODEL || DEFAULT_MODEL;

  const res = await fetch(GROQ_URL, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${key}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      temperature: 0.7,
      response_format: { type: "json_object" },
      messages: [
        { role: "system", content: system },
        { role: "user", content: user },
      ],
    }),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`GROQ HTTP ${res.status}: ${body}`);
  }
  const data = (await res.json()) as any;
  const content = data?.choices?.[0]?.message?.content ?? "{}";
  return JSON.parse(content);
}

/**
 * What a callable reports about **who answered** — `C13`'s honesty half
 * (#54 piece 3, #32 §5).
 *
 * Spread into every callable's own return value rather than wrapping it, so the
 * response shape stays backward compatible: a client that predates `C13` reads
 * the fields it always read and ignores these.
 */
interface Answered {
  json: any;
  answeredBy: "user" | "proxy";
  keyError?: { class: FailureClass; status: number };
}

/**
 * #32 §5's ladder — **rungs one and two**, which are both this function's.
 *
 * ```
 * user key  ->  free model (GROQ, project key)  ->  local fallback (spec §8)
 * ```
 *
 * The third rung is the Android client's and is reached by this function
 * throwing or by it reporting that nothing answered. The ladder **never
 * degrades below where the app stood before `C13`**: with no credential in the
 * payload this is exactly the old `callGroqJson` call, byte for byte.
 *
 * ## What is logged when a user's key fails
 *
 * The adapter, the status and the class. **Never the provider's error body** —
 * the spec line #32 filed after finding `callGroqJson` throwing
 * `GROQ HTTP ${status}: ${body}` into `logger.error`, which is harmless on the
 * project's own key and is a third-party provider's error text in Google's logs
 * the moment a user key is in play. `providers.ts` makes that structural: its
 * `ProviderError` has no body field to log, and the response body is never read.
 *
 * `logger.warn` and not `logger.error`: a user key failing is an expected state
 * of this system with a defined behaviour, not a fault of the service.
 */
async function answer(
  request: CallableRequest,
  system: string,
  user: string,
): Promise<Answered> {
  const cred = credentialFromRequest(request.data);
  if (!cred) return { json: await callGroqJson(system, user), answeredBy: "proxy" };

  try {
    return { json: await callUserProvider(cred, system, user), answeredBy: "user" };
  } catch (e) {
    const keyError = e instanceof ProviderError
      ? { class: e.failureClass, status: e.status }
      : { class: "transient" as FailureClass, status: 0 };
    logger.warn("user provider failed, falling back to the free model", {
      provider: cred.provider,
      status: keyError.status,
      class: keyError.class,
    });
    // Rung two. If THIS throws as well, it propagates to the caller's catch,
    // which is rung three — and `keyError` goes with it, because a dead key is
    // still dead when the free model is down too.
    try {
      return { json: await callGroqJson(system, user), answeredBy: "proxy", keyError };
    } catch (proxyError) {
      throw new LadderError(keyError, proxyError);
    }
  }
}

/**
 * Both rungs failed. Carries the user-key class forward so the caller can still
 * report it — the combination where the app is furthest from working is the one
 * where it must not go quiet. See `AiAnswer.keyFailure` on the Kotlin side.
 */
class LadderError extends Error {
  constructor(
    readonly keyError: { class: FailureClass; status: number },
    readonly cause: unknown,
  ) {
    super("both the user key and the free model failed");
    this.name = "LadderError";
  }
}

/** The `answeredBy` / `keyError` half of a response, ready to spread. */
function provenance(a: Answered) {
  return a.keyError
    ? { answeredBy: a.answeredBy, keyError: a.keyError }
    : { answeredBy: a.answeredBy };
}

/**
 * The same half for a call where **nothing answered** — the third rung, reported
 * rather than inferred.
 *
 * `"none"` is deliberately distinct from omitting the field: an omitted
 * `answeredBy` means a deployment that predates `C13` and really did answer on
 * the project key, so collapsing the two would make an outage read as *"the free
 * model answered"*. `AiCallEnvelope.answeredBy` states the same rule from the
 * other end.
 */
function nothingAnswered(e: unknown) {
  const keyError = e instanceof LadderError ? e.keyError : undefined;
  return keyError ? { answeredBy: "none", keyError } : { answeredBy: "none" };
}

/** Recommendations + encouragement (spec §6 Core). */
export const getRecommendations = onCall(async (request: CallableRequest) => {
  const { goals = [], completedTasksLast7d = 0, totalPoints = 0 } = request.data ?? {};

  const system =
    "You are GoalPilot, an upbeat but practical life-goal coach. " +
    "Reply ONLY with a JSON object of the exact shape: " +
    "{\"recommendations\":[{\"id\":string,\"title\":string,\"message\":string," +
    "\"type\":\"ENCOURAGEMENT|SUGGESTION|WARNING|INSIGHT\"," +
    "\"relatedGoalId\":string|null}]}. " +
    "Return at most 4 short, specific, motivating items.";
  const user =
    `Goals: ${JSON.stringify(goals)}. ` +
    `Completed tasks in the last 7 days: ${completedTasksLast7d}. ` +
    `Total points: ${totalPoints}. ` +
    "Give recommendations to keep momentum and improve weaker goals.";

  try {
    const a = await answer(request, system, user);
    return {
      recommendations: Array.isArray(a.json?.recommendations) ? a.json.recommendations : [],
      ...provenance(a),
    };
  } catch (e) {
    logger.error("getRecommendations failed", e);
    // The client falls back to spec §8's local guidance on an empty list, so the
    // honest report is that NOTHING answered — not that the free model did.
    return { recommendations: [], ...nothingAnswered(e) };
  }
});

/**
 * `classify` — which existing goal a free-text task belongs to, or that none does
 * (`docs/PRODUCT_v0.3.md` §3.3 D, §3.4; `#6`, `R3`).
 *
 * **This is the highest-volume AI call in the app** — one per quick-add and one per
 * imported Google Tasks row — **and the one where the only measured failure lives**:
 * of `C11a`'s 248 live calls the sole two failures were *semantic*, a plausible id
 * that was not real. So every echoed id is checked against the list that travelled
 * with the request, here and nowhere else (§3.4: *"validation lives in the Cloud
 * Function, singly"*).
 *
 * **What the client does with the answer is decided by presence, not by value.** An
 * absent `suggestedGoalId` is the new-goal branch, which is the one row of §3.4's
 * fallback table that speaks; every other absence is silent. That is why nothing in
 * here substitutes: a fabricated value would be indistinguishable from an answer at
 * exactly the field the branch is chosen on.
 *
 * Also carries the estimate group — what the task is worth and **how many minutes it
 * takes**, the raw material of the time-allocation chart — validated independently of
 * the filing fields, so a nonsense duration costs the user nothing but the duration.
 */
export const classifyTask = onCall(async (request: CallableRequest) => {
  const { taskTitle = "", goals = [], lifeAreas = [] } = request.data ?? {};
  const lists = listsFromRequest(request.data);

  const system =
    "Classify a task into one of the user's existing goals, or suggest creating a new goal, " +
    "and file it under one of the user's life areas. " +
    "Reply ONLY with a JSON object: " +
    "{\"suggestedGoalId\":string|null,\"suggestedNewGoalTitle\":string|null," +
    `\"suggestedCategory\":\"${CATEGORIES.join("|")}\",` +
    "\"suggestedLifeAreaId\":string|null," +
    "\"difficulty\":string,\"estimatedMinutes\":number," +
    "\"confidence\":number,\"rationale\":string}. " +
    "suggestedGoalId MUST be one of the given goal ids, or null if no existing goal fits — " +
    "never invent an id and never adapt one. " +
    "suggestedLifeAreaId MUST be one of the given life area ids, or null if none fits. " +
    "suggestedNewGoalTitle is only meaningful when suggestedGoalId is null. " +
    "difficulty is exactly one of LIGHT, ROUTINE or DEMANDING -- how demanding the " +
    "work is, never how long it takes (#55, §3.3 A: there is no points field). " +
    "estimatedMinutes is how long the task realistically takes to do, 5-480, " +
    "for one sitting of it (a 30-minute run is 30, not the whole training plan). " +
    "confidence is 0..1. The task title may be in any language, including Hebrew.";
  const user =
    `Task: "${taskTitle}". Existing goals: ${JSON.stringify(goals)}. ` +
    `Life areas: ${JSON.stringify(lifeAreas)}.`;

  try {
    const a = await answer(request, system, user);
    return { ...validateClassification(a.json, lists), ...provenance(a) };
  } catch (e) {
    // §3.4, the **transport** class: whole-response fallback, silent, no retry — and
    // the whole-response fallback is the CLIENT's (spec §8), which has to work offline
    // anyway and can keyword-match against goals this function never received in full.
    //
    // This used to return a fabricated object here: `suggestedGoalId: null` plus
    // `suggestedNewGoalTitle: taskTitle.slice(0, 40)` and a made-up 10 points / 30
    // minutes. Two things were wrong with it. It was a **second** implementation of the
    // client's own fallback, one the client could not tell apart from a real answer; and
    // because it nulled the goal id, every transport failure took the **new-goal
    // branch** — the one branch in §3.4's table that speaks. A dead network made the app
    // announce a new goal. Throwing puts the failure back where the contract says it is.
    logger.error("classifyTask failed", e);
    // `C13` note: this stays a THROW rather than a `nothingAnswered` object,
    // because §3.4's transport class hands the whole response to the client's own
    // fallback and an object here would be the second implementation that
    // paragraph exists to forbid. The client's catch records `AiAnswer.Local`,
    // which is the same third rung by a different route — what it loses is the
    // user-key CLASS, so a dead key discovered on a classify call is reported by
    // the next feed or scoring call instead of this one.
    throw e;
  }
});

/**
 * Estimate what a task costs — **how demanding it is** and how long it takes
 * (spec §3.3 A, §1.4, #55).
 *
 * One call returns both because the free GROQ tier allows 30 requests/minute and
 * the Google Tasks import already spends one per imported row; asking twice about
 * the same sentence would halve what a single import can cover.
 *
 * ### There is no `points` field, and there never will be (§3.3 A)
 *
 * This used to prompt for `points` as an integer 5-50 and return one. §1.4 computes points
 * as `round(minutes / 3) × difficulty` **in the app**, with the multipliers in Kotlin and
 * never in a prompt, because a model that can emit a number can move a currency by phrasing.
 * `C11a` measured this model's free numbers swinging 2x run-to-run and 1.8x between
 * languages; a prompt-declared enum is the one thing it was measured perfect at.
 *
 * So the contract narrowed from *a number in a range* to *one of three words*, and the
 * failure mode narrowed with it: an unparseable answer is `ROUTINE`, which is x1.0 and prices
 * the task on its minutes alone -- the absence of a judgement rather than a guess at one.
 */
export const scoreTask = onCall(async (request: CallableRequest) => {
  const { taskTitle = "" } = request.data ?? {};
  const system =
    "Judge how demanding a personal task is and how long one sitting of it takes. " +
    "difficulty is exactly one of LIGHT, ROUTINE or DEMANDING: LIGHT for waiting, " +
    "idling or half-attended work; ROUTINE for ordinary focused work; DEMANDING for " +
    "concentrated, unpleasant or draining work. minutes is an integer 5-480. " +
    "The title may be in any language, including Hebrew. " +
    "Reply ONLY with JSON: {\"difficulty\":string,\"minutes\":number}.";
  try {
    const a = await answer(request, system, `Task: "${taskTitle}"`);
    const minutes = Number(a.json?.minutes);
    return {
      difficulty: asDifficulty(a.json?.difficulty),
      // Absent when the model did not say (#9, §3.4). It used to fall back to
      // `points * 3` -- a duration manufactured out of a score, which is the inversion
      // #55 deleted. The client asks the user instead.
      ...(Number.isFinite(minutes)
        ? { minutes: Math.min(480, Math.max(5, Math.round(minutes))) }
        : {}),
      ...provenance(a),
    };
  } catch (e) {
    logger.error("scoreTask failed", e);
    // No judgement and no duration: nothing spoke. ROUTINE is x1.0, so the client prices
    // the task on whatever minutes the user supplies and nothing has been invented.
    return { difficulty: "ROUTINE", ...nothingAnswered(e) };
  }
});

/**
 * `measure` — a concrete measure for a goal that has none (spec §1.3, §3.3 E, §3.4;
 * `C7` #14, `C22` #44, #65).
 *
 * **`C7`'s fifth AI feature, and the one `C11b` never wrote a format for.** §10.1 carries
 * the whole account of how the hand-off was lost; the short version is that two tickets each
 * said *"fifth"* about a different feature.
 *
 * ## What this call may and may not author
 *
 * It authors a **kind**, a **word**, a **basis** and the **name of an arithmetic**. It does
 * NOT author a number, and there is no field here it could put one in — §3.3 E, and §0.5's
 * strongest form. `C11a` measured free numbers swinging 2x run-to-run; the target is what
 * the feature would be judged on, so the client computes it from `C9a`'s schedule and
 * `C18`'s sub-tree, both of which it already holds.
 *
 * ## Wide by contract, and one goal per call today
 *
 * §3.2's wide call: the request carries `goals[]` and the response carries `proposals[]`,
 * so batching costs nothing to add later. The client currently sends **one goal — the one
 * being opened** — because §1.3 puts the offer only on the goal's own screen, so firing over
 * the whole list would spend calls on goals nobody opened. The load ceiling is unchanged
 * either way: dismissal is permanent (§1.3), so a goal is proposed **at most once, ever**,
 * and the feature adds no recurring load against the free tier's 30 RPM.
 *
 * ## Failure is silent, and the client has a real fallback rather than a degraded one
 *
 * §3.4's `measure` row: any absent field falls back to the **mechanical proposal** —
 * `openStepCount >= 2` counts the steps, else `occurrencesPerWeek >= 1` counts the
 * occurrences, else nothing at all and the app says nothing. That lives in
 * `ProposeMeasureUseCase` on the client, where it also has to work offline. So on transport
 * failure this returns an **empty list** rather than throwing: unlike `classify`, whose
 * whole-response fallback needs the client to see a failure, an empty `proposals` here is
 * both the honest report and exactly the input the mechanical path wants.
 */
export const proposeMeasure = onCall(async (request: CallableRequest) => {
  const { language = "en", goals = [] } = request.data ?? {};
  const lists = listsFromMeasureRequest(request.data);

  const system =
    "Propose ONE concrete measure for each goal that currently has no number. " +
    "Reply ONLY with a JSON object: {\"proposals\":[{\"goalId\":string," +
    `\"measureKind\":\"${MEASURE_KINDS.join("|")}\",` +
    `\"word\":string,\"basis\":\"${BASES.join("|")}\",` +
    `\"targetSource\":\"${TARGET_SOURCES.join("|")}\"}]}. ` +
    "goalId MUST be one of the given goal ids -- never invent one and never adapt one; " +
    "omit the goal entirely rather than guess. " +
    `word is the user's own unit for what is counted ("runs a week", "pages", "km"), ` +
    `at most ${MAX_WORD} characters, authored in the language named below. ` +
    "basis is OUTCOME when the number says how far along the goal itself is, or LEADING " +
    "when it measures the recurring behaviour that produces the outcome -- prefer LEADING " +
    "over inventing an outcome number for a goal whose result cannot honestly be counted. " +
    "targetSource names WHERE THE APP GETS THE NUMBER, and you must not supply the number " +
    "itself: SCHEDULE if the goal's occurrencesPerWeek is the right target, STEPS if its " +
    "openStepCount is, USER if neither and the person must set it. " +
    "Propose nothing for a goal you cannot phrase a real unit for -- a missing proposal is " +
    "correct, and a vague one is not.";
  const user =
    `Language: ${language}. Goals: ${JSON.stringify(goals)}.`;

  try {
    const a = await answer(request, system, user);
    return { proposals: validateProposals(a.json, lists), ...provenance(a) };
  } catch (e) {
    // §3.4's transport class: silent, no retry. An EMPTY list rather than a throw, and the
    // difference from `classifyTask` above is the difference in their fallbacks -- the
    // client's mechanical proposal is not a second implementation of this call, it is a
    // different (arithmetic) answer to the same question, so handing it an empty list is
    // handing it exactly its own input. Nothing is fabricated: zero proposals is what a
    // call that did not happen produced.
    logger.error("proposeMeasure failed", e);
    return { proposals: [], ...nothingAnswered(e) };
  }
});

/**
 * `fileGoal` — which life area a **goal** belongs to, from its title alone (spec §3.3 D's
 * schema, §0.7's silence rule; Ido 2026-08-24).
 *
 * ## Why this is `classify`'s schema and not a new one
 *
 * §3.3 D already answers *"which goal, **which life area**, or a new goal"* — for a **task**.
 * The question asked here is that schema's middle third asked about a goal, so it reuses
 * `classify.ts`'s validator unchanged rather than growing a second copy of the same membership
 * test. The lists arrive the same way: `goals[]` is simply absent, so `goalIds` is empty, every
 * `suggestedGoalId` fails membership and the field can never appear. That is the reuse being
 * safe **structurally** rather than by a promise not to send one.
 *
 * ## It is silent, and that is §0.7 rather than a UX preference
 *
 * > The app may act silently on **instrumental** structure, but must ask before asserting an
 * > **intrinsic** edge.
 *
 * Filing a goal under a life area is instrumental — it decides which column of the time chart
 * the goal reports into, and it is one tap to change. Deciding the goal **exists** is intrinsic,
 * and that is the user's own act here: they typed the title. So no dialog, no toggle, and no
 * default to configure, exactly as `SmartFiling` already decided for tasks.
 *
 * ## What the client does with an absent field
 *
 * Nothing. An absent `suggestedLifeAreaId` leaves the goal **unfiled**, which §1.2 makes a
 * legitimate state — not a degraded one — and an absent `suggestedCategory` leaves whatever the
 * form already had. There is no branch here that speaks, and that is the whole of §3.4's
 * treatment of this call.
 */
export const fileGoal = onCall(async (request: CallableRequest) => {
  const { goalTitle = "", lifeAreas = [] } = request.data ?? {};
  const lists = listsFromRequest(request.data);

  const system =
    "File a personal goal under one of the user's own life areas, and pick the category " +
    "that best fits it. Reply ONLY with a JSON object: " +
    "{\"suggestedLifeAreaId\":string|null," +
    `\"suggestedCategory\":\"${CATEGORIES.join("|")}\",` +
    "\"confidence\":number,\"rationale\":string}. " +
    "suggestedLifeAreaId MUST be one of the given life area ids, or null if none of them " +
    "genuinely fits -- never invent an id and never adapt one. " +
    "Judge from the goal's title alone; the user has not said where it belongs. " +
    "confidence is 0..1. The title may be in any language, including Hebrew.";
  const user =
    `Goal: "${goalTitle}". Life areas: ${JSON.stringify(lifeAreas)}.`;

  try {
    const a = await answer(request, system, user);
    return { ...validateClassification(a.json, lists), ...provenance(a) };
  } catch (e) {
    // §3.4's transport class: silent, no retry. An OBJECT rather than a throw, and the
    // difference from `classifyTask` is that this call has no fallback on the client to hand
    // the whole response to -- there is nothing to keyword-match a life area against that
    // would not be a second, worse copy of this judgement. An empty object leaves the goal
    // unfiled, which is a state §1.2 already calls legitimate, so nothing is fabricated and
    // nothing is lost but the suggestion.
    logger.error("fileGoal failed", e);
    return { ...nothingAnswered(e) };
  }
});

/**
 * `planGoal` — §3.3 **B**'s `plan`, and §3.7's proposed draft (`C8` #24).
 *
 * ## The draft gate is the reason the model is allowed this much latitude
 *
 * §3.7: *"nothing the model decides here may reach Firestore without passing his eyes."* This
 * function therefore returns a **proposal** and writes nothing; the client shows it as a draft
 * with a keep/drop per step, and only what survives that sheet becomes a task. Every other
 * restraint in this file exists because a call writes something; this one is loose on purpose
 * and gated at the other end.
 *
 * ## Two things the model may not author, both enforced in `plan.ts`
 *
 * - **No ids.** A new step carries none and is identified by its **position** (§3.3 B). The
 *   client mints the id, which makes a truncated or duplicated id unrepresentable rather than
 *   merely checked.
 * - **No dates.** It answers in `dayOffset` — days from the day the plan is applied — and the
 *   app resolves that against its own clock and zone. §0.5, and the practical half: a draft
 *   §3.7 says has **no expiry** would otherwise carry dates that were true when it was drawn.
 *
 * ## Failure is reported, not swallowed
 *
 * Unlike `proposeMeasure`, whose empty list is exactly what the client's mechanical fallback
 * wants, there is no arithmetic that produces a plan. So a transport failure **throws**, the
 * client records `AiAnswer.Local` and the sheet says the plan could not be fetched — §0.4:
 * this call is one the user pressed a button for, and a failure they can act on (press it
 * again) must not be silent.
 */
/**
 * Whether a thrown error is GROQ rejecting **its own model's** output as invalid JSON while
 * reporting **nothing** it generated.
 *
 * ```
 * GROQ HTTP 400: {"code":"json_validate_failed", "failed_generation":""}
 * ```
 *
 * `Observed:` 2026-08-24, immediately after `planGoal` first deployed — **two of three** live
 * calls for the same goal failed this way while the third returned a clean ten-item plan, and a
 * Hebrew call succeeded throughout. The empty `failed_generation` is the tell: the model
 * (`openai/gpt-oss-20b`, a reasoning model) spent its budget reasoning and emitted no content at
 * all, so `response_format: json_object` had nothing to validate. It is a property of a long
 * generation on this model, not of the request — which is why the same request succeeds on the
 * next attempt.
 *
 * `Untested:` whether the other four callables can hit it. None has been seen to, and all four
 * ask for far shorter answers; `plan` is by a wide margin the longest generation in this file.
 */
function isEmptyJsonGeneration(e: unknown): boolean {
  const message = e instanceof Error ? e.message : String(e);
  return message.includes("json_validate_failed") && message.includes('"failed_generation":""');
}

/**
 * [answer], with **one** extra attempt when the model returns nothing at all.
 *
 * ## Why this is not the retry §3.4 forbids
 *
 * §3.4's `transport` class — *"no key, `5xx`, timeout, `429`, a retired model id"* — is
 * **silent, no retry**, and that rule stands untouched: every one of those means the request did
 * not get through, and repeating it spends a second call against a 30-RPM ceiling to learn the
 * same thing. This is a different event. The request got through, the provider answered `400`,
 * and what it is complaining about is **its own output being empty** — a per-generation dice
 * roll (see [isEmptyJsonGeneration]) which the next attempt genuinely re-rolls.
 *
 * ## Why only here, and why only once
 *
 * `plan` is the one call in this file with **no fallback of any kind** on the client — there is
 * no arithmetic that produces a plan — and it is the one the user pressed a button for. The
 * other four all degrade into something usable, so a retry would buy them nothing they do not
 * already have. And once, not until-it-works: two attempts turn a measured ~1-in-3 failure into
 * ~1-in-9, while an unbounded loop would spend the user's whole quota on a bad minute.
 */
async function answerRetryingEmptyJson(
  request: CallableRequest,
  system: string,
  user: string,
): Promise<Answered> {
  try {
    return await answer(request, system, user);
  } catch (e) {
    if (!isEmptyJsonGeneration(e)) throw e;
    logger.warn("planGoal: the model returned no content; retrying once");
    return await answer(request, system, user);
  }
}

export const planGoal = onCall(async (request: CallableRequest) => {
  const { language = "en", goalTitle = "", goalDescription = "", measureWord = "" } =
    request.data ?? {};

  const system =
    "Propose a short, concrete work plan for one personal goal. " +
    "Reply ONLY with a JSON object: {\"items\":[{\"title\":string," +
    `\"label\":\"${LABELS.join("|")}\",` +
    `\"difficulty\":\"${PLAN_DIFFICULTIES.join("|")}\",` +
    "\"estimatedMinutes\":number,\"dayOffset\":number,\"timeOfDay\":string}]}. " +
    `Give between 5 and ${MAX_ITEMS} items, ordered from soonest to latest -- ${MAX_ITEMS} is a ` +
    "hard ceiling and a long plan is not a better one. " +
    // The ceiling is an OUTPUT-TOKEN budget as much as a design choice: a 19-item plan
    // truncated mid-JSON and GROQ rejected its own reply with `json_validate_failed`
    // (2026-08-24, live). Short titles are the other half of the same budget.
    "Keep every title under eight words. " +
    "label is STATE_YOU_REACH for a milestone -- a state the person reaches, which is not " +
    "work and is never priced -- or WORK_YOU_DO for a thing they actually sit down and do. " +
    "difficulty, estimatedMinutes and timeOfDay are for WORK_YOU_DO items ONLY and MUST be " +
    "omitted entirely on a STATE_YOU_REACH item. " +
    `estimatedMinutes is ${PLAN_MIN_MINUTES}-${PLAN_MAX_MINUTES}, for one sitting of the step. ` +
    "difficulty is exactly one of LIGHT, ROUTINE or DEMANDING -- how demanding the work is, " +
    "never how long it takes. " +
    `dayOffset is a whole number of days from TODAY, 0-${MAX_DAY_OFFSET}: 0 is today, 1 is ` +
    "tomorrow. NEVER answer with a calendar date -- the app owns the calendar. " +
    // ⚠️ **Measured, not guessed.** The first live call after deploying this function returned a
    // 19-step marathon plan in which all 7 STATE_YOU_REACH items carried NO `dayOffset` -- so
    // every milestone landed undated while every WORK_YOU_DO item got a date. The sentence above
    // about the three leaf-only fields is what caused it: the model generalised "omit these on a
    // container" to include `dayOffset`, which is not on that list. The validator could not
    // catch it, because a step with no date is legal (§2.2) -- it is exactly what
    // `Task.occurrence = null` means. It is a PROMPT defect and only reading the real answer
    // finds it.
    "EVERY item needs a dayOffset, INCLUDING a STATE_YOU_REACH item -- a milestone is a date " +
    "you reach it by, and a plan whose milestones have no dates is not a schedule. " +
    "timeOfDay is \"HH:MM\" on a 24-hour clock, and only for a step that genuinely wants a " +
    "slot in the day; omit it for a step that is simply due that day. " +
    "Spread the plan realistically over time rather than piling it into one week. " +
    "Write every title in the language named below, and write it as the person would.";
  const user =
    `Language: ${language}. Goal: "${goalTitle}". ` +
    (goalDescription ? `Description: "${goalDescription}". ` : "") +
    (measureWord ? `What it counts: "${measureWord}". ` : "");

  try {
    const a = await answerRetryingEmptyJson(request, system, user);
    return { ...validatePlan(a.json), ...provenance(a) };
  } catch (e) {
    logger.error("planGoal failed", e);
    // A THROW, like `classifyTask` and unlike `proposeMeasure`. There is no arithmetic on the
    // client that produces a plan, so an empty `items` here would be indistinguishable from a
    // model that had nothing to propose -- and the user pressed a button. See the KDoc above.
    throw e;
  }
});
