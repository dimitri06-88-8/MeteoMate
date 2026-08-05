package com.example.meteomate.work.notification

import com.example.meteomate.data.AppSettings
import com.example.meteomate.data.model.ForecastItem
import com.example.meteomate.data.model.ForecastResponse
import com.example.meteomate.data.model.WeatherResponse
import java.time.ZonedDateTime
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeatherAlertKind(val stateKey: String, val priority: Int) {
    THUNDERSTORM("thunderstorm", 100),
    ICE("ice", 95),
    WIND("wind", 90),
    RAPID_PRESSURE_DROP("rapid_pressure", 85),
    RAPID_TEMPERATURE_CHANGE("rapid_temperature", 80),
    HEAT("heat", 70),
    FROST("frost", 70),
    SNOW("snow", 60),
    RAIN("rain", 50)
}

data class DetectedWeatherAlert(
    val kind: WeatherAlertKind,
    val value: Double? = null,
    val threshold: Int? = null
) {
    val deduplicationKey: String
        get() = if (kind == WeatherAlertKind.WIND && threshold != null) {
            "${kind.stateKey}_$threshold"
        } else {
            kind.stateKey
        }
}

object WeatherNotificationRules {
    const val ALERT_COOLDOWN_MILLIS = 12L * 60L * 60L * 1000L
    const val OBSERVATION_WINDOW_MILLIS = 6L * 60L * 60L * 1000L
    const val OBSERVATION_REFRESH_MILLIS = 3L * 60L * 60L * 1000L

    private const val FORECAST_HORIZON_SECONDS = 12L * 60L * 60L
    private const val HIGH_PRECIPITATION_PROBABILITY = 0.5
    private const val HEAT_THRESHOLD_CELSIUS = 35.0
    private const val FROST_THRESHOLD_CELSIUS = -20.0
    private const val RAPID_TEMPERATURE_THRESHOLD_CELSIUS = 5.0
    private const val RAPID_PRESSURE_DROP_THRESHOLD_HPA = 5

    fun isQuietTime(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        if (startMinutes == endMinutes) return false
        return if (startMinutes < endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    fun isMorningSummaryDue(
        now: ZonedDateTime,
        scheduledMinutes: Int,
        wasSentToday: Boolean
    ): Boolean {
        if (wasSentToday) return false
        val currentMinutes = now.hour * 60 + now.minute
        val minutesAfterSchedule = currentMinutes - scheduledMinutes
        return minutesAfterSchedule in 0..SUMMARY_DELIVERY_WINDOW_MINUTES
    }

    fun detectForecastAlerts(
        settings: AppSettings,
        current: WeatherResponse,
        forecast: ForecastResponse?,
        nowEpochSeconds: Long
    ): List<DetectedWeatherAlert> {
        val upcoming = forecast?.list.orEmpty().filter {
            it.dt in nowEpochSeconds..(nowEpochSeconds + FORECAST_HORIZON_SECONDS)
        }
        val currentWeatherIds = current.weather.map { it.id }
        val likelyUpcoming = upcoming.filter { it.precipitationProbability >= HIGH_PRECIPITATION_PROBABILITY }
        val likelyUpcomingIds = likelyUpcoming.flatMap { item -> item.weather.map { it.id } }
        val allWeatherIds = currentWeatherIds + likelyUpcomingIds
        val alerts = mutableListOf<DetectedWeatherAlert>()

        val windValues = buildList {
            add(current.wind.speed)
            add(current.wind.gust ?: current.wind.speed)
            upcoming.forEach {
                add(it.wind.speed)
                add(it.wind.gust ?: it.wind.speed)
            }
        }
        val maxWind = windValues.maxOrNull() ?: 0.0
        val reachedWindThreshold = activeWindThresholds(settings)
            .filter { maxWind >= it }
            .maxOrNull()
        if (reachedWindThreshold != null) {
            alerts += DetectedWeatherAlert(
                kind = WeatherAlertKind.WIND,
                value = maxWind,
                threshold = reachedWindThreshold
            )
        }

        if (settings.thunderstormNotificationsEnabled && allWeatherIds.any { it in 200..299 }) {
            alerts += DetectedWeatherAlert(WeatherAlertKind.THUNDERSTORM)
        }
        if (settings.iceNotificationsEnabled && allWeatherIds.any { it == 511 }) {
            alerts += DetectedWeatherAlert(WeatherAlertKind.ICE)
        }
        if (settings.snowNotificationsEnabled && allWeatherIds.any { it in 600..699 }) {
            alerts += DetectedWeatherAlert(WeatherAlertKind.SNOW)
        }
        if (settings.rainNotificationsEnabled && allWeatherIds.any { it in 300..599 && it != 511 }) {
            alerts += DetectedWeatherAlert(WeatherAlertKind.RAIN)
        }

        val temperatures = buildList {
            add(current.main.temp)
            upcoming.forEach { add(it.main.temp) }
        }
        val maximumTemperature = temperatures.maxOrNull() ?: current.main.temp
        val minimumTemperature = temperatures.minOrNull() ?: current.main.temp
        if (settings.heatNotificationsEnabled && maximumTemperature >= HEAT_THRESHOLD_CELSIUS) {
            alerts += DetectedWeatherAlert(WeatherAlertKind.HEAT, maximumTemperature)
        }
        if (settings.frostNotificationsEnabled && minimumTemperature <= FROST_THRESHOLD_CELSIUS) {
            alerts += DetectedWeatherAlert(WeatherAlertKind.FROST, minimumTemperature)
        }

        return alerts.sortedByDescending { it.kind.priority }
    }

    fun detectRapidChanges(
        settings: AppSettings,
        previous: WeatherObservation?,
        current: WeatherObservation
    ): List<DetectedWeatherAlert> {
        if (previous == null) return emptyList()
        val elapsed = current.timestampMillis - previous.timestampMillis
        if (elapsed <= 0L || elapsed > OBSERVATION_WINDOW_MILLIS) return emptyList()

        val alerts = mutableListOf<DetectedWeatherAlert>()
        val temperatureChange = current.temperatureCelsius - previous.temperatureCelsius
        if (
            settings.rapidTemperatureChangeNotificationsEnabled &&
            abs(temperatureChange) >= RAPID_TEMPERATURE_THRESHOLD_CELSIUS
        ) {
            alerts += DetectedWeatherAlert(
                WeatherAlertKind.RAPID_TEMPERATURE_CHANGE,
                temperatureChange
            )
        }

        val pressureDrop = previous.pressureHpa - current.pressureHpa
        if (
            settings.rapidPressureDropNotificationsEnabled &&
            pressureDrop >= RAPID_PRESSURE_DROP_THRESHOLD_HPA
        ) {
            alerts += DetectedWeatherAlert(
                WeatherAlertKind.RAPID_PRESSURE_DROP,
                pressureDrop.toDouble()
            )
        }
        return alerts.sortedByDescending { it.kind.priority }
    }

    fun shouldRefreshObservation(previous: WeatherObservation?, nowMillis: Long): Boolean =
        previous == null || nowMillis - previous.timestampMillis >= OBSERVATION_REFRESH_MILLIS

    fun createSummaryBody(
        settings: AppSettings,
        current: WeatherResponse,
        forecast: ForecastResponse?
    ): String {
        val nextDay = forecast?.list.orEmpty().filter {
            it.dt in current.dt..(current.dt + 24L * 60L * 60L)
        }
        val temperatures = buildList {
            add(current.main.temp)
            nextDay.forEach { item ->
                add(item.main.tempMin)
                add(item.main.tempMax)
            }
        }
        val minimum = temperatures.minOrNull() ?: current.main.temp
        val maximum = temperatures.maxOrNull() ?: current.main.temp
        val precipitation = nextDay.maxOfOrNull(ForecastItem::precipitationProbability)
            ?.times(100)
            ?.roundToInt()
            ?: 0
        val unit = settings.temperatureUnit.label
        val currentTemperature = formatTemperature(current.main.temp, settings)
        val minTemperature = formatTemperature(minimum, settings)
        val maxTemperature = formatTemperature(maximum, settings)
        val description = current.weather.firstOrNull()?.description.orEmpty()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val offset = ZoneOffset.ofTotalSeconds(forecast?.city?.timezone ?: current.timezone)
        val currentLocalDate = Instant.ofEpochSecond(current.dt).atOffset(offset).toLocalDate()
        val todayItems = nextDay.filter { item ->
            Instant.ofEpochSecond(item.dt).atOffset(offset).toLocalDate() == currentLocalDate
        }
        val daytimePrecipitation = todayItems
            .filter { Instant.ofEpochSecond(it.dt).atOffset(offset).hour in 6..23 }
            .maxOfOrNull(ForecastItem::precipitationProbability)
            ?.times(100)
            ?.roundToInt()
            ?: 0
        val morningTemperature = todayItems
            .filter { Instant.ofEpochSecond(it.dt).atOffset(offset).hour in 6..11 }
            .map { it.main.temp }
            .averageOrNull()
        val eveningTemperature = todayItems
            .filter { Instant.ofEpochSecond(it.dt).atOffset(offset).hour in 17..22 }
            .map { it.main.temp }
            .averageOrNull()
        return buildString {
            append("Сейчас $currentTemperature$unit")
            if (description.isNotBlank()) append(", ${description.lowercase()}")
            append(". За 24 часа: $minTemperature…$maxTemperature$unit")
            if (precipitation > 0) append(", вероятность осадков до $precipitation%")
            append('.')
            if (daytimePrecipitation >= 50) append(" Возьмите зонт.")
            if (
                morningTemperature != null &&
                eveningTemperature != null &&
                eveningTemperature - morningTemperature >= 3.0
            ) {
                append(" Утром будет холоднее, чем вечером.")
            }
        }
    }

    private fun activeWindThresholds(settings: AppSettings): Set<Int> = buildSet {
        if (settings.strongWindNotificationsEnabled) add(15)
        if (settings.alert10Enabled) add(10)
        if (settings.alert15Enabled) add(15)
        if (settings.alert20Enabled) add(20)
    }

    private fun formatTemperature(celsius: Double, settings: AppSettings): Int {
        val value = when (settings.temperatureUnit.name) {
            "FAHRENHEIT" -> celsius * 9.0 / 5.0 + 32.0
            else -> celsius
        }
        return value.roundToInt()
    }

    private const val SUMMARY_DELIVERY_WINDOW_MINUTES = 60
}

private fun List<Double>.averageOrNull(): Double? =
    if (isEmpty()) null else average()
