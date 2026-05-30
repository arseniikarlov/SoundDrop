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
    SLAP,
    ALL,
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
    val displayName: String = "doh1",
)

data class AppSettings(
    val isArmed: Boolean = false,
    val shakeEnabled: Boolean = true,
    val throwEnabled: Boolean = true,
    val slapEnabled: Boolean = true,
    val shakeDeltaThreshold: Float = 13.5f,
    val throwImpactThreshold: Float = 22.0f,
    val slapImpactThreshold: Float = 18.0f,
    val cooldownMs: Int = 1400,
    val playbackVolume: Float = 0.9f,
    val shakeSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "clean_doh1",
        displayName = "doh1",
    ),
    val throwSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "clean_tom_scream",
        displayName = "tom_scream",
    ),
    val slapSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "clean_untitled2",
        displayName = "untitled2",
    ),
    val customSounds: List<CustomSound> = emptyList(),
)

fun AppSettings.toDetectorConfig(): DetectorConfig = DetectorConfig(
    shakeEnabled = shakeEnabled,
    throwEnabled = throwEnabled,
    slapEnabled = slapEnabled,
    shakeDeltaThreshold = shakeDeltaThreshold,
    throwImpactThreshold = throwImpactThreshold,
    slapImpactThreshold = slapImpactThreshold,
    cooldownMs = cooldownMs,
)

fun AppSettings.soundFor(eventType: MotionEventType): SoundAssignment = when (eventType) {
    MotionEventType.SHAKE -> shakeSound
    MotionEventType.THROW -> throwSound
    MotionEventType.SLAP -> slapSound
}

fun displayNameWithoutAudioExtension(rawName: String): String {
    val trimmed = rawName.trim()
    if (trimmed.isBlank()) {
        return "audio"
    }
    val knownExtensions = listOf(
        ".mp3",
        ".wav",
        ".m4a",
        ".aac",
        ".ogg",
        ".flac",
        ".opus",
        ".amr",
        ".mp4",
        ".mov",
        ".webm",
    )
    return knownExtensions
        .firstOrNull { extension -> trimmed.endsWith(extension, ignoreCase = true) }
        ?.let { extension -> trimmed.dropLast(extension.length).trim() }
        ?.takeIf { it.isNotBlank() }
        ?: trimmed
}
