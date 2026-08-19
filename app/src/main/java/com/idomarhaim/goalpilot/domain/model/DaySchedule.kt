package com.idomarhaim.goalpilot.domain.model

import java.time.LocalTime

/**
 * Spec §4.9's **Your day** — the two settings three other tickets assumed
 * somebody owned.
 *
 * One type rather than two independent preferences, because the second is a
 * function of the first: §4.9 defaults *Plan tomorrow at* to **one hour before
 * waking hours end**, *"derived, so it is sane untouched and moves when waking
 * hours move"*. Two loose fields cannot express "moves when they move" — they
 * can only be written to agree, which is §0.3's *second number that quietly
 * disagrees* with the disagreement pre-installed.
 *
 * So the override is [planningOverrideMinutes], nullable, and *unset* is a
 * state the type can be in rather than a value someone has to keep refreshing.
 * [planningFollowsWaking] is what the screen's consequence line reads, and it is
 * why that line can tell the truth in both directions.
 *
 * Pure domain. Everything is minutes-since-midnight, `0..1439`.
 */
data class DaySchedule(
    val waking: WakingHours = WakingHours.DEFAULT,
    /** `null` = follow [waking], which is the default and the sane untouched state. */
    val planningOverrideMinutes: Int? = null,
) {

    /** Whether *Plan tomorrow at* is still derived, or has been pinned by hand. */
    val planningFollowsWaking: Boolean get() = planningOverrideMinutes == null

    /** The time the nightly planning prompt fires — derived unless overridden. */
    val planningMinutes: Int get() = planningOverrideMinutes ?: waking.derivedPlanningMinutes

    val planningTime: LocalTime get() = minutesToTime(planningMinutes)

    companion object {
        val DEFAULT: DaySchedule = DaySchedule()
    }
}

/**
 * The span the user is awake — §4.9's **Awake between**, defaulting to
 * **07:00 – 23:00**.
 *
 * Two consumers, neither of them this screen's to wire (`#8`'s scheduled half
 * owns both): it is the **clamp** on §2.5's backwards-computed reminder — a
 * reminder is never moved outside it — and the **denominator** of `C9b`'s load
 * bar.
 *
 * **A span may wrap past midnight** (`22:00 – 06:00`), and that is not an edge
 * case worth forbidding: a night-shift user's waking hours are exactly that
 * shape, and a clamp that rejects them silently clamps their reminders into the
 * hours they are asleep. [lengthMinutes] does the modular arithmetic once, here,
 * so no consumer has to remember to.
 */
data class WakingHours(
    val startMinutes: Int,
    val endMinutes: Int,
) {

    val start: LocalTime get() = minutesToTime(startMinutes)

    val end: LocalTime get() = minutesToTime(endMinutes)

    /**
     * How long the user is awake. Wraps past midnight; a start equal to an end
     * reads as **zero**, not twenty-four hours — an empty span is what someone
     * setting both handles to the same time has actually said, and a consumer
     * dividing by it should see zero rather than a silently plausible 1440.
     */
    val lengthMinutes: Int
        get() = Math.floorMod(endMinutes - startMinutes, MINUTES_PER_DAY)

    /**
     * §4.9's derived *Plan tomorrow at*: **one hour before waking hours end**.
     * Wraps, so a span ending at 00:30 plans at 23:30 rather than at −30.
     */
    val derivedPlanningMinutes: Int
        get() = Math.floorMod(endMinutes - MINUTES_PER_HOUR, MINUTES_PER_DAY)

    /**
     * Where `C9b`'s load bar reddens, in minutes of planned work.
     *
     * `Inferred:` from the single pair §4.9 states — *"07:00 – 23:00 … a 16 h
     * day, so `C9b`'s bar reddens at 12 h"*. One pair fixes a fraction only if
     * the relationship is proportional, and ¾ is the fraction that produces it.
     * `Untested:` nothing consumes this yet — `#8`'s scheduled half is the load
     * bar's owner and will be the first reader.
     *
     * It lives **here** rather than being re-derived at the bar, for §0.2's
     * reason: two places computing "three quarters of the waking day" is one
     * refactor away from being two different numbers, and the bar is on a
     * screen nobody is looking at while they move this setting.
     */
    val loadBarRedMinutes: Int
        get() = (lengthMinutes * LOAD_BAR_RED_NUMERATOR) / LOAD_BAR_RED_DENOMINATOR

    companion object {
        /** §4.9's defaults table: a 16 h day. */
        val DEFAULT: WakingHours = WakingHours(startMinutes = 7 * 60, endMinutes = 23 * 60)

        private const val LOAD_BAR_RED_NUMERATOR = 3
        private const val LOAD_BAR_RED_DENOMINATOR = 4
    }
}

const val MINUTES_PER_DAY: Int = 24 * 60
const val MINUTES_PER_HOUR: Int = 60

/**
 * Minutes-since-midnight as a wall-clock time, tolerant of anything stored.
 *
 * Clamped by `floorMod` rather than validated: the caller is a preference read,
 * and a corrupt value should land the user on a wrong-but-usable time they can
 * see and fix, not on an exception before the first frame.
 */
fun minutesToTime(minutes: Int): LocalTime =
    LocalTime.of(
        Math.floorMod(minutes, MINUTES_PER_DAY) / MINUTES_PER_HOUR,
        Math.floorMod(minutes, MINUTES_PER_HOUR),
    )
