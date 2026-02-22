import Foundation
import UserNotifications

// NOTE: Add Firebase SDK via Swift Package Manager:
//   URL: https://github.com/firebase/firebase-ios-sdk
//   Select: FirebaseMessaging, FirebaseAnalytics
// Firebase auto-activates when the SDK is added — no manual uncommenting needed.

#if canImport(FirebaseCore)
import FirebaseCore
#endif

#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif

/// AppDelegate pour gérer les notifications et l'initialisation Firebase
/// Utilisé par SwiftUI via UIApplicationDelegateAdaptor
class AppDelegate: NSObject, UIApplicationDelegate {
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        
        // Firebase — auto-configures when SDK is present
        #if canImport(FirebaseCore)
        FirebaseApp.configure()
        AppLogger.success("Firebase configured successfully")
        #else
        AppLogger.info("Firebase SDK not installed — push notifications via APNS only")
        #endif
        
        #if canImport(FirebaseMessaging)
        Messaging.messaging().delegate = self
        #endif
        
        // Demander les permissions de notifications
        NotificationService.shared.requestPermissions()
        
        return true
    }
    
    // MARK: - Remote Notifications
    
    /// Token APNS reçu avec succès
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()
        AppLogger.network("APNS Device Token received")
        
        #if canImport(FirebaseMessaging)
        Messaging.messaging().apnsToken = deviceToken
        #endif
    }
    
    /// Échec d'enregistrement APNS
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        AppLogger.error("Failed to register for remote notifications: \(error.localizedDescription)")
    }
    
    /// Notification reçue en arrière-plan — route via NavigationManager
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        AppLogger.network("Remote notification received (background)")
        
        // Route notification to the appropriate screen
        handleNotificationNavigation(userInfo: userInfo)
        
        completionHandler(.newData)
    }
    
    // MARK: - Notification Routing
    
    /// Routes push notification data to NavigationManager for deep link navigation
    private func handleNotificationNavigation(userInfo: [AnyHashable: Any]) {
        guard let type = userInfo["type"] as? String else { return }
        
        DispatchQueue.main.async {
            switch type {
            case "new_follower":
                if let userId = userInfo["follower_id"] as? String {
                    NavigationManager.shared.navigateToProfile(userId: userId)
                }
            case "new_like", "new_comment":
                if let postId = userInfo["post_id"] as? String {
                    NavigationManager.shared.navigateToPost(postId: postId)
                }
            case "new_order", "order_status_update":
                if let orderId = userInfo["order_id"] as? String {
                    NavigationManager.shared.navigateToOrder(orderId: orderId)
                }
            case "product_promotion":
                if let productId = userInfo["product_id"] as? String {
                    NavigationManager.shared.navigateToProduct(productId: productId)
                }
            default:
                AppLogger.warning("Unhandled notification type: \(type)")
            }
        }
    }
}

// MARK: - Firebase Messaging Delegate
#if canImport(FirebaseMessaging)
extension AppDelegate: MessagingDelegate {
    
    /// Called when FCM token is received or refreshed
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        AppLogger.network("FCM Token received")
        
        // Register the FCM token with NotificationService
        NotificationService.shared.registerFCMToken(token)
        
        // Send token to BuyV backend for push targeting
        NotificationService.shared.sendTokenToBackend(token: token)
    }
}
#endif
