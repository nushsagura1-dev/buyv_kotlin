import Foundation
import SwiftUI
import Combine

struct AdminStats {
    var totalUsers: Int = 0
    var totalOrders: Int = 0
    var totalProducts: Int = 0
    var totalPosts: Int = 0
    var verifiedUsers: Int = 0
    var newUsersToday: Int = 0
    var newUsersThisWeek: Int = 0
    var totalReels: Int = 0
    var pendingOrders: Int = 0
    var totalRevenue: Double = 0.0
}

@MainActor
class AdminDashboardViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var stats: AdminStats = AdminStats()
    @Published var recentUsers: [RecentUserResponse] = []
    @Published var recentOrders: [RecentOrderResponse] = []
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var errorMessage: String?
    
    private let adminApi = AdminApiService.shared
    
    // MARK: - Formatted Stat Helpers
    var formattedTotalRevenue: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSNumber(value: stats.totalRevenue)) ?? "$\(String(format: "%.2f", stats.totalRevenue))"
    }
    
    var formattedUserCount: String { formatCompact(stats.totalUsers) }
    var formattedOrderCount: String { formatCompact(stats.totalOrders) }
    var formattedProductCount: String { formatCompact(stats.totalProducts) }
    var formattedPostCount: String { formatCompact(stats.totalPosts) }
    var formattedReelCount: String { formatCompact(stats.totalReels) }
    
    // MARK: - Computed Properties
    var hasData: Bool { stats.totalUsers > 0 || stats.totalOrders > 0 }
    var hasError: Bool { errorMessage != nil }
    var hasRecentUsers: Bool { !recentUsers.isEmpty }
    var hasRecentOrders: Bool { !recentOrders.isEmpty }
    var pendingOrdersCount: Int { stats.pendingOrders }
    var verifiedUsersCount: Int { stats.verifiedUsers }
    var verificationRate: Double {
        guard stats.totalUsers > 0 else { return 0 }
        return Double(stats.verifiedUsers) / Double(stats.totalUsers) * 100
    }
    var formattedVerificationRate: String {
        String(format: "%.1f%%", verificationRate)
    }
    
    // MARK: - All Stat Cards Data
    var allStatCards: [(title: String, value: String, icon: String, color: Color)] {
        [
            ("Total Users", "\(stats.totalUsers)", "person.3.fill", .blue),
            ("Total Orders", "\(stats.totalOrders)", "cart.fill", .green),
            ("Total Products", "\(stats.totalProducts)", "bag.fill", .orange),
            ("Total Posts", "\(stats.totalPosts)", "photo.fill", .purple),
            ("Total Reels", "\(stats.totalReels)", "play.rectangle.fill", .pink),
            ("Verified Users", "\(stats.verifiedUsers)", "checkmark.seal.fill", .teal),
            ("Pending Orders", "\(stats.pendingOrders)", "clock.fill", .yellow),
            ("Revenue", formattedTotalRevenue, "dollarsign.circle.fill", .mint)
        ]
    }
    
    // MARK: - Methods
    func loadDashboardData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let response = try await adminApi.getDashboardStats()
            stats = AdminStats(
                totalUsers: response.totalUsers,
                totalOrders: response.totalOrders,
                totalProducts: response.totalProducts,
                totalPosts: response.totalPosts,
                verifiedUsers: response.verifiedUsers,
                newUsersToday: response.newUsersToday,
                newUsersThisWeek: response.newUsersThisWeek,
                totalReels: response.totalReels,
                pendingOrders: response.pendingOrders,
                totalRevenue: response.totalRevenue
            )
            
            // Load recent data in parallel
            async let usersTask = adminApi.getRecentUsers(limit: 5)
            async let ordersTask = adminApi.getRecentOrders(limit: 5)
            
            let (users, orders) = try await (usersTask, ordersTask)
            self.recentUsers = users
            self.recentOrders = orders
            
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
        isRefreshing = false
    }
    
    func refresh() {
        isRefreshing = true
        Task {
            await loadDashboardData()
        }
    }
    
    func retry() {
        errorMessage = nil
        Task {
            await loadDashboardData()
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    func logout() {
        adminApi.logout()
        SessionManager.shared.logout()
    }
    
    // MARK: - Formatting Helpers
    private func formatCompact(_ value: Int) -> String {
        if value >= 1_000_000 {
            return String(format: "%.1fM", Double(value) / 1_000_000)
        } else if value >= 1_000 {
            return String(format: "%.1fK", Double(value) / 1_000)
        }
        return "\(value)"
    }
}
