package com.example.meteomate.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeocodingResponseTest {

    @Test
    fun localizedName_returnsRussianNameWhenAvailable() {
        val city = city(name = "Moscow", localNames = mapOf("ru" to "Москва"))

        assertEquals("Москва", city.localizedName())
    }

    @Test
    fun localizedName_fallsBackToApiNameWhenRussianNameIsMissingOrBlank() {
        assertEquals("Moscow", city(name = "Moscow", localNames = null).localizedName())
        assertEquals(
            "Moscow",
            city(name = "Moscow", localNames = mapOf("ru" to "")).localizedName()
        )
    }

    @Test
    fun openMeteoResult_keepsRussianCityRegionAndCountry() {
        val result = OpenMeteoGeocodingResult(
            id = 1486209,
            name = "Екатеринбург",
            latitude = 56.85733,
            longitude = 60.61529,
            country = "Россия",
            countryCode = "RU",
            admin1 = "Свердловская Область"
        ).toGeocodingResponse()

        assertEquals("Екатеринбург", result.localizedName())
        assertEquals("Свердловская Область", result.state)
        assertEquals("Россия", result.country)
    }

    private fun city(
        name: String,
        localNames: Map<String, String>?
    ) = GeocodingResponse(
        name = name,
        localNames = localNames,
        lat = 55.7558,
        lon = 37.6173,
        country = "RU",
        state = "Moscow"
    )
}
