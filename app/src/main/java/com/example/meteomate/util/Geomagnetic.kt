package com.example.meteomate.util

import com.example.meteomate.data.model.GeomagneticSnapshot
import com.example.meteomate.data.model.NoaaPlanetaryKpEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun buildGeomagneticSnapshot(
    entries: List<NoaaPlanetaryKpEntry>,
    now: Instant = Instant.now()
): GeomagneticSnapshot? {
    val valid = entries.mapNotNull { entry ->
        val kp = entry.kp?.takeIf { it in 0.0..9.0 } ?: return@mapNotNull null
        val time = parseNoaaTime(entry.timeTag) ?: return@mapNotNull null
        ParsedKp(entry, kp, time)
    }
    val past = valid.filter { !it.time.isAfter(now) }
    val current = past
        .filter { it.entry.observed.equals("observed", true) || it.entry.observed.equals("estimated", true) }
        .maxByOrNull { it.time }
        ?: past.maxByOrNull { it.time }
        ?: return null

    val forecastEnd = now.plus(Duration.ofHours(24))
    val forecast = valid.filter {
        it.entry.observed.equals("predicted", true) &&
            !it.time.isBefore(now) && !it.time.isAfter(forecastEnd)
    }
    val peak = forecast.maxByOrNull { it.kp }

    return GeomagneticSnapshot(
        currentKp = current.kp,
        currentTime = current.time.toString(),
        currentScale = current.entry.noaaScale,
        maximumForecastKp = peak?.kp,
        forecastPeakTime = peak?.time?.toString(),
        forecastScale = peak?.entry?.noaaScale
    )
}

fun geomagneticActivityLabel(kp: Double, noaaScale: String? = null): String =
    when (noaaScale?.uppercase()) {
        "G1" -> "Слабая магнитная буря (G1)"
        "G2" -> "Умеренная магнитная буря (G2)"
        "G3" -> "Сильная магнитная буря (G3)"
        "G4" -> "Очень сильная магнитная буря (G4)"
        "G5" -> "Экстремальная магнитная буря (G5)"
        else -> when {
            kp >= 9 -> "Экстремальная магнитная буря"
            kp >= 8 -> "Очень сильная магнитная буря"
            kp >= 7 -> "Сильная магнитная буря"
            kp >= 6 -> "Умеренная магнитная буря"
            kp >= 5 -> "Слабая магнитная буря"
            kp >= 4 -> "Повышенная активность"
            else -> "Спокойная обстановка"
        }
    }

private fun parseNoaaTime(value: String): Instant? = runCatching {
    if (value.endsWith("Z", ignoreCase = true)) Instant.parse(value)
    else LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
}.getOrNull()

private data class ParsedKp(
    val entry: NoaaPlanetaryKpEntry,
    val kp: Double,
    val time: Instant
)
