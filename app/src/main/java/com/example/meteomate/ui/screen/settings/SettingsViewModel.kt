package com.example.meteomate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meteomate.data.PressureUnit
import com.example.meteomate.data.SettingsDataStore
import com.example.meteomate.data.TemperatureUnit
import com.example.meteomate.data.DisplayMode
import com.example.meteomate.data.CardOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings = settingsDataStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.meteomate.data.AppSettings()
    )

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { settingsDataStore.setTemperatureUnit(unit) }
    }

    fun setPressureUnit(unit: PressureUnit) {
        viewModelScope.launch { settingsDataStore.setPressureUnit(unit) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        viewModelScope.launch { settingsDataStore.setDisplayMode(mode) }
    }

    fun setCardOrder(order: CardOrder) {
        viewModelScope.launch { settingsDataStore.setCardOrder(order) }
    }

    fun setWeatherAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setWeatherAnimationsEnabled(enabled) }
    }

    fun setMorningSummaryEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setMorningSummaryEnabled(enabled)
    }

    fun setMorningSummaryTime(minutes: Int) = updateNotificationSetting {
        settingsDataStore.setMorningSummaryTime(minutes)
    }

    fun setGoldenHourNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setGoldenHourNotificationsEnabled(enabled)
    }

    fun setStrongWindNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setStrongWindNotificationsEnabled(enabled)
    }

    fun setRainNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setRainNotificationsEnabled(enabled)
    }

    fun setThunderstormNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setThunderstormNotificationsEnabled(enabled)
    }

    fun setSnowNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setSnowNotificationsEnabled(enabled)
    }

    fun setHeatNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setHeatNotificationsEnabled(enabled)
    }

    fun setFrostNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setFrostNotificationsEnabled(enabled)
    }

    fun setIceNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setIceNotificationsEnabled(enabled)
    }

    fun setRapidTemperatureChangeNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setRapidTemperatureChangeNotificationsEnabled(enabled)
    }

    fun setRapidPressureDropNotificationsEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setRapidPressureDropNotificationsEnabled(enabled)
    }

    fun setQuietHoursEnabled(enabled: Boolean) = updateNotificationSetting {
        settingsDataStore.setQuietHoursEnabled(enabled)
    }

    fun setQuietHoursStart(minutes: Int) = updateNotificationSetting {
        settingsDataStore.setQuietHoursStart(minutes)
    }

    fun setQuietHoursEnd(minutes: Int) = updateNotificationSetting {
        settingsDataStore.setQuietHoursEnd(minutes)
    }

    private fun updateNotificationSetting(update: suspend () -> Unit) {
        viewModelScope.launch {
            update()
        }
    }
}
