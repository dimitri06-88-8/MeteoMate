package com.example.meteomate.data.api

import com.example.meteomate.data.model.NoaaPlanetaryKpEntry
import retrofit2.http.GET

interface SpaceWeatherApi {
    @GET("products/noaa-planetary-k-index-forecast.json")
    suspend fun getPlanetaryKpForecast(): List<NoaaPlanetaryKpEntry>
}
