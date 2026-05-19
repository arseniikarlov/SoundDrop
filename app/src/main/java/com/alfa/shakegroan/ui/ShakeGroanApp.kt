package com.alfa.shakegroan.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.BuiltInPack
import com.alfa.shakegroan.data.PlaybackMode
import com.alfa.shakegroan.ui.theme.DeepNight
import com.alfa.shakegroan.ui.theme.GlassCyan
import com.alfa.shakegroan.ui.theme.GlassPink
import com.alfa.shakegroan.ui.theme.PanelLine
import kotlin.math.roundToInt

@Composable
fun ShakeGroanApp(
    viewModel: MainViewModel,
    onAddSounds: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasAccelerometer = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepNight,
                        Color(0xFF0A1025),
                        Color(0xFF040711),
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        CosmicBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FauxStatusBar()
            HeroHeader()
            ServiceStatusCard(
                state = uiState,
                hasAccelerometer = hasAccelerometer,
                onArmedChange = viewModel::setArmed
            )
            SectionTitle("Настройки движения")
            MotionSettingsRow(
                settings = uiState.settings,
                onShakeThresholdChange = viewModel::setShakeThreshold,
                onThrowThresholdChange = viewModel::setThrowThreshold
            )
            SectionTitle("Детекция падения")
            DetectionCard(
                settings = uiState.settings,
                onShakeEnabledChange = viewModel::setShakeEnabled,
                onThrowEnabledChange = viewModel::setThrowEnabled
            )
            SectionTitle("Настройки звука")
            VolumeCard(
                settings = uiState.settings,
                onVolumeChange = viewModel::setVolume
            )
            SelectedSoundCard(
                settings = uiState.settings,
                onModeChange = viewModel::setPlaybackMode,
                onBuiltInPackChange = viewModel::setBuiltInPack,
                onTestSound = viewModel::testSound
            )
            MySoundsSection(
                settings = uiState.settings,
                onAddSounds = onAddSounds,
                onClearSounds = viewModel::clearCustomSounds
            )
        }

        BottomDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun FauxStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "12:30",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.BatteryFull,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "100%",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun HeroHeader() {
    val title = remember {
        buildAnnotatedString {
            pushStyle(
                SpanStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF62E7FF),
                            Color(0xFF7DC7FF),
                            Color(0xFFB892FF),
                            Color(0xFFFF74C8),
                        )
                    )
                )
            )
            append("Fall Ouch!")
            pop()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Я ору, когда ты роняешь 😱",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.62f)
            )
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, PanelLine, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ServiceStatusCard(
    state: MainUiState,
    hasAccelerometer: Boolean,
    onArmedChange: (Boolean) -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(30.dp),
        overlay = Brush.horizontalGradient(
            colors = listOf(
                GlassCyan.copy(alpha = 0.08f),
                Color.Transparent,
                GlassPink.copy(alpha = 0.08f),
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbIcon(
                icon = Icons.Rounded.Shield,
                tint = GlassCyan,
                size = 76.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (state.settings.isArmed) "Сервис активен" else "Сервис выключен",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (!hasAccelerometer) {
                        "На этом устройстве нет акселерометра"
                    } else if (state.settings.isArmed) {
                        "Я на страже 24/7"
                    } else {
                        "Фоновый мониторинг пока спит"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Text(
                    text = if (!hasAccelerometer) {
                        "Без датчика движения приложение не сможет услышать падение"
                    } else if (state.settings.isArmed) {
                        "Готов орать в любой момент"
                    } else {
                        "Включи тумблер и сверни приложение"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f)
                )
            }
            Switch(
                checked = state.settings.isArmed && hasAccelerometer,
                onCheckedChange = onArmedChange,
                enabled = hasAccelerometer,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF18B8FF),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                    uncheckedBorderColor = Color.Transparent,
                    checkedBorderColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, PanelLine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Последний ор",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f)
                )
                Text(
                    text = state.lastTriggerLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassCyan,
                    textAlign = TextAlign.End,
                    modifier = Modifier.wrapContentWidth(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun MotionSettingsRow(
    settings: AppSettings,
    onShakeThresholdChange: (Float) -> Unit,
    onThrowThresholdChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MotionMetricCard(
            modifier = Modifier.weight(1f),
            accent = GlassCyan,
            icon = Icons.Rounded.GraphicEq,
            title = "Чувствительность",
            subtitle = sensitivityLabel(shakeNormalized(settings)),
            value = shakeNormalized(settings),
            onValueChange = { normalized ->
                onShakeThresholdChange(lerp(22f, 8f, normalized))
            }
        )
        MotionMetricCard(
            modifier = Modifier.weight(1f),
            accent = GlassPink,
            icon = Icons.Rounded.Tune,
            title = "Фильтр движений",
            subtitle = filterLabel(throwNormalized(settings)),
            value = throwNormalized(settings),
            onValueChange = { normalized ->
                onThrowThresholdChange(lerp(14f, 30f, normalized))
            }
        )
    }
}

@Composable
private fun MotionMetricCard(
    modifier: Modifier,
    accent: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        overlay = Brush.verticalGradient(
            colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OrbIcon(
                    icon = icon,
                    tint = accent,
                    size = 52.dp
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectionCard(
    settings: AppSettings,
    onShakeEnabledChange: (Boolean) -> Unit,
    onThrowEnabledChange: (Boolean) -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(28.dp),
        overlay = Brush.horizontalGradient(
            colors = listOf(
                GlassPink.copy(alpha = 0.12f),
                Color.Transparent,
                GlassCyan.copy(alpha = 0.08f),
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbIcon(
                icon = Icons.Rounded.PhoneAndroid,
                tint = GlassPink,
                size = 66.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Реагировать на",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detectionModeLabel(settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = GlassPink
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TogglePill(
                        label = "Тряска",
                        checked = settings.shakeEnabled,
                        accent = GlassCyan,
                        onClick = { onShakeEnabledChange(!settings.shakeEnabled) }
                    )
                    TogglePill(
                        label = "Падение",
                        checked = settings.throwEnabled,
                        accent = GlassPink,
                        onClick = { onThrowEnabledChange(!settings.throwEnabled) }
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun VolumeCard(
    settings: AppSettings,
    onVolumeChange: (Float) -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(28.dp),
        overlay = Brush.horizontalGradient(
            colors = listOf(
                GlassPink.copy(alpha = 0.14f),
                Color.Transparent,
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbIcon(
                icon = Icons.Rounded.VolumeUp,
                tint = GlassPink,
                size = 64.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Громкость крика",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(settings.playbackVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GlassPink
                )
                Slider(
                    value = settings.playbackVolume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = GlassPink,
                        activeTrackColor = GlassPink,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedSoundCard(
    settings: AppSettings,
    onModeChange: (PlaybackMode) -> Unit,
    onBuiltInPackChange: (BuiltInPack) -> Unit,
    onTestSound: () -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(28.dp),
        overlay = Brush.horizontalGradient(
            colors = listOf(
                GlassCyan.copy(alpha = 0.12f),
                Color.Transparent,
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbIcon(
                icon = Icons.Rounded.MusicNote,
                tint = GlassCyan,
                size = 64.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Выбранный звук",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = selectedSoundPreview(settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = GlassCyan
                )
                Text(
                    text = playbackModeDescription(settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f)
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(GlassCyan.copy(alpha = 0.12f))
                    .border(2.dp, GlassCyan.copy(alpha = 0.7f), CircleShape)
                    .clickable(onClick = onTestSound),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = GlassCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaybackMode.entries.forEach { mode ->
                TogglePill(
                    label = playbackModeShortLabel(mode),
                    checked = settings.playbackMode == mode,
                    accent = if (mode == PlaybackMode.BUILT_IN) GlassCyan else GlassPink,
                    onClick = { onModeChange(mode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Встроенный набор",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.62f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BuiltInPack.entries.forEach { pack ->
                TogglePill(
                    label = builtInPackShortLabel(pack),
                    checked = settings.builtInPack == pack,
                    accent = if (pack == BuiltInPack.CLEAN) GlassCyan else GlassPink,
                    onClick = { onBuiltInPackChange(pack) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MySoundsSection(
    settings: AppSettings,
    onAddSounds: () -> Unit,
    onClearSounds: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои звуки",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            if (settings.customSounds.isNotEmpty()) {
                Text(
                    text = "Очистить",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onClearSounds)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        GlassCard(
            shape = RoundedCornerShape(28.dp),
            overlay = Brush.horizontalGradient(
                colors = listOf(
                    GlassPink.copy(alpha = 0.16f),
                    Color.Transparent,
                    GlassCyan.copy(alpha = 0.08f),
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x22FF5DB2))
                    .border(1.dp, GlassPink.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    .clickable(onClick = onAddSounds)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        tint = GlassPink,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Загрузить свой звук",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "MP3, WAV, M4A до 10 МБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.58f)
                        )
                    }
                }
            }

            if (settings.customSounds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    settings.customSounds.take(6).forEach { sound ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, PanelLine)
                        ) {
                            Text(
                                text = sound.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.84f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomDock(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0B1123).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, PanelLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem(
                icon = Icons.Rounded.Home,
                label = "Главная",
                selected = true
            )
            BottomItem(
                icon = Icons.Rounded.MusicNote,
                label = "Звуки"
            )
            BottomItem(
                icon = Icons.Rounded.History,
                label = "История"
            )
            BottomItem(
                icon = Icons.Rounded.Person,
                label = "Профиль"
            )
        }
    }
}

@Composable
private fun BottomItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
) {
    val tint = if (selected) GlassCyan else Color.White.copy(alpha = 0.54f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tint
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    overlay: Brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.02f),
        )
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, PanelLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(overlay)
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun OrbIcon(
    icon: ImageVector,
    tint: Color,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.2f),
                        Color(0xFF0D1630).copy(alpha = 0.96f),
                    )
                )
            )
            .border(1.dp, tint.copy(alpha = 0.36f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

@Composable
private fun TogglePill(
    label: String,
    checked: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (checked) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = 0.56f) else PanelLine),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (checked) accent else Color.White.copy(alpha = 0.3f))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CosmicBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        PlanetGlow(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-110).dp, y = 26.dp),
            colors = listOf(Color(0xFF23D8FF).copy(alpha = 0.36f), Color.Transparent)
        )
        PlanetGlow(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = 82.dp),
            colors = listOf(Color(0xFFFF57BA).copy(alpha = 0.34f), Color.Transparent)
        )
        PlanetGlow(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 150.dp),
            colors = listOf(Color(0xFF7D43FF).copy(alpha = 0.14f), Color.Transparent)
        )
    }
}

@Composable
private fun PlanetGlow(
    modifier: Modifier,
    colors: List<Color>,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = colors))
    )
}

private fun sensitivityLabel(value: Float): String = when {
    value < 0.34f -> "Низкая"
    value < 0.68f -> "Средняя"
    else -> "Высокая"
}

private fun filterLabel(value: Float): String = when {
    value < 0.34f -> "Мягкий"
    value < 0.68f -> "Умеренный"
    else -> "Жесткий"
}

private fun detectionModeLabel(settings: AppSettings): String = when {
    settings.throwEnabled && settings.shakeEnabled -> "Падение и сильную тряску"
    settings.throwEnabled -> "Только падение"
    settings.shakeEnabled -> "Только встряску"
    else -> "Сейчас триггеры выключены"
}

private fun selectedSoundPreview(settings: AppSettings): String = when (settings.playbackMode) {
    PlaybackMode.CUSTOM_ONLY -> settings.customSounds.firstOrNull()?.displayName ?: "Свой файл не выбран"
    PlaybackMode.MIXED -> "Свои файлы + ${BuiltInSoundCatalog.labelFor(settings.builtInPack).lowercase()}"
    PlaybackMode.BUILT_IN -> BuiltInSoundCatalog.previewFor(settings.builtInPack)
}

private fun playbackModeDescription(settings: AppSettings): String = when (settings.playbackMode) {
    PlaybackMode.BUILT_IN -> BuiltInSoundCatalog.detailFor(settings.builtInPack)
    PlaybackMode.CUSTOM_ONLY -> "Только загруженные пользователем файлы"
    PlaybackMode.MIXED -> "Смешанный режим: свои файлы + ${BuiltInSoundCatalog.labelFor(settings.builtInPack).lowercase()}"
}

private fun playbackModeShortLabel(mode: PlaybackMode): String = when (mode) {
    PlaybackMode.BUILT_IN -> "Встроенный"
    PlaybackMode.CUSTOM_ONLY -> "Только свои"
    PlaybackMode.MIXED -> "Смешанный"
}

private fun builtInPackShortLabel(pack: BuiltInPack): String = when (pack) {
    BuiltInPack.CLEAN -> "Не мат"
    BuiltInPack.PROFANE -> "Мат"
}

private fun shakeNormalized(settings: AppSettings): Float =
    ((22f - settings.shakeDeltaThreshold) / (22f - 8f)).coerceIn(0f, 1f)

private fun throwNormalized(settings: AppSettings): Float =
    ((settings.throwImpactThreshold - 14f) / (30f - 14f)).coerceIn(0f, 1f)

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)
