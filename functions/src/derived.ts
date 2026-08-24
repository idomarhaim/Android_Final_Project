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

/**
 * A participant's fact for one challenge, at `users/{uid}/challengeReports/{challengeId}`.
 *
 * **Two shapes, never both** - see `ChallengeRepositoryImpl.linkGoal`, which writes it with
 * an unmerged `set()` precisely so that the exclusion is structural:
 *
 *  - `{ goalId, linkedAt }` - the section-6 scoring path. The score is *movement in that
 *    goal since you joined*, summed here from the goal's own progress entries.
 *  - `{ value, reportedAt }` - a number the participant typed.
 *
 * The exclusion is what makes the standings badge answerable. If both could sit on one fact
 * this function would have to pick a winner, and the row would then be labelled with the
 * outcome of a tiebreak rather than with what the participant actually did.
 */
export interface ReportFact {
  value?: number | null;
  goalId?: string | null;
  /** When the typed number was written. Meaningless on the linked shape, and unread there. */
  reportedAt?: number | null;
}

/**
 * One `users/{uid}/goals/{goalId}/progress/{entryId}` document, as this projection reads it.
 *
 * `createdAt` is what puts an entry inside or outside the scoring window; `value` is what it
 * contributes. Nothing else on the entry matters here - a note, an image and a `sourceKey`
 * are the client's business, and `sourceKey` has already done its job upstream by stopping a
 * Health Connect re-sync writing the same day twice (#49, spec §6).
 */
export interface ProgressFact {
  value?: number | null;
  createdAt?: number | null;
}

/**
 * The window a participant's movement is counted over. Mirrors `ScoringWindow` in
 * `domain/model/Challenge.kt` - one rule, two languages.
 *
 * `from` is `max(joinedAt, startAt)` and `until` is `endAt` or null for open-ended. Both
 * bounds are documented on the Kotlin side, including which half is §6's own words
 * and which is a derivation from the phase model.
 */
export interface ScoringWindow {
  from: number;
  until: number | null;
}

/** Whether the participant's fact points at a goal - the automatic path, not a typed number. */
export function linkedGoalId(report: ReportFact | null | undefined): string | null {
  const id = report?.goalId;
  return typeof id === "string" && id.trim().length > 0 ? id.trim() : null;
}

/**
 * The window `challenge` scores a participant who joined at `joinedAt` over.
 *
 * Mirrors `Challenge.scoringWindowFor`. A `startAt` of `0` means *no start date*, so it must
 * not become a lower bound by accident - `Math.max` over a non-finite or absent value is
 * exactly the arithmetic that silently produces `NaN`, which would then include nothing at
 * all and read as *this challenge scores zero*.
 */
export function scoringWindow(
  challenge: { startAt?: number | null; endAt?: number | null } | null | undefined,
  joinedAt: number | null | undefined,
): ScoringWindow {
  const joined = Number(joinedAt);
  const startAt = Number(challenge?.startAt);
  const endAt = Number(challenge?.endAt);
  const from = Math.max(
    Number.isFinite(joined) ? joined : 0,
    Number.isFinite(startAt) ? startAt : 0,
  );
  return { from, until: Number.isFinite(endAt) && endAt > 0 ? endAt : null };
}

/**
 * §6's score: **movement in the linked goal since you joined**, not the goal's
 * current value.
 *
 * > Summed from timestamped entries rather than stored as a delta, so **relink, unlink,
 * > backfill and dedup all fall out**, and joining with a year-old goal **imports no
 * > history**.
 *
 * That is the whole reason this takes the entry list rather than a `currentValue`: a stored
 * delta would need a baseline captured at join time, and every one of those four cases is a
 * baseline going stale. Here there is no baseline to go stale - re-linking a different goal
 * re-sums a different list, unlinking sums nothing, a backfilled entry lands inside or
 * outside the window on its own timestamp, and a de-duplicated `sourceKey` never produced a
 * second entry to sum in the first place.
 *
 * **Structurally idempotent**, like every other projection here: the whole entry set is the
 * input and no prior score is, so a redelivered event and a manual re-run write the same
 * number (§5.2's ground for rejecting `FieldValue.increment`).
 *
 * **Floored at zero, and that floor is load-bearing rather than defensive.** A weight-loss
 * goal logs *downwards*, so a real participant's movement is genuinely negative, and a
 * negative score would sort them **below** somebody who has done nothing while the standings
 * offer no way to say why. §6 does not adjudicate direction, so the honest reading is
 * that a challenge ranks on *how far you moved it the way the challenge counts*; the
 * losing-weight race is the case that needs a direction, and it is named as owed work in
 * `CHANGELOG/2026-08-24/challenge-scoring.md` rather than guessed at here.
 */
export function scoreFromProgress(
  entries: readonly ProgressFact[],
  window: ScoringWindow,
): number {
  let total = 0;
  for (const entry of entries) {
    const at = Number(entry?.createdAt);
    if (!Number.isFinite(at)) continue;
    if (at < window.from) continue;
    if (window.until !== null && at >= window.until) continue;
    const value = Number(entry?.value);
    if (!Number.isFinite(value)) continue;
    total += value;
  }
  return Math.max(0, total);
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

/** What a participant row says about where its number came from - mirrors `ScoreSource`. */
export type ScoreSource = "NONE" | "DERIVED" | "REPORTED";

/** The score plus its provenance, as one participant row's projection. */
export interface Standing {
  score: number;
  scoreSource: ScoreSource;
  /** When the typed number was reported; `0` for anything not typed. */
  reportedAt: number;
}

/**
 * The whole of one participant row's projection, as a pure function of their fact.
 *
 * `null` means **write nothing**, and it is a real answer rather than an error path - the
 * same semantics `scoreFromReport` already had, kept because the reason is unchanged:
 * deleting a fact (undoing a report, or leaving) must not silently zero a standing other
 * people are reading. The last number stands until the participant row itself is deleted.
 *
 * The `scoreSource` is written by the server and pinned by `firestore.rules`, and that is
 * not belt-and-braces. A participant who could write their own label could type a number
 * and mark it `DERIVED` - the label would then assert exactly the thing it exists to deny.
 * What it can still never be is *evidence about the walk*: §6's honest residual is
 * that this stops a win being **typed**, not a reading being **forged**.
 */
export function standingFromFact(
  report: ReportFact | null | undefined,
  progress: readonly ProgressFact[],
  window: ScoringWindow,
  reportedAt: number | null | undefined,
): Standing | null {
  if (report === null || report === undefined) return null;
  if (linkedGoalId(report) !== null) {
    return {
      score: scoreFromProgress(progress, window),
      scoreSource: "DERIVED",
      // Zero, always: there is no typed number behind this, so the badge has no date to
      // show and must not borrow the link's own timestamp to invent one.
      reportedAt: 0,
    };
  }
  const score = scoreFromReport(report);
  if (score === null) return null;
  const at = Number(reportedAt);
  return {
    score,
    scoreSource: "REPORTED",
    reportedAt: Number.isFinite(at) && at > 0 ? at : 0,
  };
}
