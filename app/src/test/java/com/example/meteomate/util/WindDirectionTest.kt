package com.example.meteomate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WindDirectionTest {

    @Test
    fun cardinalAndIntercardinalDirections_areMappedToRussianLabels() {
        assertEquals("С", windDirectionLabel(0).abbreviation)
        assertEquals("СВ", windDirectionLabel(45).abbreviation)
        assertEquals("В", windDirectionLabel(90).abbreviation)
        assertEquals("Ю", windDirectionLabel(180).abbreviation)
        assertEquals("З", windDirectionLabel(270).abbreviation)
        assertEquals("СЗ", windDirectionLabel(315).abbreviation)
    }

    @Test
    fun degrees_areNormalizedAndRoundedToNearestSector() {
        assertEquals("С", windDirectionLabel(359).abbreviation)
        assertEquals("С", windDirectionLabel(22).abbreviation)
        assertEquals("СВ", windDirectionLabel(23).abbreviation)
        assertEquals("З", windDirectionLabel(-90).abbreviation)
    }

    @Test
    fun windValues_keepOneDecimalInsteadOfTruncating() {
        assertEquals("4,8", formatWindValue(4.78).replace('.', ','))
        assertEquals("5", formatWindValue(5.0))
    }
}
