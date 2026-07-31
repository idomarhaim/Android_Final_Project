package com.idomarhaim.goalpilot.domain.model

/** The signed-in user and their gamification state (spec §6 Core: points/levels). */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val points: Long = 0L,
    /** Short shareable code others use to add this user — see [FriendCode]. */
    val friendCode: String = "",
    val createdAtEpochMillis: Long = 0L,
) {
    val level: Int get() = Leveling.levelForPoints(points)

    /** Fraction (0f..1f) of the way from the current level to the next. */
    val levelProgress: Float get() = Leveling.progressWithinLevel(points)

    val pointsToNextLevel: Long get() = Leveling.pointsToNextLevel(points)
}

/**
 * Level curve for gamification (spec §6 Bonus: "Points accumulation and
 * level-ups"). Level n requires a quadratically-growing amount of points so
 * early levels come fast and later ones feel earned.
 *
 * pointsForLevel(n) = 50 * (n-1) * n  → L1:0, L2:100, L3:300, L4:600, ...
 */
object Leveling {
    private const val FACTOR = 50L

    fun pointsForLevel(level: Int): Long {
        val n = level.coerceAtLeast(1).toLong()
        return FACTOR * (n - 1) * n
    }

    fun levelForPoints(points: Long): Int {
        var level = 1
        while (pointsForLevel(level + 1) <= points) level++
        return level
    }

    fun progressWithinLevel(points: Long): Float {
        val level = levelForPoints(points)
        val floor = pointsForLevel(level)
        val ceil = pointsForLevel(level + 1)
        if (ceil <= floor) return 0f
        return ((points - floor).toFloat() / (ceil - floor).toFloat()).coerceIn(0f, 1f)
    }

    fun pointsToNextLevel(points: Long): Long {
        val level = levelForPoints(points)
        return (pointsForLevel(level + 1) - points).coerceAtLeast(0)
    }
}
