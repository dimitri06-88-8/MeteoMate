package com.example.meteomate.ui.component

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.meteomate.R
import kotlin.random.Random

@Composable
fun WeatherLoadingAnimation(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    val transition = rememberInfiniteTransition(label = "loading")

    val gradientProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    val ring1Alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    val ring1Scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1scale"
    )

    val ring2Alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    val ring2Scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2scale"
    )

    val ring3Alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic, delayMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    val ring3Scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic, delayMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3scale"
    )

    val floatY by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val dotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dotAlpha2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dotAlpha3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    val particles = remember {
        List(12) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.002f + Random.nextFloat() * 0.004f,
                size = 1f + Random.nextFloat() * 2f,
                delay = Random.nextFloat() * 4000f
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGradientBackground(gradientProgress)

            particles.forEach { particle ->
                drawParticle(particle)
            }

            val cx = size.width / 2f
            val cy = size.height / 2f - 40f
            val maxRing = size.minDimension * 0.5f

            drawRing(cx, cy, maxRing, ring1Scale, ring1Alpha, accentColor)
            drawRing(cx, cy, maxRing, ring2Scale, ring2Alpha, accentColor)
            drawRing(cx, cy, maxRing, ring3Scale, ring3Alpha, accentColor)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            Text(
                text = "\u26C5",
                fontSize = 64.sp,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .graphicsLayer { translationY = floatY }
            )

            Text(
                text = "MeteoMate",
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.loading) + if (dotAlpha > 0.7f) "." else " " +
                        if (dotAlpha2 > 0.7f) "." else " " +
                        if (dotAlpha3 > 0.7f) "." else " ",
                style = MaterialTheme.typography.bodyMedium,
                color = accentColor.copy(alpha = 0.7f)
            )
        }
    }
}

private val gradientColors = listOf(
    Color(0xFF4A90D9), Color(0xFF357ABD), Color(0xFF2E6BA8),
    Color(0xFF4A7BB5), Color(0xFF5B8FC9), Color(0xFF4A90D9)
)

private fun DrawScope.drawGradientBackground(progress: Float) {
    val index = (progress * (gradientColors.size - 1)).toInt().coerceAtMost(gradientColors.size - 2)
    val frac = (progress * (gradientColors.size - 1)) - index
    val c1 = gradientColors[index]
    val c2 = gradientColors[index + 1]
    val color = Color(
        red = c1.red + (c2.red - c1.red) * frac,
        green = c1.green + (c2.green - c1.green) * frac,
        blue = c1.blue + (c2.blue - c1.blue) * frac,
        alpha = 0.3f
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color, color.copy(alpha = 0.1f), color.copy(alpha = 0.05f))
        ),
        size = size
    )
}

private fun DrawScope.drawRing(cx: Float, cy: Float, maxR: Float, scale: Float, alpha: Float, color: Color) {
    if (alpha <= 0.01f) return
    val radius = maxR * scale
    drawCircle(
        color = color.copy(alpha = alpha * 0.5f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
    )
}

private data class Particle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val delay: Float
)

private fun DrawScope.drawParticle(particle: Particle) {
    val time = System.nanoTime() / 1_000_000_000f
    val t = (time * particle.speed + particle.delay) % 1f
    val px = particle.x * size.width
    val py = (particle.y - t) * size.height + 50f
    if (py < -50f) return
    drawCircle(
        color = Color.White.copy(alpha = (0.4f * (1f - t)).coerceIn(0f, 0.4f)),
        radius = particle.size,
        center = Offset(px, py)
    )
}
