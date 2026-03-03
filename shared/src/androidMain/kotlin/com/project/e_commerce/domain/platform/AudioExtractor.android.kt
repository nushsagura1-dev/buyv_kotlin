package com.project.e_commerce.domain.platform

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Android implementation of [AudioExtractor] using [MediaExtractor] + [MediaMuxer].
 *
 * Demux-only (no re-encode): copies the existing audio track directly → fast & lossless.
 * Output format: MPEG-4 container (.mp4) with the original audio codec preserved.
 *
 * @param context Android application context (injected by Koin via `androidContext()`).
 */
actual class AudioExtractor(private val context: Context) {

    actual val isSupported: Boolean = true

    actual suspend fun extractAudio(
        videoUrl: String,
        maxDurationSeconds: Int
    ): AudioExtractionResult = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(videoUrl)

            // ── 1. Locate the first audio track ──────────────────────────────────────
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext AudioExtractionResult.Error("No audio track found in video")
            }
            extractor.selectTrack(audioTrackIndex)

            // ── 2. Prepare output file ────────────────────────────────────────────────
            val outputFile = File(
                context.cacheDir,
                "buyv_audio_${System.currentTimeMillis()}.mp4"
            )
            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val outputTrack = muxer.addTrack(audioFormat)
            muxer.start()

            // ── 3. Copy audio samples up to maxDurationSeconds ────────────────────────
            val maxDurationUs = maxDurationSeconds * 1_000_000L
            val buffer = ByteBuffer.allocate(1 * 1024 * 1024) // 1 MB
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break                      // end of stream
                if (extractor.sampleTime > maxDurationUs) break      // duration cap
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags.toInt()
                muxer.writeSampleData(outputTrack, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()

            // ── 4. Compute actual duration ────────────────────────────────────────────
            val rawDurationUs: Long = if (audioFormat.containsKey(MediaFormat.KEY_DURATION))
                audioFormat.getLong(MediaFormat.KEY_DURATION) else 0L
            val durationMs = minOf(rawDurationUs / 1_000L, maxDurationSeconds * 1000L)
            val mimeType = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4"

            AudioExtractionResult.Success(
                filePath = outputFile.absolutePath,
                durationMs = durationMs,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            AudioExtractionResult.Error(e.message ?: "Audio extraction failed")
        } finally {
            extractor.release()
        }
    }
}
