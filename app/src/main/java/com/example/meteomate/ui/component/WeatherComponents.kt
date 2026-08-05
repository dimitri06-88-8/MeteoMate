package com.example.meteomate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.meteomate.data.PressureUnit
import com.example.meteomate.data.TemperatureUnit
import com.example.meteomate.data.model.GeomagneticSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import com.example.meteomate.ui.screen.DailyForecastItem
import com.example.meteomate.ui.screen.AirQualitySnapshot
import com.example.meteomate.ui.screen.HourlyForecastItem
import com.example.meteomate.ui.screen.PrecipitationNowcastItem
import com.example.meteomate.util.DateUtils
import com.example.meteomate.util.TemperatureFormatter
import com.example.meteomate.util.formatWindValue
import com.example.meteomate.util.windDirectionLabel
import com.example.meteomate.util.geomagneticActivityLabel
import com.example.meteomate.util.WeatherCode
import androidx.compose.ui.res.stringResource
import com.example.meteomate.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.meteomate.util.WeatherFacts
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.roundToInt

@Composable
fun HeroWeather(
    temperature: Double,
    weatherCode: Int,
    weatherDescription: String,
    locationName: String,
    modifier: Modifier = Modifier,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = locationName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_to_favorites),
                    tint = if (isFavorite) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = WeatherCode.emoji(weatherCode),
            fontSize = 52.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = TemperatureFormatter.format(temperature, temperatureUnit),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = translateWeatherDescription(weatherDescription).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun HourlyForecastRow(
    forecast: List<HourlyForecastItem>,
    modifier: Modifier = Modifier,
    precipitationNowcast: List<PrecipitationNowcastItem> = emptyList(),
    timezoneOffsetSeconds: Int = 0,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    val visibleForecast = forecast.take(48)
    if (visibleForecast.isEmpty() && precipitationNowcast.isEmpty()) return

    var expandedHourKey by remember(visibleForecast) {
        mutableStateOf<String?>(null)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (visibleForecast.isNotEmpty()) {
            Text(
                text = if (visibleForecast.all { it.intervalHours == 1 }) {
                    stringResource(R.string.hourly_forecast_48h)
                } else {
                    stringResource(R.string.fallback_forecast_3h)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        if (precipitationNowcast.isNotEmpty()) {
            PrecipitationNowcast(
                nowcast = precipitationNowcast,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (visibleForecast.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(
                    items = visibleForecast,
                    key = { index, item -> "${item.date}-${item.time}-$index" }
                ) { index, item ->
                val itemKey = "${item.date}-${item.time}-$index"
                val startsNewDay = index == 0 || visibleForecast[index - 1].date != item.date

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (startsNewDay) {
                            Text(
                                text = formatHourlyDate(item.date, timezoneOffsetSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                            )
                        }
                    }

                    HourCard(
                        item = item,
                        temperatureUnit = temperatureUnit,
                        isNow = item.isCurrent,
                        expanded = expandedHourKey == itemKey,
                        onToggle = {
                            expandedHourKey = if (expandedHourKey == itemKey) null else itemKey
                        }
                    )
                }
            }
            }

            if (visibleForecast.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                HourlyForecastCharts(
                    forecast = visibleForecast,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HourCard(
    item: HourlyForecastItem,
    temperatureUnit: TemperatureUnit,
    isNow: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val cardWidth by animateDpAsState(if (expanded) 216.dp else 84.dp)
    val accent = BlueAccent
    val cardBrush = if (item.isDay) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color(0xFFFFD166).copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF152444).copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.09f)
            )
        )
    }

    Box(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBrush)
            .border(
                width = if (expanded) 1.dp else 0.5.dp,
                color = if (expanded) accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isNow) stringResource(R.string.now) else item.time,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = WeatherCode.emoji(item.weatherCode),
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = TemperatureFormatter.format(item.temperature, temperatureUnit),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = if (expanded) stringResource(R.string.hide_details) else stringResource(R.string.show_details),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (item.isDay) stringResource(R.string.daytime) else stringResource(R.string.nighttime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                )
                HourDetailLine(
                    label = stringResource(R.string.feels_like),
                    value = TemperatureFormatter.format(item.apparentTemperature, temperatureUnit)
                )
                HourDetailLine(
                    label = stringResource(R.string.precipitation),
                    value = if (item.precipitation > 0.0) {
                        "${precipitationPercent(item.precipitationProbability)}% · ${formatCompactDecimal(item.precipitation)} мм"
                    } else {
                        "${precipitationPercent(item.precipitationProbability)}%"
                    }
                )
                HourDetailLine(
                    label = stringResource(R.string.wind_speed),
                    value = "${formatCompactDecimal(item.windSpeed)} м/с${item.windDeg?.let { " ${windDirectionLabel(it).abbreviation}" } ?: ""}"
                )
                item.windGust?.let { gust ->
                    HourDetailLine(
                        label = stringResource(R.string.gusts_label),
                        value = "${formatCompactDecimal(gust)} м/с"
                    )
                }
                HourDetailLine(
                    label = stringResource(R.string.pressure),
                    value = "${item.pressure} гПа"
                )
            }
        }
    }
}

@Composable
private fun HourDetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun HourlyForecastCharts(
    forecast: List<HourlyForecastItem>,
    modifier: Modifier = Modifier
) {
    val temperatureColor = Color(0xFFFF6B35)
    val apparentColor = Color(0xFF4A90E2)
    val precipitationColor = Color(0xFF45A7F5)
    val windColor = Color(0xFFFFB020)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    val minimumTemperature = forecast.minOf { minOf(it.temperature, it.apparentTemperature) }
    val maximumTemperature = forecast.maxOf { maxOf(it.temperature, it.apparentTemperature) }
    val temperatureRange = (maximumTemperature - minimumTemperature).coerceAtLeast(1.0)
    val maximumWind = forecast.maxOf { it.windSpeed }.coerceAtLeast(1.0)

    LiquidGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.hourly_trends),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChartLegend(stringResource(R.string.temperature), temperatureColor)
            ChartLegend(stringResource(R.string.feels_like), apparentColor)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(top = 5.dp)
        ) {
            for (line in 0..2) {
                val y = size.height * line / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }

            fun xFor(index: Int): Float = size.width * index / forecast.lastIndex.coerceAtLeast(1).toFloat()
            fun yFor(value: Double): Float =
                size.height - ((value - minimumTemperature) / temperatureRange).toFloat() * size.height

            val temperaturePath = Path()
            val apparentPath = Path()
            forecast.forEachIndexed { index, item ->
                val x = xFor(index)
                val temperatureY = yFor(item.temperature)
                val apparentY = yFor(item.apparentTemperature)
                if (index == 0) {
                    temperaturePath.moveTo(x, temperatureY)
                    apparentPath.moveTo(x, apparentY)
                } else {
                    temperaturePath.lineTo(x, temperatureY)
                    apparentPath.lineTo(x, apparentY)
                }
            }
            drawPath(temperaturePath, temperatureColor, style = Stroke(width = 2.dp.toPx()))
            drawPath(apparentPath, apparentColor, style = Stroke(width = 1.5.dp.toPx()))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChartLegend(stringResource(R.string.precipitation), precipitationColor)
            ChartLegend(stringResource(R.string.wind_speed), windColor)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 5.dp)
        ) {
            drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            val step = size.width / forecast.size.coerceAtLeast(1)
            val windPath = Path()

            forecast.forEachIndexed { index, item ->
                val x = step * (index + 0.5f)
                val probability = precipitationPercent(item.precipitationProbability) / 100f
                val precipitationHeight = size.height * probability
                drawLine(
                    color = precipitationColor.copy(alpha = 0.55f),
                    start = Offset(x, size.height),
                    end = Offset(x, size.height - precipitationHeight),
                    strokeWidth = (step * 0.5f).coerceAtLeast(1.dp.toPx()),
                    cap = StrokeCap.Round
                )

                val windY = size.height - (item.windSpeed / maximumWind).toFloat() * size.height
                if (index == 0) windPath.moveTo(x, windY) else windPath.lineTo(x, windY)
            }
            drawPath(windPath, windColor, style = Stroke(width = 1.75.dp.toPx()))
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrecipitationNowcast(
    nowcast: List<PrecipitationNowcastItem>,
    modifier: Modifier = Modifier
) {
    val values = nowcast.take(8)
    val maximumPrecipitation = values.maxOfOrNull { it.precipitation }?.coerceAtLeast(0.5) ?: 0.5
    val precipitationColor = Color(0xFF45A7F5)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val currentTimeMarkerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val peak = values.maxOfOrNull { it.precipitation } ?: 0.0

    LiquidGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.precipitation_nowcast),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (peak <= 0.0) {
                    stringResource(R.string.no_precipitation_short)
                } else {
                    stringResource(R.string.precipitation_up_to, formatCompactDecimal(peak))
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (peak > 0.0) precipitationColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val chartTop = 4.dp.toPx()
            val chartBottom = size.height - 4.dp.toPx()
            val chartHeight = chartBottom - chartTop

            for (line in 0..2) {
                val y = chartTop + chartHeight * line / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (values.size == 1) {
                val y = chartBottom -
                    (values.first().precipitation / maximumPrecipitation).toFloat() * chartHeight
                drawLine(
                    precipitationColor,
                    Offset(0f, y),
                    Offset(size.width, y),
                    2.dp.toPx()
                )
            } else if (values.size > 1) {
                fun xFor(index: Int): Float =
                    size.width * index / values.lastIndex.toFloat()
                fun yFor(value: Double): Float =
                    chartBottom - (value / maximumPrecipitation).toFloat() * chartHeight

                val linePath = Path()
                values.forEachIndexed { index, item ->
                    val x = xFor(index)
                    val y = yFor(item.precipitation)
                    if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }

                val fillPath = Path().apply {
                    moveTo(0f, chartBottom)
                    values.forEachIndexed { index, item ->
                        lineTo(xFor(index), yFor(item.precipitation))
                    }
                    lineTo(size.width, chartBottom)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            precipitationColor.copy(alpha = 0.42f),
                            precipitationColor.copy(alpha = 0.04f)
                        ),
                        startY = chartTop,
                        endY = chartBottom
                    )
                )
                drawPath(
                    path = linePath,
                    color = precipitationColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawLine(
                    color = currentTimeMarkerColor,
                    start = Offset(0f, chartTop),
                    end = Offset(0f, chartBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        if (values.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(values.first().time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (values.size > 2) {
                    Text(values[values.size / 2].time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (values.size > 1) {
                    Text(values.last().time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun precipitationPercent(value: Double): Int {
    return value.roundToInt().coerceIn(0, 100)
}

private fun formatCompactDecimal(value: Double): String {
    return if (value == value.roundToInt().toDouble()) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatHourlyDate(date: String, timezoneOffsetSeconds: Int): String {
    val cityToday = java.time.LocalDateTime.ofInstant(
        java.time.Instant.now(),
        java.time.ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds)
    ).toLocalDate()
    if (runCatching { LocalDate.parse(date) }.getOrNull() == cityToday) return "Сегодня"
    return try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
            .replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        date
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3A3A3E).copy(alpha = 0.6f),
                            Color(0xFF1C1C1E).copy(alpha = 0.5f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.35f),
                            Color(0xFFE8F4FF).copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.4f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(2000f, 1200f)
                    )
                }
            )
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.02f),
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.08f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 800f)
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.06f)
                        ),
                        start = Offset(0f, 400f),
                        end = Offset(0f, 600f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun HealthEnvironmentSection(
    temperature: Double,
    feelsLike: Double,
    humidity: Int,
    windSpeed: Double,
    precipitationProbability: Double,
    visibilityMeters: Int?,
    uvIndex: Double?,
    airQuality: AirQualitySnapshot?,
    geomagnetic: GeomagneticSnapshot?,
    yesterdayAverageTemperature: Double?,
    history: List<HourlyForecastItem>,
    modifier: Modifier = Modifier
) {
    val aqi = airQuality?.aqi
    val comfortScore = remember(temperature, feelsLike, humidity, windSpeed, precipitationProbability, aqi, uvIndex) {
        var score = 100
        score -= (kotlin.math.abs(feelsLike - 21.0) * 3.0).roundToInt().coerceAtMost(35)
        score -= (kotlin.math.abs(humidity - 50) / 3).coerceAtMost(15)
        score -= (windSpeed * 2).roundToInt().coerceAtMost(15)
        score -= (precipitationProbability / 5).roundToInt().coerceAtMost(20)
        score -= when {
            aqi == null -> 0
            aqi > 150 -> 25
            aqi > 100 -> 15
            aqi > 50 -> 7
            else -> 0
        }
        score -= if ((uvIndex ?: 0.0) >= 8) 10 else 0
        score.coerceIn(0, 100)
    }
    val clothing = when {
        feelsLike <= -10 -> "Тёплая куртка, шапка и перчатки"
        feelsLike <= 5 -> "Куртка и тёплая обувь"
        feelsLike <= 14 -> "Лёгкая куртка или ветровка"
        feelsLike <= 22 -> "Лёгкая одежда, можно взять кофту"
        else -> "Лёгкая одежда и головной убор"
    }
    val activityText = when {
        (aqi ?: 0) > 150 -> "Лучше сократить прогулку и тренировку на улице"
        precipitationProbability >= 70 -> "Для бега неблагоприятно: вероятны осадки"
        windSpeed >= 12 -> "Для бега неблагоприятно: сильный ветер"
        comfortScore >= 75 -> "Хорошие условия для прогулки и бега"
        comfortScore >= 50 -> "Для прогулки нормально, бег — по самочувствию"
        else -> "Условия на улице некомфортные"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Здоровье и окружающая среда",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )
        LiquidGlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnvironmentMetric(
                    title = "Качество воздуха",
                    value = aqi?.let { "$it AQI" } ?: "Нет данных",
                    subtitle = aqiLabel(aqi),
                    modifier = Modifier.weight(1f)
                )
                EnvironmentMetric(
                    title = "УФ-индекс",
                    value = uvIndex?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—",
                    subtitle = uvLabel(uvIndex),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnvironmentMetric(
                    title = "PM2.5",
                    value = airQuality?.let { String.format(Locale.getDefault(), "%.1f мкг/м³", it.pm25) } ?: "—",
                    subtitle = "Мелкие частицы",
                    modifier = Modifier.weight(1f)
                )
                EnvironmentMetric(
                    title = "PM10",
                    value = airQuality?.let { String.format(Locale.getDefault(), "%.1f мкг/м³", it.pm10) } ?: "—",
                    subtitle = "Крупные частицы",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnvironmentMetric(
                    title = "Видимость",
                    value = visibilityMeters?.let { String.format(Locale.getDefault(), "%.1f км", it / 1000.0) } ?: "—",
                    subtitle = "На текущий момент",
                    modifier = Modifier.weight(1f)
                )
                EnvironmentMetric(
                    title = "Комфорт",
                    value = "$comfortScore/100",
                    subtitle = when { comfortScore >= 75 -> "Комфортно"; comfortScore >= 50 -> "Умеренно"; else -> "Некомфортно" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        GeomagneticActivityCard(geomagnetic)

        Spacer(Modifier.height(12.dp))
        LiquidGlassCard {
            Text("Рекомендации", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("👕 $clothing", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface)
            Text("🏃 $activityText", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface)
            if ((uvIndex ?: 0.0) >= 3) {
                Text("☀️ Используйте защиту от солнца", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LiquidGlassCard {
                val delta = yesterdayAverageTemperature?.let { temperature - it }
                Text("Последние 24 часа", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = delta?.let {
                        val sign = if (it > 0) "+" else ""
                        "Сейчас на $sign${String.format(Locale.getDefault(), "%.1f", it)}° относительно среднего за прошлые сутки"
                    } ?: "История температуры",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val values = history.map { it.temperature }
                Canvas(modifier = Modifier.fillMaxWidth().height(90.dp).padding(top = 12.dp)) {
                    if (values.size < 2) return@Canvas
                    val min = values.minOrNull() ?: return@Canvas
                    val max = values.maxOrNull() ?: return@Canvas
                    val range = (max - min).takeIf { it > 0.1 } ?: 1.0
                    val path = Path()
                    values.forEachIndexed { index, value ->
                        val x = size.width * index / (values.size - 1).toFloat()
                        val y = size.height - ((value - min) / range).toFloat() * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = Color(0xFFFF6B3D), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}

@Composable
private fun GeomagneticActivityCard(snapshot: GeomagneticSnapshot?) {
    LiquidGlassCard {
        Text(
            text = "Магнитная активность",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (snapshot == null) {
            Text(
                text = "Данные временно недоступны",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            return@LiquidGlassCard
        }
        Text(
            text = "Kp ${formatCompactDecimal(snapshot.currentKp)} · " +
                geomagneticActivityLabel(snapshot.currentKp, snapshot.currentScale),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
        val forecastKp = snapshot.maximumForecastKp
        val forecastText = when {
            forecastKp == null -> "Прогноз на ближайшие 24 часа временно недоступен"
            forecastKp >= 5.0 -> "В ближайшие 24 ч: " +
                geomagneticActivityLabel(forecastKp, snapshot.forecastScale) +
                " · максимум Kp ${formatCompactDecimal(forecastKp)}"
            else -> "В ближайшие 24 ч магнитная буря не ожидается · максимум Kp " +
                formatCompactDecimal(forecastKp)
        }
        Text(
            text = forecastText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = "Планетарный индекс NOAA SWPC",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun EnvironmentMetric(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun aqiLabel(aqi: Int?): String = when {
    aqi == null -> "Источник недоступен"
    aqi <= 50 -> "Хорошее"
    aqi <= 100 -> "Умеренное"
    aqi <= 150 -> "Вредно для чувствительных"
    aqi <= 200 -> "Вредное"
    else -> "Очень вредное"
}

private fun uvLabel(uv: Double?): String = when {
    uv == null -> "Нет данных"
    uv < 3 -> "Низкий"
    uv < 6 -> "Средний"
    uv < 8 -> "Высокий"
    uv < 11 -> "Очень высокий"
    else -> "Экстремальный"
}

@Composable
fun WindVisualization(
    windSpeed: Double,
    windDeg: Int?,
    windGust: Double?,
    modifier: Modifier = Modifier
) {
    val direction = windDeg?.let(::windDirectionLabel)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val speedKmh = windSpeed * 3.6
    val speedKn = windSpeed * 1.94384
    val beaufort = beaufortForce(windSpeed)

    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.wind_speed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = (minOf(cx, cy) - 24.dp.toPx()) * 0.85f
                        val tickColor = onSurface.copy(alpha = 0.15f)

                        for (i in 0..11) {
                            val a = i * 30f - 90f
                            val rad = Math.toRadians(a.toDouble())
                            val c = cos(rad).toFloat()
                            val s = sin(rad).toFloat()
                            val isCardinal = i % 3 == 0
                            val inner = if (isCardinal) r * 0.82f else r * 0.9f
                            drawLine(
                                color = tickColor,
                                start = Offset(cx + inner * c, cy + inner * s),
                                end = Offset(cx + r * c, cy + r * s),
                                strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        drawCircle(
                            color = tickColor,
                            radius = 2.dp.toPx(),
                            center = Offset(cx, cy)
                        )

                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatWindValue(windSpeed),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        Text(
                            text = "м/с",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${speedKmh.toInt()}", fontWeight = FontWeight.Bold, color = onSurface, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(4.dp))
                        Text("км/ч", color = onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${speedKn.toInt()}", fontWeight = FontWeight.Bold, color = onSurface, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(4.dp))
                        Text("уз", color = onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (windGust != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatWindValue(windGust), fontWeight = FontWeight.Bold, color = Color(0xFFFF6B35), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(4.dp))
                            Text("м/с порыв", color = onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = direction?.abbreviation ?: "—",
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = direction?.let { "${it.name} · ${windDeg}°" } ?: "Нет данных",
                        color = onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "F${beaufort} · ${beaufortLabel(beaufort)}",
                    color = onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun beaufortForce(speedMs: Double): Int = when {
    speedMs < 0.3 -> 0
    speedMs < 1.5 -> 1
    speedMs < 3.3 -> 2
    speedMs < 5.5 -> 3
    speedMs < 7.9 -> 4
    speedMs < 10.7 -> 5
    speedMs < 13.8 -> 6
    speedMs < 17.1 -> 7
    speedMs < 20.7 -> 8
    speedMs < 24.4 -> 9
    speedMs < 28.4 -> 10
    speedMs < 32.6 -> 11
    else -> 12
}

private fun beaufortLabel(force: Int): String = when (force) {
    0 -> "Штиль"
    1 -> "Тихий ветер"
    2 -> "Лёгкий бриз"
    3 -> "Слабый бриз"
    4 -> "Умеренный бриз"
    5 -> "Свежий бриз"
    6 -> "Сильный бриз"
    7 -> "Слабый шторм"
    8 -> "Шторм"
    9 -> "Сильный шторм"
    10 -> "Буря"
    11 -> "Сильная буря"
    else -> "Ураган"
}

@Composable
fun DetailGrid(
    feelsLike: Double,
    humidity: Int,
    windSpeed: Double,
    windDeg: Int?,
    pressure: Int,
    visibility: Int?,
    cloudCoverage: Int?,
    sunrise: Long,
    sunset: Long,
    timezoneOffsetSeconds: Int = 0,
    modifier: Modifier = Modifier,
    pressureUnit: PressureUnit = PressureUnit.HPA,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailTile(
                label = stringResource(R.string.feels_like),
                value = TemperatureFormatter.format(feelsLike, temperatureUnit),
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = stringResource(R.string.wind_speed),
                value = buildWindString(windSpeed, windDeg),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailTile(
                label = stringResource(R.string.humidity),
                value = "$humidity%",
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = stringResource(R.string.pressure),
                value = formatPressure(pressure, pressureUnit),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailTile(
                label = stringResource(R.string.sunrise),
                value = formatUnixTime(sunrise, timezoneOffsetSeconds),
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = stringResource(R.string.sunset),
                value = formatUnixTime(sunset, timezoneOffsetSeconds),
                modifier = Modifier.weight(1f)
            )
        }
        if (visibility != null || cloudCoverage != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailTile(
                    label = stringResource(R.string.visibility),
                    value = if (visibility != null) formatVisibility(visibility) else "—",
                    modifier = Modifier.weight(1f)
                )
                DetailTile(
                    label = stringResource(R.string.cloud_cover),
                    value = if (cloudCoverage != null) "$cloudCoverage%" else "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DetailTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SunArcGraph(
    sunrise: Long,
    sunset: Long,
    timezoneOffsetSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis() / 1000L) }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(sunrise, sunset) {
        while (true) {
            currentTime = System.currentTimeMillis() / 1000L
            delay(10_000L)
        }
    }

    val dayDuration = sunset - sunrise
    val progress = if (dayDuration > 0) {
        ((currentTime - sunrise).toFloat() / dayDuration).coerceIn(0f, 1f)
    } else 0.5f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "sunProgress"
    )

    LiquidGlassCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.sun_path),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 40.dp, end = 40.dp, top = 16.dp, bottom = 28.dp)
            ) {
                val w = size.width
                val h = size.height
                val arcH = h * 0.7f

                val path = Path()
                for (i in 0..100) {
                    val t = i / 100f
                    val x = w * t
                    val y = h - arcH * sin(PI.toFloat() * t)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF8C00).copy(alpha = 0.5f),
                            Color(0xFFFFD700).copy(alpha = 0.9f),
                            Color(0xFFFF8C00).copy(alpha = 0.5f)
                        )
                    ),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                val sunX = w * animatedProgress
                val sunY = h - arcH * sin(PI.toFloat() * animatedProgress)
                val sunPos = Offset(sunX, sunY)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x44FFD700), Color(0x00FFA500)),
                        center = sunPos
                    ),
                    radius = 45.dp.toPx(),
                    center = sunPos
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x88FFF8DC), Color(0x00FFD700)),
                        center = sunPos
                    ),
                    radius = 20.dp.toPx(),
                    center = sunPos
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF8DC), Color(0xFFFFD700), Color(0xE6FFA500)),
                        center = sunPos
                    ),
                    radius = 8.dp.toPx(),
                    center = sunPos
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatUnixTime(sunrise, timezoneOffsetSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatUnixTime(currentTime, timezoneOffsetSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                    Text(
                        text = formatUnixTime(sunset, timezoneOffsetSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

}

@Composable
fun ForecastSection(
    forecast: List<DailyForecastItem>,
    modifier: Modifier = Modifier,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.forecast),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LiquidGlassCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                forecast.forEachIndexed { index, item ->
                    ForecastRow(item = item, temperatureUnit = temperatureUnit)
                    if (index < forecast.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .height(0.5.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastRow(item: DailyForecastItem, temperatureUnit: TemperatureUnit) {
    val isToday = DateUtils.isToday(item.day)
    val displayDate = if (isToday) stringResource(R.string.today) else DateUtils.formatDay(item.day)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(96.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = WeatherCode.emoji(item.weatherCode),
            fontSize = 16.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = TemperatureFormatter.format(item.maxTemp, temperatureUnit),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = TemperatureFormatter.format(item.minTemp, temperatureUnit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "${(item.precipitationProbability * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = BlueAccent,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

private val BlueAccent: Color
    @Composable get() = if (MaterialTheme.colorScheme.background == Color.Black) {
        Color(0xFF0A84FF)
    } else {
        Color(0xFF007AFF)
    }

private fun buildWindString(speed: Double, deg: Int?): String {
    val direction = deg?.let { " ${windDirectionLabel(it).abbreviation}" }.orEmpty()
    return "${formatWindValue(speed)} м/с$direction"
}

private fun formatPressure(pressure: Int, unit: PressureUnit): String {
    return when (unit) {
        PressureUnit.HPA -> "$pressure гПа"
        PressureUnit.MMHG -> "${(pressure * 0.75006).toInt()} мм рт. ст."
    }
}

private fun formatUnixTime(seconds: Long, timezoneOffsetSeconds: Int): String =
    if (seconds <= 0L) {
        "—"
    } else {
        Instant.ofEpochSecond(seconds)
            .atOffset(ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

private fun formatVisibility(meters: Int): String {
    return if (meters >= 1000) {
        "${meters / 1000} км"
    } else {
        "$meters м"
    }
}

private fun translateWeatherDescription(en: String): String {
    val map = mapOf(
        "clear sky" to "Ясно",
        "few clouds" to "Мало облаков",
        "scattered clouds" to "Рассеянные облака",
        "broken clouds" to "Разорванная облачность",
        "overcast clouds" to "Пасмурно",
        "light rain" to "Небольшой дождь",
        "moderate rain" to "Умеренный дождь",
        "heavy rain" to "Сильный дождь",
        "very heavy rain" to "Очень сильный дождь",
        "extreme rain" to "Экстремальный дождь",
        "freezing rain" to "Ледяной дождь",
        "light shower rain" to "Небольшой ливень",
        "shower rain" to "Ливень",
        "heavy shower rain" to "Сильный ливень",
        "light drizzle" to "Морось",
        "drizzle" to "Морось",
        "heavy drizzle" to "Сильная морось",
        "light snow" to "Небольшой снег",
        "snow" to "Снег",
        "heavy snow" to "Сильный снег",
        "sleet" to "Мокрый снег",
        "light rain and snow" to "Небольшой дождь со снегом",
        "rain and snow" to "Дождь со снегом",
        "thunderstorm" to "Гроза",
        "light thunderstorm" to "Небольшая гроза",
        "heavy thunderstorm" to "Сильная гроза",
        "ragged thunderstorm" to "Рваная гроза",
        "thunderstorm with light rain" to "Гроза с небольшим дождём",
        "thunderstorm with rain" to "Гроза с дождём",
        "thunderstorm with heavy rain" to "Гроза с сильным дождём",
        "mist" to "Туман",
        "fog" to "Туман",
        "light intensity drizzle" to "Небольшая морось",
        "heavy intensity drizzle" to "Сильная морось",
        "light intensity shower rain" to "Небольшой интенсивный ливень",
        "heavy intensity shower rain" to "Сильный интенсивный ливень",
        "very heavy rain" to "Очень сильный дождь",
        "haze" to "Дымка",
        "smoke" to "Дым",
        "dust" to "Пыль",
        "sand" to "Песок",
        "ash" to "Вулканический пепел",
        "squalls" to "Шквалы",
        "tornado" to "Смерч"
    )
    return map[en.lowercase()] ?: en
}

@Composable
fun WeatherFactsSection(modifier: Modifier = Modifier) {
    val fact = remember { mutableStateOf(WeatherFacts.random()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            fact.value = WeatherFacts.random()
        }
    }

    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\uD83D\uDCA1",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.weather_fact),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = fact.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
