import SwiftUI
import Shared

struct ProductSearchView: View {
    @StateObject private var viewModel = ProductSearchViewModel()
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search Bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    
                    TextField("Search products...", text: $viewModel.searchQuery)
                        .textFieldStyle(.plain)
                    
                    if !viewModel.searchQuery.isEmpty {
                        Button(action: {
                            viewModel.searchQuery = ""
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                    }
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top, 8)
                
                // Filter Button
                HStack {
                    Button(action: {
                        withAnimation {
                            viewModel.showFilters.toggle()
                        }
                    }) {
                        HStack {
                            Image(systemName: "line.3.horizontal.decrease.circle")
                            Text("Filters")
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(viewModel.hasActiveFilters ? AppColors.primary : Color(.systemGray6))
                        .foregroundColor(viewModel.hasActiveFilters ? .white : .primary)
                        .cornerRadius(8)
                    }
                    
                    Spacer()
                    
                    Text("\(viewModel.filteredProducts.count) results")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    if viewModel.hasActiveFilters {
                        Button("Clear All") {
                            viewModel.clearFilters()
                        }
                        .font(.footnote)
                        .foregroundColor(AppColors.primary)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                
                // Filters Section (Expandable)
                if viewModel.showFilters {
                    VStack(alignment: .leading, spacing: 16) {
                        // Category Filter
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Category")
                                .font(.headline)
                            
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(viewModel.categories, id: \.self) { category in
                                        Button(action: {
                                            viewModel.toggleCategory(category)
                                        }) {
                                            Text(category)
                                                .font(.footnote)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(
                                                    viewModel.selectedCategory == category ?
                                                    AppColors.primary : Color(.systemGray6)
                                                )
                                                .foregroundColor(
                                                    viewModel.selectedCategory == category ?
                                                    .white : .primary
                                                )
                                                .cornerRadius(16)
                                        }
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }
                        
                        // Price Range Filter
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Price Range")
                                .font(.headline)
                            
                            HStack(spacing: 12) {
                                TextField("Min", text: Binding(
                                    get: { viewModel.minPrice?.description ?? "" },
                                    set: { viewModel.minPrice = Double($0) }
                                ))
                                .textFieldStyle(.roundedBorder)
                                .keyboardType(.decimalPad)
                                
                                Text("—")
                                    .foregroundColor(.gray)
                                
                                TextField("Max", text: Binding(
                                    get: { viewModel.maxPrice?.description ?? "" },
                                    set: { viewModel.maxPrice = Double($0) }
                                ))
                                .textFieldStyle(.roundedBorder)
                                .keyboardType(.decimalPad)
                            }
                        }
                        .padding(.horizontal)
                    }
                    .padding(.vertical)
                    .background(Color(.systemGray6).opacity(0.5))
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
                
                Divider()
                
                // Results
                if viewModel.isLoading {
                    ProgressView("Loading products...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.errorMessage != nil {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 50))
                            .foregroundColor(AppColors.primary)
                        Text(viewModel.errorMessage ?? "")
                            .multilineTextAlignment(.center)
                        Button("Retry") {
                            viewModel.loadProducts()
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.primary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.filteredProducts.isEmpty && !viewModel.showEmptyState {
                    VStack(spacing: 16) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 50))
                            .foregroundColor(.gray)
                        Text("Search for products or use filters")
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.filteredProducts.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 50))
                            .foregroundColor(.gray)
                        Text("No products found")
                            .foregroundColor(.gray)
                        Text("Try adjusting your filters")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.filteredProducts, id: \.id) { product in
                                NavigationLink(destination: ProductDetailView(productName: product.name)) {
                                    ProductSearchCard(product: product)
                                }
                            }
                        }
                        .padding()
                    }
                    .refreshable {
                        viewModel.loadProducts()
                    }
                }
            }
            .navigationTitle("Search Products")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                viewModel.loadProducts()
            }
        }
    }
}

// MARK: - Product Search Card (Enhanced)
struct ProductSearchCard: View {
    let product: Product
    @State private var isFavorite = false
    @State private var addedToCart = false
    
    var body: some View {
        HStack(spacing: 12) {
            // Product Image with rating overlay
            ZStack(alignment: .bottomLeading) {
                AsyncImage(url: URL(string: product.images.first ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "photo")
                            .font(.title2)
                            .foregroundColor(.gray)
                    default:
                        ProgressView()
                    }
                }
                .frame(width: 90, height: 90)
                .background(Color.gray.opacity(0.1))
                .cornerRadius(10)
                .clipped()
                
                // Rating badge
                if product.rating > 0 {
                    HStack(spacing: 2) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 8))
                        Text(String(format: "%.1f", product.rating))
                            .font(.system(size: 9, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(Color.black.opacity(0.65))
                    .cornerRadius(4)
                    .padding(4)
                }
            }
            
            // Product Info
            VStack(alignment: .leading, spacing: 4) {
                Text(product.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(2)
                    .foregroundColor(.primary)
                
                Text(product.category)
                    .font(.caption)
                    .foregroundColor(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(AppColors.chipsColor)
                    .cornerRadius(4)
                
                Spacer()
                
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 0) {
                        if product.originalPrice > 0 && product.originalPrice > product.price {
                            Text("$\(String(format: "%.2f", product.originalPrice))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .strikethrough()
                        }
                        Text("$\(product.price, specifier: "%.2f")")
                            .font(.headline)
                            .foregroundColor(AppColors.primary)
                    }
                    
                    Spacer()
                    
                    // Quick Add to Cart
                    Button(action: {
                        addToCart()
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: addedToCart ? "checkmark" : "cart.badge.plus")
                                .font(.caption)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(addedToCart ? Color.green : AppColors.primary)
                        .foregroundColor(.white)
                        .cornerRadius(6)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            // Favorite button
            VStack {
                Button(action: {
                    withAnimation(.spring(response: 0.3)) {
                        isFavorite.toggle()
                    }
                }) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .font(.system(size: 18))
                        .foregroundColor(isFavorite ? .red : .gray)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.gray)
                    .font(.caption)
            }
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
    
    private func addToCart() {
        guard !addedToCart else { return }
        let userId = SessionManager.shared.currentUserId
        let cartItem = CartItem(
            id: UUID().uuidString,
            productId: product.id,
            productName: product.name,
            productImage: product.images.first ?? product.imageUrl,
            price: product.price,
            quantity: 1,
            size: nil,
            color: nil,
            attributes: [:],
            addedAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        DependencyWrapper.shared.addToCartUseCase.invoke(userId: userId, item: cartItem) { result, error in
            DispatchQueue.main.async {
                if result is ResultSuccess<KotlinUnit> {
                    withAnimation { addedToCart = true }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        withAnimation { addedToCart = false }
                    }
                }
            }
        }
    }
}
