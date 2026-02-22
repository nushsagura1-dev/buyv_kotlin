import Foundation
import Shared

class FavouriteViewModel: ObservableObject {
    @Published var bookmarkedPosts: [Post] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var removingPostIds: Set<String> = []
    
    private let getBookmarkedPostsUseCase = DependencyWrapper.shared.getBookmarkedPostsUseCase
    private let unbookmarkPostUseCase = DependencyWrapper.shared.unbookmarkPostUseCase
    
    // MARK: - Computed Properties
    var isEmpty: Bool { bookmarkedPosts.isEmpty }
    var bookmarkCount: Int { bookmarkedPosts.count }
    var hasError: Bool { errorMessage != nil }
    
    // MARK: - Load Bookmarks
    func loadBookmarks() {
        isLoading = bookmarkedPosts.isEmpty
        errorMessage = nil
        
        let userId = SessionManager.shared.currentUserId
        
        getBookmarkedPostsUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    return
                }
                
                if let result = result as? ResultSuccess<NSArray> {
                    self.bookmarkedPosts = (result.data as? [Post]) ?? []
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    // MARK: - Remove Bookmark (with backend call + optimistic UI)
    func removeBookmark(postId: String) {
        guard !removingPostIds.contains(postId) else { return }
        
        // Optimistic removal — save for rollback
        let removedPost = bookmarkedPosts.first { $0.id == postId }
        let removedIndex = bookmarkedPosts.firstIndex { $0.id == postId }
        
        removingPostIds.insert(postId)
        bookmarkedPosts.removeAll { $0.id == postId }
        
        let userId = SessionManager.shared.currentUserId
        
        unbookmarkPostUseCase.invoke(userId: userId, postId: postId) { result, error in
            DispatchQueue.main.async {
                self.removingPostIds.remove(postId)
                
                if let error = error {
                    // Rollback on failure
                    if let post = removedPost, let index = removedIndex {
                        let safeIndex = min(index, self.bookmarkedPosts.count)
                        self.bookmarkedPosts.insert(post, at: safeIndex)
                    }
                    self.errorMessage = "Failed to remove bookmark: \(error.localizedDescription)"
                    return
                }
                
                if let errorResult = result as? ResultError {
                    // Rollback on API error
                    if let post = removedPost, let index = removedIndex {
                        let safeIndex = min(index, self.bookmarkedPosts.count)
                        self.bookmarkedPosts.insert(post, at: safeIndex)
                    }
                    self.errorMessage = "Failed to remove bookmark: \(errorResult.error.message)"
                }
                // Success: post already removed optimistically
            }
        }
    }
    
    // MARK: - Check if post is bookmarked
    func isBookmarked(postId: String) -> Bool {
        bookmarkedPosts.contains { $0.id == postId }
    }
    
    // MARK: - Refresh
    func refresh() {
        loadBookmarks()
    }
    
    // MARK: - Helpers
    func clearError() {
        errorMessage = nil
    }
    
    func isRemoving(postId: String) -> Bool {
        removingPostIds.contains(postId)
    }
}
