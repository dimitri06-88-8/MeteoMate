package com.example.meteomate.data.api

import com.example.meteomate.data.model.OpenMeteoAirQualityResponse
import com.example.meteomate.data.model.OpenMeteoGeocodingResponse
import com.example.meteomate.data.model.OpenMeteoWindResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    companion object {
        const val CURRENT_VARIABLES =
            "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code," +
                "surface_pressure,cloud_cover,visibility,wind_speed_10m," +
                "wind_direction_10m,wind_gusts_10m,precipitation,rain,showers,snowfall"
        const val HOURLY_VARIABLES =
            "temperature_2m,apparent_temperature,weather_code,precipitation_probability," +
                "precipitation,surface_pressure,relative_humidity_2m,cloud_cover,visibility," +
                "uv_index,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day"
        const val DAILY_VARIABLES =
            "weather_code,temperature_2m_max,temperature_2m_min," +
                "precipitation_probability_max,wind_speed_10m_max,sunrise,sunset,uv_index_max"
    }

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchCities(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "ru",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse

    @GET("v1/forecast")
    suspend fun getWindForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = CURRENT_VARIABLES,
        @Query("hourly") hourly: String = HOURLY_VARIABLES,
        @Query("daily") daily: String = DAILY_VARIABLES,
        @Query("minutely_15") minutely15: String = "precipitation",
        @Query("forecast_minutely_15") forecastMinutely15: Int = 8,
        @Query("forecast_hours") forecastHours: Int = 48,
        @Query("past_hours") pastHours: Int = 24,
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("models") model: String = "best_match",
        @Query("timezone") timezone: String = "auto",
        @Query("wind_speed_unit") windSpeedUnit: String = "ms"
    ): OpenMeteoWindResponse

    @GET("v1/forecast")
    suspend fun getCurrentWind(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        @Query("models") model: String,
        @Query("timezone") timezone: String = "auto",
        @Query("wind_speed_unit") windSpeedUnit: String = "ms"
    ): OpenMeteoWindResponse

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "us_aqi,pm2_5,pm10",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoAirQualityResponse
}
