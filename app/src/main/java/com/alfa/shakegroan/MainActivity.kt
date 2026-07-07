package com.alfa.shakegroan

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.alfa.shakegroan.data.AppMetricsRepository
import com.alfa.shakegroan.data.PickedSound
import com.alfa.shakegroan.data.displayNameWithoutAudioExtension
import com.alfa.shakegroan.service.BackgroundMonitorService
import com.alfa.shakegroan.ui.MainViewModel
import com.alfa.shakegroan.ui.ShakeGroanApp
import com.alfa.shakegroan.ui.theme.ShakeGroanTheme
import com.alfa.shakegroan.widget.FallOuchWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var runtimeReceiverRegistered = false

    private val runtimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BackgroundMonitorService.ACTION_RUNTIME_UPDATE) {
                return
            }

            viewModel.onServiceRuntimeUpdate(
                lastTriggerLabel = intent.getStringExtra(BackgroundMonitorService.EXTRA_LAST_TRIGGER),
                statusMessage = intent.getStringExtra(BackgroundMonitorService.EXTRA_STATUS_MESSAGE),
                isArmed = intent.extras?.let {
                    if (it.containsKey(BackgroundMonitorService.EXTRA_IS_ARMED)) {
                        it.getBoolean(BackgroundMonitorService.EXTRA_IS_ARMED)
                    } else {
                        null
                    }
                }
            )
        }
    }

    private val openAudioFiles =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val pickedSounds = uris.mapNotNull { uri ->
                persistReadPermission(uri)
                PickedSound(
                    uri = uri.toString(),
                    displayName = queryDisplayName(uri.toString()) ?: uri.lastPathSegment ?: "audio"
                )
            }

            if (pickedSounds.isNotEmpty()) {
                viewModel.addCustomSounds(pickedSounds)
            }
        }

    private val openVideoFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }
            persistReadPermission(uri)
            viewModel.prepareVideoDraft(
                uriString = uri.toString(),
                displayName = displayNameWithoutAudioExtension(
                    queryDisplayName(uri.toString()) ?: uri.lastPathSegment ?: "video"
                ),
            )
        }

    private val requestMicrophonePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.startRecordingSession()
            } else {
                viewModel.onRecordPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        recordWidgetOpenIfNeeded(intent)

        setContent {
            ShakeGroanTheme {
                ShakeGroanApp(
                    viewModel = viewModel,
                    onPickAudioFiles = { openAudioFiles.launch(arrayOf("audio/*")) },
                    onPickVideoFile = { openVideoFile.launch(arrayOf("video/*")) },
                    onRequestRecording = {
                        val permissionState = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            viewModel.startRecordingSession()
                        } else {
                            requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recordWidgetOpenIfNeeded(intent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.reloadSettingsFromStorage()
        if (!runtimeReceiverRegistered) {
            val filter = IntentFilter(BackgroundMonitorService.ACTION_RUNTIME_UPDATE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(runtimeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                ContextCompat.registerReceiver(
                    this,
                    runtimeReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            runtimeReceiverRegistered = true
        }
    }

    override fun onStop() {
        if (runtimeReceiverRegistered) {
            unregisterReceiver(runtimeReceiver)
            runtimeReceiverRegistered = false
        }
        super.onStop()
    }

    private fun persistReadPermission(uri: android.net.Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun queryDisplayName(uriString: String): String? {
        val uri = android.net.Uri.parse(uriString)
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cursor: Cursor = contentResolver.query(uri, projection, null, null, null) ?: return null
        cursor.use {
            if (!it.moveToFirst()) {
                return null
            }

            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index == -1) {
                return null
            }

            return it.getString(index)
        }
    }

    private fun recordWidgetOpenIfNeeded(intent: Intent?) {
        if (intent?.getBooleanExtra(FallOuchWidgetProvider.EXTRA_OPENED_FROM_WIDGET, false) == true) {
            AppMetricsRepository(this).recordWidgetOpen()
        }
    }
}
