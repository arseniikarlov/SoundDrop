package com.alfa.shakegroan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alfa.shakegroan.audio.PlaybackSource
import com.alfa.shakegroan.audio.SoundPlayer
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.AppSettingsRepository
import com.alfa.shakegroan.data.BuiltInPack
import com.alfa.shakegroan.data.CustomSound
import com.alfa.shakegroan.data.PickedSound
import com.alfa.shakegroan.data.PlaybackMode
import com.alfa.shakegroan.service.BackgroundMonitorService
import com.alfa.shakegroan.widget.FallOuchWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val lastTriggerLabel: String = "Пока тишина",
    val statusMessage: String = "Fall Ouch! готов: по умолчанию включен не-матный набор, но режим можно переключить",
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

    fun setPlaybackMode(value: PlaybackMode) = updateSettings { copy(playbackMode = value) }

    fun setBuiltInPack(value: BuiltInPack) = updateSettings { copy(builtInPack = value) }

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
        updateSettings { copy(customSounds = emptyList()) }
        _uiState.update { current ->
            current.copy(statusMessage = "Свои звуки очищены")
        }
    }

    fun testSound() {
        val source = soundPlayer.play(_uiState.value.settings)
        _uiState.update { current ->
            current.copy(
                statusMessage = playbackMessage("Тест", source)
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

    fun onMotionDetectedLocally() {
        val source = soundPlayer.play(_uiState.value.settings)
        _uiState.update { current ->
            current.copy(
                statusMessage = playbackMessage("Локально сработал", source)
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

    private fun playbackMessage(prefix: String, source: PlaybackSource): String = when (source) {
        PlaybackSource.BUILT_IN_CLEAN -> "$prefix встроенный не-матный звук"
        PlaybackSource.BUILT_IN_PROFANE -> "$prefix встроенный матный режим"
        PlaybackSource.CUSTOM -> "$prefix пользовательский звук"
    }
}
