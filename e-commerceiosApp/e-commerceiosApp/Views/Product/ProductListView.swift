import SwiftUI
import Shared

struct ProductListView: View {
    @StateObject private var viewModel = ProductListViewModel()
    @State private var selectedCategory: String = "All"
    
    private let categories = ["All", "Fashion", "Electronics", "Home", "Beauty", "Sports", "Toys"]
    
    let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]
    
    private var filteredProducts: [Product] {
        if selectedCategory == "All" { return viewModel.products }
        return viewModel.products.filter { $0.category.localizedCaseInsensitiveContains(selectedCategory) }
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Featured Banner
                featuredBanner
                
                // Category Chips
                categoryChips
                
                // Sort Bar
                sortBar
                
                // Product Grid
                if viewModel.isLoading && viewModel.products.isEmpty {
                    ProgressView("Loading products...")
                        .padding(.top, 60)
                } else if filteredProducts.isEmpty {
                    emptyState
                } else {
                    productGrid
                }
            }
        }
        .navigationTitle("Products")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable {
            viewModel.loadProducts()
        }
        .onAppear {
            if viewModel.products.isEmpty {
                viewModel.loadProducts()
            }
        }
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
             Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("Retry"), action: {
                 viewModel.loadProducts()
             }))
        }
    }
    
    // MARK: - Featured Banner
    private var featuredBanner: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: [AppColors.primary, AppColors.primary.opacity(0.7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .frame(height: 140)
            
            VStack(alignment: .leading, spacing: 6) {
                Text("New Arrivals")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                Text("Discover the latest trending products")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.9))
                
                Button(action: {
                    viewModel.changeSortOption(.recent)
                }) {
                    Text("Shop Now")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                        .background(Color.white)
                        .cornerRadius(16)
                }
                .padding(.top, 4)
            }
            .padding()
            
            // Decorative
            HStack {
                Spacer()
                Image(systemName: "bag.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.white.opacity(0.15))
                    .padding(.trailing, 20)
            }
        }
        .cornerRadius(16)
        .padding(.horizontal)
        .padding(.top, 8)
    }
    
    // MARK: - Category Chips
    private var categoryChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(categories, id: \.self) { category in
                    Button(action: { withAnimation { selectedCategory = category } }) {
                        Text(category)
                            .font(.caption)
                            .fontWeight(selectedCategory == category ? .bold : .regular)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(
                                selectedCategory == category
                                    ? AppColors.primary
                                    : Color(.systemGray6)
                            )
                            .foregroundColor(
                                selectedCategory == category ? .white : .primary
                            )
                            .cornerRadius(20)
                    }
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 10)
    }
    
    // MARK: - Sort Bar
    private var sortBar: some View {
        HStack {
            Text("\(filteredProducts.count) products")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Spacer()
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(ProductSortOption.allCases, id: \.self) { option in
                        Button(action: { viewModel.changeSortOption(option) }) {
                            Text(option.rawValue)
                                .font(.caption2)
                                .fontWeight(viewModel.sortOption == option ? .bold : .regular)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(
                                    viewModel.sortOption == option
                                        ? AppColors.primary.opacity(0.15)
                                        : Color.clear
                                )
                                .foregroundColor(
                                    viewModel.sortOption == option ? AppColors.primary : .secondary
                                )
                                .cornerRadius(12)
                        }
                    }
                }
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 4)
    }
    
    // MARK: - Product Grid
    private var productGrid: some View {
        LazyVGrid(columns: columns, spacing: 14) {
            ForEach(filteredProducts, id: \.id) { product in
                NavigationLink(destination: ProductDetailView(productId: product.id)) {
                    ProductCard(product: product)
                }
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 16)
    }
    
    // MARK: - Empty State
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "bag.badge.questionmark")
                .font(.system(size: 50))
                .foregroundColor(.gray.opacity(0.5))
            Text("No products found")
                .font(.headline)
                .foregroundColor(.secondary)
            if selectedCategory != "All" {
                Button("Show All Products") {
                    selectedCategory = "All"
                }
                .foregroundColor(AppColors.primary)
            }
        }
        .padding(.top, 60)
    }
}

// MARK: - Product Card
struct ProductCard: View {
    let product: Product
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Image
            ZStack(alignment: .topTrailing) {
                AsyncImage(url: URL(string: product.imageUrl)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.gray.opacity(0.1)
                        .overlay(
                            ProgressView()
                        )
                }
                .frame(height: 160)
                .clipped()
                
                // Rating badge
                if product.rating > 0 {
                    HStack(spacing: 2) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 8))
                            .foregroundColor(.orange)
                        Text(String(format: "%.1f", product.rating))
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(8)
                    .padding(6)
                }
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)
                    .foregroundColor(.primary)
                
                // Category
                Text(product.category)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                
                // Price
                HStack(spacing: 4) {
                    Text("$\(String(format: "%.2f", product.price))")
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(AppColors.primary)
                    
                    if product.originalPrice > product.price {
                        Text("$\(String(format: "%.2f", product.originalPrice))")
                            .font(.caption2)
                            .strikethrough()
                            .foregroundColor(.gray)
                        
                        let discount = Int(((product.originalPrice - product.price) / product.originalPrice) * 100)
                        Text("-\(discount)%")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.red)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(4)
                    }
                }
            }
            .padding(8)
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
    }
}
