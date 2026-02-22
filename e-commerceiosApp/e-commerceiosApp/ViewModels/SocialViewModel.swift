import Foundation
import Shared

class SocialViewModel: ObservableObject {
    @Published var searchResults: [UserProfile] = []
    @Published var followers: [UserProfile] = []
    @Published var following: [UserProfile] = []
    
    // Pour le profil visité
    @Published var userPosts: [UserPost] = []
    @Published var bookmarkedPosts: [UserPost] = []
    @Published var likedPosts: [UserPost] = []
    @Published var isFollowing: Bool = false
    @Published var selectedUser: UserProfile?
    
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    // Computed
    var totalLikes: Int {
        userPosts.reduce(0) { $0 + Int($1.likesCount) }
    }
    
    // Dependencies
    private let followUserUseCase = DependencyWrapper.shared.followUserUseCase
    private let unfollowUserUseCase = DependencyWrapper.shared.unfollowUserUseCase
    private let getFollowersUseCase = DependencyWrapper.shared.getFollowersUseCase
    private let getFollowingUseCase = DependencyWrapper.shared.getFollowingUseCase
    private let getFollowingStatusUseCase = DependencyWrapper.shared.getFollowingStatusUseCase
    private let getUserPostsUseCase = DependencyWrapper.shared.getUserPostsUseCase
    private let updateUserProfileUseCase = DependencyWrapper.shared.updateUserProfileUseCase
    private let searchUsersUseCase = DependencyWrapper.shared.searchUsersUseCase
    private let getLikedPostsUseCase = DependencyWrapper.shared.getLikedPostsUseCase
    private let getBookmarkedPostsUseCase = DependencyWrapper.shared.getBookmarkedPostsUseCase
    
    // Recherche
    func searchUsers(query: String) {
        guard !query.isEmpty else {
            searchResults = []
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        searchUsersUseCase.invoke(query: query) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if let success = result as? ResultSuccess<NSArray> {
                     if let users = success.data as? [UserProfile] {
                         self.searchResults = users
                     }
                } else if let errorResult = result as? ResultError {
                    // Ignorer erreur temporaire pour demo ou afficher
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // Actions Follow
    func followUser(targetId: String) {
        let currentUserId = SessionManager.shared.currentUserId
        isLoading = true
        
        followUserUseCase.invoke(followerId: currentUserId, followedId: targetId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if result is ResultSuccess<KotlinUnit> {
                    self.isFollowing = true
                    // Update stats if needed locally
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    func unfollowUser(targetId: String) {
        let currentUserId = SessionManager.shared.currentUserId
        isLoading = true
        
        unfollowUserUseCase.invoke(followerId: currentUserId, followedId: targetId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                 if result is ResultSuccess<KotlinUnit> {
                    self.isFollowing = false
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    func checkFollowingStatus(targetId: String) {
        let currentUserId = SessionManager.shared.currentUserId
        
        getFollowingStatusUseCase.invoke(followerId: currentUserId, followedId: targetId) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<KotlinBoolean> {
                    self.isFollowing = success.data as? Bool ?? false
                }
            }
        }
    }
    
    // Data Loading
    func loadUserPosts(userId: String) {
        isLoading = true
        getUserPostsUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if let success = result as? ResultSuccess<NSArray> {
                    if let posts = success.data as? [UserPost] {
                        self.userPosts = posts
                    }
                } else if let errorResult = result as? ResultError {
                    // "Not implemented" likely
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    func loadFollowers(userId: String) {
        isLoading = true
        getFollowersUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                 if let success = result as? ResultSuccess<NSArray> {
                    if let users = success.data as? [UserProfile] {
                        self.followers = users
                    }
                }
            }
        }
    }
    
    func loadFollowing(userId: String) {
        isLoading = true
        getFollowingUseCase.invoke(userId: userId) { result, error in
             DispatchQueue.main.async {
                self.isLoading = false
                 if let success = result as? ResultSuccess<NSArray> {
                    if let users = success.data as? [UserProfile] {
                        self.following = users
                    }
                }
            }
        }
    }
    
    func updateUserProfile(username: String, bio: String, photoUrl: String?, completion: @escaping (Bool) -> Void) {
        let userId = SessionManager.shared.currentUserId
        isLoading = true
        
        updateUserProfileUseCase.invoke(userId: userId, username: username, bio: bio, photoUrl: photoUrl) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if result is ResultSuccess<UserProfile> {
                    completion(true)
                } else {
                    if let errorResult = result as? ResultError {
                        self.errorMessage = errorResult.error.message
                    }
                    completion(false)
                }
            }
        }
    }
    
    // Utils
    func selectUser(_ user: UserProfile) {
        self.selectedUser = user
        self.userPosts = []
        self.checkFollowingStatus(targetId: user.uid)
        self.loadUserPosts(userId: user.uid)
    }
    
    // MARK: - Bookmarked Posts
    func loadBookmarkedPosts(userId: String) {
        getBookmarkedPostsUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<NSArray> {
                    if let posts = success.data as? [Post] {
                        self.bookmarkedPosts = posts.map { post in
                            UserPost(
                                id: post.id,
                                userId: post.userId,
                                type: "REEL",
                                title: post.caption,
                                description: post.caption,
                                mediaUrl: post.mediaUrl,
                                thumbnailUrl: post.thumbnailUrl,
                                images: [],
                                likesCount: Int32(post.likesCount),
                                commentsCount: Int32(post.commentsCount),
                                viewsCount: 0,
                                isPublished: true,
                                createdAt: post.createdAt,
                                updatedAt: post.updatedAt
                            )
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Liked Posts
    func loadLikedPosts(userId: String) {
        getLikedPostsUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<NSArray> {
                    if let posts = success.data as? [Post] {
                        self.likedPosts = posts.map { post in
                            UserPost(
                                id: post.id,
                                userId: post.userId,
                                type: "REEL",
                                title: post.caption,
                                description: post.caption,
                                mediaUrl: post.mediaUrl,
                                thumbnailUrl: post.thumbnailUrl,
                                images: [],
                                likesCount: Int32(post.likesCount),
                                commentsCount: Int32(post.commentsCount),
                                viewsCount: 0,
                                isPublished: true,
                                createdAt: post.createdAt,
                                updatedAt: post.updatedAt
                            )
                        }
                    }
                }
            }
        }
    }
}
