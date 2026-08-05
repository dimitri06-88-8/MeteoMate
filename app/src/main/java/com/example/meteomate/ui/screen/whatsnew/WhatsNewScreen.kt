package com.example.meteomate.ui.screen.whatsnew

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meteomate.BuildConfig
import com.example.meteomate.R
import com.example.meteomate.ui.component.LiquidGlassCard
import com.example.meteomate.ui.theme.ClearDayBottom
import com.example.meteomate.ui.theme.ClearDayTop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    automatic: Boolean,
    onClose: () -> Unit
) {
    BackHandler(enabled = automatic, onBack = onClose)
    val features = listOf(
        Triple("🔔", stringResource(R.string.whats_new_notifications_title), stringResource(R.string.whats_new_notifications_description)),
        Triple("💨", stringResource(R.string.whats_new_wind_models_title), stringResource(R.string.whats_new_wind_models_description)),
        Triple("☀️", stringResource(R.string.whats_new_uv_title), stringResource(R.string.whats_new_uv_description)),
        Triple("📷", stringResource(R.string.whats_new_golden_hour_title), stringResource(R.string.whats_new_golden_hour_description)),
        Triple("🏆", stringResource(R.string.whats_new_records_title), stringResource(R.string.whats_new_records_description)),
        Triple("🧲", stringResource(R.string.whats_new_geomagnetic_title), stringResource(R.string.whats_new_geomagnetic_description)),
        Triple("▣", stringResource(R.string.whats_new_widget_title), stringResource(R.string.whats_new_widget_description))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whats_new_title)) },
                navigationIcon = {
                    if (!automatic) {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(ClearDayTop, ClearDayTop.copy(alpha = 0.86f), ClearDayBottom)
            )
        )
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "MeteoMate ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.whats_new_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )

            features.forEach { (emoji, title, description) ->
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(if (automatic) stringResource(R.string.whats_new_continue) else stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
