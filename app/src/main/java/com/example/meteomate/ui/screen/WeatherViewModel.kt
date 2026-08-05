package com.example.meteomate.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meteomate.data.SettingsDataStore
import com.example.meteomate.data.WeatherObservationKind
import com.example.meteomate.data.WeatherObservationProgress
import com.example.meteomate.data.WEATHER_OBSERVATION_COOLDOWN_MILLIS
import com.example.meteomate.data.WindModel
import com.example.meteomate.data.local.CachedWeather
import com.example.meteomate.data.local.FavoriteCity
import com.example.meteomate.data.local.RecentSearch
import com.example.meteomate.data.local.WeatherDao
import com.example.meteomate.data.model.GeocodingResponse
import com.example.meteomate.data.model.GeomagneticSnapshot
import com.example.meteomate.data.model.OpenMeteoWindResponse
import com.example.meteomate.data.repository.WeatherRepository
import com.example.meteomate.util.DateUtils
import com.example.meteomate.util.OpenMeteoWeatherCode
import com.example.meteomate.util.Resource
import com.example.meteomate.util.buildGeomagneticSnapshot
import com.example.meteomate.work.notification.GoldenHourScheduler
import com.google.gson.Gson
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import javax.inject.Inject

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val state: WeatherState) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

data class WeatherState(
    val cacheSchemaVersion: Int = 4,
    val temperature: Double = 0.0,
    val feelsLike: Double = 0.0,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val windDeg: Int? = null,
    val windGust: Double? = null,
    val pressure: Int = 0,
    val weatherCode: Int = 800,
    val weatherDescription: String = "",
    val locationName: String = "",
    val cityId: Long = 0,
    val sunrise: Long = 0,
    val sunset: Long = 0,
    val visibility: Int? = null,
    val cloudCoverage: Int? = null,
    val country: String = "",
    val dailyForecast: List<DailyForecastItem> = emptyList(),
    val hourlyForecast: List<HourlyForecastItem> = emptyList(),
    val precipitationNowcast: List<PrecipitationNowcastItem> = emptyList(),
    val windHourlyForecast: List<HourlyForecastItem> = emptyList(),
    val modelComparison: List<ModelComparisonItem> = emptyList(),
    val hourlyHistory: List<HourlyForecastItem> = emptyList(),
    val airQuality: AirQualitySnapshot? = null,
    val geomagnetic: GeomagneticSnapshot? = null,
    val uvIndex: Double? = null,
    val yesterdayAverageTemperature: Double? = null,
    val isFavorite: Boolean = false,
    val windModel: WindModel = WindModel.GFS27,
    val lastUpdated: Long = System.currentTimeMillis(),
    val timezoneOffsetSeconds: Int = 0,
    val isOffline: Boolean = false,
    val dataSource: String = "OpenWeather + Open-Meteo"
)

data class AirQualitySnapshot(
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val measuredAt: String = ""
)

data class ModelComparisonItem(
    val model: WindModel,
    val windSpeed: Double,
    val windGust: Double?,
    val windDeg: Int?
)

data class DailyForecastItem(
    val day: String,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitationProbability: Double,
    val maxWindSpeed: Double
)

data class HourlyForecastItem(
    val time: String,
    val date: String = "",
    val temperature: Double,
    val apparentTemperature: Double = temperature,
    val weatherCode: Int,
    val precipitationProbability: Double = 0.0,
    val precipitation: Double = 0.0,
    val windSpeed: Double,
    val windDeg: Int?,
    val windGust: Double?,
    val pressure: Int,
    val isDay: Boolean = true,
    val isCurrent: Boolean = false,
    val intervalHours: Int = 1
)

data class PrecipitationNowcastItem(
    val time: String,
    val date: String = "",
    val precipitation: Double
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val settingsDataStore: SettingsDataStore,
    private val weatherDao: WeatherDao,
    private val goldenHourScheduler: GoldenHourScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResponse>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResponse>> = _searchResults.asStateFlow()

    private val _favoriteCities = MutableStateFlow<List<FavoriteCity>>(emptyList())
    val favoriteCities: StateFlow<List<FavoriteCity>> = _favoriteCities.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearch>> = _recentSearches.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    val settings = settingsDataStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.meteomate.data.AppSettings()
    )

    val observationProgress = settingsDataStore.weatherObservationProgress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeatherObservationProgress()
    )

    private val _observationFeedback = MutableStateFlow<String?>(null)
    val observationFeedback: StateFlow<String?> = _observationFeedback.asStateFlow()


    private val gson = Gson()

    private var loadJob: kotlinx.coroutines.Job? = null
    private var windLoadJob: kotlinx.coroutines.Job? = null
    private var modelComparisonJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            try {
                val lat = settingsDataStore.lastCityLat.first()
                val lon = settingsDataStore.lastCityLon.first()
                if (lat != null && lon != null) {
                    val name = settingsDataStore.lastCityName.first() ?: ""
                    val id = settingsDataStore.lastCityId.first() ?: 0
                    loadWeather(lat, lon, name, id)
                } else {
                    _uiState.value = WeatherUiState.Error(
                        "Разрешите доступ к местоположению или выберите город в поиске"
                    )
                }
            } catch (_: Exception) {
                _uiState.value = WeatherUiState.Error(
                    "Не удалось восстановить последний город. Выберите город в поиске"
                )
            }
        }
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _favoriteCities.value = weatherDao.getFavoriteCities()
            _recentSearches.value = weatherDao.getRecentSearches()
        }
    }

    fun reportLocationUnavailable(message: String) {
        if (_currentLocation.value == null) {
            _uiState.value = WeatherUiState.Error(message)
        }
    }

    fun submitWeatherObservation(kind: WeatherObservationKind) {
        val weather = (_uiState.value as? WeatherUiState.Success)?.state ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - observationProgress.value.lastReportAt
        if (elapsed in 0 until WEATHER_OBSERVATION_COOLDOWN_MILLIS) {
            val hoursLeft = ((WEATHER_OBSERVATION_COOLDOWN_MILLIS - elapsed + 3_599_999L) / 3_600_000L)
            _observationFeedback.value = "Следующее наблюдение можно сохранить через $hoursLeft ч."
            return
        }
        viewModelScope.launch {
            val secretWasAlreadyUnlocked = observationProgress.value.secretBadgeUnlocked
            val unlockSecret = kind == WeatherObservationKind.THUNDERSTORM && weather.weatherCode in 200..232
            val newTotal = observationProgress.value.totalReports + 1
            settingsDataStore.recordWeatherObservation(kind, weather.locationName, unlockSecret)
            _observationFeedback.value = when {
                unlockSecret && !secretWasAlreadyUnlocked -> "Секретная награда открыта: Резонанс атмосферы!"
                newTotal == 1 -> "Открыта награда «Первый сигнал»!"
                newTotal == 7 -> "Открыта награда «Разведчик неба»!"
                newTotal == 20 -> "Открыта награда «Штормовой кристалл»!"
                else -> "Наблюдение сохранено. Спасибо!"
            }
            delay(4_000)
            _observationFeedback.value = null
        }
    }


    fun loadWeather(
        lat: Double,
        lon: Double,
        locationName: String = "",
        cityId: Long = 0,
        forceRefresh: Boolean = false
    ) {
        val previousLocation = _currentLocation.value
        val previousState = (_uiState.value as? WeatherUiState.Success)?.state
        if (
            !forceRefresh && previousLocation != null && previousState != null &&
            kotlin.math.abs(previousLocation.first - lat) < 0.0005 &&
            kotlin.math.abs(previousLocation.second - lon) < 0.0005 &&
            (locationName.isBlank() || locationName == previousState.locationName) &&
            !previousState.isOffline &&
            System.currentTimeMillis() - previousState.lastUpdated < MIN_REFRESH_INTERVAL_MILLIS
        ) return
        _currentLocation.value = lat to lon
        loadJob?.cancel()
        windLoadJob?.cancel()
        modelComparisonJob?.cancel()
        loadJob = viewModelScope.launch {
            val appSettings = settingsDataStore.settings.first()
            val cachedEntity = weatherDao.getCachedWeatherByLocation(lat, lon)
            val cachedState = cachedEntity?.let { cached ->
                try {
                    val storedVersion = JsonParser.parseString(cached.jsonData)
                        .asJsonObject
                        .get("cacheSchemaVersion")
                        ?.asInt
                    if (storedVersion == CACHE_SCHEMA_VERSION) {
                        gson.fromJson(cached.jsonData, WeatherState::class.java)
                            ?.let(::sanitizeCachedState)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (cachedState != null) {
                _uiState.value = WeatherUiState.Success(
                    cachedState.copy(
                        isFavorite = weatherDao.isFavorite(lat, lon),
                        windModel = appSettings.selectedWindModel,
                        isOffline = true
                    )
                )
            } else {
                _uiState.value = WeatherUiState.Loading
            }

            val currentDeferred = async { repository.getCurrentWeather(lat, lon) }
            val forecastDeferred = async { repository.getForecast(lat, lon) }
            val hourlyDeferred = async { repository.getHourlyForecast(lat, lon) }
            val airQualityDeferred = async { repository.getAirQuality(lat, lon) }
            val geomagneticDeferred = async { repository.getGeomagneticForecast() }
            val currentResult = currentDeferred.await()
            val forecastResult = forecastDeferred.await()
            val hourlyResult = hourlyDeferred.await()
            val airQualityResult = airQualityDeferred.await()
            val geomagneticResult = geomagneticDeferred.await()

            val openMeteo = (hourlyResult as? Resource.Success)?.data
            val openWeatherCurrent = (currentResult as? Resource.Success)?.data
            val current = openMeteo?.let {
                buildOpenMeteoFallbackWeather(it, lat, lon, locationName, cityId, cachedState)
            } ?: openWeatherCurrent

            if (current != null) {
                val forecast = (forecastResult as? Resource.Success)?.data
                val dailyItems = openMeteo?.let(::mapOpenMeteoDaily).orEmpty().ifEmpty {
                    forecast?.let(::aggregateForecast) ?: cachedState?.dailyForecast.orEmpty()
                }
                val fallbackHourlyItems = forecast?.list?.take(16)?.map { item ->
                    val localDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(item.dt),
                        ZoneOffset.ofTotalSeconds(forecast.city.timezone)
                    )
                    HourlyForecastItem(
                        time = localDateTime.format(FALLBACK_TIME_FORMATTER),
                        date = localDateTime.toLocalDate().toString(),
                        temperature = item.main.temp,
                        apparentTemperature = item.main.feelsLike,
                        weatherCode = item.weather.firstOrNull()?.id ?: 800,
                        precipitationProbability = item.precipitationProbability * 100.0,
                        windSpeed = item.wind.speed,
                        windDeg = item.wind.deg,
                        windGust = item.wind.gust,
                        pressure = item.main.pressure,
                        isDay = localDateTime.hour in 6..21,
                        isCurrent = false,
                        intervalHours = 3
                    )
                }.orEmpty()
                val hourlyItems = openMeteo?.let {
                    mapOpenMeteoHourly(it, current)
                }.orEmpty().ifEmpty {
                    cachedState?.hourlyForecast.orEmpty().ifEmpty { fallbackHourlyItems }
                }
                val precipitationNowcast = openMeteo?.let {
                    mapPrecipitationNowcast(it, current)
                }.orEmpty()
                val windHourlyItems = fallbackHourlyItems.ifEmpty {
                    cachedState?.windHourlyForecast.orEmpty()
                }
                val hourlyHistory = openMeteo?.let {
                    mapOpenMeteoHistory(it, current)
                }.orEmpty().ifEmpty { cachedState?.hourlyHistory.orEmpty() }
                val currentLocalHourIndex = openMeteo?.hourly?.time?.indexOfFirst {
                    isCurrentHour(it, openMeteo.utcOffsetSeconds ?: current.timezone)
                } ?: -1
                val uvIndex = openMeteo?.hourly?.uvIndex?.getOrNull(currentLocalHourIndex)
                    ?: cachedState?.uvIndex
                val airQuality = (airQualityResult as? Resource.Success)?.data?.current?.let { air ->
                    val aqi = air.usAqi ?: return@let null
                    AirQualitySnapshot(
                        aqi = aqi,
                        pm25 = air.pm25 ?: 0.0,
                        pm10 = air.pm10 ?: 0.0,
                        measuredAt = air.time.orEmpty()
                    )
                } ?: cachedState?.airQuality
                val geomagnetic = (geomagneticResult as? Resource.Success)?.data
                    ?.let(::buildGeomagneticSnapshot)
                    ?: cachedState?.geomagnetic

                val effectiveCityId = if (cityId != 0L) cityId else current.id
                val isFav = weatherDao.isFavorite(lat, lon)
                val state = WeatherState(
                    temperature = current.main.temp,
                    feelsLike = current.main.feelsLike,
                    humidity = current.main.humidity,
                    windSpeed = current.wind.speed,
                    windDeg = current.wind.deg,
                    windGust = current.wind.gust,
                    pressure = current.main.pressure,
                    weatherCode = current.weather.firstOrNull()?.id ?: 800,
                    weatherDescription = current.weather.firstOrNull()?.description ?: "",
                    locationName = locationName.ifEmpty {
                        listOfNotNull(current.name, current.sys.country)
                            .joinToString(", ")
                    },
                    cityId = effectiveCityId,
                    sunrise = current.sys.sunrise,
                    sunset = current.sys.sunset,
                    visibility = current.visibility,
                    cloudCoverage = current.clouds?.all,
                    country = current.sys.country ?: "",
                    dailyForecast = dailyItems,
                    hourlyForecast = hourlyItems,
                    precipitationNowcast = precipitationNowcast,
                    windHourlyForecast = windHourlyItems,
                    hourlyHistory = hourlyHistory,
                    airQuality = airQuality,
                    geomagnetic = geomagnetic,
                    uvIndex = uvIndex,
                    yesterdayAverageTemperature = hourlyHistory
                        .map { it.temperature }
                        .takeIf { it.isNotEmpty() }
                        ?.average(),
                    isFavorite = isFav,
                    windModel = appSettings.selectedWindModel,
                    lastUpdated = System.currentTimeMillis(),
                    timezoneOffsetSeconds = openMeteo?.utcOffsetSeconds
                        ?: forecast?.city?.timezone
                        ?: current.timezone,
                    isOffline = false,
                    dataSource = if (openMeteo != null) {
                        "Open-Meteo + NOAA SWPC"
                    } else "Резервный источник OpenWeather + NOAA SWPC"
                )

                weatherDao.cacheWeather(
                    CachedWeather(
                        cityId = effectiveCityId,
                        cityName = locationName.ifEmpty { current.name },
                        lat = lat,
                        lon = lon,
                        jsonData = gson.toJson(state)
                    )
                )

                settingsDataStore.saveLastCity(effectiveCityId, locationName.ifEmpty { current.name }, lat, lon)
                if (appSettings.goldenHourNotificationsEnabled && openMeteo != null) {
                    goldenHourScheduler.schedule(openMeteo, state.locationName)
                }
                weatherDao.addRecentSearch(
                    RecentSearch(
                        name = locationName.ifEmpty { current.name },
                        lat = lat,
                        lon = lon,
                        country = current.sys.country,
                        lastSearched = System.currentTimeMillis()
                    )
                )

                _uiState.value = WeatherUiState.Success(state)
                loadFavorites()
                loadWindFromOpenMeteo(lat, lon, appSettings.selectedWindModel)
                loadModelComparison()
            } else if (cachedState == null) {
                    val errorMsg = when {
                        currentResult is Resource.Error -> currentResult.message
                        else -> "Неизвестная ошибка"
                    }
                    _uiState.value = WeatherUiState.Error(errorMsg)
            } else {
                loadWindFromOpenMeteo(lat, lon, appSettings.selectedWindModel)
            }
        }
    }

    private fun mapOpenMeteoHourly(
        data: OpenMeteoWindResponse,
        current: com.example.meteomate.data.model.WeatherResponse
    ): List<HourlyForecastItem> {
        val cityNow = cityNow(data.utcOffsetSeconds ?: 0)
        return mapOpenMeteoItems(data, current)
            .filter { item ->
                parseForecastDateTime(item.date, item.time)?.let { it >= cityNow } != false
            }
            .take(HOURLY_FORECAST_HOURS)
    }

    private fun mapOpenMeteoHistory(
        data: OpenMeteoWindResponse,
        current: com.example.meteomate.data.model.WeatherResponse
    ): List<HourlyForecastItem> {
        val now = cityNow(data.utcOffsetSeconds ?: 0)
        return mapOpenMeteoItems(data, current)
            .filter { item -> parseForecastDateTime(item.date, item.time)?.let { it < now } == true }
            .takeLast(24)
    }

    private fun mapOpenMeteoItems(
        data: OpenMeteoWindResponse,
        current: com.example.meteomate.data.model.WeatherResponse
    ): List<HourlyForecastItem> {
        val hourly = data.hourly ?: return emptyList()
        return hourly.time.indices.mapNotNull { index ->
            val isoTime = hourly.time[index]
            if (isoTime.isBlank()) return@mapNotNull null
            val isCurrent = isCurrentHour(isoTime, data.utcOffsetSeconds ?: 0)
            HourlyForecastItem(
                time = DateUtils.formatTimeFromISO(isoTime),
                date = isoTime.substringBefore('T'),
                temperature = if (isCurrent) current.main.temp
                    else hourly.temperature.getOrNull(index) ?: current.main.temp,
                apparentTemperature = if (isCurrent) current.main.feelsLike
                    else hourly.apparentTemperature.getOrNull(index)
                        ?: hourly.temperature.getOrNull(index)
                        ?: current.main.feelsLike,
                weatherCode = if (isCurrent) {
                    current.weather.firstOrNull()?.id ?: 800
                } else OpenMeteoWeatherCode.toOpenWeather(hourly.weatherCode.getOrNull(index)),
                precipitationProbability = hourly.precipitationProbability
                    .getOrNull(index)?.toDouble() ?: 0.0,
                precipitation = hourly.precipitation.getOrNull(index) ?: 0.0,
                windSpeed = hourly.windSpeed.getOrNull(index) ?: current.wind.speed,
                windDeg = hourly.windDirection.getOrNull(index),
                windGust = hourly.windGusts.getOrNull(index),
                pressure = hourly.pressure.getOrNull(index)?.toInt() ?: current.main.pressure,
                isDay = hourly.isDay.getOrNull(index)?.let { it != 0 }
                    ?: fallbackIsDayFromIso(isoTime),
                isCurrent = isCurrent
            )
        }
    }

    private fun mapOpenMeteoDaily(data: OpenMeteoWindResponse): List<DailyForecastItem> {
        val daily = data.daily ?: return emptyList()
        val dates = daily.time.orEmpty()
        val today = cityNow(data.utcOffsetSeconds ?: 0).toLocalDate().toString()
        return dates.indices.mapNotNull { index ->
            val date = dates.getOrNull(index) ?: return@mapNotNull null
            if (date < today) return@mapNotNull null
            DailyForecastItem(
                day = date,
                weatherCode = OpenMeteoWeatherCode.toOpenWeather(
                    daily.weatherCode.orEmpty().getOrNull(index)
                ),
                maxTemp = daily.temperatureMax.orEmpty().getOrNull(index)
                    ?: return@mapNotNull null,
                minTemp = daily.temperatureMin.orEmpty().getOrNull(index)
                    ?: return@mapNotNull null,
                precipitationProbability =
                    (daily.precipitationProbabilityMax.orEmpty().getOrNull(index) ?: 0) / 100.0,
                maxWindSpeed = daily.windSpeedMax.orEmpty().getOrNull(index) ?: 0.0
            )
        }.take(7)
    }

    private fun mapPrecipitationNowcast(
        data: OpenMeteoWindResponse,
        current: com.example.meteomate.data.model.WeatherResponse
    ): List<PrecipitationNowcastItem> {
        val minutely = data.minutely15 ?: return emptyList()
        val items = minutely.time.indices.take(NOWCAST_INTERVALS).mapNotNull { index ->
            val isoTime = minutely.time[index]
            if (isoTime.isBlank()) return@mapNotNull null
            minutely.precipitation.getOrNull(index)?.let { precipitation ->
                PrecipitationNowcastItem(
                    time = DateUtils.formatTimeFromISO(isoTime),
                    date = isoTime.substringBefore('T'),
                    precipitation = precipitation
                )
            }
        }
        if (items.isEmpty() || items.any { it.precipitation > 0.0 }) return items

        val hourly = data.hourly
        val hourlyFallback = items.mapIndexed { index, item ->
            val hourlyAmount = hourly?.precipitation?.getOrNull(index / 4) ?: 0.0
            item.copy(precipitation = hourlyAmount / 4.0)
        }
        if (hourlyFallback.any { it.precipitation > 0.0 }) return hourlyFallback

        val currentAmount = listOfNotNull(
            data.current?.precipitation,
            data.current?.rain,
            data.current?.showers,
            data.current?.snowfall
        ).maxOrNull() ?: 0.0
        val currentCode = current.weather.firstOrNull()?.id ?: 800
        val weatherShowsPrecipitation = currentCode in 200..699
        val detectedAmount = currentAmount.takeIf { it > 0.0 }
            ?: if (weatherShowsPrecipitation) QUALITATIVE_PRECIPITATION_FALLBACK_MM else 0.0

        return items.mapIndexed { index, item ->
            if (index == 0) item.copy(precipitation = detectedAmount) else item
        }
    }

    private fun sanitizeCachedState(state: WeatherState): WeatherState {
        val cityNow = LocalDateTime.ofInstant(
            Instant.now(),
            ZoneOffset.ofTotalSeconds(state.timezoneOffsetSeconds)
        ).truncatedTo(ChronoUnit.HOURS)
        val remainingHours = state.hourlyForecast.mapNotNull { item ->
            val dateTime = parseForecastDateTime(item.date, item.time)
                ?: return@mapNotNull item.copy(isCurrent = false)
            if (dateTime < cityNow) {
                null
            } else {
                item.copy(isCurrent = item.intervalHours == 1 && dateTime == cityNow)
            }
        }
        return state.copy(
            hourlyForecast = remainingHours,
            precipitationNowcast = emptyList()
        )
    }

    private fun isCurrentHour(isoTime: String, offsetSeconds: Int): Boolean {
        val forecastTime = parseIsoDateTime(isoTime) ?: return false
        return forecastTime.truncatedTo(ChronoUnit.HOURS) == cityNow(offsetSeconds)
    }

    private fun cityNow(offsetSeconds: Int): LocalDateTime = LocalDateTime.ofInstant(
        Instant.now(),
        ZoneOffset.ofTotalSeconds(offsetSeconds)
    ).truncatedTo(ChronoUnit.HOURS)

    private fun fallbackIsDayFromIso(isoTime: String): Boolean =
        parseIsoDateTime(isoTime)?.hour?.let { it in 6..21 } ?: true

    private fun parseForecastDateTime(date: String, time: String): LocalDateTime? = try {
        LocalDateTime.parse("${date}T$time", ISO_DATE_TIME_FORMATTER)
    } catch (_: Exception) {
        null
    }

    private fun parseIsoDateTime(value: String): LocalDateTime? = try {
        LocalDateTime.parse(value, ISO_DATE_TIME_FORMATTER)
    } catch (_: Exception) {
        null
    }

    private fun buildOpenMeteoFallbackWeather(
        data: OpenMeteoWindResponse,
        lat: Double,
        lon: Double,
        locationName: String,
        requestedCityId: Long,
        cachedState: WeatherState?
    ): com.example.meteomate.data.model.WeatherResponse? {
        val current = data.current ?: return null
        val temperature = current.temperature ?: return null
        val offset = data.utcOffsetSeconds ?: cachedState?.timezoneOffsetSeconds ?: 0
        val today = cityNow(offset).toLocalDate().toString()
        val dailyIndex = data.daily?.time.orEmpty().indexOf(today)
        fun dailyEpoch(values: List<String?>?): Long = values.orEmpty()
            .getOrNull(dailyIndex)
            ?.let(::parseIsoDateTime)
            ?.toEpochSecond(ZoneOffset.ofTotalSeconds(offset))
            ?: 0L
        val openWeatherCode = OpenMeteoWeatherCode.toOpenWeather(current.weatherCode)
        val fallbackName = locationName.ifBlank {
            cachedState?.locationName?.takeIf { it.isNotBlank() } ?: "Текущее местоположение"
        }
        val stableId = requestedCityId.takeIf { it != 0L }
            ?: cachedState?.cityId?.takeIf { it != 0L }
            ?: (lat.toBits() xor lon.toBits())
        return com.example.meteomate.data.model.WeatherResponse(
            coord = com.example.meteomate.data.model.Coordinates(lon = lon, lat = lat),
            weather = listOf(
                com.example.meteomate.data.model.WeatherCondition(
                    id = openWeatherCode,
                    main = "Open-Meteo",
                    description = openMeteoDescription(current.weatherCode),
                    icon = ""
                )
            ),
            main = com.example.meteomate.data.model.MainData(
                temp = temperature,
                feelsLike = current.apparentTemperature ?: temperature,
                tempMin = temperature,
                tempMax = temperature,
                pressure = current.surfacePressure?.roundToInt() ?: cachedState?.pressure ?: 0,
                humidity = current.humidity ?: cachedState?.humidity ?: 0,
                seaLevel = null,
                grndLevel = null
            ),
            visibility = current.visibility?.roundToInt() ?: cachedState?.visibility,
            wind = com.example.meteomate.data.model.WindData(
                speed = current.windSpeed ?: cachedState?.windSpeed ?: 0.0,
                deg = current.windDirection ?: cachedState?.windDeg,
                gust = current.windGusts ?: cachedState?.windGust
            ),
            clouds = current.cloudCover?.let { com.example.meteomate.data.model.CloudsData(it) },
            dt = Instant.now().epochSecond,
            sys = com.example.meteomate.data.model.SystemData(
                country = cachedState?.country,
                sunrise = dailyEpoch(data.daily?.sunrise).takeIf { it > 0 } ?: cachedState?.sunrise ?: 0,
                sunset = dailyEpoch(data.daily?.sunset).takeIf { it > 0 } ?: cachedState?.sunset ?: 0
            ),
            timezone = offset,
            id = stableId,
            name = fallbackName,
            cod = 200
        )
    }

    private fun openMeteoDescription(code: Int?): String = when (code) {
        0 -> "Ясно"
        1, 2 -> "Переменная облачность"
        3 -> "Пасмурно"
        45, 48 -> "Туман"
        51, 53, 55, 56, 57 -> "Морось"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Дождь"
        71, 73, 75, 77, 85, 86 -> "Снег"
        95, 96, 99 -> "Гроза"
        else -> "Погода"
    }

    private fun aggregateForecast(forecast: com.example.meteomate.data.model.ForecastResponse): List<DailyForecastItem> {
        return forecast.list
            .groupBy { DateUtils.extractDate(it.dtText) }
            .entries
            .map { (date, items) ->
                val weatherCodes = items.flatMap { it.weather.map { w -> w.id } }
                DailyForecastItem(
                    day = date,
                    weatherCode = weatherCodes.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 800,
                    maxTemp = items.maxOf { it.main.tempMax },
                    minTemp = items.minOf { it.main.tempMin },
                    precipitationProbability = items.maxOf { it.precipitationProbability },
                    maxWindSpeed = items.maxOf { it.wind.speed }
                )
            }
    }

    fun searchCities(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            when (val result = repository.searchCities(query)) {
                is Resource.Success -> _searchResults.value = result.data
                else -> _searchResults.value = emptyList()
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun retry() {
        val currentState = _uiState.value
        val locationName = if (currentState is WeatherUiState.Success) {
            currentState.state.locationName
        } else {
            ""
        }
        _currentLocation.value?.let { (lat, lon) ->
            loadWeather(lat, lon, locationName, forceRefresh = true)
        }
    }

    fun setError(message: String) {
        if (_uiState.value is WeatherUiState.Loading) {
            _uiState.value = WeatherUiState.Error(message)
        }
    }

    fun toggleFavorite() {
        val location = _currentLocation.value ?: return
        val (lat, lon) = location
        viewModelScope.launch {
            val state = _uiState.value
            if (state is WeatherUiState.Success) {
                val isFav = weatherDao.isFavorite(lat, lon)
                if (isFav) {
                    val favs = weatherDao.getFavoriteCities()
                    favs.firstOrNull { it.lat == lat && it.lon == lon }?.let {
                        weatherDao.removeFavoriteCity(it.id)
                    }
                } else {
                    weatherDao.addFavoriteCity(
                        com.example.meteomate.data.local.FavoriteCity(
                            name = state.state.locationName,
                            lat = lat,
                            lon = lon,
                            country = state.state.country
                        )
                    )
                }
                _uiState.value = WeatherUiState.Success(
                    state.state.copy(isFavorite = !isFav)
                )
                loadFavorites()
            }
        }
    }

    fun toggleFavoriteCity(city: FavoriteCity) {
        viewModelScope.launch {
            weatherDao.removeFavoriteCity(city.id)
            loadFavorites()
        }
    }

    fun switchFavoriteCity(direction: Int) {
        val cities = _favoriteCities.value
        if (cities.size < 2) return
        val location = _currentLocation.value
        val currentIndex = cities.indexOfFirst { city ->
            location?.let { (lat, lon) -> city.lat == lat && city.lon == lon } == true
        }.takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + if (direction >= 0) 1 else -1 + cities.size).mod(cities.size)
        val next = cities[nextIndex]
        loadWeather(next.lat, next.lon, next.name)
    }

    fun isCityFavorite(lat: Double, lon: Double): Boolean {
        return _favoriteCities.value.any { it.lat == lat && it.lon == lon }
    }

    fun selectWindModel(model: WindModel) {
        viewModelScope.launch {
            settingsDataStore.setSelectedWindModel(model)
            val state = _uiState.value
            if (state is WeatherUiState.Success) {
                _uiState.value = WeatherUiState.Success(
                    state.state.copy(windModel = model)
                )
                _currentLocation.value?.let { (lat, lon) ->
                    loadWindFromOpenMeteo(lat, lon, model)
                }
            }
        }
    }

    fun loadWindFromOpenMeteo(lat: Double, lon: Double, model: WindModel) {
        windLoadJob?.cancel()
        windLoadJob = viewModelScope.launch {
            val result = repository.getWindFromOpenMeteo(lat, lon, model.apiModelCode)
            if (result is Resource.Success) {
                if (_currentLocation.value != lat to lon) return@launch
                val data = result.data
                val currentState = _uiState.value
                if (currentState is WeatherUiState.Success) {
                    val currentWindSpeed = data.current?.windSpeed ?: currentState.state.windSpeed
                    val currentWindDeg = data.current?.windDirection ?: currentState.state.windDeg
                    val currentWindGust = data.current?.windGusts ?: currentState.state.windGust

                    val hourlyItems = if (data.hourly != null) {
                        val times = data.hourly.time
                        val speeds = data.hourly.windSpeed
                        val dirs = data.hourly.windDirection
                        val gusts = data.hourly.windGusts
                        times.indices.filter { i ->
                            parseIsoDateTime(times[i])?.let {
                                it >= cityNow(data.utcOffsetSeconds ?: currentState.state.timezoneOffsetSeconds)
                            } != false
                        }.take(24).map { i ->
                            HourlyForecastItem(
                                time = DateUtils.formatTimeFromISO(times[i]),
                                date = times[i].substringBefore('T'),
                                temperature = data.hourly.temperature.getOrNull(i)
                                    ?: currentState.state.temperature,
                                apparentTemperature = data.hourly.apparentTemperature.getOrNull(i)
                                    ?: currentState.state.feelsLike,
                                weatherCode = OpenMeteoWeatherCode.toOpenWeather(
                                    data.hourly.weatherCode.getOrNull(i)
                                ),
                                precipitationProbability = data.hourly.precipitationProbability
                                    .getOrNull(i)?.toDouble() ?: 0.0,
                                precipitation = data.hourly.precipitation.getOrNull(i) ?: 0.0,
                                windSpeed = speeds.getOrNull(i) ?: currentState.state.windSpeed,
                                windDeg = dirs.getOrNull(i),
                                windGust = gusts.getOrNull(i),
                                pressure = data.hourly.pressure.getOrNull(i)?.toInt()
                                    ?: currentState.state.pressure,
                                isDay = data.hourly.isDay.getOrNull(i)?.let { it != 0 }
                                    ?: fallbackIsDayFromIso(times[i]),
                                isCurrent = isCurrentHour(
                                    times[i],
                                    data.utcOffsetSeconds ?: currentState.state.timezoneOffsetSeconds
                                )
                            )
                        }
                    } else {
                        currentState.state.windHourlyForecast
                    }

                    val updatedState = currentState.state.copy(
                            windSpeed = currentWindSpeed,
                            windDeg = currentWindDeg,
                            windGust = currentWindGust,
                            windHourlyForecast = hourlyItems,
                            windModel = model
                    )
                    _uiState.value = WeatherUiState.Success(updatedState)
                    weatherDao.cacheWeather(
                        CachedWeather(
                            cityId = updatedState.cityId,
                            cityName = updatedState.locationName,
                            lat = lat,
                            lon = lon,
                            jsonData = gson.toJson(updatedState)
                        )
                    )
                }
            }
        }
    }

    fun toggleAlert10() {
        modelComparisonJob?.cancel()
        modelComparisonJob = viewModelScope.launch {
            val newValue = !settings.value.alert10Enabled
            settingsDataStore.setAlert10(newValue)
        }
    }

    fun toggleAlert15() {
        viewModelScope.launch {
            val newValue = !settings.value.alert15Enabled
            settingsDataStore.setAlert15(newValue)
        }
    }

    fun toggleAlert20() {
        viewModelScope.launch {
            val newValue = !settings.value.alert20Enabled
            settingsDataStore.setAlert20(newValue)
        }
    }

    fun loadModelComparison() {
        val loc = _currentLocation.value ?: return
        val modelsToCompare = listOf(
            WindModel.GFS27,
            WindModel.ECMWF,
            WindModel.ICON13,
            WindModel.HRRR
        )
        viewModelScope.launch {
            val results = modelsToCompare.map { model ->
                async {
                    val result = repository.getCurrentWindForModel(loc.first, loc.second, model.apiModelCode)
                    if (result is Resource.Success) {
                        val data = result.data
                        ModelComparisonItem(
                            model = model,
                            windSpeed = data.current?.windSpeed ?: 0.0,
                            windGust = data.current?.windGusts,
                            windDeg = data.current?.windDirection
                        )
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val currentState = _uiState.value
            if (currentState is WeatherUiState.Success && _currentLocation.value == loc) {
                _uiState.value = WeatherUiState.Success(
                    currentState.state.copy(modelComparison = results)
                )
            }
        }
    }

    private companion object {
        const val CACHE_SCHEMA_VERSION = 4
        const val HOURLY_FORECAST_HOURS = 48
        const val NOWCAST_INTERVALS = 8
        const val QUALITATIVE_PRECIPITATION_FALLBACK_MM = 0.1
        const val MIN_REFRESH_INTERVAL_MILLIS = 5 * 60 * 1000L
        val ISO_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val FALLBACK_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm")
    }
}
