package com.idomarhaim.goalpilot.domain.model

/**
 * How far a goal has come, computed from the facts rather than read from a stored
 * counter (spec §4.6, §5.2, [#49](https://github.com/idomarhaim/Android_Final_Project/issues/49)).
 *
 * ### Why this exists at all
 *
 * `goal.currentValue` used to be a stored aggregate advanced by two client writers.
 * `ProgressRepositoryImpl.logProgress` wrote the entry and *then* called
 * `GoalRepository.addProgress`, in two non-atomic steps with nothing reconciling
 * them: anything ending the process in between left the entry recorded and the
 * counter not, permanently, invisibly, and compounding — every later log added to
 * the wrong base. The second writer, `TaskRepositoryImpl.setDone`, moved the same
 * field from a different path.
 *
 * The repair is not a transaction. §5.2's rule — *a derived number gets a stored
 * writer if and only if somebody who cannot read its inputs has to read it* —
 * settles it against `firestore.rules` rather than taste: `users/{uid}/goals` is
 * read under `isOwner(uid)`, so **the reader is the writer** and there is nobody
 * who cannot reach the inputs. With no stored aggregate there is no second number
 * left to disagree with the facts, so the corruption is not repaired but made
 * unrepresentable (§0.2, §0.3).
 *
 * ### What counts as a fact
 *
 * Two, and both are already stored for their own reasons:
 *
 * - every [ProgressEntry] logged against the goal contributes its `value`;
 * - every completed [Task] linked to the goal contributes its
 *   `progressContribution`.
 *
 * The task half is not an embellishment. `TaskRepositoryImpl.setDone`'s write was
 * the *only* thing crediting a ticked task to its goal, so deleting that writer
 * without summing completions here would quietly stop tasks moving goals at all —
 * the deletion is behaviour-preserving only with both halves present.
 *
 * A completed task is summed rather than made to emit a progress entry on tick.
 * Emitting one would re-create precisely the defect being deleted: a second write
 * that has to agree with the first, plus an untick that now has to find and undo
 * it. Summing over `isDone` is idempotent *structurally* — running it twice gives
 * the same number — which is the property §5.2 chose `C1`'s shape for.
 *
 * ### No clamp, and no backfill
 *
 * Nothing here is clamped. §1.5 makes overshoot legal and shown — past the target
 * the app stops speaking in percent and says *"beat it by 1.5 kg"* — and a clamp
 * at the arithmetic would put that back out of reach for every caller at once.
 * The four clamps §1.5 names are deleted with this change.
 *
 * No migration is needed either: a goal with no entries and no completed tasks
 * sums to `0.0`, which is exactly what its stored `currentValue` defaulted to, so
 * existing documents read identically on day one and their stored field simply
 * stops being consulted.
 */
object DerivedProgress {

    /**
     * The derived `currentValue` for every goal named by [entries] or [tasks].
     *
     * Goals with no facts are **absent** rather than present at `0.0`; callers
     * resolve a missing key to zero themselves, which keeps this from having to be
     * told the goal list just to enumerate it.
     */
    fun currentValues(
        entries: List<ProgressEntry>,
        tasks: List<Task>,
    ): Map<String, Double> {
        val sums = HashMap<String, Double>()
        for (entry in entries) {
            val goalId = entry.goalId
            if (goalId.isBlank()) continue
            sums[goalId] = (sums[goalId] ?: 0.0) + entry.value
        }
        for (task in tasks) {
            if (!task.isDone) continue
            val goalId = task.goalId?.takeIf { it.isNotBlank() } ?: continue
            sums[goalId] = (sums[goalId] ?: 0.0) + task.progressContribution
        }
        return sums
    }

    /**
     * The derived `currentValue` for one goal. `0.0` when nothing has been logged
     * and nothing linked to it is done — the same number an untouched goal has
     * always read.
     *
     * Delegates to [currentValues] rather than running its own loop. A second copy
     * of the summing rule is a second thing to keep in step, which is the class of
     * defect this whole change exists to remove; building a one-key map is not a
     * cost worth paying that with.
     */
    fun currentValueOf(
        goalId: String,
        entries: List<ProgressEntry>,
        tasks: List<Task>,
    ): Double {
        if (goalId.isBlank()) return 0.0
        return currentValues(entries, tasks)[goalId] ?: 0.0
    }
}

/**
 * Replaces each goal's `currentValue` with the sum over [entries] and [tasks].
 *
 * This is the single seam where a `Goal` acquires its progress number, so a
 * caller that has a `Goal` in hand can keep reading `goal.currentValue` exactly as
 * before — the field survives as a *view* of the facts, and only its source
 * changed. That is deliberate: making every screen call a use case instead would
 * have spread one arithmetic decision across a dozen files for no gain.
 */
fun List<Goal>.withDerivedProgress(
    entries: List<ProgressEntry>,
    tasks: List<Task>,
): List<Goal> {
    if (isEmpty()) return this
    val sums = DerivedProgress.currentValues(entries, tasks)
    return map { goal -> goal.copy(currentValue = sums[goal.id] ?: 0.0) }
}

/** [withDerivedProgress] for a single goal. */
fun Goal.withDerivedProgress(
    entries: List<ProgressEntry>,
    tasks: List<Task>,
): Goal = copy(currentValue = DerivedProgress.currentValueOf(id, entries, tasks))
