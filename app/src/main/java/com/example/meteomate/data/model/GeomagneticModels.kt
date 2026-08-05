package com.example.meteomate.data.model

import com.google.gson.annotations.SerializedName

data class NoaaPlanetaryKpEntry(
    @SerializedName("time_tag") val timeTag: String,
    val kp: Double?,
    val observed: String?,
    @SerializedName("noaa_scale") val noaaScale: String?
)

data class GeomagneticSnapshot(
    val currentKp: Double,
    val currentTime: String,
    val currentScale: String? = null,
    val maximumForecastKp: Double? = null,
    val forecastPeakTime: String? = null,
    val forecastScale: String? = null
)
