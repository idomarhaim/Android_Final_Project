package com.idomarhaim.goalpilot.feature.challenges

import com.idomarhaim.goalpilot.core.util.DateTimeUtils
import java.time.Instant
import java.time.ZoneOffset

/**
 * Turning what the date picker returns into what [com.idomarhaim.goalpilot.domain.model.phaseAt]
 * expects.
 *
 * Kept out of the Compose file deliberately: both are pure, and both encode an
 * off-by-one that is only visible once — so they are worth asserting rather than
 * eyeballing on a screen.
 */

/**
 * The Material date picker reports **UTC midnight** for the day that was tapped.
 * Read back in any zone west of Greenwich that instant is the previous calendar
 * day, so the day is extracted in UTC and re-anchored to local midnight rather
 * than stored as-is.
 */
fun localStartOfPickedDay(utcMillis: Long): Long =
    DateTimeUtils.startOfDay(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())

/**
 * The end bound is **exclusive** — `phaseAt` ends a challenge once `now >= endAt`
 * — so a challenge that should run *through* the chosen day ends at the following
 * local midnight. Storing the chosen day's own midnight would end a
 * one-day challenge before it began.
 */
fun exclusiveEndOfPickedDay(utcMillis: Long): Long =
    DateTimeUtils.startOfDay(
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().plusDays(1),
    )
