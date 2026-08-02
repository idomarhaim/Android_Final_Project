package com.idomarhaim.goalpilot.domain.model

import java.time.Instant
import java.time.ZoneId

/**
 * Fitness & sleep figures read from Android Health Connect (spec §5, §6
 * nice-to-have).
 *
 * Deliberately free of every Health Connect and Android type. The data layer maps
 * the SDK's records onto these, which means the rules that decide *how a reading
 * becomes progress* are plain Kotlin and run in a JVM unit test — there is no
 * emulator, no provider app and no granted permission anywhere in that path.
 */

/** Whether this device can serve health data, and whether it currently will. */
enum class HealthAvailability {
    /**
     * No Health Connect provider on the device. This is the normal state on a
     * stock emulator image and on any phone where the user has never installed
     * Health Connect — it is not an error, and the UI says so plainly.
     */
    NOT_SUPPORTED,

    /** A provider exists but is too old to talk to; the user updates it from Play. */
    PROVIDER_UPDATE_REQUIRED,

    /** Provider is ready, but the read permissions have not been granted yet. */
    PERMISSIONS_REQUIRED,

    AVAILABLE,
}

/** Steps walked on one local calendar day. */
data class DailySteps(val epochDay: Long, val steps: Long)

/**
 * One night's sleep, attributed to the local date the user **woke up** on.
 *
 * Sleep almost always crosses midnight, so a session has two candidate dates.
 * Waking day is the conventional choice and the one a person means by "how did I
 * sleep last night" — bucketing by start date would file Friday night's sleep
 * under Friday and leave Saturday looking empty.
 */
data class SleepNight(val epochDay: Long, val minutes: Int) {
    val hours: Double get() = minutes / 60.0
}

/** A raw sleep session as Health Connect reports it, before bucketing into nights. */
data class SleepInterval(val startEpochMillis: Long, val endEpochMillis: Long)

/** Everything one Health Connect read returned. */
data class HealthSnapshot(
    val steps: List<DailySteps> = emptyList(),
    val sleep: List<SleepNight> = emptyList(),
) {
    /** True when there is nothing worth showing the user — no steps and no sleep. */
    val isEmpty: Boolean
        get() = steps.none { it.steps > 0 } && sleep.none { it.minutes > 0 }
}

/**
 * Buckets raw sleep sessions into nights, summing the time actually asleep.
 *
 * Overlapping sessions are merged first. Health Connect aggregates every app on
 * the device, so a phone with both a watch app and a sleep tracker installed
 * reports the same night twice; summing those durations naively would claim
 * sixteen hours of sleep.
 */
fun List<SleepInterval>.toSleepNights(zone: ZoneId = ZoneId.systemDefault()): List<SleepNight> =
    filter { it.endEpochMillis > it.startEpochMillis }
        .mergeOverlapping()
        .groupBy { interval ->
            Instant.ofEpochMilli(interval.endEpochMillis).atZone(zone).toLocalDate().toEpochDay()
        }
        .map { (epochDay, sessions) ->
            val millis = sessions.sumOf { it.endEpochMillis - it.startEpochMillis }
            SleepNight(epochDay = epochDay, minutes = (millis / 60_000L).toInt())
        }
        .filter { it.minutes > 0 }
        .sortedBy { it.epochDay }

/** Collapses overlapping or touching intervals into the union of their ranges. */
private fun List<SleepInterval>.mergeOverlapping(): List<SleepInterval> {
    if (size < 2) return this
    val sorted = sortedBy { it.startEpochMillis }
    val merged = mutableListOf(sorted.first())
    for (next in sorted.drop(1)) {
        val last = merged.last()
        if (next.startEpochMillis <= last.endEpochMillis) {
            merged[merged.lastIndex] =
                last.copy(endEpochMillis = maxOf(last.endEpochMillis, next.endEpochMillis))
        } else {
            merged += next
        }
    }
    return merged
}
