import SwiftUI
import Shared

struct CartView: View {
    @StateObject private var viewModel = CartViewModel()
    @State private var showCheckout = false
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                if !viewModel.isLoggedIn {
                    loginPrompt()
                } else if viewModel.isLoading && viewModel.cart == nil {
                    ProgressView("Loading cart...")
                } else if let cart = viewModel.cart, !cart.items.isEmpty {
                    cartContent(cart: cart)
                } else {
                    emptyCartView()
                }
            }
            .navigationTitle("My Cart")
            .toolbar {
                if let cart = viewModel.cart, !cart.items.isEmpty {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(action: { viewModel.clearCart() }) {
                            Text("Clear All")
                                .font(.subheadline)
                                .foregroundColor(.red)
                        }
                    }
                }
            }
            .onAppear {
                if viewModel.isLoggedIn { viewModel.loadCart() }
            }
            .refreshable {
                viewModel.refreshCart()
            }
            .alert(item: Binding<ErrorWrapper?>(
                get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
                set: { _ in viewModel.clearError() }
            )) { errorWrapper in
                Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
            }
        }
    }
    
    // MARK: - Login Prompt
    @ViewBuilder
    private func loginPrompt() -> some View {
        VStack(spacing: 20) {
            Image(systemName: "person.crop.circle.badge.exclamationmark")
                .font(.system(size: 60))
                .foregroundColor(AppColors.primary)
            
            Text("Sign in to view your cart")
                .font(.title3)
                .fontWeight(.semibold)
            
            Text("You need to be logged in to add items and manage your shopping cart.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            NavigationLink(destination: LoginView()) {
                Text("Sign In")
                    .fontWeight(.bold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(
                        LinearGradient(colors: [AppColors.primary, AppColors.primary.opacity(0.8)],
                                       startPoint: .leading, endPoint: .trailing)
                    )
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
        }
    }
    
    // MARK: - Cart Content
    @ViewBuilder
    private func cartContent(cart: Cart) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(spacing: 12) {
                    // Cart items
                    ForEach(cart.items, id: \.id) { item in
                        CartItemRow(item: item, viewModel: viewModel)
                    }
                    
                    // Promo code section
                    promoCodeSection()
                }
                .padding()
            }
            
            // Bottom summary + checkout
            bottomSummary()
        }
    }
    
    // MARK: - Promo Code Section
    @ViewBuilder
    private func promoCodeSection() -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Promo Code")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)
            
            if viewModel.promoApplied {
                HStack {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundColor(.green)
                    Text(viewModel.promoCode.uppercased())
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text("-$\(String(format: "%.2f", viewModel.promoDiscount))")
                        .font(.subheadline)
                        .foregroundColor(.green)
                    Spacer()
                    Button(action: { viewModel.removePromoCode() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color.green.opacity(0.1))
                .cornerRadius(10)
            } else {
                HStack(spacing: 8) {
                    Image(systemName: "tag.fill")
                        .foregroundColor(AppColors.primary)
                    
                    TextField("Enter promo code", text: $viewModel.promoCode)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .autocapitalization(.allCharacters)
                    
                    Button(action: { viewModel.applyPromoCode() }) {
                        Text("Apply")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(AppColors.primary)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .disabled(viewModel.promoCode.isEmpty)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 2)
    }
    
    // MARK: - Bottom Summary
    @ViewBuilder
    private func bottomSummary() -> some View {
        VStack(spacing: 8) {
            Divider()
            
            VStack(spacing: 6) {
                // Subtotal
                HStack {
                    Text("Subtotal (\(viewModel.itemCount) items)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("$\(String(format: "%.2f", viewModel.subtotal))")
                        .font(.subheadline)
                }
                
                // Shipping
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "shippingbox.fill")
                            .font(.caption)
                        Text("Shipping")
                            .font(.subheadline)
                    }
                    .foregroundColor(.secondary)
                    Spacer()
                    Text("$\(String(format: "%.2f", viewModel.shippingCost))")
                        .font(.subheadline)
                }
                
                // Tax (show only if > 0)
                if viewModel.tax > 0 {
                    HStack {
                        Text("Tax")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text("$\(String(format: "%.2f", viewModel.tax))")
                            .font(.subheadline)
                    }
                }
                
                // Promo discount
                if viewModel.promoApplied {
                    HStack {
                        HStack(spacing: 4) {
                            Image(systemName: "tag.fill")
                                .font(.caption)
                            Text("Promo Discount")
                                .font(.subheadline)
                        }
                        .foregroundColor(.green)
                        Spacer()
                        Text("-$\(String(format: "%.2f", viewModel.promoDiscount))")
                            .font(.subheadline)
                            .foregroundColor(.green)
                    }
                }
                
                Divider()
                
                // Total
                HStack {
                    Text("Total")
                        .font(.title3)
                        .fontWeight(.bold)
                    Spacer()
                    Text("$\(String(format: "%.2f", viewModel.total))")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                }
            }
            .padding(.horizontal)
            .padding(.top, 8)
            
            // Checkout button
            NavigationLink(destination: CheckoutView(), isActive: $showCheckout) {
                EmptyView()
            }
            
            Button(action: { showCheckout = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                        .font(.subheadline)
                    Text("Proceed to Checkout")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(
                    LinearGradient(colors: [AppColors.primary, AppColors.primary.opacity(0.85)],
                                   startPoint: .leading, endPoint: .trailing)
                )
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
        .background(Color.white)
        .shadow(color: Color.black.opacity(0.1), radius: 4, y: -2)
    }
    
    // MARK: - Empty Cart
    @ViewBuilder
    private func emptyCartView() -> some View {
        VStack(spacing: 20) {
            Image(systemName: "cart")
                .font(.system(size: 70))
                .foregroundColor(.gray.opacity(0.5))
            
            Text("Your cart is empty")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text("Browse our products and add items to your cart to get started.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Button(action: { dismiss() }) {
                HStack {
                    Image(systemName: "bag.fill")
                    Text("Shop Now")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(AppColors.primary)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .padding(.horizontal, 40)
        }
    }
}

// MARK: - Cart Item Row
struct CartItemRow: View {
    let item: CartItem
    @ObservedObject var viewModel: CartViewModel
    
    var body: some View {
        HStack(spacing: 12) {
            // Product image
            AsyncImage(url: URL(string: item.productImage)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    Image(systemName: "photo")
                        .font(.title2)
                        .foregroundColor(.gray)
                default:
                    ProgressView()
                }
            }
            .frame(width: 85, height: 85)
            .background(Color.gray.opacity(0.1))
            .cornerRadius(10)
            .clipped()
            
            VStack(alignment: .leading, spacing: 4) {
                Text(item.productName)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(2)
                
                // Size & Color attributes
                if let size = item.size, !size.isEmpty, let color = item.color, !color.isEmpty {
                    HStack(spacing: 6) {
                        Text("Size: \(size)")
                            .font(.caption)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(4)
                        Text("Color: \(color)")
                            .font(.caption)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(4)
                    }
                    .foregroundColor(.secondary)
                } else if let size = item.size, !size.isEmpty {
                    Text("Size: \(size)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(4)
                } else if let color = item.color, !color.isEmpty {
                    Text("Color: \(color)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(4)
                }
                
                Text("$\(String(format: "%.2f", item.price))")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.primary)
                
                HStack {
                    // Quantity selector
                    HStack(spacing: 0) {
                        Button(action: {
                            if item.quantity > 1 {
                                viewModel.updateQuantity(itemId: item.id, quantity: Int(item.quantity) - 1)
                            } else {
                                viewModel.removeItem(itemId: item.id)
                            }
                        }) {
                            Image(systemName: "minus")
                                .font(.caption)
                                .frame(width: 28, height: 28)
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(6)
                        }
                        
                        Text("\(item.quantity)")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .frame(minWidth: 32)
                        
                        Button(action: {
                            viewModel.updateQuantity(itemId: item.id, quantity: Int(item.quantity) + 1)
                        }) {
                            Image(systemName: "plus")
                                .font(.caption)
                                .frame(width: 28, height: 28)
                                .background(AppColors.primary.opacity(0.15))
                                .cornerRadius(6)
                        }
                    }
                    .foregroundColor(.primary)
                    
                    Spacer()
                    
                    // Line total
                    Text("$\(String(format: "%.2f", item.price * Double(item.quantity)))")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    
                    // Delete button
                    Button(action: { viewModel.removeItem(itemId: item.id) }) {
                        Image(systemName: "trash.fill")
                            .font(.caption)
                            .foregroundColor(.red)
                            .frame(width: 28, height: 28)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(6)
                    }
                    .padding(.leading, 4)
                }
                .padding(.top, 2)
            }
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 2)
    }
}
