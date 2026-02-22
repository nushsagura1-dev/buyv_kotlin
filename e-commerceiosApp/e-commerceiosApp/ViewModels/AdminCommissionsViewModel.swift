import Foundation
import SwiftUI

@MainActor
class AdminCommissionsViewModel: ObservableObject {
    @Published var commissions: [CommissionResponse] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var selectedFilter: String? = nil // nil = all
    @Published var actionInProgress = false
    @Published var successMessage: String?
    @Published var selectedCommission: CommissionResponse? = nil
    @Published var updatingCommissionId: Int? = nil
    
    let statusFilters = ["pending", "approved", "rejected", "paid"]
    
    // MARK: - Currency Formatter
    private static let currencyFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "USD"
        f.maximumFractionDigits = 2
        return f
    }()
    
    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
    
    private static let isoFormatterFallback: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()
    
    private static let displayDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f
    }()
    
    // MARK: - Computed Properties
    var filteredCommissions: [CommissionResponse] {
        guard let filter = selectedFilter else { return commissions }
        return commissions.filter { $0.status == filter }
    }
    
    var stats: [String: Int] {
        var result: [String: Int] = ["all": commissions.count]
        for status in statusFilters {
            result[status] = commissions.filter { $0.status == status }.count
        }
        return result
    }
    
    var totalAmount: Double {
        filteredCommissions.reduce(0) { $0 + $1.commissionAmount }
    }
    
    var formattedTotalAmount: String {
        Self.formatCurrency(totalAmount)
    }
    
    var overallTotalAmount: Double {
        commissions.reduce(0) { $0 + $1.commissionAmount }
    }
    
    var formattedOverallTotal: String {
        Self.formatCurrency(overallTotalAmount)
    }
    
    var pendingAmount: Double {
        commissions.filter { $0.status == "pending" }.reduce(0) { $0 + $1.commissionAmount }
    }
    
    var formattedPendingAmount: String {
        Self.formatCurrency(pendingAmount)
    }
    
    var paidAmount: Double {
        commissions.filter { $0.status == "paid" }.reduce(0) { $0 + $1.commissionAmount }
    }
    
    var formattedPaidAmount: String {
        Self.formatCurrency(paidAmount)
    }
    
    var isEmpty: Bool { commissions.isEmpty && !isLoading }
    var hasCommissions: Bool { !commissions.isEmpty }
    var hasError: Bool { errorMessage != nil }
    var filteredCount: Int { filteredCommissions.count }
    var totalCount: Int { commissions.count }
    
    /// Status badges with label, count, and color
    var statusBadges: [(status: String?, label: String, count: Int, color: Color)] {
        var badges: [(status: String?, label: String, count: Int, color: Color)] = [
            (nil, "All", commissions.count, .primary)
        ]
        for status in statusFilters {
            let count = commissions.filter { $0.status == status }.count
            badges.append((status, status.capitalized, count, Self.statusColor(status)))
        }
        return badges
    }
    
    // MARK: - Data Loading
    func loadCommissions() async {
        isLoading = true
        errorMessage = nil
        
        do {
            commissions = try await AdminApiService.shared.getAllCommissions()
            isLoading = false
            isRefreshing = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
            isRefreshing = false
        }
    }
    
    // MARK: - Status Update
    func updateStatus(commissionId: Int, newStatus: String) async {
        actionInProgress = true
        updatingCommissionId = commissionId
        
        do {
            let response = try await AdminApiService.shared.updateCommissionStatus(
                commissionId: commissionId,
                status: newStatus
            )
            successMessage = response.message
            // Reload to get fresh data
            await loadCommissions()
        } catch {
            errorMessage = error.localizedDescription
        }
        
        actionInProgress = false
        updatingCommissionId = nil
    }
    
    func isUpdating(_ commissionId: Int) -> Bool {
        updatingCommissionId == commissionId
    }
    
    // MARK: - Selection & Filters
    func selectFilter(_ filter: String?) {
        selectedFilter = filter
    }
    
    func selectCommission(_ commission: CommissionResponse?) {
        selectedCommission = commission
    }
    
    // MARK: - Actions
    func refresh() async {
        isRefreshing = true
        await loadCommissions()
    }
    
    func retry() async {
        errorMessage = nil
        await loadCommissions()
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    func clearSuccess() {
        successMessage = nil
    }
    
    // MARK: - Static Helpers
    static func formatCurrency(_ amount: Double) -> String {
        currencyFormatter.string(from: NSNumber(value: amount)) ?? "$\(String(format: "%.2f", amount))"
    }
    
    static func formatDate(_ dateString: String) -> String {
        if let date = isoFormatter.date(from: dateString) {
            return displayDateFormatter.string(from: date)
        }
        if let date = isoFormatterFallback.date(from: dateString) {
            return displayDateFormatter.string(from: date)
        }
        return dateString
    }
    
    static func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "pending":   return .orange
        case "approved":  return .blue
        case "rejected":  return .red
        case "paid":      return .green
        default:          return .gray
        }
    }
    
    static func statusIcon(_ status: String) -> String {
        switch status.lowercased() {
        case "pending":   return "clock.fill"
        case "approved":  return "checkmark.circle.fill"
        case "rejected":  return "xmark.circle.fill"
        case "paid":      return "dollarsign.circle.fill"
        default:          return "questionmark.circle"
        }
    }
}
