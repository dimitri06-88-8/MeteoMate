package com.example.meteomate.work.notification

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldenHourSchedulerTest {
    @Test
    fun createsMorningAndEveningPlansThirtyMinutesEarly() {
        val zone = ZoneId.of("Europe/Moscow")
        val now = LocalDateTime.of(2026, 7, 20, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val plans = nextSolarNotificationPlans(
            sunriseTimes = listOf("2026-07-20T04:10"),
            sunsetTimes = listOf("2026-07-20T20:55"),
            timezoneId = zone.id,
            fallbackOffsetSeconds = 0,
            nowMillis = now
        )

        assertEquals(2, plans.size)
        plans.forEach { plan ->
            assertEquals(30L * 60L * 1000L, plan.eventEpochMillis - plan.notificationEpochMillis)
        }
    }

    @Test
    fun timezoneId_handlesDaylightSavingOffset() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = LocalDateTime.of(2026, 3, 28, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val plan = nextSolarNotificationPlans(
            sunriseTimes = listOf("2026-03-29T06:00"),
            sunsetTimes = emptyList(),
            timezoneId = zone.id,
            fallbackOffsetSeconds = 3600,
            nowMillis = now
        ).single()

        val event = java.time.Instant.ofEpochMilli(plan.eventEpochMillis).atZone(zone)
        assertEquals(2 * 60 * 60, event.offset.totalSeconds)
    }

    @Test
    fun passedNotificationIsSkipped() {
        val zone = ZoneId.of("UTC")
        val now = LocalDateTime.of(2026, 7, 20, 8, 0).atZone(zone).toInstant().toEpochMilli()
        val plans = nextSolarNotificationPlans(
            sunriseTimes = listOf("2026-07-20T06:00"),
            sunsetTimes = emptyList(),
            timezoneId = zone.id,
            fallbackOffsetSeconds = 0,
            nowMillis = now
        )
        assertTrue(plans.isEmpty())
    }
}

