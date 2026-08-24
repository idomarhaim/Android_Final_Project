/**
 * `plan`'s validator — `docs/PRODUCT_v0.3.md` §3.3 **B** and §3.7 (`C8` #24).
 *
 * §3.4 puts validation in the Cloud Function **singly**, so this file is the only place these
 * rules are checked at all. It imports nothing from Firebase for the same reason `classify.ts`
 * and `measure.ts` do not: `test/plan.test.mjs` then runs it as plain `node --test` in
 * milliseconds, with no emulator.
 *
 * ## What the model authors here, and the two things it does not
 *
 * §3.7: *"the model labels **each item**, and the shape **falls out**"*. So the response carries
 * a **label** per item and never a plan-level shape, and §0.5's split is kept in two places
 * this file enforces mechanically:
 *
 * 1. **No ids.** §3.3 B: *"The model may not mint an id. A new step carries **no `id` at all**
 *    and is identified by its position; the client assigns the id on receipt — making the
 *    truncation failure **structurally unrepresentable** rather than merely checked."* This
 *    validator therefore never reads an id field, and [ValidatedPlanItem] has none to read.
 * 2. **No dates.** An absolute date from a model is a value the app can check nothing about,
 *    and it is stale the moment the draft is left overnight — which §3.7 says drafts routinely
 *    are (*"the draft persists exactly as left … no expiry"*). What comes back is
 *    [ValidatedPlanItem.dayOffset], **days from the day the plan is applied**, and the client
 *    resolves it against its own clock and zone. Same argument as `measure`'s `targetSource`:
 *    the model says *where in the schedule* the step sits, and the app does the arithmetic.
 *
 * ## `label` gates pricing, and it is a cross-field rule
 *
 * §3.3 B: *"a `STATE_YOU_REACH` item is a container, so `difficulty`/`estimatedMinutes` present
 * on one is a validation failure **of those two fields**, not of the item."* `timeOfDay` joins
 * them for the same reason — a milestone is a state you reach, and a state does not occupy a
 * slot in the day. So this validator **strips** those three from a container rather than
 * dropping the container, and `C18`'s container rule then holds by construction: a milestone
 * that reached the client with minutes on it would be a priced container.
 *
 * ## What a whole-group failure is here
 *
 * A plan whose every item failed is **no plan** — an empty `items` array, which the client
 * reads as *the model had nothing to say* and **reports**. §0.4 decides that: this call is
 * user-invoked, so its failure is one the user can act on, and silence would leave a pressed
 * button looking like a plan with no steps in it. Otherwise a plan is the sum of the items
 * that passed; a spoiled step costs that step and nothing else.
 */

/** §3.3 B's two labels. `STATE_YOU_REACH` is a container; `WORK_YOU_DO` is a leaf. */
export const LABELS = ["STATE_YOU_REACH", "WORK_YOU_DO"] as const;

/** §1.4's three difficulties, as the wire spells them. Mirrors `domain/model/Difficulty`. */
export const DIFFICULTIES = ["LIGHT", "ROUTINE", "DEMANDING"] as const;

/** §3.3 A's minutes range, reused unchanged — a plan step is a task like any other. */
export const MIN_MINUTES = 5;
export const MAX_MINUTES = 480;

/**
 * How far ahead a step may be placed, in days.
 *
 * A year, because a plan for *"run a marathon"* legitimately reaches one and nothing shorter is
 * defensible as a rule rather than as a taste. The bound is aimed at a model answering in a
 * different unit — `dayOffset: 20260901` is the failure it catches — not at a step genuinely
 * eleven months out.
 */
export const MAX_DAY_OFFSET = 365;

/**
 * Longest step title. **Content** the moment it lands among the user's tasks (§3.3 D's rule,
 * which governs every free field the model authors), so it is never truncated: a model that
 * answers with a paragraph has not answered with a step title, and the honest result is that
 * the step does not exist.
 */
export const MAX_TITLE = 120;

/** Longest `changeNotes` entry. Speech, and a runaway one is still not a sentence. */
const MAX_NOTE = 200;

/** How many steps one plan may carry. Past this it is not a plan, it is a list. */
export const MAX_ITEMS = 20;

/** How many `changeNotes` survive. */
const MAX_NOTES = 10;

/** One step of a plan, carrying only the fields that passed. */
export interface ValidatedPlanItem {
  /** The step, in the request's `language`. Content — never re-rendered (`C15b`). */
  title: string;
  /** `STATE_YOU_REACH` | `WORK_YOU_DO`. Always present: an unlabelled item is dropped. */
  label: string;
  /** Leaves only — absent on a container, and stripped rather than failing the item. */
  difficulty?: string;
  /** Leaves only. */
  estimatedMinutes?: number;
  /** Days from the day the plan is applied. The client owns the calendar arithmetic. */
  dayOffset?: number;
  /** `HH:MM`, 24-hour. Leaves only; what turns an all-day step into a slot in the day. */
  timeOfDay?: string;
}

/** A validated `plan` response. */
export interface ValidatedPlan {
  items: ValidatedPlanItem[];
  changeNotes: string[];
}

/** A non-blank string no longer than `max`, trimmed — or `undefined`. Never truncates. */
function asText(value: unknown, max: number): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > max) return undefined;
  return trimmed;
}

/** A finite integer, or `undefined`. Rejects `NaN`, `Infinity` and `"30"`; rounds `30.5`. */
function asInt(value: unknown): number | undefined {
  if (typeof value !== "number" || !Number.isFinite(value)) return undefined;
  return Number.isInteger(value) ? value : Math.round(value);
}

/** A member of `allowed`, upper-cased, or `undefined`. */
function asMember(value: unknown, allowed: readonly string[]): string | undefined {
  if (typeof value !== "string") return undefined;
  const upper = value.trim().toUpperCase();
  return allowed.includes(upper) ? upper : undefined;
}

/**
 * `HH:MM` on a 24-hour clock, or `undefined`.
 *
 * Strict about the **shape** and silent about the taste: `03:00` is a legal answer and the app
 * is not the judge of when somebody trains. What it rejects is a value the client's
 * `LocalTime.parse` would throw on — the one failure here that would cost the whole draft
 * rather than one step.
 */
export function asTimeOfDay(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const m = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(value.trim());
  return m ? `${m[1]}:${m[2]}` : undefined;
}

/**
 * Validates one item. Returns `undefined` for a thing that is not a step at all — no title, or
 * no label — because those two are what makes it one.
 *
 * Everything else stands or falls alone, and a container has its three leaf fields **stripped**
 * (§3.3 B): the item survives, the pricing does not.
 */
function validateItem(raw: unknown): ValidatedPlanItem | undefined {
  if (raw === null || typeof raw !== "object") return undefined;
  const m = raw as Record<string, unknown>;

  const title = asText(m.title, MAX_TITLE);
  if (title === undefined) return undefined;

  const label = asMember(m.label, LABELS);
  if (label === undefined) return undefined;

  const out: ValidatedPlanItem = { title, label };

  // A step may be placed nowhere at all, and that is a legal plan item: it goes on the goal's
  // list with no *when*, which is exactly what `Task.occurrence = null` already means (§2.2).
  // So an absent or unusable offset costs the step its date and never the step.
  const dayOffset = asInt(m.dayOffset);
  if (dayOffset !== undefined && dayOffset >= 0 && dayOffset <= MAX_DAY_OFFSET) {
    out.dayOffset = dayOffset;
  }

  // §3.3 B's cross-field rule. Nothing below runs for a container, which is what makes
  // "a priced milestone" unrepresentable downstream rather than merely unlikely.
  if (label === "STATE_YOU_REACH") return out;

  const difficulty = asMember(m.difficulty, DIFFICULTIES);
  if (difficulty !== undefined) out.difficulty = difficulty;

  const minutes = asInt(m.estimatedMinutes);
  if (minutes !== undefined && minutes >= MIN_MINUTES && minutes <= MAX_MINUTES) {
    out.estimatedMinutes = minutes;
  }

  const time = asTimeOfDay(m.timeOfDay);
  if (time !== undefined) out.timeOfDay = time;

  return out;
}

/**
 * Validates a whole `plan` response.
 *
 * Order is preserved and is load-bearing: §3.3 B identifies a new step **by its position**, so
 * the client assigns ids down this array and a reordering here would silently re-label the
 * user's plan.
 */
export function validatePlan(raw: unknown): ValidatedPlan {
  const empty: ValidatedPlan = { items: [], changeNotes: [] };
  if (raw === null || typeof raw !== "object") return empty;
  const m = raw as Record<string, unknown>;

  const items: ValidatedPlanItem[] = [];
  if (Array.isArray(m.items)) {
    for (const item of m.items) {
      if (items.length >= MAX_ITEMS) break;
      const validated = validateItem(item);
      if (validated !== undefined) items.push(validated);
    }
  }

  const changeNotes: string[] = [];
  if (Array.isArray(m.changeNotes)) {
    for (const note of m.changeNotes) {
      if (changeNotes.length >= MAX_NOTES) break;
      const text = asText(note, MAX_NOTE);
      if (text !== undefined) changeNotes.push(text);
    }
  }

  return { items, changeNotes };
}
