package com.example.meteomate.util

import com.example.meteomate.R
import com.example.meteomate.data.TemperatureUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object WeatherCode {
    private val descriptions: Map<IntRange, Int> = mapOf(
        200..299 to R.string.thunderstorm,
        300..399 to R.string.drizzle,
        500..599 to R.string.rain,
        600..699 to R.string.snowfall,
        700..799 to R.string.fog,
        800..800 to R.string.clear_sky,
        801..802 to R.string.partly_cloudy,
        803..804 to R.string.overcast
    )

    private val emojis: Map<IntRange, String> = mapOf(
        200..299 to "\u26C8\uFE0F",
        300..399 to "\uD83C\uDF26",
        500..599 to "\uD83C\uDF27",
        600..699 to "\u2744\uFE0F",
        700..799 to "\uD83C\uDF2B",
        800..800 to "\u2600\uFE0F",
        801..802 to "\u26C5",
        803..804 to "\u2601\uFE0F"
    )

    fun description(code: Int): Int =
        descriptions.entries.firstOrNull { code in it.key }?.value ?: R.string.clear_sky

    fun emoji(code: Int): String =
        emojis.entries.firstOrNull { code in it.key }?.value ?: "\u2600\uFE0F"
}

object OpenMeteoWeatherCode {
    /** Converts WMO weather interpretation codes to the OpenWeather code ranges used by the UI. */
    fun toOpenWeather(code: Int?): Int = when (code) {
        0 -> 800
        1, 2 -> 802
        3 -> 804
        45, 48 -> 741
        51, 53, 55, 56, 57 -> 300
        61, 63, 65, 66, 67, 80, 81, 82 -> 500
        71, 73, 75, 77, 85, 86 -> 600
        95, 96, 99 -> 200
        else -> 800
    }
}

object TemperatureFormatter {
    private const val CELSIUS = "\u00B0"

    fun format(temp: Double): String = "${temp.toInt()}$CELSIUS"

    fun format(tempCelsius: Double, unit: TemperatureUnit): String {
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> tempCelsius
            TemperatureUnit.FAHRENHEIT -> tempCelsius * 9.0 / 5.0 + 32.0
        }
        val suffix = if (unit == TemperatureUnit.CELSIUS) "C" else "F"
        return "${value.toInt()}°$suffix"
    }
}

object DateUtils {
    private val apiFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun isToday(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date == LocalDate.now()
        } catch (_: Exception) {
            false
        }
    }

    fun extractDate(dateStr: String): String {
        return try {
            val dateTime = LocalDateTime.parse(dateStr, apiFormatter)
            dateTime.format(dateFormatter)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatDay(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatTime(dateTimeStr: String): String {
        return try {
            val dateTime = LocalDateTime.parse(dateTimeStr, apiFormatter)
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            dateTimeStr.substringOrEmpty(11, 16)
        }
    }

    fun formatTimeFromISO(isoStr: String): String {
        return try {
            val clean = isoStr.removeSuffix("Z")
            val dateTime = if (clean.contains("T")) {
                LocalDateTime.parse(clean)
            } else {
                LocalDateTime.parse("${clean}T00:00:00")
            }
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            isoStr.takeLast(5)
        }
    }
}

private fun String.substringOrEmpty(startIndex: Int, endIndex: Int): String {
    return try {
        substring(startIndex, endIndex)
    } catch (_: Exception) {
        this
    }
}
