package com.project.e_commerce.domain.platform

import kotlinx.coroutines.flow.Flow

/**
 * 2.18 — Caméra In-App avec Filtres (Phase 2)
 *
 * Cross-platform contract for accessing the device camera, applying real-time
 * GPU filters and recording video clips for the Reels / Sound-Sync feed.
 *
 * Android implementation: CameraX (camera2 backend) + GPUImage for filters
 * iOS implementation    : AVCaptureSession + CIFilter for filters
 *
 * Registration in Koin:
 *   Android: `single { CameraController(get()) }` (Context injected via androidContext())
 *   iOS    : `single { CameraController() }`
 *
 * Lifecycle:
 *   1. Call [startPreview] once the preview surface/view is ready.
 *   2. Optionally call [applyFilter] to change the active GPU filter.
 *   3. Call [startRecording] to begin capture; collect the returned [Flow].
 *   4. Call [stopRecording] to finalize; the flow emits [CaptureState.Completed].
 *   5. Call [release] in onStop / ScenePhase.background to free GPU/codec resources.
 */
expect class CameraController {

    /**
     * `true` when a physical camera is available on this device.
     * Always `true` on phones; may be `false` on emulators without camera support.
     */
    val isSupported: Boolean

    /**
     * Start the camera preview stream.
     * Must be called after the preview surface has been attached to the controller.
     * No-op if preview is already running.
     */
    fun startPreview()

    /**
     * Stop the camera preview stream and release codec resources.
     * The preview surface/view can be safely detached after this call.
     */
    fun stopPreview()

    /**
     * Toggle between the front and rear cameras.
     * Preview will restart automatically with the selected lens.
     */
    fun flipCamera()

    /**
     * Apply a named GPU filter to the live preview and recording.
     *
     * @param filterId One of the IDs returned by [getAvailableFilters], or `""` to remove filters.
     */
    fun applyFilter(filterId: String)

    /**
     * Returns the list of GPU/image filters available on this platform.
     * Filters map to GPUImage preset effects on Android,
     * and CIFilter preset names on iOS.
     */
    fun getAvailableFilters(): List<FilterInfo>

    /**
     * Begins video recording and returns a cold [Flow] that emits [CaptureState] events.
     *
     * - Emits [CaptureState.Recording] immediately when capture starts.
     * - Emits [CaptureState.Completed] with the output URI when [stopRecording] is called
     *   or [maxDurationMs] is reached.
     * - Emits [CaptureState.Error] on codec / permission failure.
     *
     * @param maxDurationMs Hard cap on recording length in milliseconds (default: 30 s).
     */
    suspend fun startRecording(maxDurationMs: Long = 30_000L): Flow<CaptureState>

    /**
     * Stops an active recording session.
     * The [startRecording] flow will emit [CaptureState.Completed] with the output URI.
     * No-op when not recording.
     */
    fun stopRecording()

    /**
     * Releases all camera and codec resources.
     * Must be called when the screen owning the preview is destroyed.
     * The controller instance cannot be reused after this call.
     */
    fun release()
}

// ─────────────────────────────────────────────
// Supporting data types (shared / commonMain)
// ─────────────────────────────────────────────

/**
 * Metadata for a single GPU / image filter selectable in the filter strip.
 *
 * @param id          Stable identifier used in [CameraController.applyFilter].
 * @param name        Human-readable display name (e.g. "Warm", "Fade").
 * @param previewUrl  Optional thumbnail URL for the filter chip image.
 */
data class FilterInfo(
    val id: String,
    val name: String,
    val previewUrl: String = ""
)

/**
 * State emitted by the [CameraController.startRecording] flow.
 */
sealed class CaptureState {

    /** Recording has started successfully. */
    object Recording : CaptureState()

    /**
     * Recording finished and the output file is ready.
     *
     * @param outputUri Absolute local URI/path of the recorded video file.
     * @param durationMs Actual recorded duration in milliseconds.
     */
    data class Completed(
        val outputUri: String,
        val durationMs: Long = 0L
    ) : CaptureState()

    /**
     * Recording or preview encountered an unrecoverable error.
     *
     * @param message Developer-facing diagnostic message.
     */
    data class Error(val message: String) : CaptureState()
}
