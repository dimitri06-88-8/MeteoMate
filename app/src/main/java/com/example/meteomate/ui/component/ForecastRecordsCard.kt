package com.example.meteomate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meteomate.data.TemperatureUnit
import com.example.meteomate.ui.screen.DailyForecastItem
import com.example.meteomate.ui.screen.calculateForecastRecords
import com.example.meteomate.util.DateUtils
import com.example.meteomate.util.TemperatureFormatter
import com.example.meteomate.util.formatWindValue

@Composable
fun ForecastRecordsCard(
    forecast: List<DailyForecastItem>,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val records = remember(forecast) { calculateForecastRecords(forecast) } ?: return
    LiquidGlassCard(modifier = modifier) {
        Text(
            text = "Рекорды прогноза на 7 дней",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        ForecastRecordRow(
            emoji = "🔥",
            label = "Самый жаркий день",
            value = TemperatureFormatter.format(records.hottest.maxTemp, temperatureUnit),
            day = DateUtils.formatDay(records.hottest.day)
        )
        ForecastRecordRow(
            emoji = "❄️",
            label = "Самый холодный день",
            value = TemperatureFormatter.format(records.coldest.minTemp, temperatureUnit),
            day = DateUtils.formatDay(records.coldest.day)
        )
        ForecastRecordRow(
            emoji = "💨",
            label = "Самый ветреный день",
            value = "${formatWindValue(records.windiest.maxWindSpeed)} м/с",
            day = DateUtils.formatDay(records.windiest.day)
        )
        Text(
            text = "Рекорды рассчитаны только по текущему прогнозу, а не по многолетней истории.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ForecastRecordRow(
    emoji: String,
    label: String,
    value: String,
    day: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}
