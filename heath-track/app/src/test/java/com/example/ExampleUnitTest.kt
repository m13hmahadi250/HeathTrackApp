package com.example

import org.junit.Assert.*
import org.junit.Test
import com.example.salat.PrayerTimeCalculator
import com.example.salat.CalculationMethods
import com.example.salat.MadhabOptions
import com.example.salat.PrayerName
import java.time.LocalDate

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPrayerTimeCalculation_Chattogram() {
    val date = LocalDate.of(2026, 9, 12)
    val schedule = PrayerTimeCalculator.calculateSchedule(
      date = date,
      latitude = 22.3569,
      longitude = 91.7832,
      timeZoneId = "Asia/Dhaka",
      method = CalculationMethods.KARACHI,
      madhab = MadhabOptions.HANAFI,
      locationName = "Chattogram, Bangladesh"
    )

    assertNotNull(schedule.fajr)
    assertNotNull(schedule.dhuhr)
    assertNotNull(schedule.asr)
    assertNotNull(schedule.maghrib)
    assertNotNull(schedule.isha)

    // Fajr is before sunrise
    assertTrue(schedule.fajr < schedule.sunrise)
    // Sunrise is before Dhuhr
    assertTrue(schedule.sunrise < schedule.dhuhr)
    // Dhuhr is before Asr
    assertTrue(schedule.dhuhr < schedule.asr)
    // Asr is before Maghrib
    assertTrue(schedule.asr < schedule.maghrib)
    // Maghrib is before Isha
    assertTrue(schedule.maghrib < schedule.isha)

    // Tomorrow schedule test
    val tomorrow = date.plusDays(1)
    val tomorrowSchedule = PrayerTimeCalculator.calculateSchedule(
      date = tomorrow,
      latitude = 22.3569,
      longitude = 91.7832,
      timeZoneId = "Asia/Dhaka",
      method = CalculationMethods.KARACHI,
      madhab = MadhabOptions.HANAFI,
      locationName = "Chattogram, Bangladesh"
    )

    val status = PrayerTimeCalculator.computePrayerStatus(schedule, tomorrowSchedule)
    assertNotNull(status.currentPrayer)
    assertNotNull(status.nextPrayer)
    assertTrue(status.progress in 0f..1f)
  }
}
