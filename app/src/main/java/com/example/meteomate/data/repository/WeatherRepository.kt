package com.example.meteomate.data.repository

import com.example.meteomate.data.api.OpenMeteoApi
import com.example.meteomate.data.api.WeatherApi
import com.example.meteomate.data.api.SpaceWeatherApi
import com.example.meteomate.data.model.ForecastResponse
import com.example.meteomate.data.model.GeocodingResponse
import com.example.meteomate.data.model.OpenMeteoWindResponse
import com.example.meteomate.data.model.OpenMeteoAirQualityResponse
import com.example.meteomate.data.model.WeatherResponse
import com.example.meteomate.data.model.NoaaPlanetaryKpEntry
import com.example.meteomate.util.Resource
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val openMeteoApi: OpenMeteoApi,
    private val spaceWeatherApi: SpaceWeatherApi
) {

    private var geomagneticCache: CachedGeomagnetic? = null

    suspend fun getCurrentWeather(lat: Double, lon: Double): Resource<WeatherResponse> {
        return safeApiCall { api.getCurrentWeather(lat, lon) }
    }

    suspend fun getForecast(lat: Double, lon: Double): Resource<ForecastResponse> {
        return safeApiCall { api.getForecast(lat, lon) }
    }

    suspend fun searchCities(query: String): Resource<List<GeocodingResponse>> {
        return safeApiCall {
            openMeteoApi.searchCities(query).results.orEmpty().map { it.toGeocodingResponse() }
        }
    }

    suspend fun getWindFromOpenMeteo(
        lat: Double,
        lon: Double,
        modelCode: String
    ): Resource<OpenMeteoWindResponse> {
        return safeApiCall {
            openMeteoApi.getWindForecast(
                lat = lat,
                lon = lon,
                model = modelCode
            )
        }
    }

    suspend fun getHourlyForecast(
        lat: Double,
        lon: Double
    ): Resource<OpenMeteoWindResponse> {
        return safeApiCall {
            openMeteoApi.getWindForecast(
                lat = lat,
                lon = lon,
                model = "best_match"
            )
        }
    }

    suspend fun getAirQuality(lat: Double, lon: Double): Resource<OpenMeteoAirQualityResponse> {
        return safeApiCall { openMeteoApi.getAirQuality(lat, lon) }
    }

    suspend fun getGeomagneticForecast(): Resource<List<NoaaPlanetaryKpEntry>> {
        val now = System.currentTimeMillis()
        geomagneticCache?.takeIf { now - it.savedAt < GEOMAGNETIC_REFRESH_MILLIS }?.let {
            return Resource.Success(it.entries)
        }
        return when (val result = safeApiCall { spaceWeatherApi.getPlanetaryKpForecast() }) {
            is Resource.Success -> {
                geomagneticCache = CachedGeomagnetic(now, result.data)
                result
            }
            is Resource.Error -> geomagneticCache
                ?.takeIf { now - it.savedAt < GEOMAGNETIC_STALE_MILLIS }
                ?.let { Resource.Success(it.entries) }
                ?: result
            is Resource.Loading -> result
        }
    }

    suspend fun getCurrentWindForModel(
        lat: Double,
        lon: Double,
        modelCode: String
    ): Resource<OpenMeteoWindResponse> = safeApiCall {
        openMeteoApi.getCurrentWind(lat = lat, lon = lon, model = modelCode)
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Resource<T> {
        return try {
            Resource.Success(call())
        } catch (e: HttpException) {
            val message = when (e.code()) {
                400, 422 -> "Источник не поддерживает выбранную модель или регион"
                401 -> "Не удалось авторизоваться в источнике погоды"
                404 -> "Город не найден"
                429 -> "Слишком много запросов. Повторите немного позже"
                in 500..599 -> "Источник погоды временно недоступен"
                else -> "Ошибка источника погоды: ${e.code()}"
            }
            Resource.Error(message, e)
        } catch (e: SocketTimeoutException) {
            Resource.Error("Источник погоды не ответил вовремя", e)
        } catch (e: UnknownHostException) {
            Resource.Error("Нет подключения к интернету", e)
        } catch (e: IOException) {
            Resource.Error("Ошибка сети. Проверьте подключение", e)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Неизвестная ошибка", e)
        }
    }

    private data class CachedGeomagnetic(
        val savedAt: Long,
        val entries: List<NoaaPlanetaryKpEntry>
    )

    private companion object {
        const val GEOMAGNETIC_REFRESH_MILLIS = 15L * 60L * 1000L
        const val GEOMAGNETIC_STALE_MILLIS = 6L * 60L * 60L * 1000L
    }
}
