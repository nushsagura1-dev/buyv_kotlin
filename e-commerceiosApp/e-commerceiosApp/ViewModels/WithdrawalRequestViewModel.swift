import Foundation
import SwiftUI

enum PaymentMethod: String, CaseIterable {
    case paypal = "paypal"
    case bankTransfer = "bank_transfer"
    
    var displayName: String {
        switch self {
        case .paypal: return "PayPal"
        case .bankTransfer: return "Bank Transfer"
        }
    }
    
    var icon: String {
        switch self {
        case .paypal: return "envelope.fill"
        case .bankTransfer: return "building.columns.fill"
        }
    }
}

@MainActor
class WithdrawalRequestViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var amount: String = ""
    @Published var paymentMethod: PaymentMethod = .paypal
    
    // PayPal fields
    @Published var paypalEmail: String = ""
    
    // Bank Transfer fields
    @Published var accountHolderName: String = ""
    @Published var bankName: String = ""
    @Published var accountNumber: String = ""
    @Published var routingNumber: String = ""
    
    @Published var stats: WithdrawalStatsResponse?
    @Published var isLoading: Bool = false
    @Published var isSubmitting: Bool = false
    @Published var isSuccess: Bool = false
    @Published var errorMessage: String?
    @Published var submittedAmount: Double = 0
    
    private let api = MarketplaceApiService.shared
    
    var availableBalance: Double { stats?.availableBalance ?? 0 }
    var pendingBalance: Double { stats?.pendingBalance ?? 0 }
    var totalWithdrawn: Double { stats?.totalWithdrawn ?? 0 }
    
    var parsedAmount: Double { Double(amount) ?? 0 }
    
    var amountError: String? {
        guard !amount.isEmpty else { return nil }
        let amt = parsedAmount
        if amt < 50 { return "Minimum withdrawal is $50.00" }
        if amt > 10000 { return "Maximum withdrawal is $10,000.00" }
        if amt > availableBalance { return "Insufficient balance" }
        return nil
    }
    
    var isFormValid: Bool {
        let amt = parsedAmount
        guard amt >= 50 && amt <= 10000 && amt <= availableBalance else { return false }
        
        switch paymentMethod {
        case .paypal:
            return !paypalEmail.isEmpty && paypalEmail.contains("@")
        case .bankTransfer:
            return !accountHolderName.isEmpty && !bankName.isEmpty && !accountNumber.isEmpty && !routingNumber.isEmpty
        }
    }
    
    // MARK: - Methods
    
    func loadStats() {
        isLoading = true
        Task {
            do {
                stats = try await api.getWithdrawalStats()
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
    
    func setMaxAmount() {
        amount = String(format: "%.2f", min(availableBalance, 10000))
    }
    
    func submit() {
        guard isFormValid else { return }
        
        isSubmitting = true
        errorMessage = nil
        
        var paymentDetails: [String: String] = [:]
        
        switch paymentMethod {
        case .paypal:
            paymentDetails["paypal_email"] = paypalEmail
        case .bankTransfer:
            paymentDetails["account_holder_name"] = accountHolderName
            paymentDetails["bank_name"] = bankName
            paymentDetails["account_number"] = accountNumber
            paymentDetails["routing_number"] = routingNumber
        }
        
        Task {
            do {
                _ = try await api.requestWithdrawal(
                    amount: parsedAmount,
                    paymentMethod: paymentMethod.rawValue,
                    paymentDetails: paymentDetails
                )
                submittedAmount = parsedAmount
                isSuccess = true
            } catch {
                errorMessage = error.localizedDescription
            }
            isSubmitting = false
        }
    }
    
    func resetForm() {
        amount = ""
        paypalEmail = ""
        accountHolderName = ""
        bankName = ""
        accountNumber = ""
        routingNumber = ""
        isSuccess = false
        errorMessage = nil
    }
}
