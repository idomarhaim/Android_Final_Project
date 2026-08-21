/**
 * The derived-state arithmetic, as pure functions over facts.
 *
 * `docs/PRODUCT_v0.3.md` §5.2 (`C20`, #42):
 *
 * > A derived number gets a stored writer if and only if somebody who cannot read its
 * > inputs has to read it.
 *
 * Two numbers meet that test — `publicProfiles/{uid}.points`, which a leaderboard reader
 * cannot compute because `users/{uid}/**` is `isOwner(uid)`, and
 * `challenges/{id}/participants/{uid}.score`, which crosses the same boundary. Everything
 * else in the map is computed at the point of use and stored nowhere.
 *
 * **Project from facts; never recompute-and-store.** These functions take the *whole* fact
 * set and return the number. Run one twice and it writes the same value, because there is
 * no prior value in the input — idempotence is **structural**, not something the caller has
 * to be careful about. That is also why `FieldValue.increment` stays rejected by §5.2:
 * `increment` *is* the accumulator, and an accumulator is exactly the state this shape
 * removes.
 *
 * **Nothing here reads Firebase.** The trigger wiring lives in `index.ts`; this file is
 * plain arithmetic so `test/projection.test.mjs` can run it with no emulator, against
 * `shared-fixtures/derived-state.json` — the same file the JVM suite reads. §5.2's honest
 * residual is that this arithmetic now exists in Kotlin *and* TypeScript and the two can
 * disagree; that fixture is what stops them.
 */

/** §1.4's difficulty multipliers, mirroring `domain/model/Difficulty` — one table, two languages. */
const MULTIPLIERS: Record<string, number> = {
  LIGHT: 0.75,
  ROUTINE: 1.0,
  DEMANDING: 1.5,
};

/**
 * A banked completion, as stored at `users/{uid}/completionFacts/{taskId}` (#55, §1.4).
 *
 * It carries the **inputs**, never the number. `pointsOf` below is the whole of the
 * arithmetic, and it mirrors `TaskScoring.pointsFor` on the client byte for byte in
 * behaviour; `shared-fixtures/derived-state.json` is what holds the two together.
 */
export interface CompletionFact {
  minutes?: number | null;
  difficulty?: string | null;
}

/**
 * A task, as the projection needs to see it **when it has no fact document**.
 *
 * This is the pre-#55 shape and it is a **migration** reader, not the live one: a task
 * completed before the fact collection existed carries `done` + `points` on its own document
 * and has never been re-ticked. Counting it here is what stops every user's lifetime total
 * collapsing the moment this function deploys — the alternative is a backfill write over
 * every task in every account, for a number that is already correct.
 */
export interface LegacyTaskFact {
  done?: boolean;
  points?: number | null;
}

/** `round(minutes / 3) × difficulty`, rounded to an integer currency (§1.4). */
export function pointsOf(fact: CompletionFact): number {
  const minutes = Number(fact?.minutes);
  if (!Number.isFinite(minutes)) return 0;
  const multiplier = MULTIPLIERS[String(fact?.difficulty ?? "ROUTINE").toUpperCase()] ?? 1.0;
  return Math.round(Math.round(minutes / 3) * multiplier);
}

/** A participant's self-reported measurement for one challenge. */
export interface ReportFact {
  value?: number | null;
}

/**
 * Total points = the sum over **banked completion facts**, plus the pre-#55 tasks that have
 * no fact yet, floored at zero.
 *
 * ### Why the second argument exists, and when it will stop mattering
 *
 * §1.4 moved the completion into its own document and banked its **inputs** there, so the
 * live sum is over [facts] alone. A task completed *before* that change carries `done` and
 * `points` on the task document and no fact — and it stays that way until something writes
 * it, because #55 deliberately performs no backfill. So the total is the union of the two,
 * keyed by task id: [legacyTasks] is a map so that a task which HAS been re-ticked since the
 * migration is counted once, from its fact, and never twice.
 *
 * As accounts migrate through ordinary use the legacy half empties by itself, and this
 * argument becomes a list of zeroes. It is not a permanent second source of truth; it is the
 * old one, draining.
 *
 * The floor is not defensive tidiness: durations are editable, so a fact set whose sum is
 * somehow negative is reachable, and a negative total would run `levelForPoints` below 1.
 */
export function pointsFromFacts(
  facts: Readonly<Record<string, CompletionFact>>,
  legacyTasks: Readonly<Record<string, LegacyTaskFact>> = {},
): number {
  let total = 0;
  for (const fact of Object.values(facts)) total += pointsOf(fact);
  for (const [taskId, task] of Object.entries(legacyTasks)) {
    // A fact wins over the legacy fields on the same task -- the fact is what was actually
    // banked, and the legacy fields are what the migrating write is about to clear.
    if (taskId in facts) continue;
    if (task?.done === true) total += Number(task.points) || 0;
  }
  return Math.max(0, total);
}

/**
 * The level curve, mirroring `domain/model/Leveling` byte for byte in behaviour:
 * `pointsForLevel(n) = 50 * (n-1) * n` → L1:0, L2:100, L3:300, L4:600, …
 *
 * **No caller stores this.** `publicProfiles.level` was deleted by `C20` — it was a stored
 * function of `points` in the *same document*, so every reader of it could already compute
 * it. It is here because the fixture asserts it on both sides, which is how the two curves
 * are held together.
 */
export function pointsForLevel(level: number): number {
  const n = Math.max(1, Math.trunc(level));
  return 50 * (n - 1) * n;
}

export function levelForPoints(points: number): number {
  let level = 1;
  while (pointsForLevel(level + 1) <= points) level++;
  return level;
}

/**
 * The score a report projects onto the participant row, or `null` for *write nothing*.
 *
 * `null` is a real answer and not an error path. Deleting the report fact — leaving the
 * challenge, or undoing a report — must not silently zero a standing that other people are
 * reading; the last reported number stands until the participant row itself is deleted.
 * The UI reports an **absolute total** (`ChallengesViewModel.submitScore` pre-fills the
 * field with what you already reported), so the latest fact *is* the score, and projecting
 * it is idempotent for the same structural reason as points.
 */
export function scoreFromReport(report: ReportFact | null | undefined): number | null {
  if (report === null || report === undefined) return null;
  const value = Number(report.value);
  if (!Number.isFinite(value)) return null;
  return Math.max(0, value);
}
