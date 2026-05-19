package com.alfa.shakegroan.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.PlaybackMode
import java.util.Locale
import kotlin.random.Random

class SoundPlayer(
    context: Context,
    private val onInfo: (String) -> Unit,
) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var ttsReady = false
    private val utterances = listOf(
        "бля!",
        "ёб твою мать!",
        "какого хера!",
        "пиздец!",
        "сука, бля!",
    )
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
                onInfo("TTS не инициализировался, встроенный мат может молчать")
            }
        }
    }

    fun play(settings: AppSettings): PlaybackSource {
        val customUri = selectCustomUri(settings)
        if (customUri != null && playCustomSound(customUri, settings.playbackVolume)) {
            return PlaybackSource.CUSTOM
        }

        playBuiltInSound()
        return PlaybackSource.BUILT_IN
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun selectCustomUri(settings: AppSettings): String? = when (settings.playbackMode) {
        PlaybackMode.BUILT_IN -> null
        PlaybackMode.CUSTOM_ONLY -> settings.customSounds.randomOrNull()?.uri
        PlaybackMode.MIXED -> {
            val customUri = settings.customSounds.randomOrNull()?.uri
            if (customUri != null && Random.nextBoolean()) customUri else null
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
            onInfo("Ошибка доступа к пользовательскому аудио, использую встроенный мат")
            false
        }
    }

    private fun playBuiltInSound() {
        if (!ttsReady) {
            onInfo("Системный TTS ещё не готов для русского мата")
            return
        }

        val engine = textToSpeech ?: return
        engine.stop()
        engine.speak(
            utterances.random(),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "swear-${System.currentTimeMillis()}"
        )
    }
}

enum class PlaybackSource {
    BUILT_IN,
    CUSTOM,
}
