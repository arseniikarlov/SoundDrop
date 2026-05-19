package com.alfa.shakegroan.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
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
    private var mediaPlayer: MediaPlayer? = null
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
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    fun preview(assignment: SoundAssignment, volume: Float): PlaybackSource {
        return playAssignment(assignment, volume)
    }

    private fun playAssignment(
        assignment: SoundAssignment,
        volume: Float,
    ): PlaybackSource = when (assignment.sourceType) {
        SoundSourceType.CUSTOM -> {
            if (playCustomSound(assignment.reference, volume)) {
                PlaybackSource.CUSTOM
            } else {
                val fallback = BuiltInSoundCatalog.defaultShakeAssignment()
                playCleanBundledSound(fallback.reference, volume)
                PlaybackSource.BUILT_IN_CLEAN
            }
        }

        SoundSourceType.BUILT_IN_PROFANE -> {
            if (playProfaneSpeech()) {
                PlaybackSource.BUILT_IN_PROFANE
            } else {
                onInfo("Матный TTS не готов, включаю встроенный не-матный набор")
                val fallback = BuiltInSoundCatalog.defaultShakeAssignment()
                playCleanBundledSound(fallback.reference, volume)
                PlaybackSource.BUILT_IN_CLEAN
            }
        }

        SoundSourceType.BUILT_IN_CLEAN -> {
            playCleanBundledSound(assignment.reference, volume)
            PlaybackSource.BUILT_IN_CLEAN
        }
    }

    private fun playCustomSound(uriString: String, volume: Float): Boolean {
        return runCatching {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(appContext, Uri.parse(uriString))?.apply {
                setVolume(volume, volume)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    mediaPlayer = null
                    onInfo("Не удалось воспроизвести один из пользовательских файлов")
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
    ) {
        val sound = BuiltInSoundCatalog.cleanSoundById(soundId)
        if (sound == null) {
            onInfo("Не найден ни один встроенный не-матный файл")
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(appContext, sound.resId)?.apply {
            setVolume(volume, volume)
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                mediaPlayer = null
                onInfo("Не удалось воспроизвести один из встроенных не-матных файлов")
                true
            }
            start()
        }
        if (mediaPlayer == null) {
            onInfo("Не удалось открыть встроенный не-матный файл")
        }
    }

    private fun playProfaneSpeech(): Boolean {
        if (!ttsReady) {
            return false
        }

        val engine = textToSpeech ?: return false
        engine.stop()
        engine.speak(
            BuiltInSoundCatalog.profanePhrases.random(),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "swear-${System.currentTimeMillis()}"
        )
        return true
    }
}

enum class PlaybackSource {
    BUILT_IN_CLEAN,
    BUILT_IN_PROFANE,
    CUSTOM,
}
