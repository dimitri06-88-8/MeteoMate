package com.example.meteomate.data.api

import com.example.meteomate.data.model.ForecastResponse
import com.example.meteomate.data.model.GeocodingResponse
import com.example.meteomate.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appId: String = ApiConstants.WEATHER_API_KEY,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): WeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appId: String = ApiConstants.WEATHER_API_KEY,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): ForecastResponse

    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("appid") appId: String = ApiConstants.WEATHER_API_KEY
    ): List<GeocodingResponse>
}
