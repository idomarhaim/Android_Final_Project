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

/** One task, as the projection needs to see it. Extra Firestore fields are ignored. */
export interface TaskFact {
  done?: boolean;
  points?: number | null;
}

/** A participant's self-reported measurement for one challenge. */
export interface ReportFact {
  value?: number | null;
}

/**
 * Total points = the sum of `points` over the **done** tasks, floored at zero.
 *
 * Mirrors what the Kotlin client shows the owner, who reads the same facts directly out of
 * the offline cache and never waits for this function. The floor is not defensive tidiness:
 * task point values are editable, so a fact set whose sum is negative is reachable, and a
 * negative total would run `levelForPoints` below level 1.
 */
export function pointsFromTasks(tasks: readonly TaskFact[]): number {
  const total = tasks.reduce(
    (sum, t) => (t?.done === true ? sum + (Number(t.points) || 0) : sum),
    0,
  );
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
