import Foundation
import SwiftUI

@MainActor
class PromoterDashboardViewModel: ObservableObject {
    // MARK: - Time Period Filter
    enum TimePeriod: String, CaseIterable {
        case week = "7d"
        case month = "30d"
        case quarter = "90d"
        case allTime = "all"
        
        var label: String {
            switch self {
            case .week: return "This Week"
            case .month: return "This Month"
            case .quarter: return "Quarter"
            case .allTime: return "All Time"
            }
        }
        
        var icon: String {
            switch self {
            case .week: return "calendar"
            case .month: return "calendar.badge.clock"
            case .quarter: return "calendar.badge.plus"
            case .allTime: return "infinity"
            }
        }
    }
    
    // MARK: - Published Properties
    @Published var wallet: WalletResponse?
    @Published var commissions: [CommissionResponse] = []
    @Published var promotions: [PromotionResponse] = []
    @Published var recentSales: [AffiliateSaleResponse] = []
    @Published var selectedPeriod: TimePeriod = .allTime
    
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var errorMessage: String?
    
    private let api = MarketplaceApiService.shared
    private static let currencyFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "USD"
        f.maximumFractionDigits = 2
        return f
    }()
    
    private static let percentFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .percent
        f.maximumFractionDigits = 1
        f.multiplier = 1
        return f
    }()
    
    // MARK: - Raw Computed Properties
    var totalEarned: Double { wallet?.totalEarned ?? 0 }
    var availableBalance: Double { wallet?.availableAmount ?? 0 }
    var pendingAmount: Double { wallet?.pendingAmount ?? 0 }
    var withdrawnAmount: Double { wallet?.withdrawnAmount ?? 0 }
    var totalSalesCount: Int { wallet?.totalSalesCount ?? 0 }
    
    var totalCommissionsAmount: Double {
        commissions.reduce(0) { $0 + $1.commissionAmount }
    }
    
    var pendingCommissions: Int {
        commissions.filter { $0.status == "pending" }.count
    }
    
    var paidCommissions: Int {
        commissions.filter { $0.status == "paid" }.count
    }
    
    var totalPromotionViews: Int {
        promotions.reduce(0) { $0 + ($1.viewsCount ?? 0) }
    }
    
    var totalPromotionClicks: Int {
        promotions.reduce(0) { $0 + ($1.clicksCount ?? 0) }
    }
    
    var totalPromotionSales: Int {
        promotions.reduce(0) { $0 + ($1.salesCount ?? 0) }
    }
    
    var ctr: Double {
        guard totalPromotionViews > 0 else { return 0 }
        return Double(totalPromotionClicks) / Double(totalPromotionViews) * 100
    }
    
    var conversionRate: Double {
        guard totalPromotionClicks > 0 else { return 0 }
        return Double(totalPromotionSales) / Double(totalPromotionClicks) * 100
    }
    
    // MARK: - Formatted Computed Properties
    var formattedTotalEarned: String { formatCurrency(totalEarned) }
    var formattedAvailableBalance: String { formatCurrency(availableBalance) }
    var formattedPendingAmount: String { formatCurrency(pendingAmount) }
    var formattedWithdrawnAmount: String { formatCurrency(withdrawnAmount) }
    var formattedTotalCommissions: String { formatCurrency(totalCommissionsAmount) }
    
    var formattedCTR: String { formatPercent(ctr) }
    var formattedConversionRate: String { formatPercent(conversionRate) }
    
    var formattedViews: String { formatCompact(totalPromotionViews) }
    var formattedClicks: String { formatCompact(totalPromotionClicks) }
    var formattedSalesCount: String { "\(totalSalesCount)" }
    
    // MARK: - Status Computed Properties
    var hasData: Bool { wallet != nil }
    var hasError: Bool { errorMessage != nil }
    var hasCommissions: Bool { !commissions.isEmpty }
    var hasPromotions: Bool { !promotions.isEmpty }
    var hasSales: Bool { !recentSales.isEmpty }
    var activePromotionsCount: Int { promotions.filter { $0.isActive }.count }
    
    // MARK: - Data Loading
    func loadDashboard() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                async let walletTask = api.getWallet()
                async let commissionsTask = api.getMyCommissions()
                async let salesTask = api.getAffiliateSales()
                
                let (walletResult, commissionsResult, salesResult) = try await (walletTask, commissionsTask, salesTask)
                
                self.wallet = walletResult
                self.commissions = commissionsResult
                self.recentSales = Array(salesResult.prefix(10))
                
                // Load promotions with user ID from wallet
                if let userId = wallet?.userId {
                    self.promotions = (try? await api.getMyPromotions(userId: userId)) ?? []
                }
            } catch {
                self.errorMessage = error.localizedDescription
            }
            
            self.isLoading = false
            self.isRefreshing = false
        }
    }
    
    // MARK: - Actions
    func refresh() {
        isRefreshing = true
        loadDashboard()
    }
    
    func retry() {
        errorMessage = nil
        loadDashboard()
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    func setTimePeriod(_ period: TimePeriod) {
        selectedPeriod = period
        // Re-load with period filter (backend can use this for date-range queries)
        loadDashboard()
    }
    
    // MARK: - Formatting Helpers
    private func formatCurrency(_ amount: Double) -> String {
        Self.currencyFormatter.string(from: NSNumber(value: amount)) ?? "$\(String(format: "%.2f", amount))"
    }
    
    private func formatPercent(_ value: Double) -> String {
        "\(String(format: "%.1f", value))%"
    }
    
    private func formatCompact(_ value: Int) -> String {
        if value >= 1_000_000 {
            return String(format: "%.1fM", Double(value) / 1_000_000)
        } else if value >= 1_000 {
            return String(format: "%.1fK", Double(value) / 1_000)
        }
        return "\(value)"
    }
}
