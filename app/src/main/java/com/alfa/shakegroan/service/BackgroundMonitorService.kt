package com.alfa.shakegroan.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.alfa.shakegroan.MainActivity
import com.alfa.shakegroan.R
import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import com.alfa.shakegroan.audio.PlaybackSource
import com.alfa.shakegroan.audio.SoundPlayer
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.AppSettingsRepository
import com.alfa.shakegroan.data.toDetectorConfig
import com.alfa.shakegroan.motion.MotionEventType
import com.alfa.shakegroan.motion.MotionSensorMonitor
import com.alfa.shakegroan.widget.FallOuchWidgetUpdater

class BackgroundMonitorService : Service() {

    private lateinit var repository: AppSettingsRepository
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var sensorMonitor: MotionSensorMonitor
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager
    private lateinit var screenOffWakeLock: PowerManager.WakeLock
    private var currentSettings: AppSettings = AppSettings()
    private var lastTriggerLabel: String = "Пока тишина"
    private var screenStateReceiverRegistered = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> updateScreenOffWakeLock()
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> releaseScreenOffWakeLock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppSettingsRepository(applicationContext)
        notificationManager = getSystemService(NotificationManager::class.java)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        screenOffWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:FallOuchScreenOffMonitor"
        ).apply {
            setReferenceCounted(false)
        }
        createNotificationChannel()
        soundPlayer = SoundPlayer(applicationContext) { info ->
            broadcastUpdate(
                statusMessage = info,
                isArmed = currentSettings.isArmed,
            )
            updateNotification(info)
        }
        sensorMonitor = MotionSensorMonitor(applicationContext) { eventType ->
            handleMotionEvent(eventType)
        }
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring(fromUser = true)
                return START_NOT_STICKY
            }
        }

        currentSettings = repository.load()
        if (!currentSettings.isArmed) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground("Работает при блокировке: приложение реагирует даже с выключенным экраном")
        sensorMonitor.start(currentSettings.toDetectorConfig())
        updateScreenOffWakeLock()
        FallOuchWidgetUpdater.refreshAll(this)
        broadcastUpdate(
            statusMessage = "Работает при блокировке",
            isArmed = true,
        )
        return START_STICKY
    }

    override fun onDestroy() {
        releaseScreenOffWakeLock()
        unregisterScreenStateReceiver()
        sensorMonitor.stop()
        soundPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleMotionEvent(eventType: MotionEventType) {
        currentSettings = repository.load()
        val assignment = when (eventType) {
            MotionEventType.SHAKE -> currentSettings.shakeSound
            MotionEventType.THROW -> currentSettings.throwSound
            MotionEventType.SLAP -> currentSettings.slapSound
        }
        val source = soundPlayer.play(currentSettings, eventType)
        lastTriggerLabel = when (eventType) {
            MotionEventType.SHAKE -> "Последнее событие: тряска -> ${assignment.displayName}"
            MotionEventType.THROW -> "Последнее событие: падение -> ${assignment.displayName}"
            MotionEventType.SLAP -> "Последнее событие: шлепок -> ${assignment.displayName}"
        }
        val statusMessage = when (source) {
            PlaybackSource.BUILT_IN_CLEAN -> "Сработал встроенный файл ${BuiltInSoundCatalog.labelFor(assignment)}"
            PlaybackSource.BUILT_IN_PROFANE -> "Сработал матный режим ${BuiltInSoundCatalog.labelFor(assignment)}"
            PlaybackSource.CUSTOM -> "Сработал пользовательский звук ${assignment.displayName}"
        }

        broadcastUpdate(
            lastTrigger = lastTriggerLabel,
            statusMessage = statusMessage,
            isArmed = true,
        )
        updateNotification(statusMessage)
    }

    private fun stopMonitoring(fromUser: Boolean) {
        releaseScreenOffWakeLock()
        sensorMonitor.stop()
        currentSettings = repository.load().copy(isArmed = false)
        repository.save(currentSettings)
        if (fromUser) {
            lastTriggerLabel = "Мониторинг остановлен"
        }
        broadcastUpdate(
            lastTrigger = lastTriggerLabel,
            statusMessage = "Фоновый режим выключен",
            isArmed = false,
        )
        FallOuchWidgetUpdater.refreshAll(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(statusMessage: String) {
        val notification = buildNotification(statusMessage)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }

            else -> startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(statusMessage: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(statusMessage))
    }

    private fun buildNotification(statusMessage: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Fall Ouch! работает в фоне")
        .setContentText(statusMessage)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("$statusMessage\nДетекция работает при блокировке. Выключить можно из приложения или кнопкой в уведомлении.")
        )
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(openAppPendingIntent())
        .addAction(0, "Выключить", stopServicePendingIntent())
        .build()

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopServicePendingIntent(): PendingIntent {
        val intent = Intent(this, BackgroundMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Fall Ouch! Background Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Уведомление для фонового мониторинга движения и звуковых реакций"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun registerScreenStateReceiver() {
        if (screenStateReceiverRegistered) {
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        screenStateReceiverRegistered = true
    }

    private fun unregisterScreenStateReceiver() {
        if (!screenStateReceiverRegistered) {
            return
        }
        runCatching {
            unregisterReceiver(screenStateReceiver)
        }
        screenStateReceiverRegistered = false
    }

    private fun updateScreenOffWakeLock() {
        currentSettings = repository.load()
        val shouldHoldWakeLock = currentSettings.isArmed &&
            !powerManager.isInteractive &&
            sensorMonitor.requiresScreenOffWakeLock()
        if (shouldHoldWakeLock) {
            acquireScreenOffWakeLock()
        } else {
            releaseScreenOffWakeLock()
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireScreenOffWakeLock() {
        if (!screenOffWakeLock.isHeld) {
            screenOffWakeLock.acquire()
        }
    }

    private fun releaseScreenOffWakeLock() {
        if (screenOffWakeLock.isHeld) {
            screenOffWakeLock.release()
        }
    }

    private fun broadcastUpdate(
        lastTrigger: String? = null,
        statusMessage: String,
        isArmed: Boolean,
    ) {
        val intent = Intent(ACTION_RUNTIME_UPDATE).apply {
            `package` = packageName
            putExtra(EXTRA_STATUS_MESSAGE, statusMessage)
            putExtra(EXTRA_IS_ARMED, isArmed)
            lastTrigger?.let { putExtra(EXTRA_LAST_TRIGGER, it) }
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_RUNTIME_UPDATE = "com.alfa.shakegroan.RUNTIME_UPDATE"
        const val EXTRA_LAST_TRIGGER = "extra_last_trigger"
        const val EXTRA_STATUS_MESSAGE = "extra_status_message"
        const val EXTRA_IS_ARMED = "extra_is_armed"

        private const val ACTION_STOP = "com.alfa.shakegroan.action.STOP_MONITORING"
        private const val CHANNEL_ID = "sounddrop_background_monitor"
        private const val NOTIFICATION_ID = 1107

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, BackgroundMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundMonitorService::class.java))
        }
    }
}
