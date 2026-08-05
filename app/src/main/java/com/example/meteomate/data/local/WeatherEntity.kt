package com.example.meteomate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_weather")
data class CachedWeather(
    @PrimaryKey val cityId: Long,
    val cityName: String,
    val lat: Double,
    val lon: Double,
    val jsonData: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_cities")
data class FavoriteCity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val state: String? = null
)

@Entity(tableName = "recent_searches")
data class RecentSearch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val state: String? = null,
    val lastSearched: Long = System.currentTimeMillis()
)
