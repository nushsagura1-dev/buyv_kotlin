package com.project.e_commerce.domain.platform

/**
 * 2.17 — Sound Reuse / Audio Extraction
 *
 * Cross-platform contract for extracting audio tracks from video files.
 *
 * Android implementation: [MediaExtractor] + [MediaMuxer] → outputs .mp4 audio track
 * iOS implementation    : AVAssetExportSession             → outputs .m4a audio track
 *
 * Registration in Koin:
 *   Android: `single { AudioExtractor(get()) }` (Context injected via androidContext())
 *   iOS    : `single { AudioExtractor() }`
 */
expect class AudioExtractor {

    /**
     * Whether audio extraction is supported on this platform.
     * Always `true` on Android and iOS; may be `false` on platforms without camera/media APIs.
     */
    val isSupported: Boolean

    /**
     * Extracts the audio track from a remote or local video URL and writes it
     * to a temporary file in the device's cache directory.
     *
     * @param videoUrl   HTTP(S) URL or local file path of the source video.
     * @param maxDurationSeconds Maximum duration of audio to extract (≤ 60 recommended).
     * @return [AudioExtractionResult.Success] with output path on success, or an error/unsupported result.
     */
    suspend fun extractAudio(videoUrl: String, maxDurationSeconds: Int = 60): AudioExtractionResult
}

/**
 * Result of an [AudioExtractor.extractAudio] call.
 */
sealed class AudioExtractionResult {

    /** Audio was successfully extracted. */
    data class Success(
        /** Absolute path to the extracted audio file in device cache. */
        val filePath: String,
        /** Duration of the extracted segment in milliseconds. */
        val durationMs: Long,
        /** MIME type of the output file, e.g. `"audio/mp4"` or `"audio/m4a"`. */
        val mimeType: String
    ) : AudioExtractionResult()

    /** Extraction failed with a diagnostic message. */
    data class Error(val message: String) : AudioExtractionResult()

    /** Platform does not support audio extraction (e.g. desktop/web previews). */
    object Unsupported : AudioExtractionResult()
}
