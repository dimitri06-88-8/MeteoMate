package com.example.meteomate.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class WeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        WeatherWidgetScheduler.startPeriodicUpdates(context)
        WeatherWidgetScheduler.refresh(context)
    }

    override fun onEnabled(context: Context) {
        WeatherWidgetScheduler.startPeriodicUpdates(context)
        WeatherWidgetScheduler.refresh(context)
    }

    override fun onDisabled(context: Context) {
        WeatherWidgetScheduler.stopPeriodicUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            WeatherWidgetScheduler.refresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_REFRESH = "com.example.meteomate.widget.REFRESH"
    }
}
