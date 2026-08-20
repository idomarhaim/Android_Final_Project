package com.idomarhaim.goalpilot.domain.model

import java.time.LocalDateTime

/**
 * §2.5's reminder arithmetic: **when to remind, computed backwards from how long the work
 * takes**, clamped to waking hours, carrying *why it moved*.
 *
 * §2.5 calls this *"the one thing this app knows that Google Calendar does not"*. A calendar
 * reminds you a fixed interval before a deadline because a deadline is all it holds; GoalPilot
 * holds [TaskDuration.minutesOf], so it can remind you at the moment you would have to **start**
 * — which for a four-hour job due at 06:00 is the previous evening, not 05:45.
 *
 * **Pure domain, and deliberately string-free.** It returns a [ReminderPlan] describing the
 * decision; rendering that into a sentence is the notification layer's job, so the arithmetic
 * can be tested without a device, a locale or a resource table (§0.2 — one place computes it,
 * one place words it).
 *
 * ⚠️ **Its scheduled consumer does not exist yet, and that is not an oversight.** §2.5 says
 * *one reminder per occurrence, timed per rung*, and there is **no occurrence model in this
 * codebase**: `Task` carries no due date, §2.2's four rungs (`ALL_DAY`/`DEADLINE`/`BLOCK`/`SPAN`)
 * appear in no Kotlin file, and §2.1's `Occurrence` is unbuilt — that is `C9a` #25. So the
 * arithmetic ships here, tested and ready, and the thing that walks a list of occurrences
 * calling it is #25's to write. Building a scheduler over a model that does not exist would
 * have meant inventing the model in passing, on a ticket nobody reviewed for it.
 */
object ReminderTiming {

    /**
     * When to fire the reminder for something due at [dueAt] that takes [durationMinutes].
     *
     * The **ideal** moment is `dueAt - durationMinutes`: start then and you finish exactly on
     * time. If that moment falls while the user is asleep it is moved **earlier**, to the last
     * waking minute before it — never later. Later is the tempting direction and it is wrong:
     * a reminder that arrives after the moment you needed to start is a notification about a
     * deadline you have already lost, which is §2.3's *"an over-eager agent manufactures
     * failures"* wearing a different hat.
     *
     * [durationMinutes] is expected to come from [TaskDuration.minutesOf], which never returns
     * zero. A non-positive value is treated as zero rather than rejected — a reminder at the
     * deadline itself is a defensible answer, and throwing here would take down a notification
     * path over a number nobody typed.
     */
    fun plan(
        dueAt: LocalDateTime,
        durationMinutes: Int,
        waking: WakingHours,
    ): ReminderPlan {
        val duration = durationMinutes.coerceAtLeast(0)
        val ideal = dueAt.minusMinutes(duration.toLong())
        val moved = lastWakingInstantAtOrBefore(ideal, waking)
        return when {
            // Already inside waking hours, or there is no waking window to clamp into.
            moved == null || moved == ideal ->
                ReminderPlan(dueAt, ideal, ideal, duration, movedForSleep = false)

            else ->
                ReminderPlan(dueAt, ideal, moved, duration, movedForSleep = true)
        }
    }

    /**
     * The latest instant at or before [instant] that the user is awake, or `null` when there
     * is nothing to clamp into.
     *
     * `null` means one of two things, and they collapse deliberately: [instant] is already
     * awake (nothing to move), or [WakingHours.lengthMinutes] is zero. **An empty waking span
     * is honoured by not moving the reminder at all** — the user has said they are awake for
     * zero minutes a day, which cannot be satisfied, and searching for a waking minute inside
     * it would not terminate. Firing at the honest computed time beats not reminding at all.
     */
    private fun lastWakingInstantAtOrBefore(
        instant: LocalDateTime,
        waking: WakingHours,
    ): LocalDateTime? {
        if (waking.lengthMinutes <= 0) return null
        if (waking.containsMinuteOfDay(instant.hour * MINUTES_PER_HOUR + instant.minute)) {
            return instant
        }
        // `endMinutes` is exclusive — see WakingHours.containsMinuteOfDay — so the last minute
        // the user is awake is the one before it. A span ending at 23:00 clamps to 22:59, not
        // to 23:00, because 23:00 is already asleep by the same predicate the rest of this
        // object uses. One definition, applied consistently, beats a boundary that reads
        // rounder and disagrees with the test for it.
        val lastWakingMinuteOfDay = Math.floorMod(waking.endMinutes - 1, MINUTES_PER_DAY)
        val sameDay = instant.toLocalDate()
            .atStartOfDay()
            .plusMinutes(lastWakingMinuteOfDay.toLong())
        return if (sameDay.isAfter(instant)) sameDay.minusDays(1) else sameDay
    }
}

/**
 * The outcome of [ReminderTiming.plan] — the decision, not the sentence.
 *
 * [idealAt] is kept beside [fireAt] on purpose: *why it moved* is not a flag, it is the gap
 * between the two, and a caller that only had the answer could say **that** the reminder moved
 * but never **from where**. §2.5's example sentence quotes both.
 */
data class ReminderPlan(
    val dueAt: LocalDateTime,
    /** `dueAt - durationMinutes`: the moment you would have to start to finish on time. */
    val idealAt: LocalDateTime,
    /** When the reminder actually fires. Equal to [idealAt] unless it was clamped. */
    val fireAt: LocalDateTime,
    val durationMinutes: Int,
    /** Whether [fireAt] was pulled earlier because [idealAt] fell in the user's sleep. */
    val movedForSleep: Boolean,
) {
    /**
     * Whether this reminder's moment has already gone by.
     *
     * Not folded into [ReminderTiming.plan], which takes no clock: the arithmetic is a pure
     * function of a deadline, a duration and a setting, and mixing `now` into it would make
     * every test of it a test of the test's own clock.
     */
    fun isPast(now: LocalDateTime): Boolean = !fireAt.isAfter(now)
}

/**
 * Whether the user is awake at [minuteOfDay], handling a span that wraps past midnight.
 *
 * `end` is **exclusive**, so `07:00 – 23:00` is awake at 22:59 and asleep at 23:00. That
 * choice is what makes [WakingHours.lengthMinutes] and this predicate agree: a span whose
 * start equals its end is empty in both, rather than empty in one and twenty-four hours long
 * in the other.
 */
fun WakingHours.containsMinuteOfDay(minuteOfDay: Int): Boolean {
    if (lengthMinutes <= 0) return false
    return Math.floorMod(minuteOfDay - startMinutes, MINUTES_PER_DAY) < lengthMinutes
}
