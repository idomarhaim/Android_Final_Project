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
     *
     * It advances nothing else. The goal's
     * [currentValue][com.idomarhaim.goalpilot.domain.model.Goal.currentValue] is a
     * sum over these entries
     * ([DerivedProgress][com.idomarhaim.goalpilot.domain.model.DerivedProgress], #49), so
     * writing the entry *is* recording the progress — there is no counter left to
     * keep in step, and therefore no window in which the two can disagree.
     */
    suspend fun logProgress(entry: ProgressEntry, imageUri: Uri?): Resource<String>
}
