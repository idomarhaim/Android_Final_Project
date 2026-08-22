/**
 * `measure`'s validator — `docs/PRODUCT_v0.3.md` §3.3 E and §3.4 (`C7` #14, `C22` #44,
 * #65).
 *
 * ## The feature `C7` handed on and nobody wrote
 *
 * `C7` specced an agent that proposes a concrete measure for an unmeasured goal and passed
 * it to `C11b` in so many words — *"a **fifth AI feature** for #30 to write an output format
 * for."* `#30` wrote four schemas and reached "five" by counting `classify`, so **two
 * tickets each said "fifth" about a different feature** and this one never got a wire
 * format. §10.1 is the record. This file is it.
 *
 * ## There is no number in the response, and that is the whole design
 *
 * §3.3 E: `targetSource` is a **prompt-declared enum naming which arithmetic the app runs**
 * — `SCHEDULE`, `STEPS` or `USER`. The client computes the target from structure it already
 * holds. Forced rather than chosen: `C11a` measured this model's free numbers swinging **2x
 * run-to-run** and **1.8x between languages**, and the target is the one field the whole
 * feature would be judged on. §0.5's strongest form — *never let a value cross the wire
 * whose failure you would have to detect.*
 *
 * So this validator has **no numeric field to range-check**, which is not an omission: it is
 * the schema working. Compare `classify.ts`, which spends two constants and a clamp comment
 * on `estimatedMinutes` alone.
 *
 * ## The contract differs from `classify`'s, per feature (§3.4)
 *
 * `classify` validates every field independently — a bad duration does not cost you the goal
 * id. **`measure` drops an element whole**: §3.3 E says *"a proposal missing `measureKind`,
 * `word` or `targetSource` is not a proposal"*, because a kind with no word is not a
 * degraded measure, it is not a measure. `basis` is the one field that may fall back, and it
 * falls back to `OUTCOME` for the reason given at [asBasis].
 *
 * **Nothing here reads Firebase**, deliberately, so `test/measure.test.mjs` runs it as plain
 * `node --test` in milliseconds — the same arrangement `classify.ts` and `derived.ts` have.
 */

/** §1.3's seven measure kinds. Shared with `index.ts`'s prompt, so the two cannot drift. */
export const MEASURE_KINDS = [
  "COUNT", "DURATION", "DISTANCE", "VOLUME", "MASS", "MONEY", "PERCENT",
] as const;

/** §3.3 E's `basis`. `LEADING` is §1.3's *measure the behaviour that produces the outcome*. */
export const BASES = ["OUTCOME", "LEADING"] as const;

/** §3.3 E's `targetSource` — which arithmetic the **client** runs. Never a number. */
export const TARGET_SOURCES = ["SCHEDULE", "STEPS", "USER"] as const;

/**
 * Longest `word` that survives validation — §3.3 E's cap.
 *
 * `word` is **content** (§3.5): the moment the user accepts it, it is his, never re-rendered
 * and never translated (`C15b`). So it must not be silently truncated into something he did
 * not choose — a model that answers with a sentence has not answered with a unit, and the
 * honest result is **no proposal**, not a cut-off one. Same rule and same reasoning as
 * `classify.ts`'s `MAX_GOAL_TITLE`.
 */
export const MAX_WORD = 24;

/**
 * Most proposals one response may carry.
 *
 * §3.2's wide call is wide over *goals*, so the natural ceiling is the request's own goal
 * list — enforced by membership below, which already drops anything not asked about. This
 * cap is the second half of that: a response repeating one valid id a thousand times passes
 * membership on every element. Deduplication by `goalId` (below) is what actually bounds it;
 * this is the belt to that pair of braces.
 */
export const MAX_PROPOSALS = 50;

/** One validated proposal. Every field required — §3.3 E drops a partial element whole. */
export interface ValidatedProposal {
  goalId: string;
  measureKind: string;
  word: string;
  basis: string;
  targetSource: string;
}

/**
 * The membership list that travelled **with the request** — §3.3 E's *"MUST be a member of
 * `goals[].id`"*.
 *
 * From the caller rather than from Firestore, exactly as `classify.ts` argues: re-reading
 * here would cost document reads and would answer a *different* question — whether the goal
 * exists **now**, rather than whether it was one of the goals the model was actually offered.
 */
export interface MeasureLists {
  goalIds: string[];
}

/** A non-blank string no longer than [max], trimmed — or `undefined`. */
function asText(value: unknown, max: number): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > max) return undefined;
  return trimmed;
}

/** A member of [allowed], upper-cased, or `undefined`. */
function asEnum(value: unknown, allowed: readonly string[]): string | undefined {
  if (typeof value !== "string") return undefined;
  const upper = value.trim().toUpperCase();
  return allowed.includes(upper) ? upper : undefined;
}

/**
 * One of §3.3 E's two bases, defaulting to `OUTCOME`.
 *
 * **The one field here that substitutes rather than dropping the element**, and the asymmetry
 * is deliberate. `basis` does not change what is proposed — the kind, the word and the
 * arithmetic are all already fixed by the other three fields; it changes only how the offer
 * is *described* on screen. Dropping a whole usable proposal over a caption would be strictly
 * worse for the user than describing it in the more conservative of two ways.
 *
 * `OUTCOME` is that conservative way: it claims the number says something about the goal
 * itself, which is the weaker and more ordinary reading. Claiming `LEADING` by default would
 * assert the app had spotted a proxy relationship it was never told about.
 */
export function asBasis(value: unknown): string {
  return asEnum(value, BASES) ?? "OUTCOME";
}

/**
 * Validates one `measure` response against the goal ids the request carried.
 *
 * Returns **only the elements that passed whole**. An element is dropped when its `goalId` is
 * not a member (`C11a`'s one measured failure class across 248 live calls), when its
 * `measureKind` or `targetSource` is out of enum, or when its `word` is blank or over
 * [MAX_WORD].
 *
 * **Duplicates are dropped, keeping the first.** Two proposals for one goal is not a richer
 * answer, it is an unanswerable screen: §1.3 offers *a* measure, and the surface has one
 * accept button. First rather than last because the model's own ordering is the only
 * preference signal available, and inventing a tie-break would be the app judging.
 */
export function validateProposals(raw: unknown, lists: MeasureLists): ValidatedProposal[] {
  const source = (raw as Record<string, unknown> | null)?.proposals;
  if (!Array.isArray(source)) return [];

  const out: ValidatedProposal[] = [];
  const seen = new Set<string>();

  for (const item of source) {
    if (out.length >= MAX_PROPOSALS) break;
    if (item === null || typeof item !== "object") continue;
    const m = item as Record<string, unknown>;

    const goalId = typeof m.goalId === "string" && lists.goalIds.includes(m.goalId)
      ? m.goalId
      : undefined;
    if (goalId === undefined || seen.has(goalId)) continue;

    const measureKind = asEnum(m.measureKind, MEASURE_KINDS);
    const targetSource = asEnum(m.targetSource, TARGET_SOURCES);
    const word = asText(m.word, MAX_WORD);
    // §3.3 E: missing any of these three and it is not a proposal, so the whole element
    // goes. `basis` is absent from this test on purpose -- `asBasis` substitutes.
    if (measureKind === undefined || targetSource === undefined || word === undefined) continue;

    seen.add(goalId);
    out.push({ goalId, measureKind, word, basis: asBasis(m.basis), targetSource });
  }

  return out;
}

/**
 * Pulls the goal-id membership list out of a `measure` request payload.
 *
 * Tolerant in and strict out, like `classify.ts`'s twin: a malformed `goals` array yields an
 * **empty** list, so every proposal fails membership and the client falls back to §3.4's
 * mechanical proposal. That is the safe direction — the failure the app survives is *"it
 * offered nothing"*, never *"it offered a measure for a goal that does not exist"*.
 */
export function listsFromMeasureRequest(data: unknown): MeasureLists {
  const d = (data ?? {}) as Record<string, unknown>;
  const value = d.goals;
  if (!Array.isArray(value)) return { goalIds: [] };
  const goalIds: string[] = [];
  for (const item of value) {
    if (item === null || typeof item !== "object") continue;
    const id = (item as Record<string, unknown>).id;
    if (typeof id === "string" && id.length > 0) goalIds.push(id);
  }
  return { goalIds };
}
