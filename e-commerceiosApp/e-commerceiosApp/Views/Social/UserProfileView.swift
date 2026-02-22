import SwiftUI
import Shared

// MARK: - User Profile Tab
enum UserProfileTab: Int, CaseIterable {
    case reels = 0
    case liked = 1
    
    var icon: String {
        switch self {
        case .reels: return "play.square.stack"
        case .liked: return "heart"
        }
    }
}

struct UserProfileView: View {
    let user: UserProfile
    @StateObject private var viewModel = SocialViewModel()
    @State private var selectedTab: UserProfileTab = .reels
    @State private var showShareSheet = false
    @State private var showReportAlert = false
    @State private var showBlockAlert = false
    @State private var showReportSuccess = false
    @State private var showBlockSuccess = false
    @State private var showMessageComingSoon = false
    
    private var isOwnProfile: Bool {
        user.uid == SessionManager.shared.currentUserId
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Header
                profileHeader
                
                // Bio
                if !user.bio.isEmpty {
                    Text(user.bio)
                        .font(.subheadline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)
                        .padding(.top, 8)
                }
                
                // Action Buttons
                actionButtons
                
                // Tab Selector
                tabSelector
                
                // Tab Content
                tabContent
            }
        }
        .refreshable {
            viewModel.selectUser(user)
        }
        .navigationTitle(user.username)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: { shareProfile() }) {
                        Label("Share Profile", systemImage: "square.and.arrow.up")
                    }
                    if !isOwnProfile {
                        Button(role: .destructive, action: { showReportAlert = true }) {
                            Label("Report User", systemImage: "exclamationmark.triangle")
                        }
                        Button(role: .destructive, action: { showBlockAlert = true }) {
                            Label("Block User", systemImage: "hand.raised")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                }
            }
        }
        .onAppear {
            viewModel.selectUser(user)
        }
        .alert("Report User", isPresented: $showReportAlert) {
            Button("Report", role: .destructive) {
                withAnimation { showReportSuccess = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                    withAnimation { showReportSuccess = false }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to report @\(user.username)? This will be reviewed by our team.")
        }
        .alert("Block User", isPresented: $showBlockAlert) {
            Button("Block", role: .destructive) {
                withAnimation { showBlockSuccess = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                    withAnimation { showBlockSuccess = false }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Block @\(user.username)? You won't see their content and they won't be able to find your profile.")
        }
        .overlay(
            Group {
                if showReportSuccess {
                    notificationBanner(icon: "checkmark.circle.fill", text: "Report submitted. We'll review it shortly.", color: .green)
                }
                if showBlockSuccess {
                    notificationBanner(icon: "hand.raised.fill", text: "@\(user.username) has been blocked", color: .red)
                }
                if showMessageComingSoon {
                    notificationBanner(icon: "envelope.badge", text: "Messaging coming soon!", color: AppColors.primary)
                }
            },
            alignment: .top
        )
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
             Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
        }
    }
    
    // MARK: - Profile Header
    private var profileHeader: some View {
        VStack(spacing: 16) {
            HStack(alignment: .center, spacing: 20) {
                // Avatar
                if let urlString = user.profileImageUrl, !urlString.isEmpty, let url = URL(string: urlString) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.gray.opacity(0.3)
                    }
                    .frame(width: 86, height: 86)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.gray.opacity(0.2), lineWidth: 1))
                } else {
                    Circle()
                        .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
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
                        Text("\(viewModel.userPosts.count)")
                            .font(.headline).fontWeight(.bold)
                        Text("Posts")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    
                    NavigationLink(destination: FollowListView(userId: user.uid, type: .followers)) {
                        VStack(spacing: 2) {
                            Text("\(user.followersCount)")
                                .font(.headline).fontWeight(.bold).foregroundColor(.primary)
                            Text("Followers")
                                .font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    NavigationLink(destination: FollowListView(userId: user.uid, type: .following)) {
                        VStack(spacing: 2) {
                            Text("\(user.followingCount)")
                                .font(.headline).fontWeight(.bold).foregroundColor(.primary)
                            Text("Following")
                                .font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    VStack(spacing: 2) {
                        Text(formatCount(Int(user.likesCount)))
                            .font(.headline).fontWeight(.bold)
                        Text("Likes")
                            .font(.caption).foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal)
            
            // Name + Username
            VStack(alignment: .leading, spacing: 2) {
                Text(user.displayName)
                    .font(.subheadline).fontWeight(.bold)
                if !user.username.isEmpty && user.username != user.displayName {
                    Text("@\(user.username)")
                        .font(.caption).foregroundColor(.secondary)
                }
                if user.role == "promoter" || user.role == "admin" {
                    HStack(spacing: 4) {
                        Image(systemName: user.role == "admin" ? "shield.checkered" : "star.fill")
                            .font(.system(size: 10))
                        Text(user.role.capitalized)
                            .font(.system(size: 11, weight: .medium))
                    }
                    .foregroundColor(user.role == "admin" ? .red : AppColors.primary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background((user.role == "admin" ? Color.red : AppColors.primary).opacity(0.1))
                    .cornerRadius(8)
                    .padding(.top, 2)
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
            if !isOwnProfile {
                // Follow/Unfollow
                Button(action: {
                    if viewModel.isFollowing {
                        viewModel.unfollowUser(targetId: user.uid)
                    } else {
                        viewModel.followUser(targetId: user.uid)
                    }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: viewModel.isFollowing ? "person.badge.minus" : "person.badge.plus")
                            .font(.caption)
                        Text(viewModel.isFollowing ? "Following" : "Follow")
                            .font(.subheadline).fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(viewModel.isFollowing ? Color(.systemGray5) : AppColors.primary)
                    .foregroundColor(viewModel.isFollowing ? .primary : .white)
                    .cornerRadius(8)
                }
                
                // Message
                Button(action: {
                    withAnimation { showMessageComingSoon = true }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation { showMessageComingSoon = false }
                    }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "envelope")
                            .font(.caption)
                        Text("Message")
                            .font(.subheadline).fontWeight(.medium)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .foregroundColor(.primary)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.4), lineWidth: 1))
                }
            }
            
            // Share
            Button(action: shareProfile) {
                Image(systemName: "square.and.arrow.up")
                    .font(.subheadline)
                    .padding(.vertical, 9)
                    .padding(.horizontal, 14)
                    .foregroundColor(.primary)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.4), lineWidth: 1))
            }
        }
        .padding(.horizontal)
        .padding(.top, 12)
    }
    
    // MARK: - Tab Selector
    private var tabSelector: some View {
        HStack(spacing: 0) {
            ForEach(UserProfileTab.allCases, id: \.self) { tab in
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
        case .liked:
            likedPlaceholder
        }
    }
    
    private var reelsGrid: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .padding(.top, 40)
            } else if viewModel.userPosts.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "video")
                        .font(.system(size: 50))
                        .foregroundColor(.gray.opacity(0.5))
                    Text("No posts yet")
                        .font(.headline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 60)
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 2) {
                    ForEach(viewModel.userPosts, id: \.id) { post in
                        ZStack(alignment: .bottomLeading) {
                            PostThumbnailView(post: post)
                            
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
                    }
                }
            }
        }
    }
    
    private var likedPlaceholder: some View {
        VStack(spacing: 12) {
            Image(systemName: "heart")
                .font(.system(size: 50))
                .foregroundColor(.gray.opacity(0.5))
            Text("Liked posts are private")
                .font(.headline)
                .foregroundColor(.secondary)
            Text("Only the user can see what they've liked")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.top, 60)
    }
    
    // MARK: - Helpers
    
    private func notificationBanner(icon: String, text: String, color: Color) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundColor(color)
            Text(text)
                .font(.subheadline)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color(.systemBackground))
        .cornerRadius(24)
        .shadow(radius: 8)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
    
    private func shareProfile() {
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

struct StatView: View {
    let value: Int32
    let title: String
    
    var body: some View {
        VStack {
            Text("\(value)")
                .fontWeight(.bold)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct PostThumbnailView: View {
    let post: UserPost
    
    var body: some View {
        if let url = URL(string: post.thumbnailUrl ?? post.mediaUrl) {
            AsyncImage(url: url) { image in
                 image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.gray.opacity(0.3)
            }
            .frame(height: 120)
            .clipped()
        } else {
            Rectangle()
                .fill(Color.gray.opacity(0.3))
                .frame(height: 120)
        }
    }
}
