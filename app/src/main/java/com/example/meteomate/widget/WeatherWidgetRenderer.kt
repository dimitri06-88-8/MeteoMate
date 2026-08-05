package com.example.meteomate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.meteomate.MainActivity
import com.example.meteomate.R
import com.example.meteomate.data.TemperatureUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object WeatherWidgetRenderer {
    fun updateAll(
        context: Context,
        snapshot: WidgetWeatherSnapshot?,
        temperatureUnit: TemperatureUnit,
        status: String
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, WeatherWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        ids.forEach { id -> manager.updateAppWidget(id, createViews(context, snapshot, temperatureUnit, status)) }
    }

    fun showEmpty(context: Context, message: String) {
        updateAll(context, null, TemperatureUnit.CELSIUS, message)
    }

    private fun createViews(
        context: Context,
        snapshot: WidgetWeatherSnapshot?,
        temperatureUnit: TemperatureUnit,
        status: String
    ): RemoteViews = RemoteViews(context.packageName, R.layout.weather_widget).apply {
        setTextViewText(R.id.widget_city, snapshot?.cityName ?: context.getString(R.string.app_name))
        setTextViewText(
            R.id.widget_temperature,
            snapshot?.let { formatTemperature(it.temperatureCelsius, temperatureUnit) } ?: "—"
        )
        setTextViewText(
            R.id.widget_precipitation,
            snapshot?.let {
                context.getString(
                    R.string.weather_widget_precipitation_value,
                    formatDecimal(it.precipitationMillimeters),
                    it.precipitationProbability
                )
            } ?: context.getString(R.string.weather_widget_precipitation_empty)
        )
        setTextViewText(
            R.id.widget_wind,
            snapshot?.let {
                context.getString(R.string.weather_widget_wind_value, formatDecimal(it.windSpeedMs))
            } ?: context.getString(R.string.weather_widget_wind_empty)
        )
        val updated = snapshot?.let(::formatUpdatedTime)
        setTextViewText(
            R.id.widget_status,
            if (updated == null) status else "$status · $updated"
        )
        setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent(context))
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            4101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
            action = WeatherWidgetProvider.ACTION_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            4102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
        val value = if (unit == TemperatureUnit.FAHRENHEIT) celsius * 9.0 / 5.0 + 32.0 else celsius
        val suffix = if (unit == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
        return "${value.roundToInt()}$suffix"
    }

    private fun formatDecimal(value: Double): String =
        if (value == value.roundToInt().toDouble()) {
            value.roundToInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }

    private fun formatUpdatedTime(snapshot: WidgetWeatherSnapshot): String {
        val zone = runCatching { ZoneId.of(snapshot.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        return Instant.ofEpochMilli(snapshot.updatedAtMillis).atZone(zone).format(TIME_FORMATTER)
    }

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
}
