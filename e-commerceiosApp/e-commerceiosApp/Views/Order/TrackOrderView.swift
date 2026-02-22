import SwiftUI
import Shared

/// Order tracking view with visual timeline — equivalent to Android TrackOrderScreen
struct TrackOrderView: View {
    let orderId: String
    @StateObject private var viewModel = OrderViewModel()
    @StateObject private var cartViewModel = CartViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showCancelAlert = false
    @State private var showCopiedToast = false
    @State private var showContactSupport = false
    @State private var showReorderSuccess = false
    @State private var isReordering = false
    @State private var showDriverCallToast = false
    @State private var showDriverMessageToast = false
    
    // Order status progression
    private let statusSteps: [(status: String, icon: String, label: String, description: String)] = [
        ("PENDING", "clock.fill", "Order Placed", "Your order has been received"),
        ("CONFIRMED", "checkmark.circle.fill", "Confirmed", "Seller has confirmed your order"),
        ("SHIPPED", "shippingbox.fill", "Shipped", "Your order is on its way"),
        ("DELIVERED", "house.fill", "Delivered", "Package has been delivered")
    ]
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            if viewModel.isLoading {
                VStack(spacing: 12) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading order...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            } else if let order = viewModel.selectedOrder {
                ScrollView {
                    VStack(spacing: 20) {
                        // Order Header Card
                        orderHeaderCard(order: order)
                        
                        // Estimated Delivery
                        if order.status.name != "DELIVERED" && order.status.name != "CANCELED" {
                            estimatedDeliveryCard(order: order)
                        }
                        
                        // Status Timeline
                        statusTimeline(currentStatus: order.status.name)
                        
                        // Delivery Info (driver placeholder)
                        if order.status.name == "SHIPPED" {
                            deliveryDriverCard
                        }
                        
                        // Map placeholder
                        if order.status.name == "SHIPPED" {
                            mapPlaceholder
                        }
                        
                        // Shipping Info
                        if let address = order.shippingAddress {
                            shippingCard(address: address)
                        }
                        
                        // Items Summary
                        itemsSummary(order: order)
                        
                        // Action Buttons
                        actionButtons(order: order)
                    }
                    .padding()
                }
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 40))
                        .foregroundColor(.orange)
                    Text(error)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)
                    Button("Retry") {
                        viewModel.loadOrderDetails(orderId: orderId)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.primary)
                }
                .padding()
            }
            
            // Copied toast
            if showCopiedToast {
                VStack {
                    Spacer()
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                        Text("Order number copied!")
                    }
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(Color.black.opacity(0.8))
                    .cornerRadius(24)
                    .padding(.bottom, 32)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(10)
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
        .navigationTitle("Track Order")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    if let order = viewModel.selectedOrder {
                        Button(action: {
                            UIPasteboard.general.string = order.orderNumber
                            withAnimation { showCopiedToast = true }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                withAnimation { showCopiedToast = false }
                            }
                        }) {
                            Label("Copy Order #", systemImage: "doc.on.doc")
                        }
                    }
                    Button(action: { showContactSupport = true }) {
                        Label("Contact Support", systemImage: "headphones")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(.primary)
                }
            }
        }
        .onAppear {
            viewModel.loadOrderDetails(orderId: orderId)
        }
        .overlay(alignment: .bottom) {
            VStack(spacing: 8) {
                if showDriverCallToast {
                    HStack {
                        Image(systemName: "phone.fill")
                            .foregroundColor(.green)
                        Text("Calling driver...")
                            .fontWeight(.medium)
                    }
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                if showDriverMessageToast {
                    HStack {
                        Image(systemName: "message.fill")
                            .foregroundColor(AppColors.primary)
                        Text("In-app messaging coming soon!")
                            .fontWeight(.medium)
                    }
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .padding(.bottom, 20)
            .animation(.easeInOut, value: showDriverCallToast)
            .animation(.easeInOut, value: showDriverMessageToast)
        }
        .sheet(isPresented: $showContactSupport) {
            contactSupportSheet
        }
    }
    
    // MARK: - Order Header Card
    @ViewBuilder
    private func orderHeaderCard(order: Order) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Order #\(order.orderNumber)")
                        .font(.headline)
                    Text(formatDate(order.createdAt))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                statusBadge(status: order.status.name)
            }
            
            Divider()
            
            HStack {
                VStack(alignment: .leading) {
                    Text("Total")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("$\(String(format: "%.2f", order.total))")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("Items")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(order.items.count)")
                        .font(.title3)
                        .fontWeight(.bold)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("Payment")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    HStack(spacing: 2) {
                        Image(systemName: "creditcard.fill")
                            .font(.caption)
                        Text("Card")
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Estimated Delivery
    @ViewBuilder
    private func estimatedDeliveryCard(order: Order) -> some View {
        let daysLeft = estimatedDaysLeft(orderStatus: order.status.name, createdAt: order.createdAt)
        
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(AppColors.primary.opacity(0.1))
                    .frame(width: 50, height: 50)
                Image(systemName: "clock.badge.checkmark")
                    .font(.system(size: 22))
                    .foregroundColor(AppColors.primary)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text("Estimated Delivery")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Text(estimatedDeliveryDate(createdAt: order.createdAt))
                    .font(.headline)
                    .fontWeight(.bold)
                Text("\(daysLeft) days remaining")
                    .font(.caption)
                    .foregroundColor(AppColors.primary)
            }
            
            Spacer()
            
            // Progress ring
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 4)
                    .frame(width: 44, height: 44)
                Circle()
                    .trim(from: 0, to: deliveryProgress(status: order.status.name))
                    .stroke(AppColors.primary, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .frame(width: 44, height: 44)
                    .rotationEffect(.degrees(-90))
                Text("\(Int(deliveryProgress(status: order.status.name) * 100))%")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(AppColors.primary)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Status Timeline
    @ViewBuilder
    private func statusTimeline(currentStatus: String) -> some View {
        let currentIndex = statusSteps.firstIndex(where: { $0.status == currentStatus }) ?? -1
        let isCancelled = currentStatus == "CANCELED"
        
        VStack(alignment: .leading, spacing: 0) {
            Text("Order Status")
                .font(.headline)
                .padding(.bottom, 16)
            
            if isCancelled {
                // Cancelled state
                HStack(spacing: 16) {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 44, height: 44)
                        .overlay(
                            Image(systemName: "xmark")
                                .foregroundColor(.white)
                                .font(.system(size: 18, weight: .bold))
                        )
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Order Cancelled")
                            .font(.headline)
                            .foregroundColor(.red)
                        Text("This order has been cancelled. A refund will be processed within 5-7 business days.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            } else {
                ForEach(Array(statusSteps.enumerated()), id: \.offset) { index, step in
                    HStack(alignment: .top, spacing: 16) {
                        // Timeline connector + icon
                        VStack(spacing: 0) {
                            ZStack {
                                Circle()
                                    .fill(index <= currentIndex ? AppColors.primary : Color.gray.opacity(0.2))
                                    .frame(width: 44, height: 44)
                                
                                if index == currentIndex {
                                    // Pulsing ring for current step
                                    Circle()
                                        .stroke(AppColors.primary.opacity(0.3), lineWidth: 3)
                                        .frame(width: 52, height: 52)
                                }
                                
                                Image(systemName: step.icon)
                                    .foregroundColor(index <= currentIndex ? .white : .gray)
                                    .font(.system(size: 16))
                            }
                            
                            if index < statusSteps.count - 1 {
                                Rectangle()
                                    .fill(index < currentIndex ? AppColors.primary : Color.gray.opacity(0.2))
                                    .frame(width: 3, height: 50)
                            }
                        }
                        
                        // Label + description
                        VStack(alignment: .leading, spacing: 4) {
                            Text(step.label)
                                .font(.subheadline)
                                .fontWeight(index <= currentIndex ? .semibold : .regular)
                                .foregroundColor(index <= currentIndex ? .primary : .gray)
                            
                            Text(step.description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            if index == currentIndex {
                                HStack(spacing: 4) {
                                    Circle().fill(AppColors.primary).frame(width: 6, height: 6)
                                    Text("Current status")
                                        .font(.caption)
                                        .fontWeight(.medium)
                                        .foregroundColor(AppColors.primary)
                                }
                                .padding(.top, 2)
                            } else if index < currentIndex {
                                HStack(spacing: 4) {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 8, weight: .bold))
                                    Text("Completed")
                                        .font(.caption)
                                }
                                .foregroundColor(.green)
                            }
                        }
                        .padding(.top, 8)
                        
                        Spacer()
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Delivery Driver Card
    private var deliveryDriverCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "person.crop.circle.fill")
                    .foregroundColor(AppColors.primary)
                Text("Delivery Details")
                    .font(.headline)
            }
            
            Divider()
            
            HStack(spacing: 12) {
                // Driver avatar placeholder
                ZStack {
                    Circle()
                        .fill(AppColors.primary.opacity(0.15))
                        .frame(width: 50, height: 50)
                    Image(systemName: "person.fill")
                        .font(.system(size: 22))
                        .foregroundColor(AppColors.primary)
                }
                
                VStack(alignment: .leading, spacing: 3) {
                    Text("Delivery Partner")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text("BuyV Express Delivery")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    HStack(spacing: 2) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 9))
                            .foregroundColor(.orange)
                        Text("4.8")
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                }
                
                Spacer()
                
                // Contact buttons
                HStack(spacing: 12) {
                    Button(action: {
                        if let url = URL(string: "tel://+15551234567") {
                            UIApplication.shared.open(url)
                        } else {
                            showDriverCallToast = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { showDriverCallToast = false }
                        }
                    }) {
                        Image(systemName: "phone.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(Color.green)
                            .clipShape(Circle())
                    }
                    
                    Button(action: {
                        showDriverMessageToast = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { showDriverMessageToast = false }
                    }) {
                        Image(systemName: "message.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(AppColors.primary)
                            .clipShape(Circle())
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Map Placeholder
    private var mapPlaceholder: some View {
        VStack(spacing: 0) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.gray.opacity(0.1))
                    .frame(height: 160)
                
                VStack(spacing: 8) {
                    Image(systemName: "map.fill")
                        .font(.system(size: 36))
                        .foregroundColor(AppColors.primary.opacity(0.5))
                    Text("Live tracking coming soon")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            // Route info
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "building.2.fill")
                        .font(.caption)
                        .foregroundColor(.blue)
                    Text("Warehouse")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                Image(systemName: "arrow.right")
                    .font(.caption2)
                    .foregroundColor(.gray)
                Spacer()
                HStack(spacing: 6) {
                    Image(systemName: "truck.box.fill")
                        .font(.caption)
                        .foregroundColor(AppColors.primary)
                    Text("In Transit")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(AppColors.primary)
                }
                Spacer()
                Image(systemName: "arrow.right")
                    .font(.caption2)
                    .foregroundColor(.gray)
                Spacer()
                HStack(spacing: 6) {
                    Image(systemName: "house.fill")
                        .font(.caption)
                        .foregroundColor(.green)
                    Text("Your Address")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.top, 12)
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Shipping Card
    @ViewBuilder
    private func shippingCard(address: ShippingAddress) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "location.fill")
                    .foregroundColor(AppColors.primary)
                Text("Shipping Address")
                    .font(.headline)
                Spacer()
                Button(action: {
                    let addressString = "\(address.street), \(address.city), \(address.state) \(address.zipCode)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
                    if let url = URL(string: "maps://?address=\(addressString)") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    Image(systemName: "map")
                        .font(.caption)
                        .foregroundColor(AppColors.primary)
                }
            }
            
            Divider()
            
            VStack(alignment: .leading, spacing: 4) {
                Text(address.name)
                    .fontWeight(.medium)
                Text(address.street)
                Text("\(address.city), \(address.state) \(address.zipCode)")
                Text(address.country)
                HStack {
                    Image(systemName: "phone.fill")
                        .font(.caption)
                    Text(address.phone)
                }
                .foregroundColor(.secondary)
                .font(.caption)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Items Summary
    @ViewBuilder
    private func itemsSummary(order: Order) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Items (\(order.items.count))")
                .font(.headline)
            
            Divider()
            
            ForEach(order.items, id: \.id) { item in
                HStack {
                    if !item.productImage.isEmpty, let url = URL(string: item.productImage) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().aspectRatio(contentMode: .fill)
                            default:
                                Color.gray.opacity(0.1)
                            }
                        }
                        .frame(width: 50, height: 50)
                        .cornerRadius(6)
                        .clipped()
                    } else {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color.gray.opacity(0.1))
                            .frame(width: 50, height: 50)
                            .overlay(Image(systemName: "photo").foregroundColor(.gray).font(.caption))
                    }
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.productName)
                            .font(.subheadline)
                            .lineLimit(1)
                        Text("Qty: \(item.quantity)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Text("$\(String(format: "%.2f", item.price * Double(item.quantity)))")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                
                if item.id != order.items.last?.id {
                    Divider()
                }
            }
            
            Divider()
            
            // Total row
            HStack {
                Text("Total")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Spacer()
                Text("$\(String(format: "%.2f", order.total))")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.primary)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    // MARK: - Action Buttons
    @ViewBuilder
    private func actionButtons(order: Order) -> some View {
        VStack(spacing: 10) {
            // Cancel Button (only for PENDING orders)
            if order.status.name == "PENDING" {
                Button(action: { showCancelAlert = true }) {
                    HStack {
                        Image(systemName: "xmark.circle.fill")
                        Text("Cancel Order")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.red)
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(12)
                }
                .alert("Cancel Order?", isPresented: $showCancelAlert) {
                    Button("Keep Order", role: .cancel) {}
                    Button("Cancel Order", role: .destructive) {
                        viewModel.cancelOrder(orderId: String(order.id), reason: "Cancelled by user")
                    }
                } message: {
                    Text("Are you sure you want to cancel this order? This action cannot be undone.")
                }
            }
            
            // Reorder (for delivered/cancelled)
            if order.status.name == "DELIVERED" || order.status.name == "CANCELED" {
                Button(action: {
                    reorderItems(order: order)
                }) {
                    HStack {
                        if isReordering {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                        Text(isReordering ? "Adding to cart..." : "Reorder")
                    }
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.white)
                    .background(isReordering ? Color.gray : AppColors.primary)
                    .cornerRadius(12)
                }
                .disabled(isReordering)
            }
            
            // Contact Support
            Button(action: { showContactSupport = true }) {
                HStack {
                    Image(systemName: "headphones")
                    Text("Need Help?")
                }
                .font(.subheadline)
                .frame(maxWidth: .infinity)
                .padding()
                .foregroundColor(AppColors.primary)
                .background(AppColors.primary.opacity(0.1))
                .cornerRadius(12)
            }
        }
    }
    
    // MARK: - Contact Support Sheet
    private var contactSupportSheet: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()
                
                Image(systemName: "headphones.circle.fill")
                    .font(.system(size: 60))
                    .foregroundColor(AppColors.primary)
                
                Text("How can we help?")
                    .font(.title2)
                    .fontWeight(.bold)
                
                VStack(spacing: 12) {
                    supportOption(icon: "envelope.fill", title: "Email Support", subtitle: "support@buyv.app")
                    supportOption(icon: "bubble.left.and.bubble.right.fill", title: "Live Chat", subtitle: "Available 9AM - 9PM")
                    supportOption(icon: "phone.fill", title: "Call Us", subtitle: "+1 (800) BUYV-HELP")
                    supportOption(icon: "questionmark.circle.fill", title: "FAQ", subtitle: "Browse common questions")
                }
                .padding(.horizontal)
                
                Spacer()
            }
            .navigationTitle("Contact Support")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { showContactSupport = false }
                }
            }
        }
    }
    
    private func supportOption(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundColor(AppColors.primary)
                .frame(width: 36, height: 36)
                .background(AppColors.primary.opacity(0.1))
                .clipShape(Circle())
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.gray)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    // MARK: - Helpers
    private func statusBadge(status: String) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(statusColor(status))
                .frame(width: 6, height: 6)
            Text(status)
                .font(.caption)
                .fontWeight(.semibold)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(statusColor(status).opacity(0.12))
        .foregroundColor(statusColor(status))
        .cornerRadius(16)
    }
    
    private func statusColor(_ status: String) -> Color {
        switch status {
        case "PENDING": return .orange
        case "CONFIRMED": return .blue
        case "SHIPPED": return .purple
        case "DELIVERED": return .green
        case "CANCELED": return .red
        default: return .gray
        }
    }
    
    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
    
    private func estimatedDeliveryDate(createdAt: Int64) -> String {
        let orderDate = Date(timeIntervalSince1970: TimeInterval(createdAt / 1000))
        let deliveryDate = Calendar.current.date(byAdding: .day, value: 7, to: orderDate) ?? orderDate
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy"
        return formatter.string(from: deliveryDate)
    }
    
    private func estimatedDaysLeft(orderStatus: String, createdAt: Int64) -> Int {
        let orderDate = Date(timeIntervalSince1970: TimeInterval(createdAt / 1000))
        let deliveryDate = Calendar.current.date(byAdding: .day, value: 7, to: orderDate) ?? orderDate
        let remaining = Calendar.current.dateComponents([.day], from: Date(), to: deliveryDate).day ?? 0
        return max(remaining, 0)
    }
    
    private func deliveryProgress(status: String) -> CGFloat {
        switch status {
        case "PENDING": return 0.15
        case "CONFIRMED": return 0.40
        case "SHIPPED": return 0.70
        case "DELIVERED": return 1.0
        default: return 0
        }
    }
    
    // MARK: - Reorder
    private func reorderItems(order: Order) {
        isReordering = true
        let items = order.items
        
        // Add each item back to cart
        for item in items {
            cartViewModel.addToCart(productId: item.productId, quantity: Int(item.quantity))
        }
        
        // Show success after a short delay for the API calls
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            isReordering = false
            showReorderSuccess = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                withAnimation { showReorderSuccess = false }
            }
        }
    }
}
