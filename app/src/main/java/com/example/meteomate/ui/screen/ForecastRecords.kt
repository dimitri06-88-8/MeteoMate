package com.example.meteomate.ui.screen

data class ForecastRecords(
    val hottest: DailyForecastItem,
    val coldest: DailyForecastItem,
    val windiest: DailyForecastItem
)

fun calculateForecastRecords(forecast: List<DailyForecastItem>): ForecastRecords? {
    if (forecast.isEmpty()) return null
    return ForecastRecords(
        hottest = forecast.maxBy { it.maxTemp },
        coldest = forecast.minBy { it.minTemp },
        windiest = forecast.maxBy { it.maxWindSpeed }
    )
}
