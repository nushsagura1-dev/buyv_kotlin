import SwiftUI
import Shared

// MARK: - Order Filter
enum OrderFilter: String, CaseIterable {
    case all = "All"
    case pending = "Pending"
    case confirmed = "Confirmed"
    case shipped = "Shipped"
    case delivered = "Delivered"
    case canceled = "Canceled"
    
    var orderStatus: OrderStatus? {
        switch self {
        case .all: return nil
        case .pending: return .pending
        case .confirmed: return .confirmed
        case .shipped: return .shipped
        case .delivered: return .delivered
        case .canceled: return .canceled
        }
    }
}

struct OrderListView: View {
    @StateObject private var viewModel = OrderViewModel()
    @State private var selectedFilter: OrderFilter = .all
    @State private var showCancelDialog = false
    @State private var orderToCancel: Order?
    @State private var cancelReason = ""
    
    private var filteredOrders: [Order] {
        guard let status = selectedFilter.orderStatus else {
            return viewModel.orders
        }
        return viewModel.orders.filter { $0.status == status }
    }
    
    private func orderCount(for filter: OrderFilter) -> Int {
        guard let status = filter.orderStatus else {
            return viewModel.orders.count
        }
        return viewModel.orders.filter { $0.status == status }.count
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Status filter tabs
                    filterTabs
                    
                    // Content
                    if viewModel.isLoading && viewModel.orders.isEmpty {
                        Spacer()
                        ProgressView("Loading orders...")
                        Spacer()
                    } else if filteredOrders.isEmpty {
                        Spacer()
                        emptyState
                        Spacer()
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(filteredOrders, id: \.id) { order in
                                    NavigationLink(destination: OrderDetailsView(orderId: order.id)) {
                                        OrderItemRow(
                                            order: order,
                                            onCancel: {
                                                orderToCancel = order
                                                showCancelDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                            .padding(.horizontal)
                            .padding(.top, 12)
                        }
                        .refreshable {
                            viewModel.loadUserOrders()
                        }
                    }
                }
            }
            .navigationTitle("My Orders")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.loadUserOrders() }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .onAppear {
                viewModel.loadUserOrders()
            }
            .alert("Cancel Order", isPresented: $showCancelDialog) {
                TextField("Reason (optional)", text: $cancelReason)
                Button("Cancel Order", role: .destructive) {
                    if let order = orderToCancel {
                        viewModel.cancelOrder(orderId: order.id, reason: cancelReason)
                        cancelReason = ""
                    }
                }
                Button("Keep Order", role: .cancel) {
                    cancelReason = ""
                }
            } message: {
                Text("Are you sure you want to cancel this order?")
            }
            .alert(item: Binding<ErrorWrapper?>(
                get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
                set: { viewModel.errorMessage = $0?.message }
            )) { errorWrapper in
                 Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
            }
        }
    }
    
    // MARK: - Filter Tabs
    private var filterTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(OrderFilter.allCases, id: \.self) { filter in
                    let count = orderCount(for: filter)
                    Button(action: { withAnimation(.easeInOut(duration: 0.2)) { selectedFilter = filter } }) {
                        HStack(spacing: 4) {
                            Text(filter.rawValue)
                                .font(.subheadline)
                                .fontWeight(selectedFilter == filter ? .bold : .regular)
                            
                            if count > 0 {
                                Text("\(count)")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(selectedFilter == filter ? AppColors.primary : .secondary)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(
                                        Capsule()
                                            .fill(selectedFilter == filter ? AppColors.primary.opacity(0.15) : Color.gray.opacity(0.15))
                                    )
                            }
                        }
                        .foregroundColor(selectedFilter == filter ? AppColors.primary : .secondary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(
                            Capsule()
                                .fill(selectedFilter == filter ? AppColors.primary.opacity(0.1) : Color.clear)
                        )
                        .overlay(
                            Capsule()
                                .stroke(selectedFilter == filter ? AppColors.primary : Color.gray.opacity(0.3), lineWidth: 1)
                        )
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 10)
        }
        .background(Color.white)
    }
    
    // MARK: - Empty State
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: selectedFilter == .all ? "box.truck" : "magnifyingglass")
                .font(.system(size: 60))
                .foregroundColor(.gray.opacity(0.5))
            Text(selectedFilter == .all ? "No orders yet" : "No \(selectedFilter.rawValue.lowercased()) orders")
                .font(.title3)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
            if selectedFilter == .all {
                Text("Start shopping to see your orders here!")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

// MARK: - Order Item Row
struct OrderItemRow: View {
    let order: Order
    var onCancel: (() -> Void)? = nil
    @State private var showReviewToast = false
    
    private var isCancellable: Bool {
        order.status == .pending || order.status == .confirmed
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Order ID + Status
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Order #\(order.orderNumber)")
                        .font(.headline)
                        .foregroundColor(.primary)
                    Text(formatDate(order.createdAt))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                OrderStatusBadge(status: order.status)
            }
            
            Divider()
            
            // Product thumbnails row
            if !order.items.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(order.items.prefix(4)), id: \.id) { item in
                            VStack(spacing: 4) {
                                if !item.productImage.isEmpty, let url = URL(string: item.productImage) {
                                    AsyncImage(url: url) { image in
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        RoundedRectangle(cornerRadius: 6)
                                            .fill(Color.gray.opacity(0.15))
                                            .overlay(
                                                Image(systemName: "photo")
                                                    .foregroundColor(.gray.opacity(0.5))
                                            )
                                    }
                                    .frame(width: 56, height: 56)
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                                } else {
                                    RoundedRectangle(cornerRadius: 6)
                                        .fill(Color.gray.opacity(0.15))
                                        .frame(width: 56, height: 56)
                                        .overlay(
                                            Image(systemName: "bag")
                                                .foregroundColor(.gray.opacity(0.5))
                                        )
                                }
                                
                                Text("x\(item.quantity)")
                                    .font(.system(size: 10))
                                    .foregroundColor(.secondary)
                            }
                        }
                        
                        if order.items.count > 4 {
                            VStack {
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(Color.gray.opacity(0.1))
                                    .frame(width: 56, height: 56)
                                    .overlay(
                                        Text("+\(order.items.count - 4)")
                                            .font(.subheadline)
                                            .fontWeight(.medium)
                                            .foregroundColor(.secondary)
                                    )
                                Text("")
                                    .font(.system(size: 10))
                            }
                        }
                    }
                }
            }
            
            // Footer: Total + Action buttons
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(order.items.count) item\(order.items.count == 1 ? "" : "s")")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("$\(String(format: "%.2f", order.total))")
                        .font(.headline)
                        .foregroundColor(AppColors.primary)
                }
                
                Spacer()
                
                // Action buttons based on status
                if order.status == .delivered {
                    Button(action: {
                        withAnimation { showReviewToast = true }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation { showReviewToast = false }
                        }
                    }) {
                        Label("Review", systemImage: "star")
                            .font(.caption)
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(AppColors.primary)
                            .cornerRadius(16)
                    }
                }
                
                if isCancellable, let onCancel = onCancel {
                    Button(action: onCancel) {
                        Label("Cancel", systemImage: "xmark")
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .overlay(
                                Capsule().stroke(Color.red, lineWidth: 1)
                            )
                    }
                }
            }
            
            // Tracking info for shipped orders
            if let tracking = order.trackingNumber, !tracking.isEmpty,
               order.status == .shipped || order.status == .outForDelivery {
                HStack(spacing: 6) {
                    Image(systemName: "shippingbox")
                        .font(.caption)
                        .foregroundColor(.purple)
                    Text("Tracking: \(tracking)")
                        .font(.caption)
                        .foregroundColor(.purple)
                }
                .padding(.top, 2)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
        .overlay(
            Group {
                if showReviewToast {
                    VStack {
                        HStack(spacing: 8) {
                            Image(systemName: "star.fill")
                                .foregroundColor(.yellow)
                            Text("Product reviews coming soon!")
                                .font(.caption)
                                .fontWeight(.medium)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color(.systemBackground))
                        .cornerRadius(20)
                        .shadow(radius: 4)
                    }
                    .transition(.scale.combined(with: .opacity))
                }
            },
            alignment: .center
        )
    }
    
    func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

struct OrderStatusBadge: View {
    let status: OrderStatus
    
    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(statusColor)
                .frame(width: 6, height: 6)
            Text(status.displayName)
                .font(.caption)
                .fontWeight(.semibold)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(statusColor.opacity(0.12))
        .foregroundColor(statusColor)
        .cornerRadius(12)
    }
    
    private var statusColor: Color {
        switch status {
        case .pending: return .orange
        case .confirmed: return .blue
        case .processing: return .indigo
        case .shipped: return .purple
        case .outForDelivery: return .teal
        case .delivered: return .green
        case .canceled: return .red
        case .returned: return .pink
        case .refunded: return .gray
        default: return .gray
        }
    }
}
