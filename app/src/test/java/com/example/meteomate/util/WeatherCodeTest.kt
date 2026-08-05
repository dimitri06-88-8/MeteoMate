package com.example.meteomate.util

import org.junit.Assert.*
import org.junit.Test

class WeatherCodeTest {

    @Test
    fun emoji_clearSky() {
        val result = WeatherCode.emoji(800)
        assertEquals("\u2600\uFE0F", result)
    }

    @Test
    fun emoji_thunderstorm() {
        val result = WeatherCode.emoji(200)
        assertEquals("\u26C8\uFE0F", result)
    }

    @Test
    fun emoji_unknownCode_returnsDefault() {
        val result = WeatherCode.emoji(999)
        assertEquals("\u2600\uFE0F", result)
    }

    @Test
    fun description_clearSky() {
        val result = WeatherCode.description(800)
        assertEquals(com.example.meteomate.R.string.clear_sky, result)
    }

    @Test
    fun openMeteoCode_isConvertedToUiWeatherCode() {
        assertEquals(800, OpenMeteoWeatherCode.toOpenWeather(0))
        assertEquals(741, OpenMeteoWeatherCode.toOpenWeather(45))
        assertEquals(500, OpenMeteoWeatherCode.toOpenWeather(82))
        assertEquals(600, OpenMeteoWeatherCode.toOpenWeather(85))
        assertEquals(200, OpenMeteoWeatherCode.toOpenWeather(99))
    }
}
