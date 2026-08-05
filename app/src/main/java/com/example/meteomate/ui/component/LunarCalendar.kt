package com.example.meteomate.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.meteomate.R
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object MoonPhaseCalculator {
    fun calculate(date: Calendar): MoonPhaseInfo {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)

        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        val julianDay = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        val knownNewMoon = 2451550.1
        val lunarCycle = 29.53058867
        val daysSince = julianDay - knownNewMoon
        var phase = (daysSince / lunarCycle) % 1.0
        if (phase < 0) phase += 1.0

        val illumination = (1.0 - cos(phase * 2.0 * PI)) / 2.0

        val name = when {
            phase < 0.025 || phase >= 0.975 -> MoonPhase.NewMoon
            phase < 0.25 -> MoonPhase.WaxingCrescent
            phase < 0.275 -> MoonPhase.FirstQuarter
            phase < 0.5 -> MoonPhase.WaxingGibbous
            phase < 0.525 -> MoonPhase.FullMoon
            phase < 0.75 -> MoonPhase.WaningGibbous
            phase < 0.775 -> MoonPhase.LastQuarter
            else -> MoonPhase.WaningCrescent
        }

        val nextNewMoon = knownNewMoon + lunarCycle * (kotlin.math.floor(daysSince / lunarCycle) + 1)
        val nextFullMoon = knownNewMoon + lunarCycle * (kotlin.math.floor((daysSince - 0.5) / lunarCycle) + 0.5 + 1)

        return MoonPhaseInfo(
            phase = phase.toFloat(),
            illumination = illumination.toFloat(),
            name = name,
            nextNewMoon = julianDayToCalendar(nextNewMoon),
            nextFullMoon = julianDayToCalendar(nextFullMoon)
        )
    }

    private fun julianDayToCalendar(jd: Double): Calendar {
        val a = if (jd < 2299161) jd.toInt() else {
            val alpha = ((jd - 1867216.25) / 36524.25).toInt()
            jd.toInt() + 1 + alpha - alpha / 4
        }
        val b = a + 1524
        val c = ((b - 122.1) / 365.25).toInt()
        val d = (365.25 * c).toInt()
        val e = ((b - d) / 30.6001).toInt()
        val day = b - d - (30.6001 * e).toInt()
        val month = if (e < 14) e - 1 else e - 13
        val year = if (month > 2) c - 4716 else c - 4715
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 12, 0, 0)
        return cal
    }
}

enum class MoonPhase(val displayNameRes: Int, val emoji: String) {
    NewMoon(R.string.new_moon, "\uD83C\uDF11"),
    WaxingCrescent(R.string.waxing_crescent, "\uD83C\uDF12"),
    FirstQuarter(R.string.first_quarter, "\uD83C\uDF13"),
    WaxingGibbous(R.string.waxing_gibbous, "\uD83C\uDF14"),
    FullMoon(R.string.full_moon, "\uD83C\uDF15"),
    WaningGibbous(R.string.waning_gibbous, "\uD83C\uDF16"),
    LastQuarter(R.string.last_quarter, "\uD83C\uDF17"),
    WaningCrescent(R.string.waning_crescent, "\uD83C\uDF18")
}

data class MoonPhaseInfo(
    val phase: Float,
    val illumination: Float,
    val name: MoonPhase,
    val nextNewMoon: Calendar,
    val nextFullMoon: Calendar
)

@Composable
fun MoonPhaseSection(modifier: Modifier = Modifier) {
    var info by remember { mutableStateOf(MoonPhaseCalculator.calculate(Calendar.getInstance())) }

    LaunchedEffect(Unit) {
        while (true) {
            info = MoonPhaseCalculator.calculate(Calendar.getInstance())
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val animatedIllumination by animateFloatAsState(
        targetValue = info.illumination,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "moonIllumination"
    )

    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.lunar_calendar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoonPhaseCanvas(
                    phase = info.phase,
                    size = 96.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = info.name.emoji,
                        fontSize = 28.sp
                    )
                    Text(
                        text = stringResource(info.name.displayNameRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(info.illumination * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DateLabel(
                    emoji = "\uD83C\uDF11",
                    label = stringResource(R.string.new_moon),
                    date = info.nextNewMoon
                )
                DateLabel(
                    emoji = "\uD83C\uDF15",
                    label = stringResource(R.string.full_moon),
                    date = info.nextFullMoon
                )
            }
        }
    }
}

@Composable
private fun DateLabel(emoji: String, label: String, date: Calendar) {
    val day = date.get(Calendar.DAY_OF_MONTH)
    val month = date.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$day $month",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MoonPhaseCanvas(
    phase: Float,
    size: androidx.compose.ui.unit.Dp = 96.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = size.toPx() / 2f
        val cy = size.toPx() / 2f
        val radius = size.toPx() / 2f - 4.dp.toPx()

        val isWaxing = phase < 0.5

        drawCircle(
            color = Color(0xFF2C2C3E),
            radius = radius,
            center = Offset(cx, cy)
        )

        drawCircle(
            color = Color(0xFF1a1a2e),
            radius = radius,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )

        val glowRadius = radius + 6.dp.toPx()
        drawCircle(
            color = Color(0x22B0C4DE),
            radius = glowRadius,
            center = Offset(cx, cy)
        )

        val illuminated = Path()
        val shadow = Path()

        if (phase in 0.49f..0.51f) {
            illuminated.addOval(
                androidx.compose.ui.geometry.Rect(
                    Offset(cx - radius, cy - radius),
                    Offset(cx + radius, cy + radius)
                )
            )
        } else {
            val rightWidth = when {
                phase <= 0.25f -> phase * 4f
                phase <= 0.5f -> 1f - (phase - 0.25f) * 4f
                phase <= 0.75f -> (phase - 0.5f) * 4f
                else -> 1f - (phase - 0.75f) * 4f
            }

            val leftWidth = if (isWaxing) {
                if (phase <= 0.25f) 0f else (phase - 0.25f) * 4f
            } else {
                if (phase <= 0.75f) 1f else 1f - (phase - 0.75f) * 4f
            }

            val rw = radius * rightWidth
            val lw = radius * leftWidth

            illuminated.apply {
                moveTo(cx - lw, cy - radius * 0.3f)
                for (i in 0..36) {
                    val angle = PI.toFloat() * i / 36f
                    val y = cy - radius * cos(angle)
                    val x = cx + rw * sin(angle)
                    lineTo(x, y)
                }
                for (i in 36 downTo 0) {
                    val angle = PI.toFloat() * i / 36f
                    val y = cy - radius * cos(angle)
                    val x = cx - lw * sin(angle)
                    lineTo(x, y)
                }
                close()
            }

            shadow.apply {
                moveTo(cx + rw, cy - radius)
                for (i in 0..36) {
                    val angle = PI.toFloat() * i / 36f
                    val y = cy - radius * cos(angle)
                    val x = cx + rw * sin(angle)
                    lineTo(x, y)
                }
                for (i in 36 downTo 0) {
                    val angle = PI.toFloat() * i / 36f
                    val y = cy - radius * cos(angle)
                    val x = cx - lw * sin(angle)
                    lineTo(x, y)
                }
                close()
            }
        }

        drawPath(
            path = illuminated,
            color = Color(0xFFF5F5DC).copy(alpha = 0.9f)
        )

        drawPath(
            path = shadow,
            color = Color(0xFF1a1a2e)
        )

        if (phase in 0.48f..0.52f) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color(0x22FFF8DC), Color(0x00FFF8DC)),
                    center = Offset(cx, cy)
                ),
                radius = radius * 1.8f,
                center = Offset(cx, cy)
            )
        }
    }
}


