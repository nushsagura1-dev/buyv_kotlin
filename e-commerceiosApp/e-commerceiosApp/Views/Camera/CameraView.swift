import SwiftUI
import AVFoundation
import shared // KMP shared module — exposes CameraController + FilterInfo

/**
 * 2.18 — CameraView (iOS / SwiftUI)
 *
 * Full-screen camera UI with:
 *   • AVCaptureSession live preview (via UIViewControllerRepresentable)
 *   • Filter strip — horizontal ScrollView with tap-to-select chips
 *   • Record button — tap to start/stop video capture
 *   • Flip camera button
 *   • Camera + Microphone permission request on appear
 *
 * Navigation:
 *   When recording completes `onVideoReady(outputUri)` is called so the parent
 *   can push to a trim/publish screen.
 *
 * Usage:
 * ```swift
 * NavigationLink {
 *     CameraView { uri in
 *         // navigate to publish screen
 *     }
 * }
 * ```
 */
struct CameraView: View {

    // ── Dependencies ────────────────────────────────────────────
    /// Injected from parent or created inline; backed by AVCaptureSession.
    @StateObject private var viewModel = CameraViewModel()

    var onVideoReady: ((String) -> Void)?
    var onDismiss:    (() -> Void)?

    // ── Body ────────────────────────────────────────────────────
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if viewModel.permissionsGranted {
                // Live camera preview
                CameraPreviewRepresentable(controller: viewModel.cameraController)
                    .ignoresSafeArea()
            } else {
                permissionDeniedView
            }

            // Top bar
            VStack {
                topBar
                Spacer()
                bottomBar
            }
        }
        .statusBarHidden(true)
        .onAppear   { viewModel.requestPermissionsIfNeeded() }
        .onChange(of: viewModel.lastOutputUri) { uri in
            if let uri { onVideoReady?(uri) }
        }
    }

    // ── Top bar ─────────────────────────────────────────────────
    private var topBar: some View {
        HStack {
            Button(action: { onDismiss?() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(Color.black.opacity(0.35))
                    .clipShape(Circle())
            }
            Spacer()
            Button(action: viewModel.flipCamera) {
                Image(systemName: "camera.rotate")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(Color.black.opacity(0.35))
                    .clipShape(Circle())
            }
            .accessibilityIdentifier("flip_camera_button")
        }
        .padding(.horizontal, 16)
        .padding(.top, 56)
    }

    // ── Bottom bar ───────────────────────────────────────────────
    private var bottomBar: some View {
        VStack(spacing: 20) {
            filterStrip
            HStack(spacing: 40) {
                Spacer()
                recordButton
                Spacer()
            }
            Text(viewModel.isRecording ? "Tap to stop" : "Tap to record")
                .font(.caption)
                .foregroundColor(.white.opacity(0.7))
        }
        .padding(.bottom, 40)
    }

    // ── Filter strip ─────────────────────────────────────────────
    private var filterStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(viewModel.availableFilters, id: \.id) { filter in
                    FilterChipView(
                        filter     : filter,
                        isSelected : viewModel.selectedFilterId == filter.id
                    ) {
                        viewModel.selectFilter(filterId: filter.id)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // ── Record button ────────────────────────────────────────────
    private var recordButton: some View {
        Button(action: viewModel.toggleRecording) {
            ZStack {
                Circle()
                    .stroke(
                        viewModel.isRecording ? Color.red : Color.white,
                        lineWidth: 4
                    )
                    .frame(width: 78, height: 78)

                if viewModel.isRecording {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color.red)
                        .frame(width: 30, height: 30)
                } else {
                    Circle()
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 64, height: 64)
                }
            }
        }
        .accessibilityIdentifier("record_button")
    }

    // ── Permission denied placeholder ────────────────────────────
    private var permissionDeniedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.slash")
                .font(.system(size: 54))
                .foregroundColor(.white)
            Text("Camera access required")
                .foregroundColor(.white)
                .font(.headline)
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .foregroundColor(.orange)
        }
    }
}

// ─────────────────────────────────────────────
// Filter chip
// ─────────────────────────────────────────────

private struct FilterChipView: View {
    let filter:     FilterInfo
    let isSelected: Bool
    let onTap:      () -> Void

    var body: some View {
        Text(filter.name)
            .font(.caption)
            .foregroundColor(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background(
                isSelected ? Color.orange : Color.white.opacity(0.2)
            )
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(isSelected ? Color.clear : Color.white.opacity(0.4), lineWidth: 1)
            )
            .onTapGesture { onTap() }
            .accessibilityIdentifier("filter_chip_\(filter.id)")
    }
}

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

@MainActor
final class CameraViewModel: ObservableObject {

    let cameraController = CameraController()

    @Published var isRecording      = false
    @Published var selectedFilterId = "none"
    @Published var lastOutputUri:   String? = nil
    @Published var errorMessage:    String? = nil
    @Published var permissionsGranted = false

    var availableFilters: [FilterInfo] { cameraController.getAvailableFilters() }

    // ── Permission ────────────────────────────────────────────────
    func requestPermissionsIfNeeded() {
        Task {
            let camStatus  = AVCaptureDevice.authorizationStatus(for: .video)
            let micStatus  = AVCaptureDevice.authorizationStatus(for: .audio)

            var camGranted = camStatus  == .authorized
            var micGranted = micStatus  == .authorized

            if camStatus == .notDetermined {
                camGranted = await AVCaptureDevice.requestAccess(for: .video)
            }
            if micStatus == .notDetermined {
                micGranted = await AVCaptureDevice.requestAccess(for: .audio)
            }

            permissionsGranted = camGranted && micGranted
            if permissionsGranted {
                cameraController.startPreview()
            }
        }
    }

    // ── Actions ────────────────────────────────────────────────
    func toggleRecording() {
        if isRecording {
            cameraController.stopRecording()
            isRecording = false
        } else {
            isRecording = true
            Task {
                do {
                    // Collect CaptureState emissions from the KMP flow
                    let flow = try await cameraController.startRecording(maxDurationMs: 30_000)
                    for await state in flow {
                        switch state {
                        case is CaptureState.Recording:
                            break // already flagged
                        case let done as CaptureState.Completed:
                            isRecording    = false
                            lastOutputUri  = done.outputUri
                        case let err as CaptureState.Error:
                            isRecording   = false
                            errorMessage  = err.message
                        default:
                            break
                        }
                    }
                } catch {
                    isRecording  = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }

    func selectFilter(filterId: String) {
        selectedFilterId = filterId
        cameraController.applyFilter(filterId: filterId)
    }

    func flipCamera() {
        cameraController.flipCamera()
    }
}

// ─────────────────────────────────────────────
// UIViewControllerRepresentable — AVPreviewLayer
// ─────────────────────────────────────────────

/**
 * Wraps an AVCaptureVideoPreviewLayer inside a SwiftUI view using
 * UIViewControllerRepresentable so the preview fills the available space.
 */
private struct CameraPreviewRepresentable: UIViewControllerRepresentable {
    let controller: CameraController

    func makeUIViewController(context: Context) -> _CameraPreviewVC {
        let vc = _CameraPreviewVC()
        vc.previewLayer = controller.previewLayer
        return vc
    }

    func updateUIViewController(_ uiViewController: _CameraPreviewVC, context: Context) {
        uiViewController.previewLayer = controller.previewLayer
        uiViewController.layoutPreviewLayer()
    }
}

final class _CameraPreviewVC: UIViewController {
    var previewLayer: AVCaptureVideoPreviewLayer?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        layoutPreviewLayer()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        layoutPreviewLayer()
    }

    func layoutPreviewLayer() {
        guard let layer = previewLayer else { return }
        if layer.superlayer == nil {
            view.layer.insertSublayer(layer, at: 0)
        }
        layer.frame = view.bounds
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

#Preview {
    CameraView()
}
