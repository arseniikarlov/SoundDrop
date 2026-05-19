package com.alfa.shakegroan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.alfa.shakegroan.data.AppSettingsRepository
import com.alfa.shakegroan.service.BackgroundMonitorService

class FallOuchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        FallOuchWidgetUpdater.update(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_MONITORING -> {
                toggleMonitoring(context)
                FallOuchWidgetUpdater.refreshAll(context)
            }
        }
    }

    private fun toggleMonitoring(context: Context) {
        val repository = AppSettingsRepository(context)
        val currentSettings = repository.load()
        val updatedSettings = currentSettings.copy(isArmed = !currentSettings.isArmed)
        repository.save(updatedSettings)
        if (updatedSettings.isArmed) {
            BackgroundMonitorService.startOrUpdate(context)
        } else {
            BackgroundMonitorService.stop(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_MONITORING = "com.alfa.shakegroan.widget.action.TOGGLE_MONITORING"
    }
}
