import Foundation
import Shared

// MARK: - Profile UI State
enum ProfileUIState {
    case idle
    case loading
    case loaded
    case error(String)
}

class ProfileViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var user: UserProfile?
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var uiState: ProfileUIState = .idle
    
    // Content tabs data
    @Published var userPosts: [UserPost] = []
    @Published var bookmarkedPosts: [UserPost] = []
    @Published var likedPosts: [UserPost] = []
    @Published var isLoadingPosts = false
    @Published var isLoadingBookmarks = false
    @Published var isLoadingLiked = false
    
    // Post deletion
    @Published var isDeletingPost = false
    @Published var postDeletedSuccessfully = false
    
    // Profile update
    @Published var isUpdatingProfile = false
    @Published var profileUpdatedSuccessfully = false
    
    // MARK: - Computed Properties
    var postsCount: Int { userPosts.count }
    var followersCount: Int { Int(user?.followersCount ?? 0) }
    var followingCount: Int { Int(user?.followingCount ?? 0) }
    var likesCount: Int { Int(user?.likesCount ?? 0) }
    var totalPostLikes: Int { userPosts.reduce(0) { $0 + Int($1.likesCount) } }
    
    var isLoggedIn: Bool { SessionManager.shared.authToken != nil }
    var displayName: String { user?.displayName ?? "User" }
    var username: String { user?.username ?? "" }
    var bio: String { user?.bio ?? "" }
    var profileImageUrl: String? { user?.profileImageUrl }
    var userId: String { user?.uid ?? SessionManager.shared.currentUserId }
    
    // MARK: - Dependencies
    private let logoutUseCase = DependencyWrapper.shared.logoutUseCase
    private let getUserProfileUseCase = DependencyWrapper.shared.getUserProfileUseCase
    private let getUserPostsUseCase = DependencyWrapper.shared.getUserPostsUseCase
    private let getLikedPostsUseCase = DependencyWrapper.shared.getLikedPostsUseCase
    private let getBookmarkedPostsUseCase = DependencyWrapper.shared.getBookmarkedPostsUseCase
    private let deletePostUseCase = DependencyWrapper.shared.deletePostUseCase
    private let updateUserProfileUseCase = DependencyWrapper.shared.updateUserProfileUseCase
    
    // MARK: - Init
    init() {
        self.user = SessionManager.shared.currentUser
        if user != nil {
            loadAllProfileData()
        }
    }
    
    // MARK: - Load Profile from API
    func loadUserProfile() {
        let uid = SessionManager.shared.currentUserId
        guard !uid.isEmpty else {
            uiState = .error("No user logged in")
            return
        }
        
        isLoading = true
        uiState = .loading
        errorMessage = nil
        
        getUserProfileUseCase.invoke(userId: uid) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let success = result as? ResultSuccess<UserProfile> {
                    self.user = success.data
                    SessionManager.shared.currentUser = success.data
                    self.uiState = .loaded
                } else if let errorResult = result as? ResultError {
                    let msg = errorResult.error.message ?? "Failed to load profile"
                    self.errorMessage = msg
                    self.uiState = .error(msg)
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                    self.uiState = .error(error.localizedDescription)
                }
            }
        }
    }
    
    // MARK: - Load All Profile Data
    func loadAllProfileData() {
        loadUserPosts()
        loadBookmarkedPosts()
        loadLikedPosts()
    }
    
    // MARK: - Refresh Profile
    func refreshProfile() {
        isRefreshing = true
        
        let uid = SessionManager.shared.currentUserId
        guard !uid.isEmpty else {
            isRefreshing = false
            return
        }
        
        getUserProfileUseCase.invoke(userId: uid) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<UserProfile> {
                    self.user = success.data
                    SessionManager.shared.currentUser = success.data
                }
                self.isRefreshing = false
                // Also refresh content
                self.loadAllProfileData()
            }
        }
    }
    
    // MARK: - Load User Posts
    func loadUserPosts() {
        let uid = userId
        guard !uid.isEmpty else { return }
        
        isLoadingPosts = true
        
        getUserPostsUseCase.invoke(userId: uid) { result, error in
            DispatchQueue.main.async {
                self.isLoadingPosts = false
                
                if let success = result as? ResultSuccess<NSArray>,
                   let posts = success.data as? [UserPost] {
                    self.userPosts = posts
                }
            }
        }
    }
    
    // MARK: - Load Bookmarked Posts
    func loadBookmarkedPosts() {
        let uid = userId
        guard !uid.isEmpty else { return }
        
        isLoadingBookmarks = true
        
        getBookmarkedPostsUseCase.invoke(userId: uid) { result, error in
            DispatchQueue.main.async {
                self.isLoadingBookmarks = false
                
                if let success = result as? ResultSuccess<NSArray>,
                   let posts = success.data as? [UserPost] {
                    self.bookmarkedPosts = posts
                }
            }
        }
    }
    
    // MARK: - Load Liked Posts
    func loadLikedPosts() {
        let uid = userId
        guard !uid.isEmpty else { return }
        
        isLoadingLiked = true
        
        getLikedPostsUseCase.invoke(userId: uid) { result, error in
            DispatchQueue.main.async {
                self.isLoadingLiked = false
                
                if let success = result as? ResultSuccess<NSArray>,
                   let posts = success.data as? [UserPost] {
                    self.likedPosts = posts
                }
            }
        }
    }
    
    // MARK: - Delete Post
    func deletePost(_ post: UserPost) {
        isDeletingPost = true
        postDeletedSuccessfully = false
        
        deletePostUseCase.invoke(postId: post.id) { result, error in
            DispatchQueue.main.async {
                self.isDeletingPost = false
                
                if error == nil {
                    self.userPosts.removeAll { $0.id == post.id }
                    self.postDeletedSuccessfully = true
                } else {
                    self.errorMessage = error?.localizedDescription ?? "Failed to delete post"
                }
            }
        }
    }
    
    // MARK: - Logout
    func logout() {
        isLoading = true
        logoutUseCase.invoke { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if result is ResultSuccess<KotlinUnit> {
                    SessionManager.shared.clearUser()
                    self.user = nil
                    self.userPosts = []
                    self.bookmarkedPosts = []
                    self.likedPosts = []
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // MARK: - Clear Error
    func clearError() {
        errorMessage = nil
    }
    
    // MARK: - Format Count Helper
    func formatCount(_ count: Int) -> String {
        if count >= 1_000_000 { return String(format: "%.1fM", Double(count) / 1_000_000) }
        if count >= 1_000 { return String(format: "%.1fK", Double(count) / 1_000) }
        return "\(count)"
    }
}
