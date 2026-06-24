package com.alfa.shakegroan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.alfa.shakegroan.data.AppMetricsRepository
import com.alfa.shakegroan.data.AppSettingsRepository
import com.alfa.shakegroan.service.BackgroundMonitorService

class FallOuchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        AppMetricsRepository(context).recordWidgetInstalled(appWidgetIds.size)
        FallOuchWidgetUpdater.update(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AppMetricsRepository(context).recordWidgetInstalled(activeWidgetCount = 1)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        AppMetricsRepository(context).recordWidgetInstalled(activeWidgetCount = 0)
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
        val metricsRepository = AppMetricsRepository(context)
        val currentSettings = repository.load()
        val updatedSettings = currentSettings.copy(isArmed = !currentSettings.isArmed)
        metricsRepository.recordWidgetToggle()
        metricsRepository.recordArmedChange(updatedSettings.isArmed)
        repository.save(updatedSettings)
        if (updatedSettings.isArmed) {
            BackgroundMonitorService.startOrUpdate(context)
        } else {
            BackgroundMonitorService.stop(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_MONITORING = "com.alfa.shakegroan.widget.action.TOGGLE_MONITORING"
        const val EXTRA_OPENED_FROM_WIDGET = "com.alfa.shakegroan.widget.extra.OPENED_FROM_WIDGET"
    }
}
