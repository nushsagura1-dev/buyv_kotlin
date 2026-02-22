import Foundation
import SwiftUI

@MainActor
class AdminCJImportViewModel: ObservableObject {
    // Search state
    @Published var searchQuery = ""
    @Published var selectedCategory = "All"
    @Published var searchResults: [CJProduct] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var totalResults = 0
    @Published var currentPage = 1
    @Published var errorMessage: String?
    
    // Import state
    @Published var selectedProduct: CJProduct?
    @Published var isImporting = false
    @Published var importingProductId: String?
    @Published var importedProductIds: Set<String> = []
    @Published var showImportDialog = false
    @Published var showImportSuccess = false
    @Published var lastImportedProduct: CJImportResponse?
    @Published var importError: String?
    
    // Import form fields
    @Published var sellingPrice = ""
    @Published var commissionRate = "10"
    @Published var customDescription = ""
    
    let categories = ["All", "Electronics", "Clothing", "Home & Garden", "Beauty", "Sports", "Toys", "Jewelry", "Automotive", "Pet Supplies"]
    
    var hasMorePages: Bool {
        searchResults.count < totalResults
    }
    
    func searchProducts() async {
        guard !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        
        isLoading = true
        errorMessage = nil
        currentPage = 1
        searchResults = []
        
        do {
            let response = try await AdminApiService.shared.searchCJProducts(
                query: searchQuery,
                category: selectedCategory == "All" ? nil : selectedCategory,
                page: 1
            )
            searchResults = response.products
            totalResults = response.total
            currentPage = response.page
            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
    
    func loadNextPage() async {
        guard hasMorePages, !isLoadingMore else { return }
        
        isLoadingMore = true
        let nextPage = currentPage + 1
        
        do {
            let response = try await AdminApiService.shared.searchCJProducts(
                query: searchQuery,
                category: selectedCategory == "All" ? nil : selectedCategory,
                page: nextPage
            )
            searchResults.append(contentsOf: response.products)
            totalResults = response.total
            currentPage = response.page
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoadingMore = false
    }
    
    func selectProduct(_ product: CJProduct) {
        selectedProduct = product
        sellingPrice = String(format: "%.2f", product.sellPrice * 1.5) // Default 50% markup
        commissionRate = "10"
        customDescription = ""
        importError = nil
        showImportDialog = true
    }
    
    func importProduct() async {
        guard let product = selectedProduct else { return }
        
        guard let price = Double(sellingPrice), price > 0 else {
            importError = "Please enter a valid selling price"
            return
        }
        guard let commission = Double(commissionRate), commission >= 0, commission <= 100 else {
            importError = "Commission rate must be between 0 and 100"
            return
        }
        
        isImporting = true
        importingProductId = product.productId
        importError = nil
        
        do {
            let request = CJImportRequest(
                cjProductId: product.productId,
                cjVariantId: product.variants?.first?.variantId,
                commissionRate: commission,
                categoryId: nil,
                customDescription: customDescription.isEmpty ? nil : customDescription,
                sellingPrice: price
            )
            
            let response = try await AdminApiService.shared.importCJProduct(request: request)
            lastImportedProduct = response
            importedProductIds.insert(product.productId)
            showImportDialog = false
            showImportSuccess = true
        } catch {
            importError = error.localizedDescription
        }
        
        isImporting = false
        importingProductId = nil
    }
    
    func dismissImportSuccess() {
        showImportSuccess = false
        lastImportedProduct = nil
    }
    
    func clearSearch() {
        searchQuery = ""
        searchResults = []
        totalResults = 0
        currentPage = 1
    }
    
    func selectCategory(_ category: String) {
        selectedCategory = category
        if !searchQuery.isEmpty {
            Task { await searchProducts() }
        }
    }
}
