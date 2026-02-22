import SwiftUI

struct AdminCommissionsView: View {
    @StateObject private var viewModel = AdminCommissionsViewModel()
    @State private var showActionMenu = false
    @State private var selectedCommission: CommissionResponse?
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            headerSection
            
            // Filter Chips
            filterChips
            
            // Success banner
            if let success = viewModel.successMessage {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text(success)
                        .font(.caption)
                    Spacer()
                    Button("Dismiss") { viewModel.successMessage = nil }
                        .font(.caption)
                }
                .padding(.horizontal)
                .padding(.vertical, 6)
                .background(Color.green.opacity(0.1))
            }
            
            // Content
            if viewModel.isLoading && viewModel.commissions.isEmpty {
                Spacer()
                ProgressView("Loading commissions...")
                Spacer()
            } else if let error = viewModel.errorMessage, viewModel.commissions.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 40))
                        .foregroundColor(.red)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Button("Retry") { Task { await viewModel.loadCommissions() } }
                        .buttonStyle(.borderedProminent)
                }
                Spacer()
            } else if viewModel.filteredCommissions.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "dollarsign.circle")
                        .font(.system(size: 50))
                        .foregroundColor(.secondary)
                    Text("No commissions found")
                        .font(.headline)
                        .foregroundColor(.secondary)
                }
                Spacer()
            } else {
                // Total amount banner
                HStack {
                    Text("\(viewModel.filteredCount) commissions")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("Total: \(viewModel.formattedTotalAmount)")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
                .padding(.horizontal)
                .padding(.vertical, 4)
                
                List {
                    ForEach(viewModel.filteredCommissions) { commission in
                        CommissionCard(
                            commission: commission,
                            onApprove: {
                                Task { await viewModel.updateStatus(commissionId: commission.id, newStatus: "approved") }
                            },
                            onReject: {
                                Task { await viewModel.updateStatus(commissionId: commission.id, newStatus: "rejected") }
                            },
                            onMarkPaid: {
                                Task { await viewModel.updateStatus(commissionId: commission.id, newStatus: "paid") }
                            }
                        )
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    }
                }
                .listStyle(.plain)
                .refreshable {
                    await viewModel.refresh()
                }
            }
        }
        .navigationTitle("Commissions")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            Task { await viewModel.loadCommissions() }
        }
    }
    
    // MARK: - Header
    private var headerSection: some View {
        HStack(spacing: 12) {
            Image(systemName: "dollarsign.circle.fill")
                .font(.title2)
                .foregroundColor(.white)
            
            VStack(alignment: .leading) {
                Text("Commission Management")
                    .font(.headline)
                    .foregroundColor(.white)
                Text("Approve, reject and track commissions")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
            }
            
            Spacer()
        }
        .padding()
        .background(
            LinearGradient(colors: [Color(red: 0.38, green: 0.49, blue: 0.55), Color(red: 0.26, green: 0.34, blue: 0.39)], startPoint: .leading, endPoint: .trailing)
        )
    }
    
    // MARK: - Filter Chips (using VM statusBadges)
    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.statusBadges, id: \.label) { badge in
                    CommissionFilterChip(
                        title: badge.label,
                        count: badge.count,
                        isSelected: viewModel.selectedFilter == badge.status,
                        color: badge.color
                    ) {
                        viewModel.selectFilter(badge.status)
                    }
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Commission Card
struct CommissionCard: View {
    let commission: CommissionResponse
    let onApprove: () -> Void
    let onReject: () -> Void
    let onMarkPaid: () -> Void
    
    @State private var showActions = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Product name + menu
            HStack {
                Text(commission.productName)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .lineLimit(1)
                
                Spacer()
                
                Menu {
                    if commission.status == "pending" {
                        Button(action: onApprove) {
                            Label("Approve", systemImage: "checkmark.circle")
                        }
                        Button(role: .destructive, action: onReject) {
                            Label("Reject", systemImage: "xmark.circle")
                        }
                    }
                    if commission.status == "approved" {
                        Button(action: onMarkPaid) {
                            Label("Mark as Paid", systemImage: "banknote")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(.secondary)
                }
            }
            
            // Commission ID
            Text("Commission #\(commission.id)")
                .font(.caption2)
                .foregroundColor(.secondary)
            
            Divider()
            
            // Details grid
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("User ID")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                    Text(String(commission.userId.prefix(8)) + "...")
                        .font(.caption)
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Order")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                    Text("#\(commission.orderId)")
                        .font(.caption)
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Product Price")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                    Text(String(format: "$%.2f", commission.productPrice))
                        .font(.caption)
                }
            }
            
            // Commission amount + rate
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Commission Rate")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                    Text(commission.formattedRate)
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Commission")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                    Text(commission.formattedAmount)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
            }
            
            // Date + Status
            HStack {
                Text(AdminCommissionsViewModel.formatDate(commission.createdAt))
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                if let paidAt = commission.paidAt {
                    Text("Paid: \(AdminCommissionsViewModel.formatDate(paidAt))")
                        .font(.caption2)
                        .foregroundColor(.blue)
                }
                
                Spacer()
                
                // Status badge with icon
                HStack(spacing: 4) {
                    Image(systemName: AdminCommissionsViewModel.statusIcon(commission.status))
                        .font(.system(size: 9))
                    Text(commission.status.capitalized)
                        .font(.caption2)
                        .fontWeight(.semibold)
                }
                .foregroundColor(AdminCommissionsViewModel.statusColor(commission.status))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(AdminCommissionsViewModel.statusColor(commission.status).opacity(0.15))
                .cornerRadius(10)
            }
        }
        .padding(12)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 3, y: 1)
    }
}

// MARK: - Commission Filter Chip
struct CommissionFilterChip: View {
    let title: String
    let count: Int
    let isSelected: Bool
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.caption)
                    .fontWeight(.medium)
                Text("(\(count))")
                    .font(.system(size: 10))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(isSelected ? color.opacity(0.2) : Color(.systemGray6))
            .foregroundColor(isSelected ? color : .secondary)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? color : .clear, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
