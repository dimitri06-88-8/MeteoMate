package com.example.meteomate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meteomate.R
import com.example.meteomate.ui.screen.wind.WindContent
import com.example.meteomate.ui.screen.observations.WeatherObservationsScreen
import com.example.meteomate.ui.theme.ClearDayBottom
import com.example.meteomate.ui.theme.ClearDayTop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPagerScreen(
    onNavigateToSettings: () -> Unit = {},
    weatherViewModel: WeatherViewModel = hiltViewModel()
) {
    val tabs = listOf(
        stringResource(R.string.tab_weather),
        stringResource(R.string.tab_wind),
        stringResource(R.string.tab_observations)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val uiState by weatherViewModel.uiState.collectAsState()
    val observationProgress by weatherViewModel.observationProgress.collectAsState()
    val observationFeedback by weatherViewModel.observationFeedback.collectAsState()

    val isWeatherReady = uiState is WeatherUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeteoMate") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState is WeatherUiState.Success) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text = title,
                                    color = when (pagerState.currentPage) {
                                        index -> MaterialTheme.colorScheme.onBackground
                                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> WeatherScreen(
                        onNavigateToWind = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        viewModel = weatherViewModel
                    )
                    1 -> {
                        if (isWeatherReady) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                ClearDayTop,
                                                ClearDayTop.copy(alpha = 0.85f),
                                                ClearDayBottom.copy(alpha = 0.9f),
                                                ClearDayBottom
                                            )
                                        )
                                    )
                            ) {
                                val state = (uiState as? WeatherUiState.Success)?.state
                                val settings by weatherViewModel.settings.collectAsState()
                                WindContent(
                                    selectedModel = state?.windModel ?: settings.selectedWindModel,
                                    windSpeed = state?.windSpeed ?: 0.0,
                                    windDeg = state?.windDeg,
                                    windGust = state?.windGust,
                                    windHourlyForecast = state?.windHourlyForecast ?: emptyList(),
                                    modelComparison = state?.modelComparison ?: emptyList(),
                                    alert10 = settings.alert10Enabled,
                                    alert15 = settings.alert15Enabled,
                                    alert20 = settings.alert20Enabled,
                                    onModelSelected = weatherViewModel::selectWindModel,
                                    onAlert10Toggle = { weatherViewModel.toggleAlert10() },
                                    onAlert15Toggle = { weatherViewModel.toggleAlert15() },
                                    onAlert20Toggle = { weatherViewModel.toggleAlert20() },
                                    onNavigateToSettings = onNavigateToSettings
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                ClearDayTop,
                                                ClearDayTop.copy(alpha = 0.85f),
                                                ClearDayBottom.copy(alpha = 0.9f),
                                                ClearDayBottom
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.loading_weather),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    2 -> {
                        val state = (uiState as? WeatherUiState.Success)?.state
                        if (state != null) {
                            WeatherObservationsScreen(
                                progress = observationProgress,
                                locationName = state.locationName,
                                weatherDescription = state.weatherDescription,
                                feedback = observationFeedback,
                                onSubmit = weatherViewModel::submitWeatherObservation
                            )
                        }
                    }
                }
            }
        }
    }
}
