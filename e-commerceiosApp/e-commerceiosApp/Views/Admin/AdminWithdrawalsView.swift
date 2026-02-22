import SwiftUI

struct AdminWithdrawalsView: View {
    @StateObject private var viewModel = AdminWithdrawalsViewModel()
    
    var body: some View {
        VStack(spacing: 0) {
            // Stats Header
            statsHeader
            
            // Filter Chips
            filterChips
            
            // Success banner
            if let action = viewModel.lastAction {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text(action)
                        .font(.caption)
                    Spacer()
                    Button("Dismiss") { viewModel.clearLastAction() }
                        .font(.caption)
                }
                .padding(.horizontal)
                .padding(.vertical, 6)
                .background(Color.green.opacity(0.1))
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        viewModel.clearLastAction()
                    }
                }
            }
            
            // Content
            if viewModel.isLoading && viewModel.withdrawals.isEmpty {
                Spacer()
                ProgressView("Loading withdrawals...")
                Spacer()
            } else if let error = viewModel.errorMessage, viewModel.withdrawals.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 40))
                        .foregroundColor(.red)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Button("Retry") { Task { await viewModel.loadWithdrawals() } }
                        .buttonStyle(.borderedProminent)
                }
                Spacer()
            } else if viewModel.filteredWithdrawals.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "checkmark.shield")
                        .font(.system(size: 50))
                        .foregroundColor(.green.opacity(0.5))
                    Text("All Clear!")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Text("No withdrawal requests to process")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                Spacer()
            } else {
                List {
                    ForEach(viewModel.filteredWithdrawals) { withdrawal in
                        WithdrawalAdminCard(withdrawal: withdrawal)
                            .onTapGesture {
                                viewModel.selectWithdrawal(withdrawal)
                            }
                            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    }
                }
                .listStyle(.plain)
                .refreshable {
                    await viewModel.loadWithdrawals()
                }
            }
        }
        .navigationTitle("Withdrawals")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { Task { await viewModel.loadWithdrawals() } }) {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
        .onAppear {
            Task { await viewModel.loadWithdrawals() }
        }
        .sheet(isPresented: $viewModel.showDetailSheet) {
            if let withdrawal = viewModel.selectedWithdrawal {
                WithdrawalDetailSheet(withdrawal: withdrawal, viewModel: viewModel)
            }
        }
    }
    
    // MARK: - Stats Header
    private var statsHeader: some View {
        VStack(spacing: 12) {
            Text("Total Pending")
                .font(.caption)
                .foregroundColor(.white.opacity(0.8))
            
            Text(String(format: "$%.2f", viewModel.totalPendingAmount))
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.white)
            
            HStack(spacing: 16) {
                WithdrawalStatCircle(
                    count: viewModel.countByStatus("pending"),
                    label: "Pending",
                    color: .orange
                )
                WithdrawalStatCircle(
                    count: viewModel.countByStatus("approved"),
                    label: "Approved",
                    color: .green
                )
                WithdrawalStatCircle(
                    count: viewModel.countByStatus("completed"),
                    label: "Done",
                    color: .blue
                )
                WithdrawalStatCircle(
                    count: viewModel.countByStatus("rejected"),
                    label: "Rejected",
                    color: .red
                )
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(
            LinearGradient(colors: [Color(red: 0.38, green: 0.0, blue: 0.92), Color(red: 0.74, green: 0.0, blue: 0.63)],
                           startPoint: .leading, endPoint: .trailing)
        )
    }
    
    // MARK: - Filter Chips
    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChipButton(
                    title: "All",
                    isSelected: viewModel.selectedFilter == nil,
                    color: .gray
                ) {
                    viewModel.filterByStatus(nil)
                }
                
                ForEach(viewModel.statusFilters, id: \.self) { status in
                    FilterChipButton(
                        title: status.capitalized,
                        isSelected: viewModel.selectedFilter == status,
                        color: statusColor(status)
                    ) {
                        viewModel.filterByStatus(status)
                    }
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
    }
    
    private func statusColor(_ status: String) -> Color {
        switch status {
        case "pending": return .orange
        case "approved": return .green
        case "completed": return .blue
        case "rejected": return .red
        default: return .gray
        }
    }
}

// MARK: - Withdrawal Admin Card
struct WithdrawalAdminCard: View {
    let withdrawal: AdminWithdrawalResponse
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Name + Status
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(withdrawal.displayName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text("ID: #\(withdrawal.id)")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Text(withdrawal.status.capitalized)
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(withdrawal.statusColor)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(withdrawal.statusColor.opacity(0.1))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(withdrawal.statusColor, lineWidth: 1)
                    )
                    .cornerRadius(12)
            }
            
            Divider()
            
            // Info row
            HStack(spacing: 0) {
                WithdrawalInfoItem(
                    label: "Amount",
                    value: withdrawal.formattedAmount,
                    color: .purple
                )
                
                Spacer()
                
                WithdrawalInfoItem(
                    label: "Method",
                    value: withdrawal.paymentMethodDisplay,
                    color: .blue
                )
                
                Spacer()
                
                WithdrawalInfoItem(
                    label: "Date",
                    value: shortDate(withdrawal.createdAt),
                    color: .green
                )
            }
        }
        .padding(12)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 3, y: 1)
    }
    
    private func shortDate(_ dateStr: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateStr) {
            let df = DateFormatter()
            df.dateFormat = "MM/dd/yy"
            return df.string(from: date)
        }
        
        formatter.formatOptions = [.withInternetDateTime]
        if let date = formatter.date(from: dateStr) {
            let df = DateFormatter()
            df.dateFormat = "MM/dd/yy"
            return df.string(from: date)
        }
        
        return dateStr.prefix(10).description
    }
}

// MARK: - Withdrawal Detail Sheet
struct WithdrawalDetailSheet: View {
    let withdrawal: AdminWithdrawalResponse
    @ObservedObject var viewModel: AdminWithdrawalsViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // Status badge
                    Text(withdrawal.status.uppercased())
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(withdrawal.statusColor)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                        .background(withdrawal.statusColor.opacity(0.1))
                        .cornerRadius(16)
                    
                    // Details card
                    VStack(alignment: .leading, spacing: 12) {
                        DetailRow(label: "Request ID", value: "#\(withdrawal.id)")
                        DetailRow(label: "Promoter", value: withdrawal.displayName)
                        DetailRow(label: "Amount", value: withdrawal.formattedAmount, valueColor: .purple)
                        DetailRow(label: "Payment Method", value: withdrawal.paymentMethodDisplay)
                        
                        // Payment details
                        if let details = withdrawal.paymentDetails, !details.isEmpty {
                            Divider()
                            Text("Payment Details")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.secondary)
                            
                            ForEach(Array(details.keys.sorted()), id: \.self) { key in
                                DetailRow(
                                    label: key.replacingOccurrences(of: "_", with: " ").capitalized,
                                    value: details[key] ?? ""
                                )
                            }
                        }
                        
                        Divider()
                        
                        // Timestamps
                        DetailRow(label: "Created", value: formatDate(withdrawal.createdAt))
                        if let approved = withdrawal.approvedAt {
                            DetailRow(label: "Approved", value: formatDate(approved))
                        }
                        if let completed = withdrawal.completedAt {
                            DetailRow(label: "Completed", value: formatDate(completed))
                        }
                        if let rejected = withdrawal.rejectedAt {
                            DetailRow(label: "Rejected", value: formatDate(rejected), valueColor: .red)
                        }
                        
                        if let txId = withdrawal.transactionId {
                            DetailRow(label: "Transaction ID", value: txId, valueColor: .blue)
                        }
                        
                        // Admin notes
                        if let notes = withdrawal.adminNotes ?? withdrawal.rejectionReason, !notes.isEmpty {
                            Divider()
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Admin Notes")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.secondary)
                                Text(notes)
                                    .font(.subheadline)
                                    .padding(8)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(Color.yellow.opacity(0.1))
                                    .cornerRadius(8)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 3)
                    
                    // Processing error
                    if let error = viewModel.processingError {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                    }
                    
                    // Action Buttons
                    if withdrawal.status == "pending" {
                        HStack(spacing: 12) {
                            Button(action: {
                                viewModel.resetForms()
                                viewModel.showApproveDialog = true
                            }) {
                                Label("Approve", systemImage: "checkmark.circle.fill")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color.green)
                                    .cornerRadius(10)
                            }
                            
                            Button(action: {
                                viewModel.resetForms()
                                viewModel.showRejectDialog = true
                            }) {
                                Label("Reject", systemImage: "xmark.circle.fill")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color.red)
                                    .cornerRadius(10)
                            }
                        }
                    }
                    
                    if withdrawal.status == "approved" {
                        Button(action: {
                            viewModel.resetForms()
                            viewModel.showCompleteDialog = true
                        }) {
                            Label("Mark as Completed", systemImage: "checkmark.shield.fill")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.blue)
                                .cornerRadius(10)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Withdrawal Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .alert("Approve Withdrawal", isPresented: $viewModel.showApproveDialog) {
                TextField("Admin notes (optional)", text: $viewModel.adminNotes)
                Button("Cancel", role: .cancel) { }
                Button("Approve") {
                    Task { await viewModel.approveWithdrawal() }
                }
            } message: {
                Text("Approve withdrawal of \(withdrawal.formattedAmount)?")
            }
            .alert("Reject Withdrawal", isPresented: $viewModel.showRejectDialog) {
                TextField("Reason (min 10 chars)", text: $viewModel.rejectReason)
                Button("Cancel", role: .cancel) { }
                Button("Reject", role: .destructive) {
                    Task { await viewModel.rejectWithdrawal() }
                }
            } message: {
                Text("Reject withdrawal of \(withdrawal.formattedAmount)? Funds will be returned to the wallet.")
            }
            .alert("Complete Withdrawal", isPresented: $viewModel.showCompleteDialog) {
                TextField("Transaction ID (required)", text: $viewModel.transactionId)
                TextField("Admin notes (optional)", text: $viewModel.completeNotes)
                Button("Cancel", role: .cancel) { }
                Button("Complete") {
                    Task { await viewModel.completeWithdrawal() }
                }
            } message: {
                Text("Mark withdrawal of \(withdrawal.formattedAmount) as completed (payment sent)?")
            }
        }
    }
    
    private func formatDate(_ dateStr: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateStr) {
            let df = DateFormatter()
            df.dateStyle = .medium
            df.timeStyle = .short
            return df.string(from: date)
        }
        
        formatter.formatOptions = [.withInternetDateTime]
        if let date = formatter.date(from: dateStr) {
            let df = DateFormatter()
            df.dateStyle = .medium
            df.timeStyle = .short
            return df.string(from: date)
        }
        
        return dateStr
    }
}

// MARK: - Supporting Views

struct DetailRow: View {
    let label: String
    let value: String
    var valueColor: Color = .primary
    
    var body: some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(valueColor)
        }
    }
}

struct WithdrawalStatCircle: View {
    let count: Int
    let label: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(color)
                .clipShape(Circle())
            
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(.white.opacity(0.8))
        }
    }
}

struct WithdrawalInfoItem: View {
    let label: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(.secondary)
            Text(value)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(color)
        }
    }
}

struct FilterChipButton: View {
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
                        .stroke(isSelected ? color : .clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}
