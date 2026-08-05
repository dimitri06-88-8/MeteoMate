package com.example.meteomate.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meteomate.R
import com.example.meteomate.data.SettingsDataStore
import com.example.meteomate.data.repository.WeatherRepository
import com.example.meteomate.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

@HiltWorker
class WeatherWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val repository: WeatherRepository,
    private val stateStore: WeatherWidgetStateStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        if (!hasWidgets()) return@coroutineScope Result.success()
        val settings = settingsDataStore.settings.first()
        val city = settingsDataStore.lastCity.first()
        if (city == null) {
            WeatherWidgetRenderer.showEmpty(
                applicationContext,
                applicationContext.getString(R.string.weather_widget_open_app)
            )
            return@coroutineScope Result.success()
        }

        val cached = stateStore.read()?.takeIf { it.matches(city.lat, city.lon) }
        if (cached != null) {
            WeatherWidgetRenderer.updateAll(
                applicationContext,
                cached,
                settings.temperatureUnit,
                applicationContext.getString(R.string.weather_widget_updating)
            )
        }

        val currentRequest = async { repository.getCurrentWeather(city.lat, city.lon) }
        val openMeteoRequest = async { repository.getHourlyForecast(city.lat, city.lon) }
        val currentResult = currentRequest.await()
        val openMeteoResult = openMeteoRequest.await()
        val openMeteo = (openMeteoResult as? Resource.Success)?.data
        val openCurrent = openMeteo?.current
        val openWeather = (currentResult as? Resource.Success)?.data
        val hourly = openMeteo?.hourly
        val hourlyIndex = nearestHourlyIndex(hourly?.time.orEmpty(), openCurrent?.time)

        val temperature = openWeather?.main?.temp ?: openCurrent?.temperature
        val windSpeed = openWeather?.wind?.speed ?: openCurrent?.windSpeed
        if (temperature != null && windSpeed != null && temperature.isFinite() && windSpeed.isFinite()) {
            val precipitation = openCurrent?.precipitation
                ?: hourly?.precipitation?.getOrNull(hourlyIndex)
                ?: 0.0
            val probability = hourly?.precipitationProbability
                ?.getOrNull(hourlyIndex)
                ?.coerceIn(0, 100)
                ?: 0
            val timezoneId = openMeteo?.timezone
                ?: ZoneOffset.ofTotalSeconds(openMeteo?.utcOffsetSeconds ?: openWeather?.timezone ?: 0).id
            val snapshot = WidgetWeatherSnapshot(
                cityName = city.name,
                lat = city.lat,
                lon = city.lon,
                temperatureCelsius = temperature,
                precipitationMillimeters = precipitation.coerceAtLeast(0.0),
                precipitationProbability = probability,
                windSpeedMs = windSpeed.coerceAtLeast(0.0),
                updatedAtMillis = System.currentTimeMillis(),
                timezoneId = timezoneId
            )
            stateStore.save(snapshot)
            WeatherWidgetRenderer.updateAll(
                applicationContext,
                snapshot,
                settings.temperatureUnit,
                applicationContext.getString(R.string.weather_widget_updated)
            )
        } else if (cached != null) {
            WeatherWidgetRenderer.updateAll(
                applicationContext,
                cached,
                settings.temperatureUnit,
                applicationContext.getString(R.string.weather_widget_offline)
            )
        } else {
            WeatherWidgetRenderer.showEmpty(
                applicationContext,
                applicationContext.getString(R.string.weather_widget_unavailable)
            )
        }
        Result.success()
    }

    private fun hasWidgets(): Boolean {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(applicationContext, WeatherWidgetProvider::class.java)
        return manager.getAppWidgetIds(component).isNotEmpty()
    }
}
