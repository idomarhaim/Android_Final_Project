package com.idomarhaim.goalpilot.domain.model

/**
 * How demanding a task is, as a **multiplier on effort** — `docs/PRODUCT_v0.3.md` §1.4
 * ([#55](https://github.com/idomarhaim/Android_Final_Project/issues/55)).
 *
 * ### Why the multipliers live here and not in the prompt
 *
 * §1.4: *"the multipliers live in the **app**, never in the prompt — the model must not be
 * able to move a currency by phrasing."* The model is asked which of these three words fits
 * a task; it is never asked what the word is worth. That is §0.5 — *the AI judges, the app computes* — and it is what makes the point value reproducible across runs and across languages, which
 * `C11a` measured free numbers failing at (2× run-to-run, 1.8× between languages).
 *
 * ### Why an enum and not a number
 *
 * A stored multiplier would be a second place the currency could be re-priced from, and the
 * three constants below would then have to agree with whatever any document happened to
 * carry. A name has no such freedom: re-pricing means editing this file, which is a code
 * review rather than a write.
 *
 * The three names are `LIGHT`, `ROUTINE` and `DEMANDING` because they describe **the work**,
 * not the person doing it — §1.7's one-axis rule. `ROUTINE` is `×1.0` and is the default, so
 * a task nobody has judged is priced exactly on its minutes and today's anchor survives: a
 * 30-minute routine task is still worth 10 points.
 */
enum class Difficulty(val multiplier: Double) {

    /** Less than the minutes suggest — waiting, idling, something half-attended. */
    LIGHT(0.75),

    /** The default, and the anchor: effort is worth exactly what it took. */
    ROUTINE(1.0),

    /** Concentrated or unpleasant work — the minutes under-report it. */
    DEMANDING(1.5);

    companion object {

        /**
         * Unknown, misspelled and absent all read as [ROUTINE].
         *
         * The same shape as `DurationSource.fromName`, and for the same reason: a stored
         * string that no longer parses must resolve to the **neutral** value rather than to
         * an error, because the alternative is a task that cannot be read at all. `ROUTINE`
         * is neutral in the strong sense — it is `×1.0`, so an unreadable difficulty prices
         * the task on its minutes alone, which is what a task with no judgement is worth.
         */
        fun fromName(name: String?): Difficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ROUTINE
    }
}
