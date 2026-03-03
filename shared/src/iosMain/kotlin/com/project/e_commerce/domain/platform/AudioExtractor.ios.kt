package com.project.e_commerce.domain.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetExportPresetAppleM4A
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatus
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRange
import platform.CoreMedia.CMTimeRangeMake
import platform.Foundation.NSDate
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import kotlin.coroutines.resume

/**
 * iOS implementation of [AudioExtractor] using [AVAssetExportSession].
 *
 * Outputs an Apple M4A file (.m4a / AAC) to the system temp directory.
 * The file is NOT deleted automatically — callers should clean up after upload.
 *
 * Registered in Koin as `single { AudioExtractor() }` (no constructor deps on iOS).
 */
actual class AudioExtractor {

    actual val isSupported: Boolean = true

    actual suspend fun extractAudio(
        videoUrl: String,
        maxDurationSeconds: Int
    ): AudioExtractionResult = suspendCancellableCoroutine { cont ->

        val sourceUrl = NSURL.URLWithString(videoUrl)
            ?: run {
                cont.resume(AudioExtractionResult.Error("Invalid video URL: $videoUrl"))
                return@suspendCancellableCoroutine
            }

        val asset = AVURLAsset.assetWithURL(sourceUrl) as? AVURLAsset
            ?: run {
                cont.resume(AudioExtractionResult.Error("Cannot load AVURLAsset"))
                return@suspendCancellableCoroutine
            }

        // Verify audio track exists
        @Suppress("UNCHECKED_CAST")
        val audioTracks = asset.tracksWithMediaType(AVMediaTypeAudio) as List<*>
        if (audioTracks.isEmpty()) {
            cont.resume(AudioExtractionResult.Error("No audio track in video"))
            return@suspendCancellableCoroutine
        }

        // Prepare output path
        val outputPath = NSTemporaryDirectory() +
                "buyv_audio_${NSDate().timeIntervalSince1970}.m4a"
        val outputUrl = NSURL.fileURLWithPath(outputPath)

        val session = AVAssetExportSession.exportSessionWithAsset(
            asset,
            presetName = AVAssetExportPresetAppleM4A
        ) ?: run {
            cont.resume(AudioExtractionResult.Error("Cannot create AVAssetExportSession"))
            return@suspendCancellableCoroutine
        }

        session.outputURL = outputUrl
        session.outputFileType = AVFileTypeAppleM4A

        // Clamp to maxDurationSeconds
        val maxCMTime = CMTimeMake(value = maxDurationSeconds.toLong(), timescale = 1)
        session.timeRange = CMTimeRangeMake(start = platform.CoreMedia.kCMTimeZero, duration = maxCMTime)

        session.exportAsynchronouslyWithCompletionHandler {
            when (session.status) {
                AVAssetExportSessionStatus.AVAssetExportSessionStatusCompleted -> {
                    val durationSec = minOf(
                        asset.duration.value.toDouble() / asset.duration.timescale.toDouble(),
                        maxDurationSeconds.toDouble()
                    )
                    cont.resume(
                        AudioExtractionResult.Success(
                            filePath = outputPath,
                            durationMs = (durationSec * 1000).toLong(),
                            mimeType = "audio/m4a"
                        )
                    )
                }
                else -> {
                    val msg = session.error?.localizedDescription
                        ?: "Export failed (status=${session.status})"
                    cont.resume(AudioExtractionResult.Error(msg))
                }
            }
        }

        cont.invokeOnCancellation { session.cancelExport() }
    }
}
