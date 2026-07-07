package com.alfa.shakegroan.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import com.alfa.shakegroan.data.CustomSound
import com.alfa.shakegroan.data.displayNameWithoutAudioExtension
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

enum class DraftSourceKind {
    VIDEO,
    RECORDING,
}

data class EditableClipDraft(
    val sourceKind: DraftSourceKind,
    val sourceUri: String,
    val sourceLabel: String,
    val proposedDisplayName: String,
    val durationMs: Long,
    val isTemporarySource: Boolean = false,
)

data class TrimSelection(
    val startMs: Long,
    val endMs: Long,
)

object TrimSelectionNormalizer {
    const val MIN_CLIP_MS = 400L

    fun normalize(
        durationMs: Long,
        startMs: Long,
        endMs: Long,
        minClipMs: Long = MIN_CLIP_MS,
    ): TrimSelection {
        val safeDuration = durationMs.coerceAtLeast(0L)
        if (safeDuration == 0L) {
            return TrimSelection(startMs = 0L, endMs = 0L)
        }

        val safeMin = minClipMs.coerceAtLeast(1L).coerceAtMost(safeDuration)
        val normalizedStart = startMs.coerceIn(0L, max(0L, safeDuration - safeMin))
        val normalizedEnd = endMs.coerceIn(normalizedStart + safeMin, safeDuration)
        return TrimSelection(
            startMs = normalizedStart,
            endMs = normalizedEnd,
        )
    }
}

class AudioStudioManager(
    context: Context,
    private val onInfo: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val previewHandler = Handler(Looper.getMainLooper())

    private var previewPlayer: MediaPlayer? = null
    private var previewStopRunnable: Runnable? = null
    private var previewProgressRunnable: Runnable? = null
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null

    fun createDraftFromVideo(
        uriString: String,
        displayName: String,
    ): EditableClipDraft {
        val uri = Uri.parse(uriString)
        ensureAudioTrackExists(uri)
        val sourceLabel = displayName.ifBlank { "video.mp4" }
        return EditableClipDraft(
            sourceKind = DraftSourceKind.VIDEO,
            sourceUri = uriString,
            sourceLabel = sourceLabel,
            proposedDisplayName = buildDisplayName(sourceLabel, suffix = "_clip"),
            durationMs = readDurationMs(uri).coerceAtLeast(1000L),
            isTemporarySource = false,
        )
    }

    fun startRecording() {
        if (recorder != null) {
            return
        }

        val outputFile = createDraftFile(prefix = "recording", extension = ".m4a")
        val nextRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        recordingFile = outputFile
        recorder = nextRecorder
    }

    fun stopRecording(): EditableClipDraft {
        val activeRecorder = recorder ?: throw IllegalStateException("Запись не запущена")
        val outputFile = recordingFile ?: throw IllegalStateException("Файл записи не найден")

        try {
            activeRecorder.stop()
        } catch (error: Exception) {
            outputFile.delete()
            throw IllegalStateException("Не удалось завершить запись. Попробуй записать чуть дольше.", error)
        } finally {
            activeRecorder.reset()
            activeRecorder.release()
            recorder = null
            recordingFile = null
        }

        val outputUri = Uri.fromFile(outputFile).toString()
        return EditableClipDraft(
            sourceKind = DraftSourceKind.RECORDING,
            sourceUri = outputUri,
            sourceLabel = outputFile.name,
            proposedDisplayName = buildDisplayName(outputFile.name),
            durationMs = readDurationMs(Uri.parse(outputUri)).coerceAtLeast(1000L),
            isTemporarySource = true,
        )
    }

    fun cancelRecording() {
        val activeRecorder = recorder
        val outputFile = recordingFile
        recorder = null
        recordingFile = null

        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.reset() }
            runCatching { activeRecorder.release() }
        }

        outputFile?.delete()
    }

    fun saveTrimmedClip(
        draft: EditableClipDraft,
        requestedDisplayName: String,
        startMs: Long,
        endMs: Long,
    ): CustomSound {
        val selection = TrimSelectionNormalizer.normalize(
            durationMs = draft.durationMs,
            startMs = startMs,
            endMs = endMs,
        )
        val outputFile = createUniqueSoundFile(requestedDisplayName)
        trimAudioTrack(
            sourceUri = Uri.parse(draft.sourceUri),
            outputFile = outputFile,
            startUs = selection.startMs * 1000L,
            endUs = selection.endMs * 1000L,
        )
        return CustomSound(
            uri = Uri.fromFile(outputFile).toString(),
            displayName = displayNameWithoutAudioExtension(outputFile.name),
        )
    }

    fun saveEditedCustomSound(
        uriString: String,
        requestedDisplayName: String,
        startMs: Long,
        endMs: Long,
        durationMs: Long,
    ): CustomSound {
        val selection = TrimSelectionNormalizer.normalize(
            durationMs = durationMs,
            startMs = startMs,
            endMs = endMs,
        )
        val sourceUri = Uri.parse(uriString)
        val isFullSelection = selection.startMs <= 50L &&
            selection.endMs >= durationMs.coerceAtLeast(1L) - 50L

        if (isFullSelection && sourceUri.scheme == "file") {
            return CustomSound(
                uri = uriString,
                displayName = displayNameWithoutAudioExtension(requestedDisplayName),
            )
        }

        if (isFullSelection) {
            return importCustomSound(uriString, requestedDisplayName)
        }

        val outputFile = createUniqueSoundFile(requestedDisplayName)
        try {
            trimAudioTrack(
                sourceUri = sourceUri,
                outputFile = outputFile,
                startUs = selection.startMs * 1000L,
                endUs = selection.endMs * 1000L,
            )
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        }

        return CustomSound(
            uri = Uri.fromFile(outputFile).toString(),
            displayName = displayNameWithoutAudioExtension(outputFile.name),
        )
    }

    fun importCustomSound(
        uriString: String,
        requestedDisplayName: String,
    ): CustomSound {
        val sourceUri = Uri.parse(uriString)
        val outputFile = createUniqueSoundFile(
            displayName = requestedDisplayName,
            extension = importExtension(sourceUri, requestedDisplayName),
            preserveExistingExtension = true,
        )

        try {
            openInputStream(sourceUri).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            ensureAudioTrackExists(Uri.fromFile(outputFile))
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        }

        return CustomSound(
            uri = Uri.fromFile(outputFile).toString(),
            displayName = displayNameWithoutAudioExtension(outputFile.name),
        )
    }

    fun playPreview(
        draft: EditableClipDraft,
        startMs: Long,
        endMs: Long,
        volume: Float,
        onProgress: (Float) -> Unit,
        onFinished: () -> Unit,
    ) {
        val selection = TrimSelectionNormalizer.normalize(
            durationMs = draft.durationMs,
            startMs = startMs,
            endMs = endMs,
        )
        stopPreview()
        onProgress(0f)

        val uri = Uri.parse(draft.sourceUri)
        val player = MediaPlayer()
        val playerVolume = effectivePreviewVolume(volume)
        previewPlayer = player
        player.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setVolume(playerVolume, playerVolume)
            if (selection.startMs > 0L) {
                mediaPlayer.seekTo(selection.startMs.toInt())
            }
            mediaPlayer.start()
            val duration = max(250L, selection.endMs - selection.startMs)
            startPreviewProgressUpdates(
                player = mediaPlayer,
                startMs = selection.startMs,
                durationMs = duration,
                onProgress = onProgress,
            )
            previewStopRunnable = Runnable {
                onProgress(1f)
                stopPreview()
                onFinished()
            }.also { runnable ->
                previewHandler.postDelayed(runnable, duration)
            }
        }
        player.setOnCompletionListener {
            onProgress(1f)
            stopPreview()
            onFinished()
        }
        player.setOnErrorListener { mediaPlayer, _, _ ->
            mediaPlayer.release()
            previewPlayer = null
            onInfo("Не удалось проиграть выбранный фрагмент")
            onFinished()
            true
        }
        setDataSource(player, uri)
        player.prepareAsync()
    }

    fun stopPreview() {
        previewStopRunnable?.let(previewHandler::removeCallbacks)
        previewStopRunnable = null
        stopPreviewProgressUpdates()
        previewPlayer?.stopSafely()
        previewPlayer?.release()
        previewPlayer = null
    }

    private fun effectivePreviewVolume(volume: Float): Float {
        val safeVolume = volume.coerceIn(0f, 1f)
        return safeVolume * safeVolume
    }

    private fun startPreviewProgressUpdates(
        player: MediaPlayer,
        startMs: Long,
        durationMs: Long,
        onProgress: (Float) -> Unit,
    ) {
        stopPreviewProgressUpdates()
        previewProgressRunnable = object : Runnable {
            override fun run() {
                val progress = runCatching {
                    val elapsedMs = (player.currentPosition - startMs).coerceAtLeast(0)
                    (elapsedMs / durationMs.toFloat()).coerceIn(0f, 1f)
                }.getOrDefault(0f)
                onProgress(progress)
                val shouldContinue = previewPlayer === player &&
                    runCatching { player.isPlaying }.getOrDefault(false)
                if (shouldContinue) {
                    previewHandler.postDelayed(this, 80L)
                }
            }
        }.also(previewHandler::post)
    }

    private fun stopPreviewProgressUpdates() {
        previewProgressRunnable?.let(previewHandler::removeCallbacks)
        previewProgressRunnable = null
    }

    fun readWaveform(
        draft: EditableClipDraft,
        sampleCount: Int = 72,
    ): List<Float> = readWaveform(draft.sourceUri, sampleCount)

    fun readWaveform(
        uriString: String,
        sampleCount: Int = 72,
    ): List<Float> {
        val safeSampleCount = sampleCount.coerceIn(24, 160)
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        return try {
            val uri = Uri.parse(uriString)
            setDataSource(extractor, uri)
            val audioTrackIndex = findAudioTrackIndex(extractor)
                ?: throw IllegalStateException("В выбранном источнике нет аудиодорожки")
            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Не удалось определить формат аудио")
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
            } else {
                (readDurationMs(uri) * 1000L).coerceAtLeast(1L)
            }
            val buckets = FloatArray(safeSampleCount)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        val sampleSize = if (inputBuffer == null) {
                            -1
                        } else {
                            inputBuffer.clear()
                            extractor.readSampleData(inputBuffer, 0)
                        }

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime.coerceAtLeast(0L),
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else {
                            AudioFormat.ENCODING_PCM_16BIT
                        }
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outputIndex >= 0) {
                        decoder.getOutputBuffer(outputIndex)?.let { outputBuffer ->
                            if (bufferInfo.size > 0) {
                                writeWaveformBucket(
                                    outputBuffer = outputBuffer,
                                    bufferInfo = bufferInfo,
                                    pcmEncoding = pcmEncoding,
                                    durationUs = durationUs,
                                    buckets = buckets,
                                )
                            }
                        }
                        outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            normalizeWaveform(buckets)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
    }

    fun discardDraft(draft: EditableClipDraft?) {
        stopPreview()
        if (draft?.isTemporarySource == true) {
            deleteLocalUri(draft.sourceUri)
        }
    }

    fun deleteCustomSoundFile(uriString: String) {
        deleteLocalUri(uriString)
    }

    fun readDurationMs(uriString: String): Long {
        return readDurationMs(Uri.parse(uriString)).coerceAtLeast(1000L)
    }

    fun release() {
        stopPreview()
        cancelRecording()
    }

    private fun trimAudioTrack(
        sourceUri: Uri,
        outputFile: File,
        startUs: Long,
        endUs: Long,
    ) {
        val extractor = MediaExtractor()
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        try {
            setDataSource(extractor, sourceUri)
            val audioTrackIndex = findAudioTrackIndex(extractor)
                ?: throw IllegalStateException("В выбранном источнике нет аудиодорожки")
            extractor.selectTrack(audioTrackIndex)
            val trackFormat = extractor.getTrackFormat(audioTrackIndex)
            val muxerTrackIndex = muxer.addTrack(trackFormat)
            val maxInputSize = if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }
            val buffer = java.nio.ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var firstSampleTimeUs = -1L
            var muxerStarted = false

            while (true) {
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0L || sampleTimeUs >= endUs) {
                    break
                }

                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize <= 0) {
                    break
                }

                if (!muxerStarted) {
                    muxer.start()
                    muxerStarted = true
                    firstSampleTimeUs = sampleTimeUs
                }

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L)
                bufferInfo.flags = muxerSampleFlags(extractor.sampleFlags)
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            if (!muxerStarted) {
                throw IllegalStateException("Не удалось вырезать фрагмент. Попробуй выбрать диапазон побольше.")
            }
        } finally {
            runCatching { extractor.release() }
            runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun muxerSampleFlags(extractorFlags: Int): Int {
        var muxerFlags = 0
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            muxerFlags = muxerFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
            muxerFlags = muxerFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return muxerFlags
    }

    private fun writeWaveformBucket(
        outputBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        pcmEncoding: Int,
        durationUs: Long,
        buckets: FloatArray,
    ) {
        val duplicate = outputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        duplicate.position(bufferInfo.offset)
        duplicate.limit(bufferInfo.offset + bufferInfo.size)

        var peak = 0f
        when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                while (duplicate.remaining() >= 4) {
                    peak = max(peak, abs(duplicate.getFloat()).coerceIn(0f, 1f))
                }
            }

            AudioFormat.ENCODING_PCM_8BIT -> {
                while (duplicate.remaining() >= 1) {
                    val centered = duplicate.get().toInt() - 128
                    peak = max(peak, abs(centered) / 128f)
                }
            }

            else -> {
                while (duplicate.remaining() >= 2) {
                    peak = max(peak, abs(duplicate.getShort() / 32768f))
                }
            }
        }

        val bucketIndex = (
            bufferInfo.presentationTimeUs.coerceAtLeast(0L).toDouble() /
                durationUs.coerceAtLeast(1L).toDouble() *
                buckets.size
            ).toInt().coerceIn(0, buckets.lastIndex)
        buckets[bucketIndex] = max(buckets[bucketIndex], peak)
    }

    private fun normalizeWaveform(buckets: FloatArray): List<Float> {
        val peak = buckets.maxOrNull()?.takeIf { it > 0.001f } ?: return buckets.map { 0f }
        return buckets.map { (it / peak).coerceIn(0f, 1f) }
    }

    private fun ensureAudioTrackExists(uri: Uri) {
        val extractor = MediaExtractor()
        try {
            setDataSource(extractor, uri)
            if (findAudioTrackIndex(extractor) == null) {
                throw IllegalStateException("В этом видео не найден звук")
            }
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun findAudioTrackIndex(extractor: MediaExtractor): Int? {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                return index
            }
        }
        return null
    }

    private fun readDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            setDataSource(retriever, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun createDraftFile(
        prefix: String,
        extension: String,
    ): File {
        val directory = File(appContext.cacheDir, "audio_drafts").apply { mkdirs() }
        return File(directory, "${prefix}_${System.currentTimeMillis()}$extension")
    }

    private fun createUniqueSoundFile(
        displayName: String,
        extension: String = ".m4a",
        preserveExistingExtension: Boolean = false,
    ): File {
        val directory = File(appContext.filesDir, "custom_sounds").apply { mkdirs() }
        val normalizedName = normalizeDisplayName(
            rawLabel = displayName,
            extension = extension,
            preserveExistingExtension = preserveExistingExtension,
        )
        val desired = File(directory, normalizedName)
        if (!desired.exists()) {
            return desired
        }

        val dotIndex = normalizedName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) normalizedName.substring(0, dotIndex) else normalizedName
        val suffix = if (dotIndex > 0) normalizedName.substring(dotIndex) else extension
        return File(directory, "${baseName}_${System.currentTimeMillis()}$suffix")
    }

    private fun buildDisplayName(
        rawLabel: String,
        suffix: String = "",
    ): String {
        val base = rawLabel.substringBeforeLast('.').trim().ifBlank { "fall_ouch" }
        return displayNameWithoutAudioExtension(base + suffix)
    }

    private fun normalizeDisplayName(
        rawLabel: String,
        extension: String,
        preserveExistingExtension: Boolean,
    ): String {
        val safeExtension = extension.takeIf { it.startsWith(".") } ?: ".$extension"
        val withExtension = if (preserveExistingExtension && hasKnownAudioExtension(rawLabel)) {
            rawLabel
        } else {
            "${displayNameWithoutAudioExtension(rawLabel)}$safeExtension"
        }
        val sanitized = withExtension.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return sanitized.ifBlank { "fall_ouch_${System.currentTimeMillis()}$safeExtension" }
    }

    private fun hasKnownAudioExtension(rawLabel: String): Boolean {
        val lower = rawLabel.lowercase(Locale.US)
        return knownAudioExtensions.any { extension -> lower.endsWith(extension) }
    }

    private fun importExtension(
        uri: Uri,
        displayName: String,
    ): String {
        val lowerName = displayName.lowercase(Locale.US)
        knownAudioExtensions.firstOrNull { extension -> lowerName.endsWith(extension) }?.let {
            return it
        }
        val mimeExtension = appContext.contentResolver.getType(uri)
            ?.let { mimeType -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) }
            ?.takeIf { it.isNotBlank() }
        return mimeExtension?.let { ".$it" } ?: ".m4a"
    }

    private fun openInputStream(uri: Uri): java.io.InputStream {
        if (uri.scheme == "file") {
            return FileInputStream(uri.path ?: throw IllegalStateException("Не удалось прочитать локальный файл"))
        }
        return appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть выбранный звук")
    }

    private fun deleteLocalUri(uriString: String) {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            uri.path?.let(::File)?.delete()
        }
    }

    private fun setDataSource(
        extractor: MediaExtractor,
        uri: Uri,
    ) {
        if (uri.scheme == "file") {
            extractor.setDataSource(uri.path ?: throw IllegalStateException("Не удалось прочитать локальный файл"))
        } else {
            extractor.setDataSource(appContext, uri, null)
        }
    }

    private fun setDataSource(
        retriever: MediaMetadataRetriever,
        uri: Uri,
    ) {
        if (uri.scheme == "file") {
            retriever.setDataSource(uri.path ?: throw IllegalStateException("Не удалось прочитать локальный файл"))
        } else {
            retriever.setDataSource(appContext, uri)
        }
    }

    private fun setDataSource(
        player: MediaPlayer,
        uri: Uri,
    ) {
        if (uri.scheme == "file") {
            player.setDataSource(uri.path ?: throw IllegalStateException("Не удалось прочитать локальный файл"))
        } else {
            player.setDataSource(appContext, uri)
        }
    }

    private fun MediaPlayer.stopSafely() {
        runCatching {
            if (isPlaying) {
                stop()
            }
        }
    }

    private companion object {
        val knownAudioExtensions = listOf(
            ".mp3",
            ".wav",
            ".m4a",
            ".aac",
            ".ogg",
            ".flac",
            ".opus",
            ".amr",
        )
    }
}
