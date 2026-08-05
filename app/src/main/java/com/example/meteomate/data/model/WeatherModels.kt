package com.example.meteomate.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val coord: Coordinates,
    val weather: List<WeatherCondition>,
    val main: MainData,
    val visibility: Int?,
    val wind: WindData,
    val clouds: CloudsData?,
    val dt: Long,
    val sys: SystemData,
    val timezone: Int,
    val id: Long,
    val name: String,
    val cod: Int
)

data class Coordinates(
    val lon: Double,
    val lat: Double
)

data class WeatherCondition(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class MainData(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("sea_level") val seaLevel: Int?,
    @SerializedName("grnd_level") val grndLevel: Int?
)

data class WindData(
    val speed: Double,
    val deg: Int?,
    val gust: Double?
)

data class CloudsData(
    val all: Int
)

data class SystemData(
    val country: String?,
    val sunrise: Long,
    val sunset: Long
)

data class ForecastResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastItem>,
    val city: ForecastCity
)

data class ForecastItem(
    val dt: Long,
    val main: MainData,
    val weather: List<WeatherCondition>,
    val clouds: CloudsData?,
    val wind: WindData,
    val visibility: Int?,
    @SerializedName("pop") val precipitationProbability: Double,
    @SerializedName("dt_txt") val dtText: String
)

data class ForecastCity(
    val id: Long,
    val name: String,
    val coord: Coordinates,
    val country: String?,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

data class GeocodingResponse(
    val name: String,
    @SerializedName("local_names") val localNames: Map<String, String>?,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?
) {
    fun localizedName(languageCode: String = "ru"): String =
        localNames?.get(languageCode)?.takeIf { it.isNotBlank() } ?: name
}
