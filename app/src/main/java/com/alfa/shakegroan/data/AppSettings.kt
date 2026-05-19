package com.alfa.shakegroan.data

import com.alfa.shakegroan.motion.DetectorConfig
import com.alfa.shakegroan.motion.MotionEventType

enum class PlaybackMode {
    BUILT_IN,
    CUSTOM_ONLY,
    MIXED,
}

enum class BuiltInPack {
    CLEAN,
    PROFANE,
}

enum class SoundSourceType {
    BUILT_IN_CLEAN,
    BUILT_IN_PROFANE,
    CUSTOM,
}

enum class AssignTarget {
    SHAKE,
    THROW,
    BOTH,
}

data class CustomSound(
    val uri: String,
    val displayName: String,
)

data class PickedSound(
    val uri: String,
    val displayName: String,
)

data class SoundAssignment(
    val sourceType: SoundSourceType = SoundSourceType.BUILT_IN_CLEAN,
    val reference: String = "clean_doh1",
    val displayName: String = "doh1.mp3",
)

data class AppSettings(
    val isArmed: Boolean = false,
    val shakeEnabled: Boolean = true,
    val throwEnabled: Boolean = true,
    val shakeDeltaThreshold: Float = 13.5f,
    val throwImpactThreshold: Float = 22.0f,
    val cooldownMs: Int = 1400,
    val playbackVolume: Float = 0.9f,
    val shakeSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "clean_doh1",
        displayName = "doh1.mp3",
    ),
    val throwSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "clean_gta_wasted_5",
        displayName = "5-gta-wasted.mp3",
    ),
    val customSounds: List<CustomSound> = emptyList(),
)

fun AppSettings.toDetectorConfig(): DetectorConfig = DetectorConfig(
    shakeEnabled = shakeEnabled,
    throwEnabled = throwEnabled,
    shakeDeltaThreshold = shakeDeltaThreshold,
    throwImpactThreshold = throwImpactThreshold,
    cooldownMs = cooldownMs,
)

fun AppSettings.soundFor(eventType: MotionEventType): SoundAssignment = when (eventType) {
    MotionEventType.SHAKE -> shakeSound
    MotionEventType.THROW -> throwSound
}
