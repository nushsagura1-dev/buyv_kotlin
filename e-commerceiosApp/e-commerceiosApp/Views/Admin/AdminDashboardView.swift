import SwiftUI

struct AdminDashboardView: View {
    @StateObject private var viewModel = AdminDashboardViewModel()
    @State private var showLogoutAlert = false
    @State private var expandedSection: String? = nil
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Error Banner
                    if let error = viewModel.errorMessage {
                        errorBanner(message: error)
                    }
                    
                    // Stats Cards (all 8)
                    statsSection
                    
                    // Pending Orders Warning
                    if viewModel.pendingOrdersCount > 0 {
                        pendingOrdersBanner
                    }
                    
                    // Quick Actions
                    quickActionsSection
                    
                    // Recent Users
                    if viewModel.hasRecentUsers {
                        recentUsersSection
                    }
                    
                    // Recent Orders
                    if viewModel.hasRecentOrders {
                        recentOrdersSection
                    }
                    
                    // Management Sections
                    managementSections
                }
                .padding()
            }
            .navigationTitle("Admin Dashboard")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showLogoutAlert = true
                    }) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .foregroundColor(.red)
                    }
                }
            }
            .refreshable {
                await viewModel.loadDashboardData()
            }
            .alert("Logout", isPresented: $showLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Logout", role: .destructive) {
                    viewModel.logout()
                }
            } message: {
                Text("Are you sure you want to logout?")
            }
            .onAppear {
                Task {
                    await viewModel.loadDashboardData()
                }
            }
            .overlay {
                if viewModel.isLoading && !viewModel.hasData {
                    ProgressView("Loading dashboard...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(.ultraThinMaterial)
                }
            }
        }
    }
    
    // MARK: - Error Banner
    private func errorBanner(message: String) -> some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
            Text(message)
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
            Button("Retry") {
                viewModel.retry()
            }
            .font(.caption.bold())
            .foregroundColor(AppColors.primary)
        }
        .padding(12)
        .background(Color.red.opacity(0.1))
        .cornerRadius(10)
    }
    
    // MARK: - Pending Orders Banner
    private var pendingOrdersBanner: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.circle.fill")
                .font(.title2)
                .foregroundColor(.orange)
            VStack(alignment: .leading, spacing: 2) {
                Text("\(viewModel.pendingOrdersCount) Pending Orders")
                    .font(.subheadline.bold())
                Text("Require attention")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            NavigationLink(destination: AdminOrdersView()) {
                Text("View")
                    .font(.caption.bold())
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.orange)
                    .cornerRadius(8)
            }
        }
        .padding()
        .background(Color.orange.opacity(0.1))
        .cornerRadius(12)
    }
    
    // MARK: - Stats Section (all 8 stat cards)
    private var statsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Statistics")
                    .font(.title2)
                    .fontWeight(.bold)
                Spacer()
                if viewModel.stats.newUsersToday > 0 {
                    Text("+\(viewModel.stats.newUsersToday) today")
                        .font(.caption)
                        .foregroundColor(.green)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(6)
                }
            }
            
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                ForEach(viewModel.allStatCards, id: \.title) { card in
                    StatCard(
                        title: card.title,
                        value: card.value,
                        icon: card.icon,
                        color: card.color
                    )
                }
            }
        }
    }
    
    // MARK: - Recent Users Section
    private var recentUsersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Users")
                    .font(.headline)
                Spacer()
                NavigationLink(destination: AdminUsersView()) {
                    Text("See All")
                        .font(.caption)
                        .foregroundColor(AppColors.primary)
                }
            }
            
            VStack(spacing: 0) {
                ForEach(viewModel.recentUsers, id: \.id) { user in
                    HStack(spacing: 12) {
                        // Avatar
                        ZStack {
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [AppColors.primary.opacity(0.3), AppColors.secondaryColor.opacity(0.3)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(width: 40, height: 40)
                            
                            Text(String(user.username.prefix(1)).uppercased())
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(AppColors.primary)
                        }
                        
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(user.username)
                                    .font(.subheadline.weight(.medium))
                                if user.isVerified {
                                    Image(systemName: "checkmark.seal.fill")
                                        .font(.system(size: 12))
                                        .foregroundColor(.blue)
                                }
                            }
                            Text(user.email)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                        
                        Spacer()
                        
                        Text(user.role ?? "user")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(4)
                    }
                    .padding(.vertical, 8)
                    .padding(.horizontal, 12)
                    
                    if user.id != viewModel.recentUsers.last?.id {
                        Divider().padding(.leading, 64)
                    }
                }
            }
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
        }
    }
    
    // MARK: - Recent Orders Section
    private var recentOrdersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Orders")
                    .font(.headline)
                Spacer()
                NavigationLink(destination: AdminOrdersView()) {
                    Text("See All")
                        .font(.caption)
                        .foregroundColor(AppColors.primary)
                }
            }
            
            VStack(spacing: 0) {
                ForEach(viewModel.recentOrders, id: \.id) { order in
                    HStack(spacing: 12) {
                        // Order icon
                        Image(systemName: "bag.fill")
                            .font(.system(size: 16))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(orderStatusColor(order.status))
                            .cornerRadius(8)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Order #\(order.id)")
                                .font(.subheadline.weight(.medium))
                            Text(order.userEmail ?? "Unknown user")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                        
                        Spacer()
                        
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("$\(String(format: "%.2f", order.totalAmount))")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(AppColors.primary)
                            Text(order.status.capitalized)
                                .font(.caption2)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(orderStatusColor(order.status))
                                .cornerRadius(4)
                        }
                    }
                    .padding(.vertical, 8)
                    .padding(.horizontal, 12)
                    
                    if order.id != viewModel.recentOrders.last?.id {
                        Divider().padding(.leading, 60)
                    }
                }
            }
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 5, y: 2)
        }
    }
    
    private func orderStatusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "pending": return .orange
        case "processing": return .blue
        case "shipped": return .purple
        case "delivered": return .green
        case "cancelled": return .red
        default: return .gray
        }
    }
    
    // MARK: - Quick Actions
    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Actions")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                NavigationLink(destination: AdminProductsView()) {
                    QuickActionButton(
                        title: "Products",
                        icon: "bag.badge.plus",
                        color: .orange
                    )
                }
                
                NavigationLink(destination: AdminOrdersView()) {
                    QuickActionButton(
                        title: "Orders",
                        icon: "cart.badge.plus",
                        color: .green
                    )
                }
                
                NavigationLink(destination: AdminUsersView()) {
                    QuickActionButton(
                        title: "Users",
                        icon: "person.crop.circle.badge.checkmark",
                        color: .blue
                    )
                }
                
                NavigationLink(destination: AdminCJImportView()) {
                    QuickActionButton(
                        title: "CJ Import",
                        icon: "square.and.arrow.down.fill",
                        color: .indigo
                    )
                }
                
                NavigationLink(destination: AdminCommissionsView()) {
                    QuickActionButton(
                        title: "Commissions",
                        icon: "dollarsign.circle.fill",
                        color: .teal
                    )
                }
                
                NavigationLink(destination: AdminWithdrawalsView()) {
                    QuickActionButton(
                        title: "Withdrawals",
                        icon: "banknote.fill",
                        color: .purple
                    )
                }
            }
        }
    }
    
    // MARK: - Management Sections
    private var managementSections: some View {
        VStack(alignment: .leading, spacing: 20) {
            // User Management
            ManagementSection(
                title: "User Management",
                items: [
                    ManagementItem(title: "Users", icon: "person.fill", destination: AnyView(AdminUsersView())),
                    ManagementItem(title: "Follows", icon: "person.2.fill", destination: AnyView(AdminFollowsPlaceholderView()))
                ]
            )
            
            // Content Management
            ManagementSection(
                title: "Content Management",
                items: [
                    ManagementItem(title: "Posts", icon: "photo.fill", destination: AnyView(AdminPostsPlaceholderView())),
                    ManagementItem(title: "Comments", icon: "bubble.left.fill", destination: AnyView(AdminCommentsPlaceholderView()))
                ]
            )
            
            // Commerce Management
            ManagementSection(
                title: "Commerce",
                items: [
                    ManagementItem(title: "Orders", icon: "cart.fill", destination: AnyView(AdminOrdersView())),
                    ManagementItem(title: "Products", icon: "bag.fill", destination: AnyView(AdminProductsView())),
                    ManagementItem(title: "CJ Import", icon: "square.and.arrow.down.fill", destination: AnyView(AdminCJImportView())),
                    ManagementItem(title: "Categories", icon: "folder.fill", destination: AnyView(AdminCategoriesPlaceholderView()))
                ]
            )
            
            // Finance Management
            ManagementSection(
                title: "Finance",
                items: [
                    ManagementItem(title: "Commissions", icon: "dollarsign.circle.fill", destination: AnyView(AdminCommissionsView())),
                    ManagementItem(title: "Withdrawals", icon: "banknote.fill", destination: AnyView(AdminWithdrawalsView())),
                    ManagementItem(title: "Promoter Wallets", icon: "wallet.pass.fill", destination: AnyView(AdminPromoterWalletsView())),
                    ManagementItem(title: "Affiliate Sales", icon: "chart.line.uptrend.xyaxis", destination: AnyView(AdminAffiliateSalesView()))
                ]
            )
        }
    }
}

// MARK: - Stat Card
struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 30))
                .foregroundColor(color)
            
            Text(value)
                .font(.title)
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

// MARK: - Quick Action Button
struct QuickActionButton: View {
    let title: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(.white)
            
            Text(title)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(color)
        .cornerRadius(12)
    }
}

// MARK: - Management Section
struct ManagementSection: View {
    let title: String
    let items: [ManagementItem]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
            
            VStack(spacing: 0) {
                ForEach(items.indices, id: \.self) { index in
                    NavigationLink(destination: items[index].destination) {
                        HStack {
                            Image(systemName: items[index].icon)
                                .foregroundColor(.orange)
                                .frame(width: 30)
                            
                            Text(items[index].title)
                                .foregroundColor(.primary)
                            
                            Spacer()
                            
                            Image(systemName: "chevron.right")
                                .foregroundColor(.gray)
                                .font(.caption)
                        }
                        .padding()
                        .background(Color.white)
                    }
                    
                    if index < items.count - 1 {
                        Divider()
                            .padding(.leading, 50)
                    }
                }
            }
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
        }
    }
}

// MARK: - Management Item
struct ManagementItem {
    let title: String
    let icon: String
    let destination: AnyView
}

// MARK: - Placeholder Views (replace stubs with proper placeholders)
struct AdminFollowsPlaceholderView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 56))
                    .foregroundColor(AppColors.primary.opacity(0.3))
                    .padding(.top, 40)
                
                Text("Follows Management")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("Manage user follow relationships and analytics across the platform.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                VStack(alignment: .leading, spacing: 12) {
                    featureRow(icon: "chart.bar.fill", text: "Follow/Unfollow analytics over time")
                    featureRow(icon: "person.badge.plus", text: "View top followed users")
                    featureRow(icon: "bell.badge.fill", text: "Monitor suspicious follow patterns")
                    featureRow(icon: "arrow.triangle.2.circlepath", text: "Bulk follow management tools")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                Text("Planned for v2.1")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .navigationTitle("Follows")
    }
}

struct AdminPostsPlaceholderView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "photo.stack.fill")
                    .font(.system(size: 56))
                    .foregroundColor(AppColors.primary.opacity(0.3))
                    .padding(.top, 40)
                
                Text("Posts Management")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("View, moderate, and manage user posts and reels across the platform.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                VStack(alignment: .leading, spacing: 12) {
                    featureRow(icon: "flag.fill", text: "Review flagged/reported posts")
                    featureRow(icon: "eye.slash", text: "Hide or remove inappropriate content")
                    featureRow(icon: "chart.line.uptrend.xyaxis", text: "Post engagement analytics")
                    featureRow(icon: "pin.fill", text: "Pin or feature trending posts")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                Text("Planned for v2.1")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .navigationTitle("Posts")
    }
}

struct AdminCommentsPlaceholderView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "bubble.left.and.bubble.right.fill")
                    .font(.system(size: 56))
                    .foregroundColor(AppColors.primary.opacity(0.3))
                    .padding(.top, 40)
                
                Text("Comments Management")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("Moderate and manage user comments across the platform.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                VStack(alignment: .leading, spacing: 12) {
                    featureRow(icon: "flag.fill", text: "Review reported comments")
                    featureRow(icon: "trash.fill", text: "Bulk delete spam comments")
                    featureRow(icon: "text.magnifyingglass", text: "Search and filter comments")
                    featureRow(icon: "shield.checkered", text: "Auto-moderation rules")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                Text("Planned for v2.2")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .navigationTitle("Comments")
    }
}

struct AdminCategoriesPlaceholderView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "folder.fill.badge.gearshape")
                    .font(.system(size: 56))
                    .foregroundColor(AppColors.primary.opacity(0.3))
                    .padding(.top, 40)
                
                Text("Categories Management")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("Create, edit, and organize product categories for the marketplace.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                VStack(alignment: .leading, spacing: 12) {
                    featureRow(icon: "plus.circle.fill", text: "Create and edit categories")
                    featureRow(icon: "arrow.up.arrow.down", text: "Reorder category display priority")
                    featureRow(icon: "photo.fill", text: "Category image management")
                    featureRow(icon: "tag.fill", text: "Subcategory and tag support")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                Text("Planned for v2.0")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .navigationTitle("Categories")
    }
}

// Shared helper for placeholder feature rows
private func featureRow(icon: String, text: String) -> some View {
    HStack(spacing: 12) {
        Image(systemName: icon)
            .font(.system(size: 14))
            .foregroundColor(AppColors.primary)
            .frame(width: 24)
        Text(text)
            .font(.subheadline)
            .foregroundColor(.primary)
        Spacer()
    }
}

struct AdminNotificationsView: View {
    var body: some View {
        Text("Admin Notifications View - Coming Soon")
            .navigationTitle("Send Notifications")
    }
}

// MARK: - Promoter Wallets (Android parity)
struct AdminPromoterWalletsView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "wallet.pass.fill")
                    .font(.system(size: 56))
                    .foregroundColor(AppColors.primary.opacity(0.3))
                    .padding(.top, 40)
                
                Text("Promoter Wallets")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("Monitor and manage promoter wallet balances, withdrawals, and transaction history.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                VStack(alignment: .leading, spacing: 12) {
                    featureRow(icon: "creditcard.fill", text: "Wallet balance overview")
                    featureRow(icon: "checkmark.seal.fill", text: "Withdrawal validation")
                    featureRow(icon: "clock.arrow.circlepath", text: "Transaction history")
                    featureRow(icon: "exclamationmark.triangle.fill", text: "Anomaly detection")
                    featureRow(icon: "chart.bar.fill", text: "Earnings statistics")
                    featureRow(icon: "doc.text.fill", text: "Accounting export")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                Text("Planned for v2.1")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .navigationTitle("Promoter Wallets")
    }
}

// MARK: - Affiliate Sales (Android parity)
struct AdminAffiliateSalesView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Image(systemName: "chart.line.uptrend.xyaxis")
                    .font(.system(size: 56))
                    .foregroundColor(AppColors.primary.opacity(0.3))
                    .padding(.top, 40)
                
                Text("Affiliate Sales")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text("Track affiliate sales performance, commissions, and growth across the platform.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                VStack(alignment: .leading, spacing: 12) {
                    featureRow(icon: "bag.fill", text: "Affiliate sales overview")
                    featureRow(icon: "dollarsign.circle.fill", text: "Commission tracking")
                    featureRow(icon: "checkmark.shield.fill", text: "Transaction validation")
                    featureRow(icon: "chart.bar.doc.horizontal.fill", text: "Performance reports")
                    featureRow(icon: "arrow.up.right", text: "Growth charts")
                    featureRow(icon: "doc.text.fill", text: "Accounting export")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                Text("Planned for v2.1")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .navigationTitle("Affiliate Sales")
    }
}

#Preview {
    AdminDashboardView()
}
