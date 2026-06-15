package com.alfa.shakegroan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import com.alfa.shakegroan.audio.PlaybackSource
import com.alfa.shakegroan.audio.SoundPlayer
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.AppSettingsRepository
import com.alfa.shakegroan.data.AssignTarget
import com.alfa.shakegroan.data.CustomSound
import com.alfa.shakegroan.data.PickedSound
import com.alfa.shakegroan.data.SoundAssignment
import com.alfa.shakegroan.data.SoundSourceType
import com.alfa.shakegroan.data.displayNameWithoutAudioExtension
import com.alfa.shakegroan.media.AudioStudioManager
import com.alfa.shakegroan.media.EditableClipDraft
import com.alfa.shakegroan.motion.MotionEventType
import com.alfa.shakegroan.service.BackgroundMonitorService
import com.alfa.shakegroan.widget.FallOuchWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecordingUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
)

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val lastTriggerLabel: String = "Пока тишина",
    val statusMessage: String = "Fall Ouch! готов: можно ставить отдельные звуки на падение и шлепок",
    val clipDraft: EditableClipDraft? = null,
    val draftWaveform: List<Float> = emptyList(),
    val draftWaveformLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isPreviewingDraft: Boolean = false,
    val draftPreviewProgress: Float = 0f,
    val previewingSoundKey: String? = null,
    val previewProgress: Float = 0f,
    val recording: RecordingUiState = RecordingUiState(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppSettingsRepository(application)
    private val soundPlayer = SoundPlayer(application) { message ->
        _uiState.update { current -> current.copy(statusMessage = message) }
    }
    private val audioStudio = AudioStudioManager(application) { message ->
        _uiState.update { current -> current.copy(statusMessage = message) }
    }

    private val _uiState = MutableStateFlow(MainUiState(settings = repository.load()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var recordingTickerJob: Job? = null

    init {
        syncBackgroundService(_uiState.value.settings)
    }

    fun setArmed(value: Boolean) = updateSettings { copy(isArmed = value) }

    fun markIntroGuideSeen() = updateSettings { copy(hasSeenIntroGuide = true) }

    fun setThrowEnabled(value: Boolean) = updateSettings { copy(throwEnabled = value) }

    fun setSlapEnabled(value: Boolean) = updateSettings { copy(slapEnabled = value) }

    fun setThrowThreshold(value: Float) = updateSettings { copy(throwImpactThreshold = value) }

    fun setSlapThreshold(value: Float) = updateSettings { copy(slapImpactThreshold = value) }

    fun setCooldownMs(value: Int) = updateSettings { copy(cooldownMs = value) }

    fun setVolume(value: Float) = updateSettings { copy(playbackVolume = value) }

    fun addCustomSounds(newSounds: List<PickedSound>) {
        addCustomSoundEntries(
            newSounds.map { sound ->
                CustomSound(
                    uri = sound.uri,
                    displayName = displayNameWithoutAudioExtension(sound.displayName),
                )
            },
            successMessage = "Добавлено звуков: ${newSounds.size}",
        )
    }

    fun clearCustomSounds() {
        updateSettings {
            val fallbackThrow = if (throwSound.sourceType == SoundSourceType.CUSTOM) {
                BuiltInSoundCatalog.defaultThrowAssignment()
            } else {
                throwSound
            }
            val fallbackSlap = if (slapSound.sourceType == SoundSourceType.CUSTOM) {
                BuiltInSoundCatalog.defaultSlapAssignment()
            } else {
                slapSound
            }
            copy(
                customSounds = emptyList(),
                throwSound = fallbackThrow,
                slapSound = fallbackSlap,
            )
        }
        _uiState.update { current ->
            current.copy(statusMessage = "Мои звуки очищены, назначения с файлами сброшены на встроенные")
        }
    }

    fun renameCustomSound(
        uri: String,
        displayName: String,
    ) {
        val normalizedName = displayNameWithoutAudioExtension(displayName)
        if (normalizedName.isBlank()) {
            _uiState.update { current ->
                current.copy(statusMessage = "Название не должно быть пустым")
            }
            return
        }

        updateSettings {
            val updatedSounds = customSounds.map { sound ->
                if (sound.uri == uri) {
                    sound.copy(displayName = normalizedName)
                } else {
                    sound
                }
            }
            val updateAssignment: (SoundAssignment) -> SoundAssignment = { assignment ->
                if (assignment.sourceType == SoundSourceType.CUSTOM && assignment.reference == uri) {
                    assignment.copy(displayName = normalizedName)
                } else {
                    assignment
                }
            }
            copy(
                customSounds = updatedSounds,
                throwSound = updateAssignment(throwSound),
                slapSound = updateAssignment(slapSound),
            )
        }

        _uiState.update { current ->
            current.copy(statusMessage = "Переименовал звук в `${normalizedName}`")
        }
    }

    fun deleteCustomSound(uri: String) {
        updateSettings {
            val fallbackThrow = if (throwSound.sourceType == SoundSourceType.CUSTOM && throwSound.reference == uri) {
                BuiltInSoundCatalog.defaultThrowAssignment()
            } else {
                throwSound
            }
            val fallbackSlap = if (slapSound.sourceType == SoundSourceType.CUSTOM && slapSound.reference == uri) {
                BuiltInSoundCatalog.defaultSlapAssignment()
            } else {
                slapSound
            }
            copy(
                customSounds = customSounds.filterNot { it.uri == uri },
                throwSound = fallbackThrow,
                slapSound = fallbackSlap,
            )
        }

        _uiState.update { current ->
            current.copy(statusMessage = "Звук удалён из Моих звуков")
        }
    }

    fun assignSound(
        assignment: SoundAssignment,
        target: AssignTarget,
    ) {
        updateSettings {
            when (target) {
                AssignTarget.THROW -> copy(throwSound = assignment)
                AssignTarget.SLAP -> copy(slapSound = assignment)
                AssignTarget.ALL -> copy(
                    throwSound = assignment,
                    slapSound = assignment,
                )
            }
        }
        _uiState.update { current ->
            current.copy(
                statusMessage = when (target) {
                    AssignTarget.THROW -> "Поставил `${assignment.displayName}` на падение"
                    AssignTarget.SLAP -> "Поставил `${assignment.displayName}` на шлепок"
                    AssignTarget.ALL -> "Поставил `${assignment.displayName}` на оба события"
                }
            )
        }
    }

    fun toggleSoundPreview(assignment: SoundAssignment) {
        val key = soundPreviewKey(assignment)
        if (_uiState.value.previewingSoundKey == key) {
            stopSoundPreview()
            return
        }

        val source = soundPlayer.preview(
            assignment = assignment,
            volume = _uiState.value.settings.playbackVolume,
            onFinished = {
                _uiState.update { current ->
                    if (current.previewingSoundKey == key) {
                        current.copy(
                            previewingSoundKey = null,
                            previewProgress = 0f,
                        )
                    } else {
                        current
                    }
                }
            },
            onProgress = { progress ->
                _uiState.update { current ->
                    if (current.previewingSoundKey == key) {
                        current.copy(previewProgress = progress)
                    } else {
                        current
                    }
                }
            },
        )
        _uiState.update { current ->
            current.copy(
                previewingSoundKey = key,
                previewProgress = 0f,
                statusMessage = playbackMessage("Предпрослушка", source, assignment.displayName)
            )
        }
    }

    fun stopSoundPreview() {
        soundPlayer.stopActivePlayback()
        _uiState.update { current ->
            current.copy(
                previewingSoundKey = null,
                previewProgress = 0f,
            )
        }
    }

    fun previewAssignedSound(target: AssignTarget) {
        val settings = _uiState.value.settings
        val assignment = when (target) {
            AssignTarget.THROW -> settings.throwSound
            AssignTarget.SLAP -> settings.slapSound
            AssignTarget.ALL -> settings.throwSound
        }
        toggleSoundPreview(assignment)
    }

    fun prepareVideoDraft(
        uriString: String,
        displayName: String,
    ) {
        viewModelScope.launch {
            setProcessing(true, "Проверяю видео и подготавливаю звук")
            val result = withContext(Dispatchers.IO) {
                runCatching { audioStudio.createDraftFromVideo(uriString, displayName) }
            }
            result.onSuccess { draft ->
                replaceDraft(draft)
                _uiState.update { current ->
                    current.copy(
                        isProcessing = false,
                        statusMessage = "Видео готово. Выбери нужный кусок и сохрани его в Мои звуки."
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isProcessing = false,
                        statusMessage = error.message ?: "Не получилось открыть видео"
                    )
                }
            }
        }
    }

    fun startRecordingSession() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { audioStudio.startRecording() }
            }
            result.onSuccess {
                startRecordingTicker()
                _uiState.update { current ->
                    current.copy(
                        recording = RecordingUiState(isRecording = true, elapsedMs = 0L),
                        statusMessage = "Идёт запись. Скажи фразу или включи нужный звук."
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(statusMessage = error.message ?: "Не удалось начать запись")
                }
            }
        }
    }

    fun stopRecordingSession(displayName: String? = null) {
        if (!_uiState.value.recording.isRecording) {
            return
        }

        viewModelScope.launch {
            setProcessing(true, "Сохраняю запись и открываю обрезку")
            val result = withContext(Dispatchers.IO) {
                runCatching { audioStudio.stopRecording() }
            }
            stopRecordingTicker()
            result.onSuccess { draft ->
                val namedDraft = displayName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { draft.copy(proposedDisplayName = displayNameWithoutAudioExtension(it)) }
                    ?: draft
                replaceDraft(namedDraft)
                _uiState.update { current ->
                    current.copy(
                        isProcessing = false,
                        recording = RecordingUiState(),
                        statusMessage = "Запись готова. Теперь вырежи нужный момент."
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isProcessing = false,
                        recording = RecordingUiState(),
                        statusMessage = error.message ?: "Не удалось завершить запись"
                    )
                }
            }
        }
    }

    fun cancelRecordingSession() {
        stopRecordingTicker()
        viewModelScope.launch(Dispatchers.IO) {
            audioStudio.cancelRecording()
        }
        _uiState.update { current ->
            current.copy(
                recording = RecordingUiState(),
                statusMessage = "Запись отменена"
            )
        }
    }

    fun onRecordPermissionDenied() {
        _uiState.update { current ->
            current.copy(statusMessage = "Нужен доступ к микрофону, иначе экран записи не запустится")
        }
    }

    fun toggleDraftPreview(
        startMs: Long,
        endMs: Long,
    ) {
        val draft = _uiState.value.clipDraft ?: return
        if (_uiState.value.isPreviewingDraft) {
            stopDraftPreview()
            return
        }

        runCatching {
            audioStudio.playPreview(
                draft = draft,
                startMs = startMs,
                endMs = endMs,
                volume = _uiState.value.settings.playbackVolume,
                onProgress = { progress ->
                    _uiState.update { current ->
                        if (current.isPreviewingDraft) {
                            current.copy(draftPreviewProgress = progress)
                        } else {
                            current
                        }
                    }
                },
                onFinished = {
                    _uiState.update { current ->
                        current.copy(
                            isPreviewingDraft = false,
                            draftPreviewProgress = 0f,
                        )
                    }
                },
            )
        }.onSuccess {
            _uiState.update { current ->
                current.copy(
                    isPreviewingDraft = true,
                    draftPreviewProgress = 0f,
                    statusMessage = "Слушаю выбранный фрагмент"
                )
            }
        }.onFailure { error ->
            _uiState.update { current ->
                current.copy(
                    isPreviewingDraft = false,
                    draftPreviewProgress = 0f,
                    statusMessage = error.message ?: "Не удалось проиграть фрагмент"
                )
            }
        }
    }

    fun stopDraftPreview() {
        audioStudio.stopPreview()
        _uiState.update { current ->
            current.copy(
                isPreviewingDraft = false,
                draftPreviewProgress = 0f,
            )
        }
    }

    fun saveDraftToMySounds(
        displayName: String,
        startMs: Long,
        endMs: Long,
    ) {
        val draft = _uiState.value.clipDraft ?: return
        viewModelScope.launch {
            setProcessing(true, "Сохраняю новый звук в Мои звуки")
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    audioStudio.saveTrimmedClip(
                        draft = draft,
                        requestedDisplayName = displayName,
                        startMs = startMs,
                        endMs = endMs,
                    )
                }
            }
            result.onSuccess { customSound ->
                val currentDraft = _uiState.value.clipDraft
                addCustomSoundEntries(
                    listOf(customSound),
                    successMessage = "Добавил `${customSound.displayName}` в Мои звуки",
                )
                audioStudio.discardDraft(currentDraft)
                _uiState.update { current ->
                    current.copy(
                        clipDraft = null,
                        draftWaveform = emptyList(),
                        draftWaveformLoading = false,
                        isProcessing = false,
                        isPreviewingDraft = false,
                        draftPreviewProgress = 0f,
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isProcessing = false,
                        statusMessage = error.message ?: "Не удалось сохранить звук"
                    )
                }
            }
        }
    }

    fun discardClipDraft() {
        val draft = _uiState.value.clipDraft
        if (draft == null) {
            return
        }
        audioStudio.discardDraft(draft)
        _uiState.update { current ->
            current.copy(
                clipDraft = null,
                draftWaveform = emptyList(),
                draftWaveformLoading = false,
                isPreviewingDraft = false,
                draftPreviewProgress = 0f,
                statusMessage = "Черновик звука убран"
            )
        }
    }

    fun onServiceRuntimeUpdate(
        lastTriggerLabel: String?,
        statusMessage: String?,
        isArmed: Boolean?,
    ) {
        _uiState.update { current ->
            current.copy(
                settings = if (isArmed == null) {
                    current.settings
                } else {
                    current.settings.copy(isArmed = isArmed)
                },
                lastTriggerLabel = lastTriggerLabel ?: current.lastTriggerLabel,
                statusMessage = statusMessage ?: current.statusMessage,
            )
        }
    }

    fun onMotionDetectedLocally(eventType: MotionEventType) {
        val settings = _uiState.value.settings
        val assignment = when (eventType) {
            MotionEventType.THROW -> settings.throwSound
            MotionEventType.SLAP -> settings.slapSound
        }
        val source = soundPlayer.play(settings, eventType)
        _uiState.update { current ->
            current.copy(
                statusMessage = playbackMessage("Локально сработал", source, assignment.displayName)
            )
        }
    }

    override fun onCleared() {
        stopRecordingTicker()
        soundPlayer.release()
        audioStudio.release()
        super.onCleared()
    }

    private fun addCustomSoundEntries(
        sounds: List<CustomSound>,
        successMessage: String,
    ) {
        val uniqueByUri = (_uiState.value.settings.customSounds + sounds).distinctBy { it.uri }
        updateSettings {
            copy(customSounds = uniqueByUri)
        }
        _uiState.update { current ->
            current.copy(statusMessage = successMessage)
        }
    }

    private fun replaceDraft(draft: EditableClipDraft) {
        val previousDraft = _uiState.value.clipDraft
        if (previousDraft != null && previousDraft.sourceUri != draft.sourceUri) {
            audioStudio.discardDraft(previousDraft)
        }
        _uiState.update { current ->
            current.copy(
                clipDraft = draft,
                draftWaveform = emptyList(),
                draftWaveformLoading = true,
                isPreviewingDraft = false,
                draftPreviewProgress = 0f,
            )
        }
        loadDraftWaveform(draft)
    }

    private fun loadDraftWaveform(draft: EditableClipDraft) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { audioStudio.readWaveform(draft) }
            }
            _uiState.update { current ->
                if (current.clipDraft?.sourceUri != draft.sourceUri) {
                    current
                } else {
                    current.copy(
                        draftWaveform = result.getOrDefault(emptyList()),
                        draftWaveformLoading = false,
                        statusMessage = result.exceptionOrNull()?.message ?: current.statusMessage,
                    )
                }
            }
        }
    }

    private fun setProcessing(
        value: Boolean,
        message: String? = null,
    ) {
        _uiState.update { current ->
            current.copy(
                isProcessing = value,
                statusMessage = message ?: current.statusMessage,
            )
        }
    }

    private fun startRecordingTicker() {
        stopRecordingTicker()
        recordingTickerJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            while (true) {
                _uiState.update { current ->
                    current.copy(
                        recording = current.recording.copy(
                            isRecording = true,
                            elapsedMs = System.currentTimeMillis() - startedAt,
                        )
                    )
                }
                delay(100L)
            }
        }
    }

    private fun stopRecordingTicker() {
        recordingTickerJob?.cancel()
        recordingTickerJob = null
    }

    private fun updateSettings(transform: AppSettings.() -> AppSettings) {
        _uiState.update { current ->
            val updatedSettings = current.settings.transform()
            repository.save(updatedSettings)
            syncBackgroundService(updatedSettings)
            FallOuchWidgetUpdater.refreshAll(getApplication())
            current.copy(settings = updatedSettings)
        }
    }

    private fun syncBackgroundService(settings: AppSettings) {
        if (settings.isArmed) {
            BackgroundMonitorService.startOrUpdate(getApplication())
        } else {
            BackgroundMonitorService.stop(getApplication())
        }
    }

    private fun playbackMessage(
        prefix: String,
        source: PlaybackSource,
        displayName: String,
    ): String = when (source) {
        PlaybackSource.BUILT_IN_CLEAN -> "$prefix: встроенный файл `$displayName`"
        PlaybackSource.BUILT_IN_PROFANE -> "$prefix: матный режим `$displayName`"
        PlaybackSource.CUSTOM -> "$prefix: пользовательский звук `$displayName`"
    }
}

private fun soundPreviewKey(assignment: SoundAssignment): String {
    return "${assignment.sourceType.name}|${assignment.reference}"
}
