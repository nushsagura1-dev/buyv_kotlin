import Foundation
import SwiftUI
import Combine

// MARK: - Sort Options
enum MarketplaceSortOption: String, CaseIterable {
    case relevance = "relevance"
    case priceLowToHigh = "price_low_to_high"
    case priceHighToLow = "price_high_to_low"
    case commission = "commission"
    case newest = "newest"
    case popular = "popular"
    
    var displayName: String {
        switch self {
        case .relevance: return "Relevance"
        case .priceLowToHigh: return "Price ↑"
        case .priceHighToLow: return "Price ↓"
        case .commission: return "Commission ↑"
        case .newest: return "Newest"
        case .popular: return "Popular"
        }
    }
    
    var icon: String {
        switch self {
        case .relevance: return "line.3.horizontal.decrease"
        case .priceLowToHigh: return "arrow.up"
        case .priceHighToLow: return "arrow.down"
        case .commission: return "percent"
        case .newest: return "clock"
        case .popular: return "flame"
        }
    }
}

@MainActor
class MarketplaceViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var products: [MarketplaceProductResponse] = []
    @Published var categories: [CategoryResponse] = []
    @Published var featuredProducts: [MarketplaceProductResponse] = []
    
    @Published var searchQuery: String = ""
    @Published var selectedCategory: String?
    @Published var selectedSort: MarketplaceSortOption = .relevance
    @Published var minPrice: Double?
    @Published var maxPrice: Double?
    @Published var minCommission: Double?
    
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var errorMessage: String?
    @Published var showFilters: Bool = false
    
    // Pagination
    private var currentPage: Int = 1
    private var totalPages: Int = 1
    private let pageSize: Int = 20
    var hasMorePages: Bool { currentPage < totalPages }
    
    private let api = MarketplaceApiService.shared
    private var cancellables = Set<AnyCancellable>()
    private var searchTask: Task<Void, Never>?
    
    // MARK: - Init
    init() {
        setupSearchDebounce()
    }
    
    private func setupSearchDebounce() {
        $searchQuery
            .debounce(for: .milliseconds(400), scheduler: RunLoop.main)
            .removeDuplicates()
            .sink { [weak self] query in
                guard let self = self else { return }
                if query.count >= 3 || query.isEmpty {
                    self.resetAndLoad()
                }
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Data Loading
    
    func loadInitialData() {
        Task {
            isLoading = true
            errorMessage = nil
            
            async let categoriesTask = api.getCategories()
            async let featuredTask = api.getFeaturedProducts(limit: 6)
            async let productsTask = api.getProducts(
                category: selectedCategory,
                minPrice: minPrice,
                maxPrice: maxPrice,
                minCommission: minCommission,
                search: searchQuery.isEmpty ? nil : searchQuery,
                sortBy: selectedSort.rawValue,
                page: 1,
                limit: pageSize
            )
            
            do {
                let (cats, featured, productList) = try await (categoriesTask, featuredTask, productsTask)
                self.categories = cats
                self.featuredProducts = featured
                self.products = productList.items
                self.totalPages = productList.totalPages
                self.currentPage = 1
            } catch {
                self.errorMessage = error.localizedDescription
            }
            
            self.isLoading = false
        }
    }
    
    func resetAndLoad() {
        currentPage = 1
        products = []
        loadProducts()
    }
    
    func loadProducts() {
        guard !isLoading else { return }
        
        Task {
            isLoading = currentPage == 1
            isLoadingMore = currentPage > 1
            errorMessage = nil
            
            do {
                let result = try await api.getProducts(
                    category: selectedCategory,
                    minPrice: minPrice,
                    maxPrice: maxPrice,
                    minCommission: minCommission,
                    search: searchQuery.isEmpty ? nil : searchQuery,
                    sortBy: selectedSort.rawValue,
                    page: currentPage,
                    limit: pageSize
                )
                
                if currentPage == 1 {
                    products = result.items
                } else {
                    products.append(contentsOf: result.items)
                }
                
                totalPages = result.totalPages
            } catch {
                errorMessage = error.localizedDescription
            }
            
            isLoading = false
            isLoadingMore = false
        }
    }
    
    func loadNextPage() {
        guard hasMorePages && !isLoadingMore else { return }
        currentPage += 1
        loadProducts()
    }
    
    // MARK: - Filters
    
    func selectCategory(_ category: String?) {
        selectedCategory = category
        resetAndLoad()
    }
    
    func selectSort(_ sort: MarketplaceSortOption) {
        selectedSort = sort
        resetAndLoad()
    }
    
    func applyAdvancedFilters(minPrice: Double?, maxPrice: Double?, minCommission: Double?) {
        self.minPrice = minPrice
        self.maxPrice = maxPrice
        self.minCommission = minCommission
        resetAndLoad()
    }
    
    func clearFilters() {
        selectedCategory = nil
        selectedSort = .relevance
        minPrice = nil
        maxPrice = nil
        minCommission = nil
        searchQuery = ""
        resetAndLoad()
    }
}
