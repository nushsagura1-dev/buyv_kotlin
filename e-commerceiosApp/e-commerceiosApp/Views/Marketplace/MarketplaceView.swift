import SwiftUI

struct MarketplaceView: View {
    @StateObject private var viewModel = MarketplaceViewModel()
    @State private var showFilterSheet = false
    @State private var filterMinPrice = ""
    @State private var filterMaxPrice = ""
    @State private var filterMinCommission = ""
    
    var body: some View {
        VStack(spacing: 0) {
            // Search Bar
            searchBar
            
            // Category Filter Row
            if !viewModel.categories.isEmpty {
                categoryFilterRow
            }
            
            // Sort Options Row
            sortOptionsRow
            
            // Content
            if viewModel.isLoading && viewModel.products.isEmpty {
                loadingView
            } else if let error = viewModel.errorMessage, viewModel.products.isEmpty {
                errorView(error)
            } else if viewModel.products.isEmpty {
                emptyView
            } else {
                productGrid
            }
        }
        .navigationTitle("Marketplace")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showFilterSheet = true }) {
                    Image(systemName: "slider.horizontal.3")
                        .foregroundColor(hasActiveFilters ? .orange : .primary)
                }
            }
        }
        .sheet(isPresented: $showFilterSheet) {
            filterBottomSheet
        }
        .onAppear {
            if viewModel.products.isEmpty {
                viewModel.loadInitialData()
            }
        }
    }
    
    private var hasActiveFilters: Bool {
        viewModel.minPrice != nil || viewModel.maxPrice != nil || viewModel.minCommission != nil
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search marketplace products...", text: $viewModel.searchQuery)
                .textFieldStyle(.plain)
            
            if !viewModel.searchQuery.isEmpty {
                Button(action: { viewModel.searchQuery = "" }) {
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
    }
    
    // MARK: - Category Row
    private var categoryFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // "All" chip
                FilterChipView(
                    title: "All",
                    isSelected: viewModel.selectedCategory == nil,
                    action: { viewModel.selectCategory(nil) }
                )
                
                ForEach(viewModel.categories) { category in
                    FilterChipView(
                        title: category.name,
                        isSelected: viewModel.selectedCategory == category.id,
                        action: { viewModel.selectCategory(category.id) }
                    )
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }
    
    // MARK: - Sort Row
    private var sortOptionsRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(MarketplaceSortOption.allCases, id: \.self) { option in
                    Button(action: { viewModel.selectSort(option) }) {
                        HStack(spacing: 4) {
                            Image(systemName: option.icon)
                                .font(.caption2)
                            Text(option.displayName)
                                .font(.caption)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(viewModel.selectedSort == option ? Color.orange : Color(.systemGray5))
                        .foregroundColor(viewModel.selectedSort == option ? .white : .primary)
                        .cornerRadius(16)
                    }
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
    }
    
    // MARK: - Product Grid
    private var productGrid: some View {
        ScrollView {
            // Featured Section
            if !viewModel.featuredProducts.isEmpty && viewModel.searchQuery.isEmpty && viewModel.selectedCategory == nil {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "star.fill")
                            .foregroundColor(.yellow)
                        Text("Featured Products")
                            .font(.headline)
                    }
                    .padding(.horizontal)
                    
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(viewModel.featuredProducts) { product in
                                FeaturedProductCard(product: product)
                                    .frame(width: 200)
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical, 8)
                
                Divider()
                    .padding(.horizontal)
            }
            
            // Product Grid
            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 12),
                GridItem(.flexible(), spacing: 12)
            ], spacing: 12) {
                ForEach(viewModel.products) { product in
                    NavigationLink(destination: MarketplaceProductDetailView(productId: product.id)) {
                        MarketplaceProductCard(product: product)
                    }
                    .buttonStyle(.plain)
                    .onAppear {
                        // Load more when reaching the end
                        if product.id == viewModel.products.last?.id {
                            viewModel.loadNextPage()
                        }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.top, 8)
            
            // Load More Indicator
            if viewModel.isLoadingMore {
                ProgressView()
                    .padding()
            }
            
            // Bottom Padding
            Color.clear.frame(height: 20)
        }
        .refreshable {
            viewModel.loadProducts()
        }
    }
    
    // MARK: - States
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
            Text("Loading products...")
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.orange)
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundColor(.gray)
            Button("Retry") {
                viewModel.loadInitialData()
            }
            .buttonStyle(.borderedProminent)
            .tint(.orange)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
    
    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: "storefront")
                .font(.system(size: 50))
                .foregroundColor(.gray)
            Text("No products found")
                .font(.headline)
                .foregroundColor(.gray)
            Text("Try adjusting your filters or search terms")
                .font(.caption)
                .foregroundColor(.secondary)
            
            if hasActiveFilters {
                Button("Clear Filters") {
                    viewModel.clearFilters()
                }
                .buttonStyle(.borderedProminent)
                .tint(.orange)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    // MARK: - Filter Sheet
    private var filterBottomSheet: some View {
        NavigationView {
            Form {
                Section("Price Range") {
                    HStack {
                        TextField("Min Price", text: $filterMinPrice)
                            .keyboardType(.decimalPad)
                        Text("-")
                        TextField("Max Price", text: $filterMaxPrice)
                            .keyboardType(.decimalPad)
                    }
                }
                
                Section("Commission") {
                    TextField("Min Commission %", text: $filterMinCommission)
                        .keyboardType(.decimalPad)
                }
                
                Section {
                    Button("Apply Filters") {
                        viewModel.applyAdvancedFilters(
                            minPrice: Double(filterMinPrice),
                            maxPrice: Double(filterMaxPrice),
                            minCommission: Double(filterMinCommission)
                        )
                        showFilterSheet = false
                    }
                    .frame(maxWidth: .infinity)
                    .foregroundColor(.white)
                    .listRowBackground(Color.orange)
                    
                    Button("Clear All") {
                        filterMinPrice = ""
                        filterMaxPrice = ""
                        filterMinCommission = ""
                        viewModel.clearFilters()
                        showFilterSheet = false
                    }
                    .frame(maxWidth: .infinity)
                    .foregroundColor(.red)
                }
            }
            .navigationTitle("Advanced Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { showFilterSheet = false }
                }
            }
        }
        .onAppear {
            filterMinPrice = viewModel.minPrice.map { String($0) } ?? ""
            filterMaxPrice = viewModel.maxPrice.map { String($0) } ?? ""
            filterMinCommission = viewModel.minCommission.map { String($0) } ?? ""
        }
    }
}

// MARK: - Filter Chip View
struct FilterChipView: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.orange : Color(.systemGray5))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

// MARK: - Product Card
struct MarketplaceProductCard: View {
    let product: MarketplaceProductResponse
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Image
            ZStack(alignment: .topTrailing) {
                AsyncImage(url: URL(string: product.displayImage)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color(.systemGray5)
                        .overlay(
                            Image(systemName: "photo")
                                .font(.title2)
                                .foregroundColor(.gray)
                        )
                }
                .frame(height: 160)
                .clipped()
                .cornerRadius(10)
                
                // Commission Badge
                if product.commissionRate > 0 {
                    Text(product.formattedCommission)
                        .font(.caption2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                        .padding(6)
                }
            }
            
            // Name
            Text(product.name)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(2)
                .foregroundColor(.primary)
            
            // Price
            HStack(spacing: 4) {
                Text(product.formattedPrice)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
                
                if product.originalPrice > product.sellingPrice {
                    Text(String(format: "$%.2f", product.originalPrice))
                        .font(.caption2)
                        .strikethrough()
                        .foregroundColor(.gray)
                }
            }
            
            // Commission Estimate
            if product.commissionRate > 0 {
                Text("Est. \(String(format: "$%.2f", product.estimatedCommission)) commission")
                    .font(.caption2)
                    .foregroundColor(.green)
            }
        }
    }
}

// MARK: - Featured Product Card
struct FeaturedProductCard: View {
    let product: MarketplaceProductResponse
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            AsyncImage(url: URL(string: product.displayImage)) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Color(.systemGray5)
            }
            .frame(height: 120)
            .clipped()
            .cornerRadius(10)
            
            Text(product.name)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(1)
            
            HStack {
                Text(product.formattedPrice)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
                
                Spacer()
                
                if product.commissionRate > 0 {
                    HStack(spacing: 2) {
                        Image(systemName: "percent")
                            .font(.system(size: 8))
                        Text(product.formattedCommission)
                            .font(.caption2)
                    }
                    .foregroundColor(.green)
                }
            }
        }
        .padding(8)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Marketplace Product Detail View
struct MarketplaceProductDetailView: View {
    let productId: String
    @State private var product: MarketplaceProductResponse?
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var selectedImageIndex = 0
    
    var body: some View {
        Group {
            if isLoading {
                ProgressView("Loading product...")
            } else if let error = errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text(error)
                        .multilineTextAlignment(.center)
                }
            } else if let product = product {
                productDetailContent(product)
            }
        }
        .navigationTitle("Product Details")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadProduct()
        }
    }
    
    private func loadProduct() async {
        isLoading = true
        do {
            product = try await MarketplaceApiService.shared.getProduct(productId: productId)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
    
    private func productDetailContent(_ product: MarketplaceProductResponse) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Image Gallery
                if let images = product.images, !images.isEmpty {
                    TabView(selection: $selectedImageIndex) {
                        ForEach(images.indices, id: \.self) { index in
                            AsyncImage(url: URL(string: images[index])) { image in
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                            } placeholder: {
                                Color(.systemGray5)
                            }
                            .tag(index)
                        }
                    }
                    .tabViewStyle(.page)
                    .frame(height: 300)
                } else {
                    AsyncImage(url: URL(string: product.displayImage)) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    } placeholder: {
                        Color(.systemGray5)
                    }
                    .frame(height: 300)
                }
                
                VStack(alignment: .leading, spacing: 12) {
                    // Category
                    if let categoryName = product.categoryName {
                        Text(categoryName)
                            .font(.caption)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.orange.opacity(0.2))
                            .foregroundColor(.orange)
                            .cornerRadius(8)
                    }
                    
                    // Name
                    Text(product.name)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    // Price
                    HStack(alignment: .bottom, spacing: 8) {
                        Text(product.formattedPrice)
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                        
                        if product.originalPrice > product.sellingPrice {
                            Text(String(format: "$%.2f", product.originalPrice))
                                .font(.subheadline)
                                .strikethrough()
                                .foregroundColor(.gray)
                            
                            let discount = Int(((product.originalPrice - product.sellingPrice) / product.originalPrice) * 100)
                            Text("-\(discount)%")
                                .font(.caption)
                                .fontWeight(.bold)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.red)
                                .foregroundColor(.white)
                                .cornerRadius(4)
                        }
                    }
                    
                    // Commission Info
                    if product.commissionRate > 0 {
                        HStack {
                            Image(systemName: "dollarsign.circle.fill")
                                .foregroundColor(.green)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Commission: \(product.formattedCommission)")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.green)
                                Text("Estimated: \(String(format: "$%.2f", product.estimatedCommission)) per sale")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding()
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(10)
                    }
                    
                    Divider()
                    
                    // Stats
                    HStack(spacing: 20) {
                        StatItem(title: "Sales", value: "\(product.totalSales ?? 0)", icon: "cart.fill")
                        StatItem(title: "Views", value: "\(product.totalViews ?? 0)", icon: "eye.fill")
                        if let rating = product.averageRating, rating > 0 {
                            StatItem(title: "Rating", value: String(format: "%.1f", rating), icon: "star.fill")
                        }
                    }
                    
                    Divider()
                    
                    // Description
                    Text("Description")
                        .font(.headline)
                    
                    Text(product.description)
                        .font(.body)
                        .foregroundColor(.secondary)
                    
                    // Tags
                    if let tags = product.tags, !tags.isEmpty {
                        Divider()
                        Text("Tags")
                            .font(.headline)
                        
                        FlowLayout(spacing: 8) {
                            ForEach(tags, id: \.self) { tag in
                                Text("#\(tag)")
                                    .font(.caption)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(Color(.systemGray5))
                                    .cornerRadius(12)
                            }
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

// MARK: - Stat Item
struct StatItem: View {
    let title: String
    let value: String
    let icon: String
    
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.orange)
            Text(value)
                .font(.subheadline)
                .fontWeight(.bold)
            Text(title)
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Flow Layout (for tags)
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(in: proposal.replacingUnspecifiedDimensions().width, subviews: subviews, spacing: spacing)
        return result.size
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(in: bounds.width, subviews: subviews, spacing: spacing)
        for (index, subview) in subviews.enumerated() {
            let point = result.positions[index]
            subview.place(at: CGPoint(x: point.x + bounds.minX, y: point.y + bounds.minY), proposal: .unspecified)
        }
    }
    
    struct FlowResult {
        var positions: [CGPoint] = []
        var size: CGSize = .zero
        
        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var x: CGFloat = 0
            var y: CGFloat = 0
            var rowHeight: CGFloat = 0
            
            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)
                
                if x + size.width > maxWidth && x > 0 {
                    x = 0
                    y += rowHeight + spacing
                    rowHeight = 0
                }
                
                positions.append(CGPoint(x: x, y: y))
                rowHeight = max(rowHeight, size.height)
                x += size.width + spacing
                
                self.size.width = max(self.size.width, x)
            }
            
            self.size.height = y + rowHeight
        }
    }
}

#Preview {
    NavigationView {
        MarketplaceView()
    }
}
