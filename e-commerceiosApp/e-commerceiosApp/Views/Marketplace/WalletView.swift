import SwiftUI

struct WalletView: View {
    @StateObject private var viewModel = WalletViewModel()
    
    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading wallet...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage, viewModel.wallet == nil {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text(error)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    Button("Retry") { viewModel.loadWallet() }
                        .buttonStyle(.borderedProminent)
                        .tint(.orange)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()
            } else {
                walletContent
            }
        }
        .navigationTitle("Wallet")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { viewModel.refresh() }) {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
        .onAppear {
            if viewModel.wallet == nil { viewModel.loadWallet() }
        }
    }
    
    // MARK: - Wallet Content
    private var walletContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Balance Card
                balanceCard
                
                // Stats Row
                statsRow
                
                // Withdraw Button
                if viewModel.canWithdraw {
                    NavigationLink(destination: WithdrawalRequestView()) {
                        HStack {
                            Image(systemName: "arrow.up.circle.fill")
                            Text("Request Withdrawal")
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.orange)
                        .cornerRadius(12)
                    }
                    .padding(.horizontal)
                } else {
                    Text("Minimum $50.00 required for withdrawal")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)
                }
                
                // Transaction History
                transactionHistory
            }
            .padding(.vertical)
        }
        .refreshable {
            viewModel.loadWallet()
        }
    }
    
    // MARK: - Balance Card
    private var balanceCard: some View {
        VStack(spacing: 12) {
            Text("Available Balance")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.8))
            
            Text(String(format: "$%.2f", viewModel.availableAmount))
                .font(.system(size: 42, weight: .bold))
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
        .background(
            LinearGradient(
                colors: [Color.orange, Color.orange.opacity(0.7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
        .padding(.horizontal)
    }
    
    // MARK: - Stats Row
    private var statsRow: some View {
        HStack(spacing: 12) {
            WalletStatCard(
                title: "Pending",
                amount: viewModel.pendingAmount,
                color: .orange
            )
            
            WalletStatCard(
                title: "Total Earned",
                amount: viewModel.totalEarned,
                color: .green
            )
            
            WalletStatCard(
                title: "Withdrawn",
                amount: viewModel.withdrawnAmount,
                color: .blue
            )
        }
        .padding(.horizontal)
    }
    
    // MARK: - Transaction History
    private var transactionHistory: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Transaction History")
                .font(.headline)
                .padding(.horizontal)
            
            if viewModel.transactions.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 40))
                        .foregroundColor(.gray)
                    Text("No transactions yet")
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 30)
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(viewModel.transactions) { transaction in
                        TransactionRow(transaction: transaction)
                        
                        if transaction.id != viewModel.transactions.last?.id {
                            Divider()
                                .padding(.horizontal)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Wallet Stat Card
struct WalletStatCard: View {
    let title: String
    let amount: Double
    let color: Color
    
    var body: some View {
        VStack(spacing: 6) {
            Text(String(format: "$%.2f", amount))
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(color)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

// MARK: - Transaction Row
struct TransactionRow: View {
    let transaction: WalletTransactionResponse
    
    var body: some View {
        HStack(spacing: 12) {
            // Type Icon
            Image(systemName: typeIcon)
                .font(.title3)
                .foregroundColor(typeColor)
                .frame(width: 40, height: 40)
                .background(typeColor.opacity(0.1))
                .cornerRadius(10)
            
            // Description
            VStack(alignment: .leading, spacing: 4) {
                Text(transaction.description ?? transactionTypeLabel)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(transaction.createdAt)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Amount
            VStack(alignment: .trailing, spacing: 4) {
                Text(formattedAmount)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(transaction.amount >= 0 ? .green : .red)
                
                Text(String(format: "$%.2f", transaction.balanceAfter))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
    }
    
    private var typeIcon: String {
        switch transaction.type.lowercased() {
        case "commission": return "dollarsign.circle.fill"
        case "withdrawal": return "arrow.up.circle.fill"
        case "adjustment": return "slider.horizontal.3"
        case "refund": return "arrow.uturn.backward.circle.fill"
        default: return "circle.fill"
        }
    }
    
    private var typeColor: Color {
        switch transaction.type.lowercased() {
        case "commission": return .green
        case "withdrawal": return .red
        case "adjustment": return .blue
        case "refund": return .orange
        default: return .gray
        }
    }
    
    private var transactionTypeLabel: String {
        transaction.type.capitalized
    }
    
    private var formattedAmount: String {
        let prefix = transaction.amount >= 0 ? "+" : ""
        return "\(prefix)\(String(format: "$%.2f", transaction.amount))"
    }
}

#Preview {
    NavigationView {
        WalletView()
    }
}
