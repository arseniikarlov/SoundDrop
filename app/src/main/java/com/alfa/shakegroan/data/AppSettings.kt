package com.alfa.shakegroan.data

import com.alfa.shakegroan.motion.DetectorConfig

enum class PlaybackMode {
    BUILT_IN,
    CUSTOM_ONLY,
    MIXED,
}

data class CustomSound(
    val uri: String,
    val displayName: String,
)

data class PickedSound(
    val uri: String,
    val displayName: String,
)

data class AppSettings(
    val isArmed: Boolean = false,
    val shakeEnabled: Boolean = true,
    val throwEnabled: Boolean = true,
    val shakeDeltaThreshold: Float = 13.5f,
    val throwImpactThreshold: Float = 22.0f,
    val cooldownMs: Int = 1400,
    val playbackVolume: Float = 0.9f,
    val playbackMode: PlaybackMode = PlaybackMode.MIXED,
    val customSounds: List<CustomSound> = emptyList(),
)

fun AppSettings.toDetectorConfig(): DetectorConfig = DetectorConfig(
    shakeEnabled = shakeEnabled,
    throwEnabled = throwEnabled,
    shakeDeltaThreshold = shakeDeltaThreshold,
    throwImpactThreshold = throwImpactThreshold,
    cooldownMs = cooldownMs,
)
