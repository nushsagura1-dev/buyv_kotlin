import Foundation
import Shared

enum ProductSortOption: String, CaseIterable {
    case relevance = "Relevance"
    case recent = "Recent"
    case popular = "Popular"
    case priceAsc = "Price ↑"
    case priceDesc = "Price ↓"
    
    var icon: String {
        switch self {
        case .relevance: return "sparkles"
        case .recent: return "clock"
        case .popular: return "flame"
        case .priceAsc: return "arrow.up"
        case .priceDesc: return "arrow.down"
        }
    }
}

class ProductListViewModel: ObservableObject {
    @Published var products: [Product] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var sortOption: ProductSortOption = .relevance
    @Published var selectedCategoryName: String?
    @Published var priceRange: ClosedRange<Double>?
    @Published var searchQuery: String = ""
    
    private let getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
    private var allProducts: [Product] = []
    
    // MARK: - Computed Properties
    var isEmpty: Bool { filteredProducts.isEmpty && !isLoading }
    var totalCount: Int { filteredProducts.count }
    var hasProducts: Bool { !products.isEmpty }
    var hasError: Bool { errorMessage != nil }
    
    var filteredProducts: [Product] {
        var result = allProducts
        
        // Category filter
        if let category = selectedCategoryName {
            result = result.filter { $0.categoryName == category }
        }
        
        // Price range filter
        if let range = priceRange {
            result = result.filter { $0.price >= range.lowerBound && $0.price <= range.upperBound }
        }
        
        // Search filter
        if !searchQuery.isEmpty {
            let query = searchQuery.lowercased()
            result = result.filter {
                $0.name.lowercased().contains(query) ||
                $0.description_.lowercased().contains(query)
            }
        }
        
        return result
    }
    
    var availableCategories: [String] {
        let cats = Set(allProducts.compactMap { $0.categoryName.isEmpty ? nil : $0.categoryName })
        return cats.sorted()
    }
    
    var priceRangeBounds: ClosedRange<Double>? {
        guard !allProducts.isEmpty else { return nil }
        let prices = allProducts.map { $0.price }
        guard let minPrice = prices.min(), let maxPrice = prices.max() else { return nil }
        return minPrice...maxPrice
    }
    
    // MARK: - Load Products
    func loadProducts() {
        isLoading = allProducts.isEmpty
        errorMessage = nil
        
        // Use the appropriate method based on sort option
        switch sortOption {
        case .recent:
            getProductsUseCase.getRecentProducts(limit: 50) { result, error in
                self.handleResult(result: result, error: error)
            }
        case .popular:
            getProductsUseCase.getPopularProducts(limit: 50) { result, error in
                self.handleResult(result: result, error: error)
            }
        case .relevance, .priceAsc, .priceDesc:
            getProductsUseCase.invoke { result, error in
                self.handleResult(result: result, error: error)
            }
        }
    }
    
    private func handleResult(result: Any?, error: Error?) {
        DispatchQueue.main.async {
            self.isLoading = false
            self.isRefreshing = false
            
            if let result = result as? ResultSuccess<NSArray> {
                if var productList = result.data as? [Product] {
                    // Client-side sort for price options
                    switch self.sortOption {
                    case .priceAsc:
                        productList.sort { $0.price < $1.price }
                    case .priceDesc:
                        productList.sort { $0.price > $1.price }
                    default:
                        break
                    }
                    self.allProducts = productList
                    self.products = productList
                } else {
                    self.allProducts = []
                    self.products = []
                    AppLogger.warning("Failed to cast result to [Product]")
                }
            } else if let errorResult = result as? ResultError {
                self.errorMessage = errorResult.error.message
            } else if let error = error {
                self.errorMessage = error.localizedDescription
            }
        }
    }
    
    // MARK: - Sort & Filter
    func changeSortOption(_ option: ProductSortOption) {
        guard option != sortOption else { return }
        sortOption = option
        loadProducts()
    }
    
    func filterByCategory(_ categoryName: String?) {
        selectedCategoryName = categoryName
    }
    
    func setPriceRange(_ range: ClosedRange<Double>?) {
        priceRange = range
    }
    
    func searchProducts(_ query: String) {
        searchQuery = query
    }
    
    func clearFilters() {
        selectedCategoryName = nil
        priceRange = nil
        searchQuery = ""
    }
    
    // MARK: - Refresh
    func refresh() {
        isRefreshing = true
        loadProducts()
    }
    
    func retry() {
        errorMessage = nil
        loadProducts()
    }
    
    func clearError() {
        errorMessage = nil
    }
}
