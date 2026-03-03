package com.project.e_commerce.domain.platform

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * 2.18 — Android actual: CameraController
 *
 * Backed by CameraX (camera-camera2) for preview + video capture,
 * and GPUImage for real-time GPU filter rendering.
 *
 * Koin registration:
 *   `single { CameraController(androidContext()) }`
 *
 * Surface attachment:
 *   Call [attachPreviewView] after creation, before [startPreview].
 */
actual class CameraController(private val context: Context) {

    private val tag = "CameraController"

    actual val isSupported: Boolean
        get() = context.packageManager
            .hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)

    // CameraX
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var previewUseCase: Preview? = null
    private var previewView: PreviewView? = null
    private var currentSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // GPUImage filter
    private var gpuImage: GPUImage? = null
    private var currentFilterId: String = ""

    // ──────────────────────────────────
    // Public surface attachment
    // ──────────────────────────────────

    /**
     * Must be called once the [PreviewView] is inflated / attached to the window.
     * Typically called from the AndroidView factory lambda in [CameraScreen].
     */
    fun attachPreviewView(view: PreviewView) {
        previewView = view
        gpuImage = GPUImage(context)
    }

    // ──────────────────────────────────
    // Preview
    // ──────────────────────────────────

    actual fun startPreview() {
        val view = previewView ?: run {
            Log.w(tag, "startPreview: no PreviewView attached — call attachPreviewView first")
            return
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindCameraUseCases(view)
            } catch (e: Exception) {
                Log.e(tag, "startPreview: failed to get CameraProvider", e)
            }
        }, context.mainExecutor)
    }

    actual fun stopPreview() {
        cameraProvider?.unbindAll()
    }

    actual fun flipCamera() {
        currentSelector = if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        previewView?.let { bindCameraUseCases(it) }
    }

    // ──────────────────────────────────
    // Filters (GPUImage)
    // ──────────────────────────────────

    actual fun applyFilter(filterId: String) {
        currentFilterId = filterId
        gpuImage?.setFilter(resolveGpuImageFilter(filterId))
    }

    actual fun getAvailableFilters(): List<FilterInfo> = listOf(
        FilterInfo("none",    "Original"),
        FilterInfo("warm",    "Warm"),
        FilterInfo("cool",    "Cool"),
        FilterInfo("fade",    "Fade"),
        FilterInfo("vivid",   "Vivid"),
        FilterInfo("noir",    "Noir"),
        FilterInfo("chrome",  "Chrome"),
        FilterInfo("instant", "Instant")
    )

    // ──────────────────────────────────
    // Recording
    // ──────────────────────────────────

    @SuppressLint("MissingPermission")
    actual suspend fun startRecording(maxDurationMs: Long): Flow<CaptureState> = callbackFlow {
        val vc = videoCapture ?: run {
            // Ensure use cases are bound
            val view = previewView
            if (view != null) bindCameraUseCases(view)
            videoCapture ?: run {
                trySend(CaptureState.Error("VideoCapture not ready"))
                close()
                return@callbackFlow
            }
        }

        val outputFile = createOutputFile()
        val outputOptions = FileOutputOptions.Builder(outputFile).apply {
            setDurationLimitMillis(maxDurationMs)
        }.build()

        trySend(CaptureState.Recording)

        val startTimeMs = System.currentTimeMillis()

        activeRecording = vc.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(cameraExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        val durationMs = System.currentTimeMillis() - startTimeMs
                        if (event.hasError()) {
                            trySend(CaptureState.Error("Recording error: ${event.cause?.message}"))
                        } else {
                            trySend(CaptureState.Completed(
                                outputUri = event.outputResults.outputUri.toString(),
                                durationMs = durationMs
                            ))
                        }
                        close()
                    }
                    else -> { /* VideoRecordEvent.Start, Status — ignore */ }
                }
            }

        awaitClose { stopRecording() }
    }

    actual fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    actual fun release() {
        stopRecording()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        gpuImage = null
        previewView = null
    }

    // ──────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────

    private fun bindCameraUseCases(view: PreviewView) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(view.surfaceProvider)
        }
        previewUseCase = preview

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        val vc = VideoCapture.withOutput(recorder)
        videoCapture = vc

        provider.unbindAll()
        try {
            provider.bindToLifecycle(
                view.context as androidx.lifecycle.LifecycleOwner,
                currentSelector,
                preview,
                vc
            )
        } catch (e: Exception) {
            Log.e(tag, "bindCameraUseCases: failed", e)
        }
    }

    private fun createOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(context.cacheDir, "buyv_camera").apply { mkdirs() }
        return File(dir, "VID_$timestamp.mp4")
    }

    private fun resolveGpuImageFilter(filterId: String): GPUImageFilter = when (filterId) {
        "warm"    -> GPUImageRGBFilter(1.1f, 0.95f, 0.85f)
        "cool"    -> GPUImageRGBFilter(0.85f, 0.95f, 1.1f)
        "fade"    -> GPUImageOpacityFilter(0.85f)
        "vivid"   -> GPUImageSaturationFilter(1.6f)
        "noir"    -> GPUImageGrayscaleFilter()
        "chrome"  -> GPUImageContrastFilter(1.4f)
        "instant" -> GPUImageVignetteFilter()
        else      -> GPUImageFilter() // identity / no-op
    }
}
