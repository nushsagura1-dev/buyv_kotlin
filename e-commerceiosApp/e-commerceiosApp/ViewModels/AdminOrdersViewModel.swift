import Foundation
import SwiftUI
import Combine

struct OrderStats {
    var total: Int = 0
    var pending: Int = 0
    var processing: Int = 0
    var shipped: Int = 0
    var delivered: Int = 0
    var cancelled: Int = 0
}

@MainActor
class AdminOrdersViewModel: ObservableObject {
    // MARK: - Status Options
    static let allStatuses = ["pending", "processing", "shipped", "delivered", "cancelled"]
    
    // MARK: - Published Properties
    @Published var selectedStatus: String?
    @Published var searchQuery: String = ""
    
    @Published var allOrders: [AdminOrderResponse] = []
    @Published var filteredOrders: [AdminOrderResponse] = []
    @Published var stats: OrderStats = OrderStats()
    
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var errorMessage: String?
    @Published var updatingOrderId: Int?
    
    // API Service
    private let adminApi = AdminApiService.shared
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Computed Properties
    var hasError: Bool { errorMessage != nil }
    var isEmpty: Bool { filteredOrders.isEmpty && !isLoading }
    var totalCount: Int { allOrders.count }
    var filteredCount: Int { filteredOrders.count }
    var hasOrders: Bool { !allOrders.isEmpty }
    
    var totalRevenue: Double {
        allOrders.filter { $0.status == "delivered" }
            .reduce(0) { $0 + $1.totalAmount }
    }
    
    var formattedTotalRevenue: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        return formatter.string(from: NSNumber(value: totalRevenue)) ?? "$\(String(format: "%.2f", totalRevenue))"
    }
    
    /// All status badge data for the filter chips
    var statusBadges: [(status: String, label: String, count: Int, color: Color)] {
        [
            ("pending", "Pending", stats.pending, .orange),
            ("processing", "Processing", stats.processing, .blue),
            ("shipped", "Shipped", stats.shipped, .purple),
            ("delivered", "Delivered", stats.delivered, .green),
            ("cancelled", "Cancelled", stats.cancelled, .red)
        ]
    }
    
    // MARK: - Initialization
    init() {
        setupObservers()
    }
    
    // MARK: - Setup Observers
    private func setupObservers() {
        $selectedStatus
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
        
        $searchQuery
            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Methods
    func loadOrders() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let orders = try await adminApi.getAllOrders()
                self.allOrders = orders
                self.calculateStats()
                self.applyFilters()
                self.isLoading = false
                self.isRefreshing = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
                self.isRefreshing = false
            }
        }
    }
    
    private func calculateStats() {
        stats = OrderStats(
            total: allOrders.count,
            pending: allOrders.filter { $0.status == "pending" }.count,
            processing: allOrders.filter { $0.status == "processing" }.count,
            shipped: allOrders.filter { $0.status == "shipped" }.count,
            delivered: allOrders.filter { $0.status == "delivered" }.count,
            cancelled: allOrders.filter { $0.status == "cancelled" }.count
        )
    }
    
    func applyFilters() {
        var result = allOrders
        
        // Status filter
        if let status = selectedStatus {
            result = result.filter { $0.status == status }
        }
        
        // Search filter
        let query = searchQuery.trimmingCharacters(in: .whitespaces).lowercased()
        if !query.isEmpty {
            result = result.filter { order in
                "\(order.id)".contains(query) ||
                (order.userEmail?.lowercased().contains(query) ?? false) ||
                order.status.lowercased().contains(query)
            }
        }
        
        // Sort by most recent first
        result.sort { $0.id > $1.id }
        
        filteredOrders = result
    }
    
    func updateOrderStatus(_ order: AdminOrderResponse, newStatus: String) {
        updatingOrderId = order.id
        Task {
            do {
                _ = try await adminApi.updateOrderStatus(orderId: order.id, status: newStatus)
                // Reload to get fresh data from backend
                loadOrders()
            } catch {
                errorMessage = error.localizedDescription
            }
            updatingOrderId = nil
        }
    }
    
    func isUpdating(_ orderId: Int) -> Bool {
        updatingOrderId == orderId
    }
    
    // MARK: - Actions
    func refresh() {
        isRefreshing = true
        loadOrders()
    }
    
    func retry() {
        errorMessage = nil
        loadOrders()
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    func clearFilters() {
        selectedStatus = nil
        searchQuery = ""
    }
    
    // MARK: - Date Formatting
    static func formatOrderDate(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        
        // Fallback: try without fractional seconds
        isoFormatter.formatOptions = [.withInternetDateTime]
        if let date = isoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
    
    static func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "pending": return .orange
        case "processing": return .blue
        case "shipped": return .purple
        case "delivered": return .green
        case "cancelled": return .red
        default: return .gray
        }
    }
}
