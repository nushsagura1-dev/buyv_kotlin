import Foundation
import SwiftUI
import Shared

class SearchReelsViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var searchQuery: String = ""
    @Published var searchResults: [Product] = []
    @Published var trendingReels: [Product] = []
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var hasSearched: Bool = false
    @Published var selectedCategory: String = "All"
    @Published var errorMessage: String?
    @Published var searchHistory: [String] = [] {
        didSet { persistSearchHistory() }
    }
    
    private let getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
    private var allReels: [Product] = []
    private var searchWorkItem: DispatchWorkItem?
    private static let searchDebounceMs = 400
    private static let maxHistoryCount = 10
    private static let historyKey = "reels_search_history"
    
    let categories = ["All", "Fashion", "Electronics", "Beauty", "Home", "Sports", "Food"]
    
    // MARK: - Computed Properties
    var isEmpty: Bool { searchResults.isEmpty && hasSearched }
    var resultCount: Int { searchResults.count }
    var totalReelsCount: Int { allReels.count }
    var trendingCount: Int { trendingReels.count }
    var hasError: Bool { errorMessage != nil }
    var isSearchActive: Bool { !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty }
    
    var resultsSummary: String {
        if isEmpty { return "No results found" }
        return "\(resultCount) reel\(resultCount == 1 ? "" : "s") found"
    }
    
    // MARK: - Init
    init() {
        loadSearchHistory()
        loadAllReels()
    }
    
    // MARK: - Data Loading
    func loadAllReels() {
        isLoading = true
        errorMessage = nil
        
        getProductsUseCase.getAllProducts { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                self.isRefreshing = false
                
                if let success = result as? ResultSuccess<NSArray> {
                    if let products = success.data as? [Product] {
                        self.allReels = products.filter { !$0.reelVideoUrl.isEmpty }
                        // Trending: sorted by rating (best content rises to top)
                        self.trendingReels = Array(
                            self.allReels
                                .sorted { $0.rating > $1.rating }
                                .prefix(12)
                        )
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    // MARK: - Debounced Search
    func debouncedSearch(query: String) {
        searchQuery = query
        searchWorkItem?.cancel()
        
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            if hasSearched {
                searchResults = []
                hasSearched = false
            }
            return
        }
        
        let workItem = DispatchWorkItem { [weak self] in
            self?.search()
        }
        searchWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + .milliseconds(Self.searchDebounceMs),
            execute: workItem
        )
    }
    
    func search() {
        hasSearched = true
        let query = searchQuery.trimmingCharacters(in: .whitespaces).lowercased()
        
        guard !query.isEmpty else {
            searchResults = []
            return
        }
        
        // Save to history
        addToSearchHistory(query)
        
        searchResults = allReels.filter { reel in
            let matchesQuery = reel.name.lowercased().contains(query) ||
                reel.description_.lowercased().contains(query) ||
                reel.tags.lowercased().contains(query) ||
                reel.categoryName.lowercased().contains(query)
            
            let matchesCategory = selectedCategory == "All" ||
                reel.categoryName.lowercased() == selectedCategory.lowercased()
            
            return matchesQuery && matchesCategory
        }
    }
    
    // MARK: - Category Filter
    func filterByCategory(_ category: String) {
        selectedCategory = category
        if hasSearched && !searchQuery.isEmpty {
            search()
        }
    }
    
    // MARK: - Search History
    private func loadSearchHistory() {
        searchHistory = UserDefaults.standard.stringArray(forKey: Self.historyKey) ?? []
    }
    
    private func persistSearchHistory() {
        UserDefaults.standard.set(searchHistory, forKey: Self.historyKey)
    }
    
    private func addToSearchHistory(_ query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        
        // Remove existing entry if present, then insert at front
        searchHistory.removeAll { $0.lowercased() == trimmed.lowercased() }
        searchHistory.insert(trimmed, at: 0)
        
        // Keep max count
        if searchHistory.count > Self.maxHistoryCount {
            searchHistory = Array(searchHistory.prefix(Self.maxHistoryCount))
        }
    }
    
    func removeFromHistory(_ query: String) {
        searchHistory.removeAll { $0 == query }
    }
    
    func clearSearchHistory() {
        searchHistory = []
    }
    
    func selectFromHistory(_ query: String) {
        searchQuery = query
        search()
    }
    
    // MARK: - Actions
    func clearSearch() {
        searchQuery = ""
        searchResults = []
        hasSearched = false
        selectedCategory = "All"
        searchWorkItem?.cancel()
    }
    
    func refresh() {
        isRefreshing = true
        errorMessage = nil
        loadAllReels()
    }
    
    func retry() {
        errorMessage = nil
        loadAllReels()
    }
    
    func clearError() {
        errorMessage = nil
    }
}
