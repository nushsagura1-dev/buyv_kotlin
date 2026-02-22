import SwiftUI
import Shared

struct ReelsView: View {
    @StateObject private var viewModel = ReelsViewModel()
    @State private var selectedTab = "For you"
    @State private var showLoginPrompt = false
    
    private let tabs = ["For you", "Following"]
    
    var filteredReels: [Product] {
        if selectedTab == "Following" {
            return viewModel.reels.filter { reel in
                viewModel.followingUserIds.contains(String(reel.userId))
            }
        }
        return viewModel.reels
    }
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            if viewModel.isLoading && viewModel.reels.isEmpty {
                VStack(spacing: 16) {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.5)
                    Text("Loading reels...")
                        .foregroundColor(.gray)
                        .font(.subheadline)
                }
            } else if let errorMessage = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.orange)
                    Text("Error")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text(errorMessage)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    Button("Retry") {
                        viewModel.loadReels()
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 10)
                    .background(Color.white)
                    .foregroundColor(.black)
                    .cornerRadius(8)
                }
            } else if filteredReels.isEmpty && selectedTab == "Following" {
                VStack(spacing: 16) {
                    Image(systemName: "person.2.slash")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    Text("No reels from people you follow")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text("Follow users to see their reels here")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                    Button("Explore For You") {
                        selectedTab = "For you"
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(AppColors.primary)
                    .foregroundColor(.white)
                    .cornerRadius(20)
                }
            } else {
                TabView {
                    ForEach(filteredReels, id: \.id) { reel in
                        ReelItemView(
                            reel: reel,
                            viewModel: viewModel,
                            showLoginPrompt: $showLoginPrompt
                        )
                        .rotationEffect(Angle(degrees: -90))
                        .frame(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)
                        .onAppear {
                            viewModel.trackView(reelId: String(reel.id))
                        }
                    }
                }
                .rotationEffect(Angle(degrees: 90))
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .frame(width: UIScreen.main.bounds.height, height: UIScreen.main.bounds.width)
                .ignoresSafeArea()
            }
            
            // Top Tab Header (overlay)
            VStack {
                HStack {
                    Spacer()
                    ReelsTopHeader(selectedTab: $selectedTab, tabs: tabs)
                    Spacer()
                    NavigationLink(destination: SearchReelsView()) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(8)
                            .background(Color.black.opacity(0.3))
                            .clipShape(Circle())
                    }
                    .padding(.trailing, 12)
                }
                Spacer()
            }
        }
        .onAppear {
            viewModel.loadFollowingList()
        }
        .sheet(isPresented: $showLoginPrompt) {
            RequireLoginPromptView()
        }
    }
}

// MARK: - Top Tab Header
struct ReelsTopHeader: View {
    @Binding var selectedTab: String
    let tabs: [String]
    
    var body: some View {
        HStack(spacing: 24) {
            ForEach(tabs, id: \.self) { tab in
                Button(action: { selectedTab = tab }) {
                    VStack(spacing: 4) {
                        Text(tab)
                            .font(.system(size: 16, weight: selectedTab == tab ? .bold : .regular))
                            .foregroundColor(selectedTab == tab ? .white : .white.opacity(0.6))
                        
                        Rectangle()
                            .fill(selectedTab == tab ? Color.white : Color.clear)
                            .frame(height: 2)
                            .frame(width: 32)
                    }
                }
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 4)
    }
}

// MARK: - Reel Item View (full-screen reel)
struct ReelItemView: View {
    let reel: Product
    @ObservedObject var viewModel: ReelsViewModel
    @Binding var showLoginPrompt: Bool
    
    @State private var showComments = false
    @State private var showAddToCart = false
    @State private var showHeartAnimation = false
    @State private var heartAnimationScale: CGFloat = 0.0
    @State private var heartAnimationOpacity: Double = 0.0
    
    private let reelId: String
    
    init(reel: Product, viewModel: ReelsViewModel, showLoginPrompt: Binding<Bool>) {
        self.reel = reel
        self.viewModel = viewModel
        self._showLoginPrompt = showLoginPrompt
        self.reelId = String(reel.id)
    }
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Video Layer
            VideoPlayerView(url: reel.reelVideoUrl)
                .ignoresSafeArea()
                .onTapGesture(count: 2) {
                    handleDoubleTap()
                }
            
            // Overlay Gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    .black.opacity(0.3),
                    .clear,
                    .clear,
                    .black.opacity(0.7)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .allowsHitTesting(false)
            
            // Heart Animation (center)
            if showHeartAnimation {
                Image(systemName: "heart.fill")
                    .font(.system(size: 100))
                    .foregroundColor(.red)
                    .scaleEffect(heartAnimationScale)
                    .opacity(heartAnimationOpacity)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            
            // Interactions (Right Side)
            VStack(spacing: 20) {
                Spacer()
                
                // User Avatar
                NavigationLink(destination: UserProfileView(userId: reel.userId)) {
                    ZStack(alignment: .bottom) {
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [.purple, .pink, .orange],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 46, height: 46)
                        
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(width: 42, height: 42)
                            .overlay(
                                Text(String(reel.name.prefix(1)).uppercased())
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                            )
                        
                        // Follow badge
                        if !viewModel.followingUserIds.contains(reel.userId) && viewModel.isLoggedIn {
                            Image(systemName: "plus.circle.fill")
                                .font(.system(size: 16))
                                .foregroundColor(.red)
                                .background(Circle().fill(.white).frame(width: 14, height: 14))
                                .offset(y: 8)
                        }
                    }
                }
                
                // Like
                Button(action: {
                    guard viewModel.isLoggedIn else { showLoginPrompt = true; return }
                    viewModel.toggleLike(reelId: reelId)
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: viewModel.isLiked(reelId: reelId) ? "heart.fill" : "heart")
                            .font(.system(size: 28))
                            .foregroundColor(viewModel.isLiked(reelId: reelId) ? .red : .white)
                            .scaleEffect(viewModel.isLiked(reelId: reelId) ? 1.1 : 1.0)
                            .animation(.spring(response: 0.3), value: viewModel.isLiked(reelId: reelId))
                        Text("\(viewModel.reelLikeCounts[reelId] ?? 0)")
                            .foregroundColor(.white)
                            .font(.caption2)
                            .fontWeight(.semibold)
                    }
                }
                
                // Comment
                Button(action: { showComments = true }) {
                    VStack(spacing: 4) {
                        Image(systemName: "bubble.right.fill")
                            .font(.system(size: 26))
                            .foregroundColor(.white)
                        Text("\(viewModel.reelCommentCounts[reelId] ?? 0)")
                            .foregroundColor(.white)
                            .font(.caption2)
                            .fontWeight(.semibold)
                    }
                }
                
                // Bookmark
                Button(action: {
                    guard viewModel.isLoggedIn else { showLoginPrompt = true; return }
                    viewModel.toggleBookmark(reelId: reelId)
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: viewModel.isBookmarked(reelId: reelId) ? "bookmark.fill" : "bookmark")
                            .font(.system(size: 26))
                            .foregroundColor(viewModel.isBookmarked(reelId: reelId) ? .yellow : .white)
                            .animation(.spring(response: 0.3), value: viewModel.isBookmarked(reelId: reelId))
                        Text("Save")
                            .foregroundColor(.white)
                            .font(.caption2)
                    }
                }
                
                // Share
                Button(action: { shareReel(reel: reel) }) {
                    VStack(spacing: 4) {
                        Image(systemName: "paperplane.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                        Text("Share")
                            .foregroundColor(.white)
                            .font(.caption2)
                    }
                }
                
                // Add to Cart
                Button(action: { showAddToCart = true }) {
                    Image(systemName: "cart.fill")
                        .font(.system(size: 22))
                        .padding(12)
                        .background(
                            LinearGradient(
                                colors: [AppColors.primary, .orange],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(Circle())
                        .foregroundColor(.white)
                        .shadow(color: .orange.opacity(0.5), radius: 6)
                }
            }
            .padding(.trailing, 12)
            .padding(.bottom, 90)
            .frame(maxWidth: .infinity, alignment: .trailing)
            
            // Info (Bottom Left) — Product details
            VStack(alignment: .leading, spacing: 8) {
                // User info row
                HStack(spacing: 8) {
                    Circle()
                        .fill(Color.white.opacity(0.3))
                        .frame(width: 32, height: 32)
                        .overlay(
                            Text(String(reel.name.prefix(1)).uppercased())
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        )
                    
                    Text(reel.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                    
                    if !reel.categoryName.isEmpty {
                        Text("•")
                            .foregroundColor(.white.opacity(0.5))
                        Text(reel.categoryName)
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.white.opacity(0.15))
                            .cornerRadius(8)
                    }
                }
                
                // Description
                if !reel.description_.isEmpty {
                    Text(reel.description_)
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.9))
                        .lineLimit(2)
                        .padding(.trailing, 70)
                }
                
                // Price & Product Link
                HStack(spacing: 12) {
                    // Price badge
                    HStack(spacing: 4) {
                        Text("$")
                            .font(.system(size: 14))
                            .foregroundColor(.orange)
                        Text(String(format: "%.2f", Double(reel.price) ?? 0.0))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.orange)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.black.opacity(0.5))
                    .cornerRadius(16)
                    
                    // Original price (if discounted)
                    if reel.originalPrice > 0 && reel.originalPrice > (Double(reel.price) ?? 0.0) {
                        Text("$\(String(format: "%.2f", reel.originalPrice))")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.5))
                            .strikethrough()
                    }
                    
                    NavigationLink(destination: ProductDetailView(productId: reelId)) {
                        HStack(spacing: 4) {
                            Image(systemName: "bag.fill")
                                .font(.caption)
                            Text("View Product")
                                .font(.caption)
                                .fontWeight(.medium)
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(AppColors.primary.opacity(0.8))
                        .cornerRadius(16)
                    }
                }
                
                // Tags
                if !reel.tags.isEmpty {
                    HStack(spacing: 6) {
                        ForEach(reel.tags.components(separatedBy: ",").prefix(3), id: \.self) { tag in
                            Text("#\(tag.trimmingCharacters(in: .whitespaces))")
                                .font(.caption2)
                                .foregroundColor(.cyan)
                        }
                    }
                }
            }
            .padding(.leading, 16)
            .padding(.bottom, 30)
            .padding(.trailing, 70)
        }
        .sheet(isPresented: $showComments) {
            CommentsView(postId: reelId)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showAddToCart) {
            AddToCartSheet(product: reel)
                .presentationDetents([.height(350)])
                .presentationDragIndicator(.visible)
        }
    }
    
    // MARK: - Double Tap Like
    private func handleDoubleTap() {
        guard viewModel.isLoggedIn else {
            showLoginPrompt = true
            return
        }
        
        // Always like on double-tap (don't toggle)
        if !viewModel.isLiked(reelId: reelId) {
            viewModel.toggleLike(reelId: reelId)
        }
        
        // Heart animation
        showHeartAnimation = true
        heartAnimationScale = 0.0
        heartAnimationOpacity = 1.0
        
        withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) {
            heartAnimationScale = 1.2
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            withAnimation(.spring(response: 0.2)) {
                heartAnimationScale = 1.0
            }
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            withAnimation(.easeOut(duration: 0.3)) {
                heartAnimationOpacity = 0.0
            }
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) {
            showHeartAnimation = false
        }
    }
    
    private func shareReel(reel: Product) {
        let url = "buyv://app/product/\(reel.id)"
        let activityVC = UIActivityViewController(
            activityItems: ["\(reel.name) - \(url)"],
            applicationActivities: nil
        )
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = windowScene.windows.first?.rootViewController {
            var topVC = rootVC
            while let presented = topVC.presentedViewController {
                topVC = presented
            }
            topVC.present(activityVC, animated: true)
        }
    }
}

// MARK: - Add to Cart Sheet
struct AddToCartSheet: View {
    let product: Product
    @StateObject private var cartViewModel = CartViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var quantity = 1
    @State private var added = false
    
    var body: some View {
        VStack(spacing: 20) {
            // Product Info
            HStack(spacing: 16) {
                // Product image
                AsyncImage(url: URL(string: product.image)) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.gray.opacity(0.2)
                        .overlay(Image(systemName: "photo").foregroundColor(.gray))
                }
                .frame(width: 80, height: 80)
                .cornerRadius(12)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(product.name)
                        .font(.headline)
                        .lineLimit(2)
                    
                    if !product.categoryName.isEmpty {
                        Text(product.categoryName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Text("$\(String(format: "%.2f", Double(product.price) ?? 0.0))")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                }
                
                Spacer()
            }
            .padding(.horizontal)
            
            // Quantity Selector
            HStack {
                Text("Quantity")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                HStack(spacing: 16) {
                    Button(action: { if quantity > 1 { quantity -= 1 } }) {
                        Image(systemName: "minus.circle.fill")
                            .font(.title2)
                            .foregroundColor(quantity > 1 ? AppColors.primary : .gray)
                    }
                    .disabled(quantity <= 1)
                    
                    Text("\(quantity)")
                        .font(.title3)
                        .fontWeight(.semibold)
                        .frame(width: 40)
                    
                    Button(action: { quantity += 1 }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.primary)
                    }
                }
            }
            .padding(.horizontal)
            
            // Total
            HStack {
                Text("Total")
                    .font(.headline)
                Spacer()
                Text("$\(String(format: "%.2f", (Double(product.price) ?? 0.0) * Double(quantity)))")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.primary)
            }
            .padding(.horizontal)
            
            // Add to Cart Button
            Button(action: {
                cartViewModel.addToCart(product: product, quantity: Int32(quantity))
                added = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    dismiss()
                }
            }) {
                HStack {
                    Image(systemName: added ? "checkmark.circle.fill" : "cart.badge.plus")
                    Text(added ? "Added!" : "Add to Cart")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(added ? Color.green : AppColors.primary)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(added)
            .padding(.horizontal)
        }
        .padding(.top, 20)
    }
}

#Preview {
    NavigationView {
        ReelsView()
    }
}