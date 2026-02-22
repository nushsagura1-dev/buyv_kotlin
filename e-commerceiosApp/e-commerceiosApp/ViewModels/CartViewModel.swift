import Foundation
import Shared

// MARK: - Cart State
enum CartState {
    case idle
    case loading
    case loaded
    case empty
    case error(String)
}

class CartViewModel: ObservableObject {
    @Published var cart: Cart?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isCheckingOut = false
    @Published var promoCode: String = ""
    @Published var promoApplied = false
    @Published var promoDiscount: Double = 0
    @Published var cartState: CartState = .idle
    @Published var isAddingToCart = false
    @Published var addedToCartMessage: String?
    @Published var lastRemovedItem: CartItem?
    
    let shippingCost: Double = 8.0
    let taxRate: Double = 0.0 // Tax-free for now, configurable
    let freeShippingThreshold: Double = 100.0
    
    // MARK: - Computed Properties
    var subtotal: Double { cart?.subtotal ?? 0 }
    var tax: Double { subtotal * taxRate }
    var effectiveShipping: Double { subtotal >= freeShippingThreshold ? 0 : shippingCost }
    var total: Double { subtotal + effectiveShipping + tax - promoDiscount }
    var itemCount: Int { Int(cart?.items.count ?? 0) }
    var isLoggedIn: Bool { SessionManager.shared.authToken != nil }
    var isEmpty: Bool { cart == nil || itemCount == 0 }
    var hasFreeShipping: Bool { subtotal >= freeShippingThreshold }
    var amountToFreeShipping: Double { max(0, freeShippingThreshold - subtotal) }
    
    var savings: Double {
        promoDiscount + (hasFreeShipping ? shippingCost : 0)
    }
    
    // MARK: - Dependencies
    private let getCartUseCase = DependencyWrapper.shared.getCartUseCase
    private let addToCartUseCase = DependencyWrapper.shared.addToCartUseCase
    private let removeFromCartUseCase = DependencyWrapper.shared.removeFromCartUseCase
    private let updateCartItemUseCase = DependencyWrapper.shared.updateCartItemUseCase
    private let clearCartUseCase = DependencyWrapper.shared.clearCartUseCase
    
    // MARK: - Load Cart
    func loadCart() {
        isLoading = true
        errorMessage = nil
        cartState = .loading
        
        let userId = SessionManager.shared.currentUserId
        
        getCartUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if let result = result as? ResultSuccess<Cart> {
                    self.cart = result.data
                    self.cartState = (result.data?.items.count ?? 0) > 0 ? .loaded : .empty
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                    self.cartState = .error(errorResult.error.message ?? "Unknown error")
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                    self.cartState = .error(error.localizedDescription)
                }
            }
        }
    }
    
    func refreshCart() {
        loadCart()
    }
    
    // MARK: - Add to Cart
    func addToCart(productId: String, quantity: Int = 1) {
        let userId = SessionManager.shared.currentUserId
        isAddingToCart = true
        addedToCartMessage = nil
        
        addToCartUseCase.invoke(userId: userId, productId: productId, quantity: Int32(quantity)) { result, error in
            DispatchQueue.main.async {
                self.isAddingToCart = false
                
                if result is ResultSuccess<KotlinUnit> {
                    self.addedToCartMessage = "Added to cart!"
                    self.loadCart() // Refresh cart
                    
                    // Auto-dismiss message after 2 seconds
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        self.addedToCartMessage = nil
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // MARK: - Check if item is in cart
    func isInCart(productId: String) -> Bool {
        guard let items = cart?.items as? [CartItem] else { return false }
        return items.contains { $0.productId == productId }
    }
    
    func quantityInCart(productId: String) -> Int {
        guard let items = cart?.items as? [CartItem] else { return 0 }
        return Int(items.first { $0.productId == productId }?.quantity ?? 0)
    }
    
    // MARK: - Remove Item
    func removeItem(itemId: String) {
        let userId = SessionManager.shared.currentUserId
        isLoading = true
        
        // Save item for undo
        if let items = cart?.items as? [CartItem] {
            lastRemovedItem = items.first { $0.id == itemId }
        }
        
        removeFromCartUseCase.invoke(userId: userId, itemId: itemId) { result, error in
            DispatchQueue.main.async {
                if let _ = result as? ResultSuccess<KotlinUnit> {
                    self.loadCart()
                } else if let errorResult = result as? ResultError {
                    self.isLoading = false
                    self.errorMessage = errorResult.error.message
                    self.lastRemovedItem = nil
                } else if let error = error {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                    self.lastRemovedItem = nil
                }
            }
        }
    }
    
    // MARK: - Update Quantity
    func updateQuantity(itemId: String, quantity: Int) {
        if quantity <= 0 {
            removeItem(itemId: itemId)
            return
        }
        
        let userId = SessionManager.shared.currentUserId
        isLoading = true
        
        updateCartItemUseCase.invoke(userId: userId, itemId: itemId, quantity: Int32(quantity)) { result, error in
            DispatchQueue.main.async {
                if let _ = result as? ResultSuccess<KotlinUnit> {
                    self.loadCart()
                } else if let errorResult = result as? ResultError {
                    self.isLoading = false
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // MARK: - Clear Cart
    func clearCart() {
        let userId = SessionManager.shared.currentUserId
        
        clearCartUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                if let _ = result as? ResultSuccess<KotlinUnit> {
                    self.cart = nil
                    self.cartState = .empty
                    self.removePromoCode()
                }
            }
        }
    }
    
    // MARK: - Error Handling
    func clearError() {
        errorMessage = nil
    }
    
    // MARK: - Promo Code
    func applyPromoCode() {
        guard !promoCode.isEmpty else { return }
        
        let code = promoCode.uppercased()
        if code == "BUYV10" {
            promoDiscount = subtotal * 0.10
            promoApplied = true
        } else if code == "BUYV20" {
            promoDiscount = subtotal * 0.20
            promoApplied = true
        } else if code == "FREESHIP" {
            promoDiscount = effectiveShipping
            promoApplied = true
        } else {
            promoApplied = false
            promoDiscount = 0
            errorMessage = "Invalid promo code"
        }
    }
    
    func removePromoCode() {
        promoCode = ""
        promoApplied = false
        promoDiscount = 0
    }
}
