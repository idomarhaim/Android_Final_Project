package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.LifeArea
import javax.inject.Inject

/**
 * Works out the **smallest** set of `sortOrder` writes that turns the displayed
 * order into the one the user just dragged into.
 *
 * It lives here rather than in the ViewModel for the usual reason — a pure
 * function over a list is the only part of drag-to-reorder a JVM test can pin
 * down (`LifeAreaOrderingTest`), and it is the part that is easy to get wrong.
 *
 * Two decisions it makes:
 *
 * 1. **Slots, not renumbering.** Moving one card must not rewrite every document
 *    in the collection. The positions outside the moved span keep the areas they
 *    already had, so only the span between the old and new index changes hands —
 *    the `sortOrder` *values* stay put and their owners rotate through them.
 * 2. **Unless the values cannot carry an order.** Slot reuse only works while the
 *    existing values are strictly increasing. Duplicates mean the list is really
 *    ordered by the `(sortOrder, name)` tie-break the repository applies, and
 *    rotating values through a tie would silently land the card somewhere the
 *    user did not drop it. Then, and only then, this renumbers the whole list
 *    0..n-1 — one costly write that makes every later drag cheap again.
 */
class ReorderLifeAreasUseCase @Inject constructor() {

    /**
     * @param areas the areas **in display order** (what `observeLifeAreas` emits).
     * @return `id → new sortOrder`, holding only the areas that must actually be
     *   written. Empty when the move changes nothing, or the indices are bogus.
     */
    operator fun invoke(areas: List<LifeArea>, fromIndex: Int, toIndex: Int): Map<String, Int> {
        if (fromIndex == toIndex) return emptyMap()
        if (fromIndex !in areas.indices || toIndex !in areas.indices) return emptyMap()

        val moved = areas.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        val slots = areas.map { it.sortOrder }
        val targets = if (slots.zipWithNext().all { (a, b) -> a < b }) slots else areas.indices.toList()

        return moved
            .withIndex()
            .mapNotNull { (index, area) ->
                if (area.sortOrder == targets[index]) null else area.id to targets[index]
            }
            .toMap()
    }
}
