package com.example.meteomate.ui.screen.whatsnew

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meteomate.BuildConfig
import com.example.meteomate.data.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WhatsNewStartupState(
    val isLoaded: Boolean = false,
    val shouldShowOnStart: Boolean = false
)

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _startupState = MutableStateFlow(WhatsNewStartupState())
    val startupState: StateFlow<WhatsNewStartupState> = _startupState.asStateFlow()

    init {
        viewModelScope.launch {
            val lastSeen = settingsDataStore.lastSeenWhatsNewVersionCode.first()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val isUpdatedInstall = packageInfo.lastUpdateTime > packageInfo.firstInstallTime
            _startupState.value = WhatsNewStartupState(
                isLoaded = true,
                shouldShowOnStart = shouldShowWhatsNew(
                    lastSeenVersionCode = lastSeen,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    isUpdatedInstall = isUpdatedInstall
                )
            )
        }
    }

    fun markCurrentVersionShown() {
        _startupState.value = _startupState.value.copy(shouldShowOnStart = false)
        viewModelScope.launch {
            settingsDataStore.markWhatsNewShown(BuildConfig.VERSION_CODE)
        }
    }
}

