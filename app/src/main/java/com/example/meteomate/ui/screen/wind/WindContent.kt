package com.example.meteomate.ui.screen.wind

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.meteomate.R
import com.example.meteomate.data.WindModel
import com.example.meteomate.ui.component.LiquidGlassCard
import com.example.meteomate.ui.screen.HourlyForecastItem
import com.example.meteomate.ui.screen.ModelComparisonItem
import com.example.meteomate.ui.theme.SystemGray6Dark
import com.example.meteomate.util.formatWindValue
import com.example.meteomate.util.windDirectionLabel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WindContent(
    selectedModel: WindModel = WindModel.GFS27,
    windSpeed: Double = 0.0,
    windDeg: Int? = null,
    windGust: Double? = null,
    windHourlyForecast: List<HourlyForecastItem> = emptyList(),
    modelComparison: List<ModelComparisonItem> = emptyList(),
    alert10: Boolean = false,
    alert15: Boolean = false,
    alert20: Boolean = false,
    onModelSelected: (WindModel) -> Unit = {},
    onAlert10Toggle: () -> Unit = {},
    onAlert15Toggle: () -> Unit = {},
    onAlert20Toggle: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var showModelMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        ModelSelectorCard(
            selectedModel = selectedModel,
            expanded = showModelMenu,
            onExpandedChange = { showModelMenu = it },
            onModelSelected = { model ->
                showModelMenu = false
                onModelSelected(model)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        WindCurrentCard(selectedModel, windSpeed, windDeg, windGust)
        Spacer(modifier = Modifier.height(12.dp))

        WindHourlyForecastCard(windHourlyForecast)
        Spacer(modifier = Modifier.height(12.dp))

        WindForecastChart(selectedModel, windHourlyForecast)
        Spacer(modifier = Modifier.height(12.dp))

        WindRoseCard(windHourlyForecast)
        Spacer(modifier = Modifier.height(12.dp))

        WindAlertsCard(alert10, alert15, alert20, onAlert10Toggle, onAlert15Toggle, onAlert20Toggle)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModelSelectorCard(
    selectedModel: WindModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModelSelected: (WindModel) -> Unit
) {
    LiquidGlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.weather_model),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SystemGray6Dark.copy(alpha = 0.5f))
                        .clickable { onExpandedChange(true) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedModel.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedModel.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.free_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF30D158),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF30D158).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    WindModel.entries.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (model == selectedModel) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = model.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = { onModelSelected(model) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WindCurrentCard(model: WindModel, windSpeed: Double, windDeg: Int?, windGust: Double?) {
    val direction = windDeg?.let(::windDirectionLabel)
    val dirLabel = direction?.abbreviation ?: "--"
    LiquidGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(SystemGray6Dark.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dirLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )
                        direction?.let {
                            Text("${windDeg}°", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatWindValue(windSpeed),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " м/с",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row {
                    Text(
                        text = "${stringResource(R.string.gusts_label)}: ${windGust?.let(::formatWindValue) ?: "--"} м/с",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dirLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0A84FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun WindForecastChart(model: WindModel, windHourlyForecast: List<HourlyForecastItem>) {
    val displayItems = windHourlyForecast.take(13)
    val hours = displayItems.map { it.time }
    val speeds = displayItems.map { it.windSpeed }
    val gusts = displayItems.map { it.windGust ?: it.windSpeed }
    val maxVal = (speeds + gusts).maxOrNull()?.let { kotlin.math.ceil(it).toInt().coerceAtLeast(5) } ?: 10
    val chartHeight = 120.dp

    LiquidGlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.wind_speed_gusts),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (displayItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(chartHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_forecast_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$maxVal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("${(maxVal * 2 / 3)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("${(maxVal / 3)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            displayItems.forEachIndexed { i, _ ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height((chartHeight - 16.dp) * (speeds[i].toFloat() / maxVal) + 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF0A84FF))
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height((chartHeight - 16.dp) * (gusts[i].toFloat() / maxVal) + 4.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(Color(0xFFFF9F0A).copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                hours.forEachIndexed { i, h ->
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF0A84FF))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.speed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFF9F0A).copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.gusts_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WindRoseCard(windHourlyForecast: List<HourlyForecastItem>) {
    val directions = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
    val freq = remember(windHourlyForecast) {
        IntArray(8).also { arr ->
            windHourlyForecast.forEach { item ->
                val deg = item.windDeg ?: return@forEach
                val idx = (((deg + 22.5) % 360) / 45).toInt()
                arr[idx]++
            }
        }
    }
    val maxFreq = freq.maxOrNull()?.coerceAtLeast(1) ?: 1

    LiquidGlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.wind_rose),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wind_direction_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SystemGray6Dark.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            WindRoseCanvas(
                directions = directions,
                freq = freq,
                maxFreq = maxFreq,
                modifier = Modifier.size(180.dp)
            )
        }
    }
}

@Composable
private fun WindRoseCanvas(
    directions: List<String>,
    freq: IntArray,
    maxFreq: Int,
    modifier: Modifier = Modifier
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val petalColor = Color(0xFF0A84FF)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy) * 0.85f
        val petalWidth = 0.18f

        for (r in 1..4) {
            drawCircle(gridColor, radius * r / 4f, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }

        directions.forEachIndexed { i, dir ->
            val angle = Math.toRadians((i * 45.0) - 90.0)
            val norm = freq[i].toFloat() / maxFreq
            val petalLen = radius * norm.coerceAtLeast(0.05f)

            val x1 = cx + (radius * 0.15f * cos(angle)).toFloat()
            val y1 = cy + (radius * 0.15f * sin(angle)).toFloat()
            val x2 = cx + (petalLen * cos(angle)).toFloat()
            val y2 = cy + (petalLen * sin(angle)).toFloat()

            val perpX = (-sin(angle) * petalWidth * radius * 0.3).toFloat()
            val perpY = (cos(angle) * petalWidth * radius * 0.3).toFloat()

            val path = Path().apply {
                moveTo(x1 - perpX, y1 - perpY)
                lineTo(x2, y2)
                lineTo(x1 + perpX, y1 + perpY)
                close()
            }
            drawPath(path, petalColor.copy(alpha = 0.7f))

            val labelDist = radius + 14.dp.toPx()
            val lx = cx + (labelDist * cos(angle)).toFloat()
            val ly = cy + (labelDist * sin(angle)).toFloat()
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(dir, lx, ly + 4.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun WindHourlyForecastCard(windHourlyForecast: List<HourlyForecastItem>) {
    val items = windHourlyForecast.take(8)

    LiquidGlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.hourly_wind_forecast),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.hourly_wind_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_forecast_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items.forEach { item ->
                val dirLabel = item.windDeg?.let { windDirectionLabel(it).abbreviation } ?: "--"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(48.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                        Text(
                            text = dirLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )
                        item.windDeg?.let { Text("$it°", fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width((item.windSpeed / 20f * 100).dp.coerceAtMost(100.dp))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF0A84FF))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${formatWindValue(item.windSpeed)} м/с",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (item.windGust != null && item.windGust > item.windSpeed) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width((item.windGust / 20f * 100).dp.coerceAtMost(100.dp))
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFFF9F0A).copy(alpha = 0.7f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${formatWindValue(item.windGust)} ${stringResource(R.string.gust_suffix)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WindAlertsCard(
    alert10: Boolean,
    alert15: Boolean,
    alert20: Boolean,
    onAlert10Toggle: () -> Unit,
    onAlert15Toggle: () -> Unit,
    onAlert20Toggle: () -> Unit
) {
    LiquidGlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.wind_alerts),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wind_alerts_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        listOf(
            Triple(stringResource(R.string.alert_at, "10 м/с"), "\uD83D\uDFE2", alert10) to onAlert10Toggle,
            Triple(stringResource(R.string.alert_at, "15 м/с"), "\uD83D\uDFE1", alert15) to onAlert15Toggle,
            Triple(stringResource(R.string.alert_at, "20 м/с"), "\uD83D\uDD34", alert20) to onAlert20Toggle
        ).forEach { (data, toggle) ->
            val (label, dot, enabled) = data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { toggle() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dot, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (enabled) Color(0xFF30D158) else SystemGray6Dark)
                        .clickable { toggle() },
                    contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.notifications_permission),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
