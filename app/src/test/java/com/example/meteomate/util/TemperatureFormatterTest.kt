package com.example.meteomate.util

import com.example.meteomate.data.TemperatureUnit
import org.junit.Assert.*
import org.junit.Test

class TemperatureFormatterTest {

    @Test
    fun format_positiveTemperature() {
        val result = TemperatureFormatter.format(25.7)
        assertEquals("25°", result)
    }

    @Test
    fun format_negativeTemperature() {
        val result = TemperatureFormatter.format(-5.3)
        assertEquals("-5°", result)
    }

    @Test
    fun format_zeroTemperature() {
        val result = TemperatureFormatter.format(0.0)
        assertEquals("0°", result)
    }

    @Test
    fun format_convertsCelsiusToFahrenheit() {
        assertEquals("68°F", TemperatureFormatter.format(20.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("20°C", TemperatureFormatter.format(20.0, TemperatureUnit.CELSIUS))
    }
}
