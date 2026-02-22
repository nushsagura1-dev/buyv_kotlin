import SwiftUI

struct AdminUsersView: View {
    @StateObject private var viewModel = AdminUsersViewModel()
    @State private var showConfirmDialog = false
    @State private var confirmAction: ConfirmAction?
    @State private var userToDelete: UserManagementResponse?
    
    enum ConfirmAction {
        case verify, unverify
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Search Bar
            searchBar
            
            // Filter Chips
            filterChips
            
            // Error banner
            if let error = viewModel.errorMessage {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.red)
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                    Spacer()
                    Button("Dismiss") {
                        viewModel.errorMessage = nil
                    }
                    .font(.caption)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color.red.opacity(0.1))
            }
            
            // Success banner
            if let success = viewModel.successMessage {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text(success)
                        .font(.caption)
                        .foregroundColor(.green)
                    Spacer()
                    Button("Dismiss") {
                        viewModel.successMessage = nil
                    }
                    .font(.caption)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color.green.opacity(0.1))
            }
            
            // Content
            if viewModel.isLoading && viewModel.users.isEmpty {
                Spacer()
                ProgressView("Loading users...")
                Spacer()
            } else if viewModel.filteredUsers.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "person.slash")
                        .font(.system(size: 50))
                        .foregroundColor(.secondary)
                    Text("No users found")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    if !viewModel.searchQuery.isEmpty {
                        Text("Try a different search term")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
            } else {
                List {
                    ForEach(viewModel.filteredUsers) { user in
                        AdminUserCard(
                            user: user,
                            isSelected: viewModel.selectedUsers.contains(user.id),
                            onToggleSelect: { viewModel.toggleSelection(user.id) },
                            onDelete: { userToDelete = user }
                        )
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    }
                }
                .listStyle(.plain)
                .refreshable {
                    await viewModel.loadUsers()
                }
            }
            
            // Bottom Action Bar
            if viewModel.hasSelection {
                bottomActionBar
            }
        }
        .navigationTitle("Users Management")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { viewModel.selectAll() }) {
                    Image(systemName: viewModel.selectedUsers.count == viewModel.filteredUsers.count && !viewModel.filteredUsers.isEmpty
                          ? "checkmark.circle.fill" : "checkmark.circle")
                }
            }
        }
        .onAppear {
            Task { await viewModel.loadUsers() }
        }
        .alert("Confirm Action", isPresented: $showConfirmDialog) {
            Button("Cancel", role: .cancel) { }
            Button(confirmAction == .verify ? "Verify" : "Unverify",
                   role: confirmAction == .unverify ? .destructive : nil) {
                Task {
                    if confirmAction == .verify {
                        await viewModel.verifySelected()
                    } else {
                        await viewModel.unverifySelected()
                    }
                }
            }
        } message: {
            Text(confirmAction == .verify
                 ? "Verify \(viewModel.selectedCount) selected user(s)?"
                 : "Remove verification for \(viewModel.selectedCount) selected user(s)?")
        }
        .alert("Delete User", isPresented: Binding(
            get: { userToDelete != nil },
            set: { if !$0 { userToDelete = nil } }
        )) {
            Button("Cancel", role: .cancel) { userToDelete = nil }
            Button("Delete", role: .destructive) {
                if let user = userToDelete {
                    Task { await viewModel.deleteUser(user.id) }
                    userToDelete = nil
                }
            }
        } message: {
            if let user = userToDelete {
                Text("Are you sure you want to delete @\(user.username)? This action cannot be undone.")
            }
        }
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            TextField("Search by username or email...", text: $viewModel.searchQuery)
                .textFieldStyle(.plain)
                .autocapitalization(.none)
            if !viewModel.searchQuery.isEmpty {
                Button(action: { viewModel.searchQuery = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(10)
        .background(Color(.systemGray6))
        .cornerRadius(10)
        .padding(.horizontal)
        .padding(.top, 8)
    }
    
    // MARK: - Filter Chips
    private var filterChips: some View {
        HStack(spacing: 8) {
            UserFilterChip(
                title: "All",
                isSelected: viewModel.filterVerified == nil,
                color: .gray
            ) {
                viewModel.filterVerified = nil
            }
            
            UserFilterChip(
                title: "Verified",
                isSelected: viewModel.filterVerified == true,
                color: .green
            ) {
                viewModel.filterVerified = viewModel.filterVerified == true ? nil : true
            }
            
            UserFilterChip(
                title: "Unverified",
                isSelected: viewModel.filterVerified == false,
                color: .orange
            ) {
                viewModel.filterVerified = viewModel.filterVerified == false ? nil : false
            }
            
            Spacer()
            
            Text("\(viewModel.filteredUsers.count) users")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        HStack(spacing: 12) {
            Text("\(viewModel.selectedCount) selected")
                .font(.subheadline)
                .fontWeight(.medium)
            
            Spacer()
            
            Button(action: {
                confirmAction = .verify
                showConfirmDialog = true
            }) {
                Label("Verify", systemImage: "checkmark.seal.fill")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.green)
                    .cornerRadius(8)
            }
            
            Button(action: {
                confirmAction = .unverify
                showConfirmDialog = true
            }) {
                Label("Unverify", systemImage: "xmark.seal.fill")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.orange)
                    .cornerRadius(8)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .shadow(color: Color.black.opacity(0.1), radius: 5, y: -2)
    }
}

// MARK: - User Card
struct AdminUserCard: View {
    let user: UserManagementResponse
    let isSelected: Bool
    let onToggleSelect: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Selection checkbox
            Button(action: onToggleSelect) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .blue : .gray)
                    .font(.title3)
            }
            .buttonStyle(.plain)
            
            // Avatar
            Circle()
                .fill(Color.blue.opacity(0.2))
                .frame(width: 50, height: 50)
                .overlay(
                    Text(user.displayName.prefix(1).uppercased())
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                )
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Text(user.username)
                        .font(.subheadline)
                        .fontWeight(.bold)
                    
                    if user.isVerified {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(.blue)
                            .font(.caption)
                    }
                }
                
                Text(user.email)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                
                HStack(spacing: 12) {
                    StatBadge(icon: "video.fill", value: "\(user.reelsCount)", color: .purple)
                    StatBadge(icon: "person.2.fill", value: "\(user.followersCount)", color: .blue)
                }
            }
            
            Spacer()
            
            // Delete button
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .foregroundColor(.red)
                    .font(.caption)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .background(isSelected ? Color.blue.opacity(0.05) : Color.clear)
        .cornerRadius(8)
    }
}

// MARK: - Supporting Views
struct StatBadge: View {
    let icon: String
    let value: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 10))
                .foregroundColor(color)
            Text(value)
                .font(.system(size: 11))
                .foregroundColor(.secondary)
        }
    }
}

struct UserFilterChip: View {
    let title: String
    let isSelected: Bool
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .fontWeight(.medium)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? color.opacity(0.2) : Color(.systemGray6))
                .foregroundColor(isSelected ? color : .secondary)
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(isSelected ? color : Color.clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}
