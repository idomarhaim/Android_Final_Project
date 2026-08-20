package com.idomarhaim.goalpilot.domain.model

/**
 * What kind of quantity a goal counts — spec §1.3 (`C7`
 * [#14](https://github.com/idomarhaim/Android_Final_Project/issues/14)).
 *
 * **The list is closed at seven and the kind is app logic**, because it is what
 * fixes the arithmetic: a ladder of fill buttons, a rounding rule, and one day a
 * conversion all need to know whether `4` means four books or four litres. The
 * companion half — the *word* — is user content and is never translated
 * (§5.1 `C15b`). Getting that boundary wrong is how #51's sweep gets re-broken,
 * so the two live in one type, [Measure], with the boundary written on it.
 *
 * **There is deliberately no `UNKNOWN` member.** §1.3 fixes the list at seven and
 * says its labels are translated; an eighth would be an untranslatable label
 * inside a closed set. A goal whose kind nobody recorded is expressed as
 * `Measure(kind = null, …)` instead — see [Measure.kind].
 *
 * **No `label` constructor argument, and that is not an omission.** A language
 * switch cannot reach a constructor argument (`kb/dev/untranslatable-idioms.md`
 * §1), which is exactly the defect [GoalCategory.label] is deprecated for.
 * Display text for these seven lives in `feature/goals/GoalMeasureStrings.kt`,
 * ready to become a string resource when #51 resumes.
 */
enum class MeasureKind {
    /** Discrete things: books, chapters, pushups. Whole numbers only. */
    COUNT,

    /** Time spent: hours, minutes. */
    DURATION,

    /** How far: km, miles. */
    DISTANCE,

    /** How much liquid or bulk: litres, cups. */
    VOLUME,

    /** How heavy: kg, lb. */
    MASS,

    /** Currency. */
    MONEY,

    /**
     * A share of a whole.
     *
     * §7.1: `"%"` survives **only as a chosen** `PERCENT` measure. A goal that
     * merely *defaulted* to `"%"` — which is every goal written before this
     * change, because the field's default was `"%"` — becomes **absent** instead.
     * `GoalDto.resolvedMeasure` carries why the two cannot be told apart on the
     * wire, and why absent is the recoverable direction.
     */
    PERCENT,
    ;

    companion object {
        /** The stored name back to a member, or `null` for absent/unrecognised. */
        fun fromName(name: String?): MeasureKind? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * A goal's measure — spec §1.3's *closed kind plus a free word*.
 *
 * It is the only shape where *nothing is unsayable and nothing is unknowable*: a
 * fixed unit list is knowledgeable but mute, free text is expressive but stupid,
 * and a dimensional model buys conversions this app never performs.
 *
 * **A goal may carry no measure at all, and absence is the default** (`E6`), so
 * [Goal.measure] is nullable. This type is what a goal has *when it measures
 * something*.
 */
data class Measure(
    /**
     * What is being counted — or `null` when the answer was never recorded.
     *
     * **Null is the migration's half-way state, not a normal value.** A goal
     * written before §1.3 carries free text like `"litres"` and nothing else: the
     * word survives losslessly, but classifying it would mean matching on user
     * text, which #11's brief forbids outright (*"never invent a kind from a
     * string match"*) and which is broken by construction — the word may be in
     * any language, abbreviated, or misspelt.
     *
     * So the word is kept, the kind is left open, and the app **asks** (`C22`
     * #44's measure proposal). Everything that needs arithmetic — [FillLadder]
     * above all — treats a null kind as *not yet answerable* rather than guessing.
     */
    val kind: MeasureKind?,
    /**
     * The user's own word for the unit: `"books"`, `"litres"`, `"km"`.
     *
     * **User content — never translated** (§5.1 `C15b`, spec §8). It is displayed
     * verbatim in every language.
     */
    val word: String,
) {
    /**
     * Whether the app knows what this counts, and so whether it may compute with
     * it. False only for a pre-§1.3 document the migration could not classify.
     */
    val isClassified: Boolean get() = kind != null

    companion object {
        /**
         * A measure, or `null` when there is nothing to record.
         *
         * A blank word with no kind is not a half-measure, it is the absence
         * §1.3 makes the default — collapsing it here keeps *no measure*
         * single-valued instead of letting `Measure(null, "")` be a second
         * spelling of it.
         */
        fun of(kind: MeasureKind?, word: String?): Measure? {
            val trimmed = word?.trim().orEmpty()
            if (kind == null && trimmed.isEmpty()) return null
            return Measure(kind, trimmed)
        }
    }
}

/**
 * How progress is put in, **per goal** — spec §1.3.
 *
 * > **Input mode**, per goal: `buttons · number · tick · auto`. Whether logging
 * > **adds or sets** rides this — per goal, because a global rule is the
 * > granularity error.
 *
 * The list is closed at four because §1.3 closes it. Which of the four a user may
 * actually *pick* is a separate question, answered by [OFFERED] — the same
 * one-switch shape [AppLanguage.OFFERED] uses, and for the same reason: a subset
 * that lives in one named list is auditable, whereas a subset spread across the
 * pickers that happen to render it is not.
 */
enum class InputMode(
    /**
     * Whether a log against a goal in this mode **adds to** or **replaces** what
     * is there — §1.3's rule, pinned here rather than at the call site because
     * *per goal* means it has to travel with the goal.
     *
     * **Behaviour, not a label**, so a constructor argument is the right home for
     * it — unlike [GoalCategory.label], whose problem was that a language switch
     * cannot reach one.
     */
    val logging: LoggingRule,
) {
    /**
     * A row of repeat-tappable fill buttons — `R25`, #11. Each tap writes one
     * [ProgressEntry], so the tally is a sum over entries and needs no second
     * counter (§4.6).
     */
    BUTTONS(LoggingRule.ADDS),

    /** The Amount dialog: type a number. Today's behaviour, and the default. */
    NUMBER(LoggingRule.ADDS),

    /**
     * Done or not done, with nothing in between — so a log **sets** rather than
     * adds.
     *
     * **Declared and not offered.** It is in §1.3's closed list, but the write
     * path a `SETS` mode needs does not exist: with `currentValue` a sum over
     * entries (§4.6), *set* means something other than *append an entry*, and
     * deciding what it means belongs to #22. Keeping it out of [OFFERED] is what
     * stops a user picking a mode that would do nothing.
     */
    TICK(LoggingRule.SETS),

    /**
     * Filled in by a sync rather than by hand — today, Health Connect.
     *
     * **Derived, not offered:** a goal carrying a `healthSourceKey` *is* in this
     * mode, and one that does not cannot be put into it by choosing, because
     * choosing it would not create a data source. It **adds**, because the sync
     * tops a day up by the difference rather than restating it
     * ([BuildHealthProposalsUseCase][com.idomarhaim.goalpilot.domain.usecase.BuildHealthProposalsUseCase]),
     * and because §4.6 keeps a synced reading correctable by logging beside it.
     */
    AUTO(LoggingRule.ADDS),
    ;

    companion object {
        /**
         * The modes the edit screen may offer.
         *
         * The two implemented write paths, and nothing else. [TICK] has no write
         * path yet and [AUTO] is a property of the goal's data source rather than
         * a choice — both are documented on their own members.
         */
        val OFFERED: List<InputMode> = listOf(NUMBER, BUTTONS)

        /** The stored name back to a member, or `null` for absent/unrecognised. */
        fun fromName(name: String?): InputMode? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/** §1.3's *adds or sets*, as a value so [InputMode] can carry it. */
enum class LoggingRule { ADDS, SETS }
