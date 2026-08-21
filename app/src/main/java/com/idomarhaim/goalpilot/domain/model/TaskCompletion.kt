package com.idomarhaim.goalpilot.domain.model

/**
 * The shape a **completion fact** has to have, wherever it is written — `#7`, `R6`,
 * `#55`, `docs/PRODUCT_v0.3.md` §1.4 / §4.6.
 *
 * `R6` is *"there should be a way to complete the task from within 'quick add'"*, and §1.4's
 * answer to *what does completing award* ends on a constraint about **plumbing**, not about
 * arithmetic: completion stamps its facts into **one** timestamped record and the lifetime
 * total is a sum over those records, *"so a create-and-complete must emit that same fact, not
 * a second pipe"*.
 *
 * A task created already-done therefore cannot be `upsertTask()` followed by `setDone()`. That
 * is two round trips, two failure modes, and a window in which the task exists un-completed —
 * and the second write is the one on a path that has just been shown to work, so the failure
 * it guards against is the one it cannot handle. The create carries the completion instead,
 * and this object is what "carrying it" means.
 *
 * ### `#55` moved the fact, and the invariant went from *upheld* to *unrepresentable*
 *
 * Until `#55` the fact was two fields on the task document, `done` and `completedAt`, and
 * this object existed to normalise the four states they can be in into the two that are
 * legal. That was a real hazard, because the two fields were read by **different consumers
 * that disagreed about which one was the fact** — the projection function counted `done`
 * while the weekly summary, the dashboard's done-this-week count and the time chart all
 * required the stamp. A done task with no stamp moved the points and was invisible
 * everywhere the user could go looking for them.
 *
 * There is now **one** field, [Task.completion], and it is an object that either exists or
 * does not. `isDone` is `completion != null`. The half-written state has no representation,
 * so nothing has to normalise it and no reader can pick the wrong half — which is §0.2 and
 * §0.3 arriving at the same place from different directions.
 *
 * ### What is left here
 *
 * [of], which is the single place a completion fact is **minted**. Both callers are in
 * `TaskRepositoryImpl` — the tick (`setDone`) and the born-done create (`upsertTask`) — and
 * they call the same function with the same arguments, which is what *"not a second pipe"*
 * means once the fact has its own document. If a third completion surface ever appears, it
 * gets this function, not a copy of its body.
 */
object TaskCompletion {

    /**
     * The fact a completion of [task] emits, as of [nowMillis].
     *
     * ### It banks what is true **now**, and never re-reads it
     *
     * [CompletionFact.minutes] and [CompletionFact.difficulty] are copied off the task at the
     * moment of the tick and are then frozen. Correcting a duration afterwards changes what
     * the task is *estimated* at and leaves what it *earned* alone — that is §1.4's *"points
     * are banked as their inputs"*, and it is what makes an untick give back exactly what the
     * tick gave.
     *
     * [TaskDuration.minutesOf] rather than `task.estimatedMinutes` on purpose: a task with no
     * duration is worth `DEFAULT_MINUTES`, and banking the resolved number means the fact is
     * complete on its own. A reader of `completionFacts` never has to go and find the task to
     * know what the completion was worth — which is the property the projection function is
     * built on.
     *
     * ### Re-completing an already-done task keeps the original stamp
     *
     * A re-save must never move the time at which something happened;
     * `AnalyticsViewModel`'s duration backfill saves completed tasks routinely, and re-dating
     * them would rewrite history in the time chart every time a duration was corrected. So
     * a **banked** [Task.completion] is returned whole — its inputs included, since re-banking
     * today's estimate is the same re-pricing the freeze exists to prevent.
     *
     * ### An unstamped fact is a request, not a record — and this is the case that bit
     *
     * `#7`'s born-done surfaces cannot supply a timestamp: only the write knows `now`. They
     * therefore hand in a **placeholder** `CompletionFact()`, whose `completedAtEpochMillis`
     * is `0L`, meaning *complete this at the write*. A plain `?:` treats that placeholder as
     * an existing fact and returns it untouched — banking a completion stamped at the epoch,
     * with `DEFAULT_MINUTES` it was never asked about.
     *
     * That is the **exact** failure this object was created for, arriving through the new
     * shape: a task that awards points and is invisible in every window-based reader, because
     * the weekly summary, the dashboard's done-this-week count and the time chart all filter
     * on a stamp that reads as 1970. `Observed:` 2026-08-21 — `TaskCompletionTest`'s first
     * case went red on the `?:` version, which is why the check below is on the timestamp
     * rather than on nullness.
     */
    fun of(task: Task, nowMillis: Long): CompletionFact {
        val banked = task.completion
        if (banked != null && banked.completedAtEpochMillis > 0L) return banked
        return CompletionFact(
            completedAtEpochMillis = nowMillis,
            minutes = TaskDuration.minutesOf(task),
            difficulty = task.difficulty,
        )
    }
}
