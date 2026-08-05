package com.example.meteomate.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherWidgetStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): WidgetWeatherSnapshot? {
        if (!preferences.contains(KEY_UPDATED_AT)) return null
        return WidgetWeatherSnapshot(
            cityName = preferences.getString(KEY_CITY_NAME, null).orEmpty(),
            lat = Double.fromBits(preferences.getLong(KEY_LAT, 0L)),
            lon = Double.fromBits(preferences.getLong(KEY_LON, 0L)),
            temperatureCelsius = Double.fromBits(preferences.getLong(KEY_TEMPERATURE, 0L)),
            precipitationMillimeters = Double.fromBits(preferences.getLong(KEY_PRECIPITATION, 0L)),
            precipitationProbability = preferences.getInt(KEY_PRECIPITATION_PROBABILITY, 0),
            windSpeedMs = Double.fromBits(preferences.getLong(KEY_WIND_SPEED, 0L)),
            updatedAtMillis = preferences.getLong(KEY_UPDATED_AT, 0L),
            timezoneId = preferences.getString(KEY_TIMEZONE_ID, null).orEmpty()
        ).takeIf(WidgetWeatherSnapshot::isValid)
    }

    fun save(snapshot: WidgetWeatherSnapshot) {
        if (!snapshot.isValid()) return
        preferences.edit()
            .putString(KEY_CITY_NAME, snapshot.cityName)
            .putLong(KEY_LAT, snapshot.lat.toBits())
            .putLong(KEY_LON, snapshot.lon.toBits())
            .putLong(KEY_TEMPERATURE, snapshot.temperatureCelsius.toBits())
            .putLong(KEY_PRECIPITATION, snapshot.precipitationMillimeters.toBits())
            .putInt(KEY_PRECIPITATION_PROBABILITY, snapshot.precipitationProbability)
            .putLong(KEY_WIND_SPEED, snapshot.windSpeedMs.toBits())
            .putLong(KEY_UPDATED_AT, snapshot.updatedAtMillis)
            .putString(KEY_TIMEZONE_ID, snapshot.timezoneId)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "weather_widget_state"
        const val KEY_CITY_NAME = "city_name"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_PRECIPITATION = "precipitation"
        const val KEY_PRECIPITATION_PROBABILITY = "precipitation_probability"
        const val KEY_WIND_SPEED = "wind_speed"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_TIMEZONE_ID = "timezone_id"
    }
}

