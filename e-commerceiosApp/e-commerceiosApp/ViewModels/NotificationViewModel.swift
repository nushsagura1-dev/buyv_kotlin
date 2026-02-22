import Foundation
import SwiftUI
import Combine

// MARK: - Notification API Models

struct NotificationResponse: Codable, Identifiable {
    let id: Int
    let userId: String
    let title: String
    let body: String
    let type: String?
    let data: [String: String]?
    let isRead: Bool
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case userId
        case title, body, type, data
        case isRead
        case createdAt
    }
    
    /// Map notification type to SF Symbol
    var icon: String {
        switch type {
        case "new_follower": return "person.fill.badge.plus"
        case "new_like": return "heart.fill"
        case "new_comment": return "bubble.left.fill"
        case "new_order", "order_status_update": return "bag.fill"
        case "promotion": return "megaphone.fill"
        default: return "bell.fill"
        }
    }
    
    /// Map notification type to color
    var iconColor: Color {
        switch type {
        case "new_follower": return .blue
        case "new_like": return .red
        case "new_comment": return .green
        case "new_order", "order_status_update": return .orange
        case "promotion": return .purple
        default: return .gray
        }
    }
    
    /// Formatted relative time
    var relativeTime: String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let date = formatter.date(from: createdAt) else {
            // Try without fractional seconds
            formatter.formatOptions = [.withInternetDateTime]
            guard let date2 = formatter.date(from: createdAt) else {
                return createdAt
            }
            return RelativeDateTimeFormatter().localizedString(for: date2, relativeTo: Date())
        }
        
        return RelativeDateTimeFormatter().localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - ViewModel

@MainActor
class NotificationViewModel: ObservableObject {
    @Published var notifications: [NotificationResponse] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    var unreadCount: Int {
        notifications.filter { !$0.isRead }.count
    }
    
    private let baseURL = ApiConfig.baseURL
    
    private let session = URLSession.shared
    
    func loadNotifications() {
        guard let token = SessionManager.shared.authToken else {
            errorMessage = "Please log in to view notifications"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                var request = URLRequest(url: URL(string: "\(baseURL)/notifications/me")!)
                request.httpMethod = "GET"
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                
                let (data, response) = try await session.data(for: request)
                
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode) else {
                    let httpResponse = response as? HTTPURLResponse
                    errorMessage = "Failed to load notifications (HTTP \(httpResponse?.statusCode ?? 0))"
                    isLoading = false
                    return
                }
                
                let decoder = JSONDecoder()
                let notifs = try decoder.decode([NotificationResponse].self, from: data)
                self.notifications = notifs
                self.isLoading = false
                
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    func markAsRead(_ notification: NotificationResponse) {
        guard let token = SessionManager.shared.authToken else { return }
        
        // Optimistic UI update
        if let index = notifications.firstIndex(where: { $0.id == notification.id }) {
            // Create updated notification (since struct is immutable, we reload)
            Task {
                do {
                    var request = URLRequest(url: URL(string: "\(baseURL)/notifications/\(notification.id)/read")!)
                    request.httpMethod = "POST"
                    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                    
                    let (_, response) = try await session.data(for: request)
                    
                    guard let httpResponse = response as? HTTPURLResponse,
                          (200...299).contains(httpResponse.statusCode) else { return }
                    
                    // Reload to get fresh state
                    loadNotifications()
                } catch {
                    AppLogger.error("Failed to mark notification as read: \(error)")
                }
            }
        }
    }
    
    func clearAll() {
        guard let token = SessionManager.shared.authToken else { return }
        
        // Optimistic UI update
        let backup = notifications
        notifications.removeAll()
        
        Task {
            do {
                var request = URLRequest(url: URL(string: "\(baseURL)/notifications/")!)
                request.httpMethod = "DELETE"
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                
                let (_, response) = try await session.data(for: request)
                
                guard let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode) else {
                    // Revert on failure
                    self.notifications = backup
                    self.errorMessage = "Failed to clear notifications"
                    return
                }
            } catch {
                self.notifications = backup
                self.errorMessage = error.localizedDescription
            }
        }
    }
    
    func markAllAsRead() {
        guard let token = SessionManager.shared.authToken else { return }
        
        let unread = notifications.filter { !$0.isRead }
        guard !unread.isEmpty else { return }
        
        // Mark all as read by calling markAsRead for each unread
        Task {
            for notif in unread {
                do {
                    var request = URLRequest(url: URL(string: "\(baseURL)/notifications/\(notif.id)/read")!)
                    request.httpMethod = "POST"
                    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                    let _ = try await session.data(for: request)
                } catch {
                    // Continue with others
                }
            }
            // Reload all to reflect state
            loadNotifications()
        }
    }
    
    func deleteNotification(at offsets: IndexSet) {
        guard let token = SessionManager.shared.authToken else { return }
        
        let toDelete = offsets.map { notifications[$0] }
        
        // Optimistic UI update
        let backup = notifications
        notifications.remove(atOffsets: offsets)
        
        Task {
            for notif in toDelete {
                do {
                    var request = URLRequest(url: URL(string: "\(baseURL)/notifications/\(notif.id)")!)
                    request.httpMethod = "DELETE"
                    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                    
                    let (_, response) = try await session.data(for: request)
                    
                    guard let httpResponse = response as? HTTPURLResponse,
                          (200...299).contains(httpResponse.statusCode) else {
                        // Revert on failure
                        self.notifications = backup
                        self.errorMessage = "Failed to delete notification"
                        return
                    }
                } catch {
                    self.notifications = backup
                    self.errorMessage = error.localizedDescription
                    return
                }
            }
        }
    }
}
