import SwiftUI
import Shared

struct CheckoutView: View {
    @StateObject private var viewModel = OrderViewModel()
    @StateObject private var cartViewModel = CartViewModel()
    @StateObject private var paymentService = StripePaymentService.shared
    @Environment(\.presentationMode) var presentationMode
    
    // Address Form State
    @State private var name = ""
    @State private var street = ""
    @State private var city = ""
    @State private var state = ""
    @State private var zipCode = ""
    @State private var country = ""
    @State private var phone = ""
    
    @State private var showSuccess = false
    @State private var paymentStatus: String = ""
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 16) {
                    
                    // Test mode notice
                    testModeNotice()
                    
                    // Order Summary
                    orderSummarySection()
                    
                    // Shipping Address Form
                    shippingAddressSection()
                    
                    // Payment Method — Stripe Integration
                    paymentMethodSection()
                    
                    // Price Breakdown
                    priceBreakdown()
                    
                    // Place Order Button
                    placeOrderButton()
                }
                .padding(.vertical)
            }
            .navigationTitle("Checkout")
            
            // Success Navigation
            NavigationLink(destination: OrderSuccessView(), isActive: $showSuccess) {
                EmptyView()
            }
        }
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
            Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
        }
        .onAppear {
            viewModel.loadCartTotal()
            cartViewModel.loadCart()
        }
    }
    
    // MARK: - Test Mode Notice
    @ViewBuilder
    private func testModeNotice() -> some View {
        HStack(spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.orange)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Test Mode")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
                Text("Use card: 4242 4242 4242 4242")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("Any future date, any 3-digit CVC")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding()
        .background(Color.orange.opacity(0.1))
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.orange.opacity(0.3), lineWidth: 1)
        )
        .padding(.horizontal)
    }
    
    // MARK: - Order Summary
    @ViewBuilder
    private func orderSummarySection() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "bag.fill")
                    .foregroundColor(AppColors.primary)
                Text("Order Summary")
                    .font(.headline)
                Spacer()
                Text("\(cartViewModel.itemCount) items")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            if let cart = cartViewModel.cart {
                Divider()
                
                ForEach(cart.items, id: \.id) { item in
                    HStack(spacing: 10) {
                        AsyncImage(url: URL(string: item.productImage)) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().aspectRatio(contentMode: .fill)
                            default:
                                Color.gray.opacity(0.15)
                            }
                        }
                        .frame(width: 45, height: 45)
                        .cornerRadius(6)
                        .clipped()
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.productName)
                                .font(.subheadline)
                                .lineLimit(1)
                            
                            HStack(spacing: 4) {
                                Text("Qty: \(item.quantity)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                if let size = item.size, !size.isEmpty {
                                    Text("• \(size)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                if let color = item.color, !color.isEmpty {
                                    Text("• \(color)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        
                        Spacer()
                        
                        Text("$\(String(format: "%.2f", item.price * Double(item.quantity)))")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }
                }
            } else if cartViewModel.isLoading {
                HStack {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
                .padding(.vertical, 8)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: Color.black.opacity(0.05), radius: 2)
        .padding(.horizontal)
    }
    
    // MARK: - Shipping Address
    @ViewBuilder
    private func shippingAddressSection() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "shippingbox.fill")
                    .foregroundColor(AppColors.primary)
                Text("Shipping Address")
                    .font(.headline)
            }
            
            Group {
                TextField("Full Name", text: $name)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                
                TextField("Street Address", text: $street)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                
                HStack {
                    TextField("City", text: $city)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                    TextField("State", text: $state)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                
                HStack {
                    TextField("Zip Code", text: $zipCode)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.numberPad)
                    TextField("Country", text: $country)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                
                TextField("Phone Number", text: $phone)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.phonePad)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: Color.black.opacity(0.05), radius: 2)
        .padding(.horizontal)
    }
    
    // MARK: - Payment Method
    @ViewBuilder
    private func paymentMethodSection() -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: "creditcard.fill")
                    .foregroundColor(AppColors.primary)
                Text("Payment Method")
                    .font(.headline)
            }
            
            HStack {
                Image(systemName: "creditcard.fill")
                    .foregroundColor(AppColors.primary)
                Text("Pay with Stripe")
                    .fontWeight(.medium)
                Spacer()
                Image(systemName: "lock.fill")
                    .font(.caption)
                    .foregroundColor(.green)
                Text("Secure")
                    .font(.caption)
                    .foregroundColor(.green)
            }
            .padding()
            .background(Color.gray.opacity(0.08))
            .cornerRadius(8)
            
            // Payment status message
            if !paymentStatus.isEmpty {
                HStack {
                    Image(systemName: paymentStatus.contains("✅") ? "checkmark.circle.fill" : "info.circle.fill")
                        .foregroundColor(paymentStatus.contains("✅") ? .green : .orange)
                    Text(paymentStatus)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 4)
            }
            
            // Stripe payment error
            if let error = paymentService.paymentError {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.red)
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }
                .padding(.horizontal, 4)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: Color.black.opacity(0.05), radius: 2)
        .padding(.horizontal)
    }
    
    // MARK: - Price Breakdown
    @ViewBuilder
    private func priceBreakdown() -> some View {
        VStack(spacing: 8) {
            HStack {
                Text("Subtotal")
                    .foregroundColor(.secondary)
                Spacer()
                Text("$\(String(format: "%.2f", cartViewModel.subtotal))")
            }
            .font(.subheadline)
            
            HStack {
                HStack(spacing: 4) {
                    Image(systemName: "shippingbox")
                        .font(.caption)
                    Text("Shipping")
                }
                .foregroundColor(.secondary)
                Spacer()
                Text("$\(String(format: "%.2f", cartViewModel.shippingCost))")
            }
            .font(.subheadline)
            
            if cartViewModel.tax > 0 {
                HStack {
                    Text("Tax")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("$\(String(format: "%.2f", cartViewModel.tax))")
                }
                .font(.subheadline)
            }
            
            if cartViewModel.promoApplied {
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "tag.fill")
                            .font(.caption)
                        Text("Promo Discount")
                    }
                    .foregroundColor(.green)
                    Spacer()
                    Text("-$\(String(format: "%.2f", cartViewModel.promoDiscount))")
                        .foregroundColor(.green)
                }
                .font(.subheadline)
            }
            
            Divider()
            
            HStack {
                Text("Total")
                    .font(.title3)
                    .fontWeight(.bold)
                Spacer()
                Text("$\(String(format: "%.2f", cartViewModel.total))")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.primary)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: Color.black.opacity(0.05), radius: 2)
        .padding(.horizontal)
    }
    
    // MARK: - Place Order Button
    @ViewBuilder
    private func placeOrderButton() -> some View {
        Button(action: placeOrder) {
            if viewModel.isLoading || paymentService.isProcessing {
                HStack(spacing: 8) {
                    ProgressView()
                        .tint(.white)
                    Text(paymentService.isProcessing ? "Processing Payment..." : "Placing Order...")
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity)
            } else {
                HStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                    Text("Pay $\(String(format: "%.2f", cartViewModel.total)) & Place Order")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(
            isFormValid
                ? LinearGradient(colors: [AppColors.primary, AppColors.primary.opacity(0.85)],
                                 startPoint: .leading, endPoint: .trailing)
                : LinearGradient(colors: [Color.gray, Color.gray],
                                 startPoint: .leading, endPoint: .trailing)
        )
        .foregroundColor(.white)
        .cornerRadius(12)
        .padding(.horizontal)
        .disabled(!isFormValid || viewModel.isLoading || paymentService.isProcessing)
    }
    
    // MARK: - Form Validation
    var isFormValid: Bool {
        !name.isEmpty && !street.isEmpty && !city.isEmpty && !zipCode.isEmpty && !phone.isEmpty
    }
    
    // MARK: - Place Order Action
    func placeOrder() {
        let address = Address(
            id: UUID().uuidString,
            name: name,
            street: street,
            city: city,
            state: state,
            zipCode: zipCode,
            country: country,
            phone: phone,
            isDefault: false
        )
        
        Task {
            paymentStatus = "Initiating payment..."
            
            let amountInCents = cartViewModel.total > 0
                ? Int(cartViewModel.total * 100)
                : 100
            
            let result = await paymentService.processPayment(amountInCents: amountInCents)
            
            switch result {
            case .completed:
                paymentStatus = "✅ Payment successful"
                viewModel.checkout(shippingAddress: address) { success in
                    if success {
                        showSuccess = true
                    }
                }
                
            case .cancelled:
                paymentStatus = "Payment cancelled"
                
            case .failed(let error):
                paymentStatus = "Payment failed: \(error)"
            }
        }
    }
}

// MARK: - Order Success View
struct OrderSuccessView: View {
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Success animation
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.1))
                    .frame(width: 140, height: 140)
                
                Circle()
                    .fill(Color.green.opacity(0.2))
                    .frame(width: 110, height: 110)
                
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.green)
            }
            
            VStack(spacing: 10) {
                Text("Order Placed!")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Your order has been successfully placed.\nYou'll receive a confirmation soon.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal)
            
            // Order info card
            VStack(spacing: 8) {
                HStack {
                    Image(systemName: "clock.fill")
                        .foregroundColor(AppColors.primary)
                    Text("Estimated delivery: 3-5 business days")
                        .font(.subheadline)
                }
                
                HStack {
                    Image(systemName: "envelope.fill")
                        .foregroundColor(AppColors.primary)
                    Text("Order confirmation sent to your email")
                        .font(.subheadline)
                }
            }
            .padding()
            .background(AppColors.primary.opacity(0.08))
            .cornerRadius(12)
            .padding(.horizontal, 24)
            
            Spacer()
            
            VStack(spacing: 12) {
                NavigationLink(destination: OrderListView()) {
                    HStack {
                        Image(systemName: "list.bullet.rectangle")
                        Text("View My Orders")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                
                Button(action: {
                    // Pop to root
                }) {
                    Text("Continue Shopping")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.gray.opacity(0.1))
                        .foregroundColor(.primary)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
        .navigationBarBackButtonHidden(true)
    }
}
