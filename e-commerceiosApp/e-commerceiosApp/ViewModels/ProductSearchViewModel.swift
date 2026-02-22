import Foundation
import SwiftUI
import Shared

@MainActor
class ProductSearchViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var searchQuery: String = ""
    @Published var selectedCategory: String?
    @Published var minPrice: Double?
    @Published var maxPrice: Double?
    @Published var showFilters: Bool = false
    
    @Published var allProducts: [Product] = []
    @Published var filteredProducts: [Product] = []
    @Published var categories: [String] = []
    
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    // Use Cases
    private let getProductsUseCase: GetProductsUseCase
    private let getCategoriesUseCase: GetCategoriesUseCase
    
    // MARK: - Initialization
    init() {
        self.getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
        self.getCategoriesUseCase = DependencyWrapper.shared.getCategoriesUseCase
    }
    
    // MARK: - Computed Properties
    var hasActiveFilters: Bool {
        selectedCategory != nil || minPrice != nil || maxPrice != nil
    }
    
    var showEmptyState: Bool {
        !searchQuery.isEmpty || selectedCategory != nil || minPrice != nil || maxPrice != nil
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
            // Search query filter
            let matchesSearch = searchQuery.isEmpty ||
                product.name.localizedCaseInsensitiveContains(searchQuery) ||
                product.description_.localizedCaseInsensitiveContains(searchQuery)
            
            // Category filter
            let matchesCategory = selectedCategory == nil ||
                product.category == selectedCategory
            
            // Price filter
            let matchesPriceMin = minPrice == nil ||
                product.price >= (minPrice ?? 0)
            
            let matchesPriceMax = maxPrice == nil ||
                product.price <= (maxPrice ?? .infinity)
            
            return matchesSearch && matchesCategory && matchesPriceMin && matchesPriceMax
        }
    }
    
    func toggleCategory(_ category: String) {
        if selectedCategory == category {
            selectedCategory = nil
        } else {
            selectedCategory = category
        }
        applyFilters()
    }
    
    func clearFilters() {
        selectedCategory = nil
        minPrice = nil
        maxPrice = nil
        searchQuery = ""
        applyFilters()
    }
    
    // MARK: - Observers
    func observeChanges() {
        // Apply filters when search query changes
        $searchQuery
            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
        
        // Apply filters when price changes
        $minPrice
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
        
        $maxPrice
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
    }
    
    private var cancellables = Set<AnyCancellable>()
}

import Combine
