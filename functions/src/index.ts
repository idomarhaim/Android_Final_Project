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

// Region must match FirebaseFunctions.getInstance(region) on the client.
setGlobalOptions({ region: "us-central1", maxInstances: 5 });

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
// GROQ deprecated llama-3.1-8b-instant on 2026-06-17 (shutdown 2026-08-16) and
// recommends openai/gpt-oss-20b as its replacement. Override per deployment with
// the GROQ_MODEL env var; check https://console.groq.com/docs/deprecations before
// pinning a new one.
const DEFAULT_MODEL = "openai/gpt-oss-20b";

const CATEGORIES = [
  "HEALTH", "FITNESS", "SLEEP", "NUTRITION", "RELATIONSHIPS",
  "CAREER", "PROJECTS", "LEARNING", "FINANCE", "OTHER",
];

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

/** Classify a free-text task onto a goal, or suggest a new goal (spec §6 Bonus). */
export const classifyTask = onCall(async (request: CallableRequest) => {
  const { taskTitle = "", goals = [] } = request.data ?? {};

  const system =
    "Classify a task into one of the user's existing goals, or suggest creating a new goal. " +
    "Reply ONLY with a JSON object: " +
    "{\"suggestedGoalId\":string|null,\"suggestedNewGoalTitle\":string|null," +
    `\"suggestedCategory\":\"${CATEGORIES.join("|")}\",` +
    "\"estimatedPoints\":number,\"confidence\":number,\"rationale\":string}. " +
    "estimatedPoints is 5-50 by difficulty; confidence is 0..1.";
  const user = `Task: "${taskTitle}". Existing goals: ${JSON.stringify(goals)}.`;

  try {
    return await callGroqJson(system, user);
  } catch (e) {
    logger.error("classifyTask failed", e);
    return {
      suggestedGoalId: null,
      suggestedNewGoalTitle: String(taskTitle).slice(0, 40),
      suggestedCategory: "OTHER",
      estimatedPoints: 10,
      confidence: 0,
      rationale: "fallback",
    };
  }
});

/** Estimate point value for a task (spec §6 Core: point scoring). */
export const scoreTask = onCall(async (request: CallableRequest) => {
  const { taskTitle = "" } = request.data ?? {};
  const system =
    "Estimate difficulty points (integer 5-50) for a personal task. " +
    "Reply ONLY with JSON: {\"points\":number}.";
  try {
    const json = await callGroqJson(system, `Task: "${taskTitle}"`);
    const points = Number(json?.points);
    return { points: Number.isFinite(points) ? Math.min(50, Math.max(5, Math.round(points))) : 10 };
  } catch (e) {
    logger.error("scoreTask failed", e);
    return { points: 10 };
  }
});
