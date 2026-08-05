package com.example.meteomate.widget

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WidgetWeatherSnapshot(
    val cityName: String,
    val lat: Double,
    val lon: Double,
    val temperatureCelsius: Double,
    val precipitationMillimeters: Double,
    val precipitationProbability: Int,
    val windSpeedMs: Double,
    val updatedAtMillis: Long,
    val timezoneId: String
) {
    fun isValid(): Boolean =
        cityName.isNotBlank() &&
            lat.isFinite() && lon.isFinite() &&
            temperatureCelsius.isFinite() &&
            precipitationMillimeters.isFinite() &&
            windSpeedMs.isFinite() &&
            precipitationProbability in 0..100 &&
            updatedAtMillis > 0L

    fun matches(lat: Double, lon: Double): Boolean =
        kotlin.math.abs(this.lat - lat) < 0.0005 &&
            kotlin.math.abs(this.lon - lon) < 0.0005
}

fun nearestHourlyIndex(times: List<String>, currentLocalIsoTime: String?): Int {
    if (times.isEmpty()) return -1
    val current = currentLocalIsoTime?.let(::parseLocalDateTime) ?: return 0
    val exact = times.indexOfFirst { parseLocalDateTime(it) == current }
    if (exact >= 0) return exact
    return times.indexOfFirst { time ->
        parseLocalDateTime(time)?.let { !it.isBefore(current) } == true
    }.takeIf { it >= 0 } ?: times.lastIndex
}

private fun parseLocalDateTime(value: String): LocalDateTime? = runCatching {
    LocalDateTime.parse(value.removeSuffix("Z"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}.getOrNull()

