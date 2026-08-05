package com.example.meteomate.ui.screen.wind

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.meteomate.R
import com.example.meteomate.data.WindModel
import com.example.meteomate.ui.screen.HourlyForecastItem
import com.example.meteomate.ui.theme.ClearDayBottom
import com.example.meteomate.ui.theme.ClearDayTop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    selectedModel: WindModel = WindModel.GFS27,
    windSpeed: Double = 0.0,
    windDeg: Int? = null,
    windGust: Double? = null,
    windHourlyForecast: List<HourlyForecastItem> = emptyList(),
    onModelSelected: (WindModel) -> Unit = {}
) {
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.tab_wind)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
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
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                WindContent(
                    selectedModel = selectedModel,
                    windSpeed = windSpeed,
                    windDeg = windDeg,
                    windGust = windGust,
                    windHourlyForecast = windHourlyForecast,
                    onModelSelected = onModelSelected,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}
