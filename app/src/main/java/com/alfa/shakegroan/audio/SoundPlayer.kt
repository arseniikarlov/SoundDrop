package com.alfa.shakegroan.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.alfa.shakegroan.data.AppSettings
import com.alfa.shakegroan.data.SoundAssignment
import com.alfa.shakegroan.data.SoundSourceType
import com.alfa.shakegroan.data.soundFor
import com.alfa.shakegroan.motion.MotionEventType

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

    fun play(settings: AppSettings, eventType: MotionEventType): PlaybackSource {
        return playAssignment(settings.soundFor(eventType), settings.playbackVolume)
    }

    fun release() {
        stopActivePlayback()
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
                val fallback = BuiltInSoundCatalog.defaultSlapAssignment()
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
            val playerVolume = effectivePlaybackVolume(volume)
            val targetUri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                if (targetUri.scheme == "file") {
                    setDataSource(targetUri.path ?: uriString)
                } else {
                    setDataSource(appContext, targetUri)
                }
                prepare()
                setVolume(playerVolume, playerVolume)
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
            ?: BuiltInSoundCatalog.cleanSoundById(BuiltInSoundCatalog.defaultSlapAssignment().reference)
        if (sound == null) {
            onInfo("Не найден ни один встроенный файл")
            return
        }

        stopActivePlayback()
        previewFinishedCallback = onFinished
        previewProgressCallback = onProgress
        val playerVolume = effectivePlaybackVolume(volume)
        mediaPlayer = MediaPlayer.create(appContext, sound.resId)?.apply {
            setVolume(playerVolume, playerVolume)
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
                finishPreviewIfNeeded()
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                mediaPlayer = null
                onInfo("Не удалось воспроизвести один из встроенных файлов")
                finishPreviewIfNeeded()
                true
            }
            start()
            startProgressUpdates(this)
        }
        if (mediaPlayer == null) {
            onInfo("Не удалось открыть встроенный файл")
        }
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

    private fun effectivePlaybackVolume(volume: Float): Float {
        val safeVolume = volume.coerceIn(0f, 1f)
        return safeVolume * safeVolume
    }
}

enum class PlaybackSource {
    BUILT_IN_CLEAN,
    CUSTOM,
}
