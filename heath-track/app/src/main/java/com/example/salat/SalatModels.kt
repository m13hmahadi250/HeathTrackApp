package com.example.salat

enum class PrayerName(val displayName: String, val arabicName: String, val isObligatory: Boolean = true) {
    FAJR("Fajr", "الفجر", true),
    SUNRISE("Sunrise", "الشروق", false),
    DHUHR("Dhuhr", "الظهر", true),
    ASR("Asr", "العصر", true),
    MAGHRIB("Maghrib", "المغرب", true),
    ISHA("Isha", "العشاء", true)
}

data class SalatSchedule(
    val date: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val locationName: String,
    val method: String,
    val madhab: String,
    val timeZoneId: String,
    val isOfflineCalculated: Boolean = true
) {
    fun getTimeForPrayer(prayer: PrayerName): String = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.SUNRISE -> sunrise
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }
}

data class PrayerStatus(
    val currentPrayer: PrayerName,
    val nextPrayer: PrayerName,
    val nextPrayerTime: String,
    val timeRemainingNextMs: Long,
    val timeRemainingNextFormatted: String,
    val currentPrayerEndsInMs: Long,
    val currentPrayerEndsInFormatted: String,
    val progress: Float
)

data class SalatDaySummary(
    val date: String,
    val dayOfWeek: String,
    val completedPrayers: Set<String>,
    val totalObligatory: Int = 5,
    val isToday: Boolean = false
) {
    val completedCount: Int get() = completedPrayers.size
    val isAllCompleted: Boolean get() = completedCount >= totalObligatory
}

data class CityLocation(
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
) {
    val displayName: String get() = "$city, $country"
}

object PredefinedCities {
    val POPULAR_CITIES = listOf(
        CityLocation("Chattogram", "Bangladesh", 22.3569, 91.7832, "Asia/Dhaka"),
        CityLocation("Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka"),
        CityLocation("Sylhet", "Bangladesh", 24.8949, 91.8687, "Asia/Dhaka"),
        CityLocation("Cox's Bazar", "Bangladesh", 21.4272, 92.0058, "Asia/Dhaka"),
        CityLocation("Rajshahi", "Bangladesh", 24.3745, 88.6042, "Asia/Dhaka"),
        CityLocation("Khulna", "Bangladesh", 22.8456, 89.5403, "Asia/Dhaka"),
        CityLocation("Barishal", "Bangladesh", 22.7010, 90.3535, "Asia/Dhaka"),
        CityLocation("Rangpur", "Bangladesh", 25.7439, 89.2752, "Asia/Dhaka"),
        CityLocation("Mymensingh", "Bangladesh", 24.7471, 90.4203, "Asia/Dhaka"),
        CityLocation("Cumilla", "Bangladesh", 23.4682, 91.1788, "Asia/Dhaka"),
        CityLocation("Bogura", "Bangladesh", 24.8481, 89.3730, "Asia/Dhaka"),
        CityLocation("Narayanganj", "Bangladesh", 23.6238, 90.5000, "Asia/Dhaka"),
        CityLocation("Gazipur", "Bangladesh", 23.9999, 90.4203, "Asia/Dhaka"),
        CityLocation("Dinajpur", "Bangladesh", 25.6217, 88.6354, "Asia/Dhaka"),
        CityLocation("Feni", "Bangladesh", 23.0159, 91.3976, "Asia/Dhaka"),
        CityLocation("Jashore", "Bangladesh", 23.1664, 89.2081, "Asia/Dhaka"),
        CityLocation("Kushtia", "Bangladesh", 23.9013, 89.1205, "Asia/Dhaka"),
        CityLocation("Pabna", "Bangladesh", 24.0129, 89.2486, "Asia/Dhaka"),
        CityLocation("Brahmanbaria", "Bangladesh", 23.9608, 91.1115, "Asia/Dhaka"),
        CityLocation("Tangail", "Bangladesh", 24.2513, 89.9167, "Asia/Dhaka"),
        CityLocation("Noakhali", "Bangladesh", 22.8696, 91.0994, "Asia/Dhaka"),
        CityLocation("Chandpur", "Bangladesh", 23.2333, 90.6667, "Asia/Dhaka"),
        CityLocation("Narsingdi", "Bangladesh", 23.9322, 90.7154, "Asia/Dhaka"),
        CityLocation("Faridpur", "Bangladesh", 23.6071, 89.8429, "Asia/Dhaka"),
        CityLocation("Jamalpur", "Bangladesh", 24.9375, 89.9378, "Asia/Dhaka"),
        CityLocation("Sirajganj", "Bangladesh", 24.4534, 89.7008, "Asia/Dhaka"),
        CityLocation("Natore", "Bangladesh", 24.4206, 88.9324, "Asia/Dhaka"),
        CityLocation("Naogaon", "Bangladesh", 24.7936, 88.9318, "Asia/Dhaka"),
        CityLocation("Joypurhat", "Bangladesh", 25.1015, 89.0270, "Asia/Dhaka"),
        CityLocation("Chapainawabganj", "Bangladesh", 24.5965, 88.2775, "Asia/Dhaka"),
        CityLocation("Kurigram", "Bangladesh", 25.8054, 89.6362, "Asia/Dhaka"),
        CityLocation("Lalmonirhat", "Bangladesh", 25.9923, 89.2847, "Asia/Dhaka"),
        CityLocation("Nilphamari", "Bangladesh", 25.9318, 88.8560, "Asia/Dhaka"),
        CityLocation("Gaibandha", "Bangladesh", 25.3288, 89.5281, "Asia/Dhaka"),
        CityLocation("Thakurgaon", "Bangladesh", 26.0337, 88.4617, "Asia/Dhaka"),
        CityLocation("Panchagarh", "Bangladesh", 26.3411, 88.5542, "Asia/Dhaka"),
        CityLocation("Patuakhali", "Bangladesh", 22.3596, 90.3299, "Asia/Dhaka"),
        CityLocation("Bhola", "Bangladesh", 22.6859, 90.6481, "Asia/Dhaka"),
        CityLocation("Pirojpur", "Bangladesh", 22.5841, 89.9720, "Asia/Dhaka"),
        CityLocation("Jhalokathi", "Bangladesh", 22.6406, 90.1987, "Asia/Dhaka"),
        CityLocation("Barguna", "Bangladesh", 22.0953, 90.1121, "Asia/Dhaka"),
        CityLocation("Bagerhat", "Bangladesh", 22.6516, 89.7859, "Asia/Dhaka"),
        CityLocation("Satkhira", "Bangladesh", 22.7185, 89.0705, "Asia/Dhaka"),
        CityLocation("Chuadanga", "Bangladesh", 23.6402, 88.8418, "Asia/Dhaka"),
        CityLocation("Meherpur", "Bangladesh", 23.7622, 88.6318, "Asia/Dhaka"),
        CityLocation("Jhenaidah", "Bangladesh", 23.5450, 89.1726, "Asia/Dhaka"),
        CityLocation("Magura", "Bangladesh", 23.4873, 89.4199, "Asia/Dhaka"),
        CityLocation("Narail", "Bangladesh", 23.1725, 89.5127, "Asia/Dhaka"),
        CityLocation("Gopalganj", "Bangladesh", 23.0051, 89.8266, "Asia/Dhaka"),
        CityLocation("Madaripur", "Bangladesh", 23.1641, 90.1897, "Asia/Dhaka"),
        CityLocation("Shariatpur", "Bangladesh", 23.2423, 90.4348, "Asia/Dhaka"),
        CityLocation("Rajbari", "Bangladesh", 23.7574, 89.6445, "Asia/Dhaka"),
        CityLocation("Manikganj", "Bangladesh", 23.8617, 90.0003, "Asia/Dhaka"),
        CityLocation("Munshiganj", "Bangladesh", 23.5422, 90.5305, "Asia/Dhaka"),
        CityLocation("Kishoreganj", "Bangladesh", 24.4449, 90.7766, "Asia/Dhaka"),
        CityLocation("Netrokona", "Bangladesh", 24.8709, 90.7279, "Asia/Dhaka"),
        CityLocation("Sherpur", "Bangladesh", 25.0205, 90.0153, "Asia/Dhaka"),
        CityLocation("Sunamganj", "Bangladesh", 25.0658, 91.3950, "Asia/Dhaka"),
        CityLocation("Habiganj", "Bangladesh", 24.3749, 91.4155, "Asia/Dhaka"),
        CityLocation("Moulvibazar", "Bangladesh", 24.4829, 91.7774, "Asia/Dhaka"),
        CityLocation("Bandarban", "Bangladesh", 22.1953, 92.2184, "Asia/Dhaka"),
        CityLocation("Rangamati", "Bangladesh", 22.7324, 92.2985, "Asia/Dhaka"),
        CityLocation("Khagrachhari", "Bangladesh", 23.1193, 91.9847, "Asia/Dhaka"),
        CityLocation("Lakshmipur", "Bangladesh", 22.9425, 90.8412, "Asia/Dhaka")
    )
}

object CalculationMethods {
    const val KARACHI = "University of Islamic Sciences, Karachi"
    const val MWL = "Muslim World League"
    const val EGYPTIAN = "Egyptian General Authority of Survey"
    const val MAKKAH = "Umm Al-Qura University, Makkah"
    const val TEHRAN = "Institute of Geophysics, University of Tehran"
    const val MOONSIGHTING = "Moonsighting Committee"
    const val ISNA = "Islamic Society of North America"

    val ALL_METHODS = listOf(
        KARACHI,
        MWL,
        EGYPTIAN,
        MAKKAH,
        TEHRAN,
        MOONSIGHTING,
        ISNA
    )

    fun getFajrAngle(method: String): Double = when (method) {
        EGYPTIAN -> 19.5
        MAKKAH -> 18.5
        TEHRAN -> 17.7
        ISNA -> 15.0
        KARACHI, MWL, MOONSIGHTING -> 18.0
        else -> 18.0
    }

    fun getIshaAngle(method: String): Double = when (method) {
        EGYPTIAN -> 17.5
        TEHRAN -> 14.0
        ISNA -> 15.0
        MWL -> 17.0
        KARACHI, MOONSIGHTING -> 18.0
        MAKKAH -> 0.0 // Special: 90 min after Maghrib
        else -> 18.0
    }

    fun isIshaFixedInterval(method: String): Boolean = method == MAKKAH
    fun getIshaFixedMinutes(method: String): Int = if (method == MAKKAH) 90 else 0
}

object MadhabOptions {
    const val HANAFI = "Hanafi"
    const val SHAFI = "Shafi, Maliki, Hanbali"

    val ALL_MADHABS = listOf(
        HANAFI,
        SHAFI
    )

    fun getShadowMultiplier(madhab: String): Double = when (madhab) {
        HANAFI -> 2.0
        else -> 1.0
    }
}

// ----------------------------------------------------------------------------
// HIJRI / ISLAMIC CALENDAR HELPER
// ----------------------------------------------------------------------------

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthName: String,
    val year: Int,
    val formatted: String,
    val shortFormatted: String
)

object HijriCalendarHelper {
    private val MONTH_NAMES = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    /**
     * Converts a Gregorian LocalDate to an astronomical tabular Hijri date (Kuwaiti algorithm)
     * clearly labeled as estimated/calculated.
     */
    fun getHijriDate(date: java.time.LocalDate, dayOffset: Int = 0): HijriDate {
        val targetDate = date.plusDays(dayOffset.toLong())
        val year = targetDate.year
        val month = targetDate.monthValue
        val day = targetDate.dayOfMonth

        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        // Islamic Epoch JDN is 1948440
        val l = jdn - 1948440 + 10632
        val n = (l - 1) / 10631
        val lPrime = l - 10631 * n + 354
        val j = ((10985 - lPrime) / 5316) * ((50 * lPrime) / 17719) + (lPrime / 5670) * ((43 * lPrime) / 15238)
        val lDoublePrime = lPrime - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hijriMonth = (24 * lDoublePrime) / 709
        val hijriDay = lDoublePrime - (709 * hijriMonth) / 24
        val hijriYear = 30 * n + j - 30

        val validMonth = (hijriMonth.toInt()).coerceIn(1, 12)
        val validDay = (hijriDay.toInt()).coerceIn(1, 30)
        val monthName = MONTH_NAMES[validMonth - 1]

        return HijriDate(
            day = validDay,
            month = validMonth,
            monthName = monthName,
            year = hijriYear.toInt(),
            formatted = "$validDay $monthName $hijriYear AH (Estimated)",
            shortFormatted = "$validDay $monthName $hijriYear AH"
        )
    }
}

// ----------------------------------------------------------------------------
// QIBLA DIRECTION & KAABA CALCULATOR
// ----------------------------------------------------------------------------

data class QiblaData(
    val bearingDegrees: Float,
    val cardinalDirection: String,
    val distanceKm: Int,
    val locationName: String,
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125
)

object QiblaCalculator {
    // Exact Holy Kaaba coordinates in Makkah al-Mukarramah
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /**
     * Calculates great-circle initial bearing from user coordinate to Kaaba.
     * Works 100% offline using trigonometric formula.
     */
    fun calculateBearing(userLat: Double, userLon: Double): Float {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - userLon)

        val y = Math.sin(deltaLambda)
        val x = Math.cos(phi1) * Math.tan(phi2) - Math.sin(phi1) * Math.cos(deltaLambda)
        var qibla = Math.toDegrees(Math.atan2(y, x)).toFloat()
        return (qibla + 360f) % 360f
    }

    /**
     * Calculates distance to Kaaba in kilometers using Haversine formula.
     */
    fun calculateDistanceKm(userLat: Double, userLon: Double): Int {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LATITUDE - userLat)
        val dLon = Math.toRadians(KAABA_LONGITUDE - userLon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(KAABA_LATITUDE)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadiusKm * c).toInt()
    }

    fun getCardinalDirection(degrees: Float): String {
        val normalized = (degrees % 360 + 360) % 360
        return when {
            normalized in 348.75..360.0 || normalized < 11.25 -> "N"
            normalized in 11.25..33.75 -> "NNE"
            normalized in 33.75..56.25 -> "NE"
            normalized in 56.25..78.75 -> "ENE"
            normalized in 78.75..101.25 -> "E"
            normalized in 101.25..123.75 -> "ESE"
            normalized in 123.75..146.25 -> "SE"
            normalized in 146.25..168.75 -> "SSE"
            normalized in 168.75..191.25 -> "S"
            normalized in 191.25..213.75 -> "SSW"
            normalized in 213.75..236.25 -> "SW"
            normalized in 236.25..258.75 -> "WSW"
            normalized in 258.75..281.25 -> "W"
            normalized in 281.25..303.75 -> "WNW"
            normalized in 303.75..326.25 -> "NW"
            else -> "NNW"
        }
    }

    fun getQiblaData(userLat: Double, userLon: Double, locationName: String): QiblaData {
        val bearing = calculateBearing(userLat, userLon)
        val cardinal = getCardinalDirection(bearing)
        val dist = calculateDistanceKm(userLat, userLon)
        return QiblaData(
            bearingDegrees = bearing,
            cardinalDirection = cardinal,
            distanceKm = dist,
            locationName = locationName,
            latitude = userLat,
            longitude = userLon
        )
    }
}

// ----------------------------------------------------------------------------
// PRAYER HISTORY & STATS MODELS
// ----------------------------------------------------------------------------

enum class PrayerHistoryFilter {
    SEVEN_DAYS,
    THIRTY_DAYS,
    ALL_TIME
}

data class DayPrayerRecord(
    val date: String, // YYYY-MM-DD
    val dayOfWeek: String, // Mon, Tue...
    val formattedDate: String, // e.g. "12 Sep 2026"
    val completedPrayers: Set<String>,
    val totalObligatory: Int = 5,
    val isToday: Boolean = false
) {
    val completedCount: Int get() = completedPrayers.size
    val completionPercentage: Int get() = ((completedCount / 5f) * 100).toInt()
    val isComplete: Boolean get() = completedCount >= 5

    fun isPrayerCompleted(name: String): Boolean = completedPrayers.contains(name)
}

data class PrayerHistoryStats(
    val totalDaysTracked: Int,
    val totalCompletedPrayers: Int,
    val totalPossiblePrayers: Int,
    val overallCompletionRate: Int, // 0 - 100%
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val prayerBreakdown: Map<PrayerName, Pair<Int, Int>> // Prayer -> (completedCount, totalOpportunities)
)
