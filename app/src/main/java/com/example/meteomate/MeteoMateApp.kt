package com.example.meteomate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.meteomate.data.SettingsDataStore
import com.example.meteomate.work.WeatherRefreshWorker
import com.example.meteomate.work.notification.WeatherNotificationManager
import com.example.meteomate.work.notification.GoldenHourScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MeteoMateApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var notificationManager: WeatherNotificationManager
    @Inject lateinit var goldenHourScheduler: GoldenHourScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannels()
        applicationScope.launch {
            settingsDataStore.settings.collect { settings ->
                WeatherRefreshWorker.syncScheduling(this@MeteoMateApp, settings)
                if (!settings.goldenHourNotificationsEnabled) goldenHourScheduler.cancel()
            }
        }
    }
}
