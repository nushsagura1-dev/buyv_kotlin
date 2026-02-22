import SwiftUI
import Shared

struct RecentlyViewedView: View {
    @StateObject private var viewModel = RecentlyViewedViewModel()
    @State private var showClearConfirmation = false
    
    private let columns = [
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2)
    ]
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading history...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            } else if let error = viewModel.errorMessage {
                errorState(message: error)
            } else if viewModel.recentProducts.isEmpty {
                emptyState
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Header info
                        headerSection
                        
                        // Products Grid
                        LazyVGrid(columns: columns, spacing: 2) {
                            ForEach(viewModel.recentProducts, id: \.id) { product in
                                NavigationLink(destination: ProductDetailView(productId: String(product.id))) {
                                    RecentProductCard(product: product, onRemove: {
                                        withAnimation(.easeInOut) {
                                            viewModel.removeItem(productId: String(product.id))
                                        }
                                    })
                                }
                            }
                        }
                        .padding(.horizontal, 2)
                        
                        // Footer info
                        if viewModel.itemCount > 0 {
                            HStack {
                                Image(systemName: "info.circle")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                Text("Long-press an item to remove it. History is stored locally.")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 16)
                        }
                    }
                }
            }
        }
        .navigationTitle("Recently Viewed")
        .refreshable {
            viewModel.refresh()
        }
        .alert("Clear History", isPresented: $showClearConfirmation) {
            Button("Clear All", role: .destructive) {
                withAnimation { viewModel.clearHistory() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will remove all \(viewModel.itemCount) items from your viewing history. This cannot be undone.")
        }
    }
    
    // MARK: - Header
    private var headerSection: some View {
        VStack(spacing: 12) {
            // Gradient banner
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Your History")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    Text("\(viewModel.formattedItemCount) viewed")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
                
                Spacer()
                
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 32))
                    .foregroundColor(.white.opacity(0.6))
            }
            .padding()
            .background(
                LinearGradient(
                    colors: [AppColors.primary, AppColors.primary.opacity(0.7)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .cornerRadius(16)
            .padding(.horizontal)
            .padding(.top, 8)
            
            // Action bar
            HStack {
                // Sort indicator
                HStack(spacing: 4) {
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("Most recent first")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Button(action: { showClearConfirmation = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "trash")
                            .font(.caption)
                        Text("Clear All")
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    .foregroundColor(.red)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(8)
                }
            }
            .padding(.horizontal)
        }
    }
    
    // MARK: - Empty State
    private var emptyState: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(AppColors.primary.opacity(0.1))
                    .frame(width: 120, height: 120)
                
                Image(systemName: "clock")
                    .font(.system(size: 48))
                    .foregroundColor(AppColors.primary.opacity(0.5))
            }
            
            VStack(spacing: 8) {
                Text("No recently viewed items")
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text("Products and reels you view will appear here\nso you can easily find them again")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            NavigationLink(destination: ProductListView()) {
                HStack(spacing: 8) {
                    Image(systemName: "bag")
                    Text("Browse Products")
                }
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(AppColors.primary)
                .cornerRadius(10)
            }
        }
    }
    
    // MARK: - Error State
    private func errorState(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundColor(.orange)
            
            Text("Something went wrong")
                .font(.headline)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Button(action: { viewModel.retry() }) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.clockwise")
                    Text("Try Again")
                }
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(AppColors.primary)
                .cornerRadius(10)
            }
        }
    }
}

// MARK: - Recent Product Card
struct RecentProductCard: View {
    let product: Product
    var onRemove: (() -> Void)? = nil
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Image with multi-priority fallback
            AsyncImage(url: URL(string: bestImageUrl)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    ZStack {
                        Color.gray.opacity(0.15)
                        VStack(spacing: 4) {
                            Image(systemName: "photo")
                                .font(.title3)
                                .foregroundColor(.gray.opacity(0.5))
                            Text(product.name.prefix(8) + "...")
                                .font(.system(size: 8))
                                .foregroundColor(.gray)
                        }
                    }
                default:
                    Color.gray.opacity(0.1)
                        .overlay(
                            ProgressView()
                                .scaleEffect(0.7)
                        )
                }
            }
            .frame(height: 160)
            .clipped()
            
            // Gradient overlay
            LinearGradient(
                colors: [.clear, .clear, .black.opacity(0.8)],
                startPoint: .top,
                endPoint: .bottom
            )
            
            // Info
            VStack(alignment: .leading, spacing: 2) {
                Text(product.name)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .lineLimit(1)
                
                HStack(spacing: 4) {
                    Text("$\(String(format: "%.2f", Double(product.price) ?? 0.0))")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                    
                    if let original = Double(product.originalPrice), original > 0,
                       let current = Double(product.price), current < original {
                        Text("$\(String(format: "%.0f", original))")
                            .font(.system(size: 8))
                            .strikethrough()
                            .foregroundColor(.white.opacity(0.6))
                    }
                }
            }
            .padding(6)
            
            // Video indicator
            if !product.reelVideoUrl.isEmpty {
                VStack {
                    HStack {
                        Spacer()
                        Image(systemName: "play.fill")
                            .font(.caption2)
                            .foregroundColor(.white)
                            .padding(5)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(4)
                            .padding(4)
                    }
                    Spacer()
                }
            }
            
            // Rating badge
            if let rating = product.rating, rating > 0 {
                VStack {
                    HStack {
                        HStack(spacing: 2) {
                            Image(systemName: "star.fill")
                                .font(.system(size: 7))
                                .foregroundColor(.yellow)
                            Text(String(format: "%.1f", rating))
                                .font(.system(size: 8, weight: .semibold))
                                .foregroundColor(.white)
                        }
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(4)
                        .padding(4)
                        
                        Spacer()
                    }
                    Spacer()
                }
            }
        }
        .cornerRadius(6)
        .contextMenu {
            if let onRemove = onRemove {
                Button(role: .destructive) {
                    onRemove()
                } label: {
                    Label("Remove from History", systemImage: "clock.badge.xmark")
                }
            }
            
            Button {
                // Share product
                let text = "\(product.name) - $\(product.price) on BuyV"
                let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                   let root = windowScene.windows.first?.rootViewController {
                    root.present(av, animated: true)
                }
            } label: {
                Label("Share", systemImage: "square.and.arrow.up")
            }
        }
    }
    
    // Multi-priority image: imageUrl > image > empty
    private var bestImageUrl: String {
        if let imageUrl = product.imageUrl, !imageUrl.isEmpty {
            return imageUrl
        }
        return product.image
    }
}

#Preview {
    NavigationView {
        RecentlyViewedView()
    }
}
