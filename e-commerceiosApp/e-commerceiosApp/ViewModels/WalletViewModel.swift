import Foundation
import SwiftUI

@MainActor
class WalletViewModel: ObservableObject {
    @Published var wallet: WalletResponse?
    @Published var transactions: [WalletTransactionResponse] = []
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var errorMessage: String?
    @Published var selectedFilter: TransactionFilter = .all
    
    enum TransactionFilter: String, CaseIterable {
        case all = "All"
        case earnings = "Earnings"
        case withdrawals = "Withdrawals"
        case pending = "Pending"
        
        var icon: String {
            switch self {
            case .all: return "list.bullet"
            case .earnings: return "arrow.down.circle.fill"
            case .withdrawals: return "arrow.up.circle.fill"
            case .pending: return "clock.fill"
            }
        }
    }
    
    private let api = MarketplaceApiService.shared
    private static let currencyFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "USD"
        f.minimumFractionDigits = 2
        f.maximumFractionDigits = 2
        return f
    }()
    
    // MARK: - Computed Properties
    var availableAmount: Double { wallet?.availableAmount ?? 0 }
    var pendingAmount: Double { wallet?.pendingAmount ?? 0 }
    var totalEarned: Double { wallet?.totalEarned ?? 0 }
    var withdrawnAmount: Double { wallet?.withdrawnAmount ?? 0 }
    var canWithdraw: Bool { wallet?.canWithdraw ?? false }
    
    var formattedAvailable: String { formatCurrency(availableAmount) }
    var formattedPending: String { formatCurrency(pendingAmount) }
    var formattedTotalEarned: String { formatCurrency(totalEarned) }
    var formattedWithdrawn: String { formatCurrency(withdrawnAmount) }
    
    var minimumWithdrawalAmount: Double { 10.0 }
    var isEligibleForWithdrawal: Bool {
        canWithdraw && availableAmount >= minimumWithdrawalAmount
    }
    
    var withdrawalIneligibilityReason: String? {
        if !canWithdraw { return "Withdrawals are currently disabled for your account" }
        if availableAmount < minimumWithdrawalAmount {
            return "Minimum withdrawal amount is \(formatCurrency(minimumWithdrawalAmount))"
        }
        return nil
    }
    
    var filteredTransactions: [WalletTransactionResponse] {
        switch selectedFilter {
        case .all:
            return transactions
        case .earnings:
            return transactions.filter { $0.type == "earning" || $0.type == "commission" || $0.type == "sale" }
        case .withdrawals:
            return transactions.filter { $0.type == "withdrawal" }
        case .pending:
            return transactions.filter { $0.status == "pending" }
        }
    }
    
    var hasTransactions: Bool { !transactions.isEmpty }
    var hasFilteredTransactions: Bool { !filteredTransactions.isEmpty }
    var transactionCount: Int { filteredTransactions.count }
    
    var earningsThisMonth: Double {
        let calendar = Calendar.current
        let now = Date()
        return transactions
            .filter { tx in
                (tx.type == "earning" || tx.type == "commission" || tx.type == "sale") &&
                calendar.isDate(Date(timeIntervalSince1970: TimeInterval(tx.createdAt / 1000)), equalTo: now, toGranularity: .month)
            }
            .reduce(0) { $0 + $1.amount }
    }
    
    var formattedEarningsThisMonth: String { formatCurrency(earningsThisMonth) }
    
    // MARK: - Actions
    func loadWallet() {
        isLoading = wallet == nil
        errorMessage = nil
        
        Task {
            do {
                async let walletTask = api.getWallet()
                async let transactionsTask = api.getWalletTransactions(limit: 50)
                
                let (walletResult, transactionsResult) = try await (walletTask, transactionsTask)
                self.wallet = walletResult
                self.transactions = transactionsResult
            } catch {
                self.errorMessage = error.localizedDescription
            }
            
            self.isLoading = false
            self.isRefreshing = false
        }
    }
    
    func refresh() {
        isRefreshing = true
        loadWallet()
    }
    
    func retry() {
        errorMessage = nil
        loadWallet()
    }
    
    func setFilter(_ filter: TransactionFilter) {
        withAnimation(.easeInOut(duration: 0.2)) {
            selectedFilter = filter
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    // MARK: - Formatting Helpers
    func formatCurrency(_ amount: Double) -> String {
        Self.currencyFormatter.string(from: NSNumber(value: amount)) ?? "$\(String(format: "%.2f", amount))"
    }
    
    func formatTransactionDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}
