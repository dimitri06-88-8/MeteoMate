package com.example.meteomate.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastRecordsTest {
    @Test
    fun selectsHottestColdestAndWindiestDays() {
        val records = calculateForecastRecords(
            listOf(
                item("2026-07-20", max = 25.0, min = 14.0, wind = 3.0),
                item("2026-07-21", max = 31.0, min = 18.0, wind = 4.0),
                item("2026-07-22", max = 20.0, min = 8.0, wind = 11.0)
            )
        )

        requireNotNull(records)
        assertEquals("2026-07-21", records.hottest.day)
        assertEquals("2026-07-22", records.coldest.day)
        assertEquals("2026-07-22", records.windiest.day)
    }

    @Test
    fun emptyForecast_hasNoRecords() {
        assertNull(calculateForecastRecords(emptyList()))
    }

    private fun item(day: String, max: Double, min: Double, wind: Double) = DailyForecastItem(
        day = day,
        weatherCode = 800,
        maxTemp = max,
        minTemp = min,
        precipitationProbability = 0.0,
        maxWindSpeed = wind
    )
}

