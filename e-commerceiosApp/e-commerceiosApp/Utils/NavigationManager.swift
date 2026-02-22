import SwiftUI
import Combine

/// Manages deep link and notification-driven navigation across the app.
/// Observed by MainTabView to trigger programmatic navigation.
class NavigationManager: ObservableObject {
    static let shared = NavigationManager()
    
    // MARK: - Navigation Destinations
    
    enum Destination: Equatable {
        case profile(userId: String)
        case product(productId: String)
        case order(orderId: String)
        case post(postId: String) // navigates to ReelsView with specific reel
        
        static func == (lhs: Destination, rhs: Destination) -> Bool {
            switch (lhs, rhs) {
            case (.profile(let a), .profile(let b)): return a == b
            case (.product(let a), .product(let b)): return a == b
            case (.order(let a), .order(let b)): return a == b
            case (.post(let a), .post(let b)): return a == b
            default: return false
            }
        }
    }
    
    // MARK: - Published State
    
    /// The pending navigation destination. Views observe this and navigate when set.
    @Published var pendingDestination: Destination?
    
    /// Which tab to select (0=Home, 1=Discover, 2=Reels, 3=Cart, 4=Profile)
    @Published var selectedTab: Int = 0
    
    // Sheet presentation states
    @Published var showProfileSheet: Bool = false
    @Published var showProductDetail: Bool = false
    @Published var showOrderDetail: Bool = false
    
    // Detail IDs for sheet navigation
    @Published var targetProfileUserId: String?
    @Published var targetProductId: String?
    @Published var targetOrderId: String?
    
    // MARK: - Navigation Methods
    
    func navigateToProfile(userId: String) {
        DispatchQueue.main.async {
            self.targetProfileUserId = userId
            self.selectedTab = 0 // Home tab
            self.showProfileSheet = true
            self.pendingDestination = .profile(userId: userId)
        }
    }
    
    func navigateToProduct(productId: String) {
        DispatchQueue.main.async {
            self.targetProductId = productId
            self.selectedTab = 0 // Home tab
            self.showProductDetail = true
            self.pendingDestination = .product(productId: productId)
        }
    }
    
    func navigateToOrder(orderId: String) {
        DispatchQueue.main.async {
            self.targetOrderId = orderId
            self.selectedTab = 4 // Profile tab (orders are under settings)
            self.showOrderDetail = true
            self.pendingDestination = .order(orderId: orderId)
        }
    }
    
    func navigateToPost(postId: String) {
        DispatchQueue.main.async {
            self.selectedTab = 2 // Reels tab
            self.pendingDestination = .post(postId: postId)
        }
    }
    
    /// Clear pending navigation after it has been handled
    func clearDestination() {
        pendingDestination = nil
        showProfileSheet = false
        showProductDetail = false
        showOrderDetail = false
        targetProfileUserId = nil
        targetProductId = nil
        targetOrderId = nil
    }
}
