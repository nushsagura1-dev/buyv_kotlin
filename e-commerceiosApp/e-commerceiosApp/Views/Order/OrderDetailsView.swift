import SwiftUI
import Shared

struct OrderDetailsView: View {
    let orderId: String
    @StateObject private var viewModel = OrderViewModel()
    @StateObject private var cartViewModel = CartViewModel()
    @Environment(\.presentationMode) var presentationMode
    @State private var showCancelAlert = false
    @State private var copiedOrderNumber = false
    @State private var showReorderSuccess = false
    @State private var isReordering = false
    @State private var showReviewToast = false
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView("Loading order...")
            } else if let order = viewModel.selectedOrder {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Header Card
                        orderHeaderCard(order: order)
                        
                        // Order Timeline
                        orderTimeline(order: order)
                        
                        // Estimated Delivery
                        if order.status != .canceled && order.status != .delivered && order.status != .refunded {
                            estimatedDeliveryCard(order: order)
                        }
                        
                        // Items
                        itemsCard(order: order)
                        
                        // Shipping Address
                        if let address = order.shippingAddress {
                            shippingCard(address: address)
                        }
                        
                        // Payment Summary
                        paymentCard(order: order)
                        
                        // Action Buttons
                        actionButtons(order: order)
                    }
                    .padding()
                }
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text("Error: \(error)")
                        .multilineTextAlignment(.center)
                        .padding()
                    Button("Retry") {
                        viewModel.loadOrderDetails(orderId: orderId)
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .background(AppColors.primary)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
            }
            
            // Reorder success toast
            if showReorderSuccess {
                VStack {
                    Spacer()
                    HStack(spacing: 8) {
                        Image(systemName: "cart.badge.plus")
                        Text("Items added to cart!")
                    }
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(Color.green.opacity(0.9))
                    .cornerRadius(24)
                    .padding(.bottom, 32)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(10)
            }
        }
        .navigationTitle("Order Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: shareOrder) {
                        Label("Share Order", systemImage: "square.and.arrow.up")
                    }
                    Button(action: { copyOrderNumber() }) {
                        Label("Copy Order Number", systemImage: "doc.on.doc")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .onAppear {
            viewModel.loadOrderDetails(orderId: orderId)
        }
        .alert("Cancel Order", isPresented: $showCancelAlert) {
            Button("Keep Order", role: .cancel) {}
            Button("Cancel Order", role: .destructive) {
                viewModel.cancelOrder(orderId: orderId)
            }
        } message: {
            Text("Are you sure you want to cancel this order? This action cannot be undone.")
        }
        .overlay(alignment: .bottom) {
            if copiedOrderNumber {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text("Order number copied!")
                        .fontWeight(.medium)
                }
                .padding()
                .background(.ultraThinMaterial)
                .cornerRadius(12)
                .padding(.bottom, 20)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
            if showReviewToast {
                HStack {
                    Image(systemName: "star.fill")
                        .foregroundColor(.orange)
                    Text("Product reviews coming soon!")
                        .fontWeight(.medium)
                }
                .padding()
                .background(.ultraThinMaterial)
                .cornerRadius(12)
                .padding(.bottom, 20)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }
    
    // MARK: - Header Card
    private func orderHeaderCard(order: Order) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Order #\(order.orderNumber)")
                        .font(.title3)
                        .fontWeight(.bold)
                    Text(formatDate(order.createdAt))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Status badge
                Text(order.status.displayName)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(statusColor(item: order.status))
                    .cornerRadius(16)
            }
            
            // Item count
            HStack(spacing: 4) {
                Image(systemName: "bag")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("\(order.items.count) item\(order.items.count > 1 ? "s" : "")")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Order Timeline
    private func orderTimeline(order: Order) -> some View {
        let steps: [(String, String, OrderStatus)] = [
            ("clock", "Placed", .pending),
            ("checkmark.circle", "Confirmed", .confirmed),
            ("shippingbox", "Shipped", .shipped),
            ("truck.box", "Out for Delivery", .outForDelivery),
            ("house", "Delivered", .delivered)
        ]
        
        let currentIndex = steps.firstIndex(where: { $0.2 == order.status }) ?? -1
        let isCanceled = order.status == .canceled || order.status == .refunded
        
        return VStack(alignment: .leading, spacing: 0) {
            Text("Order Progress")
                .font(.headline)
                .padding(.bottom, 12)
            
            ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                HStack(spacing: 12) {
                    VStack(spacing: 0) {
                        Circle()
                            .fill(isCanceled ? Color.gray.opacity(0.3) :
                                    (index <= currentIndex ? AppColors.primary : Color.gray.opacity(0.3)))
                            .frame(width: 28, height: 28)
                            .overlay(
                                Image(systemName: index <= currentIndex && !isCanceled ? "checkmark" : step.0)
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(index <= currentIndex && !isCanceled ? .white : .gray)
                            )
                        
                        if index < steps.count - 1 {
                            Rectangle()
                                .fill(isCanceled ? Color.gray.opacity(0.2) :
                                        (index < currentIndex ? AppColors.primary : Color.gray.opacity(0.2)))
                                .frame(width: 2, height: 24)
                        }
                    }
                    
                    Text(step.1)
                        .font(.subheadline)
                        .foregroundColor(index <= currentIndex && !isCanceled ? .primary : .secondary)
                        .fontWeight(index == currentIndex && !isCanceled ? .semibold : .regular)
                    
                    Spacer()
                }
            }
            
            if isCanceled {
                HStack(spacing: 12) {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 28, height: 28)
                        .overlay(
                            Image(systemName: "xmark")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        )
                    
                    Text(order.status == .refunded ? "Refunded" : "Canceled")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.red)
                }
                .padding(.top, 8)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Estimated Delivery
    private func estimatedDeliveryCard(order: Order) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "calendar.badge.clock")
                .font(.title2)
                .foregroundColor(AppColors.primary)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Estimated Delivery")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(estimatedDeliveryDate(from: order.createdAt))
                    .font(.subheadline)
                    .fontWeight(.semibold)
            }
            
            Spacer()
        }
        .padding()
        .background(AppColors.primary.opacity(0.08))
        .cornerRadius(12)
    }
    
    // MARK: - Items Card
    private func itemsCard(order: Order) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Items (\(order.items.count))")
                .font(.headline)
                .padding()
            
            Divider()
            
            ForEach(order.items, id: \.id) { item in
                OrderItemRowDetailed(item: item)
                if item.id != order.items.last?.id {
                    Divider().padding(.leading, 86)
                }
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Shipping Card
    private func shippingCard(address: Address) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "location.fill")
                    .foregroundColor(AppColors.primary)
                Text("Shipping Address")
                    .font(.headline)
            }
            
            Divider()
            
            VStack(alignment: .leading, spacing: 4) {
                Text(address.name)
                    .fontWeight(.medium)
                Text(address.street)
                    .foregroundColor(.secondary)
                Text("\(address.city), \(address.state) \(address.zipCode)")
                    .foregroundColor(.secondary)
                Text(address.country)
                    .foregroundColor(.secondary)
                HStack(spacing: 4) {
                    Image(systemName: "phone")
                        .font(.caption2)
                    Text(address.phone)
                }
                .foregroundColor(.secondary)
                .font(.caption)
                .padding(.top, 2)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Payment Card
    private func paymentCard(order: Order) -> some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: "creditcard")
                    .foregroundColor(AppColors.primary)
                Text("Payment Summary")
                    .font(.headline)
                Spacer()
            }
            
            Divider()
            
            SummaryRow(title: "Subtotal", value: order.subtotal)
            SummaryRow(title: "Shipping", value: order.shipping)
            SummaryRow(title: "Tax", value: order.tax)
            
            Divider()
            
            HStack {
                Text("Total")
                    .font(.headline)
                    .fontWeight(.bold)
                Spacer()
                Text("$\(String(format: "%.2f", order.total))")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.primary)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Action Buttons
    private func actionButtons(order: Order) -> some View {
        VStack(spacing: 10) {
            // Track Order
            if order.status != .canceled && order.status != .refunded {
                NavigationLink(destination: TrackOrderView(orderId: orderId)) {
                    HStack {
                        Image(systemName: "location.fill")
                        Text("Track Order")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
            }
            
            HStack(spacing: 10) {
                // Cancel Order (only for pending/confirmed)
                if order.status == .pending || order.status == .confirmed {
                    Button(action: { showCancelAlert = true }) {
                        HStack {
                            Image(systemName: "xmark.circle")
                            Text("Cancel")
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .foregroundColor(.red)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.red, lineWidth: 1))
                    }
                }
                
                // Reorder (for delivered/canceled)
                if order.status == .delivered || order.status == .canceled {
                    Button(action: reorder) {
                        HStack {
                            if isReordering {
                                ProgressView()
                                    .tint(AppColors.primary)
                            } else {
                                Image(systemName: "arrow.counterclockwise")
                            }
                            Text(isReordering ? "Adding to cart..." : "Reorder")
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .foregroundColor(AppColors.primary)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(AppColors.primary, lineWidth: 1))
                    }
                    .disabled(isReordering)
                }
            }
            
            // Leave Review (for delivered)
            if order.status == .delivered {
                Button(action: {
                    showReviewToast = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        showReviewToast = false
                    }
                }) {
                    HStack {
                        Image(systemName: "star")
                        Text("Leave a Review")
                            .fontWeight(.medium)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .foregroundColor(.orange)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.orange, lineWidth: 1))
                }
            }
        }
        .padding(.bottom, 8)
    }
    
    // MARK: - Helpers
    func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
    
    func estimatedDeliveryDate(from timestamp: Int64) -> String {
        let orderDate = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let deliveryDate = Calendar.current.date(byAdding: .day, value: 7, to: orderDate) ?? orderDate
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: deliveryDate)
    }
    
    func statusColor(item: OrderStatus) -> Color {
        switch item {
        case .pending: return .orange
        case .confirmed: return .blue
        case .processing: return .cyan
        case .shipped: return .purple
        case .outForDelivery: return .indigo
        case .delivered: return .green
        case .canceled: return .red
        case .returned: return .gray
        case .refunded: return .pink
        default: return .gray
        }
    }
    
    private func copyOrderNumber() {
        guard let order = viewModel.selectedOrder else { return }
        UIPasteboard.general.string = order.orderNumber
        withAnimation { copiedOrderNumber = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation { copiedOrderNumber = false }
        }
    }
    
    private func shareOrder() {
        guard let order = viewModel.selectedOrder else { return }
        let text = "BuyV Order #\(order.orderNumber) - \(order.items.count) items - $\(String(format: "%.2f", order.total))"
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = windowScene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
    }
    
    private func reorder() {
        guard let order = viewModel.selectedOrder else { return }
        isReordering = true
        
        for item in order.items {
            cartViewModel.addToCart(productId: item.productId, quantity: Int(item.quantity))
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            isReordering = false
            showReorderSuccess = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                withAnimation { showReorderSuccess = false }
            }
        }
    }
}

struct OrderItemRowDetailed: View {
    let item: OrderItem
    
    var body: some View {
        HStack(alignment: .top) {
            // Placeholder image - In real app use AsyncImage with AsyncImageLoader
            if !item.productImage.isEmpty, let url = URL(string: item.productImage) {
                 AsyncImage(url: url) { phase in
                     switch phase {
                     case .empty:
                         Color.gray.opacity(0.1)
                     case .success(let image):
                         image.resizable().aspectRatio(contentMode: .fill)
                     case .failure:
                         Color.gray.opacity(0.3)
                     @unknown default:
                         EmptyView()
                     }
                 }
                 .frame(width: 70, height: 70)
                 .cornerRadius(6)
                 .clipped()
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.1))
                    .frame(width: 70, height: 70)
                    .cornerRadius(6)
                    .overlay(Image(systemName: "photo").foregroundColor(.gray))
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(item.productName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)
                
                HStack {
                    if let size = item.size {
                        Text("Size: \(size)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(4)
                    }
                     if let color = item.color {
                        Text(color)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(4)
                    }
                }
                
                Text("Quantity: \(item.quantity)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.leading, 8)
            
            Spacer()
            
            Text("$\(String(format: "%.2f", item.price * Double(item.quantity)))")
                .fontWeight(.bold)
                .font(.subheadline)
        }
        .padding()
    }
}

struct SummaryRow: View {
    let title: String
    let value: Double
    
    var body: some View {
        HStack {
            Text(title)
                .foregroundColor(.secondary)
            Spacer()
            Text("$\(String(format: "%.2f", value))")
        }
    }
}
