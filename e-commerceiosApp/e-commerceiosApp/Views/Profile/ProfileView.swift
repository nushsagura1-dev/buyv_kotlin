import SwiftUI
import Shared

// MARK: - Profile Tab
enum ProfileTab: Int, CaseIterable {
    case reels = 0
    case products = 1
    case saved = 2
    case liked = 3
    
    var icon: String {
        switch self {
        case .reels: return "play.square.stack"
        case .products: return "bag"
        case .saved: return "bookmark"
        case .liked: return "heart"
        }
    }
    
    var label: String {
        switch self {
        case .reels: return "Reels"
        case .products: return "Products"
        case .saved: return "Saved"
        case .liked: return "Liked"
        }
    }
}

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @StateObject private var socialViewModel = SocialViewModel()
    @State private var showEditProfile = false
    @State private var selectedTab: ProfileTab = .reels
    @State private var showDeleteConfirmation = false
    @State private var postToDelete: UserPost?
    @State private var showCreatePost = false
    @State private var showDeleteSuccess = false
    @State private var isDeletingPost = false
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 0) {
                        if let user = viewModel.user {
                            // Profile Header
                            profileHeader(user: user)
                            
                            // Bio
                            if !user.bio.isEmpty {
                                Text(user.bio)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.horizontal)
                                    .padding(.top, 8)
                            }
                            
                            // Edit Profile + Share + Wallet
                            actionButtons
                            
                            // Tab selector: Reels / Products / Saved / Liked
                            tabSelector
                            
                            // Tab content
                            tabContent
                            
                        } else {
                            VStack(spacing: 12) {
                                Text("No user logged in")
                                    .foregroundColor(.secondary)
                                Button("Logout Force") {
                                    SessionManager.shared.clearUser()
                                }
                            }
                            .padding(.top, 100)
                        }
                    }
                }
                .refreshable {
                    if let userId = viewModel.user?.uid {
                        socialViewModel.loadUserPosts(userId: userId)
                        socialViewModel.loadBookmarkedPosts(userId: userId)
                        socialViewModel.loadLikedPosts(userId: userId)
                    }
                }
                
                // Floating Create Button
                if viewModel.user != nil {
                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Button(action: { showCreatePost = true }) {
                                Image(systemName: "plus")
                                    .font(.title2.bold())
                                    .foregroundColor(.white)
                                    .frame(width: 56, height: 56)
                                    .background(
                                        LinearGradient(colors: [AppColors.primary, AppColors.primary.opacity(0.8)],
                                                       startPoint: .topLeading, endPoint: .bottomTrailing)
                                    )
                                    .clipShape(Circle())
                                    .shadow(color: AppColors.primary.opacity(0.4), radius: 8, y: 4)
                            }
                            .padding(.trailing, 20)
                            .padding(.bottom, 20)
                        }
                    }
                }
                
                // Delete post loading overlay
                if isDeletingPost {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .overlay(
                            VStack(spacing: 12) {
                                ProgressView()
                                    .tint(.white)
                                    .scaleEffect(1.2)
                                Text("Deleting post...")
                                    .font(.subheadline)
                                    .foregroundColor(.white)
                            }
                            .padding(24)
                            .background(Color.black.opacity(0.7))
                            .cornerRadius(16)
                        )
                }
                
                // Delete success toast
                if showDeleteSuccess {
                    VStack {
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Post deleted successfully")
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
                    .zIndex(20)
                }
            }
            .navigationTitle("Profile")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    NavigationLink(destination: WalletView()) {
                        Image(systemName: "wallet.pass")
                            .foregroundColor(AppColors.primary)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        NavigationLink(destination: SettingsView()) {
                            Label("Settings", systemImage: "gearshape")
                        }
                        NavigationLink(destination: OrderListView()) {
                            Label("My Orders", systemImage: "bag")
                        }
                        NavigationLink(destination: ChangePasswordView()) {
                            Label("Change Password", systemImage: "lock.shield")
                        }
                        
                        Divider()
                        
                        Button(role: .destructive, action: viewModel.logout) {
                            Label("Log Out", systemImage: "arrow.right.square")
                        }
                    } label: {
                        Image(systemName: "line.3.horizontal")
                    }
                }
            }
            .onAppear {
                if let userId = viewModel.user?.uid {
                    socialViewModel.loadUserPosts(userId: userId)
                    socialViewModel.loadBookmarkedPosts(userId: userId)
                    socialViewModel.loadLikedPosts(userId: userId)
                }
            }
            .sheet(isPresented: $showEditProfile) {
                if let user = viewModel.user {
                    EditProfileView(user: user)
                }
            }
            .sheet(isPresented: $showCreatePost) {
                CreatePostView()
            }
            .alert("Delete Post?", isPresented: $showDeleteConfirmation) {
                Button("Delete", role: .destructive) {
                    if let post = postToDelete {
                        deletePost(post)
                    }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This action cannot be undone.")
            }
            .alert(item: Binding<ErrorWrapper?>(
                get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
                set: { viewModel.errorMessage = $0?.message }
            )) { errorWrapper in
                 Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
            }
        }
    }
    
    // MARK: - Profile Header
    private func profileHeader(user: UserProfile) -> some View {
        VStack(spacing: 16) {
            HStack(alignment: .center, spacing: 20) {
                // Avatar
                if let photoUrl = user.profileImageUrl, !photoUrl.isEmpty {
                    AsyncImage(url: URL(string: photoUrl)) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.gray
                    }
                    .frame(width: 86, height: 86)
                    .clipShape(Circle())
                    .overlay(
                        Circle().stroke(Color.gray.opacity(0.2), lineWidth: 1)
                    )
                } else {
                    Circle()
                        .fill(
                            LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .frame(width: 86, height: 86)
                        .overlay(
                            Text(user.displayName.prefix(1).uppercased())
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.white)
                        )
                }
                
                // Stats
                HStack(spacing: 20) {
                    VStack(spacing: 2) {
                        Text("\(socialViewModel.userPosts.count)")
                            .font(.headline)
                            .fontWeight(.bold)
                        Text("Posts")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    NavigationLink(destination: FollowListView(userId: user.uid, type: .followers)) {
                        VStack(spacing: 2) {
                            Text("\(user.followersCount)")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                            Text("Followers")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    NavigationLink(destination: FollowListView(userId: user.uid, type: .following)) {
                        VStack(spacing: 2) {
                            Text("\(user.followingCount)")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                            Text("Following")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    VStack(spacing: 2) {
                        Text(formatCount(Int(user.likesCount)))
                            .font(.headline)
                            .fontWeight(.bold)
                        Text("Likes")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal)
            
            // Display name + username
            VStack(alignment: .leading, spacing: 2) {
                Text(user.displayName)
                    .font(.subheadline)
                    .fontWeight(.bold)
                
                if !user.username.isEmpty && user.username != user.displayName {
                    Text("@\(user.username)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
        }
        .padding(.top, 16)
    }
    
    // MARK: - Action Buttons
    private var actionButtons: some View {
        HStack(spacing: 8) {
            Button(action: { showEditProfile = true }) {
                Text("Edit Profile")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.gray.opacity(0.5), lineWidth: 1)
                    )
            }
            
            Button(action: shareProfile) {
                Text("Share Profile")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.gray.opacity(0.5), lineWidth: 1)
                    )
            }
        }
        .padding(.horizontal)
        .padding(.top, 12)
    }
    
    // MARK: - Tab Selector
    private var tabSelector: some View {
        HStack(spacing: 0) {
            ForEach(ProfileTab.allCases, id: \.self) { tab in
                Button(action: { withAnimation(.easeInOut(duration: 0.2)) { selectedTab = tab } }) {
                    VStack(spacing: 6) {
                        Image(systemName: tab.icon)
                            .font(.title3)
                            .foregroundColor(selectedTab == tab ? AppColors.primary : .gray)
                        
                        Rectangle()
                            .fill(selectedTab == tab ? AppColors.primary : Color.clear)
                            .frame(height: 2)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.top, 16)
    }
    
    // MARK: - Tab Content
    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .reels:
            reelsGrid
        case .products:
            productsGrid
        case .saved:
            bookmarksGrid
        case .liked:
            likedGrid
        }
    }
    
    // MARK: - Reels Grid (with delete & view count)
    private var reelsGrid: some View {
        Group {
            if socialViewModel.userPosts.isEmpty {
                emptyState(icon: "video", title: "No reels yet", subtitle: "Create your first reel!")
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 2) {
                    ForEach(socialViewModel.userPosts, id: \.id) { post in
                        ZStack(alignment: .bottomLeading) {
                            PostThumbnailView(post: post)
                            
                            // View count overlay
                            HStack(spacing: 2) {
                                Image(systemName: "play.fill")
                                    .font(.system(size: 8))
                                Text(formatCount(Int(post.viewsCount)))
                                    .font(.system(size: 10, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(Color.black.opacity(0.6))
                            .cornerRadius(4)
                            .padding(4)
                        }
                        .contextMenu {
                            Button(role: .destructive) {
                                postToDelete = post
                                showDeleteConfirmation = true
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Products Grid
    private var productsGrid: some View {
        Group {
            // Products tab shows user's products (if they're a seller)
            emptyState(icon: "bag", title: "No products yet", subtitle: "Start selling on BuyV!")
        }
    }
    
    // MARK: - Bookmarks Grid
    private var bookmarksGrid: some View {
        Group {
            if socialViewModel.bookmarkedPosts.isEmpty {
                emptyState(icon: "bookmark", title: "No saved posts", subtitle: "Bookmark posts to see them here")
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 2) {
                    ForEach(socialViewModel.bookmarkedPosts, id: \.id) { post in
                        PostThumbnailView(post: post)
                    }
                }
            }
        }
    }
    
    // MARK: - Liked Grid
    private var likedGrid: some View {
        Group {
            if socialViewModel.likedPosts.isEmpty {
                emptyState(icon: "heart", title: "No liked posts", subtitle: "Like posts to see them here")
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 2) {
                    ForEach(socialViewModel.likedPosts, id: \.id) { post in
                        PostThumbnailView(post: post)
                    }
                }
            }
        }
    }
    
    // MARK: - Empty State
    private func emptyState(icon: String, title: String, subtitle: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 50))
                .foregroundColor(.gray.opacity(0.5))
            Text(title)
                .font(.headline)
                .foregroundColor(.secondary)
            Text(subtitle)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
        .padding(.bottom, 40)
    }
    
    // MARK: - Helpers
    
    private func deletePost(_ post: UserPost) {
        isDeletingPost = true
        let deleteUseCase = DependencyWrapper.shared.deletePostUseCase
        deleteUseCase.invoke(postId: post.id) { _, error in
            DispatchQueue.main.async {
                isDeletingPost = false
                if error == nil {
                    socialViewModel.userPosts.removeAll { $0.id == post.id }
                    withAnimation(.spring()) {
                        showDeleteSuccess = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation { showDeleteSuccess = false }
                    }
                }
            }
        }
    }
    
    private func shareProfile() {
        guard let user = viewModel.user else { return }
        let text = "Check out @\(user.username) on BuyV! buyv://profile/\(user.uid)"
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = windowScene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
    }
    
    private func formatCount(_ count: Int) -> String {
        if count >= 1_000_000 { return String(format: "%.1fM", Double(count) / 1_000_000) }
        if count >= 1_000 { return String(format: "%.1fK", Double(count) / 1_000) }
        return "\(count)"
    }
}

// MARK: - Profile Menu Row
struct ProfileMenuRow: View {
    let icon: String
    let title: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .foregroundColor(color)
                .frame(width: 28)
            
            Text(title)
                .foregroundColor(.primary)
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 12)
    }
}
