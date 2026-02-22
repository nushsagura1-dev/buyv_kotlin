import SwiftUI
import Shared

struct FollowListView: View {
    let userId: String
    let type: FollowListType
    @StateObject private var viewModel = SocialViewModel()
    @State private var selectedTab: FollowListType
    @State private var searchQuery = ""
    
    enum FollowListType: Int, CaseIterable {
        case followers = 0
        case following = 1
        
        var title: String {
            switch self {
            case .followers: return "Followers"
            case .following: return "Following"
            }
        }
        
        var icon: String {
            switch self {
            case .followers: return "person.2"
            case .following: return "person.badge.plus"
            }
        }
    }
    
    init(userId: String, type: FollowListType) {
        self.userId = userId
        self.type = type
        _selectedTab = State(initialValue: type)
    }
    
    private var currentUsers: [UserProfile] {
        let users = selectedTab == .followers ? viewModel.followers : viewModel.following
        if searchQuery.isEmpty { return users }
        return users.filter {
            $0.displayName.localizedCaseInsensitiveContains(searchQuery) ||
            $0.username.localizedCaseInsensitiveContains(searchQuery)
        }
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Tab Selector
            tabSelector
            
            // Search
            searchBar
            
            // Content
            if viewModel.isLoading {
                Spacer()
                ProgressView("Loading...")
                Spacer()
            } else if currentUsers.isEmpty {
                Spacer()
                emptyState
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(currentUsers, id: \.uid) { user in
                            NavigationLink(destination: UserProfileView(user: user)) {
                                FollowUserRow(
                                    user: user,
                                    isOwnProfile: userId == SessionManager.shared.currentUserId,
                                    isFollowing: checkIsFollowing(user),
                                    onFollowToggle: { toggleFollow(user) }
                                )
                            }
                            .buttonStyle(PlainButtonStyle())
                            
                            Divider().padding(.leading, 72)
                        }
                    }
                }
                .refreshable {
                    loadData()
                }
            }
        }
        .navigationTitle(selectedTab.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Text("\(currentUsers.count)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
            }
        }
        .onAppear { loadData() }
        .onChange(of: selectedTab) { _ in loadData() }
    }
    
    // MARK: - Tab Selector
    private var tabSelector: some View {
        HStack(spacing: 0) {
            ForEach(FollowListType.allCases, id: \.self) { tab in
                Button(action: { withAnimation(.easeInOut(duration: 0.2)) { selectedTab = tab } }) {
                    VStack(spacing: 8) {
                        HStack(spacing: 6) {
                            Image(systemName: tab.icon)
                                .font(.caption)
                            Text(tab.title)
                                .font(.subheadline)
                                .fontWeight(selectedTab == tab ? .semibold : .regular)
                            
                            // Count badge
                            let count = tab == .followers ? viewModel.followers.count : viewModel.following.count
                            Text("\(count)")
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(selectedTab == tab ? .white : .secondary)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(selectedTab == tab ? AppColors.primary : Color.gray.opacity(0.2))
                                .cornerRadius(10)
                        }
                        .foregroundColor(selectedTab == tab ? AppColors.primary : .gray)
                        
                        Rectangle()
                            .fill(selectedTab == tab ? AppColors.primary : Color.clear)
                            .frame(height: 2)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.top, 4)
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            TextField("Search...", text: $searchQuery)
                .textFieldStyle(.plain)
            if !searchQuery.isEmpty {
                Button(action: { searchQuery = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(10)
        .background(Color(.systemGray6))
        .cornerRadius(10)
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Empty State
    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: searchQuery.isEmpty ? "person.2.slash" : "magnifyingglass")
                .font(.system(size: 48))
                .foregroundColor(.gray.opacity(0.5))
            Text(searchQuery.isEmpty ? "No \(selectedTab.title.lowercased()) yet" : "No results found")
                .font(.headline)
                .foregroundColor(.secondary)
            if !searchQuery.isEmpty {
                Text("Try a different search term")
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
        }
    }
    
    // MARK: - Helpers
    private func loadData() {
        viewModel.loadFollowers(userId: userId)
        viewModel.loadFollowing(userId: userId)
    }
    
    private func checkIsFollowing(_ user: UserProfile) -> Bool {
        viewModel.following.contains(where: { $0.uid == user.uid })
    }
    
    private func toggleFollow(_ user: UserProfile) {
        if checkIsFollowing(user) {
            viewModel.unfollowUser(targetId: user.uid)
        } else {
            viewModel.followUser(targetId: user.uid)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { loadData() }
    }
}

// MARK: - Follow User Row
struct FollowUserRow: View {
    let user: UserProfile
    let isOwnProfile: Bool
    let isFollowing: Bool
    let onFollowToggle: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Avatar
            if let urlString = user.profileImageUrl, !urlString.isEmpty, let url = URL(string: urlString) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.gray.opacity(0.3)
                }
                .frame(width: 50, height: 50)
                .clipShape(Circle())
            } else {
                Circle()
                    .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 50, height: 50)
                    .overlay(
                        Text(user.displayName.prefix(1).uppercased())
                            .font(.headline).foregroundColor(.white)
                    )
            }
            
            // Info
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(user.displayName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    
                    if user.role == "admin" || user.role == "promoter" {
                        Image(systemName: user.role == "admin" ? "checkmark.seal.fill" : "star.fill")
                            .font(.system(size: 10))
                            .foregroundColor(user.role == "admin" ? .blue : AppColors.primary)
                    }
                }
                
                Text("@\(user.username)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                if !user.bio.isEmpty {
                    Text(user.bio)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
            
            Spacer()
            
            // Follow/Unfollow button (only if not own profile and not viewing self in list)
            if isOwnProfile && user.uid != SessionManager.shared.currentUserId {
                Button(action: onFollowToggle) {
                    Text(isFollowing ? "Following" : "Follow")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(isFollowing ? .primary : .white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                        .background(isFollowing ? Color(.systemGray5) : AppColors.primary)
                        .cornerRadius(16)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
    }
}
