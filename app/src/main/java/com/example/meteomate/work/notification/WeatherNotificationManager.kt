package com.example.meteomate.work.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.meteomate.MainActivity
import com.example.meteomate.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val alertsChannel = NotificationChannel(
            ALERTS_CHANNEL_ID,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alerts_description)
        }
        val summaryChannel = NotificationChannel(
            SUMMARY_CHANNEL_ID,
            context.getString(R.string.notification_channel_summary),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_summary_description)
        }
        val solarChannel = NotificationChannel(
            SOLAR_CHANNEL_ID,
            context.getString(R.string.notification_channel_solar),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_solar_description)
        }
        val uvTimerChannel = NotificationChannel(
            UV_TIMER_CHANNEL_ID,
            context.getString(R.string.notification_channel_uv_timer),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_uv_timer_description)
        }
        manager.createNotificationChannels(
            listOf(alertsChannel, summaryChannel, solarChannel, uvTimerChannel)
        )
    }

    fun canPostNotifications(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)

    fun postMorningSummary(cityName: String, body: String): Boolean {
        if (!canPostNotifications() || !isChannelEnabled(SUMMARY_CHANNEL_ID)) return false
        val title = context.getString(R.string.notification_morning_summary_title, cityName)
        val notification = baseBuilder(SUMMARY_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return notifySafely(SUMMARY_NOTIFICATION_ID, notification)
    }

    fun postGoldenHour(
        cityName: String,
        type: SolarEventType,
        eventEpochMillis: Long,
        timezoneId: String
    ): Boolean {
        if (!canPostNotifications() || !isChannelEnabled(SOLAR_CHANNEL_ID)) return false
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val eventTime = Instant.ofEpochMilli(eventEpochMillis).atZone(zone).format(TIME_FORMATTER)
        val title = when (type) {
            SolarEventType.SUNRISE -> context.getString(R.string.notification_golden_hour_morning_title)
            SolarEventType.SUNSET -> context.getString(R.string.notification_golden_hour_evening_title)
        }
        val body = when (type) {
            SolarEventType.SUNRISE -> context.getString(
                R.string.notification_golden_hour_morning_body,
                cityName,
                eventTime
            )
            SolarEventType.SUNSET -> context.getString(
                R.string.notification_golden_hour_evening_body,
                cityName,
                eventTime
            )
        }
        val notification = baseBuilder(SOLAR_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val id = if (type == SolarEventType.SUNRISE) {
            MORNING_GOLDEN_HOUR_NOTIFICATION_ID
        } else {
            EVENING_GOLDEN_HOUR_NOTIFICATION_ID
        }
        return notifySafely(id, notification)
    }

    fun postUvTimerFinished(): Boolean {
        if (!canPostNotifications() || !isChannelEnabled(UV_TIMER_CHANNEL_ID)) return false
        val title = context.getString(R.string.notification_uv_timer_title)
        val body = context.getString(R.string.notification_uv_timer_body)
        val notification = baseBuilder(UV_TIMER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return notifySafely(UV_TIMER_NOTIFICATION_ID, notification)
    }

    fun postWeatherAlerts(
        cityName: String,
        alerts: List<DetectedWeatherAlert>,
        notificationKey: String = cityName
    ): Boolean {
        if (!canPostNotifications() || !isChannelEnabled(ALERTS_CHANNEL_ID) || alerts.isEmpty()) return false
        val messages = alerts.map(::alertMessage)
        val body = messages.joinToString(separator = "\n") { "• $it" }
        val title = context.getString(R.string.notification_weather_alert_title, cityName)
        val notification = baseBuilder(ALERTS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(messages.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        val notificationId = ALERT_NOTIFICATION_ID_BASE +
            ((notificationKey.hashCode() and Int.MAX_VALUE) % MAX_CITY_NOTIFICATION_IDS)
        return notifySafely(notificationId, notification)
    }

    private fun alertMessage(alert: DetectedWeatherAlert): String = when (alert.kind) {
        WeatherAlertKind.THUNDERSTORM -> context.getString(R.string.notification_alert_thunderstorm)
        WeatherAlertKind.ICE -> context.getString(R.string.notification_alert_ice)
        WeatherAlertKind.SNOW -> context.getString(R.string.notification_alert_snow)
        WeatherAlertKind.RAIN -> context.getString(R.string.notification_alert_rain)
        WeatherAlertKind.WIND -> context.getString(
            R.string.notification_alert_wind,
            alert.value?.roundToInt() ?: alert.threshold ?: 15
        )
        WeatherAlertKind.HEAT -> context.getString(
            R.string.notification_alert_heat,
            alert.value?.roundToInt() ?: 35
        )
        WeatherAlertKind.FROST -> context.getString(
            R.string.notification_alert_frost,
            alert.value?.roundToInt() ?: -20
        )
        WeatherAlertKind.RAPID_TEMPERATURE_CHANGE -> context.getString(
            R.string.notification_alert_rapid_temperature,
            abs(alert.value ?: 0.0).roundToInt()
        )
        WeatherAlertKind.RAPID_PRESSURE_DROP -> context.getString(
            R.string.notification_alert_rapid_pressure,
            alert.value?.roundToInt() ?: 5
        )
    }

    private fun baseBuilder(channelId: String): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_weather)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
    }

    private fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(channelId)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(id: Int, notification: android.app.Notification): Boolean {
        if (!canPostNotifications()) return false
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val ALERTS_CHANNEL_ID = "weather_alerts"
        const val SUMMARY_CHANNEL_ID = "morning_summary"
        const val SOLAR_CHANNEL_ID = "solar_events"
        const val UV_TIMER_CHANNEL_ID = "uv_timer"
        private const val ALERT_NOTIFICATION_ID_BASE = 10_000
        private const val SUMMARY_NOTIFICATION_ID = 1202
        private const val MORNING_GOLDEN_HOUR_NOTIFICATION_ID = 1203
        private const val EVENING_GOLDEN_HOUR_NOTIFICATION_ID = 1204
        private const val UV_TIMER_NOTIFICATION_ID = 1205
        private const val MAX_CITY_NOTIFICATION_IDS = 10_000
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }
}
