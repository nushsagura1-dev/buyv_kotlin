import Foundation
import Shared

class OrderViewModel: ObservableObject {
    @Published var orders: [Order] = []
    @Published var selectedOrder: Order? = nil
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var cartTotal: Double = 0.0
    
    // Dependencies
    private let getOrdersByUserUseCase = DependencyWrapper.shared.getOrdersByUserUseCase
    private let getOrderDetailsUseCase = DependencyWrapper.shared.getOrderDetailsUseCase
    private let createOrderUseCase = DependencyWrapper.shared.createOrderUseCase
    private let cancelOrderUseCase = DependencyWrapper.shared.cancelOrderUseCase
    
    // Helper UseCases for Checkout
    private let getCartUseCase = DependencyWrapper.shared.getCartUseCase
    private let clearCartUseCase = DependencyWrapper.shared.clearCartUseCase
    
    // Charger toutes les commandes de l'utilisateur
    func loadUserOrders() {
        let userId = SessionManager.shared.currentUserId
        isLoading = true
        errorMessage = nil
        
        getOrdersByUserUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if let successResult = result as? ResultSuccess<NSArray> {
                     if let ordersList = successResult.data as? [Order] {
                         self.orders = ordersList
                     } else {
                         self.errorMessage = "Failed to parse orders"
                     }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // Charger les détails d'une commande
    func loadOrderDetails(orderId: String) {
        isLoading = true
        errorMessage = nil
        
        getOrderDetailsUseCase.invoke(orderId: orderId) { result, error in
             DispatchQueue.main.async {
                 self.isLoading = false
                 if let successResult = result as? ResultSuccess<Order> {
                     self.selectedOrder = successResult.data
                 } else if let errorResult = result as? ResultError {
                     self.errorMessage = errorResult.error.message
                 } else if let error = error {
                     self.errorMessage = error.localizedDescription
                 }
             }
        }
    }
    
    /// Pre-loads the cart total for Stripe payment amount calculation
    func loadCartTotal() {
        let userId = SessionManager.shared.currentUserId
        getCartUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                if let successResult = result as? ResultSuccess<Cart> {
                    self.cartTotal = successResult.data.subtotal
                }
            }
        }
    }
    
    // Checkout: Convert Cart to Order and Create
    func checkout(shippingAddress: Address, completion: @escaping (Bool) -> Void) {
        let userId = SessionManager.shared.currentUserId
        isLoading = true
        
        // 1. Get current cart freshly
        getCartUseCase.invoke(userId: userId) { result, error in
             DispatchQueue.main.async {
                 if let successResult = result as? ResultSuccess<Cart> {
                     let cart = successResult.data
                     self.cartTotal = cart.subtotal
                     
                     if cart.items.isEmpty {
                         self.errorMessage = "Cart is empty"
                         self.isLoading = false
                         completion(false)
                         return
                     }
                     
                     // 2. Map CartItems to OrderItems
                     var orderItems: [OrderItem] = []
                     for cartItem in cart.items {
                         let orderItem = OrderItem(
                             productId: cartItem.productId,
                             productName: cartItem.productName,
                             productImage: cartItem.productImage,
                             price: cartItem.price,
                             quantity: cartItem.quantity,
                             size: cartItem.size,
                             color: cartItem.color
                         )
                         orderItems.append(orderItem)
                     }
                     
                     // 3. Create Order Object
                     let newOrder = Order(
                         id: UUID().uuidString,
                         userId: userId,
                         items: orderItems,
                         total: cart.subtotal, // Assuming taxes/shipping handled elsewhere or simply subtotal for MVP
                         status: OrderStatus.pending,
                         shippingAddress: shippingAddress,
                         createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                         updatedAt: Int64(Date().timeIntervalSince1970 * 1000)
                     )
                     
                     // 4. Submit Order
                     self.createOrderUseCase.invoke(order: newOrder) { createResult, createError in
                         DispatchQueue.main.async {
                             if createResult is ResultSuccess<Order> {
                                 // 5. Clear Cart on success
                                 self.clearCartUseCase.invoke(userId: userId) { _, _ in
                                     DispatchQueue.main.async {
                                         self.isLoading = false
                                         completion(true)
                                     }
                                 }
                             } else if let errorResult = createResult as? ResultError {
                                 self.errorMessage = errorResult.error.message
                                 self.isLoading = false
                                 completion(false)
                             } else if let error = createError {
                                 self.errorMessage = error.localizedDescription
                                 self.isLoading = false
                                 completion(false)
                             }
                         }
                     }
                     
                 } else {
                     self.errorMessage = "Failed to load cart for checkout"
                     self.isLoading = false
                     completion(false)
                 }
             }
        }
    }
    
    // Annuler une commande
    func cancelOrder(orderId: String, reason: String) {
        isLoading = true
        
        cancelOrderUseCase.invoke(orderId: orderId, reason: reason) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if result is ResultSuccess<KotlinUnit> {
                    // Refresh details
                    self.loadOrderDetails(orderId: orderId)
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}
