import Foundation
import Shared

class CommentsViewModel: ObservableObject {
    @Published var comments: [CommentData] = []
    @Published var isLoading = false
    @Published var isSending = false
    @Published var errorMessage: String?
    @Published var sortOrder: CommentSortOrder = .newest
    @Published var deletingCommentIds: Set<String> = []
    
    enum CommentSortOrder: String, CaseIterable {
        case newest = "Newest"
        case oldest = "Oldest"
        case mostLiked = "Most Liked"
    }
    
    private let getCommentsUseCase = DependencyWrapper.shared.getCommentsUseCase
    private let addCommentUseCase = DependencyWrapper.shared.addCommentUseCase
    private let deleteCommentUseCase = DependencyWrapper.shared.deleteCommentUseCase
    private let likeCommentUseCase = DependencyWrapper.shared.likeCommentUseCase
    
    struct CommentData: Identifiable {
        let id: String
        let username: String
        let content: String
        let timeAgo: String
        let createdAtTimestamp: TimeInterval
        var likesCount: Int
        var isLiked: Bool
        let isOwn: Bool
    }
    
    // MARK: - Computed Properties
    var commentCount: Int { comments.count }
    var isEmpty: Bool { comments.isEmpty }
    var hasComments: Bool { !comments.isEmpty }
    
    var sortedComments: [CommentData] {
        switch sortOrder {
        case .newest:
            return comments.sorted { $0.createdAtTimestamp > $1.createdAtTimestamp }
        case .oldest:
            return comments.sorted { $0.createdAtTimestamp < $1.createdAtTimestamp }
        case .mostLiked:
            return comments.sorted { $0.likesCount > $1.likesCount }
        }
    }
    
    func setSortOrder(_ order: CommentSortOrder) {
        sortOrder = order
    }
    
    // MARK: - Load Comments
    func loadComments(postId: String) {
        isLoading = comments.isEmpty
        errorMessage = nil
        
        getCommentsUseCase.invoke(postId: postId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    return
                }
                
                if let result = result as? ResultSuccess<NSArray> {
                    let currentUserId = SessionManager.shared.currentUserId
                    self.comments = (result.data as? [Comment_])?.map { comment in
                        let timestamp = self.parseTimestamp(comment.createdAt)
                        return CommentData(
                            id: comment.id,
                            username: comment.username,
                            content: comment.content,
                            timeAgo: self.formatTimeAgo(timestamp),
                            createdAtTimestamp: timestamp,
                            likesCount: Int(comment.likesCount),
                            isLiked: comment.isLiked,
                            isOwn: comment.userId == currentUserId
                        )
                    } ?? []
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    // MARK: - Add Comment (with optimistic insert)
    func addComment(postId: String, content: String) {
        guard !content.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        
        isSending = true
        
        // Optimistic insert
        let optimisticId = UUID().uuidString
        let currentUser = SessionManager.shared.currentUser
        let optimistic = CommentData(
            id: optimisticId,
            username: currentUser?.displayName ?? "You",
            content: content,
            timeAgo: "Just now",
            createdAtTimestamp: Date().timeIntervalSince1970,
            likesCount: 0,
            isLiked: false,
            isOwn: true
        )
        comments.append(optimistic)
        
        let userId = SessionManager.shared.currentUserId
        
        addCommentUseCase.invoke(postId: postId, userId: userId, content: content) { result, error in
            DispatchQueue.main.async {
                self.isSending = false
                
                if let error = error {
                    // Remove optimistic comment on failure
                    self.comments.removeAll { $0.id == optimisticId }
                    self.errorMessage = error.localizedDescription
                    return
                }
                
                // Reload to get server-assigned ID and timestamp
                self.loadComments(postId: postId)
            }
        }
    }
    
    // MARK: - Delete Comment (with optimistic removal)
    func deleteComment(commentId: String, postId: String) {
        guard !deletingCommentIds.contains(commentId) else { return }
        
        let removedComment = comments.first { $0.id == commentId }
        let removedIndex = comments.firstIndex { $0.id == commentId }
        
        deletingCommentIds.insert(commentId)
        comments.removeAll { $0.id == commentId }
        
        deleteCommentUseCase.invoke(commentId: commentId) { result, error in
            DispatchQueue.main.async {
                self.deletingCommentIds.remove(commentId)
                
                if let error = error {
                    // Rollback on failure
                    if let comment = removedComment, let index = removedIndex {
                        let safeIndex = min(index, self.comments.count)
                        self.comments.insert(comment, at: safeIndex)
                    }
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // MARK: - Like Comment (with optimistic toggle)
    func likeComment(commentId: String) {
        // Optimistic toggle
        if let index = comments.firstIndex(where: { $0.id == commentId }) {
            comments[index].isLiked.toggle()
            comments[index].likesCount += comments[index].isLiked ? 1 : -1
        }
        
        likeCommentUseCase.invoke(commentId: commentId) { result, error in
            DispatchQueue.main.async {
                if error != nil {
                    // Revert on failure
                    if let index = self.comments.firstIndex(where: { $0.id == commentId }) {
                        self.comments[index].isLiked.toggle()
                        self.comments[index].likesCount += self.comments[index].isLiked ? 1 : -1
                    }
                }
            }
        }
    }
    
    // MARK: - Helpers
    func clearError() {
        errorMessage = nil
    }
    
    func isDeleting(commentId: String) -> Bool {
        deletingCommentIds.contains(commentId)
    }
    
    private func parseTimestamp(_ dateString: String) -> TimeInterval {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            return date.timeIntervalSince1970
        }
        return Date().timeIntervalSince1970
    }
    
    private func formatTimeAgo(_ timestamp: TimeInterval) -> String {
        let interval = Date().timeIntervalSince1970 - timestamp
        
        if interval < 60 { return "Just now" }
        if interval < 3600 { return "\(Int(interval / 60))m" }
        if interval < 86400 { return "\(Int(interval / 3600))h" }
        if interval < 604800 { return "\(Int(interval / 86400))d" }
        return "\(Int(interval / 604800))w"
    }
}
