import SwiftUI

struct PromoterDashboardView: View {
    @StateObject private var viewModel = PromoterDashboardViewModel()
    
    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading dashboard...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text(error)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    Button("Retry") { viewModel.loadDashboard() }
                        .buttonStyle(.borderedProminent)
                        .tint(.orange)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()
            } else {
                dashboardContent
            }
        }
        .navigationTitle("Promoter Dashboard")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { viewModel.loadDashboard() }) {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
        .onAppear {
            if viewModel.wallet == nil { viewModel.loadDashboard() }
        }
    }
    
    // MARK: - Dashboard Content
    private var dashboardContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Earnings Overview
                earningsCard
                
                // Performance Metrics
                performanceSection
                
                // Conversion Analytics
                conversionSection
                
                // Quick Actions
                actionsSection
                
                // Recent Sales
                if !viewModel.recentSales.isEmpty {
                    recentSalesSection
                }
            }
            .padding()
        }
        .refreshable {
            viewModel.loadDashboard()
        }
    }
    
    // MARK: - Earnings Card
    private var earningsCard: some View {
        VStack(spacing: 16) {
            // Total Earned (big)
            VStack(spacing: 4) {
                Text("Total Earned")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
                Text(String(format: "$%.2f", viewModel.totalEarned))
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(.white)
            }
            
            Divider()
                .overlay(Color.white.opacity(0.3))
            
            // Balance Details
            HStack(spacing: 0) {
                EarningsStatView(
                    title: "Available",
                    amount: viewModel.availableBalance,
                    color: .green
                )
                
                EarningsStatView(
                    title: "Pending",
                    amount: viewModel.pendingAmount,
                    color: .yellow
                )
                
                EarningsStatView(
                    title: "Withdrawn",
                    amount: viewModel.withdrawnAmount,
                    color: .blue
                )
            }
        }
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color.orange, Color.orange.opacity(0.8)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
    }
    
    // MARK: - Performance Section
    private var performanceSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Performance")
                .font(.headline)
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                MetricCard(
                    title: "Total Sales",
                    value: "\(viewModel.totalSalesCount)",
                    icon: "cart.fill",
                    color: .blue
                )
                
                MetricCard(
                    title: "Commissions",
                    value: "\(viewModel.commissions.count)",
                    icon: "dollarsign.circle.fill",
                    color: .green
                )
                
                MetricCard(
                    title: "Views",
                    value: "\(viewModel.totalPromotionViews)",
                    icon: "eye.fill",
                    color: .purple
                )
                
                MetricCard(
                    title: "Clicks",
                    value: "\(viewModel.totalPromotionClicks)",
                    icon: "hand.tap.fill",
                    color: .orange
                )
            }
        }
    }
    
    // MARK: - Conversion Section
    private var conversionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Conversion Analytics")
                .font(.headline)
            
            VStack(spacing: 12) {
                ConversionBar(
                    title: "Click-Through Rate",
                    value: viewModel.ctr,
                    maxValue: 100,
                    color: .blue
                )
                
                ConversionBar(
                    title: "Conversion Rate",
                    value: viewModel.conversionRate,
                    maxValue: 100,
                    color: .green
                )
                
                HStack {
                    VStack(alignment: .leading) {
                        Text("Avg. Commission / Sale")
                            .font(.caption)
                            .foregroundColor(.gray)
                        let avgCommission = viewModel.totalSalesCount > 0 ? viewModel.totalCommissionsAmount / Double(viewModel.totalSalesCount) : 0
                        Text(String(format: "$%.2f", avgCommission))
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing) {
                        Text("Pending Commissions")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("\(viewModel.pendingCommissions)")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
        }
    }
    
    // MARK: - Actions Section
    private var actionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Actions")
                .font(.headline)
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                NavigationLink(destination: MyCommissionsView()) {
                    ActionButton(title: "My Commissions", icon: "dollarsign.circle", color: .green)
                }
                
                NavigationLink(destination: MyPromotionsView()) {
                    ActionButton(title: "My Promotions", icon: "megaphone", color: .purple)
                }
                
                NavigationLink(destination: AffiliateSalesView()) {
                    ActionButton(title: "Affiliate Sales", icon: "chart.bar", color: .blue)
                }
                
                NavigationLink(destination: WalletView()) {
                    ActionButton(title: "Wallet", icon: "wallet.pass", color: .orange)
                }
                
                if viewModel.wallet?.canWithdraw == true {
                    NavigationLink(destination: WithdrawalRequestView()) {
                        ActionButton(title: "Withdraw", icon: "arrow.up.circle", color: .red)
                    }
                }
            }
        }
    }
    
    // MARK: - Recent Sales Section
    private var recentSalesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Sales")
                    .font(.headline)
                Spacer()
                NavigationLink(destination: AffiliateSalesView()) {
                    Text("See All")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }
            
            ForEach(viewModel.recentSales.prefix(5)) { sale in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(sale.productName ?? "Product #\(sale.productId)")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text("Qty: \(sale.quantity)")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text(String(format: "+$%.2f", sale.commissionAmount))
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                        
                        Text(sale.commissionStatus.capitalized)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(sale.commissionStatus == "paid" ? Color.green.opacity(0.2) : Color.orange.opacity(0.2))
                            .foregroundColor(sale.commissionStatus == "paid" ? .green : .orange)
                            .cornerRadius(6)
                    }
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }
        }
    }
}

// MARK: - Earnings Stat View
struct EarningsStatView: View {
    let title: String
    let amount: Double
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(String(format: "$%.2f", amount))
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(.white)
            Text(title)
                .font(.caption2)
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Metric Card
struct MetricCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(color.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - Conversion Bar
struct ConversionBar: View {
    let title: String
    let value: Double
    let maxValue: Double
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.gray)
                Spacer()
                Text(String(format: "%.1f%%", value))
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(color)
            }
            
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(.systemGray5))
                        .frame(height: 8)
                    
                    RoundedRectangle(cornerRadius: 4)
                        .fill(color)
                        .frame(width: max(0, geometry.size.width * CGFloat(min(value / maxValue, 1.0))), height: 8)
                }
            }
            .frame(height: 8)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

// MARK: - Action Button
struct ActionButton: View {
    let title: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - My Commissions View
struct MyCommissionsView: View {
    @State private var commissions: [CommissionResponse] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var selectedStatus: String?
    
    private let statusOptions = [nil, "pending", "approved", "paid", "rejected"]
    
    var filteredCommissions: [CommissionResponse] {
        guard let status = selectedStatus else { return commissions }
        return commissions.filter { $0.status == status }
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Status Filter
            Picker("Status", selection: $selectedStatus) {
                Text("All").tag(nil as String?)
                Text("Pending").tag("pending" as String?)
                Text("Approved").tag("approved" as String?)
                Text("Paid").tag("paid" as String?)
            }
            .pickerStyle(.segmented)
            .padding()
            
            if isLoading {
                ProgressView("Loading commissions...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if filteredCommissions.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "dollarsign.circle")
                        .font(.system(size: 50))
                        .foregroundColor(.gray)
                    Text("No commissions found")
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(filteredCommissions) { commission in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(commission.productName ?? "Product #\(commission.productId ?? 0)")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            if let rate = commission.commissionRate {
                                Text("Rate: \(String(format: "%.0f%%", rate))")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                            
                            Text(commission.createdAt)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        VStack(alignment: .trailing, spacing: 4) {
                            Text(String(format: "$%.2f", commission.commissionAmount))
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                            
                            Text(commission.status.capitalized)
                                .font(.caption2)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(statusColor(commission.status).opacity(0.2))
                                .foregroundColor(statusColor(commission.status))
                                .cornerRadius(6)
                        }
                    }
                }
            }
        }
        .navigationTitle("My Commissions")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            do {
                commissions = try await MarketplaceApiService.shared.getMyCommissions()
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
    
    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "pending": return .orange
        case "approved": return .blue
        case "paid": return .green
        case "rejected": return .red
        default: return .gray
        }
    }
}

// MARK: - My Promotions View
struct MyPromotionsView: View {
    @State private var promotions: [PromotionResponse] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    
    var body: some View {
        Group {
            if isLoading {
                ProgressView("Loading promotions...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if promotions.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "megaphone")
                        .font(.system(size: 50))
                        .foregroundColor(.gray)
                    Text("No promotions yet")
                        .foregroundColor(.gray)
                    Text("Promote products to earn commissions")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(promotions) { promo in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Promotion #\(promo.id.prefix(8))")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            Spacer()
                            
                            if promo.isOfficial == true {
                                Text("Official")
                                    .font(.caption2)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(Color.blue.opacity(0.2))
                                    .foregroundColor(.blue)
                                    .cornerRadius(6)
                            }
                        }
                        
                        HStack(spacing: 16) {
                            HStack(spacing: 4) {
                                Image(systemName: "eye")
                                    .font(.caption2)
                                Text("\(promo.viewsCount ?? 0)")
                                    .font(.caption)
                            }
                            .foregroundColor(.blue)
                            
                            HStack(spacing: 4) {
                                Image(systemName: "hand.tap")
                                    .font(.caption2)
                                Text("\(promo.clicksCount ?? 0)")
                                    .font(.caption)
                            }
                            .foregroundColor(.orange)
                            
                            HStack(spacing: 4) {
                                Image(systemName: "cart")
                                    .font(.caption2)
                                Text("\(promo.salesCount ?? 0)")
                                    .font(.caption)
                            }
                            .foregroundColor(.green)
                            
                            Spacer()
                            
                            Text(String(format: "$%.2f", promo.totalCommissionEarned ?? 0))
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("My Promotions")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            // Try to get user ID from wallet
            do {
                let wallet = try await MarketplaceApiService.shared.getWallet()
                promotions = try await MarketplaceApiService.shared.getMyPromotions(userId: wallet.userId)
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}

// MARK: - Affiliate Sales View
struct AffiliateSalesView: View {
    @State private var sales: [AffiliateSaleResponse] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var selectedStatus: String?
    
    var filteredSales: [AffiliateSaleResponse] {
        guard let status = selectedStatus else { return sales }
        return sales.filter { $0.commissionStatus == status }
    }
    
    var totalCommission: Double {
        filteredSales.reduce(0) { $0 + $1.commissionAmount }
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Total banner
            HStack {
                VStack(alignment: .leading) {
                    Text("Total Commission")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                    Text(String(format: "$%.2f", totalCommission))
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
                Spacer()
                Text("\(filteredSales.count) sales")
                    .foregroundColor(.white.opacity(0.8))
            }
            .padding()
            .background(Color.orange)
            
            // Status Filter
            Picker("Status", selection: $selectedStatus) {
                Text("All").tag(nil as String?)
                Text("Pending").tag("pending" as String?)
                Text("Paid").tag("paid" as String?)
                Text("Cancelled").tag("cancelled" as String?)
            }
            .pickerStyle(.segmented)
            .padding()
            
            if isLoading {
                ProgressView("Loading sales...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if filteredSales.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "chart.bar")
                        .font(.system(size: 50))
                        .foregroundColor(.gray)
                    Text("No affiliate sales found")
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(filteredSales) { sale in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(sale.productName ?? "Product")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            Text("Qty: \(sale.quantity) × \(String(format: "$%.2f", sale.unitPrice))")
                                .font(.caption)
                                .foregroundColor(.gray)
                            
                            Text(sale.createdAt)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        VStack(alignment: .trailing, spacing: 4) {
                            Text(String(format: "+$%.2f", sale.commissionAmount))
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                            
                            Text(sale.commissionStatus.capitalized)
                                .font(.caption2)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(sale.commissionStatus == "paid" ? Color.green.opacity(0.2) : Color.orange.opacity(0.2))
                                .foregroundColor(sale.commissionStatus == "paid" ? .green : .orange)
                                .cornerRadius(6)
                        }
                    }
                }
            }
        }
        .navigationTitle("Affiliate Sales")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            do {
                sales = try await MarketplaceApiService.shared.getAffiliateSales()
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}

#Preview {
    NavigationView {
        PromoterDashboardView()
    }
}
