import Foundation
import SwiftUI

@MainActor
class AdminWithdrawalsViewModel: ObservableObject {
    @Published var withdrawals: [AdminWithdrawalResponse] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var selectedFilter: String? = nil
    @Published var selectedWithdrawal: AdminWithdrawalResponse?
    
    // Action state
    @Published var isProcessing = false
    @Published var processingError: String?
    @Published var lastAction: String?
    
    // Dialog state
    @Published var showDetailSheet = false
    @Published var showApproveDialog = false
    @Published var showRejectDialog = false
    @Published var showCompleteDialog = false
    
    // Form fields
    @Published var adminNotes = ""
    @Published var rejectReason = ""
    @Published var transactionId = ""
    @Published var completeNotes = ""
    
    let statusFilters = ["pending", "approved", "completed", "rejected"]
    
    var filteredWithdrawals: [AdminWithdrawalResponse] {
        guard let filter = selectedFilter else { return withdrawals }
        return withdrawals.filter { $0.status == filter }
    }
    
    var totalPendingAmount: Double {
        withdrawals.filter { $0.status == "pending" }.reduce(0) { $0 + $1.amount }
    }
    
    func countByStatus(_ status: String) -> Int {
        withdrawals.filter { $0.status == status }.count
    }
    
    func loadWithdrawals() async {
        isLoading = true
        errorMessage = nil
        
        do {
            withdrawals = try await AdminApiService.shared.getAdminWithdrawals()
            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
    
    func filterByStatus(_ status: String?) {
        selectedFilter = status
    }
    
    func selectWithdrawal(_ withdrawal: AdminWithdrawalResponse) {
        selectedWithdrawal = withdrawal
        showDetailSheet = true
    }
    
    func approveWithdrawal() async {
        guard let withdrawal = selectedWithdrawal else { return }
        isProcessing = true
        processingError = nil
        
        do {
            _ = try await AdminApiService.shared.approveWithdrawal(
                id: withdrawal.id,
                adminNotes: adminNotes.isEmpty ? nil : adminNotes
            )
            lastAction = "Withdrawal #\(withdrawal.id) approved"
            adminNotes = ""
            showApproveDialog = false
            showDetailSheet = false
            await loadWithdrawals()
        } catch {
            processingError = error.localizedDescription
        }
        
        isProcessing = false
    }
    
    func rejectWithdrawal() async {
        guard let withdrawal = selectedWithdrawal else { return }
        guard rejectReason.count >= 10 else {
            processingError = "Rejection reason must be at least 10 characters"
            return
        }
        
        isProcessing = true
        processingError = nil
        
        do {
            _ = try await AdminApiService.shared.rejectWithdrawal(
                id: withdrawal.id,
                adminNotes: rejectReason
            )
            lastAction = "Withdrawal #\(withdrawal.id) rejected"
            rejectReason = ""
            showRejectDialog = false
            showDetailSheet = false
            await loadWithdrawals()
        } catch {
            processingError = error.localizedDescription
        }
        
        isProcessing = false
    }
    
    func completeWithdrawal() async {
        guard let withdrawal = selectedWithdrawal else { return }
        guard transactionId.count >= 5 else {
            processingError = "Transaction ID must be at least 5 characters"
            return
        }
        
        isProcessing = true
        processingError = nil
        
        do {
            _ = try await AdminApiService.shared.completeWithdrawal(
                id: withdrawal.id,
                transactionId: transactionId,
                adminNotes: completeNotes.isEmpty ? nil : completeNotes
            )
            lastAction = "Withdrawal #\(withdrawal.id) completed"
            transactionId = ""
            completeNotes = ""
            showCompleteDialog = false
            showDetailSheet = false
            await loadWithdrawals()
        } catch {
            processingError = error.localizedDescription
        }
        
        isProcessing = false
    }
    
    func clearLastAction() {
        lastAction = nil
    }
    
    func resetForms() {
        adminNotes = ""
        rejectReason = ""
        transactionId = ""
        completeNotes = ""
        processingError = nil
    }
}
