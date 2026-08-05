package com.example.meteomate.ui.component

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meteomate.util.UvRiskLevel
import com.example.meteomate.util.uvGuidance
import com.example.meteomate.work.notification.UvTimerScheduler
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun SolarFeaturesCard(
    uvIndex: Double?,
    sunrise: Long,
    sunset: Long,
    timezoneOffsetSeconds: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val timerPreferences = remember(context) {
        context.getSharedPreferences(UvTimerScheduler.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    val guidance = remember(uvIndex) { uvGuidance(uvIndex) }
    val reminderMinutes = guidance.protectionReminderMinutes
    var timerEndMillis by rememberSaveable {
        mutableLongStateOf(timerPreferences.getLong(UvTimerScheduler.KEY_END_MILLIS, 0L))
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timerEndMillis) {
        while (timerEndMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
        if (timerEndMillis > 0L) {
            nowMillis = System.currentTimeMillis()
            timerPreferences.edit().remove(UvTimerScheduler.KEY_END_MILLIS).apply()
            timerEndMillis = 0L
        }
    }

    val remainingSeconds = ((timerEndMillis - nowMillis).coerceAtLeast(0L) + 999L) / 1_000L
    val timerRunning = remainingSeconds > 0L
    val totalSeconds = (reminderMinutes ?: 0) * 60L
    val progress = if (timerRunning && totalSeconds > 0L) {
        (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else {
        0f
    }

    LiquidGlassCard(modifier = modifier) {
        Text(
            text = "Солнце и золотой час",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "УФ-индекс",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uvIndex?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = uvRiskColor(guidance.level)
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    text = guidance.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = guidance.advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (reminderMinutes != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (timerRunning) {
                    "Проверить защиту через ${formatCountdown(remainingSeconds)}"
                } else {
                    "Таймер напоминания о защите: $reminderMinutes мин"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (timerRunning) {
                LinearProgressIndicator(
                    { progress },
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (timerRunning) {
                    TextButton(onClick = {
                        timerEndMillis = 0L
                        timerPreferences.edit().remove(UvTimerScheduler.KEY_END_MILLIS).apply()
                        UvTimerScheduler.cancel(context)
                    }) {
                        Text("Сбросить")
                    }
                } else {
                    Button(
                        onClick = {
                            nowMillis = System.currentTimeMillis()
                            timerEndMillis = nowMillis + reminderMinutes * 60_000L
                            timerPreferences.edit()
                                .putLong(UvTimerScheduler.KEY_END_MILLIS, timerEndMillis)
                                .apply()
                            UvTimerScheduler.schedule(context, timerEndMillis)
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) {
                        Text("Запустить таймер")
                    }
                }
            }
            Text(
                text = "Это напоминание, а не гарантированное безопасное время. УФ-чувствительность индивидуальна.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Золотой час",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (sunrise > 0L && sunset > sunrise) {
            val morningEnd = sunrise + GOLDEN_HOUR_SECONDS
            val eveningStart = sunset - GOLDEN_HOUR_SECONDS
            Text(
                text = "Утром ${formatCityTime(sunrise, timezoneOffsetSeconds)}–${formatCityTime(morningEnd, timezoneOffsetSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Вечером ${formatCityTime(eveningStart, timezoneOffsetSeconds)}–${formatCityTime(sunset, timezoneOffsetSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Время станет доступно после обновления прогноза.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Интервалы приблизительные; облачность и рельеф могут изменить освещение.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun uvRiskColor(level: UvRiskLevel): Color = when (level) {
    UvRiskLevel.UNKNOWN -> Color(0xFF8E8E93)
    UvRiskLevel.LOW -> Color(0xFF34C759)
    UvRiskLevel.MODERATE_TO_HIGH -> Color(0xFFFF9500)
    UvRiskLevel.VERY_HIGH_TO_EXTREME -> Color(0xFFFF3B30)
}

private fun formatCountdown(seconds: Long): String = String.format(
    Locale.getDefault(),
    "%02d:%02d",
    seconds / 60,
    seconds % 60
)

private fun formatCityTime(epochSeconds: Long, offsetSeconds: Int): String =
    Instant.ofEpochSecond(epochSeconds)
        .atOffset(ZoneOffset.ofTotalSeconds(offsetSeconds))
        .format(TIME_FORMATTER)

private const val GOLDEN_HOUR_SECONDS = 60L * 60L
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
