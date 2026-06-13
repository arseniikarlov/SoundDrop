package com.alfa.shakegroan.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.SoundAssignment
import com.alfa.shakegroan.data.SoundSourceType
import com.alfa.shakegroan.data.soundFor
import com.alfa.shakegroan.motion.MotionEventType
import java.util.Locale

class SoundPlayer(
    context: Context,
    private val onInfo: (String) -> Unit,
) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var previewFinishedCallback: (() -> Unit)? = null
    private var previewProgressCallback: ((Float) -> Unit)? = null
    private var previewProgressRunnable: Runnable? = null
    private var ttsReady = false
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                val engine = textToSpeech
                if (engine == null) {
                    onInfo("TTS инициализировался слишком рано")
                } else {
                    val localeResult = engine.setLanguage(Locale.forLanguageTag("ru"))
                    if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.language = Locale.US
                    }
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post { finishPreviewIfNeeded() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post { finishPreviewIfNeeded() }
                        }

                        override fun onError(
                            utteranceId: String?,
                            errorCode: Int,
                        ) {
                            mainHandler.post { finishPreviewIfNeeded() }
                        }
                    })
                    engine.setPitch(0.92f)
                    engine.setSpeechRate(0.86f)
                }
            } else {
                onInfo("TTS не инициализировался, матный встроенный режим может молчать")
            }
        }
    }

    fun play(settings: AppSettings, eventType: MotionEventType): PlaybackSource {
        return playAssignment(settings.soundFor(eventType), settings.playbackVolume)
    }

    fun release() {
        stopActivePlayback()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    fun preview(
        assignment: SoundAssignment,
        volume: Float,
        onFinished: () -> Unit,
        onProgress: (Float) -> Unit,
    ): PlaybackSource {
        return playAssignment(assignment, volume, onFinished, onProgress)
    }

    fun stopActivePlayback() {
        previewFinishedCallback = null
        stopProgressUpdates(clearCallback = true)
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
    }

    private fun playAssignment(
        assignment: SoundAssignment,
        volume: Float,
        onFinished: (() -> Unit)? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): PlaybackSource = when (assignment.sourceType) {
        SoundSourceType.CUSTOM -> {
            if (playCustomSound(assignment.reference, volume, onFinished, onProgress)) {
                PlaybackSource.CUSTOM
            } else {
                val fallback = BuiltInSoundCatalog.defaultShakeAssignment()
                playCleanBundledSound(fallback.reference, volume, onFinished, onProgress)
                PlaybackSource.BUILT_IN_CLEAN
            }
        }

        SoundSourceType.BUILT_IN_PROFANE -> {
            if (playProfaneSpeech(onFinished, onProgress)) {
                PlaybackSource.BUILT_IN_PROFANE
            } else {
                onInfo("Матный TTS не готов, включаю встроенный не-матный набор")
                val fallback = BuiltInSoundCatalog.defaultShakeAssignment()
                playCleanBundledSound(fallback.reference, volume, onFinished, onProgress)
                PlaybackSource.BUILT_IN_CLEAN
            }
        }

        SoundSourceType.BUILT_IN_CLEAN -> {
            playCleanBundledSound(assignment.reference, volume, onFinished, onProgress)
            PlaybackSource.BUILT_IN_CLEAN
        }
    }

    private fun playCustomSound(
        uriString: String,
        volume: Float,
        onFinished: (() -> Unit)?,
        onProgress: ((Float) -> Unit)?,
    ): Boolean {
        return runCatching {
            stopActivePlayback()
            previewFinishedCallback = onFinished
            previewProgressCallback = onProgress
            val targetUri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                if (targetUri.scheme == "file") {
                    setDataSource(targetUri.path ?: uriString)
                } else {
                    setDataSource(appContext, targetUri)
                }
                prepare()
                setVolume(volume, volume)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    finishPreviewIfNeeded()
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    mediaPlayer = null
                    onInfo("Не удалось воспроизвести один из пользовательских файлов")
                    finishPreviewIfNeeded()
                    true
                }
                start()
                startProgressUpdates(this)
            }
            mediaPlayer != null
        }.getOrElse {
            onInfo("Ошибка доступа к пользовательскому аудио, использую встроенный набор")
            false
        }
    }

    private fun playCleanBundledSound(
        soundId: String,
        volume: Float,
        onFinished: (() -> Unit)?,
        onProgress: ((Float) -> Unit)?,
    ) {
        val sound = BuiltInSoundCatalog.cleanSoundById(soundId)
        if (sound == null) {
            onInfo("Не найден ни один встроенный не-матный файл")
            return
        }

        stopActivePlayback()
        previewFinishedCallback = onFinished
        previewProgressCallback = onProgress
        mediaPlayer = MediaPlayer.create(appContext, sound.resId)?.apply {
            setVolume(volume, volume)
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
                finishPreviewIfNeeded()
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                mediaPlayer = null
                onInfo("Не удалось воспроизвести один из встроенных не-матных файлов")
                finishPreviewIfNeeded()
                true
            }
            start()
            startProgressUpdates(this)
        }
        if (mediaPlayer == null) {
            onInfo("Не удалось открыть встроенный не-матный файл")
        }
    }

    private fun playProfaneSpeech(
        onFinished: (() -> Unit)?,
        onProgress: ((Float) -> Unit)?,
    ): Boolean {
        if (!ttsReady) {
            return false
        }

        val engine = textToSpeech ?: return false
        stopActivePlayback()
        previewFinishedCallback = onFinished
        previewProgressCallback = onProgress
        onProgress?.invoke(0.12f)
        engine.stop()
        engine.speak(
            BuiltInSoundCatalog.profanePhrases.random(),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "swear-${System.currentTimeMillis()}"
        )
        return true
    }

    private fun finishPreviewIfNeeded() {
        previewProgressCallback?.invoke(1f)
        stopProgressUpdates(clearCallback = true)
        val callback = previewFinishedCallback ?: return
        previewFinishedCallback = null
        callback()
    }

    private fun startProgressUpdates(player: MediaPlayer) {
        stopProgressUpdates(clearCallback = false)
        previewProgressCallback?.invoke(0f)
        previewProgressRunnable = object : Runnable {
            override fun run() {
                val progress = runCatching {
                    val duration = player.duration.coerceAtLeast(1)
                    (player.currentPosition / duration.toFloat()).coerceIn(0f, 1f)
                }.getOrDefault(0f)
                previewProgressCallback?.invoke(progress)
                val shouldContinue = mediaPlayer === player &&
                    runCatching { player.isPlaying }.getOrDefault(false)
                if (shouldContinue) {
                    mainHandler.postDelayed(this, 90L)
                }
            }
        }.also(mainHandler::post)
    }

    private fun stopProgressUpdates(clearCallback: Boolean) {
        previewProgressRunnable?.let(mainHandler::removeCallbacks)
        previewProgressRunnable = null
        if (clearCallback) {
            previewProgressCallback = null
        }
    }
}

enum class PlaybackSource {
    BUILT_IN_CLEAN,
    BUILT_IN_PROFANE,
    CUSTOM,
}
