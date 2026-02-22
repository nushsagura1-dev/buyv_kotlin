import SwiftUI
import Shared

/// Sound/Music page — equivalent to Android SoundPageScreen
/// Shows all reels using a specific sound/music track
struct SoundPageView: View {
    let soundName: String
    let soundUrl: String
    
    @StateObject private var viewModel = ReelsViewModel()
    @State private var isPlaying = false
    @State private var isSaved = false
    @State private var showShareSheet = false
    @State private var rotationAngle: Double = 0
    @State private var showReportAlert = false
    @State private var showReportSuccess = false
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 20) {
                    // Sound Header
                    soundHeader
                    
                    // Action Buttons
                    actionButtons
                    
                    // Stats
                    statsRow
                    
                    // Audio Waveform visual
                    if isPlaying {
                        waveformView
                            .transition(.opacity.combined(with: .scale))
                    }
                    
                    // Error state
                    if let error = viewModel.errorMessage {
                        VStack(spacing: 12) {
                            Image(systemName: "exclamationmark.triangle")
                                .font(.system(size: 40))
                                .foregroundColor(.orange)
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                            Button("Try Again") {
                                viewModel.loadReels()
                            }
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(AppColors.primary)
                        }
                        .padding(.vertical, 20)
                    }
                    
                    // Reels Grid
                    reelsGrid
                }
                .padding()
            }
            .refreshable {
                viewModel.loadReels()
            }
        }
        .navigationTitle("Sound")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: { isSaved.toggle() }) {
                        Label(isSaved ? "Remove from Saved" : "Save Sound", systemImage: isSaved ? "bookmark.fill" : "bookmark")
                    }
                    Button(action: { showShareSheet = true }) {
                        Label("Share Sound", systemImage: "square.and.arrow.up")
                    }
                    Button(action: { showReportAlert = true }) {
                        Label("Report", systemImage: "exclamationmark.triangle")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(.primary)
                }
            }
        }
        .onAppear {
            viewModel.loadReels()
        }
        .alert("Report Sound", isPresented: $showReportAlert) {
            Button("Report", role: .destructive) {
                withAnimation { showReportSuccess = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                    withAnimation { showReportSuccess = false }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Report this sound for inappropriate content? Our team will review it.")
        }
        .overlay(
            Group {
                if showReportSuccess {
                    VStack {
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Report submitted")
                                .font(.subheadline)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color(.systemBackground))
                        .cornerRadius(24)
                        .shadow(radius: 8)
                        .padding(.top, 8)
                        Spacer()
                    }
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
            },
            alignment: .top
        )
    }
    
    // MARK: - Sound Header
    private var soundHeader: some View {
        VStack(spacing: 16) {
            // Album art / Sound icon with spinning animation
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppColors.primary, AppColors.primary.opacity(0.4), Color.purple.opacity(0.6)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 120, height: 120)
                    .shadow(color: AppColors.primary.opacity(0.3), radius: 12, y: 4)
                
                // Inner disc
                Circle()
                    .fill(Color.white.opacity(0.15))
                    .frame(width: 50, height: 50)
                
                // Center hole
                Circle()
                    .fill(AppColors.background)
                    .frame(width: 16, height: 16)
                
                Image(systemName: "music.note")
                    .font(.system(size: 32))
                    .foregroundColor(.white)
                    .offset(x: 20, y: -20)
                
                // Spinning ring when playing
                if isPlaying {
                    Circle()
                        .stroke(
                            AngularGradient(colors: [.white.opacity(0.6), .clear, .white.opacity(0.6)], center: .center),
                            lineWidth: 3
                        )
                        .frame(width: 130, height: 130)
                        .rotationEffect(.degrees(rotationAngle))
                        .onAppear {
                            withAnimation(.linear(duration: 4).repeatForever(autoreverses: false)) {
                                rotationAngle = 360
                            }
                        }
                }
            }
            
            // Sound name + artist
            VStack(spacing: 4) {
                Text(soundName)
                    .font(.title2)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("Original Sound")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            // Play / Pause button
            Button(action: {
                withAnimation(.spring(response: 0.4)) {
                    isPlaying.toggle()
                    if !isPlaying { rotationAngle = 0 }
                }
            }) {
                HStack(spacing: 8) {
                    Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 14))
                    Text(isPlaying ? "Pause" : "Play Sound")
                        .fontWeight(.semibold)
                }
                .padding(.horizontal, 32)
                .padding(.vertical, 12)
                .background(AppColors.primary)
                .foregroundColor(.white)
                .cornerRadius(24)
                .shadow(color: AppColors.primary.opacity(0.3), radius: 6, y: 3)
            }
        }
        .padding(.vertical, 8)
    }
    
    // MARK: - Action Buttons
    private var actionButtons: some View {
        HStack(spacing: 0) {
            actionButton(icon: isSaved ? "bookmark.fill" : "bookmark", label: isSaved ? "Saved" : "Save", color: isSaved ? AppColors.primary : .primary) {
                withAnimation(.spring(response: 0.3)) {
                    isSaved.toggle()
                }
            }
            
            actionButton(icon: "square.and.arrow.up", label: "Share", color: .primary) {
                showShareSheet = true
            }
            
            actionButton(icon: "music.note.list", label: "Use Sound", color: AppColors.primary) {
                // Navigate to create post with this sound
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    private func actionButton(icon: String, label: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(color)
                Text(label)
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
    }
    
    // MARK: - Stats Row
    private var statsRow: some View {
        HStack(spacing: 0) {
            statItem(value: "\(viewModel.reels.count)", label: "Videos", icon: "play.rectangle.fill")
            Divider().frame(height: 40)
            statItem(value: formatCount(viewModel.reels.count * 150), label: "Views", icon: "eye.fill")
            Divider().frame(height: 40)
            statItem(value: formatCount(viewModel.reels.count * 30), label: "Likes", icon: "heart.fill")
            Divider().frame(height: 40)
            statItem(value: formatCount(viewModel.reels.count * 5), label: "Shares", icon: "arrow.turn.up.right")
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    private func statItem(value: String, label: String, icon: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(AppColors.primary)
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
    
    // MARK: - Waveform Visualization
    private var waveformView: some View {
        VStack(spacing: 6) {
            HStack(spacing: 3) {
                ForEach(0..<30, id: \.self) { i in
                    WaveBar(index: i, isPlaying: isPlaying)
                }
            }
            .frame(height: 40)
            .padding(.horizontal, 8)
            
            Text("Now Playing")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Reels Grid
    private var reelsGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Videos using this sound")
                    .font(.headline)
                Spacer()
                Text("\(viewModel.reels.count)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
            }
            
            if viewModel.isLoading {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        ProgressView()
                        Text("Loading videos...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                }
                .padding(.vertical, 40)
            } else if viewModel.reels.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "music.note.list")
                        .font(.system(size: 40))
                        .foregroundColor(.gray)
                    Text("No videos with this sound yet")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text("Be the first to create one!")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
            } else {
                LazyVGrid(columns: [
                    GridItem(.flexible(), spacing: 4),
                    GridItem(.flexible(), spacing: 4),
                    GridItem(.flexible(), spacing: 4)
                ], spacing: 4) {
                    ForEach(viewModel.reels, id: \.id) { reel in
                        SoundReelThumbnail(reel: reel)
                    }
                }
            }
        }
    }
    
    // MARK: - Helpers
    private func formatCount(_ count: Int) -> String {
        if count >= 1_000_000 {
            return String(format: "%.1fM", Double(count) / 1_000_000)
        } else if count >= 1_000 {
            return String(format: "%.1fK", Double(count) / 1_000)
        }
        return "\(count)"
    }
}

// MARK: - Wave Bar Animation
struct WaveBar: View {
    let index: Int
    let isPlaying: Bool
    
    @State private var height: CGFloat = 5
    
    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(
                LinearGradient(
                    colors: [AppColors.primary, AppColors.primary.opacity(0.5)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .frame(width: 4, height: height)
            .onAppear {
                if isPlaying {
                    withAnimation(
                        .easeInOut(duration: Double.random(in: 0.3...0.7))
                        .repeatForever(autoreverses: true)
                        .delay(Double(index) * 0.05)
                    ) {
                        height = CGFloat.random(in: 8...35)
                    }
                }
            }
            .onChange(of: isPlaying) { playing in
                if playing {
                    withAnimation(
                        .easeInOut(duration: Double.random(in: 0.3...0.7))
                        .repeatForever(autoreverses: true)
                    ) {
                        height = CGFloat.random(in: 8...35)
                    }
                } else {
                    withAnimation(.easeOut(duration: 0.3)) {
                        height = 5
                    }
                }
            }
    }
}

// MARK: - Sound Reel Thumbnail
struct SoundReelThumbnail: View {
    let reel: Product
    
    var body: some View {
        NavigationLink(destination: ProductDetailView(productId: String(reel.id))) {
            ZStack(alignment: .bottomLeading) {
                // Thumbnail
                if !reel.imageUrl.isEmpty, let url = URL(string: reel.imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        default:
                            Rectangle().fill(Color.gray.opacity(0.1))
                        }
                    }
                } else {
                    Rectangle()
                        .fill(Color.gray.opacity(0.15))
                        .overlay(
                            Image(systemName: "play.rectangle.fill")
                                .foregroundColor(.gray)
                        )
                }
                
                // Gradient overlay
                LinearGradient(
                    colors: [.clear, .black.opacity(0.5)],
                    startPoint: .center,
                    endPoint: .bottom
                )
                
                // Play icon overlay
                VStack {
                    Spacer()
                    HStack(spacing: 4) {
                        Image(systemName: "play.fill")
                            .font(.caption2)
                        Text("Watch")
                            .font(.caption2)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(Color.black.opacity(0.5))
                    .cornerRadius(4)
                    .padding(4)
                }
                
                // Music note badge
                VStack {
                    HStack {
                        Spacer()
                        Image(systemName: "music.note")
                            .font(.caption2)
                            .foregroundColor(.white)
                            .padding(4)
                            .background(AppColors.primary.opacity(0.8))
                            .clipShape(Circle())
                            .padding(4)
                    }
                    Spacer()
                }
            }
            .frame(height: 160)
            .clipped()
            .cornerRadius(8)
        }
    }
}
