package com.example.meteomate.ui.screen.observations

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meteomate.data.MeteoBadge
import com.example.meteomate.data.WeatherObservationKind
import com.example.meteomate.data.WeatherObservationProgress
import com.example.meteomate.data.WEATHER_OBSERVATION_COOLDOWN_MILLIS
import com.example.meteomate.data.isUnlocked
import com.example.meteomate.data.meteoBadges
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherObservationsScreen(
    progress: WeatherObservationProgress,
    locationName: String,
    weatherDescription: String,
    feedback: String?,
    onSubmit: (WeatherObservationKind) -> Unit
) {
    var selected by remember { mutableStateOf<WeatherObservationKind?>(null) }
    val cooldown = System.currentTimeMillis() - progress.lastReportAt in 0 until WEATHER_OBSERVATION_COOLDOWN_MILLIS
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF071321), Color(0xFF102B49), Color(0xFF17264A), Color(0xFF0A142A))
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Метеонаблюдения", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("Сверяйте прогноз с погодой за окном и собирайте метеознаки.",
                    color = Color.White.copy(alpha = .72f))
            }
            item {
                GlassCard {
                    Text(locationName.ifBlank { "Текущее местоположение" }, fontWeight = FontWeight.SemiBold)
                    Text("По прогнозу: $weatherDescription", style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = .68f))
                    Text("Что сейчас за окном?", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherObservationKind.entries.forEach { kind ->
                            FilterChip(
                                selected = selected == kind,
                                onClick = { selected = kind },
                                label = { Text("${kind.emoji} ${kind.label}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = .08f),
                                    labelColor = Color.White.copy(alpha = .82f),
                                    selectedContainerColor = Color(0xFF487DFF).copy(alpha = .52f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Button(
                        onClick = { selected?.let(onSubmit); selected = null },
                        enabled = selected != null && !cooldown,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text(if (cooldown) "Сегодня наблюдение уже сохранено" else "Сохранить наблюдение") }
                    feedback?.let {
                        Text(it, color = Color(0xFF8FC7FF), fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            item { ProgressCard(progress) }
            item { Text("Коллекция", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White) }
            meteoBadges.chunked(2).forEach { badges ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        badges.forEach { BadgeCard(it, progress, Modifier.weight(1f)) }
                        if (badges.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item {
                Text("Наблюдения сохраняются только на этом устройстве и не отправляются третьим лицам.",
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = .52f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: WeatherObservationProgress) {
    val next = listOf(1, 7, 20).firstOrNull { progress.totalReports < it }
    val previous = when (next) { 7 -> 1; 20 -> 7; else -> 0 }
    val fraction = next?.let {
        ((progress.totalReports - previous).toFloat() / (it - previous)).coerceIn(0f, 1f)
    } ?: 1f
    GlassCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Ваш вклад", fontWeight = FontWeight.SemiBold)
                Text("${progress.totalReports} ${reportWord(progress.totalReports)}",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text("🛰️", style = MaterialTheme.typography.displaySmall)
        }
        LinearProgressIndicator({ fraction }, Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp))
        Text(next?.let { "До следующего метеознака: ${it - progress.totalReports}" }
            ?: "Основная коллекция собрана", style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = .66f), modifier = Modifier.padding(top = 8.dp))
        if (progress.lastReportAt > 0) {
            val time = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(progress.lastReportAt))
            Text("Последнее: ${progress.lastKind?.emoji.orEmpty()} ${progress.lastKind?.label.orEmpty()} · $time",
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .58f))
        }
    }
}

@Composable
private fun BadgeCard(badge: MeteoBadge, progress: WeatherObservationProgress, modifier: Modifier) {
    val unlocked = badge.isUnlocked(progress)
    Card(modifier, RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(
        containerColor = if (unlocked) Color(0xFF294F7A) else Color(0xFF152844),
        contentColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (unlocked) badge.emoji else "◌", style = MaterialTheme.typography.headlineLarge)
            Text(if (badge.isSecret && !unlocked) "Секретный знак" else badge.title,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Text(if (badge.isSecret && !unlocked) "Условие пока неизвестно" else badge.description,
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .62f))
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF142944).copy(alpha = .94f),
        contentColor = Color.White
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

private fun reportWord(value: Int): String = when {
    value % 100 in 11..14 -> "наблюдений"
    value % 10 == 1 -> "наблюдение"
    value % 10 in 2..4 -> "наблюдения"
    else -> "наблюдений"
}
