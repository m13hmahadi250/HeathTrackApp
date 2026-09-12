package com.example.salat

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.CachedPrayerTimesEntity
import com.example.data.local.SalatConfigEntity
import com.example.data.local.SalatDao
import com.example.data.local.VitaFlowDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for Salat / Prayer times with Room persistent local storage.
 * Ensures prayer times are fetched from local Room database first and
 * refreshed only when a network connection is available.
 */
class PrayerScheduleRepository(
    private val context: Context,
    private val salatDao: SalatDao = VitaFlowDatabase.getInstance(context).salatDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "PrayerScheduleRepo"

        @Volatile
        private var INSTANCE: PrayerScheduleRepository? = null

        fun getInstance(context: Context): PrayerScheduleRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrayerScheduleRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Checks if a network connection is currently available and validated.
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns a reactive Flow of the prayer schedule for the given date.
     * Observes the local Room database directly.
     */
    fun getPrayerScheduleFlow(date: LocalDate, config: SalatConfigEntity): Flow<SalatSchedule> {
        val dateStr = date.toString()
        return salatDao.getCachedPrayerTimes(dateStr)
            .map { cached ->
                if (cached != null && isCacheValidForConfig(cached, config)) {
                    cached.toSalatSchedule(isOffline = false)
                } else {
                    // Fallback to local calculation immediately
                    val calculated = PrayerTimeCalculator.calculateSchedule(
                        date = date,
                        latitude = config.latitude,
                        longitude = config.longitude,
                        timeZoneId = config.timeZoneId,
                        method = config.calculationMethod,
                        madhab = config.madhab,
                        locationName = config.locationName
                    )
                    // Asynchronously save to Room cache
                    cacheScheduleInRoom(calculated, config)
                    calculated
                }
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Fetches the prayer schedule for a specific date with local-first strategy:
     * 1. Query Room local database first.
     * 2. If present and valid, return immediately (fast & offline).
     * 3. If missing or network refresh requested and network is available, refresh and update Room.
     * 4. If offline, compute using astronomical formulas and save into Room.
     */
    suspend fun getPrayerSchedule(
        date: LocalDate,
        config: SalatConfigEntity,
        forceNetworkRefresh: Boolean = false
    ): SalatSchedule = withContext(ioDispatcher) {
        val dateStr = date.toString()

        // 1. Fetch from Room local storage first
        val cached = salatDao.getCachedPrayerTimesOnce(dateStr)
        if (cached != null && !forceNetworkRefresh && isCacheValidForConfig(cached, config)) {
            Log.d(TAG, "Loaded prayer schedule for $dateStr from Room database cache.")
            return@withContext cached.toSalatSchedule(isOffline = false)
        }

        // 2. If network is available and refresh needed, try online fetch
        if (isNetworkAvailable() && (forceNetworkRefresh || cached == null || !isCacheValidForConfig(cached, config))) {
            val onlineSchedule = fetchFromNetwork(date, config)
            if (onlineSchedule != null) {
                Log.i(TAG, "Fetched prayer schedule for $dateStr from network and cached to Room.")
                salatDao.insertCachedPrayerTimes(onlineSchedule.toEntity(config))
                return@withContext onlineSchedule
            }
        }

        // 3. Offline calculation fallback and persist to Room
        Log.d(TAG, "Calculating prayer schedule for $dateStr offline and saving to Room.")
        val calculated = PrayerTimeCalculator.calculateSchedule(
            date = date,
            latitude = config.latitude,
            longitude = config.longitude,
            timeZoneId = config.timeZoneId,
            method = config.calculationMethod,
            madhab = config.madhab,
            locationName = config.locationName
        )
        salatDao.insertCachedPrayerTimes(calculated.toEntity(config))
        calculated
    }

    /**
     * Preloads and caches in Room a range of days (e.g. next 7 days or current month).
     */
    suspend fun preloadRange(startDate: LocalDate, daysCount: Int, config: SalatConfigEntity) = withContext(ioDispatcher) {
        val list = mutableListOf<CachedPrayerTimesEntity>()
        for (i in 0 until daysCount) {
            val d = startDate.plusDays(i.toLong())
            val existing = salatDao.getCachedPrayerTimesOnce(d.toString())
            if (existing == null || !isCacheValidForConfig(existing, config)) {
                val sched = PrayerTimeCalculator.calculateSchedule(
                    date = d,
                    latitude = config.latitude,
                    longitude = config.longitude,
                    timeZoneId = config.timeZoneId,
                    method = config.calculationMethod,
                    madhab = config.madhab,
                    locationName = config.locationName
                )
                list.add(sched.toEntity(config))
            }
        }
        if (list.isNotEmpty()) {
            salatDao.insertAllCachedPrayerTimes(list)
            Log.d(TAG, "Pre-cached ${list.size} days of prayer schedules in Room database.")
        }
    }

    private suspend fun cacheScheduleInRoom(schedule: SalatSchedule, config: SalatConfigEntity) {
        try {
            salatDao.insertCachedPrayerTimes(schedule.toEntity(config))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache prayer schedule in Room: ${e.message}")
        }
    }

    private fun isCacheValidForConfig(cached: CachedPrayerTimesEntity, config: SalatConfigEntity): Boolean {
        return cached.method.equals(config.calculationMethod, ignoreCase = true) &&
                cached.madhab.equals(config.madhab, ignoreCase = true) &&
                cached.locationName.equals(config.locationName, ignoreCase = true)
    }

    /**
     * Optional network fetch from Aladhan API when connection is active.
     */
    private fun fetchFromNetwork(date: LocalDate, config: SalatConfigEntity): SalatSchedule? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val dateFormatted = date.format(formatter)
            val methodId = when (config.calculationMethod.lowercase()) {
                "karachi" -> 1
                "isna" -> 2
                "mwl" -> 3
                "makkah" -> 4
                "egyptian" -> 5
                "tehran" -> 7
                "moonsighting" -> 15
                else -> 1
            }
            val schoolId = if (config.madhab.equals("Hanafi", ignoreCase = true)) 1 else 0

            val urlString = "https://api.aladhan.com/v1/timings/$dateFormatted" +
                    "?latitude=${config.latitude}&longitude=${config.longitude}" +
                    "&method=$methodId&school=$schoolId"

            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.optInt("code") == 200) {
                    val timings = json.getJSONObject("data").getJSONObject("timings")
                    val fajr = cleanTimeString(timings.getString("Fajr"))
                    val sunrise = cleanTimeString(timings.getString("Sunrise"))
                    val dhuhr = cleanTimeString(timings.getString("Dhuhr"))
                    val asr = cleanTimeString(timings.getString("Asr"))
                    val maghrib = cleanTimeString(timings.getString("Maghrib"))
                    val isha = cleanTimeString(timings.getString("Isha"))

                    return SalatSchedule(
                        date = date.toString(),
                        fajr = fajr,
                        sunrise = sunrise,
                        dhuhr = dhuhr,
                        asr = asr,
                        maghrib = maghrib,
                        isha = isha,
                        locationName = config.locationName,
                        method = config.calculationMethod,
                        madhab = config.madhab,
                        timeZoneId = config.timeZoneId,
                        isOfflineCalculated = false
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "Network fetch skipped or timed out (${e.message}), using offline calculator.")
            null
        }
    }

    private fun cleanTimeString(raw: String): String {
        // e.g. "05:12 (BST)" -> "05:12"
        return raw.substringBefore(" ").trim().take(5)
    }

    private fun CachedPrayerTimesEntity.toSalatSchedule(isOffline: Boolean): SalatSchedule {
        return SalatSchedule(
            date = date,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            locationName = locationName,
            method = method,
            madhab = madhab,
            timeZoneId = timeZoneId,
            isOfflineCalculated = isOffline
        )
    }

    private fun SalatSchedule.toEntity(config: SalatConfigEntity): CachedPrayerTimesEntity {
        return CachedPrayerTimesEntity(
            date = date,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            locationName = config.locationName,
            method = config.calculationMethod,
            madhab = config.madhab,
            timeZoneId = config.timeZoneId,
            calculationTimestamp = System.currentTimeMillis()
        )
    }
}
