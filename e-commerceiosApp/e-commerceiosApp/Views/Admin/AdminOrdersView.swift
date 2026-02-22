import SwiftUI

struct AdminOrdersView: View {
    @StateObject private var viewModel = AdminOrdersViewModel()
    @State private var selectedOrder: AdminOrderResponse?
    
    var body: some View {
        VStack(spacing: 0) {
            // Search Bar
            searchBar
            
            // Stats Row
            statsRow
            
            // Status Filter Chips (use VM statusBadges)
            statusFilterChips
            
            Divider()
            
            // Orders count + revenue
            if viewModel.hasOrders {
                HStack {
                    Text("\(viewModel.filteredCount) of \(viewModel.totalCount) orders")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("Revenue: \(viewModel.formattedTotalRevenue)")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
                .padding(.horizontal)
                .padding(.vertical, 4)
            }
            
            // Orders List
            if viewModel.isLoading && !viewModel.hasOrders {
                Spacer()
                ProgressView("Loading orders...")
                Spacer()
            } else if let error = viewModel.errorMessage, !viewModel.hasOrders {
                Spacer()
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text(error)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)
                    Button("Retry") { viewModel.retry() }
                        .buttonStyle(.borderedProminent)
                        .tint(.orange)
                }
                Spacer()
            } else if viewModel.filteredOrders.isEmpty {
                Spacer()
                VStack(spacing: 16) {
                    Image(systemName: viewModel.searchQuery.isEmpty ? "cart" : "magnifyingglass")
                        .font(.system(size: 50))
                        .foregroundColor(.gray)
                    Text(viewModel.searchQuery.isEmpty ? "No orders found" : "No orders match \"\(viewModel.searchQuery)\"")
                        .foregroundColor(.gray)
                    if !viewModel.searchQuery.isEmpty || viewModel.selectedStatus != nil {
                        Button("Clear Filters") { viewModel.clearFilters() }
                            .font(.caption)
                            .foregroundColor(AppColors.primary)
                    }
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.filteredOrders, id: \.id) { order in
                            AdminOrderCard(
                                order: order,
                                isUpdating: viewModel.isUpdating(order.id),
                                onTap: { selectedOrder = order },
                                onUpdateStatus: { newStatus in
                                    viewModel.updateOrderStatus(order, newStatus: newStatus)
                                }
                            )
                        }
                    }
                    .padding()
                }
                .refreshable { viewModel.refresh() }
            }
        }
        .navigationTitle("Orders Management")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $selectedOrder) { order in
            AdminOrderDetailView(order: order)
        }
        .onAppear {
            if !viewModel.hasOrders { viewModel.loadOrders() }
        }
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            TextField("Search orders...", text: $viewModel.searchQuery)
                .textFieldStyle(.plain)
                .font(.subheadline)
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
    
    // MARK: - Stats Row
    private var statsRow: some View {
        HStack(spacing: 12) {
            OrderStatBadge(title: "Total", count: viewModel.totalCount, color: .blue)
            OrderStatBadge(title: "Pending", count: viewModel.stats.pending, color: .orange)
            OrderStatBadge(title: "Shipped", count: viewModel.stats.shipped, color: .purple)
            OrderStatBadge(title: "Delivered", count: viewModel.stats.delivered, color: .green)
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Status Filter Chips (using VM statusBadges)
    private var statusFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.statusBadges, id: \.label) { badge in
                    Button(action: { viewModel.selectedStatus = badge.status }) {
                        HStack(spacing: 4) {
                            Text(badge.label)
                                .font(.caption)
                                .fontWeight(.medium)
                            Text("(\(badge.count))")
                                .font(.system(size: 10))
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(viewModel.selectedStatus == badge.status ? badge.color.opacity(0.2) : Color(.systemGray6))
                        .foregroundColor(viewModel.selectedStatus == badge.status ? badge.color : .secondary)
                        .cornerRadius(16)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(viewModel.selectedStatus == badge.status ? badge.color : .clear, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal)
        }
    }
}

// MARK: - Order Stat Badge
struct OrderStatBadge: View {
    let title: String
    let count: Int
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(color)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

// MARK: - Admin Order Card
struct AdminOrderCard: View {
    let order: AdminOrderResponse
    var isUpdating: Bool = false
    let onTap: () -> Void
    let onUpdateStatus: (String) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Order #\(order.id)")
                        .font(.headline)
                    
                    Text(AdminOrdersViewModel.formatOrderDate(order.createdAt))
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                StatusBadge(status: order.status)
            }
            
            // Order Details
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("User ID: \(order.userId)")
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    Text("\(order.items?.count ?? 0) items")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                Text("$\(order.total, specifier: "%.2f")")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
            }
            
            // Action Buttons
            HStack(spacing: 12) {
                Button(action: onTap) {
                    HStack {
                        Image(systemName: "eye")
                        Text("View Details")
                    }
                    .font(.caption)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(Color.blue.opacity(0.2))
                    .foregroundColor(.blue)
                    .cornerRadius(8)
                }
                
                if isUpdating {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                } else {
                    Menu {
                        Button("Pending") { onUpdateStatus("pending") }
                        Button("Processing") { onUpdateStatus("processing") }
                        Button("Shipped") { onUpdateStatus("shipped") }
                        Button("Delivered") { onUpdateStatus("delivered") }
                        Button("Cancelled") { onUpdateStatus("cancelled") }
                    } label: {
                        HStack {
                            Image(systemName: "arrow.triangle.2.circlepath")
                            Text("Update Status")
                        }
                        .font(.caption)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color.orange.opacity(0.2))
                        .foregroundColor(.orange)
                        .cornerRadius(8)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

// MARK: - Status Badge
struct StatusBadge: View {
    let status: String
    
    var body: some View {
        let color = AdminOrdersViewModel.statusColor(status)
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 6, height: 6)
            Text(status.capitalized)
                .font(.caption)
                .fontWeight(.medium)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(color.opacity(0.15))
        .foregroundColor(color)
        .cornerRadius(12)
    }
}

// MARK: - Admin Order Detail View
struct AdminOrderDetailView: View {
    let order: AdminOrderResponse
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Order Info
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Order #\(order.id)")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        StatusBadge(status: order.status)
                        
                        Text("Created: \(AdminOrdersViewModel.formatOrderDate(order.createdAt))")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    
                    Divider()
                    
                    // User Info
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Customer")
                            .font(.headline)
                        
                        Text("User ID: \(order.userId)")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    
                    Divider()
                    
                    // Items
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Items (\(order.items?.count ?? 0))")
                            .font(.headline)
                        
                        if let items = order.items {
                            ForEach(items, id: \.productId) { item in
                                HStack {
                                    VStack(alignment: .leading) {
                                        Text(item.productName ?? "Product \(item.productId ?? "-")")
                                            .font(.subheadline)
                                        Text("Qty: \(item.quantity)")
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                    }
                                    
                                    Spacer()
                                    
                                    Text("$\(item.price * Double(item.quantity), specifier: "%.2f")")
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                }
                                .padding()
                                .background(Color(.systemGray6))
                                .cornerRadius(8)
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Total
                    HStack {
                        Text("Total")
                            .font(.title3)
                            .fontWeight(.bold)
                        
                        Spacer()
                        
                        Text("$\(order.total, specifier: "%.2f")")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                    }
                }
                .padding()
            }
            .navigationTitle("Order Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}

#Preview {
    NavigationView {
        AdminOrdersView()
    }
}
