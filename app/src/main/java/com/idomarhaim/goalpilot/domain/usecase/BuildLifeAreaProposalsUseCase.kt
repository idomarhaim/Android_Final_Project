package com.idomarhaim.goalpilot.domain.usecase

import com.idomarhaim.goalpilot.domain.model.LifeArea
import com.idomarhaim.goalpilot.domain.model.LifeAreaPalette
import javax.inject.Inject

/** A Google Tasks list, reduced to what the life-area sync needs. */
data class GoogleTaskList(val id: String, val title: String)

/** What the sync would do with one Google Tasks list, pending the user's tick. */
enum class LifeAreaSyncAction {
    /** No area matches the list — create one. */
    CREATE,

    /** An area already carries this name — point it at the list, keep its colour. */
    LINK,
}

/**
 * One reviewable row of the life-area sync. Nothing is written until the user
 * confirms — the same policy as smart add, the task import and the health sync.
 */
data class LifeAreaProposal(
    val googleListId: String,
    val name: String,
    val action: LifeAreaSyncAction,
    val colorHex: String,
    val iconKey: String,
    /** Set for [LifeAreaSyncAction.LINK]: the area that will be linked. */
    val existingAreaId: String? = null,
    val sortOrder: Int = 0,
    val selected: Boolean = true,
)

/**
 * Turns the user's Google Tasks lists into life-area proposals
 * (spec §6 nice-to-have: Google Tasks integration).
 *
 * The user's Google Tasks lists *are* their declared areas of life — that is the
 * whole point of syncing them rather than asking the user to retype them. This
 * decides three things, and it decides them here rather than in the ViewModel so
 * `LifeAreaSyncTest` can pin them down without Firebase or Google in the room:
 *
 * 1. **Already synced** lists (an area carries their `googleListId`) produce no
 *    row at all — a second sync must not offer to duplicate the first one's work.
 * 2. **Name collisions** produce a LINK, not a CREATE. Someone who typed "Health"
 *    by hand and then syncs a "Health" list wants one area, not two.
 * 3. **New lists** get a colour that is not already crowding the pie and an icon
 *    guessed from the name (bilingual — see [LifeAreaPalette.iconKeyFor]).
 */
class BuildLifeAreaProposalsUseCase @Inject constructor() {

    operator fun invoke(
        lists: List<GoogleTaskList>,
        existingAreas: List<LifeArea>,
    ): List<LifeAreaProposal> {
        val linkedListIds = existingAreas.mapNotNull { it.googleListId?.takeIf(String::isNotBlank) }.toSet()
        val byName = existingAreas.associateBy { it.name.trim().lowercase() }
        // Colours are handed out as we go, so two new lists in the same run never
        // come out the same hue.
        val takenColours = existingAreas.map { it.colorHex }.toMutableList()
        var order = (existingAreas.maxOfOrNull { it.sortOrder } ?: -1) + 1

        return lists
            .asSequence()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .filterNot { it.id in linkedListIds }
            // Google returns lists in the order they appear in the sidebar, but a
            // duplicate title would produce two identical-looking rows.
            .distinctBy { it.title.trim().lowercase() }
            .map { list ->
                val name = list.title.trim()
                val existing = byName[name.lowercase()]
                if (existing != null) {
                    LifeAreaProposal(
                        googleListId = list.id,
                        name = existing.name,
                        action = LifeAreaSyncAction.LINK,
                        colorHex = existing.colorHex,
                        iconKey = existing.iconKey,
                        existingAreaId = existing.id,
                        sortOrder = existing.sortOrder,
                    )
                } else {
                    val colour = LifeAreaPalette.nextHex(takenColours)
                    takenColours += colour
                    LifeAreaProposal(
                        googleListId = list.id,
                        name = name,
                        action = LifeAreaSyncAction.CREATE,
                        colorHex = colour,
                        iconKey = LifeAreaPalette.iconKeyFor(name),
                        sortOrder = order++,
                    )
                }
            }
            .toList()
    }
}
