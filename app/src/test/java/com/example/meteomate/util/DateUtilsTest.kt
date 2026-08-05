package com.example.meteomate.util

import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {

    @Test
    fun extractDate_returnsDatePart() {
        val result = DateUtils.extractDate("2024-01-15 12:00:00")
        assertEquals("2024-01-15", result)
    }

    @Test
    fun extractDate_returnsOriginalOnInvalidInput() {
        val result = DateUtils.extractDate("invalid")
        assertEquals("invalid", result)
    }

    @Test
    fun formatTime_returnsTimePart() {
        val result = DateUtils.formatTime("2024-01-15 14:30:00")
        assertEquals("14:30", result)
    }

    @Test
    fun formatTime_fallsBackOnInvalidInput() {
        val result = DateUtils.formatTime("short")
        assertEquals("short", result)
    }

    @Test
    fun isToday_returnsFalseForPastDate() {
        val result = DateUtils.isToday("2020-01-01")
        assertFalse(result)
    }

    @Test
    fun isToday_returnsFalseForInvalidDate() {
        val result = DateUtils.isToday("not-a-date")
        assertFalse(result)
    }
}
