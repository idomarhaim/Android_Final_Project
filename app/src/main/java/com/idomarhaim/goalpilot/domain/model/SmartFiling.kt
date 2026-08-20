package com.idomarhaim.goalpilot.domain.model

/**
 * Where a smart-added task goes, and whether the app says anything about it — `#6`, `R3`,
 * `docs/PRODUCT_v0.3.md` §0.7 / §3.4 / §3.5.
 *
 * `R3` was *"it asks for approval on where to file every task you enter; the default should be
 * that it does not ask and just does it"*, and the triage promoted it out of settings: §0.7
 * makes silence the **rule**, not a preference.
 *
 * > The app may act silently on **instrumental** structure, but must ask before asserting an
 * > **intrinsic** edge.
 *
 * Filing is instrumental, so it is silent **always** — there is no dialog, no toggle, and no
 * default to configure. Deciding a *new goal* exists is intrinsic, so the sorter may propose
 * one but never assert one, and it says so when it does. That is the whole branch table, and
 * it is a pure function of the classification and the user's own lists so that it can be
 * tested without a device, a network or Firebase.
 *
 * **The decision is taken on presence, never on value.** §3.3's contract is that a field which
 * fails validation is *absent*, and the Cloud Function is the single site that enforces it
 * (§3.4) — so the only question asked here is whether an id **resolves to something the user
 * actually has**. That is resolution, not a second validator: it cannot disagree with the
 * Function, because it either finds the object or does not.
 */
sealed interface FilingDecision {

    /**
     * Whether this outcome is one the app says a sentence about.
     *
     * §3.4: *"Every row is silent except the last… An absent `suggestedGoalId` does not
     * **degrade** the outcome, it **changes** it."* Which is §0.4's test — *speak about a
     * failure the user can act on; stay silent about one they cannot* — applied honestly: a
     * task that landed where it belongs needs no announcement, and a task with no goal is not
     * a worse filing but a different one, which he may want to do something about.
     */
    val speaks: Boolean

    /**
     * The task belongs to a goal the user already has. **Silent** — no dialog, ever.
     *
     * The life area is deliberately not part of this decision: the goal already knows which
     * areas it serves, and re-filing it from a task the sorter just read would let one
     * quick-add rewrite the shape of the user's week.
     */
    data class ExistingGoal(val goalId: String, val goalTitle: String) : FilingDecision {
        override val speaks: Boolean get() = false
    }

    /**
     * No existing goal fits, and the sorter has a title worth proposing. **Speaks.**
     *
     * The goal is created carrying [DeclaredBy.AI_SUGGESTED], which is §1.1's *pending*: it
     * exists, it holds the task, and it is marked as nobody's but the model's until the user
     * keeps it or drops the marker. That marking is what keeps this side of §0.7 — the app has
     * not asserted an intrinsic edge, it has written down a proposal and said so.
     *
     * [title] is **content** the moment it lands among his goals (§3.3 D), so it is stored as
     * authored and never re-rendered or translated (`C15b`).
     */
    data class NewGoal(
        val title: String,
        val category: GoalCategory,
        val lifeAreaId: String?,
    ) : FilingDecision {
        override val speaks: Boolean get() = true
    }

    /**
     * No existing goal fits and there is nothing worth proposing. The task is filed with **no
     * goal at all**, and the app **speaks**.
     *
     * §3.5: *"the sorter must **never** invent a goal; low confidence leaves `goalId` null."*
     * This is that row. The tempting alternative — make a goal up out of the task's own title
     * whenever the model is unsure — is precisely the thing §0.7 forbids, and it is worse than
     * it looks: an invented goal is an intrinsic claim about what the user wants his life to
     * be, minted from a sentence he typed in three seconds.
     *
     * An unfiled task is not lost. It is in his task list, it is in this message, and the goal
     * it belongs to is one tap away — which is the correct amount of work for a decision only
     * he can make.
     */
    data class NoGoal(val suggestedLifeAreaId: String?) : FilingDecision {
        override val speaks: Boolean get() = true
    }
}

/** The branch table §3.4 describes, as one pure function. */
object SmartFiling {

    /**
     * How sure the sorter must be before it is worth proposing a **new goal**.
     *
     * No spec line fixes a number, so this one is derived and stated rather than smuggled in.
     * `0.5` is *more likely than not*, and the two values the app itself produces sit either
     * side of it on purpose: the offline heuristic reports `0.4` when it matched a goal by
     * keyword — which takes the existing-goal branch anyway and never consults this — and
     * `0.2` when it matched nothing, which is exactly the case that must **not** become a new
     * goal. A guess the sorter itself doubts is the worst possible author for an intrinsic
     * edge.
     */
    const val MIN_CONFIDENCE_FOR_NEW_GOAL = 0.5f

    /**
     * Decides where a smart-added task goes.
     *
     * **The task's own title is not a parameter, and that is the point.** Every earlier draft
     * of this had one, and every use of it was the same use: fall back to the task title when
     * the sorter has no goal to propose. A task is a thing you do and a goal is a thing you
     * want; promoting one into the other is how an app ends up telling someone that *buy milk*
     * is one of their life goals. Not taking the argument makes that unwritable rather than
     * merely discouraged.
     *
     * [goals] and [lifeAreas] are the user's own, and are the same lists that travelled with
     * the request — so an id that resolves here is provably an id he has.
     *
     * **An absent confidence is treated as low.** §3.3 makes absence the signal for a value
     * that failed validation, and the client must not substitute one; between the two ways of
     * resolving it, declining to invent a goal on a number nobody stated is the direction §0.7
     * points in. [TaskClassification.confidence] defaults to `0f`, so this needs no special
     * case — it needs only to not be written the other way round.
     */
    fun decide(
        classification: TaskClassification,
        goals: List<Goal>,
        lifeAreas: List<LifeArea>,
    ): FilingDecision {
        val matched = goals.firstOrNull { it.id == classification.suggestedGoalId }
        if (matched != null) return FilingDecision.ExistingGoal(matched.id, matched.title)

        // Resolution, not validation: the Function already checked membership (§3.4), and an
        // id that survives that and still does not resolve here means the area was deleted
        // between the two — in which case unfiled is the honest answer, not an error.
        val areaId = lifeAreas.firstOrNull { it.id == classification.suggestedLifeAreaId }?.id

        val proposed = classification.suggestedNewGoalTitle?.trim().orEmpty()
        val confident = classification.confidence >= MIN_CONFIDENCE_FOR_NEW_GOAL
        return if (proposed.isNotBlank() && confident) {
            FilingDecision.NewGoal(
                title = proposed,
                category = classification.suggestedCategory,
                lifeAreaId = areaId,
            )
        } else {
            FilingDecision.NoGoal(suggestedLifeAreaId = areaId)
        }
    }
}
