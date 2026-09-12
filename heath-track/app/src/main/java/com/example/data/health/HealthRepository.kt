package com.example.data.health

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.data.local.HealthSyncDao
import com.example.data.local.HealthSyncEntity
import com.example.data.local.SleepLogDao
import com.example.data.local.SleepLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

interface HealthRepository {
    fun isAvailable(): Boolean
    fun getAvailabilityStatus(): String
    val isConnected: StateFlow<Boolean>
    fun setConnected(connected: Boolean)
    fun getSyncDataForDate(date: String): Flow<HealthSyncEntity?>
    fun getRecentSyncData(): Flow<List<HealthSyncEntity>>
    suspend fun syncNow(date: String): Boolean
    suspend fun checkPermissionsGranted(): Boolean
    fun getPermissionsToRequest(): Set<String>
    suspend fun saveManualRecord(date: String, steps: Int, distanceMeters: Float, activeCalories: Int, heartRate: Int)
    suspend fun fetchLast7DaysSleepFromHealthConnect(): Map<String, Int>
}

class HealthRepositoryImpl(
    private val context: Context,
    private val healthSyncDao: HealthSyncDao,
    private val sleepLogDao: SleepLogDao? = null
) : HealthRepository {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    override fun getPermissionsToRequest(): Set<String> = permissions

    private fun getClient(): HealthConnectClient? {
        return if (isAvailable()) {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (_: Throwable) {
            // For the cloud preview/streaming emulator where Health Connect isn't pre-installed,
            // we catch the exception and return false, but we allow manual data entry.
            false
        }
    }

    override fun getAvailabilityStatus(): String {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> "Available"
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Update Required"
                else -> "Not Supported on Device"
            }
        } catch (_: Throwable) {
            "Not Available"
        }
    }

    override suspend fun checkPermissionsGranted(): Boolean = withContext(Dispatchers.IO) {
        val client = getClient() ?: return@withContext false
        try {
            val granted = client.permissionController.getGrantedPermissions()
            val hasAnyPermission = granted.isNotEmpty()
            if (hasAnyPermission) {
                _isConnected.value = true
            }
            hasAnyPermission
        } catch (_: Exception) {
            false
        }
    }

    override fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    override fun getSyncDataForDate(date: String): Flow<HealthSyncEntity?> {
        return healthSyncDao.getSyncRecordForDate(date)
    }

    override fun getRecentSyncData(): Flow<List<HealthSyncEntity>> {
        return healthSyncDao.getRecentSyncRecords()
    }

    override suspend fun syncNow(date: String): Boolean = withContext(Dispatchers.IO) {
        val client = getClient()
        if (client == null) {
            return@withContext false
        }

        try {
            val localDate = LocalDate.parse(date)
            val startTime = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            var steps = 0
            try {
                val res = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                steps = res[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
            } catch (_: Exception) { }

            var distance = 0f
            try {
                val res = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                distance = res[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toFloat() ?: 0f
            } catch (_: Exception) { }

            var calories = 0
            try {
                val res = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                calories = res[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt() ?: 0
            } catch (_: Exception) { }

            var avgHr = 0
            try {
                val hrRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = timeRange,
                        pageSize = 50
                    )
                )
                val allSamples = hrRecords.records.flatMap { it.samples }
                if (allSamples.isNotEmpty()) {
                    avgHr = allSamples.map { it.beatsPerMinute }.average().toInt()
                }
            } catch (_: Exception) { }

            try {
                val sleepRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = timeRange
                    )
                )
                if (sleepRecords.records.isNotEmpty()) {
                    val totalMillis = sleepRecords.records.sumOf {
                        java.time.Duration.between(it.startTime, it.endTime).toMillis()
                    }
                    val sleepMins = (totalMillis / (1000 * 60)).toInt()
                    if (sleepMins > 0 && sleepLogDao != null) {
                        val bedStr = sleepRecords.records.first().startTime.atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5)
                        val wakeStr = sleepRecords.records.last().endTime.atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5)
                        sleepLogDao.insertOrUpdateSleepLog(
                            SleepLogEntity(
                                date = date,
                                bedtime = bedStr,
                                wakeTime = wakeStr,
                                durationMinutes = sleepMins,
                                quality = if (sleepMins >= 420) "Restful" else "Moderate",
                                source = "Health Connect"
                            )
                        )
                    }
                }
            } catch (_: Exception) { }

            healthSyncDao.insertOrUpdateSyncRecord(
                HealthSyncEntity(
                    date = date,
                    steps = steps,
                    distanceMeters = distance,
                    activeCalories = calories,
                    heartRateBpm = avgHr,
                    source = "Health Connect",
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
            )
            _isConnected.value = true
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun fetchLast7DaysSleepFromHealthConnect(): Map<String, Int> = withContext(Dispatchers.IO) {
        val client = getClient() ?: return@withContext emptyMap()
        val resultMap = mutableMapOf<String, Int>()
        try {
            val today = LocalDate.now()
            val startDate = today.minusDays(6)
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            val sleepRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = timeRange
                )
            )

            for (record in sleepRecords.records) {
                val recordDate = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val duration = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toInt()
                if (duration > 0) {
                    resultMap[recordDate] = (resultMap[recordDate] ?: 0) + duration
                }
            }
        } catch (_: Exception) { }
        resultMap
    }

    override suspend fun saveManualRecord(
        date: String,
        steps: Int,
        distanceMeters: Float,
        activeCalories: Int,
        heartRate: Int
    ) {
        healthSyncDao.insertOrUpdateSyncRecord(
            HealthSyncEntity(
                date = date,
                steps = steps,
                distanceMeters = distanceMeters,
                activeCalories = activeCalories,
                heartRateBpm = heartRate,
                source = "Manual Input",
                lastSyncedTimestamp = System.currentTimeMillis()
            )
        )
    }
}

