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
 * - every completed [Task] with an edge to the goal contributes **that edge's declared
 *   contribution** ([GoalEdge.contribution]), and nothing at all when the edge declares
 *   none.
 *
 * That second bullet is §1.5 as of `#55`: *"an edge declares its contribution in the
 * objective's own word, or contributes nothing to the measure."* It was
 * `Task.progressContribution`, a number on the task rather than on the pair — which cannot
 * be right, because a 30-minute run is `1` to *"run 20 times"* and `5` to *"run 100 km"*.
 * Its `1.0` default was a silence rather than a value, and a silence now adds nothing.
 *
 * **Nothing already stored changed value.** A task written before `#55` reads its stored
 * `progressContribution` onto its edge verbatim (`TaskDto.toDomain`), so every existing goal
 * sums to exactly what it summed to yesterday. Only tasks created *after* the change arrive
 * with an undeclared contribution.
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
            for (edge in task.goalEdges) {
                val goalId = edge.goalId.takeIf { it.isNotBlank() } ?: continue
                // An undeclared contribution adds nothing (§1.5). Skipping rather than
                // adding 0.0 keeps a goal that is named only by silent edges **absent**
                // from the map, which is the state `currentValues` already documents as
                // "no facts" -- one meaning, one representation.
                val contribution = edge.contribution ?: continue
                sums[goalId] = (sums[goalId] ?: 0.0) + contribution
            }
        }
        return sums
    }

    /**
     * How many progress entries name each goal — the count behind
     * [Goal.loggedEntryCount] (`#66`).
     *
     * A **separate pass from [currentValues]** rather than a second return value
     * on it, and the reason is that they are not the same population. That one
     * sums entries **and** the declared contributions of completed tasks; this
     * one counts entries alone, because the number it feeds is rendered beside a
     * *Progress log* the user can go and count. Merging them would have to pick
     * one population for both and would make one of the two numbers wrong.
     *
     * Goals with no entries are **absent** rather than present at `0`, the same
     * contract [currentValues] documents, so a caller resolves a missing key
     * itself and this never has to be handed the goal list to enumerate it.
     */
    fun entryCounts(entries: List<ProgressEntry>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (entry in entries) {
            val goalId = entry.goalId
            if (goalId.isBlank()) continue
            counts[goalId] = (counts[goalId] ?: 0) + 1
        }
        return counts
    }

    /** [entryCounts] for one goal. `0` when nothing has been logged against it. */
    fun entryCountOf(goalId: String, entries: List<ProgressEntry>): Int {
        if (goalId.isBlank()) return 0
        return entryCounts(entries)[goalId] ?: 0
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

    /**
     * *"Overall progress"* across [goals] — the mean of how **complete** each one
     * is, where a goal is at most complete, `0f..1f`.
     *
     * ### Why this clamps when [Goal.progressFraction] deliberately does not
     *
     * §1.5 makes overshoot legal *and shown*, and #49 deleted four clamps to get
     * there. This is not a fifth. The clamp that died was on **the goal's own
     * number**, which made a beaten goal unreadable on the one screen a human
     * writes to; this one is at an **aggregation site**, and the two are different
     * things — the same distinction as `GpLinearProgress` clamping at the draw
     * call. A goal that is at 300% still says 300% everywhere it speaks for itself.
     *
     * Unclamped, this was a **mean of incomparable quantities**. §4.4 already
     * refuses that shape one chart over: *"a percentage is a fraction of its own
     * target, so ranking by movement partly ranks how modest the goals are"* —
     * which is why the effort/outcome chart orders only minutes. An average of
     * per-goal percentages has the same flaw, plus one the ranking does not: it is
     * **unbounded**, so a single goal accumulating past a periodic target drags a
     * headline that claims to describe everything.
     *
     * *Observed:* on a device, 2026-08-16 — the dashboard read
     * **"Overall progress 16259%"** (`widget-pack`'s device pass, `d2cbaef`). The
     * ring beside that text looked perfectly normal, because `ProgressRing` clamps
     * at the draw call; only the number lied, and the same number is put into a
     * **shared post** by `SocialRepositoryImpl:189`.
     */
    fun overallCompletion(fractions: Iterable<Float>): Float {
        var sum = 0f
        var count = 0
        for (f in fractions) {
            sum += f.coerceIn(0f, 1f)
            count++
        }
        return if (count == 0) 0f else sum / count
    }

    /**
     * [overallCompletion] over goals — **the measured ones**, `#66`.
     *
     * A goal with no measure has `currentValue / 100.0`, where the `100.0` is
     * §1.3's default for a goal that said nothing. It is therefore not a low
     * score: it is **not a score**, and averaging it in makes the dashboard's
     * *Overall progress* headline a mean of numbers and non-numbers. Three
     * unmeasured goals beside one finished goal read **25 %**, which states that
     * three quarters of the work is outstanding on goals that were never
     * counting anything.
     *
     * The filter is here rather than at the caller for the reason the clamp is:
     * this is the aggregation site, and putting the rule at one of two callers
     * would leave the other one wrong — which is the shape [Goal.isUnmeasured]'s
     * KDoc argues against for the marker and the percentage.
     *
     * **`0f` when no goal has a measure**, which is what an untouched account
     * already read and is the honest floor. `Untested:` what the dashboard should
     * *say* in that state — a `0 %` ring for an account whose goals all
     * legitimately have no number is the ticket's own defect one layer up — but
     * `feature/dashboard/` is held by `61-google-calendar`, so the display half is
     * named on the board and left alone rather than half-fixed.
     */
    fun overallCompletionOf(goals: List<Goal>): Float =
        overallCompletion(goals.filterNot { it.isUnmeasured }.map { it.progressFraction })
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
    // Counted in the same pass, at the same seam, for the same reason (`#66`):
    // a screen holding a `Goal` can read `loggedEntryCount` without knowing that
    // entries exist, exactly as it reads `currentValue` without knowing they do.
    val counts = DerivedProgress.entryCounts(entries)
    return map { goal ->
        goal.copy(
            currentValue = sums[goal.id] ?: 0.0,
            loggedEntryCount = counts[goal.id] ?: 0,
        )
    }
}

/** [withDerivedProgress] for a single goal. */
fun Goal.withDerivedProgress(
    entries: List<ProgressEntry>,
    tasks: List<Task>,
): Goal = copy(
    currentValue = DerivedProgress.currentValueOf(id, entries, tasks),
    loggedEntryCount = DerivedProgress.entryCountOf(id, entries),
)
