package com.example.meteomate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String = "",
    val icon: ImageVector? = null
) {
    data object Main : Screen("main", "MeteoMate", null)
    data object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
    data object WhatsNewAuto : Screen("whats_new_auto", "Что изменилось?", null)
    data object WhatsNew : Screen("whats_new", "Что изменилось?", null)
}
