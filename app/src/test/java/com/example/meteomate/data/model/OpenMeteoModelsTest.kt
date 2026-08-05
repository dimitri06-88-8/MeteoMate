package com.example.meteomate.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoModelsTest {
    private val gson = Gson()

    @Test
    fun hourlySeries_areParsedFromApiResponse() {
        val json = """
            {
              "latitude": 55.75,
              "longitude": 37.61,
              "hourly": {
                "time": ["2026-07-14T12:00", "2026-07-14T13:00"],
                "temperature_2m": [21.4, 22.0],
                "wind_speed_10m": [3.2, 4.1],
                "wind_direction_10m": [180, 190],
                "wind_gusts_10m": [6.3, 7.5],
                "surface_pressure": [1008.2, 1007.9]
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, OpenMeteoWindResponse::class.java)
        val hourly = requireNotNull(response.hourly)

        assertEquals(listOf("2026-07-14T12:00", "2026-07-14T13:00"), hourly.time)
        assertEquals(listOf(21.4, 22.0), hourly.temperature)
        assertEquals(listOf(3.2, 4.1), hourly.windSpeed)
        assertEquals(listOf(180, 190), hourly.windDirection)
        assertEquals(listOf(6.3, 7.5), hourly.windGusts)
        assertEquals(listOf(1008.2, 1007.9), hourly.pressure)
    }

    @Test
    fun absentOptionalSections_doNotBreakDeserialization() {
        val response = gson.fromJson("{}", OpenMeteoWindResponse::class.java)

        assertNotNull(response)
        assertTrue(response.timeSeriesAreAbsent())
    }

    @Test
    fun nullHourlySeries_areExposedAsEmptyLists() {
        val json = """
            {
              "hourly": {
                "time": null,
                "temperature_2m": null,
                "wind_speed_10m": null,
                "wind_direction_10m": null,
                "wind_gusts_10m": null
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, OpenMeteoWindResponse::class.java)
        val hourly = requireNotNull(response.hourly)

        assertTrue(hourly.time.isEmpty())
        assertTrue(hourly.temperature.isEmpty())
        assertTrue(hourly.windSpeed.isEmpty())
        assertTrue(hourly.windDirection.isEmpty())
        assertTrue(hourly.windGusts.isEmpty())
    }

    private fun OpenMeteoWindResponse.timeSeriesAreAbsent(): Boolean =
        current == null && hourly == null && daily == null && minutely15 == null
}
