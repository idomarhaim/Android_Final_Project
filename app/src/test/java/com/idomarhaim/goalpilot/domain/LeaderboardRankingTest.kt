package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.LeaderboardEntry
import com.idomarhaim.goalpilot.domain.model.rankedByPoints
import org.junit.Test

/** Domain (unit) tests for leaderboard ordering (spec §7: friends leaderboard). */
class LeaderboardRankingTest {

    private fun entry(uid: String, points: Long) = LeaderboardEntry(uid = uid, points = points)

    @Test
    fun `orders by points descending and stamps 1-based ranks`() {
        val ranked = listOf(entry("b", 50), entry("a", 300), entry("c", 120)).rankedByPoints()

        assertThat(ranked.map { it.uid }).containsExactly("a", "c", "b").inOrder()
        assertThat(ranked.map { it.rank }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `ties break deterministically so rows do not jump between snapshots`() {
        val first = listOf(entry("z", 100), entry("a", 100)).rankedByPoints()
        val second = listOf(entry("a", 100), entry("z", 100)).rankedByPoints()

        assertThat(first.map { it.uid }).isEqualTo(second.map { it.uid })
    }

    @Test
    fun `ranks are relative to the filtered set, not a global position`() {
        // The "Friends" tab passes only friends, so the top friend must be #1 even
        // if they sit far down the global board.
        val ranked = listOf(entry("friend", 20), entry("me", 5)).rankedByPoints()

        assertThat(ranked.first().uid).isEqualTo("friend")
        assertThat(ranked.first().rank).isEqualTo(1)
    }

    @Test
    fun `empty input stays empty`() {
        assertThat(emptyList<LeaderboardEntry>().rankedByPoints()).isEmpty()
    }
}
