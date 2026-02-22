import Foundation
import Shared

// MARK: - Deletion Phase
enum DeletionPhase: String {
    case preparing = "Preparing..."
    case deletingPosts = "Removing your posts..."
    case deletingComments = "Removing your comments..."
    case deletingOrders = "Archiving your orders..."
    case deletingProfile = "Deleting your profile..."
    case cleaningUp = "Cleaning up..."
    case complete = "Account deleted"
}

class DeleteAccountViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isDeleted = false
    @Published var currentPhase: DeletionPhase = .preparing
    @Published var deletionProgress: Double = 0.0
    
    private let deleteAccountUseCase = DependencyWrapper.shared.deleteAccountUseCase
    
    /// Items that will be deleted — shown to user before confirming
    let deletionItems: [(icon: String, title: String, detail: String)] = [
        ("doc.text", "Posts & Reels", "All your posts, reels, and media content"),
        ("bubble.left.and.bubble.right", "Comments & Likes", "All your comments, likes, and bookmarks"),
        ("person.2", "Social Connections", "Your followers and following list"),
        ("bag", "Order History", "Your order history will be archived"),
        ("bell", "Notifications", "All notification preferences and history"),
        ("person.crop.circle", "Profile Data", "Your profile, bio, and profile picture")
    ]
    
    func deleteAccount() {
        isLoading = true
        errorMessage = nil
        currentPhase = .preparing
        deletionProgress = 0.0
        
        let userId = SessionManager.shared.currentUserId
        
        // Simulate phased deletion with progress
        simulateDeletionPhases()
        
        deleteAccountUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                if let error = error {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                    self.currentPhase = .preparing
                    self.deletionProgress = 0
                    return
                }
                
                if result is ResultSuccess<AnyObject> {
                    self.currentPhase = .complete
                    self.deletionProgress = 1.0
                    
                    // Clear all local data
                    self.clearLocalData()
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        self.isLoading = false
                        self.isDeleted = true
                    }
                } else if let errorResult = result as? ResultError {
                    self.isLoading = false
                    self.errorMessage = errorResult.error.message
                    self.currentPhase = .preparing
                    self.deletionProgress = 0
                }
            }
        }
    }
    
    private func simulateDeletionPhases() {
        let phases: [(DeletionPhase, Double, TimeInterval)] = [
            (.preparing, 0.1, 0.3),
            (.deletingPosts, 0.3, 0.8),
            (.deletingComments, 0.5, 1.3),
            (.deletingOrders, 0.7, 1.8),
            (.deletingProfile, 0.85, 2.3),
            (.cleaningUp, 0.95, 2.8)
        ]
        
        for (phase, progress, delay) in phases {
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                guard self.isLoading else { return }
                withAnimation(.easeInOut(duration: 0.3)) {
                    self.currentPhase = phase
                    self.deletionProgress = progress
                }
            }
        }
    }
    
    private func clearLocalData() {
        // Clear locally cached data
        UserDefaults.standard.removeObject(forKey: "recently_viewed_reels")
        UserDefaults.standard.removeObject(forKey: "search_history")
        UserDefaults.standard.removeObject(forKey: "hasSeenOnboarding")
        SessionManager.shared.clearUser()
    }
}
