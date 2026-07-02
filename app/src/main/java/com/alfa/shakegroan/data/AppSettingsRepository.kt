package com.alfa.shakegroan.data

import android.content.Context
import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import org.json.JSONArray
import org.json.JSONObject

class AppSettingsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val settingsVersion = preferences.getInt(KEY_SETTINGS_VERSION, 1)
        val storedThrowThreshold = preferences.getFloat(KEY_THROW_THRESHOLD, DEFAULT_THROW_THRESHOLD)
        val storedSlapThreshold = preferences.getFloat(KEY_SLAP_THRESHOLD, DEFAULT_SLAP_THRESHOLD)
        val storedCooldownMs = preferences.getInt(KEY_COOLDOWN_MS, 1000)
        val throwImpactThreshold = when {
            settingsVersion < CURRENT_SETTINGS_VERSION &&
                (storedThrowThreshold == 22.0f || storedThrowThreshold == 19.0f || storedThrowThreshold == 47.5f || storedThrowThreshold == 67.0f || storedThrowThreshold == 95.0f) -> DEFAULT_THROW_THRESHOLD
            else -> storedThrowThreshold
        }
        val slapImpactThreshold = when {
            settingsVersion < CURRENT_SETTINGS_VERSION && (storedSlapThreshold == 13.0f || storedSlapThreshold == 18.0f) -> DEFAULT_SLAP_THRESHOLD
            else -> storedSlapThreshold
        }
        val cooldownMs = if (settingsVersion < CURRENT_SETTINGS_VERSION && storedCooldownMs == 1400) {
            1000
        } else {
            storedCooldownMs
        }
        val playbackVolume = if (settingsVersion < CURRENT_SETTINGS_VERSION && preferences.getFloat(KEY_VOLUME, DEFAULT_VOLUME) == 0.9f) {
            DEFAULT_VOLUME
        } else {
            preferences.getFloat(KEY_VOLUME, DEFAULT_VOLUME)
        }
        val snapshot = StoredSettingsSnapshot(
            isArmed = preferences.getBoolean(KEY_ARMED, false),
            hasSeenIntroGuide = preferences.getBoolean(KEY_SEEN_INTRO_GUIDE, false),
            throwEnabled = preferences.getBoolean(KEY_THROW_ENABLED, true),
            slapEnabled = preferences.getBoolean(KEY_SLAP_ENABLED, DEFAULT_SLAP_ENABLED),
            throwImpactThreshold = throwImpactThreshold,
            slapImpactThreshold = slapImpactThreshold,
            cooldownMs = cooldownMs,
            playbackVolume = playbackVolume,
            languageCode = preferences.getString(KEY_LANGUAGE, null),
            throwSoundRaw = preferences.getString(KEY_THROW_SOUND, null),
            slapSoundRaw = preferences.getString(KEY_SLAP_SOUND, null),
            customSoundsRaw = preferences.getString(KEY_CUSTOM_SOUNDS, null),
            legacyPlaybackModeRaw = preferences.getString(KEY_PLAYBACK_MODE, PlaybackMode.BUILT_IN.name),
            legacyBuiltInPackRaw = preferences.getString(KEY_BUILT_IN_PACK, BuiltInPack.CLEAN.name),
        )
        val settings = AppSettingsStorageMapper.fromSnapshot(snapshot)
        if (settingsVersion < CURRENT_SETTINGS_VERSION) {
            save(settings)
        }
        return settings
    }

    fun save(settings: AppSettings) {
        preferences.edit()
            .putInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION)
            .putBoolean(KEY_ARMED, settings.isArmed)
            .putBoolean(KEY_SEEN_INTRO_GUIDE, settings.hasSeenIntroGuide)
            .putBoolean(KEY_THROW_ENABLED, settings.throwEnabled)
            .putBoolean(KEY_SLAP_ENABLED, settings.slapEnabled)
            .putFloat(KEY_THROW_THRESHOLD, settings.throwImpactThreshold)
            .putFloat(KEY_SLAP_THRESHOLD, settings.slapImpactThreshold)
            .putInt(KEY_COOLDOWN_MS, settings.cooldownMs)
            .putFloat(KEY_VOLUME, settings.playbackVolume)
            .putString(KEY_LANGUAGE, settings.languageCode)
            .putString(KEY_THROW_SOUND, AppSettingsStorageMapper.encodeAssignment(settings.throwSound))
            .putString(KEY_SLAP_SOUND, AppSettingsStorageMapper.encodeAssignment(settings.slapSound))
            .putString(KEY_CUSTOM_SOUNDS, AppSettingsStorageMapper.encodeCustomSounds(settings.customSounds))
            .apply()
    }

    private companion object {
        const val CURRENT_SETTINGS_VERSION = 6
        const val DEFAULT_THROW_THRESHOLD = 50.0f
        const val DEFAULT_SLAP_THRESHOLD = 24.0f
        const val DEFAULT_VOLUME = 0.5f
        const val DEFAULT_SLAP_ENABLED = false
        const val PREFS_NAME = "shake_groan_settings"
        const val KEY_SETTINGS_VERSION = "settings_version"
        const val KEY_ARMED = "armed"
        const val KEY_SEEN_INTRO_GUIDE = "seen_intro_guide"
        const val KEY_THROW_ENABLED = "throw_enabled"
        const val KEY_SLAP_ENABLED = "slap_enabled"
        const val KEY_THROW_THRESHOLD = "throw_threshold"
        const val KEY_SLAP_THRESHOLD = "slap_threshold"
        const val KEY_COOLDOWN_MS = "cooldown_ms"
        const val KEY_VOLUME = "volume"
        const val KEY_LANGUAGE = "language"
        const val KEY_THROW_SOUND = "throw_sound"
        const val KEY_SLAP_SOUND = "slap_sound"
        const val KEY_PLAYBACK_MODE = "playback_mode"
        const val KEY_BUILT_IN_PACK = "built_in_pack"
        const val KEY_CUSTOM_SOUNDS = "custom_sounds"
    }
}

internal data class StoredSettingsSnapshot(
    val isArmed: Boolean = false,
    val hasSeenIntroGuide: Boolean = false,
    val throwEnabled: Boolean = true,
    val slapEnabled: Boolean = false,
    val throwImpactThreshold: Float = 50.0f,
    val slapImpactThreshold: Float = 24.0f,
    val cooldownMs: Int = 1000,
    val playbackVolume: Float = 0.5f,
    val languageCode: String? = null,
    val throwSoundRaw: String? = null,
    val slapSoundRaw: String? = null,
    val customSoundsRaw: String? = null,
    val legacyPlaybackModeRaw: String? = PlaybackMode.BUILT_IN.name,
    val legacyBuiltInPackRaw: String? = BuiltInPack.CLEAN.name,
)

internal object AppSettingsStorageMapper {

    fun fromSnapshot(snapshot: StoredSettingsSnapshot): AppSettings {
        val customSounds = decodeCustomSounds(snapshot.customSoundsRaw)
        val legacyPlaybackMode = snapshot.legacyPlaybackModeRaw
            ?.let { raw -> PlaybackMode.entries.firstOrNull { it.name == raw } }
            ?: PlaybackMode.BUILT_IN
        val defaultThrowSound = legacyDefaultAssignment(
            event = DefaultEvent.THROW,
            customSounds = customSounds,
            playbackMode = legacyPlaybackMode,
        )
        val defaultSlapSound = legacyDefaultAssignment(
            event = DefaultEvent.SLAP,
            customSounds = customSounds,
            playbackMode = legacyPlaybackMode,
        )

        val throwSound = decodeAssignment(snapshot.throwSoundRaw, defaultThrowSound).migrateLegacyDefaultThrow(defaultThrowSound)

        return AppSettings(
            isArmed = snapshot.isArmed,
            hasSeenIntroGuide = snapshot.hasSeenIntroGuide,
            throwEnabled = snapshot.throwEnabled,
            slapEnabled = snapshot.slapEnabled,
            throwImpactThreshold = snapshot.throwImpactThreshold,
            slapImpactThreshold = snapshot.slapImpactThreshold,
            cooldownMs = snapshot.cooldownMs,
            playbackVolume = snapshot.playbackVolume,
            languageCode = AppLanguage.fromCode(snapshot.languageCode)?.code,
            throwSound = throwSound,
            slapSound = decodeAssignment(snapshot.slapSoundRaw, defaultSlapSound),
            customSounds = customSounds,
        )
    }

    fun encodeAssignment(assignment: SoundAssignment): String {
        return JSONObject()
            .put("sourceType", assignment.sourceType.name)
            .put("reference", assignment.reference)
            .put("displayName", displayNameWithoutAudioExtension(assignment.displayName))
            .toString()
    }

    fun decodeAssignment(rawValue: String?, defaultValue: SoundAssignment): SoundAssignment {
        if (rawValue.isNullOrBlank()) {
            return defaultValue
        }

        return runCatching {
            val jsonObject = JSONObject(rawValue)
            val sourceType = jsonObject.optString("sourceType")
                .let { raw -> SoundSourceType.entries.firstOrNull { it.name == raw } }
                ?: defaultValue.sourceType
            val reference = jsonObject.optString("reference", defaultValue.reference)
            if (sourceType == SoundSourceType.BUILT_IN_CLEAN && BuiltInSoundCatalog.cleanSoundById(reference) == null) {
                return@runCatching defaultValue
            }
            val displayName = displayNameWithoutAudioExtension(
                jsonObject.optString("displayName", defaultValue.displayName)
            )
            SoundAssignment(
                sourceType = sourceType,
                reference = reference,
                displayName = displayName,
            )
        }.getOrDefault(defaultValue)
    }

    fun encodeCustomSounds(sounds: List<CustomSound>): String {
        val jsonArray = JSONArray()
        sounds.forEach { sound ->
            jsonArray.put(
                JSONObject()
                    .put("uri", sound.uri)
                    .put("displayName", displayNameWithoutAudioExtension(sound.displayName))
            )
        }
        return jsonArray.toString()
    }

    fun decodeCustomSounds(rawValue: String?): List<CustomSound> {
        if (rawValue.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            buildList {
                val jsonArray = JSONArray(rawValue)
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    val uri = item.optString("uri")
                    val displayName = displayNameWithoutAudioExtension(item.optString("displayName", "audio"))
                    if (uri.isNotBlank()) {
                        add(CustomSound(uri = uri, displayName = displayName))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun legacyDefaultAssignment(
        event: DefaultEvent,
        customSounds: List<CustomSound>,
        playbackMode: PlaybackMode,
    ): SoundAssignment {
        if (playbackMode == PlaybackMode.CUSTOM_ONLY && customSounds.isNotEmpty()) {
            return BuiltInSoundCatalog.assignmentFor(customSounds.first())
        }

        if (event == DefaultEvent.THROW && playbackMode == PlaybackMode.MIXED && customSounds.isNotEmpty()) {
            return BuiltInSoundCatalog.assignmentFor(customSounds.first())
        }

        return when (event) {
            DefaultEvent.THROW -> BuiltInSoundCatalog.defaultThrowAssignment()
            DefaultEvent.SLAP -> BuiltInSoundCatalog.defaultSlapAssignment()
        }
    }

    private fun SoundAssignment.migrateLegacyDefaultThrow(defaultThrowSound: SoundAssignment): SoundAssignment {
        return if (sourceType == SoundSourceType.BUILT_IN_CLEAN && reference in legacyThrowDefaultIds) {
            defaultThrowSound
        } else {
            this
        }
    }

    private val legacyThrowDefaultIds = setOf(
        "archive_aaaaaa",
        "archive4_aaaaaa",
        "archive_tom_scream2",
        "clean_tom_scream",
    )

    private enum class DefaultEvent {
        THROW,
        SLAP,
    }
}
