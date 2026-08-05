package com.example.meteomate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedWeather::class, FavoriteCity::class, RecentSearch::class],
    version = 2,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
