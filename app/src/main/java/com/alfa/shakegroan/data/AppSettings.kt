package com.alfa.shakegroan.data

import com.alfa.shakegroan.motion.DetectorConfig
import com.alfa.shakegroan.motion.MotionEventType
import java.util.Locale

enum class PlaybackMode {
    BUILT_IN,
    CUSTOM_ONLY,
    MIXED,
}

enum class BuiltInPack {
    CLEAN,
}

enum class SoundSourceType {
    BUILT_IN_CLEAN,
    CUSTOM,
}

enum class AppLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String,
) {
    EN_US("en-US", "English US", "English US"),
    ES("es", "Spanish", "Español"),
    IT("it", "Italian", "Italiano"),
    PT_BR("pt-BR", "Portuguese Brazil", "Português Brasil"),
    DE("de", "German", "Deutsch"),
    FR("fr", "French", "Français"),
    JA("ja", "Japanese", "日本語"),
    KO("ko", "Korean", "한국어"),
    RU("ru", "Russian", "Русский"),
    HI("hi", "Hindi", "हिन्दी"),
    ID("id", "Indonesian", "Bahasa Indonesia");

    companion object {
        fun fromCode(code: String?): AppLanguage? {
            if (code.isNullOrBlank()) {
                return null
            }
            return entries.firstOrNull { language -> language.code.equals(code, ignoreCase = true) }
        }

        fun defaultForDevice(locale: Locale = Locale.getDefault()): AppLanguage {
            val language = locale.language.lowercase(Locale.US)
            val country = locale.country.uppercase(Locale.US)
            return when {
                language == "pt" && country == "BR" -> PT_BR
                language == "en" -> EN_US
                language == "es" -> ES
                language == "it" -> IT
                language == "de" -> DE
                language == "fr" -> FR
                language == "ja" -> JA
                language == "ko" -> KO
                language == "ru" -> RU
                language == "hi" -> HI
                language == "id" || language == "in" -> ID
                else -> EN_US
            }
        }
    }
}

enum class AssignTarget {
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
    val reference: String = "archive4_oof",
    val displayName: String = "Oof!",
)

data class AppSettings(
    val isArmed: Boolean = false,
    val hasSeenIntroGuide: Boolean = false,
    val throwEnabled: Boolean = true,
    val slapEnabled: Boolean = false,
    val throwImpactThreshold: Float = 50.0f,
    val slapImpactThreshold: Float = 24.0f,
    val cooldownMs: Int = 1000,
    val playbackVolume: Float = 0.5f,
    val languageCode: String? = null,
    val throwSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "archive4_tom_scream",
        displayName = "Tom Scream",
    ),
    val slapSound: SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = "archive4_oof",
        displayName = "Oof!",
    ),
    val customSounds: List<CustomSound> = emptyList(),
)

fun AppSettings.effectiveLanguage(): AppLanguage {
    return AppLanguage.fromCode(languageCode) ?: AppLanguage.defaultForDevice()
}

fun AppSettings.toDetectorConfig(): DetectorConfig = DetectorConfig(
    throwEnabled = throwEnabled,
    slapEnabled = slapEnabled,
    throwImpactThreshold = throwImpactThreshold,
    slapImpactThreshold = slapImpactThreshold,
    cooldownMs = cooldownMs,
)

fun AppSettings.soundFor(eventType: MotionEventType): SoundAssignment = when (eventType) {
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
