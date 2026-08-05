package com.example.meteomate.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class WeatherAnimationType {
    ClearDay, ClearNight, Cloudy, Rain, Snow, Thunderstorm, Fog
}

@Composable
fun WeatherBackgroundAnimation(
    weatherCode: Int,
    modifier: Modifier = Modifier
) {
    val isNight = weatherCode == 800 &&
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) !in 6..20

    val weatherType = when {
        weatherCode in 200..299 -> WeatherAnimationType.Thunderstorm
        weatherCode in 300..599 -> WeatherAnimationType.Rain
        weatherCode in 600..699 -> WeatherAnimationType.Snow
        weatherCode in 700..799 -> WeatherAnimationType.Fog
        weatherCode == 800 -> if (isNight) WeatherAnimationType.ClearNight else WeatherAnimationType.ClearDay
        weatherCode in 801..804 -> WeatherAnimationType.Cloudy
        else -> WeatherAnimationType.ClearDay
    }

    val transition = rememberInfiniteTransition(label = "weather_bg")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val flashAlpha by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "flash"
    )

    val clearTransition = rememberInfiniteTransition(label = "clear_bg")
    val sunPulse by clearTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = androidx.compose.animation.core.EaseInOutCubic), RepeatMode.Reverse),
        label = "sunPulse"
    )
    val cloudPhase by clearTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudPhase"
    )

    val params = remember { AnimationParams.generate() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val ctx = AnimationContext(phase, flashAlpha, sunPulse, cloudPhase, params, size)
        val strategy: WeatherDrawStrategy = when (weatherType) {
            WeatherAnimationType.ClearDay -> ClearDayStrategy
            WeatherAnimationType.ClearNight -> ClearNightStrategy
            WeatherAnimationType.Cloudy -> CloudyStrategy
            WeatherAnimationType.Rain -> RainStrategy
            WeatherAnimationType.Snow -> SnowStrategy
            WeatherAnimationType.Thunderstorm -> ThunderstormStrategy
            WeatherAnimationType.Fog -> FogStrategy
        }
        strategy.draw(this, ctx)
    }
}

private data class AnimationParams(
    val particles: List<ParticleParams>,
    val rainDrops: List<RainDropParams>,
    val snowflakes: List<SnowflakeParams>,
    val cloudLayers: List<CloudLayerParams>,
    val stars: List<StarParams>,
    val mistLayers: List<MistLayerParams>
) {
    companion object {
        fun generate() = AnimationParams(
            particles = (1..30).map { ParticleParams(it.toFloat(), 0.3f + Random.nextFloat() * 0.7f, 1.5f + Random.nextFloat() * 2.5f, Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 0.3f) },
            rainDrops = (1..60).map { RainDropParams(it.toFloat(), 0.6f + Random.nextFloat() * 0.4f, 15f + Random.nextFloat() * 20f, Random.nextFloat(), Random.nextFloat()) },
            snowflakes = (1..40).map { SnowflakeParams(it.toFloat(), 0.15f + Random.nextFloat() * 0.3f, 1.5f + Random.nextFloat() * 3f, Random.nextFloat(), 15f + Random.nextFloat() * 30f, 0.5f + Random.nextFloat() * 1.5f, Random.nextFloat() * 5f) },
            cloudLayers = listOf(CloudLayerParams(0.02f, 0.15f, 1.2f, 0.12f), CloudLayerParams(0.035f, 0.35f, 0.9f, 0.09f), CloudLayerParams(0.05f, 0.55f, 0.7f, 0.06f), CloudLayerParams(0.015f, 0.08f, 1.5f, 0.15f)),
            stars = (1..50).map { StarParams(Random.nextFloat(), Random.nextFloat(), 0.8f + Random.nextFloat() * 1.8f, 0.5f + Random.nextFloat() * 2f, Random.nextFloat() * PI.toFloat() * 2f) },
            mistLayers = (1..5).map { MistLayerParams(it.toFloat(), 0.01f + Random.nextFloat() * 0.02f, Random.nextFloat() * 0.8f, 1.5f + Random.nextFloat() * 1f, 0.04f + Random.nextFloat() * 0.04f, Random.nextFloat() * PI.toFloat() * 2f) }
        )
    }
}

private data class AnimationContext(
    val phase: Float, val flashAlpha: Float, val sunPulse: Float, val cloudPhase: Float,
    val params: AnimationParams, val size: androidx.compose.ui.geometry.Size
)
private data class ParticleParams(val seed: Float, val speed: Float, val size: Float, val startX: Float, val startY: Float, val wobble: Float)
private data class RainDropParams(val seed: Float, val speed: Float, val length: Float, val startX: Float, val delay: Float)
private data class SnowflakeParams(val seed: Float, val speed: Float, val size: Float, val startX: Float, val wobbleAmp: Float, val wobbleFreq: Float, val delay: Float)
private data class CloudLayerParams(val speed: Float, val height: Float, val scale: Float, val alpha: Float)
private data class StarParams(val x: Float, val y: Float, val size: Float, val twinkleSpeed: Float, val twinkleOffset: Float)
private data class MistLayerParams(val seed: Float, val speed: Float, val height: Float, val width: Float, val alpha: Float, val phase: Float)

// Strategy interfaces and implementations
private object ClearDayStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        val maxDim = maxOf(ctx.size.width, ctx.size.height)
        val cx = ctx.size.width * 0.75f
        val cy = ctx.size.height * 0.18f

        val glowRadius = maxDim * 0.35f * ctx.sunPulse
        drawCircle(brush = Brush.radialGradient(listOf(Color(0x55FFD700), Color(0x30FFA500), Color(0x10FF8C00), Color(0x00FF8C00)), center = Offset(cx, cy), radius = glowRadius), radius = glowRadius, center = Offset(cx, cy))
        val innerGlow = maxDim * 0.12f * ctx.sunPulse
        drawCircle(brush = Brush.radialGradient(listOf(Color(0x99FFF8DC), Color(0x55FFD700), Color(0x00FFA500)), center = Offset(cx, cy), radius = innerGlow), radius = innerGlow, center = Offset(cx, cy))

        val sunRadius = maxDim * 0.04f
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFF8DC), Color(0xFFFFD700), Color(0xE6FFA500)), center = Offset(cx, cy), radius = sunRadius), radius = sunRadius, center = Offset(cx, cy))

        val rayAngle = ctx.phase * PI.toFloat() * 2f
        for (i in 0 until 8) {
            val angle = rayAngle + i * (PI.toFloat() / 4f)
            val alpha = (0.04f + 0.03f * sin(angle * 2f + ctx.phase * 4f)) * ctx.sunPulse
            drawLine(color = Color(0xFFFFF8DC).copy(alpha = alpha), start = Offset(cx + cos(angle) * sunRadius * 5f, cy + sin(angle) * sunRadius * 5f), end = Offset(cx + cos(angle) * maxDim * 0.45f, cy + sin(angle) * maxDim * 0.45f), strokeWidth = 1.5f)
        }

        ctx.params.particles.forEach { p ->
            val t = (ctx.phase * p.speed + p.seed * 0.1f) % 1f
            val px = (p.startX + 0.1f * sin(t * PI.toFloat() * 2f + p.seed)) * ctx.size.width
            val py = (p.startY - t * 0.6f) * ctx.size.height
            if (py > 0f) drawCircle(color = Color(0x55FFF8DC), radius = p.size * 0.6f, center = Offset(px, py))
        }

        listOf(Pair(0.12f, 0.10f) to 0.08f, Pair(0.45f, 0.15f) to 0.06f, Pair(0.70f, 0.08f) to 0.05f).forEach { (pos, alpha) ->
            drawPuffyCloud(Offset(((pos.second + ctx.cloudPhase * 0.1f) % 1.2f - 0.1f) * ctx.size.width, pos.first * ctx.size.height), maxDim * 0.08f, alpha, Color.White)
        }
    }
}

private object ClearNightStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        val maxDim = maxOf(ctx.size.width, ctx.size.height)
        val cx = ctx.size.width * 0.75f
        val cy = ctx.size.height * 0.15f
        val moonRadius = maxDim * 0.035f

        drawCircle(brush = Brush.radialGradient(listOf(Color(0x22B0C4DE), Color(0x10B0C4DE), Color(0x00B0C4DE)), center = Offset(cx, cy), radius = maxDim * 0.3f), radius = maxDim * 0.3f, center = Offset(cx, cy))
        drawCircle(color = Color(0xCCF5F5DC), radius = moonRadius, center = Offset(cx, cy))
        drawCircle(color = Color(0x33FFF8DC), radius = moonRadius * 2f, center = Offset(cx, cy))
        drawCircle(color = Color(0x15FFF8DC), radius = moonRadius * 4f, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF1a1a2e), radius = moonRadius * 0.85f, center = Offset(cx + moonRadius * 0.25f, cy - moonRadius * 0.15f))

        ctx.params.stars.forEach { star ->
            val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(ctx.phase * PI.toFloat() * 2f * star.twinkleSpeed + star.twinkleOffset))
            val alpha = (0.4f + 0.6f * twinkle).coerceIn(0f, 1f)
            drawCircle(color = Color.White.copy(alpha = alpha), radius = star.size, center = Offset(star.x * ctx.size.width, star.y * ctx.size.height))
            if (star.size > 1.5f) drawCircle(color = Color.White.copy(alpha = alpha * 0.15f), radius = star.size * 3f, center = Offset(star.x * ctx.size.width, star.y * ctx.size.height))
        }
    }
}

private object CloudyStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        val maxDim = maxOf(ctx.size.width, ctx.size.height)
        ctx.params.cloudLayers.forEach { layer ->
            val t = (ctx.phase * layer.speed * 10f) % 2f
            val cx1 = ((t * 0.5f + layer.height * 0.3f) % 1.2f - 0.1f) * ctx.size.width
            drawPuffyCloud(Offset(cx1, layer.height * ctx.size.height), maxDim * 0.12f * layer.scale, layer.alpha, Color.White)
            val cx2 = ((t * 0.5f + layer.height * 0.3f + 0.6f) % 1.2f - 0.1f) * ctx.size.width
            drawPuffyCloud(Offset(cx2, layer.height * ctx.size.height + maxDim * 0.12f * layer.scale * 0.3f), maxDim * 0.12f * layer.scale * 0.7f, layer.alpha * 0.7f, Color.White)
        }
    }
}

private object RainStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        ctx.params.rainDrops.forEach { drop ->
            val t = (ctx.phase * drop.speed + drop.delay) % 1f
            val px = drop.startX * ctx.size.width + 10f * sin(drop.seed + t * PI.toFloat() * 2f)
            val py = (t * 1.2f - 0.1f) * ctx.size.height
            val alpha = (0.15f * (1f - abs(t - 0.5f) * 2f)).coerceIn(0.02f, 0.15f)
            drawLine(color = Color.White.copy(alpha = alpha), start = Offset(px, py - drop.length * 0.7f), end = Offset(px + drop.length * 0.3f, py), strokeWidth = 1.2f)
        }
        val splashPhase = (ctx.phase * 48f) % 1f
        for (i in 0 until 3) {
            val sx = (0.2f + 0.6f * sin(i * 2.1f + ctx.phase * 8f)) * ctx.size.width
            val sy = ctx.size.height * (0.85f + 0.1f * sin(i * 1.7f + ctx.phase * 5f))
            drawCircle(color = Color.White.copy(alpha = (0.06f * (1f - splashPhase)).coerceIn(0f, 0.06f)), radius = 3f + splashPhase * 20f, center = Offset(sx, sy), style = Stroke(width = 0.8f))
        }
    }
}

private object SnowStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        val maxDim = maxOf(ctx.size.width, ctx.size.height)
        ctx.params.snowflakes.forEach { flake ->
            val t = (ctx.phase * flake.speed + flake.delay) % 1f
            val wobble = sin(t * PI.toFloat() * 2f * flake.wobbleFreq + flake.seed) * flake.wobbleAmp
            val px = flake.startX * ctx.size.width + wobble
            val py = (t * 1.2f - 0.1f) * ctx.size.height
            val alpha = (0.5f * (1f - abs(t - 0.5f) * 1.5f)).coerceIn(0.1f, 0.5f)
            val r = (flake.size * maxDim / 1000f).coerceAtLeast(1f)
            drawCircle(color = Color.White.copy(alpha = alpha), radius = r, center = Offset(px, py))
            if (flake.size > 2.5f) drawCircle(color = Color.White.copy(alpha = alpha * 0.15f), radius = r * 3f, center = Offset(px, py))
        }
    }
}

private object ThunderstormStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        RainStrategy.draw(scope, ctx)
        val flash = ((ctx.flashAlpha + 0.1f) * 10f).toInt() % 10 == 0
        if (flash) {
            drawRect(color = Color.White.copy(alpha = 0.25f * (1f - ctx.flashAlpha * 2f % 1f)), size = ctx.size)
            val boltX = ctx.size.width * (0.3f + 0.4f * ctx.flashAlpha)
            val boltY = ctx.size.height * 0.15f
            val path = Path().apply {
                moveTo(boltX, boltY)
                var bx = boltX; var by = boltY
                repeat(5) { bx += (-20f + 40f * (ctx.phase * 7f + it * 1.3f) % 1f); by += ctx.size.height * 0.12f + 10f * (ctx.phase * 3f + it * 2.7f) % 1f; lineTo(bx, by) }
            }
            drawPath(path = path, color = Color.White.copy(alpha = 0.4f * (1f - ctx.flashAlpha * 2f % 1f)), style = Stroke(width = 2.5f))
            drawPath(path = path, color = Color(0xFFB0C4DE).copy(alpha = 0.15f * (1f - ctx.flashAlpha * 2f % 1f)), style = Stroke(width = 6f))
        }
    }
}

private object FogStrategy : WeatherDrawStrategy {
    override fun draw(scope: DrawScope, ctx: AnimationContext) = with(scope) {
        ctx.params.mistLayers.forEach { layer ->
            val t = ctx.phase * layer.speed * 100f + layer.phase
            val cx = ((t * 0.5f + 0.5f * sin(t * 0.3f + layer.seed)) % 1.5f - 0.25f) * ctx.size.width
            val cy = layer.height * ctx.size.height + 30f * sin(t * 0.2f + layer.seed * 2f)
            val w = ctx.size.width * layer.width
            val h = ctx.size.height * 0.12f
            drawOval(brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = layer.alpha), Color.White.copy(alpha = layer.alpha * 0.5f), Color.White.copy(alpha = 0f)), startX = cx - w * 0.5f, endX = cx + w * 0.5f), topLeft = Offset(cx - w * 0.5f, cy - h * 0.5f), size = androidx.compose.ui.geometry.Size(w, h))
        }
    }
}

private interface WeatherDrawStrategy {
    fun draw(scope: DrawScope, ctx: AnimationContext)
}

private fun DrawScope.drawPuffyCloud(center: Offset, scale: Float, alpha: Float, tint: Color) {
    if (alpha < 0.005f) return
    val color = tint.copy(alpha = alpha)
    listOf(Offset(0f, 0f) to 1f, Offset(-scale * 0.7f, scale * 0.1f) to 0.75f, Offset(scale * 0.75f, scale * 0.05f) to 0.8f, Offset(-scale * 0.35f, -scale * 0.35f) to 0.65f, Offset(scale * 0.4f, -scale * 0.3f) to 0.7f, Offset(scale * 1.15f, scale * 0.15f) to 0.5f, Offset(-scale * 0.9f, scale * 0.2f) to 0.5f).forEach { (off, sf) ->
        drawCircle(color = color, radius = scale * 0.45f * sf, center = center + off)
    }
}
