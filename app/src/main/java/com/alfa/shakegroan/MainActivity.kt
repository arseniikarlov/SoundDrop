package com.alfa.shakegroan

import android.database.Cursor
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.alfa.shakegroan.data.PickedSound
import com.alfa.shakegroan.ui.MainViewModel
import com.alfa.shakegroan.ui.ShakeGroanApp
import com.alfa.shakegroan.ui.theme.ShakeGroanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val openAudioFiles =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val pickedSounds = uris.mapNotNull { uri ->
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                PickedSound(
                    uri = uri.toString(),
                    displayName = queryDisplayName(uri.toString()) ?: uri.lastPathSegment ?: "audio"
                )
            }

            if (pickedSounds.isNotEmpty()) {
                viewModel.addCustomSounds(pickedSounds)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ShakeGroanTheme {
                ShakeGroanApp(
                    viewModel = viewModel,
                    onAddSounds = { openAudioFiles.launch(arrayOf("audio/*")) }
                )
            }
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
}

