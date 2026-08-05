package com.example.meteomate.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.meteomate.data.AppSettings
import com.example.meteomate.data.SettingsDataStore
import com.example.meteomate.data.local.WeatherDao
import com.example.meteomate.data.repository.WeatherRepository
import com.example.meteomate.util.Resource
import com.example.meteomate.work.notification.WeatherNotificationManager
import com.example.meteomate.work.notification.WeatherNotificationRules
import com.example.meteomate.work.notification.WeatherNotificationStateStore
import com.example.meteomate.work.notification.WeatherObservation
import com.example.meteomate.work.notification.GoldenHourScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

@HiltWorker
class WeatherRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val repository: WeatherRepository,
    private val weatherDao: WeatherDao,
    private val notificationManager: WeatherNotificationManager,
    private val notificationStateStore: WeatherNotificationStateStore,
    private val goldenHourScheduler: GoldenHourScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val settings = settingsDataStore.settings.first()
        if (!settings.hasAnyWeatherNotificationEnabled()) return@coroutineScope Result.success()
        if (!notificationManager.canPostNotifications()) return@coroutineScope Result.success()

        val city = settingsDataStore.lastCity.first() ?: return@coroutineScope Result.success()
        if (settings.goldenHourNotificationsEnabled) {
            val solarResult = repository.getHourlyForecast(city.lat, city.lon)
            if (solarResult is Resource.Success) {
                goldenHourScheduler.schedule(solarResult.data, city.name)
            } else if (!settings.hasNonSolarNotificationEnabled()) {
                return@coroutineScope retryForTransientFailure(solarResult)
            }
        } else {
            goldenHourScheduler.cancel()
        }

        if (!settings.hasNonSolarNotificationEnabled()) {
            return@coroutineScope Result.success()
        }

        val now = ZonedDateTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        if (
            settings.quietHoursEnabled &&
            WeatherNotificationRules.isQuietTime(
                nowMinutes,
                settings.quietHoursStartMinutes,
                settings.quietHoursEndMinutes
            )
        ) {
            return@coroutineScope Result.success()
        }
        val localDate = now.toLocalDate().toString()
        val summaryDue = settings.morningSummaryEnabled &&
            WeatherNotificationRules.isMorningSummaryDue(
                now = now,
                scheduledMinutes = settings.morningSummaryTimeMinutes,
                wasSentToday = notificationStateStore.wasSummarySent(localDate)
            )

        val needsForecast = summaryDue || settings.hasForecastAlertEnabled()
        val forecastRequest = if (needsForecast) {
            async { repository.getForecast(city.lat, city.lon) }
        } else {
            null
        }
        val currentResult = repository.getCurrentWeather(city.lat, city.lon)
        val current = (currentResult as? Resource.Success)?.data
            ?: return@coroutineScope retryForTransientFailure(currentResult)
        val forecast = (forecastRequest?.await() as? Resource.Success)?.data

        if (summaryDue) {
            val body = WeatherNotificationRules.createSummaryBody(settings, current, forecast)
            if (notificationManager.postMorningSummary(city.name, body)) {
                notificationStateStore.markSummarySent(localDate)
            }
        }

        val detectedAlerts = WeatherNotificationRules.detectForecastAlerts(
            settings = settings,
            current = current,
            forecast = forecast,
            nowEpochSeconds = now.toEpochSecond()
        ).toMutableList()

        val nowMillis = now.toInstant().toEpochMilli()
        val activeCityKey = cityKey(city.lat, city.lon)
        val previousObservation = notificationStateStore.getObservation(activeCityKey)
        val currentObservation = WeatherObservation(
            temperatureCelsius = current.main.temp,
            pressureHpa = current.main.pressure,
            timestampMillis = nowMillis
        )
        detectedAlerts += WeatherNotificationRules.detectRapidChanges(
            settings = settings,
            previous = previousObservation,
            current = currentObservation
        )

        if (
            WeatherNotificationRules.shouldRefreshObservation(previousObservation, nowMillis) ||
            detectedAlerts.any {
                it.kind.stateKey == "rapid_temperature" || it.kind.stateKey == "rapid_pressure"
            }
        ) {
            notificationStateStore.saveObservation(activeCityKey, currentObservation)
        }

        postDeduplicatedAlerts(
            cityKey = activeCityKey,
            cityName = city.name,
            alerts = detectedAlerts,
            nowMillis = nowMillis
        )

        if (settings.hasForecastAlertEnabled()) {
            val favoriteCities = weatherDao.getFavoriteCities()
                .filterNot { favorite ->
                    favorite.lat == city.lat && favorite.lon == city.lon
                }
            val favoriteToCheck = favoriteCities.getOrNull(
                notificationStateStore.nextFavoriteIndex(favoriteCities.size)
            )
            val favoriteAlerts = listOfNotNull(favoriteToCheck)
                .map { favorite ->
                    async {
                        val favoriteCurrent = repository.getCurrentWeather(favorite.lat, favorite.lon)
                        val favoriteForecast = repository.getForecast(favorite.lat, favorite.lon)
                        val weather = (favoriteCurrent as? Resource.Success)?.data
                            ?: return@async null
                        val upcoming = (favoriteForecast as? Resource.Success)?.data
                        val alerts = WeatherNotificationRules.detectForecastAlerts(
                            settings = settings,
                            current = weather,
                            forecast = upcoming,
                            nowEpochSeconds = now.toEpochSecond()
                        )
                        FavoriteCityAlerts(
                            key = cityKey(favorite.lat, favorite.lon),
                            name = favorite.name,
                            alerts = alerts
                        )
                    }
                }
                .awaitAll()
                .filterNotNull()

            favoriteAlerts.forEach { favorite ->
                postDeduplicatedAlerts(
                    cityKey = favorite.key,
                    cityName = favorite.name,
                    alerts = favorite.alerts,
                    nowMillis = nowMillis
                )
            }
        }
        Result.success()
    }

    private fun postDeduplicatedAlerts(
        cityKey: String,
        cityName: String,
        alerts: List<com.example.meteomate.work.notification.DetectedWeatherAlert>,
        nowMillis: Long
    ) {
        val alertsToPost = alerts
            .distinctBy { it.deduplicationKey }
            .filter { alert ->
                notificationStateStore.canPostAlert(
                    key = "${cityKey}_${alert.deduplicationKey}",
                    nowMillis = nowMillis,
                    cooldownMillis = WeatherNotificationRules.ALERT_COOLDOWN_MILLIS
                )
            }
            .sortedByDescending { it.kind.priority }

        if (notificationManager.postWeatherAlerts(cityName, alertsToPost, cityKey)) {
            alertsToPost.forEach { alert ->
                notificationStateStore.markAlertPosted(
                    "${cityKey}_${alert.deduplicationKey}",
                    nowMillis
                )
                if (alert.kind.stateKey == "wind" && alert.threshold != null) {
                    listOf(10, 15, 20)
                        .filter { threshold -> threshold <= alert.threshold }
                        .forEach { threshold ->
                            notificationStateStore.markAlertPosted(
                                "${cityKey}_wind_$threshold",
                                nowMillis
                            )
                        }
                }
            }
        }
    }

    private fun retryForTransientFailure(result: Resource<*>): Result {
        val exception = (result as? Resource.Error)?.exception
        return if (exception is IOException && runAttemptCount < MAX_RETRY_ATTEMPTS) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "weather_refresh"
        private const val TIMELY_WORK_NAME = "weather_notifications_v2"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val CHECK_INTERVAL_MINUTES = 15L

        fun syncScheduling(context: Context, settings: AppSettings) {
            if (settings.hasAnyWeatherNotificationEnabled()) enqueue(context) else cancel(context)
        }

        suspend fun syncScheduling(context: Context, settingsDataStore: SettingsDataStore) {
            syncScheduling(context, settingsDataStore.settings.first())
        }

        fun enqueue(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setInitialDelay(CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                TIMELY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(WORK_NAME)
                cancelUniqueWork(TIMELY_WORK_NAME)
            }
        }
    }
}

private data class FavoriteCityAlerts(
    val key: String,
    val name: String,
    val alerts: List<com.example.meteomate.work.notification.DetectedWeatherAlert>
)

private fun cityKey(lat: Double, lon: Double): String =
    "${lat.toBits().toString(16)}_${lon.toBits().toString(16)}"

private fun AppSettings.hasForecastAlertEnabled(): Boolean =
    strongWindNotificationsEnabled ||
        rainNotificationsEnabled ||
        thunderstormNotificationsEnabled ||
        snowNotificationsEnabled ||
        heatNotificationsEnabled ||
        frostNotificationsEnabled ||
        iceNotificationsEnabled ||
        alert10Enabled || alert15Enabled || alert20Enabled

private fun AppSettings.hasNonSolarNotificationEnabled(): Boolean =
    morningSummaryEnabled ||
        hasForecastAlertEnabled() ||
        rapidTemperatureChangeNotificationsEnabled ||
        rapidPressureDropNotificationsEnabled
