package com.example.meteomate.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetWeatherSnapshotTest {
    @Test
    fun findsExactOrNextHourlyInterval() {
        val times = listOf(
            "2026-07-20T09:00",
            "2026-07-20T10:00",
            "2026-07-20T11:00"
        )
        assertEquals(1, nearestHourlyIndex(times, "2026-07-20T10:00"))
        assertEquals(2, nearestHourlyIndex(times, "2026-07-20T10:30"))
    }

    @Test
    fun malformedCurrentTimeFallsBackToFirstValue() {
        assertEquals(0, nearestHourlyIndex(listOf("2026-07-20T09:00"), null))
    }
}
