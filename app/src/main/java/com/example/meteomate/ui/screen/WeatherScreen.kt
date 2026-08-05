package com.example.meteomate.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meteomate.R
import com.example.meteomate.data.DisplayMode
import com.example.meteomate.data.CardOrder
import com.example.meteomate.data.local.FavoriteCity
import com.example.meteomate.data.local.RecentSearch
import com.example.meteomate.data.model.GeocodingResponse
import com.example.meteomate.ui.component.DetailGrid
import com.example.meteomate.ui.component.ForecastSection
import com.example.meteomate.ui.component.ForecastRecordsCard
import com.example.meteomate.ui.component.MoonPhaseSection
import com.example.meteomate.ui.component.SunArcGraph
import com.example.meteomate.ui.component.SolarFeaturesCard
import com.example.meteomate.ui.component.WeatherCompanionHero
import com.example.meteomate.ui.component.HourlyForecastRow
import com.example.meteomate.ui.component.HealthEnvironmentSection
import com.example.meteomate.ui.component.WeatherBackgroundAnimation
import com.example.meteomate.ui.component.WeatherFactsSection
import com.example.meteomate.ui.component.WeatherLoadingAnimation
import com.example.meteomate.ui.component.WindVisualization
import com.example.meteomate.util.WeatherGradients
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onNavigateToWind: () -> Unit = {},
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val favoriteCities by viewModel.favoriteCities.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var followDeviceLocation by remember { mutableStateOf(true) }
    var hasLocationPermission by remember { mutableStateOf(hasAnyLocationPermission(context)) }

    BackHandler(enabled = isSearchFocused) {
        isSearchFocused = false
        searchQuery = ""
        viewModel.clearSearch()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            followDeviceLocation = true
        } else {
            viewModel.setError("Разрешение на местоположение отклонено. Поищите город выше для получения погоды.")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            try {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } catch (_: Exception) {
                viewModel.setError("Не удалось запросить разрешение на местоположение")
            }
        }
    }

    LaunchedEffect(hasLocationPermission, followDeviceLocation) {
        if (hasLocationPermission && followDeviceLocation) {
            while (true) {
                fetchLocationSafe(context, viewModel)
                kotlinx.coroutines.delay(LOCATION_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    val currentState = (uiState as? WeatherUiState.Success)?.state
    val weatherCode = currentState?.weatherCode ?: 800

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WeatherGradients.backgroundBrush(weatherCode))
        )

        if (settings.weatherAnimationsEnabled) {
            WeatherBackgroundAnimation(
                weatherCode = weatherCode,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            GlassSearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.searchCities(it)
                    isSearchFocused = true
                },
                onClear = {
                    searchQuery = ""
                    viewModel.clearSearch()
                },
                onFocus = { isSearchFocused = true },
                onDismiss = {
                    isSearchFocused = false
                    searchQuery = ""
                    viewModel.clearSearch()
                },
                expanded = isSearchFocused,
            )

            if (isSearchFocused) {
                if (searchQuery.length >= 2) {
                    SearchResultsList(
                        results = searchResults,
                        onCitySelected = { result ->
                            followDeviceLocation = false
                            isSearchFocused = false
                            searchQuery = ""
                            viewModel.clearSearch()
                            val name = listOfNotNull(result.localizedName(), result.state, result.country)
                                .joinToString(", ")
                            viewModel.loadWeather(result.lat, result.lon, name, result.hashCode().toLong())
                        },
                        isFavorite = { lat, lon -> viewModel.isCityFavorite(lat, lon) }
                    )
                } else {
                    FavoritesAndRecentList(
                        favorites = favoriteCities,
                        recents = recentSearches,
                        onCitySelected = { name, lat, lon ->
                            followDeviceLocation = false
                            isSearchFocused = false
                            searchQuery = ""
                            viewModel.loadWeather(lat, lon, name)
                        },
                        onRemoveFavorite = { viewModel.toggleFavoriteCity(it) }
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        scope.launch {
                            try {
                                if (followDeviceLocation && hasLocationPermission) {
                                    fetchLocationSafe(context, viewModel)
                                } else {
                                    viewModel.retry()
                                }
                            } finally {
                                isRefreshing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val state = uiState) {
                        is WeatherUiState.Loading -> {
                            WeatherLoadingAnimation(accentColor = MaterialTheme.colorScheme.onBackground)
                        }

                        is WeatherUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.retry() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Text(stringResource(R.string.retry), color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }

                        is WeatherUiState.Success -> {
                            var citySwipeDistance by remember { mutableStateOf(0f) }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                item {
                                    WeatherCompanionHero(
                                        temperature = state.state.temperature,
                                        feelsLike = state.state.feelsLike,
                                        weatherCode = state.state.weatherCode,
                                        weatherDescription = state.state.weatherDescription,
                                        locationName = state.state.locationName,
                                        humidity = state.state.humidity,
                                        windSpeed = state.state.windSpeed,
                                        precipitationProbability = state.state.hourlyForecast.firstOrNull()?.precipitationProbability ?: 0.0,
                                        isDay = state.state.hourlyForecast.firstOrNull { it.isCurrent }?.isDay
                                            ?: state.state.hourlyForecast.firstOrNull()?.isDay
                                            ?: true,
                                        temperatureUnit = settings.temperatureUnit,
                                        isFavorite = state.state.isFavorite,
                                        onToggleFavorite = { viewModel.toggleFavorite() },
                                        modifier = Modifier.pointerInput(favoriteCities) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { citySwipeDistance = 0f },
                                                onHorizontalDrag = { _, amount -> citySwipeDistance += amount },
                                                onDragEnd = {
                                                    when {
                                                        citySwipeDistance < -80f -> viewModel.switchFavoriteCity(1)
                                                        citySwipeDistance > 80f -> viewModel.switchFavoriteCity(-1)
                                                    }
                                                    citySwipeDistance = 0f
                                                }
                                            )
                                        }
                                    )
                                }

                                item {
                                    val updatedTime = remember(state.state.lastUpdated) {
                                        SimpleDateFormat("HH:mm", Locale.getDefault())
                                            .format(Date(state.state.lastUpdated))
                                    }
                                    Text(
                                        text = buildString {
                                            append(stringResource(R.string.updated_at, updatedTime))
                                            if (state.state.isOffline) append(" · офлайн")
                                            if (System.currentTimeMillis() - state.state.lastUpdated > 30 * 60 * 1000L) {
                                                append(" · данные устарели")
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(28.dp))
                                }

                                if (
                                    settings.displayMode == DisplayMode.DETAILED &&
                                    settings.cardOrder == CardOrder.DETAILS_FIRST
                                ) {
                                    item {
                                        DetailGrid(
                                            feelsLike = state.state.feelsLike,
                                            humidity = state.state.humidity,
                                            windSpeed = state.state.windSpeed,
                                            windDeg = state.state.windDeg,
                                            pressure = state.state.pressure,
                                            visibility = state.state.visibility,
                                            cloudCoverage = state.state.cloudCoverage,
                                            sunrise = state.state.sunrise,
                                            sunset = state.state.sunset,
                                            timezoneOffsetSeconds = state.state.timezoneOffsetSeconds,
                                            pressureUnit = settings.pressureUnit,
                                            temperatureUnit = settings.temperatureUnit,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                }

                                item {
                                    HourlyForecastRow(
                                        forecast = state.state.hourlyForecast,
                                        precipitationNowcast = state.state.precipitationNowcast,
                                        timezoneOffsetSeconds = state.state.timezoneOffsetSeconds,
                                        temperatureUnit = settings.temperatureUnit
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (state.state.dailyForecast.isNotEmpty()) {
                                    item {
                                        ForecastSection(
                                            forecast = state.state.dailyForecast,
                                            temperatureUnit = settings.temperatureUnit,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                    item {
                                        ForecastRecordsCard(
                                            forecast = state.state.dailyForecast,
                                            temperatureUnit = settings.temperatureUnit,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (
                                    settings.displayMode == DisplayMode.DETAILED &&
                                    settings.cardOrder == CardOrder.FORECAST_FIRST
                                ) {
                                    item {
                                        DetailGrid(
                                        feelsLike = state.state.feelsLike,
                                        humidity = state.state.humidity,
                                        windSpeed = state.state.windSpeed,
                                        windDeg = state.state.windDeg,
                                        pressure = state.state.pressure,
                                        visibility = state.state.visibility,
                                        cloudCoverage = state.state.cloudCoverage,
                                        sunrise = state.state.sunrise,
                                        sunset = state.state.sunset,
                                        timezoneOffsetSeconds = state.state.timezoneOffsetSeconds,
                                        pressureUnit = settings.pressureUnit,
                                        temperatureUnit = settings.temperatureUnit,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                item {
                                    HealthEnvironmentSection(
                                        temperature = state.state.temperature,
                                        feelsLike = state.state.feelsLike,
                                        humidity = state.state.humidity,
                                        windSpeed = state.state.windSpeed,
                                        precipitationProbability = state.state.hourlyForecast.firstOrNull()?.precipitationProbability ?: 0.0,
                                        visibilityMeters = state.state.visibility,
                                        uvIndex = state.state.uvIndex,
                                        airQuality = state.state.airQuality,
                                        geomagnetic = state.state.geomagnetic,
                                        yesterdayAverageTemperature = state.state.yesterdayAverageTemperature,
                                        history = state.state.hourlyHistory,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                item {
                                    SolarFeaturesCard(
                                        uvIndex = state.state.uvIndex,
                                        sunrise = state.state.sunrise,
                                        sunset = state.state.sunset,
                                        timezoneOffsetSeconds = state.state.timezoneOffsetSeconds,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (settings.displayMode == DisplayMode.DETAILED) {
                                    item {
                                        WindVisualization(
                                        windSpeed = state.state.windSpeed,
                                        windDeg = state.state.windDeg,
                                        windGust = state.state.windGust,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (settings.displayMode == DisplayMode.DETAILED) {
                                    item {
                                        SunArcGraph(
                                        sunrise = state.state.sunrise,
                                        sunset = state.state.sunset,
                                        timezoneOffsetSeconds = state.state.timezoneOffsetSeconds,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (settings.displayMode == DisplayMode.DETAILED) {
                                    item {
                                        MoonPhaseSection(
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (settings.displayMode == DisplayMode.DETAILED) {
                                    item {
                                        WeatherFactsSection(
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesAndRecentList(
    favorites: List<FavoriteCity>,
    recents: List<RecentSearch>,
    onCitySelected: (String, Double, Double) -> Unit,
    onRemoveFavorite: (FavoriteCity) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF01A2A48),
        shadowElevation = 12.dp
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (favorites.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.favorites),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
            items(favorites) { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCitySelected(city.name, city.lat, city.lon) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        if (city.country != null) {
                            Text(
                                text = city.country,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                    }
                    IconButton(onClick = { onRemoveFavorite(city) }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = stringResource(R.string.remove_favorite),
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (recents.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.recent),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
            items(recents) { recent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCitySelected(recent.name, recent.lat, recent.lon) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = recent.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        if (recent.country != null) {
                            Text(
                                text = recent.country,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                    }
                }
            }
        }

        if (favorites.isEmpty() && recents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_city),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onFocus: () -> Unit,
    onDismiss: () -> Unit,
    expanded: Boolean
) {
    val shape = RoundedCornerShape(14.dp)
    val isActive = query.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.12f),
                        Color(0xFFE0F0FF).copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.18f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(800f, 800f)
                )
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = shape
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).onFocusChanged { state ->
                    if (state.isFocused) onFocus()
                },
                placeholder = {
                    Text(
                        stringResource(R.string.search_city),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (isActive || expanded) {
                        IconButton(onClick = { if (isActive) onClear() else onDismiss() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.onBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<GeocodingResponse>,
    onCitySelected: (GeocodingResponse) -> Unit,
    isFavorite: (Double, Double) -> Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_results),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCitySelected(result) }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.localizedName(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            val subtitle = listOfNotNull(result.state, result.country)
                                .joinToString(", ")
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                        if (isFavorite(result.lat, result.lon)) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.favorite),
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val LOCATION_REFRESH_INTERVAL_MILLIS = 10L * 60L * 1000L
private const val LOCATION_REQUEST_TIMEOUT_MILLIS = 20_000L
private const val MAX_LOCATION_AGE_MILLIS = 30_000L

private fun hasAnyLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private suspend fun geocodeLocation(context: Context, lat: Double, lng: Double): String {
    val geocoder = Geocoder(context, Locale("ru"))
    return withTimeout(5000) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(lat, lng, 5) { addresses ->
                    if (continuation.isActive) {
                        val address = addresses.firstOrNull { address ->
                            !address.locality.isNullOrBlank() || !address.subLocality.isNullOrBlank()
                        } ?: addresses.firstOrNull()
                        val name = address?.let { addr ->
                            listOfNotNull(
                                addr.locality ?: addr.subLocality ?: addr.subAdminArea ?: addr.adminArea,
                                addr.countryName
                            ).joinToString(", ")
                        } ?: ""
                        continuation.resume(name)
                    }
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 5)
                val address = addresses?.firstOrNull { address ->
                    !address.locality.isNullOrBlank() || !address.subLocality.isNullOrBlank()
                } ?: addresses?.firstOrNull()
                val name = address?.let { addr ->
                    listOfNotNull(
                        addr.locality ?: addr.subLocality ?: addr.subAdminArea ?: addr.adminArea,
                        addr.countryName
                    ).joinToString(", ")
                } ?: ""
                name
            }
        }
    }
}

private suspend fun fetchLocationSafe(context: Context, viewModel: WeatherViewModel) {
    try {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) {
            viewModel.reportLocationUnavailable("Нет доступа к местоположению. Выберите город в поиске.")
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val priority = if (hasFineLocation) Priority.PRIORITY_HIGH_ACCURACY
            else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val granularity = if (hasFineLocation) Granularity.GRANULARITY_FINE
            else Granularity.GRANULARITY_COARSE
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setGranularity(granularity)
            .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MILLIS)
            .setDurationMillis(LOCATION_REQUEST_TIMEOUT_MILLIS)
            .build()
        val location = awaitCurrentLocation(fusedLocationClient, request)
        if (location == null) {
            viewModel.reportLocationUnavailable("Не удалось получить свежие координаты. Проверьте геолокацию.")
            return
        }
        val cityName = try {
            geocodeLocation(context, location.latitude, location.longitude)
        } catch (_: Exception) {
            ""
        }
        currentCoroutineContext().ensureActive()
        viewModel.loadWeather(location.latitude, location.longitude, cityName)
    } catch (_: Exception) {
        viewModel.reportLocationUnavailable("Не удалось определить местоположение. Повторите попытку.")
    }
}

@SuppressLint("MissingPermission")
private suspend fun awaitCurrentLocation(
    client: com.google.android.gms.location.FusedLocationProviderClient,
    request: CurrentLocationRequest
): Location? = withTimeoutOrNull(LOCATION_REQUEST_TIMEOUT_MILLIS + 1_000L) {
    suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(request, cancellation.token)
            .addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.cancel(error)
            }
    }
}
