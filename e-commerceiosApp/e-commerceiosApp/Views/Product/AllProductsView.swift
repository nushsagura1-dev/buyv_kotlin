import SwiftUI
import Shared

struct AllProductsView: View {
    @StateObject private var viewModel = AllProductsViewModel()
    @State private var viewMode: ViewMode = .grid
    @State private var showFilters = false
    
    enum ViewMode: String, CaseIterable {
        case grid = "square.grid.2x2"
        case list = "list.bullet"
    }
    
    private let gridColumns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]
    
    var body: some View {
        ZStack {
            Color(.systemGroupedBackground).ignoresSafeArea()
            
            if viewModel.isLoading && viewModel.products.isEmpty {
                ProgressView("Loading products...")
            } else if let error = viewModel.errorMessage, viewModel.products.isEmpty {
                errorState(message: error)
            } else if viewModel.products.isEmpty {
                emptyState
            } else {
                VStack(spacing: 0) {
                    // Search bar
                    searchBar
                    
                    // Toolbar: sort + view mode + filter count
                    toolbarRow
                    
                    // Category filter
                    categoryFilter
                    
                    // Active filters indicator
                    if viewModel.hasActiveFilters {
                        activeFiltersBar
                    }
                    
                    // Products
                    ScrollView {
                        if viewModel.isEmpty {
                            noResultsState
                        } else if viewMode == .grid {
                            gridView
                        } else {
                            listView
                        }
                    }
                }
            }
        }
        .navigationTitle("All Products")
        .refreshable {
            viewModel.refresh()
        }
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            TextField("Search products...", text: $viewModel.searchQuery)
                .textFieldStyle(PlainTextFieldStyle())
                .autocapitalization(.none)
            if !viewModel.searchQuery.isEmpty {
                Button {
                    viewModel.searchQuery = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(10)
        .background(Color.gray.opacity(0.1))
        .cornerRadius(10)
        .padding(.horizontal, 12)
        .padding(.top, 8)
    }
    
    // MARK: - Toolbar
    private var toolbarRow: some View {
        HStack {
            // Products count
            Text("\(viewModel.filteredCount) of \(viewModel.totalCount) products")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            // Sort menu
            Menu {
                ForEach(AllProductsViewModel.SortOption.allCases, id: \.self) { option in
                    Button(action: { viewModel.sortOption = option }) {
                        HStack {
                            Image(systemName: option.icon)
                            Text(option.label)
                            if viewModel.sortOption == option {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: viewModel.sortOption.icon)
                    Text(viewModel.sortOption.label)
                        .font(.caption)
                }
                .foregroundColor(AppColors.primary)
            }
            
            // View toggle
            Picker("View", selection: $viewMode) {
                ForEach(ViewMode.allCases, id: \.self) { mode in
                    Image(systemName: mode.rawValue)
                }
            }
            .pickerStyle(.segmented)
            .frame(width: 80)
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Active Filters Bar
    private var activeFiltersBar: some View {
        HStack {
            if viewModel.selectedCategory != "All" {
                filterChip(label: viewModel.selectedCategory, icon: "tag.fill")
            }
            if viewModel.minPrice != nil || viewModel.maxPrice != nil {
                let minStr = viewModel.minPrice.map { "$\(String(format: "%.0f", $0))" } ?? "$0"
                let maxStr = viewModel.maxPrice.map { "$\(String(format: "%.0f", $0))" } ?? "..."
                filterChip(label: "\(minStr) - \(maxStr)", icon: "dollarsign.circle")
            }
            if !viewModel.searchQuery.isEmpty {
                filterChip(label: "\"\(viewModel.searchQuery)\"", icon: "magnifyingglass")
            }
            
            Spacer()
            
            Button {
                viewModel.clearFilters()
            } label: {
                Text("Clear All")
                    .font(.caption.bold())
                    .foregroundColor(.red)
            }
        }
        .padding(.horizontal, 12)
        .padding(.bottom, 4)
    }
    
    private func filterChip(label: String, icon: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 10))
            Text(label)
                .font(.caption2)
                .lineLimit(1)
        }
        .foregroundColor(AppColors.primary)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(AppColors.primary.opacity(0.1))
        .cornerRadius(12)
    }
    
    // MARK: - Category Filter
    private var categoryFilter: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.categories, id: \.self) { category in
                    Button(action: { viewModel.selectedCategory = category }) {
                        Text(category)
                            .font(.caption)
                            .fontWeight(viewModel.selectedCategory == category ? .semibold : .regular)
                            .foregroundColor(viewModel.selectedCategory == category ? .white : .primary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(
                                viewModel.selectedCategory == category
                                    ? AppColors.primary
                                    : Color(.systemGray5)
                            )
                            .cornerRadius(16)
                    }
                }
            }
            .padding(.horizontal)
        }
        .padding(.bottom, 8)
    }
    
    // MARK: - Grid View
    private var gridView: some View {
        LazyVGrid(columns: gridColumns, spacing: 12) {
            ForEach(viewModel.filteredProducts, id: \.id) { product in
                NavigationLink(destination: ProductDetailView(productId: String(product.id))) {
                    ProductGridCard(product: product)
                }
            }
        }
        .padding(12)
    }
    
    // MARK: - List View
    private var listView: some View {
        LazyVStack(spacing: 8) {
            ForEach(viewModel.filteredProducts, id: \.id) { product in
                NavigationLink(destination: ProductDetailView(productId: String(product.id))) {
                    ProductListCard(product: product)
                }
            }
        }
        .padding(.horizontal, 12)
    }
    
    // MARK: - Empty State
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "bag")
                .font(.system(size: 56))
                .foregroundColor(.gray.opacity(0.4))
            Text("No products found")
                .font(.headline)
                .foregroundColor(.secondary)
            Button("Retry") {
                viewModel.retry()
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.primary)
        }
    }
    
    // MARK: - No Results State (filters applied but no matches)
    private var noResultsState: some View {
        VStack(spacing: 16) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 40))
                .foregroundColor(.gray.opacity(0.4))
            Text("No matching products")
                .font(.headline)
                .foregroundColor(.secondary)
            Text("Try adjusting your filters or search")
                .font(.subheadline)
                .foregroundColor(.gray)
            Button("Clear Filters") {
                viewModel.clearFilters()
            }
            .font(.subheadline.bold())
            .foregroundColor(AppColors.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }
    
    // MARK: - Error State
    private func errorState(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundColor(.orange)
            Text("Failed to load products")
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button("Try Again") {
                viewModel.retry()
            }
            .buttonStyle(.borderedProminent)
            .tint(AppColors.primary)
        }
        .padding()
    }
}

// MARK: - Product Grid Card
struct ProductGridCard: View {
    let product: Product
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Image
            AsyncImage(url: URL(string: product.image)) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().aspectRatio(contentMode: .fill)
                case .failure:
                    Color.gray.opacity(0.2)
                        .overlay(Image(systemName: "photo").foregroundColor(.gray))
                default:
                    Color.gray.opacity(0.1).overlay(ProgressView())
                }
            }
            .frame(height: 160)
            .clipped()
            .cornerRadius(8)
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                    .lineLimit(2)
                
                if !product.categoryName.isEmpty {
                    Text(product.categoryName)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Text("$\(String(format: "%.2f", Double(product.price) ?? 0.0))")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                    
                    if product.originalPrice > 0 && product.originalPrice > (Double(product.price) ?? 0.0) {
                        Text("$\(String(format: "%.0f", product.originalPrice))")
                            .font(.caption2)
                            .foregroundColor(.gray)
                            .strikethrough()
                    }
                }
                
                // Rating
                if product.rating > 0 {
                    HStack(spacing: 2) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 10))
                            .foregroundColor(.orange)
                        Text(String(format: "%.1f", product.rating))
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal, 4)
        }
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Product List Card
struct ProductListCard: View {
    let product: Product
    
    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: product.image)) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().aspectRatio(contentMode: .fill)
                default:
                    Color.gray.opacity(0.2)
                        .overlay(Image(systemName: "photo").foregroundColor(.gray))
                }
            }
            .frame(width: 80, height: 80)
            .clipped()
            .cornerRadius(8)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                    .lineLimit(2)
                
                if !product.categoryName.isEmpty {
                    Text(product.categoryName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Text("$\(String(format: "%.2f", Double(product.price) ?? 0.0))")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                    
                    Spacer()
                    
                    if product.rating > 0 {
                        HStack(spacing: 2) {
                            Image(systemName: "star.fill")
                                .font(.system(size: 10))
                                .foregroundColor(.orange)
                            Text(String(format: "%.1f", product.rating))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.gray)
        }
        .padding(10)
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.03), radius: 2, x: 0, y: 1)
    }
}

#Preview {
    NavigationView {
        AllProductsView()
    }
}
