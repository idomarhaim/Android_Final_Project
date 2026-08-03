package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.LifeArea
import kotlinx.coroutines.flow.Flow

/**
 * CRUD + observation for the user's life areas (spec §1: goals belong to areas of
 * life). Areas are the grouping the time-allocation chart reports on, so they are
 * observed as a snapshot flow like goals and tasks — a chart that lags a rename
 * by one screen entry looks broken.
 */
interface LifeAreaRepository {

    fun observeLifeAreas(includeArchived: Boolean = false): Flow<List<LifeArea>>

    /** Creates or updates an area; returns its id. */
    suspend fun upsertLifeArea(area: LifeArea): Resource<String>

    /**
     * Deletes an area and unfiles the goals that pointed at it, so no goal is left
     * holding an id nothing resolves. The chart tolerates a dangling id (it counts
     * as "Unassigned"), but a goal that silently keeps one would re-appear in the
     * wrong slice the moment an unrelated area reused that id.
     */
    suspend fun deleteLifeArea(areaId: String): Resource<Unit>

    /**
     * Points an existing area at a Google Tasks list without touching the name,
     * colour or icon the user chose. Used when the sync finds an area that already
     * carries the list's name — re-creating it would give the user two "בריאות".
     */
    suspend fun linkGoogleList(areaId: String, googleListId: String): Resource<Unit>
}
