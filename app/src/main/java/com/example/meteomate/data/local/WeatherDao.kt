package com.example.meteomate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {

    @Query("SELECT * FROM cached_weather WHERE cityId = :cityId")
    suspend fun getCachedWeather(cityId: Long): CachedWeather?

    @Query("SELECT * FROM cached_weather WHERE lat = :lat AND lon = :lon ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getCachedWeatherByLocation(lat: Double, lon: Double): CachedWeather?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheWeather(weather: CachedWeather)

    @Query("DELETE FROM cached_weather WHERE lastUpdated < :threshold")
    suspend fun deleteExpiredCache(threshold: Long)

    @Query("SELECT * FROM favorite_cities ORDER BY name ASC")
    suspend fun getFavoriteCities(): List<FavoriteCity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteCity(city: FavoriteCity)

    @Query("DELETE FROM favorite_cities WHERE id = :cityId")
    suspend fun removeFavoriteCity(cityId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_cities WHERE lat = :lat AND lon = :lon)")
    suspend fun isFavorite(lat: Double, lon: Double): Boolean

    @Query("SELECT * FROM recent_searches ORDER BY lastSearched DESC LIMIT 10")
    suspend fun getRecentSearches(): List<RecentSearch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentSearch(search: RecentSearch)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
