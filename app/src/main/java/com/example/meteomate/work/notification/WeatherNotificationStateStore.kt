package com.example.meteomate.work.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class WeatherObservation(
    val temperatureCelsius: Double,
    val pressureHpa: Int,
    val timestampMillis: Long
)

@Singleton
class WeatherNotificationStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(
        "weather_notification_state",
        Context.MODE_PRIVATE
    )

    fun wasSummarySent(localDate: String): Boolean =
        preferences.getString(KEY_LAST_SUMMARY_DATE, null) == localDate

    fun markSummarySent(localDate: String) {
        preferences.edit().putString(KEY_LAST_SUMMARY_DATE, localDate).apply()
    }

    fun canPostAlert(key: String, nowMillis: Long, cooldownMillis: Long): Boolean {
        val lastSent = preferences.getLong("$ALERT_PREFIX$key", Long.MIN_VALUE)
        return lastSent == Long.MIN_VALUE || nowMillis - lastSent >= cooldownMillis
    }

    fun markAlertPosted(key: String, nowMillis: Long) {
        preferences.edit().putLong("$ALERT_PREFIX$key", nowMillis).apply()
    }

    fun getObservation(cityKey: String): WeatherObservation? {
        val timeKey = "${KEY_OBSERVATION_TIME}_$cityKey"
        if (!preferences.contains(timeKey)) return null
        return WeatherObservation(
            temperatureCelsius = Double.fromBits(
                preferences.getLong("${KEY_OBSERVATION_TEMPERATURE}_$cityKey", 0L)
            ),
            pressureHpa = preferences.getInt("${KEY_OBSERVATION_PRESSURE}_$cityKey", 0),
            timestampMillis = preferences.getLong(timeKey, 0L)
        )
    }

    fun saveObservation(cityKey: String, observation: WeatherObservation) {
        preferences.edit()
            .putLong("${KEY_OBSERVATION_TEMPERATURE}_$cityKey", observation.temperatureCelsius.toBits())
            .putInt("${KEY_OBSERVATION_PRESSURE}_$cityKey", observation.pressureHpa)
            .putLong("${KEY_OBSERVATION_TIME}_$cityKey", observation.timestampMillis)
            .apply()
    }

    fun nextFavoriteIndex(count: Int): Int {
        if (count <= 0) return 0
        val current = preferences.getInt(KEY_FAVORITE_CURSOR, 0).mod(count)
        preferences.edit().putInt(KEY_FAVORITE_CURSOR, (current + 1).mod(count)).apply()
        return current
    }

    companion object {
        private const val KEY_LAST_SUMMARY_DATE = "last_summary_date"
        private const val KEY_OBSERVATION_TEMPERATURE = "observation_temperature"
        private const val KEY_OBSERVATION_PRESSURE = "observation_pressure"
        private const val KEY_OBSERVATION_TIME = "observation_time"
        private const val ALERT_PREFIX = "last_alert_"
        private const val KEY_FAVORITE_CURSOR = "favorite_cursor"
    }
}
