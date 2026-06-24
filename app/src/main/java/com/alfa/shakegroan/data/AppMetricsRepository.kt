package com.alfa.shakegroan.data

import android.content.Context

class AppMetricsRepository(context: Context) {

    private val transport = AppMetricsTransport(context)
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordArmedChange(isArmed: Boolean, nowMs: Long = System.currentTimeMillis()) {
        val activeStartedAt = preferences.getLong(KEY_ACTIVE_STARTED_AT_MS, 0L)
        val editor = preferences.edit()
        if (isArmed) {
            editor.putLong(KEY_ARM_COUNT, preferences.getLong(KEY_ARM_COUNT, 0L) + 1L)
            if (activeStartedAt == 0L) {
                editor.putLong(KEY_ACTIVE_STARTED_AT_MS, nowMs)
            }
            transport.sendEvent("monitoring_enabled", timestampMs = nowMs)
        } else {
            editor.putLong(KEY_DISARM_COUNT, preferences.getLong(KEY_DISARM_COUNT, 0L) + 1L)
            var activeDurationMs = 0L
            if (activeStartedAt > 0L) {
                activeDurationMs = (nowMs - activeStartedAt).coerceAtLeast(0L)
                editor.putLong(
                    KEY_TOTAL_ACTIVE_MS,
                    preferences.getLong(KEY_TOTAL_ACTIVE_MS, 0L) + activeDurationMs
                )
                editor.putLong(KEY_ACTIVE_STARTED_AT_MS, 0L)
            }
            transport.sendEvent(
                name = "monitoring_disabled",
                params = mapOf("active_duration_ms" to activeDurationMs),
                timestampMs = nowMs,
            )
        }
        editor.apply()
    }

    fun recordCustomSoundsAdded(addedCount: Int, currentCustomSoundsCount: Int) {
        if (addedCount <= 0) {
            return
        }
        preferences.edit()
            .putLong(KEY_CUSTOM_SOUND_UPLOADS, preferences.getLong(KEY_CUSTOM_SOUND_UPLOADS, 0L) + addedCount)
            .putInt(
                KEY_MAX_CUSTOM_SOUNDS,
                maxOf(preferences.getInt(KEY_MAX_CUSTOM_SOUNDS, 0), currentCustomSoundsCount)
            )
            .apply()
        transport.sendEvent(
            name = "custom_sound_added",
            params = mapOf(
                "added_count" to addedCount,
                "current_custom_sounds_count" to currentCustomSoundsCount,
            )
        )
    }

    fun recordWidgetInstalled(activeWidgetCount: Int) {
        val wasInstalled = preferences.getBoolean(KEY_WIDGET_INSTALLED, false)
        val isInstalled = activeWidgetCount > 0
        preferences.edit()
            .putBoolean(KEY_WIDGET_INSTALLED, isInstalled)
            .putInt(KEY_WIDGET_ACTIVE_COUNT, activeWidgetCount)
            .putLong(KEY_WIDGET_UPDATE_COUNT, preferences.getLong(KEY_WIDGET_UPDATE_COUNT, 0L) + 1L)
            .apply()
        if (isInstalled && !wasInstalled) {
            transport.sendEvent(
                name = "widget_installed",
                params = mapOf("active_widget_count" to activeWidgetCount)
            )
        } else if (!isInstalled && wasInstalled) {
            transport.sendEvent("widget_removed")
        }
    }

    fun recordWidgetPinRequested() {
        preferences.edit()
            .putLong(KEY_WIDGET_PIN_REQUEST_COUNT, preferences.getLong(KEY_WIDGET_PIN_REQUEST_COUNT, 0L) + 1L)
            .apply()
        transport.sendEvent("widget_pin_requested")
    }

    fun recordWidgetToggle() {
        preferences.edit()
            .putLong(KEY_WIDGET_TOGGLE_COUNT, preferences.getLong(KEY_WIDGET_TOGGLE_COUNT, 0L) + 1L)
            .apply()
        transport.sendEvent("widget_toggle")
    }

    fun recordWidgetOpen() {
        preferences.edit()
            .putLong(KEY_WIDGET_OPEN_COUNT, preferences.getLong(KEY_WIDGET_OPEN_COUNT, 0L) + 1L)
            .apply()
        transport.sendEvent("widget_open")
    }

    fun recordLanguageSelected(language: AppLanguage) {
        preferences.edit()
            .putString(KEY_LAST_LANGUAGE, language.code)
            .apply()
        transport.sendEvent(
            name = "language_selected",
            params = mapOf(
                "language_code" to language.code,
                "language_label" to language.label,
            )
        )
    }

    private companion object {
        const val PREFS_NAME = "fall_ouch_metrics"
        const val KEY_ARM_COUNT = "arm_count"
        const val KEY_DISARM_COUNT = "disarm_count"
        const val KEY_TOTAL_ACTIVE_MS = "total_active_ms"
        const val KEY_ACTIVE_STARTED_AT_MS = "active_started_at_ms"
        const val KEY_CUSTOM_SOUND_UPLOADS = "custom_sound_uploads"
        const val KEY_MAX_CUSTOM_SOUNDS = "max_custom_sounds"
        const val KEY_WIDGET_INSTALLED = "widget_installed"
        const val KEY_WIDGET_ACTIVE_COUNT = "widget_active_count"
        const val KEY_WIDGET_UPDATE_COUNT = "widget_update_count"
        const val KEY_WIDGET_PIN_REQUEST_COUNT = "widget_pin_request_count"
        const val KEY_WIDGET_TOGGLE_COUNT = "widget_toggle_count"
        const val KEY_WIDGET_OPEN_COUNT = "widget_open_count"
        const val KEY_LAST_LANGUAGE = "last_language"
    }
}
