package com.alfa.shakegroan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import com.alfa.shakegroan.motion.MotionEventType
import com.alfa.shakegroan.service.BackgroundMonitorService
import com.alfa.shakegroan.widget.FallOuchWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val lastTriggerLabel: String = "Пока тишина",
    val statusMessage: String = "Fall Ouch! готов: теперь можно ставить разные звуки на падение и тряску",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppSettingsRepository(application)
    private val soundPlayer = SoundPlayer(application) { message ->
        _uiState.update { current -> current.copy(statusMessage = message) }
    }

    private val _uiState = MutableStateFlow(MainUiState(settings = repository.load()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        syncBackgroundService(_uiState.value.settings)
    }

    fun setArmed(value: Boolean) = updateSettings { copy(isArmed = value) }

    fun setShakeEnabled(value: Boolean) = updateSettings { copy(shakeEnabled = value) }

    fun setThrowEnabled(value: Boolean) = updateSettings { copy(throwEnabled = value) }

    fun setShakeThreshold(value: Float) = updateSettings { copy(shakeDeltaThreshold = value) }

    fun setThrowThreshold(value: Float) = updateSettings { copy(throwImpactThreshold = value) }

    fun setCooldownMs(value: Int) = updateSettings { copy(cooldownMs = value) }

    fun setVolume(value: Float) = updateSettings { copy(playbackVolume = value) }

    fun addCustomSounds(newSounds: List<PickedSound>) {
        val uniqueByUri = (_uiState.value.settings.customSounds + newSounds.map {
            CustomSound(uri = it.uri, displayName = it.displayName)
        }).distinctBy { it.uri }

        updateSettings {
            copy(customSounds = uniqueByUri)
        }

        _uiState.update { current ->
            current.copy(statusMessage = "Добавлено звуков: ${newSounds.size}")
        }
    }

    fun clearCustomSounds() {
        updateSettings {
            val fallbackShake = if (shakeSound.sourceType == SoundSourceType.CUSTOM) {
                BuiltInSoundCatalog.defaultShakeAssignment()
            } else {
                shakeSound
            }
            val fallbackThrow = if (throwSound.sourceType == SoundSourceType.CUSTOM) {
                BuiltInSoundCatalog.defaultThrowAssignment()
            } else {
                throwSound
            }
            copy(
                customSounds = emptyList(),
                shakeSound = fallbackShake,
                throwSound = fallbackThrow,
            )
        }
        _uiState.update { current ->
            current.copy(statusMessage = "Мои звуки очищены, назначения с файлами сброшены на встроенные")
        }
    }

    fun assignSound(
        assignment: SoundAssignment,
        target: AssignTarget,
    ) {
        updateSettings {
            when (target) {
                AssignTarget.SHAKE -> copy(shakeSound = assignment)
                AssignTarget.THROW -> copy(throwSound = assignment)
                AssignTarget.BOTH -> copy(
                    shakeSound = assignment,
                    throwSound = assignment,
                )
            }
        }
        _uiState.update { current ->
            current.copy(
                statusMessage = when (target) {
                    AssignTarget.SHAKE -> "Поставил `${assignment.displayName}` на тряску"
                    AssignTarget.THROW -> "Поставил `${assignment.displayName}` на падение"
                    AssignTarget.BOTH -> "Поставил `${assignment.displayName}` и на тряску, и на падение"
                }
            )
        }
    }

    fun previewSound(assignment: SoundAssignment) {
        val source = soundPlayer.preview(assignment, _uiState.value.settings.playbackVolume)
        _uiState.update { current ->
            current.copy(
                statusMessage = playbackMessage("Предпрослушка", source, assignment.displayName)
            )
        }
    }

    fun previewAssignedSound(target: AssignTarget) {
        val settings = _uiState.value.settings
        val assignment = when (target) {
            AssignTarget.SHAKE -> settings.shakeSound
            AssignTarget.THROW -> settings.throwSound
            AssignTarget.BOTH -> settings.shakeSound
        }
        previewSound(assignment)
    }

    fun announceCreateSoon() {
        _uiState.update { current ->
            current.copy(
                statusMessage = "Создание звука из видео и обрезка фрагмента будут следующим шагом"
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
            MotionEventType.SHAKE -> settings.shakeSound
            MotionEventType.THROW -> settings.throwSound
        }
        val source = soundPlayer.play(settings, eventType)
        _uiState.update { current ->
            current.copy(
                statusMessage = playbackMessage("Локально сработал", source, assignment.displayName)
            )
        }
    }

    override fun onCleared() {
        soundPlayer.release()
        super.onCleared()
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
