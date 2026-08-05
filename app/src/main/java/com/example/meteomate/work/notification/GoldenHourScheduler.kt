package com.example.meteomate.work.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.meteomate.data.model.OpenMeteoWindResponse
import com.example.meteomate.work.GoldenHourNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class SolarEventType {
    SUNRISE,
    SUNSET
}

data class SolarNotificationPlan(
    val type: SolarEventType,
    val eventEpochMillis: Long,
    val notificationEpochMillis: Long,
    val timezoneId: String
)

fun nextSolarNotificationPlans(
    sunriseTimes: List<String?>,
    sunsetTimes: List<String?>,
    timezoneId: String?,
    fallbackOffsetSeconds: Int,
    nowMillis: Long,
    leadMinutes: Int = GoldenHourScheduler.DEFAULT_LEAD_MINUTES
): List<SolarNotificationPlan> {
    val zone = runCatching { timezoneId?.let(ZoneId::of) }
        .getOrNull()
        ?: ZoneOffset.ofTotalSeconds(fallbackOffsetSeconds)
    val leadMillis = leadMinutes.coerceAtLeast(0) * 60_000L

    fun next(type: SolarEventType, values: List<String?>): SolarNotificationPlan? = values
        .asSequence()
        .mapNotNull { raw ->
            raw?.takeIf(String::isNotBlank)?.let { value ->
                runCatching {
                    LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            }
        }
        .map { eventMillis ->
            SolarNotificationPlan(
                type = type,
                eventEpochMillis = eventMillis,
                notificationEpochMillis = eventMillis - leadMillis,
                timezoneId = zone.id
            )
        }
        .firstOrNull { it.notificationEpochMillis > nowMillis }

    return listOfNotNull(
        next(SolarEventType.SUNRISE, sunriseTimes),
        next(SolarEventType.SUNSET, sunsetTimes)
    )
}

@Singleton
class GoldenHourScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(response: OpenMeteoWindResponse, cityName: String, nowMillis: Long = System.currentTimeMillis()) {
        val daily = response.daily ?: return
        val plans = nextSolarNotificationPlans(
            sunriseTimes = daily.sunrise.orEmpty(),
            sunsetTimes = daily.sunset.orEmpty(),
            timezoneId = response.timezone,
            fallbackOffsetSeconds = response.utcOffsetSeconds ?: 0,
            nowMillis = nowMillis
        )
        val workManager = WorkManager.getInstance(context)
        SolarEventType.entries.forEach { type ->
            val plan = plans.firstOrNull { it.type == type }
            val workName = workName(type)
            if (plan == null) {
                workManager.cancelUniqueWork(workName)
            } else {
                val input = Data.Builder()
                    .putString(GoldenHourNotificationWorker.KEY_EVENT_TYPE, type.name)
                    .putString(GoldenHourNotificationWorker.KEY_CITY_NAME, cityName)
                    .putLong(GoldenHourNotificationWorker.KEY_EVENT_TIME, plan.eventEpochMillis)
                    .putString(GoldenHourNotificationWorker.KEY_TIMEZONE_ID, plan.timezoneId)
                    .build()
                val request = OneTimeWorkRequestBuilder<GoldenHourNotificationWorker>()
                    .setInputData(input)
                    .setInitialDelay(
                        (plan.notificationEpochMillis - nowMillis).coerceAtLeast(0L),
                        TimeUnit.MILLISECONDS
                    )
                    .build()
                workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
            }
        }
    }

    fun cancel() {
        val workManager = WorkManager.getInstance(context)
        SolarEventType.entries.forEach { workManager.cancelUniqueWork(workName(it)) }
    }

    private fun workName(type: SolarEventType): String = "$WORK_PREFIX${type.name.lowercase()}"

    companion object {
        const val DEFAULT_LEAD_MINUTES = 30
        private const val WORK_PREFIX = "golden_hour_"
    }
}

