import Foundation
import UserNotifications

/// Service de gestion des notifications Firebase pour iOS
/// Gère l'enregistrement FCM, la réception des notifications, et les permissions
class NotificationService: NSObject, UNUserNotificationCenterDelegate {
    
    static let shared = NotificationService()
    
    private var fcmToken: String?
    
    private let baseURL = ApiConfig.baseURL
    
    private override init() {
        super.init()
    }
    
    // MARK: - Configuration
    
    /// Demande les permissions de notifications à l'utilisateur
    func requestPermissions() {
        UNUserNotificationCenter.current().delegate = self
        
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions
        ) { granted, error in
            if let error = error {
                AppLogger.error("Notification permission error: \(error.localizedDescription)")
                return
            }
            
            if granted {
                AppLogger.success("Notification permission granted")
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            } else {
                AppLogger.warning("Notification permission denied")
            }
        }
    }
    
    /// Enregistre le token FCM reçu depuis Firebase
    func registerFCMToken(_ token: String) {
        self.fcmToken = token
        AppLogger.network("FCM Token registered")
    }
    
    /// Envoie le FCM token au backend pour le ciblage push
    func sendTokenToBackend(token: String) {
        guard let authToken = SessionManager.shared.authToken else {
            AppLogger.warning("Cannot send FCM token — user not authenticated")
            return
        }
        
        guard let url = URL(string: "\(baseURL)/notifications/register-device") else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        
        let body: [String: Any] = [
            "fcm_token": token,
            "platform": "ios",
            "device_id": UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        ]
        
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                AppLogger.error("Failed to send FCM token to backend: \(error.localizedDescription)")
                return
            }
            
            if let httpResponse = response as? HTTPURLResponse,
               (200...299).contains(httpResponse.statusCode) {
                AppLogger.success("FCM token sent to backend successfully")
            } else {
                AppLogger.warning("Backend rejected FCM token registration")
            }
        }.resume()
    }
    
    /// Récupère le token FCM actuel
    func getFCMToken() -> String? {
        return fcmToken
    }
    
    // MARK: - UNUserNotificationCenterDelegate
    
    /// Notification reçue quand l'app est au premier plan
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        
        AppLogger.network("Notification received (foreground)")
        
        // Extraire les données de la notification
        if let notificationType = userInfo["type"] as? String {
            handleNotification(type: notificationType, data: userInfo)
        }
        
        // Afficher la notification même si l'app est ouverte
        completionHandler([.banner, .sound, .badge])
    }
    
    /// Notification tapée par l'utilisateur — naviguer via NavigationManager
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        
        AppLogger.network("Notification tapped")
        
        // Navigate using NavigationManager instead of NSNotification
        if let notificationType = userInfo["type"] as? String {
            navigateFromNotification(type: notificationType, data: userInfo)
        }
        
        completionHandler()
    }
    
    // MARK: - Notification Handling
    
    /// Gère les différents types de notifications (foreground display)
    private func handleNotification(type: String, data: [AnyHashable: Any]) {
        switch type {
        case "new_follower":
            if let username = data["follower_username"] as? String {
                AppLogger.info("New follower: \(username)")
            }
        case "new_like":
            if let postId = data["post_id"] as? String {
                AppLogger.info("New like on post: \(postId)")
            }
        case "new_comment":
            if let postId = data["post_id"] as? String {
                AppLogger.info("New comment on post: \(postId)")
            }
        case "new_order":
            if let orderId = data["order_id"] as? String {
                AppLogger.info("New order: \(orderId)")
            }
        case "order_status_update":
            if let orderId = data["order_id"] as? String,
               let status = data["status"] as? String {
                AppLogger.info("Order \(orderId) status: \(status)")
            }
        default:
            AppLogger.warning("Unknown notification type: \(type)")
        }
    }
    
    /// Navigation basée sur le type de notification — uses NavigationManager
    private func navigateFromNotification(type: String, data: [AnyHashable: Any]) {
        DispatchQueue.main.async {
            switch type {
            case "new_follower":
                if let userId = data["follower_id"] as? String {
                    NavigationManager.shared.navigateToProfile(userId: userId)
                }
            case "new_like", "new_comment":
                if let postId = data["post_id"] as? String {
                    NavigationManager.shared.navigateToPost(postId: postId)
                }
            case "new_order", "order_status_update":
                if let orderId = data["order_id"] as? String {
                    NavigationManager.shared.navigateToOrder(orderId: orderId)
                }
            case "product_promotion":
                if let productId = data["product_id"] as? String {
                    NavigationManager.shared.navigateToProduct(productId: productId)
                }
            default:
                AppLogger.warning("Unhandled navigation for notification type: \(type)")
            }
        }
    }
}
