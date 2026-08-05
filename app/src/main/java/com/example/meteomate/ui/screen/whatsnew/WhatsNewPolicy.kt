package com.example.meteomate.ui.screen.whatsnew

fun shouldShowWhatsNew(
    lastSeenVersionCode: Int,
    currentVersionCode: Int,
    isUpdatedInstall: Boolean
): Boolean =
    currentVersionCode > lastSeenVersionCode &&
        (lastSeenVersionCode > 0 || isUpdatedInstall)

