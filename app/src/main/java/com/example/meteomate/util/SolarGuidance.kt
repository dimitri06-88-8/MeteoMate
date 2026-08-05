package com.example.meteomate.util

enum class UvRiskLevel {
    UNKNOWN,
    LOW,
    MODERATE_TO_HIGH,
    VERY_HIGH_TO_EXTREME
}

data class UvGuidance(
    val level: UvRiskLevel,
    val label: String,
    val protectionReminderMinutes: Int?,
    val advice: String
)

/**
 * Uses the public UV Index protection bands. The duration is only a reminder interval to
 * re-check sun protection; it must not be interpreted as a guaranteed burn-free exposure time.
 */
fun uvGuidance(uvIndex: Double?): UvGuidance = when {
    uvIndex == null || !uvIndex.isFinite() || uvIndex < 0.0 -> UvGuidance(
        level = UvRiskLevel.UNKNOWN,
        label = "Нет данных",
        protectionReminderMinutes = null,
        advice = "Обновите прогноз, чтобы получить рекомендации по защите от солнца."
    )
    uvIndex < 3.0 -> UvGuidance(
        level = UvRiskLevel.LOW,
        label = "Низкий риск",
        protectionReminderMinutes = 60,
        advice = "Обычно достаточно минимальной защиты, но учитывайте чувствительность кожи."
    )
    uvIndex < 8.0 -> UvGuidance(
        level = UvRiskLevel.MODERATE_TO_HIGH,
        label = "Нужна защита",
        protectionReminderMinutes = 30,
        advice = "Ищите тень, используйте закрытую одежду, очки и солнцезащитное средство."
    )
    else -> UvGuidance(
        level = UvRiskLevel.VERY_HIGH_TO_EXTREME,
        label = "Нужна усиленная защита",
        protectionReminderMinutes = 15,
        advice = "Сократите пребывание под прямым солнцем, особенно около полудня."
    )
}

