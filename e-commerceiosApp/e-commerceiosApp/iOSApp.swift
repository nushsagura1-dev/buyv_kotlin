import SwiftUI
import Shared

@main
struct iOSApp: App {
    // Intégrer AppDelegate pour gérer Firebase et notifications
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    init() {
        KoinHelper.Companion.shared.initKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Deep linking handler
                    handleDeepLink(url: url)
                }
                .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("NavigateFromNotification"))) { notification in
                    // Navigation depuis notification
                    handleNotificationNavigation(notification: notification)
                }
        }
    }
    
    // MARK: - Deep Linking
    
    private func handleDeepLink(url: URL) {
        AppLogger.network("Deep link received: \(url)")
        
        // Format attendu: buyv://app/profile/{userId}
        guard url.scheme == "buyv" else {
            AppLogger.warning("Unknown URL scheme: \(url.scheme ?? "nil")")
            return
        }
        
        let path = url.path
        let components = path.components(separatedBy: "/").filter { !$0.isEmpty }
        
        guard !components.isEmpty else { return }
        
        switch components[0] {
        case "profile":
            if components.count > 1 {
                let userId = components[1]
                navigateToProfile(userId: userId)
            }
        case "post":
            if components.count > 1 {
                let postId = components[1]
                navigateToPost(postId: postId)
            }
        case "product":
            if components.count > 1 {
                let productId = components[1]
                navigateToProduct(productId: productId)
            }
        case "order":
            if components.count > 1 {
                let orderId = components[1]
                navigateToOrder(orderId: orderId)
            }
        default:
            AppLogger.warning("Unknown deep link path: \(path)")
        }
    }
    
    // MARK: - Notification Navigation
    
    private func handleNotificationNavigation(notification: Notification) {
        guard let userInfo = notification.userInfo,
              let type = userInfo["type"] as? String,
              let data = userInfo["data"] as? [AnyHashable: Any] else { return }
        
        AppLogger.info("Navigating from notification type: \(type)")
        
        switch type {
        case "new_follower":
            if let userId = data["follower_id"] as? String {
                navigateToProfile(userId: userId)
            }
        case "new_like", "new_comment":
            if let postId = data["post_id"] as? String {
                navigateToPost(postId: postId)
            }
        case "new_order", "order_status_update":
            if let orderId = data["order_id"] as? String {
                navigateToOrder(orderId: orderId)
            }
        default:
            break
        }
    }
    
    // MARK: - Navigation Helpers
    
    private func navigateToProfile(userId: String) {
        AppLogger.debug("Navigate to profile: \(userId)")
        NavigationManager.shared.navigateToProfile(userId: userId)
    }
    
    private func navigateToPost(postId: String) {
        AppLogger.debug("Navigate to post: \(postId)")
        NavigationManager.shared.navigateToPost(postId: postId)
    }
    
    private func navigateToProduct(productId: String) {
        AppLogger.debug("Navigate to product: \(productId)")
        NavigationManager.shared.navigateToProduct(productId: productId)
    }
    
    private func navigateToOrder(orderId: String) {
        AppLogger.debug("Navigate to order: \(orderId)")
        NavigationManager.shared.navigateToOrder(orderId: orderId)
    }
}
