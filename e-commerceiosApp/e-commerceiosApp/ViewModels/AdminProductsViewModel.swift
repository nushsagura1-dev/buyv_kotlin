import Foundation
import SwiftUI
import Combine
import Shared

@MainActor
class AdminProductsViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var searchQuery: String = ""
    @Published var selectedCategory: String?
    @Published var showFeaturedOnly: Bool = false
    
    @Published var allProducts: [Product] = []
    @Published var filteredProducts: [Product] = []
    @Published var categories: [String] = []
    
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    // Use Case
    private let getProductsUseCase: GetProductsUseCase
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    init() {
        self.getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
        setupObservers()
    }
    
    // MARK: - Setup Observers
    private func setupObservers() {
        // Search query
        $searchQuery
            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
        
        // Category
        $selectedCategory
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
        
        // Featured
        $showFeaturedOnly
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Methods
    func loadProducts() {
        isLoading = true
        errorMessage = nil
        
        getProductsUseCase.invoke { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    return
                }
                
                if let success = result as? ResultSuccess<AnyObject> {
                    if let products = success.data as? [Product] {
                        self.allProducts = products
                        self.extractCategories()
                        self.applyFilters()
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    private func extractCategories() {
        let uniqueCategories = Set(allProducts.map { $0.category })
        categories = Array(uniqueCategories).sorted()
    }
    
    func applyFilters() {
        filteredProducts = allProducts.filter { product in
            let matchesSearch = searchQuery.isEmpty ||
                product.name.localizedCaseInsensitiveContains(searchQuery) ||
                product.description_.localizedCaseInsensitiveContains(searchQuery)
            
            let matchesCategory = selectedCategory == nil ||
                product.category == selectedCategory
            
            let matchesFeatured = !showFeaturedOnly || product.featured
            
            return matchesSearch && matchesCategory && matchesFeatured
        }
    }
    
    func toggleFeatured(_ product: Product) {
        // Call admin API to toggle featured status
        let productId = String(product.id)
        let updates: [String: Any] = ["is_featured": !product.featured]
        
        Task {
            do {
                _ = try await AdminApiService.shared.updateProduct(productId: productId, updates: updates)
                // Reload products to get fresh state
                loadProducts()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func addProduct(_ product: Product) {
        // POST to backend to create the product
        let body: [String: Any] = [
            "name": product.name,
            "description": product.description_,
            "original_price": product.price,
            "selling_price": product.price,
            "category_name": product.category,
            "images": product.images,
            "is_featured": product.featured,
            "status": "active"
        ]
        
        Task {
            do {
                _ = try await AdminApiService.shared.createProduct(body: body)
                loadProducts()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func updateProduct(_ product: Product) {
        // Call admin API to update product — send ALL fields
        let productId = String(product.id)
        let updates: [String: Any] = [
            "name": product.name,
            "description": product.description_,
            "original_price": product.price,
            "selling_price": product.price,
            "category_name": product.category,
            "images": product.images,
            "is_featured": product.featured
        ]
        
        Task {
            do {
                _ = try await AdminApiService.shared.updateProduct(productId: productId, updates: updates)
                loadProducts()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func deleteProduct(_ product: Product) {
        // Call admin API to delete product
        let productId = String(product.id)
        
        Task {
            do {
                _ = try await AdminApiService.shared.deleteProduct(productId: productId)
                // Remove from local list immediately for responsive UI
                allProducts.removeAll { $0.id == product.id }
                applyFilters()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}
