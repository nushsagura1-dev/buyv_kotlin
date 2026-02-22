import Foundation
import SwiftUI
import Shared

class ProductDetailViewModel: ObservableObject {
    @Published var product: Product?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var addedToCart = false
    @Published var userPosts: [Product] = []
    @Published var promotionState: PromotionState = .idle
    
    enum PromotionState {
        case idle, loading, success, error(String)
    }
    
    private let getProductDetailsUseCase = DependencyWrapper.shared.getProductDetailsUseCase
    private let addToCartUseCase = DependencyWrapper.shared.addToCartUseCase
    private let createPromotionUseCase = DependencyWrapper.shared.createPromotionUseCase
    private let getUserPostsUseCase = DependencyWrapper.shared.getUserPostsUseCase
    
    private var currentProductId: String?
    
    var isProductAvailable: Bool {
        guard let product = product else { return false }
        return product.quantity > 0
    }
    
    var estimatedCommission: Double {
        guard let product = product else { return 0 }
        return product.price * product.commissionRate / 100
    }
    
    func loadProduct(id: String) {
        // Skip reload if same product
        if id == currentProductId && product != nil { return }
        
        currentProductId = id
        isLoading = true
        errorMessage = nil
        
        getProductDetailsUseCase.invoke(productId: id) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if let result = result as? ResultSuccess<Product> {
                    self.product = result.data
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    func refresh() {
        guard let id = currentProductId else { return }
        currentProductId = nil // Force reload
        loadProduct(id: id)
    }
    
    func addToCart(quantity: Int = 1, size: String? = nil, color: String? = nil) {
        guard let product = product else { return }
        
        let cartItem = CartItem(
            id: UUID().uuidString,
            productId: product.id,
            productName: product.name,
            productImage: product.imageUrl,
            price: product.price,
            quantity: Int32(quantity),
            size: size,
            color: color,
            attributes: [:],
            addedAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        isLoading = true
        let userId = SessionManager.shared.currentUserId
        
        addToCartUseCase.invoke(userId: userId, item: cartItem) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                if let _ = result as? ResultSuccess<KotlinUnit> {
                    self.addedToCart = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        self.addedToCart = false
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    func loadUserPosts() {
        guard let userId = SessionManager.shared.currentUser?.uid else { return }
        
        getUserPostsUseCase.invoke(userId: userId) { result, error in
            DispatchQueue.main.async {
                if let result = result as? ResultSuccess<NSArray> {
                    if let posts = result.data as? [Product] {
                        self.userPosts = posts
                    }
                }
            }
        }
    }
    
    func createPromotion(postId: String) {
        guard let product = product else { return }
        
        promotionState = .loading
        
        createPromotionUseCase.invoke(
            productId: String(product.id),
            postId: postId
        ) { result, error in
            DispatchQueue.main.async {
                if let _ = result as? ResultSuccess<KotlinUnit> {
                    self.promotionState = .success
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        self.promotionState = .idle
                    }
                } else if let errorResult = result as? ResultError {
                    self.promotionState = .error(errorResult.error.message)
                } else if let error = error {
                    self.promotionState = .error(error.localizedDescription)
                }
            }
        }
    }
}
