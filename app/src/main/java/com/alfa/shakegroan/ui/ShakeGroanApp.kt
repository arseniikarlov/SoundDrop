package com.alfa.shakegroan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.PlaybackMode
import com.alfa.shakegroan.data.toDetectorConfig
import com.alfa.shakegroan.motion.MotionSensorMonitor
import com.alfa.shakegroan.ui.theme.DeepNight
import com.alfa.shakegroan.ui.theme.GlassCyan
import com.alfa.shakegroan.ui.theme.GlassPink
import com.alfa.shakegroan.ui.theme.PanelLine

@Composable
fun ShakeGroanApp(
    viewModel: MainViewModel,
    onAddSounds: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestSettings by rememberUpdatedState(uiState.settings)
    val latestCallback by rememberUpdatedState(viewModel::onMotionDetected)
    val monitor = remember(context) { MotionSensorMonitor(context, latestCallback) }

    DisposableEffect(lifecycleOwner, uiState.settings.isArmed, uiState.settings) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (latestSettings.isArmed) {
                        monitor.start(latestSettings.toDetectorConfig())
                    }
                }

                Lifecycle.Event.ON_STOP -> monitor.stop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && latestSettings.isArmed) {
            monitor.start(latestSettings.toDetectorConfig())
        } else {
            monitor.stop()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            monitor.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepNight,
                        Color(0xFF141B39),
                        Color(0xFF090D1F),
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        BackdropGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroCard(
                state = uiState,
                hasAccelerometer = monitor.hasAccelerometer(),
                onArmedChange = viewModel::setArmed,
                onTestSound = viewModel::testSound
            )
            MotionSettingsCard(
                settings = uiState.settings,
                onShakeEnabledChange = viewModel::setShakeEnabled,
                onThrowEnabledChange = viewModel::setThrowEnabled,
                onShakeThresholdChange = viewModel::setShakeThreshold,
                onThrowThresholdChange = viewModel::setThrowThreshold,
                onCooldownChange = viewModel::setCooldownMs
            )
            SoundSettingsCard(
                settings = uiState.settings,
                onAddSounds = onAddSounds,
                onClearSounds = viewModel::clearCustomSounds,
                onModeChange = viewModel::setPlaybackMode,
                onVolumeChange = viewModel::setVolume
            )
            UsageCard()
        }
    }
}

@Composable
private fun BackdropGlow() {
    Box(modifier = Modifier.fillMaxSize()) {
        GlowOrb(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-90).dp, y = (-20).dp),
            colors = listOf(GlassPink.copy(alpha = 0.55f), Color.Transparent)
        )
        GlowOrb(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = 20.dp),
            colors = listOf(GlassCyan.copy(alpha = 0.42f), Color.Transparent)
        )
        GlowOrb(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 90.dp),
            colors = listOf(Color(0xFF8B6BFF).copy(alpha = 0.28f), Color.Transparent)
        )
    }
}

@Composable
private fun GlowOrb(
    modifier: Modifier,
    colors: List<Color>,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = colors))
    )
}

@Composable
private fun HeroCard(
    state: MainUiState,
    hasAccelerometer: Boolean,
    onArmedChange: (Boolean) -> Unit,
    onTestSound: () -> Unit,
) {
    GlassPanel {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "SoundDrop",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Стеклянный пульт для звуков, который реагирует на встряску и лёгкий подброс телефона.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f)
            )
            StatusBadgeRow(
                lastTrigger = state.lastTriggerLabel,
                isArmed = state.settings.isArmed,
                customSoundCount = state.settings.customSounds.size
            )
            if (!hasAccelerometer) {
                InfoStrip(
                    text = "На устройстве не найден акселерометр. Кнопка теста всё равно позволит проверить звук.",
                    accent = GlassPink
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassToggle(
                    title = if (state.settings.isArmed) "Режим включён" else "Режим выключен",
                    subtitle = state.statusMessage,
                    checked = state.settings.isArmed,
                    onCheckedChange = onArmedChange,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onTestSound,
                    modifier = Modifier.height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Тест")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBadgeRow(
    lastTrigger: String,
    isArmed: Boolean,
    customSoundCount: Int,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatusBadge(
            title = if (isArmed) "LIVE" else "PAUSED",
            value = if (isArmed) "Слушает движения" else "Мониторинг остановлен"
        )
        StatusBadge(
            title = "FILES",
            value = "$customSoundCount пользовательских звуков"
        )
        StatusBadge(
            title = "LAST",
            value = lastTrigger.removePrefix("Последнее событие: ")
        )
    }
}

@Composable
private fun StatusBadge(
    title: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.11f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, PanelLine)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = GlassCyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MotionSettingsCard(
    settings: AppSettings,
    onShakeEnabledChange: (Boolean) -> Unit,
    onThrowEnabledChange: (Boolean) -> Unit,
    onShakeThresholdChange: (Float) -> Unit,
    onThrowThresholdChange: (Float) -> Unit,
    onCooldownChange: (Int) -> Unit,
) {
    GlassPanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(
                eyebrow = "MOTION LAB",
                title = "Настрой чувствительность движений",
                body = "Можно включить реакцию только на встряску, только на подброс или обе сразу."
            )
            ToggleRow(
                title = "Встряска",
                subtitle = "Три быстрых резких пика подряд",
                checked = settings.shakeEnabled,
                onCheckedChange = onShakeEnabledChange
            )
            SettingSlider(
                title = "Порог встряски",
                valueLabel = "%.1f".format(settings.shakeDeltaThreshold),
                value = settings.shakeDeltaThreshold,
                range = 8f..22f,
                steps = 13,
                onValueChange = onShakeThresholdChange
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            ToggleRow(
                title = "Подброс",
                subtitle = "Короткая невесомость и удар в момент поимки",
                checked = settings.throwEnabled,
                onCheckedChange = onThrowEnabledChange
            )
            SettingSlider(
                title = "Порог удара",
                valueLabel = "%.1f m/s²".format(settings.throwImpactThreshold),
                value = settings.throwImpactThreshold,
                range = 14f..30f,
                steps = 15,
                onValueChange = onThrowThresholdChange
            )
            SettingSlider(
                title = "Пауза между срабатываниями",
                valueLabel = "${settings.cooldownMs} мс",
                value = settings.cooldownMs.toFloat(),
                range = 500f..4000f,
                steps = 13,
                onValueChange = { onCooldownChange(it.toInt()) }
            )
            InfoStrip(
                text = "Для безопасности лучше использовать обычную встряску. Высоко бросать телефон не нужно.",
                accent = GlassCyan
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundSettingsCard(
    settings: AppSettings,
    onAddSounds: () -> Unit,
    onClearSounds: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    GlassPanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(
                eyebrow = "AUDIO VAULT",
                title = "Управляй тем, что именно будет звучать",
                body = "Можно оставить встроенный TTS-стон, использовать только свои файлы или чередовать оба варианта."
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlaybackMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.playbackMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.label()) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = GlassPink.copy(alpha = 0.32f),
                            selectedLabelColor = Color.White
                        ),
                    )
                }
            }
            SettingSlider(
                title = "Громкость",
                valueLabel = "${(settings.playbackVolume * 100).toInt()}%",
                value = settings.playbackVolume,
                range = 0.1f..1f,
                steps = 8,
                onValueChange = onVolumeChange
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddSounds,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassCyan.copy(alpha = 0.26f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Загрузить звуки")
                }
                TextButton(
                    onClick = onClearSounds,
                    enabled = settings.customSounds.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.88f),
                        disabledContentColor = Color.White.copy(alpha = 0.35f)
                    )
                ) {
                    Text("Очистить список")
                }
            }
            if (settings.customSounds.isEmpty()) {
                InfoStrip(
                    text = "Сейчас приложение использует только встроенные звуки TTS.",
                    accent = GlassPink
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    settings.customSounds.forEach { sound ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PanelLine)
                        ) {
                            Text(
                                text = sound.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageCard() {
    GlassPanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(
                eyebrow = "FLOW",
                title = "Как пользоваться",
                body = "Всё нужное прямо в одном экране, без скрытых меню."
            )
            UsageStep(
                index = "01",
                title = "Включи режим",
                body = "Активируй переключатель в верхней карточке, чтобы приложение начало слушать акселерометр."
            )
            UsageStep(
                index = "02",
                title = "Проверь звук",
                body = "Нажми «Тест», чтобы убедиться, что встроенный TTS или твои файлы звучат как надо."
            )
            UsageStep(
                index = "03",
                title = "Подстрой чувствительность",
                body = "Если срабатывает слишком часто, увеличь порог. Если редко, уменьшай его постепенно."
            )
            UsageStep(
                index = "04",
                title = "Добавь свои аудио",
                body = "Кнопка «Загрузить звуки» открывает системный picker и позволяет выбрать несколько файлов сразу."
            )
        }
    }
}

@Composable
private fun UsageStep(
    index: String,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.width(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.11f),
            border = BorderStroke(1.dp, PanelLine)
        ) {
            Text(
                text = index,
                modifier = Modifier.padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = GlassCyan,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, PanelLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.015f),
                        )
                    )
                )
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(
    eyebrow: String,
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = GlassCyan,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun GlassToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, PanelLine, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlassCyan.copy(alpha = 0.7f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlassPink.copy(alpha = 0.72f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassCyan,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = GlassCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun InfoStrip(
    text: String,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

private fun PlaybackMode.label(): String = when (this) {
    PlaybackMode.BUILT_IN -> "Встроенный"
    PlaybackMode.CUSTOM_ONLY -> "Только свои"
    PlaybackMode.MIXED -> "Смешанный"
}
