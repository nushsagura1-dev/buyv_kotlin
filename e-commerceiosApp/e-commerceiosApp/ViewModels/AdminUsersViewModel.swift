import Foundation
import SwiftUI

@MainActor
class AdminUsersViewModel: ObservableObject {
    @Published var users: [UserManagementResponse] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var searchQuery = ""
    @Published var filterVerified: Bool? = nil // nil = all, true = verified, false = unverified
    @Published var selectedUsers: Set<String> = []
    @Published var actionInProgress = false
    @Published var successMessage: String?
    
    var filteredUsers: [UserManagementResponse] {
        var result = users
        
        if let filterVerified = filterVerified {
            result = result.filter { $0.isVerified == filterVerified }
        }
        
        if !searchQuery.isEmpty {
            let query = searchQuery.lowercased()
            result = result.filter {
                $0.username.lowercased().contains(query) ||
                $0.email.lowercased().contains(query) ||
                $0.displayName.lowercased().contains(query)
            }
        }
        
        return result
    }
    
    var selectedCount: Int { selectedUsers.count }
    var hasSelection: Bool { !selectedUsers.isEmpty }
    
    func loadUsers() async {
        isLoading = true
        errorMessage = nil
        
        do {
            users = try await AdminApiService.shared.getUsers(limit: 200)
            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
    
    func toggleSelection(_ userId: String) {
        if selectedUsers.contains(userId) {
            selectedUsers.remove(userId)
        } else {
            selectedUsers.insert(userId)
        }
    }
    
    func selectAll() {
        let visibleIds = Set(filteredUsers.map { $0.id })
        if selectedUsers == visibleIds {
            selectedUsers.removeAll()
        } else {
            selectedUsers = visibleIds
        }
    }
    
    func verifySelected() async {
        guard !selectedUsers.isEmpty else { return }
        actionInProgress = true
        
        do {
            let response = try await AdminApiService.shared.verifyUsers(userIds: Array(selectedUsers))
            successMessage = response.message
            selectedUsers.removeAll()
            await loadUsers()
        } catch {
            errorMessage = error.localizedDescription
        }
        
        actionInProgress = false
    }
    
    func unverifySelected() async {
        guard !selectedUsers.isEmpty else { return }
        actionInProgress = true
        
        do {
            let response = try await AdminApiService.shared.unverifyUsers(userIds: Array(selectedUsers))
            successMessage = response.message
            selectedUsers.removeAll()
            await loadUsers()
        } catch {
            errorMessage = error.localizedDescription
        }
        
        actionInProgress = false
    }
    
    func deleteUser(_ userId: String) async {
        actionInProgress = true
        
        do {
            let response = try await AdminApiService.shared.deleteUser(userUid: userId)
            successMessage = response.message
            users.removeAll { $0.id == userId }
            selectedUsers.remove(userId)
        } catch {
            errorMessage = error.localizedDescription
        }
        
        actionInProgress = false
    }
}
