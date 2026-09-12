package com.example.salat

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

object PrayerTimeCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun calculateSchedule(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        method: String = CalculationMethods.KARACHI,
        madhab: String = MadhabOptions.HANAFI,
        locationName: String = "Chattogram, Bangladesh"
    ): SalatSchedule {
        val zoneId = try {
            ZoneId.of(timeZoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val zonedDateTime = date.atStartOfDay(zoneId)
        val offsetSeconds = zoneId.rules.getOffset(zonedDateTime.toInstant()).totalSeconds
        val tzOffsetHours = offsetSeconds / 3600.0

        val julianDay = getJulianDay(date.year, date.monthValue, date.dayOfMonth)
        val solar = computeSolarPosition(julianDay)

        val noon = 12.0 + tzOffsetHours - (longitude / 15.0) - solar.equationOfTime
        val dhuhrDecimal = noon + (2.0 / 60.0) // 2 min buffer after zenith

        val sunriseHA = computeHourAngle(latitude, solar.declination, 0.8333)
        val sunriseDecimal = noon - sunriseHA
        val sunsetDecimal = noon + sunriseHA

        val fajrAngle = CalculationMethods.getFajrAngle(method)
        val fajrHA = computeHourAngle(latitude, solar.declination, fajrAngle)
        val fajrDecimal = noon - fajrHA

        val shadowMultiplier = MadhabOptions.getShadowMultiplier(madhab)
        val asrHA = computeAsrHourAngle(latitude, solar.declination, shadowMultiplier)
        val asrDecimal = noon + asrHA

        val maghribDecimal = if (method == CalculationMethods.TEHRAN) {
            val tehranHA = computeHourAngle(latitude, solar.declination, 4.5)
            noon + tehranHA
        } else {
            sunsetDecimal + (2.0 / 60.0)
        }

        val ishaDecimal = if (CalculationMethods.isIshaFixedInterval(method)) {
            maghribDecimal + (CalculationMethods.getIshaFixedMinutes(method) / 60.0)
        } else {
            val ishaAngle = CalculationMethods.getIshaAngle(method)
            val ishaHA = computeHourAngle(latitude, solar.declination, ishaAngle)
            noon + ishaHA
        }

        return SalatSchedule(
            date = date.toString(),
            fajr = formatDecimalHours(fajrDecimal),
            sunrise = formatDecimalHours(sunriseDecimal),
            dhuhr = formatDecimalHours(dhuhrDecimal),
            asr = formatDecimalHours(asrDecimal),
            maghrib = formatDecimalHours(maghribDecimal),
            isha = formatDecimalHours(ishaDecimal),
            locationName = locationName,
            method = method,
            madhab = madhab,
            timeZoneId = zoneId.id,
            isOfflineCalculated = true
        )
    }

    fun computePrayerStatus(
        todaySchedule: SalatSchedule,
        tomorrowSchedule: SalatSchedule,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: LocalDateTime = LocalDateTime.now(zoneId)
    ): PrayerStatus {
        val todayDate = now.toLocalDate()
        val tomorrowDate = todayDate.plusDays(1)
        val yesterdayDate = todayDate.minusDays(1)

        val fajrTime = parseDateTime(todayDate, todaySchedule.fajr)
        val sunriseTime = parseDateTime(todayDate, todaySchedule.sunrise)
        val dhuhrTime = parseDateTime(todayDate, todaySchedule.dhuhr)
        val asrTime = parseDateTime(todayDate, todaySchedule.asr)
        val maghribTime = parseDateTime(todayDate, todaySchedule.maghrib)
        val ishaTime = parseDateTime(todayDate, todaySchedule.isha)

        val tomorrowFajrTime = parseDateTime(tomorrowDate, tomorrowSchedule.fajr)
        val yesterdayIshaTime = parseDateTime(yesterdayDate, todaySchedule.isha)

        val currentPrayer: PrayerName
        val nextPrayer: PrayerName
        val nextPrayerTime: LocalDateTime
        val currentPeriodStart: LocalDateTime
        val currentPeriodEnd: LocalDateTime

        when {
            now.isBefore(fajrTime) -> {
                currentPrayer = PrayerName.ISHA
                nextPrayer = PrayerName.FAJR
                nextPrayerTime = fajrTime
                currentPeriodStart = yesterdayIshaTime
                currentPeriodEnd = fajrTime
            }
            now.isBefore(sunriseTime) -> {
                currentPrayer = PrayerName.FAJR
                nextPrayer = PrayerName.DHUHR
                nextPrayerTime = dhuhrTime
                currentPeriodStart = fajrTime
                currentPeriodEnd = sunriseTime
            }
            now.isBefore(dhuhrTime) -> {
                currentPrayer = PrayerName.SUNRISE
                nextPrayer = PrayerName.DHUHR
                nextPrayerTime = dhuhrTime
                currentPeriodStart = sunriseTime
                currentPeriodEnd = dhuhrTime
            }
            now.isBefore(asrTime) -> {
                currentPrayer = PrayerName.DHUHR
                nextPrayer = PrayerName.ASR
                nextPrayerTime = asrTime
                currentPeriodStart = dhuhrTime
                currentPeriodEnd = asrTime
            }
            now.isBefore(maghribTime) -> {
                currentPrayer = PrayerName.ASR
                nextPrayer = PrayerName.MAGHRIB
                nextPrayerTime = maghribTime
                currentPeriodStart = asrTime
                currentPeriodEnd = maghribTime
            }
            now.isBefore(ishaTime) -> {
                currentPrayer = PrayerName.MAGHRIB
                nextPrayer = PrayerName.ISHA
                nextPrayerTime = ishaTime
                currentPeriodStart = maghribTime
                currentPeriodEnd = ishaTime
            }
            else -> {
                currentPrayer = PrayerName.ISHA
                nextPrayer = PrayerName.FAJR
                nextPrayerTime = tomorrowFajrTime
                currentPeriodStart = ishaTime
                currentPeriodEnd = tomorrowFajrTime
            }
        }

        val nowMs = now.atZone(zoneId).toInstant().toEpochMilli()
        val nextMs = nextPrayerTime.atZone(zoneId).toInstant().toEpochMilli()
        val periodStartMs = currentPeriodStart.atZone(zoneId).toInstant().toEpochMilli()
        val periodEndMs = currentPeriodEnd.atZone(zoneId).toInstant().toEpochMilli()

        val timeRemainingNextMs = (nextMs - nowMs).coerceAtLeast(0L)
        val timeRemainingEndCurrentMs = (periodEndMs - nowMs).coerceAtLeast(0L)

        val totalWindowMs = (periodEndMs - periodStartMs).coerceAtLeast(1L)
        val elapsedMs = (nowMs - periodStartMs).coerceAtLeast(0L)
        val progress = (elapsedMs.toFloat() / totalWindowMs.toFloat()).coerceIn(0f, 1f)

        return PrayerStatus(
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            nextPrayerTime = nextPrayerTime.toLocalTime().format(timeFormatter),
            timeRemainingNextMs = timeRemainingNextMs,
            timeRemainingNextFormatted = formatRemainingTime(timeRemainingNextMs),
            currentPrayerEndsInMs = timeRemainingEndCurrentMs,
            currentPrayerEndsInFormatted = formatRemainingTime(timeRemainingEndCurrentMs),
            progress = progress
        )
    }

    private fun parseDateTime(date: LocalDate, timeStr: String): LocalDateTime {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return date.atTime(LocalTime.of(h, m))
    }

    fun formatRemainingTime(millis: Long): String {
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatRemainingTimeLong(millis: Long): String {
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> "${hours} hr ${minutes} min"
            minutes > 0 -> "${minutes} min ${seconds} sec"
            else -> "${seconds} sec"
        }
    }

    fun formatTo12Hour(time24: String): String {
        val parts = time24.split(":")
        if (parts.size != 2) return time24
        val h = parts[0].toIntOrNull() ?: return time24
        val m = parts[1].toIntOrNull() ?: return time24
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = when (h % 12) {
            0 -> 12
            else -> h % 12
        }
        return String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
    }

    fun subtractMinutes(time24: String, minutes: Int): String {
        val parts = time24.split(":")
        if (parts.size != 2) return time24
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        var totalMin = (h * 60 + m - minutes) % (24 * 60)
        if (totalMin < 0) totalMin += 24 * 60
        val newH = totalMin / 60
        val newM = totalMin % 60
        return String.format(Locale.US, "%02d:%02d", newH, newM)
    }

    fun addMinutes(time24: String, minutes: Int): String {
        val parts = time24.split(":")
        if (parts.size != 2) return time24
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val totalMin = (h * 60 + m + minutes) % (24 * 60)
        val newH = totalMin / 60
        val newM = totalMin % 60
        return String.format(Locale.US, "%02d:%02d", newH, newM)
    }

    fun toBengaliDigits(text: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return text.map { ch ->
            if (ch in '0'..'9') bnDigits[ch - '0'] else ch
        }.joinToString("")
    }

    fun getPrayerBengaliName(prayer: PrayerName): String = when (prayer) {
        PrayerName.FAJR -> "ফজর"
        PrayerName.SUNRISE -> "সূর্যোদয়"
        PrayerName.DHUHR -> "যোহর"
        PrayerName.ASR -> "আসর"
        PrayerName.MAGHRIB -> "মাগরিব"
        PrayerName.ISHA -> "ইশা"
    }

    // --- Astronomical Formula Helpers ---

    private fun getJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SolarPosition(val declination: Double, val equationOfTime: Double)

    private fun computeSolarPosition(julianDay: Double): SolarPosition {
        val d0 = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d0)
        val q = fixAngle(280.459 + 0.98564736 * d0)
        val l = fixAngle(q + 1.915 * sinD(g) + 0.020 * sinD(2 * g))
        val e = 23.439 - 0.00000036 * d0
        val ra = fixAngle(atan2D(cosD(e) * sinD(l), cosD(l))) / 15.0
        val d = asinD(sinD(e) * sinD(l))
        val eqt = q / 15.0 - fixHour(ra)
        return SolarPosition(declination = d, equationOfTime = eqt)
    }

    private fun computeHourAngle(latitude: Double, declination: Double, angle: Double): Double {
        val cosH = (-sinD(angle) - sinD(latitude) * sinD(declination)) / (cosD(latitude) * cosD(declination))
        if (cosH > 1.0) return 0.0
        if (cosH < -1.0) return 180.0 / 15.0
        return acosD(cosH) / 15.0
    }

    private fun computeAsrHourAngle(latitude: Double, declination: Double, shadowMultiplier: Double): Double {
        val diff = abs(latitude - declination)
        val cotAltitude = shadowMultiplier + tanD(diff)
        val altitude = atanD(1.0 / cotAltitude)
        val cosH = (sinD(altitude) - sinD(latitude) * sinD(declination)) / (cosD(latitude) * cosD(declination))
        if (cosH > 1.0) return 0.0
        if (cosH < -1.0) return 180.0 / 15.0
        return acosD(cosH) / 15.0
    }

    private fun formatDecimalHours(decimalHours: Double): String {
        val h = fixHour(decimalHours)
        val hour = floor(h).toInt()
        val minute = (round((h - hour) * 60.0)).toInt()
        var finalHour = hour
        var finalMinute = minute
        if (finalMinute >= 60) {
            finalHour = (finalHour + 1) % 24
            finalMinute = 0
        }
        return String.format(Locale.US, "%02d:%02d", finalHour, finalMinute)
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour - 24.0 * floor(hour / 24.0)
        if (h < 0) h += 24.0
        return h
    }

    private fun sinD(degree: Double): Double = sin(Math.toRadians(degree))
    private fun cosD(degree: Double): Double = cos(Math.toRadians(degree))
    private fun tanD(degree: Double): Double = tan(Math.toRadians(degree))
    private fun asinD(v: Double): Double = Math.toDegrees(asin(v.coerceIn(-1.0, 1.0)))
    private fun acosD(v: Double): Double = Math.toDegrees(acos(v.coerceIn(-1.0, 1.0)))
    private fun atanD(v: Double): Double = Math.toDegrees(atan(v))
    private fun atan2D(y: Double, x: Double): Double = Math.toDegrees(atan2(y, x))
}
