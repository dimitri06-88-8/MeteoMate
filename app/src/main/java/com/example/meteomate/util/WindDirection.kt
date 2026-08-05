package com.example.meteomate.util

import java.util.Locale
import kotlin.math.roundToInt

data class WindDirectionLabel(
    val abbreviation: String,
    val name: String
)

private val WIND_DIRECTIONS = listOf(
    WindDirectionLabel("С", "северный"),
    WindDirectionLabel("СВ", "северо-восточный"),
    WindDirectionLabel("В", "восточный"),
    WindDirectionLabel("ЮВ", "юго-восточный"),
    WindDirectionLabel("Ю", "южный"),
    WindDirectionLabel("ЮЗ", "юго-западный"),
    WindDirectionLabel("З", "западный"),
    WindDirectionLabel("СЗ", "северо-западный")
)

fun windDirectionLabel(degrees: Int): WindDirectionLabel {
    val normalized = ((degrees % 360) + 360) % 360
    val index = ((normalized + 22.5) / 45.0).toInt() % WIND_DIRECTIONS.size
    return WIND_DIRECTIONS[index]
}

fun formatWindValue(value: Double): String =
    if (value == value.roundToInt().toDouble()) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
