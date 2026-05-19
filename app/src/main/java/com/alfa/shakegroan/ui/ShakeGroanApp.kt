package com.alfa.shakegroan.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.AssignTarget
import com.alfa.shakegroan.data.CustomSound
import com.alfa.shakegroan.data.SoundAssignment
import com.alfa.shakegroan.data.SoundSourceType
import com.alfa.shakegroan.ui.theme.DeepNight
import com.alfa.shakegroan.ui.theme.GlassCyan
import com.alfa.shakegroan.ui.theme.GlassPink
import com.alfa.shakegroan.ui.theme.PanelLine
import com.alfa.shakegroan.widget.FallOuchWidgetProvider
import kotlin.math.roundToInt

private enum class AppScreen {
    HOME,
    SOUNDS,
    SETTINGS,
}

private enum class SoundsTab {
    LIBRARY,
    MINE,
    POPULAR,
}

private enum class LibraryFilter {
    ALL,
    CLEAN,
    PROFANE,
}

private data class SoundOptionUi(
    val assignment: SoundAssignment,
    val title: String,
    val subtitle: String,
    val isPopular: Boolean = false,
)

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
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var soundsTab by rememberSaveable { mutableStateOf(SoundsTab.LIBRARY) }
    var libraryFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepNight,
                        Color(0xFF081021),
                        Color(0xFF03060F),
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        CosmicBackdrop()

        when (currentScreen) {
            AppScreen.HOME -> HomeScreen(
                state = uiState,
                hasAccelerometer = hasAccelerometer,
                onArmedChange = viewModel::setArmed,
                onShakeEnabledChange = viewModel::setShakeEnabled,
                onThrowEnabledChange = viewModel::setThrowEnabled,
                onShakeThresholdChange = viewModel::setShakeThreshold,
                onThrowThresholdChange = viewModel::setThrowThreshold,
                onVolumeChange = viewModel::setVolume,
                onOpenSounds = { currentScreen = AppScreen.SOUNDS },
                onPreviewShake = { viewModel.previewAssignedSound(AssignTarget.SHAKE) },
                onPreviewThrow = { viewModel.previewAssignedSound(AssignTarget.THROW) },
            )

            AppScreen.SOUNDS -> SoundsScreen(
                state = uiState,
                currentTab = soundsTab,
                currentFilter = libraryFilter,
                onTabChange = { soundsTab = it },
                onFilterChange = { libraryFilter = it },
                onBackHome = { currentScreen = AppScreen.HOME },
                onAddSounds = onAddSounds,
                onCreateSound = viewModel::announceCreateSoon,
                onAssignSound = viewModel::assignSound,
                onPreviewSound = viewModel::previewSound,
                onClearMine = viewModel::clearCustomSounds,
            )

            AppScreen.SETTINGS -> SettingsScreen(
                state = uiState,
                onFeedback = {
                    shareText(
                        context,
                        "Фидбэк по Fall Ouch!\n\nЧто понравилось:\nЧто хочется улучшить:\nКаких звуков не хватает:"
                    )
                },
                onInviteFriend = {
                    shareText(
                        context,
                        "Попробуй Fall Ouch! Проект и APK: https://github.com/arseniikarlov/SoundDrop"
                    )
                },
                onPinWidget = {
                    requestPinWidget(context)
                }
            )
        }

        BottomDock(
            currentScreen = currentScreen,
            onScreenChange = { currentScreen = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun HomeScreen(
    state: MainUiState,
    hasAccelerometer: Boolean,
    onArmedChange: (Boolean) -> Unit,
    onShakeEnabledChange: (Boolean) -> Unit,
    onThrowEnabledChange: (Boolean) -> Unit,
    onShakeThresholdChange: (Float) -> Unit,
    onThrowThresholdChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSounds: () -> Unit,
    onPreviewShake: () -> Unit,
    onPreviewThrow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "РЕЖИМ FALL OUCH!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasAccelerometer) state.statusMessage else "На этом устройстве нет акселерометра",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.62f)
                )
                Text(
                    text = state.lastTriggerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassCyan
                )
            }
            PowerToggleButton(
                isOn = state.settings.isArmed && hasAccelerometer,
                enabled = hasAccelerometer,
                onClick = { onArmedChange(!(state.settings.isArmed && hasAccelerometer)) }
            )
        }

        GlassCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            overlay = Brush.horizontalGradient(
                colors = listOf(
                    GlassPink.copy(alpha = 0.12f),
                    Color.Transparent,
                    GlassCyan.copy(alpha = 0.12f),
                )
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Какие звуки стоят сейчас",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = onOpenSounds,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassCyan),
                    border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Настроить")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HomeSoundRow(
                label = "Падение",
                value = state.settings.throwSound.displayName,
                accent = GlassPink,
                onPreview = onPreviewThrow
            )
            Spacer(modifier = Modifier.height(10.dp))
            HomeSoundRow(
                label = "Тряска",
                value = state.settings.shakeSound.displayName,
                accent = GlassCyan,
                onPreview = onPreviewShake
            )
        }

        GlassCard(
            modifier = Modifier.weight(0.82f),
            shape = RoundedCornerShape(28.dp),
            overlay = Brush.horizontalGradient(
                colors = listOf(
                    GlassCyan.copy(alpha = 0.1f),
                    Color.Transparent,
                )
            )
        ) {
            Text(
                text = "Какие события ловить",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EventModeButton(
                    modifier = Modifier.weight(1f),
                    title = "Тряска",
                    enabled = state.settings.shakeEnabled,
                    accent = GlassCyan,
                    onClick = { onShakeEnabledChange(!state.settings.shakeEnabled) }
                )
                EventModeButton(
                    modifier = Modifier.weight(1f),
                    title = "Падение",
                    enabled = state.settings.throwEnabled,
                    accent = GlassPink,
                    onClick = { onThrowEnabledChange(!state.settings.throwEnabled) }
                )
            }
        }

        GlassCard(
            modifier = Modifier.weight(1.15f),
            shape = RoundedCornerShape(28.dp),
            overlay = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                )
            )
        ) {
            Text(
                text = "Чувствительность и громкость",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            CompactSliderRow(
                icon = Icons.Rounded.GraphicEq,
                label = "Чувствительность тряски",
                accent = GlassCyan,
                value = shakeNormalized(state.settings),
                onValueChange = { normalized ->
                    onShakeThresholdChange(lerp(22f, 8f, normalized))
                }
            )
            CompactSliderRow(
                icon = Icons.Rounded.PhoneAndroid,
                label = "Чувствительность падения",
                accent = GlassPink,
                value = throwNormalized(state.settings),
                onValueChange = { normalized ->
                    onThrowThresholdChange(lerp(14f, 30f, normalized))
                }
            )
            CompactSliderRow(
                icon = Icons.Rounded.VolumeUp,
                label = "Громкость",
                accent = Color(0xFFFF8FD9),
                value = state.settings.playbackVolume,
                onValueChange = onVolumeChange,
                valueRange = 0.1f..1f,
                valueLabel = "${(state.settings.playbackVolume * 100).roundToInt()}%"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundsScreen(
    state: MainUiState,
    currentTab: SoundsTab,
    currentFilter: LibraryFilter,
    onTabChange: (SoundsTab) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onBackHome: () -> Unit,
    onAddSounds: () -> Unit,
    onCreateSound: () -> Unit,
    onAssignSound: (SoundAssignment, AssignTarget) -> Unit,
    onPreviewSound: (SoundAssignment) -> Unit,
    onClearMine: () -> Unit,
) {
    val libraryOptions = remember {
        BuiltInSoundCatalog.cleanSounds.map {
            SoundOptionUi(
                assignment = BuiltInSoundCatalog.assignmentFor(it),
                title = it.displayName,
                subtitle = "Встроенный не-матный звук",
                isPopular = it.isPopular,
            )
        } + SoundOptionUi(
            assignment = BuiltInSoundCatalog.profaneAssignment(),
            title = BuiltInSoundCatalog.PROFANE_SOUND_NAME,
            subtitle = "Системный TTS с матом",
            isPopular = true,
        )
    }
    val customOptions = state.settings.customSounds.map {
        SoundOptionUi(
            assignment = BuiltInSoundCatalog.assignmentFor(it),
            title = it.displayName,
            subtitle = "Мой загруженный файл"
        )
    }
    val popularOptions = libraryOptions.filter { it.isPopular }

    val filteredLibrary = when (currentFilter) {
        LibraryFilter.ALL -> libraryOptions
        LibraryFilter.CLEAN -> libraryOptions.filter { it.assignment.sourceType == SoundSourceType.BUILT_IN_CLEAN }
        LibraryFilter.PROFANE -> libraryOptions.filter { it.assignment.sourceType == SoundSourceType.BUILT_IN_PROFANE }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ЗВУКИ",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onBackHome,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassCyan),
                border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.48f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Home")
            }
        }

        GlassCard(
            shape = RoundedCornerShape(26.dp),
            overlay = Brush.horizontalGradient(
                colors = listOf(
                    GlassCyan.copy(alpha = 0.08f),
                    Color.Transparent,
                    GlassPink.copy(alpha = 0.08f),
                )
            )
        ) {
            Text(
                text = "Куда что установлено",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            SimpleStatusLine("Тряска", state.settings.shakeSound.displayName, GlassCyan)
            Spacer(modifier = Modifier.height(8.dp))
            SimpleStatusLine("Падение", state.settings.throwSound.displayName, GlassPink)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SoundsTab.entries.forEach { tab ->
                SegmentButton(
                    label = when (tab) {
                        SoundsTab.LIBRARY -> "Библиотека"
                        SoundsTab.MINE -> "Мои"
                        SoundsTab.POPULAR -> "Популярно"
                    },
                    selected = currentTab == tab,
                    accent = GlassCyan,
                    onClick = { onTabChange(tab) }
                )
            }
        }

        when (currentTab) {
            SoundsTab.LIBRARY -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryFilter.entries.forEach { filter ->
                        SegmentButton(
                            label = when (filter) {
                                LibraryFilter.ALL -> "Все"
                                LibraryFilter.CLEAN -> "Не мат"
                                LibraryFilter.PROFANE -> "Мат"
                            },
                            selected = currentFilter == filter,
                            accent = if (filter == LibraryFilter.PROFANE) GlassPink else GlassCyan,
                            onClick = { onFilterChange(filter) }
                        )
                    }
                }
                filteredLibrary.forEach { option ->
                    SoundOptionCard(
                        option = option,
                        settings = state.settings,
                        onPreview = { onPreviewSound(option.assignment) },
                        onAssign = onAssignSound
                    )
                }
            }

            SoundsTab.MINE -> {
                GlassCard(
                    shape = RoundedCornerShape(26.dp),
                    overlay = Brush.horizontalGradient(
                        colors = listOf(
                            GlassPink.copy(alpha = 0.14f),
                            Color.Transparent,
                        )
                    )
                ) {
                    Text(
                        text = "Добавить свои звуки",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAddSounds,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlassPink,
                                contentColor = Color(0xFF120919)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Загрузить")
                        }
                        OutlinedButton(
                            onClick = onCreateSound,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassCyan),
                            border = BorderStroke(1.dp, GlassCyan.copy(alpha = 0.52f))
                        ) {
                            Text("Создать")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Создать: в следующем шаге подключим импорт screen recording, вырезку аудио и обрезку фрагмента.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f)
                    )
                }

                if (customOptions.isEmpty()) {
                    EmptyStateCard("Пока нет ни одного своего звука")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Мои файлы",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Очистить",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onClearMine)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    customOptions.forEach { option ->
                        SoundOptionCard(
                            option = option,
                            settings = state.settings,
                            onPreview = { onPreviewSound(option.assignment) },
                            onAssign = onAssignSound
                        )
                    }
                }
            }

            SoundsTab.POPULAR -> {
                GlassCard(
                    shape = RoundedCornerShape(26.dp),
                    overlay = Brush.horizontalGradient(
                        colors = listOf(
                            GlassCyan.copy(alpha = 0.12f),
                            Color.Transparent,
                        )
                    )
                ) {
                    Text(
                        text = "Общий каталог для всех",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Настоящее user-generated \"Популярно\" лучше делать после бэкенда и модерации. Пока здесь лежат локальные хиты, чтобы не оставлять вкладку пустой.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.62f)
                    )
                }
                popularOptions.forEach { option ->
                    SoundOptionCard(
                        option = option.copy(subtitle = "${option.subtitle} • популярно сейчас"),
                        settings = state.settings,
                        onPreview = { onPreviewSound(option.assignment) },
                        onAssign = onAssignSound
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: MainUiState,
    onFeedback: () -> Unit,
    onInviteFriend: () -> Unit,
    onPinWidget: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "НАСТРОЙКИ",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = state.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.62f)
        )

        SettingActionCard(
            icon = Icons.Rounded.Star,
            title = "Оплата подписки",
            text = "Fall Ouch+ сделаем после платёжного контура. Туда можно вынести облачную библиотеку, популярное и продвинутый монтаж звуков.",
            buttonLabel = "Скоро",
            accent = GlassPink,
            onClick = {}
        )

        SettingActionCard(
            icon = Icons.Rounded.Mail,
            title = "Фидбэк и пожелания",
            text = "Собрал отдельную точку входа для идей. Можно отправить список хотелок одним тапом через share sheet.",
            buttonLabel = "Отправить идею",
            accent = GlassCyan,
            onClick = onFeedback
        )

        SettingActionCard(
            icon = Icons.Rounded.Widgets,
            title = "Виджет",
            text = "На большинстве Android виджет нельзя добавить полностью автоматически, но можно попросить лаунчер закрепить его прямо из приложения.",
            buttonLabel = "Добавить виджет",
            accent = GlassPink,
            onClick = onPinWidget
        )

        SettingActionCard(
            icon = Icons.Rounded.Share,
            title = "Пригласить друга",
            text = "Пока делюсь ссылкой на репозиторий проекта. Когда будет релиз, сюда же ляжет нормальная рефка.",
            buttonLabel = "Поделиться",
            accent = GlassCyan,
            onClick = onInviteFriend
        )

        GlassCard(
            shape = RoundedCornerShape(26.dp),
            overlay = Brush.horizontalGradient(
                colors = listOf(
                    GlassCyan.copy(alpha = 0.1f),
                    Color.Transparent,
                )
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OrbIcon(
                    icon = Icons.Rounded.HelpOutline,
                    tint = GlassCyan,
                    size = 50.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Подробная инструкция",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. Включи режим на Home\n2. Поставь отдельные звуки на падение и тряску\n3. Разреши уведомления\n4. Добавь виджет, если нужен быстрый toggle",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSoundRow(
    label: String,
    value: String,
    accent: Color,
    onPreview: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, PanelLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.55f), CircleShape)
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EventModeButton(
    modifier: Modifier = Modifier,
    title: String,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (enabled) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.56f) else PanelLine)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (enabled) "Ловим событие" else "Сейчас выключено",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) accent else Color.White.copy(alpha = 0.52f)
            )
        }
    }
}

@Composable
private fun CompactSliderRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueLabel: String? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel ?: sliderPercentLabel(value),
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}

@Composable
private fun SoundOptionCard(
    option: SoundOptionUi,
    settings: AppSettings,
    onPreview: () -> Unit,
    onAssign: (SoundAssignment, AssignTarget) -> Unit,
) {
    val assignedToShake = sameAssignment(settings.shakeSound, option.assignment)
    val assignedToThrow = sameAssignment(settings.throwSound, option.assignment)

    GlassCard(
        shape = RoundedCornerShape(24.dp),
        overlay = Brush.horizontalGradient(
            colors = listOf(
                if (option.assignment.sourceType == SoundSourceType.BUILT_IN_PROFANE) {
                    GlassPink.copy(alpha = 0.12f)
                } else {
                    GlassCyan.copy(alpha = 0.1f)
                },
                Color.Transparent,
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbIcon(
                icon = if (option.assignment.sourceType == SoundSourceType.BUILT_IN_PROFANE) {
                    Icons.Rounded.GraphicEq
                } else {
                    Icons.Rounded.MusicNote
                },
                tint = if (option.assignment.sourceType == SoundSourceType.BUILT_IN_PROFANE) GlassPink else GlassCyan,
                size = 54.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (assignedToShake) {
                        TinyBadge("На тряске", GlassCyan)
                    }
                    if (assignedToThrow) {
                        TinyBadge("На падении", GlassPink)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, PanelLine, CircleShape)
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SegmentButton("Тряска", false, GlassCyan) {
                onAssign(option.assignment, AssignTarget.SHAKE)
            }
            SegmentButton("Падение", false, GlassPink) {
                onAssign(option.assignment, AssignTarget.THROW)
            }
            SegmentButton("Оба", false, Color(0xFFA98CFF)) {
                onAssign(option.assignment, AssignTarget.BOTH)
            }
        }
    }
}

@Composable
private fun TinyBadge(
    label: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SimpleStatusLine(
    label: String,
    value: String,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.58f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyStateCard(
    text: String,
) {
    GlassCard(
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun SettingActionCard(
    icon: ImageVector,
    title: String,
    text: String,
    buttonLabel: String,
    accent: Color,
    onClick: () -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(26.dp),
        overlay = Brush.horizontalGradient(
            colors = listOf(accent.copy(alpha = 0.12f), Color.Transparent)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbIcon(icon = icon, tint = accent, size = 56.dp)
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
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
        ) {
            Text(buttonLabel)
        }
    }
}

@Composable
private fun PowerToggleButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (isOn) GlassCyan.copy(alpha = 0.34f) else GlassPink.copy(alpha = 0.2f),
                        Color(0xFF0E1630),
                    )
                )
            )
            .border(
                2.dp,
                if (isOn) GlassCyan.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.2f),
                CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = null,
            tint = if (!enabled) Color.White.copy(alpha = 0.3f) else if (isOn) GlassCyan else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.56f) else PanelLine),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BottomDock(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF09101F).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, PanelLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem(
                icon = Icons.Rounded.Home,
                label = "Home",
                selected = currentScreen == AppScreen.HOME,
                onClick = { onScreenChange(AppScreen.HOME) }
            )
            BottomItem(
                icon = Icons.Rounded.MusicNote,
                label = "Звуки",
                selected = currentScreen == AppScreen.SOUNDS,
                onClick = { onScreenChange(AppScreen.SOUNDS) }
            )
            BottomItem(
                icon = Icons.Rounded.Settings,
                label = "Настройки",
                selected = currentScreen == AppScreen.SETTINGS,
                onClick = { onScreenChange(AppScreen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun BottomItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) GlassCyan else Color.White.copy(alpha = 0.54f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
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
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.015f),
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
                .padding(16.dp),
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
                        tint.copy(alpha = 0.22f),
                        Color(0xFF0C1530),
                    )
                )
            )
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape),
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
private fun CosmicBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        PlanetGlow(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-100).dp, y = 48.dp),
            colors = listOf(GlassCyan.copy(alpha = 0.26f), Color.Transparent)
        )
        PlanetGlow(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = 120.dp),
            colors = listOf(GlassPink.copy(alpha = 0.24f), Color.Transparent)
        )
        PlanetGlow(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 160.dp),
            colors = listOf(Color(0xFF7040FF).copy(alpha = 0.16f), Color.Transparent)
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

private fun sameAssignment(
    left: SoundAssignment,
    right: SoundAssignment,
): Boolean {
    return left.sourceType == right.sourceType && left.reference == right.reference
}

private fun shakeNormalized(settings: AppSettings): Float =
    ((22f - settings.shakeDeltaThreshold) / (22f - 8f)).coerceIn(0f, 1f)

private fun throwNormalized(settings: AppSettings): Float =
    ((settings.throwImpactThreshold - 14f) / (30f - 14f)).coerceIn(0f, 1f)

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

private fun sliderPercentLabel(value: Float): String = "${(value * 100).roundToInt()}%"

private fun shareText(
    context: android.content.Context,
    text: String,
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, "Поделиться")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun requestPinWidget(
    context: android.content.Context,
) {
    val widgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return
    if (!widgetManager.isRequestPinAppWidgetSupported) {
        return
    }
    widgetManager.requestPinAppWidget(
        ComponentName(context, FallOuchWidgetProvider::class.java),
        null,
        null
    )
}
