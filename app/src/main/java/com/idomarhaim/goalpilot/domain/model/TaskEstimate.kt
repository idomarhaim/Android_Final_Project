package com.idomarhaim.goalpilot.domain.model

/**
 * What the LLM says about a task: **how demanding it is and how long it takes**
 * (spec §6 Core "point scoring", extended with the duration the
 * time-allocation chart is built on).
 *
 * The two travel together because they come from one `scoreTask` call: GROQ's
 * free tier allows 30 requests/minute, and asking twice for facts about the same
 * sentence would halve the number of tasks the Google Tasks import can process.
 *
 * ⚠️ **There is no `points` field, and there never will be** — §3.3 A, enforced here since
 * `#55`. The model judges (*which of three words fits this work?*) and the app computes
 * (`round(minutes / 3) × difficulty`), which is §0.5 at full strength. A `points` field
 * would let the model move a currency by phrasing, and `C11a` measured free numbers from
 * this model swinging **2× run-to-run** and **1.8× between languages**.
 */
data class TaskEstimate(
    /**
     * Which of §1.4's three words the model chose, defaulting to the neutral one.
     *
     * `ROUTINE` is `×1.0`, so a call that said nothing about difficulty prices the task on
     * its minutes alone — the same shape as a missing duration reading as absent rather than
     * as a guess.
     */
    val difficulty: Difficulty = Difficulty.ROUTINE,
    /**
     * How long the model said the task takes, or **null when it did not say**.
     *
     * Nullable since #9. Spec §3.4: *a field that fails validation is absent* — no
     * null sentinel meaning "I tried", no default, no substitute. It used to be a
     * non-null `Int` filled in by a point-derived fallback, which made every
     * silent failure indistinguishable from an answer and forced
     * `TaskScoring.looksLikeFallback` to exist to guess the difference back out
     * again. Absence is the honest value, and it is the one the caller can act on.
     */
    val minutes: Int? = null,
)

/**
 * Where a task's [Task.estimatedMinutes] came from — **stored, never reconstructed**
 * (#9, spec §1.4).
 *
 * This is `R8`'s placeholder icon expressed as data. Before #9 the app inferred
 * provenance after the fact by recomputing what each fallback *would* have
 * returned and comparing — evidence, not proof, and §0.3's *second number that
 * quietly disagrees*. The producer knows the answer at the moment it produces it,
 * so it writes it down and nothing downstream has to infer anything.
 */
enum class DurationSource {

    /**
     * The person typed the number.
     *
     * **Sticky, unconditionally and with no threshold** (§1.4, §0.6): the typed
     * number *is* the duration and no re-estimation may ever overwrite it. A fact
     * about the user's own day is theirs, and any threshold would make the app
     * judge when they are wrong about it.
     */
    USER,

    /** A model answered with it. */
    AI,

    /**
     * Nothing recorded where it came from — which is also what every task written
     * before #9 reads as, and what a skipped duration box stores.
     *
     * Not the same as [USER]: it is safe to re-estimate, because nothing says a
     * person chose it. See `DurationEntry` for why that is a fact rather than a
     * preference.
     */
    UNKNOWN;

    companion object {
        /** Unknown, misspelled and absent all read as [UNKNOWN] — the honest default. */
        fun fromName(name: String?): DurationSource =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UNKNOWN
    }
}

/**
 * The duration box's whole state: a number that may be absent, and where it came
 * from (`R8`, #9).
 *
 * It lives in the domain rather than inside `AddTaskRow` for one reason: the
 * precedence rule §1.4 calls *unconditional* is the only interesting logic in the
 * box, and a rule that can only be exercised on a running device is a rule whose
 * two directions do not both get tested. Here every transition is a pure function
 * and the composable holds one of these instead of reconstructing a duration from
 * the points field. (Since `#55` nothing could: the derivation runs the other way.)
 */
data class DurationEntry(
    val minutes: Int? = null,
    val source: DurationSource = DurationSource.UNKNOWN,
) {

    /** True once the person has entered a number — the sticky state. */
    val isTyped: Boolean get() = source == DurationSource.USER

    /**
     * `R8`, worded exactly: *"there should be an icon inside the box for as long
     * as the person has not entered a number."* So the icon is the complement of
     * [isTyped] and nothing else — an AI estimate and an empty box both show it.
     */
    val showsEstimateIcon: Boolean get() = !isTyped

    /**
     * What the box holds when an estimate arrives.
     *
     * **A typed value is returned untouched — always, whatever the estimate says.**
     * This is §1.4's rule, and the shape matters as much as the outcome: there is
     * no comparison here to soften later. `null` in means the model did not answer,
     * which empties the box rather than filling it with a guess (§3.4).
     */
    fun withEstimate(estimateMinutes: Int?): DurationEntry {
        if (isTyped) return this
        val sanitized = TaskDuration.sanitize(estimateMinutes)
        return DurationEntry(
            minutes = sanitized,
            source = if (sanitized == null) DurationSource.UNKNOWN else DurationSource.AI,
        )
    }

    /**
     * What the box holds when the title is rewritten.
     *
     * An AI estimate belonged to the old wording and is dropped — keeping it would
     * credit a rewritten task with the previous one's time. **A typed duration
     * survives**, because retitling is a re-estimation trigger and §1.4 exempts a
     * typed value from all of them.
     */
    fun withRetitle(): DurationEntry = if (isTyped) this else DurationEntry()

    /**
     * What the box holds after the person edits it.
     *
     * Clearing the field is not the same as never having typed: it returns the box
     * to [DurationSource.UNKNOWN] so the next estimate can fill it again. Otherwise
     * an empty box would stay sticky and the AI button would appear to do nothing.
     */
    fun typed(text: String): DurationEntry {
        val digits = text.filter { it.isDigit() }.take(MAX_DIGITS)
        val value = digits.toIntOrNull()
        return if (value == null) {
            DurationEntry(minutes = null, source = DurationSource.UNKNOWN)
        } else {
            DurationEntry(minutes = value, source = DurationSource.USER)
        }
    }

    /** What the box renders: the number, or empty when there is nothing to show. */
    fun text(): String = minutes?.toString().orEmpty()

    /**
     * The duration actually written when the task is created.
     *
     * §3.4: an absent estimate means *ask the user how long*, and
     * [TaskDuration.DEFAULT_MINUTES] **if they skip** — so the skip is recorded as
     * [DurationSource.UNKNOWN] rather than dressed up as an answer. A typed value
     * is clamped to the storable range but keeps its [DurationSource.USER] stamp:
     * clamping is what the database can hold, not a second opinion about the day.
     */
    fun resolve(): Pair<Int, DurationSource> {
        val stored = TaskDuration.sanitize(minutes)
            ?: return TaskDuration.DEFAULT_MINUTES to DurationSource.UNKNOWN
        return stored to source
    }

    private companion object {
        /** `480` is the ceiling, so four digits is already more than storable. */
        const val MAX_DIGITS = 4
    }
}

/**
 * How long a completed task counts for when slicing the user's time.
 *
 * Every completed task must contribute *something*, including the ones created
 * before durations existed and the ones added while offline — otherwise the pie
 * silently under-reports whole areas of the user's life and looks like a bug.
 * [minutesOf] is the single place that decides, so the chart, the analytics
 * summary and the tests all agree on the same number.
 */
object TaskDuration {

    /** Floor and ceiling for anything stored or estimated. */
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 480

    /** Used when nothing at all is known — a "half-hour chore". */
    const val DEFAULT_MINUTES = 30

    /**
     * How long a **task written before `#55`** must have been, reconstructed from the point
     * value that is the only record of effort it carries.
     *
     * ### This is the old `fallbackMinutes`, narrowed from a live path to a migration
     *
     * It used to be what [minutesOf] fell back to for *every* task with no duration — which
     * is the inversion §1.4 names: the app invented a reward number from a word count
     * (`5 + 3×words`) and then derived *how long your life took* from it, putting the
     * time-allocation chart downstream of a gamification currency. `#55` inverted the
     * constant, so nothing computes minutes from points any more.
     *
     * What survives is exactly one use, and it is **lossless by arithmetic**: a legacy point
     * value `p` reconstructs to `3p` minutes, which prices back at
     * `round(3p / 3) × 1.0 = p`. So a task completed before `#55` is worth precisely what it
     * was worth then — the migration moves where the fact lives without re-pricing a single
     * historical completion. See `TaskDto.legacyCompletion`.
     *
     * Not clamped to [MIN_MINUTES]/[MAX_MINUTES], deliberately: clamping would break that
     * identity at both ends and silently re-price the oldest and largest tasks, which is the
     * one thing this function exists to avoid. Its input is a stored point value, not user
     * input, so there is nothing here to defend against.
     */
    fun legacyMinutesFromPoints(points: Int): Int = (points * 3).coerceAtLeast(1)

    /**
     * How long the task counts for: its stored estimate, or [DEFAULT_MINUTES].
     *
     * **No longer derived from points** (`#55`, §1.4). A task with no duration is a
     * half-hour chore — the same answer `DurationEntry.resolve` gives a skipped box — rather
     * than a number reconstructed from what the task was scored, which is the direction the
     * inversion runs in now.
     */
    fun minutesOf(task: Task): Int =
        task.estimatedMinutes?.takeIf { it > 0 }?.coerceAtMost(MAX_MINUTES)
            ?: DEFAULT_MINUTES

    /** Clamps whatever the model returned into the range the UI can render. */
    fun sanitize(minutes: Int?): Int? =
        minutes?.takeIf { it > 0 }?.coerceIn(MIN_MINUTES, MAX_MINUTES)
}

/**
 * **Points, as a view of effort** — `docs/PRODUCT_v0.3.md` §1.4,
 * [#55](https://github.com/idomarhaim/Android_Final_Project/issues/55).
 *
 * ```
 * points = round(minutes / 3) × difficulty
 * ```
 *
 * ### What `#55` deleted here, and what it inverted
 *
 * - **`heuristicPoints` (`5 + 3×words`) is gone.** It priced a task by counting the words in
 *   its title, and `TaskDuration.fallbackMinutes` then derived the *duration* from that
 *   score — so an offline task's contribution to the time-allocation chart was a function of
 *   how verbosely it had been typed. §1.4: *"the fix inverts a constant rather than adding
 *   one, and it retires `heuristicPoints` outright."*
 * - **The `5..50` cap is gone.** It priced an eight-hour task like a ninety-minute one. The
 *   floor went with it: a 15-minute `LIGHT` task is worth **4**, and is not raised to 5.
 * - **The ceiling is 240**, and it is not a constant — it is what the formula *yields* at the
 *   storable maximum (`480` minutes `× DEMANDING`). Nothing clamps to it; delete the
 *   duration ceiling and this moves with it, which is the property a written-down `MAX` would
 *   not have. §1.4's *"the levelling ceiling rises 50 → 240"* is that arithmetic, and
 *   `Leveling`'s thresholds are untouched.
 *
 * ### Today's anchor survives exactly
 *
 * A 30-minute `ROUTINE` task is `round(30 / 3) × 1.0` = **10 points**, which is what it has
 * always been worth. That is deliberate: the inversion is about *which quantity is derived
 * from which*, not about re-pricing the user's history.
 *
 * ### The multipliers are here and not in the prompt
 *
 * See [Difficulty]. The model chooses a word; this object turns words into numbers, so the
 * currency cannot be moved by phrasing (§0.5).
 */
object TaskScoring {

    /**
     * The divisor: **three minutes to the point**, at `ROUTINE`.
     *
     * The one constant `#55` kept, and it is the same `3` that used to run the other way in
     * `fallbackMinutes`. Inverting a constant rather than introducing one is why today's
     * anchor lands unchanged.
     */
    const val MINUTES_PER_POINT = 3

    /**
     * What [minutes] of work at [difficulty] is worth.
     *
     * Rounded twice, and the order matters: `round(minutes / 3)` first, so the *effort* half
     * is a whole number of points before the judgement is applied, then rounded again to
     * land on an integer currency. §1.4 writes the formula that way and it is what makes
     * `30 → 10` exact instead of subject to floating point.
     *
     * Never clamped. A cap is a second opinion about how long the day was, and deleting the
     * `5..50` one is half of what this ticket is.
     */
    fun pointsFor(minutes: Int, difficulty: Difficulty): Int {
        val effortPoints = Math.round(minutes.toDouble() / MINUTES_PER_POINT)
        return Math.round(effortPoints * difficulty.multiplier).toInt()
    }
}
