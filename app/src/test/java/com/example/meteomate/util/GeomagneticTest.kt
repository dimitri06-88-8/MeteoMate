package com.example.meteomate.util

import com.example.meteomate.data.model.NoaaPlanetaryKpEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeomagneticTest {

    @Test
    fun snapshot_usesLatestObservationAndHighest24HourForecast() {
        val entries = listOf(
            entry("2026-07-20T09:00:00", 2.0, "observed"),
            entry("2026-07-20T12:00:00", 3.0, "estimated"),
            entry("2026-07-20T15:00:00", 5.0, "predicted", "G1"),
            entry("2026-07-20T18:00:00", 6.0, "predicted", "G2"),
            entry("2026-07-21T18:00:00", 8.0, "predicted", "G4")
        )

        val result = buildGeomagneticSnapshot(entries, Instant.parse("2026-07-20T12:30:00Z"))!!

        assertEquals(3.0, result.currentKp, 0.0)
        assertEquals(6.0, result.maximumForecastKp ?: -1.0, 0.0)
        assertEquals("G2", result.forecastScale)
    }

    @Test
    fun invalidOrFutureOnlyData_returnsNoSnapshot() {
        val entries = listOf(
            entry("2026-07-21T12:00:00", 4.0, "predicted"),
            entry("2026-07-20T09:00:00", 12.0, "observed")
        )

        assertNull(buildGeomagneticSnapshot(entries, Instant.parse("2026-07-20T12:00:00Z")))
    }

    @Test
    fun officialNoaaScale_hasPriorityInLabel() {
        assertEquals("Слабая магнитная буря (G1)", geomagneticActivityLabel(4.7, "G1"))
        assertEquals("Повышенная активность", geomagneticActivityLabel(4.0))
    }

    private fun entry(time: String, kp: Double, kind: String, scale: String? = null) =
        NoaaPlanetaryKpEntry(time, kp, kind, scale)
}
