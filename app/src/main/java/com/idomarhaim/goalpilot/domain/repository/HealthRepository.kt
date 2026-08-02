package com.idomarhaim.goalpilot.domain.repository

import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.HealthSnapshot

/**
 * Read-only access to the device's fitness & sleep store (spec §5, §6
 * nice-to-have). GoalPilot never writes back to Health Connect.
 *
 * [requiredPermissions] is a plain `Set<String>` rather than anything from the
 * Health Connect SDK so this interface stays Android-free like the rest of
 * `domain/`. The screen feeds it straight into the SDK's permission contract,
 * which takes the same strings.
 */
interface HealthRepository {

    /** The health read permissions this app needs, all of them. */
    val requiredPermissions: Set<String>

    suspend fun availability(): HealthAvailability

    /** Reads the last [days] local days of steps and sleep, today included. */
    suspend fun readSnapshot(days: Int = DEFAULT_DAYS): Resource<HealthSnapshot>

    companion object {
        /** A week: long enough to show a trend, short enough to review in one dialog. */
        const val DEFAULT_DAYS = 7
    }
}
