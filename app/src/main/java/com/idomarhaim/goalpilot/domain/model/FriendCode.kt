package com.idomarhaim.goalpilot.domain.model

import kotlin.random.Random

/**
 * Short, human-typeable code used to add a friend (spec §7: sharing between
 * users). The raw Firebase uid is 28 characters and unusable in a live demo, so
 * every profile also carries a 6-character code stored on the world-readable
 * `publicProfiles` projection.
 *
 * The alphabet deliberately drops the visually ambiguous `I`, `O`, `0` and `1`
 * so a code read off a friend's screen can be typed without mistakes.
 */
object FriendCode {

    const val LENGTH = 6

    /** Unambiguous uppercase alphanumerics. */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** Generates a fresh random code. Uniqueness is enforced by the caller. */
    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /**
     * Cleans up what the user typed: trims, uppercases, and drops anything not in
     * the alphabet (spaces, dashes, and the `O`/`0`, `I`/`1` look-alikes people
     * commonly mistype are simply discarded rather than silently mismatched).
     */
    fun normalize(raw: String): String =
        raw.trim().uppercase().filter { it in ALPHABET }

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}
