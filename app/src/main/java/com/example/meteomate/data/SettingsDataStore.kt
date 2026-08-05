package com.example.meteomate.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val selectedWindModel: WindModel = WindModel.GFS27,
    val displayMode: DisplayMode = DisplayMode.DETAILED,
    val cardOrder: CardOrder = CardOrder.FORECAST_FIRST,
    val weatherAnimationsEnabled: Boolean = true,
    val alert10Enabled: Boolean = false,
    val alert15Enabled: Boolean = false,
    val alert20Enabled: Boolean = false,
    val morningSummaryEnabled: Boolean = false,
    val morningSummaryTimeMinutes: Int = 8 * 60,
    val goldenHourNotificationsEnabled: Boolean = false,
    val strongWindNotificationsEnabled: Boolean = false,
    val rainNotificationsEnabled: Boolean = false,
    val thunderstormNotificationsEnabled: Boolean = false,
    val snowNotificationsEnabled: Boolean = false,
    val heatNotificationsEnabled: Boolean = false,
    val frostNotificationsEnabled: Boolean = false,
    val iceNotificationsEnabled: Boolean = false,
    val rapidTemperatureChangeNotificationsEnabled: Boolean = false,
    val rapidPressureDropNotificationsEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStartMinutes: Int = 22 * 60,
    val quietHoursEndMinutes: Int = 7 * 60
) {
    fun hasAnyWeatherNotificationEnabled(): Boolean =
        morningSummaryEnabled ||
            goldenHourNotificationsEnabled ||
            strongWindNotificationsEnabled ||
            rainNotificationsEnabled ||
            thunderstormNotificationsEnabled ||
            snowNotificationsEnabled ||
            heatNotificationsEnabled ||
            frostNotificationsEnabled ||
            iceNotificationsEnabled ||
            rapidTemperatureChangeNotificationsEnabled ||
            rapidPressureDropNotificationsEnabled ||
            alert10Enabled || alert15Enabled || alert20Enabled
}

data class SavedCity(
    val id: Long?,
    val name: String,
    val lat: Double,
    val lon: Double
)

enum class TemperatureUnit(val label: String, val apiValue: String) {
    CELSIUS("°C", "metric"),
    FAHRENHEIT("°F", "imperial")
}

enum class PressureUnit(val label: String) {
    HPA("гПа"),
    MMHG("мм рт. ст.")
}

enum class DisplayMode(val label: String) {
    COMPACT("Компактный"),
    DETAILED("Подробный")
}

enum class CardOrder(val label: String) {
    FORECAST_FIRST("Сначала прогноз"),
    DETAILS_FIRST("Сначала показатели")
}

enum class WindModel(
    val label: String,
    val description: String,
    val apiModelCode: String
) {
    ECMWF("ECMWF", "9 км · Глобальная · Обновление 4 раза/день · Золотой стандарт среднесрочных прогнозов", "ecmwf_ifs"),
    ECMWF_ENS("ECMWF-ENS", "18 км · Ансамбль из 51 члена · Диапазоны вероятностей и доверительные интервалы", "ecmwf_ifs025"),
    GFS27("GFS 27 км", "27 км · Глобальная · Обновление каждые 6ч · Бесплатная базовая модель", "gfs_seamless"),
    GFS_PLUS("GFS+", "3 км · США · Почасовые обновления · Конвективные детали ветра", "gfs_global"),
    ICON13("ICON 13 км", "13 км · Глобальная · DWD (Германия) · Хорошее покрытие Европы", "icon_global"),
    ICON7("ICON 7 км", "7 км · Европа · DWD (Германия) · Детальный ветер Европы", "icon_eu"),
    NAM("NAM 12 км", "12 км · Северная Америка · Обновление каждые 6ч · Региональные детали", "ncep_nam_conus"),
    HRRR("HRRR", "3 км · США · Почасовые обновления · Лучшая модель для порывов", "ncep_hrrr_conus"),
    WRF8("WRF 8 км", "8 км · Настраиваемая область · Исследовательская модель", "gfs_seamless"),
    OPEN_SKIRON("Open Skiron", "4 км · Средиземноморье/Европа · Метеослужба Skiron", "icon_seamless"),
    OPEN_WRF("Open WRF", "Настраиваемая · Open-source вариант WRF · Свои области", "gfs_seamless"),
    AROME("AROME", "1.3 км · Франция и окрестности · Météo-France · Сверхвысокое разрешение", "meteofrance_arome_france"),
    MFWAM("MFWAM", "25 км · Глобальный океан · Ветровое поле ECMWF для оценки волн", "ecmwf_ifs")
}

enum class WindParameter(val label: String, val description: String) {
    WIND_DIRECTION_CARDINAL("Direction", "Cardinal wind direction (N, NE, E, etc.)"),
    WIND_DIRECTION_DEGREE("Direction°", "Wind direction in degrees"),
    WIND_SPEED("Speed", "Wind speed in m/s"),
    WIND_GUSTS("Gusts", "Wind gust speed"),
    WIND_ROSE("Wind Rose", "Wind rose chart"),
    WIND_BARBS("Wind Barbs", "Wind barbs visualization"),
    WIND_BAR("Wind Bar", "Wind bar indicator")
}

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val SELECTED_WIND_MODEL = stringPreferencesKey("selected_wind_model")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val CARD_ORDER = stringPreferencesKey("card_order")
        val WEATHER_ANIMATIONS = booleanPreferencesKey("weather_animations")
        val LAST_CITY_ID = stringPreferencesKey("last_city_id")
        val LAST_CITY_NAME = stringPreferencesKey("last_city_name")
        val LAST_CITY_LAT = stringPreferencesKey("last_city_lat")
        val LAST_CITY_LON = stringPreferencesKey("last_city_lon")
        val ALERT_10 = booleanPreferencesKey("alert_10_enabled")
        val ALERT_15 = booleanPreferencesKey("alert_15_enabled")
        val ALERT_20 = booleanPreferencesKey("alert_20_enabled")
        val MORNING_SUMMARY = booleanPreferencesKey("morning_summary_enabled")
        val MORNING_SUMMARY_TIME = intPreferencesKey("morning_summary_time_minutes")
        val NOTIFY_GOLDEN_HOUR = booleanPreferencesKey("notify_golden_hour")
        val NOTIFY_STRONG_WIND = booleanPreferencesKey("notify_strong_wind")
        val NOTIFY_RAIN = booleanPreferencesKey("notify_rain")
        val NOTIFY_THUNDERSTORM = booleanPreferencesKey("notify_thunderstorm")
        val NOTIFY_SNOW = booleanPreferencesKey("notify_snow")
        val NOTIFY_HEAT = booleanPreferencesKey("notify_heat")
        val NOTIFY_FROST = booleanPreferencesKey("notify_frost")
        val NOTIFY_ICE = booleanPreferencesKey("notify_ice")
        val NOTIFY_RAPID_TEMPERATURE = booleanPreferencesKey("notify_rapid_temperature")
        val NOTIFY_RAPID_PRESSURE = booleanPreferencesKey("notify_rapid_pressure")
        val QUIET_HOURS = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start_minutes")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end_minutes")
        val OBSERVATION_TOTAL = intPreferencesKey("observation_total")
        val OBSERVATION_LAST_AT = longPreferencesKey("observation_last_at")
        val OBSERVATION_LAST_KIND = stringPreferencesKey("observation_last_kind")
        val OBSERVATION_LAST_LOCATION = stringPreferencesKey("observation_last_location")
        val OBSERVATION_SECRET_BADGE = booleanPreferencesKey("observation_secret_badge")
        val LAST_SEEN_WHATS_NEW_VERSION = intPreferencesKey("last_seen_whats_new_version")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            temperatureUnit = tryOrNull { TemperatureUnit.valueOf(prefs[Keys.TEMPERATURE_UNIT] ?: "CELSIUS") } ?: TemperatureUnit.CELSIUS,
            pressureUnit = tryOrNull { PressureUnit.valueOf(prefs[Keys.PRESSURE_UNIT] ?: "HPA") } ?: PressureUnit.HPA,
            selectedWindModel = tryOrNull { WindModel.valueOf(prefs[Keys.SELECTED_WIND_MODEL] ?: "GFS27") } ?: WindModel.GFS27,
            displayMode = tryOrNull { DisplayMode.valueOf(prefs[Keys.DISPLAY_MODE] ?: "DETAILED") } ?: DisplayMode.DETAILED,
            cardOrder = tryOrNull { CardOrder.valueOf(prefs[Keys.CARD_ORDER] ?: "FORECAST_FIRST") } ?: CardOrder.FORECAST_FIRST,
            weatherAnimationsEnabled = prefs[Keys.WEATHER_ANIMATIONS] ?: true,
            alert10Enabled = prefs[Keys.ALERT_10] ?: false,
            alert15Enabled = prefs[Keys.ALERT_15] ?: false,
            alert20Enabled = prefs[Keys.ALERT_20] ?: false,
            morningSummaryEnabled = prefs[Keys.MORNING_SUMMARY] ?: false,
            morningSummaryTimeMinutes = (prefs[Keys.MORNING_SUMMARY_TIME] ?: 8 * 60).validMinutesOfDay(8 * 60),
            goldenHourNotificationsEnabled = prefs[Keys.NOTIFY_GOLDEN_HOUR] ?: false,
            strongWindNotificationsEnabled = prefs[Keys.NOTIFY_STRONG_WIND] ?: false,
            rainNotificationsEnabled = prefs[Keys.NOTIFY_RAIN] ?: false,
            thunderstormNotificationsEnabled = prefs[Keys.NOTIFY_THUNDERSTORM] ?: false,
            snowNotificationsEnabled = prefs[Keys.NOTIFY_SNOW] ?: false,
            heatNotificationsEnabled = prefs[Keys.NOTIFY_HEAT] ?: false,
            frostNotificationsEnabled = prefs[Keys.NOTIFY_FROST] ?: false,
            iceNotificationsEnabled = prefs[Keys.NOTIFY_ICE] ?: false,
            rapidTemperatureChangeNotificationsEnabled = prefs[Keys.NOTIFY_RAPID_TEMPERATURE] ?: false,
            rapidPressureDropNotificationsEnabled = prefs[Keys.NOTIFY_RAPID_PRESSURE] ?: false,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS] ?: true,
            quietHoursStartMinutes = (prefs[Keys.QUIET_HOURS_START] ?: 22 * 60).validMinutesOfDay(22 * 60),
            quietHoursEndMinutes = (prefs[Keys.QUIET_HOURS_END] ?: 7 * 60).validMinutesOfDay(7 * 60)
        )
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.settingsStore.edit { it[Keys.TEMPERATURE_UNIT] = unit.name }
    }

    suspend fun setPressureUnit(unit: PressureUnit) {
        context.settingsStore.edit { it[Keys.PRESSURE_UNIT] = unit.name }
    }

    suspend fun setSelectedWindModel(model: WindModel) {
        context.settingsStore.edit { it[Keys.SELECTED_WIND_MODEL] = model.name }
    }

    suspend fun setDisplayMode(mode: DisplayMode) {
        context.settingsStore.edit { it[Keys.DISPLAY_MODE] = mode.name }
    }

    suspend fun setCardOrder(order: CardOrder) {
        context.settingsStore.edit { it[Keys.CARD_ORDER] = order.name }
    }

    suspend fun setWeatherAnimationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.WEATHER_ANIMATIONS] = enabled }
    }

    suspend fun saveLastCity(id: Long, name: String, lat: Double, lon: Double) {
        context.settingsStore.edit {
            it[Keys.LAST_CITY_ID] = id.toString()
            it[Keys.LAST_CITY_NAME] = name
            it[Keys.LAST_CITY_LAT] = lat.toString()
            it[Keys.LAST_CITY_LON] = lon.toString()
        }
    }

    suspend fun setAlert10(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.ALERT_10] = enabled }
    }

    suspend fun setAlert15(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.ALERT_15] = enabled }
    }

    suspend fun setAlert20(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.ALERT_20] = enabled }
    }

    suspend fun setMorningSummaryEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.MORNING_SUMMARY] = enabled }
    }

    suspend fun setMorningSummaryTime(minutes: Int) {
        context.settingsStore.edit { it[Keys.MORNING_SUMMARY_TIME] = minutes.validMinutesOfDay(8 * 60) }
    }

    suspend fun setGoldenHourNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_GOLDEN_HOUR] = enabled }
    }

    suspend fun setStrongWindNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_STRONG_WIND] = enabled }
    }

    suspend fun setRainNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_RAIN] = enabled }
    }

    suspend fun setThunderstormNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_THUNDERSTORM] = enabled }
    }

    suspend fun setSnowNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_SNOW] = enabled }
    }

    suspend fun setHeatNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_HEAT] = enabled }
    }

    suspend fun setFrostNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_FROST] = enabled }
    }

    suspend fun setIceNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_ICE] = enabled }
    }

    suspend fun setRapidTemperatureChangeNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_RAPID_TEMPERATURE] = enabled }
    }

    suspend fun setRapidPressureDropNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.NOTIFY_RAPID_PRESSURE] = enabled }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.QUIET_HOURS] = enabled }
    }

    suspend fun setQuietHoursStart(minutes: Int) {
        context.settingsStore.edit { it[Keys.QUIET_HOURS_START] = minutes.validMinutesOfDay(22 * 60) }
    }

    suspend fun setQuietHoursEnd(minutes: Int) {
        context.settingsStore.edit { it[Keys.QUIET_HOURS_END] = minutes.validMinutesOfDay(7 * 60) }
    }

    val weatherObservationProgress: Flow<WeatherObservationProgress> =
        context.settingsStore.data.map { prefs ->
            WeatherObservationProgress(
                totalReports = prefs[Keys.OBSERVATION_TOTAL] ?: 0,
                lastReportAt = prefs[Keys.OBSERVATION_LAST_AT] ?: 0L,
                lastKind = prefs[Keys.OBSERVATION_LAST_KIND]
                    ?.let { value -> tryOrNull { WeatherObservationKind.valueOf(value) } },
                lastLocation = prefs[Keys.OBSERVATION_LAST_LOCATION].orEmpty(),
                secretBadgeUnlocked = prefs[Keys.OBSERVATION_SECRET_BADGE] ?: false
            )
        }

    suspend fun recordWeatherObservation(
        kind: WeatherObservationKind,
        location: String,
        unlockSecretBadge: Boolean
    ) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.OBSERVATION_TOTAL] = (prefs[Keys.OBSERVATION_TOTAL] ?: 0) + 1
            prefs[Keys.OBSERVATION_LAST_AT] = System.currentTimeMillis()
            prefs[Keys.OBSERVATION_LAST_KIND] = kind.name
            prefs[Keys.OBSERVATION_LAST_LOCATION] = location
            if (unlockSecretBadge) prefs[Keys.OBSERVATION_SECRET_BADGE] = true
        }
    }

    val lastCityId: Flow<Long?> = context.settingsStore.data.map { it[Keys.LAST_CITY_ID]?.toLongOrNull() }
    val lastCityName: Flow<String?> = context.settingsStore.data.map { it[Keys.LAST_CITY_NAME] }
    val lastCityLat: Flow<Double?> = context.settingsStore.data.map { it[Keys.LAST_CITY_LAT]?.toDoubleOrNull() }
    val lastCityLon: Flow<Double?> = context.settingsStore.data.map { it[Keys.LAST_CITY_LON]?.toDoubleOrNull() }
    val lastCity: Flow<SavedCity?> = context.settingsStore.data.map { prefs ->
        val lat = prefs[Keys.LAST_CITY_LAT]?.toDoubleOrNull()
        val lon = prefs[Keys.LAST_CITY_LON]?.toDoubleOrNull()
        val name = prefs[Keys.LAST_CITY_NAME]
        if (lat == null || lon == null || name.isNullOrBlank()) {
            null
        } else {
            SavedCity(
                id = prefs[Keys.LAST_CITY_ID]?.toLongOrNull(),
                name = name,
                lat = lat,
                lon = lon
            )
        }
    }

    val lastSeenWhatsNewVersionCode: Flow<Int> = context.settingsStore.data.map { prefs ->
        prefs[Keys.LAST_SEEN_WHATS_NEW_VERSION] ?: 0
    }

    suspend fun markWhatsNewShown(versionCode: Int) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.LAST_SEEN_WHATS_NEW_VERSION] = maxOf(
                prefs[Keys.LAST_SEEN_WHATS_NEW_VERSION] ?: 0,
                versionCode
            )
        }
    }
}

private inline fun <T> tryOrNull(block: () -> T): T? = try { block() } catch (_: Exception) { null }

private fun Int.validMinutesOfDay(default: Int): Int = if (this in 0 until 24 * 60) this else default
