import Foundation
import Shared

class ExploreViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var categories: [Category] = []
    @Published var featuredProducts: [Product] = []
    @Published var trendingProducts: [Product] = []
    @Published var allProducts: [Product] = []
    @Published var searchedProducts: [Product] = []
    @Published var searchedUsers: [UserProfile] = []
    @Published var selectedCategoryId: String?
    @Published var isLoading = false
    @Published var isSearching = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var searchQuery: String = ""
    
    private let getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
    private let getCategoriesUseCase = DependencyWrapper.shared.getCategoriesUseCase
    private let searchUsersUseCase = DependencyWrapper.shared.searchUsersUseCase
    
    // MARK: - Debounce
    private var searchWorkItem: DispatchWorkItem?
    private static let searchDebounceMs = 400
    
    // MARK: - Computed Properties
    var isEmpty: Bool { allProducts.isEmpty && !isLoading }
    var hasResults: Bool { !searchedProducts.isEmpty || !searchedUsers.isEmpty }
    var hasError: Bool { errorMessage != nil }
    var isSearchActive: Bool { !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty }
    var featuredCount: Int { featuredProducts.count }
    var trendingCount: Int { trendingProducts.count }
    
    var categoryNames: [String] {
        categories.compactMap { $0.name.isEmpty ? nil : $0.name }
    }
    
    // MARK: - Data Loading
    func loadCategories() {
        getCategoriesUseCase.invoke { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<NSArray> {
                    if let categories = success.data as? [Category] {
                        self.categories = categories
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    func loadFeaturedProducts() {
        isLoading = true
        errorMessage = nil
        getProductsUseCase.getAllProducts { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                self.isRefreshing = false
                if let success = result as? ResultSuccess<NSArray> {
                    if let products = success.data as? [Product] {
                        self.allProducts = products
                        // Featured: first 6 products
                        self.featuredProducts = Array(products.prefix(6))
                        // Trending: top-rated products (sorted by rating descending)
                        self.trendingProducts = Array(
                            products.sorted { $0.rating > $1.rating }.prefix(8)
                        )
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    // MARK: - Search (Debounced)
    func debouncedSearch(query: String) {
        searchQuery = query
        searchWorkItem?.cancel()
        
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            clearSearch()
            return
        }
        
        let workItem = DispatchWorkItem { [weak self] in
            self?.searchProducts(query: query)
            self?.searchUsers(query: query)
        }
        searchWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + .milliseconds(Self.searchDebounceMs),
            execute: workItem
        )
    }
    
    func searchProducts(query: String) {
        guard !query.isEmpty else {
            clearSearch()
            return
        }
        
        isSearching = true
        // Filter from already-loaded products for instant results
        let localResults = allProducts.filter { product in
            product.name.localizedCaseInsensitiveContains(query) ||
            product.description_.localizedCaseInsensitiveContains(query) ||
            product.categoryName.localizedCaseInsensitiveContains(query) ||
            product.tags.localizedCaseInsensitiveContains(query)
        }
        self.searchedProducts = localResults
        
        // Also fetch from backend for comprehensive results
        getProductsUseCase.getAllProducts { result, error in
            DispatchQueue.main.async {
                self.isSearching = false
                if let success = result as? ResultSuccess<NSArray> {
                    if let products = success.data as? [Product] {
                        self.searchedProducts = products.filter { product in
                            product.name.localizedCaseInsensitiveContains(query) ||
                            product.description_.localizedCaseInsensitiveContains(query) ||
                            product.categoryName.localizedCaseInsensitiveContains(query) ||
                            product.tags.localizedCaseInsensitiveContains(query)
                        }
                    }
                }
            }
        }
    }
    
    func searchUsers(query: String) {
        guard !query.isEmpty else { return }
        
        searchUsersUseCase.invoke(query: query) { result, error in
            DispatchQueue.main.async {
                if let success = result as? ResultSuccess<NSArray> {
                    if let users = success.data as? [UserProfile] {
                        self.searchedUsers = users
                    }
                }
            }
        }
    }
    
    // MARK: - Category Filtering
    func filterByCategory(categoryId: String) {
        if selectedCategoryId == categoryId {
            // Deselect
            selectedCategoryId = nil
            searchedProducts = []
        } else {
            selectedCategoryId = categoryId
            // Filter from loaded products by category
            searchedProducts = allProducts.filter { product in
                String(product.categoryId) == categoryId
            }
        }
    }
    
    // MARK: - Actions
    func clearSearch() {
        searchQuery = ""
        searchedProducts = []
        searchedUsers = []
        selectedCategoryId = nil
        isSearching = false
        searchWorkItem?.cancel()
    }
    
    func refresh() {
        isRefreshing = true
        errorMessage = nil
        loadCategories()
        loadFeaturedProducts()
    }
    
    func retry() {
        errorMessage = nil
        loadCategories()
        loadFeaturedProducts()
    }
    
    func clearError() {
        errorMessage = nil
    }
}
