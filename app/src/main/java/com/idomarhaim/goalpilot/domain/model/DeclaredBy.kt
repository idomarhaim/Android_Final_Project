package com.idomarhaim.goalpilot.domain.model

/**
 * Who declared an objective to be a **goal** — `docs/PRODUCT_v0.3.md` §1.1 (`C4` #13,
 * `C16` #37), the marker `#6` needs and the reason `#6` is allowed to file silently at all.
 *
 * §0.7 draws the whole line the sorter lives on:
 *
 * > The app may act silently on **instrumental** structure, but must ask before asserting an
 * > **intrinsic** edge. So: the agent may file, schedule, link and break down freely. It may
 * > **never** invent a goal.
 *
 * Filing a task under a goal the user already has is instrumental, so it happens with no
 * dialog and no setting. Deciding that a *new goal* exists is intrinsic, and §3.5 gives it one
 * author: *"an intrinsic edge (a goal) — **Ido, always**"*. This enum is what lets the sorter
 * propose one without asserting it: an [AI_SUGGESTED] objective **sits pending** rather than
 * silently appearing among his goals, and either becomes [USER]'s or has the marker dropped,
 * with the object and all its edges intact either way (§1.1's *lossless demotion*).
 *
 * **The marker carries provenance rather than being a boolean**, which is `C16`'s ruling and
 * not a convenience: a boolean could say *this is a goal* but could not say *and nobody knows
 * who said so*, which is the true state of every goal already in `goalpilot-56e30`.
 *
 * **Absence — a `null` [Goal.declaredBy] — means the objective is purely instrumental**: a
 * milestone, *a goal nobody wants for itself*, living in the same collection with the same
 * shape (§1.1). Nothing in the app creates one yet; `parentIds` does not exist on `GoalDto`
 * and no screen renders a sub-objective. That state is reachable **only** by demoting an
 * AI-suggested goal, which is exactly what makes the demotion lossless — no document moves.
 *
 * See `GoalDto.declaredBy` for the one thing this enum cannot express on its own: the
 * difference between a document written *before* the field existed and one whose marker was
 * deliberately dropped.
 */
enum class DeclaredBy {
    /** Ido said this is a goal — by writing it himself, or by keeping one the sorter proposed. */
    USER,

    /**
     * The sorter proposed it and nobody has ruled on it yet.
     *
     * **This is the pending state, and it is not a separate field** (§0.2, derive don't store):
     * *pending* is precisely *the only author on record is the model*, so a second `isPending`
     * flag would be a stored restatement of this one, free to disagree with it.
     */
    AI_SUGGESTED,

    /**
     * Nothing records who made this goal.
     *
     * §7.1's backfill for every document that predates the field, and **the migration must not
     * pretend otherwise** — stamping [USER] on them would manufacture a consent that was never
     * given, on exactly the objects §0.7 says need consent.
     */
    UNKNOWN;

    /** True while the sorter's proposal is still the only thing declaring this a goal. */
    val isPending: Boolean get() = this == AI_SUGGESTED

    companion object {
        /**
         * The enum for a stored name, or **null for anything else** — including
         * [GoalDto.DECLARED_BY_NONE][com.idomarhaim.goalpilot.data.firestore.dto.GoalDto], a
         * misspelling, and a value written by a future version of the app.
         *
         * Null means *instrumental*, so an unreadable value degrades toward *not a goal*
         * rather than toward *a goal Ido declared*. That is the safe direction: the failure
         * this app can survive is a milestone it forgot to show, never a goal it asserted on
         * his behalf.
         */
        fun fromName(name: String?): DeclaredBy? = entries.firstOrNull { it.name == name }
    }
}
