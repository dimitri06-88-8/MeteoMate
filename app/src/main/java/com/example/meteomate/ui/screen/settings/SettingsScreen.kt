package com.example.meteomate.ui.screen.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.example.meteomate.R
import com.example.meteomate.BuildConfig
import com.example.meteomate.data.AppSettings
import com.example.meteomate.data.PressureUnit
import com.example.meteomate.data.TemperatureUnit
import com.example.meteomate.data.DisplayMode
import com.example.meteomate.data.CardOrder
import com.example.meteomate.ui.component.LiquidGlassCard
import com.example.meteomate.ui.theme.ClearDayTop
import com.example.meteomate.ui.theme.ClearDayBottom
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onShowWhatsNew: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ClearDayTop,
                        ClearDayTop.copy(alpha = 0.85f),
                        ClearDayBottom.copy(alpha = 0.9f),
                        ClearDayBottom
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                UnitSection(
                    title = stringResource(R.string.temperature_unit),
                    items = TemperatureUnit.entries,
                    selectedItem = settings.temperatureUnit,
                    onItemSelected = { viewModel.setTemperatureUnit(it) },
                    itemLabel = { it.label }
                )

                Spacer(modifier = Modifier.height(16.dp))

                UnitSection(
                    title = stringResource(R.string.pressure_unit),
                    items = PressureUnit.entries,
                    selectedItem = settings.pressureUnit,
                    onItemSelected = { viewModel.setPressureUnit(it) },
                    itemLabel = { it.label }
                )

                Spacer(modifier = Modifier.height(16.dp))

                UnitSection(
                    title = "Вид прогноза",
                    items = DisplayMode.entries,
                    selectedItem = settings.displayMode,
                    onItemSelected = viewModel::setDisplayMode,
                    itemLabel = { it.label }
                )

                Spacer(modifier = Modifier.height(16.dp))

                UnitSection(
                    title = "Порядок карточек",
                    items = CardOrder.entries,
                    selectedItem = settings.cardOrder,
                    onItemSelected = viewModel::setCardOrder,
                    itemLabel = { it.label }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidGlassCard {
                    NotificationSwitchRow(
                        title = "Погодные анимации",
                        subtitle = "Дождь, снег, облака и другие эффекты фона",
                        checked = settings.weatherAnimationsEnabled,
                        onCheckedChange = viewModel::setWeatherAnimationsEnabled
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                NotificationSettingsSection(
                    settings = settings,
                    viewModel = viewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowWhatsNew)
                ) {
                    Text(
                        text = stringResource(R.string.whats_new_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.whats_new_settings_subtitle,
                            BuildConfig.VERSION_NAME
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NotificationSettingsSection(
    settings: AppSettings,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    fun updateWithPermission(enabled: Boolean, update: (Boolean) -> Unit) {
        update(enabled)
        if (
            enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionGranted
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LiquidGlassCard {
        Text(
            text = stringResource(R.string.notification_settings_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.notification_settings_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Text(
                text = stringResource(R.string.notifications_permission),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        NotificationSwitchRow(
            title = stringResource(R.string.morning_summary),
            subtitle = stringResource(R.string.morning_summary_description),
            checked = settings.morningSummaryEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setMorningSummaryEnabled)
            }
        )
        if (settings.morningSummaryEnabled) {
            TimePreferenceRow(
                title = stringResource(R.string.summary_time),
                minutes = settings.morningSummaryTimeMinutes,
                onTimeSelected = viewModel::setMorningSummaryTime
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        NotificationSwitchRow(
            title = stringResource(R.string.golden_hour_notifications),
            subtitle = stringResource(R.string.golden_hour_notifications_description),
            checked = settings.goldenHourNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setGoldenHourNotificationsEnabled)
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    LiquidGlassCard {
        Text(
            text = stringResource(R.string.weather_alert_settings_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_strong_wind),
            subtitle = stringResource(R.string.alert_strong_wind_description),
            checked = settings.strongWindNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setStrongWindNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_rain),
            checked = settings.rainNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setRainNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_thunderstorm),
            checked = settings.thunderstormNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setThunderstormNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_snow),
            checked = settings.snowNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setSnowNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_heat),
            subtitle = stringResource(R.string.alert_heat_description),
            checked = settings.heatNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setHeatNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_frost),
            subtitle = stringResource(R.string.alert_frost_description),
            checked = settings.frostNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setFrostNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_ice),
            subtitle = stringResource(R.string.alert_ice_description),
            checked = settings.iceNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setIceNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_rapid_temperature),
            subtitle = stringResource(R.string.alert_rapid_temperature_description),
            checked = settings.rapidTemperatureChangeNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setRapidTemperatureChangeNotificationsEnabled)
            }
        )
        NotificationSwitchRow(
            title = stringResource(R.string.alert_rapid_pressure),
            subtitle = stringResource(R.string.alert_rapid_pressure_description),
            checked = settings.rapidPressureDropNotificationsEnabled,
            onCheckedChange = { enabled ->
                updateWithPermission(enabled, viewModel::setRapidPressureDropNotificationsEnabled)
            }
        )
        Text(
            text = stringResource(R.string.notification_forecast_limitations),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    LiquidGlassCard {
        Text(
            text = stringResource(R.string.quiet_hours_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        NotificationSwitchRow(
            title = stringResource(R.string.quiet_hours),
            subtitle = stringResource(R.string.quiet_hours_description),
            checked = settings.quietHoursEnabled,
            onCheckedChange = viewModel::setQuietHoursEnabled
        )
        if (settings.quietHoursEnabled) {
            TimePreferenceRow(
                title = stringResource(R.string.quiet_hours_start),
                minutes = settings.quietHoursStartMinutes,
                onTimeSelected = viewModel::setQuietHoursStart
            )
            TimePreferenceRow(
                title = stringResource(R.string.quiet_hours_end),
                minutes = settings.quietHoursEndMinutes,
                onTimeSelected = viewModel::setQuietHoursEnd
            )
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TimePreferenceRow(
    title: String,
    minutes: Int,
    onTimeSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val hour = minutes / 60
    val minute = minutes % 60
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        onTimeSelected(selectedHour * 60 + selectedMinute)
                    },
                    hour,
                    minute,
                    true
                ).show()
            }
        ) {
            Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        }
    }
}

@Composable
private fun <T> UnitSection(
    title: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String
) {
    LiquidGlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = item == selectedItem,
                        onClick = { onItemSelected(item) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = itemLabel(item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
