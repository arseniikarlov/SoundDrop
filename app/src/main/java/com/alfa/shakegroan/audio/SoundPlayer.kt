package com.alfa.shakegroan.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.BuiltInPack
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

    fun play(settings: AppSettings): PlaybackSource {
        val customUri = selectCustomUri(settings)
        if (customUri != null && playCustomSound(customUri, settings.playbackVolume)) {
            return PlaybackSource.CUSTOM
        }

        return playBuiltInSound(settings)
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
            onInfo("Ошибка доступа к пользовательскому аудио, использую встроенный набор")
            false
        }
    }

    private fun playBuiltInSound(settings: AppSettings): PlaybackSource {
        return when (settings.builtInPack) {
            BuiltInPack.CLEAN -> {
                playBundledCleanSound(settings.playbackVolume)
                PlaybackSource.BUILT_IN_CLEAN
            }

            BuiltInPack.PROFANE -> {
                if (playProfaneSpeech()) {
                    PlaybackSource.BUILT_IN_PROFANE
                } else {
                    onInfo("Матный TTS не готов, включаю встроенный не-матный набор")
                    playBundledCleanSound(settings.playbackVolume)
                    PlaybackSource.BUILT_IN_CLEAN
                }
            }
        }
    }

    private fun playBundledCleanSound(volume: Float) {
        val sound = BuiltInSoundCatalog.cleanSounds.randomOrNull()
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
