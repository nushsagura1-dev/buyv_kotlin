package com.project.e_commerce.domain.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.UIKit.UIDevice
import platform.darwin.dispatch_get_main_queue

/**
 * 2.18 — iOS actual: CameraController
 *
 * Backed by AVCaptureSession (camera2 parity) and CIFilter for GPU filters.
 *
 * Koin registration:
 *   `single { CameraController() }`
 *
 * Preview layer attachment:
 *   After creation call the companion [previewLayer] property and add it as a sublayer
 *   to the UIView backing the SwiftUI/UIKit camera preview container.
 */
actual class CameraController {

    actual val isSupported: Boolean
        get() = UIDevice.currentDevice.model.lowercase()
            .let { !it.contains("simulator") }
                && AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null

    // ──────────────────────────────────
    // AVFoundation session
    // ──────────────────────────────────

    val session = AVCaptureSession()
    val previewLayer: AVCaptureVideoPreviewLayer by lazy {
        AVCaptureVideoPreviewLayer(session = session).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }

    private var currentPosition = AVCaptureDevicePositionBack
    private var movieOutput: AVCaptureMovieFileOutput? = null
    private var currentFilterId: String = ""
    private var recordingDelegate: _RecordingDelegate? = null

    // ──────────────────────────────────
    // Preview
    // ──────────────────────────────────

    actual fun startPreview() {
        if (session.isRunning) return
        configureSession(position = currentPosition)
        dispatchMain { session.startRunning() }
    }

    actual fun stopPreview() {
        dispatchMain { session.stopRunning() }
    }

    actual fun flipCamera() {
        currentPosition = if (currentPosition == AVCaptureDevicePositionBack) {
            AVCaptureDevicePositionFront
        } else {
            AVCaptureDevicePositionBack
        }
        configureSession(position = currentPosition)
    }

    // ──────────────────────────────────
    // Filters (CIFilter names)
    // ──────────────────────────────────

    actual fun applyFilter(filterId: String) {
        currentFilterId = filterId
        // CIFilter is applied in the preview rendering layer (CameraView.swift)
        // via AVCaptureVideoDataOutput + AVVideoCompositionCoreAnimationTool
    }

    actual fun getAvailableFilters(): List<FilterInfo> = listOf(
        FilterInfo("none",          "Original"),
        FilterInfo("CIPhotoEffectWarm",     "Warm",    ""),
        FilterInfo("CIPhotoEffectCool",     "Cool",    ""),
        FilterInfo("CIPhotoEffectFade",     "Fade",    ""),
        FilterInfo("CIColorControls",       "Vivid",   ""),
        FilterInfo("CIPhotoEffectNoir",     "Noir",    ""),
        FilterInfo("CIPhotoEffectChrome",   "Chrome",  ""),
        FilterInfo("CIPhotoEffectInstant",  "Instant", "")
    )

    // ──────────────────────────────────
    // Recording
    // ──────────────────────────────────

    actual suspend fun startRecording(maxDurationMs: Long): Flow<CaptureState> = callbackFlow {
        val output = AVCaptureMovieFileOutput().also {
            if (maxDurationMs > 0L) {
                it.maxRecordedDuration = CMTimeMakeWithSeconds(
                    maxDurationMs / 1000.0, 600
                )
            }
        }

        if (!session.canAddOutput(output)) {
            trySend(CaptureState.Error("Cannot add movie output to session"))
            close()
            return@callbackFlow
        }
        session.addOutput(output)
        movieOutput = output

        val url = outputFileUrl()
        val delegate = _RecordingDelegate(
            onStart  = { trySend(CaptureState.Recording) },
            onFinish = { uri, err ->
                if (err != null) {
                    trySend(CaptureState.Error(err.localizedDescription))
                } else {
                    trySend(CaptureState.Completed(outputUri = uri))
                }
                close()
            }
        ).also { recordingDelegate = it }

        output.startRecordingToOutputFileURL(url, recordingDelegate = delegate)

        awaitClose { stopRecording() }
    }

    actual fun stopRecording() {
        movieOutput?.stopRecording()
        movieOutput = null
        recordingDelegate = null
    }

    actual fun release() {
        stopRecording()
        stopPreview()
        session.inputs.forEach  { session.removeInput(it  as AVCaptureInput)  }
        session.outputs.forEach { session.removeOutput(it as AVCaptureOutput) }
    }

    // ──────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────

    private fun configureSession(position: AVCaptureDevicePosition) {
        session.beginConfiguration()
        // Remove existing video inputs
        session.inputs.forEach { input ->
            if ((input as? AVCaptureDeviceInput)?.device?.hasMediaType(AVMediaTypeVideo) == true) {
                session.removeInput(input as AVCaptureDeviceInput)
            }
        }
        val device = AVCaptureDevice.defaultDeviceWithDeviceType(
            AVCaptureDeviceTypeBuiltInWideAngleCamera,
            AVMediaTypeVideo,
            position
        ) ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

        device?.let { dev ->
            try {
                val input = AVCaptureDeviceInput(device = dev, error = null)
                if (session.canAddInput(input)) {
                    session.addInput(input)
                }
            } catch (_: Exception) {}
        }

        // Audio
        if (session.inputs.none { (it as? AVCaptureDeviceInput)?.device?.hasMediaType(AVMediaTypeAudio) == true }) {
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)?.let { mic ->
                try {
                    val micInput = AVCaptureDeviceInput(device = mic, error = null)
                    if (session.canAddInput(micInput)) session.addInput(micInput)
                } catch (_: Exception) {}
            }
        }

        session.commitConfiguration()
    }

    private fun outputFileUrl(): NSURL {
        val tmp = NSTemporaryDirectory()
        val ts  = NSDate().timeIntervalSince1970.toLong()
        return NSURL.fileURLWithPath("${tmp}buyv_rec_$ts.mov")
    }

    private fun dispatchMain(block: () -> Unit) {
        dispatch_get_main_queue().let { q ->
            platform.darwin.dispatch_async(q) { block() }
        }
    }
}

// ─────────────────────────────────────────────
// AVCaptureFileOutputRecordingDelegate shim
// ─────────────────────────────────────────────

private class _RecordingDelegate(
    private val onStart:  ()                        -> Unit,
    private val onFinish: (uri: String, err: NSError?) -> Unit
) : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureFileOutput,
        didStartRecordingToOutputFileAtURL: NSURL,
        fromConnections: List<*>
    ) {
        onStart()
    }

    override fun captureOutput(
        output: AVCaptureFileOutput,
        didFinishRecordingToOutputFileAtURL: NSURL,
        fromConnections: List<*>,
        error: NSError?
    ) {
        onFinish(didFinishRecordingToOutputFileAtURL.path ?: "", error)
    }
}
