package com.alfa.shakegroan.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppSettingsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        isArmed = preferences.getBoolean(KEY_ARMED, true),
        shakeEnabled = preferences.getBoolean(KEY_SHAKE_ENABLED, true),
        throwEnabled = preferences.getBoolean(KEY_THROW_ENABLED, true),
        shakeDeltaThreshold = preferences.getFloat(KEY_SHAKE_THRESHOLD, 13.5f),
        throwImpactThreshold = preferences.getFloat(KEY_THROW_THRESHOLD, 22.0f),
        cooldownMs = preferences.getInt(KEY_COOLDOWN_MS, 1400),
        playbackVolume = preferences.getFloat(KEY_VOLUME, 0.9f),
        playbackMode = preferences.getString(KEY_PLAYBACK_MODE, PlaybackMode.MIXED.name)
            ?.let { raw -> PlaybackMode.entries.firstOrNull { it.name == raw } }
            ?: PlaybackMode.MIXED,
        customSounds = decodeCustomSounds(preferences.getString(KEY_CUSTOM_SOUNDS, null)),
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_ARMED, settings.isArmed)
            .putBoolean(KEY_SHAKE_ENABLED, settings.shakeEnabled)
            .putBoolean(KEY_THROW_ENABLED, settings.throwEnabled)
            .putFloat(KEY_SHAKE_THRESHOLD, settings.shakeDeltaThreshold)
            .putFloat(KEY_THROW_THRESHOLD, settings.throwImpactThreshold)
            .putInt(KEY_COOLDOWN_MS, settings.cooldownMs)
            .putFloat(KEY_VOLUME, settings.playbackVolume)
            .putString(KEY_PLAYBACK_MODE, settings.playbackMode.name)
            .putString(KEY_CUSTOM_SOUNDS, encodeCustomSounds(settings.customSounds))
            .apply()
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

    private companion object {
        const val PREFS_NAME = "shake_groan_settings"
        const val KEY_ARMED = "armed"
        const val KEY_SHAKE_ENABLED = "shake_enabled"
        const val KEY_THROW_ENABLED = "throw_enabled"
        const val KEY_SHAKE_THRESHOLD = "shake_threshold"
        const val KEY_THROW_THRESHOLD = "throw_threshold"
        const val KEY_COOLDOWN_MS = "cooldown_ms"
        const val KEY_VOLUME = "volume"
        const val KEY_PLAYBACK_MODE = "playback_mode"
        const val KEY_CUSTOM_SOUNDS = "custom_sounds"
    }
}
