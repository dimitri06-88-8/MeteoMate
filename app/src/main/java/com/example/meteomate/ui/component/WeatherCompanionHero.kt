package com.example.meteomate.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteomate.R
import com.example.meteomate.data.TemperatureUnit
import com.example.meteomate.util.TemperatureFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherCompanionHero(
    temperature: Double,
    feelsLike: Double,
    weatherCode: Int,
    weatherDescription: String,
    locationName: String,
    humidity: Int,
    windSpeed: Double,
    precipitationProbability: Double,
    isDay: Boolean,
    temperatureUnit: TemperatureUnit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mood by remember(weatherCode) { mutableIntStateOf(0) }
    var selectedHint by remember { mutableStateOf<String?>(null) }
    val characterInteraction = remember { MutableInteractionSource() }
    val messages = listOf(
        companionMessage(weatherCode, temperature),
        "Я сверяю небо с прогнозом — всё под контролем.",
        "Щекотно! Погоду быстрее не переключить, но я стараюсь."
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.linearGradient(companionGradient(weatherCode), Offset.Zero, Offset(900f, 1100f)))
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    locationName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_to_favorites),
                        tint = if (isFavorite) Color(0xFFFFCCD5) else Color.White.copy(alpha = .8f)
                    )
                }
            }

            Box(
                Modifier.width(225.dp).clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = .16f)).padding(14.dp)
            ) {
                Text(messages[mood % messages.size], color = Color.White, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(26.dp))
            Text(
                TemperatureFormatter.format(temperature, temperatureUnit),
                color = Color.White,
                fontSize = 68.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            )
            Text(
                "Ощущается как ${TemperatureFormatter.format(feelsLike, temperatureUnit)}",
                color = Color.White.copy(alpha = .68f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                weatherDescription.replaceFirstChar { it.uppercase() },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                precipitationSummary(weatherCode, precipitationProbability),
                color = Color.White.copy(alpha = .92f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HintChip("Когда гулять?") { selectedHint = walkingAdvice(weatherCode, precipitationProbability, windSpeed) }
                HintChip("Нужен зонт?") { selectedHint = umbrellaAdvice(weatherCode, precipitationProbability) }
                HintChip("Совет на день") { selectedHint = comfortAdvice(feelsLike, humidity, windSpeed) }
            }

            selectedHint?.let { hint ->
                Box(
                    Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = .13f)).clickable { selectedHint = null }.padding(14.dp)
                ) {
                    Text("✦  $hint", color = Color.White)
                }
            }
        }

        MeteoMateCharacter(
            weatherCode,
            isDay,
            Modifier.align(Alignment.TopEnd).padding(top = 96.dp).size(142.dp).clickable(
                interactionSource = characterInteraction,
                indication = null
            ) { mood++ }
        )
    }
}

@Composable
private fun HintChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .16f))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp)
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Medium) }
}

@Composable
private fun MeteoMateCharacter(weatherCode: Int, isDay: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        when {
            weatherCode == 800 && isDay -> drawSunCharacter()
            weatherCode == 800 -> drawMoonCharacter()
            weatherCode in 200..232 -> drawCloudCharacter(CloudDecoration.THUNDER)
            weatherCode in 300..531 -> drawCloudCharacter(CloudDecoration.RAIN)
            weatherCode in 600..622 -> drawCloudCharacter(CloudDecoration.SNOW)
            weatherCode in 700..799 -> drawFogCharacter()
            else -> drawCloudCharacter(CloudDecoration.NONE)
        }
    }
}

private enum class CloudDecoration { NONE, RAIN, SNOW, THUNDER }

private fun DrawScope.drawSunCharacter() {
    val c = Offset(size.width * .54f, size.height * .49f)
    val radius = size.minDimension * .29f
    repeat(10) { index ->
        val angle = index * (2.0 * PI / 10.0)
        val start = radius * 1.25f
        val end = radius * 1.55f
        drawLine(
            Color(0xFFFFE38A),
            Offset(c.x + cos(angle).toFloat() * start, c.y + sin(angle).toFloat() * start),
            Offset(c.x + cos(angle).toFloat() * end, c.y + sin(angle).toFloat() * end),
            size.minDimension * .055f,
            cap = StrokeCap.Round
        )
    }
    drawCircle(Color(0xFFFFD45A).copy(alpha = .22f), radius * 1.28f, c)
    drawCircle(Color(0xFFFFD45A), radius, c)
    drawFriendlyFace(c, radius)
}

private fun DrawScope.drawMoonCharacter() {
    val c = Offset(size.width * .50f, size.height * .48f)
    val radius = size.minDimension * .34f
    drawCircle(Color(0xFFFFE9A9).copy(alpha = .20f), radius * 1.25f, c)
    drawCircle(Color(0xFFFFE9A9), radius, c)
    drawCircle(Color(0xFF31529A), radius * .84f, Offset(c.x + radius * .46f, c.y - radius * .18f))
    val faceCenter = Offset(c.x - radius * .18f, c.y + radius * .08f)
    drawFriendlyFace(faceCenter, radius * .72f)
    drawCircle(Color.White.copy(alpha = .9f), radius * .055f, Offset(size.width * .80f, size.height * .20f))
    drawCircle(Color.White.copy(alpha = .65f), radius * .035f, Offset(size.width * .72f, size.height * .08f))
}

private fun DrawScope.drawCloudCharacter(decoration: CloudDecoration) {
    val cloud = when (decoration) {
        CloudDecoration.THUNDER -> Color(0xFFB6AAE8)
        CloudDecoration.RAIN -> Color(0xFF8FD9F2)
        CloudDecoration.SNOW -> Color(0xFFE7F6FF)
        CloudDecoration.NONE -> Color(0xFFBFE5E8)
    }
    val c = Offset(size.width * .52f, size.height * .48f)
    drawCircle(cloud.copy(alpha = .18f), size.minDimension * .47f, c)
    drawRoundRect(cloud, Offset(size.width * .18f, size.height * .42f),
        Size(size.width * .68f, size.height * .34f), CornerRadius(size.minDimension * .17f))
    drawCircle(cloud, size.minDimension * .22f, Offset(size.width * .39f, size.height * .40f))
    drawCircle(cloud, size.minDimension * .27f, Offset(size.width * .59f, size.height * .35f))
    drawCircle(cloud, size.minDimension * .18f, Offset(size.width * .76f, size.height * .48f))
    drawFriendlyFace(Offset(size.width * .53f, size.height * .53f), size.minDimension * .25f)

    when (decoration) {
        CloudDecoration.RAIN -> listOf(.32f, .52f, .72f).forEach { x ->
            drawLine(Color(0xFF62CFFF), Offset(size.width * x, size.height * .79f),
                Offset(size.width * (x - .035f), size.height * .91f), size.minDimension * .035f, cap = StrokeCap.Round)
        }
        CloudDecoration.SNOW -> listOf(.32f, .52f, .72f).forEach { x ->
            drawCircle(Color.White, size.minDimension * .04f, Offset(size.width * x, size.height * .86f))
        }
        CloudDecoration.THUNDER -> {
            val bolt = Path().apply {
                moveTo(size.width * .54f, size.height * .74f)
                lineTo(size.width * .43f, size.height * .88f)
                lineTo(size.width * .53f, size.height * .87f)
                lineTo(size.width * .47f, size.height * .99f)
                lineTo(size.width * .66f, size.height * .80f)
                lineTo(size.width * .56f, size.height * .81f)
                close()
            }
            drawPath(bolt, Color(0xFFFFD84D))
        }
        CloudDecoration.NONE -> Unit
    }
}

private fun DrawScope.drawFogCharacter() {
    val c = Offset(size.width * .5f, size.height * .45f)
    drawCircle(Color(0xFFB9CEDD).copy(alpha = .18f), size.minDimension * .45f, c)
    val fog = Color(0xFFC9DCE8)
    drawRoundRect(fog, Offset(size.width * .18f, size.height * .27f), Size(size.width * .64f, size.height * .18f), CornerRadius(40f))
    drawRoundRect(fog.copy(alpha = .92f), Offset(size.width * .10f, size.height * .49f), Size(size.width * .76f, size.height * .18f), CornerRadius(40f))
    drawRoundRect(fog.copy(alpha = .75f), Offset(size.width * .25f, size.height * .71f), Size(size.width * .62f, size.height * .15f), CornerRadius(40f))
    drawFriendlyFace(Offset(size.width * .50f, size.height * .53f), size.minDimension * .23f)
}

private fun DrawScope.drawFriendlyFace(center: Offset, scale: Float) {
    val ink = Color(0xFF26323D)
    val eyeWidth = scale * .19f
    val eyeHeight = scale * .31f
    val eyeY = center.y - scale * .17f
    listOf(center.x - scale * .30f, center.x + scale * .11f).forEach { eyeX ->
        drawRoundRect(ink, Offset(eyeX, eyeY), Size(eyeWidth, eyeHeight), CornerRadius(eyeWidth))
        drawCircle(Color.White.copy(alpha = .72f), eyeWidth * .16f, Offset(eyeX + eyeWidth * .65f, eyeY + eyeHeight * .24f))
    }
    drawCircle(Color(0xFFFF9AAE).copy(alpha = .42f), scale * .10f, Offset(center.x - scale * .43f, center.y + scale * .18f))
    drawCircle(Color(0xFFFF9AAE).copy(alpha = .42f), scale * .10f, Offset(center.x + scale * .43f, center.y + scale * .18f))
    drawArc(
        ink, 18f, 144f, false,
        Offset(center.x - scale * .23f, center.y + scale * .02f),
        Size(scale * .46f, scale * .34f),
        style = Stroke(scale * .075f, cap = StrokeCap.Round)
    )
}

private fun companionGradient(code: Int) = when (code) {
    in 200..232 -> listOf(Color(0xFF351C75), Color(0xFF1C4D8E), Color(0xFF152A52))
    in 500..531 -> listOf(Color(0xFF146C94), Color(0xFF1A4A76), Color(0xFF182A55))
    in 600..622 -> listOf(Color(0xFF4A79A8), Color(0xFF4668A5), Color(0xFF263E73))
    800 -> listOf(Color(0xFF246BCE), Color(0xFF3865C8), Color(0xFF183A83))
    else -> listOf(Color(0xFF286A8F), Color(0xFF395EAA), Color(0xFF24356E))
}

private fun companionMessage(code: Int, temperature: Double) = when {
    code in 200..232 -> "Гром сегодня серьёзный. Я бы выбрал уютный маршрут."
    code in 500..531 -> "Собрал для тебя все капли. Зонт пригодится."
    code in 600..622 -> "Снег меняет город — и время в пути тоже."
    code in 700..799 -> "Видимость капризничает. Двигайся без спешки."
    temperature >= 30 -> "Жарко даже мне. Вода и тень сегодня лучшие союзники."
    temperature <= -10 -> "Мороз проверяет характер. Утепляйся основательно."
    code == 800 -> "Небо открыто. Хороший день, чтобы выйти наружу."
    else -> "Я слежу за небом, пока ты планируешь день."
}

private fun precipitationSummary(code: Int, probability: Double) = when {
    code in 200..232 -> "Возможна гроза — следите за предупреждениями"
    code in 500..531 || probability >= 70 -> "Осадки весьма вероятны"
    code in 600..622 -> "Ожидается снег"
    probability >= 35 -> "Осадки возможны"
    else -> "Существенных осадков не ожидается"
}

private fun walkingAdvice(code: Int, probability: Double, windSpeed: Double) = when {
    code in 200..232 -> "Прогулку лучше перенести до окончания грозы."
    probability >= 70 -> "Выберите короткий маршрут рядом с укрытиями."
    windSpeed >= 12 -> "Сильный ветер: избегайте деревьев и шатких конструкций."
    else -> "Ближайшие часы подходят для прогулки."
}

private fun umbrellaAdvice(code: Int, probability: Double) =
    if (code in 200..531 || probability >= 45) "Да, зонт сегодня лучше взять."
    else "Скорее всего, зонт можно оставить дома."

private fun comfortAdvice(feelsLike: Double, humidity: Int, windSpeed: Double) = when {
    feelsLike <= 0 -> "Тёплая куртка и перчатки будут кстати."
    feelsLike <= 12 -> "Лучше добавить лёгкую куртку или ветровку."
    feelsLike >= 27 && humidity >= 65 -> "Будет душно: выбирайте лёгкую одежду и берите воду."
    windSpeed >= 9 -> "Одежда с защитой от ветра сделает день комфортнее."
    else -> "Условия комфортные — подойдёт обычная одежда по сезону."
}
