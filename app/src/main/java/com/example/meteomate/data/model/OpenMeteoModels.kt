package com.example.meteomate.data.model

import com.google.gson.annotations.SerializedName

data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult>? = emptyList()
)

data class OpenMeteoGeocodingResult(
    val id: Long? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    val admin1: String? = null
) {
    fun toGeocodingResponse() = GeocodingResponse(
        name = name,
        localNames = mapOf("ru" to name),
        lat = latitude,
        lon = longitude,
        country = country ?: countryCode,
        state = admin1
    )
}

data class OpenMeteoWindResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: OpenMeteoCurrentWind? = null,
    val hourly: OpenMeteoHourlyWind? = null,
    val daily: OpenMeteoDailyWeather? = null,
    @SerializedName("minutely_15") val minutely15: OpenMeteoMinutely15? = null
)

data class OpenMeteoCurrentWind(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("surface_pressure") val surfacePressure: Double? = null,
    @SerializedName("cloud_cover") val cloudCover: Int? = null,
    @SerializedName("visibility") val visibility: Double? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Int? = null,
    @SerializedName("wind_gusts_10m") val windGusts: Double? = null,
    @SerializedName("precipitation") val precipitation: Double? = null,
    @SerializedName("rain") val rain: Double? = null,
    @SerializedName("showers") val showers: Double? = null,
    @SerializedName("snowfall") val snowfall: Double? = null,
    @SerializedName("time") val time: String? = null
)

data class OpenMeteoHourlyWind(
    @SerializedName("time") private val rawTime: List<String?>? = emptyList(),
    @SerializedName("temperature_2m") private val rawTemperature: List<Double?>? = emptyList(),
    @SerializedName("apparent_temperature") private val rawApparentTemperature: List<Double?>? = emptyList(),
    @SerializedName("weather_code") private val rawWeatherCode: List<Int?>? = emptyList(),
    @SerializedName("precipitation_probability") private val rawPrecipitationProbability: List<Int?>? = emptyList(),
    @SerializedName("precipitation") private val rawPrecipitation: List<Double?>? = emptyList(),
    @SerializedName("surface_pressure") private val rawPressure: List<Double?>? = emptyList(),
    @SerializedName("relative_humidity_2m") private val rawHumidity: List<Int?>? = emptyList(),
    @SerializedName("cloud_cover") private val rawCloudCover: List<Int?>? = emptyList(),
    @SerializedName("visibility") private val rawVisibility: List<Double?>? = emptyList(),
    @SerializedName("uv_index") private val rawUvIndex: List<Double?>? = emptyList(),
    @SerializedName("wind_speed_10m") private val rawWindSpeed: List<Double?>? = emptyList(),
    @SerializedName("wind_direction_10m") private val rawWindDirection: List<Int?>? = emptyList(),
    @SerializedName("wind_gusts_10m") private val rawWindGusts: List<Double?>? = emptyList(),
    @SerializedName("is_day") private val rawIsDay: List<Int?>? = emptyList()
) {
    val time: List<String> get() = rawTime.orEmpty().map { it.orEmpty() }
    val temperature: List<Double?> get() = rawTemperature.orEmpty()
    val apparentTemperature: List<Double?> get() = rawApparentTemperature.orEmpty()
    val weatherCode: List<Int?> get() = rawWeatherCode.orEmpty()
    val precipitationProbability: List<Int?> get() = rawPrecipitationProbability.orEmpty()
    val precipitation: List<Double?> get() = rawPrecipitation.orEmpty()
    val pressure: List<Double?> get() = rawPressure.orEmpty()
    val humidity: List<Int?> get() = rawHumidity.orEmpty()
    val cloudCover: List<Int?> get() = rawCloudCover.orEmpty()
    val visibility: List<Double?> get() = rawVisibility.orEmpty()
    val uvIndex: List<Double?> get() = rawUvIndex.orEmpty()
    val windSpeed: List<Double?> get() = rawWindSpeed.orEmpty()
    val windDirection: List<Int?> get() = rawWindDirection.orEmpty()
    val windGusts: List<Double?> get() = rawWindGusts.orEmpty()
    val isDay: List<Int?> get() = rawIsDay.orEmpty()
}

data class OpenMeteoDailyWeather(
    val time: List<String?>? = emptyList(),
    @SerializedName("weather_code") val weatherCode: List<Int?>? = emptyList(),
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double?>? = emptyList(),
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double?>? = emptyList(),
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = emptyList(),
    @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double?>? = emptyList(),
    val sunrise: List<String?>? = emptyList(),
    val sunset: List<String?>? = emptyList(),
    @SerializedName("uv_index_max") val uvIndexMax: List<Double?>? = emptyList()
)

data class OpenMeteoAirQualityResponse(
    val current: OpenMeteoCurrentAirQuality? = null
)

data class OpenMeteoCurrentAirQuality(
    val time: String? = null,
    @SerializedName("us_aqi") val usAqi: Int? = null,
    @SerializedName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null
)

data class OpenMeteoMinutely15(
    @SerializedName("time") private val rawTime: List<String?>? = emptyList(),
    @SerializedName("precipitation") private val rawPrecipitation: List<Double?>? = emptyList()
) {
    val time: List<String> get() = rawTime.orEmpty().map { it.orEmpty() }
    val precipitation: List<Double?> get() = rawPrecipitation.orEmpty()
}
