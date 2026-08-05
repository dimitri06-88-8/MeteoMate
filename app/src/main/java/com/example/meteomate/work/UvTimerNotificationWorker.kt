package com.example.meteomate.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meteomate.work.notification.UvTimerScheduler
import com.example.meteomate.work.notification.WeatherNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UvTimerNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: WeatherNotificationManager
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        applicationContext.getSharedPreferences(UvTimerScheduler.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(UvTimerScheduler.KEY_END_MILLIS)
            .apply()
        notificationManager.postUvTimerFinished()
        return Result.success()
    }
}
