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
    ): PlaybackSource {
        previewFinishedCallback = onFinished
        return playAssignment(assignment, volume, onFinished)
    }

    fun stopActivePlayback() {
        previewFinishedCallback = null
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
    }

    private fun playAssignment(
        assignment: SoundAssignment,
        volume: Float,
        onFinished: (() -> Unit)? = null,
    ): PlaybackSource = when (assignment.sourceType) {
        SoundSourceType.CUSTOM -> {
            if (playCustomSound(assignment.reference, volume, onFinished)) {
                PlaybackSource.CUSTOM
            } else {
                val fallback = BuiltInSoundCatalog.defaultShakeAssignment()
                playCleanBundledSound(fallback.reference, volume, onFinished)
                PlaybackSource.BUILT_IN_CLEAN
            }
        }

        SoundSourceType.BUILT_IN_PROFANE -> {
            if (playProfaneSpeech(onFinished)) {
                PlaybackSource.BUILT_IN_PROFANE
            } else {
                onInfo("Матный TTS не готов, включаю встроенный не-матный набор")
                val fallback = BuiltInSoundCatalog.defaultShakeAssignment()
                playCleanBundledSound(fallback.reference, volume, onFinished)
                PlaybackSource.BUILT_IN_CLEAN
            }
        }

        SoundSourceType.BUILT_IN_CLEAN -> {
            playCleanBundledSound(assignment.reference, volume, onFinished)
            PlaybackSource.BUILT_IN_CLEAN
        }
    }

    private fun playCustomSound(
        uriString: String,
        volume: Float,
        onFinished: (() -> Unit)?,
    ): Boolean {
        return runCatching {
            stopActivePlayback()
            previewFinishedCallback = onFinished
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
    ) {
        val sound = BuiltInSoundCatalog.cleanSoundById(soundId)
        if (sound == null) {
            onInfo("Не найден ни один встроенный не-матный файл")
            return
        }

        stopActivePlayback()
        previewFinishedCallback = onFinished
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
        }
        if (mediaPlayer == null) {
            onInfo("Не удалось открыть встроенный не-матный файл")
        }
    }

    private fun playProfaneSpeech(onFinished: (() -> Unit)?): Boolean {
        if (!ttsReady) {
            return false
        }

        val engine = textToSpeech ?: return false
        stopActivePlayback()
        previewFinishedCallback = onFinished
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
        val callback = previewFinishedCallback ?: return
        previewFinishedCallback = null
        callback()
    }
}

enum class PlaybackSource {
    BUILT_IN_CLEAN,
    BUILT_IN_PROFANE,
    CUSTOM,
}
