package com.example.meteomate.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meteomate.data.SettingsDataStore
import com.example.meteomate.work.notification.SolarEventType
import com.example.meteomate.work.notification.WeatherNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import com.example.meteomate.work.notification.WeatherNotificationRules

@HiltWorker
class GoldenHourNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val notificationManager: WeatherNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsDataStore.settings.first()
        if (!settings.goldenHourNotificationsEnabled) {
            return Result.success()
        }
        val now = ZonedDateTime.now()
        if (
            settings.quietHoursEnabled &&
            WeatherNotificationRules.isQuietTime(
                now.hour * 60 + now.minute,
                settings.quietHoursStartMinutes,
                settings.quietHoursEndMinutes
            )
        ) return Result.success()
        val type = inputData.getString(KEY_EVENT_TYPE)
            ?.let { runCatching { SolarEventType.valueOf(it) }.getOrNull() }
            ?: return Result.failure()
        val cityName = inputData.getString(KEY_CITY_NAME).orEmpty()
        val eventTime = inputData.getLong(KEY_EVENT_TIME, 0L)
        val timezoneId = inputData.getString(KEY_TIMEZONE_ID).orEmpty()
        if (cityName.isBlank() || eventTime <= 0L) return Result.failure()

        notificationManager.postGoldenHour(
            cityName = cityName,
            type = type,
            eventEpochMillis = eventTime,
            timezoneId = timezoneId
        )
        return Result.success()
    }

    companion object {
        const val KEY_EVENT_TYPE = "event_type"
        const val KEY_CITY_NAME = "city_name"
        const val KEY_EVENT_TIME = "event_time"
        const val KEY_TIMEZONE_ID = "timezone_id"
    }
}
