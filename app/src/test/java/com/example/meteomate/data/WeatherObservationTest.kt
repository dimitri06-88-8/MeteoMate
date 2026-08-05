package com.example.meteomate.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherObservationTest {
    @Test fun reportBadgesUnlockAtTheirThresholds() {
        val first = meteoBadges.first { it.id == "first_signal" }
        val scout = meteoBadges.first { it.id == "sky_scout" }
        val crystal = meteoBadges.first { it.id == "storm_crystal" }
        assertFalse(first.isUnlocked(WeatherObservationProgress(totalReports = 0)))
        assertTrue(first.isUnlocked(WeatherObservationProgress(totalReports = 1)))
        assertFalse(scout.isUnlocked(WeatherObservationProgress(totalReports = 6)))
        assertTrue(scout.isUnlocked(WeatherObservationProgress(totalReports = 7)))
        assertFalse(crystal.isUnlocked(WeatherObservationProgress(totalReports = 19)))
        assertTrue(crystal.isUnlocked(WeatherObservationProgress(totalReports = 20)))
    }

    @Test fun secretBadgeUsesOnlySecretFlag() {
        val secret = meteoBadges.first { it.isSecret }
        assertFalse(secret.isUnlocked(WeatherObservationProgress(totalReports = 100)))
        assertTrue(secret.isUnlocked(WeatherObservationProgress(secretBadgeUnlocked = true)))
    }
}
