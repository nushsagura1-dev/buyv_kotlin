import Foundation
import Shared

class AllProductsViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var products: [Product] = []
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var errorMessage: String?
    @Published var searchQuery: String = "" {
        didSet { applyFilters() }
    }
    @Published var selectedCategory: String = "All" {
        didSet { applyFilters() }
    }
    @Published var sortOption: SortOption = .newest {
        didSet { applyFilters() }
    }
    @Published var minPrice: Double? = nil {
        didSet { applyFilters() }
    }
    @Published var maxPrice: Double? = nil {
        didSet { applyFilters() }
    }
    @Published var filteredProducts: [Product] = []
    
    // MARK: - Computed Properties
    var categories: [String] {
        var cats = Set(products.compactMap { $0.categoryName.isEmpty ? nil : $0.categoryName })
        return ["All"] + cats.sorted()
    }
    
    var isEmpty: Bool { filteredProducts.isEmpty && !isLoading }
    var totalCount: Int { products.count }
    var filteredCount: Int { filteredProducts.count }
    var hasProducts: Bool { !products.isEmpty }
    var hasError: Bool { errorMessage != nil }
    var hasActiveFilters: Bool {
        selectedCategory != "All" || sortOption != .newest ||
        minPrice != nil || maxPrice != nil || !searchQuery.isEmpty
    }
    
    var priceRange: ClosedRange<Double> {
        let prices = products.compactMap { Double($0.price) }
        guard let min = prices.min(), let max = prices.max(), min < max else {
            return 0...1000
        }
        return min...max
    }
    
    // MARK: - Sort Options
    enum SortOption: String, CaseIterable {
        case newest = "newest"
        case priceLow = "price_low"
        case priceHigh = "price_high"
        case rating = "rating"
        case name = "name"
        
        var label: String {
            switch self {
            case .newest: return "Newest"
            case .priceLow: return "Price: Low"
            case .priceHigh: return "Price: High"
            case .rating: return "Top Rated"
            case .name: return "Name A-Z"
            }
        }
        
        var icon: String {
            switch self {
            case .newest: return "clock"
            case .priceLow: return "arrow.down"
            case .priceHigh: return "arrow.up"
            case .rating: return "star.fill"
            case .name: return "textformat.abc"
            }
        }
    }
    
    private let getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
    
    // MARK: - Init
    init() {
        loadProducts()
    }
    
    // MARK: - Data Loading
    func loadProducts() {
        isLoading = true
        errorMessage = nil
        
        getProductsUseCase.getAllProducts { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                self.isRefreshing = false
                
                if let success = result as? ResultSuccess<NSArray> {
                    if let products = success.data as? [Product] {
                        self.products = products
                        self.applyFilters()
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    // MARK: - Filtering
    private func applyFilters() {
        var result = products
        
        // Search filter
        if !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty {
            let query = searchQuery.lowercased()
            result = result.filter {
                $0.name.lowercased().contains(query) ||
                $0.description_.lowercased().contains(query) ||
                $0.categoryName.lowercased().contains(query) ||
                $0.tags.lowercased().contains(query)
            }
        }
        
        // Category filter
        if selectedCategory != "All" {
            result = result.filter { $0.categoryName == selectedCategory }
        }
        
        // Price range filter
        if let min = minPrice {
            result = result.filter { (Double($0.price) ?? 0) >= min }
        }
        if let max = maxPrice {
            result = result.filter { (Double($0.price) ?? 0) <= max }
        }
        
        // Sort
        switch sortOption {
        case .newest:
            result.sort { $0.createdAt > $1.createdAt }
        case .priceLow:
            result.sort { (Double($0.price) ?? 0) < (Double($1.price) ?? 0) }
        case .priceHigh:
            result.sort { (Double($0.price) ?? 0) > (Double($1.price) ?? 0) }
        case .rating:
            result.sort { $0.rating > $1.rating }
        case .name:
            result.sort { $0.name.lowercased() < $1.name.lowercased() }
        }
        
        filteredProducts = result
    }
    
    // MARK: - Actions
    func clearFilters() {
        searchQuery = ""
        selectedCategory = "All"
        sortOption = .newest
        minPrice = nil
        maxPrice = nil
    }
    
    func setPriceRange(min: Double?, max: Double?) {
        self.minPrice = min
        self.maxPrice = max
    }
    
    func refresh() {
        isRefreshing = true
        errorMessage = nil
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
