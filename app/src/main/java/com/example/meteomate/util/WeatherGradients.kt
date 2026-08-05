package com.example.meteomate.util

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.meteomate.ui.theme.ClearDayBottom
import com.example.meteomate.ui.theme.ClearDayTop
import com.example.meteomate.ui.theme.ClearNightBottom
import com.example.meteomate.ui.theme.ClearNightTop
import com.example.meteomate.ui.theme.CloudyBottom
import com.example.meteomate.ui.theme.CloudyTop
import com.example.meteomate.ui.theme.RainBottom
import com.example.meteomate.ui.theme.RainTop
import com.example.meteomate.ui.theme.SnowBottom
import com.example.meteomate.ui.theme.SnowTop
import com.example.meteomate.ui.theme.ThunderBottom
import com.example.meteomate.ui.theme.ThunderTop
import java.util.Calendar

object WeatherGradients {
    fun forWeatherCode(code: Int): Pair<Color, Color> = when {
        code in 200..299 -> ThunderTop to ThunderBottom
        code in 300..599 -> RainTop to RainBottom
        code in 600..699 -> SnowTop to SnowBottom
        code in 700..799 -> CloudyTop to CloudyBottom
        code == 800 -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour in 6..20) ClearDayTop to ClearDayBottom
            else ClearNightTop to ClearNightBottom
        }
        code in 801..804 -> CloudyTop to CloudyBottom
        else -> ClearDayTop to ClearDayBottom
    }

    fun backgroundBrush(code: Int): Brush = Brush.verticalGradient(
        colors = let {
            val (top, bottom) = forWeatherCode(code)
            listOf(top, top.copy(alpha = 0.85f), bottom.copy(alpha = 0.9f), bottom)
        }
    )
}
