import Foundation
import Shared

class RecentlyViewedViewModel: ObservableObject {
    @Published var recentProducts: [Product] = []
    @Published var isLoading: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var errorMessage: String?
    @Published var showClearConfirmation = false
    
    private let getProductsUseCase = DependencyWrapper.shared.getProductsUseCase
    private let storageKey = "recently_viewed_reels"
    private let maxItems = 50
    
    // MARK: - Computed Properties
    var itemCount: Int { recentProducts.count }
    var isEmpty: Bool { recentProducts.isEmpty && !isLoading }
    var hasError: Bool { errorMessage != nil }
    var hasProducts: Bool { !recentProducts.isEmpty }
    
    var formattedItemCount: String {
        "\(itemCount) item\(itemCount == 1 ? "" : "s")"
    }
    
    var storedIds: [String] {
        UserDefaults.standard.stringArray(forKey: storageKey) ?? []
    }
    
    // MARK: - Init
    init() {
        loadRecentlyViewed()
    }
    
    // MARK: - Data Loading
    func loadRecentlyViewed() {
        let recentIds = UserDefaults.standard.stringArray(forKey: storageKey) ?? []
        guard !recentIds.isEmpty else {
            recentProducts = []
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        getProductsUseCase.getAllProducts { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                self.isRefreshing = false
                
                if let success = result as? ResultSuccess<NSArray> {
                    if let products = success.data as? [Product] {
                        // Maintain order from recently viewed list
                        self.recentProducts = recentIds.compactMap { id in
                            products.first { String($0.id) == id }
                        }
                    }
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                } else if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // MARK: - Clear & Remove
    func clearHistory() {
        UserDefaults.standard.removeObject(forKey: storageKey)
        recentProducts = []
    }
    
    func removeItem(productId: String) {
        var ids = UserDefaults.standard.stringArray(forKey: storageKey) ?? []
        ids.removeAll { $0 == productId }
        UserDefaults.standard.set(ids, forKey: storageKey)
        
        // Animate removal from UI
        withAnimation(.easeInOut(duration: 0.3)) {
            recentProducts.removeAll { String($0.id) == productId }
        }
    }
    
    func removeItems(at offsets: IndexSet) {
        let idsToRemove = offsets.map { String(recentProducts[$0].id) }
        var storedIds = UserDefaults.standard.stringArray(forKey: storageKey) ?? []
        storedIds.removeAll { idsToRemove.contains($0) }
        UserDefaults.standard.set(storedIds, forKey: storageKey)
        recentProducts.remove(atOffsets: offsets)
    }
    
    // MARK: - Actions
    func refresh() {
        isRefreshing = true
        loadRecentlyViewed()
    }
    
    func retry() {
        errorMessage = nil
        loadRecentlyViewed()
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    /// Call this statically from product detail / reel views to record history
    static func addToRecentlyViewed(productId: String) {
        let key = "recently_viewed_reels"
        var ids = UserDefaults.standard.stringArray(forKey: key) ?? []
        ids.removeAll { $0 == productId } // Remove duplicate
        ids.insert(productId, at: 0) // Add to front
        if ids.count > 50 {
            ids = Array(ids.prefix(50))
        }
        UserDefaults.standard.set(ids, forKey: key)
    }
    
    /// Check if a product was recently viewed
    static func isRecentlyViewed(productId: String) -> Bool {
        let key = "recently_viewed_reels"
        let ids = UserDefaults.standard.stringArray(forKey: key) ?? []
        return ids.contains(productId)
    }
}
}
