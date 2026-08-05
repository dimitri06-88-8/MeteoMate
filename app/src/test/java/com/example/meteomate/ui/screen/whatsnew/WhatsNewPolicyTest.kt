package com.example.meteomate.ui.screen.whatsnew

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewPolicyTest {
    @Test
    fun freshInstallDoesNotInterruptOnboarding() {
        assertFalse(shouldShowWhatsNew(0, 10, isUpdatedInstall = false))
    }

    @Test
    fun existingInstallWithoutMarkerShowsAfterUpdate() {
        assertTrue(shouldShowWhatsNew(0, 10, isUpdatedInstall = true))
    }

    @Test
    fun newerVersionShowsOnlyOnce() {
        assertTrue(shouldShowWhatsNew(9, 10, isUpdatedInstall = true))
        assertFalse(shouldShowWhatsNew(10, 10, isUpdatedInstall = true))
        assertFalse(shouldShowWhatsNew(11, 10, isUpdatedInstall = true))
    }
}

