package com.idomarhaim.goalpilot.data.firestore

import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SnapshotMetadata
import com.idomarhaim.goalpilot.domain.model.Freshness
import com.idomarhaim.goalpilot.domain.model.merge
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.Date

/**
 * The two facts #50 reads off a **cross-boundary** snapshot, and nothing else.
 *
 * This is the layer the discriminator actually lives in, and the only one where
 * `metadata.isFromCache` exists at all — a ViewModel test can assert that a
 * never-loaded flag reaches the screen, but only this can assert what sets it.
 *
 * `snapshot.metadata.isFromCache && snapshot.isEmpty` had **0 usages** in the app
 * before #50; both halves are pinned here because either one alone is wrong in a
 * way that reads as working.
 */
class CrossBoundaryFreshnessTest {

    private fun doc(updatedAt: Long?): DocumentSnapshot = mockk {
        every { getTimestamp(UPDATED_AT) } returns updatedAt?.let { Timestamp(Date(it)) }
    }

    private fun snapshot(
        fromCache: Boolean,
        docs: List<DocumentSnapshot> = emptyList(),
    ): QuerySnapshot = mockk {
        every { documents } returns docs
        every { metadata } returns mockk<SnapshotMetadata> {
            every { isFromCache } returns fromCache
        }
    }

    @Test
    fun `empty and from cache is never-loaded`() {
        // The whole of item 3: a first-run-offline Social tab must not render
        // "you have no friends" about data it has never read.
        assertThat(snapshot(fromCache = true).crossBoundaryFreshness().neverLoaded).isTrue()
    }

    @Test
    fun `empty from the server is a genuine empty, not never-loaded`() {
        // Reaching the server and finding nobody is a fact the app may state.
        assertThat(snapshot(fromCache = false).crossBoundaryFreshness().neverLoaded).isFalse()
    }

    @Test
    fun `non-empty from cache is ordinary offline reading`() {
        // `isFromCache` alone is not the discriminator, and this is why: serving
        // rows we have seen before, with the radio off, is exactly the case #50
        // argues must NOT get a banner or "cached" styling.
        val read = snapshot(fromCache = true, docs = listOf(doc(1_000L))).crossBoundaryFreshness()

        assertThat(read.neverLoaded).isFalse()
    }

    @Test
    fun `the stamp is the newest row, because that is what as-of claims`() {
        // "As of T" says *nothing here reflects a write after T*, so it is a max.
        // A min would report the list as older than it is; picking any single row
        // would report it as whatever that row happened to be.
        val read = snapshot(
            fromCache = false,
            docs = listOf(doc(1_000L), doc(9_000L), doc(5_000L)),
        ).crossBoundaryFreshness()

        assertThat(read.asOfEpochMillis).isEqualTo(9_000L)
    }

    @Test
    fun `rows written before the field existed contribute nothing rather than zero`() {
        // A pre-#50 row has no `updatedAt`, and a pending serverTimestamp reads as
        // null too. Treating either as 0L would be harmless in a max and wrong in
        // any other reduction — they are absent, not ancient.
        val read = snapshot(
            fromCache = false,
            docs = listOf(doc(null), doc(4_000L), doc(null)),
        ).crossBoundaryFreshness()

        assertThat(read.asOfEpochMillis).isEqualTo(4_000L)
    }

    @Test
    fun `a list where no row carries a stamp has no caption to draw`() {
        val read = snapshot(fromCache = false, docs = listOf(doc(null))).crossBoundaryFreshness()

        assertThat(read.hasStamp).isFalse()
    }

    // ── merging the chunked friends listeners ───────────────────────────

    @Test
    fun `one chunk that reached the server clears never-loaded for the whole list`() {
        // The 30-id `whereIn` cap splits the friends leaderboard across listeners.
        // That split is an implementation detail: if any chunk reached the server,
        // this device has seen the data and the list is not never-loaded.
        val merged = listOf(
            Freshness(neverLoaded = true),
            Freshness(asOfEpochMillis = 7L, neverLoaded = false),
        ).merge()

        assertThat(merged.neverLoaded).isFalse()
        assertThat(merged.asOfEpochMillis).isEqualTo(7L)
    }

    @Test
    fun `every chunk empty from cache is never-loaded`() {
        val merged = listOf(Freshness(neverLoaded = true), Freshness(neverLoaded = true)).merge()

        assertThat(merged.neverLoaded).isTrue()
    }

    @Test
    fun `no listeners at all is an ordinary empty`() {
        // No friends means no chunks. That is owner-side data the device genuinely
        // holds — "you have no friends yet" is true, and must not become
        // "Not loaded yet".
        assertThat(emptyList<Freshness>().merge().neverLoaded).isFalse()
    }
}
