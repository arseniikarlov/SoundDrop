package com.alfa.shakegroan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alfa.shakegroan.MainActivity
import com.alfa.shakegroan.R
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.AppSettingsRepository

object FallOuchWidgetUpdater {

    fun refreshAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, FallOuchWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isNotEmpty()) {
            update(context, appWidgetManager, widgetIds)
        }
    }

    fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val settings = AppSettingsRepository(context).load()
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildRemoteViews(context, appWidgetId, settings)
            )
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        settings: AppSettings,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_fall_ouch)
        val isArmed = settings.isArmed
        views.setTextViewText(
            R.id.widget_status_title,
            if (isArmed) "включен" else "выключен"
        )
        views.setTextViewText(R.id.widget_toggle_button, "⏻")
        views.setInt(
            R.id.widget_toggle_button,
            "setBackgroundResource",
            if (isArmed) R.drawable.widget_button_secondary else R.drawable.widget_button_primary
        )
        views.setTextColor(
            R.id.widget_toggle_button,
            if (isArmed) 0xFFF7FBFF.toInt() else 0xFF08111F.toInt()
        )

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(context, FallOuchWidgetProvider::class.java).apply {
                action = FallOuchWidgetProvider.ACTION_TOGGLE_MONITORING
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)
        return views
    }

}
