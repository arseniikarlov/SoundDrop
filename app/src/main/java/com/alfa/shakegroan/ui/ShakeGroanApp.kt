package com.alfa.shakegroan.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.AssignTarget
import com.alfa.shakegroan.data.CustomSound
import com.alfa.shakegroan.data.SoundAssignment
import com.alfa.shakegroan.data.SoundSourceType
import com.alfa.shakegroan.media.DraftSourceKind
import com.alfa.shakegroan.media.TrimSelectionNormalizer
import com.alfa.shakegroan.widget.FallOuchWidgetProvider
import kotlin.math.roundToInt

private val AppBackground = Color(0xFF171717)
private val AppBackgroundSoft = Color(0xFF202224)
private val AppCard = Color(0xFF343438)
private val AppCardSoft = Color(0xFF2E2F33)
private val AppStroke = Color(0xFF3D4046)
private val AppStrokeSoft = Color(0xFF4B4E55)
private val AppAccent = Color(0xFF6CA9FF)
private val AppAccentSoft = Color(0x336CA9FF)
private val AppTextMuted = Color(0xFF96989E)
private val AppTextSoft = Color(0xFFCFCFD4)
private val AppDanger = Color(0xFFE11A17)
private val AppDangerSoft = Color(0x33E11A17)

private enum class AppScreen {
    HOME,
    SETTINGS,
    UPLOAD,
    PROFILE,
    SOUNDS,
    UPLOAD_MENU,
    VIDEO_IMPORT,
    RECORD,
    TRIM,
    PROFILE_GUIDE,
}

private enum class SoundTarget {
    THROW,
    SLAP,
}

private data class SoundOptionUi(
    val assignment: SoundAssignment,
    val title: String,
    val isCustom: Boolean,
)

@Composable
fun ShakeGroanApp(
    viewModel: MainViewModel,
    onPickAudioFiles: () -> Unit,
    onPickVideoFile: () -> Unit,
    onRequestRecording: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasAccelerometer = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
    }

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var soundTarget by rememberSaveable { mutableStateOf(SoundTarget.THROW) }
    var stagedSoundKey by rememberSaveable { mutableStateOf<String?>(null) }
    var studioSourceScreen by rememberSaveable { mutableStateOf(AppScreen.UPLOAD_MENU) }
    var returnToSoundPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.clipDraft?.sourceUri) {
        if (uiState.clipDraft != null) {
            currentScreen = AppScreen.TRIM
        }
    }

    LaunchedEffect(uiState.clipDraft, uiState.isProcessing, returnToSoundPicker) {
        if (returnToSoundPicker && uiState.clipDraft == null && !uiState.isProcessing) {
            currentScreen = AppScreen.SOUNDS
            returnToSoundPicker = false
        }
    }

    fun openTargetPicker(target: SoundTarget) {
        soundTarget = target
        stagedSoundKey = assignmentKey(currentAssignmentFor(target, uiState.settings))
        currentScreen = AppScreen.SOUNDS
    }

    fun leaveSecondaryScreens(next: AppScreen) {
        if (currentScreen == AppScreen.RECORD && uiState.recording.isRecording) {
            viewModel.cancelRecordingSession()
        }
        if (currentScreen == AppScreen.TRIM) {
            viewModel.stopDraftPreview()
        }
        currentScreen = next
    }

    fun modalBackingScreen(screen: AppScreen): AppScreen? = when (screen) {
        AppScreen.SOUNDS -> AppScreen.SETTINGS
        AppScreen.UPLOAD_MENU,
        AppScreen.VIDEO_IMPORT,
        AppScreen.RECORD,
        AppScreen.TRIM -> AppScreen.UPLOAD
        AppScreen.PROFILE_GUIDE -> AppScreen.PROFILE
        else -> null
    }

    @Composable
    fun BoxScope.RenderMainScreen(screen: AppScreen) {
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                state = uiState,
                hasAccelerometer = hasAccelerometer,
                onArmedChange = viewModel::setArmed,
            )

            AppScreen.SETTINGS -> SettingsScreen(
                state = uiState,
                onOpenTarget = ::openTargetPicker,
                onThrowEnabledChange = viewModel::setThrowEnabled,
                onSlapEnabledChange = viewModel::setSlapEnabled,
                onThrowThresholdChange = viewModel::setThrowThreshold,
                onSlapThresholdChange = viewModel::setSlapThreshold,
                onVolumeChange = viewModel::setVolume,
            )

            AppScreen.UPLOAD -> UploadScreen(
                onOpenMenu = { currentScreen = AppScreen.UPLOAD_MENU }
            )

            AppScreen.PROFILE -> ProfileScreen(
                onOpenGuide = { currentScreen = AppScreen.PROFILE_GUIDE },
                onPinWidget = { requestPinWidget(context) },
                onInviteFriend = {
                    shareText(
                        context,
                        "Попробуй Fall Ouch! Проект и APK: https://github.com/arseniikarlov/SoundDrop"
                    )
                },
                onSupport = {
                    shareText(
                        context,
                        "Support по Fall Ouch!\n\nОпиши проблему, идею или вопрос."
                    )
                },
                onFeedback = {
                    shareText(
                        context,
                        "Фидбек по Fall Ouch!\n\nЧто понравилось:\nЧто улучшить:\nКаких звуков не хватает:"
                    )
                },
            )

            else -> Unit
        }

        if (screen in setOf(AppScreen.HOME, AppScreen.SETTINGS, AppScreen.UPLOAD, AppScreen.PROFILE)) {
            BottomDock(
                currentScreen = screen,
                onScreenChange = {
                    leaveSecondaryScreens(it)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }

    @Composable
    fun RenderModalScreen(screen: AppScreen) {
        when (screen) {
            AppScreen.SOUNDS -> SoundPickerScreen(
                state = uiState,
                target = soundTarget,
                selectedKey = stagedSoundKey ?: assignmentKey(currentAssignmentFor(soundTarget, uiState.settings)),
                onSelectedKeyChange = { stagedSoundKey = it },
                onCancel = {
                    viewModel.stopSoundPreview()
                    currentScreen = AppScreen.SETTINGS
                },
                onSave = {
                    resolveAssignmentByKey(
                        key = stagedSoundKey,
                        target = soundTarget,
                        settings = uiState.settings,
                    )?.let { assignment ->
                        viewModel.assignSound(assignment, soundTarget.toAssignTarget())
                    }
                    viewModel.stopSoundPreview()
                    currentScreen = AppScreen.SETTINGS
                },
                onPreview = viewModel::toggleSoundPreview,
                onOpenUpload = {
                    viewModel.stopSoundPreview()
                    returnToSoundPicker = true
                    currentScreen = AppScreen.UPLOAD
                },
                onDeleteCustomSound = viewModel::deleteCustomSound,
                onRenameCustomSound = viewModel::renameCustomSound,
            )

            AppScreen.UPLOAD_MENU -> UploadMenuScreen(
                onBack = { currentScreen = AppScreen.UPLOAD },
                onOpenVideo = {
                    studioSourceScreen = AppScreen.UPLOAD_MENU
                    currentScreen = AppScreen.VIDEO_IMPORT
                },
                onOpenRecord = {
                    studioSourceScreen = AppScreen.UPLOAD_MENU
                    currentScreen = AppScreen.RECORD
                },
                onUploadFiles = onPickAudioFiles,
            )

            AppScreen.VIDEO_IMPORT -> VideoImportScreen(
                state = uiState,
                onBack = { currentScreen = AppScreen.UPLOAD_MENU },
                onPickVideo = onPickVideoFile,
                onOpenTrim = { currentScreen = AppScreen.TRIM },
            )

            AppScreen.RECORD -> RecordScreen(
                state = uiState,
                onBack = {
                    if (uiState.recording.isRecording) {
                        viewModel.cancelRecordingSession()
                    }
                    currentScreen = AppScreen.UPLOAD_MENU
                },
                onRecordAction = { displayName ->
                    if (uiState.recording.isRecording) {
                        viewModel.stopRecordingSession(displayName)
                    } else {
                        onRequestRecording()
                    }
                },
            )

            AppScreen.TRIM -> TrimScreen(
                state = uiState,
                onBack = {
                    viewModel.stopDraftPreview()
                    currentScreen = studioSourceScreen
                },
                onTogglePreview = viewModel::toggleDraftPreview,
                onStopPreview = viewModel::stopDraftPreview,
                onSave = { displayName, startMs, endMs ->
                    returnToSoundPicker = false
                    viewModel.saveDraftToMySounds(displayName, startMs, endMs)
                    currentScreen = AppScreen.UPLOAD
                },
            )

            AppScreen.PROFILE_GUIDE -> ProfileGuideScreen(
                onBack = { currentScreen = AppScreen.PROFILE }
            )

            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding()
    ) {
        SoftBackdrop()

        val backingScreen = modalBackingScreen(currentScreen)
        if (backingScreen == null) {
            RenderMainScreen(currentScreen)
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                RenderMainScreen(backingScreen)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.08f))
            )
            RenderModalScreen(currentScreen)
        }

        if (uiState.isProcessing) {
            BusyOverlay(uiState.statusMessage)
        }
    }
}

@Composable
private fun HomeScreen(
    state: MainUiState,
    hasAccelerometer: Boolean,
    onArmedChange: (Boolean) -> Unit,
) {
    val isOn = state.settings.isArmed && hasAccelerometer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 24.dp)
            .padding(bottom = 92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        BrandLogo(
            isOn = isOn,
            modifier = Modifier.fillMaxWidth(),
            size = 88.sp,
            compact = false,
            alignCenter = true,
        )
        Spacer(modifier = Modifier.weight(1f))
        PowerHeroButton(
            isOn = isOn,
            enabled = hasAccelerometer,
            onClick = { onArmedChange(!isOn) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TinyStatusPill(
            text = when {
                !hasAccelerometer -> "режим недоступен"
                isOn -> "режим включен"
                else -> "режим выключен"
            },
            active = isOn && hasAccelerometer,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsScreen(
    state: MainUiState,
    onOpenTarget: (SoundTarget) -> Unit,
    onThrowEnabledChange: (Boolean) -> Unit,
    onSlapEnabledChange: (Boolean) -> Unit,
    onThrowThresholdChange: (Float) -> Unit,
    onSlapThresholdChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    PrimaryColumn {
        ScreenTitle("Настройки")

        Text("Звуки", style = MaterialTheme.typography.titleLarge, color = AppTextSoft)
        CardBlock {
            SettingSoundRow("Падение", state.settings.throwSound.displayName) {
                onOpenTarget(SoundTarget.THROW)
            }
            CardDivider()
            SettingSoundRow("Шлепок", state.settings.slapSound.displayName) {
                onOpenTarget(SoundTarget.SLAP)
            }
        }

        Text("Режимы", style = MaterialTheme.typography.titleLarge, color = AppTextSoft)
        CardBlock {
            ModeSliderRow(
                title = "Падение",
                enabled = state.settings.throwEnabled,
                progress = SensitivityMapper.throwProgress(state.settings.throwImpactThreshold),
                onToggle = onThrowEnabledChange,
                onProgressChange = { onThrowThresholdChange(SensitivityMapper.throwThreshold(it)) },
                accent = AppAccent,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ModeSliderRow(
                title = "Шлепок",
                enabled = state.settings.slapEnabled,
                progress = SensitivityMapper.slapProgress(state.settings.slapImpactThreshold),
                onToggle = onSlapEnabledChange,
                onProgressChange = { onSlapThresholdChange(SensitivityMapper.slapThreshold(it)) },
                accent = AppAccent,
            )
        }

        Text("Громкость", style = MaterialTheme.typography.titleLarge, color = AppTextSoft)
        CardBlock {
            VolumeSliderRow(
                progress = state.settings.playbackVolume,
                onProgressChange = onVolumeChange,
            )
        }
    }
}

@Composable
private fun UploadScreen(
    onOpenMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 24.dp)
            .padding(bottom = 92.dp)
    ) {
        Column {
            ScreenTitle("Загрузка")
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onOpenMenu,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppCard,
                    contentColor = AppTextSoft
                ),
                modifier = Modifier
                    .widthIn(min = 230.dp)
                    .height(82.dp)
            ) {
                Text(
                    text = "+ добавить звук",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    onOpenGuide: () -> Unit,
    onPinWidget: () -> Unit,
    onInviteFriend: () -> Unit,
    onSupport: () -> Unit,
    onFeedback: () -> Unit,
) {
    PrimaryColumn {
        ScreenTitle("Профиль")
        CardBlock {
            ProfileListRow("Инструкция по установке", onOpenGuide)
            CardDivider()
            ProfileListRow("Установить виджет", onPinWidget)
            CardDivider()
            ProfileListRow("Пригласить друга", onInviteFriend)
            CardDivider()
            ProfileListRow("Поддержка", onSupport)
            CardDivider()
            ProfileListRow("Фидбек/ Оставить отзыв", onFeedback)
        }
    }
}

@Composable
private fun SoundPickerScreen(
    state: MainUiState,
    target: SoundTarget,
    selectedKey: String,
    onSelectedKeyChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onPreview: (SoundAssignment) -> Unit,
    onOpenUpload: () -> Unit,
    onDeleteCustomSound: (String) -> Unit,
    onRenameCustomSound: (String, String) -> Unit,
) {
    val currentAssignment = currentAssignmentFor(target, state.settings)
    val mineOptions = state.settings.customSounds.map {
        SoundOptionUi(
            assignment = BuiltInSoundCatalog.assignmentFor(it),
            title = it.displayName,
            isCustom = true,
        )
    }
    val libraryOptions = BuiltInSoundCatalog.cleanSounds.map {
        SoundOptionUi(
            assignment = BuiltInSoundCatalog.assignmentFor(it),
            title = it.displayName,
            isCustom = false,
        )
    } + SoundOptionUi(
        assignment = BuiltInSoundCatalog.profaneAssignment(),
        title = BuiltInSoundCatalog.PROFANE_SOUND_NAME,
        isCustom = false,
    )

    val resolvedSelection = resolveAssignmentByKey(
        key = selectedKey,
        target = target,
        settings = state.settings,
    ) ?: currentAssignment

    var renameUri by rememberSaveable { mutableStateOf<String?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }

    ModalScreenScaffold {
        BottomSheetPanel(scrollable = true) {
            TopActionBar(
                title = target.label(),
                onCancel = onCancel,
                onSave = onSave,
                saveEnabled = true,
            )
            Spacer(modifier = Modifier.height(18.dp))

            SectionLabel("Мои звуки")
            CardBlock {
                mineOptions.forEachIndexed { index, option ->
                    val optionKey = assignmentKey(option.assignment)
                    val isSelected = optionKey == assignmentKey(resolvedSelection)
                    SwipeableSoundPickRow(
                        title = option.title,
                        selected = isSelected,
                        playing = state.previewingSoundKey == optionKey,
                        progress = if (state.previewingSoundKey == optionKey) state.previewProgress else 0f,
                        onClick = { onSelectedKeyChange(optionKey) },
                        onPreview = { onPreview(option.assignment) },
                        onEdit = {
                            renameUri = option.assignment.reference
                            renameValue = option.title
                        },
                        onDelete = {
                            onDeleteCustomSound(option.assignment.reference)
                            if (renameUri == option.assignment.reference) {
                                renameUri = null
                                renameValue = ""
                            }
                        },
                    )
                    if (index != mineOptions.lastIndex) {
                        CardDivider()
                    }
                }
                if (mineOptions.isNotEmpty()) {
                    CardDivider()
                }
                AddLinkRow(onClick = onOpenUpload)
            }

            if (renameUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                RenameCard(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    onCancel = {
                        renameUri = null
                        renameValue = ""
                    },
                    onSave = {
                        renameUri?.let { uri ->
                            onRenameCustomSound(uri, renameValue)
                        }
                        renameUri = null
                        renameValue = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel("Библиотека")
            CardBlock {
                libraryOptions.forEachIndexed { index, option ->
                    val optionKey = assignmentKey(option.assignment)
                    SoundPickRow(
                        title = option.title,
                        selected = optionKey == assignmentKey(resolvedSelection),
                        playing = state.previewingSoundKey == optionKey,
                        progress = if (state.previewingSoundKey == optionKey) state.previewProgress else 0f,
                        onClick = { onSelectedKeyChange(optionKey) },
                        onPreview = { onPreview(option.assignment) },
                    )
                    if (index != libraryOptions.lastIndex) {
                        CardDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadMenuScreen(
    onBack: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenRecord: () -> Unit,
    onUploadFiles: () -> Unit,
) {
    ModalScreenScaffold {
        BottomSheetPanel(scrollable = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                CircleBackButton(onBack)
            }
            Spacer(modifier = Modifier.height(18.dp))
            MenuActionButton("Извлечь из видео", onOpenVideo)
            Spacer(modifier = Modifier.height(14.dp))
            MenuActionButton("Записать", onOpenRecord)
            Spacer(modifier = Modifier.height(14.dp))
            MenuActionButton("Загрузить", onUploadFiles)
        }
    }
}

@Composable
private fun VideoImportScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenTrim: () -> Unit,
) {
    val draft = state.clipDraft?.takeIf { it.sourceKind == DraftSourceKind.VIDEO }

    ModalScreenScaffold {
        BottomSheetPanel(scrollable = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                CircleBackButton(onBack)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Извлечь из видео",
                style = MaterialTheme.typography.titleLarge,
                color = AppTextSoft,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            MenuActionButton(
                title = if (draft == null) "Выбрать видео" else draft.sourceLabel,
                onClick = if (draft == null) onPickVideo else onOpenTrim
            )
            if (draft != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Длительность ${formatDuration(draft.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted
                )
            }
        }
    }
}

@Composable
private fun RecordScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onRecordAction: (String) -> Unit,
) {
    var recordingName by rememberSaveable { mutableStateOf("") }

    ModalScreenScaffold {
        BottomSheetPanel(scrollable = true) {
            TopActionBar(
                title = "",
                onCancel = onBack,
                onSave = {
                    if (state.recording.isRecording) {
                        onRecordAction(recordingName)
                    }
                },
                saveEnabled = state.recording.isRecording,
                saveLabel = "Сохранить",
            )
            Spacer(modifier = Modifier.height(28.dp))
            OutlinedTextField(
                value = recordingName,
                onValueChange = { recordingName = it },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("название", color = AppTextMuted) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = AppTextMuted
                    )
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            CardBlock {
                WaveformPreview(
                    startFraction = 0f,
                    endFraction = 1f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                PlayPreviewButton(
                    playing = false,
                    onClick = {}
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            CardBlock {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    RecordingButton(
                        isRecording = state.recording.isRecording,
                        onClick = { onRecordAction(recordingName) },
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = if (state.recording.isRecording) {
                        "Запись ${formatDuration(state.recording.elapsedMs)}"
                    } else {
                        "Нажмите для записи"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrimScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onTogglePreview: (Long, Long) -> Unit,
    onStopPreview: () -> Unit,
    onSave: (String, Long, Long) -> Unit,
) {
    val draft = state.clipDraft ?: return
    val durationMs = draft.durationMs.coerceAtLeast(1L)
    var fileName by rememberSaveable(draft.sourceUri) { mutableStateOf(draft.proposedDisplayName) }
    var startFraction by rememberSaveable(draft.sourceUri) { mutableFloatStateOf(0f) }
    var endFraction by rememberSaveable(draft.sourceUri) { mutableFloatStateOf(1f) }

    val selection = remember(startFraction, endFraction, durationMs) {
        TrimSelectionNormalizer.normalize(
            durationMs = durationMs,
            startMs = (startFraction * durationMs).roundToInt().toLong(),
            endMs = (endFraction * durationMs).roundToInt().toLong(),
        )
    }

    ModalScreenScaffold {
        BottomSheetPanel(scrollable = true) {
            TopActionBar(
                title = "",
                onCancel = {
                    onStopPreview()
                    onBack()
                },
                onSave = { onSave(fileName, selection.startMs, selection.endMs) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("название", color = AppTextMuted) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = AppTextMuted
                    )
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            CardBlock {
                WaveformPreview(
                    startFraction = selection.startMs / durationMs.toFloat(),
                    endFraction = selection.endMs / durationMs.toFloat(),
                    samples = state.draftWaveform,
                    playFraction = if (state.isPreviewingDraft) state.draftPreviewProgress else null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.draftWaveformLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Строю волну...",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                PlayPreviewButton(
                    playing = state.isPreviewingDraft,
                    onClick = {
                        if (state.isPreviewingDraft) {
                            onStopPreview()
                        } else {
                            onTogglePreview(selection.startMs, selection.endMs)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            SliderLabel(
                label = "Фрагмент",
                value = "${formatDuration(selection.startMs)} - ${formatDuration(selection.endMs)}"
            )
            RangeSlider(
                value = (selection.startMs / durationMs.toFloat())..(selection.endMs / durationMs.toFloat()),
                onValueChange = { range ->
                    val normalized = TrimSelectionNormalizer.normalize(
                        durationMs = durationMs,
                        startMs = (range.start * durationMs).roundToInt().toLong(),
                        endMs = (range.endInclusive * durationMs).roundToInt().toLong(),
                    )
                    startFraction = normalized.startMs / durationMs.toFloat()
                    endFraction = normalized.endMs / durationMs.toFloat()
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AppAccent,
                    inactiveTrackColor = AppStrokeSoft,
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProfileGuideScreen(
    onBack: () -> Unit,
) {
    ModalScreenScaffold {
        BottomSheetPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                CircleBackButton(onBack)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Инструкция по установке",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTextSoft,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            CardBlock {
                Text(
                    text = "Зайди во вкладку «настройки»\n\nНажми на режим, который хочешь установить и выбери понравившийся звук\n\nРазреши уведомления\n\nВключи режим на Home: звук работает даже при блокировке, пока висит уведомление\n\nЕсли на Samsung/Xiaomi звук не срабатывает с выключенным экраном, отключи оптимизацию батареи для Fall Ouch!\n\nДобавь виджет для быстрого доступа",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextSoft,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun PrimaryColumn(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp)
            .padding(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = AppTextSoft,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun BrandLogo(
    isOn: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.TextUnit,
    compact: Boolean,
    alignCenter: Boolean,
) {
    val ouchColor = if (isOn) AppAccent else Color.White
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = "FALL",
            color = Color(0xFFE8E9EC),
            fontSize = size,
            lineHeight = size * 0.82f,
            letterSpacing = (-3).sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "OUCH!",
            color = ouchColor,
            fontSize = if (compact) size * 0.78f else size,
            lineHeight = size * 0.82f,
            letterSpacing = (-3).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ModalScreenScaffold(
    panelContent: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Column {
                panelContent()
            }
        }
    }
}

@Composable
private fun BottomSheetPanel(
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panelModifier = if (scrollable) {
        Modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
    } else {
        Modifier.fillMaxWidth()
    }
    val contentModifier = if (scrollable) {
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 18.dp)
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = AppBackgroundSoft.copy(alpha = 0.96f),
        modifier = panelModifier
    ) {
        Column(
            modifier = contentModifier,
            content = content
        )
    }
}

@Composable
private fun CardBlock(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppStroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            content = content
        )
    }
}

@Composable
private fun CardDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppStroke)
    )
}

@Composable
private fun SettingSoundRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = AppTextSoft,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = AppTextSoft
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTextSoft
        )
    }
}

@Composable
private fun ModeSliderRow(
    title: String,
    enabled: Boolean,
    progress: Float,
    onToggle: (Boolean) -> Unit,
    onProgressChange: (Float) -> Unit,
    accent: Color = AppAccent,
) {
    val visibleProgress = if (enabled) progress.coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (enabled) title else "$title (выкл)",
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) accent else AppTextMuted,
        )
        Slider(
            value = visibleProgress,
            onValueChange = { value ->
                val nextProgress = value.coerceIn(0f, 1f)
                if (nextProgress <= 0.01f) {
                    if (enabled) {
                        onToggle(false)
                    }
                    onProgressChange(0f)
                } else {
                    if (!enabled) {
                        onToggle(true)
                    }
                    onProgressChange(nextProgress)
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = if (enabled) accent else AppStrokeSoft,
                inactiveTrackColor = AppTextMuted,
            )
        )
    }
}

@Composable
private fun VolumeSliderRow(
    progress: Float,
    onProgressChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
            contentDescription = null,
            tint = Color.White
        )
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = AppAccent,
                inactiveTrackColor = AppTextMuted,
            )
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun TinyStatusPill(
    text: String,
    active: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2C31)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) AppAccent else AppTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PowerHeroButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val ringColor = if (isOn) AppAccent else AppTextSoft
    Box(
        modifier = Modifier
            .size(170.dp)
            .clip(CircleShape)
            .background(Color(0xFF26272B))
            .border(3.dp, ringColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = null,
            tint = ringColor,
            modifier = Modifier.size(86.dp)
        )
    }
}

@Composable
private fun TopActionBar(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    saveLabel: String = "Сохранить",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetPillButton(
            text = "Отмена",
            accent = false,
            onClick = onCancel,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppTextSoft
        )
        SheetPillButton(
            text = saveLabel,
            accent = true,
            enabled = saveEnabled,
            onClick = onSave,
        )
    }
}

@Composable
private fun SheetPillButton(
    text: String,
    accent: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF343736),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> AppTextMuted
                accent -> AppAccent
                else -> AppTextMuted
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = AppTextMuted
    )
}

@Composable
private fun SwipeableSoundPickRow(
    title: String,
    selected: Boolean,
    playing: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val revealWidthPx = with(LocalDensity.current) { 116.dp.toPx() }
    var revealed by rememberSaveable(title) { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val targetOffset = if (revealed) -revealWidthPx else 0f
    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset + dragOffset,
        label = "sound-row-offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeActionButton(
                color = AppAccent,
                icon = Icons.Rounded.Edit,
                onClick = {
                    revealed = false
                    onEdit()
                }
            )
            SwipeActionButton(
                color = AppDanger,
                icon = Icons.Rounded.Delete,
                onClick = {
                    revealed = false
                    onDelete()
                }
            )
        }

        Surface(
            color = AppCard,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(title, revealed, revealWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            revealed = targetOffset + dragOffset < -revealWidthPx * 0.45f
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = if (revealed) {
                                (dragOffset + dragAmount).coerceIn(0f, revealWidthPx)
                            } else {
                                (dragOffset + dragAmount).coerceIn(-revealWidthPx, 24f)
                            }
                        }
                    )
                }
        ) {
            SoundPickRow(
                title = title,
                selected = selected,
                playing = playing,
                progress = progress,
                onClick = {
                    if (revealed) {
                        revealed = false
                    } else {
                        onClick()
                    }
                },
                onPreview = onPreview,
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 56.dp)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun SoundPickRow(
    title: String,
    selected: Boolean,
    playing: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTextSoft,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = AppTextSoft
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            MiniCircleButton(
                background = AppBackgroundSoft,
                border = AppStrokeSoft,
                icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                tint = AppAccent,
                onClick = onPreview
            )
        }
        if (playing) {
            Spacer(modifier = Modifier.height(8.dp))
            PlaybackTimeline(progress = progress)
        }
    }
}

@Composable
private fun PlaybackTimeline(progress: Float) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .padding(horizontal = 2.dp)
    ) {
        val centerY = size.height / 2f
        val startX = 2f
        val endX = size.width - 2f
        val activeEndX = startX + (endX - startX) * safeProgress
        drawLine(
            color = AppStrokeSoft,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AppAccent,
            start = Offset(startX, centerY),
            end = Offset(activeEndX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = AppAccent,
            radius = 5f,
            center = Offset(activeEndX, centerY)
        )
    }
}

@Composable
private fun AddLinkRow(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+ Добавить звук",
            style = MaterialTheme.typography.bodyLarge,
            color = AppAccent,
        )
    }
}

@Composable
private fun RenameCard(
    value: String,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("название", color = AppTextMuted) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppStrokeSoft),
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppCard,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppTextSoft,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)
        )
    }
}

@Composable
private fun RecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color(0xFFEDEEF2))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 72.dp else 76.dp)
                .clip(CircleShape)
                .background(AppDanger),
        )
    }
}

@Composable
private fun WaveformPreview(
    startFraction: Float,
    endFraction: Float,
    samples: List<Float> = emptyList(),
    playFraction: Float? = null,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppBackgroundSoft)
            .border(1.dp, AppStrokeSoft, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        val barCount = 34
        val spacing = size.width / barCount
        val selectionStart = size.width * startFraction.coerceIn(0f, 1f)
        val selectionEnd = size.width * endFraction.coerceIn(0f, 1f)

        drawRoundRect(
            color = AppAccentSoft,
            topLeft = Offset(selectionStart, 0f),
            size = androidx.compose.ui.geometry.Size(
                width = (selectionEnd - selectionStart).coerceAtLeast(0f),
                height = size.height
            ),
            cornerRadius = CornerRadius(16f, 16f)
        )

        repeat(barCount) { index ->
            val x = index * spacing + spacing / 2f
            val sampleIndex = if (samples.isEmpty()) {
                -1
            } else {
                (index * (samples.lastIndex.toFloat() / (barCount - 1).coerceAtLeast(1))).roundToInt()
                    .coerceIn(0, samples.lastIndex)
            }
            val ratio = if (sampleIndex >= 0) {
                samples[sampleIndex].coerceIn(0f, 1f)
            } else {
                (((index * 19) % 100) / 100f).coerceIn(0.18f, 1f)
            }
            val barHeight = size.height * (0.08f + ratio * 0.82f)
            val active = x in selectionStart..selectionEnd
            drawLine(
                color = if (active) AppAccent else AppTextMuted.copy(alpha = 0.7f),
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = spacing * 0.34f,
                cap = StrokeCap.Round
            )
        }

        val handleHeight = size.height - 8f
        val maxHandleX = (size.width - 4f).coerceAtLeast(4f)
        val leftHandleX = selectionStart.coerceIn(4f, maxHandleX)
        val rightHandleX = selectionEnd.coerceIn(4f, maxHandleX)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(leftHandleX - 3f, 4f),
            size = androidx.compose.ui.geometry.Size(width = 6f, height = handleHeight),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(rightHandleX - 3f, 4f),
            size = androidx.compose.ui.geometry.Size(width = 6f, height = handleHeight),
            cornerRadius = CornerRadius(6f, 6f)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            style = Stroke(width = 2f),
            topLeft = Offset(selectionStart, 2f),
            size = androidx.compose.ui.geometry.Size(
                width = (selectionEnd - selectionStart).coerceAtLeast(0f),
                height = size.height - 4f
            ),
            cornerRadius = CornerRadius(16f, 16f)
        )

        if (playFraction != null) {
            val playX = selectionStart +
                (selectionEnd - selectionStart).coerceAtLeast(0f) * playFraction.coerceIn(0f, 1f)
            drawLine(
                color = AppAccent,
                start = Offset(playX, 2f),
                end = Offset(playX, size.height - 2f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PlayPreviewButton(
    playing: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (playing) AppAccent else AppTextMuted,
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun SliderLabel(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = AppAccent)
    }
}

@Composable
private fun ProfileListRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = AppTextSoft,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTextSoft
        )
    }
}

@Composable
private fun CircleBackButton(
    onClick: () -> Unit,
) {
    MiniCircleButton(
        background = Color.Transparent,
        border = AppTextSoft,
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        tint = AppTextSoft,
        onClick = onClick,
        rotate = 180f,
    )
}

@Composable
private fun MiniCircleButton(
    background: Color,
    border: Color,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    rotate: Float = 0f,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.graphicsLayer(rotationZ = rotate)
        )
    }
}

@Composable
private fun BusyOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AppBackgroundSoft,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppStrokeSoft),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = AppAccent, strokeWidth = 3.dp)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextSoft,
                    textAlign = TextAlign.Center
                )
            }
        }
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
        shape = RoundedCornerShape(18.dp),
        color = AppCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppStroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem(Icons.Rounded.Home, "домой", currentScreen == AppScreen.HOME) {
                onScreenChange(AppScreen.HOME)
            }
            BottomItem(Icons.Rounded.Settings, "настройки", currentScreen == AppScreen.SETTINGS) {
                onScreenChange(AppScreen.SETTINGS)
            }
            BottomItem(Icons.Rounded.CloudUpload, "загрузка", currentScreen == AppScreen.UPLOAD) {
                onScreenChange(AppScreen.UPLOAD)
            }
            BottomItem(Icons.Rounded.Person, "профиль", currentScreen == AppScreen.PROFILE) {
                onScreenChange(AppScreen.PROFILE)
            }
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
    val tint = if (selected) Color.White else AppTextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun SoftBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(start = 0.dp, top = 120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppAccent.copy(alpha = 0.05f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}

@Composable
private fun fieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppStrokeSoft,
    unfocusedBorderColor = AppStrokeSoft,
    focusedContainerColor = AppCard,
    unfocusedContainerColor = AppCard,
    cursorColor = AppAccent,
    focusedTextColor = AppTextSoft,
    unfocusedTextColor = AppTextSoft,
    focusedTrailingIconColor = AppTextMuted,
    unfocusedTrailingIconColor = AppTextMuted,
)

private fun SoundTarget.label(): String = when (this) {
    SoundTarget.THROW -> "Падение"
    SoundTarget.SLAP -> "Шлепок"
}

private fun SoundTarget.toAssignTarget(): AssignTarget = when (this) {
    SoundTarget.THROW -> AssignTarget.THROW
    SoundTarget.SLAP -> AssignTarget.SLAP
}

private fun currentAssignmentFor(
    target: SoundTarget,
    settings: AppSettings,
): SoundAssignment = when (target) {
    SoundTarget.THROW -> settings.throwSound
    SoundTarget.SLAP -> settings.slapSound
}

private fun assignmentKey(assignment: SoundAssignment): String {
    return "${assignment.sourceType.name}|${assignment.reference}"
}

private fun resolveAssignmentByKey(
    key: String?,
    target: SoundTarget,
    settings: AppSettings,
): SoundAssignment? {
    val fallback = currentAssignmentFor(target, settings)
    if (key.isNullOrBlank()) {
        return fallback
    }

    val allOptions = settings.customSounds.map(BuiltInSoundCatalog::assignmentFor) +
        BuiltInSoundCatalog.cleanSounds.map(BuiltInSoundCatalog::assignmentFor) +
        BuiltInSoundCatalog.profaneAssignment()

    return allOptions.firstOrNull { assignmentKey(it) == key } ?: fallback
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

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
