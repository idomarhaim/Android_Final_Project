/**
 * `classify`'s validator — `docs/PRODUCT_v0.3.md` §3.3 D and §3.4 (`#6`, `R3`).
 *
 * §3.4 puts this here and **only** here:
 *
 * > **Validation lives in the Cloud Function, singly.** … `classify` is the highest-volume
 * > call … and the client keeps exactly one job: spec §8's fallback, which has to work
 * > offline anyway.
 *
 * A client-side validator would be a second implementation of these rules, and two
 * implementations of a membership test are two answers to *"is this id real?"* that drift
 * apart the first time one of them is edited. The client still **resolves** ids — it looks a
 * goal up to render its title — and resolution is not validation: it cannot disagree with
 * anything, because it either finds the object or does not.
 *
 * **One rule, applied field by field: a field that fails validation is *absent*.** No `null`
 * sentinel meaning *"I tried"*, no default, no substitute (§3.3). That is what makes the
 * failure contract legible downstream — the client branches on *presence*, so a substituted
 * value is indistinguishable from an answer, while an absent one is exactly the signal §3.4
 * describes. It is also why nothing here clamps: clamping `estimatedMinutes: 900` to `480`
 * would report a number nobody said.
 *
 * **The estimate group is validated independently** (§3.3 D), meaning a bad `estimatedMinutes`
 * does not cost you the goal id and a non-member goal id does not cost you the minutes. Every
 * field in here stands or falls alone; there is no whole-response failure in `classify`.
 * (`plan`'s `items` group has one, and `measure` drops an element whole — those are different
 * features with different contracts, both in §3.3.)
 *
 * **Nothing here reads Firebase**, deliberately, so `test/classify.test.mjs` runs it as plain
 * `node --test` in milliseconds — the same arrangement `derived.ts` has with
 * `test/projection.test.mjs`, and for the same reason: the rules that decide what the app is
 * allowed to believe should be the cheapest thing in the repo to test.
 */

/** The ten `GoalCategory` names the client can deserialize. Shared with `index.ts`'s prompt. */
export const CATEGORIES = [
  "HEALTH", "FITNESS", "SLEEP", "NUTRITION", "RELATIONSHIPS",
  "CAREER", "PROJECTS", "LEARNING", "FINANCE", "OTHER",
] as const;

/**
 * The membership lists that travelled **with the request** (§3.3: *"the membership lists every
 * echoed id must be drawn from"*).
 *
 * They come from the caller rather than from Firestore on purpose. The client already holds
 * the user's goals and areas — it just rendered them — so re-reading them here would add two
 * document reads per quick-add to the highest-volume call in the app, and would still be
 * answering a *different* question: whether the id exists **now**, rather than whether it was
 * one of the options the model was actually offered.
 */
export interface ClassifyLists {
  goalIds: string[];
  lifeAreaIds: string[];
}

/**
 * The validated response. Every field is optional because every field is independently
 * omittable — that is the contract, not a convenience.
 */
export interface ValidatedClassification {
  suggestedGoalId?: string;
  suggestedNewGoalTitle?: string;
  suggestedLifeAreaId?: string;
  suggestedCategory?: string;
  confidence?: number;
  rationale?: string;
  estimatedPoints?: number;
  estimatedMinutes?: number;
}

/**
 * Longest goal title that survives validation.
 *
 * `suggestedNewGoalTitle` is **content** (§3.3 D) — the moment it lands among the user's goals
 * it is his, never re-rendered and never translated — so it must not be silently truncated
 * into something he did not write. A model that answers with a paragraph has not answered
 * with a title, and the honest result is **no title**: the client then names the new goal
 * after the task the user actually typed, which is his own words rather than a cut-off
 * version of the model's.
 */
const MAX_GOAL_TITLE = 120;

/** Longest `rationale`. Speech, not content — but a runaway one is still not a sentence. */
const MAX_RATIONALE = 400;

/** §3.3 A's range, reused here because `classify` carries the same estimate group. */
const MIN_MINUTES = 5;
const MAX_MINUTES = 480;

/**
 * `estimatedPoints`' surviving range.
 *
 * ⚠️ **This field is on borrowed time and is deliberately left alone here.** §3.3 A deletes
 * `points` from the model's vocabulary outright — *"There is no `points` field, and there
 * never will be"* — because §1.4 computes it as `round(minutes / 3) × difficulty`. That
 * inversion is `C1` [#19](https://github.com/idomarhaim/Android_Final_Project/issues/19)'s
 * ticket, not this one, and removing the field here would leave the client with no points at
 * all in the interim. So `#6` validates it and `#19` deletes it.
 */
const MIN_POINTS = 5;
const MAX_POINTS = 50;

/** A finite integer, or `undefined`. Rejects `NaN`, `Infinity`, `"30"` and `30.5` alike. */
function asInt(value: unknown): number | undefined {
  if (typeof value !== "number" || !Number.isFinite(value)) return undefined;
  return Number.isInteger(value) ? value : Math.round(value);
}

/** A non-blank string no longer than [max], trimmed — or `undefined`. */
function asText(value: unknown, max: number): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > max) return undefined;
  return trimmed;
}

/**
 * An id the caller actually sent, or `undefined`.
 *
 * This is the one check `C11a` measured a failure of: across 248 live calls the **only** two
 * failures were semantic, and *"silent id corruption"* was the observed mode. A model naming a
 * goal that does not exist is not a rare pathology to guard against defensively — it is the
 * thing that happens.
 */
function asMember(value: unknown, allowed: string[]): string | undefined {
  if (typeof value !== "string") return undefined;
  return allowed.includes(value) ? value : undefined;
}

/**
 * Validates one `classify` response against the lists the request carried.
 *
 * Returns a **new object holding only the fields that passed** — so a caller can ask
 * `"suggestedGoalId" in result` and get the branch §3.4 specifies. `raw` is untouched.
 *
 * The absent-`suggestedGoalId` case is the one the whole ticket turns on: it takes the
 * new-goal branch, and that branch is the single row in §3.4's table that **speaks**. It does
 * not speak because something went wrong — an unfiled task is not a degraded filing, it is a
 * different outcome — which is why it *tells* and never *asks* (§0.4).
 */
export function validateClassification(
  raw: unknown,
  lists: ClassifyLists,
): ValidatedClassification {
  const out: ValidatedClassification = {};
  if (raw === null || typeof raw !== "object") return out;
  const m = raw as Record<string, unknown>;

  const goalId = asMember(m.suggestedGoalId, lists.goalIds);
  if (goalId !== undefined) out.suggestedGoalId = goalId;

  const areaId = asMember(m.suggestedLifeAreaId, lists.lifeAreaIds);
  if (areaId !== undefined) out.suggestedLifeAreaId = areaId;

  const newTitle = asText(m.suggestedNewGoalTitle, MAX_GOAL_TITLE);
  if (newTitle !== undefined) out.suggestedNewGoalTitle = newTitle;

  const category = m.suggestedCategory;
  if (typeof category === "string" && (CATEGORIES as readonly string[]).includes(category)) {
    out.suggestedCategory = category;
  }

  // Confidence is the one field where the boundary values are the interesting ones: 0 is a
  // legal answer meaning "I have no idea", and it is NOT the same as saying nothing.
  const confidence = m.confidence;
  if (typeof confidence === "number" && Number.isFinite(confidence) &&
      confidence >= 0 && confidence <= 1) {
    out.confidence = confidence;
  }

  const rationale = asText(m.rationale, MAX_RATIONALE);
  if (rationale !== undefined) out.rationale = rationale;

  const points = asInt(m.estimatedPoints);
  if (points !== undefined && points >= MIN_POINTS && points <= MAX_POINTS) {
    out.estimatedPoints = points;
  }

  const minutes = asInt(m.estimatedMinutes);
  if (minutes !== undefined && minutes >= MIN_MINUTES && minutes <= MAX_MINUTES) {
    out.estimatedMinutes = minutes;
  }

  return out;
}

/**
 * Pulls the membership lists out of a `classify` request payload.
 *
 * Tolerant on the way in and strict on the way out: a malformed `goals` array yields an
 * **empty** list, which makes every echoed goal id fail membership and sends the whole call
 * down the new-goal branch. That is the safe direction — the failure the app can survive is
 * *"it did not file this one"*, never *"it filed it under a goal that does not exist"*.
 */
export function listsFromRequest(data: unknown): ClassifyLists {
  const d = (data ?? {}) as Record<string, unknown>;
  return {
    goalIds: idsOf(d.goals, "id"),
    lifeAreaIds: idsOf(d.lifeAreas, "id"),
  };
}

function idsOf(value: unknown, key: string): string[] {
  if (!Array.isArray(value)) return [];
  const ids: string[] = [];
  for (const item of value) {
    if (item === null || typeof item !== "object") continue;
    const id = (item as Record<string, unknown>)[key];
    if (typeof id === "string" && id.length > 0) ids.push(id);
  }
  return ids;
}
