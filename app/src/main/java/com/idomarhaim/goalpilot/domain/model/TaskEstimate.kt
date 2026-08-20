package com.idomarhaim.goalpilot.domain.model

/**
 * What the LLM thinks a task is worth and how long it takes
 * (spec §6 Core "point scoring", extended with the duration the
 * time-allocation chart is built on).
 *
 * The two travel together because they come from one `scoreTask` call: GROQ's
 * free tier allows 30 requests/minute, and asking twice for facts about the same
 * sentence would halve the number of tasks the Google Tasks import can process.
 */
data class TaskEstimate(
    val points: Int = 10,
    /**
     * How long the model said the task takes, or **null when it did not say**.
     *
     * Nullable since #9. Spec §3.4: *a field that fails validation is absent* — no
     * null sentinel meaning "I tried", no default, no substitute. It used to be a
     * non-null `Int` filled in by [TaskDuration.fallbackMinutes], which made every
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
 * the points field.
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
     * Offline estimate from a task's point value, **for the chart only**.
     *
     * Points are scored 5..50 by difficulty, so ×3 spans 15..150 minutes, which is
     * the right shape for personal tasks: deterministic, monotonic in difficulty,
     * and never zero.
     *
     * **Narrowed by #9 to exactly one caller, [minutesOf].** It used to stand in
     * for a missing model answer at the two `scoreTask` sites and in
     * `parseClassification`, which is how a *stored* duration could be a word-count
     * derivative wearing an estimate's clothes. Nothing stores its output any more:
     * a task with no duration keeps `estimatedMinutes = null`, and this only decides
     * what such a task is worth to the pie so that it is not dropped from it. The
     * inversion §1.4 wants — points computed *from* minutes rather than the reverse —
     * is `C1` #19 and deletes this function outright.
     */
    fun fallbackMinutes(points: Int): Int = (points * 3).coerceIn(MIN_MINUTES, MAX_MINUTES)

    /** The stored estimate when there is one, otherwise the point-based fallback. */
    fun minutesOf(task: Task): Int =
        task.estimatedMinutes?.takeIf { it > 0 }?.coerceAtMost(MAX_MINUTES)
            ?: fallbackMinutes(task.points)

    /** Clamps whatever the model returned into the range the UI can render. */
    fun sanitize(minutes: Int?): Int? =
        minutes?.takeIf { it > 0 }?.coerceIn(MIN_MINUTES, MAX_MINUTES)
}

/**
 * The offline half of task *scoring*, in the domain rather than in the repository
 * that calls the model.
 *
 * **Durations left this object in #9.** It used to carry [looksLikeFallback] and a
 * `SERVER_FALLBACK` sentinel, whose job was to recognise an estimate that no model
 * had produced by recomputing both fallbacks and comparing — *"evidence, not
 * proof"* by its own KDoc, since a model is free to land on the same numbers by
 * agreement rather than by failure. [DurationSource] records the answer at the
 * point of production instead, so there is nothing left to reconstruct and both
 * are deleted.
 *
 * What remains is points, which #9 deliberately does not touch: the inversion that
 * retires [heuristicPoints] is §1.4's, and it belongs to `C1` #19.
 */
object TaskScoring {

    /** Matches the 5..50 range the `scoreTask` Cloud Function is prompted for. */
    const val MIN_POINTS = 5
    const val MAX_POINTS = 50

    /**
     * Offline point estimate: a longer, more specific task title generally
     * describes more work. Deterministic so the UI never jumps around.
     */
    fun heuristicPoints(taskTitle: String): Int {
        val words = taskTitle.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return (MIN_POINTS + words * 3).coerceIn(MIN_POINTS, MAX_POINTS)
    }
}
