package com.idomarhaim.goalpilot.domain

import com.google.common.truth.Truth.assertThat
import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.usecase.ReorderLifeAreasUseCase
import org.junit.Test

/**
 * The arithmetic behind drag-to-reorder. The point of pinning it here is that the
 * gesture itself is untestable on the JVM, while *which documents get written* is
 * the part that costs money and can silently corrupt an order.
 */
class LifeAreaOrderingTest {

    private val reorder = ReorderLifeAreasUseCase()

    /** Areas as the repository emits them: sorted, ascending, gap-free. */
    private fun areas(vararg names: String): List<LifeArea> =
        names.mapIndexed { index, name -> LifeArea(id = name, name = name, sortOrder = index) }

    /** Applies a change map and re-sorts the way `observeLifeAreas` does. */
    private fun applied(areas: List<LifeArea>, changes: Map<String, Int>): List<String> =
        areas.map { it.copy(sortOrder = changes[it.id] ?: it.sortOrder) }
            .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase() }))
            .map { it.id }

    @Test
    fun `moving a card down rewrites only the span it crossed`() {
        val list = areas("a", "b", "c", "d", "e")

        val changes = reorder(list, fromIndex = 0, toIndex = 2)

        // d and e never moved, so they are not written.
        assertThat(changes.keys).containsExactly("a", "b", "c")
        assertThat(applied(list, changes)).containsExactly("b", "c", "a", "d", "e").inOrder()
    }

    @Test
    fun `moving a card up rewrites only the span it crossed`() {
        val list = areas("a", "b", "c", "d", "e")

        val changes = reorder(list, fromIndex = 3, toIndex = 1)

        assertThat(changes.keys).containsExactly("b", "c", "d")
        assertThat(applied(list, changes)).containsExactly("a", "d", "b", "c", "e").inOrder()
    }

    @Test
    fun `a move onto a neighbour writes exactly two documents`() {
        val list = areas("a", "b", "c", "d")

        val changes = reorder(list, fromIndex = 1, toIndex = 2)

        assertThat(changes).hasSize(2)
        assertThat(applied(list, changes)).containsExactly("a", "c", "b", "d").inOrder()
    }

    @Test
    fun `a move that changes nothing writes nothing`() {
        val list = areas("a", "b", "c")

        assertThat(reorder(list, fromIndex = 1, toIndex = 1)).isEmpty()
    }

    @Test
    fun `indices outside the list are refused rather than clamped`() {
        val list = areas("a", "b", "c")

        // A clamp would silently move the card somewhere the user did not drop it.
        assertThat(reorder(list, fromIndex = -1, toIndex = 1)).isEmpty()
        assertThat(reorder(list, fromIndex = 0, toIndex = 3)).isEmpty()
        assertThat(reorder(emptyList(), fromIndex = 0, toIndex = 0)).isEmpty()
    }

    @Test
    fun `sparse sort orders keep their own values instead of being renumbered`() {
        // What the Google Tasks sync leaves behind after a few areas are deleted.
        val list = listOf(
            LifeArea(id = "a", name = "a", sortOrder = 3),
            LifeArea(id = "b", name = "b", sortOrder = 9),
            LifeArea(id = "c", name = "c", sortOrder = 40),
        )

        val changes = reorder(list, fromIndex = 2, toIndex = 0)

        assertThat(changes).containsExactly("c", 3, "a", 9, "b", 40)
        assertThat(applied(list, changes)).containsExactly("c", "a", "b").inOrder()
    }

    @Test
    fun `duplicate sort orders force one full renumber`() {
        // Two devices creating an area at once can leave ties, and rotating values
        // through a tie would land the card wherever the name tie-break decided.
        val list = listOf(
            LifeArea(id = "a", name = "a", sortOrder = 0),
            LifeArea(id = "b", name = "b", sortOrder = 0),
            LifeArea(id = "c", name = "c", sortOrder = 0),
        )

        val changes = reorder(list, fromIndex = 2, toIndex = 0)

        assertThat(applied(list, changes)).containsExactly("c", "a", "b").inOrder()
        assertThat(changes.values.toSet()).hasSize(changes.size)
    }

    @Test
    fun `a renumber still skips documents that are already right`() {
        val list = listOf(
            LifeArea(id = "a", name = "a", sortOrder = 0),
            LifeArea(id = "b", name = "b", sortOrder = 1),
            LifeArea(id = "c", name = "c", sortOrder = 1),
        )

        val changes = reorder(list, fromIndex = 1, toIndex = 2)

        // "a" is already at index 0 and stays there — no write for it.
        assertThat(changes.keys).doesNotContain("a")
        assertThat(applied(list, changes)).containsExactly("a", "c", "b").inOrder()
    }

    @Test
    fun `two areas can be swapped either way round`() {
        val list = areas("a", "b")

        assertThat(applied(list, reorder(list, 0, 1))).containsExactly("b", "a").inOrder()
        assertThat(applied(list, reorder(list, 1, 0))).containsExactly("b", "a").inOrder()
    }
}
