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
export { projectPoints, projectChallengeScore } from "./projection";

// `classify`'s validator (spec §3.4), in its own Firebase-free module so
// `test/classify.test.mjs` can run it with no emulator — same arrangement as
// `derived.ts`. `CATEGORIES` lives there too: the list the prompt offers and the
// list the response is checked against must be one list, or the prompt can drift
// into offering a value the validator then silently drops.
import { CATEGORIES, listsFromRequest, validateClassification } from "./classify";

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
// GROQ deprecated llama-3.1-8b-instant on 2026-06-17 (shutdown 2026-08-16) and
// recommends openai/gpt-oss-20b as its replacement. Override per deployment with
// the GROQ_MODEL env var; check https://console.groq.com/docs/deprecations before
// pinning a new one.
const DEFAULT_MODEL = "openai/gpt-oss-20b";


/** Calls GROQ's OpenAI-compatible chat endpoint and parses a JSON object reply. */
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
    const json = await callGroqJson(system, user);
    return { recommendations: Array.isArray(json?.recommendations) ? json.recommendations : [] };
  } catch (e) {
    logger.error("getRecommendations failed", e);
    return { recommendations: [] };
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
    "\"estimatedPoints\":number,\"estimatedMinutes\":number," +
    "\"confidence\":number,\"rationale\":string}. " +
    "suggestedGoalId MUST be one of the given goal ids, or null if no existing goal fits — " +
    "never invent an id and never adapt one. " +
    "suggestedLifeAreaId MUST be one of the given life area ids, or null if none fits. " +
    "suggestedNewGoalTitle is only meaningful when suggestedGoalId is null. " +
    "estimatedPoints is 5-50 by difficulty. " +
    "estimatedMinutes is how long the task realistically takes to do, 5-480, " +
    "for one sitting of it (a 30-minute run is 30, not the whole training plan). " +
    "confidence is 0..1. The task title may be in any language, including Hebrew.";
  const user =
    `Task: "${taskTitle}". Existing goals: ${JSON.stringify(goals)}. ` +
    `Life areas: ${JSON.stringify(lifeAreas)}.`;

  try {
    return validateClassification(await callGroqJson(system, user), lists);
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
    throw e;
  }
});

/**
 * Estimate what a task costs (spec §6 Core: point scoring) — difficulty in points
 * and duration in minutes.
 *
 * One call returns both because the free GROQ tier allows 30 requests/minute and
 * the Google Tasks import already spends one per imported row; asking twice about
 * the same sentence would halve what a single import can cover.
 */
export const scoreTask = onCall(async (request: CallableRequest) => {
  const { taskTitle = "" } = request.data ?? {};
  const system =
    "Estimate difficulty points (integer 5-50) and duration in minutes (integer 5-480) " +
    "for one sitting of a personal task. The title may be in any language, including Hebrew. " +
    "Reply ONLY with JSON: {\"points\":number,\"minutes\":number}.";
  try {
    const json = await callGroqJson(system, `Task: "${taskTitle}"`);
    const points = Number(json?.points);
    const minutes = Number(json?.minutes);
    const safePoints = Number.isFinite(points)
      ? Math.min(50, Math.max(5, Math.round(points)))
      : 10;
    return {
      points: safePoints,
      minutes: Number.isFinite(minutes)
        ? Math.min(480, Math.max(5, Math.round(minutes)))
        : safePoints * 3,
    };
  } catch (e) {
    logger.error("scoreTask failed", e);
    return { points: 10, minutes: 30 };
  }
});
