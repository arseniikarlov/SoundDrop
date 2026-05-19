package com.alfa.shakegroan.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppSettingsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val customSounds = decodeCustomSounds(preferences.getString(KEY_CUSTOM_SOUNDS, null))
        val legacyPlaybackMode = preferences.getString(KEY_PLAYBACK_MODE, PlaybackMode.BUILT_IN.name)
            ?.let { raw -> PlaybackMode.entries.firstOrNull { it.name == raw } }
            ?: PlaybackMode.BUILT_IN
        val legacyBuiltInPack = preferences.getString(KEY_BUILT_IN_PACK, BuiltInPack.CLEAN.name)
            ?.let { raw -> BuiltInPack.entries.firstOrNull { it.name == raw } }
            ?: BuiltInPack.CLEAN

        val defaultShakeSound = legacyDefaultAssignment(
            isShake = true,
            customSounds = customSounds,
            playbackMode = legacyPlaybackMode,
            builtInPack = legacyBuiltInPack,
        )
        val defaultThrowSound = legacyDefaultAssignment(
            isShake = false,
            customSounds = customSounds,
            playbackMode = legacyPlaybackMode,
            builtInPack = legacyBuiltInPack,
        )

        return AppSettings(
            isArmed = preferences.getBoolean(KEY_ARMED, false),
            shakeEnabled = preferences.getBoolean(KEY_SHAKE_ENABLED, true),
            throwEnabled = preferences.getBoolean(KEY_THROW_ENABLED, true),
            shakeDeltaThreshold = preferences.getFloat(KEY_SHAKE_THRESHOLD, 13.5f),
            throwImpactThreshold = preferences.getFloat(KEY_THROW_THRESHOLD, 22.0f),
            cooldownMs = preferences.getInt(KEY_COOLDOWN_MS, 1400),
            playbackVolume = preferences.getFloat(KEY_VOLUME, 0.9f),
            shakeSound = decodeAssignment(
                preferences.getString(KEY_SHAKE_SOUND, null),
                defaultShakeSound,
            ),
            throwSound = decodeAssignment(
                preferences.getString(KEY_THROW_SOUND, null),
                defaultThrowSound,
            ),
            customSounds = customSounds,
        )
    }

    fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_ARMED, settings.isArmed)
            .putBoolean(KEY_SHAKE_ENABLED, settings.shakeEnabled)
            .putBoolean(KEY_THROW_ENABLED, settings.throwEnabled)
            .putFloat(KEY_SHAKE_THRESHOLD, settings.shakeDeltaThreshold)
            .putFloat(KEY_THROW_THRESHOLD, settings.throwImpactThreshold)
            .putInt(KEY_COOLDOWN_MS, settings.cooldownMs)
            .putFloat(KEY_VOLUME, settings.playbackVolume)
            .putString(KEY_SHAKE_SOUND, encodeAssignment(settings.shakeSound))
            .putString(KEY_THROW_SOUND, encodeAssignment(settings.throwSound))
            .putString(KEY_CUSTOM_SOUNDS, encodeCustomSounds(settings.customSounds))
            .apply()
    }

    private fun encodeAssignment(assignment: SoundAssignment): String {
        return JSONObject()
            .put("sourceType", assignment.sourceType.name)
            .put("reference", assignment.reference)
            .put("displayName", assignment.displayName)
            .toString()
    }

    private fun decodeAssignment(rawValue: String?, defaultValue: SoundAssignment): SoundAssignment {
        if (rawValue.isNullOrBlank()) {
            return defaultValue
        }

        return runCatching {
            val jsonObject = JSONObject(rawValue)
            val sourceType = jsonObject.optString("sourceType")
                .let { raw -> SoundSourceType.entries.firstOrNull { it.name == raw } }
                ?: defaultValue.sourceType
            val reference = jsonObject.optString("reference", defaultValue.reference)
            val displayName = jsonObject.optString("displayName", defaultValue.displayName)
            SoundAssignment(
                sourceType = sourceType,
                reference = reference,
                displayName = displayName,
            )
        }.getOrDefault(defaultValue)
    }

    private fun encodeCustomSounds(sounds: List<CustomSound>): String {
        val jsonArray = JSONArray()
        sounds.forEach { sound ->
            jsonArray.put(
                JSONObject()
                    .put("uri", sound.uri)
                    .put("displayName", sound.displayName)
            )
        }
        return jsonArray.toString()
    }

    private fun decodeCustomSounds(rawValue: String?): List<CustomSound> {
        if (rawValue.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            buildList {
                val jsonArray = JSONArray(rawValue)
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    val uri = item.optString("uri")
                    val displayName = item.optString("displayName", "audio")
                    if (uri.isNotBlank()) {
                        add(CustomSound(uri = uri, displayName = displayName))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun legacyDefaultAssignment(
        isShake: Boolean,
        customSounds: List<CustomSound>,
        playbackMode: PlaybackMode,
        builtInPack: BuiltInPack,
    ): SoundAssignment {
        if (playbackMode == PlaybackMode.CUSTOM_ONLY && customSounds.isNotEmpty()) {
            return SoundAssignment(
                sourceType = SoundSourceType.CUSTOM,
                reference = customSounds.first().uri,
                displayName = customSounds.first().displayName,
            )
        }

        if (builtInPack == BuiltInPack.PROFANE) {
            return SoundAssignment(
                sourceType = SoundSourceType.BUILT_IN_PROFANE,
                reference = "profane_tts",
                displayName = "Русский мат TTS",
            )
        }

        if (!isShake && playbackMode == PlaybackMode.MIXED && customSounds.isNotEmpty()) {
            return SoundAssignment(
                sourceType = SoundSourceType.CUSTOM,
                reference = customSounds.first().uri,
                displayName = customSounds.first().displayName,
            )
        }

        return if (isShake) {
            SoundAssignment(
                sourceType = SoundSourceType.BUILT_IN_CLEAN,
                reference = "clean_doh1",
                displayName = "doh1.mp3",
            )
        } else {
            SoundAssignment(
                sourceType = SoundSourceType.BUILT_IN_CLEAN,
                reference = "clean_gta_wasted_5",
                displayName = "5-gta-wasted.mp3",
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "shake_groan_settings"
        const val KEY_ARMED = "armed"
        const val KEY_SHAKE_ENABLED = "shake_enabled"
        const val KEY_THROW_ENABLED = "throw_enabled"
        const val KEY_SHAKE_THRESHOLD = "shake_threshold"
        const val KEY_THROW_THRESHOLD = "throw_threshold"
        const val KEY_COOLDOWN_MS = "cooldown_ms"
        const val KEY_VOLUME = "volume"
        const val KEY_SHAKE_SOUND = "shake_sound"
        const val KEY_THROW_SOUND = "throw_sound"
        const val KEY_PLAYBACK_MODE = "playback_mode"
        const val KEY_BUILT_IN_PACK = "built_in_pack"
        const val KEY_CUSTOM_SOUNDS = "custom_sounds"
    }
}
