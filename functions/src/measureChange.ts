/**
 * §6's **measure-change approval quorum** — the arithmetic half, with no Firebase imports.
 *
 * `docs/PRODUCT_v0.3.md` §6, verbatim:
 *
 * > the owner writes `pendingMeasure` on the challenge document, **each participant writes
 * > `approvedChangeId` in the one document they are permitted to write**, and the Function
 * > applies it when every row agrees.
 *
 * ### Why the feature exists at all
 *
 * A challenge's measure is **the unit every participant's score is expressed in**. Changing
 * it mid-flight silently re-denominates other people's numbers: a leaderboard that said
 * *8200 steps* yesterday and *8200 km* today has not been corrected, it has been falsified,
 * and nobody who is not looking for it will notice.
 *
 * ### The two consequences, and why the owner does NOT choose between them
 *
 * `C7` §5 offered the owner *reset* or *adapt*. Working out what **adapt** could mean is
 * what settles the design, and it turns out not to be a choice:
 *
 * - **A change of `kind`** (`COUNT` → `DISTANCE`) invalidates every participant's link,
 *   because `Challenge.canBeScoredFrom` matches on kind. Adapting it would need either a
 *   **unit conversion** — which `Measure`'s KDoc records that this app deliberately does
 *   **not** perform — or a **re-link**, which necessarily restarts the number in the new
 *   unit. So a kind change *is* a reset; there is no second option to offer.
 * - **A change of `word` alone** (`km` → `kilometres`) changes no arithmetic at all: the
 *   kind is what the scoring matches on, so every link and every number survives. That *is*
 *   "adapt", and it is free.
 *
 * So [consequenceOf] **derives** the consequence from the change rather than reading a mode
 * the owner picked. The owner is told which it will be before they propose.
 *
 * **But both still need unanimous approval**, and that is the non-obvious half. A word-only
 * change looks harmless because the arithmetic is untouched — and it is exactly the one
 * that can lie hardest: relabelling `km` as `miles` leaves every stored number alone while
 * changing what all of them claim. The gate is on the **claim**, not on the arithmetic.
 *
 * ### The quorum is everyone who is STILL HERE
 *
 * A participant who leaves has no row to write `approvedChangeId` in, so counting them
 * would let one person walking away freeze the challenge forever. [quorum] therefore reads
 * whatever rows exist at the moment it runs and asks nothing about who used to be there.
 *
 * ### While a change is pending, the challenge keeps scoring in the OLD unit
 *
 * Nothing here is read by the scoring path — the pending fields live beside the live
 * measure, never on top of it. A half-approved change must not stop the race.
 */

/** A proposed measure, as it sits on the challenge document while it waits. */
export interface PendingMeasure {
  /**
   * Identifies **this** proposal, so an approval cannot be replayed against the next one.
   *
   * Without it, a participant who approved change A would still count as approving change
   * B — the owner could withdraw, propose something else, and inherit the old consent.
   */
  changeId: string;
  kind: string;
  word: string;
}

/** The live measure a challenge is scoring in right now. */
export interface LiveMeasure {
  kind: string | null;
  word: string | null;
}

/** One participant row, reduced to the only field the quorum reads. */
export interface ApprovalRow {
  uid: string;
  approvedChangeId?: string | null;
}

/** What applying a change will do to everybody's numbers. */
export type Consequence = "RELABEL" | "RESET";

/** Reads the pending proposal off a challenge document, or `null` when there is none. */
export function pendingFrom(
  data: Record<string, unknown> | undefined,
): PendingMeasure | null {
  if (!data) return null;
  const changeId = asText(data.pendingChangeId);
  const kind = asText(data.pendingMeasureKind);
  const word = asText(data.pendingMeasureWord);
  // All three or nothing. A half-written proposal is not a proposal, and applying one
  // would write a measure with a missing half onto a live challenge.
  if (!changeId || !kind || !word) return null;
  return { changeId, kind, word };
}

/**
 * What applying [pending] does to the standings — **derived, never chosen**.
 *
 * See this file's header for why *reset* and *adapt* are not two options the owner picks
 * between: a kind change cannot be adapted without a unit conversion this app does not do,
 * and a word-only change needs no adapting because no arithmetic depends on the word.
 */
export function consequenceOf(
  live: LiveMeasure,
  pending: PendingMeasure,
): Consequence {
  // A challenge that never had a kind (a pre-§6 document whose `metricUnit` was the
  // "points" default) is GAINING one rather than changing it. There is nothing scored in
  // the old unit to invalidate -- `Challenge.isUnmeasured` blocks linking entirely -- so
  // this is a relabel, and treating it as a reset would zero numbers that came from
  // nowhere the new kind disagrees with.
  if (!live.kind) return "RELABEL";
  return live.kind === pending.kind ? "RELABEL" : "RESET";
}

/** The state of a proposal: who is in, how many have agreed, and whether it applies. */
export interface Quorum {
  total: number;
  approved: number;
  reached: boolean;
}

/**
 * Counts agreement for [pending] across the rows that exist **now**.
 *
 * `total === 0` never reaches quorum. A challenge with no participants at all is not a
 * challenge whose every member agrees — it is one the trigger has caught mid-delete, and
 * applying a measure change to it would be writing to a document nobody is reading.
 */
export function quorum(pending: PendingMeasure, rows: ApprovalRow[]): Quorum {
  const total = rows.length;
  const approved = rows.filter((r) => r.approvedChangeId === pending.changeId).length;
  return { total, approved, reached: total > 0 && approved === total };
}

/** The fields to write on the challenge document once a change applies. */
export function challengeUpdate(pending: PendingMeasure): Record<string, unknown> {
  return {
    measureKind: pending.kind,
    measureWord: pending.word,
    // Cleared in the SAME write that applies it. Leaving them behind would leave a
    // challenge advertising a proposal it has already carried out, which every client
    // would render as a second change waiting for approval.
    pendingChangeId: null,
    pendingMeasureKind: null,
    pendingMeasureWord: null,
    pendingProposedAt: null,
  };
}

/**
 * The fields to write on **every** participant row once a change applies.
 *
 * `approvedChangeId` is cleared either way: it named a proposal that no longer exists, and
 * a stale value is what would make the next proposal inherit consent nobody gave.
 *
 * On a `RESET` the three server-owned score fields go to zero **here**, rather than being
 * left for the projection to recompute, because the projection sums a fact that is about
 * to be deleted — and between the two writes the standings would show the old unit's
 * numbers under the new unit's name, which is precisely the falsification this whole
 * feature exists to prevent.
 */
export function participantUpdate(
  consequence: Consequence,
): Record<string, unknown> {
  const base: Record<string, unknown> = { approvedChangeId: null };
  if (consequence === "RELABEL") return base;
  return {
    ...base,
    score: 0,
    scoreSource: "NONE",
    reportedAt: 0,
  };
}

function asText(v: unknown): string {
  return typeof v === "string" ? v.trim() : "";
}
