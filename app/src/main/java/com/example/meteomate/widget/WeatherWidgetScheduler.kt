package com.example.meteomate.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WeatherWidgetScheduler {
    fun refresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherWidgetWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun startPeriodicUpdates(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherWidgetWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun stopPeriodicUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    private const val REFRESH_WORK_NAME = "weather_widget_refresh"
    private const val PERIODIC_WORK_NAME = "weather_widget_periodic"
}

