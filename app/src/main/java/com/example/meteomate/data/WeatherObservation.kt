package com.example.meteomate.data

const val WEATHER_OBSERVATION_COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L

enum class WeatherObservationKind(val label: String, val emoji: String) {
    DRY("Без осадков", "☀️"), DRIZZLE("Морось", "🌦️"), RAIN("Дождь", "🌧️"),
    SNOW("Снег", "🌨️"), THUNDERSTORM("Гроза", "⛈️")
}

data class WeatherObservationProgress(
    val totalReports: Int = 0,
    val lastReportAt: Long = 0L,
    val lastKind: WeatherObservationKind? = null,
    val lastLocation: String = "",
    val secretBadgeUnlocked: Boolean = false
)

data class MeteoBadge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val requiredReports: Int? = null,
    val isSecret: Boolean = false
)

val meteoBadges = listOf(
    MeteoBadge("first_signal", "Первый сигнал", "Отправьте первое наблюдение", "📡", 1),
    MeteoBadge("sky_scout", "Разведчик неба", "Отправьте 7 наблюдений", "🔭", 7),
    MeteoBadge("storm_crystal", "Штормовой кристалл", "Отправьте 20 наблюдений", "💠", 20),
    MeteoBadge("secret_resonance", "Резонанс атмосферы", "Скрытое условие", "✨", isSecret = true)
)

fun MeteoBadge.isUnlocked(progress: WeatherObservationProgress): Boolean =
    if (isSecret) progress.secretBadgeUnlocked
    else progress.totalReports >= (requiredReports ?: Int.MAX_VALUE)
