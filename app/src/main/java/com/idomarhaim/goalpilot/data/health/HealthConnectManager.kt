package com.idomarhaim.goalpilot.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.domain.model.DailySteps
import com.idomarhaim.goalpilot.domain.model.HealthAvailability
import com.idomarhaim.goalpilot.domain.model.HealthSnapshot
import com.idomarhaim.goalpilot.domain.model.SleepInterval
import com.idomarhaim.goalpilot.domain.model.toSleepNights
import com.idomarhaim.goalpilot.domain.repository.HealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads fitness & sleep from Android Health Connect (spec §5, §6 nice-to-have).
 *
 * Read-only by design — GoalPilot never writes back, so it declares only read
 * permissions and the user is never asked for write access.
 *
 * **Absence is a normal state, not a failure.** Health Connect ships as part of
 * the system on Android 14+ but is a separate app below that, and stock emulator
 * images generally do not carry it at all. Every entry point here degrades to a
 * [HealthAvailability] the UI can explain, rather than throwing: `getOrCreate`
 * raises when no provider is installed, which is precisely the common case.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : HealthRepository {

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    /** Null whenever no usable provider is installed. Built once, lazily. */
    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    override suspend fun availability(): HealthAvailability = withContext(io) {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthAvailability.NOT_SUPPORTED

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthAvailability.PROVIDER_UPDATE_REQUIRED

            else -> {
                val connected = client ?: return@withContext HealthAvailability.NOT_SUPPORTED
                val granted = runCatching {
                    connected.permissionController.getGrantedPermissions()
                }.getOrDefault(emptySet())
                if (granted.containsAll(requiredPermissions)) {
                    HealthAvailability.AVAILABLE
                } else {
                    HealthAvailability.PERMISSIONS_REQUIRED
                }
            }
        }
    }

    override suspend fun readSnapshot(days: Int): Resource<HealthSnapshot> = withContext(io) {
        val connected = client
            ?: return@withContext Resource.Error("Health Connect is not available on this device")

        val zone = ZoneId.systemDefault()
        // `days` counts today, so a 7-day window starts 6 days back.
        val windowStart = LocalDate.now(zone)
            .minusDays((days - 1).coerceAtLeast(0).toLong())
            .atStartOfDay()
        val windowEnd = LocalDateTime.now(zone)
        val range = TimeRangeFilter.between(windowStart, windowEnd)

        try {
            // Steps are aggregated by the provider rather than summed here: Health
            // Connect de-duplicates overlapping records from different apps as part
            // of aggregation, which raw readRecords does not.
            val steps = connected.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = range,
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).mapNotNull { bucket ->
                val count = bucket.result[StepsRecord.COUNT_TOTAL] ?: return@mapNotNull null
                DailySteps(epochDay = bucket.startTime.toLocalDate().toEpochDay(), steps = count)
            }.filter { it.steps > 0 }

            // Sleep is read raw, because a session crossing midnight has to be
            // attributed to the waking day — a decision the aggregator does not make
            // for us. Overlap merging happens in `toSleepNights`, which is testable.
            val sleep = connected.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = range,
                ),
            ).records
                .map { SleepInterval(it.startTime.toEpochMilli(), it.endTime.toEpochMilli()) }
                .toSleepNights(zone)

            Resource.Success(HealthSnapshot(steps = steps, sleep = sleep))
        } catch (e: SecurityException) {
            // Permissions can be revoked from the Health Connect app while GoalPilot
            // is running, so a granted-looking availability check is not a guarantee.
            Resource.Error("GoalPilot is not allowed to read health data yet", e)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Could not read from Health Connect", e)
        }
    }
}
