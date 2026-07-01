package com.idomarhaim.goalpilot.data.health

import com.idomarhaim.goalpilot.core.result.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NICE-TO-HAVE (spec §5, §6): pull fitness & sleep from Android Health Connect.
 *
 * This is a compiling stub so the rest of the app can depend on it today. It is
 * intentionally free of the (currently alpha) Health Connect client library to
 * keep the Core build rock-solid.
 *
 * TO ACTIVATE:
 *  1. Add to libs.versions.toml + app/build.gradle.kts:
 *       androidx.health.connect:connect-client:<latest stable/rc>
 *  2. Declare the health permissions + the Health Connect <queries> entry and a
 *     PermissionsRationaleActivity in AndroidManifest.xml.
 *  3. Replace the bodies below with real HealthConnectClient reads, e.g.
 *       client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange))
 *  4. Feed the results into ProgressRepository / goals (e.g. a "Steps" goal).
 *
 * See TODO/TODO_OPTIONAL/Integrations.TODO.optional.md.
 */
@Singleton
class HealthConnectManager @Inject constructor() {

    fun status(): HealthConnectStatus = HealthConnectStatus.NOT_INTEGRATED

    suspend fun readDailySteps(): Resource<Int> =
        Resource.Error("Health Connect is not enabled in this build (nice-to-have).")

    suspend fun readLastNightSleepMinutes(): Resource<Int> =
        Resource.Error("Health Connect is not enabled in this build (nice-to-have).")
}

enum class HealthConnectStatus {
    NOT_INTEGRATED,
    NOT_SUPPORTED,
    NOT_INSTALLED,
    PERMISSIONS_REQUIRED,
    AVAILABLE,
}
