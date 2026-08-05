package com.example.meteomate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.meteomate.ui.screen.MainPagerScreen
import com.example.meteomate.ui.screen.settings.SettingsScreen
import com.example.meteomate.ui.screen.whatsnew.WhatsNewScreen
import com.example.meteomate.ui.screen.whatsnew.WhatsNewViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    whatsNewViewModel: WhatsNewViewModel = hiltViewModel()
) {
    val startupState by whatsNewViewModel.startupState.collectAsState()
    if (!startupState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val startDestination = remember {
        if (startupState.shouldShowOnStart) Screen.WhatsNewAuto.route else Screen.Main.route
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Main.route) {
            MainPagerScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowWhatsNew = { navController.navigate(Screen.WhatsNew.route) }
            )
        }
        composable(Screen.WhatsNewAuto.route) {
            LaunchedEffect(Unit) { whatsNewViewModel.markCurrentVersionShown() }
            WhatsNewScreen(
                automatic = true,
                onClose = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.WhatsNewAuto.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.WhatsNew.route) {
            WhatsNewScreen(
                automatic = false,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
