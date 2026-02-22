import SwiftUI
import Shared

/// Explore/Discover view — equivalent to Android ExploreScreenWithHeader
/// Shows categories, trending products, search, and featured content with engagement overlays
struct ExploreView: View {
    @StateObject private var viewModel = ExploreViewModel()
    @State private var searchText = ""
    @State private var selectedSegment = 0 // 0=Products, 1=Users
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Search Bar
                searchBar
                
                // Segment Control
                Picker("", selection: $selectedSegment) {
                    Text("Products").tag(0)
                    Text("Users").tag(1)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal)
                .padding(.vertical, 8)
                
                if selectedSegment == 0 {
                    productExploreContent
                } else {
                    userSearchContent
                }
            }
        }
        .navigationTitle("Explore")
        .onAppear {
            viewModel.loadCategories()
            viewModel.loadFeaturedProducts()
        }
        .refreshable {
            viewModel.refresh()
        }
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            TextField("Search products, users...", text: $searchText)
                .textFieldStyle(PlainTextFieldStyle())
                .autocapitalization(.none)
                .onChange(of: searchText) { newValue in
                    if selectedSegment == 0 {
                        viewModel.debouncedSearch(query: newValue)
                    }
                }
                .onSubmit {
                    if selectedSegment == 1 {
                        viewModel.searchUsers(query: searchText)
                    }
                }
            
            if viewModel.isSearching {
                ProgressView()
                    .scaleEffect(0.7)
            }
            
            if !searchText.isEmpty {
                Button(action: {
                    searchText = ""
                    viewModel.clearSearch()
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(10)
        .background(Color.gray.opacity(0.1))
        .cornerRadius(10)
        .padding(.horizontal)
        .padding(.top, 8)
    }
    
    // MARK: - Product Explore Content
    private var productExploreContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Error state
                if let error = viewModel.errorMessage {
                    errorBanner(message: error)
                }
                
                // Show search results if searching
                if viewModel.isSearchActive || !viewModel.searchedProducts.isEmpty {
                    if viewModel.searchedProducts.isEmpty && !viewModel.isSearching {
                        emptySearchState
                    } else {
                        productGrid(title: "Search Results (\(viewModel.searchedProducts.count))", products: viewModel.searchedProducts)
                    }
                } else {
                    // Quick Access Shortcuts
                    quickAccessSection
                    
                    // Categories
                    if !viewModel.categories.isEmpty {
                        categoriesSection
                    }
                    
                    // Trending Products (sorted by rating)
                    if !viewModel.trendingProducts.isEmpty {
                        trendingSection
                    }
                    
                    // Featured Products
                    if !viewModel.featuredProducts.isEmpty {
                        productGrid(title: "Featured", products: viewModel.featuredProducts)
                    }
                    
                    // All Products
                    if !viewModel.allProducts.isEmpty {
                        productGrid(title: "All Products", products: viewModel.allProducts)
                    }
                }
                
                if viewModel.isLoading {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                    .padding()
                }
            }
            .padding(.vertical)
        }
    }
    
    // MARK: - Trending Section
    private var trendingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "flame.fill")
                    .foregroundColor(.orange)
                Text("Trending")
                    .font(.headline)
                Spacer()
                Text("\(viewModel.trendingCount)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color.gray.opacity(0.15))
                    .cornerRadius(8)
            }
            .padding(.horizontal)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.trendingProducts, id: \.id) { product in
                        NavigationLink(destination: ProductDetailView(productId: String(product.id))) {
                            TrendingProductCard(product: product)
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }
    
    // MARK: - Quick Access Section
    private var quickAccessSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                NavigationLink(destination: SearchReelsView()) {
                    QuickAccessCard(icon: "play.magnifyingglass", title: "Search Reels", color: .pink)
                }
                NavigationLink(destination: RecentlyViewedView()) {
                    QuickAccessCard(icon: "clock.arrow.circlepath", title: "Recently Viewed", color: .purple)
                }
                NavigationLink(destination: AllProductsView()) {
                    QuickAccessCard(icon: "square.grid.2x2", title: "All Products", color: .teal)
                }
            }
            .padding(.horizontal)
        }
    }
    
    // MARK: - Categories Section
    private var categoriesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Categories")
                .font(.headline)
                .padding(.horizontal)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.categories, id: \.id) { category in
                        Button(action: {
                            viewModel.filterByCategory(categoryId: String(category.id))
                        }) {
                            VStack(spacing: 8) {
                                Circle()
                                    .fill(viewModel.selectedCategoryId == String(category.id) 
                                        ? AppColors.primary 
                                        : Color.gray.opacity(0.15))
                                    .frame(width: 56, height: 56)
                                    .overlay(
                                        Image(systemName: categoryIcon(for: category.name))
                                            .font(.system(size: 22))
                                            .foregroundColor(
                                                viewModel.selectedCategoryId == String(category.id)
                                                    ? .white : AppColors.primary
                                            )
                                    )
                                
                                Text(category.name)
                                    .font(.caption2)
                                    .foregroundColor(.primary)
                                    .lineLimit(1)
                            }
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }
    
    // MARK: - Product Grid (with engagement overlays)
    @ViewBuilder
    private func productGrid(title: String, products: [Product]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.headline)
                Spacer()
                Text("\(products.count)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)
            
            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 12),
                GridItem(.flexible(), spacing: 12)
            ], spacing: 12) {
                ForEach(products, id: \.id) { product in
                    NavigationLink(destination: ProductDetailView(productId: String(product.id))) {
                        ExploreProductCard(product: product)
                    }
                }
            }
            .padding(.horizontal)
        }
    }
    
    // MARK: - User Search Content
    private var userSearchContent: some View {
        UserSearchView()
    }
    
    // MARK: - Empty Search State
    private var emptySearchState: some View {
        VStack(spacing: 16) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 40))
                .foregroundColor(.gray.opacity(0.4))
            Text("No results for \"\(searchText)\"")
                .font(.headline)
                .foregroundColor(.secondary)
            Text("Try a different search term")
                .font(.subheadline)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }
    
    // MARK: - Error Banner
    private func errorBanner(message: String) -> some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.orange)
            Text(message)
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
            Button("Retry") {
                viewModel.retry()
            }
            .font(.caption.bold())
            .foregroundColor(AppColors.primary)
        }
        .padding(12)
        .background(Color.orange.opacity(0.1))
        .cornerRadius(10)
        .padding(.horizontal)
    }
    
    // MARK: - Helpers
    private func categoryIcon(for name: String) -> String {
        switch name.lowercased() {
        case let n where n.contains("electron"): return "desktopcomputer"
        case let n where n.contains("cloth") || n.contains("fashion"): return "tshirt.fill"
        case let n where n.contains("sport"): return "sportscourt.fill"
        case let n where n.contains("home"): return "house.fill"
        case let n where n.contains("beauty"): return "sparkles"
        case let n where n.contains("book"): return "book.fill"
        case let n where n.contains("toy"): return "gamecontroller.fill"
        case let n where n.contains("food"): return "fork.knife"
        case let n where n.contains("health"): return "heart.fill"
        default: return "tag.fill"
        }
    }
}

// MARK: - Quick Access Card
struct QuickAccessCard: View {
    let icon: String
    let title: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(color.opacity(0.12))
                    .frame(width: 70, height: 70)
                Image(systemName: icon)
                    .font(.system(size: 26))
                    .foregroundColor(color)
            }
            Text(title)
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .lineLimit(1)
        }
    }
}

// MARK: - Trending Product Card (horizontal scroll)
struct TrendingProductCard: View {
    let product: Product
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .topTrailing) {
                // Product Image
                if !product.imageUrl.isEmpty, let url = URL(string: product.imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        default:
                            Rectangle().fill(Color.gray.opacity(0.1))
                                .overlay(Image(systemName: "photo").foregroundColor(.gray))
                        }
                    }
                    .frame(width: 140, height: 180)
                    .clipped()
                    .cornerRadius(10)
                } else {
                    Rectangle()
                        .fill(Color.gray.opacity(0.1))
                        .frame(width: 140, height: 180)
                        .cornerRadius(10)
                        .overlay(Image(systemName: "photo").foregroundColor(.gray))
                }
                
                // Rating badge
                if product.rating > 0 {
                    HStack(spacing: 2) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 8))
                        Text(String(format: "%.1f", product.rating))
                            .font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(6)
                    .padding(6)
                }
            }
            
            Text(product.name)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .lineLimit(1)
                .frame(width: 140, alignment: .leading)
            
            Text("$\(String(format: "%.2f", product.price))")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(AppColors.primary)
        }
    }
}

// MARK: - Explore Product Card (with engagement overlays)
struct ExploreProductCard: View {
    let product: Product
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Product Image with engagement overlay
            ZStack(alignment: .bottom) {
                if !product.imageUrl.isEmpty, let url = URL(string: product.imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        default:
                            Rectangle().fill(Color.gray.opacity(0.1))
                                .overlay(
                                    Image(systemName: "photo")
                                        .foregroundColor(.gray)
                                )
                        }
                    }
                    .frame(height: 150)
                    .clipped()
                    .cornerRadius(8)
                } else {
                    Rectangle()
                        .fill(Color.gray.opacity(0.1))
                        .frame(height: 150)
                        .cornerRadius(8)
                        .overlay(Image(systemName: "photo").foregroundColor(.gray))
                }
                
                // Engagement overlay — like & comment counts
                HStack(spacing: 10) {
                    HStack(spacing: 3) {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 10))
                        Text("\(product.likesCount)")
                            .font(.system(size: 10, weight: .semibold))
                    }
                    
                    HStack(spacing: 3) {
                        Image(systemName: "bubble.left.fill")
                            .font(.system(size: 10))
                        Text("\(product.commentsCount)")
                            .font(.system(size: 10, weight: .semibold))
                    }
                    
                    Spacer()
                    
                    if product.rating > 0 {
                        HStack(spacing: 2) {
                            Image(systemName: "star.fill")
                                .font(.system(size: 9))
                            Text(String(format: "%.1f", product.rating))
                                .font(.system(size: 10, weight: .semibold))
                        }
                    }
                }
                .foregroundColor(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .background(
                    LinearGradient(
                        colors: [.clear, .black.opacity(0.6)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .cornerRadius(8, corners: [.bottomLeft, .bottomRight])
            }
            
            Text(product.name)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .lineLimit(2)
            
            Text("$\(String(format: "%.2f", product.price))")
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(AppColors.primary)
        }
        .padding(8)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

// MARK: - Corner Radius Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
