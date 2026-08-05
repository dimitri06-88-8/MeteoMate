package com.example.meteomate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SolarGuidanceTest {
    @Test
    fun uvBands_followProtectionThresholds() {
        assertEquals(UvRiskLevel.LOW, uvGuidance(2.9).level)
        assertEquals(UvRiskLevel.MODERATE_TO_HIGH, uvGuidance(3.0).level)
        assertEquals(UvRiskLevel.MODERATE_TO_HIGH, uvGuidance(7.9).level)
        assertEquals(UvRiskLevel.VERY_HIGH_TO_EXTREME, uvGuidance(8.0).level)
    }

    @Test
    fun missingUv_hasNoTimer() {
        val guidance = uvGuidance(null)
        assertEquals(UvRiskLevel.UNKNOWN, guidance.level)
        assertNull(guidance.protectionReminderMinutes)
    }
}

