package com.idomarhaim.goalpilot.domain.repository

import android.net.Uri
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.ProgressEntry
import kotlinx.coroutines.flow.Flow

/** Manual progress logs with optional image upload (spec §6 Core). */
interface ProgressRepository {

    fun observeEntries(goalId: String): Flow<List<ProgressEntry>>

    /**
     * Logs a progress entry against a goal. If [imageUri] is provided it is
     * uploaded to Firebase Storage first and the resulting URL stored on the entry.
     * Also advances the goal's [com.idomarhaim.goalpilot.domain.model.Goal.currentValue].
     */
    suspend fun logProgress(entry: ProgressEntry, imageUri: Uri?): Resource<String>
}
