package com.example.meteomate.work.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.meteomate.work.UvTimerNotificationWorker
import java.util.concurrent.TimeUnit

object UvTimerScheduler {
    const val PREFERENCES_NAME = "uv_timer_state"
    const val KEY_END_MILLIS = "end_millis"

    fun schedule(context: Context, endMillis: Long) {
        val delay = (endMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<UvTimerNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private const val WORK_NAME = "uv_protection_timer"
}

