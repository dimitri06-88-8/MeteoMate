package com.example.meteomate.work.notification

import com.example.meteomate.data.AppSettings
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherNotificationRulesTest {
    @Test
    fun quietHours_supportIntervalAcrossMidnight() {
        assertTrue(WeatherNotificationRules.isQuietTime(23 * 60, 22 * 60, 7 * 60))
        assertTrue(WeatherNotificationRules.isQuietTime(6 * 60 + 59, 22 * 60, 7 * 60))
        assertFalse(WeatherNotificationRules.isQuietTime(12 * 60, 22 * 60, 7 * 60))
    }

    @Test
    fun equalQuietHourBounds_doNotSilenceWholeDay() {
        assertFalse(WeatherNotificationRules.isQuietTime(12 * 60, 7 * 60, 7 * 60))
    }

    @Test
    fun summary_isDueOnceInsideDeliveryWindow() {
        val now = ZonedDateTime.of(2026, 7, 14, 8, 30, 0, 0, ZoneId.of("Europe/Moscow"))

        assertTrue(
            WeatherNotificationRules.isMorningSummaryDue(now, 8 * 60, wasSentToday = false)
        )
        assertFalse(
            WeatherNotificationRules.isMorningSummaryDue(now, 8 * 60, wasSentToday = true)
        )
    }

    @Test
    fun summary_isNotDeliveredLongAfterScheduledTime() {
        val now = ZonedDateTime.of(2026, 7, 14, 9, 1, 0, 0, ZoneId.of("Europe/Moscow"))

        assertFalse(
            WeatherNotificationRules.isMorningSummaryDue(now, 8 * 60, wasSentToday = false)
        )
    }

    @Test
    fun rapidChanges_detectTemperatureAndPressureThresholds() {
        val settings = AppSettings(
            rapidTemperatureChangeNotificationsEnabled = true,
            rapidPressureDropNotificationsEnabled = true
        )
        val previous = WeatherObservation(20.0, 1015, 1_000L)
        val current = WeatherObservation(13.5, 1008, 2L * 60L * 60L * 1000L)

        val kinds = WeatherNotificationRules.detectRapidChanges(settings, previous, current)
            .map { it.kind }

        assertEquals(
            setOf(
                WeatherAlertKind.RAPID_TEMPERATURE_CHANGE,
                WeatherAlertKind.RAPID_PRESSURE_DROP
            ),
            kinds.toSet()
        )
    }

    @Test
    fun staleObservation_isNotUsedForRapidChangeAlert() {
        val settings = AppSettings(
            rapidTemperatureChangeNotificationsEnabled = true,
            rapidPressureDropNotificationsEnabled = true
        )
        val previous = WeatherObservation(20.0, 1015, 1_000L)
        val current = WeatherObservation(
            5.0,
            990,
            WeatherNotificationRules.OBSERVATION_WINDOW_MILLIS + 2_000L
        )

        assertTrue(WeatherNotificationRules.detectRapidChanges(settings, previous, current).isEmpty())
    }
}
