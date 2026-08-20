package com.idomarhaim.goalpilot.domain.model

/**
 * The shape a **completion fact** has to have, wherever it is written — `#7`, `R6`,
 * `docs/PRODUCT_v0.3.md` §1.4 / §4.6.
 *
 * `R6` is *"there should be a way to complete the task from within 'quick add'"*, and §1.4's
 * answer to *what does completing award* ends on a constraint about **plumbing**, not about
 * arithmetic: completion stamps its facts into **one** timestamped record and the lifetime
 * total is a sum over those records, *"so a create-and-complete must emit that same fact, not
 * a second pipe"*.
 *
 * A task created already-done therefore cannot be `upsertTask()` followed by `setDone()`. That
 * is two writes, two failure modes, and a window in which the task exists un-completed — and
 * the second write is the one on a path that has just been shown to work, so the failure it
 * guards against is the one it cannot handle. The create carries the completion instead, and
 * this is the rule that says what "carrying it" means.
 *
 * ### The invariant, and why a half-written fact is worse than a missing one
 *
 * > **`isDone` and `completedAtEpochMillis` are one fact, and they are written together or
 * > not at all.**
 *
 * `setDone` has always upheld it — it writes `done` and `completedAt` in the same `update`,
 * nulling the stamp on an untick. Nothing upheld it on the **create** path, because until
 * `#7` no create path could produce a done task. One that could, and did not, would be far
 * more dangerous than it looks, because the two fields are read by **different consumers that
 * disagree about which one is the fact**:
 *
 * | Reader | Reads | A done task with no stamp |
 * |---|---|---|
 * | `functions/src/derived.ts` `pointsFromTasks` | `done` only | **counts** — points are awarded |
 * | `SummaryUseCase` | `isDone && (completedAt ?: 0) >= windowStart` | silently **dropped** |
 * | `DashboardViewModel` "done this week" | same shape | silently **dropped** |
 * | `TimeAllocationUseCase` | `isDone && completedAt != null` | silently **dropped** |
 *
 * So the failure is not *"a field is missing"*. It is that **the points move and the task
 * that moved them is invisible everywhere the user could go looking for it** — a total that
 * cannot be reconciled against anything on screen, with no error and nothing red. `Observed:`
 * by reading those four call sites at `HEAD`, 2026-08-20; not by hitting it, because `#7` is
 * the first ticket that could.
 *
 * ### Why it lives here and is applied in the repository
 *
 * In the **domain**, because it is a statement about what a completion fact *is*, and because
 * a rule reachable without Firebase is a rule the JVM suite can pin — the same reason `#9` put
 * `DurationEntry` here rather than in the add row.
 *
 * Applied in `TaskRepositoryImpl.upsertTask`, because that is the single choke point every
 * task write passes through. Applying it at the two add surfaces instead would be correct
 * today and quietly wrong at the third one somebody adds later: the invariant would then live
 * in the call sites, which is where invariants go to be forgotten. `upsertTask` already
 * normalises `createdAtEpochMillis` this way, so this is the existing habit of that function
 * and not a new responsibility for it.
 */
object TaskCompletion {

    /**
     * Returns [task] with its completion fact made whole, as of [nowMillis].
     *
     * Three cases, and the third is the one that has to be spelled out:
     *
     *  - **done, no stamp** → stamped with [nowMillis]. This is `#7`'s born-done task.
     *  - **done, already stamped** → left exactly as it is. A re-save must never move the
     *    time at which something happened; `AnalyticsViewModel`'s duration backfill saves
     *    completed tasks routinely, and re-dating them would rewrite history in the time
     *    chart every time a duration was corrected.
     *  - **not done** → the stamp is **cleared**, not preserved. There is no completion, so
     *    there is no time at which it happened; keeping a leftover stamp is exactly the
     *    half-written fact above with the fields the other way round. This mirrors
     *    `setDone(false)`, which has always nulled `completedAt` — after this, the create
     *    path and the tick path produce the same shape, which is what makes the readers
     *    above safe to trust.
     */
    fun stamp(task: Task, nowMillis: Long): Task = when {
        !task.isDone -> if (task.completedAtEpochMillis == null) task else task.copy(completedAtEpochMillis = null)
        task.completedAtEpochMillis != null -> task
        else -> task.copy(completedAtEpochMillis = nowMillis)
    }

    /**
     * Whether [task] holds a completion fact every reader agrees on.
     *
     * Exposed for the tests rather than for production: it is the predicate
     * [stamp] establishes, and asserting a normaliser against a separately-written
     * predicate is worth more than asserting it against a copy of its own branches.
     */
    fun isWellFormed(task: Task): Boolean =
        if (task.isDone) task.completedAtEpochMillis != null else task.completedAtEpochMillis == null
}
