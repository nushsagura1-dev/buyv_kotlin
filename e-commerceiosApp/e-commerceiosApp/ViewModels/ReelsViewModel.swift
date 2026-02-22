import Foundation
import Shared

class ReelsViewModel: ObservableObject {
    @Published var reels: [Product] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var likedReelIds: Set<String> = []
    @Published var bookmarkedReelIds: Set<String> = []
    @Published var followingUserIds: Set<String> = []
    @Published var reelLikeCounts: [String: Int] = [:]
    @Published var reelCommentCounts: [String: Int] = [:]
    
    // Recently viewed tracking
    @Published var recentlyViewedIds: [String] = []
    
    // Dependencies
    private let getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
    private let likePostUseCase = DependencyWrapper.shared.likePostUseCase
    private let unlikePostUseCase = DependencyWrapper.shared.unlikePostUseCase
    private let checkPostLikeStatusUseCase = DependencyWrapper.shared.checkPostLikeStatusUseCase
    private let getFollowingUseCase = DependencyWrapper.shared.getFollowingUseCase
    private let bookmarkPostUseCase = DependencyWrapper.shared.bookmarkPostUseCase
    private let unbookmarkPostUseCase = DependencyWrapper.shared.unbookmarkPostUseCase
    
    var isLoggedIn: Bool {
        let uid = SessionManager.shared.currentUserId
        return uid != nil && uid != "guest_user"
    }
    
    init() {
        loadRecentlyViewed()
        loadReels()
    }
    
    func loadReels() {
        isLoading = true
        errorMessage = nil
        
        getProductsUseCase.getAllProducts { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let success = result as? ResultSuccess<NSArray> {
                     if let products = success.data as? [Product] {
                         self.reels = products.filter { product in
                             !product.reelVideoUrl.isEmpty
                         }
                         for reel in self.reels {
                             self.checkLikeStatus(reelId: String(reel.id))
                         }
                     }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    func isLiked(reelId: String) -> Bool {
        return likedReelIds.contains(reelId)
    }
    
    func isBookmarked(reelId: String) -> Bool {
        return bookmarkedReelIds.contains(reelId)
    }
    
    private func checkLikeStatus(reelId: String) {
        guard let userId = SessionManager.shared.currentUserId else { return }
        checkPostLikeStatusUseCase.invoke(postId: reelId, userId: userId) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<KotlinBoolean> {
                    if success.data?.boolValue == true {
                        self.likedReelIds.insert(reelId)
                    }
                }
            }
        }
    }
    
    func toggleLike(reelId: String) {
        guard isLoggedIn else { return }
        let wasLiked = likedReelIds.contains(reelId)
        
        // Optimistic UI update
        if wasLiked {
            likedReelIds.remove(reelId)
            reelLikeCounts[reelId] = max(0, (reelLikeCounts[reelId] ?? 1) - 1)
        } else {
            likedReelIds.insert(reelId)
            reelLikeCounts[reelId] = (reelLikeCounts[reelId] ?? 0) + 1
        }
        
        if wasLiked {
            unlikePostUseCase.invoke(postId: reelId) { result, error in
                DispatchQueue.main.async {
                    if error != nil || result is ResultError {
                        self.likedReelIds.insert(reelId)
                        self.reelLikeCounts[reelId] = (self.reelLikeCounts[reelId] ?? 0) + 1
                    }
                }
            }
        } else {
            likePostUseCase.invoke(postId: reelId) { result, error in
                DispatchQueue.main.async {
                    if error != nil || result is ResultError {
                        self.likedReelIds.remove(reelId)
                        self.reelLikeCounts[reelId] = max(0, (self.reelLikeCounts[reelId] ?? 1) - 1)
                    }
                }
            }
        }
    }
    
    func toggleBookmark(reelId: String) {
        guard isLoggedIn else { return }
        let wasBookmarked = bookmarkedReelIds.contains(reelId)
        
        if wasBookmarked {
            bookmarkedReelIds.remove(reelId)
            unbookmarkPostUseCase.invoke(postId: reelId) { result, error in
                DispatchQueue.main.async {
                    if error != nil || result is ResultError {
                        self.bookmarkedReelIds.insert(reelId)
                    }
                }
            }
        } else {
            bookmarkedReelIds.insert(reelId)
            bookmarkPostUseCase.invoke(postId: reelId) { result, error in
                DispatchQueue.main.async {
                    if error != nil || result is ResultError {
                        self.bookmarkedReelIds.remove(reelId)
                    }
                }
            }
        }
    }
    
    // MARK: - Recently Viewed
    
    func trackView(reelId: String) {
        recentlyViewedIds.removeAll { $0 == reelId }
        recentlyViewedIds.insert(reelId, at: 0)
        if recentlyViewedIds.count > 50 {
            recentlyViewedIds = Array(recentlyViewedIds.prefix(50))
        }
        saveRecentlyViewed()
    }
    
    func getRecentlyViewed() -> [Product] {
        return recentlyViewedIds.compactMap { id in
            reels.first { String($0.id) == id }
        }
    }
    
    private func saveRecentlyViewed() {
        UserDefaults.standard.set(recentlyViewedIds, forKey: "recently_viewed_reels")
    }
    
    private func loadRecentlyViewed() {
        recentlyViewedIds = UserDefaults.standard.stringArray(forKey: "recently_viewed_reels") ?? []
    }
    
    /// Load the list of user IDs that the current user follows (for Following tab filter)
    func loadFollowingList() {
        let currentUserId = SessionManager.shared.currentUserId
        guard currentUserId != "guest_user" else { return }
        
        getFollowingUseCase.invoke(userId: currentUserId) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<NSArray> {
                    if let users = success.data as? [UserProfile] {
                        self.followingUserIds = Set(users.map { $0.uid })
                    }
                }
            }
        }
    }
}
